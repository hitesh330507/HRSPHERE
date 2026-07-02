package com.hrsphere.auth.dto;

import java.time.Instant;
import java.util.List;

public class UserResponse {

  private Long id;
  private String username;
  private String email;
  private List<String> roles;
  private boolean enabled;
  private Instant createdAt;

  public UserResponse() {}

  public UserResponse(
      Long id,
      String username,
      String email,
      List<String> roles,
      boolean enabled,
      Instant createdAt) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.roles = roles;
    this.enabled = enabled;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
