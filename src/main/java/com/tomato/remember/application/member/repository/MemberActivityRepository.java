package com.tomato.remember.application.member.repository;

import com.tomato.remember.application.wsvideo.code.DeviceType;
import com.tomato.remember.application.member.code.ActivityCategory;
import com.tomato.remember.application.member.code.ActivityStatus;
import com.tomato.remember.application.member.code.ActivityType;
import com.tomato.remember.application.member.code.ImportanceLevel;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.member.entity.MemberActivity;
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
public interface MemberActivityRepository extends JpaRepository<MemberActivity, Long> {

    // 기본 조회 메서드
    Optional<MemberActivity> findByIdAndStatus(Long id, ActivityStatus status);
    Optional<MemberActivity> findByIdAndStatusNot(Long id, ActivityStatus status);

    // 작성자별 조회
    List<MemberActivity> findByAuthor(Member author);
    List<MemberActivity> findByAuthorAndStatus(Member author, ActivityStatus status);
    List<MemberActivity> findByAuthorAndStatusNot(Member author, ActivityStatus status);
    Page<MemberActivity> findByAuthor(Member author, Pageable pageable);
    Page<MemberActivity> findByAuthorAndStatus(Member author, ActivityStatus status, Pageable pageable);

    // 상태별 조회
    List<MemberActivity> findByStatus(ActivityStatus status);
    List<MemberActivity> findByStatusNot(ActivityStatus status);
    Page<MemberActivity> findByStatus(ActivityStatus status, Pageable pageable);
    Page<MemberActivity> findByStatusNot(ActivityStatus status, Pageable pageable);

    // 활동 유형별 조회
    List<MemberActivity> findByActivityType(ActivityType activityType);
    List<MemberActivity> findByActivityTypeAndStatus(ActivityType activityType, ActivityStatus status);
    Page<MemberActivity> findByActivityTypeAndStatus(ActivityType activityType, ActivityStatus status, Pageable pageable);

    // 활동 카테고리별 조회
    List<MemberActivity> findByActivityCategory(ActivityCategory activityCategory);
    List<MemberActivity> findByActivityCategoryAndStatus(ActivityCategory activityCategory, ActivityStatus status);
    Page<MemberActivity> findByActivityCategoryAndStatus(ActivityCategory activityCategory, ActivityStatus status, Pageable pageable);

    // 중요도별 조회
    List<MemberActivity> findByImportanceLevel(ImportanceLevel importanceLevel);
    List<MemberActivity> findByImportanceLevelAndStatus(ImportanceLevel importanceLevel, ActivityStatus status);
    Page<MemberActivity> findByImportanceLevelAndStatus(ImportanceLevel importanceLevel, ActivityStatus status, Pageable pageable);

    @Query("SELECT a FROM MemberActivity a WHERE a.importanceLevel IN :levels AND a.status = :status")
    List<MemberActivity> findByImportanceLevelsAndStatus(@Param("levels") List<ImportanceLevel> levels, @Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.importanceLevel IN :levels AND a.status = :status")
    Page<MemberActivity> findByImportanceLevelsAndStatus(@Param("levels") List<ImportanceLevel> levels, @Param("status") ActivityStatus status, Pageable pageable);

    // 공개 여부별 조회
    List<MemberActivity> findByIsPublic(Boolean isPublic);
    List<MemberActivity> findByIsPublicAndStatus(Boolean isPublic, ActivityStatus status);
    Page<MemberActivity> findByIsPublicAndStatus(Boolean isPublic, ActivityStatus status, Pageable pageable);

    // 메모리얼 관련 조회
    List<MemberActivity> findByMemorialId(Long memorialId);
    List<MemberActivity> findByMemorialIdAndStatus(Long memorialId, ActivityStatus status);
    Page<MemberActivity> findByMemorialIdAndStatus(Long memorialId, ActivityStatus status, Pageable pageable);

    @Query("SELECT a FROM MemberActivity a WHERE a.memorialId IS NOT NULL AND a.status = :status")
    List<MemberActivity> findMemorialActivities(@Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.memorialId IS NOT NULL AND a.status = :status")
    Page<MemberActivity> findMemorialActivities(@Param("status") ActivityStatus status, Pageable pageable);

    // 영상통화 관련 조회
    List<MemberActivity> findByVideoCallId(Long videoCallId);
    List<MemberActivity> findByVideoCallIdAndStatus(Long videoCallId, ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.videoCallId IS NOT NULL AND a.status = :status")
    List<MemberActivity> findVideoCallActivities(@Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.videoCallId IS NOT NULL AND a.status = :status")
    Page<MemberActivity> findVideoCallActivities(@Param("status") ActivityStatus status, Pageable pageable);

    // 가족 관련 조회
    List<MemberActivity> findByFamilyMemberId(Long familyMemberId);
    List<MemberActivity> findByFamilyMemberIdAndStatus(Long familyMemberId, ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.familyMemberId IS NOT NULL AND a.status = :status")
    List<MemberActivity> findFamilyActivities(@Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.familyMemberId IS NOT NULL AND a.status = :status")
    Page<MemberActivity> findFamilyActivities(@Param("status") ActivityStatus status, Pageable pageable);

    // 디바이스 유형별 조회
    List<MemberActivity> findByDeviceType(DeviceType deviceType);
    List<MemberActivity> findByDeviceTypeAndStatus(DeviceType deviceType, ActivityStatus status);

    // 시간별 조회
    List<MemberActivity> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<MemberActivity> findByCreatedAtAfter(LocalDateTime dateTime);
    List<MemberActivity> findByCreatedAtBefore(LocalDateTime dateTime);

    @Query("SELECT a FROM MemberActivity a WHERE a.createdAt >= :startTime AND a.createdAt <= :endTime AND a.status = :status")
    List<MemberActivity> findByCreatedAtBetweenAndStatus(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.createdAt >= :startTime AND a.createdAt <= :endTime AND a.status = :status")
    Page<MemberActivity> findByCreatedAtBetweenAndStatus(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("status") ActivityStatus status, Pageable pageable);

    // 검색 기능
    @Query("SELECT a FROM MemberActivity a WHERE a.title LIKE %:keyword% AND a.status = :status")
    List<MemberActivity> findByTitleContainingAndStatus(@Param("keyword") String keyword, @Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.content LIKE %:keyword% AND a.status = :status")
    List<MemberActivity> findByContentContainingAndStatus(@Param("keyword") String keyword, @Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE (a.title LIKE %:keyword% OR a.content LIKE %:keyword%) AND a.status = :status")
    Page<MemberActivity> searchByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") ActivityStatus status, Pageable pageable);

    @Query("SELECT a FROM MemberActivity a WHERE a.author = :author AND (a.title LIKE %:keyword% OR a.content LIKE %:keyword%) AND a.status = :status")
    Page<MemberActivity> searchByAuthorAndKeywordAndStatus(@Param("author") Member author, @Param("keyword") String keyword, @Param("status") ActivityStatus status, Pageable pageable);

    // 복합 조건 조회
    @Query("SELECT a FROM MemberActivity a WHERE a.author = :author AND a.activityType = :activityType AND a.status = :status")
    List<MemberActivity> findByAuthorAndActivityTypeAndStatus(@Param("author") Member author, @Param("activityType") ActivityType activityType, @Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.author = :author AND a.activityCategory = :activityCategory AND a.status = :status")
    List<MemberActivity> findByAuthorAndActivityCategoryAndStatus(@Param("author") Member author, @Param("activityCategory") ActivityCategory activityCategory, @Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.author = :author AND a.memorialId = :memorialId AND a.status = :status")
    List<MemberActivity> findByAuthorAndMemorialIdAndStatus(@Param("author") Member author, @Param("memorialId") Long memorialId, @Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.author = :author AND a.memorialId = :memorialId AND a.status = :status")
    Page<MemberActivity> findByAuthorAndMemorialIdAndStatus(@Param("author") Member author, @Param("memorialId") Long memorialId, @Param("status") ActivityStatus status, Pageable pageable);

    // 통계 관련 쿼리
    @Query("SELECT COUNT(a) FROM MemberActivity a WHERE a.status = :status")
    long countByStatus(@Param("status") ActivityStatus status);

    @Query("SELECT COUNT(a) FROM MemberActivity a WHERE a.author = :author AND a.status = :status")
    long countByAuthorAndStatus(@Param("author") Member author, @Param("status") ActivityStatus status);

    @Query("SELECT COUNT(a) FROM MemberActivity a WHERE a.activityType = :activityType AND a.status = :status")
    long countByActivityTypeAndStatus(@Param("activityType") ActivityType activityType, @Param("status") ActivityStatus status);

    @Query("SELECT COUNT(a) FROM MemberActivity a WHERE a.activityCategory = :activityCategory AND a.status = :status")
    long countByActivityCategoryAndStatus(@Param("activityCategory") ActivityCategory activityCategory, @Param("status") ActivityStatus status);

    @Query("SELECT COUNT(a) FROM MemberActivity a WHERE a.importanceLevel = :importanceLevel AND a.status = :status")
    long countByImportanceLevelAndStatus(@Param("importanceLevel") ImportanceLevel importanceLevel, @Param("status") ActivityStatus status);

    @Query("SELECT COUNT(a) FROM MemberActivity a WHERE a.memorialId = :memorialId AND a.status = :status")
    long countByMemorialIdAndStatus(@Param("memorialId") Long memorialId, @Param("status") ActivityStatus status);

    @Query("SELECT COUNT(a) FROM MemberActivity a WHERE a.createdAt >= :startTime AND a.createdAt <= :endTime")
    long countByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(a) FROM MemberActivity a WHERE a.isPublic = true AND a.status = :status")
    long countByIsPublicAndStatus(@Param("status") ActivityStatus status);

    @Query("SELECT COUNT(a) FROM MemberActivity a WHERE a.importanceLevel IN :levels AND a.status = :status")
    long countByImportanceLevelsAndStatus(@Param("levels") List<ImportanceLevel> levels, @Param("status") ActivityStatus status);

    @Query("SELECT COUNT(a) FROM MemberActivity a WHERE a.memorialId IS NOT NULL AND a.status = :status")
    long countMemorialActivities(@Param("status") ActivityStatus status);

    @Query("SELECT COUNT(a) FROM MemberActivity a WHERE a.videoCallId IS NOT NULL AND a.status = :status")
    long countVideoCallActivities(@Param("status") ActivityStatus status);

    @Query("SELECT COUNT(a) FROM MemberActivity a WHERE a.familyMemberId IS NOT NULL AND a.status = :status")
    long countFamilyActivities(@Param("status") ActivityStatus status);

    // 정렬된 조회
    @Query("SELECT a FROM MemberActivity a WHERE a.status = :status ORDER BY a.createdAt DESC")
    List<MemberActivity> findByStatusOrderByCreatedAtDesc(@Param("status") ActivityStatus status, Pageable pageable);

    @Query("SELECT a FROM MemberActivity a WHERE a.status = :status ORDER BY a.importanceLevel DESC, a.createdAt DESC")
    List<MemberActivity> findByStatusOrderByImportanceLevelDescAndCreatedAtDesc(@Param("status") ActivityStatus status, Pageable pageable);

    @Query("SELECT a FROM MemberActivity a WHERE a.author = :author ORDER BY a.createdAt DESC")
    List<MemberActivity> findByAuthorOrderByCreatedAtDesc(@Param("author") Member author, Pageable pageable);

    @Query("SELECT a FROM MemberActivity a WHERE a.author = :author AND a.status = :status ORDER BY a.createdAt DESC")
    Page<MemberActivity> findByAuthorAndStatusOrderByCreatedAtDesc(@Param("author") Member author, @Param("status") ActivityStatus status, Pageable pageable);

    @Query("SELECT a FROM MemberActivity a WHERE a.memorialId = :memorialId AND a.status = :status ORDER BY a.createdAt DESC")
    List<MemberActivity> findByMemorialIdAndStatusOrderByCreatedAtDesc(@Param("memorialId") Long memorialId, @Param("status") ActivityStatus status, Pageable pageable);

    @Query("SELECT a FROM MemberActivity a WHERE a.importanceLevel IN :levels AND a.status = :status ORDER BY a.importanceLevel DESC, a.createdAt DESC")
    List<MemberActivity> findByImportanceLevelsAndStatusOrderByImportanceAndCreatedAt(@Param("levels") List<ImportanceLevel> levels, @Param("status") ActivityStatus status, Pageable pageable);

    // 연관 엔티티와 함께 조회
    @Query("SELECT a FROM MemberActivity a LEFT JOIN FETCH a.author WHERE a.id = :id")
    Optional<MemberActivity> findByIdWithAuthor(@Param("id") Long id);

    @Query("SELECT a FROM MemberActivity a LEFT JOIN FETCH a.author WHERE a.status = :status")
    List<MemberActivity> findByStatusWithAuthor(@Param("status") ActivityStatus status);

    // 최근 활동 조회
    @Query("SELECT a FROM MemberActivity a WHERE a.author = :author AND a.status = :status ORDER BY a.createdAt DESC")
    List<MemberActivity> findRecentActivitiesByAuthor(@Param("author") Member author, @Param("status") ActivityStatus status, Pageable pageable);

    @Query("SELECT a FROM MemberActivity a WHERE a.memorialId = :memorialId AND a.status = :status ORDER BY a.createdAt DESC")
    List<MemberActivity> findRecentActivitiesByMemorial(@Param("memorialId") Long memorialId, @Param("status") ActivityStatus status, Pageable pageable);

    @Query("SELECT a FROM MemberActivity a WHERE a.activityCategory = :activityCategory AND a.status = :status ORDER BY a.createdAt DESC")
    List<MemberActivity> findRecentActivitiesByCategory(@Param("activityCategory") ActivityCategory activityCategory, @Param("status") ActivityStatus status, Pageable pageable);

    // 중요한 활동 조회
    @Query("SELECT a FROM MemberActivity a WHERE a.importanceLevel IN ('HIGH', 'CRITICAL', 'URGENT') AND a.status = :status ORDER BY a.importanceLevel DESC, a.createdAt DESC")
    List<MemberActivity> findImportantActivities(@Param("status") ActivityStatus status, Pageable pageable);

    @Query("SELECT a FROM MemberActivity a WHERE a.author = :author AND a.importanceLevel IN ('HIGH', 'CRITICAL', 'URGENT') AND a.status = :status ORDER BY a.importanceLevel DESC, a.createdAt DESC")
    List<MemberActivity> findImportantActivitiesByAuthor(@Param("author") Member author, @Param("status") ActivityStatus status, Pageable pageable);

    // 일괄 업데이트 쿼리
    @Query("UPDATE MemberActivity a SET a.status = :newStatus WHERE a.status = :oldStatus")
    int updateStatusBatch(@Param("oldStatus") ActivityStatus oldStatus, @Param("newStatus") ActivityStatus newStatus);

    @Query("UPDATE MemberActivity a SET a.status = :status WHERE a.createdAt < :dateTime AND a.status = :oldStatus")
    int updateOldActivitiesStatus(@Param("dateTime") LocalDateTime dateTime, @Param("oldStatus") ActivityStatus oldStatus, @Param("status") ActivityStatus status);

    @Query("UPDATE MemberActivity a SET a.importanceLevel = :newLevel WHERE a.importanceLevel = :oldLevel")
    int updateImportanceLevelBatch(@Param("oldLevel") ImportanceLevel oldLevel, @Param("newLevel") ImportanceLevel newLevel);

    // 그룹화된 통계
    @Query("SELECT a.status, COUNT(a) FROM MemberActivity a GROUP BY a.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT a.activityType, COUNT(a) FROM MemberActivity a WHERE a.status = :status GROUP BY a.activityType")
    List<Object[]> countByActivityTypeGrouped(@Param("status") ActivityStatus status);

    @Query("SELECT a.activityCategory, COUNT(a) FROM MemberActivity a WHERE a.status = :status GROUP BY a.activityCategory")
    List<Object[]> countByActivityCategoryGrouped(@Param("status") ActivityStatus status);

    @Query("SELECT a.importanceLevel, COUNT(a) FROM MemberActivity a WHERE a.status = :status GROUP BY a.importanceLevel")
    List<Object[]> countByImportanceLevelGrouped(@Param("status") ActivityStatus status);

    @Query("SELECT a.deviceType, COUNT(a) FROM MemberActivity a WHERE a.status = :status GROUP BY a.deviceType")
    List<Object[]> countByDeviceTypeGrouped(@Param("status") ActivityStatus status);

    @Query("SELECT DATE(a.createdAt), COUNT(a) FROM MemberActivity a WHERE a.createdAt >= :startTime AND a.createdAt <= :endTime GROUP BY DATE(a.createdAt) ORDER BY DATE(a.createdAt)")
    List<Object[]> countByCreatedAtGroupedByDate(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT HOUR(a.createdAt), COUNT(a) FROM MemberActivity a WHERE a.createdAt >= :startTime AND a.createdAt <= :endTime GROUP BY HOUR(a.createdAt) ORDER BY HOUR(a.createdAt)")
    List<Object[]> countByCreatedAtGroupedByHour(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT a.author, COUNT(a) FROM MemberActivity a WHERE a.status = :status GROUP BY a.author ORDER BY COUNT(a) DESC")
    List<Object[]> countByAuthorGrouped(@Param("status") ActivityStatus status, Pageable pageable);

    // 태그 관련 조회
    @Query("SELECT a FROM MemberActivity a WHERE a.tags LIKE %:tag% AND a.status = :status")
    List<MemberActivity> findByTagsContainingAndStatus(@Param("tag") String tag, @Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.tags LIKE %:tag% AND a.status = :status")
    Page<MemberActivity> findByTagsContainingAndStatus(@Param("tag") String tag, @Param("status") ActivityStatus status, Pageable pageable);

    // IP 주소별 조회
    @Query("SELECT a FROM MemberActivity a WHERE a.ipAddress = :ipAddress AND a.status = :status")
    List<MemberActivity> findByIpAddressAndStatus(@Param("ipAddress") String ipAddress, @Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.ipAddress = :ipAddress AND a.createdAt >= :startTime AND a.createdAt <= :endTime")
    List<MemberActivity> findByIpAddressAndCreatedAtBetween(@Param("ipAddress") String ipAddress, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // 사용자 에이전트별 조회
    @Query("SELECT a FROM MemberActivity a WHERE a.userAgent LIKE %:userAgent% AND a.status = :status")
    List<MemberActivity> findByUserAgentContainingAndStatus(@Param("userAgent") String userAgent, @Param("status") ActivityStatus status);

    // 고급 검색 쿼리
    @Query("SELECT a FROM MemberActivity a WHERE " +
           "(:author IS NULL OR a.author = :author) AND " +
           "(:activityType IS NULL OR a.activityType = :activityType) AND " +
           "(:activityCategory IS NULL OR a.activityCategory = :activityCategory) AND " +
           "(:importanceLevel IS NULL OR a.importanceLevel = :importanceLevel) AND " +
           "(:memorialId IS NULL OR a.memorialId = :memorialId) AND " +
           "(:isPublic IS NULL OR a.isPublic = :isPublic) AND " +
           "(:startTime IS NULL OR a.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR a.createdAt <= :endTime) AND " +
           "a.status = :status")
    Page<MemberActivity> findBySearchCriteria(
        @Param("author") Member author,
        @Param("activityType") ActivityType activityType,
        @Param("activityCategory") ActivityCategory activityCategory,
        @Param("importanceLevel") ImportanceLevel importanceLevel,
        @Param("memorialId") Long memorialId,
        @Param("isPublic") Boolean isPublic,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("status") ActivityStatus status,
        Pageable pageable
    );

    // 오늘의 활동 조회
    @Query("SELECT a FROM MemberActivity a WHERE DATE(a.createdAt) = CURRENT_DATE AND a.status = :status")
    List<MemberActivity> findTodayActivities(@Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.author = :author AND DATE(a.createdAt) = CURRENT_DATE AND a.status = :status")
    List<MemberActivity> findTodayActivitiesByAuthor(@Param("author") Member author, @Param("status") ActivityStatus status);

    // 주간/월간 활동 조회
    @Query("SELECT a FROM MemberActivity a WHERE a.createdAt >= :startOfWeek AND a.createdAt <= :endOfWeek AND a.status = :status")
    List<MemberActivity> findWeeklyActivities(@Param("startOfWeek") LocalDateTime startOfWeek, @Param("endOfWeek") LocalDateTime endOfWeek, @Param("status") ActivityStatus status);

    @Query("SELECT a FROM MemberActivity a WHERE a.createdAt >= :startOfMonth AND a.createdAt <= :endOfMonth AND a.status = :status")
    List<MemberActivity> findMonthlyActivities(@Param("startOfMonth") LocalDateTime startOfMonth, @Param("endOfMonth") LocalDateTime endOfMonth, @Param("status") ActivityStatus status);

    // 삭제 관련 쿼리
    @Query("SELECT a FROM MemberActivity a WHERE a.status = 'DELETED'")
    List<MemberActivity> findDeletedActivities();

    @Query("SELECT a FROM MemberActivity a WHERE a.status = 'DELETED' AND a.updatedAt < :dateTime")
    List<MemberActivity> findOldDeletedActivities(@Param("dateTime") LocalDateTime dateTime);

    // 보관된 활동 조회
    @Query("SELECT a FROM MemberActivity a WHERE a.status = 'ARCHIVED'")
    List<MemberActivity> findArchivedActivities();

    @Query("SELECT a FROM MemberActivity a WHERE a.author = :author AND a.status = 'ARCHIVED'")
    List<MemberActivity> findArchivedActivitiesByAuthor(@Param("author") Member author);

    @Query("SELECT a FROM MemberActivity a WHERE a.author = :author AND a.status = 'ARCHIVED'")
    Page<MemberActivity> findArchivedActivitiesByAuthor(@Param("author") Member author, Pageable pageable);

    // 중복 체크용 쿼리
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM MemberActivity a WHERE a.author = :author AND a.title = :title AND a.createdAt >= :startTime AND a.createdAt <= :endTime")
    boolean existsByAuthorAndTitleAndCreatedAtBetween(@Param("author") Member author, @Param("title") String title, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}