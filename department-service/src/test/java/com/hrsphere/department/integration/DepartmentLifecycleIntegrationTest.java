package com.hrsphere.department.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.hrsphere.common.dto.ApiErrorResponse;
import com.hrsphere.department.dto.CreateDepartmentRequest;
import com.hrsphere.department.dto.DepartmentResponse;
import com.hrsphere.department.dto.UpdateDepartmentRequest;
import com.hrsphere.department.entity.Department;
import com.hrsphere.department.integration.fixtures.DepartmentTestFixtures;
import com.hrsphere.department.repository.DepartmentRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("Department Service Lifecycle Integration Tests")
public class DepartmentLifecycleIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private DepartmentRepository departmentRepository;

  @BeforeEach
  void cleanDb() {
    departmentRepository.deleteAll();
  }

  @Test
  @DisplayName("Full CRUD and status lifecycle for department")
  void createDepartment_fullLifecycle() {
    // Step 1 — Create department
    CreateDepartmentRequest createReq = DepartmentTestFixtures.validCreateRequest("Engineering");
    HttpHeaders hrHdrs = hrHeaders();
    HttpEntity<CreateDepartmentRequest> createEntity = new HttpEntity<>(createReq, hrHdrs);

    ResponseEntity<DepartmentResponse> createResp = restTemplate.postForEntity(
        "/department", createEntity, DepartmentResponse.class);

    assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    DepartmentResponse created = createResp.getBody();
    assertThat(created).isNotNull();
    assertThat(created.departmentCode).matches("DEPT-\\d{4}");
    assertThat(created.name).isEqualTo("Engineering");
    assertThat(created.employeeCount).isEqualTo(0);

    // Assert exists in DB
    Optional<Department> dbOpt = departmentRepository.findById(created.id);
    assertThat(dbOpt).isPresent();
    assertThat(dbOpt.get().getName()).isEqualTo("Engineering");

    // Step 2 — Duplicate name rejected with 409
    ResponseEntity<ApiErrorResponse> duplicateResp = restTemplate.postForEntity(
        "/department", createEntity, ApiErrorResponse.class);
    assertThat(duplicateResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(duplicateResp.getBody()).isNotNull();
    assertThat(duplicateResp.getBody().message()).containsIgnoringCase("already exists");

    // Stub employee count fetch to return 10 employees
    employeeServiceMock.stubFor(get(urlEqualTo("/employees/list?departmentId=" + created.id + "&size=1"))
        .willReturn(okJson("{\"totalElements\": 10}")));

    // Step 3 — Get by ID
    HttpHeaders validatedHdrs = new HttpHeaders();
    validatedHdrs.set("X-Auth-Validated", "true");
    ResponseEntity<DepartmentResponse> getByIdResp = restTemplate.exchange(
        "/department/" + created.id, HttpMethod.GET, new HttpEntity<>(validatedHdrs), DepartmentResponse.class);
    assertThat(getByIdResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getByIdResp.getBody()).isNotNull();
    assertThat(getByIdResp.getBody().employeeCount).isEqualTo(10);

    // Step 4 — Get by code
    ResponseEntity<DepartmentResponse> getByCodeResp = restTemplate.exchange(
        "/department/code/" + created.departmentCode, HttpMethod.GET, new HttpEntity<>(validatedHdrs), DepartmentResponse.class);
    assertThat(getByCodeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getByCodeResp.getBody()).isNotNull();
    assertThat(getByCodeResp.getBody().id).isEqualTo(created.id);

    // Step 5 — Update (PUT)
    UpdateDepartmentRequest updateReq = new UpdateDepartmentRequest();
    updateReq.name = "R&D Engineering";
    updateReq.description = "New R&D description";
    HttpEntity<UpdateDepartmentRequest> updateEntity = new HttpEntity<>(updateReq, hrHdrs);

    ResponseEntity<DepartmentResponse> updateResp = restTemplate.exchange(
        "/department/" + created.id, HttpMethod.PUT, updateEntity, DepartmentResponse.class);
    assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updateResp.getBody()).isNotNull();
    assertThat(updateResp.getBody().name).isEqualTo("R&D Engineering");
    assertThat(updateResp.getBody().description).isEqualTo("New R&D description");

    // Step 6 — Soft delete (requires ADMIN)
    ResponseEntity<Void> deleteResp = restTemplate.exchange(
        "/department/" + created.id, HttpMethod.DELETE, new HttpEntity<>(adminHeaders()), Void.class);
    assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // Step 7 — Deleted department absent from list
    ResponseEntity<Map> listResp = restTemplate.exchange(
        "/department", HttpMethod.GET, new HttpEntity<>(validatedHdrs), Map.class);
    assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResp.getBody()).isNotNull();
    java.util.List<?> content = (java.util.List<?>) listResp.getBody().get("content");
    boolean found = false;
    for (Object obj : content) {
      Map<?, ?> m = (Map<?, ?>) obj;
      if (m.get("id").toString().equals(created.id.toString())) {
        found = true;
        break;
      }
    }
    assertThat(found).isFalse();

    // Step 8 — Direct GET returns 404 for soft-deleted department
    ResponseEntity<ApiErrorResponse> getDeletedResp = restTemplate.exchange(
        "/department/" + created.id, HttpMethod.GET, new HttpEntity<>(validatedHdrs), ApiErrorResponse.class);
    assertThat(getDeletedResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("Role enforcement check on department creation and deletion")
  void createDepartment_roleEnforcement() {
    CreateDepartmentRequest createReq = DepartmentTestFixtures.validCreateRequest("HR Department");
    HttpHeaders empHdrs = employeeHeaders("employee-user");
    HttpEntity<CreateDepartmentRequest> empEntity = new HttpEntity<>(createReq, empHdrs);

    // ROLE_EMPLOYEE cannot create
    ResponseEntity<ApiErrorResponse> createResp = restTemplate.postForEntity(
        "/department", empEntity, ApiErrorResponse.class);
    assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // ROLE_HR can create
    ResponseEntity<DepartmentResponse> hrCreateResp = restTemplate.postForEntity(
        "/department", new HttpEntity<>(createReq, hrHeaders()), DepartmentResponse.class);
    assertThat(hrCreateResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UUID deptId = hrCreateResp.getBody().id;

    // ROLE_HR cannot delete
    ResponseEntity<ApiErrorResponse> hrDeleteResp = restTemplate.exchange(
        "/department/" + deptId, HttpMethod.DELETE, new HttpEntity<>(hrHeaders()), ApiErrorResponse.class);
    assertThat(hrDeleteResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // ROLE_ADMIN can delete
    ResponseEntity<Void> adminDeleteResp = restTemplate.exchange(
        "/department/" + deptId, HttpMethod.DELETE, new HttpEntity<>(adminHeaders()), Void.class);
    assertThat(adminDeleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  @DisplayName("Get department details gracefully proceeds if employee-service is down")
  void getDepartment_employeeServiceDown_proceedsGracefully() {
    CreateDepartmentRequest createReq = DepartmentTestFixtures.validCreateRequest("Resilience Dept");
    ResponseEntity<DepartmentResponse> createResp = restTemplate.postForEntity(
        "/department", new HttpEntity<>(createReq, adminHeaders()), DepartmentResponse.class);
    assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UUID id = createResp.getBody().id;

    // Stub employee-service to fail/connection reset
    employeeServiceMock.stubFor(get(urlEqualTo("/employees/list?departmentId=" + id + "&size=1"))
        .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

    HttpHeaders validatedHdrs = new HttpHeaders();
    validatedHdrs.set("X-Auth-Validated", "true");

    ResponseEntity<DepartmentResponse> getResp = restTemplate.exchange(
        "/department/" + id, HttpMethod.GET, new HttpEntity<>(validatedHdrs), DepartmentResponse.class);

    // Should succeed with employeeCount as null or default, not fail with 500
    assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResp.getBody()).isNotNull();
    assertThat(getResp.getBody().employeeCount).isNull();
  }

  @Test
  @DisplayName("Validation constraints check")
  void validation_constraints() {
    // Empty name
    CreateDepartmentRequest req1 = new CreateDepartmentRequest();
    req1.name = "";
    ResponseEntity<ApiErrorResponse> resp1 = restTemplate.postForEntity(
        "/department", new HttpEntity<>(req1, adminHeaders()), ApiErrorResponse.class);
    assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // Name too long (> 100 chars)
    CreateDepartmentRequest req2 = new CreateDepartmentRequest();
    req2.name = "A".repeat(101);
    ResponseEntity<ApiErrorResponse> resp2 = restTemplate.postForEntity(
        "/department", new HttpEntity<>(req2, adminHeaders()), ApiErrorResponse.class);
    assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // Invalid UUID format in path
    HttpHeaders validatedHdrs = new HttpHeaders();
    validatedHdrs.set("X-Auth-Validated", "true");
    ResponseEntity<ApiErrorResponse> resp3 = restTemplate.exchange(
        "/department/invalid-uuid", HttpMethod.GET, new HttpEntity<>(validatedHdrs), ApiErrorResponse.class);
    assertThat(resp3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
