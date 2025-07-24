package com.tomato.remember.application.wsvideo.config;

import com.tomato.remember.common.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 네이티브 WebSocket만 사용
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final MemorialVideoWebSocketHandler memorialVideoWebSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        // 웹 브라우저용
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/web/{sessionKey}")
                .setAllowedOriginPatterns("*")
                .addInterceptors(webSocketAuthInterceptor);

        // 모바일 웹용
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/mobile-web/{sessionKey}")
                .setAllowedOriginPatterns("*")
                .addInterceptors(webSocketAuthInterceptor);

        // iOS 앱용
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/ios/{sessionKey}")
                .setAllowedOriginPatterns("*")
                .addInterceptors(webSocketAuthInterceptor);

        // Android 앱용
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/android/{sessionKey}")
                .setAllowedOriginPatterns("*")
                .addInterceptors(webSocketAuthInterceptor);

        // 하위 호환성
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/{sessionKey}")
                .setAllowedOriginPatterns("*")
                .addInterceptors(webSocketAuthInterceptor);

        // 테스트용 (인증 없음)
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/test/{sessionKey}")
                .setAllowedOriginPatterns("*");

        log.info("✅ 네이티브 WebSocket 엔드포인트 등록 완료:");
        log.info("  🔒 /ws/memorial-video/web/{{sessionKey}}");
        log.info("  🔒 /ws/memorial-video/mobile-web/{{sessionKey}}");
        log.info("  🔒 /ws/memorial-video/ios/{{sessionKey}}");
        log.info("  🔒 /ws/memorial-video/android/{{sessionKey}}");
        log.info("  🔒 /ws/memorial-video/{{sessionKey}} (호환성)");
        log.info("  🔓 /ws/memorial-video/test/{{sessionKey}} (테스트)");
        log.info("네이티브 WebSocket만 지원");
    }
}