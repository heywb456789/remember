package com.tomato.remember.application.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 프로필 업데이트 응답 DTO (수정된 버전)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateResponse {

    // ===== 기본 정보 =====

    private Long memberId;

    private String name;

    private String email;

    private String phoneNumber;

    private LocalDate birthDate;

    private String preferredLanguage;

    // ===== 이미지 정보 =====

    private Integer totalImages;

    private List<String> imageUrls;

    // ===== 알림 설정 =====

    private Map<String, Object> notificationSettings;

    // ===== 프로필 완성도 =====

    private Map<String, Object> profileCompletion;

    // ===== 시스템 정보 =====

    private LocalDateTime updatedAt;

    // ===== 헬퍼 메서드 =====

    /**
     * 영상통화 가능 여부 확인
     */
    public boolean canStartVideoCall() {
        if (profileCompletion == null) return false;

        Object canStart = profileCompletion.get("canStartVideoCall");
        return canStart instanceof Boolean && (Boolean) canStart;
    }

    /**
     * 프로필 완성도 퍼센트 반환
     */
    public int getCompletionPercentage() {
        if (profileCompletion == null) return 0;

        Object percentage = profileCompletion.get("completionPercentage");
        return percentage instanceof Integer ? (Integer) percentage : 0;
    }

    /**
     * 알림 설정 조회
     */
    public boolean getNotificationSetting(String settingName) {
        if (notificationSettings == null) return true;

        Object setting = notificationSettings.get(settingName);
        return setting instanceof Boolean ? (Boolean) setting : true;
    }

    /**
     * 생년월일 포맷팅
     */
    public String getFormattedBirthDate() {
        if (birthDate == null) return null;
        return birthDate.toString();
    }

    /**
     * 나이 계산
     */
    public int getAge() {
        if (birthDate == null) return 0;
        return LocalDate.now().getYear() - birthDate.getYear();
    }

    /**
     * 프로필 완성 여부 확인
     */
    public boolean isProfileComplete() {
        return getCompletionPercentage() >= 100;
    }

    /**
     * 이미지 업로드 완료 여부 확인
     */
    public boolean isImageUploadComplete() {
        return totalImages != null && totalImages >= 5;
    }

    @Override
    public String toString() {
        return "ProfileUpdateResponse{" +
                "memberId=" + memberId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", birthDate=" + birthDate +
                ", preferredLanguage='" + preferredLanguage + '\'' +
                ", totalImages=" + totalImages +
                ", imageUrlCount=" + (imageUrls != null ? imageUrls.size() : 0) +
                ", completionPercentage=" + getCompletionPercentage() +
                ", canStartVideoCall=" + canStartVideoCall() +
                ", updatedAt=" + updatedAt +
                '}';
    }
}