package com.hrsphere.employee.service;

import com.hrsphere.common.exception.ResourceAlreadyExistsException;
import com.hrsphere.common.exception.ResourceNotFoundException;
import com.hrsphere.employee.dto.*;
import com.hrsphere.employee.entity.Address;
import com.hrsphere.employee.entity.Employee;
import com.hrsphere.employee.entity.enums.EmploymentStatus;
import com.hrsphere.employee.entity.enums.EmploymentType;
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

  public EmployeeService(EmployeeRepository repository, EmployeeCodeGenerator codeGenerator) {
    this.repository = repository;
    this.codeGenerator = codeGenerator;
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

    // Build entity from request
    Employee e = new Employee();
    e.setEmployeeCode(codeGenerator.nextCode());
    e.setFirstName(req.firstName);
    e.setLastName(req.lastName);
    e.setEmail(req.email);
    e.setPhone(req.phone);
    e.setAuthUsername(req.authUsername);
    e.setJobTitle(req.jobTitle);
    e.setEmploymentType(req.employmentType);
    // employmentStatus defaults to ACTIVE in the entity
    e.setDateOfJoining(req.dateOfJoining);
    e.setDepartmentId(req.departmentId);
    e.setManagerId(req.managerId);
    e.setDateOfBirth(req.dateOfBirth);
    e.setGender(req.gender);
    e.setNationality(req.nationality);
    e.setBankAccountNumber(req.bankAccountNumber);
    e.setBankName(req.bankName);
    e.setTaxId(req.taxId);

    // Map AddressDto → Address entity (if provided)
    if (req.address != null) {
      Address addr = new Address();
      addr.setStreet(req.address.street);
      addr.setCity(req.address.city);
      addr.setState(req.address.state);
      addr.setPostalCode(req.address.postalCode);
      addr.setCountry(req.address.country);
      e.setAddress(addr);
    }

    repository.save(e);
    return toResponse(e);
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
    return toResponse(e);
  }

  @Transactional(readOnly = true)
  public EmployeeResponse getByCode(String code) {
    Employee e =
        repository
            .findByEmployeeCodeAndIsDeletedFalse(code)
            .orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    return toResponse(e);
  }

  @Transactional(readOnly = true)
  public EmployeeResponse getByAuthUsername(String username) {
    Employee e =
        repository
            .findByAuthUsernameAndIsDeletedFalse(username)
            .orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    return toResponse(e);
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

    if (req.firstName != null) e.setFirstName(req.firstName);
    if (req.lastName != null) e.setLastName(req.lastName);
    if (req.phone != null) e.setPhone(req.phone);
    if (req.jobTitle != null) e.setJobTitle(req.jobTitle);
    if (req.employmentType != null) e.setEmploymentType(req.employmentType);
    if (req.dateOfJoining != null) e.setDateOfJoining(req.dateOfJoining);
    if (req.departmentId != null) e.setDepartmentId(req.departmentId);
    if (req.managerId != null) e.setManagerId(req.managerId);
    if (req.dateOfBirth != null) e.setDateOfBirth(req.dateOfBirth);
    if (req.gender != null) e.setGender(req.gender);
    if (req.nationality != null) e.setNationality(req.nationality);

    if (req.authUsername != null) {
      if (!req.authUsername.equals(e.getAuthUsername())) {
        repository
            .findByAuthUsernameAndIsDeletedFalse(req.authUsername)
            .ifPresent(
                x -> {
                  throw new ResourceAlreadyExistsException("authUsername already linked");
                });
      }
      e.setAuthUsername(req.authUsername);
    }

    if (req.address != null) {
      Address addr = e.getAddress();
      if (addr == null) {
        addr = new Address();
        e.setAddress(addr);
      }
      if (req.address.street != null) addr.setStreet(req.address.street);
      if (req.address.city != null) addr.setCity(req.address.city);
      if (req.address.state != null) addr.setState(req.address.state);
      if (req.address.postalCode != null) addr.setPostalCode(req.address.postalCode);
      if (req.address.country != null) addr.setCountry(req.address.country);
    }

    e.setUpdatedAt(java.time.LocalDateTime.now());
    repository.save(e);
    return toResponse(e);
  }

  // ------------------------------------------------------------------ //
  //  TERMINATE / DELETE
  // ------------------------------------------------------------------ //

  @Transactional
  public EmployeeResponse terminateEmployee(UUID id, TerminateEmployeeRequest req, String by) {
    log.info("Terminating employee {}. Action by user: {}", id, by);
    Employee e =
        repository
            .findById(id)
            .filter(x -> !x.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    e.setEmploymentStatus(EmploymentStatus.TERMINATED);
    e.setDateOfTermination(req.dateOfTermination);
    e.setUpdatedAt(java.time.LocalDateTime.now());
    repository.save(e);
    return toResponse(e);
  }

  @Transactional
  public void softDeleteEmployee(UUID id, String by) {
    log.info("Soft deleting employee {}. Action by user: {}", id, by);
    Employee e =
        repository
            .findById(id)
            .filter(x -> !x.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    e.setDeleted(true);
    e.setDeletedAt(java.time.LocalDateTime.now());
    e.setUpdatedAt(java.time.LocalDateTime.now());
    repository.save(e);
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
    return page.map(
        e -> {
          EmployeeSummaryResponse s = new EmployeeSummaryResponse();
          s.id = e.getId();
          s.employeeCode = e.getEmployeeCode();
          s.firstName = e.getFirstName();
          s.lastName = e.getLastName();
          s.email = e.getEmail();
          s.jobTitle = e.getJobTitle();
          s.employmentType = e.getEmploymentType();
          s.employmentStatus = e.getEmploymentStatus();
          s.departmentId = e.getDepartmentId();
          s.createdAt = e.getCreatedAt();
          return s;
        });
  }

  // ------------------------------------------------------------------ //
  //  PRIVATE HELPERS
  // ------------------------------------------------------------------ //

  /**
   * Maps an {@link Employee} entity to a fully-populated {@link EmployeeResponse} DTO, including
   * proper enum values and Address entity → AddressDto conversion.
   */
  private EmployeeResponse toResponse(Employee e) {
    EmployeeResponse resp = new EmployeeResponse();
    resp.id = e.getId();
    resp.employeeCode = e.getEmployeeCode();
    resp.firstName = e.getFirstName();
    resp.lastName = e.getLastName();
    resp.email = e.getEmail();
    resp.phone = e.getPhone();
    resp.authUsername = e.getAuthUsername();
    resp.jobTitle = e.getJobTitle();
    resp.employmentType = e.getEmploymentType(); // EmploymentType enum
    resp.employmentStatus = e.getEmploymentStatus(); // EmploymentStatus enum
    resp.dateOfJoining = e.getDateOfJoining();
    resp.dateOfTermination = e.getDateOfTermination();
    resp.departmentId = e.getDepartmentId();
    resp.managerId = e.getManagerId();
    resp.dateOfBirth = e.getDateOfBirth();
    resp.gender = e.getGender(); // Gender enum
    resp.nationality = e.getNationality();

    // Map Address entity → AddressDto (type-safe, no direct assignment)
    if (e.getAddress() != null) {
      AddressDto addrDto = new AddressDto();
      addrDto.street = e.getAddress().getStreet();
      addrDto.city = e.getAddress().getCity();
      addrDto.state = e.getAddress().getState();
      addrDto.postalCode = e.getAddress().getPostalCode();
      addrDto.country = e.getAddress().getCountry();
      resp.address = addrDto;
    }

    resp.createdAt = e.getCreatedAt();
    resp.updatedAt = e.getUpdatedAt();
    return resp;
  }
}
