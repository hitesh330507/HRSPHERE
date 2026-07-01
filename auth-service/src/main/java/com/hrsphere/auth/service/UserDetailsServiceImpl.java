package com.hrsphere.auth.service;

import com.hrsphere.auth.entity.User;
import com.hrsphere.auth.repository.UserRepository;
import com.hrsphere.auth.security.UserPrincipal;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  public UserDetailsServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .orElseThrow(
                () ->
                    new UsernameNotFoundException(
                        "User not found with username or email: " + usernameOrEmail));

    return new UserPrincipal(
        user.getUsername(),
        user.getPasswordHash(),
        user.getEmail(),
        user.isEnabled(),
        user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getName()))
            .collect(Collectors.toSet()));
  }
}
