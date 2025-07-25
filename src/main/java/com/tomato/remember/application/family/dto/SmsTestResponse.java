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
@Schema(description = "SMS 테스트 응답")
public class SmsTestResponse {

    @Schema(description = "마스킹된 전화번호")
    private String maskedPhoneNumber;

    @Schema(description = "SMS 앱 실행 URL")
    private String smsUrl;

    @Schema(description = "테스트 메시지")
    private String testMessage;

    @Schema(description = "메시지 길이")
    private Integer messageLength;

    @Schema(description = "메시지 길이 정보")
    private SmsAppDataResponse.TextLengthInfo lengthInfo;
}