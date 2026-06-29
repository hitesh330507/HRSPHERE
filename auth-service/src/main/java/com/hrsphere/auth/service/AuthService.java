package com.hrsphere.auth.service;

import com.hrsphere.auth.dto.AuthResponse;
import com.hrsphere.auth.dto.LoginRequest;
import com.hrsphere.auth.dto.RegisterRequest;
import com.hrsphere.auth.entity.Role;
import com.hrsphere.auth.entity.User;
import com.hrsphere.auth.exception.UserAlreadyExistsException;
import com.hrsphere.auth.repository.RoleRepository;
import com.hrsphere.auth.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
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

  public AuthService(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
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
    return toAuthResponse(saved, "Registration successful.");
  }

  public AuthResponse login(LoginRequest request) throws AuthenticationException {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    User user =
        userRepository
            .findByUsernameOrEmail(request.getUsername(), request.getUsername())
            .orElseThrow(
                () -> new IllegalStateException("Authenticated user record could not be loaded."));

    return toAuthResponse(user, "Login successful. JWT coming in Day 7.");
  }

  private AuthResponse toAuthResponse(User user, String message) {
    List<String> roles = user.getRoles().stream().map(Role::getName).toList();
    return new AuthResponse(user.getUsername(), user.getEmail(), roles, message, Instant.now());
  }
}
