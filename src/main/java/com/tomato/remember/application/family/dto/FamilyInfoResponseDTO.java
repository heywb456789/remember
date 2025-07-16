package com.tomato.remember.application.family.dto;

import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.member.code.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 가족 구성원용 고인 상세 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyInfoResponseDTO {

    // ===== 메모리얼 기본 정보 (ReadOnly) =====

    /**
     * 메모리얼 ID
     */
    private Long memorialId;

    /**
     * 고인 이름
     */
    private String name;

    /**
     * 호칭
     */
    private String nickname;

    /**
     * 성별
     */
    private Gender gender;

    /**
     * 성별 표시명
     */
    private String genderDisplayName;

    /**
     * 생년월일
     */
    private LocalDate birthDate;

    /**
     * 기일
     */
    private LocalDate deathDate;

    /**
     * 포맷팅된 생년월일 (기존 HTML 호환)
     */
    private String formattedBirthDate;

    /**
     * 포맷팅된 기일 (기존 HTML 호환)
     */
    private String formattedDeathDate;

    /**
     * 나이 (향년)
     */
    private String formattedAge;

    /**
     * 가족 구성원의 관계 (초대 시 설정된 관계)
     */
    private String relationship;

    /**
     * 관계 표시명
     */
    private String relationshipDisplayName;

    // ===== 가족 구성원 입력 정보 =====

    /**
     * 성격이나 특징
     */
    private String personality;

    /**
     * 취미나 관심사
     */
    private String hobbies;

    /**
     * 좋아하는 음식
     */
    private String favoriteFood;

    /**
     * 기억에 남는 일화나 추억
     */
    private String specialMemories;

    /**
     * 습관이나 말버릇
     */
    private String speechHabits;

    // ===== 상태 정보 =====

    /**
     * 고인 상세 정보 입력 여부
     */
    private Boolean hasDeceasedInfo;

    /**
     * 필수 정보 완성 여부 (5개 필드 모두 입력)
     */
    private Boolean hasRequiredDeceasedInfo;

    /**
     * 입력된 필드 개수
     */
    private Integer filledFieldCount;

    /**
     * 완성도 퍼센트 (0-100)
     */
    private Integer completionPercent;

    /**
     * 수정 가능 여부 (이미 입력된 경우 false)
     */
    private Boolean canModify;

    /**
     * 영상통화 가능 여부
     */
    private Boolean canStartVideoCall;

    /**
     * 영상통화 차단 사유
     */
    private String videoCallBlockReason;

    /**
     * FamilyMember와 Memorial로부터 DTO 생성
     */
    public static FamilyInfoResponseDTO from(FamilyMember familyMember, Memorial memorial) {
        return FamilyInfoResponseDTO.builder()
                // 메모리얼 기본 정보
                .memorialId(memorial.getId())
                .name(memorial.getName())
                .nickname(memorial.getNickname())
                .gender(memorial.getGender())
                .genderDisplayName(memorial.getGender().getDisplayName())
                .birthDate(memorial.getBirthDate())
                .deathDate(memorial.getDeathDate())
                .formattedBirthDate(formatDate(memorial.getBirthDate()))
                .formattedDeathDate(formatDate(memorial.getDeathDate()))
                .formattedAge(memorial.getFormattedAge())
                .relationship(familyMember.getRelationship().name())
                .relationshipDisplayName(familyMember.getRelationshipDisplayName())

                // 가족 구성원 입력 정보 (Memorial에서 가져옴)
                .personality(null)
                .hobbies(null)
                .favoriteFood(null)
                .specialMemories(null)
                .speechHabits(null)

                // 상태 정보
                .hasDeceasedInfo(hasDeceasedInfo(memorial))
                .hasRequiredDeceasedInfo(hasRequiredDeceasedInfo(memorial))
                .filledFieldCount(calculateFilledFieldCount(memorial))
                .completionPercent(calculateCompletionPercent(memorial))
                .canModify(!hasDeceasedInfo(memorial)) // 이미 입력된 경우 수정 불가
                .canStartVideoCall(hasRequiredDeceasedInfo(memorial) && memorial.canStartVideoCall())
                .videoCallBlockReason(getVideoCallBlockReason(memorial))
                .build();
    }

    /**
     * 날짜 포맷팅
     */
    private static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * 고인 상세 정보 입력 여부 확인
     */
    private static Boolean hasDeceasedInfo(Memorial memorial) {
        return memorial.getPersonality() != null && !memorial.getPersonality().trim().isEmpty() &&
               memorial.getHobbies() != null && !memorial.getHobbies().trim().isEmpty() &&
               memorial.getFavoriteFood() != null && !memorial.getFavoriteFood().trim().isEmpty() &&
               memorial.getSpecialMemories() != null && !memorial.getSpecialMemories().trim().isEmpty() &&
               memorial.getSpeechHabits() != null && !memorial.getSpeechHabits().trim().isEmpty();
    }

    /**
     * 필수 고인 상세 정보 입력 여부 확인
     */
    private static Boolean hasRequiredDeceasedInfo(Memorial memorial) {
        return hasDeceasedInfo(memorial); // 5개 필드 모두 필수
    }

    /**
     * 입력된 필드 개수 계산
     */
    private static Integer calculateFilledFieldCount(Memorial memorial) {
        int count = 0;
        if (memorial.getPersonality() != null && !memorial.getPersonality().trim().isEmpty()) count++;
        if (memorial.getHobbies() != null && !memorial.getHobbies().trim().isEmpty()) count++;
        if (memorial.getFavoriteFood() != null && !memorial.getFavoriteFood().trim().isEmpty()) count++;
        if (memorial.getSpecialMemories() != null && !memorial.getSpecialMemories().trim().isEmpty()) count++;
        if (memorial.getSpeechHabits() != null && !memorial.getSpeechHabits().trim().isEmpty()) count++;
        return count;
    }

    /**
     * 완성도 퍼센트 계산
     */
    private static Integer calculateCompletionPercent(Memorial memorial) {
        int filledCount = calculateFilledFieldCount(memorial);
        return (filledCount * 100) / 5;
    }

    /**
     * 영상통화 차단 사유 반환
     */
    private static String getVideoCallBlockReason(Memorial memorial) {
        if (!hasRequiredDeceasedInfo(memorial)) {
            return "고인에 대한 상세 정보를 입력해주세요.";
        }

        if (!memorial.canStartVideoCall()) {
            if (!memorial.getAiTrainingCompleted()) {
                return "AI 학습이 완료되지 않았습니다.";
            }
            return "영상통화 준비가 완료되지 않았습니다.";
        }

        return null;
    }

    /**
     * 빈 응답 생성 (메모리얼 정보만 포함)
     */
    public static FamilyInfoResponseDTO empty(Memorial memorial, FamilyMember familyMember) {
        return FamilyInfoResponseDTO.builder()
                .memorialId(memorial.getId())
                .name(memorial.getName())
                .nickname(memorial.getNickname())
                .gender(memorial.getGender())
                .genderDisplayName(memorial.getGender().getDisplayName())
                .birthDate(memorial.getBirthDate())
                .deathDate(memorial.getDeathDate())
                .formattedBirthDate(formatDate(memorial.getBirthDate()))
                .formattedDeathDate(formatDate(memorial.getDeathDate()))
                .formattedAge(memorial.getFormattedAge())
                .relationship(familyMember.getRelationship().name())
                .relationshipDisplayName(familyMember.getRelationshipDisplayName())
                
                .personality(null)
                .hobbies(null)
                .favoriteFood(null)
                .specialMemories(null)
                .speechHabits(null)
                
                .hasDeceasedInfo(false)
                .hasRequiredDeceasedInfo(false)
                .filledFieldCount(0)
                .completionPercent(0)
                .canModify(true)
                .canStartVideoCall(false)
                .videoCallBlockReason("고인에 대한 상세 정보를 입력해주세요.")
                .build();
    }
}