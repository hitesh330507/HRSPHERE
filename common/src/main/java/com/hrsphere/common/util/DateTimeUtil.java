package com.hrsphere.common.util;

import java.time.Instant;

public final class DateTimeUtil {

  private DateTimeUtil() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Instant utcNow() {
    return Instant.now();
  }
}
