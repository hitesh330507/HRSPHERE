package com.hrsphere.leave.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public class ApplyLeaveRequest {

  @NotNull(message = "leaveTypeId is required")
  public UUID leaveTypeId;

  @NotNull(message = "startDate is required")
  @FutureOrPresent(message = "startDate cannot be in the past")
  public LocalDate startDate;

  @NotNull(message = "endDate is required")
  public LocalDate endDate;

  @Size(max = 500, message = "reason cannot exceed 500 characters")
  public String reason;

  public ApplyLeaveRequest() {}
}
