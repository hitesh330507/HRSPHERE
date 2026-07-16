package com.hrsphere.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.hrsphere.auth.config.JwtProperties;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private RoleRepository roleRepository;

  @Mock private AuthenticationManager authenticationManager;

  @Mock private JwtService jwtService;

  @Mock private RefreshTokenService refreshTokenService;

  @Mock private UserDetailsService userDetailsService;

  @Mock private EventPublisher eventPublisher;

  @Mock private MeterRegistry meterRegistry;

  private PasswordEncoder passwordEncoder;
  private JwtProperties jwtProperties;

  @InjectMocks private AuthService authService;

  @BeforeEach
  void setUp() {
    passwordEncoder = new BCryptPasswordEncoder();
    jwtProperties = new JwtProperties();
    jwtProperties.setAccessTokenExpiryMs(900_000L);
    authService =
        new AuthService(
            userRepository,
            roleRepository,
            passwordEncoder,
            authenticationManager,
            jwtService,
            refreshTokenService,
            userDetailsService,
            jwtProperties,
            eventPublisher,
            meterRegistry);
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

  @Test
  void register_shouldPublishUserCreatedEvent() {
    RegisterRequest request = registrationRequest();
    Role role = role("ROLE_EMPLOYEE");
    given(userRepository.existsByUsername("hitesh")).willReturn(false);
    given(userRepository.existsByEmail("hitesh@hrsphere.dev")).willReturn(false);
    given(roleRepository.findByName("ROLE_EMPLOYEE")).willReturn(Optional.of(role));
    given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
    given(userDetailsService.loadUserByUsername("hitesh")).willReturn(userDetails());
    given(jwtService.generateAccessToken(any(UserDetails.class))).willReturn("access-token");
    given(refreshTokenService.createRefreshToken("hitesh")).willReturn("refresh-token");

    authService.register(request);

    ArgumentCaptor<UserCreatedPayload> payloadCaptor =
        ArgumentCaptor.forClass(UserCreatedPayload.class);
    verify(eventPublisher)
        .publish(eq(EventType.USER_CREATED), eq("auth-service"), payloadCaptor.capture());
    UserCreatedPayload payload = payloadCaptor.getValue();
    org.assertj.core.api.Assertions.assertThat(payload.username()).isEqualTo("hitesh");
    org.assertj.core.api.Assertions.assertThat(payload.email()).isEqualTo("hitesh@hrsphere.dev");
    org.assertj.core.api.Assertions.assertThat(payload.roles()).containsExactly("ROLE_EMPLOYEE");
  }

  @Test
  void register_shouldCompleteWhenEventPublisherThrows() {
    RegisterRequest request = registrationRequest();
    Role role = role("ROLE_EMPLOYEE");
    given(userRepository.existsByUsername("hitesh")).willReturn(false);
    given(userRepository.existsByEmail("hitesh@hrsphere.dev")).willReturn(false);
    given(roleRepository.findByName("ROLE_EMPLOYEE")).willReturn(Optional.of(role));
    given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
    given(userDetailsService.loadUserByUsername("hitesh")).willReturn(userDetails());
    given(jwtService.generateAccessToken(any(UserDetails.class))).willReturn("access-token");
    given(refreshTokenService.createRefreshToken("hitesh")).willReturn("refresh-token");
    doThrow(new RuntimeException("redis unavailable"))
        .when(eventPublisher)
        .publish(eq(EventType.USER_CREATED), eq("auth-service"), any(UserCreatedPayload.class));

    assertThatCode(() -> authService.register(request)).doesNotThrowAnyException();
  }

  private RegisterRequest registrationRequest() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("hitesh");
    request.setEmail("hitesh@hrsphere.dev");
    request.setPassword("Secure123!");
    return request;
  }

  private Role role(String name) {
    Role role = new Role();
    role.setName(name);
    return role;
  }

  private UserDetails userDetails() {
    return org.springframework.security.core.userdetails.User.withUsername("hitesh")
        .password("hash")
        .roles("EMPLOYEE")
        .build();
  }
}
