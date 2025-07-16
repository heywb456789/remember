package com.tomato.remember.application.family.dto;

import com.tomato.remember.application.family.code.InviteStatus;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.member.code.Relationship;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 가족 구성원 응답 DTO
 */
@Getter
@Builder
@ToString
public class FamilyMemberResponse {

    /**
     * 가족 구성원 ID
     */
    private Long id;

    /**
     * 메모리얼 정보
     */
    private MemorialInfo memorial;

    /**
     * 구성원 정보
     */
    private MemberInfo member;

    /**
     * 초대자 정보
     */
    private MemberInfo invitedBy;

    /**
     * 고인과의 관계
     */
    private Relationship relationship;

    /**
     * 관계 표시명
     */
    private String relationshipDisplayName;

    /**
     * 초대 상태
     */
    private InviteStatus inviteStatus;

    /**
     * 초대 상태 표시명
     */
    private String inviteStatusDisplayName;

    /**
     * 권한 정보
     */
    private PermissionInfo permissions;

    /**
     * 초대 메시지
     */
    private String inviteMessage;

    /**
     * 일시 정보
     */
    private DateTimeInfo dateTime;

    /**
     * 통계 정보
     */
    private StatsInfo stats;

    /**
     * 고인 상세 정보 (가족 구성원별 개별 입력)
     */
    private DeceasedInfo deceasedInfo;

    /**
     * 메모리얼 정보
     */
    @Getter
    @Builder
    public static class MemorialInfo {
        private Long id;
        private String name;
        private String nickname;
        private String mainProfileImageUrl;
        private boolean isActive;
    }

    /**
     * 구성원 정보
     */
    @Getter
    @Builder
    public static class MemberInfo {
        private Long id;
        private String name;
        private String email;
        private String phoneNumber;
        private String profileImageUrl;
        private boolean isActive;
    }

    /**
     * 권한 정보
     */
    @Getter
    @Builder
    public static class PermissionInfo {
        private boolean memorialAccess;
        private boolean videoCallAccess;
        private boolean canModify;
    }

    /**
     * 일시 정보
     */
    @Getter
    @Builder
    public static class DateTimeInfo {
        private LocalDateTime createdAt;
        private LocalDateTime acceptedAt;
        private LocalDateTime rejectedAt;
        private LocalDateTime lastAccessAt;
        private String formattedLastAccess;
    }

    /**
     * 통계 정보
     */
    @Getter
    @Builder
    public static class StatsInfo {
        private int totalVideoCallCount;
        private int thisMonthVideoCallCount;
        private LocalDateTime lastVideoCallAt;
    }

    /**
     * Entity로부터 DTO 생성
     */
    public static FamilyMemberResponse from(FamilyMember familyMember) {
        return FamilyMemberResponse.builder()
            .id(familyMember.getId())
            .memorial(buildMemorialInfo(familyMember))
            .member(buildMemberInfo(familyMember))
            .invitedBy(buildInviterInfo(familyMember))
            .relationship(familyMember.getRelationship())
            .relationshipDisplayName(familyMember.getRelationshipDisplayName())
            .inviteStatus(familyMember.getInviteStatus())
            .inviteStatusDisplayName(familyMember.getInviteStatusDisplayName())
            .permissions(buildPermissionInfo(familyMember))
            .inviteMessage(familyMember.getInviteMessage())
            .dateTime(buildDateTimeInfo(familyMember))
            .stats(buildStatsInfo(familyMember))
            .deceasedInfo(buildDeceasedInfo(familyMember))
            .build();
    }


    private static DeceasedInfo buildDeceasedInfo(FamilyMember familyMember) {
        // 입력된 필드 개수 계산
        int filledCount = 0;
        if (familyMember.getMemberPersonality() != null && !familyMember.getMemberPersonality().trim().isEmpty()) filledCount++;
        if (familyMember.getMemberHobbies() != null && !familyMember.getMemberHobbies().trim().isEmpty()) filledCount++;
        if (familyMember.getMemberFavoriteFood() != null && !familyMember.getMemberFavoriteFood().trim().isEmpty()) filledCount++;
        if (familyMember.getMemberSpecialMemories() != null && !familyMember.getMemberSpecialMemories().trim().isEmpty()) filledCount++;
        if (familyMember.getMemberSpeechHabits() != null && !familyMember.getMemberSpeechHabits().trim().isEmpty()) filledCount++;

        return DeceasedInfo.builder()
            .personality(familyMember.getMemberPersonality())
            .hobbies(familyMember.getMemberHobbies())
            .favoriteFood(familyMember.getMemberFavoriteFood())
            .specialMemories(familyMember.getMemberSpecialMemories())
            .speechHabits(familyMember.getMemberSpeechHabits())
            .hasDeceasedInfo(familyMember.hasDeceasedInfo())
            .hasRequiredDeceasedInfo(familyMember.hasRequiredDeceasedInfo())
            .filledFieldCount(filledCount)
            .build();
    }

    /**
     * 메모리얼 정보 구성
     */
    private static MemorialInfo buildMemorialInfo(FamilyMember familyMember) {
        var memorial = familyMember.getMemorial();
        
        return MemorialInfo.builder()
            .id(memorial.getId())
            .name(memorial.getName())
            .nickname(memorial.getNickname())
            .mainProfileImageUrl(memorial.getMainProfileImageUrl())
            .isActive(memorial.isActive())
            .build();
    }

    /**
     * 구성원 정보 구성
     */
    private static MemberInfo buildMemberInfo(FamilyMember familyMember) {
        var member = familyMember.getMember();
        
        return MemberInfo.builder()
            .id(member.getId())
            .name(member.getName())
            .email(member.getEmail())
            .phoneNumber(maskPhoneNumber(member.getPhoneNumber()))
            .profileImageUrl(member.getProfileImageUrl())
            .isActive(member.isActive())
            .build();
    }

    /**
     * 초대자 정보 구성
     */
    private static MemberInfo buildInviterInfo(FamilyMember familyMember) {
        var inviter = familyMember.getInvitedBy();
        
        return MemberInfo.builder()
            .id(inviter.getId())
            .name(inviter.getName())
            .email(null) // 초대자 이메일은 노출하지 않음
            .phoneNumber(null) // 초대자 전화번호는 노출하지 않음
            .profileImageUrl(inviter.getProfileImageUrl())
            .isActive(inviter.isActive())
            .build();
    }

    /**
     * 권한 정보 구성
     */
    private static PermissionInfo buildPermissionInfo(FamilyMember familyMember) {
        return PermissionInfo.builder()
            .memorialAccess(familyMember.getMemorialAccess())
            .videoCallAccess(familyMember.getVideoCallAccess())
            .canModify(familyMember.canModifyPermissions())
            .build();
    }

    /**
     * 일시 정보 구성
     */
    private static DateTimeInfo buildDateTimeInfo(FamilyMember familyMember) {
        return DateTimeInfo.builder()
            .createdAt(familyMember.getCreatedAt())
            .acceptedAt(familyMember.getAcceptedAt())
            .rejectedAt(familyMember.getRejectedAt())
            .lastAccessAt(familyMember.getLastAccessAt())
            .formattedLastAccess(formatLastAccess(familyMember.getLastAccessAt()))
            .build();
    }

    /**
     * 통계 정보 구성 (향후 확장용)
     */
    private static StatsInfo buildStatsInfo(FamilyMember familyMember) {
        // TODO: 영상통화 통계 연동
        return StatsInfo.builder()
            .totalVideoCallCount(0)
            .thisMonthVideoCallCount(0)
            .lastVideoCallAt(null)
            .build();
    }

    /**
     * 전화번호 마스킹
     */
    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return null;
        }
        
        // 010-1234-5678 → 010-****-5678
        if (phoneNumber.contains("-")) {
            String[] parts = phoneNumber.split("-");
            if (parts.length == 3) {
                return parts[0] + "-****-" + parts[2];
            }
        }
        
        // 01012345678 → 010****5678
        if (phoneNumber.length() == 11) {
            return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7);
        }
        
        return "****";
    }

    /**
     * 마지막 접근 시간 포맷팅
     */
    private static String formatLastAccess(LocalDateTime lastAccessAt) {
        if (lastAccessAt == null) {
            return "활동 없음";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(lastAccessAt, now).toMinutes();
        
        if (minutes < 1) {
            return "방금 전";
        } else if (minutes < 60) {
            return minutes + "분 전";
        } else if (minutes < 24 * 60) {
            long hours = minutes / 60;
            return hours + "시간 전";
        } else if (minutes < 7 * 24 * 60) {
            long days = minutes / (24 * 60);
            return days + "일 전";
        } else {
            return lastAccessAt.getMonthValue() + "월 " + lastAccessAt.getDayOfMonth() + "일";
        }
    }

    // ===== 편의 메서드 =====

    /**
     * 구성원 이름 조회
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
     * 활성 상태 확인
     */
    public boolean isActive() {
        return inviteStatus == InviteStatus.ACCEPTED;
    }

    /**
     * 대기 상태 확인
     */
    public boolean isPending() {
        return inviteStatus == InviteStatus.PENDING;
    }

    /**
     * 메모리얼 접근 가능 여부
     */
    public boolean canAccessMemorial() {
        return isActive() && permissions.isMemorialAccess();
    }

    /**
     * 영상통화 접근 가능 여부
     */
    public boolean canAccessVideoCall() {
        return canAccessMemorial() && permissions.isVideoCallAccess();
    }

    /**
     * 고인 상세 정보 완성 여부
     */
    public boolean hasDeceasedInfo() {
        return deceasedInfo != null && deceasedInfo.isHasDeceasedInfo();
    }

    /**
     * 고인 상세 정보 필수 항목 완성 여부
     */
    public boolean hasRequiredDeceasedInfo() {
        return deceasedInfo != null && deceasedInfo.isHasRequiredDeceasedInfo();
    }
}