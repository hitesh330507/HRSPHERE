package com.hrsphere.employee.controller;

import com.hrsphere.common.dto.ApiErrorResponse;
import com.hrsphere.common.dto.PagedResponse;
import com.hrsphere.common.exception.AccessForbiddenException;
import com.hrsphere.employee.dto.*;
import com.hrsphere.employee.entity.enums.EmploymentStatus;
import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employees")
@Tag(
    name = "Employee Management",
    description = "Operations for managing employee lifecycles and self-service updates")
public class EmployeeController {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(EmployeeController.class);
  private final EmployeeService service;

  public EmployeeController(EmployeeService service) {
    this.service = service;
  }

  private boolean hasRole(String rolesHeader, String role) {
    if (rolesHeader == null) return false;
    return rolesHeader.contains(role);
  }

  @PostMapping("/create")
  @Operation(
      summary = "Create a new employee",
      description =
          "Creates an employee record. Requires ROLE_HR or ROLE_ADMIN. employeeCode is auto-generated (EMP-XXXX format).",
      security = @SecurityRequirement(name = "BearerAuth"))
  @ApiResponse(
      responseCode = "201",
      description = "Employee created",
      content = @Content(schema = @Schema(implementation = EmployeeResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Validation failure",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Insufficient role",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "409",
      description = "Email already exists",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  public ResponseEntity<EmployeeResponse> createEmployee(
      @RequestHeader(value = "X-Auth-Roles", required = false) String roles,
      @RequestHeader(value = "X-Auth-Username", required = false) String actingUser,
      @Valid @RequestBody CreateEmployeeRequest req) {
    if (actingUser == null) {
      log.warn("X-Auth-Username header is missing during employee creation");
    }
    if (!hasRole(roles, "ROLE_ADMIN") && !hasRole(roles, "ROLE_HR")) {
      throw new AccessForbiddenException("insufficient role");
    }
    EmployeeResponse r = service.createEmployee(req, actingUser);
    return ResponseEntity.status(HttpStatus.CREATED).body(r);
  }

  @GetMapping("/list")
  @Operation(
      summary = "List employees (paginated + filterable)",
      description = "Lists employees with filters. Requires active token validated by gateway.",
      security = @SecurityRequirement(name = "BearerAuth"))
  @Parameter(
      name = "status",
      description = "Filter by employment status",
      schema = @Schema(implementation = EmploymentStatus.class))
  @Parameter(name = "departmentId", description = "Filter by department UUID")
  @Parameter(
      name = "employmentType",
      description = "Filter by employment type",
      schema = @Schema(implementation = EmploymentType.class))
  @Parameter(name = "page", description = "Page number", example = "0")
  @Parameter(name = "size", description = "Page size", example = "20")
  @Parameter(name = "sort", description = "Sort criteria", example = "createdAt,desc")
  public ResponseEntity<?> listEmployees(
      @RequestHeader(value = "X-Auth-Validated", required = false) String validated,
      @RequestParam(required = false) EmploymentStatus status,
      @RequestParam(required = false) UUID departmentId,
      @RequestParam(required = false) EmploymentType employmentType,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt,desc") String sort) {
    if (validated == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

    String[] sortParts = sort.split(",");
    org.springframework.data.domain.Sort sortObj =
        sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
            ? org.springframework.data.domain.Sort.by(sortParts[0]).ascending()
            : org.springframework.data.domain.Sort.by(sortParts[0]).descending();

    Pageable pageable = PageRequest.of(page, size, sortObj);
    var p = service.listEmployees(status, departmentId, employmentType, pageable);
    PagedResponse<EmployeeSummaryResponse> resp =
        new PagedResponse<>(
            p.getContent(),
            p.getNumber(),
            p.getSize(),
            p.getTotalElements(),
            p.getTotalPages(),
            p.isLast());
    return ResponseEntity.ok(resp);
  }

  @GetMapping("/me")
  @Operation(
      summary = "Get own employee profile",
      description =
          "Retrieve the profile of the currently authenticated user mapped to their authUsername.",
      security = @SecurityRequirement(name = "BearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Profile retrieved successfully",
      content = @Content(schema = @Schema(implementation = EmployeeResponse.class)))
  @ApiResponse(responseCode = "401", description = "Missing or invalid headers")
  @ApiResponse(
      responseCode = "404",
      description = "No employee record linked to auth account",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  public ResponseEntity<EmployeeResponse> me(
      @RequestHeader(value = "X-Auth-Username", required = false) String username) {
    if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    return ResponseEntity.ok(service.getByAuthUsername(username));
  }

  @PatchMapping("/me")
  @Operation(
      summary = "Update own contact details",
      description =
          "Authenticated employee updates their own phone and address. Job title, department, and other HR-managed fields cannot be updated via this endpoint.",
      security = @SecurityRequirement(name = "BearerAuth"))
  @ApiResponse(
      responseCode = "200",
      description = "Profile updated successfully",
      content = @Content(schema = @Schema(implementation = EmployeeResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Validation failure",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(responseCode = "401", description = "Missing or invalid headers")
  @ApiResponse(
      responseCode = "404",
      description = "No employee record linked to your auth account yet",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  public ResponseEntity<EmployeeResponse> updateMe(
      @RequestHeader(value = "X-Auth-Username", required = false) String username,
      @Valid @RequestBody UpdateOwnProfileRequest req) {
    if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    return ResponseEntity.ok(service.updateOwnProfile(username, req));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get employee by ID",
      description = "Retrieve full details of an employee by their UUID.",
      security = @SecurityRequirement(name = "BearerAuth"))
  @Parameter(
      name = "id",
      description = "Employee UUID",
      example = "550e8400-e29b-41d4-a716-446655440000")
  @ApiResponse(
      responseCode = "200",
      description = "Employee found",
      content = @Content(schema = @Schema(implementation = EmployeeResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid UUID format",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Employee not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  public ResponseEntity<EmployeeResponse> getById(@PathVariable UUID id) {
    return ResponseEntity.ok(service.getById(id));
  }

  @GetMapping("/code/{code}")
  @Operation(
      summary = "Get employee by employee code",
      description = "Retrieve details of an employee using their unique auto-generated code.",
      security = @SecurityRequirement(name = "BearerAuth"))
  @Parameter(name = "code", description = "Employee code", example = "EMP-0001")
  @ApiResponse(
      responseCode = "200",
      description = "Employee found",
      content = @Content(schema = @Schema(implementation = EmployeeResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Employee not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  public ResponseEntity<EmployeeResponse> getByCode(@PathVariable String code) {
    return ResponseEntity.ok(service.getByCode(code));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Update an employee's details",
      description = "Updates an employee's details. Requires ROLE_HR or ROLE_ADMIN.",
      security = @SecurityRequirement(name = "BearerAuth"))
  @Parameter(
      name = "id",
      description = "Employee UUID",
      example = "550e8400-e29b-41d4-a716-446655440000")
  @ApiResponse(
      responseCode = "200",
      description = "Employee updated",
      content = @Content(schema = @Schema(implementation = EmployeeResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Validation failure",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Insufficient role",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Employee not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  public ResponseEntity<EmployeeResponse> update(
      @RequestHeader(value = "X-Auth-Roles", required = false) String roles,
      @RequestHeader(value = "X-Auth-Username", required = false) String actingUser,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateEmployeeRequest req) {
    if (actingUser == null) {
      log.warn("X-Auth-Username header is missing during employee update");
    }
    if (!hasRole(roles, "ROLE_ADMIN") && !hasRole(roles, "ROLE_HR")) {
      throw new AccessForbiddenException("insufficient role");
    }
    return ResponseEntity.ok(service.updateEmployee(id, req, actingUser));
  }

  @PatchMapping("/{id}/terminate")
  @Operation(
      summary = "Terminate an employee",
      description =
          "Terminates an employee record by setting their status to TERMINATED and date of termination. Requires ROLE_HR or ROLE_ADMIN.",
      security = @SecurityRequirement(name = "BearerAuth"))
  @Parameter(
      name = "id",
      description = "Employee UUID",
      example = "550e8400-e29b-41d4-a716-446655440000")
  @ApiResponse(
      responseCode = "200",
      description = "Employee terminated",
      content = @Content(schema = @Schema(implementation = EmployeeResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Validation failure or future termination date",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "403",
      description = "Insufficient role",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Employee not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  public ResponseEntity<EmployeeResponse> terminate(
      @RequestHeader(value = "X-Auth-Roles", required = false) String roles,
      @RequestHeader(value = "X-Auth-Username", required = false) String actingUser,
      @PathVariable UUID id,
      @Valid @RequestBody TerminateEmployeeRequest req) {
    if (actingUser == null) {
      log.warn("X-Auth-Username header is missing during employee termination");
    }
    if (!hasRole(roles, "ROLE_ADMIN") && !hasRole(roles, "ROLE_HR")) {
      throw new AccessForbiddenException("insufficient role");
    }
    return ResponseEntity.ok(service.terminateEmployee(id, req, actingUser));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Soft-delete an employee",
      description =
          "Marks the employee as deleted. The record is retained for audit purposes and is excluded from all list queries. Requires ROLE_ADMIN.",
      security = @SecurityRequirement(name = "BearerAuth"))
  @Parameter(
      name = "id",
      description = "Employee UUID",
      example = "550e8400-e29b-41d4-a716-446655440000")
  @ApiResponse(responseCode = "204", description = "Employee deleted")
  @ApiResponse(
      responseCode = "403",
      description = "Insufficient role",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Employee not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  public ResponseEntity<Void> delete(
      @RequestHeader(value = "X-Auth-Roles", required = false) String roles,
      @RequestHeader(value = "X-Auth-Username", required = false) String actingUser,
      @PathVariable UUID id) {
    if (actingUser == null) {
      log.warn("X-Auth-Username header is missing during employee deletion");
    }
    if (!hasRole(roles, "ROLE_ADMIN")) throw new AccessForbiddenException("admin only");
    service.softDeleteEmployee(id, actingUser);
    return ResponseEntity.noContent().build();
  }
}
