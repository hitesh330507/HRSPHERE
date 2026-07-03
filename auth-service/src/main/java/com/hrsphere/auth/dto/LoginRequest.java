package com.hrsphere.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User login request")
public class LoginRequest {

  @NotBlank(message = "username is required")
  @Schema(description = "Username", example = "john.doe")
  private String username;

  @NotBlank(message = "password is required")
  @Schema(description = "Password", example = "SecurePassword123!")
  private String password;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
