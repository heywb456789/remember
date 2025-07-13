package com.tomato.remember.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenUpdateResult {
    private boolean success;
    private String errorMessage;
    private long timestamp;
    
    public static TokenUpdateResult success() {
        return TokenUpdateResult.builder()
            .success(true)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    public static TokenUpdateResult failure(String errorMessage) {
        return TokenUpdateResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}