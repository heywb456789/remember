package com.tomato.remember.application.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.remember.application.auth.dto
 * @fileName : LoginRequestDTO
 * @date : 2025-07-14
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "로그인 DTO")
public class LoginRequestDTO {
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
}
