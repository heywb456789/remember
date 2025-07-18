package com.tomato.remember.application.family.repository;

import com.tomato.remember.application.family.code.InviteStatus;
import com.tomato.remember.application.family.entity.FamilyInviteToken;
import com.tomato.remember.application.memorial.entity.Memorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 가족 초대 토큰 레포지토리
 */
@Repository
public interface FamilyInviteTokenRepository extends JpaRepository<FamilyInviteToken, Long> {

    /**
     * 토큰으로 초대 정보 조회
     */
    Optional<FamilyInviteToken> findByToken(String token);

    /**
     * 사용 가능한 토큰 조회 (PENDING 상태 + 만료되지 않음)
     */
    @Query("SELECT t FROM FamilyInviteToken t WHERE t.token = :token AND t.status = 'PENDING' AND t.expiresAt > :now")
    Optional<FamilyInviteToken> findUsableToken(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * 만료된 토큰 목록 조회
     */
    @Query("SELECT t FROM FamilyInviteToken t WHERE t.status = 'PENDING' AND t.expiresAt < :now")
    List<FamilyInviteToken> findExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 특정 메모리얼과 연락처의 대기 중인 토큰 조회
     */
    @Query("SELECT t FROM FamilyInviteToken t WHERE t.memorial = :memorial AND t.contact = :contact AND t.status = 'PENDING' AND t.expiresAt > :now")
    List<FamilyInviteToken> findPendingTokensByMemorialAndContact(
        @Param("memorial") Memorial memorial,
        @Param("contact") String contact,
        @Param("now") LocalDateTime now);

    /**
     * 특정 메모리얼과 연락처의 모든 토큰 조회 (히스토리 조회용)
     */
    @Query("SELECT fit FROM FamilyInviteToken fit " +
        "WHERE fit.memorial = :memorial " +
        "AND fit.contact = :contact " +
        "ORDER BY fit.createdAt DESC")
    List<FamilyInviteToken> findByMemorialAndContactOrderByCreatedAtDesc(
        @Param("memorial") Memorial memorial,
        @Param("contact") String contact);

    /**
     * 특정 초대자의 토큰 목록 조회
     */
    List<FamilyInviteToken> findByInviterIdOrderByCreatedAtDesc(Long inviterId);

    /**
     * 특정 메모리얼의 토큰 목록 조회
     */
    List<FamilyInviteToken> findByMemorialOrderByCreatedAtDesc(Memorial memorial);

    /**
     * 특정 상태의 토큰 목록 조회
     */
    List<FamilyInviteToken> findByStatusOrderByCreatedAtDesc(InviteStatus status);

    /**
     * 특정 연락처의 토큰 목록 조회
     */
    List<FamilyInviteToken> findByContactOrderByCreatedAtDesc(String contact);

    /**
     * 특정 기간 내의 토큰 목록 조회
     */
    @Query("SELECT t FROM FamilyInviteToken t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<FamilyInviteToken> findByDateRange(@Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 초대자의 대기 중인 토큰 개수
     */
    @Query("SELECT COUNT(t) FROM FamilyInviteToken t WHERE t.inviter.id = :inviterId AND t.status = 'PENDING' AND t.expiresAt > :now")
    long countPendingTokensByInviter(@Param("inviterId") Long inviterId, @Param("now") LocalDateTime now);

    /**
     * 특정 메모리얼의 대기 중인 토큰 개수
     */
    @Query("SELECT COUNT(t) FROM FamilyInviteToken t WHERE t.memorial = :memorial AND t.status = 'PENDING' AND t.expiresAt > :now")
    long countPendingTokensByMemorial(@Param("memorial") Memorial memorial, @Param("now") LocalDateTime now);

    /**
     * 특정 연락처의 대기 중인 토큰 개수
     */
    @Query("SELECT COUNT(t) FROM FamilyInviteToken t WHERE t.contact = :contact AND t.status = 'PENDING' AND t.expiresAt > :now")
    long countPendingTokensByContact(@Param("contact") String contact, @Param("now") LocalDateTime now);
}