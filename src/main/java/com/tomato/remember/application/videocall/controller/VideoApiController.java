// VideoApiController.java - 외부 API 호출 활성화 버전

package com.tomato.remember.application.videocall.controller;

import com.tomato.remember.application.videocall.service.ExternalVideoApiService;
import com.tomato.remember.common.util.FileStorageService;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/video")
@CrossOrigin(origins = "*")
public class VideoApiController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ExternalVideoApiService externalVideoApiService;

    // 키 기반 SSE 연결 관리
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    private final Map<String, VideoCallSession> activeSessions = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // 비디오 콜 세션 정보
    public static class VideoCallSession {

        private String sessionKey;
        private String contactName;
        private LocalDateTime createdAt;
        private String status; // WAITING, PROCESSING, COMPLETED, ERROR
        private String savedFilePath; // 저장된 파일 경로

        public VideoCallSession(String sessionKey, String contactName) {
            this.sessionKey = sessionKey;
            this.contactName = contactName;
            this.createdAt = LocalDateTime.now();
            this.status = "WAITING";
        }

        // getters and setters
        public String getSessionKey() {
            return sessionKey;
        }

        public String getContactName() {
            return contactName;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getSavedFilePath() {
            return savedFilePath;
        }

        public void setSavedFilePath(String savedFilePath) {
            this.savedFilePath = savedFilePath;
        }
    }

    /**
     * 1. 비디오 콜 세션 생성 API
     */
    @PostMapping("/create-session")
    public ResponseEntity<?> createVideoCallSession(@RequestBody Map<String, Object> request) {
        try {
            String contactName = (String) request.get("contactName");
            if (contactName == null || contactName.trim().isEmpty()) {
                contactName = "Unknown";
            }

            String sessionKey = generateUniqueSessionKey();
            VideoCallSession session = new VideoCallSession(sessionKey, contactName);
            activeSessions.put(sessionKey, session);

            log.info("새 비디오 콜 세션 생성: {} (연락처: {})", sessionKey, contactName);

            return ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "세션 생성 완료"),
                "response", Map.of(
                    "sessionKey", sessionKey,
                    "contactName", contactName,
                    "createdAt", session.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            ));

        } catch (Exception e) {
            log.error("세션 생성 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", Map.of("code", "ERR_5000", "message", "세션 생성 실패"),
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 2. 키 기반 SSE 스트림 연결 API
     */
    @GetMapping(value = "/stream/{sessionKey}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBySessionKey(@PathVariable String sessionKey) {
        log.info("SSE 연결 요청: {}", sessionKey);

        VideoCallSession session = activeSessions.get(sessionKey);
        if (session == null) {
            log.warn("유효하지 않은 세션 키: {}", sessionKey);
            SseEmitter errorEmitter = new SseEmitter(1000L);
            try {
                errorEmitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("error", "유효하지 않은 세션 키입니다.")));
                errorEmitter.complete();
            } catch (IOException e) {
                log.error("SSE 에러 전송 실패", e);
            }
            return errorEmitter;
        }

        // ✅ SSE 연결 생성 (무제한 타임아웃으로 장시간 유지)
        SseEmitter emitter = new SseEmitter(0L); // 0L = 무제한 타임아웃
        sseEmitters.put(sessionKey, emitter);

        // 연결 완료 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of(
                    "message", "SSE 연결 완료",
                    "sessionKey", sessionKey,
                    "contactName", session.getContactName(),
                    "timestamp", System.currentTimeMillis(),
                    "keepAlive", true // ✅ 장시간 연결 유지 표시
                )));
        } catch (IOException e) {
            log.error("연결 완료 이벤트 전송 실패", e);
        }

        // ✅ 연결 상태 관리 - 세션은 보존
        emitter.onCompletion(() -> {
            sseEmitters.remove(sessionKey);
            log.info("SSE 연결 종료: {} (세션은 유지됨)", sessionKey);
            // ✅ 세션은 제거하지 않음 - 재연결 가능하도록 유지
        });

        emitter.onTimeout(() -> {
            sseEmitters.remove(sessionKey);
            log.warn("SSE 연결 타임아웃: {} (세션은 유지됨)", sessionKey);
            // ✅ 세션은 제거하지 않음 - 재연결 가능하도록 유지
        });

        emitter.onError((ex) -> {
            sseEmitters.remove(sessionKey);
            log.error("SSE 연결 오류: {} (세션은 유지됨)", sessionKey, ex);
            // ✅ 세션은 제거하지 않음 - 재연결 가능하도록 유지
        });

        return emitter;
    }

    /**
     * 3. 키 기반 영상 업로드 API 파일 저장 후 즉시 200 OK 응답, 백그라운드에서 외부 API 호출
     */
    @PostMapping(value = "/process/{sessionKey}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processVideoWithKey(
        @PathVariable String sessionKey,
        @RequestParam("video") MultipartFile videoFile) {
        try {
            // 세션 키 유효성 검증
            VideoCallSession session = activeSessions.get(sessionKey);
            if (session == null) {
                return ResponseEntity.status(400).body(Map.of(
                    "status", Map.of("code", "ERR_4000", "message", "유효하지 않은 세션 키입니다.")
                ));
            }

            // 세션 상태 업데이트
            session.setStatus("PROCESSING");
            log.info("영상 처리 시작 - 세션: {}, 파일: {}", sessionKey, videoFile.getOriginalFilename());

            // 파일 저장 및 변환
            String savedFilePath = fileStorageService.uploadVideoCallRecording(videoFile, sessionKey);
            session.setSavedFilePath(savedFilePath);

            log.info("파일 저장 완료 - 세션: {}, 경로: {}", sessionKey, savedFilePath);

            // ✅ 즉시 200 OK 응답 (클라이언트는 대기 영상 계속 재생)
            ResponseEntity<?> response = ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "영상 업로드 완료"),
                "response", Map.of(
                    "sessionKey", sessionKey,
                    "filePath", savedFilePath,
                    "contactName", session.getContactName(),
                    "status", "UPLOADED" // 업로드 완료 상태
                )
            ));

            // ✅ 백그라운드에서 외부 API 호출 (비동기)
            executorService.submit(() -> {
                processVideoAsyncWithExternalApi(sessionKey, savedFilePath);
            });

            return response;

        } catch (Exception e) {
            log.error("영상 처리 실패 - 세션: {}", sessionKey, e);

            VideoCallSession session = activeSessions.get(sessionKey);
            if (session != null) {
                session.setStatus("ERROR");
            }

            return ResponseEntity.status(500).body(Map.of(
                "status", Map.of("code", "ERR_5000", "message", "영상 처리 실패"),
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 4. 외부 API에서 콜백으로 응답 영상 전송받는 API
     */
    @PostMapping("/send-response/{sessionKey}")
    public ResponseEntity<?> sendResponseToUser(
        @PathVariable String sessionKey,
        @RequestBody Map<String, Object> responseData) {
        try {
            String responseVideoUrl = (String) responseData.get("videoUrl");

            log.info("응답 영상 수신 - 세션: {}, URL: {}", sessionKey, responseVideoUrl);

            // 해당 세션의 SSE로 응답 전송
            SseEmitter emitter = sseEmitters.get(sessionKey);
            if (emitter != null) {
                VideoCallSession session = activeSessions.get(sessionKey);

                emitter.send(SseEmitter.event()
                    .name("response")
                    .data(Map.of(
                        "videoUrl", responseVideoUrl,
                        "sessionKey", sessionKey,
                        "contactName", session != null ? session.getContactName() : "Unknown",
                        "timestamp", System.currentTimeMillis()
                    )));

                // 세션 상태 업데이트
                if (session != null) {
                    session.setStatus("COMPLETED");
                }

                log.info("응답 영상 전송 완료 - 세션: {}", sessionKey);

                return ResponseEntity.ok(Map.of(
                    "status", Map.of("code", "OK_0000", "message", "응답 전송 완료"),
                    "response", Map.of("sessionKey", sessionKey)
                ));
            } else {
                log.warn("연결된 세션을 찾을 수 없음 - 세션: {}", sessionKey);
                return ResponseEntity.status(404).body(Map.of(
                    "status", Map.of("code", "ERR_4040", "message", "연결된 세션을 찾을 수 없습니다")
                ));
            }

        } catch (Exception e) {
            log.error("응답 전송 실패 - 세션: {}", sessionKey, e);
            return ResponseEntity.status(500).body(Map.of(
                "status", Map.of("code", "ERR_5000", "message", "응답 전송 실패"),
                "error", e.getMessage()
            ));
        }
    }

    // ==================== 상태 조회 API들 ====================

    @GetMapping("/session/{sessionKey}")
    public ResponseEntity<?> getSessionStatus(@PathVariable String sessionKey) {
        VideoCallSession session = activeSessions.get(sessionKey);

        if (session == null) {
            return ResponseEntity.status(404).body(Map.of(
                "status", Map.of("code", "ERR_4040", "message", "세션을 찾을 수 없습니다")
            ));
        }

        boolean isConnected = sseEmitters.containsKey(sessionKey);

        return ResponseEntity.ok(Map.of(
            "status", Map.of("code", "OK_0000", "message", "세션 조회 완료"),
            "response", Map.of(
                "sessionKey", sessionKey,
                "contactName", session.getContactName(),
                "status", session.getStatus(),
                "createdAt", session.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "isConnected", isConnected,
                "savedFilePath", session.getSavedFilePath()
            )
        ));
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> getActiveSessions() {
        return ResponseEntity.ok(Map.of(
            "status", Map.of("code", "OK_0000", "message", "활성 세션 목록"),
            "response", Map.of(
                "totalSessions", activeSessions.size(),
                "connectedSessions", sseEmitters.size(),
                "sessions", activeSessions.values()
            )
        ));
    }

    @GetMapping("/external-api/status")
    public ResponseEntity<?> getExternalApiStatus() {
        try {
            boolean isConfigValid = externalVideoApiService.validateConfiguration();

            return ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "외부 API 상태 조회 완료"),
                "response", Map.of(
                    "configurationValid", isConfigValid,
                    "baseUrl", externalVideoApiService.getExternalApiBaseUrl(),
                    "endpoint", externalVideoApiService.getProcessEndpoint(),
                    "timeout", externalVideoApiService.getTimeoutSeconds(),
                    "retryCount", externalVideoApiService.getRetryCount()
                )
            ));
        } catch (Exception e) {
            log.error("외부 API 상태 조회 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", Map.of("code", "ERR_5000", "message", "외부 API 상태 조회 실패"),
                "error", e.getMessage()
            ));
        }
    }

    // ==================== Private Methods ====================

    private String generateUniqueSessionKey() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "VC_" + timestamp + "_" + uuid;
    }

    /**
     * ✅ 외부 API 비동기 호출 (백그라운드 처리)
     */
    private void processVideoAsyncWithExternalApi(String sessionKey, String savedFilePath) {
        try {
            log.info("외부 API 비동기 처리 시작 - 세션: {}", sessionKey);

            VideoCallSession session = activeSessions.get(sessionKey);
            if (session == null) {
                log.error("세션을 찾을 수 없음 - 세션: {}", sessionKey);
                return;
            }

            // ✅ 실제 외부 API 호출
            externalVideoApiService.sendVideoToExternalApiAsync(
                sessionKey,
                savedFilePath,
                // ✅ 성공 콜백 - 200 OK 받으면 조용히 완료
                (response) -> {
                    log.info("✅ 외부 API 전송 완료 - 세션: {}, 상태: {}",
                        sessionKey, response.getStatusCode());
                    session.setStatus("PROCESSING_EXTERNAL");

                    // 🚫 SSE 이벤트 전송하지 않음
                    // 외부 API가 처리 완료 후 /send-response/{sessionKey} 콜백 호출할 때까지 대기
                },
                // ❌ 실패 콜백 - 전송 자체가 실패한 경우만
                (error) -> {
                    log.error("❌ 외부 API 전송 실패 - 세션: {}", sessionKey, error);
                    session.setStatus("ERROR");
                    sendErrorToSession(sessionKey, "외부 API 전송 실패: " + error.getMessage());
                }
            );

        } catch (Exception e) {
            log.error("비동기 영상 처리 오류 - 세션: {}", sessionKey, e);
            sendErrorToSession(sessionKey, "영상 처리 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 특정 세션에 오류 전송
     */
    private void sendErrorToSession(String sessionKey, String errorMessage) {
        SseEmitter emitter = sseEmitters.get(sessionKey);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of(
                        "error", errorMessage,
                        "sessionKey", sessionKey,
                        "timestamp", System.currentTimeMillis()
                    )));

                VideoCallSession session = activeSessions.get(sessionKey);
                if (session != null) {
                    session.setStatus("ERROR");
                }

            } catch (IOException e) {
                log.error("에러 전송 실패 - 세션: {}", sessionKey, e);
                sseEmitters.remove(sessionKey);
            }
        }
    }

    /**
     * SSE 하트비트 전송 (30초마다) 클라이언트 연결 유지를 위한 주기적 신호
     */
    @Scheduled(fixedRate = 20000) // ✅ 30초 → 20초로 단축 (더 안정적)
    public void sendHeartbeat() {
        if (sseEmitters.isEmpty()) {
            return;
        }

        log.debug("💓 하트비트 전송 시작 - 연결된 세션: {}", sseEmitters.size());

        var emittersCopy = Map.copyOf(sseEmitters);

        emittersCopy.forEach((sessionKey, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data(Map.of(
                        "timestamp", System.currentTimeMillis(),
                        "sessionKey", sessionKey,
                        "message", "connection_alive",
                        "serverTime", LocalDateTime.now().toString() // ✅ 서버 시간 추가
                    )));

                log.debug("💓 하트비트 전송 성공: {}", sessionKey);

            } catch (IOException e) {
                log.warn("💔 하트비트 전송 실패 - 연결 제거: {}", sessionKey, e);
                sseEmitters.remove(sessionKey);

                // ✅ 세션은 유지하고 SSE만 정리 (재연결 가능)
                VideoCallSession session = activeSessions.get(sessionKey);
                if (session != null) {
                    log.info("🔄 세션 {} SSE 연결 끊김 (재연결 가능)", sessionKey);
                }
            } catch (Exception e) {
                log.error("💔 하트비트 전송 중 예상치 못한 오류: {}", sessionKey, e);
                sseEmitters.remove(sessionKey);
            }
        });

        log.debug("💓 하트비트 전송 완료 - 활성 연결: {}", sseEmitters.size());
    }

    /**
     * ✅ SSE 연결 상태 확인 API (디버깅용)
     */
    @GetMapping("/sse-status")
    public ResponseEntity<?> getSseStatus() {
        Map<String, Object> statusInfo = new HashMap<>();

        sseEmitters.forEach((sessionKey, emitter) -> {
            VideoCallSession session = activeSessions.get(sessionKey);
            statusInfo.put(sessionKey, Map.of(
                "hasSession", session != null,
                "sessionStatus", session != null ? session.getStatus() : "NO_SESSION",
                "createdAt", session != null ? session.getCreatedAt().toString() : "UNKNOWN",
                "connectionTime", System.currentTimeMillis()
            ));
        });

        return ResponseEntity.ok(Map.of(
            "status", Map.of("code", "OK_0000", "message", "SSE 연결 상태 조회 완료"),
            "response", Map.of(
                "totalSseConnections", sseEmitters.size(),
                "totalSessions", activeSessions.size(),
                "connections", statusInfo,
                "timestamp", System.currentTimeMillis()
            )
        ));
    }

    /**
     * 연결 상태 모니터링 (5분마다) 비정상적으로 남아있는 연결들 정리
     */
    @Scheduled(fixedRate = 300000) // ✅ 5분마다 유지 (원래대로)
    public void monitorConnections() {
        log.debug("🔍 연결 상태 모니터링 시작");

        int totalSessions = activeSessions.size();
        int activeSseConnections = sseEmitters.size();

        if (totalSessions > 0 || activeSseConnections > 0) {
            log.info("📊 연결 상태 - 전체 세션: {}, 활성 SSE: {}", totalSessions, activeSseConnections);
        }

        // ✅ SSE 연결 유지를 위한 보수적 정리 (2시간)
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(2);

        // 매우 오래된 세션만 정리 (SSE 연결이 끊어지고 완료된 것만)
        int removedSessions = 0;
        var sessionIterator = activeSessions.entrySet().iterator();
        while (sessionIterator.hasNext()) {
            var entry = sessionIterator.next();
            VideoCallSession session = entry.getValue();
            String sessionKey = entry.getKey();

            boolean isVeryOld = session.getCreatedAt().isBefore(cutoffTime);
            boolean hasNoConnection = ! sseEmitters.containsKey(sessionKey);
            boolean isCompleted = "COMPLETED".equals(session.getStatus()) ||
                "ERROR".equals(session.getStatus()) ||
                "CLEANUP".equals(session.getStatus());

            // ✅ 매우 보수적인 정리: 2시간 이상 + SSE 연결 없음 + 완료 상태
            if (isVeryOld && hasNoConnection && isCompleted) {
                sessionIterator.remove();
                removedSessions++;
                log.info("🗑️ 오래된 완료 세션 정리: {} (생성: {}, 상태: {})",
                    sessionKey, session.getCreatedAt(), session.getStatus());
            }
            // ✅ 활성 SSE가 있는 세션은 절대 정리하지 않음
            else if (sseEmitters.containsKey(sessionKey)) {
                log.debug("🔗 활성 SSE 세션 유지: {} (생성: {})",
                    sessionKey, session.getCreatedAt());
            }
        }

        // ✅ 고아 SSE 연결 정리 (세션은 없지만 SSE만 남은 경우)
        int removedSSE = 0;
        var sseIterator = sseEmitters.entrySet().iterator();
        while (sseIterator.hasNext()) {
            var entry = sseIterator.next();
            String sessionKey = entry.getKey();
            SseEmitter emitter = entry.getValue();

            if (! activeSessions.containsKey(sessionKey)) {
                try {
                    emitter.complete();
                    sseIterator.remove();
                    removedSSE++;
                    log.info("🗑️ 고아 SSE 연결 정리: {}", sessionKey);
                } catch (Exception e) {
                    log.debug("SSE 정리 중 예외 (무시됨): {}", e.getMessage());
                    sseIterator.remove();
                }
            }
        }

        if (removedSessions > 0 || removedSSE > 0) {
            log.info("✅ 연결 상태 모니터링 완료 - 정리된 세션: {}, 정리된 SSE: {}, 남은 세션: {}, 활성 SSE: {}",
                removedSessions, removedSSE, activeSessions.size(), sseEmitters.size());
        }
    }

    /**
     * 클라이언트 세션 정리 API
     */
    @PostMapping("/session/{sessionKey}/cleanup")
    public ResponseEntity<?> cleanupSession(@PathVariable String sessionKey) {
        try {
            log.info("🧹 세션 정리 요청 - 세션: {}", sessionKey);

            // SSE 연결 정리
            SseEmitter emitter = sseEmitters.remove(sessionKey);
            if (emitter != null) {
                try {
                    emitter.complete();
                    log.info("SSE 연결 정리 완료 - 세션: {}", sessionKey);
                } catch (Exception e) {
                    log.debug("SSE 정리 중 예외 (무시됨): {}", e.getMessage());
                }
            }

            // 세션 정보 조회 후 상태 업데이트
            VideoCallSession session = activeSessions.get(sessionKey);
            if (session != null) {
                session.setStatus("CLEANUP");
                log.info("세션 상태를 CLEANUP으로 변경 - 세션: {}", sessionKey);
            }

            return ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "세션 정리 완료"),
                "response", Map.of("sessionKey", sessionKey)
            ));

        } catch (Exception e) {
            log.error("세션 정리 실패 - 세션: {}", sessionKey, e);
            return ResponseEntity.status(500).body(Map.of(
                "status", Map.of("code", "ERR_5000", "message", "세션 정리 실패"),
                "error", e.getMessage()
            ));
        }
    }
}