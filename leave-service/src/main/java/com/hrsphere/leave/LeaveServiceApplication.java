package com.hrsphere.leave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.hrsphere.leave", "com.hrsphere.common"})
public class LeaveServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(LeaveServiceApplication.class, args);
  }
}
