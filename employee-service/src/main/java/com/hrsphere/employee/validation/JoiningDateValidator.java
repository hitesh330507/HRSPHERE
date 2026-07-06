package com.hrsphere.employee.validation;

import com.hrsphere.employee.dto.CreateEmployeeRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Period;

public class JoiningDateValidator
    implements ConstraintValidator<ValidJoiningDate, CreateEmployeeRequest> {

  @Override
  public boolean isValid(CreateEmployeeRequest value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    if (value.dateOfBirth == null || value.dateOfJoining == null) {
      return true;
    }
    Period ageOnJoining = Period.between(value.dateOfBirth, value.dateOfJoining);
    return ageOnJoining.getYears() >= 16;
  }
}
