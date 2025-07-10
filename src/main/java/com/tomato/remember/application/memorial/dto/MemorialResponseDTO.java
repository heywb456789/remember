package com.tomato.remember.application.memorial.dto;

import com.tomato.remember.application.member.code.Gender;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.memorial.code.AiTrainingStatus;
import com.tomato.remember.application.memorial.code.MemorialStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 메모리얼 응답 DTO (새로운 단계별 생성 구조)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialResponseDTO {

    // ===== 기본 정보 =====
    private Long id;
    private String name;
    private String description;
    private Boolean isPublic;
    private MemorialStatus status;

    // ===== 고인 정보 =====
    private Gender gender;
    private Relationship relationship;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private String birthPlace;
    private String residence;
    private String occupation;
    private String contactNumber;
    private String email;
    private String lifeStory;
    private String hobbies;
    private String personality;
    private String specialMemories;
    private String speechHabits;
    private String favoriteFood;
    private String favoritePlace;
    private String favoriteMusic;

    // ===== 다중 파일 URL 필드 =====
    private List<String> profileImageUrls; // 프로필 이미지 URL 리스트 (최대 5개)
    private List<String> voiceFileUrls; // 음성 파일 URL 리스트 (최대 3개)
    private String videoFileUrl; // 영상 파일 URL
    private String userImageUrl; // 사용자 이미지 URL

    // ===== 하위 호환성을 위한 단일 파일 URL 필드 =====
    private String profileImageUrl; // 대표 프로필 이미지 URL
    private String voiceFileUrl; // 대표 음성 파일 URL

    // ===== AI 학습 관련 =====
    private Boolean aiTrainingCompleted;
    private AiTrainingStatus aiTrainingStatus;

    // ===== 방문 및 통계 =====
    private LocalDate lastVisitAt;
    private Integer totalVisits;
    private Integer memoryCount;

    // ===== 소유자 정보 =====
    private Long ownerId;
    private String ownerName;

    // ===== 시스템 정보 =====
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== 계산된 필드 =====
    private Integer age;
    private String formattedAge;
    private Integer familyMemberCount;
    private Boolean canStartVideoCall;
    private Boolean hasRequiredFiles;
    private Boolean hasRecommendedFiles;
    private Boolean isComplete;

    // ===== 다중 파일 관련 편의 메서드 =====

    /**
     * 업로드된 프로필 이미지 개수 반환
     */
    public int getProfileImageCount() {
        return profileImageUrls != null ? profileImageUrls.size() : 0;
    }

    /**
     * 업로드된 음성 파일 개수 반환
     */
    public int getVoiceFileCount() {
        return voiceFileUrls != null ? voiceFileUrls.size() : 0;
    }

    /**
     * 프로필 이미지 최소 요구사항 충족 여부 (1장 이상)
     */
    public boolean hasMinimumProfileImages() {
        return getProfileImageCount() >= 1;
    }

    /**
     * 음성 파일 최소 요구사항 충족 여부 (1개 이상)
     */
    public boolean hasMinimumVoiceFiles() {
        return getVoiceFileCount() >= 1;
    }

    /**
     * 프로필 이미지 권장 요구사항 충족 여부 (5장)
     */
    public boolean hasRecommendedProfileImages() {
        return getProfileImageCount() >= 5;
    }

    /**
     * 음성 파일 권장 요구사항 충족 여부 (3개)
     */
    public boolean hasRecommendedVoiceFiles() {
        return getVoiceFileCount() >= 3;
    }

    /**
     * 영상 파일 업로드 여부
     */
    public boolean hasVideoFile() {
        return videoFileUrl != null && !videoFileUrl.trim().isEmpty();
    }

    /**
     * 사용자 이미지 업로드 여부
     */
    public boolean hasUserImage() {
        return userImageUrl != null && !userImageUrl.trim().isEmpty();
    }

    /**
     * 필수 파일 업로드 진행률 계산 (백분율)
     */
    public double getRequiredFileProgress() {
        int completedFiles = 0;
        int totalRequiredFiles = 2; // 프로필 이미지 1장, 음성 파일 1개

        if (hasMinimumProfileImages()) completedFiles++;
        if (hasMinimumVoiceFiles()) completedFiles++;

        return (double) completedFiles / totalRequiredFiles * 100;
    }

    /**
     * 권장 파일 업로드 진행률 계산 (백분율)
     */
    public double getRecommendedFileProgress() {
        int completedFiles = 0;
        int totalRecommendedFiles = 4; // 프로필 이미지 5장, 음성 파일 3개, 영상 파일, 사용자 이미지

        if (hasRecommendedProfileImages()) completedFiles++;
        if (hasRecommendedVoiceFiles()) completedFiles++;
        if (hasVideoFile()) completedFiles++;
        if (hasUserImage()) completedFiles++;

        return (double) completedFiles / totalRecommendedFiles * 100;
    }

    /**
     * 파일 업로드 상태 메시지
     */
    public String getFileUploadStatusMessage() {
        if (hasRecommendedFiles != null && hasRecommendedFiles) {
            return "모든 권장 파일 업로드 완료";
        }

        if (hasRequiredFiles != null && hasRequiredFiles) {
            return "필수 파일 업로드 완료";
        }

        StringBuilder status = new StringBuilder();
        if (!hasMinimumProfileImages()) {
            status.append("프로필 사진 필요 ");
        } else if (!hasRecommendedProfileImages()) {
            status.append("프로필 사진 ").append(getProfileImageCount()).append("/5장 ");
        }

        if (!hasMinimumVoiceFiles()) {
            status.append("음성 파일 필요 ");
        } else if (!hasRecommendedVoiceFiles()) {
            status.append("음성 파일 ").append(getVoiceFileCount()).append("/3개 ");
        }

        if (!hasVideoFile()) {
            status.append("영상 파일 권장 ");
        }
        if (!hasUserImage()) {
            status.append("사용자 이미지 권장 ");
        }

        return status.toString().trim();
    }

    /**
     * 대표 프로필 이미지 URL 반환 (첫 번째 이미지)
     */
    public String getMainProfileImageUrl() {
        if (profileImageUrls != null && !profileImageUrls.isEmpty()) {
            return profileImageUrls.get(0);
        }
        return profileImageUrl; // 하위 호환성
    }

    /**
     * 대표 음성 파일 URL 반환 (첫 번째 음성)
     */
    public String getMainVoiceFileUrl() {
        if (voiceFileUrls != null && !voiceFileUrls.isEmpty()) {
            return voiceFileUrls.get(0);
        }
        return voiceFileUrl; // 하위 호환성
    }

    // ===== 상태 확인 메서드 =====

    /**
     * 메모리얼 임시저장 상태 확인
     */
    public boolean isMemorialDraft() {
        return status == MemorialStatus.DRAFT;
    }

    /**
     * 메모리얼 활성 상태 확인
     */
    public boolean isMemorialActive() {
        return status == MemorialStatus.ACTIVE;
    }

    /**
     * 메모리얼 비활성 상태 확인
     */
    public boolean isMemorialInactive() {
        return status == MemorialStatus.INACTIVE;
    }

    /**
     * 메모리얼 삭제 상태 확인
     */
    public boolean isMemorialDeleted() {
        return status == MemorialStatus.DELETED;
    }

    /**
     * AI 학습 준비 상태 확인
     */
    public boolean isReadyForAiTraining() {
        return hasRecommendedFiles != null && hasRecommendedFiles &&
               (aiTrainingStatus == AiTrainingStatus.PENDING ||
                aiTrainingStatus == AiTrainingStatus.FAILED);
    }

    /**
     * 영상통화 가능 상태 확인
     */
    public boolean isVideoCallAvailable() {
        return canStartVideoCall != null && canStartVideoCall &&
               aiTrainingCompleted != null && aiTrainingCompleted;
    }

    /**
     * 메모리얼 완료 가능 여부 확인
     */
    public boolean canComplete() {
        return isMemorialDraft() &&
               name != null && !name.trim().isEmpty() &&
               gender != null &&
               relationship != null &&
               hasRequiredFiles != null && hasRequiredFiles;
    }

    /**
     * 단계별 완료 상태 확인
     */
    public boolean isStep1Complete() {
        return name != null && !name.trim().isEmpty();
    }

    public boolean isStep2Complete() {
        return gender != null && relationship != null;
    }

    public boolean isStep3Complete() {
        return hasRequiredFiles != null && hasRequiredFiles;
    }

    /**
     * 전체 완료 진행률 (백분율)
     */
    public double getCompletionProgress() {
        int completedSteps = 0;
        int totalSteps = 3;

        if (isStep1Complete()) completedSteps++;
        if (isStep2Complete()) completedSteps++;
        if (isStep3Complete()) completedSteps++;

        return (double) completedSteps / totalSteps * 100;
    }
}