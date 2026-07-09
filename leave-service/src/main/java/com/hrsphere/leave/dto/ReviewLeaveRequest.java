package com.hrsphere.leave.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewLeaveRequest {

  @NotNull(message = "decision is required")
  public ReviewDecision decision;

  @Size(max = 500, message = "comments cannot exceed 500 characters")
  public String comments;

  public ReviewLeaveRequest() {}
}
