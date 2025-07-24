package com.tomato.remember.application.wsvideo.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tomato.remember.application.wsvideo.code.VideoCallFlowState;
import com.tomato.remember.application.wsvideo.dto.MemorialVideoSession;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Memorial Video Call 세션 관리자
 * Redis 기반으로 TTL을 이용한 세션 생명주기 관리
 * LinkedHashMap 역직렬화 문제 해결
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemorialVideoSessionManager {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_KEY_PREFIX = "memorial:video:session:";
    private static final String SOCKET_MAPPING_PREFIX = "memorial:video:socket:";

    // ObjectMapper 인스턴스 생성 (일관된 직렬화/역직렬화용)
    private final ObjectMapper objectMapper = createObjectMapper();

    /**
     * ObjectMapper 설정
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 알려지지 않은 필드 무시 (currentStateDisplayName 등)
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);

        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        return mapper;
    }

    /**
     * 새로운 세션 생성
     */
    public MemorialVideoSession createSession(String contactName, Long memorialId, Long callerId) {
        String sessionKey = generateSessionKey();

        MemorialVideoSession session = MemorialVideoSession.createNew(
            sessionKey, contactName, memorialId, callerId
        );

        saveSession(session);

        log.info("🆕 새 Memorial Video 세션 생성: {} (연락처: {}, Memorial ID: {})",
            sessionKey, contactName, memorialId);

        return session;
    }

    /**
     * 세션 저장 (TTL 포함) - JSON 문자열로 저장하여 타입 안전성 보장
     */
    public void saveSession(MemorialVideoSession session) {
        String key = SESSION_KEY_PREFIX + session.getSessionKey();

        try {
            // JSON 문자열로 직렬화하여 저장
            String jsonValue = objectMapper.writeValueAsString(session);

            redisTemplate.opsForValue().set(
                key,
                jsonValue,  // JSON 문자열로 저장
                MemorialVideoSession.getTtlSeconds(),
                TimeUnit.SECONDS
            );

            log.debug("💾 세션 저장 (JSON): {} (TTL: {}초)", session.getSessionKey(), MemorialVideoSession.getTtlSeconds());

            if (shouldVerifyStateChange(session.getFlowState())) {
                verifySessionState(session.getSessionKey(), session.getFlowState());
            }

        } catch (Exception e) {
            log.error("❌ 세션 저장 실패: {} - {}", session.getSessionKey(), e.getMessage());
            // 폴백: 기존 방식으로 저장
            redisTemplate.opsForValue().set(
                key,
                session,
                MemorialVideoSession.getTtlSeconds(),
                TimeUnit.SECONDS
            );
        }
    }

    //상태 변경 검증이 필요한지 확인
    private boolean shouldVerifyStateChange(VideoCallFlowState state) {
        // 중요한 상태들만 즉시 검증
        return state == VideoCallFlowState.RECORDING ||
               state == VideoCallFlowState.PROCESSING ||
               state == VideoCallFlowState.RESPONSE_PLAYING;
    }

    /**
     * 세션 상태 즉시 검증
     */
    private void verifySessionState(String sessionKey, VideoCallFlowState expectedState) {
        try {
            // 50ms 후 검증 (Redis 쓰기 완료 대기)
            Thread.sleep(50);

            MemorialVideoSession verifySession = getSession(sessionKey, true);
            if (verifySession == null) {
                log.error("❌ 상태 검증 실패 - 세션 조회 불가: {}", sessionKey);
                return;
            }

            if (verifySession.getFlowState() != expectedState) {
                log.error("❌ 상태 검증 실패 - 세션: {}, 예상: {}, 실제: {}",
                         sessionKey, expectedState, verifySession.getFlowState());
            } else {
                log.debug("✅ 상태 검증 성공 - 세션: {}, 상태: {}", sessionKey, expectedState);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("⚠️ 상태 검증 중 인터럽트: {}", sessionKey);
        } catch (Exception e) {
            log.warn("⚠️ 상태 검증 중 오류: {} - {}", sessionKey, e.getMessage());
        }
    }

    /**
     * 세션 조회 - 강화된 타입 안전성
     */
    public MemorialVideoSession getSession(String sessionKey) {
        return getSession(sessionKey, false); // 기본값: 캐시 사용
    }


    public MemorialVideoSession getSession(String sessionKey, boolean forceRefresh) {
        String key = SESSION_KEY_PREFIX + sessionKey;

        try {
            Object sessionObj;
            if (forceRefresh) {
                log.debug("🔄 강제 새로고침 모드: {}", sessionKey);
                // Redis에서 직접 조회하여 캐시 무시
                sessionObj = redisTemplate.opsForValue().get(key);
            } else {
                sessionObj = redisTemplate.opsForValue().get(key);
            }

            if (sessionObj == null) {
                log.debug("📂 세션 조회 실패: {} (만료 또는 없음)", sessionKey);
                return null;
            }

            MemorialVideoSession session = convertToSession(sessionKey, sessionObj, key);

            if (session != null) {
                // 🔧 상태 정보 상세 로깅 (디버깅용)
                log.debug("📂 세션 조회 성공: {} (상태: {}, 나이: {}분, 강제새로고침: {}, 마지막상태변경: {}분 전)",
                         sessionKey, session.getFlowState(), session.getAgeInMinutes(), forceRefresh,
                         session.getMinutesSinceStateChange());
            }

            return session;

        } catch (Exception e) {
            log.error("❌ 세션 조회 중 예외 발생: {} - {}", sessionKey, e.getMessage());
            return null;
        }
    }

    private MemorialVideoSession convertToSession(String sessionKey, Object sessionObj, String redisKey) {
    // 1. 이미 올바른 타입인 경우
        if (sessionObj instanceof MemorialVideoSession) {
            return (MemorialVideoSession) sessionObj;
        }

        // 2. JSON 문자열인 경우
        if (sessionObj instanceof String) {
            try {
                return objectMapper.readValue((String) sessionObj, MemorialVideoSession.class);
            } catch (Exception jsonError) {
                log.error("❌ JSON 문자열 변환 실패: {} - {}", sessionKey, jsonError.getMessage());
                redisTemplate.delete(redisKey);
                return null;
            }
        }

        // 3. LinkedHashMap인 경우 (기존 데이터)
        if (sessionObj instanceof LinkedHashMap) {
            log.warn("🔧 세션 데이터 타입 변환 필요: {} (LinkedHashMap → MemorialVideoSession)", sessionKey);
            return convertLinkedHashMapToSession(sessionKey, (LinkedHashMap<?, ?>) sessionObj, redisKey);
        }

        // 4. 알 수 없는 타입
        log.error("❌ 알 수 없는 세션 데이터 타입: {} - {}", sessionKey, sessionObj.getClass().getName());
        redisTemplate.delete(redisKey);
        return null;
    }

    public MemorialVideoSession getSessionWithStateValidation(String sessionKey, VideoCallFlowState expectedState) {
        // 첫 번째 시도: 일반 조회
        MemorialVideoSession session = getSession(sessionKey, false);

        if (session != null && session.getFlowState() == expectedState) {
            log.debug("✅ 예상 상태 일치: {} - {}", sessionKey, expectedState);
            return session;
        }

        // 두 번째 시도: 강제 새로고침
        log.warn("⚠️ 상태 불일치, 강제 새로고침 시도: {} - 예상: {}, 실제: {}",
                 sessionKey, expectedState, session != null ? session.getFlowState() : "NULL");

        // 100ms 대기 후 재시도 (Redis 동기화 시간 고려)
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        session = getSession(sessionKey, true);

        if (session != null) {
            log.info("🔄 강제 새로고침 결과: {} - 상태: {} (예상: {})",
                    sessionKey, session.getFlowState(), expectedState);

            if (session.getFlowState() == expectedState) {
                log.info("✅ 재시도 후 상태 일치: {}", sessionKey);
            } else {
                log.warn("⚠️ 재시도 후에도 상태 불일치: {} - 예상: {}, 실제: {}",
                        sessionKey, expectedState, session.getFlowState());
            }
        } else {
            log.error("❌ 강제 새로고침 후에도 세션 조회 실패: {}", sessionKey);
        }

        return session;
    }

    /**
     * LinkedHashMap을 MemorialVideoSession으로 안전하게 변환
     */
    private MemorialVideoSession convertLinkedHashMapToSession(String sessionKey, LinkedHashMap<?, ?> mapData, String redisKey) {
        try {
            // LinkedHashMap에서 불필요한 필드 제거
            LinkedHashMap<String, Object> cleanedMap = new LinkedHashMap<>();

            for (Object key : mapData.keySet()) {
                String keyStr = key.toString();
                Object value = mapData.get(key);

                // 동적 계산 필드들은 제외 (문제의 원인)
                if (!keyStr.equals("currentStateDisplayName") &&
                    !keyStr.equals("currentStateDescription") &&
                    !keyStr.equals("ageInMinutes") &&
                    !keyStr.equals("minutesSinceStateChange") &&
                    !keyStr.equals("expired") &&
                    !keyStr.equals("remainingTtlSeconds")) {
                    cleanedMap.put(keyStr, value);
                }
            }

            // ObjectMapper를 사용한 변환
            String jsonString = objectMapper.writeValueAsString(cleanedMap);
            MemorialVideoSession session = objectMapper.readValue(jsonString, MemorialVideoSession.class);

            // 변환된 세션을 JSON 문자열로 다시 저장 (향후 안전성 확보)
            saveSession(session);

            log.info("✅ 세션 데이터 타입 변환 완료: {} → JSON 저장", sessionKey);
            return session;

        } catch (Exception conversionError) {
            log.error("❌ LinkedHashMap 세션 변환 실패: {} - {}", sessionKey, conversionError.getMessage());

            // 변환 실패 시 기본 세션 생성 시도
            try {
                return createFallbackSession(sessionKey, mapData);
            } catch (Exception fallbackError) {
                log.error("❌ 폴백 세션 생성도 실패: {} - {}", sessionKey, fallbackError.getMessage());
                redisTemplate.delete(redisKey);
                return null;
            }
        }
    }

    /**
     * 변환 실패 시 폴백 세션 생성
     */
    private MemorialVideoSession createFallbackSession(String sessionKey, LinkedHashMap<?, ?> mapData) {
        log.warn("🔄 폴백 세션 생성: {}", sessionKey);

        // 기본값으로 세션 생성
        String contactName = getStringValue(mapData, "contactName", "알 수 없음");
        Long memorialId = getLongValue(mapData, "memorialId", 0L);
        Long callerId = getLongValue(mapData, "callerId", 0L);

        MemorialVideoSession fallbackSession = MemorialVideoSession.createNew(
            sessionKey, contactName, memorialId, callerId
        );

        // 가능한 필드들 복원 시도
        try {
            if (mapData.containsKey("socketId")) {
                fallbackSession.setSocketId(getStringValue(mapData, "socketId", null));
            }
            if (mapData.containsKey("savedFilePath")) {
                fallbackSession.setSavedFilePath(getStringValue(mapData, "savedFilePath", null));
            }
            if (mapData.containsKey("responseVideoUrl")) {
                fallbackSession.setResponseVideoUrl(getStringValue(mapData, "responseVideoUrl", null));
            }
            if (mapData.containsKey("waitingVideoUrl")) {
                fallbackSession.setWaitingVideoUrl(getStringValue(mapData, "waitingVideoUrl", null));
            }
        } catch (Exception e) {
            log.warn("⚠️ 폴백 세션 필드 복원 중 일부 오류 (계속 진행): {}", e.getMessage());
        }

        return fallbackSession;
    }

    /**
     * Map에서 String 값 안전하게 추출
     */
    private String getStringValue(LinkedHashMap<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Map에서 Long 값 안전하게 추출
     */
    private Long getLongValue(LinkedHashMap<?, ?> map, String key, Long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.valueOf((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 세션 TTL 갱신
     */
    public boolean extendSessionTtl(String sessionKey) {
        String key = SESSION_KEY_PREFIX + sessionKey;

        MemorialVideoSession session = getSession(sessionKey);
        if (session != null) {
            session.updateActivity();

            // Redis TTL 갱신
            redisTemplate.expire(key, MemorialVideoSession.getTtlSeconds(), TimeUnit.SECONDS);

            // 세션 데이터도 업데이트 (JSON 문자열로)
            saveSession(session);

            log.debug("⏰ TTL 갱신: {} (남은 시간: {}초)", sessionKey, session.getRemainingTtlSeconds());
            return true;
        }

        return false;
    }

    /**
     * 웹소켓 연결 매핑 저장
     */
    public void mapSocketToSession(String socketId, String sessionKey) {
        String key = SOCKET_MAPPING_PREFIX + socketId;

        redisTemplate.opsForValue().set(
            key,
            sessionKey,
            MemorialVideoSession.getTtlSeconds(),
            TimeUnit.SECONDS
        );

        // 세션에도 소켓 ID 업데이트
        MemorialVideoSession session = getSession(sessionKey);
        if (session != null) {
            session.setWebSocketConnection(socketId);
            saveSession(session);
        }

        log.debug("🔗 소켓 매핑: {} → {}", socketId, sessionKey);
    }

    /**
     * 소켓 ID로 세션 키 조회
     */
    public String getSessionKeyBySocketId(String socketId) {
        String key = SOCKET_MAPPING_PREFIX + socketId;
        return (String) redisTemplate.opsForValue().get(key);
    }

    /**
     * 웹소켓 연결 해제
     */
    public void unmapSocket(String socketId) {
        String sessionKey = getSessionKeyBySocketId(socketId);

        if (sessionKey != null) {
            // 소켓 매핑 제거
            redisTemplate.delete(SOCKET_MAPPING_PREFIX + socketId);

            // 세션에서 소켓 정보 제거
            MemorialVideoSession session = getSession(sessionKey);
            if (session != null) {
                session.clearWebSocketConnection();
                saveSession(session);

                log.debug("🔌 소켓 연결 해제: {} (세션: {})", socketId, sessionKey);
            }
        }
    }

    /**
     * 세션 명시적 삭제 (정상 종료 시)
     */
    public void deleteSession(String sessionKey) {
        MemorialVideoSession session = getSession(sessionKey);

        if (session != null) {
            // 소켓 매핑도 함께 삭제
            if (session.getSocketId() != null) {
                redisTemplate.delete(SOCKET_MAPPING_PREFIX + session.getSocketId());
            }

            // 세션 삭제
            redisTemplate.delete(SESSION_KEY_PREFIX + sessionKey);

            log.info("🗑️ 세션 명시적 삭제: {} (나이: {}분)", sessionKey, session.getAgeInMinutes());
        }
    }

    /**
     * 활성 세션 목록 조회
     */
    public Set<MemorialVideoSession> getActiveSessions() {
        Set<String> keys = redisTemplate.keys(SESSION_KEY_PREFIX + "*");

        if (keys == null || keys.isEmpty()) {
            return Set.of();
        }

        return keys.stream()
            .map(key -> {
                String sessionKey = key.substring(SESSION_KEY_PREFIX.length());
                return getSession(sessionKey);
            })
            .filter(session -> session != null && !session.isExpired())
            .collect(Collectors.toSet());
    }

    /**
     * 세션 통계 조회
     */
    public SessionStatistics getSessionStatistics() {
        Set<MemorialVideoSession> activeSessions = getActiveSessions();

        long totalSessions = activeSessions.size();
        long connectedSessions = activeSessions.stream()
            .filter(MemorialVideoSession::isConnected)
            .count();
        long waitingSessions = activeSessions.stream()
            .filter(session -> "WAITING".equals(session.getStatus()))
            .count();
        long processingSessions = activeSessions.stream()
            .filter(session -> "PROCESSING".equals(session.getStatus()))
            .count();

        return SessionStatistics.builder()
            .totalSessions(totalSessions)
            .connectedSessions(connectedSessions)
            .waitingSessions(waitingSessions)
            .processingSessions(processingSessions)
            .avgSessionAgeMinutes(activeSessions.stream()
                .mapToLong(MemorialVideoSession::getAgeInMinutes)
                .average()
                .orElse(0.0))
            .build();
    }

    /**
     * 만료된 세션 수동 정리 (스케줄링용)
     */
    public int cleanupExpiredSessions() {
        Set<String> allKeys = redisTemplate.keys(SESSION_KEY_PREFIX + "*");
        int cleanedCount = 0;

        if (allKeys != null) {
            for (String key : allKeys) {
                String sessionKey = key.substring(SESSION_KEY_PREFIX.length());
                MemorialVideoSession session = getSession(sessionKey);

                if (session != null && session.isExpired()) {
                    redisTemplate.delete(key);

                    // 관련 소켓 매핑도 정리
                    if (session.getSocketId() != null) {
                        redisTemplate.delete(SOCKET_MAPPING_PREFIX + session.getSocketId());
                    }

                    cleanedCount++;
                    log.info("🧹 만료된 세션 정리: {} (나이: {}분)",
                        sessionKey, session.getAgeInMinutes());
                }
            }
        }

        if (cleanedCount > 0) {
            log.info("✅ 만료된 세션 정리 완료: {}개", cleanedCount);
        }

        return cleanedCount;
    }

    /**
     * 유니크한 세션 키 생성
     */
    private String generateSessionKey() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "MVC_" + timestamp + "_" + uuid;
    }

    /**
     * 세션 통계 내부 클래스
     */
    @lombok.Data
    @lombok.Builder
    public static class SessionStatistics {
        private long totalSessions;
        private long connectedSessions;
        private long waitingSessions;
        private long processingSessions;
        private double avgSessionAgeMinutes;
    }
}