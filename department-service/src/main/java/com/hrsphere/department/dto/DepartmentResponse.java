package com.hrsphere.department.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class DepartmentResponse {

  public UUID id;
  public String departmentCode;
  public String name;
  public String description;
  public UUID headOfDepartment;
  public Integer employeeCount;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
}
