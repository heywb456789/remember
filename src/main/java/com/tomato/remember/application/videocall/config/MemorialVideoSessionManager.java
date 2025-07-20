package com.tomato.remember.application.videocall.config;

import com.tomato.remember.application.videocall.dto.MemorialVideoSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Memorial Video Call 세션 관리자
 * Redis 기반으로 TTL을 이용한 세션 생명주기 관리
 */
@Slf4j
@Component
public class MemorialVideoSessionManager {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String SESSION_KEY_PREFIX = "memorial:video:session:";
    private static final String SOCKET_MAPPING_PREFIX = "memorial:video:socket:";
    
    /**
     * 새로운 세션 생성
     */
    public MemorialVideoSession createSession(String contactName, Long memorialId, Long callerId) {
        String sessionKey = generateSessionKey();
        
        MemorialVideoSession session = MemorialVideoSession.createNew(
            sessionKey, contactName, memorialId, callerId
        );
        
        saveSession(session);
        
        log.info("🆕 새 Memorial Video 세션 생성: {} (연락처: {}, Memorial ID: {})", 
                sessionKey, contactName, memorialId);
        
        return session;
    }
    
    /**
     * 세션 저장 (TTL 포함)
     */
    public void saveSession(MemorialVideoSession session) {
        String key = SESSION_KEY_PREFIX + session.getSessionKey();
        
        redisTemplate.opsForValue().set(
            key, 
            session, 
            MemorialVideoSession.getTtlSeconds(), 
            TimeUnit.SECONDS
        );
        
        log.debug("💾 세션 저장: {} (TTL: {}초)", session.getSessionKey(), MemorialVideoSession.getTtlSeconds());
    }
    
    /**
     * 세션 조회
     */
    public MemorialVideoSession getSession(String sessionKey) {
        String key = SESSION_KEY_PREFIX + sessionKey;
        MemorialVideoSession session = (MemorialVideoSession) redisTemplate.opsForValue().get(key);
        
        if (session != null) {
            log.debug("📂 세션 조회 성공: {} (나이: {}분)", sessionKey, session.getAgeInMinutes());
        } else {
            log.debug("📂 세션 조회 실패: {} (만료 또는 없음)", sessionKey);
        }
        
        return session;
    }
    
    /**
     * 세션 TTL 갱신
     */
    public boolean extendSessionTtl(String sessionKey) {
        String key = SESSION_KEY_PREFIX + sessionKey;
        
        MemorialVideoSession session = getSession(sessionKey);
        if (session != null) {
            session.updateActivity();
            
            // Redis TTL 갱신
            redisTemplate.expire(key, MemorialVideoSession.getTtlSeconds(), TimeUnit.SECONDS);
            
            // 세션 데이터도 업데이트
            redisTemplate.opsForValue().set(key, session, MemorialVideoSession.getTtlSeconds(), TimeUnit.SECONDS);
            
            log.debug("⏰ TTL 갱신: {} (남은 시간: {}초)", sessionKey, session.getRemainingTtlSeconds());
            return true;
        }
        
        return false;
    }
    
    /**
     * 웹소켓 연결 매핑 저장
     */
    public void mapSocketToSession(String socketId, String sessionKey) {
        String key = SOCKET_MAPPING_PREFIX + socketId;
        
        redisTemplate.opsForValue().set(
            key, 
            sessionKey, 
            MemorialVideoSession.getTtlSeconds(), 
            TimeUnit.SECONDS
        );
        
        // 세션에도 소켓 ID 업데이트
        MemorialVideoSession session = getSession(sessionKey);
        if (session != null) {
            session.setWebSocketConnection(socketId);
            saveSession(session);
        }
        
        log.debug("🔗 소켓 매핑: {} → {}", socketId, sessionKey);
    }
    
    /**
     * 소켓 ID로 세션 키 조회
     */
    public String getSessionKeyBySocketId(String socketId) {
        String key = SOCKET_MAPPING_PREFIX + socketId;
        return (String) redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 웹소켓 연결 해제
     */
    public void unmapSocket(String socketId) {
        String sessionKey = getSessionKeyBySocketId(socketId);
        
        if (sessionKey != null) {
            // 소켓 매핑 제거
            redisTemplate.delete(SOCKET_MAPPING_PREFIX + socketId);
            
            // 세션에서 소켓 정보 제거
            MemorialVideoSession session = getSession(sessionKey);
            if (session != null) {
                session.clearWebSocketConnection();
                saveSession(session);
                
                log.debug("🔌 소켓 연결 해제: {} (세션: {})", socketId, sessionKey);
            }
        }
    }
    
    /**
     * 세션 명시적 삭제 (정상 종료 시)
     */
    public void deleteSession(String sessionKey) {
        MemorialVideoSession session = getSession(sessionKey);
        
        if (session != null) {
            // 소켓 매핑도 함께 삭제
            if (session.getSocketId() != null) {
                redisTemplate.delete(SOCKET_MAPPING_PREFIX + session.getSocketId());
            }
            
            // 세션 삭제
            redisTemplate.delete(SESSION_KEY_PREFIX + sessionKey);
            
            log.info("🗑️ 세션 명시적 삭제: {} (나이: {}분)", sessionKey, session.getAgeInMinutes());
        }
    }
    
    /**
     * 활성 세션 목록 조회
     */
    public Set<MemorialVideoSession> getActiveSessions() {
        Set<String> keys = redisTemplate.keys(SESSION_KEY_PREFIX + "*");
        
        if (keys == null || keys.isEmpty()) {
            return Set.of();
        }
        
        return keys.stream()
                .map(key -> (MemorialVideoSession) redisTemplate.opsForValue().get(key))
                .filter(session -> session != null && !session.isExpired())
                .collect(Collectors.toSet());
    }
    
    /**
     * 세션 통계 조회
     */
    public SessionStatistics getSessionStatistics() {
        Set<MemorialVideoSession> activeSessions = getActiveSessions();
        
        long totalSessions = activeSessions.size();
        long connectedSessions = activeSessions.stream()
                .filter(MemorialVideoSession::isConnected)
                .count();
        long waitingSessions = activeSessions.stream()
                .filter(session -> "WAITING".equals(session.getStatus()))
                .count();
        long processingSessions = activeSessions.stream()
                .filter(session -> "PROCESSING".equals(session.getStatus()))
                .count();
        
        return SessionStatistics.builder()
                .totalSessions(totalSessions)
                .connectedSessions(connectedSessions)
                .waitingSessions(waitingSessions)
                .processingSessions(processingSessions)
                .avgSessionAgeMinutes(activeSessions.stream()
                        .mapToLong(MemorialVideoSession::getAgeInMinutes)
                        .average()
                        .orElse(0.0))
                .build();
    }
    
    /**
     * 만료된 세션 수동 정리 (스케줄링용)
     */
    public int cleanupExpiredSessions() {
        Set<String> allKeys = redisTemplate.keys(SESSION_KEY_PREFIX + "*");
        int cleanedCount = 0;
        
        if (allKeys != null) {
            for (String key : allKeys) {
                MemorialVideoSession session = (MemorialVideoSession) redisTemplate.opsForValue().get(key);
                if (session != null && session.isExpired()) {
                    redisTemplate.delete(key);
                    
                    // 관련 소켓 매핑도 정리
                    if (session.getSocketId() != null) {
                        redisTemplate.delete(SOCKET_MAPPING_PREFIX + session.getSocketId());
                    }
                    
                    cleanedCount++;
                    log.info("🧹 만료된 세션 정리: {} (나이: {}분)", 
                            session.getSessionKey(), session.getAgeInMinutes());
                }
            }
        }
        
        if (cleanedCount > 0) {
            log.info("✅ 만료된 세션 정리 완료: {}개", cleanedCount);
        }
        
        return cleanedCount;
    }
    
    /**
     * 유니크한 세션 키 생성
     */
    private String generateSessionKey() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "MVC_" + timestamp + "_" + uuid;
    }
    
    /**
     * 세션 통계 내부 클래스
     */
    @lombok.Data
    @lombok.Builder
    public static class SessionStatistics {
        private long totalSessions;
        private long connectedSessions;
        private long waitingSessions;
        private long processingSessions;
        private double avgSessionAgeMinutes;
    }
}