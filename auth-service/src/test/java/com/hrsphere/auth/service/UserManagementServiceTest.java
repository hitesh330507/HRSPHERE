package com.hrsphere.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrsphere.auth.exception.SelfModificationException;
import com.hrsphere.auth.repository.RoleRepository;
import com.hrsphere.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private RoleRepository roleRepository;

  @InjectMocks private UserManagementService userManagementService;

  @Test
  void changeUserRole_shouldThrowSelfModificationExceptionForAdmin() {
    assertThatThrownBy(() -> userManagementService.changeUserRole("admin", "ROLE_HR"))
        .isInstanceOf(SelfModificationException.class)
        .hasMessage("Admin user cannot be modified.");
  }

  @Test
  void setUserStatus_shouldThrowSelfModificationExceptionForAdmin() {
    assertThatThrownBy(() -> userManagementService.setUserStatus("admin", false))
        .isInstanceOf(SelfModificationException.class)
        .hasMessage("Admin user cannot be modified.");
  }
}
