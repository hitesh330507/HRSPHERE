package com.hrsphere.employee.dto;

import com.hrsphere.employee.entity.enums.EmploymentStatus;
import com.hrsphere.employee.entity.enums.EmploymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Abbreviated representation of an employee for lists and searches")
public class EmployeeSummaryResponse {

  @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
  public UUID id;

  @Schema(example = "EMP-0001")
  public String employeeCode;

  @Schema(example = "Hitesh")
  public String firstName;

  @Schema(example = "L")
  public String lastName;

  @Schema(example = "hitesh@company.com")
  public String email;

  @Schema(example = "Software Engineer")
  public String jobTitle;

  @Schema(description = "FULL_TIME, PART_TIME, CONTRACT, or INTERN")
  public EmploymentType employmentType;

  @Schema(description = "ACTIVE, TERMINATED")
  public EmploymentStatus employmentStatus;

  @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
  public UUID departmentId;

  @Schema(example = "2026-07-06T12:00:00")
  public LocalDateTime createdAt;
}
