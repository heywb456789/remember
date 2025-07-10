package com.tomato.remember.application.memorial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메모리얼 3단계 등록 요청 DTO (다중 파일 지원)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialStep3RequestDTO {

    @NotBlank(message = "임시 메모리얼 ID가 필요합니다.")
    private String tempMemorialId;

    // ===== 다중 파일 URL 필드 (필수) =====
    @NotEmpty(message = "프로필 이미지는 5장 필수입니다.")
    @Size(min = 5, max = 5, message = "프로필 이미지는 정확히 5장이어야 합니다.")
    private List<String> profileImageUrls; // 프로필 이미지 URL 리스트 (정확히 5개)

    @NotEmpty(message = "음성 파일은 3개 필수입니다.")
    @Size(min = 3, max = 3, message = "음성 파일은 정확히 3개여야 합니다.")
    private List<String> voiceFileUrls; // 음성 파일 URL 리스트 (정확히 3개)

    @NotBlank(message = "영상 파일은 필수입니다.")
    private String videoFileUrl; // 영상 파일 URL (1개)

    @NotBlank(message = "사용자 이미지는 필수입니다.")
    private String userImageUrl; // 사용자 이미지 URL (1개)

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
     * 모든 필수 파일 보유 여부 확인
     */
    public boolean hasAllRequiredFiles() {
        return getProfileImageCount() == 5 && // 프로필 이미지 정확히 5장
               getVoiceFileCount() == 3 && // 음성 파일 정확히 3개
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

    /**
     * 파일 업로드 진행률 계산 (백분율)
     */
    public double getFileUploadProgress() {
        int completedFiles = 0;
        int totalRequiredFiles = 4; // 프로필 이미지, 음성 파일, 영상 파일, 사용자 이미지

        if (getProfileImageCount() == 5) completedFiles++;
        if (getVoiceFileCount() == 3) completedFiles++;
        if (videoFileUrl != null && !videoFileUrl.trim().isEmpty()) completedFiles++;
        if (userImageUrl != null && !userImageUrl.trim().isEmpty()) completedFiles++;

        return (double) completedFiles / totalRequiredFiles * 100;
    }

    /**
     * 파일 업로드 상태 메시지
     */
    public String getFileUploadStatusMessage() {
        if (hasAllRequiredFiles()) {
            return "모든 파일 업로드 완료";
        }

        StringBuilder status = new StringBuilder();
        if (getProfileImageCount() < 5) {
            status.append("프로필 사진 ").append(getProfileImageCount()).append("/5장 ");
        }
        if (getVoiceFileCount() < 3) {
            status.append("음성 파일 ").append(getVoiceFileCount()).append("/3개 ");
        }
        if (videoFileUrl == null || videoFileUrl.trim().isEmpty()) {
            status.append("영상 파일 필요 ");
        }
        if (userImageUrl == null || userImageUrl.trim().isEmpty()) {
            status.append("사용자 이미지 필요 ");
        }

        return status.toString().trim();
    }

    /**
     * 파일 타입별 업로드 상태 확인
     */
    public FileUploadStatus getFileUploadStatus() {
        return FileUploadStatus.builder()
                .profileImageComplete(getProfileImageCount() == 5)
                .voiceFileComplete(getVoiceFileCount() == 3)
                .videoFileComplete(videoFileUrl != null && !videoFileUrl.trim().isEmpty())
                .userImageComplete(userImageUrl != null && !userImageUrl.trim().isEmpty())
                .allComplete(hasAllRequiredFiles())
                .profileImageCount(getProfileImageCount())
                .voiceFileCount(getVoiceFileCount())
                .progress(getFileUploadProgress())
                .statusMessage(getFileUploadStatusMessage())
                .build();
    }

    /**
     * 파일 업로드 상태 정보 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileUploadStatus {
        private boolean profileImageComplete;
        private boolean voiceFileComplete;
        private boolean videoFileComplete;
        private boolean userImageComplete;
        private boolean allComplete;
        private int profileImageCount;
        private int voiceFileCount;
        private double progress;
        private String statusMessage;
    }

    /**
     * 파일 타입별 필요 개수 정보
     */
    public static class FileRequirements {
        public static final int PROFILE_IMAGE_COUNT = 5;
        public static final int VOICE_FILE_COUNT = 3;
        public static final int VIDEO_FILE_COUNT = 1;
        public static final int USER_IMAGE_COUNT = 1;
        public static final int TOTAL_FILE_TYPES = 4;
    }
}