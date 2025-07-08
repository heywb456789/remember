package com.tomato.naraclub.common.exception;

import com.tomato.naraclub.common.code.ResponseStatus;
import lombok.Getter;

@Getter
public class APIException extends RuntimeException {

  private final ResponseStatus status;

  public APIException() {
    super(ResponseStatus.INTERNAL_SERVER_ERROR.getMessage());
    this.status = ResponseStatus.INTERNAL_SERVER_ERROR;
  }

  public APIException(String message, ResponseStatus status) {
    super(message);
    this.status = status;
  }

  public APIException(ResponseStatus status) {
    super(status.getMessage());
    this.status = status;
  }
}
