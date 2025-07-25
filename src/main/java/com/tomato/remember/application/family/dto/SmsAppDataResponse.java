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
@Schema(description = "SMS 앱 연동 데이터 응답")
public class SmsAppDataResponse {

    @Schema(description = "마스킹된 전화번호")
    private String maskedPhoneNumber;

    @Schema(description = "일반 SMS 메시지")
    private String message;

    @Schema(description = "짧은 SMS 메시지")
    private String shortMessage;

    @Schema(description = "SMS 앱 실행 URL")
    private String smsUrl;

    @Schema(description = "짧은 SMS URL")
    private String shortSmsUrl;

    @Schema(description = "메시지 길이")
    private Integer messageLength;

    @Schema(description = "짧은 메시지 길이")
    private Integer shortMessageLength;

    @Schema(description = "최대 메시지 길이")
    private Integer maxLength;

    @Schema(description = "짧은 메시지 권장 여부")
    private Boolean recommendShort;

    @Schema(description = "메시지 길이 정보")
    private TextLengthInfo lengthInfo;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextLengthInfo {
        private Integer length;
        private Boolean isTooLong;
        private Integer maxLength;
        private Integer remaining;
    }
}