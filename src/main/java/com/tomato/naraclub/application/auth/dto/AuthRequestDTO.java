package com.tomato.naraclub.application.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "인증 요청 DTO")
public class AuthRequestDTO {
    @Schema(
        description = "휴대폰 번호 (하이픈 없이 11자리)",
        example = "01012341234",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^010[0-9]{8}$", message = "올바른 휴대폰 번호 형식이 아닙니다")
    private String phoneNumber;

    @Schema(
        description = "비밀번호",
        example = "password123",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    @Schema(
        description = "자동 로그인 여부",
        example = "true",
        defaultValue = "false",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private boolean autoLogin;

    @Schema(
        description = "SMS 인증번호 (6자리)",
        example = "123456",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 6자리 숫자여야 합니다")
    private String verificationCode;

    @Schema(
        description = "사용자 키",
        example = "user_key_12345",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "사용자 키는 필수입니다")
    private String userKey;

    @Schema(
        description = "사용자 이름",
        example = "홍길동",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "이름은 필수입니다")
    private String name;

    @Schema(
        description = "마케팅 수신 동의 여부",
        example = "true",
        defaultValue = "false",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Boolean marketingAgree;
}