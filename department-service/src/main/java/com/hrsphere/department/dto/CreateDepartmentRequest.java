package com.hrsphere.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class CreateDepartmentRequest {

  @NotBlank(message = "name is required")
  @Size(max = 100, message = "name must not exceed 100 characters")
  public String name;

  @Size(max = 500, message = "description must not exceed 500 characters")
  public String description;

  public UUID headOfDepartment;
}
