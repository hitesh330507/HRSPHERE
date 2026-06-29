package com.hrsphere.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.hrsphere.auth.dto.RegisterRequest;
import com.hrsphere.auth.exception.UserAlreadyExistsException;
import com.hrsphere.auth.repository.RoleRepository;
import com.hrsphere.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private RoleRepository roleRepository;

  @Mock private AuthenticationManager authenticationManager;

  private PasswordEncoder passwordEncoder;

  @InjectMocks private AuthService authService;

  @BeforeEach
  void setUp() {
    passwordEncoder = new BCryptPasswordEncoder();
    authService =
        new AuthService(userRepository, roleRepository, passwordEncoder, authenticationManager);
  }

  @Test
  void register_shouldThrowWhenUsernameAlreadyExists() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("hitesh");
    request.setEmail("hitesh@hrsphere.dev");
    request.setPassword("Secure123!");

    given(userRepository.existsByUsername("hitesh")).willReturn(true);

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(UserAlreadyExistsException.class)
        .hasMessage("Username already exists");
  }

  @Test
  void register_shouldThrowWhenEmailAlreadyExists() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("hitesh");
    request.setEmail("hitesh@hrsphere.dev");
    request.setPassword("Secure123!");

    given(userRepository.existsByUsername("hitesh")).willReturn(false);
    given(userRepository.existsByEmail("hitesh@hrsphere.dev")).willReturn(true);

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(UserAlreadyExistsException.class)
        .hasMessage("Email already exists");
  }
}
