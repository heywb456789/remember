package com.tomato.remember.application.wsvideo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.remember.application.wsvideo.dto
 * @fileName : MemorialVideoResponse
 * @date : 2025-07-22
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialVideoResponse {

    private String contactName;          // 연락처명 (예: "김근태")
    private String waitingVideoUrl;      // 대기영상 URL
    private Boolean available;           // 영상통화 가능 여부
    private String characterType;        // 캐릭터 타입 (예: "default", "premium")
    private Boolean success;             // API 호출 성공 여부
    private String message;              // 응답 메시지

    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }

    public boolean isAvailable() {
        return Boolean.TRUE.equals(available);
    }
}


