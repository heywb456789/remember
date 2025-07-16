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

/**
 * 가족 구성원 엔티티 (Memorial과 Member 사이의 중간 테이블)
 */
@Slf4j
@Table(
        name = "t_family_member",
        indexes = {
                @Index(name = "idx01_t_family_member", columnList = "memorial_id, invite_status"),
                @Index(name = "idx02_t_family_member", columnList = "member_id, invite_status"),
                @Index(name = "idx03_t_family_member", columnList = "invited_by_id"),
                @Index(name = "idx04_t_family_member", columnList = "relationship"),
                @Index(name = "idx05_t_family_member", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk01_t_family_member", columnNames = {"memorial_id", "member_id"})
        }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMember extends Audit {

    // ===== 연관관계 =====

    @Comment("메모리얼")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memorial_id", nullable = false)
    private Memorial memorial;

    @Comment("가족 구성원")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Comment("초대한 사람")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id", nullable = false)
    private Member invitedBy;

    // ===== 관계 정보 =====

    @Comment("가족 구성원과 고인의 관계")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Relationship relationship;

    @Comment("초대 상태")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "invite_status")
    @Builder.Default
    private InviteStatus inviteStatus = InviteStatus.PENDING;

    // ===== 권한 설정 =====

    @Comment("메모리얼 접근 권한")
    @Column(nullable = false, name = "memorial_access")
    @Builder.Default
    private Boolean memorialAccess = true;

    @Comment("영상통화 접근 권한")
    @Column(nullable = false, name = "video_call_access")
    @Builder.Default
    private Boolean videoCallAccess = true;

    // ===== 초대 관련 정보 =====

    @Comment("초대 메시지")
    @Column(length = 500, name = "invite_message")
    private String inviteMessage;

    @Comment("초대 수락 일시")
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Comment("초대 거절 일시")
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Comment("초대 만료 일시")
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Comment("마지막 접근 일시")
    @Column(name = "last_access_at")
    private LocalDateTime lastAccessAt;

    // ===== 고인 상세 정보 (가족 구성원별 개별 입력) =====

    @Comment("성격이나 특징 (가족 구성원 관점)")
    @Column(length = 500, name = "member_personality")
    private String memberPersonality;

    @Comment("취미나 관심사 (가족 구성원 관점)")
    @Column(length = 300, name = "member_hobbies")
    private String memberHobbies;

    @Comment("좋아하는 음식 (가족 구성원 관점)")
    @Column(length = 300, name = "member_favorite_food")
    private String memberFavoriteFood;

    @Comment("기억에 남는 일화나 추억 (가족 구성원 관점)")
    @Column(length = 300, name = "member_special_memories")
    private String memberSpecialMemories;

    @Comment("습관이나 말버릇 (가족 구성원 관점)")
    @Column(length = 300, name = "member_speech_habits")
    private String memberSpeechHabits;

    // ===== Setter 메서드 (연관관계 관리용) =====

    /**
     * 메모리얼 설정 (연관관계 관리용)
     */
    public void setMemorial(Memorial memorial) {
        this.memorial = memorial;
    }

    // ===== 비즈니스 메서드 =====

    /**
     * 초대 수락
     */
    public void acceptInvite() {
        this.inviteStatus = InviteStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
        this.rejectedAt = null;
        log.info("가족 초대 수락 - 멤버: {}, 메모리얼: {}", member.getId(), memorial.getId());
    }

    /**
     * 초대 거절
     */
    public void rejectInvite() {
        this.inviteStatus = InviteStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
        this.acceptedAt = null;
        log.info("가족 초대 거절 - 멤버: {}, 메모리얼: {}", member.getId(), memorial.getId());
    }

    /**
     * 초대 취소
     */
    public void cancelInvite() {
        this.inviteStatus = InviteStatus.CANCELLED;
        log.info("가족 초대 취소 - 멤버: {}, 메모리얼: {}", member.getId(), memorial.getId());
    }

    /**
     * 초대 만료
     */
    public void expireInvite() {
        this.inviteStatus = InviteStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
        log.info("가족 초대 만료 - 멤버: {}, 메모리얼: {}", member.getId(), memorial.getId());
    }

    /**
     * 메모리얼 접근 권한 변경
     */
    public void updateMemorialAccess(boolean access) {
        this.memorialAccess = access;
        log.info("메모리얼 접근 권한 변경 - 멤버: {}, 메모리얼: {}, 권한: {}",
                member.getId(), memorial.getId(), access);
    }

    /**
     * 영상통화 권한 변경
     */
    public void updateVideoCallAccess(boolean access) {
        this.videoCallAccess = access;
        log.info("영상통화 권한 변경 - 멤버: {}, 메모리얼: {}, 권한: {}",
                member.getId(), memorial.getId(), access);
    }

    /**
     * 마지막 접근 시간 업데이트
     */
    public void updateLastAccess() {
        this.lastAccessAt = LocalDateTime.now();
    }

    // ===== 고인 상세 정보 업데이트 =====

    /**
     * 고인 상세 정보 업데이트
     */
    public void updateDeceasedInfo(String personality, String hobbies, String favoriteFood,
                                   String specialMemories, String speechHabits) {
        this.memberPersonality = personality;
        this.memberHobbies = hobbies;
        this.memberFavoriteFood = favoriteFood;
        this.memberSpecialMemories = specialMemories;
        this.memberSpeechHabits = speechHabits;

        log.info("고인 상세 정보 업데이트 - 멤버: {}, 메모리얼: {}", member.getId(), memorial.getId());
    }

    /**
     * 고인 상세 정보 완성 여부 확인
     */
    public boolean hasDeceasedInfo() {
        return (memberPersonality != null && !memberPersonality.trim().isEmpty()) ||
               (memberHobbies != null && !memberHobbies.trim().isEmpty()) ||
               (memberFavoriteFood != null && !memberFavoriteFood.trim().isEmpty()) ||
               (memberSpecialMemories != null && !memberSpecialMemories.trim().isEmpty()) ||
               (memberSpeechHabits != null && !memberSpeechHabits.trim().isEmpty());
    }

    /**
     * 고인 상세 정보 필수 항목 완성 여부 확인 (최소 2개 이상)
     */
    public boolean hasRequiredDeceasedInfo() {
        int filledCount = 0;

        if (memberPersonality != null && !memberPersonality.trim().isEmpty()) filledCount++;
        if (memberHobbies != null && !memberHobbies.trim().isEmpty()) filledCount++;
        if (memberFavoriteFood != null && !memberFavoriteFood.trim().isEmpty()) filledCount++;
        if (memberSpecialMemories != null && !memberSpecialMemories.trim().isEmpty()) filledCount++;
        if (memberSpeechHabits != null && !memberSpeechHabits.trim().isEmpty()) filledCount++;

        return filledCount >= 5;  // 5개 모두 있어야함
    }

    // ===== 상태 확인 메서드 =====

    /**
     * 활성 가족 구성원인지 확인
     */
    public boolean isActive() {
        return inviteStatus.isActive();
    }

    /**
     * 초대 대기 중인지 확인
     */
    public boolean isPending() {
        return inviteStatus.isPending();
    }

    /**
     * 메모리얼 접근 가능한지 확인
     */
    public boolean canAccessMemorial() {
        return isActive() && memorialAccess;
    }

    /**
     * 영상통화 접근 가능한지 확인
     */
    public boolean canAccessVideoCall() {
        return canAccessMemorial() && videoCallAccess;
    }

    /**
     * 초대 응답 가능한지 확인
     */
    public boolean canRespond() {
        return inviteStatus == InviteStatus.PENDING;
    }

    /**
     * 권한 수정 가능한지 확인
     */
    public boolean canModifyPermissions() {
        return isActive();
    }

    // ===== 정보 조회 메서드 =====

    /**
     * 관계 표시명 조회
     */
    public String getRelationshipDisplayName() {
        return relationship != null ? relationship.getDisplayName() : "미설정";
    }

    /**
     * 초대 상태 표시명 조회
     */
    public String getInviteStatusDisplayName() {
        return inviteStatus.getDisplayName();
    }

    /**
     * 멤버 이름 조회
     */
    public String getMemberName() {
        return member != null ? member.getName() : "알 수 없음";
    }

    /**
     * 메모리얼 이름 조회
     */
    public String getMemorialName() {
        return memorial != null ? memorial.getName() : "알 수 없음";
    }

    /**
     * 초대자 이름 조회
     */
    public String getInviterName() {
        return invitedBy != null ? invitedBy.getName() : "알 수 없음";
    }

    // ===== 팩토리 메서드 =====

    /**
     * 가족 구성원 초대 생성
     */
    public static FamilyMember createInvite(Memorial memorial, Member member, Member invitedBy,
                                            Relationship relationship, String inviteMessage) {
        return FamilyMember.builder()
                .memorial(memorial)
                .member(member)
                .invitedBy(invitedBy)
                .relationship(relationship)
                .inviteMessage(inviteMessage)
                .inviteStatus(InviteStatus.PENDING)
                .memorialAccess(true)
                .videoCallAccess(true)
                .build();
    }

    /**
     * 직접 가족 구성원 등록 (승인 없이)
     */
    public static FamilyMember createDirectMember(Memorial memorial, Member member, Member invitedBy,
                                                  Relationship relationship) {
        return FamilyMember.builder()
                .memorial(memorial)
                .member(member)
                .invitedBy(invitedBy)
                .relationship(relationship)
                .inviteStatus(InviteStatus.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .memorialAccess(true)
                .videoCallAccess(true)
                .build();
    }
}