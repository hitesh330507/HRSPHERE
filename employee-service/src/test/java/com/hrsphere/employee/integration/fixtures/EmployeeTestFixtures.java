package com.hrsphere.employee.integration.fixtures;

import com.hrsphere.employee.dto.AddressDto;
import com.hrsphere.employee.dto.CreateEmployeeRequest;
import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.entity.enums.Gender;
import java.time.LocalDate;
import java.util.UUID;

public class EmployeeTestFixtures {

  public static CreateEmployeeRequest validCreateRequest() {
    return validCreateRequest("john.doe@example.com");
  }

  public static CreateEmployeeRequest validCreateRequest(String email) {
    CreateEmployeeRequest req = new CreateEmployeeRequest();
    req.firstName = "John";
    req.lastName = "Doe";
    req.email = email;
    req.phone = "+1-555-0199";
    req.jobTitle = "Software Engineer";
    req.employmentType = EmploymentType.FULL_TIME;
    req.dateOfJoining = LocalDate.now().minusDays(1);
    req.dateOfBirth = LocalDate.of(1990, 1, 1);
    req.gender = Gender.MALE;
    req.nationality = "American";
    req.authUsername = "user_" + UUID.randomUUID().toString().substring(0, 8);

    AddressDto address = new AddressDto();
    address.street = "123 Main St";
    address.city = "Seattle";
    address.state = "WA";
    address.postalCode = "98101";
    address.country = "USA";
    req.address = address;

    req.bankAccountNumber = "123456789";
    req.bankName = "Chase Bank";
    req.taxId = "TX-9876";
    return req;
  }
}
