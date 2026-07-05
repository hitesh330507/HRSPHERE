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
            "auth-docs-route",
            route ->
                route
                    .path(
                        "/api/v1/auth/api-docs/**",
                        "/api/v1/auth/swagger-ui.html",
                        "/api/v1/auth/swagger-ui/**")
                    .filters(
                        filter ->
                            filter
                                .rewritePath("/api/v1/auth/(?<remaining>.*)", "/${remaining}")
                                .addRequestHeader("X-Forwarded-Prefix", "/api/v1/auth"))
                    .uri("http://auth-service:8081"))
        .route(
            "auth-service-route",
            route ->
                route
                    .path("/api/v1/auth/**")
                    .filters(filter -> filter.stripPrefix(2))
                    .uri("http://auth-service:8081"))
        .route(
            "employee-docs-route",
            route ->
                route
                    .path(
                        "/api/v1/employees/api-docs/**",
                        "/api/v1/employees/swagger-ui.html",
                        "/api/v1/employees/swagger-ui/**")
                    .filters(
                        filter ->
                            filter
                                .rewritePath("/api/v1/employees/(?<remaining>.*)", "/${remaining}")
                                .addRequestHeader("X-Forwarded-Prefix", "/api/v1/employees"))
                    .uri("http://employee-service:8082"))
        .route(
            "employee-service-route",
            route ->
                route
                    .path("/api/v1/employees/**")
                    .filters(filter -> filter.stripPrefix(2))
                    .uri("http://employee-service:8082"))
        .build();
  }
}
