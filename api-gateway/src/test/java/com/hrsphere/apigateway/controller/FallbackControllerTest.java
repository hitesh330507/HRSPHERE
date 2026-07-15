package com.hrsphere.apigateway.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrsphere.common.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class FallbackControllerTest {

  private final FallbackController controller = new FallbackController();

  @Test
  void returns503WithApiErrorResponse() {
    ResponseEntity<ApiErrorResponse> response = controller.fallback("employee-service").block();

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    assertThat(response.getBody().error())
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
    assertThat(response.getBody().message()).contains("employee-service is currently unavailable");
    assertThat(response.getBody().path()).isEqualTo("/fallback/employee-service");
  }
}
