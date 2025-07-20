package com.tomato.remember.application.videocall.controller;

import com.tomato.remember.application.videocall.config.MemorialVideoSessionManager;
import com.tomato.remember.application.videocall.config.MemorialVideoWebSocketHandler;
import com.tomato.remember.application.videocall.dto.MemorialVideoSession;
import com.tomato.remember.application.videocall.service.MemorialVideoHeartbeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * Memorial Video Call WebSocket 메시지 처리 컨트롤러
 * STOMP 프로토콜 기반 메시지 처리 (선택적 구현)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MemorialVideoWebSocketController {
    
    private MemorialVideoSessionManager sessionManager;
    
    private MemorialVideoWebSocketHandler webSocketHandler;
    
    private MemorialVideoHeartbeatService heartbeatService;
    
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 클라이언트 핑 처리
     */
    @MessageMapping("/memorial-video/{sessionKey}/ping")
    @SendTo("/topic/memorial-video/{sessionKey}/pong")
    public Map<String, Object> handlePing(@DestinationVariable String sessionKey) {
        log.debug("📍 핑 수신: {}", sessionKey);
        
        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        if (session != null) {
            sessionManager.extendSessionTtl(sessionKey);
            
            return Map.of(
                "type", "PONG",
                "sessionKey", sessionKey,
                "timestamp", System.currentTimeMillis(),
                "sessionAge", session.getAgeInMinutes(),
                "ttlRemaining", session.getRemainingTtlSeconds()
            );
        }
        
        return Map.of(
            "type", "PONG",
            "sessionKey", sessionKey,
            "timestamp", System.currentTimeMillis(),
            "error", "SESSION_NOT_FOUND"
        );
    }
    
    /**
     * 세션 상태 업데이트 처리
     */
    @MessageMapping("/memorial-video/{sessionKey}/status")
    public void handleStatusUpdate(
            @DestinationVariable String sessionKey, 
            Map<String, Object> statusData) {
        
        log.debug("📊 상태 업데이트 수신: {}", sessionKey);
        
        try {
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session != null) {
                String newStatus = (String) statusData.get("status");
                if (newStatus != null) {
                    session.setStatus(newStatus);
                    session.updateActivity();
                    sessionManager.saveSession(session);
                    
                    // 상태 변경 브로드캐스트
                    Map<String, Object> statusUpdate = Map.of(
                        "type", "STATUS_UPDATED",
                        "sessionKey", sessionKey,
                        "status", newStatus,
                        "timestamp", System.currentTimeMillis()
                    );
                    
                    messagingTemplate.convertAndSend(
                        "/topic/memorial-video/" + sessionKey + "/status", 
                        statusUpdate
                    );
                }
            }
        } catch (Exception e) {
            log.error("❌ 상태 업데이트 처리 실패: {} (세션: {})", e.getMessage(), sessionKey);
        }
    }
    
    /**
     * 브로드캐스트 메시지 전송
     */
    @MessageMapping("/memorial-video/{sessionKey}/broadcast")
    public void handleBroadcast(
            @DestinationVariable String sessionKey,
            Map<String, Object> message) {
        
        log.debug("📢 브로드캐스트 메시지: {}", sessionKey);
        
        try {
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session != null && session.isConnected()) {
                
                // 메시지에 세션 정보 추가
                message.put("sessionKey", sessionKey);
                message.put("timestamp", System.currentTimeMillis());
                message.put("sessionAge", session.getAgeInMinutes());
                
                // 해당 세션 토픽으로 브로드캐스트
                messagingTemplate.convertAndSend(
                    "/topic/memorial-video/" + sessionKey + "/broadcast",
                    message
                );
                
                sessionManager.extendSessionTtl(sessionKey);
            }
        } catch (Exception e) {
            log.error("❌ 브로드캐스트 처리 실패: {} (세션: {})", e.getMessage(), sessionKey);
        }
    }
    
    /**
     * 관리자용 - 전체 세션 상태 조회
     */
    @MessageMapping("/memorial-video/admin/sessions")
    @SendTo("/topic/memorial-video/admin/sessions")
    public Map<String, Object> getAdminSessionStatus() {
        log.debug("👨‍💼 관리자 세션 상태 요청");
        
        try {
            Set<MemorialVideoSession> activeSessions = sessionManager.getActiveSessions();
            MemorialVideoSessionManager.SessionStatistics stats = sessionManager.getSessionStatistics();
            
            return Map.of(
                "type", "ADMIN_SESSION_STATUS",
                "timestamp", System.currentTimeMillis(),
                "statistics", Map.of(
                    "totalSessions", stats.getTotalSessions(),
                    "connectedSessions", stats.getConnectedSessions(),
                    "waitingSessions", stats.getWaitingSessions(),
                    "processingSessions", stats.getProcessingSessions(),
                    "avgSessionAge", stats.getAvgSessionAgeMinutes(),
                    "activeWebSocketConnections", webSocketHandler.getActiveConnectionCount()
                ),
                "sessions", activeSessions.stream().map(session -> Map.of(
                    "sessionKey", session.getSessionKey(),
                    "contactName", session.getContactName(),
                    "status", session.getStatus(),
                    "ageInMinutes", session.getAgeInMinutes(),
                    "ttlRemaining", session.getRemainingTtlSeconds(),
                    "isConnected", session.isConnected(),
                    "reconnectCount", session.getReconnectCount()
                )).toList()
            );
            
        } catch (Exception e) {
            log.error("❌ 관리자 세션 상태 조회 실패", e);
            
            return Map.of(
                "type", "ERROR",
                "message", "세션 상태 조회 실패",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * WebSocket API - 직접 메시지 전송
     */
    @PostMapping("/api/memorial-video/websocket/send/{sessionKey}")
    @ResponseBody
    public Map<String, Object> sendWebSocketMessage(
            @PathVariable String sessionKey,
            @RequestBody Map<String, Object> message) {
        
        try {
            log.info("📤 WebSocket 메시지 전송 요청: {}", sessionKey);
            
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session == null) {
                return Map.of(
                    "success", false,
                    "error", "SESSION_NOT_FOUND",
                    "message", "세션을 찾을 수 없습니다"
                );
            }
            
            if (!session.isConnected()) {
                return Map.of(
                    "success", false,
                    "error", "SESSION_NOT_CONNECTED",
                    "message", "세션이 연결되지 않았습니다"
                );
            }
            
            // 메시지 전송
            webSocketHandler.sendMessageToSession(sessionKey, message);
            
            return Map.of(
                "success", true,
                "sessionKey", sessionKey,
                "messageType", message.get("type"),
                "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            log.error("❌ WebSocket 메시지 전송 실패: {} (세션: {})", e.getMessage(), sessionKey);
            
            return Map.of(
                "success", false,
                "error", "SEND_FAILED",
                "message", "메시지 전송 실패: " + e.getMessage()
            );
        }
    }
    
    /**
     * WebSocket API - 모든 활성 세션에 브로드캐스트
     */
    @PostMapping("/api/memorial-video/websocket/broadcast")
    @ResponseBody
    public Map<String, Object> broadcastToAllSessions(@RequestBody Map<String, Object> message) {
        try {
            log.info("📢 전체 세션 브로드캐스트 요청");
            
            Set<MemorialVideoSession> activeSessions = sessionManager.getActiveSessions();
            int sentCount = 0;
            int failedCount = 0;
            
            for (MemorialVideoSession session : activeSessions) {
                if (session.isConnected()) {
                    try {
                        webSocketHandler.sendMessageToSession(session.getSessionKey(), message);
                        sentCount++;
                    } catch (Exception e) {
                        log.warn("⚠️ 브로드캐스트 실패: {} (오류: {})", session.getSessionKey(), e.getMessage());
                        failedCount++;
                    }
                }
            }
            
            log.info("📢 브로드캐스트 완료 - 성공: {}개, 실패: {}개", sentCount, failedCount);
            
            return Map.of(
                "success", true,
                "totalSessions", activeSessions.size(),
                "sentCount", sentCount,
                "failedCount", failedCount,
                "messageType", message.get("type"),
                "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            log.error("❌ 전체 브로드캐스트 실패", e);
            
            return Map.of(
                "success", false,
                "error", "BROADCAST_FAILED",
                "message", "브로드캐스트 실패: " + e.getMessage()
            );
        }
    }
    
    /**
     * WebSocket API - 연결 상태 강제 확인
     */
    @PostMapping("/api/memorial-video/websocket/ping/{sessionKey}")
    @ResponseBody
    public Map<String, Object> pingSession(@PathVariable String sessionKey) {
        try {
            log.debug("📍 세션 핑 요청: {}", sessionKey);
            
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session == null) {
                return Map.of(
                    "success", false,
                    "error", "SESSION_NOT_FOUND"
                );
            }
            
            if (!session.isConnected()) {
                return Map.of(
                    "success", false,
                    "error", "SESSION_NOT_CONNECTED",
                    "sessionAge", session.getAgeInMinutes(),
                    "ttlRemaining", session.getRemainingTtlSeconds()
                );
            }
            
            // 핑 메시지 전송
            Map<String, Object> pingMessage = Map.of(
                "type", "PING",
                "timestamp", System.currentTimeMillis(),
                "sessionKey", sessionKey
            );
            
            webSocketHandler.sendMessageToSession(sessionKey, pingMessage);
            
            return Map.of(
                "success", true,
                "sessionKey", sessionKey,
                "sessionAge", session.getAgeInMinutes(),
                "ttlRemaining", session.getRemainingTtlSeconds(),
                "timestamp", System.currentTimeMillis()
            );
            
        } catch (Exception e) {
            log.error("❌ 세션 핑 실패: {} (세션: {})", e.getMessage(), sessionKey);
            
            return Map.of(
                "success", false,
                "error", "PING_FAILED",
                "message", e.getMessage()
            );
        }
    }
    
    /**
     * WebSocket API - 세션 강제 연결 해제
     */
    @PostMapping("/api/memorial-video/websocket/disconnect/{sessionKey}")
    @ResponseBody
    public Map<String, Object> forceDisconnectSession(
            @PathVariable String sessionKey,
            @RequestBody(required = false) Map<String, Object> request) {
        
        try {
            log.info("🔌 세션 강제 연결 해제 요청: {}", sessionKey);
            
            String reason = request != null ? 
                    (String) request.getOrDefault("reason", "FORCE_DISCONNECT") : "FORCE_DISCONNECT";
            
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session != null && session.isConnected()) {
                
                // 연결 해제 메시지 전송
                Map<String, Object> disconnectMessage = Map.of(
                    "type", "FORCE_DISCONNECT",
                    "reason", reason,
                    "message", "관리자에 의해 연결이 해제되었습니다",
                    "timestamp", System.currentTimeMillis()
                );
                
                webSocketHandler.sendMessageToSession(sessionKey, disconnectMessage);
                
                // 세션 상태 업데이트
                session.clearWebSocketConnection();
                sessionManager.saveSession(session);
                
                return Map.of(
                    "success", true,
                    "sessionKey", sessionKey,
                    "reason", reason,
                    "timestamp", System.currentTimeMillis()
                );
            }
            
            return Map.of(
                "success", false,
                "error", "SESSION_NOT_CONNECTED",
                "message", "세션이 연결되지 않았거나 존재하지 않습니다"
            );
            
        } catch (Exception e) {
            log.error("❌ 세션 강제 연결 해제 실패: {} (세션: {})", e.getMessage(), sessionKey);
            
            return Map.of(
                "success", false,
                "error", "DISCONNECT_FAILED",
                "message", e.getMessage()
            );
        }
    }
}