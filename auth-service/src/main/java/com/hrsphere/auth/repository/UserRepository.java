package com.hrsphere.auth.repository;

import com.hrsphere.auth.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  @EntityGraph(attributePaths = "roles")
  Optional<User> findByUsernameOrEmail(String username, String email);

  @EntityGraph(attributePaths = "roles")
  Page<User> findAll(Pageable pageable);

  @EntityGraph(attributePaths = "roles")
  List<User> findAll();
}
