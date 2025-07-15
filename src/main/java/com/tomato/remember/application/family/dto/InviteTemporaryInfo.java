package com.tomato.remember.application.family.dto;

import com.tomato.remember.application.member.code.Relationship;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 초대 임시 정보 DTO
 * - 로그인/회원가입 시 임시 저장용
 * - Redis 또는 세션에 저장
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteTemporaryInfo {
    
    /**
     * 초대 토큰
     */
    private String token;
    
    /**
     * 메모리얼 ID
     */
    private Long memorialId;
    
    /**
     * 메모리얼 이름
     */
    private String memorialName;
    
    /**
     * 초대자 ID
     */
    private Long inviterId;
    
    /**
     * 초대자 이름
     */
    private String inviterName;
    
    /**
     * 고인과의 관계
     */
    private Relationship relationship;
    
    /**
     * 관계 표시명
     */
    private String relationshipDisplayName;
    
    /**
     * 초대 메시지
     */
    private String inviteMessage;
    
    /**
     * 연락처 (이메일 또는 전화번호)
     */
    private String contact;
    
    /**
     * 초대 방법 (email, sms)
     */
    private String method;
    
    /**
     * 토큰 만료 시간
     */
    private LocalDateTime expiresAt;
    
    /**
     * 임시 저장 시간
     */
    private LocalDateTime storedAt;
    
    /**
     * 처리 완료 여부
     */
    private boolean processed;
    
    // ===== 유틸리티 메서드 =====
    
    /**
     * 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * 임시 저장 만료 여부 확인 (24시간)
     */
    public boolean isStorageExpired() {
        return LocalDateTime.now().isAfter(storedAt.plusHours(24));
    }
    
    /**
     * 유효성 확인
     */
    public boolean isValid() {
        return !isExpired() && !isStorageExpired() && !processed;
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
    
    /**
     * 처리 완료 표시
     */
    public InviteTemporaryInfo markAsProcessed() {
        return InviteTemporaryInfo.builder()
                .token(this.token)
                .memorialId(this.memorialId)
                .memorialName(this.memorialName)
                .inviterId(this.inviterId)
                .inviterName(this.inviterName)
                .relationship(this.relationship)
                .relationshipDisplayName(this.relationshipDisplayName)
                .inviteMessage(this.inviteMessage)
                .contact(this.contact)
                .method(this.method)
                .expiresAt(this.expiresAt)
                .storedAt(this.storedAt)
                .processed(true)
                .build();
    }
    
    /**
     * 현재 시간으로 저장 시간 설정
     */
    public static InviteTemporaryInfo withCurrentStoredTime(InviteTemporaryInfo info) {
        return InviteTemporaryInfo.builder()
                .token(info.token)
                .memorialId(info.memorialId)
                .memorialName(info.memorialName)
                .inviterId(info.inviterId)
                .inviterName(info.inviterName)
                .relationship(info.relationship)
                .relationshipDisplayName(info.relationshipDisplayName)
                .inviteMessage(info.inviteMessage)
                .contact(info.contact)
                .method(info.method)
                .expiresAt(info.expiresAt)
                .storedAt(LocalDateTime.now())
                .processed(false)
                .build();
    }
    
    // ===== 정보 조회 메서드 =====
    
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
     * 초대 요약 정보
     */
    public String getSummary() {
        return String.format("[%s]님이 [%s] 메모리얼에 [%s] 관계로 초대", 
                inviterName, memorialName, relationshipDisplayName);
    }
    
    /**
     * 연락처 마스킹
     */
    public String getMaskedContact() {
        if (isEmailMethod()) {
            return maskEmail(contact);
        } else if (isSmsMethod()) {
            return maskPhoneNumber(contact);
        }
        return "****";
    }
    
    // ===== 마스킹 유틸리티 =====
    
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
}