package com.hrsphere.auth.exception;

public class TokenExpiredException extends InvalidTokenException {

  public TokenExpiredException(String message) {
    super(message);
  }

  public TokenExpiredException(String message, Throwable cause) {
    super(message, cause);
  }
}
