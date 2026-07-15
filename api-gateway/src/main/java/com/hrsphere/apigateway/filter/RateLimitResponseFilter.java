package com.hrsphere.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrsphere.common.dto.ApiErrorResponse;
import java.time.Instant;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class RateLimitResponseFilter implements WebFilter {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(RateLimitResponseFilter.class);
  private final ObjectMapper objectMapper;

  public RateLimitResponseFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    org.springframework.http.server.reactive.ServerHttpResponse originalResponse =
        exchange.getResponse();
    org.springframework.http.server.reactive.ServerHttpResponseDecorator decoratedResponse =
        new org.springframework.http.server.reactive.ServerHttpResponseDecorator(originalResponse) {
          @Override
          public Mono<Void> setComplete() {
            var statusCode = getStatusCode();
            if (statusCode != null && statusCode.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
              log.info(
                  "RateLimitResponseFilter: setComplete called with status 429. Writing custom body.");
              getHeaders().setContentType(MediaType.APPLICATION_JSON);
              ApiErrorResponse errorResponse =
                  new ApiErrorResponse(
                      Instant.now(),
                      HttpStatus.TOO_MANY_REQUESTS.value(),
                      HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                      "Rate limit exceeded",
                      exchange.getRequest().getPath().value());
              try {
                byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
                DataBuffer buffer = bufferFactory().wrap(bytes);
                return writeWith(Mono.just(buffer));
              } catch (JsonProcessingException e) {
                log.error("Failed to serialize 429 response", e);
                return Mono.error(e);
              }
            }
            return super.setComplete();
          }
        };

    return chain.filter(exchange.mutate().response(decoratedResponse).build());
  }
}
