package com.tomato.remember.application.memorial.repository;

import com.tomato.remember.application.family.code.InviteStatus;
import com.tomato.remember.application.member.code.Gender;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.memorial.code.AiTrainingStatus;
import com.tomato.remember.application.memorial.code.MemorialStatus;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemorialRepository extends JpaRepository<Memorial, Long> {

    // 기본 조회 메서드
    Optional<Memorial> findByIdAndStatus(Long id, MemorialStatus status);
    Optional<Memorial> findByIdAndStatusNot(Long id, MemorialStatus status);
    
    // 소유자별 조회
    List<Memorial> findByOwner(Member owner);
    List<Memorial> findByOwnerAndStatus(Member owner, MemorialStatus status);
    List<Memorial> findByOwnerAndStatusNot(Member owner, MemorialStatus status);
    Page<Memorial> findByOwner(Member owner, Pageable pageable);
    Page<Memorial> findByOwnerAndStatus(Member owner, MemorialStatus status, Pageable pageable);

    // 상태별 조회
    List<Memorial> findByStatus(MemorialStatus status);
    List<Memorial> findByStatusNot(MemorialStatus status);
    Page<Memorial> findByStatus(MemorialStatus status, Pageable pageable);
    Page<Memorial> findByStatusNot(MemorialStatus status, Pageable pageable);

    // 공개 여부별 조회
    List<Memorial> findByIsPublic(Boolean isPublic);
    List<Memorial> findByIsPublicAndStatus(Boolean isPublic, MemorialStatus status);
    Page<Memorial> findByIsPublicAndStatus(Boolean isPublic, MemorialStatus status, Pageable pageable);

    // 성별별 조회
    List<Memorial> findByGender(Gender gender);
    List<Memorial> findByGenderAndStatus(Gender gender, MemorialStatus status);
    Page<Memorial> findByGenderAndStatus(Gender gender, MemorialStatus status, Pageable pageable);

    // 관계별 조회
    List<Memorial> findByRelationship(Relationship relationship);
    List<Memorial> findByRelationshipAndStatus(Relationship relationship, MemorialStatus status);
    Page<Memorial> findByRelationshipAndStatus(Relationship relationship, MemorialStatus status, Pageable pageable);

    // AI 학습 상태별 조회
    List<Memorial> findByAiTrainingStatus(AiTrainingStatus aiTrainingStatus);
    List<Memorial> findByAiTrainingStatusAndStatus(AiTrainingStatus aiTrainingStatus, MemorialStatus status);
    List<Memorial> findByAiTrainingCompleted(Boolean aiTrainingCompleted);
    List<Memorial> findByAiTrainingCompletedAndStatus(Boolean aiTrainingCompleted, MemorialStatus status);
    Page<Memorial> findByAiTrainingStatus(AiTrainingStatus aiTrainingStatus, Pageable pageable);

    // 이름으로 검색
    List<Memorial> findByNameContaining(String name);
    List<Memorial> findByNameContainingAndStatus(String name, MemorialStatus status);
    Page<Memorial> findByNameContainingAndStatus(String name, MemorialStatus status, Pageable pageable);

    // 닉네임으로 검색
    List<Memorial> findByNicknameContaining(String nickname);
    List<Memorial> findByNicknameContainingAndStatus(String nickname, MemorialStatus status);

    // 날짜별 조회
    List<Memorial> findByBirthDate(LocalDate birthDate);
    List<Memorial> findByDeathDate(LocalDate deathDate);
    List<Memorial> findByBirthDateBetween(LocalDate startDate, LocalDate endDate);
    List<Memorial> findByDeathDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT m FROM Memorial m WHERE m.birthDate >= :startDate AND m.birthDate <= :endDate AND m.status = :status")
    List<Memorial> findByBirthDateBetweenAndStatus(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("status") MemorialStatus status);

    @Query("SELECT m FROM Memorial m WHERE m.deathDate >= :startDate AND m.deathDate <= :endDate AND m.status = :status")
    List<Memorial> findByDeathDateBetweenAndStatus(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("status") MemorialStatus status);

    // 기일 관련 조회
    @Query("SELECT m FROM Memorial m WHERE MONTH(m.deathDate) = :month AND DAY(m.deathDate) = :day AND m.status = :status")
    List<Memorial> findByDeathDateMonthAndDay(@Param("month") int month, @Param("day") int day, @Param("status") MemorialStatus status);

    @Query("SELECT m FROM Memorial m WHERE MONTH(m.birthDate) = :month AND DAY(m.birthDate) = :day AND m.status = :status")
    List<Memorial> findByBirthDateMonthAndDay(@Param("month") int month, @Param("day") int day, @Param("status") MemorialStatus status);

    // 방문 관련 조회
    List<Memorial> findByLastVisitAt(LocalDate lastVisitAt);
    List<Memorial> findByLastVisitAtBefore(LocalDate date);
    List<Memorial> findByLastVisitAtAfter(LocalDate date);
    List<Memorial> findByLastVisitAtBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT m FROM Memorial m WHERE m.lastVisitAt IS NULL AND m.status = :status")
    List<Memorial> findNeverVisitedMemorials(@Param("status") MemorialStatus status);

    @Query("SELECT m FROM Memorial m WHERE m.lastVisitAt < :date AND m.status = :status")
    List<Memorial> findMemorialsNotVisitedSince(@Param("date") LocalDate date, @Param("status") MemorialStatus status);

    // 통계 관련 쿼리
    @Query("SELECT COUNT(m) FROM Memorial m WHERE m.status = :status")
    long countByStatus(@Param("status") MemorialStatus status);

    @Query("SELECT COUNT(m) FROM Memorial m WHERE m.owner = :owner AND m.status = :status")
    long countByOwnerAndStatus(@Param("owner") Member owner, @Param("status") MemorialStatus status);

    @Query("SELECT COUNT(m) FROM Memorial m WHERE m.gender = :gender AND m.status = :status")
    long countByGenderAndStatus(@Param("gender") Gender gender, @Param("status") MemorialStatus status);

    @Query("SELECT COUNT(m) FROM Memorial m WHERE m.relationship = :relationship AND m.status = :status")
    long countByRelationshipAndStatus(@Param("relationship") Relationship relationship, @Param("status") MemorialStatus status);

    @Query("SELECT COUNT(m) FROM Memorial m WHERE m.aiTrainingCompleted = true AND m.status = :status")
    long countByAiTrainingCompletedAndStatus(@Param("status") MemorialStatus status);

    @Query("SELECT COUNT(m) FROM Memorial m WHERE m.isPublic = true AND m.status = :status")
    long countByIsPublicAndStatus(@Param("status") MemorialStatus status);

    @Query("SELECT COUNT(m) FROM Memorial m WHERE m.createdAt >= :startTime AND m.createdAt <= :endTime")
    long countByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(m) FROM Memorial m WHERE m.totalVisits > :visits AND m.status = :status")
    long countByTotalVisitsGreaterThanAndStatus(@Param("visits") int visits, @Param("status") MemorialStatus status);

    // 파일 관련 조회
    @Query("SELECT m FROM Memorial m WHERE m.profileImageUrl IS NOT NULL AND m.status = :status")
    List<Memorial> findMemorialsWithProfileImage(@Param("status") MemorialStatus status);

    @Query("SELECT m FROM Memorial m WHERE m.voiceFileUrl IS NOT NULL AND m.status = :status")
    List<Memorial> findMemorialsWithVoiceFile(@Param("status") MemorialStatus status);

    @Query("SELECT m FROM Memorial m WHERE m.videoFileUrl IS NOT NULL AND m.status = :status")
    List<Memorial> findMemorialsWithVideoFile(@Param("status") MemorialStatus status);

    @Query("SELECT m FROM Memorial m WHERE m.userImageUrl IS NOT NULL AND m.status = :status")
    List<Memorial> findMemorialsWithUserImage(@Param("status") MemorialStatus status);

    @Query("SELECT m FROM Memorial m WHERE m.profileImageUrl IS NOT NULL AND m.voiceFileUrl IS NOT NULL AND m.videoFileUrl IS NOT NULL AND m.userImageUrl IS NOT NULL AND m.status = :status")
    List<Memorial> findMemorialsWithAllFiles(@Param("status") MemorialStatus status);

    // 비디오 콜 준비 상태 조회
    @Query("SELECT m FROM Memorial m WHERE m.profileImageUrl IS NOT NULL AND m.voiceFileUrl IS NOT NULL AND m.videoFileUrl IS NOT NULL AND m.userImageUrl IS NOT NULL AND m.aiTrainingCompleted = true AND m.status = :status")
    List<Memorial> findMemorialsReadyForVideoCall(@Param("status") MemorialStatus status);

    @Query("SELECT m FROM Memorial m WHERE m.profileImageUrl IS NOT NULL AND m.voiceFileUrl IS NOT NULL AND m.videoFileUrl IS NOT NULL AND m.userImageUrl IS NOT NULL AND m.aiTrainingCompleted = true AND m.status = :status")
    Page<Memorial> findMemorialsReadyForVideoCall(@Param("status") MemorialStatus status, Pageable pageable);

    // 복잡한 검색 쿼리
    @Query("SELECT m FROM Memorial m WHERE (m.name LIKE %:keyword% OR m.nickname LIKE %:keyword%) AND m.status = :status")
    List<Memorial> searchByNameOrNickname(@Param("keyword") String keyword, @Param("status") MemorialStatus status);

    @Query("SELECT m FROM Memorial m WHERE (m.name LIKE %:keyword% OR m.nickname LIKE %:keyword%) AND m.status = :status")
    Page<Memorial> searchByNameOrNickname(@Param("keyword") String keyword, @Param("status") MemorialStatus status, Pageable pageable);

    @Query("SELECT m FROM Memorial m WHERE m.owner = :owner AND (m.name LIKE %:keyword% OR m.nickname LIKE %:keyword%) AND m.status = :status")
    List<Memorial> searchByOwnerAndKeyword(@Param("owner") Member owner, @Param("keyword") String keyword, @Param("status") MemorialStatus status);

    // 관심사 관련 조회
    @Query("SELECT m FROM Memorial m WHERE m.interests LIKE %:interest% AND m.status = :status")
    List<Memorial> findByInterestsContaining(@Param("interest") String interest, @Param("status") MemorialStatus status);

    // 정렬된 조회
    @Query("SELECT m FROM Memorial m WHERE m.status = :status ORDER BY m.totalVisits DESC")
    List<Memorial> findByStatusOrderByTotalVisitsDesc(@Param("status") MemorialStatus status, Pageable pageable);

    @Query("SELECT m FROM Memorial m WHERE m.status = :status ORDER BY m.lastVisitAt DESC NULLS LAST")
    List<Memorial> findByStatusOrderByLastVisitAtDesc(@Param("status") MemorialStatus status, Pageable pageable);

    @Query("SELECT m FROM Memorial m WHERE m.status = :status ORDER BY m.createdAt DESC")
    Page<Memorial> findByStatusOrderByCreatedAtDesc(@Param("status") MemorialStatus status, Pageable pageable);

    @Query("SELECT m FROM Memorial m WHERE m.status = :status ORDER BY m.memoryCount DESC")
    List<Memorial> findByStatusOrderByMemoryCountDesc(@Param("status") MemorialStatus status, Pageable pageable);

    // 연관 엔티티와 함께 조회
    @Query("SELECT m FROM Memorial m LEFT JOIN FETCH m.owner WHERE m.id = :id")
    Optional<Memorial> findByIdWithOwner(@Param("id") Long id);

    @Query("SELECT m FROM Memorial m LEFT JOIN FETCH m.familyMembers WHERE m.id = :id")
    Optional<Memorial> findByIdWithFamilyMembers(@Param("id") Long id);

    @Query("SELECT m FROM Memorial m LEFT JOIN FETCH m.videoCalls WHERE m.id = :id")
    Optional<Memorial> findByIdWithVideoCalls(@Param("id") Long id);

    @Query("SELECT m FROM Memorial m LEFT JOIN FETCH m.owner LEFT JOIN FETCH m.familyMembers WHERE m.id = :id")
    Optional<Memorial> findByIdWithOwnerAndFamilyMembers(@Param("id") Long id);

    // 가족 구성원 관련 조회
    @Query("SELECT m FROM Memorial m JOIN m.familyMembers fm WHERE fm.member = :member AND fm.inviteStatus = :inviteStatus AND m.status = :status")
    List<Memorial> findByFamilyMemberAndStatus(@Param("member") Member member, @Param("inviteStatus") InviteStatus inviteStatus, @Param("status") MemorialStatus status);

    @Query("SELECT m FROM Memorial m JOIN m.familyMembers fm WHERE fm.member = :member AND fm.inviteStatus = :inviteStatus AND m.status = :status")
    Page<Memorial> findByFamilyMemberAndStatus(@Param("member") Member member, @Param("inviteStatus") InviteStatus inviteStatus, @Param("status") MemorialStatus status, Pageable pageable);

    @Query("SELECT m FROM Memorial m WHERE m.owner = :owner OR EXISTS (SELECT 1 FROM FamilyMember fm WHERE fm.memorial = m AND fm.member = :owner AND fm.inviteStatus = :inviteStatus)")
    List<Memorial> findAccessibleMemorialsByMember(@Param("owner") Member owner, @Param("inviteStatus") InviteStatus inviteStatus);

    // 일괄 업데이트 쿼리
    @Query("UPDATE Memorial m SET m.status = :newStatus WHERE m.status = :oldStatus")
    int updateStatusBatch(@Param("oldStatus") MemorialStatus oldStatus, @Param("newStatus") MemorialStatus newStatus);

    @Query("UPDATE Memorial m SET m.aiTrainingStatus = :status WHERE m.aiTrainingStatus = :oldStatus")
    int updateAiTrainingStatusBatch(@Param("oldStatus") AiTrainingStatus oldStatus, @Param("status") AiTrainingStatus status);

    @Query("UPDATE Memorial m SET m.totalVisits = m.totalVisits + 1, m.lastVisitAt = :visitDate WHERE m.id = :id")
    int updateVisitInfo(@Param("id") Long id, @Param("visitDate") LocalDate visitDate);

    @Query("UPDATE Memorial m SET m.memoryCount = m.memoryCount + 1 WHERE m.id = :id")
    int incrementMemoryCount(@Param("id") Long id);

    @Query("UPDATE Memorial m SET m.memoryCount = m.memoryCount - 1 WHERE m.id = :id AND m.memoryCount > 0")
    int decrementMemoryCount(@Param("id") Long id);

    // 관리자용 통계 쿼리
    @Query("SELECT m.gender, COUNT(m) FROM Memorial m WHERE m.status = :status GROUP BY m.gender")
    List<Object[]> countByGenderGrouped(@Param("status") MemorialStatus status);

    @Query("SELECT m.relationship, COUNT(m) FROM Memorial m WHERE m.status = :status GROUP BY m.relationship")
    List<Object[]> countByRelationshipGrouped(@Param("status") MemorialStatus status);

    @Query("SELECT m.aiTrainingStatus, COUNT(m) FROM Memorial m WHERE m.status = :status GROUP BY m.aiTrainingStatus")
    List<Object[]> countByAiTrainingStatusGrouped(@Param("status") MemorialStatus status);

    @Query("SELECT DATE(m.createdAt), COUNT(m) FROM Memorial m WHERE m.createdAt >= :startTime AND m.createdAt <= :endTime GROUP BY DATE(m.createdAt) ORDER BY DATE(m.createdAt)")
    List<Object[]> countByCreatedAtGroupedByDate(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // 고급 검색 쿼리
    @Query("SELECT m FROM Memorial m WHERE " +
           "(:name IS NULL OR m.name LIKE %:name%) AND " +
           "(:gender IS NULL OR m.gender = :gender) AND " +
           "(:relationship IS NULL OR m.relationship = :relationship) AND " +
           "(:aiTrainingCompleted IS NULL OR m.aiTrainingCompleted = :aiTrainingCompleted) AND " +
           "(:isPublic IS NULL OR m.isPublic = :isPublic) AND " +
           "m.status = :status")
    Page<Memorial> findBySearchCriteria(
        @Param("name") String name,
        @Param("gender") Gender gender,
        @Param("relationship") Relationship relationship,
        @Param("aiTrainingCompleted") Boolean aiTrainingCompleted,
        @Param("isPublic") Boolean isPublic,
        @Param("status") MemorialStatus status,
        Pageable pageable
    );

    // 랜덤 추천
    @Query(value = "SELECT * FROM t_memorial WHERE status = :status AND is_public = true ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Memorial> findRandomPublicMemorials(@Param("status") String status, @Param("limit") int limit);
}