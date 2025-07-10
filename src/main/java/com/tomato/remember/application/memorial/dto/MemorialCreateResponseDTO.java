package com.tomato.remember.application.memorial.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemorialCreateResponseDTO {
    
    private Long memorialId;
    private String name;
    private String status;
    private String message;
    
    public static MemorialCreateResponseDTO of(Long memorialId, String name) {
        return MemorialCreateResponseDTO.builder()
                .memorialId(memorialId)
                .name(name)
                .status("DRAFT")
                .message("메모리얼이 임시 생성되었습니다.")
                .build();
    }
}