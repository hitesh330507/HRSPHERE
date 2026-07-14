package com.hrsphere.leave.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

  @Autowired protected TestRestTemplate restTemplate;

  @Autowired protected ObjectMapper objectMapper;

  @Container
  protected static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("leave_db_test")
          .withUsername("test")
          .withPassword("test");

  @Container
  protected static GenericContainer<?> redis =
      new GenericContainer<>("redis:7-alpine")
          .withExposedPorts(6379)
          .withCommand("redis-server --requirepass testpass");

  protected static WireMockServer employeeServiceMock;

  @BeforeAll
  static void startWireMock() {
    employeeServiceMock = new WireMockServer(0); // random port
    employeeServiceMock.start();
  }

  @AfterAll
  static void stopWireMock() {
    if (employeeServiceMock != null) {
      employeeServiceMock.stop();
    }
  }

  @BeforeEach
  void resetWireMockAndRequestFactory() {
    if (employeeServiceMock != null) {
      employeeServiceMock.resetAll();
    }
    if (restTemplate != null && restTemplate.getRestTemplate() != null) {
      restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.url", postgres::getJdbcUrl);
    registry.add("spring.flyway.user", postgres::getUsername);
    registry.add("spring.flyway.password", postgres::getPassword);

    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    registry.add("spring.data.redis.password", () -> "testpass");

    registry.add(
        "employee-service.base-url", () -> "http://localhost:" + employeeServiceMock.port());
  }

  protected HttpHeaders adminHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Auth-Username", "admin");
    headers.set("X-Auth-Roles", "ROLE_ADMIN");
    headers.set("X-Auth-Validated", "true");
    return headers;
  }

  protected HttpHeaders hrHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Auth-Username", "hruser");
    headers.set("X-Auth-Roles", "ROLE_HR");
    headers.set("X-Auth-Validated", "true");
    return headers;
  }

  protected HttpHeaders employeeHeaders(String username) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Auth-Username", username);
    headers.set("X-Auth-Roles", "ROLE_EMPLOYEE");
    headers.set("X-Auth-Validated", "true");
    return headers;
  }

  protected void stubEmployeeLookup(
      String authUsername, UUID employeeId, String firstName, String lastName) {
    try {
      employeeServiceMock.stubFor(
          get(urlPathEqualTo("/employees/lookup"))
              .withQueryParam("authUsername", equalTo(authUsername))
              .willReturn(
                  okJson(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "employeeId", employeeId.toString(),
                              "firstName", firstName,
                              "lastName", lastName)))));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected void stubEmployeeGet(UUID employeeId, String firstName, String lastName) {
    try {
      employeeServiceMock.stubFor(
          get(urlPathEqualTo("/employees/" + employeeId))
              .willReturn(
                  okJson(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "id",
                              employeeId.toString(),
                              "firstName",
                              firstName,
                              "lastName",
                              lastName,
                              "email",
                              "testemployee@example.com")))));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
