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
@Schema(description = "초대 토큰 유효성 응답")
public class TokenValidationResponse {

    @Schema(description = "토큰 유효성")
    private Boolean valid;

    @Schema(description = "토큰 만료 여부")
    private Boolean expired;

    @Schema(description = "토큰 상태", example = "PENDING")
    private String status;

    @Schema(description = "남은 시간 (시간)")
    private Long remainingHours;

    @Schema(description = "만료 시간")
    private String expiresAt;
}