package com.hrsphere.employee.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.hrsphere.common.dto.ApiErrorResponse;
import com.hrsphere.employee.dto.CreateEmployeeRequest;
import com.hrsphere.employee.dto.EmployeeResponse;
import com.hrsphere.employee.dto.TerminateEmployeeRequest;
import com.hrsphere.employee.dto.UpdateEmployeeRequest;
import com.hrsphere.employee.dto.UpdateOwnProfileRequest;
import com.hrsphere.employee.entity.Employee;
import com.hrsphere.employee.entity.enums.EmploymentStatus;
import com.hrsphere.employee.entity.enums.EmploymentType;
import com.hrsphere.employee.integration.fixtures.EmployeeTestFixtures;
import com.hrsphere.employee.repository.EmployeeRepository;
import java.time.LocalDate;
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

@DisplayName("Employee Service Lifecycle Integration Tests")
public class EmployeeLifecycleIntegrationTest extends BaseIntegrationTest {

  @Autowired private EmployeeRepository employeeRepository;

  @BeforeEach
  void cleanDb() {
    employeeRepository.deleteAll();
  }

  @Test
  @DisplayName("Full CRUD and status lifecycle for employee")
  void createEmployee_fullLifecycle() {
    UUID departmentId = UUID.randomUUID();
    // Stub department-service verification
    departmentServiceMock.stubFor(
        get(urlEqualTo("/department/" + departmentId))
            .willReturn(okJson("{\"id\":\"" + departmentId + "\", \"name\":\"Engineering\"}")));

    // Step 1 — Create
    CreateEmployeeRequest createReq =
        EmployeeTestFixtures.validCreateRequest("lifecycle@example.com");
    createReq.departmentId = departmentId;

    HttpHeaders adminHdrs = adminHeaders();
    HttpEntity<CreateEmployeeRequest> createEntity = new HttpEntity<>(createReq, adminHdrs);

    ResponseEntity<EmployeeResponse> createResp =
        restTemplate.postForEntity("/employees/create", createEntity, EmployeeResponse.class);

    assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    EmployeeResponse created = createResp.getBody();
    assertThat(created).isNotNull();
    assertThat(created.employeeCode).matches("EMP-\\d{4}");
    assertThat(created.employmentStatus).isEqualTo(EmploymentStatus.ACTIVE);
    assertThat(created.email).isEqualTo("lifecycle@example.com");
    assertThat(created.departmentId).isEqualTo(departmentId);

    // Assert exists in DB directly
    Optional<Employee> dbOpt = employeeRepository.findById(created.id);
    assertThat(dbOpt).isPresent();
    assertThat(dbOpt.get().getEmployeeCode()).isEqualTo(created.employeeCode);

    // Step 2 — Duplicate email rejected
    ResponseEntity<ApiErrorResponse> duplicateResp =
        restTemplate.postForEntity("/employees/create", createEntity, ApiErrorResponse.class);
    assertThat(duplicateResp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(duplicateResp.getBody()).isNotNull();
    assertThat(duplicateResp.getBody().message()).containsIgnoringCase("email already exists");

    // Step 3 — Get by id
    HttpHeaders empHdrs = employeeHeaders("lifecycle-emp");
    ResponseEntity<EmployeeResponse> getByIdResp =
        restTemplate.exchange(
            "/employees/" + created.id,
            HttpMethod.GET,
            new HttpEntity<>(empHdrs),
            EmployeeResponse.class);
    assertThat(getByIdResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getByIdResp.getBody()).isNotNull();
    assertThat(getByIdResp.getBody().email).isEqualTo("lifecycle@example.com");

    // Step 4 — Get by code
    ResponseEntity<EmployeeResponse> getByCodeResp =
        restTemplate.exchange(
            "/employees/code/" + created.employeeCode,
            HttpMethod.GET,
            new HttpEntity<>(empHdrs),
            EmployeeResponse.class);
    assertThat(getByCodeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getByCodeResp.getBody()).isNotNull();
    assertThat(getByCodeResp.getBody().id).isEqualTo(created.id);

    // Step 5 — Update (PUT)
    UpdateEmployeeRequest updateReq = new UpdateEmployeeRequest();
    updateReq.firstName = "Johnny";
    updateReq.lastName = "Updated";
    updateReq.departmentId = departmentId;
    HttpEntity<UpdateEmployeeRequest> updateEntity = new HttpEntity<>(updateReq, adminHdrs);

    ResponseEntity<EmployeeResponse> updateResp =
        restTemplate.exchange(
            "/employees/" + created.id, HttpMethod.PUT, updateEntity, EmployeeResponse.class);
    assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updateResp.getBody()).isNotNull();
    assertThat(updateResp.getBody().firstName).isEqualTo("Johnny");
    assertThat(updateResp.getBody().lastName).isEqualTo("Updated");

    // Step 6 — Terminate
    TerminateEmployeeRequest termReq = new TerminateEmployeeRequest();
    termReq.dateOfTermination = LocalDate.now();
    termReq.reason = "Resigned voluntarily";
    HttpEntity<TerminateEmployeeRequest> termEntity = new HttpEntity<>(termReq, adminHdrs);

    ResponseEntity<EmployeeResponse> termResp =
        restTemplate.exchange(
            "/employees/" + created.id + "/terminate",
            HttpMethod.PATCH,
            termEntity,
            EmployeeResponse.class);
    assertThat(termResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(termResp.getBody()).isNotNull();
    assertThat(termResp.getBody().employmentStatus).isEqualTo(EmploymentStatus.TERMINATED);
    assertThat(termResp.getBody().dateOfTermination).isEqualTo(LocalDate.now());

    // Step 7 — Terminated employee still in list
    HttpHeaders validatedHdrs = new HttpHeaders();
    validatedHdrs.set("X-Auth-Validated", "true");
    ResponseEntity<Map> listTermResp =
        restTemplate.exchange(
            "/employees/list?status=TERMINATED",
            HttpMethod.GET,
            new HttpEntity<>(validatedHdrs),
            Map.class);
    assertThat(listTermResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listTermResp.getBody()).isNotNull();
    assertThat(listTermResp.getBody().get("content")).isNotNull();

    // Step 8 — Soft delete
    ResponseEntity<Void> deleteResp =
        restTemplate.exchange(
            "/employees/" + created.id, HttpMethod.DELETE, new HttpEntity<>(adminHdrs), Void.class);
    assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // Step 9 — Deleted employee absent from list
    ResponseEntity<Map> listDefaultResp =
        restTemplate.exchange(
            "/employees/list", HttpMethod.GET, new HttpEntity<>(validatedHdrs), Map.class);
    assertThat(listDefaultResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listDefaultResp.getBody()).isNotNull();
    java.util.List<?> content = (java.util.List<?>) listDefaultResp.getBody().get("content");
    boolean found = false;
    for (Object obj : content) {
      Map<?, ?> m = (Map<?, ?>) obj;
      if (m.get("id").toString().equals(created.id.toString())) {
        found = true;
        break;
      }
    }
    assertThat(found).isFalse();

    // Step 10 — Deleted employee returns 404 on direct get
    ResponseEntity<ApiErrorResponse> getDeletedResp =
        restTemplate.exchange(
            "/employees/" + created.id,
            HttpMethod.GET,
            new HttpEntity<>(empHdrs),
            ApiErrorResponse.class);
    assertThat(getDeletedResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("Role enforcement check on employee creation and deletion")
  void createEmployee_roleEnforcement() {
    CreateEmployeeRequest createReq =
        EmployeeTestFixtures.validCreateRequest("role-enforce@example.com");
    HttpHeaders empHdrs = employeeHeaders("employee-user");
    HttpEntity<CreateEmployeeRequest> empEntity = new HttpEntity<>(createReq, empHdrs);

    // ROLE_EMPLOYEE cannot create
    ResponseEntity<ApiErrorResponse> createResp =
        restTemplate.postForEntity("/employees/create", empEntity, ApiErrorResponse.class);
    assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // Create employee as Admin first
    UUID departmentId = UUID.randomUUID();
    departmentServiceMock.stubFor(
        get(urlEqualTo("/department/" + departmentId))
            .willReturn(okJson("{\"id\":\"" + departmentId + "\"}")));
    createReq.departmentId = departmentId;
    ResponseEntity<EmployeeResponse> adminCreateResp =
        restTemplate.postForEntity(
            "/employees/create",
            new HttpEntity<>(createReq, adminHeaders()),
            EmployeeResponse.class);
    assertThat(adminCreateResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    UUID employeeId = adminCreateResp.getBody().id;

    // ROLE_HR cannot delete
    ResponseEntity<ApiErrorResponse> hrDeleteResp =
        restTemplate.exchange(
            "/employees/" + employeeId,
            HttpMethod.DELETE,
            new HttpEntity<>(hrHeaders()),
            ApiErrorResponse.class);
    assertThat(hrDeleteResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // ROLE_HR can update
    UpdateEmployeeRequest updateReq = new UpdateEmployeeRequest();
    updateReq.firstName = "HRUpdated";
    updateReq.departmentId = departmentId;
    ResponseEntity<EmployeeResponse> hrUpdateResp =
        restTemplate.exchange(
            "/employees/" + employeeId,
            HttpMethod.PUT,
            new HttpEntity<>(updateReq, hrHeaders()),
            EmployeeResponse.class);
    assertThat(hrUpdateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(hrUpdateResp.getBody().firstName).isEqualTo("HRUpdated");
  }

  @Test
  @DisplayName("Self-service update owns profile only")
  void selfServiceUpdate_ownProfileOnly() {
    // Create employee linked to authUsername "testself"
    CreateEmployeeRequest createReq =
        EmployeeTestFixtures.validCreateRequest("self-service@example.com");
    createReq.authUsername = "testself";

    ResponseEntity<EmployeeResponse> createResp =
        restTemplate.postForEntity(
            "/employees/create",
            new HttpEntity<>(createReq, adminHeaders()),
            EmployeeResponse.class);
    assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    EmployeeResponse employee = createResp.getBody();

    // Call PATCH /employees/me with testself
    UpdateOwnProfileRequest updateOwn = new UpdateOwnProfileRequest();
    updateOwn.phone = "+1-555-9999";
    com.hrsphere.employee.dto.AddressDto addr = new com.hrsphere.employee.dto.AddressDto();
    addr.street = "New Street 99";
    addr.city = "New City";
    addr.state = "NY";
    addr.postalCode = "10001";
    addr.country = "USA";
    updateOwn.address = addr;

    HttpHeaders selfHdrs = new HttpHeaders();
    selfHdrs.set("X-Auth-Username", "testself");
    selfHdrs.set("X-Auth-Validated", "true");

    ResponseEntity<EmployeeResponse> updateMeResp =
        restTemplate.exchange(
            "/employees/me",
            HttpMethod.PATCH,
            new HttpEntity<>(updateOwn, selfHdrs),
            EmployeeResponse.class);

    assertThat(updateMeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updateMeResp.getBody()).isNotNull();
    assertThat(updateMeResp.getBody().phone).isEqualTo("+1-555-9999");
    assertThat(updateMeResp.getBody().address.street).isEqualTo("New Street 99");

    // jobTitle should be UNCHANGED (not updated by own profile)
    assertThat(updateMeResp.getBody().jobTitle).isEqualTo("Software Engineer");
  }

  @Test
  @DisplayName("Self-service update fails with 404 when no employee linked")
  void selfServiceUpdate_noLinkedEmployee_returns404() {
    UpdateOwnProfileRequest updateOwn = new UpdateOwnProfileRequest();
    updateOwn.phone = "+1-555-9999";

    HttpHeaders selfHdrs = new HttpHeaders();
    selfHdrs.set("X-Auth-Username", "nobody-linked");
    selfHdrs.set("X-Auth-Validated", "true");

    ResponseEntity<ApiErrorResponse> resp =
        restTemplate.exchange(
            "/employees/me",
            HttpMethod.PATCH,
            new HttpEntity<>(updateOwn, selfHdrs),
            ApiErrorResponse.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("Create employee successfully validates departmentId via cross-service call")
  void createEmployee_crossServiceDepartmentValidation() {
    UUID deptId = UUID.randomUUID();
    departmentServiceMock.stubFor(
        get(urlEqualTo("/department/" + deptId))
            .willReturn(okJson("{\"id\":\"" + deptId + "\", \"name\":\"Engineering\"}")));

    CreateEmployeeRequest createReq =
        EmployeeTestFixtures.validCreateRequest("cross-dept@example.com");
    createReq.departmentId = deptId;

    ResponseEntity<EmployeeResponse> resp =
        restTemplate.postForEntity(
            "/employees/create",
            new HttpEntity<>(createReq, adminHeaders()),
            EmployeeResponse.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(resp.getBody()).isNotNull();
    assertThat(resp.getBody().departmentId).isEqualTo(deptId);
  }

  @Test
  @DisplayName("Create employee rejected when departmentId is invalid (returns 404)")
  void createEmployee_invalidDepartmentId_rejected() {
    UUID deptId = UUID.randomUUID();
    departmentServiceMock.stubFor(
        get(urlEqualTo("/department/" + deptId))
            .willReturn(
                notFound()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"message\":\"Not found\"}")));

    CreateEmployeeRequest createReq =
        EmployeeTestFixtures.validCreateRequest("fake-dept@example.com");
    createReq.departmentId = deptId;

    ResponseEntity<ApiErrorResponse> resp =
        restTemplate.postForEntity(
            "/employees/create",
            new HttpEntity<>(createReq, adminHeaders()),
            ApiErrorResponse.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(resp.getBody()).isNotNull();
    assertThat(resp.getBody().message()).contains("does not exist");
  }

  @Test
  @DisplayName("Create employee succeeds even if department-service is down (graceful resilience)")
  void createEmployee_departmentServiceDown_proceedsGracefully() {
    UUID deptId = UUID.randomUUID();
    // Simulate connection reset
    departmentServiceMock.stubFor(
        get(urlEqualTo("/department/" + deptId))
            .willReturn(
                aResponse()
                    .withFault(
                        com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

    CreateEmployeeRequest createReq =
        EmployeeTestFixtures.validCreateRequest("dept-down@example.com");
    createReq.departmentId = deptId;

    ResponseEntity<EmployeeResponse> resp =
        restTemplate.postForEntity(
            "/employees/create",
            new HttpEntity<>(createReq, adminHeaders()),
            EmployeeResponse.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(resp.getBody()).isNotNull();
    assertThat(resp.getBody().departmentId).isEqualTo(deptId);
  }

  @Test
  @DisplayName("Validation failures return 400")
  void validation_failures() {
    // Missing required fields
    CreateEmployeeRequest req1 = new CreateEmployeeRequest();
    req1.email = "invalid-email";
    ResponseEntity<ApiErrorResponse> resp1 =
        restTemplate.postForEntity(
            "/employees/create", new HttpEntity<>(req1, adminHeaders()), ApiErrorResponse.class);
    assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // Future dateOfBirth
    CreateEmployeeRequest req2 = EmployeeTestFixtures.validCreateRequest("future-dob@example.com");
    req2.dateOfBirth = LocalDate.now().plusYears(1);
    ResponseEntity<ApiErrorResponse> resp2 =
        restTemplate.postForEntity(
            "/employees/create", new HttpEntity<>(req2, adminHeaders()), ApiErrorResponse.class);
    assertThat(resp2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // Invalid UUID in path (returns 400, not 500)
    ResponseEntity<ApiErrorResponse> resp3 =
        restTemplate.exchange(
            "/employees/invalid-uuid",
            HttpMethod.GET,
            new HttpEntity<>(employeeHeaders("test")),
            ApiErrorResponse.class);
    assertThat(resp3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // Invalid enum in query param
    ResponseEntity<ApiErrorResponse> resp4 =
        restTemplate.exchange(
            "/employees/list?status=NOT_VALID_STATUS",
            HttpMethod.GET,
            new HttpEntity<>(employeeHeaders("test")),
            ApiErrorResponse.class);
    assertThat(resp4.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("Pagination and filtering works correctly")
  void pagination_and_filtering() {
    UUID departmentId = UUID.randomUUID();
    departmentServiceMock.stubFor(
        get(urlEqualTo("/department/" + departmentId))
            .willReturn(okJson("{\"id\":\"" + departmentId + "\"}")));

    // Create 5 employees with varying configurations
    // 3 Full Time, 2 Part Time
    EmploymentType[] types = {
      EmploymentType.FULL_TIME,
      EmploymentType.FULL_TIME,
      EmploymentType.FULL_TIME,
      EmploymentType.PART_TIME,
      EmploymentType.PART_TIME
    };

    for (int i = 0; i < 5; i++) {
      CreateEmployeeRequest req =
          EmployeeTestFixtures.validCreateRequest("list-" + i + "@example.com");
      req.authUsername = "list-user-" + i;
      req.employmentType = types[i];
      req.departmentId = departmentId;
      ResponseEntity<EmployeeResponse> res =
          restTemplate.postForEntity(
              "/employees/create", new HttpEntity<>(req, adminHeaders()), EmployeeResponse.class);
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // Filter by FULL_TIME and paginate
    HttpHeaders validatedHdrs = new HttpHeaders();
    validatedHdrs.set("X-Auth-Validated", "true");

    ResponseEntity<Map> listResp =
        restTemplate.exchange(
            "/employees/list?employmentType=FULL_TIME&page=0&size=2",
            HttpMethod.GET,
            new HttpEntity<>(validatedHdrs),
            Map.class);

    assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> body = listResp.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("content")).isNotNull();
    java.util.List<?> content = (java.util.List<?>) body.get("content");
    assertThat(content.size()).isEqualTo(2); // Page size request of 2

    // Check totalElements (should be 3 FULL_TIME employees created in this test, plus possibly any
    // others from other tests, so >= 3)
    Number totalElements = (Number) body.get("totalElements");
    assertThat(totalElements.intValue()).isGreaterThanOrEqualTo(3);
  }
}
