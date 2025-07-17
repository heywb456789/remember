// VideoApiController.java - 외부 API 호출 활성화 버전

package com.tomato.remember.application.videocall.controller;

import com.tomato.remember.application.videocall.service.ExternalVideoApiService;
import com.tomato.remember.common.util.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
        public String getSessionKey() { return sessionKey; }
        public String getContactName() { return contactName; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getSavedFilePath() { return savedFilePath; }
        public void setSavedFilePath(String savedFilePath) { this.savedFilePath = savedFilePath; }
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

        // SSE 연결 생성 (5분 타임아웃)
        SseEmitter emitter = new SseEmitter(300000L);
        sseEmitters.put(sessionKey, emitter);

        // 연결 완료 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of(
                    "message", "SSE 연결 완료",
                    "sessionKey", sessionKey,
                    "contactName", session.getContactName(),
                    "timestamp", System.currentTimeMillis()
                )));
        } catch (IOException e) {
            log.error("연결 완료 이벤트 전송 실패", e);
        }

        // 연결 상태 관리
        emitter.onCompletion(() -> {
            sseEmitters.remove(sessionKey);
            log.info("SSE 연결 종료: {}", sessionKey);
        });

        emitter.onTimeout(() -> {
            sseEmitters.remove(sessionKey);
            activeSessions.remove(sessionKey);
            log.warn("SSE 연결 타임아웃: {}", sessionKey);
        });

        emitter.onError((ex) -> {
            sseEmitters.remove(sessionKey);
            log.error("SSE 연결 오류: {}", sessionKey, ex);
        });

        return emitter;
    }

    /**
     * 3. 키 기반 영상 업로드 API
     * 파일 저장 후 즉시 200 OK 응답, 백그라운드에서 외부 API 호출
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
                // 성공 콜백 - 외부 API가 성공적으로 요청을 받았을 때
                (response) -> {
                    log.info("외부 API 요청 전송 성공 - 세션: {}, 응답: {}", sessionKey, response);
                    session.setStatus("PROCESSING_EXTERNAL");

                    // 여기서는 단순히 요청 전송 성공만 로깅
                    // 실제 결과는 외부 API가 콜백으로 send-response API를 호출할 때 처리됨
                },
                // 실패 콜백 - 외부 API 호출 자체가 실패했을 때
                (error) -> {
                    log.error("외부 API 요청 전송 실패 - 세션: {}", sessionKey, error);
                    session.setStatus("ERROR");
                    sendErrorToSession(sessionKey, "외부 API 연결 실패: " + error.getMessage());
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
}