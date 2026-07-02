package com.hrsphere.apigateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrsphere.apigateway.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GatewayJwtServiceTest {

  private GatewayJwtService jwtService;
  private SecretKey signingKey;

  @BeforeEach
  void setUp() {
    JwtProperties jwtProperties = new JwtProperties();
    jwtProperties.setSecret("c29tZS1zZWN1cmUtY2hhbm5lbC1zZWNyZXQta2V5LTEyMzQ1Njc4OTA=");
    jwtProperties.setAccessTokenExpiryMs(900_000L);
    jwtService = new GatewayJwtService(jwtProperties);
    signingKey =
        Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(jwtProperties.getSecret()));
  }

  @Test
  void validateToken_returnsTrueForValidToken() {
    Instant now = Instant.now();
    String token =
        Jwts.builder()
            .subject("alice")
            .claim("roles", List.of("ROLE_EMPLOYEE"))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(600)))
            .signWith(signingKey)
            .compact();

    assertThat(jwtService.validateToken(token)).isTrue();
  }

  @Test
  void validateToken_returnsFalseForExpiredToken() {
    Instant now = Instant.now();
    String token =
        Jwts.builder()
            .subject("alice")
            .claim("roles", List.of("ROLE_EMPLOYEE"))
            .issuedAt(Date.from(now.minusSeconds(3600)))
            .expiration(Date.from(now.minusSeconds(1)))
            .signWith(signingKey)
            .compact();

    assertThat(jwtService.validateToken(token)).isFalse();
    assertThat(jwtService.isTokenExpired(token)).isTrue();
  }

  @Test
  void validateToken_returnsFalseForTamperedSignature() {
    Instant now = Instant.now();
    String token =
        Jwts.builder()
            .subject("alice")
            .claim("roles", List.of("ROLE_EMPLOYEE"))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(600)))
            .signWith(signingKey)
            .compact();

    String tampered = token + "x";
    assertThat(jwtService.validateToken(tampered)).isFalse();
  }

  @Test
  void extractUsername_returnsSubject() {
    Instant now = Instant.now();
    String token =
        Jwts.builder()
            .subject("bob")
            .claim("roles", List.of("ROLE_HR"))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(600)))
            .signWith(signingKey)
            .compact();

    assertThat(jwtService.extractUsername(token)).isEqualTo("bob");
  }
}
