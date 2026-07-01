package com.hrsphere.auth.security;

import java.util.Collection;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

  private final String username;
  private final String password;
  private final String email;
  private final boolean enabled;
  private final Set<GrantedAuthority> authorities;

  public UserPrincipal(
      String username,
      String password,
      String email,
      boolean enabled,
      Set<GrantedAuthority> authorities) {
    this.username = username;
    this.password = password;
    this.email = email;
    this.enabled = enabled;
    this.authorities = authorities;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
