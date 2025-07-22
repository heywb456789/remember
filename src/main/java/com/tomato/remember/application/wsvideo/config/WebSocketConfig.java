package com.tomato.remember.application.wsvideo.config;

import com.tomato.remember.common.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Memorial Video Call WebSocket 설정 - 인증 인터셉터 포함
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

        log.info("🔧 WebSocket 핸들러 등록 시작 (인증 인터셉터 포함)");

        // === 기존 엔드포인트 (하위 호환성) - 인증 적용 ===
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/{sessionKey}")
                .setAllowedOrigins("*") // 개발용, 프로덕션에서는 특정 도메인만 허용
                .addInterceptors(webSocketAuthInterceptor) // 🔒 인증 인터셉터 추가
                .withSockJS(); // SockJS 폴백 지원

        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/native/{sessionKey}")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthInterceptor); // 🔒 인증 인터셉터 추가

        // === 새로운 디바이스별 엔드포인트 - 모두 인증 적용 ===

        // 웹 브라우저용 (PC/태블릿)
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/web/{sessionKey}")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthInterceptor) // 🔒 인증 인터셉터 추가
                .withSockJS();

        // 모바일 웹 브라우저용
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/mobile-web/{sessionKey}")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthInterceptor) // 🔒 인증 인터셉터 추가
                .withSockJS();

        // iOS 앱용 (네이티브)
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/ios/{sessionKey}")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthInterceptor); // 🔒 인증 인터셉터 추가

        // Android 앱용 (네이티브)
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/android/{sessionKey}")
                .setAllowedOrigins("*")
                .addInterceptors(webSocketAuthInterceptor); // 🔒 인증 인터셉터 추가

        // === 개발/테스트용 엔드포인트 (인증 없음) ===
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/test/{sessionKey}")
                .setAllowedOrigins("*")
                .withSockJS();

        log.info("✅ WebSocket 엔드포인트 등록 완료 (인증 보안 적용):");
        log.info("  🔒 /ws/memorial-video/{sessionKey} (인증 필수)");
        log.info("  🔒 /ws/memorial-video/web/{sessionKey} (인증 필수)");
        log.info("  🔒 /ws/memorial-video/mobile-web/{sessionKey} (인증 필수)");
        log.info("  🔒 /ws/memorial-video/ios/{sessionKey} (인증 필수)");
        log.info("  🔒 /ws/memorial-video/android/{sessionKey} (인증 필수)");
        log.info("  🔓 /ws/memorial-video/test/{sessionKey} (테스트용 - 인증 없음)");
    }
}