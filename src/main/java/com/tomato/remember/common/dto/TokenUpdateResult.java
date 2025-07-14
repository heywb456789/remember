package com.tomato.remember.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenUpdateResult {
    private final boolean success;
    private final String errorMessage;

    public static TokenUpdateResult success() {
        return TokenUpdateResult.builder()
                .success(true)
                .build();
    }

    public static TokenUpdateResult failure(String errorMessage) {
        return TokenUpdateResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}