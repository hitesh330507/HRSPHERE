package com.hrsphere.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to change user role")
public class ChangeRoleRequest {

  @NotBlank(message = "role is required")
  @Pattern(
      regexp = "ROLE_ADMIN|ROLE_HR|ROLE_EMPLOYEE",
      message = "role must be one of ROLE_ADMIN, ROLE_HR, ROLE_EMPLOYEE")
  @Schema(
      description = "New role for the user",
      example = "ROLE_HR",
      allowableValues = {"ROLE_ADMIN", "ROLE_HR", "ROLE_EMPLOYEE"})
  private String role;

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
