package com.hrsphere.leave.controller;

import com.hrsphere.common.exception.AccessForbiddenException;
import com.hrsphere.leave.dto.*;
import com.hrsphere.leave.entity.LeaveType;
import com.hrsphere.leave.entity.enums.LeaveStatus;
import com.hrsphere.leave.repository.LeaveTypeRepository;
import com.hrsphere.leave.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leave")
public class LeaveController {

  private static final Logger log = LoggerFactory.getLogger(LeaveController.class);

  private final LeaveService leaveService;
  private final LeaveTypeRepository leaveTypeRepository;

  public LeaveController(LeaveService leaveService, LeaveTypeRepository leaveTypeRepository) {
    this.leaveService = leaveService;
    this.leaveTypeRepository = leaveTypeRepository;
  }

  // ------------------------------------------------------------------ //
  //  APPLY
  // ------------------------------------------------------------------ //

  @PostMapping("/requests")
  @Operation(
      summary = "Apply for leave",
      description = "Creates a new pending leave request. Authenticated users only.",
      security = @SecurityRequirement(name = "BearerAuth"))
  public ResponseEntity<LeaveRequestResponse> apply(
      @RequestHeader(value = "X-Auth-Username", required = false) String username,
      @Valid @RequestBody ApplyLeaveRequest req) {
    if (username == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(leaveService.applyForLeave(username, req));
  }

  // ------------------------------------------------------------------ //
  //  LIST ME
  // ------------------------------------------------------------------ //

  @GetMapping("/requests/me")
  @Operation(
      summary = "List own leave requests",
      description = "Retrieve a page of leave requests applied by the current authenticated user.",
      security = @SecurityRequirement(name = "BearerAuth"))
  public ResponseEntity<Page<LeaveRequestSummaryResponse>> listMyRequests(
      @RequestHeader(value = "X-Auth-Username", required = false) String username,
      Pageable pageable) {
    if (username == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.ok(leaveService.listMyLeaveRequests(username, pageable));
  }

  // ------------------------------------------------------------------ //
  //  LIST ALL (HR/ADMIN)
  // ------------------------------------------------------------------ //

  @GetMapping("/requests")
  @Operation(
      summary = "List all leave requests",
      description =
          "Retrieve all leave requests, optionally filtered by status. Requires ROLE_HR or ROLE_ADMIN.",
      security = @SecurityRequirement(name = "BearerAuth"))
  public ResponseEntity<Page<LeaveRequestSummaryResponse>> listAllRequests(
      @RequestHeader(value = "X-Auth-Roles", required = false) String roles,
      @RequestParam(required = false) LeaveStatus status,
      Pageable pageable) {
    if (!hasRole(roles, "ROLE_ADMIN") && !hasRole(roles, "ROLE_HR")) {
      throw new AccessForbiddenException("Insufficient role");
    }
    return ResponseEntity.ok(leaveService.listAllLeaveRequests(status, pageable));
  }

  // ------------------------------------------------------------------ //
  //  GET SINGLE REQUEST
  // ------------------------------------------------------------------ //

  @GetMapping("/requests/{id}")
  @Operation(
      summary = "Get leave request by ID",
      description =
          "Retrieve details of a specific leave request. Accessible by the request owner or HR/Admin.",
      security = @SecurityRequirement(name = "BearerAuth"))
  public ResponseEntity<LeaveRequestResponse> getRequestById(
      @RequestHeader(value = "X-Auth-Username", required = false) String username,
      @RequestHeader(value = "X-Auth-Roles", required = false) String roles,
      @PathVariable UUID id) {
    if (username == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.ok(leaveService.getLeaveRequestById(id, username, roles));
  }

  // ------------------------------------------------------------------ //
  //  REVIEW (HR/ADMIN)
  // ------------------------------------------------------------------ //

  @PatchMapping("/requests/{id}/review")
  @Operation(
      summary = "Review a leave request",
      description = "Approves or rejects a pending leave request. Requires ROLE_HR or ROLE_ADMIN.",
      security = @SecurityRequirement(name = "BearerAuth"))
  public ResponseEntity<LeaveRequestResponse> review(
      @RequestHeader(value = "X-Auth-Roles", required = false) String roles,
      @RequestHeader(value = "X-Auth-Username", required = false) String reviewerUsername,
      @PathVariable UUID id,
      @Valid @RequestBody ReviewLeaveRequest req) {
    if (reviewerUsername == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (!hasRole(roles, "ROLE_ADMIN") && !hasRole(roles, "ROLE_HR")) {
      throw new AccessForbiddenException("Insufficient role");
    }
    return ResponseEntity.ok(leaveService.reviewLeaveRequest(id, req, reviewerUsername));
  }

  // ------------------------------------------------------------------ //
  //  CANCEL
  // ------------------------------------------------------------------ //

  @PatchMapping("/requests/{id}/cancel")
  @Operation(
      summary = "Cancel a leave request",
      description = "Cancels a pending or approved leave request. Only request owner is allowed.",
      security = @SecurityRequirement(name = "BearerAuth"))
  public ResponseEntity<LeaveRequestResponse> cancel(
      @RequestHeader(value = "X-Auth-Username", required = false) String username,
      @PathVariable UUID id) {
    if (username == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.ok(leaveService.cancelLeaveRequest(id, username));
  }

  // ------------------------------------------------------------------ //
  //  GET BALANCES
  // ------------------------------------------------------------------ //

  @GetMapping("/balances/me")
  @Operation(
      summary = "Get own leave balances",
      description = "Retrieve current leave balances for the authenticated user for a given year.",
      security = @SecurityRequirement(name = "BearerAuth"))
  public ResponseEntity<List<LeaveBalanceResponse>> getMyBalances(
      @RequestHeader(value = "X-Auth-Username", required = false) String username,
      @RequestParam(required = false) Integer year) {
    if (username == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.ok(leaveService.getMyBalances(username, year));
  }

  // ------------------------------------------------------------------ //
  //  LEAVE TYPES
  // ------------------------------------------------------------------ //

  @GetMapping("/types")
  @Operation(
      summary = "Get all leave types",
      description = "Retrieve all available leave types in the system.",
      security = @SecurityRequirement(name = "BearerAuth"))
  public ResponseEntity<List<LeaveType>> getLeaveTypes() {
    return ResponseEntity.ok(leaveTypeRepository.findAll());
  }

  // ------------------------------------------------------------------ //
  //  HELPERS
  // ------------------------------------------------------------------ //

  private boolean hasRole(String rolesHeader, String targetRole) {
    if (rolesHeader == null) return false;
    return Arrays.asList(rolesHeader.split(",")).contains(targetRole);
  }
}
