package com.hrsphere.auth.service;

import com.hrsphere.auth.config.JwtProperties;
import com.hrsphere.auth.dto.AuthResponse;
import com.hrsphere.auth.dto.LoginRequest;
import com.hrsphere.auth.dto.LogoutResponse;
import com.hrsphere.auth.dto.RegisterRequest;
import com.hrsphere.auth.entity.Role;
import com.hrsphere.auth.entity.User;
import com.hrsphere.auth.event.UserCreatedPayload;
import com.hrsphere.auth.exception.UserAlreadyExistsException;
import com.hrsphere.auth.repository.RoleRepository;
import com.hrsphere.auth.repository.UserRepository;
import com.hrsphere.common.event.EventPublisher;
import com.hrsphere.common.event.EventType;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private static final String DEFAULT_ROLE = "ROLE_EMPLOYEE";
  private static final String SERVICE_NAME = "auth-service";

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final UserDetailsService userDetailsService;
  private final JwtProperties jwtProperties;
  private final EventPublisher eventPublisher;
  private final MeterRegistry meterRegistry;

  public AuthService(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      UserDetailsService userDetailsService,
      JwtProperties jwtProperties,
      EventPublisher eventPublisher,
      MeterRegistry meterRegistry) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.userDetailsService = userDetailsService;
    this.jwtProperties = jwtProperties;
    this.eventPublisher = eventPublisher;
    this.meterRegistry = meterRegistry;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new UserAlreadyExistsException("Username already exists");
    }

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new UserAlreadyExistsException("Email already exists");
    }

    String roleName = Optional.ofNullable(request.getRole()).orElse(DEFAULT_ROLE);
    Role role =
        roleRepository
            .findByName(roleName)
            .orElseThrow(
                () -> new IllegalStateException("The role " + roleName + " is not available."));

    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setEnabled(true);
    user.setRoles(Set.of(role));

    User saved = userRepository.save(user);
    publishUserCreatedAfterCommit(saved);

    UserDetails userDetails = userDetailsService.loadUserByUsername(saved.getUsername());
    String accessToken = jwtService.generateAccessToken(userDetails);
    String refreshToken = null;
    try {
      refreshToken = refreshTokenService.createRefreshToken(saved.getUsername());
    } catch (RuntimeException e) {
      log.error(
          "Failed to create refresh token during registration (Redis down): {}", e.getMessage(), e);
    }
    return buildAuthResponse(saved, accessToken, refreshToken);
  }

  public AuthResponse login(LoginRequest request) throws AuthenticationException {
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

      User user =
          userRepository
              .findByUsernameOrEmail(request.getUsername(), request.getUsername())
              .orElseThrow(
                  () ->
                      new IllegalStateException("Authenticated user record could not be loaded."));

      AuthResponse response = toAuthResponse(user);
      try {
        meterRegistry.counter("auth_login_success").increment();
      } catch (Exception e) {
        log.warn("Failed to increment auth_login_success counter: {}", e.getMessage());
      }
      return response;
    } catch (Exception e) {
      try {
        meterRegistry.counter("auth_login_failure").increment();
      } catch (Exception ex) {
        log.warn("Failed to increment auth_login_failure counter: {}", ex.getMessage());
      }
      throw e;
    }
  }

  public AuthResponse refresh(String refreshToken) {
    String username = refreshTokenService.validateRefreshToken(refreshToken);
    User user =
        userRepository
            .findByUsernameOrEmail(username, username)
            .orElseThrow(
                () -> new IllegalStateException("Authenticated user record could not be loaded."));

    String newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken, username);
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    String accessToken = jwtService.generateAccessToken(userDetails);
    return buildAuthResponse(user, accessToken, newRefreshToken);
  }

  public LogoutResponse logout(String refreshToken) {
    refreshTokenService.deleteRefreshToken(refreshToken);
    return new LogoutResponse("Logged out successfully");
  }

  private AuthResponse toAuthResponse(User user) {
    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
    String accessToken = jwtService.generateAccessToken(userDetails);
    String refreshToken = refreshTokenService.createRefreshToken(user.getUsername());
    return buildAuthResponse(user, accessToken, refreshToken);
  }

  private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
    List<String> roles = user.getRoles().stream().map(Role::getName).toList();
    return new AuthResponse(
        accessToken,
        refreshToken,
        "Bearer",
        jwtProperties.getAccessTokenExpiryMs(),
        user.getUsername(),
        user.getEmail(),
        roles);
  }

  private void publishUserCreatedAfterCommit(User user) {
    List<String> roles = user.getRoles().stream().map(Role::getName).toList();
    UserCreatedPayload payload = new UserCreatedPayload(user.getUsername(), user.getEmail(), roles);
    Runnable publish = () -> publishUserCreated(payload);

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              publish.run();
            }
          });
    } else {
      publish.run();
    }
  }

  private void publishUserCreated(UserCreatedPayload payload) {
    try {
      eventPublisher.publish(EventType.USER_CREATED, SERVICE_NAME, payload);
    } catch (RuntimeException e) {
      log.error("Failed to dispatch user.created event: {}", e.getMessage(), e);
    }
  }
}
