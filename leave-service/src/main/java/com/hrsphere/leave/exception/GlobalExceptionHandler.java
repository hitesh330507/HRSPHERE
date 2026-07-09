package com.hrsphere.leave.exception;

import com.hrsphere.common.dto.ApiErrorResponse;
import com.hrsphere.common.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
      ResourceNotFoundException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  @ExceptionHandler(InvalidReferenceException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidReference(
      InvalidReferenceException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(AccessForbiddenException.class)
  public ResponseEntity<ApiErrorResponse> handleAccessForbidden(
      AccessForbiddenException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request);
  }

  @ExceptionHandler(InsufficientLeaveBalanceException.class)
  public ResponseEntity<ApiErrorResponse> handleInsufficientLeaveBalance(
      InsufficientLeaveBalanceException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(OverlappingLeaveRequestException.class)
  public ResponseEntity<ApiErrorResponse> handleOverlappingLeaveRequest(
      OverlappingLeaveRequestException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
  }

  @ExceptionHandler(InvalidStateTransitionException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidStateTransition(
      InvalidStateTransitionException exception, HttpServletRequest request) {
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

  @ExceptionHandler(
      org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
      org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception,
      HttpServletRequest request) {
    String requiredType =
        exception.getRequiredType() != null
            ? exception.getRequiredType().getSimpleName()
            : "required type";
    String message =
        String.format(
            "Invalid value '%s' for parameter '%s': expected %s format",
            exception.getValue(), exception.getName(), requiredType);
    return buildResponse(HttpStatus.BAD_REQUEST, message, request);
  }

  @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
  public ResponseEntity<ApiErrorResponse> handleResourceAccess(
      org.springframework.web.client.ResourceAccessException exception,
      HttpServletRequest request) {
    log.error("Downstream service is offline: {}", exception.getMessage());
    return buildResponse(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Downstream service is offline. Please try again later.",
        request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
      Exception exception, HttpServletRequest request) {
    log.error("Unhandled exception while processing {}", request.getRequestURI(), exception);
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(
      HttpStatus status, String message, HttpServletRequest request) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            java.time.Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI());
    return new ResponseEntity<>(body, status);
  }
}
