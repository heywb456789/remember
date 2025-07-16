package com.tomato.remember.application.memorial.dto;

import com.tomato.remember.application.member.code.Gender;
import com.tomato.remember.application.member.code.Relationship;
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
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 메모리얼 생성 요청 DTO (List 형태 질문 답변 지원)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialCreateRequestDTO {

    // ===== 필수 기본 정보 =====

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자 이내로 입력해주세요")
    private String name;

    @NotBlank(message = "호칭은 필수입니다")
    @Size(max = 30, message = "호칭은 30자 이내로 입력해주세요")
    private String nickname;

    @NotNull(message = "성별을 선택해주세요")
    private Gender gender;

    @NotNull(message = "나와의 관계를 선택해주세요")
    private Relationship relationship;

    // ===== 선택 기본 정보 =====

    private LocalDate birthDate;
    private LocalDate deathDate;

    // ===== 동적 질문 답변들 =====

    /**
     * 질문 답변 리스트
     */
    private List<MemorialQuestionAnswerDTO> questionAnswers;

    // ===== 기타 설정 =====

    private String description;

    @Builder.Default
    private Boolean isPublic = false;

    // ===== 헬퍼 메서드 =====

    /**
     * 질문 답변 리스트를 Map으로 변환 (트림 처리)
     */
    public Map<Long, String> getQuestionAnswersAsMap() {
        Map<Long, String> resultMap = new HashMap<>();

        if (questionAnswers != null) {
            for (MemorialQuestionAnswerDTO dto : questionAnswers) {
                if (dto.hasAnswer()) {
                    resultMap.put(dto.getQuestionId(), dto.getTrimmedAnswer());
                }
            }
        }

        return resultMap;
    }

    /**
     * 특정 질문의 답변 조회
     */
    public String getAnswerForQuestion(Long questionId) {
        if (questionAnswers == null) {
            return null;
        }

        return questionAnswers.stream()
                .filter(dto -> dto.getQuestionId().equals(questionId))
                .filter(MemorialQuestionAnswerDTO::hasAnswer)
                .map(MemorialQuestionAnswerDTO::getTrimmedAnswer)
                .findFirst()
                .orElse(null);
    }

    /**
     * 답변이 있는 질문 개수 조회
     */
    public int getAnsweredQuestionCount() {
        if (questionAnswers == null) {
            return 0;
        }

        return (int) questionAnswers.stream()
                .filter(MemorialQuestionAnswerDTO::hasAnswer)
                .count();
    }

    /**
     * 답변 유효성 검사
     */
    public boolean hasValidAnswers() {
        return questionAnswers != null && !questionAnswers.isEmpty() && getAnsweredQuestionCount() > 0;
    }

    /**
     * 빈 답변 제거
     */
    public void removeEmptyAnswers() {
        if (questionAnswers != null) {
            questionAnswers.removeIf(dto -> !dto.hasAnswer());
        }
    }

    /**
     * 질문 답변 초기화
     */
    public void initializeQuestionAnswers() {
        if (questionAnswers == null) {
            questionAnswers = new ArrayList<>();
        }
    }
}