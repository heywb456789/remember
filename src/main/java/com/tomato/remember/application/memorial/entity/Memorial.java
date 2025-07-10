package com.tomato.remember.application.memorial.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomato.remember.application.family.code.FamilyRole;
import com.tomato.remember.application.family.code.InviteStatus;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.member.code.Gender;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.code.AiTrainingStatus;
import com.tomato.remember.application.memorial.code.MemorialStatus;
import com.tomato.remember.application.memorial.dto.MemorialResponseDTO;
import com.tomato.remember.application.videocall.entity.VideoCall;
import com.tomato.remember.common.audit.Audit;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 메모리얼 엔티티
 */
@Slf4j
@Table(
    name = "t_memorial",
    indexes = {
        @Index(name = "idx01_t_memorial", columnList = "created_at"),
        @Index(name = "idx02_t_memorial", columnList = "owner_id"),
        @Index(name = "idx03_t_memorial", columnList = "name"),
        @Index(name = "idx04_t_memorial", columnList = "gender"),
        @Index(name = "idx05_t_memorial", columnList = "relationship"),
        @Index(name = "idx06_t_memorial", columnList = "status"),
        @Index(name = "idx07_t_memorial", columnList = "ai_training_status")
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Memorial extends Audit {

    // ===== 기본 정보 =====

    @Comment("메모리얼 이름")
    @Column(nullable = false, length = 50)
    private String name;

    @Comment("메모리얼 설명")
    @Column(length = 500)
    private String description;

    @Comment("공개 여부")
    @Column(nullable = false, name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Comment("메모리얼 상태")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MemorialStatus status = MemorialStatus.DRAFT;

    // ===== 고인 정보 =====

    @Comment("성별")
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Comment("등록자와의 관계")
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Relationship relationship;

    @Comment("생년월일")
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Comment("기일")
    @Column(name = "death_date")
    private LocalDate deathDate;

    @Comment("출생지")
    @Column(length = 100, name = "birth_place")
    private String birthPlace;

    @Comment("거주지")
    @Column(length = 100)
    private String residence;

    @Comment("직업")
    @Column(length = 100)
    private String occupation;

    @Comment("연락처")
    @Column(length = 15, name = "contact_number")
    private String contactNumber;

    @Comment("이메일")
    @Column(length = 100)
    private String email;

    @Comment("생전 이야기")
    @Column(length = 1000, name = "life_story")
    private String lifeStory;

    @Comment("취미")
    @Column(length = 500)
    private String hobbies;

    @Comment("성격")
    @Column(length = 500)
    private String personality;

    @Comment("특별한 기억")
    @Column(length = 1000, name = "special_memories")
    private String specialMemories;

    @Comment("말버릇")
    @Column(length = 500, name = "speech_habits")
    private String speechHabits;

    @Comment("좋아하는 음식")
    @Column(length = 500, name = "favorite_food")
    private String favoriteFood;

    @Comment("좋아하는 장소")
    @Column(length = 500, name = "favorite_place")
    private String favoritePlace;

    @Comment("좋아하는 음악")
    @Column(length = 500, name = "favorite_music")
    private String favoriteMusic;

    // ===== 미디어 파일 URL (다중 파일 지원) =====

    @Comment("프로필 이미지 URL들 (JSON 배열, 최대 5개)")
    @Column(name = "profile_image_urls", columnDefinition = "TEXT")
    private String profileImageUrls;

    @Comment("음성 파일 URL들 (JSON 배열, 최대 3개)")
    @Column(name = "voice_file_urls", columnDefinition = "TEXT")
    private String voiceFileUrls;

    @Comment("영상 파일 URL")
    @Column(name = "video_file_url", columnDefinition = "TEXT")
    private String videoFileUrl;

    @Comment("사용자 이미지 URL (AI 학습용)")
    @Column(name = "user_image_url", columnDefinition = "TEXT")
    private String userImageUrl;

    // ===== 하위 호환성을 위한 단일 파일 URL =====

    @Comment("대표 프로필 이미지 URL (하위 호환성)")
    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Comment("대표 음성 파일 URL (하위 호환성)")
    @Column(name = "voice_file_url", columnDefinition = "TEXT")
    private String voiceFileUrl;

    // ===== AI 학습 관련 =====

    @Comment("AI 학습 완료 여부")
    @Column(nullable = false, name = "ai_training_completed")
    @Builder.Default
    private Boolean aiTrainingCompleted = false;

    @Comment("AI 학습 상태")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "ai_training_status")
    @Builder.Default
    private AiTrainingStatus aiTrainingStatus = AiTrainingStatus.PENDING;

    // ===== 방문 및 통계 =====

    @Comment("마지막 방문 일시")
    @Column(name = "last_visit_at")
    private LocalDate lastVisitAt;

    @Comment("총 방문 횟수")
    @Column(nullable = false, name = "total_visits")
    @Builder.Default
    private Integer totalVisits = 0;

    @Comment("저장된 메모리 개수")
    @Column(nullable = false, name = "memory_count")
    @Builder.Default
    private Integer memoryCount = 0;

    // ===== 연관관계 =====

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @OneToMany(mappedBy = "memorial", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FamilyMember> familyMembers = new ArrayList<>();

    @OneToMany(mappedBy = "memorial", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VideoCall> videoCalls = new ArrayList<>();

    // ===== 새로운 단계별 생성 메서드 =====

    /**
     * 고인 정보 업데이트 (2단계)
     */
    public void updateDeceasedInfo(Gender gender, Relationship relationship,
                                   LocalDate birthDate, LocalDate deathDate,
                                   String birthPlace, String residence, String occupation,
                                   String contactNumber, String email, String lifeStory,
                                   String hobbies, String personality, String specialMemories,
                                   String speechHabits, String favoriteFood, String favoritePlace,
                                   String favoriteMusic) {
        this.gender = gender;
        this.relationship = relationship;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
        this.birthPlace = birthPlace;
        this.residence = residence;
        this.occupation = occupation;
        this.contactNumber = contactNumber;
        this.email = email;
        this.lifeStory = lifeStory;
        this.hobbies = hobbies;
        this.personality = personality;
        this.specialMemories = specialMemories;
        this.speechHabits = speechHabits;
        this.favoriteFood = favoriteFood;
        this.favoritePlace = favoritePlace;
        this.favoriteMusic = favoriteMusic;
    }

    /**
     * 프로필 이미지들 업데이트 (JSON 배열로 저장)
     */
    public void updateProfileImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            this.profileImageUrls = null;
            this.profileImageUrl = null;
            return;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.profileImageUrls = objectMapper.writeValueAsString(imageUrls);
            this.profileImageUrl = imageUrls.get(0); // 첫 번째를 대표 이미지로 설정
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize profile image URLs", e);
            // 실패 시 첫 번째 이미지만 저장
            this.profileImageUrl = imageUrls.get(0);
        }
    }

    /**
     * 음성 파일들 업데이트 (JSON 배열로 저장)
     */
    public void updateVoiceFiles(List<String> voiceUrls) {
        if (voiceUrls == null || voiceUrls.isEmpty()) {
            this.voiceFileUrls = null;
            this.voiceFileUrl = null;
            return;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.voiceFileUrls = objectMapper.writeValueAsString(voiceUrls);
            this.voiceFileUrl = voiceUrls.get(0); // 첫 번째를 대표 파일로 설정
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize voice file URLs", e);
            // 실패 시 첫 번째 파일만 저장
            this.voiceFileUrl = voiceUrls.get(0);
        }
    }

    /**
     * 비디오 파일 업데이트
     */
    public void updateVideoFile(String videoUrl) {
        this.videoFileUrl = videoUrl;
    }

    /**
     * 사용자 이미지 업데이트
     */
    public void updateUserImage(String userImageUrl) {
        this.userImageUrl = userImageUrl;
    }

    // ===== 파일 URL 목록 조회 메서드 =====

    /**
     * 프로필 이미지 URL 목록 조회
     */
    public List<String> getProfileImageUrlList() {
        if (this.profileImageUrls == null || this.profileImageUrls.trim().isEmpty()) {
            // 하위 호환성: 기존 단일 URL이 있으면 반환
            if (this.profileImageUrl != null) {
                return Arrays.asList(this.profileImageUrl);
            }
            return new ArrayList<>();
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(this.profileImageUrls,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize profile image URLs", e);
            return new ArrayList<>();
        }
    }

    /**
     * 음성 파일 URL 목록 조회
     */
    public List<String> getVoiceFileUrlList() {
        if (this.voiceFileUrls == null || this.voiceFileUrls.trim().isEmpty()) {
            // 하위 호환성: 기존 단일 URL이 있으면 반환
            if (this.voiceFileUrl != null) {
                return Arrays.asList(this.voiceFileUrl);
            }
            return new ArrayList<>();
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(this.voiceFileUrls,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize voice file URLs", e);
            return new ArrayList<>();
        }
    }

    // ===== 상태 관리 메서드 =====

    /**
     * 상태 설정
     */
    public void setStatus(MemorialStatus status) {
        this.status = status;
    }

    /**
     * 상태 확인 메서드들
     */
    public boolean isDraft() {
        return this.status == MemorialStatus.DRAFT;
    }

    public boolean isActive() {
        return this.status == MemorialStatus.ACTIVE;
    }

    public boolean isInactive() {
        return this.status == MemorialStatus.INACTIVE;
    }

    public boolean isDeleted() {
        return this.status == MemorialStatus.DELETED;
    }

    // ===== 권한 확인 메서드 =====

    /**
     * 볼 수 있는 권한 확인
     */
    public boolean canBeViewedBy(Member member) {
        // 삭제된 메모리얼은 볼 수 없음
        if (this.isDeleted()) {
            return false;
        }

        // 소유자는 항상 볼 수 있음
        if (this.owner.equals(member)) {
            return true;
        }

        // 공개 메모리얼이고 활성 상태면 볼 수 있음
        if (this.isPublic && this.isActive()) {
            return true;
        }

        // 가족 구성원은 볼 수 있음
        return familyMembers.stream()
                .anyMatch(fm -> fm.getMember().equals(member) &&
                        fm.getInviteStatus() == InviteStatus.ACCEPTED);
    }

    /**
     * 수정할 수 있는 권한 확인
     */
    public boolean canBeEditedBy(Member member) {
        // 삭제된 메모리얼은 수정할 수 없음
        if (this.isDeleted()) {
            return false;
        }

        // 소유자는 항상 수정 가능
        if (this.owner.equals(member)) {
            return true;
        }

        // 메인 가족 구성원은 수정 가능
        return familyMembers.stream()
                .anyMatch(fm -> fm.getMember().equals(member) &&
                        fm.getFamilyRole() == FamilyRole.MAIN &&
                        fm.getInviteStatus() == InviteStatus.ACCEPTED);
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 방문 기록
     */
    public void recordVisit() {
        this.lastVisitAt = LocalDate.now();
        this.totalVisits = (this.totalVisits == null ? 0 : this.totalVisits) + 1;
    }

    /**
     * 필수 파일 확인
     */
    public boolean hasRequiredFiles() {
        List<String> profileImages = getProfileImageUrlList();
        List<String> voiceFiles = getVoiceFileUrlList();

        // 프로필 이미지 최소 1개, 음성 파일 최소 1개 필요
        return !profileImages.isEmpty() && !voiceFiles.isEmpty();
    }

    /**
     * 권장 파일 확인 (AI 학습용)
     */
    public boolean hasRecommendedFiles() {
        List<String> profileImages = getProfileImageUrlList();
        List<String> voiceFiles = getVoiceFileUrlList();

        // 프로필 이미지 5개, 음성 파일 3개, 비디오 파일 1개 권장
        return profileImages.size() >= 5 &&
               voiceFiles.size() >= 3 &&
               this.videoFileUrl != null;
    }

    /**
     * 완전성 확인
     */
    public boolean isComplete() {
        return this.name != null && !this.name.trim().isEmpty() &&
               this.gender != null &&
               this.relationship != null &&
               hasRequiredFiles();
    }

    /**
     * 영상통화 가능 여부 확인
     */
    public boolean canStartVideoCall() {
        return this.isActive() &&
               this.aiTrainingCompleted &&
               this.aiTrainingStatus == AiTrainingStatus.COMPLETED &&
               hasRecommendedFiles();
    }

    /**
     * 나이 계산
     */
    public int getAge() {
        if (birthDate == null || deathDate == null) {
            return 0;
        }
        return deathDate.getYear() - birthDate.getYear();
    }

    /**
     * 나이 포맷팅
     */
    public String getFormattedAge() {
        int age = getAge();
        return age > 0 ? age + "세" : "나이 정보 없음";
    }

    // ===== AI 학습 관련 메서드 =====

    /**
     * AI 학습 시작
     */
    public void startAiTraining() {
        this.aiTrainingCompleted = false;
        this.aiTrainingStatus = AiTrainingStatus.IN_PROGRESS;
    }

    /**
     * AI 학습 완료
     */
    public void completeAiTraining() {
        this.aiTrainingCompleted = true;
        this.aiTrainingStatus = AiTrainingStatus.COMPLETED;
    }

    /**
     * AI 학습 실패
     */
    public void failAiTraining() {
        this.aiTrainingCompleted = false;
        this.aiTrainingStatus = AiTrainingStatus.FAILED;
    }

    /**
     * AI 학습 상태 확인
     */
    public boolean isAiTrainingCompleted() {
        return aiTrainingCompleted && aiTrainingStatus == AiTrainingStatus.COMPLETED;
    }

    public boolean isAiTrainingInProgress() {
        return aiTrainingStatus == AiTrainingStatus.IN_PROGRESS;
    }

    public boolean isAiTrainingPending() {
        return aiTrainingStatus == AiTrainingStatus.PENDING;
    }

    public boolean isAiTrainingFailed() {
        return aiTrainingStatus == AiTrainingStatus.FAILED;
    }

    // ===== 가족 구성원 관련 메서드 =====

    /**
     * 가족 구성원 수 조회
     */
    public int getFamilyMemberCount() {
        return familyMembers.size();
    }

    /**
     * 활성 가족 구성원 조회
     */
    public List<FamilyMember> getActiveFamilyMembers() {
        return familyMembers.stream()
                .filter(fm -> fm.getInviteStatus() == InviteStatus.ACCEPTED)
                .collect(Collectors.toList());
    }

    // ===== 영상통화 관련 메서드 =====

    /**
     * 최근 영상통화 기록 조회
     */
    public List<VideoCall> getRecentVideoCalls(int limit) {
        return videoCalls.stream()
                .sorted((v1, v2) -> v2.getCreatedAt().compareTo(v1.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ===== 메모리 관련 메서드 =====

    /**
     * 메모리 개수 증가
     */
    public void increaseMemoryCount() {
        this.memoryCount++;
    }

    /**
     * 메모리 개수 감소
     */
    public void decreaseMemoryCount() {
        if (this.memoryCount > 0) {
            this.memoryCount--;
        }
    }

    // ===== DTO 변환 메서드 =====

    /**
     * MemorialResponseDTO로 변환
     */
    public MemorialResponseDTO toResponseDTO() {
        return MemorialResponseDTO.builder()
                .id(this.id)
                .name(this.name)
                .description(this.description)
                .gender(this.gender)
                .relationship(this.relationship)
                .birthDate(this.birthDate)
                .deathDate(this.deathDate)
                .birthPlace(this.birthPlace)
                .residence(this.residence)
                .occupation(this.occupation)
                .contactNumber(this.contactNumber)
                .email(this.email)
                .lifeStory(this.lifeStory)
                .hobbies(this.hobbies)
                .personality(this.personality)
                .specialMemories(this.specialMemories)
                .speechHabits(this.speechHabits)
                .favoriteFood(this.favoriteFood)
                .favoritePlace(this.favoritePlace)
                .favoriteMusic(this.favoriteMusic)
                .profileImageUrls(this.getProfileImageUrlList())
                .voiceFileUrls(this.getVoiceFileUrlList())
                .profileImageUrl(this.profileImageUrl) // 하위 호환성
                .voiceFileUrl(this.voiceFileUrl) // 하위 호환성
                .videoFileUrl(this.videoFileUrl)
                .userImageUrl(this.userImageUrl)
                .aiTrainingCompleted(this.aiTrainingCompleted)
                .aiTrainingStatus(this.aiTrainingStatus)
                .lastVisitAt(this.lastVisitAt)
                .totalVisits(this.totalVisits)
                .memoryCount(this.memoryCount)
                .status(this.status)
                .isPublic(this.isPublic)
                .ownerId(this.owner.getId())
                .ownerName(this.owner.getName())
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .age(this.getAge())
                .formattedAge(this.getFormattedAge())
                .familyMemberCount(this.getFamilyMemberCount())
                .canStartVideoCall(this.canStartVideoCall())
                .hasRequiredFiles(this.hasRequiredFiles())
                .hasRecommendedFiles(this.hasRecommendedFiles())
                .isComplete(this.isComplete())
                .build();
    }
}