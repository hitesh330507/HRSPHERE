package com.hrsphere.leave.service;

import com.hrsphere.common.event.EventPublisher;
import com.hrsphere.common.event.EventType;
import com.hrsphere.common.exception.*;
import com.hrsphere.leave.dto.*;
import com.hrsphere.leave.entity.LeaveBalance;
import com.hrsphere.leave.entity.LeaveRequest;
import com.hrsphere.leave.entity.LeaveType;
import com.hrsphere.leave.entity.enums.LeaveStatus;
import com.hrsphere.leave.event.LeaveApprovedPayload;
import com.hrsphere.leave.mapper.LeaveMapper;
import com.hrsphere.leave.repository.LeaveBalanceRepository;
import com.hrsphere.leave.repository.LeaveRequestRepository;
import com.hrsphere.leave.repository.LeaveTypeRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class LeaveService {

  private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

  private final LeaveTypeRepository leaveTypeRepository;
  private final LeaveRequestRepository leaveRequestRepository;
  private final LeaveBalanceRepository leaveBalanceRepository;
  private final LeaveMapper leaveMapper;
  private final RestTemplate restTemplate;
  private final EventPublisher eventPublisher;
  private final MeterRegistry meterRegistry;

  private final String employeeServiceUrl;

  public LeaveService(
      LeaveTypeRepository leaveTypeRepository,
      LeaveRequestRepository leaveRequestRepository,
      LeaveBalanceRepository leaveBalanceRepository,
      LeaveMapper leaveMapper,
      RestTemplate restTemplate,
      EventPublisher eventPublisher,
      MeterRegistry meterRegistry,
      @Value("${employee-service.base-url:http://employee-service:8082}")
          String employeeServiceUrl) {
    this.leaveTypeRepository = leaveTypeRepository;
    this.leaveRequestRepository = leaveRequestRepository;
    this.leaveBalanceRepository = leaveBalanceRepository;
    this.leaveMapper = leaveMapper;
    this.restTemplate = restTemplate;
    this.eventPublisher = eventPublisher;
    this.meterRegistry = meterRegistry;
    this.employeeServiceUrl = employeeServiceUrl;
  }

  // ------------------------------------------------------------------ //
  //  APPLY LEAVE
  // ------------------------------------------------------------------ //

  @Transactional
  public LeaveRequestResponse applyForLeave(String authUsername, ApplyLeaveRequest req) {
    log.info("Employee '{}' applying for leave", authUsername);

    // 1. Resolve employee identity via lookup
    EmployeeLookupResponse employee = lookupEmployeeByUsername(authUsername);

    // 2. Resolve Leave Type
    LeaveType leaveType =
        leaveTypeRepository
            .findById(req.leaveTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("Leave type not found"));

    // 3. Date Validation
    if (req.endDate.isBefore(req.startDate)) {
      throw new InvalidReferenceException("End date must be on or after start date");
    }

    int numberOfDays = (int) ChronoUnit.DAYS.between(req.startDate, req.endDate) + 1;
    int year = req.startDate.getYear();

    // 4. Check for overlapping PENDING or APPROVED requests
    boolean overlaps =
        leaveRequestRepository.hasOverlappingRequest(
            employee.employeeId, req.startDate, req.endDate);
    if (overlaps) {
      throw new OverlappingLeaveRequestException("Leave request overlaps with an existing request");
    }

    // 5. Retrieve or lazily allocate balance
    LeaveBalance balance = getOrAllocateBalance(employee.employeeId, leaveType, year);

    // 6. Check balance sufficiency
    if (balance.getRemainingDays() < numberOfDays) {
      throw new InsufficientLeaveBalanceException(
          String.format(
              "Insufficient leave balance. Remaining: %d, Requested: %d",
              balance.getRemainingDays(), numberOfDays));
    }

    // 7. Save Leave Request
    LeaveRequest request = new LeaveRequest();
    request.setEmployeeId(employee.employeeId);
    request.setLeaveType(leaveType);
    request.setStartDate(req.startDate);
    request.setEndDate(req.endDate);
    request.setNumberOfDays(numberOfDays);
    request.setReason(req.reason);
    request.setStatus(LeaveStatus.PENDING);
    request.setAppliedAt(LocalDateTime.now());

    LeaveRequest savedRequest = leaveRequestRepository.save(request);
    log.info(
        "Leave request created for employee '{}' with ID {}", authUsername, savedRequest.getId());

    LeaveRequestResponse resp = leaveMapper.toResponse(savedRequest);
    resp.employeeName = employee.firstName + " " + employee.lastName;

    try {
      meterRegistry.counter("leave_requests_applied", "leaveType", leaveType.getCode()).increment();
    } catch (Exception e) {
      log.warn("Failed to increment leave_requests_applied counter: {}", e.getMessage());
    }

    return resp;
  }

  // ------------------------------------------------------------------ //
  //  REVIEW LEAVE REQUEST
  // ------------------------------------------------------------------ //

  @Transactional
  public LeaveRequestResponse reviewLeaveRequest(
      UUID requestId, ReviewLeaveRequest req, String reviewerUsername) {
    log.info("Reviewing leave request {} by reviewer '{}'", requestId, reviewerUsername);

    LeaveRequest request =
        leaveRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

    if (request.getStatus() != LeaveStatus.PENDING) {
      throw new InvalidStateTransitionException("Only pending leave requests can be reviewed");
    }

    int year = request.getStartDate().getYear();
    LeaveBalance balance =
        leaveBalanceRepository
            .findByEmployeeIdAndLeaveTypeIdAndYear(
                request.getEmployeeId(), request.getLeaveType().getId(), year)
            .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));

    if (req.decision == ReviewDecision.APPROVE) {
      // Re-validate balance sufficiency at decision time
      if (balance.getRemainingDays() < request.getNumberOfDays()) {
        throw new InsufficientLeaveBalanceException(
            String.format(
                "Insufficient leave balance at approval time. Remaining: %d, Requested: %d",
                balance.getRemainingDays(), request.getNumberOfDays()));
      }

      // Deduct balance
      balance.setUsedDays(balance.getUsedDays() + request.getNumberOfDays());
      balance.setRemainingDays(balance.getRemainingDays() - request.getNumberOfDays());
      balance.setUpdatedAt(LocalDateTime.now());
      leaveBalanceRepository.save(balance);

      request.setStatus(LeaveStatus.APPROVED);

      try {
        meterRegistry.counter("leave_requests_approved", "leaveType", request.getLeaveType().getCode()).increment();
      } catch (Exception ex) {
        log.warn("Failed to increment leave_requests_approved counter: {}", ex.getMessage());
      }

      // Publish event (fire-and-forget, log error if Redis fails)
      try {
        LeaveApprovedPayload payload =
            new LeaveApprovedPayload(
                request.getEmployeeId(),
                request.getLeaveType().getCode(),
                request.getStartDate(),
                request.getEndDate(),
                request.getNumberOfDays());
        eventPublisher.publish(EventType.LEAVE_APPROVED, "leave-service", payload);
        log.info("Published leave.approved event for employee {}", request.getEmployeeId());
      } catch (Exception ex) {
        log.error("Failed to publish leave.approved event to Redis backbone: {}", ex.getMessage());
      }

    } else {
      request.setStatus(LeaveStatus.REJECTED);
      try {
        meterRegistry.counter("leave_requests_rejected", "leaveType", request.getLeaveType().getCode()).increment();
      } catch (Exception ex) {
        log.warn("Failed to increment leave_requests_rejected counter: {}", ex.getMessage());
      }
    }

    request.setReviewedBy(reviewerUsername);
    request.setReviewedAt(LocalDateTime.now());
    request.setReviewComments(req.comments);
    request.setUpdatedAt(LocalDateTime.now());

    LeaveRequest reviewedRequest = leaveRequestRepository.save(request);
    log.info("Leave request {} reviewed successfully", requestId);

    try {
      if (request.getAppliedAt() != null && reviewedRequest.getReviewedAt() != null) {
        Duration duration = Duration.between(request.getAppliedAt(), reviewedRequest.getReviewedAt());
        meterRegistry.timer("leave_request_review_duration").record(duration);
      }
    } catch (Exception ex) {
      log.warn("Failed to record leave_request_review_duration timer: {}", ex.getMessage());
    }

    LeaveRequestResponse resp = leaveMapper.toResponse(reviewedRequest);
    resp.employeeName = fetchEmployeeName(reviewedRequest.getEmployeeId());
    return resp;
  }

  // ------------------------------------------------------------------ //
  //  CANCEL LEAVE REQUEST
  // ------------------------------------------------------------------ //

  @Transactional
  public LeaveRequestResponse cancelLeaveRequest(UUID requestId, String authUsername) {
    log.info("Employee '{}' requesting cancellation of leave request {}", authUsername, requestId);

    EmployeeLookupResponse employee = lookupEmployeeByUsername(authUsername);

    LeaveRequest request =
        leaveRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

    if (!request.getEmployeeId().equals(employee.employeeId)) {
      throw new AccessForbiddenException("You do not have permission to cancel this leave request");
    }

    if (request.getStatus() != LeaveStatus.PENDING && request.getStatus() != LeaveStatus.APPROVED) {
      throw new InvalidStateTransitionException(
          "Cannot cancel a request that is not pending or approved");
    }

    if (request.getStatus() == LeaveStatus.APPROVED) {
      // Refund balance
      int year = request.getStartDate().getYear();
      LeaveBalance balance =
          leaveBalanceRepository
              .findByEmployeeIdAndLeaveTypeIdAndYear(
                  request.getEmployeeId(), request.getLeaveType().getId(), year)
              .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));

      balance.setUsedDays(balance.getUsedDays() - request.getNumberOfDays());
      balance.setRemainingDays(balance.getRemainingDays() + request.getNumberOfDays());
      balance.setUpdatedAt(LocalDateTime.now());
      leaveBalanceRepository.save(balance);
      log.info(
          "Refunded {} days to employee {}", request.getNumberOfDays(), request.getEmployeeId());
    }

    request.setStatus(LeaveStatus.CANCELLED);
    request.setUpdatedAt(LocalDateTime.now());

    LeaveRequest cancelledRequest = leaveRequestRepository.save(request);
    log.info("Leave request {} cancelled", requestId);

    LeaveRequestResponse resp = leaveMapper.toResponse(cancelledRequest);
    resp.employeeName = employee.firstName + " " + employee.lastName;
    return resp;
  }

  // ------------------------------------------------------------------ //
  //  GET BALANCES
  // ------------------------------------------------------------------ //

  @Transactional
  public List<LeaveBalanceResponse> getMyBalances(String authUsername, Integer year) {
    EmployeeLookupResponse employee = lookupEmployeeByUsername(authUsername);
    int checkYear = (year != null) ? year : LocalDate.now().getYear();

    log.info("Retrieving balances for employee '{}' for year {}", authUsername, checkYear);

    // Lazily allocate balances for any seeded leave types
    List<LeaveType> leaveTypes = leaveTypeRepository.findAll();
    for (LeaveType type : leaveTypes) {
      getOrAllocateBalance(employee.employeeId, type, checkYear);
    }

    List<LeaveBalance> balances =
        leaveBalanceRepository.findAllByEmployeeIdAndYear(employee.employeeId, checkYear);
    List<LeaveBalanceResponse> responses = new ArrayList<>();
    for (LeaveBalance b : balances) {
      responses.add(leaveMapper.toBalanceResponse(b));
    }
    return responses;
  }

  // ------------------------------------------------------------------ //
  //  READ SINGLE REQUEST
  // ------------------------------------------------------------------ //

  @Transactional(readOnly = true)
  public LeaveRequestResponse getLeaveRequestById(
      UUID requestId, String authUsername, String authRoles) {
    LeaveRequest request =
        leaveRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

    boolean isHrOrAdmin =
        authRoles != null && (authRoles.contains("ROLE_HR") || authRoles.contains("ROLE_ADMIN"));

    if (!isHrOrAdmin) {
      EmployeeLookupResponse employee = lookupEmployeeByUsername(authUsername);
      if (!request.getEmployeeId().equals(employee.employeeId)) {
        throw new AccessForbiddenException("Access denied");
      }
    }

    LeaveRequestResponse resp = leaveMapper.toResponse(request);
    resp.employeeName = fetchEmployeeName(request.getEmployeeId());
    return resp;
  }

  // ------------------------------------------------------------------ //
  //  LIST REQUESTS (MY / ALL)
  // ------------------------------------------------------------------ //

  @Transactional(readOnly = true)
  public Page<LeaveRequestSummaryResponse> listMyLeaveRequests(
      String authUsername, Pageable pageable) {
    EmployeeLookupResponse employee = lookupEmployeeByUsername(authUsername);
    Page<LeaveRequest> requests =
        leaveRequestRepository.findAllByEmployeeId(employee.employeeId, pageable);

    String name = employee.firstName + " " + employee.lastName;
    return requests.map(
        r -> {
          LeaveRequestSummaryResponse summary = leaveMapper.toSummaryResponse(r);
          summary.employeeName = name;
          return summary;
        });
  }

  @Transactional(readOnly = true)
  public Page<LeaveRequestSummaryResponse> listAllLeaveRequests(
      LeaveStatus status, Pageable pageable) {
    Page<LeaveRequest> requests;
    if (status != null) {
      requests = leaveRequestRepository.findAllByStatus(status, pageable);
    } else {
      requests = leaveRequestRepository.findAll(pageable);
    }

    // Defensive lookup caching to prevent duplicate REST requests inside the page batch
    Map<UUID, String> employeeNameCache = new HashMap<>();

    return requests.map(
        r -> {
          LeaveRequestSummaryResponse summary = leaveMapper.toSummaryResponse(r);
          summary.employeeName =
              employeeNameCache.computeIfAbsent(r.getEmployeeId(), this::fetchEmployeeName);
          return summary;
        });
  }

  // ------------------------------------------------------------------ //
  //  HELPERS & REST CLIENTS
  // ------------------------------------------------------------------ //

  private LeaveBalance getOrAllocateBalance(UUID employeeId, LeaveType leaveType, int year) {
    Optional<LeaveBalance> opt =
        leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(
            employeeId, leaveType.getId(), year);

    if (opt.isPresent()) {
      return opt.get();
    }

    log.info(
        "Allocating leave balance for employee {}, leave type {}, year {}",
        employeeId,
        leaveType.getCode(),
        year);
    LeaveBalance balance = new LeaveBalance();
    balance.setEmployeeId(employeeId);
    balance.setLeaveType(leaveType);
    balance.setYear(year);
    balance.setAllocatedDays(leaveType.getDefaultAnnualDays());
    balance.setUsedDays(0);
    balance.setRemainingDays(leaveType.getDefaultAnnualDays());

    return leaveBalanceRepository.save(balance);
  }

  private EmployeeLookupResponse lookupEmployeeByUsername(String username) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-Auth-Validated", "true");
      headers.set("X-Auth-Username", "leave-service");
      headers.set("X-Auth-Roles", "ROLE_ADMIN");
      HttpEntity<Void> entity = new HttpEntity<>(headers);

      String url = employeeServiceUrl + "/employees/lookup?authUsername=" + username;
      log.debug("Sending lookup request to employee-service: {}", url);

      ResponseEntity<EmployeeLookupResponse> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, EmployeeLookupResponse.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        return response.getBody();
      }
      throw new ResourceNotFoundException("Employee profile not found");
    } catch (org.springframework.web.client.HttpClientErrorException.NotFound nf) {
      throw new ResourceNotFoundException("Employee not found for username: " + username);
    }
  }

  private String fetchEmployeeName(UUID employeeId) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-Auth-Validated", "true");
      headers.set("X-Auth-Username", "leave-service");
      headers.set("X-Auth-Roles", "ROLE_ADMIN");
      HttpEntity<Void> entity = new HttpEntity<>(headers);

      String url = employeeServiceUrl + "/employees/" + employeeId;
      log.debug("Sending employee fetch request to: {}", url);

      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        Object first = response.getBody().get("firstName");
        Object last = response.getBody().get("lastName");
        return (first != null ? first.toString() : "")
            + " "
            + (last != null ? last.toString() : "");
      }
      return "Unknown Employee";
    } catch (Exception e) {
      log.warn("Failed to fetch name for employee {}: {}", employeeId, e.getMessage());
      return "Unknown Employee";
    }
  }
}
