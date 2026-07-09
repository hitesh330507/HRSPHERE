package com.hrsphere.leave.dto;

import com.hrsphere.leave.entity.enums.LeaveStatus;
import java.time.LocalDate;
import java.util.UUID;

public class LeaveRequestSummaryResponse {
  public UUID id;
  public UUID employeeId;
  public String employeeName;
  public String leaveTypeCode;
  public LocalDate startDate;
  public LocalDate endDate;
  public Integer numberOfDays;
  public LeaveStatus status;

  public LeaveRequestSummaryResponse() {}
}
