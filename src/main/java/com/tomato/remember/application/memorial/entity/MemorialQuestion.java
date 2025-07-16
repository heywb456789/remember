package com.tomato.remember.application.memorial.entity;

import com.tomato.remember.common.audit.Audit;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

/**
 * 고인 상세 정보 질문 엔티티
 */
@Slf4j
@Table(
    name = "t_memorial_question",
    indexes = {
        @Index(name = "idx01_t_memorial_question", columnList = "sort_order, is_active"),
        @Index(name = "idx02_t_memorial_question", columnList = "category"),
        @Index(name = "idx03_t_memorial_question", columnList = "is_active, created_at")
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialQuestion extends Audit {

    @Comment("질문 내용")
    @Column(nullable = false, length = 500, name = "question_text")
    private String questionText;

    @Comment("플레이스홀더 텍스트")
    @Column(length = 300, name = "placeholder_text")
    private String placeholderText;

    @Comment("최대 글자 수")
    @Column(nullable = false, name = "max_length")
    @Builder.Default
    private Integer maxLength = 500;

    @Comment("최소 글자 수")
    @Column(nullable = false, name = "min_length")
    @Builder.Default
    private Integer minLength = 5;

    @Comment("필수 입력 여부")
    @Column(nullable = false, name = "is_required")
    @Builder.Default
    private Boolean isRequired = true;

    @Comment("정렬 순서")
    @Column(nullable = false, name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Comment("활성 여부")
    @Column(nullable = false, name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Comment("질문 설명 (관리자용)")
    @Column(length = 200)
    private String description;

    @Comment("질문 카테고리")
    @Column(length = 50)
    private String category;

    // ===== 연관관계 =====

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemorialAnswer> answers = new ArrayList<>();

    // ===== 비즈니스 메서드 =====

    /**
     * 질문 활성화
     */
    public void activate() {
        this.isActive = true;
        log.info("질문 활성화 - ID: {}, 내용: {}", this.id, this.questionText);
    }

    /**
     * 질문 비활성화
     */
    public void deactivate() {
        this.isActive = false;
        log.info("질문 비활성화 - ID: {}, 내용: {}", this.id, this.questionText);
    }

    /**
     * 질문 내용 업데이트
     */
    public void updateQuestion(String questionText, String placeholderText, 
                              Integer maxLength, Integer minLength, 
                              Boolean isRequired, String category, String description) {
        this.questionText = questionText;
        this.placeholderText = placeholderText;
        this.maxLength = maxLength;
        this.minLength = minLength;
        this.isRequired = isRequired;
        this.category = category;
        this.description = description;
        
        log.info("질문 내용 업데이트 - ID: {}", this.id);
    }

    /**
     * 정렬 순서 변경
     */
    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
        log.info("질문 정렬 순서 변경 - ID: {}, 순서: {}", this.id, sortOrder);
    }

    /**
     * 답변 개수 조회
     */
    public int getAnswerCount() {
        return answers.size();
    }

    /**
     * 완료된 답변 개수 조회
     */
    public long getCompletedAnswerCount() {
        return answers.stream()
                .filter(MemorialAnswer::getIsComplete)
                .count();
    }

    /**
     * 유효성 검사
     */
    public boolean isValid() {
        return this.questionText != null && 
               !this.questionText.trim().isEmpty() &&
               this.maxLength > 0 &&
               this.minLength >= 0 &&
               this.minLength <= this.maxLength;
    }

    // ===== 팩토리 메서드 =====

    /**
     * 새 질문 생성
     */
    public static MemorialQuestion createQuestion(String questionText, String placeholderText,
                                                 Integer maxLength, Integer minLength,
                                                 Boolean isRequired, Integer sortOrder,
                                                 String category, String description) {
        return MemorialQuestion.builder()
                .questionText(questionText)
                .placeholderText(placeholderText)
                .maxLength(maxLength)
                .minLength(minLength)
                .isRequired(isRequired)
                .sortOrder(sortOrder)
                .category(category)
                .description(description)
                .isActive(true)
                .build();
    }
}