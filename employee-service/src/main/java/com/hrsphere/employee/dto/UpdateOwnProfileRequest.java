package com.hrsphere.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public class UpdateOwnProfileRequest {

  @Size(max = 20)
  @Schema(
      description = "Updatable by the employee themselves. Only phone can be changed here.",
      example = "+91-9876543210")
  public String phone;

  @Valid public AddressDto address;
}
