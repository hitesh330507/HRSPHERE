package com.hrsphere.apigateway.controller;

import com.hrsphere.common.dto.ApiErrorResponse;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

  @RequestMapping("/fallback/{service}")
  public Mono<ResponseEntity<ApiErrorResponse>> fallback(@PathVariable String service) {
    ApiErrorResponse error =
        new ApiErrorResponse(
            Instant.now(),
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
            String.format("%s is currently unavailable. Please try again shortly.", service),
            "/fallback/" + service);
    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error));
  }
}
