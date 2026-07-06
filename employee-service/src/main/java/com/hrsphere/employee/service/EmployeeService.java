package com.hrsphere.employee.service;

import com.hrsphere.common.exception.ResourceAlreadyExistsException;
import com.hrsphere.common.exception.ResourceNotFoundException;
import com.hrsphere.employee.dto.*;
import com.hrsphere.employee.entity.Employee;
import com.hrsphere.employee.entity.enums.EmploymentStatus;
import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.mapper.EmployeeMapper;
import com.hrsphere.employee.repository.EmployeeRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(EmployeeService.class);
  private final EmployeeRepository repository;
  private final EmployeeCodeGenerator codeGenerator;
  private final EmployeeMapper mapper;

  public EmployeeService(
      EmployeeRepository repository, EmployeeCodeGenerator codeGenerator, EmployeeMapper mapper) {
    this.repository = repository;
    this.codeGenerator = codeGenerator;
    this.mapper = mapper;
  }

  // ------------------------------------------------------------------ //
  //  CREATE
  // ------------------------------------------------------------------ //

  @Transactional
  public EmployeeResponse createEmployee(CreateEmployeeRequest req, String createdBy) {
    log.info("Creating employee. Action by user: {}", createdBy);
    // Uniqueness checks
    repository
        .findByEmailAndIsDeletedFalse(req.email)
        .ifPresent(
            e -> {
              throw new ResourceAlreadyExistsException("email already exists");
            });

    if (req.authUsername != null) {
      repository
          .findByAuthUsernameAndIsDeletedFalse(req.authUsername)
          .ifPresent(
              e -> {
                throw new ResourceAlreadyExistsException("authUsername already linked");
              });
    }

    // Build entity using MapStruct mapper
    Employee e = mapper.toEntity(req);
    e.setEmployeeCode(codeGenerator.nextCode());
    e.setEmploymentStatus(EmploymentStatus.ACTIVE);

    repository.save(e);

    log.info(
        "Employee created: Code={}, Name={} {}, CreatedBy={}",
        e.getEmployeeCode(),
        e.getFirstName(),
        e.getLastName(),
        createdBy);

    return mapper.toResponse(e);
  }

  // ------------------------------------------------------------------ //
  //  READ
  // ------------------------------------------------------------------ //

  @Transactional(readOnly = true)
  public EmployeeResponse getById(UUID id) {
    Employee e =
        repository
            .findById(id)
            .filter(x -> !x.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    return mapper.toResponse(e);
  }

  @Transactional(readOnly = true)
  public EmployeeResponse getByCode(String code) {
    Employee e =
        repository
            .findByEmployeeCodeAndIsDeletedFalse(code)
            .orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    return mapper.toResponse(e);
  }

  @Transactional(readOnly = true)
  public EmployeeResponse getByAuthUsername(String username) {
    Employee e =
        repository
            .findByAuthUsernameAndIsDeletedFalse(username)
            .orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    return mapper.toResponse(e);
  }

  // ------------------------------------------------------------------ //
  //  UPDATE
  // ------------------------------------------------------------------ //

  @Transactional
  public EmployeeResponse updateEmployee(UUID id, UpdateEmployeeRequest req, String updatedBy) {
    log.info("Updating employee {}. Action by user: {}", id, updatedBy);
    Employee e =
        repository
            .findById(id)
            .filter(x -> !x.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("employee not found"));

    if (req.authUsername != null && !req.authUsername.equals(e.getAuthUsername())) {
      repository
          .findByAuthUsernameAndIsDeletedFalse(req.authUsername)
          .ifPresent(
              x -> {
                throw new ResourceAlreadyExistsException("authUsername already linked");
              });
    }

    mapper.updateEntityFromRequest(req, e);
    e.setUpdatedAt(java.time.LocalDateTime.now());
    repository.save(e);

    java.util.List<String> changedFields = new java.util.ArrayList<>();
    if (req.firstName != null) changedFields.add("firstName");
    if (req.lastName != null) changedFields.add("lastName");
    if (req.phone != null) changedFields.add("phone");
    if (req.jobTitle != null) changedFields.add("jobTitle");
    if (req.employmentType != null) changedFields.add("employmentType");
    if (req.dateOfJoining != null) changedFields.add("dateOfJoining");
    if (req.departmentId != null) changedFields.add("departmentId");
    if (req.managerId != null) changedFields.add("managerId");
    if (req.dateOfBirth != null) changedFields.add("dateOfBirth");
    if (req.gender != null) changedFields.add("gender");
    if (req.nationality != null) changedFields.add("nationality");
    if (req.address != null) changedFields.add("address");
    if (req.authUsername != null) changedFields.add("authUsername");

    log.info(
        "Employee updated: Code={}, FieldsChanged={}, UpdatedBy={}",
        e.getEmployeeCode(),
        changedFields,
        updatedBy);

    return mapper.toResponse(e);
  }

  @Transactional
  public EmployeeResponse updateOwnProfile(String authUsername, UpdateOwnProfileRequest request) {
    Employee e =
        repository
            .findByAuthUsernameAndIsDeletedFalse(authUsername)
            .orElseThrow(() -> new ResourceNotFoundException("employee not found"));

    mapper.updateOwnProfile(request, e);
    e.setUpdatedAt(java.time.LocalDateTime.now());
    repository.save(e);

    log.info("Own profile updated: Code={}, UpdatedBy={}", e.getEmployeeCode(), authUsername);

    return mapper.toResponse(e);
  }

  // ------------------------------------------------------------------ //
  //  TERMINATE / DELETE
  // ------------------------------------------------------------------ //

  @Transactional
  public EmployeeResponse terminateEmployee(UUID id, TerminateEmployeeRequest req, String by) {
    Employee e =
        repository
            .findById(id)
            .filter(x -> !x.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    e.setEmploymentStatus(EmploymentStatus.TERMINATED);
    e.setDateOfTermination(req.dateOfTermination);
    e.setUpdatedAt(java.time.LocalDateTime.now());
    repository.save(e);

    log.info(
        "Employee terminated: Code={}, DateOfTermination={}, TerminatedBy={}",
        e.getEmployeeCode(),
        req.dateOfTermination,
        by);

    return mapper.toResponse(e);
  }

  @Transactional
  public void softDeleteEmployee(UUID id, String by) {
    Employee e =
        repository
            .findById(id)
            .filter(x -> !x.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    e.setDeleted(true);
    e.setDeletedAt(java.time.LocalDateTime.now());
    e.setUpdatedAt(java.time.LocalDateTime.now());
    repository.save(e);

    log.info("Employee deleted: Code={}, DeletedBy={}", e.getEmployeeCode(), by);
  }

  // ------------------------------------------------------------------ //
  //  LIST
  // ------------------------------------------------------------------ //

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<EmployeeSummaryResponse> listEmployees(
      EmploymentStatus status,
      java.util.UUID departmentId,
      EmploymentType type,
      Pageable pageable) {
    Page<Employee> page = repository.findAllWithFilters(status, departmentId, type, pageable);
    return page.map(mapper::toSummaryResponse);
  }
}
