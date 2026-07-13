package com.hrsphere.department.service;

import com.hrsphere.common.exception.ResourceAlreadyExistsException;
import com.hrsphere.common.exception.ResourceNotFoundException;
import com.hrsphere.department.dto.*;
import com.hrsphere.department.entity.Department;
import com.hrsphere.department.mapper.DepartmentMapper;
import com.hrsphere.department.repository.DepartmentRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class DepartmentService {

  private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

  private final DepartmentRepository repository;
  private final DepartmentCodeGenerator codeGenerator;
  private final DepartmentMapper mapper;
  private final RestTemplate restTemplate;
  private final String employeeServiceBaseUrl;

  public DepartmentService(
      DepartmentRepository repository,
      DepartmentCodeGenerator codeGenerator,
      DepartmentMapper mapper,
      RestTemplate restTemplate,
      @Value("${employee-service.base-url:http://employee-service:8082}") String employeeServiceBaseUrl) {
    this.repository = repository;
    this.codeGenerator = codeGenerator;
    this.mapper = mapper;
    this.restTemplate = restTemplate;
    this.employeeServiceBaseUrl = employeeServiceBaseUrl;
  }

  // ------------------------------------------------------------------ //
  //  CREATE
  // ------------------------------------------------------------------ //

  @Transactional
  public DepartmentResponse createDepartment(CreateDepartmentRequest req, String createdBy) {
    log.info("Creating department. Action by user: {}", createdBy);

    repository
        .findByNameAndIsDeletedFalse(req.name)
        .ifPresent(
            d -> {
              throw new ResourceAlreadyExistsException("department name already exists");
            });

    Department d = mapper.toEntity(req);
    d.setDepartmentCode(codeGenerator.nextCode());

    repository.save(d);

    log.info(
        "Department created: Code={}, Name={}, CreatedBy={}",
        d.getDepartmentCode(),
        d.getName(),
        createdBy);

    DepartmentResponse resp = mapper.toResponse(d);
    resp.employeeCount = 0; // New department has 0 employees
    return resp;
  }

  // ------------------------------------------------------------------ //
  //  READ
  // ------------------------------------------------------------------ //

  @Transactional(readOnly = true)
  public DepartmentResponse getById(UUID id) {
    Department d =
        repository
            .findById(id)
            .filter(x -> !x.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("department not found"));

    DepartmentResponse resp = mapper.toResponse(d);
    resp.employeeCount = fetchEmployeeCount(id);
    return resp;
  }

  @Transactional(readOnly = true)
  public DepartmentResponse getByCode(String code) {
    Department d =
        repository
            .findByDepartmentCodeAndIsDeletedFalse(code)
            .orElseThrow(() -> new ResourceNotFoundException("department not found"));

    DepartmentResponse resp = mapper.toResponse(d);
    resp.employeeCount = fetchEmployeeCount(d.getId());
    return resp;
  }

  // ------------------------------------------------------------------ //
  //  UPDATE
  // ------------------------------------------------------------------ //

  @Transactional
  public DepartmentResponse updateDepartment(
      UUID id, UpdateDepartmentRequest req, String updatedBy) {
    log.info("Updating department {}. Action by user: {}", id, updatedBy);

    Department d =
        repository
            .findById(id)
            .filter(x -> !x.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("department not found"));

    if (req.name != null && !req.name.equals(d.getName())) {
      repository
          .findByNameAndIsDeletedFalse(req.name)
          .ifPresent(
              x -> {
                throw new ResourceAlreadyExistsException("department name already exists");
              });
    }

    mapper.updateEntityFromRequest(req, d);
    d.setUpdatedAt(LocalDateTime.now());
    repository.save(d);

    log.info("Department updated: Code={}, UpdatedBy={}", d.getDepartmentCode(), updatedBy);

    DepartmentResponse resp = mapper.toResponse(d);
    resp.employeeCount = fetchEmployeeCount(id);
    return resp;
  }

  // ------------------------------------------------------------------ //
  //  DELETE
  // ------------------------------------------------------------------ //

  @Transactional
  public void softDeleteDepartment(UUID id, String deletedBy) {
    log.info("Soft-deleting department {}. Action by user: {}", id, deletedBy);

    Department d =
        repository
            .findById(id)
            .filter(x -> !x.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("department not found"));

    d.setDeleted(true);
    d.setDeletedAt(LocalDateTime.now());
    d.setUpdatedAt(LocalDateTime.now());
    repository.save(d);

    log.info("Department deleted: Code={}, DeletedBy={}", d.getDepartmentCode(), deletedBy);
  }

  // ------------------------------------------------------------------ //
  //  LIST
  // ------------------------------------------------------------------ //

  @Transactional(readOnly = true)
  public Page<DepartmentSummaryResponse> listDepartments(Pageable pageable) {
    Page<Department> page = repository.findAllByIsDeletedFalse(pageable);
    return page.map(
        d -> {
          DepartmentSummaryResponse summary = mapper.toSummaryResponse(d);
          summary.employeeCount = fetchEmployeeCount(d.getId());
          return summary;
        });
  }

  // ------------------------------------------------------------------ //
  //  HELPER: Inter-service call
  // ------------------------------------------------------------------ //

  private Integer fetchEmployeeCount(UUID departmentId) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-Auth-Validated", "true");
      headers.set("X-Auth-Username", "department-service");
      headers.set("X-Auth-Roles", "ROLE_ADMIN");
      HttpEntity<Void> entity = new HttpEntity<>(headers);

      String url =
          employeeServiceBaseUrl + "/employees/list?departmentId=" + departmentId + "&size=1";

      log.debug("Fetching employee count from: {}", url);
      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        Object totalElementsObj = response.getBody().get("totalElements");
        if (totalElementsObj instanceof Number) {
          return ((Number) totalElementsObj).intValue();
        }
      }
      return null;
    } catch (Exception e) {
      // TODO: Resilience4j (Day 18) circuit breaker will handle this more properly
      log.warn(
          "Failed to fetch employee count from employee-service for department {}: {}",
          departmentId,
          e.getMessage());
      return null;
    }
  }
}
