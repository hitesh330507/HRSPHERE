package com.hrsphere.employee.dto;

import com.hrsphere.employee.entity.enums.EmploymentStatus;
import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.entity.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Detailed representation of an employee profile")
public class EmployeeResponse {

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

  @Schema(example = "+91-9876543210")
  public String phone;

  @Schema(example = "hitesh_l")
  public String authUsername;

  @Schema(example = "Software Engineer")
  public String jobTitle;

  @Schema(description = "FULL_TIME, PART_TIME, CONTRACT, or INTERN")
  public EmploymentType employmentType;

  @Schema(description = "ACTIVE, TERMINATED")
  public EmploymentStatus employmentStatus;

  @Schema(example = "2026-01-01")
  public LocalDate dateOfJoining;

  @Schema(example = "2028-12-31")
  public LocalDate dateOfTermination;

  @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
  public UUID departmentId;

  @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
  public UUID managerId;

  @Schema(example = "1990-01-01")
  public LocalDate dateOfBirth;

  @Schema(description = "MALE, FEMALE, OTHER")
  public Gender gender;

  @Schema(example = "Indian")
  public String nationality;

  public AddressDto address;

  @Schema(example = "2026-07-06T12:00:00")
  public LocalDateTime createdAt;

  @Schema(example = "2026-07-06T12:30:00")
  public LocalDateTime updatedAt;
}
