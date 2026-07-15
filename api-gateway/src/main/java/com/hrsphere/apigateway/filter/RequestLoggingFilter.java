package com.hrsphere.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    long startTime = System.currentTimeMillis();
    ServerHttpRequest request = exchange.getRequest();

    return chain
        .filter(exchange)
        .then(
            Mono.fromRunnable(
                () -> {
                  long duration = System.currentTimeMillis() - startTime;
                  ServerHttpResponse response = exchange.getResponse();
                  String correlationId =
                      exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);

                  log.info(
                      "correlationId={} method={} path={} status={} durationMs={}",
                      correlationId != null ? correlationId : "unknown",
                      request.getMethod(),
                      request.getPath(),
                      response.getStatusCode() != null
                          ? response.getStatusCode().value()
                          : "unknown",
                      duration);
                }));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }
}
