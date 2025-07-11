package com.tomato.remember.application.memorial.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemorialCreateResponseDTO {
    
    private Long memorialId;
    private String name;
    private String nickname;
    private String message;
    private Boolean success;

    public static MemorialCreateResponseDTO success(Long memorialId, String name, String nickname) {
        return MemorialCreateResponseDTO.builder()
                .memorialId(memorialId)
                .name(name)
                .nickname(nickname)
                .message("메모리얼이 성공적으로 등록되었습니다.")
                .success(true)
                .build();
    }

    public static MemorialCreateResponseDTO failure(String message) {
        return MemorialCreateResponseDTO.builder()
                .message(message)
                .success(false)
                .build();
    }
}