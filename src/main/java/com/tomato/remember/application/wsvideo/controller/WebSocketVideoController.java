package com.tomato.remember.application.wsvideo.controller;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.application.wsvideo.config.MemorialVideoSessionManager;
import com.tomato.remember.application.wsvideo.config.MemorialVideoWebSocketHandler;
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
            String waitingVideoUrl = waitingVideoService.getWaitingVideoUrl(
                request.getContactKey(), deviceType);
            session.setWaitingVideoUrl(waitingVideoUrl);

            // 초기 상태 설정
            session.transitionToState(VideoCallFlowState.INITIALIZING);
            sessionManager.saveSession(session);

            // 디바이스 등록
            deviceManager.registerDevice(session.getSessionKey(), request.getDeviceId(), deviceType);

            // 현재 요청의 액세스 토큰 추출 (WebSocket URL에 포함할 용도)
            String currentAccessToken = null;
            try {
                // Authorization 헤더에서 토큰 추출
                String authHeader = httpRequest.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    currentAccessToken = authHeader.substring(7).trim();
                }
            } catch (Exception e) {
                log.warn("토큰 추출 실패 - 기본 WebSocket 경로 제공: {}", e.getMessage());
            }

            // WebSocket 경로에 토큰 파라미터 포함
            String websocketPath = deviceType.getWebSocketPath(session.getSessionKey());
            if (currentAccessToken != null) {
                websocketPath += "?token=" + java.net.URLEncoder.encode(currentAccessToken, "UTF-8");
            }

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

            String waitingVideoUrl = waitingVideoService.getWaitingVideoUrl("kimgeuntae", deviceType);

            Map<String, Object> response = Map.of(
                "contactName", request.getContactName() != null ? request.getContactName() : "김근태",
                "waitingVideoUrl", waitingVideoUrl,
                "memberId", authenticatedMember.getId(),
                "memberName", authenticatedMember.getName(),
                "memorialId", request.getMemorialId(),
                "deviceType", deviceType.name()
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

        Member authenticatedMember = userDetails.getMember();
        if (authenticatedMember == null) {
            return ResponseEntity.status(401).body(Map.of(
                "status", Map.of("code", "AUTH_4010", "message", "인증이 필요합니다")
            ));
        }

        try {
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "status", Map.of("code", "ERR_4040", "message", "세션을 찾을 수 없습니다")
                ));
            }

            if (session.isExpired()) {
                return ResponseEntity.status(410).body(Map.of(
                    "status", Map.of("code", "ERR_4100", "message", "세션이 만료되었습니다")
                ));
            }

            // 세션 소유권 검증
            if (!authenticatedMember.getId().equals(session.getCallerId())) {
                log.warn("🔒 영상 처리 권한 없음 - 토큰회원ID: {}, 세션회원ID: {}",
                        authenticatedMember.getId(), session.getCallerId());
                return ResponseEntity.status(403).body(Map.of(
                    "status", Map.of("code", "AUTH_4030", "message", "해당 세션에 접근할 권한이 없습니다")
                ));
            }

            log.info("📹 인증된 영상 처리 시작 - 세션: {}, 파일: {}, 대상: {}, 회원ID: {}",
                    sessionKey, videoFile.getOriginalFilename(), contactKey, authenticatedMember.getId());

            // 상태 전환: PROCESSING_UPLOAD
            flowManager.transitionToState(sessionKey, VideoCallFlowState.PROCESSING_UPLOAD);

            // 파일 저장
            String savedFilePath = fileStorageService.uploadVideoCallRecording(videoFile, sessionKey);
            session.setSavedFilePath(savedFilePath);
            session.addMetadata("contactKey", contactKey);
            sessionManager.saveSession(session);

            // 즉시 응답
            Map<String, Object> response = Map.of(
                "sessionKey", sessionKey,
                "filePath", savedFilePath,
                "contactKey", contactKey,
                "uploadStatus", "COMPLETED",
                "nextState", "PROCESSING_AI",
                "authenticatedMemberId", authenticatedMember.getId()
            );

            // 백그라운드에서 외부 API 호출
            executorService.submit(() -> {
                processVideoAsyncWithWebSocket(sessionKey, savedFilePath, contactKey);
            });

            return ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "인증된 영상 업로드 완료"),
                "response", response
            ));

        } catch (Exception e) {
            log.error("❌ 인증된 영상 처리 실패 - 세션: {}, 회원ID: {}", sessionKey, authenticatedMember.getId(), e);

            // 오류 상태로 전환
            flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR_PROCESSING);

            return ResponseEntity.status(500).body(Map.of(
                "status", Map.of("code", "ERR_5000", "message", "영상 처리 실패"),
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 4. 외부 API 콜백 (세션 소유권 검증)
     */
    @PostMapping("/callback/{sessionKey}")
    public ResponseEntity<?> receiveResponse(@PathVariable String sessionKey,
                                           @RequestBody Map<String, Object> responseData,
                                           @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member authenticatedMember = userDetails.getMember();
        if (authenticatedMember == null) {
            return ResponseEntity.status(401).body(Map.of(
                "status", Map.of("code", "AUTH_4010", "message", "인증이 필요합니다")
            ));
        }

        try {
            String responseVideoUrl = (String) responseData.get("videoUrl");
            log.info("🎬 인증된 응답영상 콜백 수신 - 세션: {}, URL: {}, 회원ID: {}",
                    sessionKey, responseVideoUrl, authenticatedMember.getId());

            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "status", Map.of("code", "ERR_4040", "message", "세션을 찾을 수 없습니다")
                ));
            }

            if (session.isExpired()) {
                return ResponseEntity.status(410).body(Map.of(
                    "status", Map.of("code", "ERR_4100", "message", "세션이 만료되었습니다")
                ));
            }

            // 세션 소유권 검증
            if (!authenticatedMember.getId().equals(session.getCallerId())) {
                log.warn("🔒 응답영상 콜백 권한 없음 - 토큰회원ID: {}, 세션회원ID: {}",
                        authenticatedMember.getId(), session.getCallerId());
                return ResponseEntity.status(403).body(Map.of(
                    "status", Map.of("code", "AUTH_4030", "message", "해당 세션에 접근할 권한이 없습니다")
                ));
            }

            // 상태 전환: RESPONSE_READY
            flowManager.transitionToState(sessionKey, VideoCallFlowState.RESPONSE_READY);

            // 응답영상 URL 설정
            session.setResponseVideoUrl(responseVideoUrl);
            sessionManager.saveSession(session);

            // WebSocket으로 응답영상 전송
            webSocketHandler.sendResponseVideo(sessionKey, responseVideoUrl);

            return ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "인증된 응답영상 전송 완료"),
                "response", Map.of(
                    "sessionKey", sessionKey,
                    "responseVideoUrl", responseVideoUrl,
                    "currentState", session.getFlowState().name(),
                    "authenticatedMemberId", authenticatedMember.getId()
                )
            ));

        } catch (Exception e) {
            log.error("❌ 인증된 응답영상 콜백 처리 실패 - 세션: {}, 회원ID: {}",
                     sessionKey, authenticatedMember.getId(), e);
            flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR_PROCESSING);

            return ResponseEntity.status(500).body(Map.of(
                "status", Map.of("code", "ERR_5000", "message", "응답영상 처리 실패"),
                "error", e.getMessage()
            ));
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

    /**
     * 외부 API 비동기 처리 (WebSocket 알림 포함)
     */
    private void processVideoAsyncWithWebSocket(String sessionKey, String savedFilePath, String contactKey) {
        try {
            log.info("🚀 외부 API 비동기 처리 시작 - 세션: {}", sessionKey);

            // 상태 전환: AI 처리 중
            flowManager.transitionToState(sessionKey, VideoCallFlowState.PROCESSING_AI);

            // 외부 API 호출
            externalVideoApiService.sendVideoToExternalApiAsync(
                sessionKey,
                savedFilePath,
                contactKey,
                // 성공 콜백
                (response) -> {
                    log.info("✅ 외부 API 전송 완료 - 세션: {}", sessionKey);
                    flowManager.transitionToState(sessionKey, VideoCallFlowState.PROCESSING_COMPLETE);
                },
                // 실패 콜백
                (error) -> {
                    log.error("❌ 외부 API 전송 실패 - 세션: {}", sessionKey, error);
                    flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR_PROCESSING);
                }
            );

        } catch (Exception e) {
            log.error("❌ 외부 API 비동기 처리 오류 - 세션: {}", sessionKey, e);
            flowManager.transitionToState(sessionKey, VideoCallFlowState.ERROR_PROCESSING);
        }
    }

    // ========== DTO Classes ==========

    public static class CreateSessionRequest {
        private String contactName = "김근태";
        private String contactKey = "kimgeuntae";
        private Long memorialId;
        private Long callerId;
        private DeviceType deviceType;
        private String deviceId;

        // getters and setters
        public String getContactName() { return contactName; }
        public void setContactName(String contactName) { this.contactName = contactName; }
        public String getContactKey() { return contactKey; }
        public void setContactKey(String contactKey) { this.contactKey = contactKey; }
        public Long getMemorialId() { return memorialId; }
        public void setMemorialId(Long memorialId) { this.memorialId = memorialId; }
        public Long getCallerId() { return callerId; }
        public void setCallerId(Long callerId) { this.callerId = callerId; }
        public DeviceType getDeviceType() { return deviceType; }
        public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    }

    public static class InitialDataRequest {
        private Long memberId;
        private Long memorialId;
        private String contactName;
        private DeviceType deviceType;

        // getters and setters
        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        public Long getMemorialId() { return memorialId; }
        public void setMemorialId(Long memorialId) { this.memorialId = memorialId; }
        public String getContactName() { return contactName; }
        public void setContactName(String contactName) { this.contactName = contactName; }
        public DeviceType getDeviceType() { return deviceType; }
        public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }
    }

    public static class DeviceRegistrationRequest {
        private String deviceId;
        private DeviceType deviceType;
        private boolean setPrimary = false;

        // getters and setters
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public DeviceType getDeviceType() { return deviceType; }
        public void setDeviceType(DeviceType deviceType) { this.deviceType = deviceType; }
        public boolean isSetPrimary() { return setPrimary; }
        public void setSetPrimary(boolean setPrimary) { this.setPrimary = setPrimary; }
    }
}