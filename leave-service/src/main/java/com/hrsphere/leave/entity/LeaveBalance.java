package com.hrsphere.leave.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "leave_balances",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"employee_id", "leave_type_id", "year"})})
public class LeaveBalance {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id = UUID.randomUUID();

  @Column(name = "employee_id", nullable = false)
  private UUID employeeId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "leave_type_id", nullable = false)
  private LeaveType leaveType;

  @Column(name = "year", nullable = false)
  private Integer year;

  @Column(name = "allocated_days", nullable = false)
  private Integer allocatedDays;

  @Column(name = "used_days", nullable = false)
  private Integer usedDays = 0;

  @Column(name = "remaining_days", nullable = false)
  private Integer remainingDays;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  public LeaveBalance() {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getEmployeeId() {
    return employeeId;
  }

  public void setEmployeeId(UUID employeeId) {
    this.employeeId = employeeId;
  }

  public LeaveType getLeaveType() {
    return leaveType;
  }

  public void setLeaveType(LeaveType leaveType) {
    this.leaveType = leaveType;
  }

  public Integer getYear() {
    return year;
  }

  public void setYear(Integer year) {
    this.year = year;
  }

  public Integer getAllocatedDays() {
    return allocatedDays;
  }

  public void setAllocatedDays(Integer allocatedDays) {
    this.allocatedDays = allocatedDays;
  }

  public Integer getUsedDays() {
    return usedDays;
  }

  public void setUsedDays(Integer usedDays) {
    this.usedDays = usedDays;
  }

  public Integer getRemainingDays() {
    return remainingDays;
  }

  public void setRemainingDays(Integer remainingDays) {
    this.remainingDays = remainingDays;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
