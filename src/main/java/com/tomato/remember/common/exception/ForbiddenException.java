package com.tomato.remember.common.exception;

import com.tomato.remember.common.code.ResponseStatus;

public class ForbiddenException extends RuntimeException {

  public ForbiddenException() {
    super(ResponseStatus.FORBIDDEN.getMessage());
  }
}
