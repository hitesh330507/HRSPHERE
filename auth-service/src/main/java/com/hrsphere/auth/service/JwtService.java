package com.hrsphere.auth.service;

import com.hrsphere.auth.config.JwtProperties;
import com.hrsphere.auth.exception.InvalidTokenException;
import com.hrsphere.auth.exception.TokenExpiredException;
import com.hrsphere.auth.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final JwtProperties jwtProperties;

  public JwtService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  public String generateAccessToken(UserDetails userDetails) {
    Instant now = Instant.now();
    Instant expiration = now.plusMillis(jwtProperties.getAccessTokenExpiryMs());
    Map<String, Object> claims = new HashMap<>();
    claims.put(
        "roles",
        userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());

    if (userDetails instanceof UserPrincipal principal && principal.getEmail() != null) {
      claims.put("email", principal.getEmail());
    }

    return Jwts.builder()
        .claims(claims)
        .subject(userDetails.getUsername())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiration))
        .signWith(getSigningKey(), Jwts.SIG.HS256)
        .compact();
  }

  public boolean validateToken(String token, UserDetails userDetails) {
    try {
      Claims claims = extractAllClaims(token);
      return !isTokenExpired(claims) && userDetails.getUsername().equals(claims.getSubject());
    } catch (RuntimeException exception) {
      return false;
    }
  }

  public String extractUsername(String token) {
    return extractAllClaims(token).getSubject();
  }

  public Claims extractAllClaims(String token) {
    try {
      return Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (ExpiredJwtException exception) {
      throw new TokenExpiredException("Access token expired. Use /auth/refresh.", exception);
    } catch (JwtException | IllegalArgumentException exception) {
      throw new InvalidTokenException("Invalid or malformed token", exception);
    }
  }

  public boolean isTokenExpired(String token) {
    return isTokenExpired(extractAllClaims(token));
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
