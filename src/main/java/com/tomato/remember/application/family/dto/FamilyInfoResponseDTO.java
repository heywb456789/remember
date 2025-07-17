package com.tomato.remember.application.family.dto;

import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.member.code.Gender;
import com.tomato.remember.application.memorial.dto.MemorialQuestionAnswerDTO;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.entity.MemorialAnswer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 가족 구성원용 고인 상세 정보 응답 DTO (리팩토링된 버전)
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
     * 포맷팅된 생년월일
     */
    private String formattedBirthDate;

    /**
     * 포맷팅된 기일
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

    // ===== 동적 질문 답변들 =====

    /**
     * 동적 질문 답변 리스트
     */
    @Builder.Default
    private List<FamilyQuestionAnswerDTO> questionAnswers = new ArrayList<>();

    // ===== 상태 정보 =====

    /**
     * 완성도 퍼센트 (0-100)
     */
    private Integer completionPercent;

    /**
     * 영상통화 가능 여부
     */
    private Boolean canStartVideoCall;

    /**
     * 영상통화 차단 사유
     */
    private String videoCallBlockReason;

    // ===== 편의 메서드 =====

    /**
     * 입력된 답변이 있는지 확인
     */
    public boolean hasAnswers() {
        return questionAnswers != null && !questionAnswers.isEmpty();
    }

    /**
     * 특정 질문의 답변 조회
     */
    public String getAnswerForQuestion(Long questionId) {
        if (questionAnswers == null) {
            return null;
        }

        return questionAnswers.stream()
                .filter(answer -> answer.getQuestionId().equals(questionId))
                .map(FamilyQuestionAnswerDTO::getAnswerText)
                .findFirst()
                .orElse(null);
    }

    /**
     * 답변된 질문 수
     */
    public int getAnsweredQuestionCount() {
        return questionAnswers != null ? questionAnswers.size() : 0;
    }

    // ===== 정적 컨버터 메서드들 =====

    /**
     * 입력 모드용 DTO 생성 (답변 없음)
     */
    public static FamilyInfoResponseDTO forInput(Memorial memorial, FamilyMember familyMember) {
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
                .questionAnswers(new ArrayList<>())
                .completionPercent(0)
                .canStartVideoCall(false)
                .videoCallBlockReason("고인에 대한 상세 정보를 입력해주세요.")
                .build();
    }

    /**
     * 조회 모드용 DTO 생성 (답변 포함)
     */
    public static FamilyInfoResponseDTO forView(Memorial memorial,
                                               FamilyMember familyMember,
                                               List<MemorialAnswer> answers,
                                               Integer completionPercent) {

        List<FamilyQuestionAnswerDTO> questionAnswers = answers.stream()
                .filter(answer -> answer.getAnswerText() != null && !answer.getAnswerText().trim().isEmpty())
                .map(answer -> FamilyQuestionAnswerDTO.builder()
                        .questionId(answer.getQuestion().getId())
                        .questionText(answer.getQuestion().getQuestionText())
                        .answerText(answer.getAnswerText())
                        .answerLength(answer.getAnswerLength())
                        .isComplete(answer.getIsComplete())
                        .build())
                .collect(Collectors.toList());

        boolean canStartVideoCall = memorial.canStartVideoCall() &&
                                   familyMember.getVideoCallAccess() &&
                                   completionPercent >= 80; // 80% 이상 완성 시 영상통화 가능

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
                .questionAnswers(questionAnswers)
                .completionPercent(completionPercent)
                .canStartVideoCall(canStartVideoCall)
                .videoCallBlockReason(getVideoCallBlockReason(memorial, familyMember, canStartVideoCall))
                .build();
    }

    // ===== 유틸리티 메서드들 =====

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
     * 영상통화 차단 사유 반환
     */
    private static String getVideoCallBlockReason(Memorial memorial, FamilyMember familyMember, boolean canStartVideoCall) {
        if (canStartVideoCall) {
            return null;
        }

        if (!memorial.canStartVideoCall()) {
            if (!memorial.getAiTrainingCompleted()) {
                return "AI 학습이 완료되지 않았습니다.";
            }
            if (!memorial.hasRequiredFiles()) {
                return "필요한 파일이 모두 업로드되지 않았습니다.";
            }
            return "메모리얼 소유자가 영상통화를 활성화하지 않았습니다.";
        }

        if (!familyMember.getVideoCallAccess()) {
            return "영상통화 권한이 없습니다.";
        }

        return "고인에 대한 상세 정보를 더 입력해주세요. (완성도 80% 이상 필요)";
    }
}