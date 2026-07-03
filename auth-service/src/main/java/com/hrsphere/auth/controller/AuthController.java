package com.hrsphere.auth.controller;

import com.hrsphere.auth.dto.AuthResponse;
import com.hrsphere.auth.dto.ChangeRoleRequest;
import com.hrsphere.auth.dto.ChangeStatusRequest;
import com.hrsphere.auth.dto.LoginRequest;
import com.hrsphere.auth.dto.LogoutResponse;
import com.hrsphere.auth.dto.RefreshTokenRequest;
import com.hrsphere.auth.dto.RegisterRequest;
import com.hrsphere.auth.dto.UserResponse;
import com.hrsphere.auth.dto.UserSummaryResponse;
import com.hrsphere.auth.service.AuthService;
import com.hrsphere.auth.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Register, login, refresh, logout")
public class AuthController {

  private final AuthService authService;
  private final UserManagementService userManagementService;

  public AuthController(AuthService authService, UserManagementService userManagementService) {
    this.authService = authService;
    this.userManagementService = userManagementService;
  }

  @PostMapping("/register")
  @Operation(summary = "Register a new user")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failure"),
        @ApiResponse(responseCode = "409", description = "Username or email already taken")
      })
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/login")
  @Operation(summary = "Login and receive JWT tokens")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
      })
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  @Operation(summary = "Refresh access token using refresh token")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "New tokens issued"),
        @ApiResponse(responseCode = "401", description = "Refresh token invalid or expired")
      })
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    AuthResponse response = authService.refresh(request.getRefreshToken());
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  @Operation(summary = "Logout — invalidates refresh token")
  @SecurityRequirement(name = "BearerAuth")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Logged out successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
      })
  public ResponseEntity<LogoutResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
    LogoutResponse response = authService.logout(request.getRefreshToken());
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/me")
  @Operation(summary = "Get current authenticated user profile")
  @SecurityRequirement(name = "BearerAuth")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User profile retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
      })
  public ResponseEntity<UserResponse> getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return ResponseEntity.ok(userManagementService.getUserByUsername(username));
  }

  @GetMapping("/admin/users")
  @PreAuthorize("hasRole('ADMIN')")
  @Tag(name = "Admin — User Management")
  @Operation(summary = "List all users (paginated)")
  @SecurityRequirement(name = "BearerAuth")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
      })
  public ResponseEntity<Map<String, Object>> getAllUsers(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size) {
    Page<UserResponse> users = userManagementService.getAllUsers(PageRequest.of(page, size));
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("content", users.getContent());
    payload.put("page", users.getNumber());
    payload.put("size", users.getSize());
    payload.put("totalElements", users.getTotalElements());
    payload.put("totalPages", users.getTotalPages());
    return ResponseEntity.ok(payload);
  }

  @GetMapping("/admin/users/{username}")
  @PreAuthorize("hasRole('ADMIN')")
  @Tag(name = "Admin — User Management")
  @Operation(summary = "Get user details by username")
  @SecurityRequirement(name = "BearerAuth")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User details retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  public ResponseEntity<UserResponse> getUserByUsername(@PathVariable("username") String username) {
    return ResponseEntity.ok(userManagementService.getUserByUsername(username));
  }

  @PatchMapping("/admin/users/{username}/role")
  @PreAuthorize("hasRole('ADMIN')")
  @Tag(name = "Admin — User Management")
  @Operation(summary = "Change user role")
  @SecurityRequirement(name = "BearerAuth")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Role changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid role"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  public ResponseEntity<UserResponse> changeUserRole(
      @PathVariable("username") String username, @Valid @RequestBody ChangeRoleRequest request) {
    UserResponse response = userManagementService.changeUserRole(username, request.getRole());
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/admin/users/{username}/status")
  @PreAuthorize("hasRole('ADMIN')")
  @Tag(name = "Admin — User Management")
  @Operation(summary = "Enable or disable a user")
  @SecurityRequirement(name = "BearerAuth")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User status changed successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot modify admin status"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "User not found")
      })
  public ResponseEntity<UserResponse> changeUserStatus(
      @PathVariable("username") String username, @Valid @RequestBody ChangeStatusRequest request) {
    UserResponse response = userManagementService.setUserStatus(username, request.getEnabled());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/hr/users/summary")
  @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
  @Tag(name = "HR — User Summary")
  @Operation(summary = "Get user summary statistics")
  @SecurityRequirement(name = "BearerAuth")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Summary retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
      })
  public ResponseEntity<UserSummaryResponse> getUserSummary() {
    return ResponseEntity.ok(userManagementService.getUserSummary());
  }
}
