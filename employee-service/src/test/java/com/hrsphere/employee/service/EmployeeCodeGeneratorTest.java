package com.hrsphere.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class EmployeeCodeGeneratorTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @InjectMocks private EmployeeCodeGenerator generator;

  @Test
  void nextCode_shouldFormatCorrectly() {
    given(jdbcTemplate.queryForObject("SELECT nextval('employee_code_seq')", Long.class))
        .willReturn(5L);

    String code = generator.nextCode();

    assertThat(code).isEqualTo("EMP-0005");
  }

  @Test
  void nextCode_shouldDefaultTo1WhenNull() {
    given(jdbcTemplate.queryForObject("SELECT nextval('employee_code_seq')", Long.class))
        .willReturn(null);

    String code = generator.nextCode();

    assertThat(code).isEqualTo("EMP-0001");
  }
}
