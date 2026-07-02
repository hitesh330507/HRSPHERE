package com.hrsphere.auth.exception;

import com.hrsphere.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ApiErrorResponse> handleUserAlreadyExists(
      UserAlreadyExistsException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
  }

  @ExceptionHandler(UsernameNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleUsernameNotFound(
      UsernameNotFoundException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiErrorResponse> handleAuthenticationFailure(
      AuthenticationException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
  }

  @ExceptionHandler(InvalidTokenException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidToken(
      InvalidTokenException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
  }

  @ExceptionHandler(TokenExpiredException.class)
  public ResponseEntity<ApiErrorResponse> handleTokenExpired(
      TokenExpiredException exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.UNAUTHORIZED, "Access token expired. Use /auth/refresh.", request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleAccessDenied(
      AccessDeniedException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.FORBIDDEN, "Access denied. Insufficient permissions.", request);
  }

  @ExceptionHandler(SelfModificationException.class)
  public ResponseEntity<ApiErrorResponse> handleSelfModification(
      SelfModificationException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(InvalidRoleException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidRole(
      InvalidRoleException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationFailure(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .collect(java.util.stream.Collectors.joining("; ", "", ""));
    if (message.isBlank()) {
      message = "Validation failed";
    }
    return buildResponse(HttpStatus.BAD_REQUEST, message, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
      Exception exception, HttpServletRequest request) {
    log.error("Unhandled exception while processing {}", request.getRequestURI(), exception);
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(
      HttpServletRequest request, HttpStatus status, String message) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            java.time.Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI());
    return new ResponseEntity<>(body, status);
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(
      HttpStatus status, String message, HttpServletRequest request) {
    return buildResponse(request, status, message);
  }
}
