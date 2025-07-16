package com.tomato.remember.application.memorial.entity;

import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.audit.Audit;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Comment;

/**
 * 고인 상세 정보 답변 엔티티
 */
@Slf4j
@Table(
    name = "t_memorial_answer",
    indexes = {
        @Index(name = "idx01_t_memorial_answer", columnList = "memorial_id, member_id"),
        @Index(name = "idx02_t_memorial_answer", columnList = "memorial_id, family_member_id"),
        @Index(name = "idx03_t_memorial_answer", columnList = "question_id"),
        @Index(name = "idx04_t_memorial_answer", columnList = "memorial_id, is_complete"),
        @Index(name = "idx05_t_memorial_answer", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk01_t_memorial_answer", columnNames = {"memorial_id", "member_id", "question_id"}),
        @UniqueConstraint(name = "uk02_t_memorial_answer", columnNames = {"memorial_id", "family_member_id", "question_id"})
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialAnswer extends Audit {

    // ===== 연관관계 =====

    @Comment("메모리얼")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memorial_id", nullable = false)
    private Memorial memorial;

    @Comment("답변자 (메모리얼 생성자)")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Comment("가족 구성원 (가족 답변)")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_member_id")
    private FamilyMember familyMember;

    @Comment("질문")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private MemorialQuestion question;

    // ===== 답변 내용 =====

    @Comment("답변 내용")
    @Column(columnDefinition = "TEXT", name = "answer_text")
    private String answerText;

    @Comment("답변 글자 수")
    @Column(name = "answer_length")
    @Builder.Default
    private Integer answerLength = 0;

    @Comment("답변 완료 여부")
    @Column(nullable = false, name = "is_complete")
    @Builder.Default
    private Boolean isComplete = false;

    // ===== 비즈니스 메서드 =====

    /**
     * 답변 업데이트
     */
    public void updateAnswer(String answerText) {
        this.answerText = answerText;
        this.answerLength = answerText != null ? answerText.length() : 0;
        this.isComplete = isAnswerComplete();
        
        log.info("답변 업데이트 - 답변ID: {}, 질문ID: {}, 글자수: {}", 
                this.id, this.question.getId(), this.answerLength);
    }

    /**
     * 답변 완료 여부 확인
     */
    public boolean isAnswerComplete() {
        if (answerText == null || answerText.trim().isEmpty()) {
            return false;
        }
        
        return answerLength >= question.getMinLength() && 
               answerLength <= question.getMaxLength();
    }

    /**
     * 답변자 정보 조회
     */
    public String getAnswererName() {
        if (member != null) {
            return member.getName();
        }
        if (familyMember != null) {
            return familyMember.getMemberName();
        }
        return "알 수 없음";
    }

    /**
     * 답변자 타입 확인
     */
    public boolean isOwnerAnswer() {
        return member != null;
    }

    /**
     * 가족 구성원 답변 여부 확인
     */
    public boolean isFamilyAnswer() {
        return familyMember != null;
    }

    /**
     * 답변 유효성 검사
     */
    public boolean isValid() {
        // 답변자는 둘 중 하나만 있어야 함
        boolean hasValidAnswerer = (member != null) ^ (familyMember != null);
        
        // 질문과 메모리얼은 필수
        boolean hasRequiredFields = question != null && memorial != null;
        
        return hasValidAnswerer && hasRequiredFields;
    }

    // ===== 팩토리 메서드 =====

    /**
     * 메모리얼 생성자 답변 생성
     */
    public static MemorialAnswer createOwnerAnswer(Memorial memorial, Member member, 
                                                  MemorialQuestion question, String answerText) {
        MemorialAnswer answer = MemorialAnswer.builder()
                .memorial(memorial)
                .member(member)
                .question(question)
                .build();
        
        answer.updateAnswer(answerText);
        return answer;
    }

    /**
     * 가족 구성원 답변 생성
     */
    public static MemorialAnswer createFamilyAnswer(Memorial memorial, FamilyMember familyMember, 
                                                   MemorialQuestion question, String answerText) {
        MemorialAnswer answer = MemorialAnswer.builder()
                .memorial(memorial)
                .familyMember(familyMember)
                .question(question)
                .build();
        
        answer.updateAnswer(answerText);
        return answer;
    }

    /**
     * 빈 답변 생성 (나중에 업데이트용)
     */
    public static MemorialAnswer createEmptyAnswer(Memorial memorial, Member member, 
                                                  MemorialQuestion question) {
        return MemorialAnswer.builder()
                .memorial(memorial)
                .member(member)
                .question(question)
                .answerText("")
                .answerLength(0)
                .isComplete(false)
                .build();
    }

    /**
     * 가족 구성원 빈 답변 생성
     */
    public static MemorialAnswer createEmptyFamilyAnswer(Memorial memorial, FamilyMember familyMember, 
                                                        MemorialQuestion question) {
        return MemorialAnswer.builder()
                .memorial(memorial)
                .familyMember(familyMember)
                .question(question)
                .answerText("")
                .answerLength(0)
                .isComplete(false)
                .build();
    }
}