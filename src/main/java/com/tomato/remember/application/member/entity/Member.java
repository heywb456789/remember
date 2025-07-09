package com.tomato.remember.application.member.entity;

import com.tomato.remember.admin.user.dto.AppUserResponse;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.member.dto.MemberDTO;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.videocall.entity.VideoCall;
import com.tomato.remember.common.audit.Audit;
import com.tomato.remember.common.code.MemberRole;
import com.tomato.remember.common.code.MemberStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

@Table(
    name = "t_member",
    indexes = {
        @Index(name = "idx01_t_member", columnList = "created_at"),
        @Index(name = "idx02_t_member", columnList = "user_key"),
        @Index(name = "idx03_t_member", columnList = "phone_number"),
        @Index(name = "idx04_t_member", columnList = "invite_code"),
        @Index(name = "idx05_t_member", columnList = "status"),
        @Index(name = "idx06_t_member", columnList = "role")
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Member extends Audit {

    @Comment("토마토 OneId")
    @Column(unique = true, length = 100)
    private String userKey;

    @Comment("비밀번호")
    @Column(length = 100)
    private String password;

    @Comment("휴대폰 번호")
    @Column(length = 20)
    private String phoneNumber;

    @Comment("초대코드 - 가입시 자동생성")
    @Column(nullable = false, length = 10)
    private String inviteCode;

    @Comment("회원 상태")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    @Comment("회원 권한")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MemberRole role = MemberRole.USER_ACTIVE;

    @Comment("이메일")
    @Column(length = 100)
    private String email;

    @Comment("사용자 명")
    @Column(length = 50, nullable = false)
    private String name;

    @Comment("마지막 접속 시간")
    @Column(name = "last_access_at")
    private LocalDateTime lastAccessAt;

    @Comment("프로필 이미지")
    @Column(name = "profile_img")
    private String profileImg;

    @Comment("추천인")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id")
    private Member inviter;

    @Comment("선호 언어 코드")
    @Column(nullable = false, length = 10, name = "preferred_language")
    @Builder.Default
    private String preferredLanguage = "KO";

    @Comment("마케팅 동의")
    @Column(nullable = false, name = "marketing_agreed")
    @Builder.Default
    private Boolean marketingAgreed = false;

    @Comment("푸시 알림 설정")
    @Column(nullable = false, name = "push_notification")
    @Builder.Default
    private Boolean pushNotification = true;

    @Comment("추모 활동 알림")
    @Column(nullable = false, name = "memorial_notification")
    @Builder.Default
    private Boolean memorialNotification = true;

    @Comment("결제 관련 알림")
    @Column(nullable = false, name = "payment_notification")
    @Builder.Default
    private Boolean paymentNotification = true;

    @Comment("가족 활동 알림")
    @Column(nullable = false, name = "family_notification")
    @Builder.Default
    private Boolean familyNotification = true;

    @Comment("3개월 무료 체험 시작일")
    @Column(name = "free_trial_start_at")
    private LocalDateTime freeTrialStartAt;

    @Comment("3개월 무료 체험 종료일")
    @Column(name = "free_trial_end_at")
    private LocalDateTime freeTrialEndAt;

    // 연관관계
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Memorial> ownedMemorials = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FamilyMember> familyMemberships = new ArrayList<>();

    @OneToMany(mappedBy = "caller", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VideoCall> videoCalls = new ArrayList<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemberActivity> memberActivities = new ArrayList<>();

    // 비즈니스 메서드
    public void setStatus(MemberStatus status) {
        this.status = status;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public void setLastAccessAt() {
        this.lastAccessAt = LocalDateTime.now();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProfileImg(String profileImg) {
        this.profileImg = profileImg;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public void setMarketingAgreed(Boolean marketingAgreed) {
        this.marketingAgreed = marketingAgreed;
    }

    public void setPushNotification(Boolean pushNotification) {
        this.pushNotification = pushNotification;
    }

    public void setMemorialNotification(Boolean memorialNotification) {
        this.memorialNotification = memorialNotification;
    }

    public void setPaymentNotification(Boolean paymentNotification) {
        this.paymentNotification = paymentNotification;
    }

    public void setFamilyNotification(Boolean familyNotification) {
        this.familyNotification = familyNotification;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void startFreeTrial() {
        this.freeTrialStartAt = LocalDateTime.now();
        this.freeTrialEndAt = LocalDateTime.now().plusMonths(3);
    }

    public boolean isInFreeTrial() {
        if (freeTrialStartAt == null || freeTrialEndAt == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(freeTrialStartAt) && now.isBefore(freeTrialEndAt);
    }

    public void deleteMemInfo() {
        this.name = "탈퇴한 회원";
        this.phoneNumber = "";
        this.email = "";
        this.userKey = UUID.randomUUID().toString();
        this.status = MemberStatus.DELETED;
        this.role = MemberRole.USER_INACTIVE;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public boolean isBlocked() {
        return status == MemberStatus.BLOCKED;
    }

    public boolean isDeleted() {
        return status == MemberStatus.DELETED;
    }

    public boolean isUserActive() {
        return role == MemberRole.USER_ACTIVE;
    }

    public boolean isUserInactive() {
        return role == MemberRole.USER_INACTIVE;
    }

    public boolean canCreateMemorial() {
        return isActive() && isUserActive();
    }

    public boolean canAccessMemorial() {
        return isActive();
    }

    public boolean canInviteFamily() {
        return isActive() && isUserActive();
    }

    public int getOwnedMemorialCount() {
        return ownedMemorials.size();
    }

    public int getFamilyMembershipCount() {
        return familyMemberships.size();
    }

    public int getVideoCallCount() {
        return videoCalls.size();
    }

    public MemberDTO convertDTO() {
        return MemberDTO.builder()
            .id(id)
            .createdAt(createdAt)
            .password(password)
            .phoneNumber(phoneNumber)
            .inviteCode(inviteCode)
            .status(status)
            .role(role)
            .email(email)
            .name(name)
            .lastAccessAt(lastAccessAt)
            .profileImg(profileImg)
            .preferredLanguage(preferredLanguage)
            .marketingAgreed(marketingAgreed)
            .pushNotification(pushNotification)
            .memorialNotification(memorialNotification)
            .paymentNotification(paymentNotification)
            .familyNotification(familyNotification)
            .freeTrialStartAt(freeTrialStartAt)
            .freeTrialEndAt(freeTrialEndAt)
            .build();
    }

    public AppUserResponse convertAppUserResponse() {
        return AppUserResponse.builder()
            .userId(id)
            .userName(name)
            .userKey(userKey)
            .phoneNumber(phoneNumber)
            .inviteCode(inviteCode)
            .profileImageUrl(profileImg)
            .status(status)
            .role(role)
            .email(email)
            .preferredLanguage(preferredLanguage)
            .marketingAgreed(marketingAgreed)
            .pushNotification(pushNotification)
            .memorialNotification(memorialNotification)
            .paymentNotification(paymentNotification)
            .familyNotification(familyNotification)
            .freeTrialStartAt(freeTrialStartAt)
            .freeTrialEndAt(freeTrialEndAt)
            .build();
    }
}