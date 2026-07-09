package com.hrsphere.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Minimal details of an employee for lookup purposes")
public class EmployeeLookupResponse {

  @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
  public UUID employeeId;

  @Schema(example = "Hitesh")
  public String firstName;

  @Schema(example = "L")
  public String lastName;

  public EmployeeLookupResponse() {}

  public EmployeeLookupResponse(UUID employeeId, String firstName, String lastName) {
    this.employeeId = employeeId;
    this.firstName = firstName;
    this.lastName = lastName;
  }
}
