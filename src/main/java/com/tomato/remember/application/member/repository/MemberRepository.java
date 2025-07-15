package com.tomato.remember.application.member.repository;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.code.MemberRole;
import com.tomato.remember.common.code.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 기본 조회 메서드
    Optional<Member> findByUserKey(String userKey);
    Optional<Member> findByPhoneNumber(String phoneNumber);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByInviteCode(String inviteCode);

    // 상태별 조회 메서드
    Optional<Member> findByUserKeyAndStatus(String userKey, MemberStatus status);
    Optional<Member> findByUserKeyAndStatusNot(String userKey, MemberStatus status);
    Optional<Member> findByIdAndStatus(Long id, MemberStatus status);
    Optional<Member> findByIdAndStatusNot(Long id, MemberStatus status);
    Optional<Member> findByPhoneNumberAndStatus(String phoneNumber, MemberStatus status);
    Optional<Member> findByPhoneNumberAndStatusNot(String phoneNumber, MemberStatus status);
    Optional<Member> findByEmailAndStatus(String email, MemberStatus status);
    Optional<Member> findByEmailAndStatusNot(String email, MemberStatus status);

    // 존재 여부 확인 메서드
    boolean existsByUserKey(String userKey);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    boolean existsByInviteCode(String inviteCode);
    boolean existsByUserKeyAndStatus(String userKey, MemberStatus status);
    boolean existsByUserKeyAndStatusNot(String userKey, MemberStatus status);
    boolean existsByPhoneNumberAndStatusNot(String phoneNumber, MemberStatus status);
    boolean existsByEmailAndStatusNot(String email, MemberStatus status);

    // 상태별 목록 조회
    List<Member> findByStatus(MemberStatus status);
    List<Member> findByStatusNot(MemberStatus status);
    List<Member> findByRole(MemberRole role);
    List<Member> findByStatusAndRole(MemberStatus status, MemberRole role);
    Page<Member> findByStatus(MemberStatus status, Pageable pageable);
    Page<Member> findByStatusNot(MemberStatus status, Pageable pageable);
    Page<Member> findByRole(MemberRole role, Pageable pageable);

    // 추천인 관련 조회
    List<Member> findByInviter(Member inviter);
    List<Member> findByInviterAndStatus(Member inviter, MemberStatus status);
    Page<Member> findByInviter(Member inviter, Pageable pageable);

    // 무료 체험 관련 조회
    List<Member> findByFreeTrialEndAtBefore(LocalDateTime dateTime);
    List<Member> findByFreeTrialEndAtAfter(LocalDateTime dateTime);
    List<Member> findByFreeTrialStartAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT m FROM Member m WHERE m.freeTrialStartAt IS NOT NULL AND m.freeTrialEndAt IS NOT NULL AND :now BETWEEN m.freeTrialStartAt AND m.freeTrialEndAt")
    List<Member> findMembersInFreeTrial(@Param("now") LocalDateTime now);

    @Query("SELECT m FROM Member m WHERE m.freeTrialEndAt IS NOT NULL AND m.freeTrialEndAt BETWEEN :startTime AND :endTime")
    List<Member> findMembersWithTrialExpiringBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // 활동 관련 조회
    List<Member> findByLastAccessAtBefore(LocalDateTime dateTime);
    List<Member> findByLastAccessAtAfter(LocalDateTime dateTime);
    List<Member> findByLastAccessAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT m FROM Member m WHERE m.lastAccessAt IS NULL OR m.lastAccessAt < :dateTime")
    List<Member> findInactiveMembers(@Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT m FROM Member m WHERE m.lastAccessAt >= :dateTime AND m.status = :status")
    List<Member> findActiveMembers(@Param("dateTime") LocalDateTime dateTime, @Param("status") MemberStatus status);

    // 알림 설정 관련 조회
    List<Member> findByPushNotificationTrue();
    List<Member> findByMemorialNotificationTrue();
    List<Member> findByPaymentNotificationTrue();
    List<Member> findByFamilyNotificationTrue();
    List<Member> findByMarketingAgreedTrue();

    @Query("SELECT m FROM Member m WHERE m.pushNotification = true AND m.status = :status")
    List<Member> findMembersWithPushNotificationEnabled(@Param("status") MemberStatus status);

    @Query("SELECT m FROM Member m WHERE m.memorialNotification = true AND m.status = :status")
    List<Member> findMembersWithMemorialNotificationEnabled(@Param("status") MemberStatus status);

    // 언어별 조회
    List<Member> findByPreferredLanguage(String preferredLanguage);
    Page<Member> findByPreferredLanguage(String preferredLanguage, Pageable pageable);

    @Query("SELECT m FROM Member m WHERE m.preferredLanguage = :language AND m.status = :status")
    List<Member> findByPreferredLanguageAndStatus(@Param("language") String language, @Param("status") MemberStatus status);

    // 검색 기능
    @Query("SELECT m FROM Member m WHERE m.name LIKE %:keyword% AND m.status != :excludeStatus")
    List<Member> findByNameContainingAndStatusNot(@Param("keyword") String keyword, @Param("excludeStatus") MemberStatus excludeStatus);

    @Query("SELECT m FROM Member m WHERE (m.name LIKE %:keyword% OR m.email LIKE %:keyword% OR m.phoneNumber LIKE %:keyword%) AND m.status != :excludeStatus")
    Page<Member> searchMembers(@Param("keyword") String keyword, @Param("excludeStatus") MemberStatus excludeStatus, Pageable pageable);

    // 통계 관련 쿼리
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = :status")
    long countByStatus(@Param("status") MemberStatus status);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.role = :role")
    long countByRole(@Param("role") MemberRole role);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = :status AND m.role = :role")
    long countByStatusAndRole(@Param("status") MemberStatus status, @Param("role") MemberRole role);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.createdAt >= :startTime AND m.createdAt <= :endTime")
    long countByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.freeTrialStartAt IS NOT NULL AND m.freeTrialEndAt IS NOT NULL AND :now BETWEEN m.freeTrialStartAt AND m.freeTrialEndAt")
    long countMembersInFreeTrial(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.lastAccessAt >= :dateTime")
    long countActiveMembers(@Param("dateTime") LocalDateTime dateTime);

    // 추천인 통계
    @Query("SELECT COUNT(m) FROM Member m WHERE m.inviter = :inviter AND m.status != :excludeStatus")
    long countByInviterAndStatusNot(@Param("inviter") Member inviter, @Param("excludeStatus") MemberStatus excludeStatus);

    @Query("SELECT m.inviter, COUNT(m) FROM Member m WHERE m.inviter IS NOT NULL AND m.status != :excludeStatus GROUP BY m.inviter ORDER BY COUNT(m) DESC")
    List<Object[]> findTopInviters(@Param("excludeStatus") MemberStatus excludeStatus, Pageable pageable);

    // 일괄 업데이트 쿼리
    @Query("UPDATE Member m SET m.status = :newStatus WHERE m.status = :oldStatus")
    int updateStatusBatch(@Param("oldStatus") MemberStatus oldStatus, @Param("newStatus") MemberStatus newStatus);

    @Query("UPDATE Member m SET m.role = :newRole WHERE m.role = :oldRole")
    int updateRoleBatch(@Param("oldRole") MemberRole oldRole, @Param("newRole") MemberRole newRole);

    @Query("UPDATE Member m SET m.status = :status WHERE m.lastAccessAt < :dateTime")
    int updateInactiveMembersStatus(@Param("dateTime") LocalDateTime dateTime, @Param("status") MemberStatus status);

    @Query("UPDATE Member m SET m.lastAccessAt = :now WHERE m.id = :memberId")
    int updateLastAccessAt(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    // 복잡한 비즈니스 로직 쿼리
    @Query("SELECT m FROM Member m WHERE m.status = :status AND m.ownedMemorials IS NOT EMPTY")
    List<Member> findMembersWithMemorials(@Param("status") MemberStatus status);

    @Query("SELECT m FROM Member m WHERE m.status = :status AND m.videoCalls IS NOT EMPTY")
    List<Member> findMembersWithVideoCalls(@Param("status") MemberStatus status);

    @Query("SELECT m FROM Member m WHERE m.status = :status AND SIZE(m.ownedMemorials) > :count")
    List<Member> findMembersWithMoreThanXMemorials(@Param("status") MemberStatus status, @Param("count") int count);

    @Query("SELECT m FROM Member m WHERE m.status = :status AND SIZE(m.videoCalls) > :count")
    List<Member> findMembersWithMoreThanXVideoCalls(@Param("status") MemberStatus status, @Param("count") int count);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.ownedMemorials WHERE m.id = :memberId")
    Optional<Member> findByIdWithMemorials(@Param("memberId") Long memberId);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.videoCalls WHERE m.id = :memberId")
    Optional<Member> findByIdWithVideoCalls(@Param("memberId") Long memberId);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.familyMemberships WHERE m.id = :memberId")
    Optional<Member> findByIdWithFamilyMemberships(@Param("memberId") Long memberId);

    // 관리자용 쿼리
    @Query("SELECT m FROM Member m WHERE m.status = :status ORDER BY m.createdAt DESC")
    Page<Member> findByStatusOrderByCreatedAtDesc(@Param("status") MemberStatus status, Pageable pageable);

    @Query("SELECT m FROM Member m WHERE m.status = :status ORDER BY m.lastAccessAt DESC NULLS LAST")
    Page<Member> findByStatusOrderByLastAccessAtDesc(@Param("status") MemberStatus status, Pageable pageable);

    @Query("SELECT m FROM Member m WHERE m.createdAt >= :startTime AND m.createdAt <= :endTime ORDER BY m.createdAt DESC")
    List<Member> findMembersRegisteredBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // 중복 체크용 쿼리
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Member m WHERE m.userKey = :userKey AND m.id != :excludeId")
    boolean existsByUserKeyAndIdNot(@Param("userKey") String userKey, @Param("excludeId") Long excludeId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Member m WHERE m.phoneNumber = :phoneNumber AND m.id != :excludeId")
    boolean existsByPhoneNumberAndIdNot(@Param("phoneNumber") String phoneNumber, @Param("excludeId") Long excludeId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Member m WHERE m.email = :email AND m.id != :excludeId")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("excludeId") Long excludeId);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.profileImages WHERE m.id = :memberId")
    Member findByIdWithProfileImages(@Param("memberId") Long id);
}