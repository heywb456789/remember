package com.tomato.remember.application.family.service;

import com.tomato.remember.application.family.dto.FamilyInviteRequest;
import com.tomato.remember.application.family.entity.FamilyInviteToken;
import com.tomato.remember.application.family.repository.FamilyInviteTokenRepository;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.repository.MemorialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 가족 초대 처리 서비스 (단순화된 버전)
 * - 초대 토큰 관리
 * - 초대 발송 처리
 * - sessionStorage 기반 토큰 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyInviteService {

    private final FamilyInviteTokenRepository inviteTokenRepository;
    private final MemorialRepository memorialRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    /**
     * 가족 구성원 초대 발송
     */
    @Transactional
    public String sendFamilyInvite(Member inviter, FamilyInviteRequest request) {
        log.info("가족 구성원 초대 발송 시작 - 초대자: {}, 메모리얼: {}, 연락처: {}",
                inviter.getId(), request.getMemorialId(), maskContact(request.getContact()));

        try {
            // 1. 메모리얼 조회 및 권한 확인
            Memorial memorial = validateMemorialAccess(inviter, request.getMemorialId());

            // 2. 연락처 유효성 검사
            validateContact(request.getMethod(), request.getContact());

            // 3. 중복 초대 확인
            checkDuplicateInvite(memorial, request.getContact());

            // 4. 초대 토큰 생성
            FamilyInviteToken inviteToken = createInviteToken(memorial, inviter, request);

            // 5. 초대 발송 처리
            String result = processInviteSending(inviteToken);

            log.info("가족 구성원 초대 발송 완료 - 토큰: {}, 메모리얼: {}, 연락처: {}",
                    inviteToken.getToken().substring(0, 8) + "...",
                    memorial.getId(),
                    maskContact(request.getContact()));

            return result;

        } catch (Exception e) {
            log.error("가족 구성원 초대 발송 실패 - 초대자: {}, 메모리얼: {}",
                    inviter.getId(), request.getMemorialId(), e);
            throw e;
        }
    }

    /**
     * SMS 앱 연동 데이터 조회
     */
    public Map<String, Object> getSmsAppData(String token) {
        log.info("SMS 앱 연동 데이터 조회 - 토큰: {}", token.substring(0, 8) + "...");

        Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            log.warn("SMS 앱 데이터 조회 실패 - 토큰을 찾을 수 없음: {}", token.substring(0, 8) + "...");
            return Map.of("error", "토큰을 찾을 수 없습니다.");
        }

        FamilyInviteToken inviteToken = tokenOpt.get();

        if (!inviteToken.isSmsMethod()) {
            log.warn("SMS 앱 데이터 조회 실패 - SMS 방식이 아님: {}", token.substring(0, 8) + "...");
            return Map.of("error", "SMS 방식이 아닙니다.");
        }

        return smsService.createSmsAppData(inviteToken);
    }

    /**
     * 초대 토큰 상태 확인
     */
    public boolean isTokenValid(String token) {
        Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findUsableToken(token, LocalDateTime.now());
        return tokenOpt.isPresent();
    }

    /**
     * 초대 토큰으로 초대 처리 (sessionStorage 기반)
     */
    @Transactional
    public String processInviteByToken(String token, Member acceptingMember) {
        log.info("초대 토큰 처리 시작 - 토큰: {}, 수락자: {}",
                token.substring(0, 8) + "...", acceptingMember.getId());

        try {
            // 1. 토큰 유효성 검증
            Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findUsableToken(token, LocalDateTime.now());

            if (tokenOpt.isEmpty()) {
                throw new IllegalArgumentException("유효하지 않거나 만료된 초대 토큰입니다.");
            }

            FamilyInviteToken inviteToken = tokenOpt.get();

            // 2. 초대 수락 처리
            inviteToken.accept(acceptingMember);
            inviteTokenRepository.save(inviteToken);

            // 3. 가족 구성원 등록 (실제 서비스에서는 FamilyService.acceptInvite 호출)
            // TODO: FamilyService.acceptInvite(inviteToken, acceptingMember) 호출

            log.info("초대 토큰 처리 완료 - 토큰: {}, 수락자: {}, 메모리얼: {}",
                    token.substring(0, 8) + "...",
                    acceptingMember.getId(),
                    inviteToken.getMemorial().getId());

            return String.format("%s 메모리얼에 가족 구성원으로 등록되었습니다.",
                    inviteToken.getMemorialName());

        } catch (Exception e) {
            log.error("초대 토큰 처리 실패 - 토큰: {}, 수락자: {}",
                    token.substring(0, 8) + "...", acceptingMember.getId(), e);
            throw e;
        }
    }

    /**
     * 만료된 토큰 정리 (스케줄러에서 호출)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("만료된 초대 토큰 정리 시작");

        List<FamilyInviteToken> expiredTokens = inviteTokenRepository.findExpiredTokens(LocalDateTime.now());

        int processedCount = 0;
        for (FamilyInviteToken token : expiredTokens) {
            token.expire();
            processedCount++;
        }

        if (processedCount > 0) {
            inviteTokenRepository.saveAll(expiredTokens);
        }

        log.info("만료된 초대 토큰 정리 완료 - 처리된 토큰 수: {}", processedCount);
    }

    // ===== 헬퍼 메서드 =====

    /**
     * 메모리얼 접근 권한 검증
     */
    private Memorial validateMemorialAccess(Member inviter, Long memorialId) {
        Memorial memorial = memorialRepository.findByIdAndOwner(memorialId, inviter)
                .orElseThrow(() -> new IllegalArgumentException("메모리얼을 찾을 수 없거나 접근 권한이 없습니다."));

        if (!memorial.isActive()) {
            throw new IllegalArgumentException("비활성화된 메모리얼입니다.");
        }

        return memorial;
    }

    /**
     * 연락처 유효성 검사
     */
    private void validateContact(String method, String contact) {
        if ("email".equals(method)) {
            if (!emailService.canSendEmail(contact)) {
                throw new IllegalArgumentException("유효하지 않은 이메일 주소입니다.");
            }
        } else if ("sms".equals(method)) {
            if (!smsService.isValidPhoneNumber(contact)) {
                throw new IllegalArgumentException("유효하지 않은 전화번호입니다.");
            }
        } else {
            throw new IllegalArgumentException("지원하지 않는 초대 방법입니다.");
        }
    }

    /**
     * 중복 초대 확인
     */
    private void checkDuplicateInvite(Memorial memorial, String contact) {
        List<FamilyInviteToken> existingTokens = inviteTokenRepository
                .findPendingTokensByMemorialAndContact(memorial, contact, LocalDateTime.now());

        if (!existingTokens.isEmpty()) {
            throw new IllegalArgumentException("이미 초대가 진행 중인 연락처입니다.");
        }
    }

    /**
     * 초대 토큰 생성
     */
    private FamilyInviteToken createInviteToken(Memorial memorial, Member inviter, FamilyInviteRequest request) {
        FamilyInviteToken inviteToken = FamilyInviteToken.createInviteToken(
                memorial,
                inviter,
                request.getRelationship(),
                request.getMethod(),
                request.getContact(),
                request.getMessage()
        );

        return inviteTokenRepository.save(inviteToken);
    }

    /**
     * 초대 발송 처리
     */
    private String processInviteSending(FamilyInviteToken inviteToken) {
        if (inviteToken.isEmailMethod()) {
            return processEmailInvite(inviteToken);
        } else if (inviteToken.isSmsMethod()) {
            return processSmsInvite(inviteToken);
        } else {
            throw new IllegalArgumentException("지원하지 않는 초대 방법입니다.");
        }
    }

    /**
     * 이메일 초대 처리
     */
    private String processEmailInvite(FamilyInviteToken inviteToken) {
        try {
            emailService.sendFamilyInviteEmail(inviteToken);
            return "이메일로 초대 링크가 발송되었습니다.";
        } catch (Exception e) {
            log.error("이메일 초대 발송 실패 - 토큰: {}",
                    inviteToken.getToken().substring(0, 8) + "...", e);

            // 토큰 삭제 (발송 실패 시)
            inviteTokenRepository.delete(inviteToken);

            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * SMS 초대 처리
     */
    private String processSmsInvite(FamilyInviteToken inviteToken) {
        try {
            // SMS는 실제 발송하지 않고, 앱 연동용 데이터만 생성
            Map<String, Object> smsData = smsService.createSmsAppData(inviteToken);

            log.info("SMS 초대 데이터 생성 완료 - 토큰: {}, 메시지 길이: {}자",
                    inviteToken.getToken().substring(0, 8) + "...",
                    smsData.get("messageLength"));

            return "SMS 초대 준비가 완료되었습니다. 문자 앱을 통해 발송해 주세요.";

        } catch (Exception e) {
            log.error("SMS 초대 데이터 생성 실패 - 토큰: {}",
                    inviteToken.getToken().substring(0, 8) + "...", e);

            // 토큰 삭제 (실패 시)
            inviteTokenRepository.delete(inviteToken);

            throw new RuntimeException("SMS 초대 데이터 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 연락처 마스킹
     */
    private String maskContact(String contact) {
        if (contact == null || contact.length() < 4) {
            return "****";
        }

        if (contact.contains("@")) {
            // 이메일 마스킹
            int atIndex = contact.indexOf('@');
            if (atIndex > 2) {
                return contact.substring(0, 2) + "**" + contact.substring(atIndex);
            }
            return "**" + contact.substring(atIndex);
        } else {
            // 전화번호 마스킹
            return contact.substring(0, 3) + "****" + contact.substring(contact.length() - 4);
        }
    }
}