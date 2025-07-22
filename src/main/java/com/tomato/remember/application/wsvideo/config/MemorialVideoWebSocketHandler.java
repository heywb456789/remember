package com.tomato.remember.application.wsvideo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomato.remember.application.wsvideo.code.VideoCallFlowState;
import com.tomato.remember.application.wsvideo.dto.MemorialVideoSession;
import com.tomato.remember.common.security.JwtTokenProvider;
import com.tomato.remember.application.wsvideo.service.MultiDeviceManager;
import com.tomato.remember.application.wsvideo.service.VideoCallFlowManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memorial Video Call WebSocket 핸들러 - 기존 JwtTokenProvider 사용
 * 인터셉터에서 1차 인증 후, 핸들러에서 2차 검증 수행
 */
@Slf4j
@Component
public class MemorialVideoWebSocketHandler extends TextWebSocketHandler {

    private final MemorialVideoSessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final VideoCallFlowManager flowManager;
    private final MultiDeviceManager deviceManager;
    private final JwtTokenProvider jwtTokenProvider;

    // 활성 웹소켓 연결 관리 (메모리 내)
    private final Map<String, WebSocketSession> activeConnections = new ConcurrentHashMap<>();

    public MemorialVideoWebSocketHandler(
            MemorialVideoSessionManager sessionManager,
            ObjectMapper objectMapper,
            @Lazy VideoCallFlowManager flowManager,
            @Lazy MultiDeviceManager deviceManager,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
        this.flowManager = flowManager;
        this.deviceManager = deviceManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession socket) throws Exception {
        // 인터셉터에서 인증된 정보 확인
        Boolean authenticated = (Boolean) socket.getAttributes().get("authenticated");
        Long memberId = (Long) socket.getAttributes().get("memberId");
        String userKey = (String) socket.getAttributes().get("userKey");
        String email = (String) socket.getAttributes().get("email");
        String sessionKey = (String) socket.getAttributes().get("sessionKey");

        if (!Boolean.TRUE.equals(authenticated) || memberId == null || sessionKey == null) {
            log.error("🔒 인증되지 않은 WebSocket 연결 시도 - SocketId: {}", socket.getId());
            socket.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
            return;
        }

        log.info("🔗 인증된 WebSocket 연결 설정: {} (회원ID: {}, 이메일: {}, SocketId: {})",
                sessionKey, memberId, email, socket.getId());

        try {
            // 기존 연결 정리 (재연결 시)
            cleanupExistingConnection(sessionKey);

            // 새 연결 등록
            activeConnections.put(socket.getId(), socket);
            sessionManager.mapSocketToSession(socket.getId(), sessionKey);

            log.info("✅ 인증된 WebSocket 연결 등록 완료: {} → {} (회원ID: {})",
                    sessionKey, socket.getId(), memberId);

        } catch (Exception e) {
            log.error("❌ 인증된 WebSocket 연결 설정 실패: {} (회원ID: {})", sessionKey, memberId, e);
            socket.close(CloseStatus.SERVER_ERROR.withReason("Connection setup failed"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession socket, TextMessage message) throws Exception {
        // 연결된 소켓의 인증 정보 확인
        Long authenticatedMemberId = (Long) socket.getAttributes().get("memberId");
        String sessionKey = (String) socket.getAttributes().get("sessionKey");

        if (authenticatedMemberId == null || sessionKey == null) {
            log.warn("⚠️ 인증 정보가 없는 소켓에서 메시지 수신: {}", socket.getId());
            sendErrorMessage(socket, "AUTHENTICATION_FAILED", "인증 정보가 없습니다");
            socket.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        try {
            Map<String, Object> messageData = objectMapper.readValue(message.getPayload(), Map.class);
            String messageType = (String) messageData.get("type");

            log.debug("📨 인증된 메시지 수신: {} (타입: {}, 회원ID: {})",
                     sessionKey, messageType, authenticatedMemberId);

            // 메시지별 추가 권한 검증 수행
            if (!validateMessagePermissions(messageType, messageData, authenticatedMemberId, sessionKey)) {
                log.warn("🔒 메시지 권한 검증 실패: {} (타입: {}, 회원ID: {})",
                        sessionKey, messageType, authenticatedMemberId);
                sendErrorMessage(socket, "PERMISSION_DENIED", "해당 작업에 대한 권한이 없습니다");
                return;
            }

            // 기존 메시지 처리 로직
            switch (messageType) {
                case "CONNECT":
                    handleConnectMessage(socket, sessionKey, messageData, authenticatedMemberId);
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
                case "CLIENT_STATE_CHANGE":
                    handleClientStateChange(sessionKey, messageData);
                    break;
                case "PERMISSION_STATUS":
                    handlePermissionStatus(sessionKey, messageData);
                    break;
                case "DEVICE_INFO":
                    handleDeviceInfo(sessionKey, messageData);
                    break;
                case "WAITING_VIDEO_STARTED":
                    handleWaitingVideoStarted(sessionKey, messageData);
                    break;
                case "WAITING_VIDEO_ERROR":
                    handleWaitingVideoError(sessionKey, messageData);
                    break;
                case "RECORDING_READY":
                    handleRecordingReady(sessionKey, messageData);
                    break;
                case "RECORDING_STARTED":
                    handleRecordingStarted(sessionKey, messageData);
                    break;
                case "RECORDING_STOPPED":
                    handleRecordingStopped(sessionKey, messageData);
                    break;
                case "RECORDING_ERROR":
                    handleRecordingError(sessionKey, messageData);
                    break;
                case "RESPONSE_VIDEO_STARTED":
                    handleResponseVideoStarted(sessionKey, messageData);
                    break;
                case "RESPONSE_VIDEO_ENDED":
                    handleResponseVideoEnded(sessionKey, messageData);
                    break;
                case "RESPONSE_VIDEO_ERROR":
                    handleResponseVideoError(sessionKey, messageData);
                    break;
                case "TOKEN_REFRESH":
                    handleTokenRefresh(socket, sessionKey, messageData, authenticatedMemberId);
                    break;

                default:
                    log.warn("⚠️ 알 수 없는 메시지 타입: {} (세션: {})", messageType, sessionKey);
                    sendErrorMessage(socket, "UNKNOWN_MESSAGE_TYPE", "알 수 없는 메시지 타입입니다");
            }

        } catch (Exception e) {
            log.error("❌ 메시지 처리 오류: {} (세션: {}, 회원ID: {})",
                     e.getMessage(), sessionKey, authenticatedMemberId);
            sendErrorMessage(socket, "MESSAGE_PROCESSING_ERROR", "메시지 처리 중 오류가 발생했습니다");
        }
    }

    /**
     * 메시지별 권한 검증
     */
    private boolean validateMessagePermissions(String messageType, Map<String, Object> messageData,
                                             Long authenticatedMemberId, String sessionKey) {
        try {
            // 세션 존재 및 소유권 재확인
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session == null) {
                log.warn("🔒 세션이 존재하지 않음 - SessionKey: {}", sessionKey);
                return false;
            }

            if (!authenticatedMemberId.equals(session.getCallerId())) {
                log.warn("🔒 세션 소유권 불일치 - 토큰회원ID: {}, 세션회원ID: {}",
                        authenticatedMemberId, session.getCallerId());
                return false;
            }

            // 메시지 타입별 특별 권한 확인
            switch (messageType) {
                case "CONNECT":
                    // CONNECT 메시지의 memberId와 토큰의 memberId 일치 확인
                    Object memberIdObj = messageData.get("memberId");
                    if (memberIdObj != null) {
                        Long messageMemberId = Long.valueOf(memberIdObj.toString());
                        if (!authenticatedMemberId.equals(messageMemberId)) {
                            log.warn("🔒 CONNECT 메시지 회원ID 불일치 - 토큰: {}, 메시지: {}",
                                    authenticatedMemberId, messageMemberId);
                            return false;
                        }
                    }
                    break;

                // 기타 메시지는 세션 소유권만 확인하면 충분
                default:
                    break;
            }

            return true;

        } catch (Exception e) {
            log.error("🔒 메시지 권한 검증 중 오류: {} (타입: {})", sessionKey, messageType, e);
            return false;
        }
    }

    /**
     * 토큰 갱신 처리 (기존 JwtTokenProvider 사용)
     */
    private void handleTokenRefresh(WebSocketSession socket, String sessionKey,
                                   Map<String, Object> messageData, Long authenticatedMemberId) {
        String newAccessToken = (String) messageData.get("accessToken");

        if (newAccessToken == null || newAccessToken.isEmpty()) {
            log.warn("🔒 토큰 갱신 실패: 토큰이 비어있음 - SessionKey: {}", sessionKey);
            sendErrorMessage(socket, "TOKEN_MISSING", "새로운 토큰이 제공되지 않았습니다");
            return;
        }

        try {
            // 기존 JwtTokenProvider로 새 토큰 검증
            if (!jwtTokenProvider.validateMemberToken(newAccessToken)) {
                log.warn("🔒 토큰 갱신 실패: 토큰 검증 실패 - SessionKey: {}", sessionKey);
                sendErrorMessage(socket, "TOKEN_INVALID", "유효하지 않은 토큰입니다");
                return;
            }

            // 토큰에서 회원 정보 추출
            Map<String, Object> memberClaims = jwtTokenProvider.getMemberClaims(newAccessToken);
            Long tokenMemberId = ((Number) memberClaims.get("memberId")).longValue();

            // 회원ID 일치 확인
            if (!authenticatedMemberId.equals(tokenMemberId)) {
                log.warn("🔒 토큰 갱신 실패: 회원ID 불일치 - 기존: {}, 신규: {}",
                        authenticatedMemberId, tokenMemberId);
                sendErrorMessage(socket, "TOKEN_MEMBER_MISMATCH", "토큰의 회원 정보가 일치하지 않습니다");
                return;
            }

            // 소켓 속성 업데이트
            socket.getAttributes().put("authToken", newAccessToken);
            socket.getAttributes().put("userKey", memberClaims.get("userKey"));
            socket.getAttributes().put("email", memberClaims.get("email"));

            log.info("✅ 토큰 갱신 성공 - SessionKey: {}, MemberId: {}", sessionKey, authenticatedMemberId);

            // 갱신 성공 응답
            Map<String, Object> response = Map.of(
                "type", "TOKEN_REFRESHED",
                "sessionKey", sessionKey,
                "success", true,
                "timestamp", System.currentTimeMillis()
            );

            sendMessage(socket, response);

        } catch (Exception e) {
            log.error("❌ 토큰 갱신 중 오류: {} - {}", sessionKey, e.getMessage());
            sendErrorMessage(socket, "TOKEN_REFRESH_ERROR", "토큰 갱신 중 오류가 발생했습니다");
        }
    }

    /**
     * CONNECT 메시지 처리 (인증 강화)
     */
    private void handleConnectMessage(WebSocketSession socket, String sessionKey,
                                     Map<String, Object> messageData, Long authenticatedMemberId) throws Exception {
        String contactName = (String) messageData.get("contactName");
        Boolean isReconnect = (Boolean) messageData.getOrDefault("reconnect", false);
        Long memorialId = messageData.get("memorialId") != null ?
                Long.valueOf(messageData.get("memorialId").toString()) : null;
        Long callerId = messageData.get("callerId") != null ?
                Long.valueOf(messageData.get("callerId").toString()) : null;

        // CONNECT 메시지의 회원ID 검증 (추가 보안)
        if (callerId != null && !authenticatedMemberId.equals(callerId)) {
            log.warn("🔒 CONNECT 메시지 회원ID 불일치 - 토큰: {}, 메시지: {}",
                    authenticatedMemberId, callerId);
            sendErrorMessage(socket, "MEMBER_ID_MISMATCH", "토큰과 메시지의 회원 정보가 일치하지 않습니다");
            return;
        }

        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        boolean sessionRecovered = false;

        if (session != null && isReconnect) {
            // 기존 세션 복구 - 소유권 재확인
            if (!authenticatedMemberId.equals(session.getCallerId())) {
                log.warn("🔒 세션 복구 권한 없음 - 토큰회원ID: {}, 세션회원ID: {}",
                        authenticatedMemberId, session.getCallerId());
                sendErrorMessage(socket, "SESSION_ACCESS_DENIED", "해당 세션에 접근할 권한이 없습니다");
                return;
            }

            session.handleReconnect(socket.getId());
            sessionManager.saveSession(session);
            sessionRecovered = true;
            log.info("🔄 인증된 세션 복구: {} (재연결 횟수: {}, 회원ID: {})",
                    sessionKey, session.getReconnectCount(), authenticatedMemberId);
        } else {
            // 새 세션 생성 또는 기존 세션 업데이트
            if (session == null) {
                session = sessionManager.createSession(contactName, memorialId, authenticatedMemberId);
                log.info("🆕 새 인증 세션 생성: {} (회원ID: {})", sessionKey, authenticatedMemberId);
            }

            session.setWebSocketConnection(socket.getId());
            sessionManager.saveSession(session);
        }

        // 연결 완료 응답
        Map<String, Object> response = Map.of(
            "type", "CONNECTED",
            "sessionKey", sessionKey,
            "contactName", session.getContactName(),
            "reconnected", sessionRecovered,
            "sessionAge", session.getAgeInMinutes(),
            "ttlRemaining", session.getRemainingTtlSeconds(),
            "reconnectCount", session.getReconnectCount(),
            "authenticated", true,
            "memberId", authenticatedMemberId
        );

        sendMessage(socket, response);

        log.info("✅ 인증된 CONNECT 처리 완료: {} (복구: {}, 회원ID: {})",
                sessionKey, sessionRecovered, authenticatedMemberId);
    }

    // 기존 메서드들 (변경사항 없음)
    private void handleHeartbeatResponse(String sessionKey, Map<String, Object> messageData) {
        boolean extended = sessionManager.extendSessionTtl(sessionKey);
        if (extended) {
            log.debug("💓 인증된 하트비트 응답 처리: {} (TTL 갱신)", sessionKey);
        } else {
            log.warn("💔 하트비트 응답 실패: {} (세션 없음)", sessionKey);
        }
    }

    private void handleVideoUploadComplete(String sessionKey, Map<String, Object> messageData) {
        String filePath = (String) messageData.get("filePath");

        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        if (session != null) {
            session.setProcessing(filePath);
            sessionManager.saveSession(session);

            log.info("📹 인증된 영상 업로드 완료: {} (파일: {})", sessionKey, filePath);
        }
    }

    private void handleDisconnectMessage(String sessionKey, Map<String, Object> messageData) throws Exception {
        String reason = (String) messageData.getOrDefault("reason", "UNKNOWN");

        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        if (session != null) {
            if ("USER_ACTION".equals(reason)) {
                sessionManager.deleteSession(sessionKey);
                log.info("🚪 인증된 사용자 종료: {} (사유: {})", sessionKey, reason);
            } else {
                session.clearWebSocketConnection();
                sessionManager.saveSession(session);
                log.info("🔄 인증된 연결 해제: {} (사유: {}, 세션 유지)", sessionKey, reason);
            }
        }

        WebSocketSession socket = activeConnections.get(session.getSocketId());
        if (socket != null && socket.isOpen()) {
            socket.close(CloseStatus.NORMAL);
        }
    }

    // ... 기존의 다른 handle 메서드들은 동일하게 유지 ...
    private void handleClientStateChange(String sessionKey, Map<String, Object> messageData) {
        String newStateStr = (String) messageData.get("newState");
        String reason = (String) messageData.getOrDefault("reason", "CLIENT_REQUEST");

        try {
            VideoCallFlowState newState = VideoCallFlowState.valueOf(newStateStr);
            boolean success = flowManager.transitionToState(sessionKey, newState);

            if (success) {
                log.info("🔄 클라이언트 상태 변경 성공: {} -> {}", sessionKey, newState);
            } else {
                log.warn("❌ 클라이언트 상태 변경 실패: {} -> {}", sessionKey, newState);
                sendErrorMessage(getSocketBySessionKey(sessionKey), "INVALID_STATE_TRANSITION", "잘못된 상태 전환입니다");
            }
        } catch (IllegalArgumentException e) {
            log.error("❌ 잘못된 상태값: {}", newStateStr);
            sendErrorMessage(getSocketBySessionKey(sessionKey), "INVALID_STATE", "잘못된 상태값입니다");
        }
    }

    private void handlePermissionStatus(String sessionKey, Map<String, Object> messageData) {
        Boolean cameraGranted = (Boolean) messageData.get("cameraGranted");
        Boolean microphoneGranted = (Boolean) messageData.get("microphoneGranted");

        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        if (session != null) {
            session.addMetadata("cameraGranted", cameraGranted);
            session.addMetadata("microphoneGranted", microphoneGranted);
            sessionManager.saveSession(session);

            if (Boolean.TRUE.equals(cameraGranted) && Boolean.TRUE.equals(microphoneGranted)) {
                flowManager.transitionToState(sessionKey, VideoCallFlowState.PERMISSION_GRANTED);
            }

            log.info("📹🎤 권한 상태 업데이트: {} (카메라: {}, 마이크: {})", sessionKey, cameraGranted, microphoneGranted);
        }
    }

    private void handleDeviceInfo(String sessionKey, Map<String, Object> messageData) {
        String deviceId = (String) messageData.get("deviceId");
        String deviceTypeStr = (String) messageData.get("deviceType");

        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        if (session != null) {
            session.addMetadata("clientDeviceId", deviceId);
            session.addMetadata("clientDeviceType", deviceTypeStr);
            session.addMetadata("userAgent", messageData.get("userAgent"));
            session.addMetadata("screenResolution", messageData.get("screenResolution"));
            sessionManager.saveSession(session);

            log.info("📱 디바이스 정보 업데이트: {} (ID: {}, 타입: {})", sessionKey, deviceId, deviceTypeStr);
        }
    }

    private void handleWaitingVideoStarted(String sessionKey, Map<String, Object> messageData) {
        flowManager.transitionToState(sessionKey, VideoCallFlowState.WAITING_PLAYING);
        log.info("🎬 대기영상 재생 시작: {}", sessionKey);
    }

    private void handleWaitingVideoError(String sessionKey, Map<String, Object> messageData) {
        String error = (String) messageData.get("error");
        flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR_PROCESSING);
        log.error("❌ 대기영상 오류: {} (오류: {})", sessionKey, error);
    }

    private void handleRecordingReady(String sessionKey, Map<String, Object> messageData) {
        log.info("🎙️ 녹화 준비 완료: {}", sessionKey);
        flowManager.transitionToState(sessionKey, VideoCallFlowState.RECORDING_COUNTDOWN);
    }

    private void handleRecordingStarted(String sessionKey, Map<String, Object> messageData) {
        flowManager.transitionToState(sessionKey, VideoCallFlowState.RECORDING_ACTIVE);
        log.info("🔴 녹화 시작: {}", sessionKey);
    }

    private void handleRecordingStopped(String sessionKey, Map<String, Object> messageData) {
        Integer duration = (Integer) messageData.get("duration");
        flowManager.transitionToState(sessionKey, VideoCallFlowState.RECORDING_COMPLETE);
        log.info("⏹️ 녹화 중지: {} (길이: {}초)", sessionKey, duration);
    }

    private void handleRecordingError(String sessionKey, Map<String, Object> messageData) {
        String error = (String) messageData.get("error");
        flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR_PROCESSING);
        log.error("❌ 녹화 오류: {} (오류: {})", sessionKey, error);
    }

    private void handleResponseVideoStarted(String sessionKey, Map<String, Object> messageData) {
        flowManager.transitionToState(sessionKey, VideoCallFlowState.RESPONSE_PLAYING);
        log.info("🎬 응답영상 재생 시작: {}", sessionKey);
    }

    private void handleResponseVideoEnded(String sessionKey, Map<String, Object> messageData) {
        flowManager.transitionToState(sessionKey, VideoCallFlowState.RESPONSE_COMPLETE);
        log.info("✅ 응답영상 재생 완료: {}", sessionKey);
        flowManager.transitionToState(sessionKey, VideoCallFlowState.WAITING_PLAYING);
    }

    private void handleResponseVideoError(String sessionKey, Map<String, Object> messageData) {
        String error = (String) messageData.get("error");
        flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR_PROCESSING);
        log.error("❌ 응답영상 오류: {} (오류: {})", sessionKey, error);
    }

    // 기존 유틸리티 메서드들
    private WebSocketSession getSocketBySessionKey(String sessionKey) {
        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        if (session != null && session.getSocketId() != null) {
            return activeConnections.get(session.getSocketId());
        }
        return null;
    }

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

    @Override
    public void afterConnectionClosed(WebSocketSession socket, CloseStatus status) throws Exception {
        String sessionKey = (String) socket.getAttributes().get("sessionKey");
        Long memberId = (Long) socket.getAttributes().get("memberId");

        log.info("🔌 인증된 WebSocket 연결 종료: {} (상태: {}, 세션: {}, 회원ID: {})",
                socket.getId(), status.getCode(), sessionKey, memberId);

        activeConnections.remove(socket.getId());

        if (sessionKey != null) {
            sessionManager.unmapSocket(socket.getId());

            if (status.getCode() != CloseStatus.NORMAL.getCode()) {
                log.info("🔄 비정상 종료 - 세션 유지: {} (재연결 가능)", sessionKey);
            } else {
                log.info("✅ 정상 종료: {}", sessionKey);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession socket, Throwable exception) throws Exception {
        String sessionKey = (String) socket.getAttributes().get("sessionKey");
        Long memberId = (Long) socket.getAttributes().get("memberId");

        log.error("🔥 인증된 WebSocket 전송 오류: {} (세션: {}, 회원ID: {})",
                 exception.getMessage(), sessionKey, memberId);

        if (socket.isOpen()) {
            socket.close(CloseStatus.SERVER_ERROR);
        }
    }

    public int getActiveConnectionCount() {
        return activeConnections.size();
    }

    private void sendMessage(WebSocketSession socket, Map<String, Object> message) throws Exception {
        if (socket.isOpen()) {
            String jsonMessage = objectMapper.writeValueAsString(message);
            socket.sendMessage(new TextMessage(jsonMessage));
        }
    }

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