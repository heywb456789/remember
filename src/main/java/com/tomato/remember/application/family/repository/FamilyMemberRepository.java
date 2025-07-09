package com.tomato.remember.application.family.repository;

import com.tomato.remember.application.family.code.DeviceType;
import com.tomato.remember.application.family.code.FamilyRole;
import com.tomato.remember.application.family.code.InviteMethod;
import com.tomato.remember.application.family.code.InviteStatus;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.code.Language;
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
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    // 기본 조회 메서드
    Optional<FamilyMember> findByMemorialAndMember(Memorial memorial, Member member);
    Optional<FamilyMember> findByMemorialAndMemberAndInviteStatus(Memorial memorial, Member member, InviteStatus inviteStatus);
    Optional<FamilyMember> findByInviteToken(String inviteToken);
    Optional<FamilyMember> findByInviteTokenAndInviteStatus(String inviteToken, InviteStatus inviteStatus);

    // 메모리얼별 조회
    List<FamilyMember> findByMemorial(Memorial memorial);
    List<FamilyMember> findByMemorialAndInviteStatus(Memorial memorial, InviteStatus inviteStatus);
    List<FamilyMember> findByMemorialAndInviteStatusNot(Memorial memorial, InviteStatus inviteStatus);
    Page<FamilyMember> findByMemorial(Memorial memorial, Pageable pageable);
    Page<FamilyMember> findByMemorialAndInviteStatus(Memorial memorial, InviteStatus inviteStatus, Pageable pageable);

    // 회원별 조회
    List<FamilyMember> findByMember(Member member);
    List<FamilyMember> findByMemberAndInviteStatus(Member member, InviteStatus inviteStatus);
    List<FamilyMember> findByMemberAndInviteStatusNot(Member member, InviteStatus inviteStatus);
    Page<FamilyMember> findByMember(Member member, Pageable pageable);
    Page<FamilyMember> findByMemberAndInviteStatus(Member member, InviteStatus inviteStatus, Pageable pageable);

    // 초대자별 조회
    List<FamilyMember> findByInviter(Member inviter);
    List<FamilyMember> findByInviterAndInviteStatus(Member inviter, InviteStatus inviteStatus);
    Page<FamilyMember> findByInviter(Member inviter, Pageable pageable);

    // 초대 상태별 조회
    List<FamilyMember> findByInviteStatus(InviteStatus inviteStatus);
    Page<FamilyMember> findByInviteStatus(InviteStatus inviteStatus, Pageable pageable);

    // 가족 권한별 조회
    List<FamilyMember> findByFamilyRole(FamilyRole familyRole);
    List<FamilyMember> findByFamilyRoleAndInviteStatus(FamilyRole familyRole, InviteStatus inviteStatus);
    Page<FamilyMember> findByFamilyRoleAndInviteStatus(FamilyRole familyRole, InviteStatus inviteStatus, Pageable pageable);

    // 관계별 조회
    List<FamilyMember> findByRelationship(Relationship relationship);
    List<FamilyMember> findByRelationshipAndInviteStatus(Relationship relationship, InviteStatus inviteStatus);
    Page<FamilyMember> findByRelationshipAndInviteStatus(Relationship relationship, InviteStatus inviteStatus, Pageable pageable);

    // 초대 방법별 조회
    List<FamilyMember> findByInviteMethod(InviteMethod inviteMethod);
    List<FamilyMember> findByInviteMethodAndInviteStatus(InviteMethod inviteMethod, InviteStatus inviteStatus);

    // 언어별 조회
    List<FamilyMember> findByLanguage(Language language);
    List<FamilyMember> findByLanguageAndInviteStatus(Language language, InviteStatus inviteStatus);

    // 디바이스 유형별 조회
    List<FamilyMember> findByDeviceType(DeviceType deviceType);
    List<FamilyMember> findByDeviceTypeAndInviteStatus(DeviceType deviceType, InviteStatus inviteStatus);

    // 알림 설정별 조회
    List<FamilyMember> findByNotificationEnabled(Boolean notificationEnabled);
    List<FamilyMember> findByNotificationEnabledAndInviteStatus(Boolean notificationEnabled, InviteStatus inviteStatus);

    // 초대 연락처별 조회
    List<FamilyMember> findByInviteContact(String inviteContact);
    List<FamilyMember> findByInviteContactAndInviteStatus(String inviteContact, InviteStatus inviteStatus);
    Optional<FamilyMember> findByInviteContactAndInviteStatusAndMemorial(String inviteContact, InviteStatus inviteStatus, Memorial memorial);

    // 존재 여부 확인
    boolean existsByMemorialAndMember(Memorial memorial, Member member);
    boolean existsByMemorialAndMemberAndInviteStatus(Memorial memorial, Member member, InviteStatus inviteStatus);
    boolean existsByInviteToken(String inviteToken);
    boolean existsByInviteTokenAndInviteStatus(String inviteToken, InviteStatus inviteStatus);
    boolean existsByInviteContactAndMemorial(String inviteContact, Memorial memorial);

    // 시간별 조회
    List<FamilyMember> findByInvitedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<FamilyMember> findByAcceptedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<FamilyMember> findByRejectedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<FamilyMember> findByLastActivityAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.invitedAt >= :startTime AND fm.invitedAt <= :endTime AND fm.inviteStatus = :inviteStatus")
    List<FamilyMember> findByInvitedAtBetweenAndInviteStatus(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.acceptedAt >= :startTime AND fm.acceptedAt <= :endTime AND fm.inviteStatus = :inviteStatus")
    List<FamilyMember> findByAcceptedAtBetweenAndInviteStatus(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("inviteStatus") InviteStatus inviteStatus);

    // 만료된 초대 조회
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.inviteStatus = :inviteStatus AND fm.invitedAt < :expiredTime")
    List<FamilyMember> findExpiredInvites(@Param("inviteStatus") InviteStatus inviteStatus, @Param("expiredTime") LocalDateTime expiredTime);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.inviteStatus = 'PENDING' AND fm.invitedAt < :expiredTime")
    List<FamilyMember> findPendingInvitesOlderThan(@Param("expiredTime") LocalDateTime expiredTime);

    // 활성 참여자 조회
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.inviteStatus = 'ACCEPTED' AND fm.member IS NOT NULL")
    List<FamilyMember> findActiveParticipants();

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.inviteStatus = 'ACCEPTED' AND fm.member IS NOT NULL")
    List<FamilyMember> findActiveParticipantsByMemorial(@Param("memorial") Memorial memorial);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.inviteStatus = 'ACCEPTED' AND fm.member IS NOT NULL")
    Page<FamilyMember> findActiveParticipantsByMemorial(@Param("memorial") Memorial memorial, Pageable pageable);

    // 주계정 조회
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.familyRole = 'MAIN' AND fm.inviteStatus = 'ACCEPTED'")
    List<FamilyMember> findMainAccountsByMemorial(@Param("memorial") Memorial memorial);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.familyRole = 'MAIN' AND fm.inviteStatus = 'ACCEPTED'")
    Optional<FamilyMember> findMainAccountByMemorial(@Param("memorial") Memorial memorial);

    // 부계정 조회
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.familyRole = 'SUB' AND fm.inviteStatus = 'ACCEPTED'")
    List<FamilyMember> findSubAccountsByMemorial(@Param("memorial") Memorial memorial);

    // 복합 조건 조회
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.familyRole = :familyRole AND fm.inviteStatus = :inviteStatus")
    List<FamilyMember> findByMemorialAndFamilyRoleAndInviteStatus(@Param("memorial") Memorial memorial, @Param("familyRole") FamilyRole familyRole, @Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.member = :member AND fm.familyRole = :familyRole AND fm.inviteStatus = :inviteStatus")
    List<FamilyMember> findByMemberAndFamilyRoleAndInviteStatus(@Param("member") Member member, @Param("familyRole") FamilyRole familyRole, @Param("inviteStatus") InviteStatus inviteStatus);

    // 통계 관련 쿼리
    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.inviteStatus = :inviteStatus")
    long countByInviteStatus(@Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.inviteStatus = :inviteStatus")
    long countByMemorialAndInviteStatus(@Param("memorial") Memorial memorial, @Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.member = :member AND fm.inviteStatus = :inviteStatus")
    long countByMemberAndInviteStatus(@Param("member") Member member, @Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.inviter = :inviter AND fm.inviteStatus = :inviteStatus")
    long countByInviterAndInviteStatus(@Param("inviter") Member inviter, @Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.familyRole = :familyRole AND fm.inviteStatus = :inviteStatus")
    long countByFamilyRoleAndInviteStatus(@Param("familyRole") FamilyRole familyRole, @Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.relationship = :relationship AND fm.inviteStatus = :inviteStatus")
    long countByRelationshipAndInviteStatus(@Param("relationship") Relationship relationship, @Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.inviteMethod = :inviteMethod AND fm.inviteStatus = :inviteStatus")
    long countByInviteMethodAndInviteStatus(@Param("inviteMethod") InviteMethod inviteMethod, @Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.invitedAt >= :startTime AND fm.invitedAt <= :endTime")
    long countByInvitedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.acceptedAt >= :startTime AND fm.acceptedAt <= :endTime")
    long countByAcceptedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.notificationEnabled = true AND fm.inviteStatus = 'ACCEPTED'")
    long countMembersWithNotificationEnabled();

    // 정렬된 조회
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.memorial = :memorial ORDER BY fm.acceptedAt DESC NULLS LAST, fm.invitedAt DESC")
    List<FamilyMember> findByMemorialOrderByAcceptedAtDesc(@Param("memorial") Memorial memorial);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.memorial = :memorial ORDER BY fm.acceptedAt DESC NULLS LAST, fm.invitedAt DESC")
    Page<FamilyMember> findByMemorialOrderByAcceptedAtDesc(@Param("memorial") Memorial memorial, Pageable pageable);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.inviteStatus = :inviteStatus ORDER BY fm.invitedAt DESC")
    List<FamilyMember> findByInviteStatusOrderByInvitedAtDesc(@Param("inviteStatus") InviteStatus inviteStatus, Pageable pageable);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.inviteStatus = :inviteStatus ORDER BY fm.lastActivityAt DESC NULLS LAST")
    List<FamilyMember> findByInviteStatusOrderByLastActivityAtDesc(@Param("inviteStatus") InviteStatus inviteStatus, Pageable pageable);

    // 연관 엔티티와 함께 조회
    @Query("SELECT fm FROM FamilyMember fm LEFT JOIN FETCH fm.memorial WHERE fm.id = :id")
    Optional<FamilyMember> findByIdWithMemorial(@Param("id") Long id);

    @Query("SELECT fm FROM FamilyMember fm LEFT JOIN FETCH fm.member WHERE fm.id = :id")
    Optional<FamilyMember> findByIdWithMember(@Param("id") Long id);

    @Query("SELECT fm FROM FamilyMember fm LEFT JOIN FETCH fm.inviter WHERE fm.id = :id")
    Optional<FamilyMember> findByIdWithInviter(@Param("id") Long id);

    @Query("SELECT fm FROM FamilyMember fm LEFT JOIN FETCH fm.memorial LEFT JOIN FETCH fm.member WHERE fm.id = :id")
    Optional<FamilyMember> findByIdWithMemorialAndMember(@Param("id") Long id);

    @Query("SELECT fm FROM FamilyMember fm LEFT JOIN FETCH fm.memorial LEFT JOIN FETCH fm.member LEFT JOIN FETCH fm.inviter WHERE fm.id = :id")
    Optional<FamilyMember> findByIdWithAllRelations(@Param("id") Long id);

    // 검색 기능
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.familyNickname LIKE %:keyword% AND fm.inviteStatus = :inviteStatus")
    List<FamilyMember> findByFamilyNicknameContainingAndInviteStatus(@Param("keyword") String keyword, @Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.inviteContact LIKE %:keyword% AND fm.inviteStatus = :inviteStatus")
    List<FamilyMember> findByInviteContactContainingAndInviteStatus(@Param("keyword") String keyword, @Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT fm FROM FamilyMember fm WHERE (fm.familyNickname LIKE %:keyword% OR fm.inviteContact LIKE %:keyword%) AND fm.inviteStatus = :inviteStatus")
    Page<FamilyMember> searchByKeywordAndInviteStatus(@Param("keyword") String keyword, @Param("inviteStatus") InviteStatus inviteStatus, Pageable pageable);

    // 최근 활동 조회
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.inviteStatus = 'ACCEPTED' AND fm.lastActivityAt >= :dateTime")
    List<FamilyMember> findRecentlyActiveMembers(@Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.inviteStatus = 'ACCEPTED' AND fm.lastActivityAt >= :dateTime")
    List<FamilyMember> findRecentlyActiveMembersByMemorial(@Param("memorial") Memorial memorial, @Param("dateTime") LocalDateTime dateTime);

    // 비활성 회원 조회
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.inviteStatus = 'ACCEPTED' AND (fm.lastActivityAt IS NULL OR fm.lastActivityAt < :dateTime)")
    List<FamilyMember> findInactiveMembers(@Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.inviteStatus = 'ACCEPTED' AND (fm.lastActivityAt IS NULL OR fm.lastActivityAt < :dateTime)")
    List<FamilyMember> findInactiveMembersByMemorial(@Param("memorial") Memorial memorial, @Param("dateTime") LocalDateTime dateTime);

    // 일괄 업데이트 쿼리
    @Query("UPDATE FamilyMember fm SET fm.inviteStatus = :newStatus WHERE fm.inviteStatus = :oldStatus")
    int updateInviteStatusBatch(@Param("oldStatus") InviteStatus oldStatus, @Param("newStatus") InviteStatus newStatus);

    @Query("UPDATE FamilyMember fm SET fm.inviteStatus = 'EXPIRED' WHERE fm.inviteStatus = 'PENDING' AND fm.invitedAt < :expiredTime")
    int expireOldInvites(@Param("expiredTime") LocalDateTime expiredTime);

    @Query("UPDATE FamilyMember fm SET fm.lastActivityAt = :now WHERE fm.id = :id")
    int updateLastActivityAt(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Query("UPDATE FamilyMember fm SET fm.notificationEnabled = :enabled WHERE fm.id = :id")
    int updateNotificationEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);

    // 그룹화된 통계
    @Query("SELECT fm.inviteStatus, COUNT(fm) FROM FamilyMember fm GROUP BY fm.inviteStatus")
    List<Object[]> countByInviteStatusGrouped();

    @Query("SELECT fm.familyRole, COUNT(fm) FROM FamilyMember fm WHERE fm.inviteStatus = :inviteStatus GROUP BY fm.familyRole")
    List<Object[]> countByFamilyRoleGrouped(@Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT fm.relationship, COUNT(fm) FROM FamilyMember fm WHERE fm.inviteStatus = :inviteStatus GROUP BY fm.relationship")
    List<Object[]> countByRelationshipGrouped(@Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT fm.inviteMethod, COUNT(fm) FROM FamilyMember fm WHERE fm.inviteStatus = :inviteStatus GROUP BY fm.inviteMethod")
    List<Object[]> countByInviteMethodGrouped(@Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT fm.language, COUNT(fm) FROM FamilyMember fm WHERE fm.inviteStatus = :inviteStatus GROUP BY fm.language")
    List<Object[]> countByLanguageGrouped(@Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT fm.deviceType, COUNT(fm) FROM FamilyMember fm WHERE fm.inviteStatus = :inviteStatus GROUP BY fm.deviceType")
    List<Object[]> countByDeviceTypeGrouped(@Param("inviteStatus") InviteStatus inviteStatus);

    @Query("SELECT DATE(fm.invitedAt), COUNT(fm) FROM FamilyMember fm WHERE fm.invitedAt >= :startTime AND fm.invitedAt <= :endTime GROUP BY DATE(fm.invitedAt) ORDER BY DATE(fm.invitedAt)")
    List<Object[]> countByInvitedAtGroupedByDate(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT DATE(fm.acceptedAt), COUNT(fm) FROM FamilyMember fm WHERE fm.acceptedAt >= :startTime AND fm.acceptedAt <= :endTime GROUP BY DATE(fm.acceptedAt) ORDER BY DATE(fm.acceptedAt)")
    List<Object[]> countByAcceptedAtGroupedByDate(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // 고급 검색 쿼리
    @Query("SELECT fm FROM FamilyMember fm WHERE " +
           "(:memorial IS NULL OR fm.memorial = :memorial) AND " +
           "(:member IS NULL OR fm.member = :member) AND " +
           "(:inviter IS NULL OR fm.inviter = :inviter) AND " +
           "(:familyRole IS NULL OR fm.familyRole = :familyRole) AND " +
           "(:relationship IS NULL OR fm.relationship = :relationship) AND " +
           "(:inviteStatus IS NULL OR fm.inviteStatus = :inviteStatus) AND " +
           "(:inviteMethod IS NULL OR fm.inviteMethod = :inviteMethod) AND " +
           "(:language IS NULL OR fm.language = :language) AND " +
           "(:notificationEnabled IS NULL OR fm.notificationEnabled = :notificationEnabled) AND " +
           "(:startTime IS NULL OR fm.invitedAt >= :startTime) AND " +
           "(:endTime IS NULL OR fm.invitedAt <= :endTime)")
    Page<FamilyMember> findBySearchCriteria(
        @Param("memorial") Memorial memorial,
        @Param("member") Member member,
        @Param("inviter") Member inviter,
        @Param("familyRole") FamilyRole familyRole,
        @Param("relationship") Relationship relationship,
        @Param("inviteStatus") InviteStatus inviteStatus,
        @Param("inviteMethod") InviteMethod inviteMethod,
        @Param("language") Language language,
        @Param("notificationEnabled") Boolean notificationEnabled,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );

    // 권한 관련 조회
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.member = :member AND fm.inviteStatus = 'ACCEPTED' AND fm.familyRole = 'MAIN'")
    Optional<FamilyMember> findMainAccountByMemorialAndMember(@Param("memorial") Memorial memorial, @Param("member") Member member);

    @Query("SELECT CASE WHEN COUNT(fm) > 0 THEN true ELSE false END FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.member = :member AND fm.inviteStatus = 'ACCEPTED' AND fm.familyRole = 'MAIN'")
    boolean isMainAccountByMemorialAndMember(@Param("memorial") Memorial memorial, @Param("member") Member member);

    @Query("SELECT CASE WHEN COUNT(fm) > 0 THEN true ELSE false END FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.member = :member AND fm.inviteStatus = 'ACCEPTED'")
    boolean canAccessMemorialByMember(@Param("memorial") Memorial memorial, @Param("member") Member member);

    // 초대 토큰 유효성 검증
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.inviteToken = :token AND fm.inviteStatus = 'PENDING' AND fm.invitedAt >= :validSince")
    Optional<FamilyMember> findValidInviteToken(@Param("token") String token, @Param("validSince") LocalDateTime validSince);

    @Query("SELECT CASE WHEN COUNT(fm) > 0 THEN true ELSE false END FROM FamilyMember fm WHERE fm.inviteToken = :token AND fm.inviteStatus = 'PENDING' AND fm.invitedAt >= :validSince")
    boolean isValidInviteToken(@Param("token") String token, @Param("validSince") LocalDateTime validSince);

    // 중복 초대 방지
    @Query("SELECT CASE WHEN COUNT(fm) > 0 THEN true ELSE false END FROM FamilyMember fm WHERE fm.memorial = :memorial AND fm.inviteContact = :contact AND fm.inviteStatus IN ('PENDING', 'ACCEPTED')")
    boolean existsActiveInviteByMemorialAndContact(@Param("memorial") Memorial memorial, @Param("contact") String contact);

    // 삭제 관련 쿼리
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.inviteStatus = 'REJECTED' AND fm.rejectedAt < :dateTime")
    List<FamilyMember> findOldRejectedInvites(@Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.inviteStatus = 'EXPIRED' AND fm.updatedAt < :dateTime")
    List<FamilyMember> findOldExpiredInvites(@Param("dateTime") LocalDateTime dateTime);
}