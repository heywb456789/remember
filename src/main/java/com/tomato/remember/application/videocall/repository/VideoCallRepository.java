package com.tomato.remember.application.videocall.repository;

import com.tomato.remember.application.family.code.DeviceType;
import com.tomato.remember.application.family.code.FeedbackCategory;
import com.tomato.remember.application.videocall.code.CallStatus;
import com.tomato.remember.application.videocall.code.CallType;
import com.tomato.remember.application.videocall.code.ConversationTopic;
import com.tomato.remember.application.videocall.code.EmotionType;
import com.tomato.remember.application.videocall.entity.VideoCall;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.member.entity.Member;
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
public interface VideoCallRepository extends JpaRepository<VideoCall, Long> {

    // 기본 조회 메서드
    Optional<VideoCall> findByIdAndStatus(Long id, CallStatus status);
    Optional<VideoCall> findByIdAndStatusNot(Long id, CallStatus status);

    // 상태별 조회
    List<VideoCall> findByStatus(CallStatus status);
    List<VideoCall> findByStatusNot(CallStatus status);
    Page<VideoCall> findByStatus(CallStatus status, Pageable pageable);
    Page<VideoCall> findByStatusNot(CallStatus status, Pageable pageable);

    // 호출자별 조회
    List<VideoCall> findByCaller(Member caller);
    List<VideoCall> findByCallerAndStatus(Member caller, CallStatus status);
    List<VideoCall> findByCallerAndStatusNot(Member caller, CallStatus status);
    Page<VideoCall> findByCaller(Member caller, Pageable pageable);
    Page<VideoCall> findByCallerAndStatus(Member caller, CallStatus status, Pageable pageable);

    // 메모리얼별 조회
    List<VideoCall> findByMemorial(Memorial memorial);
    List<VideoCall> findByMemorialAndStatus(Memorial memorial, CallStatus status);
    List<VideoCall> findByMemorialAndStatusNot(Memorial memorial, CallStatus status);
    Page<VideoCall> findByMemorial(Memorial memorial, Pageable pageable);
    Page<VideoCall> findByMemorialAndStatus(Memorial memorial, CallStatus status, Pageable pageable);

    // 통화 유형별 조회
    List<VideoCall> findByCallType(CallType callType);
    List<VideoCall> findByCallTypeAndStatus(CallType callType, CallStatus status);
    Page<VideoCall> findByCallTypeAndStatus(CallType callType, CallStatus status, Pageable pageable);

    // 감정 분석별 조회
    List<VideoCall> findByEmotionAnalysis(EmotionType emotionType);
    List<VideoCall> findByEmotionAnalysisAndStatus(EmotionType emotionType, CallStatus status);
    Page<VideoCall> findByEmotionAnalysisAndStatus(EmotionType emotionType, CallStatus status, Pageable pageable);

    // 대화 주제별 조회
    List<VideoCall> findByConversationTopic(ConversationTopic conversationTopic);
    List<VideoCall> findByConversationTopicAndStatus(ConversationTopic conversationTopic, CallStatus status);

    // 피드백 카테고리별 조회
    List<VideoCall> findByFeedbackCategory(FeedbackCategory feedbackCategory);
    List<VideoCall> findByFeedbackCategoryAndStatus(FeedbackCategory feedbackCategory, CallStatus status);

    // 오류 유형별 조회

    // 디바이스 유형별 조회
    List<VideoCall> findByDeviceType(DeviceType deviceType);
    List<VideoCall> findByDeviceTypeAndStatus(DeviceType deviceType, CallStatus status);

    // 시간별 조회
    List<VideoCall> findByCallStartedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<VideoCall> findByCallEndedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    List<VideoCall> findByCallStartedAtAfter(LocalDateTime dateTime);
    List<VideoCall> findByCallEndedAtBefore(LocalDateTime dateTime);

    @Query("SELECT v FROM VideoCall v WHERE v.callStartedAt >= :startTime AND v.callStartedAt <= :endTime AND v.status = :status")
    List<VideoCall> findByCallStartedAtBetweenAndStatus(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("status") CallStatus status);

    @Query("SELECT v FROM VideoCall v WHERE v.callEndedAt >= :startTime AND v.callEndedAt <= :endTime AND v.status = :status")
    List<VideoCall> findByCallEndedAtBetweenAndStatus(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("status") CallStatus status);

    // 통화 시간별 조회
    List<VideoCall> findByDurationSecondsGreaterThan(Integer duration);
    List<VideoCall> findByDurationSecondsLessThan(Integer duration);
    List<VideoCall> findByDurationSecondsBetween(Integer minDuration, Integer maxDuration);

    @Query("SELECT v FROM VideoCall v WHERE v.durationSeconds > :duration AND v.status = :status")
    List<VideoCall> findLongCalls(@Param("duration") Integer duration, @Param("status") CallStatus status);

    @Query("SELECT v FROM VideoCall v WHERE v.durationSeconds < :duration AND v.status = :status")
    List<VideoCall> findShortCalls(@Param("duration") Integer duration, @Param("status") CallStatus status);

    @Query("SELECT v FROM VideoCall v WHERE v.durationSeconds >= :minDuration AND v.durationSeconds <= :maxDuration AND v.status = :status")
    List<VideoCall> findCallsByDurationRange(@Param("minDuration") Integer minDuration, @Param("maxDuration") Integer maxDuration, @Param("status") CallStatus status);

    // 품질 점수별 조회
    List<VideoCall> findByNetworkQualityScoreGreaterThan(Integer score);
    List<VideoCall> findByVoiceQualityScoreGreaterThan(Integer score);
    List<VideoCall> findByNetworkQualityScoreLessThan(Integer score);
    List<VideoCall> findByVoiceQualityScoreLessThan(Integer score);

    @Query("SELECT v FROM VideoCall v WHERE v.networkQualityScore >= :score AND v.voiceQualityScore >= :score AND v.status = :status")
    List<VideoCall> findHighQualityCalls(@Param("score") Integer score, @Param("status") CallStatus status);

    @Query("SELECT v FROM VideoCall v WHERE (v.networkQualityScore < :score OR v.voiceQualityScore < :score) AND v.status = :status")
    List<VideoCall> findLowQualityCalls(@Param("score") Integer score, @Param("status") CallStatus status);

    // 만족도별 조회
    List<VideoCall> findBySatisfactionRatingGreaterThan(Integer rating);
    List<VideoCall> findBySatisfactionRatingLessThan(Integer rating);
    List<VideoCall> findBySatisfactionRating(Integer rating);

    @Query("SELECT v FROM VideoCall v WHERE v.satisfactionRating >= :rating AND v.status = :status")
    List<VideoCall> findHighSatisfactionCalls(@Param("rating") Integer rating, @Param("status") CallStatus status);

    @Query("SELECT v FROM VideoCall v WHERE v.satisfactionRating <= :rating AND v.status = :status")
    List<VideoCall> findLowSatisfactionCalls(@Param("rating") Integer rating, @Param("status") CallStatus status);

    // 피드백 관련 조회
    @Query("SELECT v FROM VideoCall v WHERE v.feedback IS NOT NULL AND v.satisfactionRating IS NOT NULL AND v.status = :status")
    List<VideoCall> findCallsWithFeedback(@Param("status") CallStatus status);

    @Query("SELECT v FROM VideoCall v WHERE v.feedback IS NULL AND v.status = :status")
    List<VideoCall> findCallsWithoutFeedback(@Param("status") CallStatus status);

    // AI 처리 시간별 조회
    List<VideoCall> findByAiProcessingTimeMsGreaterThan(Long processingTime);
    List<VideoCall> findByAiProcessingTimeMsLessThan(Long processingTime);

    @Query("SELECT v FROM VideoCall v WHERE v.aiProcessingTimeMs > :processingTime AND v.status = :status")
    List<VideoCall> findSlowAiProcessingCalls(@Param("processingTime") Long processingTime, @Param("status") CallStatus status);

    @Query("SELECT v FROM VideoCall v WHERE v.aiProcessingTimeMs <= :processingTime AND v.status = :status")
    List<VideoCall> findFastAiProcessingCalls(@Param("processingTime") Long processingTime, @Param("status") CallStatus status);

    // 연결 시도별 조회
    List<VideoCall> findByConnectionAttemptsGreaterThan(Integer attempts);
    List<VideoCall> findByConnectionAttemptsLessThan(Integer attempts);

    @Query("SELECT v FROM VideoCall v WHERE v.connectionAttempts > :attempts AND v.status = :status")
    List<VideoCall> findMultipleAttemptCalls(@Param("attempts") Integer attempts, @Param("status") CallStatus status);

    // 복합 조건 조회
    @Query("SELECT v FROM VideoCall v WHERE v.caller = :caller AND v.memorial = :memorial AND v.status = :status")
    List<VideoCall> findByCallerAndMemorialAndStatus(@Param("caller") Member caller, @Param("memorial") Memorial memorial, @Param("status") CallStatus status);

    @Query("SELECT v FROM VideoCall v WHERE v.caller = :caller AND v.memorial = :memorial AND v.status = :status")
    Page<VideoCall> findByCallerAndMemorialAndStatus(@Param("caller") Member caller, @Param("memorial") Memorial memorial, @Param("status") CallStatus status, Pageable pageable);

    // 통계 관련 쿼리
    @Query("SELECT COUNT(v) FROM VideoCall v WHERE v.status = :status")
    long countByStatus(@Param("status") CallStatus status);

    @Query("SELECT COUNT(v) FROM VideoCall v WHERE v.caller = :caller AND v.status = :status")
    long countByCallerAndStatus(@Param("caller") Member caller, @Param("status") CallStatus status);

    @Query("SELECT COUNT(v) FROM VideoCall v WHERE v.memorial = :memorial AND v.status = :status")
    long countByMemorialAndStatus(@Param("memorial") Memorial memorial, @Param("status") CallStatus status);

    @Query("SELECT COUNT(v) FROM VideoCall v WHERE v.callType = :callType AND v.status = :status")
    long countByCallTypeAndStatus(@Param("callType") CallType callType, @Param("status") CallStatus status);

    @Query("SELECT COUNT(v) FROM VideoCall v WHERE v.emotionAnalysis = :emotionType AND v.status = :status")
    long countByEmotionAnalysisAndStatus(@Param("emotionType") EmotionType emotionType, @Param("status") CallStatus status);

    @Query("SELECT COUNT(v) FROM VideoCall v WHERE v.callStartedAt >= :startTime AND v.callStartedAt <= :endTime")
    long countByCallStartedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(v) FROM VideoCall v WHERE v.durationSeconds > :duration AND v.status = :status")
    long countLongCalls(@Param("duration") Integer duration, @Param("status") CallStatus status);

    @Query("SELECT COUNT(v) FROM VideoCall v WHERE v.satisfactionRating >= :rating AND v.status = :status")
    long countHighSatisfactionCalls(@Param("rating") Integer rating, @Param("status") CallStatus status);

    @Query("SELECT COUNT(v) FROM VideoCall v WHERE v.networkQualityScore >= :score AND v.voiceQualityScore >= :score AND v.status = :status")
    long countHighQualityCalls(@Param("score") Integer score, @Param("status") CallStatus status);

    // 평균 통계
    @Query("SELECT AVG(v.durationSeconds) FROM VideoCall v WHERE v.status = :status AND v.durationSeconds IS NOT NULL")
    Double getAverageDurationByStatus(@Param("status") CallStatus status);

    @Query("SELECT AVG(v.satisfactionRating) FROM VideoCall v WHERE v.status = :status AND v.satisfactionRating IS NOT NULL")
    Double getAverageSatisfactionRatingByStatus(@Param("status") CallStatus status);

    @Query("SELECT AVG(v.networkQualityScore) FROM VideoCall v WHERE v.status = :status AND v.networkQualityScore IS NOT NULL")
    Double getAverageNetworkQualityByStatus(@Param("status") CallStatus status);

    @Query("SELECT AVG(v.voiceQualityScore) FROM VideoCall v WHERE v.status = :status AND v.voiceQualityScore IS NOT NULL")
    Double getAverageVoiceQualityByStatus(@Param("status") CallStatus status);

    @Query("SELECT AVG(v.aiProcessingTimeMs) FROM VideoCall v WHERE v.status = :status AND v.aiProcessingTimeMs IS NOT NULL")
    Double getAverageAiProcessingTimeByStatus(@Param("status") CallStatus status);

    // 정렬된 조회
    @Query("SELECT v FROM VideoCall v WHERE v.status = :status ORDER BY v.callStartedAt DESC")
    List<VideoCall> findByStatusOrderByCallStartedAtDesc(@Param("status") CallStatus status, Pageable pageable);

    @Query("SELECT v FROM VideoCall v WHERE v.status = :status ORDER BY v.durationSeconds DESC")
    List<VideoCall> findByStatusOrderByDurationDesc(@Param("status") CallStatus status, Pageable pageable);

    @Query("SELECT v FROM VideoCall v WHERE v.status = :status ORDER BY v.satisfactionRating DESC NULLS LAST")
    List<VideoCall> findByStatusOrderBySatisfactionRatingDesc(@Param("status") CallStatus status, Pageable pageable);

    @Query("SELECT v FROM VideoCall v WHERE v.caller = :caller ORDER BY v.callStartedAt DESC")
    List<VideoCall> findByCallerOrderByCallStartedAtDesc(@Param("caller") Member caller, Pageable pageable);

    @Query("SELECT v FROM VideoCall v WHERE v.memorial = :memorial ORDER BY v.callStartedAt DESC")
    List<VideoCall> findByMemorialOrderByCallStartedAtDesc(@Param("memorial") Memorial memorial, Pageable pageable);

    // 연관 엔티티와 함께 조회
    @Query("SELECT v FROM VideoCall v LEFT JOIN FETCH v.caller WHERE v.id = :id")
    Optional<VideoCall> findByIdWithCaller(@Param("id") Long id);

    @Query("SELECT v FROM VideoCall v LEFT JOIN FETCH v.memorial WHERE v.id = :id")
    Optional<VideoCall> findByIdWithMemorial(@Param("id") Long id);

    @Query("SELECT v FROM VideoCall v LEFT JOIN FETCH v.caller LEFT JOIN FETCH v.memorial WHERE v.id = :id")
    Optional<VideoCall> findByIdWithCallerAndMemorial(@Param("id") Long id);

    // 진행 중인 통화 조회
    @Query("SELECT v FROM VideoCall v WHERE v.status IN :statuses")
    List<VideoCall> findActiveCallsByStatuses(@Param("statuses") List<CallStatus> statuses);

    @Query("SELECT v FROM VideoCall v WHERE v.status = :status AND v.callStartedAt IS NOT NULL AND v.callEndedAt IS NULL")
    List<VideoCall> findOngoingCalls(@Param("status") CallStatus status);

    // 일괄 업데이트 쿼리
    @Query("UPDATE VideoCall v SET v.status = :newStatus WHERE v.status = :oldStatus")
    int updateStatusBatch(@Param("oldStatus") CallStatus oldStatus, @Param("newStatus") CallStatus newStatus);

    @Query("UPDATE VideoCall v SET v.status = :status WHERE v.callStartedAt < :timeout AND v.status IN :inProgressStatuses")
    int updateTimeoutCalls(@Param("timeout") LocalDateTime timeout, @Param("inProgressStatuses") List<CallStatus> inProgressStatuses, @Param("status") CallStatus status);

    // 그룹화된 통계
    @Query("SELECT v.status, COUNT(v) FROM VideoCall v GROUP BY v.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT v.callType, COUNT(v) FROM VideoCall v WHERE v.status = :status GROUP BY v.callType")
    List<Object[]> countByCallTypeGrouped(@Param("status") CallStatus status);

    @Query("SELECT v.emotionAnalysis, COUNT(v) FROM VideoCall v WHERE v.status = :status GROUP BY v.emotionAnalysis")
    List<Object[]> countByEmotionAnalysisGrouped(@Param("status") CallStatus status);

    @Query("SELECT v.deviceType, COUNT(v) FROM VideoCall v WHERE v.status = :status GROUP BY v.deviceType")
    List<Object[]> countByDeviceTypeGrouped(@Param("status") CallStatus status);

    @Query("SELECT DATE(v.callStartedAt), COUNT(v) FROM VideoCall v WHERE v.callStartedAt >= :startTime AND v.callStartedAt <= :endTime GROUP BY DATE(v.callStartedAt) ORDER BY DATE(v.callStartedAt)")
    List<Object[]> countByCallStartedAtGroupedByDate(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT HOUR(v.callStartedAt), COUNT(v) FROM VideoCall v WHERE v.callStartedAt >= :startTime AND v.callStartedAt <= :endTime GROUP BY HOUR(v.callStartedAt) ORDER BY HOUR(v.callStartedAt)")
    List<Object[]> countByCallStartedAtGroupedByHour(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // 최근 통화 조회
    @Query("SELECT v FROM VideoCall v WHERE v.caller = :caller AND v.status = :status ORDER BY v.callStartedAt DESC")
    List<VideoCall> findRecentCallsByCaller(@Param("caller") Member caller, @Param("status") CallStatus status, Pageable pageable);

    @Query("SELECT v FROM VideoCall v WHERE v.memorial = :memorial AND v.status = :status ORDER BY v.callStartedAt DESC")
    List<VideoCall> findRecentCallsByMemorial(@Param("memorial") Memorial memorial, @Param("status") CallStatus status, Pageable pageable);

    // 고급 검색 쿼리
    @Query("SELECT v FROM VideoCall v WHERE " +
           "(:caller IS NULL OR v.caller = :caller) AND " +
           "(:memorial IS NULL OR v.memorial = :memorial) AND " +
           "(:status IS NULL OR v.status = :status) AND " +
           "(:callType IS NULL OR v.callType = :callType) AND " +
           "(:emotionType IS NULL OR v.emotionAnalysis = :emotionType) AND " +
           "(:startTime IS NULL OR v.callStartedAt >= :startTime) AND " +
           "(:endTime IS NULL OR v.callStartedAt <= :endTime)")
    Page<VideoCall> findBySearchCriteria(
        @Param("caller") Member caller,
        @Param("memorial") Memorial memorial,
        @Param("status") CallStatus status,
        @Param("callType") CallType callType,
        @Param("emotionType") EmotionType emotionType,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );
}