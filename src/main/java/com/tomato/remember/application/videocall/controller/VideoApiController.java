// VideoApiController.java - 3시간 TTL + SessionStorage 지원 버전

package com.tomato.remember.application.videocall.controller;

import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.application.videocall.dto.VideoCallFeedbackRequestDTO;
import com.tomato.remember.application.videocall.dto.VideoCallFeedbackResponseDTO;
import com.tomato.remember.application.videocall.entity.VideoCallSampleReview;
import com.tomato.remember.application.videocall.service.ExternalVideoApiService;
import com.tomato.remember.application.videocall.service.VideoCallSampleReviewService;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.exception.APIException;
import com.tomato.remember.common.util.FileStorageService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequiredArgsConstructor
public class VideoApiController {

    private final FileStorageService fileStorageService;

    private final ExternalVideoApiService externalVideoApiService;

    // 키 기반 SSE 연결 관리
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    private final Map<String, VideoCallSession> activeSessions = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // 비디오 콜 세션 정보 (3시간 TTL 지원)
    public static class VideoCallSession {

        private String sessionKey;
        private String contactName;
        private LocalDateTime createdAt;
        private LocalDateTime lastActivity;
        private String status; // WAITING, PROCESSING, COMPLETED, ERROR, CLEANUP
        private String savedFilePath;

        // 3시간 TTL
        private static final long SESSION_TTL_HOURS = 3;

        public VideoCallSession(String sessionKey, String contactName) {
            this.sessionKey = sessionKey;
            this.contactName = contactName;
            this.createdAt = LocalDateTime.now();
            this.lastActivity = LocalDateTime.now();
            this.status = "WAITING";
        }

        public boolean isExpired() {
            return lastActivity.isBefore(LocalDateTime.now().minusHours(SESSION_TTL_HOURS));
        }

        public void updateActivity() {
            this.lastActivity = LocalDateTime.now();
        }

        public long getAgeInMinutes() {
            return Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        }

        public long getInactiveMinutes() {
            return Duration.between(lastActivity, LocalDateTime.now()).toMinutes();
        }

        public double getRemainingHours() {
            long activeMinutes = Duration.between(lastActivity, LocalDateTime.now()).toMinutes();
            return Math.max(0, SESSION_TTL_HOURS - (activeMinutes / 60.0));
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

        public LocalDateTime getLastActivity() {
            return lastActivity;
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

            log.info("새 비디오 콜 세션 생성: {} (연락처: {}, TTL: 3시간)", sessionKey, contactName);

            return ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "세션 생성 완료"),
                "response", Map.of(
                    "sessionKey", sessionKey,
                    "contactName", contactName,
                    "createdAt", session.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "ttlHours", 3,
                    "maxInactiveMinutes", 180
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
     * 2. 키 기반 SSE 스트림 연결 API (3시간 TTL 지원)
     */
    @GetMapping(value = "/stream/{sessionKey}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamBySessionKey(@PathVariable String sessionKey) {
    log.info("SSE 연결 요청: {}", sessionKey);

    VideoCallSession session = activeSessions.get(sessionKey);
    if (session == null) {
        log.warn("유효하지 않은 세션 키: {}", sessionKey);
        return createErrorEmitter("유효하지 않은 세션 키입니다.");
    }

    // 세션 만료 확인
    if (session.isExpired()) {
        log.warn("만료된 세션 키: {} (생성: {}, 마지막활동: {})",
            sessionKey, session.getCreatedAt(), session.getLastActivity());

        activeSessions.remove(sessionKey);
        sseEmitters.remove(sessionKey);

        return createErrorEmitter("세션이 만료되었습니다. 새로 시작해주세요.");
    }

    // 🔧 기존 SSE 연결 정리 (재연결 시)
    SseEmitter existingEmitter = sseEmitters.get(sessionKey);
    if (existingEmitter != null) {
        log.info("🔄 기존 SSE 연결 정리 후 재연결: {}", sessionKey);
        try {
            existingEmitter.complete();
        } catch (Exception e) {
            log.debug("기존 SSE 정리 중 예외 (무시됨): {}", e.getMessage());
        }
        sseEmitters.remove(sessionKey);
    }

    // 세션 활동 시간 업데이트
    session.updateActivity();

    // 새 SSE 연결 생성
    SseEmitter emitter = new SseEmitter(0L);
    sseEmitters.put(sessionKey, emitter);

    // 연결 완료 이벤트 전송
    try {
        Map<String, Object> connectedData = new HashMap<>();
        connectedData.put("message", "SSE 연결 완료");
        connectedData.put("sessionKey", sessionKey);
        connectedData.put("contactName", session.getContactName());
        connectedData.put("timestamp", System.currentTimeMillis());
        connectedData.put("sessionAge", session.getAgeInMinutes());
        connectedData.put("remainingHours", session.getRemainingHours());
        connectedData.put("keepAlive", true);
        connectedData.put("reconnected", existingEmitter != null); // 재연결 여부 표시

        emitter.send(SseEmitter.event()
            .name("connected")
            .data(connectedData));

        log.info("✅ SSE 연결 성공: {} (재연결: {})", sessionKey, existingEmitter != null);

    } catch (IOException e) {
        log.error("연결 완료 이벤트 전송 실패", e);
        sseEmitters.remove(sessionKey);
        return createErrorEmitter("연결 이벤트 전송 실패");
    }

    // 🔧 개선된 연결 상태 관리
    emitter.onCompletion(() -> {
        sseEmitters.remove(sessionKey);
        log.info("SSE 연결 완료됨: {} (세션은 유지)", sessionKey);
    });

    emitter.onTimeout(() -> {
        sseEmitters.remove(sessionKey);
        log.warn("SSE 연결 타임아웃: {} (세션은 유지)", sessionKey);
    });

    emitter.onError((ex) -> {
        sseEmitters.remove(sessionKey);
        log.error("SSE 연결 오류: {} (세션은 유지) - 오류: {}", sessionKey, ex.getMessage());
    });

    return emitter;
}

    /**
     * 3. 키 기반 영상 업로드 API
     */
    @PostMapping(value = "/process/{sessionKey}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processVideoWithKey(
        @PathVariable String sessionKey,
        @RequestParam(value = "contactKey", required = false, defaultValue = "kimgeuntae") String contactKey,
        @RequestParam("video") MultipartFile videoFile) {
        try {

            log.info("비디오 처리 요청 - 세션: {},대상: {}, 파일크기: {}",
                sessionKey,
                contactKey,
                videoFile.getSize());

            // 세션 키 유효성 검증
            VideoCallSession session = activeSessions.get(sessionKey);
            if (session == null) {
                return ResponseEntity.status(400).body(Map.of(
                    "status", Map.of("code", "ERR_4000", "message", "유효하지 않은 세션 키입니다.")
                ));
            }

            // 세션 만료 확인
            if (session.isExpired()) {
                activeSessions.remove(sessionKey);
                sseEmitters.remove(sessionKey);
                return ResponseEntity.status(410).body(Map.of(
                    "status", Map.of("code", "ERR_4100", "message", "세션이 만료되었습니다.")
                ));
            }

            // 세션 상태 및 활동 시간 업데이트
            session.setStatus("PROCESSING");
            session.updateActivity();

            log.info("영상 처리 시작 - 세션: {} (나이: {}분), 파일: {}",
                sessionKey, session.getAgeInMinutes(), videoFile.getOriginalFilename());

            // 파일 저장 및 변환
            String savedFilePath = fileStorageService.uploadVideoCallRecording(videoFile, sessionKey);
            session.setSavedFilePath(savedFilePath);

            log.info("파일 저장 완료 - 세션: {}, 경로: {}", sessionKey, savedFilePath);

            // 즉시 200 OK 응답
            ResponseEntity<?> response = ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "영상 업로드 완료"),
                "response", Map.of(
                    "sessionKey", sessionKey,
                    "filePath", savedFilePath,
                    "contactName", session.getContactName(),
                    "status", "UPLOADED",
                    "sessionAge", session.getAgeInMinutes(),
                    "remainingHours", session.getRemainingHours()
                )
            ));

            // 백그라운드에서 외부 API 호출
            executorService.submit(() -> {
                processVideoAsyncWithExternalApi(sessionKey, savedFilePath, contactKey);
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

            // 세션 유효성 확인
            VideoCallSession session = activeSessions.get(sessionKey);
            if (session == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "status", Map.of("code", "ERR_4040", "message", "세션을 찾을 수 없습니다")
                ));
            }

            // 세션 만료 확인
            if (session.isExpired()) {
                activeSessions.remove(sessionKey);
                sseEmitters.remove(sessionKey);
                return ResponseEntity.status(410).body(Map.of(
                    "status", Map.of("code", "ERR_4100", "message", "세션이 만료되었습니다")
                ));
            }

            // 세션 활동 시간 업데이트
            session.updateActivity();

            // SSE로 응답 전송
            SseEmitter emitter = sseEmitters.get(sessionKey);
            if (emitter != null) {
                // SSE 전송 데이터 구성
                Map<String, Object> sseData = new HashMap<>();
                sseData.put("videoUrl", responseVideoUrl);
                sseData.put("sessionKey", sessionKey);
                sseData.put("contactName", session.getContactName());
                sseData.put("timestamp", System.currentTimeMillis());
                sseData.put("sessionAge", session.getAgeInMinutes());
                sseData.put("remainingHours", session.getRemainingHours());

                emitter.send(SseEmitter.event()
                    .name("response")
                    .data(sseData));

                // 세션 상태 업데이트
                session.setStatus("COMPLETED");

                log.info("응답 영상 전송 완료 - 세션: {} (나이: {}분)", sessionKey, session.getAgeInMinutes());

                // 응답 데이터 구성
                Map<String, Object> responseResult = new HashMap<>();
                responseResult.put("sessionKey", sessionKey);
                responseResult.put("sessionAge", session.getAgeInMinutes());
                responseResult.put("remainingHours", session.getRemainingHours());

                return ResponseEntity.ok(Map.of(
                    "status", Map.of("code", "OK_0000", "message", "응답 전송 완료"),
                    "response", responseResult
                ));
            } else {
                log.warn("연결된 SSE를 찾을 수 없음 - 세션: {}", sessionKey);
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

    /**
     * 세션 상태 조회 (3시간 TTL 정보 포함)
     */
    @GetMapping("/session/{sessionKey}")
    public ResponseEntity<?> getSessionStatus(@PathVariable String sessionKey) {
        VideoCallSession session = activeSessions.get(sessionKey);

        if (session == null) {
            return ResponseEntity.status(404).body(Map.of(
                "status", Map.of("code", "ERR_4040", "message", "세션을 찾을 수 없습니다")
            ));
        }

        // 세션 만료 확인
        if (session.isExpired()) {
            // 만료된 세션 즉시 정리
            activeSessions.remove(sessionKey);
            sseEmitters.remove(sessionKey);

            log.info("만료된 세션 정리: {} (생성: {}, 마지막활동: {})",
                sessionKey, session.getCreatedAt(), session.getLastActivity());

            // 만료된 세션 응답용 Map 생성
            Map<String, Object> expiredResponse = new HashMap<>();
            expiredResponse.put("sessionKey", sessionKey);
            expiredResponse.put("expired", true);
            expiredResponse.put("createdAt", session.getCreatedAt());
            expiredResponse.put("lastActivity", session.getLastActivity());
            expiredResponse.put("inactiveMinutes", session.getInactiveMinutes());

            return ResponseEntity.status(410).body(Map.of(
                "status", Map.of("code", "ERR_4100", "message", "세션이 만료되었습니다"),
                "response", expiredResponse
            ));
        }

        // 세션 활동 시간 업데이트
        session.updateActivity();

        boolean isConnected = sseEmitters.containsKey(sessionKey);

        // 📦 HashMap을 사용하여 응답 데이터 구성
        Map<String, Object> sessionResponse = new HashMap<>();
        sessionResponse.put("sessionKey", sessionKey);
        sessionResponse.put("contactName", session.getContactName());
        sessionResponse.put("status", session.getStatus());
        sessionResponse.put("createdAt", session.getCreatedAt());
        sessionResponse.put("lastActivity", session.getLastActivity());
        sessionResponse.put("ageInMinutes", session.getAgeInMinutes());
        sessionResponse.put("inactiveMinutes", session.getInactiveMinutes());
        sessionResponse.put("remainingHours", session.getRemainingHours());
        sessionResponse.put("isExpired", session.isExpired());
        sessionResponse.put("isConnected", isConnected);
        sessionResponse.put("savedFilePath", session.getSavedFilePath());
        sessionResponse.put("ttlHours", 3);

        return ResponseEntity.ok(Map.of(
            "status", Map.of("code", "OK_0000", "message", "세션 조회 완료"),
            "response", sessionResponse
        ));
    }

    /**
     * 활성 세션 목록 조회
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getActiveSessions() {
        // 만료된 세션 사전 정리
        cleanupExpiredSessionsNow();

        // 세션 목록 구성
        List<Map<String, Object>> sessionList = activeSessions.values().stream()
            .map(session -> {
                Map<String, Object> sessionInfo = new HashMap<>();
                sessionInfo.put("sessionKey", session.getSessionKey());
                sessionInfo.put("contactName", session.getContactName());
                sessionInfo.put("status", session.getStatus());
                sessionInfo.put("ageInMinutes", session.getAgeInMinutes());
                sessionInfo.put("remainingHours", session.getRemainingHours());
                sessionInfo.put("isExpired", session.isExpired());
                sessionInfo.put("isConnected", sseEmitters.containsKey(session.getSessionKey()));
                return sessionInfo;
            })
            .toList();

        // 응답 데이터 구성
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("totalSessions", activeSessions.size());
        responseData.put("connectedSessions", sseEmitters.size());
        responseData.put("ttlHours", 3);
        responseData.put("sessions", sessionList);

        return ResponseEntity.ok(Map.of(
            "status", Map.of("code", "OK_0000", "message", "활성 세션 목록"),
            "response", responseData
        ));
    }

    /**
     * 외부 API 상태 조회
     */
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

    /**
     * 유니크한 세션 키 생성
     */
    private String generateUniqueSessionKey() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "VC_" + timestamp + "_" + uuid;
    }

    /**
     * 에러 SSE 이미터 생성
     */
    private SseEmitter createErrorEmitter(String errorMessage) {
        SseEmitter errorEmitter = new SseEmitter(1000L);
        try {
            errorEmitter.send(SseEmitter.event()
                .name("error")
                .data(Map.of(
                    "error", errorMessage,
                    "timestamp", System.currentTimeMillis()
                )));
            errorEmitter.complete();
        } catch (IOException e) {
            log.error("SSE 에러 전송 실패", e);
        }
        return errorEmitter;
    }

    /**
     * 외부 API 비동기 호출
     */
    private void processVideoAsyncWithExternalApi(String sessionKey, String savedFilePath, String contactKey) {
        try {
            log.info("외부 API 비동기 처리 시작 - 세션: {}", sessionKey);

            VideoCallSession session = activeSessions.get(sessionKey);
            if (session == null) {
                log.error("세션을 찾을 수 없음 - 세션: {}", sessionKey);
                return;
            }

            // 세션 만료 확인
            if (session.isExpired()) {
                log.warn("만료된 세션 - 외부 API 호출 중단: {}", sessionKey);
                return;
            }

            // 세션 활동 시간 업데이트
            session.updateActivity();

            // 외부 API 호출
            externalVideoApiService.sendVideoToExternalApiAsync(
                sessionKey,
                savedFilePath,
                contactKey,
                // 성공 콜백
                (response) -> {
                    log.info("✅ 외부 API 전송 완료 - 세션: {}, 상태: {}",
                        sessionKey, response.getStatusCode());

                    VideoCallSession currentSession = activeSessions.get(sessionKey);
                    if (currentSession != null) {
                        currentSession.setStatus("PROCESSING_EXTERNAL");
                        currentSession.updateActivity();
                    }
                },
                // 실패 콜백
                (error) -> {
                    log.error("❌ 외부 API 전송 실패 - 세션: {}", sessionKey, error);

                    VideoCallSession currentSession = activeSessions.get(sessionKey);
                    if (currentSession != null) {
                        currentSession.setStatus("ERROR");
                    }

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
     * 즉시 만료된 세션 정리
     */
    private void cleanupExpiredSessionsNow() {
        int removedSessions = 0;
        int removedSSE = 0;

        var sessionIterator = activeSessions.entrySet().iterator();
        while (sessionIterator.hasNext()) {
            var entry = sessionIterator.next();
            VideoCallSession session = entry.getValue();
            String sessionKey = entry.getKey();

            if (session.isExpired()) {
                sessionIterator.remove();
                removedSessions++;

                // 관련 SSE 연결도 정리
                SseEmitter emitter = sseEmitters.remove(sessionKey);
                if (emitter != null) {
                    try {
                        emitter.complete();
                        removedSSE++;
                    } catch (Exception e) {
                        log.debug("SSE 정리 중 예외 (무시됨): {}", e.getMessage());
                    }
                }

                log.info("만료된 세션 즉시 정리: {} (생성: {}, 마지막활동: {}, 비활성: {}분)",
                    sessionKey, session.getCreatedAt(), session.getLastActivity(), session.getInactiveMinutes());
            }
        }

        if (removedSessions > 0) {
            log.info("즉시 세션 정리 완료 - 세션: {}, SSE: {}", removedSessions, removedSSE);
        }
    }

    // ==================== 스케줄링 작업들 ====================

    /**
     * SSE 하트비트 전송 (20초마다)
     */
    @Scheduled(fixedRate = 20000)
public void sendHeartbeat() {
    if (sseEmitters.isEmpty()) {
        return;
    }

    log.debug("💓 하트비트 전송 시작 - 연결된 세션: {}", sseEmitters.size());

    // 🔧 ConcurrentModificationException 방지를 위한 복사본 생성
    Map<String, SseEmitter> emittersCopy = new HashMap<>(sseEmitters);
    List<String> disconnectedSessions = new ArrayList<>();

    emittersCopy.forEach((sessionKey, emitter) -> {
        try {
            // 세션 활동 시간 업데이트
            VideoCallSession session = activeSessions.get(sessionKey);
            if (session != null) {
                session.updateActivity();
            }

            // 🔧 하트비트 전송 전 연결 상태 확인
            if (isEmitterClosed(emitter)) {
                log.warn("💔 이미 닫힌 SSE 연결 감지: {}", sessionKey);
                disconnectedSessions.add(sessionKey);
                return;
            }

            emitter.send(SseEmitter.event()
                .name("heartbeat")
                .data(Map.of(
                    "timestamp", System.currentTimeMillis(),
                    "sessionKey", sessionKey,
                    "message", "connection_alive",
                    "serverTime", LocalDateTime.now().toString(),
                    "sessionAge", session != null ? session.getAgeInMinutes() : 0,
                    "remainingHours", session != null ? session.getRemainingHours() : 0
                )));

            log.debug("💓 하트비트 전송 성공: {} (나이: {}분)", sessionKey,
                session != null ? session.getAgeInMinutes() : 0);

        } catch (IOException e) {
            log.warn("💔 하트비트 전송 실패 - 연결 중단됨: {} (오류: {})", sessionKey, e.getMessage());
            disconnectedSessions.add(sessionKey);
        } catch (IllegalStateException e) {
            log.warn("💔 하트비트 전송 실패 - SSE 상태 이상: {} (오류: {})", sessionKey, e.getMessage());
            disconnectedSessions.add(sessionKey);
        } catch (Exception e) {
            log.error("💔 하트비트 전송 중 예상치 못한 오류: {} (오류: {})", sessionKey, e.getMessage());
            disconnectedSessions.add(sessionKey);
        }
    });

    // 🔧 연결이 끊어진 세션들 정리
    disconnectedSessions.forEach(sessionKey -> {
        sseEmitters.remove(sessionKey);
        log.info("🗑️ 연결 끊어진 SSE 정리: {}", sessionKey);

        // ✅ 중요: 세션은 유지 (재연결 가능하도록)
        VideoCallSession session = activeSessions.get(sessionKey);
        if (session != null) {
            log.info("🔄 세션 {} SSE 연결 끊김 (재연결 가능, 나이: {}분)",
                sessionKey, session.getAgeInMinutes());
        }
    });

    log.debug("💓 하트비트 전송 완료 - 활성 연결: {}, 정리된 연결: {}",
        sseEmitters.size(), disconnectedSessions.size());
}

// 🔧 SSE 연결 상태 확인 헬퍼 메서드
private boolean isEmitterClosed(SseEmitter emitter) {
    try {
        // 빈 이벤트로 연결 상태 테스트
        emitter.send(SseEmitter.event().name("ping").data(""));
        return false;
    } catch (Exception e) {
        return true;
    }
}

    /**
     * 만료된 세션 정리 (10분마다)
     */
    @Scheduled(fixedRate = 600000) // 10분마다
    public void cleanupExpiredSessions() {
        log.info("🧹 만료된 세션 정리 시작 (TTL: 3시간)");

        int removedSessions = 0;
        int removedSSE = 0;

        // 1. 만료된 세션 정리
        var sessionIterator = activeSessions.entrySet().iterator();
        while (sessionIterator.hasNext()) {
            var entry = sessionIterator.next();
            VideoCallSession session = entry.getValue();
            String sessionKey = entry.getKey();

            if (session.isExpired()) {
                sessionIterator.remove();
                removedSessions++;

                // 관련 SSE 연결도 정리
                SseEmitter emitter = sseEmitters.remove(sessionKey);
                if (emitter != null) {
                    try {
                        emitter.complete();
                        removedSSE++;
                    } catch (Exception e) {
                        log.debug("SSE 정리 중 예외 (무시됨): {}", e.getMessage());
                    }
                }

                log.info("🗑️ 만료된 세션 정리: {} (생성: {}, 마지막활동: {}, 비활성: {}분)",
                    sessionKey, session.getCreatedAt(), session.getLastActivity(), session.getInactiveMinutes());
            }
        }

        // 2. 고아 SSE 연결 정리
        var sseIterator = sseEmitters.entrySet().iterator();
        while (sseIterator.hasNext()) {
            var entry = sseIterator.next();
            String sessionKey = entry.getKey();

            if (! activeSessions.containsKey(sessionKey)) {
                try {
                    entry.getValue().complete();
                    sseIterator.remove();
                    removedSSE++;
                    log.info("🗑️ 고아 SSE 연결 정리: {}", sessionKey);
                } catch (Exception e) {
                    log.debug("고아 SSE 정리 중 예외 (무시됨): {}", e.getMessage());
                    sseIterator.remove();
                }
            }
        }

        if (removedSessions > 0 || removedSSE > 0) {
            log.info("✅ 세션 정리 완료 - 세션: {}, SSE: {}, 남은 세션: {}, 활성 SSE: {}",
                removedSessions, removedSSE, activeSessions.size(), sseEmitters.size());
        }
    }

    /**
     * 연결 상태 모니터링 (5분마다)
     */
    @Scheduled(fixedRate = 300000) // 5분마다
    public void monitorConnections() {
        log.debug("🔍 연결 상태 모니터링 시작");

        int totalSessions = activeSessions.size();
        int activeSseConnections = sseEmitters.size();
        int expiredSessions = (int) activeSessions.values().stream()
            .filter(VideoCallSession::isExpired)
            .count();

        if (totalSessions > 0 || activeSseConnections > 0) {
            log.info("📊 연결 상태 - 전체 세션: {}, 만료 세션: {}, 활성 SSE: {}",
                totalSessions, expiredSessions, activeSseConnections);
        }

        // 세션 나이 분석
        activeSessions.values().forEach(session -> {
            if (session.getAgeInMinutes() > 60) { // 1시간 이상
                log.debug("🕐 장기 세션: {} (나이: {}분, 남은시간: {:.1f}시간)",
                    session.getSessionKey(), session.getAgeInMinutes(), session.getRemainingHours());
            }
        });
    }

    /**
     * SSE 연결 상태 확인 API
     */
    @GetMapping("/sse-status")
    public ResponseEntity<?> getSseStatus() {
        Map<String, Object> statusInfo = new HashMap<>();

        sseEmitters.forEach((sessionKey, emitter) -> {
            VideoCallSession session = activeSessions.get(sessionKey);

            // 각 세션의 상태 정보 구성
            Map<String, Object> sessionStatus = new HashMap<>();
            sessionStatus.put("hasSession", session != null);
            sessionStatus.put("sessionStatus", session != null ? session.getStatus() : "NO_SESSION");
            sessionStatus.put("createdAt", session != null ? session.getCreatedAt().toString() : "UNKNOWN");
            sessionStatus.put("lastActivity", session != null ? session.getLastActivity().toString() : "UNKNOWN");
            sessionStatus.put("ageInMinutes", session != null ? session.getAgeInMinutes() : 0);
            sessionStatus.put("remainingHours", session != null ? session.getRemainingHours() : 0);
            sessionStatus.put("isExpired", session != null ? session.isExpired() : true);
            sessionStatus.put("connectionTime", System.currentTimeMillis());

            statusInfo.put(sessionKey, sessionStatus);
        });

        // 응답 데이터 구성
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("totalSseConnections", sseEmitters.size());
        responseData.put("totalSessions", activeSessions.size());
        responseData.put("ttlHours", 3);
        responseData.put("connections", statusInfo);
        responseData.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(Map.of(
            "status", Map.of("code", "OK_0000", "message", "SSE 연결 상태 조회 완료"),
            "response", responseData
        ));
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
                log.info("세션 상태를 CLEANUP으로 변경 - 세션: {} (나이: {}분)",
                    sessionKey, session.getAgeInMinutes());
            }

            return ResponseEntity.ok(Map.of(
                "status", Map.of("code", "OK_0000", "message", "세션 정리 완료"),
                "response", Map.of(
                    "sessionKey", sessionKey,
                    "timestamp", System.currentTimeMillis()
                )
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