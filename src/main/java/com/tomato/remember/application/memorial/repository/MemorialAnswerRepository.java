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

    /**
     * 특정 메모리얼과 가족 구성원의 모든 답변 조회
     */
    @Query("SELECT ma FROM MemorialAnswer ma " +
        "WHERE ma.memorial = :memorial AND ma.familyMember = :familyMember " +
        "ORDER BY ma.question.sortOrder ASC")
    List<MemorialAnswer> findByMemorialAndFamilyMember(@Param("memorial") Memorial memorial,
        @Param("familyMember") FamilyMember familyMember);

    /**
     * 특정 메모리얼과 가족 구성원의 완료된 답변 수 조회
     */
    @Query("SELECT COUNT(ma) FROM MemorialAnswer ma " +
        "WHERE ma.memorial = :memorial AND ma.familyMember = :familyMember " +
        "AND ma.isComplete = true")
    long countCompletedAnswersByMemorialAndFamilyMember(@Param("memorial") Memorial memorial,
        @Param("familyMember") FamilyMember familyMember);

    /**
     * 특정 메모리얼과 가족 구성원의 특정 질문 답변 조회
     */
    @Query("SELECT ma FROM MemorialAnswer ma " +
        "WHERE ma.memorial = :memorial AND ma.familyMember = :familyMember " +
        "AND ma.question = :question")
    Optional<MemorialAnswer> findByMemorialAndFamilyMemberAndQuestion(@Param("memorial") Memorial memorial,
        @Param("familyMember") FamilyMember familyMember,
        @Param("question") MemorialQuestion question);

    /**
     * 특정 메모리얼의 모든 가족 구성원 답변 조회 (관리자용)
     */
    @Query("SELECT ma FROM MemorialAnswer ma " +
        "WHERE ma.memorial = :memorial AND ma.familyMember IS NOT NULL " +
        "ORDER BY ma.familyMember.id ASC, ma.question.sortOrder ASC")
    List<MemorialAnswer> findAllFamilyAnswersByMemorial(@Param("memorial") Memorial memorial);

    /**
     * 특정 가족 구성원의 모든 답변 수 조회
     */
    @Query("SELECT COUNT(ma) FROM MemorialAnswer ma " +
        "WHERE ma.familyMember = :familyMember")
    long countAnswersByFamilyMember(@Param("familyMember") FamilyMember familyMember);

    /**
     * 특정 가족 구성원의 완료된 답변 수 조회
     */
    @Query("SELECT COUNT(ma) FROM MemorialAnswer ma " +
        "WHERE ma.familyMember = :familyMember AND ma.isComplete = true")
    long countCompletedAnswersByFamilyMember(@Param("familyMember") FamilyMember familyMember);

    /**
     * 특정 메모리얼에서 답변한 가족 구성원 수 조회
     */
    @Query("SELECT COUNT(DISTINCT ma.familyMember) FROM MemorialAnswer ma " +
        "WHERE ma.memorial = :memorial AND ma.familyMember IS NOT NULL " +
        "AND ma.isComplete = true")
    long countContributingFamilyMembers(@Param("memorial") Memorial memorial);

    /**
     * 특정 메모리얼과 가족 구성원의 답변 존재 여부 확인
     */
    @Query("SELECT COUNT(ma) > 0 FROM MemorialAnswer ma " +
        "WHERE ma.memorial = :memorial AND ma.familyMember = :familyMember " +
        "AND ma.isComplete = true")
    boolean existsCompletedAnswersByMemorialAndFamilyMember(@Param("memorial") Memorial memorial,
        @Param("familyMember") FamilyMember familyMember);
}