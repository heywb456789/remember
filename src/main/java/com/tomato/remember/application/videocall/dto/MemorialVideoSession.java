package com.tomato.remember.application.videocall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Duration;

/**
 * Memorial Video Call 세션 정보
 * Redis에 저장되어 TTL 기반으로 관리됨
 */
@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class MemorialVideoSession {
    
    private String sessionKey;
    private String contactName;
    private Long memorialId;
    private Long callerId;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    private String status; // WAITING, CONNECTING, CONNECTED, PROCESSING, COMPLETED, ERROR, DISCONNECTED
    
    private String savedFilePath;
    private String responseVideoUrl;
    
    // WebSocket 연결 정보
    private String socketId;
    private boolean isConnected;
    private int reconnectCount;
    
    // TTL 관리 (초 단위)
    private static final long SESSION_TTL_SECONDS = 3600; // 1시간
    private static final long HEARTBEAT_INTERVAL_SECONDS = 30; // 30초
    
    /**
     * 새로운 세션 생성
     */
    public static MemorialVideoSession createNew(String sessionKey, String contactName, Long memorialId, Long callerId) {
        return MemorialVideoSession.builder()
                .sessionKey(sessionKey)
                .contactName(contactName)
                .memorialId(memorialId)
                .callerId(callerId)
                .createdAt(LocalDateTime.now())
                .lastActivity(LocalDateTime.now())
                .status("WAITING")
                .isConnected(false)
                .reconnectCount(0)
                .build();
    }
    
    /**
     * 활동 시간 업데이트 (TTL 갱신용)
     */
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    /**
     * 웹소켓 연결 설정
     */
    public void setWebSocketConnection(String socketId) {
        this.socketId = socketId;
        this.isConnected = true;
        this.status = "CONNECTED";
        updateActivity();
    }
    
    /**
     * 웹소켓 연결 해제
     */
    public void clearWebSocketConnection() {
        this.socketId = null;
        this.isConnected = false;
        this.status = "DISCONNECTED";
        updateActivity();
    }
    
    /**
     * 재연결 처리
     */
    public void handleReconnect(String newSocketId) {
        this.socketId = newSocketId;
        this.isConnected = true;
        this.reconnectCount++;
        this.status = "CONNECTED";
        updateActivity();
    }
    
    /**
     * 세션 나이 (분 단위)
     */
    public long getAgeInMinutes() {
        return Duration.between(createdAt, LocalDateTime.now()).toMinutes();
    }
    
    /**
     * 비활성 시간 (분 단위)
     */
    public long getInactiveMinutes() {
        return Duration.between(lastActivity, LocalDateTime.now()).toMinutes();
    }
    
    /**
     * 남은 TTL 시간 (초 단위)
     */
    public long getRemainingTtlSeconds() {
        long inactiveSeconds = Duration.between(lastActivity, LocalDateTime.now()).getSeconds();
        return Math.max(0, SESSION_TTL_SECONDS - inactiveSeconds);
    }
    
    /**
     * 세션 만료 여부 확인
     */
    public boolean isExpired() {
        return getRemainingTtlSeconds() <= 0;
    }
    
    /**
     * 처리 중 상태로 변경
     */
    public void setProcessing(String filePath) {
        this.status = "PROCESSING";
        this.savedFilePath = filePath;
        updateActivity();
    }
    
    /**
     * 완료 상태로 변경
     */
    public void setCompleted(String responseVideoUrl) {
        this.status = "COMPLETED";
        this.responseVideoUrl = responseVideoUrl;
        updateActivity();
    }
    
    /**
     * 오류 상태로 변경
     */
    public void setError() {
        this.status = "ERROR";
        updateActivity();
    }
    
    /**
     * TTL 값 반환 (Redis 저장 시 사용)
     */
    public static long getTtlSeconds() {
        return SESSION_TTL_SECONDS;
    }
    
    /**
     * 하트비트 간격 반환
     */
    public static long getHeartbeatIntervalSeconds() {
        return HEARTBEAT_INTERVAL_SECONDS;
    }
}