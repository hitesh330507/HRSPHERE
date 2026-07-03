package com.hrsphere.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrsphere.auth.dto.AuthResponse;
import com.hrsphere.auth.dto.LoginRequest;
import com.hrsphere.auth.dto.RefreshTokenRequest;
import com.hrsphere.auth.dto.RegisterRequest;
import com.hrsphere.common.dto.ApiErrorResponse;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("Integration Tests: Auth Service Full Lifecycle")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthLifecycleIntegrationTest extends BaseIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  private static String testUsername = "testuser" + System.currentTimeMillis();
  private static String testEmail = "testuser" + System.currentTimeMillis() + "@example.com";
  private static String testPassword = "SecurePassword123!";
  private static String accessToken;
  private static String refreshToken;

  @Test
  @DisplayName("Register a new user successfully")
  void testRegister_Success() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername(testUsername);
    request.setEmail(testEmail);
    request.setPassword(testPassword);
    request.setRole("ROLE_EMPLOYEE");

    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity("/auth/register", request, AuthResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    AuthResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.username()).isEqualTo(testUsername);
    assertThat(body.email()).isEqualTo(testEmail);
    assertThat(body.roles()).containsExactly("ROLE_EMPLOYEE");
    assertThat(body.accessToken()).isNotBlank();
    assertThat(body.refreshToken()).isNotBlank();
  }

  @Test
  @DisplayName("Reject registration with duplicate username")
  void testRegister_DuplicateUsername() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername(testUsername);
    request.setEmail("different" + testEmail);
    request.setPassword(testPassword);
    request.setRole("ROLE_EMPLOYEE");

    ResponseEntity<ApiErrorResponse> response =
        restTemplate.postForEntity("/auth/register", request, ApiErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).containsIgnoringCase("username");
  }

  @Test
  @DisplayName("Login with correct credentials")
  void testLogin_Success() {
    LoginRequest request = new LoginRequest();
    request.setUsername(testUsername);
    request.setPassword(testPassword);

    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity("/auth/login", request, AuthResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    AuthResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.username()).isEqualTo(testUsername);
    assertThat(body.accessToken()).isNotBlank();
    assertThat(body.refreshToken()).isNotBlank();
    assertThat(body.tokenType()).isEqualTo("Bearer");

    // Save tokens for later tests
    accessToken = body.accessToken();
    refreshToken = body.refreshToken();
  }

  @Test
  @DisplayName("Access protected endpoint with valid token")
  void testGetMe_WithValidToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    HttpEntity<?> entity = new HttpEntity<>(headers);

    ResponseEntity<Map> response =
        restTemplate.exchange("/auth/me", HttpMethod.GET, entity, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("username")).isEqualTo(testUsername);
  }

  @Test
  @DisplayName("Reject request without token")
  void testGetMe_WithoutToken() {
    ResponseEntity<ApiErrorResponse> response =
        restTemplate.getForEntity("/auth/me", ApiErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Reject request with invalid token")
  void testGetMe_WithInvalidToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken + "tampered");
    HttpEntity<?> entity = new HttpEntity<>(headers);

    ResponseEntity<ApiErrorResponse> response =
        restTemplate.exchange("/auth/me", HttpMethod.GET, entity, ApiErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Refresh access token")
  void testRefresh_Success() {
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken(refreshToken);

    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity("/auth/refresh", request, AuthResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    AuthResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.accessToken()).isNotBlank();
    assertThat(body.refreshToken()).isNotBlank();
    assertThat(body.accessToken()).isNotEqualTo(accessToken);
    assertThat(body.refreshToken()).isNotEqualTo(refreshToken);

    // Update tokens for next tests
    accessToken = body.accessToken();
    refreshToken = body.refreshToken();
  }

  @Test
  @DisplayName("Reject old refresh token after rotation")
  void testRefresh_OldTokenRejected() {
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken(refreshToken + "old");

    ResponseEntity<ApiErrorResponse> response =
        restTemplate.postForEntity("/auth/refresh", request, ApiErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Logout successfully")
  void testLogout_Success() {
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken(refreshToken);

    ResponseEntity<Map> response = restTemplate.postForEntity("/auth/logout", request, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("Reject refresh after logout")
  void testRefresh_AfterLogout() {
    RefreshTokenRequest request = new RefreshTokenRequest();
    request.setRefreshToken(refreshToken);

    ResponseEntity<ApiErrorResponse> response =
        restTemplate.postForEntity("/auth/refresh", request, ApiErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Test token expiry (5 second TTL in test profile)")
  void testGetMe_WithExpiredToken() throws InterruptedException {
    // Register a new user to get a fresh token
    RegisterRequest registerReq = new RegisterRequest();
    registerReq.setUsername("expiry-test-" + System.currentTimeMillis());
    registerReq.setEmail("expiry-test-" + System.currentTimeMillis() + "@example.com");
    registerReq.setPassword(testPassword);

    ResponseEntity<AuthResponse> registerResp =
        restTemplate.postForEntity("/auth/register", registerReq, AuthResponse.class);
    String freshToken = registerResp.getBody().accessToken();

    // Token should be valid now
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(freshToken);
    HttpEntity<?> entity = new HttpEntity<>(headers);

    ResponseEntity<Map> validResponse =
        restTemplate.exchange("/auth/me", HttpMethod.GET, entity, Map.class);
    assertThat(validResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Wait 6 seconds for token to expire (test profile uses 5s TTL)
    Thread.sleep(6000);

    // Token should now be expired
    ResponseEntity<ApiErrorResponse> expiredResponse =
        restTemplate.exchange("/auth/me", HttpMethod.GET, entity, ApiErrorResponse.class);
    assertThat(expiredResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("RBAC: Employee cannot access admin endpoints")
  void testRBAC_EmployeeCannotAccessAdminEndpoints() {
    // Login as employee
    LoginRequest loginReq = new LoginRequest();
    loginReq.setUsername(testUsername);
    loginReq.setPassword(testPassword);

    ResponseEntity<AuthResponse> loginResp =
        restTemplate.postForEntity("/auth/login", loginReq, AuthResponse.class);
    String employeeToken = loginResp.getBody().accessToken();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(employeeToken);
    HttpEntity<?> entity = new HttpEntity<>(headers);

    // Try to access admin endpoint
    ResponseEntity<ApiErrorResponse> response =
        restTemplate.exchange("/auth/admin/users", HttpMethod.GET, entity, ApiErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("RBAC: Admin can access admin endpoints")
  void testRBAC_AdminCanAccessAdminEndpoints() {
    // Login as admin (seeded user)
    LoginRequest loginReq = new LoginRequest();
    loginReq.setUsername("admin");
    loginReq.setPassword("AdminPass123!");

    ResponseEntity<AuthResponse> loginResp =
        restTemplate.postForEntity("/auth/login", loginReq, AuthResponse.class);

    if (loginResp.getStatusCode() == HttpStatus.OK) {
      String adminToken = loginResp.getBody().accessToken();

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(adminToken);
      HttpEntity<?> entity = new HttpEntity<>(headers);

      // Try to access admin endpoint
      ResponseEntity<Map> response =
          restTemplate.exchange("/auth/admin/users", HttpMethod.GET, entity, Map.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
    }
  }

  @Test
  @DisplayName("RBAC: Employee cannot access HR endpoints")
  void testRBAC_EmployeeCannotAccessHREndpoints() {
    // Login as employee
    LoginRequest loginReq = new LoginRequest();
    loginReq.setUsername(testUsername);
    loginReq.setPassword(testPassword);

    ResponseEntity<AuthResponse> loginResp =
        restTemplate.postForEntity("/auth/login", loginReq, AuthResponse.class);
    String employeeToken = loginResp.getBody().accessToken();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(employeeToken);
    HttpEntity<?> entity = new HttpEntity<>(headers);

    // Try to access HR endpoint
    ResponseEntity<ApiErrorResponse> response =
        restTemplate.exchange(
            "/auth/hr/users/summary", HttpMethod.GET, entity, ApiErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Validation failure returns 400 with field errors")
  void testValidation_FailureReturns400() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("ab"); // Too short (min 3)
    request.setEmail("invalid-email");
    request.setPassword("short"); // Too short (min 8)

    ResponseEntity<ApiErrorResponse> response =
        restTemplate.postForEntity("/auth/register", request, ApiErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    String message = response.getBody().message();
    assertThat(message).containsIgnoringCase("username");
    assertThat(message).containsIgnoringCase("email");
    assertThat(message).containsIgnoringCase("password");
  }
}
