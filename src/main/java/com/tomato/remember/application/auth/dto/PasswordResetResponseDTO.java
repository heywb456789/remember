package com.tomato.remember.application.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "비밀번호 재설정 응답 DTO")
public class PasswordResetResponseDTO {

    @Schema(description = "처리 결과", example = "true")
    private boolean success;

    @Schema(description = "메시지", example = "인증번호가 발송되었습니다")
    private String message;

    @Schema(description = "사용자 존재 여부 (인증번호 요청 시)", example = "true")
    private Boolean userExists;

    @Schema(description = "인증 성공 여부 (인증번호 확인 시)", example = "true")
    private Boolean verified;

    // 정적 팩토리 메서드들
    public static PasswordResetResponseDTO sendSuccess() {
        return PasswordResetResponseDTO.builder()
                .success(true)
                .message("인증번호가 발송되었습니다")
                .userExists(true)
                .build();
    }

    public static PasswordResetResponseDTO sendFailed(String message) {
        return PasswordResetResponseDTO.builder()
                .success(false)
                .message(message)
                .userExists(false)
                .build();
    }

    public static PasswordResetResponseDTO verifySuccess() {
        return PasswordResetResponseDTO.builder()
                .success(true)
                .message("인증이 완료되었습니다")
                .verified(true)
                .build();
    }

    public static PasswordResetResponseDTO verifyFailed() {
        return PasswordResetResponseDTO.builder()
                .success(false)
                .message("인증번호가 올바르지 않습니다")
                .verified(false)
                .build();
    }

    public static PasswordResetResponseDTO resetSuccess() {
        return PasswordResetResponseDTO.builder()
                .success(true)
                .message("비밀번호가 성공적으로 변경되었습니다")
                .build();
    }

    public static PasswordResetResponseDTO resetFailed(String message) {
        return PasswordResetResponseDTO.builder()
                .success(false)
                .message(message)
                .build();
    }
}