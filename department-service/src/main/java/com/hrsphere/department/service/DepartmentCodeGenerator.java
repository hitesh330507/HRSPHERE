package com.hrsphere.department.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DepartmentCodeGenerator {

  private final JdbcTemplate jdbc;

  public DepartmentCodeGenerator(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public synchronized String nextCode() {
    // Known limitation: not thread-safe under high concurrency (concurrency gap if multiple threads
    // query MAX simultaneously)
    String maxCode =
        jdbc.queryForObject("SELECT max(department_code) FROM departments", String.class);
    if (maxCode == null) {
      return "DEPT-0001";
    }
    try {
      String numericPart = maxCode.substring(5); // "DEPT-" is 5 characters
      int nextVal = Integer.parseInt(numericPart) + 1;
      return String.format("DEPT-%04d", nextVal);
    } catch (Exception e) {
      return "DEPT-0001";
    }
  }
}
