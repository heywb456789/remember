package com.tomato.remember.application.oneld.dto;

import lombok.Data;


@Data
public class OneIdVerifyResponse {
    private boolean result;
    private int code;
    private String message;
    private boolean value;
}
