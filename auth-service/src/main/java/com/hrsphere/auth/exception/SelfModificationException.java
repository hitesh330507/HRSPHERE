package com.hrsphere.auth.exception;

import com.hrsphere.common.exception.BaseException;

public class SelfModificationException extends BaseException {

  public SelfModificationException(String message) {
    super(message);
  }
}
