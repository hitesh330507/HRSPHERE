package com.hrsphere.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

  @NotBlank(message = "username is required")
  @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
  private String username;

  @NotBlank(message = "email is required")
  @Email(message = "email must be a valid email address")
  private String email;

  @NotBlank(message = "password is required")
  @Size(min = 8, message = "password must be at least 8 characters")
  private String password;

  @Pattern(
      regexp = "ROLE_ADMIN|ROLE_HR|ROLE_EMPLOYEE",
      message = "role must be one of ROLE_ADMIN, ROLE_HR, ROLE_EMPLOYEE")
  private String role;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
