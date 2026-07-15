package com.hrsphere.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class RequestLoggingFilterTest {

  private final RequestLoggingFilter filter = new RequestLoggingFilter();

  @Test
  void requestLoggingFilterLogsAndDelegates() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/me").build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);
    exchange.getAttributes().put(CorrelationIdFilter.CORRELATION_ID_KEY, "test-correlation-id");

    AtomicBoolean chainCalled = new AtomicBoolean(false);
    GatewayFilterChain chain =
        e -> {
          chainCalled.set(true);
          return Mono.empty();
        };

    filter.filter(exchange, chain).block();

    assertThat(chainCalled.get()).isTrue();
  }
}
