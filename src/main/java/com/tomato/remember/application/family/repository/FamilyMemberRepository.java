package com.tomato.remember.application.family.repository;

import com.tomato.remember.application.family.code.InviteStatus;
import com.tomato.remember.application.family.dto.FamilySearchCondition;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.family.repository.custom.FamilyMemberRepositoryCustom;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.code.MemorialStatus;
import com.tomato.remember.application.memorial.entity.Memorial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 가족 구성원 레포지토리
 */
@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long>, FamilyMemberRepositoryCustom {

    // ===== 기본 조회 메서드 =====

    /**
     * 메모리얼의 모든 가족 구성원 조회
     */
    List<FamilyMember> findByMemorialOrderByCreatedAtDesc(Memorial memorial);

    /**
     * 메모리얼의 가족 구성원 페이징 조회
     */
    Page<FamilyMember> findByMemorialOrderByCreatedAtDesc(Memorial memorial, Pageable pageable);

    /**
     * 메모리얼의 특정 상태 가족 구성원 조회
     */
    List<FamilyMember> findByMemorialAndInviteStatusOrderByCreatedAtDesc(Memorial memorial, InviteStatus inviteStatus);

    /**
     * 특정 메모리얼의 활성 가족 구성원 조회 (페이징)
     */
    Page<FamilyMember> findByMemorialAndInviteStatusOrderByCreatedAtDesc(Memorial memorial, InviteStatus inviteStatus,
        Pageable pageable);

    /**
     * 메모리얼의 활성 가족 구성원 조회
     */
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.inviteStatus = 'ACCEPTED' ORDER BY fm.createdAt DESC")
    List<FamilyMember> findActiveMembers(@Param("memorial") Memorial memorial);

    /**
     * 회원이 속한 모든 가족 그룹 조회
     */
    List<FamilyMember> findByMemberOrderByCreatedAtDesc(Member member);

    /**
     * 특정 멤버의 가족 구성원 관계 조회 (페이징)
     */
    Page<FamilyMember> findByMemberOrderByCreatedAtDesc(Member member, Pageable pageable);

    /**
     * 회원이 접근 가능한 메모리얼 조회
     */
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.member = :member AND fm.inviteStatus = 'ACCEPTED' AND fm.memorialAccess = true ORDER BY fm.lastAccessAt DESC NULLS LAST, fm.createdAt DESC")
    List<FamilyMember> findAccessibleMemorials(@Param("member") Member member);

    /**
     * 회원이 영상통화 가능한 메모리얼 조회
     */
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.member = :member AND fm.inviteStatus = 'ACCEPTED' AND fm.memorialAccess = true AND fm.videoCallAccess = true ORDER BY fm.lastAccessAt DESC NULLS LAST, fm.createdAt DESC")
    List<FamilyMember> findVideoCallAccessibleMemorials(@Param("member") Member member);

    // ===== 특정 관계 조회 =====

    /**
     * 특정 메모리얼과 회원의 관계 조회
     */
    @Query("SELECT fm FROM FamilyMember fm " +
        "WHERE fm.memorial = :memorial " +
        "AND fm.member = :member")
    Optional<FamilyMember> findByMemorialAndMember(
        @Param("memorial") Memorial memorial,
        @Param("member") Member member);

    /**
     * 특정 메모리얼과 회원으로 가족 구성원 조회 (활성 상태만)
     */
    @Query("SELECT fm FROM FamilyMember fm " +
        "WHERE fm.memorial = :memorial " +
        "AND fm.member = :member " +
        "AND fm.inviteStatus = 'ACCEPTED'")
    Optional<FamilyMember> findActiveByMemorialAndMember(
        @Param("memorial") Memorial memorial,
        @Param("member") Member member);

    /**
     * 특정 메모리얼과 회원 ID로 가족 구성원 조회
     */
    @Query("SELECT fm FROM FamilyMember fm " +
        "WHERE fm.memorial.id = :memorialId " +
        "AND fm.member.id = :memberId")
    Optional<FamilyMember> findByMemorialIdAndMemberId(
        @Param("memorialId") Long memorialId,
        @Param("memberId") Long memberId);

    /**
     * 특정 메모리얼과 회원의 활성 관계 조회
     */
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.member = :member AND fm.inviteStatus = 'ACCEPTED'")
    Optional<FamilyMember> findActiveRelation(@Param("memorial") Memorial memorial, @Param("member") Member member);

    // ===== 권한 체크 메서드 =====

    /**
     * 메모리얼 접근 권한 확인
     */
    @Query("SELECT CASE WHEN COUNT(fm) > 0 THEN true ELSE false END FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.member = :member AND fm.inviteStatus = 'ACCEPTED' AND fm.memorialAccess = true")
    boolean hasMemorialAccess(@Param("memorial") Memorial memorial, @Param("member") Member member);

    /**
     * 영상통화 접근 권한 확인
     */
    @Query("SELECT CASE WHEN COUNT(fm) > 0 THEN true ELSE false END FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.member = :member AND fm.inviteStatus = 'ACCEPTED' AND fm.memorialAccess = true AND fm.videoCallAccess = true")
    boolean hasVideoCallAccess(@Param("memorial") Memorial memorial, @Param("member") Member member);

    /**
     * 관계 존재 여부 확인
     */
    boolean existsByMemorialAndMember(Memorial memorial, Member member);

    /**
     * 활성 관계 존재 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(fm) > 0 THEN true ELSE false END FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.member = :member AND fm.inviteStatus = 'ACCEPTED'")
    boolean existsActiveRelation(@Param("memorial") Memorial memorial, @Param("member") Member member);

    // ===== 초대 관련 메서드 =====

    /**
     * 회원이 받은 초대 목록 조회
     */
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.member = :member AND fm.inviteStatus = 'PENDING' ORDER BY fm.createdAt DESC")
    List<FamilyMember> findPendingInvitations(@Param("member") Member member);

    /**
     * 회원이 보낸 초대 목록 조회
     */
    List<FamilyMember> findByInvitedByOrderByCreatedAtDesc(Member invitedBy);

    /**
     * 특정 기간 이전의 대기 중인 초대 조회 (만료 처리용)
     */
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.inviteStatus = 'PENDING' AND fm.createdAt < :expireTime")
    List<FamilyMember> findExpiredInvitations(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 특정 소유자의 메모리얼 수 조회
     */
//    long countByOwner(Member owner);

    /**
     * 특정 소유자의 활성 메모리얼 조회
     */
//    List<Memorial> findByOwnerAndStatusOrderByCreatedAtDesc(Member owner, MemorialStatus status);

    // ===== 통계 메서드 =====

    /**
     * 특정 메모리얼의 가족 구성원 수 조회
     */
    long countByMemorial(Memorial memorial);

    /**
     * 특정 메모리얼의 활성 가족 구성원 수 조회
     */
    long countByMemorialAndInviteStatus(Memorial memorial, InviteStatus inviteStatus);

    /**
     * 연락처로 가족 구성원 조회 (중복 초대 확인용)
     */
    @Query("SELECT fm FROM FamilyMember fm JOIN fm.member m WHERE m.email = :email OR m.phoneNumber = :phoneNumber")
    List<FamilyMember> findByContact(@Param("email") String email, @Param("phoneNumber") String phoneNumber);

    /**
     * 가족 구성원 검색 (복합 조건)
     */
    Page<FamilyMember> searchFamilyMembers(Member owner, FamilySearchCondition condition, Pageable pageable);

    /**
     * 메모리얼의 가족 구성원 수 조회
     */
    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.inviteStatus = 'ACCEPTED'")
    long countActiveMembers(@Param("memorial") Memorial memorial);

    /**
     * 회원이 속한 가족 그룹 수 조회
     */
    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.member = :member AND fm.inviteStatus = 'ACCEPTED'")
    long countMemberFamilies(@Param("member") Member member);

    /**
     * 대기 중인 초대 수 조회
     */
    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.member = :member AND fm.inviteStatus = 'PENDING'")
    long countPendingInvitations(@Param("member") Member member);

    // ===== 페이징 조회 =====

    /**
     * 회원의 가족 그룹 페이징 조회
     */
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.member = :member AND fm.inviteStatus = 'ACCEPTED' ORDER BY fm.lastAccessAt DESC NULLS LAST, fm.createdAt DESC")
    Page<FamilyMember> findMemberFamilies(@Param("member") Member member, Pageable pageable);

    // ===== 복합 조회 =====

    /**
     * 사용자가 접근 가능한 모든 가족 구성원 조회 (한 번의 쿼리로)
     */
    @Query("SELECT DISTINCT fm FROM FamilyMember fm " +
        "JOIN FETCH fm.member m " +
        "JOIN FETCH fm.memorial mem " +
        "JOIN FETCH mem.owner owner " +
        "WHERE (mem.owner = :currentUser " +
        "   OR EXISTS (SELECT fm2 FROM FamilyMember fm2 " +
        "              WHERE fm2.memorial = mem " +
        "              AND fm2.member = :currentUser " +
        "              AND fm2.inviteStatus = 'ACCEPTED' " +
        "              AND fm2.memorialAccess = true)) " +
        "ORDER BY fm.createdAt DESC")
    List<FamilyMember> findAllAccessibleFamilyMembers(@Param("currentUser") Member currentUser);

    /**
     * 메모리얼별 가족 구성원 정보와 함께 조회
     */
    @Query("SELECT fm FROM FamilyMember fm " +
        "JOIN FETCH fm.member m " +
        "JOIN FETCH fm.memorial mem " +
        "WHERE fm.memorial = :memorial " +
        "ORDER BY fm.inviteStatus ASC, fm.createdAt DESC")
    List<FamilyMember> findByMemorialWithDetails(@Param("memorial") Memorial memorial);

    /**
     * 회원의 접근 가능한 메모리얼 상세 정보와 함께 조회
     */
    @Query("SELECT fm FROM FamilyMember fm " +
        "JOIN FETCH fm.memorial mem " +
        "JOIN FETCH mem.owner owner " +
        "WHERE fm.member = :member AND fm.inviteStatus = 'ACCEPTED' AND fm.memorialAccess = true " +
        "ORDER BY fm.lastAccessAt DESC NULLS LAST, fm.createdAt DESC")
    List<FamilyMember> findAccessibleMemorialsWithDetails(@Param("member") Member member);
}