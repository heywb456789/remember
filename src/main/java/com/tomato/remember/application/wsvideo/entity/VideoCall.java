package com.tomato.remember.application.wsvideo.entity;

import com.tomato.remember.application.wsvideo.code.DeviceType;
import com.tomato.remember.application.family.code.FeedbackCategory;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.videocall.code.CallStatus;
import com.tomato.remember.application.videocall.code.CallType;
import com.tomato.remember.application.videocall.code.ConversationTopic;
import com.tomato.remember.application.videocall.code.EmotionType;
import com.tomato.remember.common.audit.Audit;
import com.tomato.remember.common.code.QualityLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

@Table(
    name = "t_video_call",
    indexes = {
        @Index(name = "idx01_t_video_call", columnList = "created_at"),
        @Index(name = "idx02_t_video_call", columnList = "memorial_id"),
        @Index(name = "idx03_t_video_call", columnList = "caller_id"),
        @Index(name = "idx04_t_video_call", columnList = "call_started_at"),
        @Index(name = "idx05_t_video_call", columnList = "status"),
        @Index(name = "idx06_t_video_call", columnList = "call_type")
    }
)
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VideoCall extends Audit {

    @Comment("통화 상태")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CallStatus status = CallStatus.WAITING;

    @Comment("통화 유형")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "call_type")
    @Builder.Default
    private CallType callType = CallType.NORMAL;

    @Comment("통화 시작 시간")
    @Column(name = "call_started_at")
    private LocalDateTime callStartedAt;

    @Comment("통화 종료 시간")
    @Column(name = "call_ended_at")
    private LocalDateTime callEndedAt;

    @Comment("통화 시간 (초)")
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Comment("사용자 음성 파일 URL")
    @Column(name = "user_audio_file_url")
    private String userAudioFileUrl;

    @Comment("AI 응답 음성 파일 URL")
    @Column(name = "ai_audio_file_url")
    private String aiAudioFileUrl;

    @Comment("생성된 응답 영상 URL")
    @Column(name = "response_video_url")
    private String responseVideoUrl;

    @Comment("AI 응답 텍스트")
    @Column(columnDefinition = "TEXT", name = "ai_response_text")
    private String aiResponseText;

    @Comment("사용자 음성 텍스트 (STT)")
    @Column(columnDefinition = "TEXT", name = "user_speech_text")
    private String userSpeechText;

    @Comment("대화 주제")
    @Enumerated(EnumType.STRING)
    @Column(length = 30, name = "conversation_topic")
    private ConversationTopic conversationTopic;

    @Comment("감정 분석 결과")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, name = "emotion_analysis")
    private EmotionType emotionAnalysis;

    @Comment("만족도 평가 (1-5)")
    @Column(name = "satisfaction_rating")
    private Integer satisfactionRating;

    @Comment("피드백 내용")
    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Comment("피드백 카테고리")
    @Enumerated(EnumType.STRING)
    @Column(length = 30, name = "feedback_category")
    private FeedbackCategory feedbackCategory;

    @Comment("연결 시도 횟수")
    @Column(nullable = false, name = "connection_attempts")
    @Builder.Default
    private Integer connectionAttempts = 0;

    @Comment("오류 메시지")
    @Column(columnDefinition = "TEXT", name = "error_message")
    private String errorMessage;

    @Comment("사용자 디바이스 정보")
    @Enumerated(EnumType.STRING)
    @Column(length = 20, name = "device_type")
    private DeviceType deviceType;

    @Comment("네트워크 품질 점수")
    @Column(name = "network_quality_score")
    private Integer networkQualityScore;

    @Comment("AI 처리 시간 (밀리초)")
    @Column(name = "ai_processing_time_ms")
    private Long aiProcessingTimeMs;

    @Comment("음성 품질 점수")
    @Column(name = "voice_quality_score")
    private Integer voiceQualityScore;

    @Comment("추가 메타데이터 (JSON)")
    @Column(columnDefinition = "TEXT", name = "metadata")
    private String metadata;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memorial_id", nullable = false)
    private Memorial memorial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caller_id", nullable = false)
    private Member caller;

    // 비즈니스 메서드
    public void setStatus(CallStatus status) {
        this.status = status;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }

    public void setConversationTopic(ConversationTopic conversationTopic) {
        this.conversationTopic = conversationTopic;
    }

    public void setEmotionAnalysis(EmotionType emotionAnalysis) {
        this.emotionAnalysis = emotionAnalysis;
    }

    public void setFeedbackCategory(FeedbackCategory feedbackCategory) {
        this.feedbackCategory = feedbackCategory;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public void setNetworkQualityScore(Integer networkQualityScore) {
        this.networkQualityScore = networkQualityScore;
    }

    public void setVoiceQualityScore(Integer voiceQualityScore) {
        this.voiceQualityScore = voiceQualityScore;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setUserAudioFileUrl(String userAudioFileUrl) {
        this.userAudioFileUrl = userAudioFileUrl;
    }

    public void setAiAudioFileUrl(String aiAudioFileUrl) {
        this.aiAudioFileUrl = aiAudioFileUrl;
    }

    public void setResponseVideoUrl(String responseVideoUrl) {
        this.responseVideoUrl = responseVideoUrl;
    }

    public void setAiResponseText(String aiResponseText) {
        this.aiResponseText = aiResponseText;
    }

    public void setUserSpeechText(String userSpeechText) {
        this.userSpeechText = userSpeechText;
    }

    public void setAiProcessingTimeMs(Long aiProcessingTimeMs) {
        this.aiProcessingTimeMs = aiProcessingTimeMs;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void startCall() {
        this.status = CallStatus.IN_PROGRESS;
        this.callStartedAt = LocalDateTime.now();
    }

    public void endCall() {
        this.status = CallStatus.COMPLETED;
        this.callEndedAt = LocalDateTime.now();
        if (callStartedAt != null) {
            this.durationSeconds = (int) java.time.Duration.between(callStartedAt, callEndedAt).getSeconds();
        }
    }

    public void failCall(String errorMessage) {
        this.status = CallStatus.FAILED;
        this.callEndedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public void cancelCall() {
        this.status = CallStatus.CANCELLED;
        this.callEndedAt = LocalDateTime.now();
    }

    public void addConnectionAttempt() {
        this.connectionAttempts++;
    }

    public void submitFeedback(Integer rating, String feedback, FeedbackCategory category) {
        this.satisfactionRating = rating;
        this.feedback = feedback;
        this.feedbackCategory = category;
    }

    public boolean isWaiting() {
        return status == CallStatus.WAITING;
    }

    public boolean isConnecting() {
        return status == CallStatus.CONNECTING;
    }

    public boolean isInProgress() {
        return status == CallStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return status == CallStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == CallStatus.FAILED;
    }

    public boolean isCancelled() {
        return status == CallStatus.CANCELLED;
    }

    public boolean isNormalCall() {
        return callType == CallType.NORMAL;
    }

    public boolean isEmergencyCall() {
        return callType == CallType.EMERGENCY;
    }

    public boolean isScheduledCall() {
        return callType == CallType.SCHEDULED;
    }

    public String getFormattedDuration() {
        if (durationSeconds == null) return "00:00";

        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public boolean hasFeedback() {
        return satisfactionRating != null && feedback != null;
    }

    public boolean isSuccessful() {
        return isCompleted() && durationSeconds != null && durationSeconds > 0;
    }

    public QualityLevel getNetworkQualityLevel() {
        if (networkQualityScore == null) return QualityLevel.VERY_POOR;
        return QualityLevel.fromScore(networkQualityScore);
    }

    public QualityLevel getVoiceQualityLevel() {
        if (voiceQualityScore == null) return QualityLevel.VERY_POOR;
        return QualityLevel.fromScore(voiceQualityScore);
    }

    public boolean isHighQuality() {
        return getNetworkQualityLevel() == QualityLevel.EXCELLENT ||
               getNetworkQualityLevel() == QualityLevel.GOOD;
    }

    public boolean isLongCall() {
        return durationSeconds != null && durationSeconds > 300; // 5분 이상
    }

    public boolean isShortCall() {
        return durationSeconds != null && durationSeconds < 30; // 30초 미만
    }

    public boolean isPositiveEmotion() {
        return emotionAnalysis != null && (
            emotionAnalysis == EmotionType.HAPPY ||
            emotionAnalysis == EmotionType.EXCITED ||
            emotionAnalysis == EmotionType.CALM ||
            emotionAnalysis == EmotionType.GRATEFUL ||
            emotionAnalysis == EmotionType.HOPEFUL ||
            emotionAnalysis == EmotionType.PEACEFUL ||
            emotionAnalysis == EmotionType.PROUD ||
            emotionAnalysis == EmotionType.RELIEVED
        );
    }

    public boolean isNegativeEmotion() {
        return emotionAnalysis != null && (
            emotionAnalysis == EmotionType.SAD ||
            emotionAnalysis == EmotionType.ANGRY ||
            emotionAnalysis == EmotionType.FEAR ||
            emotionAnalysis == EmotionType.WORRIED ||
            emotionAnalysis == EmotionType.LONELY ||
            emotionAnalysis == EmotionType.DISAPPOINTED
        );
    }

    public boolean isGoodFeedback() {
        return satisfactionRating != null && satisfactionRating >= 4;
    }

    public boolean isBadFeedback() {
        return satisfactionRating != null && satisfactionRating <= 2;
    }

    public String getStatusDisplayName() {
        return status.getDisplayName();
    }

    public String getCallTypeDisplayName() {
        return callType.getDisplayName();
    }

    public String getConversationTopicDisplayName() {
        return conversationTopic != null ? conversationTopic.getDisplayName() : "없음";
    }

    public String getEmotionDisplayName() {
        return emotionAnalysis != null ? emotionAnalysis.getDisplayName() : "분석 안됨";
    }

    public String getFeedbackCategoryDisplayName() {
        return feedbackCategory != null ? feedbackCategory.getDisplayName() : "없음";
    }

    public String getDeviceTypeDisplayName() {
        return deviceType != null ? deviceType.getDisplayName() : "알 수 없음";
    }
}