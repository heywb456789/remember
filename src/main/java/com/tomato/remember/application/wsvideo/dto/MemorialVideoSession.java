package com.tomato.remember.application.wsvideo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tomato.remember.application.wsvideo.code.DeviceType;
import com.tomato.remember.application.wsvideo.code.VideoCallFlowState;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Memorial Video Call 세션 엔티티 Redis에 저장되는 세션 데이터
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemorialVideoSession implements Serializable {

    private static final long serialVersionUID = 1L;

    // TTL 설정 (1시간 = 3600초)
    private static final int TTL_SECONDS = 3600;
    // 하트비트 간격 (30초)
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;

    // 기본 정보
    private String sessionKey;
    private String contactName;
    private Long memorialId;
    private Long callerId;

    // 상태 정보
    private String status = "WAITING"; // WAITING, PROCESSING, COMPLETED, ERROR
    private VideoCallFlowState flowState = VideoCallFlowState.INITIALIZING;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    private LocalDateTime lastStateChange;

    // 연결 정보
    private String socketId;
    private int reconnectCount = 0;
    private DeviceType deviceType = DeviceType.WEB; // 디바이스 타입
    private String deviceId; // 디바이스 고유 ID
    private boolean isPrimaryDevice = true; // 주 제어 디바이스 여부

    // 파일 정보
    private String savedFilePath;
    private String responseVideoUrl;
    private String waitingVideoUrl; // 대기영상 URL

    // === 메타데이터 ===
    private Map<String, Object> metadata = new HashMap<>();

    public static MemorialVideoSession createNew(String sessionKey, String contactName, Long memorialId,
        Long callerId) {
        MemorialVideoSession session = new MemorialVideoSession();
        session.sessionKey = sessionKey;
        session.contactName = contactName;
        session.memorialId = memorialId;
        session.callerId = callerId;
        session.deviceType = DeviceType.WEB; // 기본값
        session.deviceId = java.util.UUID.randomUUID().toString(); // 자동 생성
        session.createdAt = LocalDateTime.now();
        session.lastActivity = LocalDateTime.now();
        session.lastStateChange = LocalDateTime.now();
        session.status = "WAITING";
        session.flowState = VideoCallFlowState.INITIALIZING;
        session.reconnectCount = 0;
        session.isPrimaryDevice = true;

        return session;
    }

    /**
     * 새 세션 생성 (확장버전)
     */
    public static MemorialVideoSession createNew(String sessionKey, String contactName,
        Long memorialId, Long callerId,
        DeviceType deviceType, String deviceId) {
        MemorialVideoSession session = new MemorialVideoSession();
        session.sessionKey = sessionKey;
        session.contactName = contactName;
        session.memorialId = memorialId;
        session.callerId = callerId;
        session.deviceType = deviceType;
        session.deviceId = deviceId;
        session.createdAt = LocalDateTime.now();
        session.lastActivity = LocalDateTime.now();
        session.lastStateChange = LocalDateTime.now();
        session.status = "WAITING";
        session.flowState = VideoCallFlowState.INITIALIZING;
        session.reconnectCount = 0;
        session.isPrimaryDevice = true;

        return session;
    }

    /**
     * 상태 전환 (새로운 메서드)
     */
    public boolean transitionToState(VideoCallFlowState newState) {
        if (! this.flowState.canTransitionTo(newState)) {
            return false;
        }

        this.flowState = newState;
        this.lastStateChange = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();

        // 기존 status도 업데이트 (하위 호환성)
        this.status = mapFlowStateToStatus(newState);

        return true;
    }

    /**
     * FlowState를 기존 status로 매핑 (하위 호환성)
     */
    private String mapFlowStateToStatus(VideoCallFlowState flowState) {
        return switch (flowState) {
            case INITIALIZING, PERMISSION_REQUESTING, PERMISSION_GRANTED,
                 WAITING_READY, WAITING_PLAYING -> "WAITING";
            case RECORDING_COUNTDOWN, RECORDING_ACTIVE, RECORDING_COMPLETE,
                 PROCESSING_UPLOAD, PROCESSING_AI -> "PROCESSING";
            case PROCESSING_COMPLETE, RESPONSE_READY, RESPONSE_PLAYING,
                 RESPONSE_COMPLETE -> "COMPLETED";
            case CALL_ENDING, CALL_COMPLETED -> "COMPLETED";
            case ERROR_NETWORK, ERROR_PERMISSION, ERROR_PROCESSING, ERROR_TIMEOUT -> "ERROR";
        };
    }

    /**
     * WebSocket 연결 설정
     */
    public void setWebSocketConnection(String socketId) {
        this.socketId = socketId;
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * WebSocket 연결 해제
     */
    public void clearWebSocketConnection() {
        this.socketId = null;
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * 재연결 처리
     */
    public void handleReconnect(String newSocketId) {
        this.socketId = newSocketId;
        this.reconnectCount++;
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * 처리 중 상태로 변경 (기존 호환성)
     */
    public void setProcessing(String filePath) {
        this.status = "PROCESSING";
        this.savedFilePath = filePath;
        this.lastActivity = LocalDateTime.now();
        // 새로운 flowState도 업데이트
        this.flowState = VideoCallFlowState.PROCESSING_UPLOAD;
        this.lastStateChange = LocalDateTime.now();
    }

    /**
     * 완료 상태로 변경 (기존 호환성)
     */
    public void setCompleted(String responseVideoUrl) {
        this.status = "COMPLETED";
        this.responseVideoUrl = responseVideoUrl;
        this.lastActivity = LocalDateTime.now();
        // 새로운 flowState도 업데이트
        this.flowState = VideoCallFlowState.RESPONSE_READY;
        this.lastStateChange = LocalDateTime.now();
    }

    /**
     * 오류 상태로 변경 (기존 호환성)
     */
    public void setError() {
        this.status = "ERROR";
        this.lastActivity = LocalDateTime.now();
        // 새로운 flowState도 업데이트
        this.flowState = VideoCallFlowState.ERROR_PROCESSING;
        this.lastStateChange = LocalDateTime.now();
    }

    /**
     * 활동 시간 업데이트
     */
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * 연결 여부 확인
     */
    public boolean isConnected() {
        return this.socketId != null && ! this.flowState.isErrorState();
    }

    /**
     * 디바이스 정보 설정
     */
    public void setDeviceInfo(DeviceType deviceType, String deviceId, boolean isPrimary) {
        this.deviceType = deviceType;
        this.deviceId = deviceId;
        this.isPrimaryDevice = isPrimary;
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * 메타데이터 추가
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    /**
     * 메타데이터 조회
     */
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }

    /**
     * 세션 나이 (분)
     */
    @JsonIgnore
    public long getAgeInMinutes() {
        return ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now());
    }

    /**
     * 마지막 상태 변경 이후 경과 시간 (분)
     */
    @JsonIgnore
    public long getMinutesSinceStateChange() {
        return ChronoUnit.MINUTES.between(lastStateChange, LocalDateTime.now());
    }

    /**
     * 세션 만료 여부
     */
    @JsonIgnore
    public boolean isExpired() {
        return getAgeInMinutes() > (TTL_SECONDS / 60);
    }

    /**
     * 남은 TTL (초)
     */
    @JsonIgnore
    public long getRemainingTtlSeconds() {
        long ageSeconds = ChronoUnit.SECONDS.between(lastActivity, LocalDateTime.now());
        return Math.max(0, TTL_SECONDS - ageSeconds);
    }

    /**
     * TTL 설정값 조회 (정적)
     */
    public static int getTtlSeconds() {
        return TTL_SECONDS;
    }

    /**
     * 하트비트 간격 조회 (정적)
     */
    public static int getHeartbeatIntervalSeconds() {
        return HEARTBEAT_INTERVAL_SECONDS;
    }

    /**
     * 현재 상태 표시명
     */
    public String getCurrentStateDisplayName() {
        return flowState.getDisplayName();
    }

    /**
     * 현재 상태 설명
     */
    public String getCurrentStateDescription() {
        return flowState.getDescription();
    }

    @Override
    public String toString() {
        return String.format(
            "MemorialVideoSession{sessionKey='%s', contactName='%s', flowState='%s', " +
                "deviceType='%s', age=%dmin, connected=%s, primary=%s}",
            sessionKey, contactName, flowState.name(),
            deviceType.name(), getAgeInMinutes(), isConnected(), isPrimaryDevice
        );
    }
}