package com.hrsphere.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

  @Bean
  public RouteLocator routeLocator(RouteLocatorBuilder builder) {
    return builder
        .routes()
        .route(
            "auth-service-route",
            route ->
                route.path("/api/v1/auth/**")
                    .filters(filter -> filter.stripPrefix(2))
                    .uri("http://auth-service:8081"))
        .route(
            "employee-service-route",
            route ->
                route.path("/api/v1/employee/**")
                    .filters(filter -> filter.stripPrefix(3))
                    .uri("http://employee-service:8082"))
        .build();
  }
}
