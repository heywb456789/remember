package com.tomato.remember.application.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 비밀번호 재설정 관련 One-ID API 응답 DTO
 * value 필드가 빈 문자열로 오는 경우를 처리하기 위한 전용 DTO
 */
@Data
public class PasswordResetOneIdResponse {
    
    private boolean result;
    private int code;
    private String message;
    
    /**
     * 비밀번호 재설정 관련 API는 value가 빈 문자열로 오므로 String으로 처리
     */
    @JsonProperty("value")
    private String value;
    
    /**
     * API 성공 여부 판단
     */
    public boolean isSuccess() {
        return result && code == 0;
    }
}