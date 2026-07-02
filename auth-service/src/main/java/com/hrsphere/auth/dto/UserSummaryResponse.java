package com.hrsphere.auth.dto;

import java.util.Map;

public class UserSummaryResponse {

  private long totalUsers;
  private Map<String, Long> byRole;
  private long activeUsers;
  private long inactiveUsers;

  public UserSummaryResponse() {}

  public UserSummaryResponse(
      long totalUsers, Map<String, Long> byRole, long activeUsers, long inactiveUsers) {
    this.totalUsers = totalUsers;
    this.byRole = byRole;
    this.activeUsers = activeUsers;
    this.inactiveUsers = inactiveUsers;
  }

  public long getTotalUsers() {
    return totalUsers;
  }

  public void setTotalUsers(long totalUsers) {
    this.totalUsers = totalUsers;
  }

  public Map<String, Long> getByRole() {
    return byRole;
  }

  public void setByRole(Map<String, Long> byRole) {
    this.byRole = byRole;
  }

  public long getActiveUsers() {
    return activeUsers;
  }

  public void setActiveUsers(long activeUsers) {
    this.activeUsers = activeUsers;
  }

  public long getInactiveUsers() {
    return inactiveUsers;
  }

  public void setInactiveUsers(long inactiveUsers) {
    this.inactiveUsers = inactiveUsers;
  }
}
