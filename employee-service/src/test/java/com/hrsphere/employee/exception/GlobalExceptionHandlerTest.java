package com.hrsphere.employee.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrsphere.common.dto.ApiErrorResponse;
import com.hrsphere.common.exception.AccessForbiddenException;
import com.hrsphere.common.exception.ResourceAlreadyExistsException;
import com.hrsphere.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void resourceNotFoundException_shouldReturnNotFound() {
    HttpServletRequest request = new MockHttpServletRequest("GET", "/employees/123");
    ResponseEntity<ApiErrorResponse> response =
        handler.handleResourceNotFound(new ResourceNotFoundException("Not found"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Not found");
  }

  @Test
  void resourceAlreadyExistsException_shouldReturnConflict() {
    HttpServletRequest request = new MockHttpServletRequest("POST", "/employees");
    ResponseEntity<ApiErrorResponse> response =
        handler.handleResourceAlreadyExists(
            new ResourceAlreadyExistsException("Already exists"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Already exists");
  }

  @Test
  void accessForbiddenException_shouldReturnForbidden() {
    HttpServletRequest request = new MockHttpServletRequest("POST", "/employees");
    ResponseEntity<ApiErrorResponse> response =
        handler.handleAccessForbidden(new AccessForbiddenException("Forbidden"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Forbidden");
  }

  @Test
  void generalException_shouldReturnInternalServerError() {
    HttpServletRequest request = new MockHttpServletRequest("GET", "/employees");
    ResponseEntity<ApiErrorResponse> response =
        handler.handleUnexpectedException(new RuntimeException("Oops"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred.");
  }
}
