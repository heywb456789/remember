package com.tomato.remember.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenRefreshResult {
    private boolean success;
    private String newAccessToken;
    private String newRefreshToken;
    private Long memberId;
    private String errorMessage;
    
    public static TokenRefreshResult success(String accessToken, String refreshToken, Long memberId) {
        return TokenRefreshResult.builder()
            .success(true)
            .newAccessToken(accessToken)
            .newRefreshToken(refreshToken)
            .memberId(memberId)
            .build();
    }
    
    public static TokenRefreshResult failure(String errorMessage) {
        return TokenRefreshResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}