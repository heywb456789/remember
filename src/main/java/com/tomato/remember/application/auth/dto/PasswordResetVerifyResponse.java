package com.tomato.remember.application.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 비밀번호 재설정 인증번호 확인 및 비밀번호 재설정 응답 DTO
 * value 필드가 boolean으로 오는 경우를 처리
 */
@Data
public class PasswordResetVerifyResponse {
    
    private boolean result;
    private int code;
    private String message;
    
    /**
     * 인증번호 확인 결과 (true/false)
     */
    @JsonProperty("value")
    private boolean value;
    
    /**
     * API 성공 여부 판단
     */
    public boolean isSuccess() {
        return result && code == 0;
    }
    
    /**
     * 인증/재설정 성공 여부
     */
    public boolean isVerified() {
        return isSuccess() && value;
    }
}