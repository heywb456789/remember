package com.tomato.remember.application.videocall.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomato.remember.application.videocall.dto.MemorialVideoSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Memorial Video Call WebSocket 핸들러
 * 클라이언트와의 실시간 통신 처리
 */
@Slf4j
@Component
public class MemorialVideoWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private MemorialVideoSessionManager sessionManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 활성 웹소켓 연결 관리 (메모리 내)
    private final Map<String, WebSocketSession> activeConnections = new ConcurrentHashMap<>();
    
    private static final Pattern SESSION_KEY_PATTERN = Pattern.compile("/ws/memorial-video/(?:native/)?([^/?]+)");
    
    @Override
    public void afterConnectionEstablished(WebSocketSession socket) throws Exception {
        String sessionKey = extractSessionKey(socket);
        
        if (sessionKey == null) {
            log.error("❌ 세션 키를 추출할 수 없음: {}", socket.getUri());
            socket.close(CloseStatus.BAD_DATA.withReason("Invalid session key"));
            return;
        }
        
        log.info("🔗 웹소켓 연결 시도: {} (Socket ID: {})", sessionKey, socket.getId());
        
        try {
            // 기존 연결 정리 (재연결 시)
            cleanupExistingConnection(sessionKey);
            
            // 새 연결 등록
            activeConnections.put(socket.getId(), socket);
            sessionManager.mapSocketToSession(socket.getId(), sessionKey);
            
            // 세션 복구 또는 생성 대기 (클라이언트 CONNECT 메시지에서 처리)
            log.info("✅ 웹소켓 연결 등록 완료: {} → {}", sessionKey, socket.getId());
            
        } catch (Exception e) {
            log.error("❌ 웹소켓 연결 설정 실패: {}", sessionKey, e);
            socket.close(CloseStatus.SERVER_ERROR.withReason("Connection setup failed"));
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession socket, TextMessage message) throws Exception {
        String sessionKey = sessionManager.getSessionKeyBySocketId(socket.getId());
        
        if (sessionKey == null) {
            log.warn("⚠️ 매핑되지 않은 소켓에서 메시지 수신: {}", socket.getId());
            sendErrorMessage(socket, "INVALID_SESSION", "세션을 찾을 수 없습니다");
            return;
        }
        
        try {
            Map<String, Object> messageData = objectMapper.readValue(message.getPayload(), Map.class);
            String messageType = (String) messageData.get("type");
            
            log.debug("📨 메시지 수신: {} (타입: {})", sessionKey, messageType);
            
            switch (messageType) {
                case "CONNECT":
                    handleConnectMessage(socket, sessionKey, messageData);
                    break;
                    
                case "HEARTBEAT_RESPONSE":
                    handleHeartbeatResponse(sessionKey, messageData);
                    break;
                    
                case "VIDEO_UPLOAD_COMPLETE":
                    handleVideoUploadComplete(sessionKey, messageData);
                    break;
                    
                case "DISCONNECT":
                    handleDisconnectMessage(sessionKey, messageData);
                    break;
                    
                default:
                    log.warn("⚠️ 알 수 없는 메시지 타입: {} (세션: {})", messageType, sessionKey);
                    sendErrorMessage(socket, "UNKNOWN_MESSAGE_TYPE", "알 수 없는 메시지 타입입니다");
            }
            
        } catch (Exception e) {
            log.error("❌ 메시지 처리 오류: {} (세션: {})", e.getMessage(), sessionKey);
            sendErrorMessage(socket, "MESSAGE_PROCESSING_ERROR", "메시지 처리 중 오류가 발생했습니다");
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession socket, CloseStatus status) throws Exception {
        String sessionKey = sessionManager.getSessionKeyBySocketId(socket.getId());
        
        log.info("🔌 웹소켓 연결 종료: {} (상태: {}, 세션: {})", 
                socket.getId(), status.getCode(), sessionKey);
        
        // 연결 정리
        activeConnections.remove(socket.getId());
        
        if (sessionKey != null) {
            sessionManager.unmapSocket(socket.getId());
            
            // 정상 종료(1000)가 아닌 경우 세션 유지 (TTL에 의해 자동 정리)
            if (status.getCode() != CloseStatus.NORMAL.getCode()) {
                log.info("🔄 비정상 종료 - 세션 유지: {} (재연결 가능)", sessionKey);
            } else {
                log.info("✅ 정상 종료: {}", sessionKey);
            }
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession socket, Throwable exception) throws Exception {
        String sessionKey = sessionManager.getSessionKeyBySocketId(socket.getId());
        
        log.error("🔥 웹소켓 전송 오류: {} (세션: {})", exception.getMessage(), sessionKey);
        
        // 오류 발생 시 연결 정리
        if (socket.isOpen()) {
            socket.close(CloseStatus.SERVER_ERROR);
        }
    }
    
    /**
     * CONNECT 메시지 처리
     */
    private void handleConnectMessage(WebSocketSession socket, String sessionKey, Map<String, Object> messageData) throws Exception {
        String contactName = (String) messageData.get("contactName");
        Boolean isReconnect = (Boolean) messageData.getOrDefault("reconnect", false);
        Long memorialId = messageData.get("memorialId") != null ? 
                Long.valueOf(messageData.get("memorialId").toString()) : null;
        Long callerId = messageData.get("callerId") != null ? 
                Long.valueOf(messageData.get("callerId").toString()) : null;
        
        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        boolean sessionRecovered = false;
        
        if (session != null && isReconnect) {
            // 기존 세션 복구
            session.handleReconnect(socket.getId());
            sessionManager.saveSession(session);
            sessionRecovered = true;
            log.info("🔄 세션 복구: {} (재연결 횟수: {})", sessionKey, session.getReconnectCount());
        } else {
            // 새 세션 생성
            session = sessionManager.createSession(contactName, memorialId, callerId);
            session.setWebSocketConnection(socket.getId());
            sessionManager.saveSession(session);
            log.info("🆕 새 세션 생성: {}", sessionKey);
        }
        
        // 연결 완료 응답
        Map<String, Object> response = Map.of(
            "type", "CONNECTED",
            "sessionKey", sessionKey,
            "contactName", session.getContactName(),
            "reconnected", sessionRecovered,
            "sessionAge", session.getAgeInMinutes(),
            "ttlRemaining", session.getRemainingTtlSeconds(),
            "reconnectCount", session.getReconnectCount()
        );
        
        sendMessage(socket, response);
        
        log.info("✅ CONNECT 처리 완료: {} (복구: {})", sessionKey, sessionRecovered);
    }
    
    /**
     * 하트비트 응답 처리
     */
    private void handleHeartbeatResponse(String sessionKey, Map<String, Object> messageData) {
        // TTL 갱신
        boolean extended = sessionManager.extendSessionTtl(sessionKey);
        
        if (extended) {
            log.debug("💓 하트비트 응답 처리: {} (TTL 갱신)", sessionKey);
        } else {
            log.warn("💔 하트비트 응답 실패: {} (세션 없음)", sessionKey);
        }
    }
    
    /**
     * 영상 업로드 완료 처리
     */
    private void handleVideoUploadComplete(String sessionKey, Map<String, Object> messageData) {
        String filePath = (String) messageData.get("filePath");
        
        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        if (session != null) {
            session.setProcessing(filePath);
            sessionManager.saveSession(session);
            
            log.info("📹 영상 업로드 완료: {} (파일: {})", sessionKey, filePath);
            
            // TODO: 외부 AI API 호출 로직 추가
            // externalVideoApiService.sendVideoToExternalApiAsync(sessionKey, filePath);
        }
    }
    
    /**
     * 연결 해제 메시지 처리
     */
    private void handleDisconnectMessage(String sessionKey, Map<String, Object> messageData) throws Exception {
        String reason = (String) messageData.getOrDefault("reason", "UNKNOWN");
        
        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        if (session != null) {
            if ("USER_ACTION".equals(reason)) {
                // 사용자 명시적 종료 - 세션 완전 삭제
                sessionManager.deleteSession(sessionKey);
                log.info("🚪 사용자 종료: {} (사유: {})", sessionKey, reason);
            } else {
                // 기타 사유 - 세션 유지
                session.clearWebSocketConnection();
                sessionManager.saveSession(session);
                log.info("🔄 연결 해제: {} (사유: {}, 세션 유지)", sessionKey, reason);
            }
        }
        
        // 소켓 연결도 정리
        WebSocketSession socket = activeConnections.get(session.getSocketId());
        if (socket != null && socket.isOpen()) {
            socket.close(CloseStatus.NORMAL);
        }
    }
    
    /**
     * 특정 세션에 메시지 전송
     */
    public void sendMessageToSession(String sessionKey, Map<String, Object> message) {
        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        
        if (session != null && session.isConnected()) {
            WebSocketSession socket = activeConnections.get(session.getSocketId());
            
            if (socket != null && socket.isOpen()) {
                try {
                    sendMessage(socket, message);
                    log.debug("📤 세션 메시지 전송: {} (타입: {})", sessionKey, message.get("type"));
                } catch (Exception e) {
                    log.error("❌ 세션 메시지 전송 실패: {} (오류: {})", sessionKey, e.getMessage());
                }
            }
        }
    }
    
    /**
     * 응답 영상 URL 전송
     */
    public void sendResponseVideo(String sessionKey, String videoUrl) {
        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        
        if (session != null) {
            session.setCompleted(videoUrl);
            sessionManager.saveSession(session);
            
            Map<String, Object> message = Map.of(
                "type", "RESPONSE_VIDEO",
                "sessionKey", sessionKey,
                "videoUrl", videoUrl,
                "contactName", session.getContactName(),
                "timestamp", System.currentTimeMillis()
            );
            
            sendMessageToSession(sessionKey, message);
            log.info("🎬 응답 영상 전송: {} (URL: {})", sessionKey, videoUrl);
        }
    }
    
    /**
     * 활성 연결 수 조회
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }
    
    /**
     * 웹소켓에 메시지 전송 (공통)
     */
    private void sendMessage(WebSocketSession socket, Map<String, Object> message) throws Exception {
        if (socket.isOpen()) {
            String jsonMessage = objectMapper.writeValueAsString(message);
            socket.sendMessage(new TextMessage(jsonMessage));
        }
    }
    
    /**
     * 오류 메시지 전송
     */
    private void sendErrorMessage(WebSocketSession socket, String code, String message) {
        try {
            Map<String, Object> errorMessage = Map.of(
                "type", "ERROR",
                "code", code,
                "message", message,
                "timestamp", System.currentTimeMillis()
            );
            
            sendMessage(socket, errorMessage);
        } catch (Exception e) {
            log.error("❌ 오류 메시지 전송 실패: {}", e.getMessage());
        }
    }
    
    /**
     * URL에서 세션 키 추출
     */
    private String extractSessionKey(WebSocketSession socket) {
        URI uri = socket.getUri();
        if (uri == null) return null;
        
        Matcher matcher = SESSION_KEY_PATTERN.matcher(uri.getPath());
        return matcher.find() ? matcher.group(1) : null;
    }
    
    /**
     * 기존 연결 정리 (재연결 시)
     */
    private void cleanupExistingConnection(String sessionKey) {
        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        
        if (session != null && session.getSocketId() != null) {
            WebSocketSession oldSocket = activeConnections.get(session.getSocketId());
            
            if (oldSocket != null && oldSocket.isOpen()) {
                try {
                    log.info("🧹 기존 연결 정리: {} (이전 소켓: {})", sessionKey, session.getSocketId());
                    oldSocket.close(CloseStatus.GOING_AWAY.withReason("New connection established"));
                } catch (Exception e) {
                    log.warn("⚠️ 기존 연결 정리 중 오류: {}", e.getMessage());
                }
            }
            
            activeConnections.remove(session.getSocketId());
        }
    }
}