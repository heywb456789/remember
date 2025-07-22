package com.tomato.remember.common.security;

import com.tomato.remember.application.wsvideo.config.MemorialVideoSessionManager;
import com.tomato.remember.common.security.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
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
 * WebSocket 연결 시점 인증 인터셉터 - 기존 JwtTokenProvider 사용
 * Handshake 과정에서 JWT 토큰을 검증하여 인증되지 않은 연결을 차단
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemorialVideoSessionManager sessionManager;

    // 세션 키 추출용 패턴
    private static final Pattern SESSION_KEY_PATTERN = Pattern.compile(
        "/ws/memorial-video/(?:web/|mobile-web/|ios/|android/|native/)?([^/?]+)"
    );

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                 ServerHttpResponse response,
                                 WebSocketHandler wsHandler,
                                 Map<String, Object> attributes) throws Exception {
        
        URI uri = request.getURI();
        String path = uri.getPath();
        String query = uri.getQuery();

        log.info("🔍 WebSocket 인증 체크 시작 - Path: {}, Query: {}", path, query);

        try {
            // 1. URL에서 세션 키 추출
            String sessionKey = extractSessionKey(path);
            if (sessionKey == null) {
                log.warn("🔒 WebSocket 인증 실패: 세션 키 추출 불가 - Path: {}", path);
                return false;
            }

            // 2. URL 파라미터에서 토큰 추출
            String token = extractTokenFromQuery(query);
            if (token == null || token.isEmpty()) {
                log.warn("🔒 WebSocket 인증 실패: 토큰 누락 - SessionKey: {}", sessionKey);
                return false;
            }

            // 3. 기존 JwtTokenProvider로 회원 토큰 검증
            if (!jwtTokenProvider.validateMemberToken(token)) {
                log.warn("🔒 WebSocket 인증 실패: 토큰 검증 실패 - SessionKey: {}", sessionKey);
                return false;
            }

            // 4. 토큰에서 회원 정보 추출
            Map<String, Object> memberClaims = jwtTokenProvider.getMemberClaims(token);
            Long memberId = ((Number) memberClaims.get("memberId")).longValue();
            String userKey = (String) memberClaims.get("userKey");
            String email = (String) memberClaims.get("email");

            if (memberId == null) {
                log.warn("🔒 WebSocket 인증 실패: 회원ID 누락 - SessionKey: {}", sessionKey);
                return false;
            }

            // 5. 세션 존재 및 소유권 확인
            if (!validateSessionAccess(sessionKey, memberId)) {
                log.warn("🔒 WebSocket 세션 접근 권한 없음 - SessionKey: {}, MemberId: {}", 
                        sessionKey, memberId);
                return false;
            }

            // 6. 인증 성공 - WebSocket 세션에 인증 정보 저장
            attributes.put("authenticated", true);
            attributes.put("memberId", memberId);
            attributes.put("userKey", userKey);
            attributes.put("email", email);
            attributes.put("sessionKey", sessionKey);
            attributes.put("authToken", token);

            log.info("✅ WebSocket 인증 성공 - SessionKey: {}, MemberId: {}, Email: {}", 
                    sessionKey, memberId, email);

            return true;

        } catch (ExpiredJwtException e) {
            log.warn("🔒 WebSocket 토큰 만료: {} - SessionKey: {}", e.getMessage(), 
                    extractSessionKey(path));
            return false;

        } catch (Exception e) {
            log.error("🔒 WebSocket 인증 중 예외 발생 - Path: {}", path, e);
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
     * 세션 존재 및 접근 권한 확인
     */
    private boolean validateSessionAccess(String sessionKey, Long memberId) {
        try {
            // 세션 존재 확인
            var session = sessionManager.getSession(sessionKey);
            if (session == null) {
                log.warn("🔒 세션이 존재하지 않음 - SessionKey: {}", sessionKey);
                return false;
            }

            // 세션 만료 확인
            if (session.isExpired()) {
                log.warn("🔒 세션이 만료됨 - SessionKey: {}, Age: {}분", 
                        sessionKey, session.getAgeInMinutes());
                return false;
            }

            // 세션 소유권 확인
            if (!memberId.equals(session.getCallerId())) {
                log.warn("🔒 세션 소유권 불일치 - SessionKey: {}, 토큰회원ID: {}, 세션회원ID: {}", 
                        sessionKey, memberId, session.getCallerId());
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("🔒 세션 검증 중 오류 - SessionKey: {}, MemberId: {}", sessionKey, memberId, e);
            return false;
        }
    }

    private String extractTokenFromQuery(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        // ?token=xxx&other=yyy 형태에서 token 값 추출
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith("token=")) {
                String token = param.substring(6); // "token=" 제거
                try {
                    // URL 디코딩
                    return java.net.URLDecoder.decode(token, "UTF-8");
                } catch (Exception e) {
                    log.warn("🔒 토큰 URL 디코딩 실패: {}", e.getMessage());
                    return token; // 디코딩 실패시 원본 반환
                }
            }
        }

        return null;
    }
}