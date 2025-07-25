package com.tomato.remember.application.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 토마토 그룹 프록시 API
 * CORS 문제 해결을 위해 서버에서 토마토 API를 대신 호출
 */
@Slf4j
@RestController
@RequestMapping("/api/tomato")
@CrossOrigin(origins = "*") // 개발용
@RequiredArgsConstructor
public class TomatoGroupApiController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 캐시 저장소
    private final Map<String, CacheItem> cache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MINUTES = 30;

    // 토마토 원본 API URL
    private static final String TOMATO_API_URL = "https://tomato.etomato.com/tomatogroup_20250423.json";


    /**
     * 토마토 그룹 데이터 조회 (BOM 처리 강화)
     */
    @GetMapping("/group")
    public ResponseEntity<?> getTomatoGroup() {
        log.info("🍅 토마토 그룹 데이터 요청 (강화 버전)");

        try {
            // 1. 캐시 확인
            CacheItem cachedItem = cache.get("tomato_group");
            if (cachedItem != null && !cachedItem.isExpired()) {
                log.info("💾 캐시된 토마토 데이터 반환");
                return ResponseEntity.ok(createSuccessResponse(cachedItem.getData()));
            }

            // 2. 토마토 원본 API 호출
            log.info("🌐 토마토 원본 API 호출: {}", TOMATO_API_URL);
            String rawResponse = restTemplate.getForObject(TOMATO_API_URL, String.class);

            if (rawResponse == null || rawResponse.isEmpty()) {
                throw new RuntimeException("토마토 API에서 빈 응답 수신");
            }

            // 3. 문자 정리 (BOM 및 특수문자 완전 제거)
            String cleanResponse = cleanJsonResponse(rawResponse);
            log.info("🔧 응답 정리 완료: {} → {} bytes", rawResponse.length(), cleanResponse.length());

            // 4. JSON 유효성 검증
            JsonNode jsonNode = validateAndParseJson(cleanResponse);

            // 5. 토마토 그룹 데이터 검증
            JsonNode tomatogroupNode = jsonNode.get("tomatogroup");
            if (tomatogroupNode == null || !tomatogroupNode.isArray() || tomatogroupNode.size() == 0) {
                throw new RuntimeException("토마토 그룹 데이터가 비어있음");
            }

            log.info("✅ 토마토 데이터 검증 완료: {}개 앱", tomatogroupNode.size());

            // 6. 캐시 저장
            cache.put("tomato_group", new CacheItem(cleanResponse));

            // 7. 성공 응답
            return ResponseEntity.ok(createSuccessResponse(cleanResponse));

        } catch (HttpClientErrorException e) {
            log.error("❌ 토마토 API HTTP 오류: {} - {}", e.getStatusCode(), e.getMessage());
            return createErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "TOMATO_API_HTTP_ERROR", "토마토 서비스 일시 불가: " + e.getStatusCode());

        } catch (ResourceAccessException e) {
            log.error("❌ 토마토 API 연결 오류: {}", e.getMessage());
            return createErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "TOMATO_API_CONNECTION_ERROR", "토마토 서비스 연결 실패");

        } catch (Exception e) {
            log.error("❌ 토마토 데이터 처리 실패", e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "TOMATO_DATA_PROCESSING_ERROR", "토마토 데이터 처리 중 오류: " + e.getMessage());
        }
    }

    /**
     * JSON 응답 정리 (BOM 및 특수문자 제거)
     */
    private String cleanJsonResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) {
            return rawResponse;
        }

        String cleaned = rawResponse;

        // 1. UTF-8 BOM 제거 (0xFEFF)
        if (cleaned.startsWith("\uFEFF")) {
            cleaned = cleaned.substring(1);
            log.debug("🔧 UTF-8 BOM 제거");
        }

        // 2. UTF-16 BOM 제거 (0xFFFE)
        if (cleaned.startsWith("\uFFFE")) {
            cleaned = cleaned.substring(1);
            log.debug("🔧 UTF-16 BOM 제거");
        }

        // 3. 기타 제어 문자 제거
        cleaned = cleaned.replaceAll("[\u0000-\u001F\u007F-\u009F]", "");

        // 4. 앞뒤 공백 제거
        cleaned = cleaned.trim();

        // 5. 기본 JSON 구조 확인
        if (!cleaned.startsWith("{") && !cleaned.startsWith("[")) {
            throw new RuntimeException("JSON이 올바른 구조로 시작하지 않음");
        }

        return cleaned;
    }

    /**
     * JSON 파싱 및 유효성 검증
     */
    private JsonNode validateAndParseJson(String jsonString) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            if (jsonNode == null) {
                throw new RuntimeException("JSON 파싱 결과가 null");
            }

            if (!jsonNode.has("tomatogroup")) {
                throw new RuntimeException("tomatogroup 필드가 없음");
            }

            return jsonNode;

        } catch (Exception e) {
            log.error("❌ JSON 파싱 오류: {}", e.getMessage());
            log.error("📄 문제가 된 JSON (처음 500자): {}",
                jsonString.substring(0, Math.min(jsonString.length(), 500)));

            // 문자 코드 분석
            if (jsonString.length() > 0) {
                char firstChar = jsonString.charAt(0);
                log.error("첫 번째 문자 분석: '{}' (Unicode: U+{:04X})",
                    firstChar, (int) firstChar);
            }

            throw new RuntimeException("JSON 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * 캐시 초기화
     */
    @DeleteMapping("/group/cache")
    public ResponseEntity<?> clearCache() {
        log.info("🗑️ 토마토 캐시 초기화");
        cache.clear();

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "토마토 캐시가 초기화되었습니다",
            "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * API 상태 확인
     */
    @GetMapping("/group/status")
    public ResponseEntity<?> getStatus() {
        log.info("📊 토마토 API 상태 확인");

        Map<String, Object> status = new HashMap<>();

        // 캐시 상태
        CacheItem cachedItem = cache.get("tomato_group");
        status.put("cache", Map.of(
            "exists", cachedItem != null,
            "expired", cachedItem != null && cachedItem.isExpired(),
            "createdAt", cachedItem != null ? cachedItem.getCreatedAt() : null
        ));

        // API 연결 테스트
        try {
            String testResponse = restTemplate.getForObject(TOMATO_API_URL, String.class);
            status.put("api", Map.of(
                "connectable", true,
                "responseLength", testResponse != null ? testResponse.length() : 0,
                "hasBOM", testResponse != null && testResponse.startsWith("\uFEFF")
            ));
        } catch (Exception e) {
            status.put("api", Map.of(
                "connectable", false,
                "error", e.getMessage()
            ));
        }

        status.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", status
        ));
    }

    /**
     * 성공 응답 생성
     */
    private Map<String, Object> createSuccessResponse(String data) {
        return Map.of(
            "success", true,
            "data", data,
            "timestamp", LocalDateTime.now(),
            "source", "enhanced_tomato_api"
        );
    }

    /**
     * 오류 응답 생성
     */
    private ResponseEntity<?> createErrorResponse(HttpStatus status, String errorCode, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "success", false,
            "error", Map.of(
                "code", errorCode,
                "message", message
            ),
            "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * 캐시 아이템
     */
    private static class CacheItem {
        private final String data;
        private final LocalDateTime createdAt;

        public CacheItem(String data) {
            this.data = data;
            this.createdAt = LocalDateTime.now();
        }

        public String getData() { return data; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        
        public boolean isExpired() {
            return createdAt.isBefore(LocalDateTime.now().minusMinutes(CACHE_DURATION_MINUTES));
        }
    }
}