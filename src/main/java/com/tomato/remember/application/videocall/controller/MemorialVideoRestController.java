package com.tomato.remember.application.videocall.controller;

import com.tomato.remember.application.videocall.config.MemorialVideoSessionManager;
import com.tomato.remember.application.videocall.config.MemorialVideoWebSocketHandler;
import com.tomato.remember.application.videocall.dto.MemorialVideoSession;
import com.tomato.remember.application.videocall.service.MemorialVideoHeartbeatService;
import com.tomato.remember.common.util.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Memorial Video Call REST API 컨트롤러
 * 파일 업로드, 세션 관리, 외부 API 콜백 등 처리
 */
@Slf4j
@RestController
@RequestMapping("/api/memorial-video")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MemorialVideoRestController {

    private MemorialVideoSessionManager sessionManager;

    private MemorialVideoWebSocketHandler webSocketHandler;

    private MemorialVideoHeartbeatService heartbeatService;

    private FileStorageService fileStorageService;

    // TODO: 외부 API 서비스 주입
    // @Autowired
    // private ExternalVideoApiService externalVideoApiService;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 1. 세션 생성 API
     */
    @PostMapping("/create-session")
    public ResponseEntity<?> createSession(@RequestBody Map<String, Object> request) {
        try {
            String contactName = (String) request.getOrDefault("contactName", "Unknown");
            Long memorialId = request.get("memorialId") != null ?
                    Long.valueOf(request.get("memorialId").toString()) : null;
            Long callerId = request.get("callerId") != null ?
                    Long.valueOf(request.get("callerId").toString()) : null;

            MemorialVideoSession session = sessionManager.createSession(contactName, memorialId, callerId);

            log.info("🆕 새 Memorial Video 세션 생성: {} (연락처: {}, Memorial ID: {})",
                    session.getSessionKey(), contactName, memorialId);

            Map<String, Object> response = Map.of(
                    "sessionKey", session.getSessionKey(),
                    "contactName", session.getContactName(),
                    "memorialId", session.getMemorialId() != null ? session.getMemorialId() : "",
                    "callerId", session.getCallerId() != null ? session.getCallerId() : "",
                    "createdAt", session.getCreatedAt().toString(),
                    "ttlSeconds", MemorialVideoSession.getTtlSeconds(),
                    "heartbeatInterval", MemorialVideoSession.getHeartbeatIntervalSeconds()
            );

            return ResponseEntity.ok(Map.of(
                    "status", Map.of("code", "OK_0000", "message", "세션 생성 완료"),
                    "response", response
            ));

        } catch (Exception e) {
            log.error("❌ 세션 생성 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", Map.of("code", "ERR_5000", "message", "세션 생성 실패"),
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 2. 영상 업로드 및 처리 API
     */
    @PostMapping(value = "/process/{sessionKey}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processVideo(
            @PathVariable String sessionKey,
            @RequestParam("video") MultipartFile videoFile) {
        try {
            // 세션 유효성 검증
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

            log.info("📹 영상 처리 시작 - 세션: {} (나이: {}분), 파일: {}",
                    sessionKey, session.getAgeInMinutes(), videoFile.getOriginalFilename());

            // 파일 저장
            String savedFilePath = fileStorageService.uploadVideoCallRecording(videoFile, sessionKey);

            // 세션 상태 업데이트
            session.setProcessing(savedFilePath);
            sessionManager.saveSession(session);

            log.info("📁 파일 저장 완료 - 세션: {}, 경로: {}", sessionKey, savedFilePath);

            // 웹소켓으로 상태 알림
            Map<String, Object> notification = Map.of(
                    "type", "VIDEO_UPLOADED",
                    "sessionKey", sessionKey,
                    "filePath", savedFilePath,
                    "status", "PROCESSING"
            );
            webSocketHandler.sendMessageToSession(sessionKey, notification);

            // 즉시 200 OK 응답
            Map<String, Object> response = Map.of(
                    "sessionKey", sessionKey,
                    "filePath", savedFilePath,
                    "contactName", session.getContactName(),
                    "status", "UPLOADED",
                    "sessionAge", session.getAgeInMinutes(),
                    "ttlRemaining", session.getRemainingTtlSeconds()
            );

            ResponseEntity<?> result = ResponseEntity.ok(Map.of(
                    "status", Map.of("code", "OK_0000", "message", "영상 업로드 완료"),
                    "response", response
            ));

            // 백그라운드에서 외부 API 호출
            executorService.submit(() -> {
                processVideoAsyncWithExternalApi(sessionKey, savedFilePath);
            });

            return result;

        } catch (Exception e) {
            log.error("❌ 영상 처리 실패 - 세션: {}", sessionKey, e);

            // 세션 오류 상태 업데이트
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session != null) {
                session.setError();
                sessionManager.saveSession(session);

                // 웹소켓으로 오류 알림
                Map<String, Object> errorNotification = Map.of(
                        "type", "ERROR",
                        "code", "VIDEO_PROCESSING_FAILED",
                        "message", "영상 처리 중 오류가 발생했습니다",
                        "sessionKey", sessionKey
                );
                webSocketHandler.sendMessageToSession(sessionKey, errorNotification);
            }

            return ResponseEntity.status(500).body(Map.of(
                    "status", Map.of("code", "ERR_5000", "message", "영상 처리 실패"),
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 3. 외부 API 콜백 - 응답 영상 수신 API
     */
    @PostMapping("/callback/response/{sessionKey}")
    public ResponseEntity<?> receiveResponseVideo(
            @PathVariable String sessionKey,
            @RequestBody Map<String, Object> responseData) {
        try {
            String responseVideoUrl = (String) responseData.get("videoUrl");

            log.info("🎬 응답 영상 콜백 수신 - 세션: {}, URL: {}", sessionKey, responseVideoUrl);

            // 세션 유효성 확인
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

            // 웹소켓으로 응답 영상 전송
            webSocketHandler.sendResponseVideo(sessionKey, responseVideoUrl);

            log.info("✅ 응답 영상 전송 완료 - 세션: {} (나이: {}분)", sessionKey, session.getAgeInMinutes());

            return ResponseEntity.ok(Map.of(
                    "status", Map.of("code", "OK_0000", "message", "응답 영상 전송 완료"),
                    "response", Map.of(
                            "sessionKey", sessionKey,
                            "sessionAge", session.getAgeInMinutes(),
                            "ttlRemaining", session.getRemainingTtlSeconds()
                    )
            ));

        } catch (Exception e) {
            log.error("❌ 응답 영상 콜백 처리 실패 - 세션: {}", sessionKey, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", Map.of("code", "ERR_5000", "message", "응답 영상 처리 실패"),
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 4. 세션 상태 조회 API
     */
    @GetMapping("/session/{sessionKey}")
    public ResponseEntity<?> getSessionStatus(@PathVariable String sessionKey) {
        MemorialVideoSession session = sessionManager.getSession(sessionKey);

        if (session == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", Map.of("code", "ERR_4040", "message", "세션을 찾을 수 없습니다")
            ));
        }

        if (session.isExpired()) {
            // 만료된 세션 정보 반환 후 정리
            Map<String, Object> expiredResponse = Map.of(
                    "sessionKey", sessionKey,
                    "expired", true,
                    "createdAt", session.getCreatedAt().toString(),
                    "lastActivity", session.getLastActivity().toString(),
                    "ageInMinutes", session.getAgeInMinutes()
            );

            sessionManager.deleteSession(sessionKey);

            return ResponseEntity.status(410).body(Map.of(
                    "status", Map.of("code", "ERR_4100", "message", "세션이 만료되었습니다"),
                    "response", expiredResponse
            ));
        }

        // 활동 시간 업데이트
        session.updateActivity();
        sessionManager.saveSession(session);

        Map<String, Object> sessionResponse = Map.of(
                "sessionKey", sessionKey,
                "contactName", session.getContactName(),
                "memorialId", session.getMemorialId() != null ? session.getMemorialId() : "",
                "callerId", session.getCallerId() != null ? session.getCallerId() : "",
                "status", session.getStatus(),
                "createdAt", session.getCreatedAt().toString(),
                "lastActivity", session.getLastActivity().toString(),
                "ageInMinutes", session.getAgeInMinutes(),
                "ttlRemaining", session.getRemainingTtlSeconds(),
                "isConnected", session.isConnected(),
                "reconnectCount", session.getReconnectCount(),
                "savedFilePath", session.getSavedFilePath() != null ? session.getSavedFilePath() : "",
                "responseVideoUrl", session.getResponseVideoUrl() != null ? session.getResponseVideoUrl() : ""
        );

        return ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "세션 조회 완료"),
                "response", sessionResponse
        ));
    }

    /**
     * 5. 활성 세션 목록 조회 API
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getActiveSessions() {
        try {
            Set<MemorialVideoSession> activeSessions = sessionManager.getActiveSessions();
            MemorialVideoSessionManager.SessionStatistics stats = sessionManager.getSessionStatistics();

            Map<String, Object> sessionListResponse = Map.of(
                    "totalSessions", stats.getTotalSessions(),
                    "connectedSessions", stats.getConnectedSessions(),
                    "waitingSessions", stats.getWaitingSessions(),
                    "processingSessions", stats.getProcessingSessions(),
                    "avgSessionAge", stats.getAvgSessionAgeMinutes(),
                    "activeWebSocketConnections", webSocketHandler.getActiveConnectionCount(),
                    "ttlSeconds", MemorialVideoSession.getTtlSeconds(),
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

            return ResponseEntity.ok(Map.of(
                    "status", Map.of("code", "OK_0000", "message", "활성 세션 목록 조회 완료"),
                    "response", sessionListResponse
            ));

        } catch (Exception e) {
            log.error("❌ 활성 세션 목록 조회 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", Map.of("code", "ERR_5000", "message", "세션 목록 조회 실패"),
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 6. 세션 수동 정리 API
     */
    @PostMapping("/session/{sessionKey}/cleanup")
    public ResponseEntity<?> cleanupSession(@PathVariable String sessionKey) {
        try {
            log.info("🧹 세션 수동 정리 요청 - 세션: {}", sessionKey);

            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session != null) {
                // 웹소켓 연결 정리 알림
                if (session.isConnected()) {
                    Map<String, Object> disconnectMessage = Map.of(
                            "type", "FORCE_DISCONNECT",
                            "reason", "MANUAL_CLEANUP",
                            "message", "관리자에 의해 세션이 정리되었습니다"
                    );
                    webSocketHandler.sendMessageToSession(sessionKey, disconnectMessage);
                }

                // 세션 삭제
                sessionManager.deleteSession(sessionKey);

                log.info("✅ 세션 수동 정리 완료 - 세션: {}", sessionKey);
            }

            return ResponseEntity.ok(Map.of(
                    "status", Map.of("code", "OK_0000", "message", "세션 정리 완료"),
                    "response", Map.of(
                            "sessionKey", sessionKey,
                            "timestamp", System.currentTimeMillis()
                    )
            ));

        } catch (Exception e) {
            log.error("❌ 세션 수동 정리 실패 - 세션: {}", sessionKey, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", Map.of("code", "ERR_5000", "message", "세션 정리 실패"),
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 7. 수동 하트비트 전송 API
     */
    @PostMapping("/session/{sessionKey}/heartbeat")
    public ResponseEntity<?> sendManualHeartbeat(@PathVariable String sessionKey) {
        try {
            boolean success = heartbeatService.sendHeartbeatToSession(sessionKey);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "status", Map.of("code", "OK_0000", "message", "하트비트 전송 완료"),
                        "response", Map.of(
                                "sessionKey", sessionKey,
                                "timestamp", System.currentTimeMillis()
                        )
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of(
                        "status", Map.of("code", "ERR_4040", "message", "세션을 찾을 수 없거나 연결되지 않음")
                ));
            }

        } catch (Exception e) {
            log.error("❌ 수동 하트비트 전송 실패 - 세션: {}", sessionKey, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", Map.of("code", "ERR_5000", "message", "하트비트 전송 실패"),
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 8. 시스템 상태 조회 API
     */
    @GetMapping("/system/status")
    public ResponseEntity<?> getSystemStatus() {
        try {
            MemorialVideoSessionManager.SessionStatistics stats = sessionManager.getSessionStatistics();

            Map<String, Object> systemStatus = Map.of(
                    "service", "Memorial Video Call WebSocket",
                    "status", "RUNNING",
                    "timestamp", System.currentTimeMillis(),
                    "sessions", Map.of(
                            "total", stats.getTotalSessions(),
                            "connected", stats.getConnectedSessions(),
                            "waiting", stats.getWaitingSessions(),
                            "processing", stats.getProcessingSessions(),
                            "avgAge", stats.getAvgSessionAgeMinutes()
                    ),
                    "webSocket", Map.of(
                            "activeConnections", webSocketHandler.getActiveConnectionCount(),
                            "endpoints", new String[]{
                                    "/ws/memorial-video/{sessionKey}",
                                    "/ws/memorial-video/native/{sessionKey}"
                            }
                    ),
                    "configuration", Map.of(
                            "sessionTtlSeconds", MemorialVideoSession.getTtlSeconds(),
                            "heartbeatIntervalSeconds", MemorialVideoSession.getHeartbeatIntervalSeconds(),
                            "maxFileSize", "50MB",
                            "supportedFormats", new String[]{"webm", "mp4", "mov", "avi"}
                    )
            );

            return ResponseEntity.ok(Map.of(
                    "status", Map.of("code", "OK_0000", "message", "시스템 상태 조회 완료"),
                    "response", systemStatus
            ));

        } catch (Exception e) {
            log.error("❌ 시스템 상태 조회 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", Map.of("code", "ERR_5000", "message", "시스템 상태 조회 실패"),
                    "error", e.getMessage()
            ));
        }
    }

    // ==================== Private Methods ====================

    /**
     * 외부 API 비동기 호출 (백그라운드 처리)
     */
    private void processVideoAsyncWithExternalApi(String sessionKey, String savedFilePath) {
        try {
            log.info("🚀 외부 API 비동기 처리 시작 - 세션: {}", sessionKey);

            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session == null || session.isExpired()) {
                log.warn("⚠️ 세션 없음 또는 만료 - 외부 API 호출 중단: {}", sessionKey);
                return;
            }

            // TTL 갱신
            sessionManager.extendSessionTtl(sessionKey);

            // TODO: 실제 외부 API 호출 로직 구현
            // 현재는 시뮬레이션
            simulateExternalApiCall(sessionKey, savedFilePath);

        } catch (Exception e) {
            log.error("❌ 외부 API 비동기 처리 오류 - 세션: {}", sessionKey, e);

            // 오류 상태 업데이트
            MemorialVideoSession session = sessionManager.getSession(sessionKey);
            if (session != null) {
                session.setError();
                sessionManager.saveSession(session);

                // 웹소켓으로 오류 알림
                Map<String, Object> errorMessage = Map.of(
                        "type", "ERROR",
                        "code", "EXTERNAL_API_ERROR",
                        "message", "외부 API 처리 중 오류가 발생했습니다: " + e.getMessage(),
                        "sessionKey", sessionKey
                );
                webSocketHandler.sendMessageToSession(sessionKey, errorMessage);
            }
        }
    }

    /**
     * 외부 API 호출 시뮬레이션 (실제 구현 시 제거)
     */
    private void simulateExternalApiCall(String sessionKey, String savedFilePath) {
        try {
            log.info("🎭 외부 API 호출 시뮬레이션 시작 - 세션: {}", sessionKey);

            // 5-15초 처리 시뮬레이션
            int processingTime = 5000 + (int)(Math.random() * 10000);
            Thread.sleep(processingTime);

            // 90% 성공률 시뮬레이션
            if (Math.random() < 0.9) {
                // 성공 시뮬레이션
                String mockResponseVideoUrl = "https://aicut.newstomato.com/remember/static/response_" +
                        sessionKey.substring(sessionKey.length() - 8) + ".mp4";

                log.info("✅ 외부 API 시뮬레이션 성공 - 세션: {}, 응답 URL: {}", sessionKey, mockResponseVideoUrl);

                // 콜백 시뮬레이션 (실제로는 외부 API가 콜백 호출)
                Map<String, Object> callbackData = Map.of("videoUrl", mockResponseVideoUrl);
                receiveResponseVideo(sessionKey, callbackData);

            } else {
                // 실패 시뮬레이션
                throw new RuntimeException("외부 API 처리 실패 (시뮬레이션)");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("외부 API 처리 중단", e);
        } catch (Exception e) {
            throw new RuntimeException("외부 API 처리 실패", e);
        }
    }
}
