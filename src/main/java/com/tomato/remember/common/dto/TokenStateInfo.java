package com.tomato.remember.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenStateInfo {
    private boolean hasAccessToken;
    private boolean hasRefreshToken;
    private boolean accessTokenValid;
    private boolean refreshTokenValid;
    private boolean isComplete;
    
    public boolean isPartiallyBroken() {
        return (hasAccessToken != hasRefreshToken) || 
               (accessTokenValid != refreshTokenValid);
    }
    
    public boolean isCompletelyBroken() {
        return !hasAccessToken && !hasRefreshToken;
    }
}