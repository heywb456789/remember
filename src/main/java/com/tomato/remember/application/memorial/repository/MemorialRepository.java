package com.tomato.remember.application.memorial.repository;

import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 메모리얼 레포지토리
 */
@Repository
public interface MemorialRepository extends JpaRepository<Memorial, Long> {

    /**
     * 소유자별 메모리얼 목록 조회 (최신순)
     */
    Page<Memorial> findByOwnerOrderByCreatedAtDesc(Member owner, Pageable pageable);

    /**
     * 소유자별 메모리얼 목록 조회 (활성 상태만)
     */
    @Query("SELECT m FROM Memorial m WHERE m.owner = :owner AND m.status = 'ACTIVE' ORDER BY m.createdAt DESC")
    Page<Memorial> findActiveByOwner(@Param("owner") Member owner, Pageable pageable);

    /**
     * 메모리얼 상세 조회 (파일 정보 포함)
     */
    @Query("SELECT m FROM Memorial m LEFT JOIN FETCH m.memorialFiles WHERE m.id = :memorialId")
    Optional<Memorial> findByIdWithFiles(@Param("memorialId") Long memorialId);

    /**
     * 소유자별 메모리얼 개수 조회
     */
    long countByOwner(Member owner);

    /**
     * 소유자별 활성 메모리얼 개수 조회
     */
    @Query("SELECT COUNT(m) FROM Memorial m WHERE m.owner = :owner AND m.status = 'ACTIVE'")
    long countActiveByOwner(@Param("owner") Member owner);

    /**
     * 메모리얼 이름으로 검색 (소유자 기준)
     */
    @Query("SELECT m FROM Memorial m WHERE m.owner = :owner AND m.name LIKE %:name% ORDER BY m.createdAt DESC")
    Page<Memorial> findByOwnerAndNameContaining(@Param("owner") Member owner, @Param("name") String name, Pageable pageable);

    /**
     * 공개 메모리얼 목록 조회
     */
    @Query("SELECT m FROM Memorial m WHERE m.isPublic = true AND m.status = 'ACTIVE' ORDER BY m.createdAt DESC")
    Page<Memorial> findPublicMemorials(Pageable pageable);

    /**
     * AI 학습 완료된 메모리얼 목록 조회
     */
    @Query("SELECT m FROM Memorial m WHERE m.owner = :owner AND m.aiTrainingCompleted = true ORDER BY m.createdAt DESC")
    List<Memorial> findAiTrainingCompletedByOwner(@Param("owner") Member owner);
}