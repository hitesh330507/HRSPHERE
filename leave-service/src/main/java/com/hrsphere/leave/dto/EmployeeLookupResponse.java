package com.hrsphere.leave.dto;

import java.util.UUID;

public class EmployeeLookupResponse {
  public UUID employeeId;
  public String firstName;
  public String lastName;

  public EmployeeLookupResponse() {}

  public EmployeeLookupResponse(UUID employeeId, String firstName, String lastName) {
    this.employeeId = employeeId;
    this.firstName = firstName;
    this.lastName = lastName;
  }
}
