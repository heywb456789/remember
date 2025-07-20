package com.tomato.remember.application.auth.service;

import com.tomato.remember.application.auth.dto.PasswordResetRequestDTO;
import com.tomato.remember.application.auth.dto.PasswordResetResponseDTO;

public interface PasswordResetService {

    /**
     * 비밀번호 재설정 인증번호 발송
     */
    PasswordResetResponseDTO sendVerificationCode(PasswordResetRequestDTO request);

    /**
     * 인증번호 확인
     */
    PasswordResetResponseDTO verifyCode(PasswordResetRequestDTO request);

    /**
     * 비밀번호 재설정
     */
    PasswordResetResponseDTO resetPassword(PasswordResetRequestDTO request);
}