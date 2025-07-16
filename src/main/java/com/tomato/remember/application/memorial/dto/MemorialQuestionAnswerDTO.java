package com.tomato.remember.application.memorial.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 질문 답변 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialQuestionAnswerDTO {

    @NotNull(message = "질문 ID는 필수입니다")
    private Long questionId;

    private String answerText;

    /**
     * 답변 존재 여부 확인
     */
    public boolean hasAnswer() {
        return answerText != null && !answerText.trim().isEmpty();
    }

    /**
     * 트림된 답변 반환
     */
    public String getTrimmedAnswer() {
        return answerText != null ? answerText.trim() : "";
    }

    /**
     * 답변 길이 조회
     */
    public int getAnswerLength() {
        return getTrimmedAnswer().length();
    }
}