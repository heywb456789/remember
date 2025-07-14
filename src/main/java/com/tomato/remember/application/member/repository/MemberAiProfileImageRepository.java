package com.tomato.remember.application.member.repository;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.member.entity.MemberAiProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 회원 AI 프로필 이미지 레포지토리 (간소화 버전)
 */
@Repository
public interface MemberAiProfileImageRepository extends JpaRepository<MemberAiProfileImage, Long> {

    // ===== 기본 조회 메서드 =====

    /**
     * 회원의 모든 프로필 이미지 조회 (정렬 순서대로)
     */
    List<MemberAiProfileImage> findByMemberOrderBySortOrderAsc(Member member);

    /**
     * 회원의 특정 순서 프로필 이미지 조회
     */
    Optional<MemberAiProfileImage> findByMemberAndSortOrder(Member member, Integer sortOrder);

    /**
     * 회원의 프로필 이미지 개수 조회
     */
    long countByMember(Member member);

    /**
     * 회원의 AI 처리 완료된 이미지 조회
     */
    @Query("SELECT img FROM MemberAiProfileImage img WHERE img.member = :member AND img.aiProcessed = true ORDER BY img.sortOrder")
    List<MemberAiProfileImage> findValidImagesByMember(@Param("member") Member member);

    /**
     * 회원의 AI 처리 대기 중인 이미지 조회
     */
    @Query("SELECT img FROM MemberAiProfileImage img WHERE img.member = :member AND img.aiProcessed = false ORDER BY img.sortOrder")
    List<MemberAiProfileImage> findPendingImagesByMember(@Param("member") Member member);

    // ===== 통계 메서드 =====

    /**
     * 회원의 AI 처리 완료된 이미지 개수
     */
    @Query("SELECT COUNT(img) FROM MemberAiProfileImage img WHERE img.member = :member AND img.aiProcessed = true")
    long countValidImagesByMember(@Param("member") Member member);

    /**
     * 회원의 AI 처리 대기 중인 이미지 개수
     */
    @Query("SELECT COUNT(img) FROM MemberAiProfileImage img WHERE img.member = :member AND img.aiProcessed = false")
    long countPendingImagesByMember(@Param("member") Member member);

    // ===== 관리 메서드 =====

    /**
     * 회원의 모든 프로필 이미지 삭제
     */
    void deleteByMember(Member member);

    /**
     * 회원의 특정 순서 이미지 삭제
     */
    void deleteByMemberAndSortOrder(Member member, Integer sortOrder);

    /**
     * AI 처리되지 않은 이미지 조회 (배치 처리용)
     */
    @Query("SELECT img FROM MemberAiProfileImage img WHERE img.aiProcessed = false ORDER BY img.createdAt")
    List<MemberAiProfileImage> findUnprocessedImages();

    // ===== 검증 메서드 =====

    /**
     * 특정 순서에 이미지가 존재하는지 확인
     */
    boolean existsByMemberAndSortOrder(Member member, Integer sortOrder);

    /**
     * 회원이 필수 이미지 개수를 업로드했는지 확인
     */
    @Query("SELECT CASE WHEN COUNT(img) >= 5 THEN true ELSE false END FROM MemberAiProfileImage img WHERE img.member = :member")
    boolean hasUploadedRequiredImages(@Param("member") Member member);

    /**
     * 회원이 영상통화 가능한 상태인지 확인 (5장 + AI 처리 완료)
     */
    @Query("SELECT CASE WHEN COUNT(img) >= 5 THEN true ELSE false END FROM MemberAiProfileImage img WHERE img.member = :member AND img.aiProcessed = true")
    boolean isVideoCallReady(@Param("member") Member member);

    /**
     * 다음 사용 가능한 정렬 순서 조회
     */
    @Query("SELECT COALESCE(MAX(img.sortOrder), 0) + 1 FROM MemberAiProfileImage img WHERE img.member = :member")
    Integer getNextSortOrder(@Param("member") Member member);
}