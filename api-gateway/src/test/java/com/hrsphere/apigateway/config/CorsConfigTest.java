package com.hrsphere.apigateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.reactive.CorsWebFilter;

class CorsConfigTest {

  private final CorsConfig corsConfig = new CorsConfig();

  @Test
  void corsWebFilterBeanIsCreated() {
    CorsWebFilter filter = corsConfig.corsWebFilter();
    assertThat(filter).isNotNull();
  }
}
