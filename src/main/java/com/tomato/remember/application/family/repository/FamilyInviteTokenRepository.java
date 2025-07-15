package com.tomato.remember.application.family.repository;

import com.tomato.remember.application.family.code.InviteStatus;
import com.tomato.remember.application.family.entity.FamilyInviteToken;
import com.tomato.remember.application.member.entity.Member;
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

    // ===== 토큰 조회 =====
    
    /**
     * 토큰으로 초대 정보 조회
     */
    Optional<FamilyInviteToken> findByToken(String token);
    
    /**
     * 사용 가능한 토큰 조회
     */
    @Query("SELECT t FROM FamilyInviteToken t WHERE t.token = :token AND t.status = 'PENDING' AND t.expiresAt > :now")
    Optional<FamilyInviteToken> findUsableToken(@Param("token") String token, @Param("now") LocalDateTime now);
    
    /**
     * 토큰 존재 여부 확인
     */
    boolean existsByToken(String token);
    
    // ===== 메모리얼별 조회 =====
    
    /**
     * 메모리얼의 모든 초대 토큰 조회
     */
    List<FamilyInviteToken> findByMemorialOrderByCreatedAtDesc(Memorial memorial);
    
    /**
     * 메모리얼의 특정 상태 초대 토큰 조회
     */
    List<FamilyInviteToken> findByMemorialAndStatusOrderByCreatedAtDesc(Memorial memorial, InviteStatus status);
    
    /**
     * 메모리얼의 대기 중인 초대 토큰 조회
     */
    @Query("SELECT t FROM FamilyInviteToken t WHERE t.memorial = :memorial AND t.status = 'PENDING' AND t.expiresAt > :now ORDER BY t.createdAt DESC")
    List<FamilyInviteToken> findPendingTokensByMemorial(@Param("memorial") Memorial memorial, @Param("now") LocalDateTime now);
    
    // ===== 초대자별 조회 =====
    
    /**
     * 초대자가 보낸 모든 초대 토큰 조회
     */
    List<FamilyInviteToken> findByInviterOrderByCreatedAtDesc(Member inviter);
    
    /**
     * 초대자가 보낸 특정 상태 초대 토큰 조회
     */
    List<FamilyInviteToken> findByInviterAndStatusOrderByCreatedAtDesc(Member inviter, InviteStatus status);
    
    /**
     * 초대자가 보낸 대기 중인 초대 토큰 조회
     */
    @Query("SELECT t FROM FamilyInviteToken t WHERE t.inviter = :inviter AND t.status = 'PENDING' AND t.expiresAt > :now ORDER BY t.createdAt DESC")
    List<FamilyInviteToken> findPendingTokensByInviter(@Param("inviter") Member inviter, @Param("now") LocalDateTime now);
    
    // ===== 연락처별 조회 =====
    
    /**
     * 연락처로 초대 토큰 조회
     */
    List<FamilyInviteToken> findByContactOrderByCreatedAtDesc(String contact);
    
    /**
     * 특정 메모리얼과 연락처의 대기 중인 초대 토큰 조회 (중복 확인용)
     */
    @Query("SELECT t FROM FamilyInviteToken t WHERE t.memorial = :memorial AND t.contact = :contact AND t.status = 'PENDING' AND t.expiresAt > :now")
    List<FamilyInviteToken> findPendingTokensByMemorialAndContact(@Param("memorial") Memorial memorial, 
                                                                  @Param("contact") String contact, 
                                                                  @Param("now") LocalDateTime now);
    
    /**
     * 연락처의 유효한 초대 토큰 존재 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM FamilyInviteToken t WHERE t.contact = :contact AND t.status = 'PENDING' AND t.expiresAt > :now")
    boolean existsValidTokenByContact(@Param("contact") String contact, @Param("now") LocalDateTime now);
    
    // ===== 만료 처리 =====
    
    /**
     * 만료된 토큰 조회
     */
    @Query("SELECT t FROM FamilyInviteToken t WHERE t.status = 'PENDING' AND t.expiresAt < :now")
    List<FamilyInviteToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * 특정 시간 이전의 만료된 토큰 조회
     */
    @Query("SELECT t FROM FamilyInviteToken t WHERE t.status = 'PENDING' AND t.expiresAt < :expireTime")
    List<FamilyInviteToken> findTokensExpiredBefore(@Param("expireTime") LocalDateTime expireTime);
    
    // ===== 통계 조회 =====
    
    /**
     * 메모리얼의 초대 토큰 수 조회
     */
    long countByMemorial(Memorial memorial);
    
    /**
     * 메모리얼의 특정 상태 초대 토큰 수 조회
     */
    long countByMemorialAndStatus(Memorial memorial, InviteStatus status);
    
    /**
     * 초대자의 초대 토큰 수 조회
     */
    long countByInviter(Member inviter);
    
    /**
     * 초대자의 특정 상태 초대 토큰 수 조회
     */
    long countByInviterAndStatus(Member inviter, InviteStatus status);
    
    /**
     * 연락처의 초대 토큰 수 조회
     */
    long countByContact(String contact);
    
    /**
     * 전체 대기 중인 초대 토큰 수 조회
     */
    @Query("SELECT COUNT(t) FROM FamilyInviteToken t WHERE t.status = 'PENDING' AND t.expiresAt > :now")
    long countPendingTokens(@Param("now") LocalDateTime now);
    
    // ===== 정리 작업 =====
    
    /**
     * 특정 상태의 오래된 토큰 삭제
     */
    void deleteByStatusAndCreatedAtBefore(InviteStatus status, LocalDateTime createdBefore);
    
    /**
     * 특정 메모리얼의 모든 토큰 삭제
     */
    void deleteByMemorial(Memorial memorial);
    
    /**
     * 특정 회원의 모든 토큰 삭제
     */
    void deleteByInviter(Member inviter);
    
    // ===== 복합 조회 =====
    
    /**
     * 메모리얼과 초대자의 토큰 조회
     */
    List<FamilyInviteToken> findByMemorialAndInviterOrderByCreatedAtDesc(Memorial memorial, Member inviter);
    
    /**
     * 최근 생성된 토큰 조회 (관리자용)
     */
    @Query("SELECT t FROM FamilyInviteToken t ORDER BY t.createdAt DESC")
    List<FamilyInviteToken> findRecentTokens();
    
    /**
     * 특정 기간 내 생성된 토큰 조회
     */
    @Query("SELECT t FROM FamilyInviteToken t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<FamilyInviteToken> findTokensCreatedBetween(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);
    
    // ===== 상태별 조회 =====
    
    /**
     * 수락된 토큰 조회
     */
    List<FamilyInviteToken> findByStatusOrderByAcceptedAtDesc(InviteStatus status);
    
    /**
     * 특정 회원이 수락한 토큰 조회
     */
    List<FamilyInviteToken> findByAcceptedMemberOrderByAcceptedAtDesc(Member acceptedMember);
    
    /**
     * 특정 회원이 수락한 토큰 수 조회
     */
    long countByAcceptedMember(Member acceptedMember);
}