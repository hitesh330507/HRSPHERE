package com.hrsphere.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrsphere.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    JwtProperties jwtProperties = new JwtProperties();
    jwtProperties.setSecret("c29tZS1zZWN1cmUtY2hhbm5lbC1zZWNyZXQta2V5LTEyMzQ1Njc4OTA=");
    jwtProperties.setAccessTokenExpiryMs(900_000L);
    jwtProperties.setRefreshTokenExpiryMs(604_800_000L);
    jwtService = new JwtService(jwtProperties);
  }

  @Test
  void generateAccessToken_shouldCreateParseableTokenWithExpectedClaims() {
    UserDetails userDetails =
        new User("hitesh", "password", List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));

    String token = jwtService.generateAccessToken(userDetails);

    Claims claims = jwtService.extractAllClaims(token);
    assertThat(token).isNotBlank();
    assertThat(claims.getSubject()).isEqualTo("hitesh");
    assertThat(claims.get("roles")).isInstanceOf(List.class);
    assertThat(claims.get("email")).isNull();
  }

  @Test
  void validateToken_shouldReturnFalseForExpiredToken() {
    JwtProperties jwtProperties = new JwtProperties();
    jwtProperties.setSecret("c29tZS1zZWN1cmUtY2hhbm5lbC1zZWNyZXQta2V5LTEyMzQ1Njc4OTA=");
    jwtProperties.setAccessTokenExpiryMs(-1L);
    jwtProperties.setRefreshTokenExpiryMs(604_800_000L);
    JwtService expiredJwtService = new JwtService(jwtProperties);

    UserDetails userDetails =
        new User("hitesh", "password", List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
    String token = expiredJwtService.generateAccessToken(userDetails);

    assertThat(expiredJwtService.validateToken(token, userDetails)).isFalse();
  }

  @Test
  void extractUsername_shouldReturnSubjectForValidToken() {
    UserDetails userDetails =
        new User("hitesh", "password", List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));

    String token = jwtService.generateAccessToken(userDetails);

    assertThat(jwtService.extractUsername(token)).isEqualTo("hitesh");
  }
}
