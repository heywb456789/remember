package com.tomato.remember.application.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "비밀번호 재설정 요청 DTO")
public class PasswordResetRequestDTO {

    @Schema(description = "휴대폰 번호", example = "01012345678", required = true)
    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^010\\d{8}$", message = "올바른 휴대폰 번호 형식이 아닙니다")
    private String phoneNumber;

    @Schema(description = "인증번호", example = "123456")
    @Size(min = 6, max = 6, message = "인증번호는 6자리여야 합니다")
    private String verificationCode;

    @Schema(description = "새 비밀번호", example = "newPassword123")
    @Size(min = 4, max = 20, message = "비밀번호는 4-20자 사이여야 합니다")
    private String newPassword;
}