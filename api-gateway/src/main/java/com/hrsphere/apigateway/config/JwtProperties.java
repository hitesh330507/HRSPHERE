package com.hrsphere.apigateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

  @NotBlank private String secret;
  private long accessTokenExpiryMs = 900_000L;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getAccessTokenExpiryMs() {
    return accessTokenExpiryMs;
  }

  public void setAccessTokenExpiryMs(long accessTokenExpiryMs) {
    this.accessTokenExpiryMs = accessTokenExpiryMs;
  }

  @Override
  public String toString() {
    return "JwtProperties{secret=****, accessTokenExpiryMs=" + accessTokenExpiryMs + '}';
  }
}
