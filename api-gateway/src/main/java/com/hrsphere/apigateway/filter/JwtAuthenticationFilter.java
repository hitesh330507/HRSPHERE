package com.hrsphere.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrsphere.apigateway.config.GatewayProperties;
import com.hrsphere.apigateway.exception.GatewayAuthException;
import com.hrsphere.apigateway.service.GatewayJwtService;
import com.hrsphere.common.dto.ApiErrorResponse;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  private final GatewayJwtService jwtService;
  private final GatewayProperties gatewayProperties;
  private final ObjectMapper objectMapper;

  public JwtAuthenticationFilter(
      GatewayJwtService jwtService,
      GatewayProperties gatewayProperties,
      ObjectMapper objectMapper) {
    this.jwtService = jwtService;
    this.gatewayProperties = gatewayProperties;
    this.objectMapper = objectMapper;
    log.info("JwtAuthenticationFilter registered and ready to validate gateway JWTs");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest sanitizedRequest =
        exchange
            .getRequest()
            .mutate()
            .headers(
                headers -> {
                  headers.remove("X-Auth-Username");
                  headers.remove("X-Auth-Roles");
                  headers.remove("X-Auth-Validated");
                })
            .build();
    exchange = exchange.mutate().request(sanitizedRequest).build();

    String path = exchange.getRequest().getPath().value();
    if (isPublicPath(path)) {
      return chain.filter(exchange);
    }

    try {
      List<String> authHeaders = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
      if (authHeaders == null || authHeaders.isEmpty()) {
        return writeErrorResponse(
            exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
      }

      String authorization = authHeaders.get(0);
      if (!authorization.startsWith("Bearer ")) {
        return writeErrorResponse(
            exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
      }

      String token = authorization.substring("Bearer ".length()).trim();
      if (!jwtService.validateToken(token)) {
        if (jwtService.isTokenExpired(token)) {
          return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "JWT token is expired");
        }
        return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "JWT token is invalid");
      }

      String username = jwtService.extractUsername(token);
      List<String> roles = jwtService.extractRoles(token);
      ServerHttpRequest mutatedRequest =
          exchange
              .getRequest()
              .mutate()
              .header("X-Auth-Username", username)
              .header("X-Auth-Roles", String.join(",", roles))
              .header("X-Auth-Validated", "true")
              .build();
      return chain.filter(exchange.mutate().request(mutatedRequest).build());
    } catch (GatewayAuthException exception) {
      return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, exception.getMessage());
    } catch (Exception exception) {
      return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "JWT token is invalid");
    }
  }

  private boolean isPublicPath(String path) {
    return gatewayProperties.getPublicPaths().stream()
        .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
  }

  private Mono<Void> writeErrorResponse(
      ServerWebExchange exchange, HttpStatus status, String message) {
    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    ApiErrorResponse body =
        new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            exchange.getRequest().getPath().value());
    try {
      byte[] bytes = objectMapper.writeValueAsBytes(body);
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (JsonProcessingException exception) {
      return exchange.getResponse().setComplete();
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
