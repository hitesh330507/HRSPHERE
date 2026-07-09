package com.hrsphere.leave.dto;

public class LeaveBalanceResponse {
  public String leaveTypeName;
  public String leaveTypeCode;
  public Integer year;
  public Integer allocatedDays;
  public Integer usedDays;
  public Integer remainingDays;

  public LeaveBalanceResponse() {}
}
