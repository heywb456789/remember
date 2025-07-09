package com.tomato.remember.application.family.entity;

import com.tomato.remember.application.family.code.DeviceType;
import com.tomato.remember.application.family.code.FamilyRole;
import com.tomato.remember.application.family.code.InviteMethod;
import com.tomato.remember.application.family.code.InviteStatus;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.common.audit.Audit;
import com.tomato.remember.common.code.Language;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

@Table(
    name = "t_family_member",
    indexes = {
        @Index(name = "idx01_t_family_member", columnList = "memorial_id"),
        @Index(name = "idx02_t_family_member", columnList = "member_id"),
        @Index(name = "idx03_t_family_member", columnList = "invite_token"),
        @Index(name = "idx04_t_family_member", columnList = "family_role"),
        @Index(name = "idx05_t_family_member", columnList = "relationship"),
        @Index(name = "idx06_t_family_member", columnList = "invite_status"),
        @Index(name = "idx07_t_family_member", columnList = "invited_at"),
        @Index(name = "idx08_t_family_member", columnList = "accepted_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_family_member", columnNames = {"memorial_id", "member_id"})
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMember extends Audit {

    @Comment("가족 내 권한")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "family_role")
    private FamilyRole familyRole;

    @Comment("고인과의 관계")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Relationship relationship;

    @Comment("초대 상태")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "invite_status")
    @Builder.Default
    private InviteStatus inviteStatus = InviteStatus.PENDING;

    @Comment("초대 토큰")
    @Column(length = 100, name = "invite_token")
    private String inviteToken;

    @Comment("초대 방법")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, name = "invite_method")
    private InviteMethod inviteMethod;

    @Comment("초대 대상 연락처")
    @Column(length = 100, name = "invite_contact")
    private String inviteContact;

    @Comment("초대 메시지")
    @Column(length = 500, name = "invite_message")
    private String inviteMessage;

    @Comment("초대 일시")
    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Comment("수락 일시")
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Comment("거절 일시")
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Comment("거절 사유")
    @Column(length = 200, name = "rejection_reason")
    private String rejectionReason;

    @Comment("마지막 활동 일시")
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Comment("알림 설정")
    @Column(nullable = false, name = "notification_enabled")
    @Builder.Default
    private Boolean notificationEnabled = true;

    @Comment("권한 설정 (JSON)")
    @Column(columnDefinition = "TEXT")
    private String permissions;

    @Comment("닉네임 (가족 내에서 사용)")
    @Column(length = 50, name = "family_nickname")
    private String familyNickname;

    @Comment("디바이스 정보")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, name = "device_type")
    private DeviceType deviceType;

    @Comment("언어 설정")
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private Language language = Language.KO;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memorial_id", nullable = false)
    private Memorial memorial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id")
    private Member inviter;

    // 비즈니스 메서드
    public void setFamilyRole(FamilyRole familyRole) {
        this.familyRole = familyRole;
    }

    public void setRelationship(Relationship relationship) {
        this.relationship = relationship;
    }

    public void setInviteStatus(InviteStatus inviteStatus) {
        this.inviteStatus = inviteStatus;
    }

    public void setInviteToken(String inviteToken) {
        this.inviteToken = inviteToken;
    }

    public void setInviteMethod(InviteMethod inviteMethod) {
        this.inviteMethod = inviteMethod;
    }

    public void setInviteContact(String inviteContact) {
        this.inviteContact = inviteContact;
    }

    public void setInviteMessage(String inviteMessage) {
        this.inviteMessage = inviteMessage;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public void setNotificationEnabled(Boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public void setFamilyNickname(String familyNickname) {
        this.familyNickname = familyNickname;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public void sendInvite(InviteMethod method, String contact, String token, String message) {
        this.inviteMethod = method;
        this.inviteContact = contact;
        this.inviteToken = token;
        this.inviteMessage = message;
        this.invitedAt = LocalDateTime.now();
        this.inviteStatus = InviteStatus.PENDING;
    }

    public void acceptInvite(Member member) {
        this.member = member;
        this.acceptedAt = LocalDateTime.now();
        this.inviteStatus = InviteStatus.ACCEPTED;
        this.lastActivityAt = LocalDateTime.now();
    }

    public void rejectInvite(String reason) {
        this.rejectedAt = LocalDateTime.now();
        this.rejectionReason = reason;
        this.inviteStatus = InviteStatus.REJECTED;
    }

    public void expireInvite() {
        this.inviteStatus = InviteStatus.EXPIRED;
    }

    public void updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public void enableNotification() {
        this.notificationEnabled = true;
    }

    public void disableNotification() {
        this.notificationEnabled = false;
    }

    public void setAsMainAccount() {
        this.familyRole = FamilyRole.MAIN;
    }

    public void setAsSubAccount() {
        this.familyRole = FamilyRole.SUB;
    }

    // 상태 확인 메서드들
    public boolean isPending() {
        return inviteStatus == InviteStatus.PENDING;
    }

    public boolean isAccepted() {
        return inviteStatus == InviteStatus.ACCEPTED;
    }

    public boolean isRejected() {
        return inviteStatus == InviteStatus.REJECTED;
    }

    public boolean isExpired() {
        return inviteStatus == InviteStatus.EXPIRED;
    }

    public boolean isMainAccount() {
        return familyRole == FamilyRole.MAIN;
    }

    public boolean isSubAccount() {
        return familyRole == FamilyRole.SUB;
    }

    public boolean isSmsInvite() {
        return inviteMethod == InviteMethod.SMS;
    }

    public boolean isEmailInvite() {
        return inviteMethod == InviteMethod.EMAIL;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public boolean isKoreanLanguage() {
        return language == Language.KO;
    }

    public boolean isEnglishLanguage() {
        return language == Language.EN;
    }

    public boolean isMobileDevice() {
        return deviceType == DeviceType.MOBILE;
    }

    public boolean isTabletDevice() {
        return deviceType == DeviceType.TABLET;
    }

    public boolean isDesktopDevice() {
        return deviceType == DeviceType.DESKTOP;
    }

    // 표시명 반환 메서드들
    public String getDisplayName() {
        if (familyNickname != null && !familyNickname.trim().isEmpty()) {
            return familyNickname;
        }
        if (member != null) {
            return member.getName();
        }
        return inviteContact;
    }

    public String getFamilyRoleDisplayName() {
        return familyRole.getDisplayName();
    }

    public String getRelationshipDisplayName() {
        return relationship.getDisplayName();
    }

    public String getInviteStatusDisplayName() {
        return inviteStatus.getDisplayName();
    }

    public String getInviteMethodDisplayName() {
        return inviteMethod != null ? inviteMethod.getDisplayName() : "없음";
    }

    public String getLanguageDisplayName() {
        return language.getDisplayName();
    }

    public String getDeviceTypeDisplayName() {
        return deviceType != null ? deviceType.getDisplayName() : "알 수 없음";
    }

    // 권한 확인 메서드들
    public boolean canManageFamily() {
        return isMainAccount() && isAccepted();
    }

    public boolean canViewMemorial() {
        return isAccepted();
    }

    public boolean canCallMemorial() {
        return isAccepted() && notificationEnabled;
    }

    public boolean canEditMemorial() {
        return isMainAccount() && isAccepted();
    }

    public boolean canInviteOthers() {
        return isMainAccount() && isAccepted();
    }

    public boolean canRemoveMembers() {
        return isMainAccount() && isAccepted();
    }

    public boolean canAccessVideoCall() {
        return isAccepted() && notificationEnabled;
    }

    public boolean canReceiveNotifications() {
        return isAccepted() && notificationEnabled;
    }

    // 상태 확인 메서드들
    public boolean isInvitePending() {
        return isPending() && invitedAt != null;
    }

    public boolean isInviteExpired() {
        return isExpired() || (isPending() && invitedAt != null &&
               invitedAt.isBefore(LocalDateTime.now().minusDays(7))); // 7일 후 만료
    }

    public boolean isActiveParticipant() {
        return isAccepted() && member != null && member.isActive();
    }

    public boolean isRecentlyActive() {
        return lastActivityAt != null &&
               lastActivityAt.isAfter(LocalDateTime.now().minusDays(30)); // 30일 이내 활동
    }

    public boolean hasValidInviteToken() {
        return inviteToken != null && !inviteToken.trim().isEmpty() &&
               isPending() && !isInviteExpired();
    }

    // 유틸리티 메서드들
    public String getInviteDuration() {
        if (invitedAt == null) return "초대 정보 없음";

        LocalDateTime now = LocalDateTime.now();
        long days = java.time.Duration.between(invitedAt, now).toDays();

        if (days == 0) return "오늘 초대됨";
        if (days == 1) return "1일 전 초대됨";
        return days + "일 전 초대됨";
    }

    public String getAcceptedDuration() {
        if (acceptedAt == null) return "수락 정보 없음";

        LocalDateTime now = LocalDateTime.now();
        long days = java.time.Duration.between(acceptedAt, now).toDays();

        if (days == 0) return "오늘 참여함";
        if (days == 1) return "1일 전 참여함";
        return days + "일 전 참여함";
    }

    public String getLastActivityDuration() {
        if (lastActivityAt == null) return "활동 기록 없음";

        LocalDateTime now = LocalDateTime.now();
        long days = java.time.Duration.between(lastActivityAt, now).toDays();

        if (days == 0) return "오늘 활동함";
        if (days == 1) return "1일 전 활동함";
        return days + "일 전 활동함";
    }

    public boolean isInviteAboutToExpire() {
        return isPending() && invitedAt != null &&
               invitedAt.isBefore(LocalDateTime.now().minusDays(5)); // 5일 후 만료 예정
    }

    // 정적 팩토리 메서드들
    public static FamilyMember createSmsInvite(
            Memorial memorial,
            Member inviter,
            String phoneNumber,
            Relationship relationship,
            FamilyRole familyRole,
            String message) {
        return FamilyMember.builder()
            .memorial(memorial)
            .inviter(inviter)
            .inviteContact(phoneNumber)
            .relationship(relationship)
            .familyRole(familyRole)
            .inviteMethod(InviteMethod.SMS)
            .inviteMessage(message)
            .inviteStatus(InviteStatus.PENDING)
            .notificationEnabled(true)
            .language(Language.KO)
            .build();
    }

    public static FamilyMember createEmailInvite(
            Memorial memorial,
            Member inviter,
            String email,
            Relationship relationship,
            FamilyRole familyRole,
            String message) {
        return FamilyMember.builder()
            .memorial(memorial)
            .inviter(inviter)
            .inviteContact(email)
            .relationship(relationship)
            .familyRole(familyRole)
            .inviteMethod(InviteMethod.EMAIL)
            .inviteMessage(message)
            .inviteStatus(InviteStatus.PENDING)
            .notificationEnabled(true)
            .language(Language.KO)
            .build();
    }

    public static FamilyMember createDirectMember(
            Memorial memorial,
            Member member,
            Relationship relationship,
            FamilyRole familyRole) {
        return FamilyMember.builder()
            .memorial(memorial)
            .member(member)
            .relationship(relationship)
            .familyRole(familyRole)
            .inviteStatus(InviteStatus.ACCEPTED)
            .acceptedAt(LocalDateTime.now())
            .lastActivityAt(LocalDateTime.now())
            .notificationEnabled(true)
            .language(Language.KO)
            .build();
    }

    // 초대 토큰 생성 (예시)
    public void generateInviteToken() {
        this.inviteToken = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    // 초대 링크 생성 (예시)
    public String generateInviteLink(String baseUrl) {
        if (inviteToken == null) {
            generateInviteToken();
        }
        return baseUrl + "/invite/" + inviteToken;
    }

    // toString 메서드
    @Override
    public String toString() {
        return "FamilyMember{" +
                "id=" + getId() +
                ", memorial=" + (memorial != null ? memorial.getName() : "null") +
                ", member=" + (member != null ? member.getName() : "null") +
                ", relationship=" + relationship +
                ", familyRole=" + familyRole +
                ", inviteStatus=" + inviteStatus +
                ", inviteContact='" + inviteContact + '\'' +
                ", familyNickname='" + familyNickname + '\'' +
                '}';
    }
}