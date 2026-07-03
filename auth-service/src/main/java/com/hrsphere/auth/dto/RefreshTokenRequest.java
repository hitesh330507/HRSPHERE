package com.hrsphere.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Token refresh request")
public class RefreshTokenRequest {

  @NotBlank(message = "refreshToken is required")
  @Schema(description = "Refresh token from previous login")
  private String refreshToken;

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
