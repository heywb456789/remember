package com.tomato.remember.application.memorial.repository;

import com.tomato.remember.application.memorial.entity.MemorialAnswer;
import com.tomato.remember.application.memorial.entity.MemorialQuestion;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.family.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 메모리얼 답변 레포지토리 (필수 메서드만)
 */
@Repository
public interface MemorialAnswerRepository extends JpaRepository<MemorialAnswer, Long> {

    /**
     * 메모리얼의 모든 답변 조회
     */
    List<MemorialAnswer> findByMemorialOrderByCreatedAtDesc(Memorial memorial);

    /**
     * 특정 질문에 대한 특정 회원의 답변 조회
     */
    @Query("SELECT ma FROM MemorialAnswer ma WHERE ma.memorial = :memorial AND ma.member = :member AND ma.question = :question")
    Optional<MemorialAnswer> findByMemorialAndMemberAndQuestion(
            @Param("memorial") Memorial memorial, 
            @Param("member") Member member, 
            @Param("question") MemorialQuestion question);

    /**
     * 메모리얼의 완료된 답변 수 조회
     */
    @Query("SELECT COUNT(ma) FROM MemorialAnswer ma WHERE ma.memorial = :memorial AND ma.isComplete = true")
    long countCompletedAnswersByMemorial(@Param("memorial") Memorial memorial);

    /**
     * 메모리얼의 답변 삭제 (메모리얼 삭제 시 사용)
     */
    void deleteByMemorial(Memorial memorial);
}