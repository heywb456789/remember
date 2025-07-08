package com.tomato.naraclub.application.oneld.dto;

import lombok.Data;


@Data
public class OneIdResponse {
    private boolean result;
    private int code;
    private String message;
    private OneIdValue value;
}
