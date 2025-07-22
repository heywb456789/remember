package com.tomato.remember.application.memorial.entity;

import com.tomato.remember.application.family.code.InviteStatus;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.member.code.Gender;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.code.AiTrainingStatus;
import com.tomato.remember.application.memorial.code.MemorialFileType;
import com.tomato.remember.application.memorial.code.MemorialStatus;
import com.tomato.remember.application.wsvideo.entity.VideoCall;
import com.tomato.remember.common.audit.Audit;
import jakarta.persistence.*;
import java.util.Objects;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.ArrayList;
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
    private MemorialStatus status = MemorialStatus.ACTIVE;

    // ===== 고인 정보 =====

    @Comment("호칭/별명")
    @Column(nullable = false, length = 30)
    private String nickname;

    @Comment("성별")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Comment("등록자(생성자)와 고인의 관계")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Relationship relationship;

    @Comment("생년월일")
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Comment("기일")
    @Column(name = "death_date")
    private LocalDate deathDate;

    // ===== 고인 상세 정보 (기획서 기준) =====

    @Comment("성격이나 특징")
    @Column(length = 500)
    private String personality;

    @Comment("취미나 관심사")
    @Column(length = 300)
    private String hobbies;

    @Comment("좋아하는 음식")
    @Column(length = 300, name = "favorite_food")
    private String favoriteFood;

    @Comment("기억에 남는 일화나 추억")
    @Column(length = 300, name = "special_memories")
    private String specialMemories;

    @Comment("습관이나 말버릇")
    @Column(length = 300, name = "speech_habits")
    private String speechHabits;

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

    @OneToMany(mappedBy = "memorial", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemorialFile> memorialFiles = new ArrayList<>();

    // ===== 헬퍼 메서드 - 파일 관련 =====

    /**
     * 프로필 이미지 목록 조회
     */
    public List<MemorialFile> getProfileImages() {
        return memorialFiles.stream()
            .filter(file -> file.getFileType() == MemorialFileType.PROFILE_IMAGE)
            .sorted((f1, f2) -> Integer.compare(f1.getSortOrder(), f2.getSortOrder()))
            .collect(Collectors.toList());
    }

    /**
     * 음성 파일 목록 조회
     */
    public List<MemorialFile> getVoiceFiles() {
        return memorialFiles.stream()
            .filter(file -> file.getFileType() == MemorialFileType.VOICE_FILE)
            .sorted((f1, f2) -> Integer.compare(f1.getSortOrder(), f2.getSortOrder()))
            .collect(Collectors.toList());
    }

    /**
     * 비디오 파일 조회
     */
    public MemorialFile getVideoFile() {
        return memorialFiles.stream()
            .filter(file -> file.getFileType() == MemorialFileType.VIDEO_FILE)
            .findFirst()
            .orElse(null);
    }

    /**
     * 대표 프로필 이미지 URL 조회
     */
    public String getMainProfileImageUrl() {
        List<MemorialFile> profileImages = getProfileImages();
        return profileImages.isEmpty() ? null : profileImages.get(0).getFileUrl();
    }

    /**
     * 파일 추가
     */
    public void addFile(MemorialFile file) {
        file.setMemorial(this);
        this.memorialFiles.add(file);
    }

    /**
     * 파일 제거
     */
    public void removeFile(MemorialFile file) {
        this.memorialFiles.remove(file);
        file.setMemorial(null);
    }

    /**
     * 특정 타입의 파일 개수 조회
     */
    public int getFileCount(MemorialFileType fileType) {
        return (int) memorialFiles.stream()
            .filter(file -> file.getFileType() == fileType)
            .count();
    }
    // ===== 헬퍼 메서드 - 가족 구성원 관련 =====

    /**
     * 활성 가족 구성원 목록 조회
     */
    public List<FamilyMember> getActiveFamilyMembers() {
        return familyMembers.stream()
                .filter(fm -> fm.getInviteStatus() == InviteStatus.ACCEPTED)
                .collect(Collectors.toList());
    }

    /**
     * 대기 중인 초대 목록 조회
     */
    public List<FamilyMember> getPendingInvitations() {
        return familyMembers.stream()
                .filter(fm -> fm.getInviteStatus() == InviteStatus.PENDING)
                .collect(Collectors.toList());
    }

    /**
     * 가족 구성원 추가
     */
    public void addFamilyMember(FamilyMember familyMember) {
        familyMember.setMemorial(this);
        this.familyMembers.add(familyMember);
    }

    /**
     * 가족 구성원 제거
     */
    public void removeFamilyMember(FamilyMember familyMember) {
        this.familyMembers.remove(familyMember);
        familyMember.setMemorial(null);
    }

    /**
     * 특정 회원이 가족 구성원인지 확인
     */
    public boolean isFamilyMember(Member member) {
        return familyMembers.stream()
                .anyMatch(fm -> fm.getMember().equals(member) &&
                        fm.getInviteStatus() == InviteStatus.ACCEPTED);
    }

    /**
     * 특정 회원의 가족 관계 조회
     */
    public FamilyMember getFamilyMember(Member member) {
        if (member == null || member.getId() == null) {
            return null;
        }
        return familyMembers.stream()
            // ID 비교로 바꿔주면 서로 다른 인스턴스라도 동일한 DB row면 매칭됩니다.
            .filter(fm -> fm.getMember() != null
                       && Objects.equals(fm.getMember().getId(), member.getId()))
            .findFirst()
            .orElse(null);
    }

    /**
     * 활성 가족 구성원 수 조회
     */
    public int getActiveFamilyMemberCount() {
        return getActiveFamilyMembers().size();
    }

    /**
     * 대기 중인 초대 수 조회
     */
    public int getPendingInvitationCount() {
        return getPendingInvitations().size();
    }


    // ===== 헬퍼 메서드 - 비즈니스 로직 =====

    /**
     * 고인 정보 업데이트
     */
    public void updateDeceasedInfo(String personality, String hobbies, String favoriteFood,
                                   String specialMemories, String speechHabits) {
        this.personality = personality;
        this.hobbies = hobbies;
        this.favoriteFood = favoriteFood;
        this.specialMemories = specialMemories;
        this.speechHabits = speechHabits;
    }

    /**
     * 필수 파일 확인
     */
    public boolean hasRequiredFiles() {
        return getFileCount(MemorialFileType.PROFILE_IMAGE) >= 5 &&
               getFileCount(MemorialFileType.VOICE_FILE) >= 3 &&
               getFileCount(MemorialFileType.VIDEO_FILE) >= 1;
    }

    /**
     * 완전성 확인
     */
    public boolean isComplete() {
        return this.name != null && !this.name.trim().isEmpty() &&
               this.nickname != null && !this.nickname.trim().isEmpty() &&
               this.gender != null &&
               this.relationship != null &&
               hasRequiredFiles();
    }

    /**
     * 방문 기록
     */
    public void recordVisit() {
        this.lastVisitAt = LocalDate.now();
        this.totalVisits = (this.totalVisits == null ? 0 : this.totalVisits) + 1;
    }

    /**
     * 영상통화 가능 여부 확인
     */
    public boolean canStartVideoCall() {
        return this.isActive() &&
               this.aiTrainingCompleted &&
               this.aiTrainingStatus == AiTrainingStatus.COMPLETED &&
               hasRequiredFiles();
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

    // ===== 상태 관리 메서드 =====

    /**
     * 상태 확인 메서드들
     */
    public boolean isActive() {
        return this.status == MemorialStatus.ACTIVE;
    }

    public boolean isInactive() {
        return this.status == MemorialStatus.INACTIVE;
    }

    public boolean isDeleted() {
        return this.status == MemorialStatus.DELETED;
    }

    /**
     * 권한 확인 메서드
     */
    public boolean canBeViewedBy(Member member) {
        if (this.isDeleted()) {
            return false;
        }

        if (this.owner.equals(member)) {
            return true;
        }

        if (this.isPublic && this.isActive()) {
            return true;
        }

        return familyMembers.stream()
            .anyMatch(fm -> fm.getMember().equals(member) &&
                fm.getInviteStatus() == InviteStatus.ACCEPTED);
    }

    // ===== AI 학습 관련 메서드 =====

    public void startAiTraining() {
        this.aiTrainingCompleted = false;
        this.aiTrainingStatus = AiTrainingStatus.IN_PROGRESS;
    }

    public void completeAiTraining() {
        this.aiTrainingCompleted = true;
        this.aiTrainingStatus = AiTrainingStatus.COMPLETED;
    }

    public void failAiTraining() {
        this.aiTrainingCompleted = false;
        this.aiTrainingStatus = AiTrainingStatus.FAILED;
    }

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


    // ===== 영상통화 관련 메서드 =====

    public List<VideoCall> getRecentVideoCalls(int limit) {
        return videoCalls.stream()
            .sorted((v1, v2) -> v2.getCreatedAt().compareTo(v1.getCreatedAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    // ===== 메모리 관련 메서드 =====

    public void increaseMemoryCount() {
        this.memoryCount++;
    }

    public void decreaseMemoryCount() {
        if (this.memoryCount > 0) {
            this.memoryCount--;
        }
    }
}