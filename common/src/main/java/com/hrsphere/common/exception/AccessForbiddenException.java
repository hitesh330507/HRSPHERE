package com.hrsphere.common.exception;

public final class AccessForbiddenException extends BaseException {

  public AccessForbiddenException(String message) {
    super(message);
  }

  public AccessForbiddenException(String message, Throwable cause) {
    super(message, cause);
  }
}
