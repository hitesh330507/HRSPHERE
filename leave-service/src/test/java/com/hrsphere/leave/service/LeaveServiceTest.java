package com.hrsphere.leave.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hrsphere.common.event.EventPublisher;
import com.hrsphere.common.exception.*;
import com.hrsphere.leave.dto.*;
import com.hrsphere.leave.entity.LeaveBalance;
import com.hrsphere.leave.entity.LeaveRequest;
import com.hrsphere.leave.entity.LeaveType;
import com.hrsphere.leave.entity.enums.LeaveStatus;
import com.hrsphere.leave.mapper.LeaveMapper;
import com.hrsphere.leave.repository.LeaveBalanceRepository;
import com.hrsphere.leave.repository.LeaveRequestRepository;
import com.hrsphere.leave.repository.LeaveTypeRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class LeaveServiceTest {

  @Mock private LeaveTypeRepository leaveTypeRepository;
  @Mock private LeaveRequestRepository leaveRequestRepository;
  @Mock private LeaveBalanceRepository leaveBalanceRepository;
  @Mock private LeaveMapper leaveMapper;
  @Mock private RestTemplate restTemplate;
  @Mock private EventPublisher eventPublisher;

  private LeaveService leaveService;

  private final UUID employeeId = UUID.randomUUID();
  private final UUID leaveTypeId = UUID.randomUUID();
  private final String username = "testuser";

  @BeforeEach
  void setUp() {
    leaveService =
        new LeaveService(
            leaveTypeRepository,
            leaveRequestRepository,
            leaveBalanceRepository,
            leaveMapper,
            restTemplate,
            eventPublisher,
            "http://employee-service:8082");
  }

  @Test
  void testApplyForLeave_Success() {
    // Arrange
    ApplyLeaveRequest req = new ApplyLeaveRequest();
    req.leaveTypeId = leaveTypeId;
    req.startDate = LocalDate.of(2026, 7, 10);
    req.endDate = LocalDate.of(2026, 7, 12); // 3 days
    req.reason = "Vacation";

    EmployeeLookupResponse empLookup = new EmployeeLookupResponse(employeeId, "Hitesh", "L");
    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(EmployeeLookupResponse.class)))
        .thenReturn(ResponseEntity.ok(empLookup));

    LeaveType leaveType = new LeaveType();
    leaveType.setId(leaveTypeId);
    leaveType.setCode("AL");
    leaveType.setDefaultAnnualDays(20);
    when(leaveTypeRepository.findById(leaveTypeId)).thenReturn(Optional.of(leaveType));

    when(leaveRequestRepository.hasOverlappingRequest(employeeId, req.startDate, req.endDate))
        .thenReturn(false);

    LeaveBalance balance = new LeaveBalance();
    balance.setEmployeeId(employeeId);
    balance.setLeaveType(leaveType);
    balance.setYear(2026);
    balance.setAllocatedDays(20);
    balance.setUsedDays(0);
    balance.setRemainingDays(20);
    when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(
            employeeId, leaveTypeId, 2026))
        .thenReturn(Optional.of(balance));

    LeaveRequest savedRequest = new LeaveRequest();
    savedRequest.setId(UUID.randomUUID());
    savedRequest.setEmployeeId(employeeId);
    savedRequest.setLeaveType(leaveType);
    savedRequest.setStartDate(req.startDate);
    savedRequest.setEndDate(req.endDate);
    savedRequest.setNumberOfDays(3);
    savedRequest.setStatus(LeaveStatus.PENDING);
    when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(savedRequest);

    LeaveRequestResponse mockResponse = new LeaveRequestResponse();
    mockResponse.id = savedRequest.getId();
    mockResponse.numberOfDays = 3;
    mockResponse.status = LeaveStatus.PENDING;
    when(leaveMapper.toResponse(savedRequest)).thenReturn(mockResponse);

    // Act
    LeaveRequestResponse result = leaveService.applyForLeave(username, req);

    // Assert
    assertNotNull(result);
    assertEquals(LeaveStatus.PENDING, result.status);
    assertEquals(3, result.numberOfDays);
    assertEquals("Hitesh L", result.employeeName);
    verify(leaveRequestRepository).save(any(LeaveRequest.class));
  }

  @Test
  void testApplyForLeave_InsufficientBalance() {
    // Arrange
    ApplyLeaveRequest req = new ApplyLeaveRequest();
    req.leaveTypeId = leaveTypeId;
    req.startDate = LocalDate.of(2026, 7, 10);
    req.endDate = LocalDate.of(2026, 7, 25); // 16 days
    req.reason = "Long vacation";

    EmployeeLookupResponse empLookup = new EmployeeLookupResponse(employeeId, "Hitesh", "L");
    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(EmployeeLookupResponse.class)))
        .thenReturn(ResponseEntity.ok(empLookup));

    LeaveType leaveType = new LeaveType();
    leaveType.setId(leaveTypeId);
    leaveType.setCode("SL");
    leaveType.setDefaultAnnualDays(10);
    when(leaveTypeRepository.findById(leaveTypeId)).thenReturn(Optional.of(leaveType));

    when(leaveRequestRepository.hasOverlappingRequest(employeeId, req.startDate, req.endDate))
        .thenReturn(false);

    LeaveBalance balance = new LeaveBalance();
    balance.setEmployeeId(employeeId);
    balance.setLeaveType(leaveType);
    balance.setYear(2026);
    balance.setAllocatedDays(10);
    balance.setUsedDays(0);
    balance.setRemainingDays(10);
    when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(
            employeeId, leaveTypeId, 2026))
        .thenReturn(Optional.of(balance));

    // Act & Assert
    assertThrows(
        InsufficientLeaveBalanceException.class, () -> leaveService.applyForLeave(username, req));
  }

  @Test
  void testApplyForLeave_Overlapping() {
    // Arrange
    ApplyLeaveRequest req = new ApplyLeaveRequest();
    req.leaveTypeId = leaveTypeId;
    req.startDate = LocalDate.of(2026, 7, 10);
    req.endDate = LocalDate.of(2026, 7, 12);

    EmployeeLookupResponse empLookup = new EmployeeLookupResponse(employeeId, "Hitesh", "L");
    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(EmployeeLookupResponse.class)))
        .thenReturn(ResponseEntity.ok(empLookup));

    LeaveType leaveType = new LeaveType();
    leaveType.setId(leaveTypeId);
    when(leaveTypeRepository.findById(leaveTypeId)).thenReturn(Optional.of(leaveType));

    when(leaveRequestRepository.hasOverlappingRequest(employeeId, req.startDate, req.endDate))
        .thenReturn(true);

    // Act & Assert
    assertThrows(
        OverlappingLeaveRequestException.class, () -> leaveService.applyForLeave(username, req));
  }

  @Test
  void testReviewLeaveRequest_ApproveSuccess() {
    // Arrange
    UUID requestId = UUID.randomUUID();
    ReviewLeaveRequest reviewReq = new ReviewLeaveRequest();
    reviewReq.decision = ReviewDecision.APPROVE;
    reviewReq.comments = "Approved";

    LeaveType leaveType = new LeaveType();
    leaveType.setId(leaveTypeId);
    leaveType.setCode("AL");

    LeaveRequest leaveReq = new LeaveRequest();
    leaveReq.setId(requestId);
    leaveReq.setEmployeeId(employeeId);
    leaveReq.setLeaveType(leaveType);
    leaveReq.setStartDate(LocalDate.of(2026, 7, 10));
    leaveReq.setEndDate(LocalDate.of(2026, 7, 12));
    leaveReq.setNumberOfDays(3);
    leaveReq.setStatus(LeaveStatus.PENDING);
    when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveReq));

    LeaveBalance balance = new LeaveBalance();
    balance.setEmployeeId(employeeId);
    balance.setLeaveType(leaveType);
    balance.setYear(2026);
    balance.setAllocatedDays(20);
    balance.setUsedDays(0);
    balance.setRemainingDays(20);
    when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(
            employeeId, leaveTypeId, 2026))
        .thenReturn(Optional.of(balance));

    when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveReq);

    LeaveRequestResponse mockResponse = new LeaveRequestResponse();
    mockResponse.id = requestId;
    mockResponse.status = LeaveStatus.APPROVED;
    when(leaveMapper.toResponse(leaveReq)).thenReturn(mockResponse);

    // Act
    LeaveRequestResponse result = leaveService.reviewLeaveRequest(requestId, reviewReq, "reviewer");

    // Assert
    assertNotNull(result);
    assertEquals(LeaveStatus.APPROVED, result.status);
    assertEquals(3, balance.getUsedDays());
    assertEquals(17, balance.getRemainingDays());
    verify(eventPublisher).publish(eq("leave.approved"), eq("leave-service"), any());
  }

  @Test
  void testReviewLeaveRequest_InvalidState() {
    // Arrange
    UUID requestId = UUID.randomUUID();
    ReviewLeaveRequest reviewReq = new ReviewLeaveRequest();
    reviewReq.decision = ReviewDecision.APPROVE;

    LeaveRequest leaveReq = new LeaveRequest();
    leaveReq.setId(requestId);
    leaveReq.setStatus(LeaveStatus.APPROVED); // Already approved
    when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveReq));

    // Act & Assert
    assertThrows(
        InvalidStateTransitionException.class,
        () -> leaveService.reviewLeaveRequest(requestId, reviewReq, "reviewer"));
  }

  @Test
  void testCancelLeaveRequest_ApprovedSuccess_RefundsBalance() {
    // Arrange
    UUID requestId = UUID.randomUUID();
    EmployeeLookupResponse empLookup = new EmployeeLookupResponse(employeeId, "Hitesh", "L");
    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(EmployeeLookupResponse.class)))
        .thenReturn(ResponseEntity.ok(empLookup));

    LeaveType leaveType = new LeaveType();
    leaveType.setId(leaveTypeId);

    LeaveRequest leaveReq = new LeaveRequest();
    leaveReq.setId(requestId);
    leaveReq.setEmployeeId(employeeId);
    leaveReq.setLeaveType(leaveType);
    leaveReq.setStartDate(LocalDate.of(2026, 7, 10));
    leaveReq.setEndDate(LocalDate.of(2026, 7, 12));
    leaveReq.setNumberOfDays(3);
    leaveReq.setStatus(LeaveStatus.APPROVED); // Approved request being cancelled
    when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveReq));

    LeaveBalance balance = new LeaveBalance();
    balance.setEmployeeId(employeeId);
    balance.setLeaveType(leaveType);
    balance.setYear(2026);
    balance.setAllocatedDays(20);
    balance.setUsedDays(3);
    balance.setRemainingDays(17);
    when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(
            employeeId, leaveTypeId, 2026))
        .thenReturn(Optional.of(balance));

    when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveReq);

    LeaveRequestResponse mockResponse = new LeaveRequestResponse();
    mockResponse.id = requestId;
    mockResponse.status = LeaveStatus.CANCELLED;
    when(leaveMapper.toResponse(leaveReq)).thenReturn(mockResponse);

    // Act
    LeaveRequestResponse result = leaveService.cancelLeaveRequest(requestId, username);

    // Assert
    assertNotNull(result);
    assertEquals(LeaveStatus.CANCELLED, result.status);
    assertEquals(0, balance.getUsedDays()); // Refunded
    assertEquals(20, balance.getRemainingDays()); // Refunded
    verify(leaveRequestRepository).save(any(LeaveRequest.class));
  }

  @Test
  void testCancelLeaveRequest_Forbidden() {
    // Arrange
    UUID requestId = UUID.randomUUID();
    EmployeeLookupResponse empLookup = new EmployeeLookupResponse(employeeId, "Hitesh", "L");
    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(EmployeeLookupResponse.class)))
        .thenReturn(ResponseEntity.ok(empLookup));

    LeaveRequest leaveReq = new LeaveRequest();
    leaveReq.setId(requestId);
    leaveReq.setEmployeeId(UUID.randomUUID()); // Different employee ID!
    when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(leaveReq));

    // Act & Assert
    assertThrows(
        AccessForbiddenException.class, () -> leaveService.cancelLeaveRequest(requestId, username));
  }
}
