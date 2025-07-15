package com.tomato.remember.application.family.service;

import com.tomato.remember.application.family.dto.FamilyInviteRequest;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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