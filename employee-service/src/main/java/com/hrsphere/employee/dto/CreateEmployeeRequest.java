package com.hrsphere.employee.dto;

import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.entity.enums.Gender;
import com.hrsphere.employee.validation.ValidJoiningDate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

@ValidJoiningDate
public class CreateEmployeeRequest {

  @NotBlank
  @Size(max = 100)
  @Schema(example = "Hitesh")
  public String firstName;

  @NotBlank
  @Size(max = 100)
  @Schema(example = "L")
  public String lastName;

  @NotBlank
  @Email
  @Size(max = 255)
  @Schema(example = "hitesh@company.com")
  public String email;

  @Size(max = 20)
  @Schema(example = "+91-9876543210")
  public String phone;

  @NotBlank
  @Size(max = 100)
  @Schema(example = "Software Engineer")
  public String jobTitle;

  @NotNull
  @Schema(description = "FULL_TIME, PART_TIME, CONTRACT, or INTERN")
  public EmploymentType employmentType;

  @NotNull
  @Schema(example = "2026-01-01")
  public java.time.LocalDate dateOfJoining;

  public UUID departmentId;
  public UUID managerId;

  @Past
  @Schema(example = "1990-01-01")
  public java.time.LocalDate dateOfBirth;

  public Gender gender;

  @Size(max = 100)
  public String nationality;

  @Valid public AddressDto address;

  @Size(max = 50)
  public String authUsername;

  @Size(max = 50)
  public String bankAccountNumber;

  @Size(max = 100)
  public String bankName;

  @Size(max = 50)
  public String taxId;
}
