package com.hrsphere.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.hrsphere.common.exception.InvalidReferenceException;
import com.hrsphere.common.exception.ResourceAlreadyExistsException;
import com.hrsphere.common.exception.ResourceNotFoundException;
import com.hrsphere.employee.dto.CreateEmployeeRequest;
import com.hrsphere.employee.dto.EmployeeResponse;
import com.hrsphere.employee.entity.Employee;
import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.mapper.EmployeeMapper;
import com.hrsphere.employee.repository.EmployeeRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

  @Mock private EmployeeRepository repository;
  @Mock private EmployeeCodeGenerator codeGenerator;
  @Mock private RestTemplate restTemplate;
  @Spy private EmployeeMapper mapper = Mappers.getMapper(EmployeeMapper.class);

  @InjectMocks private EmployeeService service;

  @Test
  void createEmployee_shouldCreateAndGenerateCode() {
    CreateEmployeeRequest req = new CreateEmployeeRequest();
    req.firstName = "Hitesh";
    req.lastName = "L";
    req.email = "hitesh.l@hrsphere.dev";
    req.jobTitle = "Backend Developer";
    req.employmentType = EmploymentType.FULL_TIME;
    req.dateOfJoining = LocalDate.of(2026, 1, 1);

    given(repository.findByEmailAndIsDeletedFalse(req.email)).willReturn(Optional.empty());
    given(codeGenerator.nextCode()).willReturn("EMP-0001");

    EmployeeResponse resp = service.createEmployee(req, "admin");

    assertThat(resp).isNotNull();
    assertThat(resp.employeeCode).isEqualTo("EMP-0001");
    assertThat(resp.firstName).isEqualTo("Hitesh");

    ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
    verify(repository).save(captor.capture());
    Employee saved = captor.getValue();
    assertThat(saved.getEmployeeCode()).isEqualTo("EMP-0001");
    assertThat(saved.getEmail()).isEqualTo("hitesh.l@hrsphere.dev");
  }

  @Test
  void createEmployee_shouldThrowWhenEmailAlreadyExists() {
    CreateEmployeeRequest req = new CreateEmployeeRequest();
    req.email = "hitesh.l@hrsphere.dev";

    given(repository.findByEmailAndIsDeletedFalse(req.email))
        .willReturn(Optional.of(new Employee()));

    assertThatThrownBy(() -> service.createEmployee(req, "admin"))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessage("email already exists");
  }

  @Test
  void getById_shouldReturnEmployee() {
    UUID id = UUID.randomUUID();
    Employee e = new Employee();
    e.setFirstName("Hitesh");

    given(repository.findById(id)).willReturn(Optional.of(e));

    EmployeeResponse resp = service.getById(id);
    assertThat(resp).isNotNull();
    assertThat(resp.firstName).isEqualTo("Hitesh");
  }

  @Test
  void getById_shouldThrowWhenNotFoundOrDeleted() {
    UUID id = UUID.randomUUID();
    Employee e = new Employee();
    e.setDeleted(true);

    given(repository.findById(id)).willReturn(Optional.of(e));

    assertThatThrownBy(() -> service.getById(id))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("employee not found");
  }

  @Test
  void softDeleteEmployee_shouldSetDeletedAndDeletedAt() {
    UUID id = UUID.randomUUID();
    Employee e = new Employee();
    e.setDeleted(false);

    given(repository.findById(id)).willReturn(Optional.of(e));

    service.softDeleteEmployee(id, "admin");

    assertThat(e.isDeleted()).isTrue();
    assertThat(e.getDeletedAt()).isNotNull();
    verify(repository).save(e);
  }

  @Test
  void createEmployee_shouldThrowWhenDepartmentIdNotFound() {
    UUID deptId = UUID.randomUUID();
    CreateEmployeeRequest req = new CreateEmployeeRequest();
    req.email = "hitesh.l@hrsphere.dev";
    req.departmentId = deptId;

    given(
            restTemplate.exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
        .willThrow(
            new org.springframework.web.client.HttpClientErrorException(
                org.springframework.http.HttpStatus.NOT_FOUND));

    assertThatThrownBy(() -> service.createEmployee(req, "admin"))
        .isInstanceOf(InvalidReferenceException.class)
        .hasMessage("Department " + deptId + " does not exist");
  }

  @Test
  void createEmployee_shouldProceedWhenDepartmentServiceThrowsException() {
    UUID deptId = UUID.randomUUID();
    CreateEmployeeRequest req = new CreateEmployeeRequest();
    req.firstName = "Hitesh";
    req.lastName = "L";
    req.email = "hitesh.l@hrsphere.dev";
    req.jobTitle = "Backend Developer";
    req.employmentType = EmploymentType.FULL_TIME;
    req.dateOfJoining = LocalDate.of(2026, 1, 1);
    req.departmentId = deptId;

    given(
            restTemplate.exchange(
                any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object.class)))
        .willThrow(new RuntimeException("Connection refused"));

    given(repository.findByEmailAndIsDeletedFalse(req.email)).willReturn(Optional.empty());
    given(codeGenerator.nextCode()).willReturn("EMP-0001");

    EmployeeResponse resp = service.createEmployee(req, "admin");

    assertThat(resp).isNotNull();
    assertThat(resp.employeeCode).isEqualTo("EMP-0001");
  }
}
