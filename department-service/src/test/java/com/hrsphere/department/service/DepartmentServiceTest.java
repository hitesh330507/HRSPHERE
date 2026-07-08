package com.hrsphere.department.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.hrsphere.common.exception.ResourceAlreadyExistsException;
import com.hrsphere.department.dto.CreateDepartmentRequest;
import com.hrsphere.department.dto.DepartmentResponse;
import com.hrsphere.department.entity.Department;
import com.hrsphere.department.mapper.DepartmentMapper;
import com.hrsphere.department.repository.DepartmentRepository;
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
class DepartmentServiceTest {

  @Mock private DepartmentRepository repository;
  @Mock private DepartmentCodeGenerator codeGenerator;
  @Mock private RestTemplate restTemplate;
  @Spy private DepartmentMapper mapper = Mappers.getMapper(DepartmentMapper.class);

  @InjectMocks private DepartmentService service;

  @Test
  void createDepartment_shouldCreateSuccessfully() {
    CreateDepartmentRequest req = new CreateDepartmentRequest();
    req.name = "Engineering";
    req.description = "Core Tech Team";

    given(repository.findByNameAndIsDeletedFalse("Engineering")).willReturn(Optional.empty());
    given(codeGenerator.nextCode()).willReturn("DEPT-0001");

    DepartmentResponse resp = service.createDepartment(req, "admin");

    assertThat(resp).isNotNull();
    assertThat(resp.departmentCode).isEqualTo("DEPT-0001");
    assertThat(resp.name).isEqualTo("Engineering");
    assertThat(resp.employeeCount).isEqualTo(0);

    ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
    verify(repository).save(captor.capture());
    Department saved = captor.getValue();
    assertThat(saved.getDepartmentCode()).isEqualTo("DEPT-0001");
    assertThat(saved.getName()).isEqualTo("Engineering");
  }

  @Test
  void createDepartment_shouldThrowWhenNameAlreadyExists() {
    CreateDepartmentRequest req = new CreateDepartmentRequest();
    req.name = "Engineering";

    given(repository.findByNameAndIsDeletedFalse("Engineering"))
        .willReturn(Optional.of(new Department()));

    assertThatThrownBy(() -> service.createDepartment(req, "admin"))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessage("department name already exists");
  }

  @Test
  void getById_shouldReturnGracefulNullForCountOnFailure() {
    UUID id = UUID.randomUUID();
    Department d = new Department();
    d.setId(id);
    d.setName("Engineering");
    d.setDepartmentCode("DEPT-0001");

    given(repository.findById(id)).willReturn(Optional.of(d));
    given(
            restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(java.util.Map.class)))
        .willThrow(new RuntimeException("Connection refused"));

    DepartmentResponse resp = service.getById(id);

    assertThat(resp).isNotNull();
    assertThat(resp.employeeCount).isNull();
  }

  @Test
  void softDelete_shouldMarkDeleted() {
    UUID id = UUID.randomUUID();
    Department d = new Department();
    d.setId(id);
    d.setDeleted(false);

    given(repository.findById(id)).willReturn(Optional.of(d));

    service.softDeleteDepartment(id, "admin");

    assertThat(d.isDeleted()).isTrue();
    assertThat(d.getDeletedAt()).isNotNull();
    verify(repository).save(d);
  }
}
