package com.tomato.remember.application.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * 통합 프로필 업데이트 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {

    // ===== 기본 정보 =====

    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    private String name;

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @Pattern(regexp = "^010[0-9]{8}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
    private String phoneNumber;

    private LocalDate birthDate;

    // ===== 설정 정보 =====

    @Pattern(regexp = "^(KO|EN|JP|ZH)$", message = "지원하지 않는 언어입니다.")
    private String preferredLanguage;

    private Boolean marketingAgreed;

    // ===== 알림 설정 =====

    private Boolean pushNotification;

    private Boolean memorialNotification;

    private Boolean paymentNotification;

    private Boolean familyNotification;

    // ===== 이미지 관련 =====

    private MultipartFile[] images;

    private Integer[] imagesToDelete;

    private Integer[] imageOrders;

    // ===== 헬퍼 메서드 =====

    /**
     * 기본 정보 변경 여부 확인
     */
    public boolean hasBasicInfoChanges() {
        return name != null || email != null || phoneNumber != null || birthDate != null;
    }

    /**
     * 알림 설정 변경 여부 확인
     */
    public boolean hasNotificationChanges() {
        return pushNotification != null || memorialNotification != null ||
               paymentNotification != null || familyNotification != null;
    }

    /**
     * 이미지 변경 여부 확인
     */
    public boolean hasImageChanges() {
        return (images != null && images.length > 0) ||
               (imagesToDelete != null && imagesToDelete.length > 0);
    }

    /**
     * 새 이미지 개수 반환
     */
    public int getNewImageCount() {
        return images != null ? images.length : 0;
    }

    /**
     * 삭제할 이미지 개수 반환
     */
    public int getDeleteImageCount() {
        return imagesToDelete != null ? imagesToDelete.length : 0;
    }

    /**
     * 휴대폰 번호 정규화 (하이픈 제거)
     */
    public String getNormalizedPhoneNumber() {
        if (phoneNumber == null) return null;
        return phoneNumber.replaceAll("[-\\s]", "");
    }

    /**
     * 이메일 정규화 (소문자 변환)
     */
    public String getNormalizedEmail() {
        if (email == null) return null;
        return email.toLowerCase().trim();
    }

    /**
     * 이름 정규화 (앞뒤 공백 제거)
     */
    public String getNormalizedName() {
        if (name == null) return null;
        return name.trim();
    }

    @Override
    public String toString() {
        return "ProfileUpdateRequest{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", birthDate=" + birthDate +
                ", preferredLanguage='" + preferredLanguage + '\'' +
                ", marketingAgreed=" + marketingAgreed +
                ", pushNotification=" + pushNotification +
                ", memorialNotification=" + memorialNotification +
                ", paymentNotification=" + paymentNotification +
                ", familyNotification=" + familyNotification +
                ", newImageCount=" + getNewImageCount() +
                ", deleteImageCount=" + getDeleteImageCount() +
                '}';
    }
}