package com.hrsphere.common.event;

public final class EventType {

  public static final String USER_CREATED = "user.created";

  // Reserved for Phase 3 continuation -- not yet published. See docs/event-catalog.md.
  public static final String EMPLOYEE_ONBOARDED = "employee.onboarded";
  public static final String LEAVE_APPROVED = "leave.approved";
  public static final String LEAVE_REJECTED = "leave.rejected";

  private EventType() {}
}
