package com.hrsphere.leave.integration.fixtures;

import com.hrsphere.leave.dto.ApplyLeaveRequest;
import com.hrsphere.leave.dto.ReviewDecision;
import com.hrsphere.leave.dto.ReviewLeaveRequest;
import java.time.LocalDate;
import java.util.UUID;

public class LeaveTestFixtures {

  public static ApplyLeaveRequest validApplyRequest(
      UUID leaveTypeId, LocalDate start, LocalDate end, String reason) {
    ApplyLeaveRequest req = new ApplyLeaveRequest();
    req.leaveTypeId = leaveTypeId;
    req.startDate = start;
    req.endDate = end;
    req.reason = reason;
    return req;
  }

  public static ReviewLeaveRequest approveRequest() {
    ReviewLeaveRequest req = new ReviewLeaveRequest();
    req.decision = ReviewDecision.APPROVE;
    req.comments = "Approved via integration test";
    return req;
  }

  public static ReviewLeaveRequest rejectRequest(String reason) {
    ReviewLeaveRequest req = new ReviewLeaveRequest();
    req.decision = ReviewDecision.REJECT;
    req.comments = reason;
    return req;
  }
}
