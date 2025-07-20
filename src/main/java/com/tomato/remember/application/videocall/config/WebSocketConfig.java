package com.tomato.remember.application.videocall.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Memorial Video Call WebSocket 설정
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private MemorialVideoWebSocketHandler memorialVideoWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Memorial Video Call WebSocket 엔드포인트 등록
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/{sessionKey}")
                .setAllowedOrigins("*") // 개발용, 프로덕션에서는 특정 도메인만 허용
                .withSockJS(); // SockJS 폴백 지원 (IE 등 구형 브라우저 대응)
        
        // SockJS 없는 네이티브 WebSocket도 지원
        registry.addHandler(memorialVideoWebSocketHandler, "/ws/memorial-video/native/{sessionKey}")
                .setAllowedOrigins("*");
    }
}