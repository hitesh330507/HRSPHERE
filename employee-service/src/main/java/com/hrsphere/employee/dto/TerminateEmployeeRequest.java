package com.hrsphere.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class TerminateEmployeeRequest {
  @NotNull
  @PastOrPresent
  @Schema(example = "2026-07-06")
  public LocalDate dateOfTermination;

  @NotBlank
  @Size(max = 500)
  @Schema(example = "Resigned voluntarily")
  public String reason;
}
