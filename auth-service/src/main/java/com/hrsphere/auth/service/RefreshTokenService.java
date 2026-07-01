package com.hrsphere.auth.service;

import com.hrsphere.auth.config.JwtProperties;
import com.hrsphere.auth.exception.InvalidTokenException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

  private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

  private final StringRedisTemplate redisTemplate;
  private final JwtProperties jwtProperties;

  public RefreshTokenService(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
    this.redisTemplate = redisTemplate;
    this.jwtProperties = jwtProperties;
  }

  public String createRefreshToken(String username) {
    String token = UUID.randomUUID().toString();
    redisTemplate
        .opsForValue()
        .set(
            REFRESH_TOKEN_PREFIX + token,
            username,
            Duration.ofMillis(jwtProperties.getRefreshTokenExpiryMs()));
    return token;
  }

  public String validateRefreshToken(String token) {
    String username = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + token);
    if (username == null || username.isBlank()) {
      throw new InvalidTokenException("Refresh token invalid or expired");
    }
    return username;
  }

  public void deleteRefreshToken(String token) {
    redisTemplate.delete(REFRESH_TOKEN_PREFIX + token);
  }

  public String rotateRefreshToken(String oldToken, String username) {
    deleteRefreshToken(oldToken);
    return createRefreshToken(username);
  }
}
