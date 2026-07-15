package com.hrsphere.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

  @Bean
  @Primary
  public KeyResolver ipKeyResolver() {
    return exchange -> {
      java.net.InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
      if (remoteAddress == null) {
        return Mono.just("unknown");
      }
      java.net.InetAddress address = remoteAddress.getAddress();
      if (address == null) {
        return Mono.just(remoteAddress.getHostString());
      }
      return Mono.just(address.getHostAddress());
    };
  }

  @Bean
  public KeyResolver userKeyResolver() {
    return exchange -> {
      String username = exchange.getRequest().getHeaders().getFirst("X-Auth-Username");
      if (username == null) {
        java.net.InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null) {
          return Mono.just("unknown");
        }
        java.net.InetAddress address = remoteAddress.getAddress();
        if (address == null) {
          return Mono.just(remoteAddress.getHostString());
        }
        return Mono.just(address.getHostAddress());
      }
      return Mono.just(username);
    };
  }
}
