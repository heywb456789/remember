package com.tomato.remember.application.memorial.repository;

import com.tomato.remember.application.memorial.code.MemorialStatus;
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

    List<Memorial> findByOwnerOrderByCreatedAtDesc(Member owner);

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
     * 특정 소유자의 활성 메모리얼 조회
     */
    List<Memorial> findByOwnerAndStatusOrderByCreatedAtDesc(Member owner, MemorialStatus status);

    /**
     * 특정 소유자의 활성 메모리얼 조회 (페이징)
     */
    Page<Memorial> findByOwnerAndStatusOrderByCreatedAtDesc(Member owner, MemorialStatus status, Pageable pageable);


    /**
     * 소유자별 활성 메모리얼 개수 조회
     */
    @Query("SELECT COUNT(m) FROM Memorial m WHERE m.owner = :owner AND m.status = 'ACTIVE'")
    long countActiveByOwner(@Param("owner") Member owner);

    /**
     * 메모리얼 이름으로 검색 (소유자 기준)
     */
    @Query("SELECT m FROM Memorial m WHERE m.owner = :owner AND m.name LIKE %:name% ORDER BY m.createdAt DESC")
    Page<Memorial> findByOwnerAndNameContaining(@Param("owner") Member owner, @Param("name") String name,
        Pageable pageable);

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

    /**
     * 메모리얼 ID와 소유자로 검증
     */
    Optional<Memorial> findByIdAndOwner(Long id, Member owner);

    /**
     * 사용자가 접근 가능한 모든 메모리얼 조회 (소유한 메모리얼 + 가족 구성원으로 참여한 메모리얼) - 소유한 메모리얼: 모든 상태 - 가족 구성원으로 참여한 메모리얼: ACCEPTED 상태 +
     * memorialAccess = true
     */
    @Query("SELECT DISTINCT m FROM Memorial m " +
        "LEFT JOIN m.familyMembers fm " +
        "WHERE (m.owner = :member) " +
        "   OR (fm.member = :member " +
        "       AND fm.inviteStatus = 'ACCEPTED' " +
        "       AND fm.memorialAccess = true " +
        "       AND m.status = 'ACTIVE') " +
        "ORDER BY m.createdAt DESC")
    Page<Memorial> findAccessibleMemorialsByMember(@Param("member") Member member, Pageable pageable);

    /**
     * 사용자가 접근 가능한 모든 메모리얼 조회 (비페이징)
     */
    @Query("SELECT DISTINCT m FROM Memorial m " +
        "LEFT JOIN m.familyMembers fm " +
        "WHERE (m.owner = :member) " +
        "   OR (fm.member = :member " +
        "       AND fm.inviteStatus = 'ACCEPTED' " +
        "       AND fm.memorialAccess = true " +
        "       AND m.status = 'ACTIVE') " +
        "ORDER BY m.createdAt DESC")
    List<Memorial> findAccessibleMemorialsByMember(@Param("member") Member member);

    /**
     * 사용자가 접근 가능한 활성 메모리얼만 조회 (페이징)
     */
    @Query("SELECT DISTINCT m FROM Memorial m " +
        "LEFT JOIN m.familyMembers fm " +
        "WHERE m.status = 'ACTIVE' " +
        "   AND ((m.owner = :member) " +
        "        OR (fm.member = :member " +
        "            AND fm.inviteStatus = 'ACCEPTED' " +
        "            AND fm.memorialAccess = true)) " +
        "ORDER BY m.createdAt DESC")
    Page<Memorial> findActiveAccessibleMemorialsByMember(@Param("member") Member member, Pageable pageable);

    /**
     * 사용자가 접근 가능한 메모리얼 개수 조회
     */
    @Query("SELECT COUNT(DISTINCT m) FROM Memorial m " +
        "LEFT JOIN m.familyMembers fm " +
        "WHERE (m.owner = :member) " +
        "   OR (fm.member = :member " +
        "       AND fm.inviteStatus = 'ACCEPTED' " +
        "       AND fm.memorialAccess = true " +
        "       AND m.status = 'ACTIVE')")
    long countAccessibleMemorialsByMember(@Param("member") Member member);

    /**
     * 사용자가 소유한 메모리얼과 가족 구성원으로 참여한 메모리얼을 구분해서 조회
     */
    @Query("SELECT DISTINCT m, " +
        "CASE WHEN m.owner = :member THEN 'OWNER' ELSE 'FAMILY_MEMBER' END as accessType " +
        "FROM Memorial m " +
        "LEFT JOIN m.familyMembers fm " +
        "WHERE (m.owner = :member) " +
        "   OR (fm.member = :member " +
        "       AND fm.inviteStatus = 'ACCEPTED' " +
        "       AND fm.memorialAccess = true " +
        "       AND m.status = 'ACTIVE') " +
        "ORDER BY m.createdAt DESC")
    List<Object[]> findAccessibleMemorialsWithAccessType(@Param("member") Member member);

    /**
     * 특정 메모리얼에 대한 사용자의 접근 권한 상세 정보 조회
     */
    @Query("SELECT m, fm FROM Memorial m " +
        "LEFT JOIN m.familyMembers fm ON fm.member = :member " +
        "WHERE m.id = :memorialId " +
        "   AND ((m.owner = :member) " +
        "        OR (fm.inviteStatus = 'ACCEPTED' " +
        "            AND fm.memorialAccess = true " +
        "            AND m.status = 'ACTIVE'))")
    Optional<Object[]> findMemorialWithAccessInfo(@Param("memorialId") Long memorialId, @Param("member") Member member);

    /**
     * 사용자가 영상통화 가능한 메모리얼 조회
     */
    @Query("SELECT DISTINCT m FROM Memorial m " +
        "LEFT JOIN m.familyMembers fm " +
        "WHERE m.status = 'ACTIVE' " +
        "   AND m.aiTrainingCompleted = true " +
        "   AND ((m.owner = :member) " +
        "        OR (fm.member = :member " +
        "            AND fm.inviteStatus = 'ACCEPTED' " +
        "            AND fm.memorialAccess = true " +
        "            AND fm.videoCallAccess = true)) " +
        "ORDER BY m.createdAt DESC")
    List<Memorial> findVideoCallAccessibleMemorials(@Param("member") Member member);

}