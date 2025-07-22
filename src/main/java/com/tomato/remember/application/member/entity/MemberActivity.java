package com.tomato.remember.application.member.entity;

import com.tomato.remember.application.wsvideo.code.DeviceType;
import com.tomato.remember.application.member.code.ActivityCategory;
import com.tomato.remember.application.member.code.ActivityStatus;
import com.tomato.remember.application.member.code.ActivityType;
import com.tomato.remember.application.member.code.ImportanceLevel;
import com.tomato.remember.common.audit.Audit;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

@Table(
    name = "t_member_activity",
    indexes = {
        @Index(name = "idx01_t_member_activity", columnList = "created_at"),
        @Index(name = "idx02_t_member_activity", columnList = "author_id"),
        @Index(name = "idx03_t_member_activity", columnList = "activity_type"),
        @Index(name = "idx04_t_member_activity", columnList = "memorial_id"),
        @Index(name = "idx05_t_member_activity", columnList = "activity_category"),
        @Index(name = "idx06_t_member_activity", columnList = "created_at, author_id"),
        @Index(name = "idx07_t_member_activity", columnList = "status"),
        @Index(name = "idx08_t_member_activity", columnList = "importance_level")
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MemberActivity extends Audit {

    @Comment("활동 유형")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, name = "activity_type")
    private ActivityType activityType;

    @Comment("활동 카테고리")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, name = "activity_category")
    private ActivityCategory activityCategory;

    @Comment("활동 제목")
    @Column(nullable = false, length = 200)
    private String title;

    @Comment("활동 내용")
    @Column(columnDefinition = "TEXT")
    private String content;

    @Comment("관련 메모리얼 ID")
    @Column(name = "memorial_id")
    private Long memorialId;

    @Comment("관련 영상통화 ID")
    @Column(name = "video_call_id")
    private Long videoCallId;

    @Comment("관련 가족 구성원 ID")
    @Column(name = "family_member_id")
    private Long familyMemberId;

    @Comment("활동 상태")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ActivityStatus status = ActivityStatus.ACTIVE;

    @Comment("중요도 레벨")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "importance_level")
    @Builder.Default
    private ImportanceLevel importanceLevel = ImportanceLevel.NORMAL;

    @Comment("공개 여부")
    @Column(nullable = false, name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Comment("IP 주소")
    @Column(length = 45, name = "ip_address")
    private String ipAddress;

    @Comment("사용자 에이전트")
    @Column(length = 500, name = "user_agent")
    private String userAgent;

    @Comment("디바이스 정보")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, name = "device_type")
    private DeviceType deviceType;

    @Comment("위치 정보")
    @Column(length = 200, name = "location_info")
    private String locationInfo;

    @Comment("추가 데이터 (JSON)")
    @Column(columnDefinition = "TEXT", name = "additional_data")
    private String additionalData;

    @Comment("태그 (쉼표로 구분)")
    @Column(length = 500)
    private String tags;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    // 비즈니스 메서드
    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public void setActivityCategory(ActivityCategory activityCategory) {
        this.activityCategory = activityCategory;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setStatus(ActivityStatus status) {
        this.status = status;
    }

    public void setImportanceLevel(ImportanceLevel importanceLevel) {
        this.importanceLevel = importanceLevel;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public void setLocationInfo(String locationInfo) {
        this.locationInfo = locationInfo;
    }

    public void setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setMemorialId(Long memorialId) {
        this.memorialId = memorialId;
    }

    public void setVideoCallId(Long videoCallId) {
        this.videoCallId = videoCallId;
    }

    public void setFamilyMemberId(Long familyMemberId) {
        this.familyMemberId = familyMemberId;
    }

    public boolean isMemorialActivity() {
        return memorialId != null;
    }

    public boolean isVideoCallActivity() {
        return videoCallId != null;
    }

    public boolean isFamilyActivity() {
        return familyMemberId != null;
    }

    public boolean isActive() {
        return status == ActivityStatus.ACTIVE;
    }

    public boolean isArchived() {
        return status == ActivityStatus.ARCHIVED;
    }

    public boolean isDeleted() {
        return status == ActivityStatus.DELETED;
    }

    public boolean isHighImportance() {
        return importanceLevel == ImportanceLevel.HIGH ||
               importanceLevel == ImportanceLevel.CRITICAL ||
               importanceLevel == ImportanceLevel.URGENT;
    }

    public boolean isLowImportance() {
        return importanceLevel == ImportanceLevel.LOW;
    }

    public boolean isCritical() {
        return importanceLevel == ImportanceLevel.CRITICAL ||
               importanceLevel == ImportanceLevel.URGENT;
    }

    public boolean isMemorialCategory() {
        return activityCategory == ActivityCategory.MEMORIAL;
    }

    public boolean isVideoCallCategory() {
        return activityCategory == ActivityCategory.VIDEO_CALL;
    }

    public boolean isFamilyCategory() {
        return activityCategory == ActivityCategory.FAMILY;
    }

    public boolean isAuthCategory() {
        return activityCategory == ActivityCategory.AUTH;
    }

    public boolean isProfileCategory() {
        return activityCategory == ActivityCategory.PROFILE;
    }

    public boolean isErrorCategory() {
        return activityCategory == ActivityCategory.ERROR;
    }

    public void archive() {
        this.status = ActivityStatus.ARCHIVED;
    }

    public void delete() {
        this.status = ActivityStatus.DELETED;
    }

    public void activate() {
        this.status = ActivityStatus.ACTIVE;
    }

    public void setHighImportance() {
        this.importanceLevel = ImportanceLevel.HIGH;
    }

    public void setCriticalImportance() {
        this.importanceLevel = ImportanceLevel.CRITICAL;
    }

    public void setUrgentImportance() {
        this.importanceLevel = ImportanceLevel.URGENT;
    }

    public void setNormalImportance() {
        this.importanceLevel = ImportanceLevel.NORMAL;
    }

    public void setLowImportance() {
        this.importanceLevel = ImportanceLevel.LOW;
    }

    // 정적 팩토리 메서드들
    public static MemberActivity createMemorialActivity(
            Member author,
            Long memorialId,
            ActivityType activityType,
            String title,
            String content) {
        return MemberActivity.builder()
            .author(author)
            .memorialId(memorialId)
            .activityType(activityType)
            .activityCategory(ActivityCategory.MEMORIAL)
            .title(title)
            .content(content)
            .status(ActivityStatus.ACTIVE)
            .importanceLevel(ImportanceLevel.NORMAL)
            .isPublic(false)
            .build();
    }

    public static MemberActivity createVideoCallActivity(
            Member author,
            Long memorialId,
            Long videoCallId,
            ActivityType activityType,
            String title,
            String content) {
        return MemberActivity.builder()
            .author(author)
            .memorialId(memorialId)
            .videoCallId(videoCallId)
            .activityType(activityType)
            .activityCategory(ActivityCategory.VIDEO_CALL)
            .title(title)
            .content(content)
            .status(ActivityStatus.ACTIVE)
            .importanceLevel(ImportanceLevel.HIGH)
            .isPublic(false)
            .build();
    }

    public static MemberActivity createFamilyActivity(
            Member author,
            Long memorialId,
            Long familyMemberId,
            ActivityType activityType,
            String title,
            String content) {
        return MemberActivity.builder()
            .author(author)
            .memorialId(memorialId)
            .familyMemberId(familyMemberId)
            .activityType(activityType)
            .activityCategory(ActivityCategory.FAMILY)
            .title(title)
            .content(content)
            .status(ActivityStatus.ACTIVE)
            .importanceLevel(ImportanceLevel.NORMAL)
            .isPublic(false)
            .build();
    }

    public static MemberActivity createAuthActivity(
            Member author,
            ActivityType activityType,
            String title,
            String content) {
        return MemberActivity.builder()
            .author(author)
            .activityType(activityType)
            .activityCategory(ActivityCategory.AUTH)
            .title(title)
            .content(content)
            .status(ActivityStatus.ACTIVE)
            .importanceLevel(ImportanceLevel.NORMAL)
            .isPublic(false)
            .build();
    }

    public static MemberActivity createErrorActivity(
            Member author,
            ActivityType activityType,
            String title,
            String content,
            String errorDetails) {
        return MemberActivity.builder()
            .author(author)
            .activityType(activityType)
            .activityCategory(ActivityCategory.ERROR)
            .title(title)
            .content(content)
            .additionalData(errorDetails)
            .status(ActivityStatus.ACTIVE)
            .importanceLevel(ImportanceLevel.CRITICAL)
            .isPublic(false)
            .build();
    }

    // 표시명 반환 메서드들
    public String getActivityTypeDisplayName() {
        return activityType.getDisplayName();
    }

    public String getActivityCategoryDisplayName() {
        return activityCategory.getDisplayName();
    }

    public String getStatusDisplayName() {
        return status.getDisplayName();
    }

    public String getImportanceLevelDisplayName() {
        return importanceLevel.getDisplayName();
    }

    public String getDeviceTypeDisplayName() {
        return deviceType != null ? deviceType.getDisplayName() : "알 수 없음";
    }

    public int getImportanceLevelValue() {
        return importanceLevel.getLevel();
    }
}