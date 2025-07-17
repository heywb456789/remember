package com.tomato.remember.application.videocall.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

/**
 * 외부 AI API 호출 서비스 (WebClient 기반)
 * 영상통화 녹화 파일을 외부 시스템으로 전송하고 처리 결과를 받는 서비스
 */
@Slf4j
@Service
public class ExternalVideoApiService {

    @Autowired
    private WebClient webClient;

    // 외부 API 설정값들 (application.yml에서 주입)
    @Value("${app.external-api.video.base-url:https://api.example.com}")
    private String externalApiBaseUrl;

    @Value("${app.external-api.video.process-endpoint:/api/v1/video/process}")
    private String processEndpoint;

    @Value("${app.external-api.video.timeout:30}")
    private int timeoutSeconds;

    @Value("${app.external-api.video.retry-count:3}")
    private int retryCount;

    @Value("${app.file.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * 외부 API로 영상 처리 요청 전송
     * 
     * @param sessionKey 세션 키
     * @param savedFilePath 저장된 파일의 상대 경로
     * @return 비동기 처리 결과
     */
    public Mono<String> sendVideoToExternalApi(String sessionKey, String savedFilePath) {
        log.info("외부 API 호출 시작 - 세션: {}, 파일: {}", sessionKey, savedFilePath);

        // 상대 경로를 절대 URL로 변환
        String fullVideoUrl = convertToFullUrl(savedFilePath);

        // 요청 데이터 구성
        Map<String, Object> requestBody = Map.of(
            "videoId", sessionKey,
            "videoUrl", fullVideoUrl
        );

        log.info("외부 API 요청 데이터: {}", requestBody);

        return webClient
            .post()
            .uri(externalApiBaseUrl + processEndpoint)
            .headers(this::setHeaders)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .retryWhen(Retry.backoff(retryCount, Duration.ofSeconds(2))
                .doBeforeRetry(retrySignal -> 
                    log.warn("외부 API 재시도 {} - 세션: {}", 
                        retrySignal.totalRetries() + 1, sessionKey))
            )
            .doOnSuccess(response -> 
                log.info("외부 API 호출 성공 - 세션: {}, 응답: {}", sessionKey, response))
            .doOnError(error -> 
                log.error("외부 API 호출 실패 - 세션: {}, 오류: {}", sessionKey, error.getMessage(), error))
            .onErrorResume(this::handleApiError);
    }

    /**
     * 동기식 API 호출 (필요한 경우)
     * 
     * @param sessionKey 세션 키
     * @param savedFilePath 저장된 파일 경로
     * @return 처리 결과
     */
    public String sendVideoToExternalApiSync(String sessionKey, String savedFilePath) {
        try {
            return sendVideoToExternalApi(sessionKey, savedFilePath)
                .block(Duration.ofSeconds(timeoutSeconds + 10));
        } catch (Exception e) {
            log.error("동기식 외부 API 호출 실패 - 세션: {}", sessionKey, e);
            return null;
        }
    }

    /**
     * 비동기 콜백 방식으로 외부 API 호출
     * 
     * @param sessionKey 세션 키
     * @param savedFilePath 저장된 파일 경로
     * @param successCallback 성공 콜백
     * @param errorCallback 실패 콜백
     */
    public void sendVideoToExternalApiAsync(String sessionKey, String savedFilePath, 
            java.util.function.Consumer<String> successCallback,
            java.util.function.Consumer<Throwable> errorCallback) {
        
        sendVideoToExternalApi(sessionKey, savedFilePath)
            .subscribe(
                response -> {
                    log.info("비동기 외부 API 성공 - 세션: {}", sessionKey);
                    if (successCallback != null) {
                        successCallback.accept(response);
                    }
                },
                error -> {
                    log.error("비동기 외부 API 실패 - 세션: {}", sessionKey, error);
                    if (errorCallback != null) {
                        errorCallback.accept(error);
                    }
                }
            );
    }

    /**
     * 커스텀 헤더 설정
     */
    private void setHeaders(HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "TomatoRemember-VideoCall/1.0");
        
        // API 키가 설정되어 있으면 Authorization 헤더 추가
//        if (apiKey != null && !apiKey.trim().isEmpty()) {
//            headers.setBearerAuth(apiKey);
//            // 또는 다른 인증 방식
//            // headers.set("X-API-Key", apiKey);
//        }
        
        // 추가 커스텀 헤더
        headers.set("X-Service", "video-call");
        headers.set("X-Timestamp", String.valueOf(System.currentTimeMillis()));
    }

    /**
     * 상대 경로를 절대 URL로 변환
     */
    private String convertToFullUrl(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new IllegalArgumentException("파일 경로가 비어있습니다.");
        }

        // 이미 절대 URL인 경우 그대로 반환
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }

        // 상대 경로를 절대 URL로 변환
        String cleanPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return String.format("%s/uploads/%s", baseUrl, cleanPath);
    }

    /**
     * API 에러 처리
     */
    private Mono<String> handleApiError(Throwable error) {
        if (error instanceof WebClientResponseException) {
            WebClientResponseException webError = (WebClientResponseException) error;
            log.error("외부 API HTTP 오류 - 상태: {}, 응답: {}", 
                webError.getStatusCode(), webError.getResponseBodyAsString());
            
            // 특정 HTTP 상태에 따른 처리
            switch (webError.getStatusCode().value()) {
                case 400:
                    return Mono.error(new RuntimeException("잘못된 요청 데이터입니다."));
                case 401:
                    return Mono.error(new RuntimeException("API 인증에 실패했습니다."));
                case 403:
                    return Mono.error(new RuntimeException("API 접근 권한이 없습니다."));
                case 404:
                    return Mono.error(new RuntimeException("API 엔드포인트를 찾을 수 없습니다."));
                case 429:
                    return Mono.error(new RuntimeException("API 호출 한도를 초과했습니다."));
                case 500:
                    return Mono.error(new RuntimeException("외부 서버 내부 오류입니다."));
                default:
                    return Mono.error(new RuntimeException("외부 API 오류: " + webError.getMessage()));
            }
        }
        
        // 네트워크 오류 등
        return Mono.error(new RuntimeException("외부 API 연결 실패: " + error.getMessage()));
    }

    /**
     * API 연결 상태 확인 (헬스체크)
     */
    public Mono<Boolean> checkApiHealth() {
        return webClient
            .get()
            .uri(externalApiBaseUrl + "/health")
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(5))
            .map(response -> true)
            .onErrorReturn(false)
            .doOnNext(isHealthy -> 
                log.info("외부 API 헬스체크 결과: {}", isHealthy ? "정상" : "비정상"));
    }

    /**
     * 설정값 유효성 검증
     */
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

    // Getter methods for configuration (테스트 및 디버깅용)
    public String getExternalApiBaseUrl() { return externalApiBaseUrl; }
    public String getProcessEndpoint() { return processEndpoint; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public int getRetryCount() { return retryCount; }
}