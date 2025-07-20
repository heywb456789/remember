package com.tomato.remember.application.auth.service;

import com.tomato.remember.application.auth.dto.PasswordResetOneIdResponse;
import com.tomato.remember.application.auth.dto.PasswordResetRequestDTO;
import com.tomato.remember.application.auth.dto.PasswordResetResponseDTO;
import com.tomato.remember.application.auth.dto.PasswordResetVerifyResponse;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.member.repository.MemberRepository;
import com.tomato.remember.application.oneld.dto.OneIdResponse;
import com.tomato.remember.application.oneld.dto.OneIdVerifyResponse;
import com.tomato.remember.application.oneld.service.TomatoAuthService;
import com.tomato.remember.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetServiceImpl implements PasswordResetService {

    private final TomatoAuthService tomatoAuthService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PasswordResetResponseDTO sendVerificationCode(PasswordResetRequestDTO request) {
        log.info("비밀번호 재설정 인증번호 발송 요청: {}", request.getPhoneNumber());

        try {
            // 1. 먼저 해당 휴대폰 번호로 가입된 사용자가 있는지 확인
            Optional<Member> memberOpt = memberRepository.findByPhoneNumber(request.getPhoneNumber());

            if (memberOpt.isEmpty()) {
                log.warn("가입되지 않은 휴대폰 번호: {}", request.getPhoneNumber());
                return PasswordResetResponseDTO.sendFailed("가입되지 않은 휴대폰 번호입니다");
            }

            // 2. One-ID 인증번호 발송 요청
            PasswordResetOneIdResponse response = tomatoAuthService.sendPasswordResetCert(request).block();

            if (response != null && response.isSuccess()) {
                log.info("비밀번호 재설정 인증번호 발송 성공: {}", request.getPhoneNumber());
                return PasswordResetResponseDTO.sendSuccess();
            } else {
                log.warn("One-ID 인증번호 발송 실패: {}", response != null ? response.getMessage() : "Unknown error");
                return PasswordResetResponseDTO.sendFailed("인증번호 발송에 실패했습니다");
            }

        } catch (Exception e) {
            log.error("비밀번호 재설정 인증번호 발송 오류: {}", e.getMessage(), e);
            return PasswordResetResponseDTO.sendFailed("인증번호 발송 중 오류가 발생했습니다");
        }
    }

    @Override
    public PasswordResetResponseDTO verifyCode(PasswordResetRequestDTO request) {
        log.info("비밀번호 재설정 인증번호 확인: {}", request.getPhoneNumber());

        try {
            // 인증번호 확인
            PasswordResetVerifyResponse response = tomatoAuthService.verifyPasswordResetCert(request).block();

            if (response != null && response.isVerified()) {
                log.info("비밀번호 재설정 인증번호 확인 성공: {}", request.getPhoneNumber());
                return PasswordResetResponseDTO.verifySuccess();
            } else {
                log.warn("비밀번호 재설정 인증번호 확인 실패: {}", request.getPhoneNumber());
                return PasswordResetResponseDTO.verifyFailed();
            }

        } catch (Exception e) {
            log.error("비밀번호 재설정 인증번호 확인 오류: {}", e.getMessage(), e);
            return PasswordResetResponseDTO.verifyFailed();
        }
    }

    @Override
    @Transactional
    public PasswordResetResponseDTO resetPassword(PasswordResetRequestDTO request) {
        log.info("비밀번호 재설정 실행: {}", request.getPhoneNumber());

        try {
            // 1. One-ID 비밀번호 재설정
            PasswordResetVerifyResponse oneIdResponse = tomatoAuthService.resetPassword(request).block();

            if (oneIdResponse == null || !oneIdResponse.isVerified()) {
                log.warn("One-ID 비밀번호 재설정 실패: {}", request.getPhoneNumber());
                return PasswordResetResponseDTO.resetFailed("One-ID 비밀번호 재설정에 실패했습니다");
            }

            // 2. 로컬 DB 비밀번호도 업데이트
            Optional<Member> memberOpt = memberRepository.findByPhoneNumber(request.getPhoneNumber());

            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();
                String encodedPassword = passwordEncoder.encode(request.getNewPassword());
                member.updatePassword(encodedPassword);
                memberRepository.save(member);

                log.info("로컬 DB 비밀번호 업데이트 완료: {} (ID: {})",
                        member.getName(), member.getId());
            } else {
                log.warn("로컬 DB에 사용자가 존재하지 않음: {}", request.getPhoneNumber());
                // One-ID는 성공했으나 로컬 DB에 사용자가 없는 경우
                // 이 경우에도 성공으로 처리 (One-ID가 메인이므로)
            }

            log.info("비밀번호 재설정 완료: {}", request.getPhoneNumber());
            return PasswordResetResponseDTO.resetSuccess();

        } catch (Exception e) {
            log.error("비밀번호 재설정 오류: {}", e.getMessage(), e);
            return PasswordResetResponseDTO.resetFailed("비밀번호 재설정 중 오류가 발생했습니다");
        }
    }
}