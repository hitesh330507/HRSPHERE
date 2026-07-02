package com.hrsphere.auth.dto;

import jakarta.validation.constraints.NotNull;

public class ChangeStatusRequest {

  @NotNull(message = "enabled is required")
  private Boolean enabled;

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
