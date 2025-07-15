package com.tomato.remember.application.family.service;

import com.tomato.remember.application.family.dto.FamilyInviteRequest;
import com.tomato.remember.application.family.dto.InviteTemporaryInfo;
import com.tomato.remember.application.family.entity.FamilyInviteToken;
import com.tomato.remember.application.family.repository.FamilyInviteTokenRepository;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.repository.MemorialRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가족 초대 처리 서비스
 * - 초대 토큰 관리
 * - 초대 발송 처리
 * - 임시 초대 정보 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyInviteService {
    private final FamilyInviteTokenRepository inviteTokenRepository;
    private final MemorialRepository memorialRepository;
    private final EmailService emailService;
    private final SmsService smsService;


    // ###################################################################
    // New 시작
    // ###################################################################
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
     * 초대 토큰으로 임시 정보 조회
     */
    public InviteTemporaryInfo getInviteTemporaryInfo(String token) {
        log.info("초대 임시 정보 조회 - 토큰: {}", token.substring(0, 8) + "...");

        Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findUsableToken(token, LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
            log.warn("유효하지 않은 초대 토큰 - 토큰: {}", token.substring(0, 8) + "...");
            return null;
        }

        FamilyInviteToken inviteToken = tokenOpt.get();

        // 토큰 사용 표시
        inviteToken.markAsUsed();
        inviteTokenRepository.save(inviteToken);

        // 임시 정보 생성
        InviteTemporaryInfo tempInfo = InviteTemporaryInfo.builder()
                .token(inviteToken.getToken())
                .memorialId(inviteToken.getMemorial().getId())
                .memorialName(inviteToken.getMemorialName())
                .inviterId(inviteToken.getInviter().getId())
                .inviterName(inviteToken.getInviterName())
                .relationship(inviteToken.getRelationship())
                .relationshipDisplayName(inviteToken.getRelationshipDisplayName())
                .inviteMessage(inviteToken.getInviteMessage())
                .contact(inviteToken.getContact())
                .method(inviteToken.getMethod())
                .expiresAt(inviteToken.getExpiresAt())
                .storedAt(LocalDateTime.now())
                .processed(false)
                .build();

        log.info("초대 임시 정보 생성 완료 - 토큰: {}, 메모리얼: {}",
                token.substring(0, 8) + "...", tempInfo.getMemorialId());

        return tempInfo;
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

    // ###################################################################
    // 헬퍼 메소드
    // ###################################################################
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

    // ###################################################################
    // New End
    // ###################################################################

    // 임시 초대 정보 저장소 (실제 환경에서는 Redis나 DB 사용)
    private final Map<String, InviteInfo> inviteStorage = new ConcurrentHashMap<>();

    /**
     * 초대 정보 조회
     */
    public InviteInfo getInviteInfo(String inviteToken) {
        log.debug("초대 정보 조회 - 토큰: {}", inviteToken.substring(0, 8) + "...");

        InviteInfo inviteInfo = inviteStorage.get(inviteToken);

        if (inviteInfo == null) {
            log.warn("초대 정보 없음 - 토큰: {}", inviteToken.substring(0, 8) + "...");
            return null;
        }

        // 만료 확인
        if (inviteInfo.isExpired()) {
            log.warn("초대 정보 만료 - 토큰: {}, 만료시간: {}",
                inviteToken.substring(0, 8) + "...", inviteInfo.getExpiresAt());
            inviteStorage.remove(inviteToken);
            return null;
        }

        return inviteInfo;
    }

    /**
     * 초대 정보 삭제
     */
    public void removeInviteInfo(String inviteToken) {
        log.info("초대 정보 삭제 - 토큰: {}", inviteToken.substring(0, 8) + "...");
        
        inviteStorage.remove(inviteToken);
    }

    /**
     * 이메일 초대 발송
     */
    private void sendEmailInvite(String email, String message, Memorial memorial, Member inviter) {
        log.info("이메일 초대 발송 - 수신자: {}, 메모리얼: {}", 
            maskEmail(email), memorial.getId());
        
        // TODO: 실제 이메일 발송 로직 구현
        // - 이메일 템플릿 적용
        // - SMTP 서버 연동
        // - 발송 결과 확인
        
        log.info("이메일 초대 발송 완료 (모의) - 수신자: {}", maskEmail(email));
    }

    /**
     * SMS 초대 발송
     */
    private void sendSmsInvite(String phoneNumber, String message, Memorial memorial, Member inviter) {
        log.info("SMS 초대 발송 - 수신자: {}, 메모리얼: {}", 
            maskPhoneNumber(phoneNumber), memorial.getId());
        
        // TODO: 실제 SMS 발송 로직 구현
        // - SMS API 연동 (예: 알리고, 네이버 클라우드 등)
        // - 문자 길이 제한 고려
        // - 발송 결과 확인
        
        log.info("SMS 초대 발송 완료 (모의) - 수신자: {}", maskPhoneNumber(phoneNumber));
    }

    /**
     * 초대 링크 생성
     */
    private String generateInviteLink(String inviteToken) {
        // TODO: 실제 도메인으로 변경
        String baseUrl = "https://app.tomatoremember.com";
        return baseUrl + "/mobile/family/invite/" + inviteToken;
    }

    /**
     * 만료된 초대 정보 정리 (스케줄러에서 호출)
     */
    public void cleanExpiredInvites() {
        log.info("만료된 초대 정보 정리 시작");
        
        int removedCount = 0;
        LocalDateTime now = LocalDateTime.now();
        
        for (Map.Entry<String, InviteInfo> entry : inviteStorage.entrySet()) {
            if (entry.getValue().getExpiresAt().isBefore(now)) {
                inviteStorage.remove(entry.getKey());
                removedCount++;
            }
        }
        
        log.info("만료된 초대 정보 정리 완료 - 제거된 개수: {}", removedCount);
    }



    // ===== 유틸리티 메서드 =====

    /**
     * 이메일 마스킹
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex > 2) {
            return email.substring(0, 2) + "**" + email.substring(atIndex);
        }
        
        return "**" + email.substring(atIndex);
    }

    /**
     * 전화번호 마스킹
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "****";
        }
        
        if (phoneNumber.contains("-")) {
            String[] parts = phoneNumber.split("-");
            if (parts.length == 3) {
                return parts[0] + "-****-" + parts[2];
            }
        }
        
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    // ===== 내부 클래스 =====

    /**
     * 초대 정보 클래스
     */
    @lombok.Getter
    @lombok.Builder
    public static class InviteInfo {
        private String inviteToken;
        private Memorial memorial;
        private Member inviter;
        private Relationship relationship;
        private String contact;
        private String method;
        private String message;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;

        /**
         * 만료 여부 확인
         */
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        /**
         * 남은 시간 (시간 단위)
         */
        public long getRemainingHours() {
            if (isExpired()) {
                return 0;
            }
            return java.time.Duration.between(LocalDateTime.now(), expiresAt).toHours();
        }

        /**
         * 이메일 방식인지 확인
         */
        public boolean isEmailMethod() {
            return "email".equals(method);
        }

        /**
         * SMS 방식인지 확인
         */
        public boolean isSmsMethod() {
            return "sms".equals(method);
        }
    }
}