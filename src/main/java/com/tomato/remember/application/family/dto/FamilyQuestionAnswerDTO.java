package com.tomato.remember.application.family.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가족 구성원 질문 답변 DTO (조회용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyQuestionAnswerDTO {
    
    /**
     * 질문 ID
     */
    private Long questionId;
    
    /**
     * 질문 텍스트
     */
    private String questionText;
    
    /**
     * 답변 텍스트
     */
    private String answerText;
    
    /**
     * 답변 길이
     */
    private Integer answerLength;
    
    /**
     * 답변 완료 여부
     */
    private Boolean isComplete;
    
    /**
     * 답변 존재 여부 확인
     */
    public boolean hasAnswer() {
        return answerText != null && !answerText.trim().isEmpty();
    }
    
    /**
     * 트림된 답변 텍스트 반환
     */
    public String getTrimmedAnswer() {
        return answerText != null ? answerText.trim() : "";
    }
}