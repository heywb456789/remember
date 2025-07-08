package com.tomato.naraclub.common.dto;

import com.tomato.naraclub.common.code.ResponseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO<T> {

  @Schema(description = "응답 상태")
  ResponseStatus status;

  @Schema(description = "응답 데이터")
  T response;

  public static <T> ResponseDTO<T> ok(T response) {
    return (ResponseDTO<T>) ResponseDTO.builder()
            .status(ResponseStatus.OK)
            .response(response)
            .build();
  }

  public static <T> ResponseDTO<T> ok() {
    return (ResponseDTO<T>) ResponseDTO.builder()
            .status(ResponseStatus.OK)
            .build();
  }

  public static <T> ResponseDTO<T> error(ResponseStatus responseStatus) {
    return (ResponseDTO<T>) ResponseDTO.builder()
            .status(responseStatus)
            .build();
  }

  public static <T> ResponseDTO<T> unauthorized() {
    return (ResponseDTO<T>) ResponseDTO.builder()
            .status(ResponseStatus.UNAUTHORIZED)
            .build();
  }

  public static <T> ResponseDTO<T> forbidden() {
    return (ResponseDTO<T>) ResponseDTO.builder()
            .status(ResponseStatus.UNAUTHORIZED)
            .build();
  }

  public static <T> ResponseDTO<T> badRequest(T response) {
    return (ResponseDTO<T>) ResponseDTO.builder()
            .status(ResponseStatus.BAD_REQUEST)
            .response(response)
            .build();
  }

  public static <T> ResponseDTO<T> internalServerError() {
    return (ResponseDTO<T>) ResponseDTO.builder()
            .status(ResponseStatus.INTERNAL_SERVER_ERROR)
            .build();
  }

  public static <T> ResponseDTO<T> noContent() {
    return (ResponseDTO<T>) ResponseDTO.builder()
            .status(ResponseStatus.NO_CHANGE_DATA)
            .build();
  }
}
