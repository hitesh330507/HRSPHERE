package com.hrsphere.department.config;

import static io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP;

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
  public OpenAPI departmentOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("HRSphere — Department Service API")
                .version("v1.0.0")
                .description(
                    "Department management. Cross-service employee count aggregation via employee-service API call."))
        .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "BearerAuth",
                    new SecurityScheme().type(HTTP).scheme("bearer").bearerFormat("JWT")));
  }
}
