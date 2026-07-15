package com.hrsphere.apigateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

class RateLimiterConfigTest {

  private final RateLimiterConfig config = new RateLimiterConfig();

  @Test
  void ipKeyResolverReturnsIp() {
    KeyResolver resolver = config.ipKeyResolver();
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/v1/auth/login")
            .remoteAddress(new InetSocketAddress("192.168.1.50", 12345))
            .build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    String key = resolver.resolve(exchange).block();
    assertThat(key).isEqualTo("192.168.1.50");
  }

  @Test
  void userKeyResolverReturnsUsernameWhenPresent() {
    KeyResolver resolver = config.userKeyResolver();
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/v1/employees/employees")
            .header("X-Auth-Username", "bob")
            .remoteAddress(new InetSocketAddress("192.168.1.50", 12345))
            .build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    String key = resolver.resolve(exchange).block();
    assertThat(key).isEqualTo("bob");
  }

  @Test
  void userKeyResolverFallsBackToIpWhenUsernameAbsent() {
    KeyResolver resolver = config.userKeyResolver();
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/v1/employees/employees")
            .remoteAddress(new InetSocketAddress("192.168.1.50", 12345))
            .build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);

    String key = resolver.resolve(exchange).block();
    assertThat(key).isEqualTo("192.168.1.50");
  }
}
