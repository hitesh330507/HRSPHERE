package com.hrsphere.department.integration.fixtures;

import com.hrsphere.department.dto.CreateDepartmentRequest;

public class DepartmentTestFixtures {

  public static CreateDepartmentRequest validCreateRequest() {
    return validCreateRequest("Engineering");
  }

  public static CreateDepartmentRequest validCreateRequest(String name) {
    CreateDepartmentRequest req = new CreateDepartmentRequest();
    req.name = name;
    req.description = "Engineering and Development Department";
    return req;
  }
}
