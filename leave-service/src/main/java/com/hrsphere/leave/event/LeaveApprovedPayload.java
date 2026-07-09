package com.hrsphere.leave.event;

import java.time.LocalDate;
import java.util.UUID;

public record LeaveApprovedPayload(
    UUID employeeId,
    String leaveTypeCode,
    LocalDate startDate,
    LocalDate endDate,
    Integer numberOfDays) {}
