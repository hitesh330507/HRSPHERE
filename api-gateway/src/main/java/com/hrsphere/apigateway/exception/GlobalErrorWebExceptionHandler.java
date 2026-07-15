package com.hrsphere.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrsphere.common.dto.ApiErrorResponse;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-1)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);
  private final ObjectMapper objectMapper;

  public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
    log.error("Gateway exception handled:", error);
    if (exchange.getResponse().isCommitted()) {
      return Mono.error(error);
    }

    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    String message = "An unexpected gateway error occurred";

    if (error instanceof ResponseStatusException responseStatusException) {
      status = HttpStatus.resolve(responseStatusException.getStatusCode().value());
      if (status == null) {
        status = HttpStatus.INTERNAL_SERVER_ERROR;
      }
      if (status == HttpStatus.TOO_MANY_REQUESTS && responseStatusException.getReason() == null) {
        message = "Rate limit exceeded";
      } else {
        message =
            responseStatusException.getReason() != null
                ? responseStatusException.getReason()
                : responseStatusException.getMessage();
      }
    } else if (error instanceof GatewayAuthException gatewayAuthException) {
      status = HttpStatus.UNAUTHORIZED;
      message = gatewayAuthException.getMessage();
    }

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
}
