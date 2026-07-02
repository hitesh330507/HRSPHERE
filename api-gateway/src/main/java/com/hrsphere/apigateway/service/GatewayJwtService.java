package com.hrsphere.apigateway.service;

import com.hrsphere.apigateway.config.JwtProperties;
import com.hrsphere.apigateway.exception.GatewayAuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class GatewayJwtService {

  private final JwtProperties jwtProperties;

  public GatewayJwtService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  public String extractUsername(String token) {
    return extractAllClaims(token).getSubject();
  }

  public List<String> extractRoles(String token) {
    Claims claims = extractAllClaims(token);
    Object rolesClaim = claims.get("roles");
    if (rolesClaim instanceof List<?> roles) {
      List<String> result = new ArrayList<>();
      for (Object role : roles) {
        if (role != null) {
          result.add(role.toString());
        }
      }
      return result;
    }
    if (rolesClaim instanceof String roleValue) {
      return List.of(roleValue);
    }
    return List.of();
  }

  public boolean isTokenExpired(String token) {
    try {
      Claims claims = parseClaims(token, true);
      return isTokenExpired(claims);
    } catch (GatewayAuthException exception) {
      return false;
    }
  }

  public boolean validateToken(String token) {
    try {
      Claims claims = parseClaims(token, true);
      return !isTokenExpired(claims);
    } catch (GatewayAuthException exception) {
      return false;
    }
  }

  private Claims extractAllClaims(String token) {
    return parseClaims(token, false);
  }

  private Claims parseClaims(String token, boolean tolerateExpired) {
    try {
      return Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (ExpiredJwtException exception) {
      if (tolerateExpired) {
        return exception.getClaims();
      }
      throw new GatewayAuthException("JWT token is expired", exception);
    } catch (JwtException | IllegalArgumentException exception) {
      throw new GatewayAuthException("JWT token is invalid", exception);
    }
  }

  private boolean isTokenExpired(Claims claims) {
    Date expiration = claims.getExpiration();
    return expiration == null || expiration.before(new Date());
  }

  private SecretKey getSigningKey() {
    try {
      byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
      return Keys.hmacShaKeyFor(keyBytes);
    } catch (IllegalArgumentException exception) {
      byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
      return Keys.hmacShaKeyFor(keyBytes);
    }
  }
}
