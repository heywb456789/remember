package com.tomato.remember.application.family.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 가족 초대 발송 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "가족 초대 발송 응답")
public class FamilyInviteResponse {

    @Schema(description = "초대 방법", example = "email")
    private String method;

    @Schema(description = "마스킹된 연락처", example = "te****@example.com")
    private String maskedContact;

    @Schema(description = "메모리얼 ID")
    private Long memorialId;

    @Schema(description = "관계 표시명", example = "자녀")
    private String relationshipDisplayName;

    @Schema(description = "초대 토큰 (SMS용)", example = "abc123...")
    private String token;

    @Schema(description = "SMS 앱 실행 URL (SMS용)")
    private String smsUrl;

    @Schema(description = "짧은 SMS 앱 실행 URL (SMS용)")
    private String shortSmsUrl;

    @Schema(description = "메시지 길이 (SMS용)")
    private Integer messageLength;

    @Schema(description = "짧은 메시지 길이 (SMS용)")
    private Integer shortMessageLength;

    @Schema(description = "짧은 메시지 권장 여부 (SMS용)")
    private Boolean recommendShort;

    @Schema(description = "처리 시간")
    private Long timestamp;

    // SMS 응답용 팩토리 메서드
    public static FamilyInviteResponse forSms(String maskedContact, Long memorialId, 
                                              String relationshipDisplayName, String token, 
                                              String smsUrl, String shortSmsUrl,
                                              Integer messageLength, Integer shortMessageLength,
                                              Boolean recommendShort) {
        return FamilyInviteResponse.builder()
                .method("sms")
                .maskedContact(maskedContact)
                .memorialId(memorialId)
                .relationshipDisplayName(relationshipDisplayName)
                .token(token)
                .smsUrl(smsUrl)
                .shortSmsUrl(shortSmsUrl)
                .messageLength(messageLength)
                .shortMessageLength(shortMessageLength)
                .recommendShort(recommendShort)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // 이메일 응답용 팩토리 메서드
    public static FamilyInviteResponse forEmail(String maskedContact, Long memorialId, 
                                                String relationshipDisplayName) {
        return FamilyInviteResponse.builder()
                .method("email")
                .maskedContact(maskedContact)
                .memorialId(memorialId)
                .relationshipDisplayName(relationshipDisplayName)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}