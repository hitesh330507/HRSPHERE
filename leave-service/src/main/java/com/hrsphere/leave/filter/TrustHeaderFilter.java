package com.hrsphere.leave.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hrsphere.common.dto.ApiErrorResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TrustHeaderFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(TrustHeaderFilter.class);

  @Value("${gateway.trust-headers-required:false}")
  private boolean trustHeadersRequired;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String path = httpRequest.getRequestURI();

    // Skip Swagger and Actuator endpoints
    if (path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/api-docs")
        || path.startsWith("/actuator")) {
      chain.doFilter(request, response);
      return;
    }

    String validatedHeader = httpRequest.getHeader("X-Auth-Validated");
    String usernameHeader = httpRequest.getHeader("X-Auth-Username");

    if (validatedHeader == null) {
      if (trustHeadersRequired) {
        log.warn("Direct request to {} blocked due to missing X-Auth-Validated header", path);
        ApiErrorResponse errorResp =
            new ApiErrorResponse(
                Instant.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Direct calls to this service are disabled. Request must be routed through the API Gateway.",
                path);
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.setContentType("application/json");
        httpResponse.getWriter().write(objectMapper.writeValueAsString(errorResp));
        return;
      } else {
        log.warn(
            "X-Auth-Validated header is missing. Direct call bypassing gateway detected for path: {}",
            path);
      }
    }

    if (usernameHeader == null) {
      log.warn(
          "X-Auth-Username header is missing. Direct call bypassing gateway detected for path: {}",
          path);
    }

    chain.doFilter(request, response);
  }
}
