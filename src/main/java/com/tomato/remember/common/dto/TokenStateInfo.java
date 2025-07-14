package com.tomato.remember.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenStateInfo {
    private final boolean hasAccessToken;
    private final boolean hasRefreshToken;
    private final boolean isPartiallyBroken;

    public boolean isComplete() {
        return hasAccessToken && hasRefreshToken;
    }

    public boolean isEmpty() {
        return !hasAccessToken && !hasRefreshToken;
    }
}