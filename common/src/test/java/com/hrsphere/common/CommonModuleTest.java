package com.hrsphere.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.hrsphere.common.dto.ApiErrorResponse;
import com.hrsphere.common.exception.ResourceNotFoundException;
import com.hrsphere.common.exception.ValidationException;
import com.hrsphere.common.util.DateTimeUtil;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CommonModuleTest {

  @Test
  void errorResponseHasExpectedValues() {
    Instant now = DateTimeUtil.utcNow();
    ApiErrorResponse response =
        new ApiErrorResponse(now, 404, "Not Found", "Resource could not be located", "/api/test");

    assertEquals(now, response.timestamp());
    assertEquals(404, response.status());
    assertEquals("Not Found", response.error());
    assertEquals("Resource could not be located", response.message());
    assertEquals("/api/test", response.path());
  }

  @Test
  void exceptionsExtendBaseException() {
    assertInstanceOf(ResourceNotFoundException.class, new ResourceNotFoundException("missing"));
    assertInstanceOf(ValidationException.class, new ValidationException("invalid"));
  }
}
