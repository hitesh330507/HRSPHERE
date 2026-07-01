package com.hrsphere.auth.service;

import com.hrsphere.auth.config.JwtProperties;
import com.hrsphere.auth.dto.AuthResponse;
import com.hrsphere.auth.dto.LoginRequest;
import com.hrsphere.auth.dto.LogoutResponse;
import com.hrsphere.auth.dto.RegisterRequest;
import com.hrsphere.auth.entity.Role;
import com.hrsphere.auth.entity.User;
import com.hrsphere.auth.exception.UserAlreadyExistsException;
import com.hrsphere.auth.repository.RoleRepository;
import com.hrsphere.auth.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final String DEFAULT_ROLE = "ROLE_EMPLOYEE";

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final UserDetailsService userDetailsService;
  private final JwtProperties jwtProperties;

  public AuthService(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      UserDetailsService userDetailsService,
      JwtProperties jwtProperties) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.userDetailsService = userDetailsService;
    this.jwtProperties = jwtProperties;
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
    return toAuthResponse(saved);
  }

  public AuthResponse login(LoginRequest request) throws AuthenticationException {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    User user =
        userRepository
            .findByUsernameOrEmail(request.getUsername(), request.getUsername())
            .orElseThrow(
                () -> new IllegalStateException("Authenticated user record could not be loaded."));

    return toAuthResponse(user);
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
}
