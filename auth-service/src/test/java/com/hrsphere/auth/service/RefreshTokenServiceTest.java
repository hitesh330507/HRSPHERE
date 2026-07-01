package com.hrsphere.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hrsphere.auth.config.JwtProperties;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RefreshTokenServiceTest {

  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOperations;
  private RefreshTokenService refreshTokenService;

  @BeforeEach
  void setUp() {
    redisTemplate = mock(StringRedisTemplate.class);
    valueOperations = mock(ValueOperations.class);
    given(redisTemplate.opsForValue()).willReturn(valueOperations);

    JwtProperties jwtProperties = new JwtProperties();
    jwtProperties.setRefreshTokenExpiryMs(604_800_000L);
    refreshTokenService = new RefreshTokenService(redisTemplate, jwtProperties);
  }

  @Test
  void create_shouldStoreRefreshTokenInRedis() {
    String token = refreshTokenService.createRefreshToken("hitesh");

    assertThat(token).isNotBlank();
    verify(valueOperations).set(eq("refresh_token:" + token), eq("hitesh"), any(Duration.class));
  }

  @Test
  void validate_shouldReturnUsernameWhenTokenExists() {
    String token = "test-token";
    given(valueOperations.get("refresh_token:" + token)).willReturn("hitesh");

    assertThat(refreshTokenService.validateRefreshToken(token)).isEqualTo("hitesh");
  }

  @Test
  void delete_shouldRemoveRefreshTokenFromRedis() {
    String token = "test-token";

    refreshTokenService.deleteRefreshToken(token);

    verify(redisTemplate).delete("refresh_token:" + token);
  }

  @Test
  void rotate_shouldDeleteOldAndCreateNewToken() {
    String oldToken = "old-token";
    given(redisTemplate.delete("refresh_token:" + oldToken)).willReturn(true);

    String newToken = refreshTokenService.rotateRefreshToken(oldToken, "hitesh");

    assertThat(newToken).isNotBlank();
    verify(redisTemplate).delete("refresh_token:" + oldToken);
    verify(valueOperations).set(eq("refresh_token:" + newToken), eq("hitesh"), any(Duration.class));
  }
}
