package com.tomato.remember.application.family.entity;

import com.tomato.remember.application.family.code.InviteStatus;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.common.audit.Audit;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 가족 초대 토큰 엔티티
 * - 초대 링크 관리
 * - 초대 상태 추적
 * - 만료 시간 관리
 */
@Slf4j
@Table(
    name = "t_family_invite_token",
    indexes = {
        @Index(name = "idx01_t_family_invite_token", columnList = "token"),
        @Index(name = "idx02_t_family_invite_token", columnList = "memorial_id"),
        @Index(name = "idx03_t_family_invite_token", columnList = "inviter_id"),
        @Index(name = "idx04_t_family_invite_token", columnList = "status"),
        @Index(name = "idx05_t_family_invite_token", columnList = "expires_at"),
        @Index(name = "idx06_t_family_invite_token", columnList = "contact")
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyInviteToken extends Audit {

    // ===== 토큰 정보 =====
    
    @Comment("초대 토큰 (UUID 기반)")
    @Column(nullable = false, unique = true, length = 100)
    private String token;
    
    @Comment("초대 상태")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InviteStatus status = InviteStatus.PENDING;
    
    @Comment("만료 시간")
    @Column(nullable = false, name = "expires_at")
    private LocalDateTime expiresAt;
    
    // ===== 초대 관련 정보 =====
    
    @Comment("메모리얼")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memorial_id", nullable = false)
    private Memorial memorial;
    
    @Comment("초대자")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private Member inviter;
    
    @Comment("고인과의 관계")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Relationship relationship;
    
    // ===== 연락처 정보 =====
    
    @Comment("초대 방법 (email, sms)")
    @Column(nullable = false, length = 10)
    private String method;
    
    @Comment("연락처 (이메일 또는 전화번호)")
    @Column(nullable = false, length = 100)
    private String contact;
    
    @Comment("초대 메시지")
    @Column(length = 500, name = "invite_message")
    private String inviteMessage;
    
    // ===== 처리 정보 =====
    
    @Comment("수락한 회원 (수락 후 설정)")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_member_id")
    private Member acceptedMember;
    
    @Comment("수락 일시")
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
    
    @Comment("거절 일시")
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
    
    @Comment("사용 일시 (링크 클릭 시)")
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    // ===== 비즈니스 메서드 =====
    
    /**
     * 토큰 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * 토큰 사용 가능 여부 확인
     */
    public boolean isUsable() {
        return status == InviteStatus.PENDING && !isExpired();
    }
    
    /**
     * 토큰 사용 처리
     */
    public void markAsUsed() {
        this.usedAt = LocalDateTime.now();
        log.info("초대 토큰 사용 처리 - 토큰: {}, 메모리얼: {}", 
                token.substring(0, 8) + "...", memorial.getId());
    }
    
    /**
     * 초대 수락 처리
     */
    public void accept(Member member) {
        if (!isUsable()) {
            throw new IllegalStateException("사용할 수 없는 토큰입니다.");
        }
        
        this.status = InviteStatus.ACCEPTED;
        this.acceptedMember = member;
        this.acceptedAt = LocalDateTime.now();
        
        log.info("초대 수락 처리 - 토큰: {}, 수락자: {}, 메모리얼: {}", 
                token.substring(0, 8) + "...", member.getId(), memorial.getId());
    }
    
    /**
     * 초대 거절 처리
     */
    public void reject() {
        if (!isUsable()) {
            throw new IllegalStateException("사용할 수 없는 토큰입니다.");
        }
        
        this.status = InviteStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
        
        log.info("초대 거절 처리 - 토큰: {}, 메모리얼: {}", 
                token.substring(0, 8) + "...", memorial.getId());
    }
    
    /**
     * 초대 만료 처리
     */
    public void expire() {
        this.status = InviteStatus.EXPIRED;
        
        log.info("초대 만료 처리 - 토큰: {}, 메모리얼: {}", 
                token.substring(0, 8) + "...", memorial.getId());
    }
    
    /**
     * 초대 취소 처리
     */
    public void cancel() {
        this.status = InviteStatus.CANCELLED;
        
        log.info("초대 취소 처리 - 토큰: {}, 메모리얼: {}", 
                token.substring(0, 8) + "...", memorial.getId());
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
    
    // ===== 팩토리 메서드 =====
    
    /**
     * 초대 토큰 생성
     */
    public static FamilyInviteToken createInviteToken(Memorial memorial, Member inviter, 
                                                      Relationship relationship, String method, 
                                                      String contact, String inviteMessage) {
        
        // 토큰 생성 (UUID 기반)
        String token = generateToken();
        
        // 만료 시간 설정 (7일 후)
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        
        return FamilyInviteToken.builder()
                .token(token)
                .memorial(memorial)
                .inviter(inviter)
                .relationship(relationship)
                .method(method)
                .contact(contact)
                .inviteMessage(inviteMessage)
                .expiresAt(expiresAt)
                .status(InviteStatus.PENDING)
                .build();
    }
    
    /**
     * 토큰 생성 (UUID 기반)
     */
    private static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") + 
               System.currentTimeMillis();
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
    
    // ===== 정보 조회 메서드 =====
    
    /**
     * 초대자 이름 조회
     */
    public String getInviterName() {
        return inviter != null ? inviter.getName() : "알 수 없음";
    }
    
    /**
     * 메모리얼 이름 조회
     */
    public String getMemorialName() {
        return memorial != null ? memorial.getName() : "알 수 없음";
    }
    
    /**
     * 관계 표시명 조회
     */
    public String getRelationshipDisplayName() {
        return relationship != null ? relationship.getDisplayName() : "미설정";
    }
    
    /**
     * 상태 표시명 조회
     */
    public String getStatusDisplayName() {
        return status.getDisplayName();
    }
}