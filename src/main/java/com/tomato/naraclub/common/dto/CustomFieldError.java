package com.tomato.naraclub.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.validation.FieldError;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldError {

  private String field;
  private String value;
  private String reason;

  public static CustomFieldError of(FieldError fieldError) {
    return CustomFieldError.builder()
        .reason(fieldError.getDefaultMessage())
        .field(fieldError.getField())
        .value((String) fieldError.getRejectedValue())
        .build();

  }

}