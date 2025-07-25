package com.tomato.remember.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 간소화된 WebSocket 핸드셰이크 인터셉터
 * 초기 메시지 기반 인증을 위해 기본 연결만 허용하고 세션키만 추출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    // 세션 키 추출용 패턴
    private static final Pattern SESSION_KEY_PATTERN = Pattern.compile(
        "/ws/memorial-video/(?:web/|mobile-web/|ios/|android/|native/|test/)?([^/?]+)"
    );

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                 ServerHttpResponse response,
                                 WebSocketHandler wsHandler,
                                 Map<String, Object> attributes) throws Exception {

        URI uri = request.getURI();
        String path = uri.getPath();

        log.info("WebSocket 연결 요청 - Path: {}", path);

        try {
            // 1. URL에서 세션 키 추출
            String sessionKey = extractSessionKey(path);
            if (sessionKey == null) {
                log.warn("🔒 WebSocket 연결 거부: 세션 키 추출 불가 - Path: {}", path);
                return false;
            }

            // 2. 디바이스 타입 추출 (경로에서)
            String deviceType = extractDeviceType(path);

            // 3. 기본 연결 정보만 저장 (인증은 초기 메시지에서 처리)
            attributes.put("sessionKey", sessionKey);
            attributes.put("deviceType", deviceType);
            attributes.put("authenticated", false); // 초기 상태는 미인증
            attributes.put("authTimeout", System.currentTimeMillis() + 5000); // 5초 타임아웃

            log.info("✅ WebSocket 연결 허용 - SessionKey: {}, DeviceType: {} (초기 메시지 인증 대기)",
                    sessionKey, deviceType);

            return true;

        } catch (Exception e) {
            log.error("🔒 WebSocket 연결 중 예외 발생 - Path: {}", path, e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                             ServerHttpResponse response,
                             WebSocketHandler wsHandler,
                             Exception exception) {

        if (exception != null) {
            log.error("🔒 WebSocket Handshake 오류 발생", exception);
        } else {
            log.debug("✅ WebSocket Handshake 완료 - Path: {}", request.getURI().getPath());
        }
    }

    /**
     * URL 경로에서 세션 키 추출
     */
    private String extractSessionKey(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        Matcher matcher = SESSION_KEY_PATTERN.matcher(path);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * URL 경로에서 디바이스 타입 추출
     */
    private String extractDeviceType(String path) {
        if (path == null) return "UNKNOWN";

        if (path.contains("/web/")) return "WEB";
        if (path.contains("/mobile-web/")) return "MOBILE_WEB";
        if (path.contains("/ios/")) return "IOS_APP";
        if (path.contains("/android/")) return "ANDROID_APP";
        if (path.contains("/native/")) return "NATIVE";
        if (path.contains("/test/")) return "TEST";

        return "WEB"; // 기본값
    }
}