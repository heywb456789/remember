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

    // ===== 시스템 정보 =====

    private LocalDateTime updatedAt;

    // ===== 헬퍼 메서드 =====


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
                ", updatedAt=" + updatedAt +
                '}';
    }
}