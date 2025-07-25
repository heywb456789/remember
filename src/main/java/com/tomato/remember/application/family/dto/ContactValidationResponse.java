package com.tomato.remember.application.family.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactValidationResponse {

    private String method;
    private String contact;
    private Boolean valid;
    private String message;
}