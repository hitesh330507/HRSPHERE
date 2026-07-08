package com.hrsphere.common.exception;

public final class InvalidReferenceException extends BaseException {

  public InvalidReferenceException(String message) {
    super(message);
  }

  public InvalidReferenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
