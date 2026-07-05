package com.hrsphere.auth.service;

import com.hrsphere.auth.dto.UserResponse;
import com.hrsphere.auth.dto.UserSummaryResponse;
import com.hrsphere.auth.entity.Role;
import com.hrsphere.auth.entity.User;
import com.hrsphere.auth.exception.InvalidRoleException;
import com.hrsphere.auth.exception.SelfModificationException;
import com.hrsphere.auth.repository.RoleRepository;
import com.hrsphere.auth.repository.UserRepository;
import com.hrsphere.common.exception.ResourceNotFoundException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

  private static final String ADMIN_USERNAME = "admin";
  private static final Set<String> VALID_ROLES = Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_EMPLOYEE");

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;

  public UserManagementService(UserRepository userRepository, RoleRepository roleRepository) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
  }

  @Transactional(readOnly = true)
  public Page<UserResponse> getAllUsers(Pageable pageable) {
    Page<User> users = userRepository.findAll(pageable);
    List<UserResponse> content = users.stream().map(this::toUserResponse).toList();
    return new PageImpl<>(content, pageable, users.getTotalElements());
  }

  @Transactional(readOnly = true)
  public UserResponse getUserByUsername(String username) {
    User user =
        userRepository
            .findByUsernameOrEmail(username, username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    return toUserResponse(user);
  }

  @Transactional
  public UserResponse changeUserRole(String username, String newRoleName) {
    if (ADMIN_USERNAME.equalsIgnoreCase(username)) {
      throw new SelfModificationException("Admin user cannot be modified.");
    }

    if (!VALID_ROLES.contains(newRoleName)) {
      throw new InvalidRoleException("Invalid role name: " + newRoleName);
    }

    User user =
        userRepository
            .findByUsernameOrEmail(username, username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

    Role role =
        roleRepository
            .findByName(newRoleName)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + newRoleName));

    user.setRoles(new HashSet<>(Set.of(role)));
    User saved = userRepository.save(user);
    return toUserResponse(saved);
  }

  @Transactional
  public UserResponse setUserStatus(String username, boolean enabled) {
    if (ADMIN_USERNAME.equalsIgnoreCase(username)) {
      throw new SelfModificationException("Admin user cannot be modified.");
    }

    User user =
        userRepository
            .findByUsernameOrEmail(username, username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

    user.setEnabled(enabled);
    User saved = userRepository.save(user);
    return toUserResponse(saved);
  }

  @Transactional(readOnly = true)
  public UserSummaryResponse getUserSummary() {
    List<User> users = userRepository.findAll();
    Map<String, Long> byRole = new LinkedHashMap<>();
    for (String roleName : VALID_ROLES) {
      long count =
          users.stream()
              .flatMap(user -> user.getRoles().stream())
              .map(Role::getName)
              .filter(roleName::equals)
              .count();
      byRole.put(roleName, count);
    }

    long activeUsers = users.stream().filter(User::isEnabled).count();
    long inactiveUsers = users.size() - activeUsers;

    return new UserSummaryResponse(users.size(), byRole, activeUsers, inactiveUsers);
  }

  private UserResponse toUserResponse(User user) {
    List<String> roles =
        user.getRoles().stream().map(Role::getName).sorted().collect(Collectors.toList());
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        roles,
        user.isEnabled(),
        user.getCreatedAt());
  }
}
