package com.tomato.naraclub.common.exception;


import com.tomato.naraclub.common.code.ResponseStatus;

public class BadRequestException extends APIException {

  public BadRequestException() {
    super(ResponseStatus.BAD_REQUEST);
  }

  // 메시지만 전달받아 BAD_REQUEST 로 래핑
  public BadRequestException(String message) {
    super(message, ResponseStatus.BAD_REQUEST);
  }

  // 만약 필요하다면, 둘 다 넘길 수 있는 생성자도 추가 가능
  public BadRequestException(String message, Throwable cause) {
    super(message, ResponseStatus.BAD_REQUEST);
    initCause(cause);
  }
}
