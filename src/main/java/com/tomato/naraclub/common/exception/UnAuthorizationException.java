package com.tomato.naraclub.common.exception;

import com.tomato.naraclub.common.code.ResponseStatus;
import lombok.Getter;

@Getter
public class UnAuthorizationException extends APIException {

  public UnAuthorizationException() {
    super(ResponseStatus.UNAUTHORIZED);
  }
  public UnAuthorizationException(String msg) {
    super(msg, ResponseStatus.UNAUTHORIZED);
  }

  public UnAuthorizationException(ResponseStatus status) {
    super(status);
  }

//  private final ResponseStatus status;
//  private final Object detail;
//
//  // 기본 생성자 (기본 메시지)
//  public UnAuthorizationException() {
//    super(ResponseStatus.UNAUTHORIZED.getMessage());
//    this.status = ResponseStatus.UNAUTHORIZED;
//    this.detail = null;
//  }
//
//  // 커스텀 메시지 생성자
//  public UnAuthorizationException(String customMessage) {
//    super(customMessage);
//    this.status = ResponseStatus.UNAUTHORIZED;
//    this.detail = null;
//  }
//
//  // 메시지와 추가 데이터(detail)를 함께 전달할 때
//  public UnAuthorizationException(String customMessage, Object detail) {
//    super(customMessage);
//    this.status = ResponseStatus.UNAUTHORIZED;
//    this.detail = detail;
//  }
}
