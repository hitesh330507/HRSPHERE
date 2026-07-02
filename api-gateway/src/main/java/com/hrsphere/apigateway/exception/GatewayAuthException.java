package com.hrsphere.apigateway.exception;

public class GatewayAuthException extends RuntimeException {

  public GatewayAuthException(String message) {
    super(message);
  }

  public GatewayAuthException(String message, Throwable cause) {
    super(message, cause);
  }
}
