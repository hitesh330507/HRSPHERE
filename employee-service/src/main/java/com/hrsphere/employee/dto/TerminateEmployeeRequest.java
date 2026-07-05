package com.hrsphere.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class TerminateEmployeeRequest {
  @NotNull public LocalDate dateOfTermination;

  @NotBlank public String reason;
}
