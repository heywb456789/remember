// ExternalVideoApiService.java - 단순 전송 버전 (컴파일 오류 해결)

package com.tomato.remember.application.videocall.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * ✅ 단순 전송 방식 - 외부 API로 전송만 하고 콜백 대기
 */
@Slf4j
@Service
public class ExternalVideoApiService {

    @Autowired
    private WebClient webClient;

    @Value("${app.external-api.video.base-url:https://api.example.com}")
    private String externalApiBaseUrl;

    @Value("${app.external-api.video.process-endpoint:/api/v1/video/process}")
    private String processEndpoint;

    @Value("${app.external-api.video.timeout:10}")  // ✅ 짧은 타임아웃
    private int timeoutSeconds;

    @Value("${app.file.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * ✅ 단순 전송 방식 - 200 OK만 확인하고 완료
     */
    public Mono<ResponseEntity<Void>> sendVideoToExternalApiSimple(String sessionKey, String savedFilePath) {
        log.info("🚀 외부 API 전송 시작 - 세션: {}, 파일: {}", sessionKey, savedFilePath);

        String fullVideoUrl = convertToFullUrl(savedFilePath);
        String fullApiUrl = externalApiBaseUrl + processEndpoint;

        Map<String, Object> requestBody = Map.of(
            "sessionKey", sessionKey,
            "videoUrl", fullVideoUrl
        );

        // ✅ 상세 로깅
        log.info("📤 외부 API 요청 정보:");
        log.info("  - API URL: {}", fullApiUrl);
        log.info("  - Video URL: {}", fullVideoUrl);
        log.info("  - Session Key: {}", sessionKey);
        log.info("  - Timeout: {}초", timeoutSeconds);
        log.info("  - Request Body: {}", requestBody);

        return webClient
            .post()
            .uri(fullApiUrl)
            .headers(this::setHeaders)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .toBodilessEntity()
            .timeout(Duration.ofSeconds(timeoutSeconds))  // ✅ 설정값 사용
            .doOnSubscribe(subscription ->
                log.info("🔄 외부 API 요청 시작 - 세션: {}", sessionKey))
            .doOnNext(response ->
                log.info("📥 외부 API 응답 수신 - 세션: {}, 상태: {}", sessionKey, response.getStatusCode()))
            .doOnSuccess(response -> {
                if (response != null) {
                    log.info("✅ 외부 API 전송 성공 - 세션: {}, 상태: {}", sessionKey, response.getStatusCode());
                } else {
                    log.warn("⚠️ 외부 API 응답이 null - 세션: {}", sessionKey);
                }
            })
            .doOnError(error -> {
                if (error instanceof WebClientResponseException) {
                    WebClientResponseException webError = (WebClientResponseException) error;
                    log.error("❌ 외부 API HTTP 오류 - 세션: {}", sessionKey);
                    log.error("  - 상태 코드: {}", webError.getStatusCode());
                    log.error("  - 응답 본문: {}", webError.getResponseBodyAsString());
                    log.error("  - 요청 URL: {}", fullApiUrl);
                } else if (error instanceof java.util.concurrent.TimeoutException) {
                    log.error("⏰ 외부 API 타임아웃 - 세션: {}, 설정: {}초", sessionKey, timeoutSeconds);
                    log.error("  - API URL: {}", fullApiUrl);
                } else {
                    log.error("❌ 외부 API 전송 실패 - 세션: {}, 오류: {}", sessionKey, error.getClass().getSimpleName());
                    log.error("  - 메시지: {}", error.getMessage());
                }
            })
            .onErrorResume(this::handleApiError);
    }

    /**
     * ✅ 비동기 콜백 방식 (단순 전송)
     */
    public void sendVideoToExternalApiAsync(String sessionKey, String savedFilePath,
            java.util.function.Consumer<ResponseEntity<Void>> successCallback,
            java.util.function.Consumer<Throwable> errorCallback) {

        sendVideoToExternalApiSimple(sessionKey, savedFilePath)
            .subscribe(
                response -> {
                    log.info("외부 API 단순 전송 성공 - 세션: {}", sessionKey);
                    if (successCallback != null) {
                        successCallback.accept(response);
                    }
                },
                error -> {
                    log.error("외부 API 단순 전송 실패 - 세션: {}", sessionKey, error);
                    if (errorCallback != null) {
                        errorCallback.accept(error);
                    }
                }
            );
    }

    // ===== Helper Methods =====

    private void setHeaders(HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "TomatoRemember-VideoCall/1.0");
        headers.set("X-Service", "video-call");
        headers.set("X-Timestamp", String.valueOf(System.currentTimeMillis()));
    }

    private String convertToFullUrl(String relativePath) {
    if (relativePath == null || relativePath.trim().isEmpty()) {
        throw new IllegalArgumentException("파일 경로가 비어있습니다.");
    }

    if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
        return relativePath;
    }

    // ✅ 앞의 / 제거
    String cleanPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;

    // ✅ baseUrl 뒤에 / 있는지 확인
    String baseUrlClean = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

    // ✅ 항상 /uploads/ 경로 포함하여 생성
    return String.format("%s/uploads/%s", baseUrlClean, cleanPath);
}

    private Mono<ResponseEntity<Void>> handleApiError(Throwable error) {
        if (error instanceof WebClientResponseException) {
            WebClientResponseException webError = (WebClientResponseException) error;
            log.error("외부 API HTTP 오류 - 상태: {}, 응답: {}",
                webError.getStatusCode(), webError.getResponseBodyAsString());
        }

        return Mono.error(new RuntimeException("외부 API 연결 실패: " + error.getMessage()));
    }

    // ===== Configuration Methods =====

    public boolean validateConfiguration() {
        boolean isValid = true;

        if (externalApiBaseUrl == null || externalApiBaseUrl.trim().isEmpty()) {
            log.error("외부 API 기본 URL이 설정되지 않았습니다.");
            isValid = false;
        }

        if (processEndpoint == null || processEndpoint.trim().isEmpty()) {
            log.error("외부 API 처리 엔드포인트가 설정되지 않았습니다.");
            isValid = false;
        }

        if (timeoutSeconds <= 0) {
            log.error("외부 API 타임아웃 설정이 잘못되었습니다: {}", timeoutSeconds);
            isValid = false;
        }

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            log.error("기본 URL이 설정되지 않았습니다.");
            isValid = false;
        }

        log.info("외부 API 설정 검증 결과: {}", isValid ? "정상" : "오류");
        return isValid;
    }

    // Getter methods
    public String getExternalApiBaseUrl() { return externalApiBaseUrl; }
    public String getProcessEndpoint() { return processEndpoint; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public int getRetryCount() { return 0; }  // ✅ 재시도 없음
}