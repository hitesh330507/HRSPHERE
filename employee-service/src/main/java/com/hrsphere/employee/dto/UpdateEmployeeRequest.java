package com.hrsphere.employee.dto;

import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.entity.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public class UpdateEmployeeRequest {
  @Size(max = 100)
  @Schema(example = "Hitesh")
  public String firstName;

  @Size(max = 100)
  @Schema(example = "L")
  public String lastName;

  @Size(max = 20)
  @Schema(example = "+91-9876543210")
  public String phone;

  @Size(max = 100)
  @Schema(example = "Software Engineer")
  public String jobTitle;

  public EmploymentType employmentType;
  public LocalDate dateOfJoining;
  public UUID departmentId;
  public UUID managerId;

  @Past
  @Schema(example = "1990-01-01")
  public LocalDate dateOfBirth;

  public Gender gender;

  @Size(max = 100)
  public String nationality;

  @Valid public AddressDto address;

  @Size(max = 50)
  public String authUsername;
}
