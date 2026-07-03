package com.hrsphere.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to change user enable/disable status")
public class ChangeStatusRequest {

  @NotNull(message = "enabled is required")
  @Schema(description = "True to enable user, false to disable", example = "true")
  private Boolean enabled;

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
