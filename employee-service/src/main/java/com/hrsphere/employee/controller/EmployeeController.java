package com.hrsphere.employee.controller;

import com.hrsphere.common.exception.AccessForbiddenException;
import com.hrsphere.common.dto.PagedResponse;
import com.hrsphere.employee.dto.*;
import com.hrsphere.employee.entity.enums.EmploymentStatus;
import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/employees")
@Tag(name = "Employee Management")
public class EmployeeController {

  private final EmployeeService service;

  public EmployeeController(EmployeeService service) {
    this.service = service;
  }

  private boolean hasRole(String rolesHeader, String role) {
    if (rolesHeader == null) return false;
    return rolesHeader.contains(role);
  }

  @PostMapping
  public ResponseEntity<EmployeeResponse> createEmployee(@RequestHeader(value = "X-Auth-Roles", required = false) String roles,
                                                         @RequestHeader(value = "X-Auth-Username", required = false) String actingUser,
                                                         @Valid @RequestBody CreateEmployeeRequest req) {
    if (!hasRole(roles, "ROLE_ADMIN") && !hasRole(roles, "ROLE_HR")) {
      throw new AccessForbiddenException("insufficient role");
    }
    EmployeeResponse r = service.createEmployee(req, actingUser);
    return ResponseEntity.status(HttpStatus.CREATED).body(r);
  }

  @GetMapping
  public ResponseEntity<?> listEmployees(@RequestHeader(value = "X-Auth-Validated", required = false) String validated,
                                         @RequestParam(required = false) EmploymentStatus status,
                                         @RequestParam(required = false) UUID departmentId,
                                         @RequestParam(required = false) EmploymentType employmentType,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
    if (validated == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    Pageable pageable = PageRequest.of(page, size);
    var p = service.listEmployees(status, departmentId, employmentType, pageable);
    PagedResponse<EmployeeSummaryResponse> resp = new PagedResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), p.isLast());
    return ResponseEntity.ok(resp);
  }

  @GetMapping("/me")
  public ResponseEntity<EmployeeResponse> me(@RequestHeader(value = "X-Auth-Username", required = false) String username) {
    if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    return ResponseEntity.ok(service.getByAuthUsername(username));
  }

  @GetMapping("/{id}")
  public ResponseEntity<EmployeeResponse> getById(@PathVariable UUID id) {
    return ResponseEntity.ok(service.getById(id));
  }

  @GetMapping("/code/{code}")
  public ResponseEntity<EmployeeResponse> getByCode(@PathVariable String code) {
    return ResponseEntity.ok(service.getByCode(code));
  }

  @PutMapping("/{id}")
  public ResponseEntity<EmployeeResponse> update(@RequestHeader(value = "X-Auth-Roles", required = false) String roles,
                                               @PathVariable UUID id,
                                               @Valid @RequestBody UpdateEmployeeRequest req) {
    if (!hasRole(roles, "ROLE_ADMIN") && !hasRole(roles, "ROLE_HR")) {
      throw new AccessForbiddenException("insufficient role");
    }
    return ResponseEntity.ok(service.updateEmployee(id, req, null));
  }

  @PatchMapping("/{id}/terminate")
  public ResponseEntity<EmployeeResponse> terminate(@RequestHeader(value = "X-Auth-Roles", required = false) String roles,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody TerminateEmployeeRequest req) {
    if (!hasRole(roles, "ROLE_ADMIN") && !hasRole(roles, "ROLE_HR")) {
      throw new AccessForbiddenException("insufficient role");
    }
    return ResponseEntity.ok(service.terminateEmployee(id, req, null));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@RequestHeader(value = "X-Auth-Roles", required = false) String roles,
                                     @PathVariable UUID id) {
    if (!hasRole(roles, "ROLE_ADMIN")) throw new AccessForbiddenException("admin only");
    service.softDeleteEmployee(id, null);
    return ResponseEntity.noContent().build();
  }
}
