package com.tomato.remember.application.memorial.entity;

import com.tomato.remember.application.family.code.FamilyRole;
import com.tomato.remember.application.family.code.InviteStatus;
import com.tomato.remember.application.member.code.Gender;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.memorial.code.AiTrainingStatus;
import com.tomato.remember.application.memorial.code.InterestType;
import com.tomato.remember.application.memorial.code.MemorialStatus;
import com.tomato.remember.application.videocall.entity.VideoCall;
import com.tomato.remember.common.audit.Audit;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

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

    @Comment("망자 이름")
    @Column(nullable = false, length = 50)
    private String name;

    @Comment("망자 호칭 (할머니, 아버지 등)")
    @Column(nullable = false, length = 30)
    private String nickname;

    @Comment("성별")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Comment("생년월일")
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Comment("기일")
    @Column(name = "death_date")
    private LocalDate deathDate;

    @Comment("등록자와의 관계")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Relationship relationship;

    @Comment("관심사 목록 (JSON 형태)")
    @Column(length = 1000)
    private String interests;

    @Comment("프로필 이미지 URL")
    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Comment("음성 파일 URL")
    @Column(name = "voice_file_url")
    private String voiceFileUrl;

    @Comment("영상 파일 URL")
    @Column(name = "video_file_url")
    private String videoFileUrl;

    @Comment("사용자 이미지 URL (AI 학습용)")
    @Column(name = "user_image_url")
    private String userImageUrl;

    @Comment("AI 학습 완료 여부")
    @Column(nullable = false, name = "ai_training_completed")
    @Builder.Default
    private Boolean aiTrainingCompleted = false;

    @Comment("AI 학습 상태")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "ai_training_status")
    @Builder.Default
    private AiTrainingStatus aiTrainingStatus = AiTrainingStatus.PENDING;

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

    @Comment("메모리얼 상태")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MemorialStatus status = MemorialStatus.ACTIVE;

    @Comment("공개 여부")
    @Column(nullable = false, name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Comment("추가 정보 (JSON)")
    @Column(columnDefinition = "TEXT", name = "additional_info")
    private String additionalInfo;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @OneToMany(mappedBy = "memorial", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FamilyMember> familyMembers = new ArrayList<>();

    @OneToMany(mappedBy = "memorial", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VideoCall> videoCalls = new ArrayList<>();

    // 비즈니스 메서드
    public void setName(String name) {
        this.name = name;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public void setDeathDate(LocalDate deathDate) {
        this.deathDate = deathDate;
    }

    public void setRelationship(Relationship relationship) {
        this.relationship = relationship;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setVoiceFileUrl(String voiceFileUrl) {
        this.voiceFileUrl = voiceFileUrl;
    }

    public void setVideoFileUrl(String videoFileUrl) {
        this.videoFileUrl = videoFileUrl;
    }

    public void setUserImageUrl(String userImageUrl) {
        this.userImageUrl = userImageUrl;
    }

    public void setStatus(MemorialStatus status) {
        this.status = status;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public void setAiTrainingStatus(AiTrainingStatus aiTrainingStatus) {
        this.aiTrainingStatus = aiTrainingStatus;
    }

    public void completeAiTraining() {
        this.aiTrainingCompleted = true;
        this.aiTrainingStatus = AiTrainingStatus.COMPLETED;
    }

    public void failAiTraining() {
        this.aiTrainingCompleted = false;
        this.aiTrainingStatus = AiTrainingStatus.FAILED;
    }

    public void startAiTraining() {
        this.aiTrainingCompleted = false;
        this.aiTrainingStatus = AiTrainingStatus.IN_PROGRESS;
    }

    public void recordVisit() {
        this.lastVisitAt = LocalDate.now();
        this.totalVisits++;
    }

    public void increaseMemoryCount() {
        this.memoryCount++;
    }

    public void decreaseMemoryCount() {
        if (this.memoryCount > 0) {
            this.memoryCount--;
        }
    }

    public boolean isReadyForVideoCall() {
        return profileImageUrl != null &&
               voiceFileUrl != null &&
               videoFileUrl != null &&
               userImageUrl != null &&
               aiTrainingCompleted &&
               status == MemorialStatus.ACTIVE;
    }

    public int getFamilyMemberCount() {
        return familyMembers.size();
    }

    public List<InterestType> getInterestTypeList() {
        if (interests == null || interests.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<InterestType> interestList = new ArrayList<>();
        String[] interestStrings = interests.split(",");

        for (String interestString : interestStrings) {
            try {
                InterestType interest = InterestType.valueOf(interestString.trim());
                interestList.add(interest);
            } catch (IllegalArgumentException e) {
                // 잘못된 관심사 무시
            }
        }

        return interestList;
    }

    public void setInterestTypes(List<InterestType> interestTypes) {
        if (interestTypes == null || interestTypes.isEmpty()) {
            this.interests = null;
        } else {
            this.interests = interestTypes.stream()
                .map(InterestType::name)
                .collect(Collectors.joining(","));
        }
    }

    public void addInterest(InterestType interestType) {
        List<InterestType> currentInterests = new ArrayList<>(getInterestTypeList());
        if (!currentInterests.contains(interestType)) {
            currentInterests.add(interestType);
            setInterestTypes(currentInterests);
        }
    }

    public void removeInterest(InterestType interestType) {
        List<InterestType> currentInterests = new ArrayList<>(getInterestTypeList());
        currentInterests.remove(interestType);
        setInterestTypes(currentInterests);
    }

    public boolean hasInterest(InterestType interestType) {
        return getInterestTypeList().contains(interestType);
    }

    public boolean isActive() {
        return status == MemorialStatus.ACTIVE;
    }

    public boolean isInactive() {
        return status == MemorialStatus.INACTIVE;
    }

    public boolean isDeleted() {
        return status == MemorialStatus.DELETED;
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

    public boolean hasRequiredFiles() {
        return profileImageUrl != null &&
               voiceFileUrl != null &&
               videoFileUrl != null &&
               userImageUrl != null;
    }

    public boolean canStartVideoCall() {
        return isActive() && isReadyForVideoCall();
    }

    public boolean canBeEditedBy(Member member) {
        return owner.equals(member) ||
               familyMembers.stream()
                   .anyMatch(fm -> fm.getMember().equals(member) &&
                                  fm.getFamilyRole() == FamilyRole.MAIN);
    }

    public boolean canBeViewedBy(Member member) {
        return isPublic ||
               owner.equals(member) ||
               familyMembers.stream()
                   .anyMatch(fm -> fm.getMember().equals(member) &&
                                  fm.getInviteStatus() == InviteStatus.ACCEPTED);
    }

    public int getAge() {
        if (birthDate == null || deathDate == null) {
            return 0;
        }
        return deathDate.getYear() - birthDate.getYear();
    }

    public String getFormattedAge() {
        int age = getAge();
        return age > 0 ? age + "세" : "나이 정보 없음";
    }

    public String getInterestDisplayNames() {
        return getInterestTypeList().stream()
            .map(InterestType::getDisplayName)
            .collect(Collectors.joining(", "));
    }

    public List<VideoCall> getRecentVideoCalls(int limit) {
        return videoCalls.stream()
            .sorted((v1, v2) -> v2.getCreatedAt().compareTo(v1.getCreatedAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<FamilyMember> getActiveFamilyMembers() {
        return familyMembers.stream()
            .filter(fm -> fm.getInviteStatus() == InviteStatus.ACCEPTED)
            .collect(Collectors.toList());
    }
}