package com.hrsphere.auth.repository;

import com.hrsphere.auth.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  @EntityGraph(attributePaths = "roles")
  Optional<User> findByUsernameOrEmail(String username, String email);
}
