package com.hrsphere.leave.dto;

import com.hrsphere.leave.entity.enums.LeaveStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class LeaveRequestResponse {
  public UUID id;
  public UUID employeeId;
  public String employeeName;
  public UUID leaveTypeId;
  public String leaveTypeName;
  public String leaveTypeCode;
  public LocalDate startDate;
  public LocalDate endDate;
  public Integer numberOfDays;
  public String reason;
  public LeaveStatus status;
  public LocalDateTime appliedAt;
  public String reviewedBy;
  public LocalDateTime reviewedAt;
  public String reviewComments;

  public LeaveRequestResponse() {}
}
