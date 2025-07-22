package com.tomato.remember.application.videocall.service;

import com.tomato.remember.application.videocall.config.MemorialVideoSessionManager;
import com.tomato.remember.application.videocall.config.MemorialVideoWebSocketHandler;
import com.tomato.remember.application.videocall.dto.MemorialVideoSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Memorial Video Call 하트비트 서비스
 * 주기적으로 하트비트를 전송하고 세션 정리를 수행
 */
@Slf4j
//@Service
@RequiredArgsConstructor
public class MemorialVideoHeartbeatService {
    
    private final MemorialVideoSessionManager sessionManager;
    
    private final MemorialVideoWebSocketHandler webSocketHandler;
    
    /**
     * 하트비트 전송 (30초마다)
     */
    @Scheduled(fixedRate = 30000) // 30초
    public void sendHeartbeat() {
        Set<MemorialVideoSession> activeSessions = sessionManager.getActiveSessions();
        
        if (activeSessions.isEmpty()) {
            return;
        }
        
        log.debug("💓 하트비트 전송 시작 - 활성 세션: {}개", activeSessions.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (MemorialVideoSession session : activeSessions) {
            if (session.isConnected()) {
                try {
                    // 하트비트 메시지 생성
                    Map<String, Object> heartbeatMessage = Map.of(
                        "type", "HEARTBEAT",
                        "timestamp", System.currentTimeMillis(),
                        "sessionKey", session.getSessionKey(),
                        "sessionAge", session.getAgeInMinutes(),
                        "ttlRemaining", session.getRemainingTtlSeconds(),
                        "reconnectCount", session.getReconnectCount()
                    );
                    
                    // 웹소켓으로 하트비트 전송
                    webSocketHandler.sendMessageToSession(session.getSessionKey(), heartbeatMessage);
                    
                    // TTL 갱신
                    sessionManager.extendSessionTtl(session.getSessionKey());
                    
                    successCount++;
                    
                    log.debug("💓 하트비트 전송: {} (나이: {}분, 남은TTL: {}초)", 
                            session.getSessionKey(), session.getAgeInMinutes(), session.getRemainingTtlSeconds());
                    
                } catch (Exception e) {
                    failureCount++;
                    log.warn("💔 하트비트 전송 실패: {} (오류: {})", session.getSessionKey(), e.getMessage());
                    
                    // 전송 실패 시 연결 상태 업데이트
                    session.clearWebSocketConnection();
                    sessionManager.saveSession(session);
                }
            }
        }
        
        if (successCount > 0 || failureCount > 0) {
            log.debug("💓 하트비트 전송 완료 - 성공: {}개, 실패: {}개", successCount, failureCount);
        }
    }
    
    /**
     * 만료된 세션 정리 (10분마다)
     */
    @Scheduled(fixedRate = 600000) // 10분
    public void cleanupExpiredSessions() {
        log.info("🧹 만료된 세션 정리 시작");
        
        int cleanedCount = sessionManager.cleanupExpiredSessions();
        
        if (cleanedCount > 0) {
            log.info("✅ 만료된 세션 정리 완료: {}개", cleanedCount);
        } else {
            log.debug("🧹 정리할 만료된 세션 없음");
        }
    }
    
    /**
     * 연결 상태 모니터링 (5분마다)
     */
    @Scheduled(fixedRate = 300000) // 5분
    public void monitorConnections() {
        MemorialVideoSessionManager.SessionStatistics stats = sessionManager.getSessionStatistics();
        int activeWebSocketConnections = webSocketHandler.getActiveConnectionCount();
        
        log.info("📊 Memorial Video Call 상태 모니터링");
        log.info("  - 전체 세션: {}개", stats.getTotalSessions());
        log.info("  - 연결된 세션: {}개", stats.getConnectedSessions());
        log.info("  - 대기 중 세션: {}개", stats.getWaitingSessions());
        log.info("  - 처리 중 세션: {}개", stats.getProcessingSessions());
        log.info("  - 활성 웹소켓 연결: {}개", activeWebSocketConnections);
        log.info("  - 평균 세션 나이: {:.1f}분", stats.getAvgSessionAgeMinutes());
        
        // 웹소켓 연결 수와 Redis 세션 수 불일치 감지
        if (activeWebSocketConnections != stats.getConnectedSessions()) {
            log.warn("⚠️ 웹소켓 연결 수와 Redis 세션 수 불일치 - 웹소켓: {}개, Redis: {}개", 
                    activeWebSocketConnections, stats.getConnectedSessions());
        }
        
        // 장기 실행 세션 알림 (30분 이상)
        Set<MemorialVideoSession> activeSessions = sessionManager.getActiveSessions();
        activeSessions.stream()
                .filter(session -> session.getAgeInMinutes() > 30)
                .forEach(session -> 
                    log.info("🕐 장기 실행 세션: {} (나이: {}분, 상태: {})", 
                            session.getSessionKey(), session.getAgeInMinutes(), session.getStatus())
                );
    }
    
    /**
     * 세션 상태 리포트 (1시간마다)
     */
    @Scheduled(fixedRate = 3600000) // 1시간
    public void generateSessionReport() {
        MemorialVideoSessionManager.SessionStatistics stats = sessionManager.getSessionStatistics();
        Set<MemorialVideoSession> activeSessions = sessionManager.getActiveSessions();
        
        log.info("📈 Memorial Video Call 세션 리포트 (1시간)");
        log.info("==========================================");
        log.info("전체 활성 세션: {}개", stats.getTotalSessions());
        log.info("연결 상태별 분포:");
        log.info("  - 연결됨: {}개", stats.getConnectedSessions());
        log.info("  - 대기 중: {}개", stats.getWaitingSessions());
        log.info("  - 처리 중: {}개", stats.getProcessingSessions());
        log.info("평균 세션 지속 시간: {:.1f}분", stats.getAvgSessionAgeMinutes());
        
        // 재연결 통계
        long reconnectedSessions = activeSessions.stream()
                .filter(session -> session.getReconnectCount() > 0)
                .count();
        
        double avgReconnectCount = activeSessions.stream()
                .filter(session -> session.getReconnectCount() > 0)
                .mapToInt(MemorialVideoSession::getReconnectCount)
                .average()
                .orElse(0.0);
        
        log.info("재연결 통계:");
        log.info("  - 재연결된 세션: {}개", reconnectedSessions);
        log.info("  - 평균 재연결 횟수: {:.1f}회", avgReconnectCount);
        
        // 상태별 세션 목록 (디버깅용)
        activeSessions.stream()
                .filter(session -> session.getAgeInMinutes() > 10) // 10분 이상만
                .forEach(session -> 
                    log.debug("세션 상세: {} | 나이: {}분 | 상태: {} | 재연결: {}회", 
                            session.getSessionKey(), 
                            session.getAgeInMinutes(), 
                            session.getStatus(),
                            session.getReconnectCount())
                );
        
        log.info("==========================================");
    }
    
    /**
     * 수동 하트비트 전송 (특정 세션)
     */
    public boolean sendHeartbeatToSession(String sessionKey) {
        try {
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            
            if (session != null && session.isConnected()) {
                Map<String, Object> heartbeatMessage = Map.of(
                    "type", "HEARTBEAT",
                    "timestamp", System.currentTimeMillis(),
                    "sessionKey", sessionKey,
                    "sessionAge", session.getAgeInMinutes(),
                    "ttlRemaining", session.getRemainingTtlSeconds(),
                    "manual", true
                );
                
                webSocketHandler.sendMessageToSession(sessionKey, heartbeatMessage);
                sessionManager.extendSessionTtl(sessionKey);
                
                log.info("💓 수동 하트비트 전송 성공: {}", sessionKey);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("💔 수동 하트비트 전송 실패: {} (오류: {})", sessionKey, e.getMessage());
            return false;
        }
    }
}