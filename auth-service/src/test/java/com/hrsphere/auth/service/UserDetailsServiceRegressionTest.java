package com.hrsphere.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.hrsphere.auth.entity.Role;
import com.hrsphere.auth.entity.User;
import com.hrsphere.auth.repository.RoleRepository;
import com.hrsphere.auth.repository.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:auth-regression;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false"
    })
class UserDetailsServiceRegressionTest {

  @Autowired private UserRepository userRepository;

  @Autowired private RoleRepository roleRepository;

  @Autowired private UserDetailsServiceImpl userDetailsService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void
      loadUserByUsername_shouldExposeAuthoritiesWhenRolesAreAccessedOutsideTheRepositoryTransaction() {
    Role role = new Role();
    role.setName("ROLE_EMPLOYEE");
    roleRepository.saveAndFlush(role);

    User user = new User();
    user.setUsername("hitesh");
    user.setEmail("hitesh@hrsphere.dev");
    user.setPasswordHash("encoded-password");
    user.setEnabled(true);
    user.setRoles(Set.of(role));
    userRepository.saveAndFlush(user);

    UserDetails userDetails =
        assertDoesNotThrow(() -> userDetailsService.loadUserByUsername("hitesh"));

    assertThat(userDetails.getUsername()).isEqualTo("hitesh");
    assertThat(userDetails.getAuthorities())
        .extracting(grantedAuthority -> grantedAuthority.getAuthority())
        .containsExactly("ROLE_EMPLOYEE");
  }
}
