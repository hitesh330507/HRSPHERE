package com.hrsphere.employee.service;

import com.hrsphere.common.exception.ResourceAlreadyExistsException;
import com.hrsphere.common.exception.ResourceNotFoundException;
import com.hrsphere.employee.dto.*;
import com.hrsphere.employee.entity.Employee;
import com.hrsphere.employee.entity.enums.EmploymentStatus;
import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EmployeeService {

  private final EmployeeRepository repository;
  private final EmployeeCodeGenerator codeGenerator;

  public EmployeeService(EmployeeRepository repository, EmployeeCodeGenerator codeGenerator) {
    this.repository = repository;
    this.codeGenerator = codeGenerator;
  }

  @Transactional
  public EmployeeResponse createEmployee(CreateEmployeeRequest req, String createdBy) {
    repository.findByEmailAndIsDeletedFalse(req.email).ifPresent(e -> {
      throw new ResourceAlreadyExistsException("email already exists");
    });
    if (req.authUsername != null) {
      repository.findByAuthUsernameAndIsDeletedFalse(req.authUsername).ifPresent(e -> {
        throw new ResourceAlreadyExistsException("authUsername already linked");
      });
    }
    Employee e = new Employee();
    e.setEmployeeCode(codeGenerator.nextCode());
    e.setFirstName(req.firstName);
    e.setLastName(req.lastName);
    e.setEmail(req.email);
    e.setPhone(req.phone);
    e.setAuthUsername(req.authUsername);
    e.setJobTitle(req.jobTitle);
    e.setEmploymentType(req.employmentType);
    e.setDateOfJoining(req.dateOfJoining);
    e.setDepartmentId(req.departmentId);
    e.setManagerId(req.managerId);
    repository.save(e);
    EmployeeResponse resp = new EmployeeResponse();
    resp.id = e.getId();
    resp.employeeCode = e.getEmployeeCode();
    resp.firstName = e.getFirstName();
    resp.lastName = e.getLastName();
    resp.email = e.getEmail();
    resp.jobTitle = e.getJobTitle();
    resp.employmentType = e.getEmploymentType();
    resp.employmentStatus = e.getEmploymentStatus();
    resp.dateOfJoining = e.getDateOfJoining();
    resp.departmentId = e.getDepartmentId();
    resp.createdAt = e.getCreatedAt();
    resp.updatedAt = e.getUpdatedAt();
    return resp;
  }

  @Transactional(readOnly = true)
  public EmployeeResponse getById(UUID id) {
    Employee e = repository.findById(id).filter(x -> !x.isDeleted()).orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    EmployeeResponse resp = new EmployeeResponse();
    resp.id = e.getId();
    resp.employeeCode = e.getEmployeeCode();
    resp.firstName = e.getFirstName();
    resp.lastName = e.getLastName();
    resp.email = e.getEmail();
    resp.jobTitle = e.getJobTitle();
    resp.employmentType = e.getEmploymentType();
    resp.employmentStatus = e.getEmploymentStatus();
    resp.dateOfJoining = e.getDateOfJoining();
    resp.departmentId = e.getDepartmentId();
    resp.createdAt = e.getCreatedAt();
    resp.updatedAt = e.getUpdatedAt();
    return resp;
  }

  @Transactional(readOnly = true)
  public EmployeeResponse getByCode(String code) {
    Employee e = repository.findByEmployeeCodeAndIsDeletedFalse(code).orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    EmployeeResponse resp = new EmployeeResponse();
    resp.id = e.getId();
    resp.employeeCode = e.getEmployeeCode();
    resp.firstName = e.getFirstName();
    resp.lastName = e.getLastName();
    resp.email = e.getEmail();
    resp.jobTitle = e.getJobTitle();
    resp.employmentType = e.getEmploymentType();
    resp.employmentStatus = e.getEmploymentStatus();
    resp.dateOfJoining = e.getDateOfJoining();
    resp.departmentId = e.getDepartmentId();
    resp.createdAt = e.getCreatedAt();
    resp.updatedAt = e.getUpdatedAt();
    return resp;
  }

  @Transactional(readOnly = true)
  public EmployeeResponse getByAuthUsername(String username) {
    Employee e = repository.findByAuthUsernameAndIsDeletedFalse(username).orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    EmployeeResponse resp = new EmployeeResponse();
    resp.id = e.getId();
    resp.employeeCode = e.getEmployeeCode();
    resp.firstName = e.getFirstName();
    resp.lastName = e.getLastName();
    resp.email = e.getEmail();
    resp.jobTitle = e.getJobTitle();
    resp.employmentType = e.getEmploymentType();
    resp.employmentStatus = e.getEmploymentStatus();
    resp.dateOfJoining = e.getDateOfJoining();
    resp.departmentId = e.getDepartmentId();
    resp.createdAt = e.getCreatedAt();
    resp.updatedAt = e.getUpdatedAt();
    return resp;
  }

  @Transactional
  public EmployeeResponse updateEmployee(UUID id, UpdateEmployeeRequest req, String updatedBy) {
    Employee e = repository.findById(id).filter(x -> !x.isDeleted()).orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    if (req.firstName != null) e.setFirstName(req.firstName);
    if (req.lastName != null) e.setLastName(req.lastName);
    if (req.phone != null) e.setPhone(req.phone);
    if (req.jobTitle != null) e.setJobTitle(req.jobTitle);
    if (req.employmentType != null) e.setEmploymentType(req.employmentType);
    if (req.departmentId != null) e.setDepartmentId(req.departmentId);
    repository.save(e);
    return getById(e.getId());
  }

  @Transactional
  public EmployeeResponse terminateEmployee(UUID id, TerminateEmployeeRequest req, String by) {
    Employee e = repository.findById(id).filter(x -> !x.isDeleted()).orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    e.setEmploymentStatus(EmploymentStatus.TERMINATED);
    e.setDateOfTermination(req.dateOfTermination);
    repository.save(e);
    return getById(e.getId());
  }

  @Transactional
  public void softDeleteEmployee(UUID id, String by) {
    Employee e = repository.findById(id).filter(x -> !x.isDeleted()).orElseThrow(() -> new ResourceNotFoundException("employee not found"));
    e.setDeleted(true);
    e.setDeletedAt(java.time.LocalDateTime.now());
    repository.save(e);
  }

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<EmployeeSummaryResponse> listEmployees(EmploymentStatus status, java.util.UUID departmentId, EmploymentType type, Pageable pageable) {
    Page<Employee> page = repository.findAllWithFilters(status, departmentId, type, pageable);
    return page.map(e -> {
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
}
