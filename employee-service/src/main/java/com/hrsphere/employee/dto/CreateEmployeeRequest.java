package com.hrsphere.employee.dto;

import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.entity.enums.Gender;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public class CreateEmployeeRequest {

  @NotBlank @Size(max=100)
  public String firstName;

  @NotBlank @Size(max=100)
  public String lastName;

  @NotBlank @Email
  public String email;

  public String phone;

  @NotBlank
  public String jobTitle;

  @NotNull
  public EmploymentType employmentType;

  @NotNull
  public LocalDate dateOfJoining;

  public UUID departmentId;
  public UUID managerId;
  public LocalDate dateOfBirth;
  public Gender gender;
  public String nationality;
  public AddressDto address;
  public String authUsername;
  public String bankAccountNumber;
  public String bankName;
  public String taxId;
}
