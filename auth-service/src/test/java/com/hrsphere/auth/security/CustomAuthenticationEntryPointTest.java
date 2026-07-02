package com.hrsphere.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hrsphere.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

class CustomAuthenticationEntryPointTest {

  private final CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();

  @Test
  void commence_shouldWriteJsonApiErrorResponse() throws IOException {
    HttpServletRequest request = new MockHttpServletRequest("GET", "/auth/admin/users");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, new AuthenticationException("Unauthenticated") {});

    assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(response.getContentType()).contains("application/json");

    ApiErrorResponse body =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .readValue(response.getContentAsByteArray(), ApiErrorResponse.class);
    assertThat(body.status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(body.message()).isEqualTo("Authentication required.");
  }
}
