package com.hrsphere.employee.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmployeeCodeGenerator {

  private final JdbcTemplate jdbc;

  public EmployeeCodeGenerator(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public String nextCode() {
    Long next = jdbc.queryForObject("SELECT nextval('employee_code_seq')", Long.class);
    if (next == null) next = 1L;
    return String.format("EMP-%04d", next);
  }
}
