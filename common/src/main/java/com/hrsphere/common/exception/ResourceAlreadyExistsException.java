package com.hrsphere.common.exception;

public final class ResourceAlreadyExistsException extends BaseException {

  public ResourceAlreadyExistsException(String message) {
    super(message);
  }

  public ResourceAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
