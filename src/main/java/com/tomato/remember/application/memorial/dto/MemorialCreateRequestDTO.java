package com.tomato.remember.application.memorial.dto;

import com.tomato.remember.application.member.code.Gender;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.memorial.code.InterestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * 메모리얼 생성 요청 DTO
 */
/**
 * 메모리얼 생성 요청 DTO (다중 파일 지원)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialCreateRequestDTO {

    @NotBlank(message = "망자 이름은 필수입니다.")
    @Size(max = 50, message = "망자 이름은 50자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "호칭은 필수입니다.")
    @Size(max = 30, message = "호칭은 30자 이하여야 합니다.")
    private String nickname;

    @NotNull(message = "성별은 필수입니다.")
    private Gender gender;

    private LocalDate birthDate;

    private LocalDate deathDate;

    @NotNull(message = "관계는 필수입니다.")
    private Relationship relationship;

    private List<InterestType> interests;

    // ===== 다중 파일 URL 필드 =====
    @Size(max = 5, message = "프로필 이미지는 최대 5장까지 업로드할 수 있습니다.")
    private List<String> profileImageUrls; // 프로필 이미지 URL 리스트 (최대 5개)

    @Size(max = 3, message = "음성 파일은 최대 3개까지 업로드할 수 있습니다.")
    private List<String> voiceFileUrls; // 음성 파일 URL 리스트 (최대 3개)

    // ===== 하위 호환성을 위한 단일 파일 URL 필드 =====
    private String profileImageUrl; // 대표 프로필 이미지 URL (하위 호환성)

    private String voiceFileUrl; // 대표 음성 파일 URL (하위 호환성)

    private String videoFileUrl; // 영상 파일 URL

    private String userImageUrl; // 사용자 이미지 URL

    @Builder.Default
    private Boolean isPublic = false;

    @Size(max = 1000, message = "추가 정보는 1000자 이하여야 합니다.")
    private String additionalInfo;

    // ===== 다중 파일 관련 편의 메서드 =====

    /**
     * 프로필 이미지 개수 반환
     */
    public int getProfileImageCount() {
        return profileImageUrls != null ? profileImageUrls.size() : 0;
    }

    /**
     * 음성 파일 개수 반환
     */
    public int getVoiceFileCount() {
        return voiceFileUrls != null ? voiceFileUrls.size() : 0;
    }

    /**
     * 필수 파일 보유 여부 확인
     */
    public boolean hasRequiredFiles() {
        return getProfileImageCount() >= 5 && // 프로필 이미지 5장 필수
               getVoiceFileCount() >= 3 && // 음성 파일 3개 필수
               videoFileUrl != null && !videoFileUrl.trim().isEmpty() &&
               userImageUrl != null && !userImageUrl.trim().isEmpty();
    }

    /**
     * 프로필 이미지 추가 가능 여부
     */
    public boolean canAddProfileImage() {
        return getProfileImageCount() < 5;
    }

    /**
     * 음성 파일 추가 가능 여부
     */
    public boolean canAddVoiceFile() {
        return getVoiceFileCount() < 3;
    }
}