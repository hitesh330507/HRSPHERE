package com.hrsphere.department.controller;

import com.hrsphere.common.dto.ApiErrorResponse;
import com.hrsphere.common.dto.PagedResponse;
import com.hrsphere.common.exception.AccessForbiddenException;
import com.hrsphere.department.dto.*;
import com.hrsphere.department.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/department")
@Tag(
    name = "Department Management",
    description = "Operations for managing departments and head count statistics")
public class DepartmentController {

  private static final Logger log = LoggerFactory.getLogger(DepartmentController.class);

  private final DepartmentService service;

  public DepartmentController(DepartmentService service) {
    this.service = service;
  }

  private boolean hasRole(String rolesHeader, String role) {
    if (rolesHeader == null) return false;
    return rolesHeader.contains(role);
  }

  @PostMapping
  @Operation(
      summary = "Create a new department",
      description =
          "Creates a department. Requires ROLE_ADMIN or ROLE_HR. departmentCode is auto-generated (DEPT-XXXX format).",
      security = @SecurityRequirement(name = "BearerAuth"))
  @ApiResponse(
      responseCode = "201",
      description = "Department created",
      content = @Content(schema = @Schema(implementation = DepartmentResponse.class)))
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
      description = "Name already exists",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  public ResponseEntity<DepartmentResponse> create(
      @RequestHeader(value = "X-Auth-Roles", required = false) String roles,
      @RequestHeader(value = "X-Auth-Username", required = false) String actingUser,
      @Valid @RequestBody CreateDepartmentRequest req) {
    if (actingUser == null) {
      log.warn("X-Auth-Username header is missing during department creation");
    }
    if (!hasRole(roles, "ROLE_ADMIN") && !hasRole(roles, "ROLE_HR")) {
      throw new AccessForbiddenException("insufficient role");
    }
    DepartmentResponse resp = service.createDepartment(req, actingUser);
    return ResponseEntity.status(HttpStatus.CREATED).body(resp);
  }

  @GetMapping
  @Operation(
      summary = "List departments (paginated)",
      description = "Lists departments. Requires active token validated by gateway.",
      security = @SecurityRequirement(name = "BearerAuth"))
  @Parameter(name = "page", description = "Page number", example = "0")
  @Parameter(name = "size", description = "Page size", example = "20")
  @Parameter(name = "sort", description = "Sort criteria", example = "createdAt,desc")
  public ResponseEntity<?> list(
      @RequestHeader(value = "X-Auth-Validated", required = false) String validated,
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
    Page<DepartmentSummaryResponse> p = service.listDepartments(pageable);
    PagedResponse<DepartmentSummaryResponse> resp =
        new PagedResponse<>(
            p.getContent(),
            p.getNumber(),
            p.getSize(),
            p.getTotalElements(),
            p.getTotalPages(),
            p.isLast());
    return ResponseEntity.ok(resp);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get department by ID",
      description = "Retrieve details of a department by its UUID.",
      security = @SecurityRequirement(name = "BearerAuth"))
  @Parameter(name = "id", description = "Department UUID")
  @ApiResponse(
      responseCode = "200",
      description = "Department found",
      content = @Content(schema = @Schema(implementation = DepartmentResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Invalid UUID format",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Department not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  public ResponseEntity<DepartmentResponse> getById(
      @RequestHeader(value = "X-Auth-Validated", required = false) String validated,
      @PathVariable UUID id) {
    if (validated == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    return ResponseEntity.ok(service.getById(id));
  }

  @GetMapping("/code/{code}")
  @Operation(
      summary = "Get department by department code",
      description = "Retrieve details of a department using its unique code.",
      security = @SecurityRequirement(name = "BearerAuth"))
  @Parameter(name = "code", description = "Department code", example = "DEPT-0001")
  @ApiResponse(
      responseCode = "200",
      description = "Department found",
      content = @Content(schema = @Schema(implementation = DepartmentResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Department not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  public ResponseEntity<DepartmentResponse> getByCode(
      @RequestHeader(value = "X-Auth-Validated", required = false) String validated,
      @PathVariable String code) {
    if (validated == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    return ResponseEntity.ok(service.getByCode(code));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Update department details",
      description = "Updates a department's details. Requires ROLE_ADMIN or ROLE_HR.",
      security = @SecurityRequirement(name = "BearerAuth"))
  @Parameter(name = "id", description = "Department UUID")
  @ApiResponse(
      responseCode = "200",
      description = "Department updated",
      content = @Content(schema = @Schema(implementation = DepartmentResponse.class)))
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
      description = "Department not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  public ResponseEntity<DepartmentResponse> update(
      @RequestHeader(value = "X-Auth-Roles", required = false) String roles,
      @RequestHeader(value = "X-Auth-Username", required = false) String actingUser,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateDepartmentRequest req) {
    if (actingUser == null) {
      log.warn("X-Auth-Username header is missing during department update");
    }
    if (!hasRole(roles, "ROLE_ADMIN") && !hasRole(roles, "ROLE_HR")) {
      throw new AccessForbiddenException("insufficient role");
    }
    return ResponseEntity.ok(service.updateDepartment(id, req, actingUser));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Soft-delete a department",
      description = "Marks the department as deleted. Requires ROLE_ADMIN.",
      security = @SecurityRequirement(name = "BearerAuth"))
  @Parameter(name = "id", description = "Department UUID")
  @ApiResponse(responseCode = "204", description = "Department deleted")
  @ApiResponse(
      responseCode = "403",
      description = "Insufficient role",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Department not found",
      content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  public ResponseEntity<Void> delete(
      @RequestHeader(value = "X-Auth-Roles", required = false) String roles,
      @RequestHeader(value = "X-Auth-Username", required = false) String actingUser,
      @PathVariable UUID id) {
    if (actingUser == null) {
      log.warn("X-Auth-Username header is missing during department deletion");
    }
    if (!hasRole(roles, "ROLE_ADMIN")) {
      throw new AccessForbiddenException("admin only");
    }
    service.softDeleteDepartment(id, actingUser);
    return ResponseEntity.noContent().build();
  }
}
