package com.tomato.remember.application.wsvideo.controller;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.application.wsvideo.config.MemorialVideoSessionManager;
import com.tomato.remember.application.wsvideo.config.MemorialVideoWebSocketHandler;
import com.tomato.remember.application.wsvideo.dto.CreateSessionRequest;
import com.tomato.remember.application.wsvideo.dto.InitialDataRequest;
import com.tomato.remember.application.wsvideo.dto.MemorialVideoSession;
import com.tomato.remember.application.videocall.service.ExternalVideoApiService;
import com.tomato.remember.application.wsvideo.code.VideoCallFlowState;
import com.tomato.remember.application.wsvideo.code.DeviceType;
import com.tomato.remember.application.wsvideo.service.MultiDeviceManager;
import com.tomato.remember.application.wsvideo.service.VideoCallFlowManager;
import com.tomato.remember.application.wsvideo.service.WaitingVideoService;
import com.tomato.remember.common.util.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WebSocket 기반 영상통화 REST API 컨트롤러 - 인증 보안 강화
 * Spring Security의 @AuthenticationPrincipal을 통한 회원 인증 적용
 */
@Slf4j
@RestController
@RequestMapping("/api/ws-video")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WebSocketVideoController {

    private final MemorialVideoSessionManager sessionManager;
    private final MemorialVideoWebSocketHandler webSocketHandler;
    private final WaitingVideoService waitingVideoService;
    private final VideoCallFlowManager flowManager;
    private final MultiDeviceManager deviceManager;
    private final ExternalVideoApiService externalVideoApiService;
    private final FileStorageService fileStorageService;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 1. 새로운 세션 생성 (회원 인증 필수)
     */
    @PostMapping("/create-session")
    public ResponseEntity<?> createSession(@RequestBody CreateSessionRequest request,
                                         @RequestHeader(value = "User-Agent", required = false) String userAgent,
                                         @AuthenticationPrincipal MemberUserDetails userDetails,
                                         HttpServletRequest httpRequest) {

        Member authenticatedMember = userDetails.getMember();

        if (authenticatedMember == null) {
            log.warn("🔒 인증되지 않은 세션 생성 시도");
            return ResponseEntity.status(401).body(Map.of(
                "status", Map.of("code", "AUTH_4010", "message", "인증이 필요합니다"),
                "error", "Authentication required"
            ));
        }

        try {
            // 요청 데이터 검증
            if (!authenticatedMember.getId().equals(request.getCallerId())) {
                log.warn("🔒 세션 생성 권한 없음 - 토큰회원ID: {}, 요청회원ID: {}",
                        authenticatedMember.getId(), request.getCallerId());
                return ResponseEntity.status(403).body(Map.of(
                    "status", Map.of("code", "AUTH_4030", "message", "권한이 없습니다"),
                    "error", "Permission denied"
                ));
            }

            // 디바이스 타입 자동 감지
            DeviceType deviceType = request.getDeviceType() != null ?
                request.getDeviceType() : DeviceType.detectFromUserAgent(userAgent);

            log.info("🆕 인증된 WebSocket 세션 생성 요청 - 연락처: {}, 디바이스: {}, 회원ID: {}",
                    request.getContactName(), deviceType, authenticatedMember.getId());

            // 세션 생성 (인증된 회원 ID 사용)
            MemorialVideoSession session = sessionManager.createSession(
                request.getContactName(),
                request.getMemorialId(),
                authenticatedMember.getId() // 인증된 회원 ID 사용
            );

            // 디바이스 정보 설정
            session.setDeviceInfo(deviceType, request.getDeviceId(), true);

            // 대기영상 URL 설정
            log.info(">>>>>>>>>>>>>>>>>>>>{} {}", request.getContactKey(), deviceType.name());
            String waitingVideoUrl = waitingVideoService.getWaitingVideoUrl(
                request.getContactKey(), deviceType);
            session.setWaitingVideoUrl(waitingVideoUrl);

            // 초기 상태 설정
            session.transitionToState(VideoCallFlowState.INITIALIZING);
            sessionManager.saveSession(session);

            // 디바이스 등록
            deviceManager.registerDevice(session.getSessionKey(), request.getDeviceId(), deviceType);

            String websocketPath = deviceType.getWebSocketPath(session.getSessionKey());

            Map<String, Object> response = new HashMap<>();
            response.put("sessionKey", session.getSessionKey());
            response.put("contactName", session.getContactName());
            response.put("contactKey", request.getContactKey());
            response.put("deviceType", deviceType.name());
            response.put("deviceId", request.getDeviceId());
            response.put("waitingVideoUrl", waitingVideoUrl);
            response.put("websocketPath", websocketPath); // 토큰 포함된 WebSocket 경로
            response.put("uiLayoutType", deviceType.getUILayoutType());
            response.put("ttlSeconds", MemorialVideoSession.getTtlSeconds());
            response.put("heartbeatInterval", MemorialVideoSession.getHeartbeatIntervalSeconds());
            response.put("authenticatedMemberId", authenticatedMember.getId());
            response.put("memberName", authenticatedMember.getName());
            response.put("authMethod", "INITIAL_MESSAGE");
            response.put("authTimeout", 5000); // 5초 타임아웃
            response.put("authRequired", true);
            response.put("instructions", Map.of(
                "step1", "WebSocket 연결 후 즉시 AUTH 메시지 전송",
                "step2", "5초 내 인증하지 않으면 연결 종료",
                "authMessage", Map.of(
                    "type", "AUTH",
                    "token", "{your_access_token}",
                    "sessionKey", session.getSessionKey(),
                    "deviceType", deviceType.name()
                )
            ));

            return ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "인증된 WebSocket 세션 생성 완료"),
                "response", response
            ));

        } catch (Exception e) {
            log.error("❌ 인증된 WebSocket 세션 생성 실패 - 회원ID: {}", authenticatedMember.getId(), e);
            return ResponseEntity.status(500).body(Map.of(
                "status", Map.of("code", "ERR_5000", "message", "세션 생성 실패"),
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 2. 초기 데이터 요청 API (인증 필수)
     */
    @PostMapping("/initial-data")
    public ResponseEntity<?> getInitialData(@RequestBody InitialDataRequest request,
                                          @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member authenticatedMember = userDetails.getMember();
        if (authenticatedMember == null) {
            return ResponseEntity.status(401).body(Map.of(
                "status", Map.of("code", "AUTH_4010", "message", "인증이 필요합니다")
            ));
        }

        try {
            // 요청 회원ID와 인증된 회원ID 일치 확인
            if (!authenticatedMember.getId().equals(request.getMemberId())) {
                log.warn("🔒 초기 데이터 요청 권한 없음 - 토큰회원ID: {}, 요청회원ID: {}",
                        authenticatedMember.getId(), request.getMemberId());
                return ResponseEntity.status(403).body(Map.of(
                    "status", Map.of("code", "AUTH_4030", "message", "권한이 없습니다")
                ));
            }

            log.info("📋 인증된 초기 데이터 요청 - 회원ID: {}, 메모리얼ID: {}",
                    authenticatedMember.getId(), request.getMemorialId());

            // 디바이스 타입별 대기영상 URL 생성
            DeviceType deviceType = request.getDeviceType() != null ?
                request.getDeviceType() : DeviceType.WEB;

            String waitingVideoUrl = waitingVideoService.getWaitingVideoUrl("rohmoohyun", deviceType);

            Map<String, Object> response = Map.of(
                "contactName", request.getContactName() != null ? request.getContactName() : "rohmoohyun",
                "waitingVideoUrl", waitingVideoUrl,
                "memberId", authenticatedMember.getId(),
                "memberName", authenticatedMember.getName(),
                "memorialId", request.getMemorialId(),
                "deviceType", deviceType.name(),
                "authMethod", "INITIAL_MESSAGE"
            );

            return ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "인증된 초기 데이터 조회 완료"),
                "response", response
            ));

        } catch (Exception e) {
            log.error("❌ 인증된 초기 데이터 조회 실패 - 회원ID: {}", authenticatedMember.getId(), e);
            return ResponseEntity.status(500).body(Map.of(
                "status", Map.of("code", "ERR_5000", "message", "초기 데이터 조회 실패"),
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 3. 영상 업로드 및 처리 (세션 소유권 검증)
     */
    @PostMapping(value = "/process/{sessionKey}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processVideo(@PathVariable String sessionKey,
                                    @RequestParam("video") MultipartFile videoFile,
                                    @RequestParam(required = false, defaultValue = "kimgeuntae") String contactKey,
                                    @AuthenticationPrincipal MemberUserDetails userDetails) {

            if (!validateSessionOwnership(sessionKey, userDetails)) {
                return createUnauthorizedResponse();
            }

            try {
                MemorialVideoSession session = sessionManager.getSession(sessionKey);

                log.info("📹 인증된 영상 처리 시작 - 세션: {}, 파일: {}, 대상: {}",
                        sessionKey, videoFile.getOriginalFilename(), contactKey);

                // ✅ 올바른 상태 전환 (PROCESSING만 사용)
                flowManager.transitionToState(sessionKey, VideoCallFlowState.PROCESSING);

                session = sessionManager.getSession(sessionKey); // 다시 조회로 동기화 확인
                if (session.getFlowState() != VideoCallFlowState.PROCESSING) {
                    log.error("상태 동기화 실패 - 예상: PROCESSING, 실제: {}", session.getFlowState());
                }

                // 파일 저장
                String savedFilePath = fileStorageService.uploadVideoCallRecording(videoFile, sessionKey);
                session.setSavedFilePath(savedFilePath);

                // 메타데이터 추가 (필요시)
                if (session.getMetadata() != null) {
                    session.addMetadata("contactKey", contactKey);
                }
                sessionManager.saveSession(session);

                // 저장 후 다시 한번 검증
                MemorialVideoSession verifySession = sessionManager.getSession(sessionKey);
                log.info("상태 검증 - 저장된 상태: {}, 파일경로: {}", verifySession.getFlowState(), verifySession.getSavedFilePath());

                // 즉시 응답
                Map<String, Object> response = Map.of(
                    "sessionKey", sessionKey,
                    "filePath", savedFilePath,
                    "contactKey", contactKey,
                    "uploadStatus", "COMPLETED",
                    "nextState", "PROCESSING", // ✅ 올바른 상태명
                    "authenticatedMemberId", userDetails.getMember().getId()
                );

                // 백그라운드 처리
                executorService.submit(() -> {
                    processVideoAsyncWithWebSocket(sessionKey, savedFilePath, contactKey);
                });

                return ResponseEntity.ok(Map.of(
                    "status", Map.of("code", "OK_0000", "message", "영상 업로드 완료"),
                    "response", response
                ));

            } catch (Exception e) {
                log.error("❌ 영상 처리 실패 - 세션: {}", sessionKey, e);

                // ✅ 올바른 오류 상태
                flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR);

                return createErrorResponse("ERR_5000", "영상 처리 실패", e.getMessage());
            }
        }

        /**
         * 4. 외부 API 콜백 (세션 소유권 검증)
         */
        @PostMapping("/callback/{sessionKey}")
        public ResponseEntity<?> receiveResponse(@PathVariable String sessionKey,
                                           @RequestBody Map<String, Object> responseData,
                                           @AuthenticationPrincipal MemberUserDetails userDetails) {

        if (!validateSessionOwnership(sessionKey, userDetails)) {
            return createUnauthorizedResponse();
        }

        try {
            String responseVideoUrl = (String) responseData.get("videoUrl");
            log.info("🎬 응답영상 콜백 수신 - 세션: {}, URL: {}", sessionKey, responseVideoUrl);

            MemorialVideoSession session = sessionManager.getSessionWithStateValidation(
            sessionKey, VideoCallFlowState.PROCESSING);

            if (session == null) {
                log.error("❌ 세션 조회 실패: {}", sessionKey);
                return createErrorResponse("ERR_4040", "세션을 찾을 수 없습니다", "");
            }

            // 🔥 여전히 PROCESSING가 아니면 추가 대기
            if (session.getFlowState() != VideoCallFlowState.PROCESSING) {
                log.warn("⚠️ 예상되지 않은 상태: {} (PROCESSING 기대), 추가 대기 시작", session.getFlowState());

                // 최대 3초 추가 대기
                session = waitForProcessingState(sessionKey, 3000);

                if (session == null || session.getFlowState() != VideoCallFlowState.PROCESSING) {
                    log.error("❌ PROCESSING 상태 대기 실패 - 현재: {}",
                             session != null ? session.getFlowState() : "NULL");

                    // 🔥 강제로 진행 (비상 대응)
                    if (session != null) {
                        log.warn("🚨 비상 대응: 현재 상태({})에서 강제 진행", session.getFlowState());
                    } else {
                        return createErrorResponse("ERR_4040", "세션 상태 불일치", "");
                    }
                }
            }

            // 올바른 상태 전환 (RESPONSE_PLAYING 사용)
            session.setResponseVideoUrl(responseVideoUrl);
            sessionManager.saveSession(session);

            MemorialVideoSession verifySession = sessionManager.getSession(sessionKey, true);
            log.info("💾 응답 URL 저장 검증: {} (URL: {})",
                    verifySession != null ? "성공" : "실패",
                    verifySession != null ? verifySession.getResponseVideoUrl() : "NULL");

            // VideoCallFlowManager가 WebSocket 브로드캐스트도 처리
            boolean success = flowManager.transitionToState(sessionKey, VideoCallFlowState.RESPONSE_PLAYING);

            if (!success) {
                log.error("❌ RESPONSE_PLAYING 상태 전환 실패");
                return createErrorResponse("ERR_5000", "상태 전환 실패", "");
            }

            return ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "응답영상 전송 완료"),
                "response", Map.of(
                    "sessionKey", sessionKey,
                    "responseVideoUrl", responseVideoUrl,
                    "currentState", session.getFlowState().name(),
                    "authenticatedMemberId", userDetails.getMember().getId()
                )
            ));

        } catch (Exception e) {
            log.error("❌ 응답영상 콜백 처리 실패 - 세션: {}", sessionKey, e);
            flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR);

            return createErrorResponse("ERR_5000", "응답영상 처리 실패", e.getMessage());
        }
    }

    /**
     * 5. 세션 상태 조회 (소유권 검증)
     */
    @GetMapping("/session/{sessionKey}")
    public ResponseEntity<?> getSessionStatus(@PathVariable String sessionKey,
                                            @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member authenticatedMember = userDetails.getMember();
        if (authenticatedMember == null) {
            return ResponseEntity.status(401).body(Map.of(
                "status", Map.of("code", "AUTH_4010", "message", "인증이 필요합니다")
            ));
        }

        MemorialVideoSession session = sessionManager.getSession(sessionKey);

        if (session == null) {
            return ResponseEntity.status(404).body(Map.of(
                "status", Map.of("code", "ERR_4040", "message", "세션을 찾을 수 없습니다")
            ));
        }

        if (session.isExpired()) {
            sessionManager.deleteSession(sessionKey);
            return ResponseEntity.status(410).body(Map.of(
                "status", Map.of("code", "ERR_4100", "message", "세션이 만료되었습니다")
            ));
        }

        // 세션 소유권 검증
        if (!authenticatedMember.getId().equals(session.getCallerId())) {
            log.warn("🔒 세션 상태 조회 권한 없음 - 토큰회원ID: {}, 세션회원ID: {}",
                    authenticatedMember.getId(), session.getCallerId());
            return ResponseEntity.status(403).body(Map.of(
                "status", Map.of("code", "AUTH_4030", "message", "해당 세션에 접근할 권한이 없습니다")
            ));
        }

        session.updateActivity();
        sessionManager.saveSession(session);

        Map<String, Object> sessionResponse = new HashMap<>();
        sessionResponse.put("sessionKey", sessionKey);
        sessionResponse.put("contactName", session.getContactName());
        sessionResponse.put("flowState", session.getFlowState().name());
        sessionResponse.put("flowStateDisplay", session.getFlowState().getDisplayName());
        sessionResponse.put("flowStateDescription", session.getFlowState().getDescription());
        sessionResponse.put("deviceType", session.getDeviceType().name());
        sessionResponse.put("deviceId", session.getDeviceId());
        sessionResponse.put("isPrimaryDevice", session.isPrimaryDevice());
        sessionResponse.put("isConnected", session.isConnected());
        sessionResponse.put("ageInMinutes", session.getAgeInMinutes());
        sessionResponse.put("minutesSinceStateChange", session.getMinutesSinceStateChange());
        sessionResponse.put("ttlRemaining", session.getRemainingTtlSeconds());
        sessionResponse.put("reconnectCount", session.getReconnectCount());
        sessionResponse.put("waitingVideoUrl", session.getWaitingVideoUrl());
        sessionResponse.put("savedFilePath", session.getSavedFilePath());
        sessionResponse.put("responseVideoUrl", session.getResponseVideoUrl());
        sessionResponse.put("metadata", session.getMetadata());
        sessionResponse.put("authenticatedMemberId", authenticatedMember.getId());
        sessionResponse.put("authMethod", "INITIAL_MESSAGE"); // 인증 방식 정보

        return ResponseEntity.ok(Map.of(
            "status", Map.of("code", "OK_0000", "message", "인증된 세션 상태 조회 완료"),
            "response", sessionResponse
        ));
    }

    /**
     * 6. 세션 정리/종료 (소유권 검증)
     */
    @DeleteMapping("/session/{sessionKey}/cleanup")
    public ResponseEntity<?> cleanupSession(@PathVariable String sessionKey,
                                          @RequestParam(required = false) String reason,
                                          @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member authenticatedMember = userDetails.getMember();
        if (authenticatedMember == null) {
            return ResponseEntity.status(401).body(Map.of(
                "status", Map.of("code", "AUTH_4010", "message", "인증이 필요합니다")
            ));
        }

        try {
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session == null) {
                return ResponseEntity.ok(Map.of(
                    "status", Map.of("code", "OK_0000", "message", "세션이 이미 정리되었거나 존재하지 않습니다")
                ));
            }

            // 세션 소유권 검증
            if (!authenticatedMember.getId().equals(session.getCallerId())) {
                log.warn("🔒 세션 정리 권한 없음 - 토큰회원ID: {}, 세션회원ID: {}",
                        authenticatedMember.getId(), session.getCallerId());
                return ResponseEntity.status(403).body(Map.of(
                    "status", Map.of("code", "AUTH_4030", "message", "해당 세션에 접근할 권한이 없습니다")
                ));
            }

            // 세션 정리
            deviceManager.cleanupSession(sessionKey);
            sessionManager.deleteSession(sessionKey);

            log.info("🧹 인증된 세션 정리 완료 - 세션: {}, 사유: {}, 회원ID: {}",
                    sessionKey, reason, authenticatedMember.getId());

            return ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "세션 정리 완료"),
                "response", Map.of(
                    "sessionKey", sessionKey,
                    "reason", reason != null ? reason : "USER_REQUEST",
                    "cleanedAt", System.currentTimeMillis(),
                    "authenticatedMemberId", authenticatedMember.getId()
                )
            ));

        } catch (Exception e) {
            log.error("❌ 인증된 세션 정리 실패 - 세션: {}, 회원ID: {}",
                     sessionKey, authenticatedMember.getId(), e);
            return ResponseEntity.status(500).body(Map.of(
                "status", Map.of("code", "ERR_5000", "message", "세션 정리 실패"),
                "error", e.getMessage()
            ));
        }
    }

    // ========== Private Methods ==========

    private MemorialVideoSession waitForProcessingState(String sessionKey, long timeoutMs) {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            MemorialVideoSession session = sessionManager.getSession(sessionKey);

            if (session != null && session.getFlowState() == VideoCallFlowState.PROCESSING) {
                log.info("✅ PROCESSING 상태 확인: {}", sessionKey);
                return session;
            }

            log.debug("⏳ PROCESSING 상태 대기: {} - 현재: {}",
                    sessionKey, session != null ? session.getFlowState() : "NULL");

            try {
                Thread.sleep(200); // 200ms 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.warn("⏰ PROCESSING 상태 대기 타임아웃: {}", sessionKey);
        return sessionManager.getSession(sessionKey);
    }

    /**
     * 외부 API 비동기 처리 (WebSocket 알림 포함)
     */
    private void processVideoAsyncWithWebSocket(String sessionKey, String savedFilePath, String contactKey) {
        try {
            log.info("🚀 외부 API 비동기 처리 시작 - 세션: {}", sessionKey);

            // 외부 API 호출
            externalVideoApiService.sendVideoToExternalApiAsync(
                sessionKey,
                savedFilePath,
                contactKey,
                // 성공 콜백
                (response) -> {
                    log.info("✅ 외부 API 전송 완료 - 세션: {}", sessionKey);
                },
                // 실패 콜백
                (error) -> {
                    log.error("❌ 외부 API 전송 실패 - 세션: {}", sessionKey, error);
                    flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR);
                }
            );

        } catch (Exception e) {
            log.error("❌ 외부 API 비동기 처리 오류 - 세션: {}", sessionKey, e);
            flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR);
        }
    }


    /**
     * 세션 소유권 검증 (공통 메서드)
     */
    private boolean validateSessionOwnership(String sessionKey, MemberUserDetails userDetails) {
        if (userDetails == null || userDetails.getMember() == null) {
            return false;
        }

        MemorialVideoSession session = sessionManager.getSession(sessionKey);
        if (session == null || session.isExpired()) {
            return false;
        }

        return userDetails.getMember().getId().equals(session.getCallerId());
    }

    /**
     * 인증 실패 응답 생성
     */
    private ResponseEntity<?> createUnauthorizedResponse() {
        return ResponseEntity.status(401).body(Map.of(
            "status", Map.of("code", "AUTH_4010", "message", "인증이 필요합니다")
        ));
    }

    /**
     * 권한 없음 응답 생성
     */
    private ResponseEntity<?> createForbiddenResponse() {
        return ResponseEntity.status(403).body(Map.of(
            "status", Map.of("code", "AUTH_4030", "message", "권한이 없습니다")
        ));
    }

    /**
     * 에러 응답 생성
     */
    private ResponseEntity<?> createErrorResponse(String code, String message, String error) {
        return ResponseEntity.status(500).body(Map.of(
            "status", Map.of("code", code, "message", message),
            "error", error
        ));
    }
}