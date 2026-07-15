package com.hrsphere.apigateway.filter;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  public static final String CORRELATION_ID_KEY = "correlationId";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
    if (correlationId == null || correlationId.trim().isEmpty()) {
      correlationId = UUID.randomUUID().toString();
    }

    // Mutate request to include the correlation ID for downstream services
    ServerHttpRequest mutatedRequest =
        exchange.getRequest().mutate().header(CORRELATION_ID_HEADER, correlationId).build();

    // Set correlation ID on response headers if not already set
    if (!exchange.getResponse().getHeaders().containsKey(CORRELATION_ID_HEADER)) {
      exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
    }

    // Save in exchange attributes for logging/other filters
    exchange.getAttributes().put(CORRELATION_ID_KEY, correlationId);

    return chain.filter(exchange.mutate().request(mutatedRequest).build());
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
