package com.hrsphere.employee.dto;

import com.hrsphere.employee.entity.enums.EmploymentStatus;
import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.entity.enums.Gender;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class EmployeeResponse {
  public UUID id;
  public String employeeCode;
  public String firstName;
  public String lastName;
  public String email;
  public String phone;
  public String authUsername;
  public String jobTitle;
  public EmploymentType employmentType;
  public EmploymentStatus employmentStatus;
  public LocalDate dateOfJoining;
  public LocalDate dateOfTermination;
  public UUID departmentId;
  public UUID managerId;
  public LocalDate dateOfBirth;
  public Gender gender;
  public String nationality;
  public AddressDto address;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
}
