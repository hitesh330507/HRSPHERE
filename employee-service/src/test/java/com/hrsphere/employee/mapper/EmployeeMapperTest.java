package com.hrsphere.employee.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.hrsphere.employee.dto.*;
import com.hrsphere.employee.entity.Address;
import com.hrsphere.employee.entity.Employee;
import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.entity.enums.Gender;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class EmployeeMapperTest {

  private final EmployeeMapper mapper = Mappers.getMapper(EmployeeMapper.class);

  @Test
  void testToEntity() {
    CreateEmployeeRequest req = new CreateEmployeeRequest();
    req.firstName = "Hitesh";
    req.lastName = "L";
    req.email = "hitesh@company.com";
    req.phone = "+91-9876543210";
    req.jobTitle = "Software Engineer";
    req.employmentType = EmploymentType.FULL_TIME;
    req.dateOfJoining = LocalDate.of(2026, 1, 1);
    req.dateOfBirth = LocalDate.of(1995, 5, 15);
    req.gender = Gender.MALE;
    req.nationality = "Indian";
    req.authUsername = "hitesh_l";

    AddressDto addrDto = new AddressDto();
    addrDto.street = "123 MG Road";
    addrDto.city = "Bengaluru";
    addrDto.state = "Karnataka";
    addrDto.postalCode = "560001";
    addrDto.country = "India";
    req.address = addrDto;

    Employee emp = mapper.toEntity(req);

    assertNotNull(emp);
    assertNotNull(emp.getId());
    assertNull(emp.getEmployeeCode());
    assertEquals("Hitesh", emp.getFirstName());
    assertEquals("L", emp.getLastName());
    assertEquals("hitesh@company.com", emp.getEmail());
    assertEquals("+91-9876543210", emp.getPhone());
    assertEquals("Software Engineer", emp.getJobTitle());
    assertEquals(EmploymentType.FULL_TIME, emp.getEmploymentType());
    assertEquals(LocalDate.of(2026, 1, 1), emp.getDateOfJoining());
    assertEquals(LocalDate.of(1995, 5, 15), emp.getDateOfBirth());
    assertEquals(Gender.MALE, emp.getGender());
    assertEquals("Indian", emp.getNationality());
    assertEquals("hitesh_l", emp.getAuthUsername());

    assertNotNull(emp.getAddress());
    assertEquals("123 MG Road", emp.getAddress().getStreet());
    assertEquals("Bengaluru", emp.getAddress().getCity());
    assertEquals("Karnataka", emp.getAddress().getState());
    assertEquals("560001", emp.getAddress().getPostalCode());
    assertEquals("India", emp.getAddress().getCountry());
  }

  @Test
  void testUpdateEntityFromRequest_PatchSemantics() {
    Employee emp = new Employee();
    emp.setFirstName("Hitesh");
    emp.setLastName("L");
    emp.setEmail("hitesh@company.com");
    emp.setJobTitle("Software Engineer");
    emp.setEmploymentType(EmploymentType.FULL_TIME);

    Address addr = new Address();
    addr.setStreet("123 MG Road");
    addr.setCity("Bengaluru");
    addr.setState("Karnataka");
    addr.setPostalCode("560001");
    addr.setCountry("India");
    emp.setAddress(addr);

    UpdateEmployeeRequest req = new UpdateEmployeeRequest();
    req.firstName = "Hitesh Updated";
    // lastName and email are null/ignored in request
    req.jobTitle = null; // Should not overwrite Software Engineer

    AddressDto addrDto = new AddressDto();
    addrDto.city = "Mysuru"; // Update city only
    // street, state, postalCode, country are null
    req.address = addrDto;

    mapper.updateEntityFromRequest(req, emp);

    assertEquals("Hitesh%s".formatted(" Updated"), emp.getFirstName());
    assertEquals("L", emp.getLastName()); // unchanged
    assertEquals("hitesh@company.com", emp.getEmail()); // unchanged
    assertEquals("Software Engineer", emp.getJobTitle()); // unchanged

    assertNotNull(emp.getAddress());
    assertEquals(
        "123 MG Road", emp.getAddress().getStreet()); // unchanged due to null value strategy
    assertEquals("Mysuru", emp.getAddress().getCity()); // updated
    assertEquals("Karnataka", emp.getAddress().getState()); // unchanged
    assertEquals("560001", emp.getAddress().getPostalCode()); // unchanged
    assertEquals("India", emp.getAddress().getCountry()); // unchanged
  }

  @Test
  void testUpdateOwnProfile_RestrictsFields() {
    Employee emp = new Employee();
    emp.setFirstName("Hitesh");
    emp.setLastName("L");
    emp.setPhone("+91-9876543210");
    emp.setJobTitle("Software Engineer");

    Address addr = new Address();
    addr.setStreet("123 MG Road");
    addr.setCity("Bengaluru");
    emp.setAddress(addr);

    UpdateOwnProfileRequest req = new UpdateOwnProfileRequest();
    req.phone = "+91-1111111111";

    AddressDto addrDto = new AddressDto();
    addrDto.city = "Chennai";
    req.address = addrDto;

    mapper.updateOwnProfile(req, emp);

    assertEquals("+91-1111111111", emp.getPhone());
    assertEquals("Hitesh", emp.getFirstName()); // restricted
    assertEquals("L", emp.getLastName()); // restricted
    assertEquals("Software Engineer", emp.getJobTitle()); // restricted

    assertNotNull(emp.getAddress());
    assertEquals("123 MG Road", emp.getAddress().getStreet()); // unchanged
    assertEquals("Chennai", emp.getAddress().getCity()); // updated
  }
}
