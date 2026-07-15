package com.hrsphere.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hrsphere.common.dto.ApiErrorResponse;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

class RateLimitResponseFilterTest {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private final RateLimitResponseFilter filter = new RateLimitResponseFilter(objectMapper);

  @Test
  void writesApiErrorResponseWhenStatusIs429() throws IOException {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    WebFilterChain chain =
        e -> {
          e.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
          return e.getResponse().setComplete();
        };

    filter.filter(exchange, chain).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(exchange.getResponse().getHeaders().getContentType())
        .isEqualTo(MediaType.APPLICATION_JSON);

    MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
    byte[] bodyBytes =
        response
            .getBody()
            .map(
                buffer -> {
                  byte[] bytes = new byte[buffer.readableByteCount()];
                  buffer.read(bytes);
                  DataBufferUtils.release(buffer);
                  return bytes;
                })
            .blockFirst();

    assertThat(bodyBytes).isNotNull();
    ApiErrorResponse apiResponse = objectMapper.readValue(bodyBytes, ApiErrorResponse.class);
    assertThat(apiResponse.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    assertThat(apiResponse.error()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
    assertThat(apiResponse.message()).isEqualTo("Rate limit exceeded");
    assertThat(apiResponse.path()).isEqualTo("/api/v1/auth/login");
  }

  @Test
  void doesNothingWhenStatusIsNot429() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    WebFilterChain chain =
        e -> {
          e.getResponse().setStatusCode(HttpStatus.OK);
          return e.getResponse().setComplete();
        };

    filter.filter(exchange, chain).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
    MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
    reactor.test.StepVerifier.create(response.getBody()).verifyComplete();
  }
}
