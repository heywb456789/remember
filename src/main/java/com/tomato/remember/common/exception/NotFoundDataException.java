package com.tomato.remember.common.exception;

import com.tomato.remember.common.code.ResponseStatus;

public class NotFoundDataException extends APIException {

  public NotFoundDataException() {
    super(ResponseStatus.DATA_NOT_FOUND);
  }
}
