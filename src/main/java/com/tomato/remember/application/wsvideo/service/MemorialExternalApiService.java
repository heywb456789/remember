package com.tomato.remember.application.wsvideo.service;

import com.tomato.remember.application.wsvideo.dto.MemorialVideoResponse;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * 메모리얼 관련 외부 API 호출 서비스 /call/memorial 외부 API 연동
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemorialExternalApiService {

    private final WebClient webClient;

    @Value("${app.external-api.memorial.base-url:https://remember.newstomato.com}")
    private String memorialApiBaseUrl;

    @Value("${app.external-api.memorial.endpoint:/call/memorial}")
    private String memorialEndpoint;

    @Value("${app.external-api.memorial.timeout:10}")
    private int timeoutSeconds;

    @Value("${app.external-api.memorial.retry-count:2}")
    private int retryCount;

    /**
     * 메모리얼 영상통화 정보 조회 POST /call/memorial
     */
    public MemorialVideoResponse getMemorialVideoInfo(Long memberId, Long memorialId) {
        log.info("🌐 메모리얼 외부 API 호출 시작 - 회원ID: {}, 메모리얼ID: {}", memberId, memorialId);

        String fullApiUrl = memorialApiBaseUrl + memorialEndpoint;

        Map<String, Object> requestBody = Map.of(
            "memberId", memberId,
            "memorialId", memorialId,
            "requestType", "VIDEO_CALL_INFO",
            "timestamp", System.currentTimeMillis()
        );

        log.info("📤 외부 API 요청 정보:");
        log.info("  - API URL: {}", fullApiUrl);
        log.info("  - 회원 ID: {}", memberId);
        log.info("  - 메모리얼 ID: {}", memorialId);
        log.info("  - Timeout: {}초", timeoutSeconds);
        log.info("  - Retry: {}회", retryCount);

        try {
            // 동기 방식으로 호출 (페이지 로딩이므로)
            ResponseEntity<Map> response = webClient
                .post()
                .uri(fullApiUrl)
                .headers(this::setHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .toEntity(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retry(retryCount)
                .doOnSubscribe(subscription ->
                    log.info("🔄 메모리얼 API 요청 시작 - 회원ID: {}", memberId))
                .doOnNext(resp ->
                    log.info("📥 메모리얼 API 응답 수신 - 상태: {}", resp.getStatusCode()))
                .block(); // 동기 처리

            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ 메모리얼 외부 API 호출 성공");
                return parseResponse(response.getBody());
            } else {
                log.warn("⚠️ 메모리얼 외부 API 응답이 null이거나 실패");
                return createFallbackResponse();
            }

        } catch (WebClientResponseException webError) {
            log.error("❌ 메모리얼 외부 API HTTP 오류");
            log.error("  - 상태 코드: {}", webError.getStatusCode());
            log.error("  - 응답 본문: {}", webError.getResponseBodyAsString());
            return createFallbackResponse();

        } catch (Exception e) {
            log.error("❌ 메모리얼 외부 API 호출 실패", e);
            return createFallbackResponse();
        }
    }

    /**
     * 영상통화 가능 여부 체크 POST /call/memorial (체크 모드)
     */
    public boolean checkVideoCallAvailable(Long memberId, Long memorialId) {
        log.info("영상통화 가능 여부 체크 - 회원ID: {}, 메모리얼ID: {}", memberId, memorialId);

        String fullApiUrl = memorialApiBaseUrl + memorialEndpoint;

        Map<String, Object> requestBody = Map.of(
            "memberId", memberId,
            "memorialId", memorialId,
            "requestType", "CHECK_AVAILABILITY",
            "timestamp", System.currentTimeMillis()
        );

        try {
            ResponseEntity<Map> response = webClient
                .post()
                .uri(fullApiUrl)
                .headers(this::setHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .toEntity(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retry(1) // 체크는 빠르게
                .block();

            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null) {
                    Boolean available = (Boolean) responseBody.get("available");
                    log.info("✅ 영상통화 가능 여부: {}", available);
                    return Boolean.TRUE.equals(available);
                }
            }

            log.warn("⚠️ 영상통화 가능 여부 확인 실패 - 기본값: false");
            return false;

        } catch (Exception e) {
            log.error("❌ 영상통화 가능 여부 체크 실패", e);
            return false; // 실패 시 불가능으로 처리
        }
    }

    /**
     * 비동기 방식 메모리얼 정보 조회 (필요시)
     */
    public Mono<MemorialVideoResponse> getMemorialVideoInfoAsync(Long memberId, Long memorialId) {
        log.info("🚀 비동기 메모리얼 외부 API 호출 - 회원ID: {}, 메모리얼ID: {}", memberId, memorialId);

        String fullApiUrl = memorialApiBaseUrl + memorialEndpoint;

        Map<String, Object> requestBody = Map.of(
            "memberId", memberId,
            "memorialId", memorialId,
            "requestType", "VIDEO_CALL_INFO",
            "timestamp", System.currentTimeMillis()
        );

        return webClient
            .post()
            .uri(fullApiUrl)
            .headers(this::setHeaders)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .toEntity(Map.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .retry(retryCount)
            .map(response -> {
                if (response.getStatusCode().is2xxSuccessful()) {
                    return parseResponse(response.getBody());
                } else {
                    return createFallbackResponse();
                }
            })
            .onErrorReturn(createFallbackResponse())
            .doOnSuccess(result ->
                log.info("✅ 비동기 메모리얼 API 완료 - 연락처: {}", result.getContactName()))
            .doOnError(error ->
                log.error("❌ 비동기 메모리얼 API 실패", error));
    }

    // ===== Private Helper Methods =====

    /**
     * 외부 API 응답 파싱
     */
    private MemorialVideoResponse parseResponse(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return createFallbackResponse();
        }

        try {
            // 외부 API 응답 구조에 맞게 파싱
            String contactName = (String) responseBody.getOrDefault("contactName", "김근태");
            String waitingVideoUrl = (String) responseBody.getOrDefault("waitingVideoUrl",
                "https://remember.newstomato.com/static/waiting_no.mp4");
            Boolean available = (Boolean) responseBody.getOrDefault("available", true);
            String characterType = (String) responseBody.getOrDefault("characterType", "default");

            log.info("📋 외부 API 응답 파싱 결과:");
            log.info("  - 연락처명: {}", contactName);
            log.info("  - 대기영상: {}", waitingVideoUrl);
            log.info("  - 가능여부: {}", available);
            log.info("  - 캐릭터: {}", characterType);

            return MemorialVideoResponse.builder()
                .contactName(contactName)
                .waitingVideoUrl(waitingVideoUrl)
                .available(available)
                .characterType(characterType)
                .success(true)
                .message("외부 API 응답 성공")
                .build();

        } catch (Exception e) {
            log.error("❌ 외부 API 응답 파싱 실패", e);
            return createFallbackResponse();
        }
    }

    /**
     * 폴백 응답 생성 (외부 API 실패시)
     */
    private MemorialVideoResponse createFallbackResponse() {
        log.info("🔄 폴백 응답 생성 - 기본값 사용");

        return MemorialVideoResponse.builder()
            .contactName("김근태") // 기본 연락처
            .waitingVideoUrl("https://remember.newstomato.com/static/waiting_no.mp4") // 기본 대기영상
            .available(true) // 기본적으로 사용 가능
            .characterType("default")
            .success(false)
            .message("외부 API 연결 실패 - 기본값 사용")
            .build();
    }

    /**
     * HTTP 헤더 설정
     */
    private void setHeaders(HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "TomatoRemember-Memorial/1.0");
        headers.set("X-Service", "memorial-video-call");
        headers.set("X-Timestamp", String.valueOf(System.currentTimeMillis()));
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
    }

    // ===== Configuration Validation =====

    /**
     * 설정 검증
     */
    public boolean validateConfiguration() {
        boolean isValid = true;

        if (memorialApiBaseUrl == null || memorialApiBaseUrl.trim().isEmpty()) {
            log.error("메모리얼 API 기본 URL이 설정되지 않았습니다.");
            isValid = false;
        }

        if (memorialEndpoint == null || memorialEndpoint.trim().isEmpty()) {
            log.error("메모리얼 API 엔드포인트가 설정되지 않았습니다.");
            isValid = false;
        }

        if (timeoutSeconds <= 0) {
            log.error("메모리얼 API 타임아웃 설정이 잘못되었습니다: {}", timeoutSeconds);
            isValid = false;
        }

        log.info("메모리얼 API 설정 검증 결과: {}", isValid ? "정상" : "오류");
        log.info("  - Base URL: {}", memorialApiBaseUrl);
        log.info("  - Endpoint: {}", memorialEndpoint);
        log.info("  - Timeout: {}초", timeoutSeconds);
        log.info("  - Retry: {}회", retryCount);

        return isValid;
    }

    // ===== Response DTO =====

    /**
     * 메모리얼 영상통화 응답 DTO
     */
    public String getMemorialApiBaseUrl() {
        return memorialApiBaseUrl;
    }

    public String getMemorialEndpoint() {
        return memorialEndpoint;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getRetryCount() {
        return retryCount;
    }
}