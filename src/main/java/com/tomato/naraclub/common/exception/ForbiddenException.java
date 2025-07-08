package com.tomato.naraclub.common.exception;

import com.tomato.naraclub.common.code.ResponseStatus;

public class ForbiddenException extends RuntimeException {

  public ForbiddenException() {
    super(ResponseStatus.FORBIDDEN.getMessage());
  }
}
