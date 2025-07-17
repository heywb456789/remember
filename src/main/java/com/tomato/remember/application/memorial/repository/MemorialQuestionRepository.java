package com.tomato.remember.application.memorial.repository;

import com.tomato.remember.application.memorial.entity.MemorialQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 질문 레포지토리
 */
@Repository
public interface MemorialQuestionRepository extends JpaRepository<MemorialQuestion, Long> {
    
    /**
     * 활성 질문 목록 조회 (정렬 순서대로)
     */
    @Query("SELECT mq FROM MemorialQuestion mq WHERE mq.isActive = true ORDER BY mq.sortOrder ASC")
    List<MemorialQuestion> findActiveQuestions();
    
    /**
     * 카테고리별 활성 질문 조회
     */
    @Query("SELECT mq FROM MemorialQuestion mq WHERE mq.isActive = true AND mq.category = :category ORDER BY mq.sortOrder ASC")
    List<MemorialQuestion> findActiveQuestionsByCategory(String category);
    
    /**
     * 정렬 순서로 질문 조회
     */
    List<MemorialQuestion> findByIsActiveTrueOrderBySortOrderAsc();
    
    /**
     * 필수 질문만 조회
     */
    @Query("SELECT mq FROM MemorialQuestion mq WHERE mq.isActive = true AND mq.isRequired = true ORDER BY mq.sortOrder ASC")
    List<MemorialQuestion> findRequiredQuestions();

    int countByIsRequiredTrue();
}
