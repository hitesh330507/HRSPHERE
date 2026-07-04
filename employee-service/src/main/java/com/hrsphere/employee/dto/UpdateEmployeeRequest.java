package com.hrsphere.employee.dto;

import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.entity.enums.Gender;
import java.time.LocalDate;
import java.util.UUID;

public class UpdateEmployeeRequest {
  public String firstName;
  public String lastName;
  public String phone;
  public String jobTitle;
  public EmploymentType employmentType;
  public LocalDate dateOfJoining;
  public UUID departmentId;
  public UUID managerId;
  public LocalDate dateOfBirth;
  public Gender gender;
  public String nationality;
  public AddressDto address;
  public String authUsername;
}
