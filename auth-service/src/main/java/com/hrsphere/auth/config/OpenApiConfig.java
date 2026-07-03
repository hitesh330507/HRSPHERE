package com.hrsphere.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI hrsphereAuthOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("HRSphere — Auth Service API")
                .description(
                    "Authentication, authorisation, and user management for HRSphere HR Management System")
                .version("v1.0.0"))
        .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "BearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "Paste your JWT access token here. Obtain one from POST /auth/login")));
  }
}
