package com.hrsphere.auth.exception;

import com.hrsphere.common.exception.BaseException;

public class InvalidTokenException extends BaseException {

  public InvalidTokenException(String message) {
    super(message);
  }

  public InvalidTokenException(String message, Throwable cause) {
    super(message, cause);
  }
}
