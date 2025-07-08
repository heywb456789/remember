package com.tomato.remember.admin.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminRefreshResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private int expiresIn;
}