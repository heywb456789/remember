package com.tomato.remember.application.memorial.dto;

import com.tomato.remember.application.memorial.entity.MemorialQuestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 질문 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialQuestionResponse {
    
    private Long id;
    private String questionText;
    private String placeholderText;
    private Integer maxLength;
    private Integer minLength;
    private Boolean isRequired;
    private Integer sortOrder;
    private String category;
    private String description;
    
    /**
     * Entity -> DTO 변환
     */
    public static MemorialQuestionResponse from(MemorialQuestion question) {
        return MemorialQuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .placeholderText(question.getPlaceholderText())
                .maxLength(question.getMaxLength())
                .minLength(question.getMinLength())
                .isRequired(question.getIsRequired())
                .sortOrder(question.getSortOrder())
                .category(question.getCategory())
                .description(question.getDescription())
                .build();
    }
    
    /**
     * Entity List -> DTO List 변환
     */
    public static List<MemorialQuestionResponse> fromList(List<MemorialQuestion> questions) {
        return questions.stream()
                .map(MemorialQuestionResponse::from)
                .collect(Collectors.toList());
    }
}