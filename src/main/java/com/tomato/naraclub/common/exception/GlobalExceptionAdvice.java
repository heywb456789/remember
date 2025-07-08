package com.tomato.naraclub.common.exception;

import com.tomato.naraclub.common.dto.CustomFieldError;
import com.tomato.naraclub.common.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Collections;
import java.util.List;

@Slf4j
@ResponseBody
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionAdvice {

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  protected ResponseDTO<Object> handleInternalServerError(Exception e) {
    log("Exception", e);
    return ResponseDTO.internalServerError();
  }

  @ExceptionHandler(APIException.class)
  protected ResponseEntity<ResponseDTO<Object>> handleApiException(APIException e) {
    log("APIException", e);
    return ResponseEntity
            .status(e.getStatus().getHttpStatus()) // 💡 enum에서 직접 HttpStatus 추출
            .body(ResponseDTO.error(e.getStatus()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  protected ResponseDTO<Object> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    log("MethodArgumentNotValidException", e);
    return ResponseDTO.badRequest(getFieldErrors(e.getBindingResult()));
  }

  private List<CustomFieldError> getFieldErrors(BindingResult bindingResult) {
    var errors = bindingResult.getFieldErrors();
    if (CollectionUtils.isEmpty(errors)) {
      return Collections.emptyList();
    }
    return errors.stream()
        .map(CustomFieldError::of)
        .toList();
  }

  private void log(String handler, Throwable e) {
    log.error("[{}: {}]", handler, e.getClass().getSimpleName(), e);
  }

}
