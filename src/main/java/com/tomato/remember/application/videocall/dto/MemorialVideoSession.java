package com.tomato.remember.application.videocall.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Memorial Video Call 세션 엔티티
 * Redis에 저장되는 세션 데이터
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
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;

    // 연결 정보
    private String socketId;
    private int reconnectCount = 0;

    // 파일 정보
    private String savedFilePath;
    private String responseVideoUrl;

    /**
     * 새 세션 생성
     */
    public static MemorialVideoSession createNew(String sessionKey, String contactName, Long memorialId, Long callerId) {
        MemorialVideoSession session = new MemorialVideoSession();
        session.sessionKey = sessionKey;
        session.contactName = contactName;
        session.memorialId = memorialId;
        session.callerId = callerId;
        session.createdAt = LocalDateTime.now();
        session.lastActivity = LocalDateTime.now();
        session.status = "WAITING";
        session.reconnectCount = 0;

        return session;
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
     * 처리 중 상태로 변경
     */
    public void setProcessing(String filePath) {
        this.status = "PROCESSING";
        this.savedFilePath = filePath;
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * 완료 상태로 변경
     */
    public void setCompleted(String responseVideoUrl) {
        this.status = "COMPLETED";
        this.responseVideoUrl = responseVideoUrl;
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * 오류 상태로 변경
     */
    public void setError() {
        this.status = "ERROR";
        this.lastActivity = LocalDateTime.now();
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
        return this.socketId != null && !"ERROR".equals(this.status);
    }

    /**
     * 세션 나이 (분)
     */
    @JsonIgnore
    public long getAgeInMinutes() {
        return ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now());
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

    @Override
    public String toString() {
        return String.format("MemorialVideoSession{sessionKey='%s', contactName='%s', status='%s', age=%dmin, connected=%s}",
                sessionKey, contactName, status, getAgeInMinutes(), isConnected());
    }
}