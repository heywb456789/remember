package com.tomato.remember.application.family.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "초대 처리 응답")
public class InviteProcessResponse {

    @Schema(description = "메모리얼 이름")
    private String memorialName;

    @Schema(description = "초대자 이름")
    private String inviterName;

    @Schema(description = "관계 표시명")
    private String relationshipDisplayName;

    @Schema(description = "처리 시간")
    private Long timestamp;

    public static InviteProcessResponse of(String memorialName, String inviterName, String relationshipDisplayName) {
        return InviteProcessResponse.builder()
                .memorialName(memorialName)
                .inviterName(inviterName)
                .relationshipDisplayName(relationshipDisplayName)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}