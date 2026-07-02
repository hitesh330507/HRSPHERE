package com.hrsphere.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrsphere.apigateway.config.GatewayProperties;
import com.hrsphere.apigateway.config.JwtProperties;
import com.hrsphere.apigateway.service.GatewayJwtService;
import com.hrsphere.common.dto.ApiErrorResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class JwtAuthenticationFilterTest {

  private JwtAuthenticationFilter filter;
  private SecretKey signingKey;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    JwtProperties jwtProperties = new JwtProperties();
    jwtProperties.setSecret("c29tZS1zZWN1cmUtY2hhbm5lbC1zZWNyZXQta2V5LTEyMzQ1Njc4OTA=");
    jwtProperties.setAccessTokenExpiryMs(900_000L);
    GatewayJwtService jwtService = new GatewayJwtService(jwtProperties);
    GatewayProperties gatewayProperties = new GatewayProperties();
    gatewayProperties.setPublicPaths(
        List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/*/actuator/**"));
    objectMapper =
        new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    filter = new JwtAuthenticationFilter(jwtService, gatewayProperties, objectMapper);
    signingKey =
        Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(jwtProperties.getSecret()));
  }

  @Test
  void publicPathBypassesFilter() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/register").build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    GatewayFilterChain chain =
        e -> {
          chainCalled.set(true);
          return Mono.empty();
        };

    filter.filter(exchange, chain).block();

    assertThat(chainCalled).isTrue();
  }

  @Test
  void missingAuthorizationHeaderProduces401() throws Exception {
    MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/me").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    GatewayFilterChain chain = e -> Mono.empty();

    filter.filter(exchange, chain).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    byte[] bodyBytes =
        DataBufferUtils.join(exchange.getResponse().getBody())
            .map(
                dataBuffer -> {
                  try {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    return bytes;
                  } finally {
                    DataBufferUtils.release(dataBuffer);
                  }
                })
            .block();
    assertThat(bodyBytes).isNotNull();
    ApiErrorResponse response = objectMapper.readValue(bodyBytes, ApiErrorResponse.class);
    assertThat(response.message()).contains("Missing or invalid Authorization header");
  }

  @Test
  void validTokenSetsDownstreamHeaders() {
    Instant now = Instant.now();
    String token =
        Jwts.builder()
            .subject("charlie")
            .claim("roles", List.of("ROLE_EMPLOYEE", "ROLE_HR"))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(600)))
            .signWith(signingKey)
            .compact();

    MockServerHttpRequest request =
        MockServerHttpRequest.get("/api/v1/auth/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();
    ServerWebExchange exchange = MockServerWebExchange.from(request);
    AtomicReference<ServerHttpRequest> capturedRequest = new AtomicReference<>();
    GatewayFilterChain chain =
        e -> {
          capturedRequest.set(e.getRequest());
          return Mono.empty();
        };

    filter.filter(exchange, chain).block();

    assertThat(capturedRequest.get()).isNotNull();
    assertThat(capturedRequest.get().getHeaders().getFirst("X-Auth-Username")).isEqualTo("charlie");
    assertThat(capturedRequest.get().getHeaders().getFirst("X-Auth-Roles"))
        .isEqualTo("ROLE_EMPLOYEE,ROLE_HR");
    assertThat(capturedRequest.get().getHeaders().getFirst("X-Auth-Validated")).isEqualTo("true");
  }
}
