package com.hrsphere.leave.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "leave_types",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = "name"),
      @UniqueConstraint(columnNames = "code")
    })
public class LeaveType {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id = UUID.randomUUID();

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "code", nullable = false, length = 10)
  private String code;

  @Column(name = "default_annual_days", nullable = false)
  private Integer defaultAnnualDays;

  @Column(name = "is_paid", nullable = false)
  private boolean isPaid = true;

  @Column(name = "requires_approval", nullable = false)
  private boolean requiresApproval = true;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  public LeaveType() {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Integer getDefaultAnnualDays() {
    return defaultAnnualDays;
  }

  public void setDefaultAnnualDays(Integer defaultAnnualDays) {
    this.defaultAnnualDays = defaultAnnualDays;
  }

  public boolean isPaid() {
    return isPaid;
  }

  public void setPaid(boolean paid) {
    isPaid = paid;
  }

  public boolean isRequiresApproval() {
    return requiresApproval;
  }

  public void setRequiresApproval(boolean requiresApproval) {
    this.requiresApproval = requiresApproval;
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
