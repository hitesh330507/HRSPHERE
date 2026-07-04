package com.hrsphere.employee.dto;

import com.hrsphere.employee.entity.enums.EmploymentStatus;
import com.hrsphere.employee.entity.enums.EmploymentType;
import java.time.LocalDateTime;
import java.util.UUID;

public class EmployeeSummaryResponse {
  public UUID id;
  public String employeeCode;
  public String firstName;
  public String lastName;
  public String email;
  public String jobTitle;
  public EmploymentType employmentType;
  public EmploymentStatus employmentStatus;
  public UUID departmentId;
  public LocalDateTime createdAt;
}
