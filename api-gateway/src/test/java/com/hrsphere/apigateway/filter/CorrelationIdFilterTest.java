package com.hrsphere.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void generatesUuidWhenHeaderAbsent() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/me").build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    AtomicReference<ServerHttpRequest> capturedRequest = new AtomicReference<>();
    GatewayFilterChain chain =
        e -> {
          capturedRequest.set(e.getRequest());
          return Mono.empty();
        };

    filter.filter(exchange, chain).block();

    String correlationId =
        capturedRequest.get().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
    assertThat(correlationId).isNotNull();
    assertThat(UUID.fromString(correlationId)).isNotNull();

    String responseCorrelationId =
        exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
    assertThat(responseCorrelationId).isEqualTo(correlationId);

    String attributeCorrelationId = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
    assertThat(attributeCorrelationId).isEqualTo(correlationId);
  }

  @Test
  void preservesExistingHeaderWhenPresent() {
    String existingId = "existing-correlation-id-123";
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/v1/auth/me")
            .header(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId)
            .build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    AtomicReference<ServerHttpRequest> capturedRequest = new AtomicReference<>();
    GatewayFilterChain chain =
        e -> {
          capturedRequest.set(e.getRequest());
          return Mono.empty();
        };

    filter.filter(exchange, chain).block();

    String correlationId =
        capturedRequest.get().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
    assertThat(correlationId).isEqualTo(existingId);

    String responseCorrelationId =
        exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
    assertThat(responseCorrelationId).isEqualTo(existingId);

    String attributeCorrelationId = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
    assertThat(attributeCorrelationId).isEqualTo(existingId);
  }
}
