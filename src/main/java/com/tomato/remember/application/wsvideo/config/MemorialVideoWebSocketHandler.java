package com.tomato.remember.application.wsvideo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomato.remember.application.wsvideo.code.DeviceType;
import com.tomato.remember.application.wsvideo.code.VideoCallFlowState;
import com.tomato.remember.application.wsvideo.dto.MemorialVideoSession;
import com.tomato.remember.common.security.JwtTokenProvider;
import com.tomato.remember.application.wsvideo.service.MultiDeviceManager;
import com.tomato.remember.application.wsvideo.service.VideoCallFlowManager;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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
 * Memorial Video Call WebSocket 핸들러 - 기존 JwtTokenProvider 사용 인터셉터에서 1차 인증 후, 핸들러에서 2차 검증 수행
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
    private final Map<String, StateChangeTracker> stateChangeTrackers = new ConcurrentHashMap<>();
    // 인증 타임아웃 관리
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<String, ScheduledFuture<?>> authTimeouts = new ConcurrentHashMap<>();

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
        String sessionKey = (String) socket.getAttributes().get("sessionKey");
        String deviceType = (String) socket.getAttributes().get("deviceType");

        log.info("🔗 WebSocket 연결 설정: {} (디바이스: {}, SocketId: {})",
            sessionKey, deviceType, socket.getId());

        try {
            // 기존 연결 정리 (재연결 시)
            cleanupExistingConnection(sessionKey);

            // 새 연결 등록
            activeConnections.put(socket.getId(), socket);
            sessionManager.mapSocketToSession(socket.getId(), sessionKey);

            // 🔒 인증 타임아웃 설정 (5초)
            scheduleAuthTimeout(socket);

            log.info("✅ WebSocket 연결 등록 완료: {} → {} (인증 대기 중)", sessionKey, socket.getId());

        } catch (Exception e) {
            log.error("❌ WebSocket 연결 설정 실패: {} (SocketId: {})", sessionKey, socket.getId(), e);
            socket.close(CloseStatus.SERVER_ERROR.withReason("Connection setup failed"));
        }
    }

    /**
     * 🔒 인증 타임아웃 설정 (5초)
     */
    private void scheduleAuthTimeout(WebSocketSession socket) {
        String sessionKey = (String) socket.getAttributes().get("sessionKey");

        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            try {
                // 인증 상태 정확히 확인
                Boolean authenticated = (Boolean) socket.getAttributes().get("authenticated");
                boolean isAuthenticated = Boolean.TRUE.equals(authenticated);

                log.debug("⏰ 인증 타임아웃 체크 - SessionKey: {}, authenticated: {}, socket.isOpen(): {}",
                    sessionKey, isAuthenticated, socket.isOpen());

                if (! isAuthenticated && socket.isOpen()) {
                    log.warn("⏰ 인증 타임아웃: {} (5초 내 인증 실패)", sessionKey);
                    sendErrorMessage(socket, "AUTH_TIMEOUT", "인증 시간이 초과되었습니다");
                    socket.close(CloseStatus.POLICY_VIOLATION.withReason("Authentication timeout"));
                } else if (isAuthenticated) {
                    log.debug("✅ 인증 완료된 연결 - 타임아웃 불필요: {}", sessionKey);
                } else {
                    log.debug("🔌 이미 닫힌 연결 - 타임아웃 불필요: {}", sessionKey);
                }
            } catch (Exception e) {
                log.error("❌ 인증 타임아웃 처리 오류: {}", sessionKey, e);
            }
        }, 5, TimeUnit.SECONDS);

        authTimeouts.put(socket.getId(), timeoutTask);
        log.debug("⏰ 인증 타임아웃 설정: {} (5초)", sessionKey);
    }

    @Override
    protected void handleTextMessage(WebSocketSession socket, TextMessage message) throws Exception {
        String sessionKey = (String) socket.getAttributes().get("sessionKey");
        Boolean authenticated = (Boolean) socket.getAttributes().get("authenticated");

        try {
            Map<String, Object> messageData = objectMapper.readValue(message.getPayload(), Map.class);
            String messageType = (String) messageData.get("type");

            log.debug("📨 메시지 수신: {} (타입: {}, 인증상태: {})", sessionKey, messageType, authenticated);

            // 🔒 AUTH 메시지 처리 (인증되지 않은 상태에서만)
            if ("AUTH".equals(messageType) && ! Boolean.TRUE.equals(authenticated)) {
                handleAuthMessage(socket, messageData);
                return;
            }

            // 🔒 인증되지 않은 연결의 다른 메시지는 거부
            if (! Boolean.TRUE.equals(authenticated)) {
                log.warn("🔒 미인증 연결에서 메시지 수신: {} (타입: {})", sessionKey, messageType);
                sendErrorMessage(socket, "AUTHENTICATION_REQUIRED", "인증이 필요합니다");
                return;
            }

            // 기존 메시지 처리 로직
            handleAuthenticatedMessage(socket, sessionKey, messageType, messageData);

        } catch (Exception e) {
            log.error("❌ 메시지 처리 오류: {} - {}", sessionKey, e.getMessage());
            sendErrorMessage(socket, "MESSAGE_PROCESSING_ERROR", "메시지 처리 중 오류가 발생했습니다");
        }
    }

    private void handleAuthenticatedMessage(WebSocketSession socket, String sessionKey,
        String messageType, Map<String, Object> messageData) throws Exception {

        Long authenticatedMemberId = (Long) socket.getAttributes().get("memberId");

        // 메시지별 권한 검증
        if (!validateMessagePermissions(messageType, messageData, authenticatedMemberId, sessionKey)) {
            sendErrorMessage(socket, "PERMISSION_DENIED", "해당 작업에 대한 권한이 없습니다");
            return;
        }

        // ✅ 간소화된 메시지 처리 (8개만)
        switch (messageType) {
            case "HEARTBEAT_RESPONSE" -> handleHeartbeatResponse(sessionKey, messageData);
            case "CLIENT_STATE_CHANGE" -> handleClientStateChange(sessionKey, messageData);
            case "RECORDING_STARTED" -> handleRecordingStarted(sessionKey, messageData);
            case "RECORDING_STOPPED" -> handleRecordingStopped(sessionKey, messageData);
            case "RECORDING_ERROR" -> handleRecordingError(sessionKey, messageData);
            case "VIDEO_UPLOAD_COMPLETE" -> handleVideoUploadComplete(sessionKey, messageData);
            case "DEVICE_INFO" -> handleDeviceInfo(sessionKey, messageData);
            case "TOKEN_REFRESH" -> handleTokenRefresh(socket, sessionKey, messageData, authenticatedMemberId);
            case "DISCONNECT" -> handleDisconnectMessage(sessionKey, messageData);

            // 간소화된 새 핸들러들
            case "WAITING_VIDEO_EVENT" -> handleWaitingVideoEvent(sessionKey, messageData);
            case "RESPONSE_VIDEO_EVENT" -> handleResponseVideoEvent(sessionKey, messageData);

            default -> {
                log.warn("⚠️ 알 수 없는 메시지 타입: {} (세션: {})", messageType, sessionKey);
                sendErrorMessage(socket, "UNKNOWN_MESSAGE_TYPE", "알 수 없는 메시지 타입입니다");
            }
        }
    }

    private void handleWaitingVideoEvent(String sessionKey, Map<String, Object> messageData) {
        String eventType = (String) messageData.get("eventType"); // "started", "error"

        switch (eventType) {
            case "started" -> {
                log.info("🎬 대기영상 시작: {}", sessionKey);
            }
            case "error" -> {
                String error = (String) messageData.get("error");
                log.error("❌ 대기영상 오류: {} - {}", sessionKey, error);
                flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR);
            }
        }
    }

    private void handleResponseVideoEvent(String sessionKey, Map<String, Object> messageData) {
        String eventType = (String) messageData.get("eventType"); // "started", "ended", "error"

        switch (eventType) {
            case "started" -> {
                log.info("🎬 응답영상 재생 시작: {}", sessionKey);
                // 이미 RESPONSE_PLAYING 상태이므로 추가 처리 불필요
            }
            case "ended" -> {
                log.info("✅ 응답영상 완료 → 대기 상태로: {}", sessionKey);
                flowManager.transitionToState(sessionKey, VideoCallFlowState.WAITING);
            }
            case "error" -> {
                String error = (String) messageData.get("error");
                log.error("❌ 응답영상 오류: {} - {}", sessionKey, error);
                flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR);
            }
        }
    }

    /**
     * 🔒 AUTH 메시지 처리 (초상세 디버깅 버전)
     */
    private void handleAuthMessage(WebSocketSession socket, Map<String, Object> messageData) throws IOException {
        String sessionKey = (String) socket.getAttributes().get("sessionKey");
        log.info("🔒 AUTH 메시지 처리 시작 - SessionKey: {}", sessionKey);

        try {
            // 1. 기본 검증
            String token = (String) messageData.get("token");
            String messageSessionKey = (String) messageData.get("sessionKey");
            String deviceTypeStr = (String) messageData.get("deviceType");

            if (token == null || token.trim().isEmpty()) {
                log.warn("🔒 AUTH 실패: 토큰 누락 - SessionKey: {}", sessionKey);
                sendErrorMessage(socket, "TOKEN_MISSING", "토큰이 제공되지 않았습니다");
                socket.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            // 2. 세션키 일치 확인
            if (messageSessionKey != null && ! sessionKey.equals(messageSessionKey)) {
                log.warn("🔒 AUTH 실패: 세션키 불일치 - URL: {}, Message: {}", sessionKey, messageSessionKey);
                sendErrorMessage(socket, "SESSION_KEY_MISMATCH", "세션키가 일치하지 않습니다");
                socket.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            // 3. JWT 토큰 검증
            if (! jwtTokenProvider.validateMemberToken(token)) {
                log.warn("🔒 AUTH 실패: 토큰 검증 실패 - SessionKey: {}", sessionKey);
                sendErrorMessage(socket, "TOKEN_INVALID", "유효하지 않은 토큰입니다");
                socket.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            // 4. 토큰에서 회원 정보 추출
            Map<String, Object> memberClaims = jwtTokenProvider.getMemberClaims(token);
            Long memberId = ((Number) memberClaims.get("memberId")).longValue();
            String userKey = (String) memberClaims.get("userKey");
            String email = (String) memberClaims.get("email");

            log.info("📋 토큰에서 추출된 회원 정보 - ID: {}, Email: {}", memberId, email);

            // 5. 세션 존재 및 소유권 확인
            if (! validateSessionAccess(sessionKey, memberId)) {
                log.warn("🔒 AUTH 실패: 세션 접근 권한 없음 - SessionKey: {}, MemberId: {}", sessionKey, memberId);
                sendErrorMessage(socket, "SESSION_ACCESS_DENIED", "해당 세션에 접근할 권한이 없습니다");
                socket.close(CloseStatus.NOT_ACCEPTABLE);
                return;
            }

            log.info("🎉 인증 검증 모두 통과 - SessionKey: {}, MemberId: {}", sessionKey, memberId);

            // 6. 🎉 인증 성공 - 속성 업데이트
            socket.getAttributes().put("authenticated", true);
            socket.getAttributes().put("memberId", memberId);
            socket.getAttributes().put("userKey", userKey);
            socket.getAttributes().put("authToken", token);
            if (deviceTypeStr != null) {
                socket.getAttributes().put("deviceType", deviceTypeStr);
            }

            log.info("✅ WebSocket 속성 업데이트 완료 - authenticated: true");

            // 7. 인증 타임아웃 취소
            cancelAuthTimeout(socket);
            log.info("✅ 인증 타임아웃 취소 완료");

            // 8. 세션 업데이트
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session != null) {
                session.setWebSocketConnection(socket.getId());
                // 디바이스 정보 업데이트
                DeviceType deviceType = parseDeviceType(deviceTypeStr);
                session.setDeviceInfo(deviceType, socket.getId(), true);
                sessionManager.saveSession(session);
                log.info("✅ 세션 정보 업데이트 완료");
            } else {
                log.warn("⚠️ 세션 업데이트 실패: 세션을 찾을 수 없음 - {}", sessionKey);
            }

            // 9. 인증 성공 응답
            Map<String, Object> response = Map.of(
                "type", "AUTH_SUCCESS",
                "sessionKey", sessionKey,
                "memberId", memberId,
                "deviceType", deviceTypeStr != null ? deviceTypeStr : "UNKNOWN",
                "timestamp", System.currentTimeMillis(),
                "message", "인증이 완료되었습니다"
            );

            sendMessage(socket, response);
            log.info("✅ AUTH_SUCCESS 메시지 전송 완료");

            log.info("🎉 WebSocket 인증 성공: {} (회원ID: {}, 이메일: {}, 디바이스: {})",
                sessionKey, memberId, email, deviceTypeStr);
            if (session != null) {
                flowManager.transitionToState(sessionKey, VideoCallFlowState.WAITING);
            }

        } catch (Exception e) {
            log.error("❌ AUTH 메시지 처리 중 오류: {} - {}", sessionKey, e.getMessage(), e);
            sendErrorMessage(socket, "AUTH_PROCESSING_ERROR", "인증 처리 중 오류가 발생했습니다");
            socket.close(CloseStatus.SERVER_ERROR);
        }
    }

    private DeviceType parseDeviceType(String deviceTypeStr) {
        if (deviceTypeStr == null) {
            return DeviceType.WEB;
        }

        try {
            return DeviceType.valueOf(deviceTypeStr);
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 디바이스 타입: {}, WEB으로 기본값 설정", deviceTypeStr);
            return DeviceType.WEB;
        }
    }

    /**
     * 🔒 인증 타임아웃 취소
     */
    private void cancelAuthTimeout(WebSocketSession socket) {
        ScheduledFuture<?> timeoutTask = authTimeouts.remove(socket.getId());
        if (timeoutTask != null && ! timeoutTask.isDone()) {
            timeoutTask.cancel(false);
            log.debug("✅ 인증 타임아웃 취소: {}", socket.getId());
        } else {
            log.debug("⚠️ 인증 타임아웃 작업을 찾을 수 없음: {}", socket.getId());
        }
    }

    /**
     * 메시지별 권한 검증
     */
    private boolean validateMessagePermissions(String messageType, Map<String, Object> messageData,
        Long authenticatedMemberId, String sessionKey) {
        try {
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session == null) {
                return false;
            }

            if (! authenticatedMemberId.equals(session.getCallerId())) {
                return false;
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
            if (! jwtTokenProvider.validateMemberToken(newAccessToken)) {
                log.warn("🔒 토큰 갱신 실패: 토큰 검증 실패 - SessionKey: {}", sessionKey);
                sendErrorMessage(socket, "TOKEN_INVALID", "유효하지 않은 토큰입니다");
                return;
            }

            // 토큰에서 회원 정보 추출
            Map<String, Object> memberClaims = jwtTokenProvider.getMemberClaims(newAccessToken);
            Long tokenMemberId = ((Number) memberClaims.get("memberId")).longValue();

            // 회원ID 일치 확인
            if (! authenticatedMemberId.equals(tokenMemberId)) {
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
     * 세션 접근 권한 검증 (초상세 디버깅 버전)
     */
    private boolean validateSessionAccess(String sessionKey, Long memberId) {
        log.info("validateSessionAccess 시작 - sessionKey: {}, memberId: {}", sessionKey, memberId);

        try {
            // 1. 기본 파라미터 검증
            log.debug("  Step 1: 파라미터 검증");
            if (sessionKey == null || sessionKey.trim().isEmpty()) {
                log.warn("🔒 세션키가 비어있음");
                return false;
            }
            log.debug("  ✅ sessionKey 정상: {}", sessionKey);

            if (memberId == null) {
                log.warn("🔒 회원ID가 null");
                return false;
            }
            log.debug("  ✅ memberId 정상: {}", memberId);

            // 2. 세션 조회
            log.debug("  Step 2: 세션 조회 시작");
            MemorialVideoSession session = null;
            try {
                session = sessionManager.getSession(sessionKey);
                log.debug("  ✅ sessionManager.getSession() 완료");
            } catch (Exception sessionGetError) {
                log.error("❌ sessionManager.getSession() 실패", sessionGetError);
                return false;
            }

            if (session == null) {
                log.warn("🔒 세션이 존재하지 않음 - SessionKey: {}", sessionKey);
                return false;
            }
            log.debug("  ✅ 세션 조회 성공: {}", session.getClass().getSimpleName());

            // 3. 세션 기본 정보 로깅
            log.debug("  Step 3: 세션 정보 검증");
            try {
                log.debug("    세션키: {}", session.getSessionKey());
                log.debug("    연락처: {}", session.getContactName());
                log.debug("    상태: {}", session.getStatus());
                log.debug("    생성시간: {}", session.getCreatedAt());
                log.debug("    마지막활동: {}", session.getLastActivity());
            } catch (Exception infoError) {
                log.error("❌ 세션 기본 정보 조회 실패", infoError);
                return false;
            }

            // 4. FlowState 검증
            log.debug("  Step 4: FlowState 검증");
            try {
                VideoCallFlowState flowState = session.getFlowState();
                log.debug("    FlowState: {}", flowState);
            } catch (Exception flowStateError) {
                log.error("❌ FlowState 조회 실패", flowStateError);
                return false;
            }

            // 5. 세션 만료 체크 (매우 세분화)
            log.debug("  Step 5: 만료 체크 시작");
            try {
                // 5-1. createdAt 먼저 체크
                LocalDateTime createdAt = session.getCreatedAt();
                log.debug("    createdAt: {}", createdAt);

                if (createdAt == null) {
                    log.warn("🔒 세션의 createdAt이 null - SessionKey: {}", sessionKey);
                    return false;
                }

                // 5-2. 현재 시간 체크
                LocalDateTime now = LocalDateTime.now();
                log.debug("    현재시간: {}", now);

                // 5-3. 시간 차이 계산
                long ageMinutes = ChronoUnit.MINUTES.between(createdAt, now);
                log.debug("    세션 나이: {}분", ageMinutes);

                // 5-4. TTL 체크
                long ttlMinutes = MemorialVideoSession.getTtlSeconds() / 60;
                log.debug("    TTL 제한: {}분", ttlMinutes);

                boolean expired = ageMinutes > ttlMinutes;
                log.debug("    만료 여부: {}", expired);

                if (expired) {
                    log.warn("🔒 세션이 만료됨 - SessionKey: {}, Age: {}분", sessionKey, ageMinutes);
                    return false;
                }

            } catch (Exception expireCheckError) {
                log.error("❌ 세션 만료 체크 중 오류 - SessionKey: {}", sessionKey, expireCheckError);
                return false;
            }
            log.debug("  ✅ 만료 체크 완료");

            // 6. callerId 체크 (매우 세분화)
            log.debug("  Step 6: callerId 체크 시작");
            Long sessionCallerId = null;
            try {
                sessionCallerId = session.getCallerId();
                log.debug("    session.getCallerId(): {}", sessionCallerId);
                log.debug("    sessionCallerId 타입: {}",
                    sessionCallerId != null ? sessionCallerId.getClass().getName() : "null");
            } catch (Exception callerIdGetError) {
                log.error("❌ session.getCallerId() 호출 실패", callerIdGetError);
                return false;
            }

            if (sessionCallerId == null) {
                log.warn("🔒 세션의 호출자 ID가 null - SessionKey: {}", sessionKey);
                return false;
            }
            log.debug("  ✅ sessionCallerId 정상: {}", sessionCallerId);

            // 7. 회원ID 일치 확인 (매우 세분화)
            log.debug("  Step 7: 회원ID 일치 확인");
            log.debug("    토큰 memberId: {} (타입: {})", memberId, memberId.getClass().getName());
            log.debug("    세션 callerId: {} (타입: {})", sessionCallerId, sessionCallerId.getClass().getName());

            boolean idsMatch = false;
            try {
                idsMatch = memberId.equals(sessionCallerId);
                log.debug("    equals() 결과: {}", idsMatch);
            } catch (Exception equalsError) {
                log.error("❌ memberId.equals() 호출 실패", equalsError);
                return false;
            }

            if (! idsMatch) {
                log.warn("🔒 세션 소유권 불일치 - SessionKey: {}, 토큰회원ID: {}, 세션회원ID: {}",
                    sessionKey, memberId, sessionCallerId);
                return false;
            }

            log.info("✅ 세션 접근 권한 검증 성공 - SessionKey: {}, 회원ID: {}", sessionKey, memberId);
            return true;

        } catch (Exception e) {
            log.error("❌ validateSessionAccess 최상위 예외 발생 - SessionKey: {}, MemberId: {}", sessionKey, memberId, e);
            // 스택 트레이스도 출력
            log.error("❌ Stack trace:", e);
            return false;
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
        if (callerId != null && ! authenticatedMemberId.equals(callerId)) {
            log.warn("🔒 CONNECT 메시지 회원ID 불일치 - 토큰: {}, 메시지: {}",
                authenticatedMemberId, callerId);
            sendErrorMessage(socket, "MEMBER_ID_MISMATCH", "토큰과 메시지의 회원 정보가 일치하지 않습니다");
            return;
        }

        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        boolean sessionRecovered = false;

        if (session != null && isReconnect) {
            // 기존 세션 복구 - 소유권 재확인
            if (! authenticatedMemberId.equals(session.getCallerId())) {
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
            session.setSavedFilePath(filePath);
            sessionManager.saveSession(session);
            log.info("📤 업로드 완료: {} ({})", sessionKey, filePath);

            // 업로드 완료되면 외부 API 호출은 별도 서비스에서 처리
            // 여기서는 상태만 유지 (PROCESSING 상태 계속)
        }
    }

    private void handleDisconnectMessage(String sessionKey, Map<String, Object> messageData) throws Exception {
        String reason = (String) messageData.getOrDefault("reason", "UNKNOWN");

        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        if (session != null) {
            String socketId = session.getSocketId();

            if ("USER_ACTION".equals(reason)) {
                sessionManager.deleteSession(sessionKey);
                log.info("🚪 인증된 사용자 종료: {} (사유: {})", sessionKey, reason);
            } else {
                session.clearWebSocketConnection();
                sessionManager.saveSession(session);
                log.info("🔄 인증된 연결 해제: {} (사유: {}, 세션 유지)", sessionKey, reason);
            }

            // 올바른 소켓 참조로 연결 종료
            if (socketId != null) {
                WebSocketSession socket = activeConnections.get(socketId);
                if (socket != null && socket.isOpen()) {
                    socket.close(CloseStatus.NORMAL);
                }
            }
        }
    }

    private void handleClientStateChange(String sessionKey, Map<String, Object> messageData) {
        String newStateStr = (String) messageData.get("newState");
        String reason = (String) messageData.getOrDefault("reason", "CLIENT_REQUEST");

        try {
            VideoCallFlowState newState = VideoCallFlowState.valueOf(newStateStr);

            if ("RESPONSE_VIDEO_ENDED".equals(reason) && newState == VideoCallFlowState.WAITING) {
                log.info("🔄 응답영상 종료로 인한 대기 상태 전환: {}", sessionKey);
            }

            // 중복 감지 및 throttle 체크
            if (!shouldProcessStateChange(sessionKey, newState, reason)) {
                return;
            }

            boolean success = flowManager.transitionToState(sessionKey, newState);

            if (success) {
                log.info("🔄 클라이언트 상태 변경 성공: {} -> {} (사유: {})", sessionKey, newState, reason);
            } else {
                log.warn("❌ 클라이언트 상태 변경 실패: {} -> {} (사유: {})", sessionKey, newState, reason);
                sendErrorMessage(getSocketBySessionKey(sessionKey), "INVALID_STATE_TRANSITION", "잘못된 상태 전환입니다");
            }
        } catch (IllegalArgumentException e) {
            log.error("❌ 잘못된 상태값: {}", newStateStr);
            sendErrorMessage(getSocketBySessionKey(sessionKey), "INVALID_STATE", "잘못된 상태값입니다");
        }
    }

    //상태 변경 추적 클래스
    private static class StateChangeTracker {
        Long lastStateChangeTime;
        VideoCallFlowState lastRequestedState;
        int requestCount = 0;
    }

    private boolean shouldProcessStateChange(String sessionKey, VideoCallFlowState newState, String reason) {
    // 1. 기본 중복 체크
        MemorialVideoSession currentSession = sessionManager.getSession(sessionKey);
        if (currentSession != null && currentSession.getFlowState() == newState) {
            log.warn("⚠️ 중복 상태 변경 요청 무시: {} (이미 {} 상태)", sessionKey, newState);
            return false;
        }

        // 2. 빠른 연속 요청 throttle (1초 내 같은 상태 요청 방지)
        StateChangeTracker tracker = stateChangeTrackers.computeIfAbsent(sessionKey,
            k -> new StateChangeTracker());

        long now = System.currentTimeMillis();
        if (tracker.lastStateChangeTime != null &&
            tracker.lastRequestedState == newState &&
            (now - tracker.lastStateChangeTime) < 1000) {

            log.warn("⚠️ 빠른 연속 상태 변경 요청 무시: {} -> {} ({}ms 전에 동일 요청)",
                    sessionKey, newState, now - tracker.lastStateChangeTime);
            return false;
        }

        // 3. 추적 정보 업데이트
        tracker.lastStateChangeTime = now;
        tracker.lastRequestedState = newState;
        tracker.requestCount++;

        log.debug("✅ 상태 변경 요청 승인: {} -> {} (요청 #{}, 사유: {})",
                 sessionKey, newState, tracker.requestCount, reason);

        return true;
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

    private void handleRecordingStarted(String sessionKey, Map<String, Object> messageData) {

        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        if (session != null && session.getFlowState() == VideoCallFlowState.RECORDING) {
            log.info("🔴 녹화 시작 확인: {} (이미 RECORDING 상태)", sessionKey);
        } else {
            log.warn("⚠️ 녹화 시작 알림이지만 RECORDING 상태 아님: {} (현재: {})",
                    sessionKey, session != null ? session.getFlowState() : "NULL");
        }
    }

    private void handleRecordingStopped(String sessionKey, Map<String, Object> messageData) {
        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        if (session != null && session.getFlowState() == VideoCallFlowState.RECORDING) {
            log.info("⏹️ 녹화 완료 → PROCESSING 상태로 전환: {}", sessionKey);
            flowManager.transitionToState(sessionKey, VideoCallFlowState.PROCESSING);
        } else {
            log.warn("⚠️ 녹화 완료 알림이지만 RECORDING 상태 아님: {} (현재: {})",
                    sessionKey, session != null ? session.getFlowState() : "NULL");
        }
    }

    private void handleRecordingError(String sessionKey, Map<String, Object> messageData) {
        String error = (String) messageData.get("error");
        log.error("❌ 녹화 오류: {} - {}", sessionKey, error);
        flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR);
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
            // 직접 응답 URL 설정
            session.setResponseVideoUrl(videoUrl);

            // 상태 전환은 VideoCallFlowManager에서 처리
            flowManager.transitionToState(sessionKey, VideoCallFlowState.RESPONSE_PLAYING);

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

        // 추적 정보 정리
        if (sessionKey != null) {
            stateChangeTrackers.remove(sessionKey);
            log.debug("🧹 상태 변경 추적 정보 정리: {}", sessionKey);
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