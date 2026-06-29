package com.hrsphere.auth.exception;

import com.hrsphere.common.exception.BaseException;

public final class UserAlreadyExistsException extends BaseException {

  public UserAlreadyExistsException(String message) {
    super(message);
  }
}
