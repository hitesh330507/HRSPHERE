package com.hrsphere.auth.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrsphere.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void accessDeniedException_shouldReturnForbiddenApiErrorResponse() {
    HttpServletRequest request = new MockHttpServletRequest("GET", "/auth/admin/users");
    request.setAttribute("jakarta.servlet.forward.request_uri", "/auth/admin/users");

    ResponseEntity<ApiErrorResponse> response =
        handler.handleAccessDenied(new AccessDeniedException("Forbidden"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Access denied. Insufficient permissions.");
    assertThat(response.getBody().status()).isEqualTo(HttpStatus.FORBIDDEN.value());
  }
}
