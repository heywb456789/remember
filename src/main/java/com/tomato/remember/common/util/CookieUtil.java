package com.tomato.remember.common.util;

import com.tomato.remember.common.dto.TokenStateInfo;
import com.tomato.remember.common.dto.TokenUpdateResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class CookieUtil {

    // 쿠키 설정값들
    @Value("${spring.security.jwt.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${spring.security.jwt.cookie.http-only:true}")
    private boolean cookieHttpOnly;

    @Value("${spring.security.jwt.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${spring.security.jwt.cookie.path:/}")
    private String cookiePath;

    @Value("${spring.security.jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${spring.security.jwt.refresh-token-expiration:1209600000}")
    private long refreshTokenExpiration;

    // 쿠키 이름 상수
    public static final String MEMBER_ACCESS_TOKEN = "MEMBER_ACCESS_TOKEN";
    public static final String MEMBER_REFRESH_TOKEN = "MEMBER_REFRESH_TOKEN";

    // 동기화용 헤더 이름
    public static final String HEADER_NEW_ACCESS_TOKEN = "X-New-Access-Token";
    public static final String HEADER_NEW_REFRESH_TOKEN = "X-New-Refresh-Token";

    /**
     * 회원 JWT 토큰 쿠키 설정 (모바일 뷰용)
     */
    public void setMemberTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        log.info("🍪 Setting member JWT token cookies...");

        if (accessToken != null) {
            ResponseCookie accessCookie = createTokenCookie(
                    MEMBER_ACCESS_TOKEN,
                    accessToken,
                    (int) (accessTokenExpiration / 1000)
            );
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            log.info("✅ {} cookie set (expires in {} seconds)",
                    MEMBER_ACCESS_TOKEN, accessTokenExpiration / 1000);
        }

        if (refreshToken != null) {
            ResponseCookie refreshCookie = createTokenCookie(
                    MEMBER_REFRESH_TOKEN,
                    refreshToken,
                    (int) (refreshTokenExpiration / 1000)
            );
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
            log.info("✅ {} cookie set (expires in {} seconds)",
                    MEMBER_REFRESH_TOKEN, refreshTokenExpiration / 1000);
        }

        log.info("✅ Member JWT token cookies setting completed");
    }

    /**
     * 회원 JWT 토큰 쿠키 + localStorage 동기화 헤더 설정
     */
    public void setMemberTokensWithSync(HttpServletResponse response, String accessToken, String refreshToken) {
        log.info("🔧 Setting member tokens with sync...");

        // 1. 쿠키 설정
        setMemberTokenCookies(response, accessToken, refreshToken);

        // 2. localStorage 동기화용 헤더 설정
        if (accessToken != null) {
            response.setHeader(HEADER_NEW_ACCESS_TOKEN, accessToken);
            log.debug("📤 Set sync header for access token");
        }

        if (refreshToken != null) {
            response.setHeader(HEADER_NEW_REFRESH_TOKEN, refreshToken);
            log.debug("📤 Set sync header for refresh token");
        }

        log.info("✅ Member tokens set with sync headers");
    }

    /**
     * 회원 토큰 쿠키에서 Access Token 추출
     */
    public String getMemberAccessToken(HttpServletRequest request) {
        return getCookieValue(request, MEMBER_ACCESS_TOKEN);
    }

    /**
     * 회원 토큰 쿠키에서 Refresh Token 추출
     */
    public String getMemberRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, MEMBER_REFRESH_TOKEN);
    }

    /**
     * 회원 JWT 토큰 쿠키 삭제
     */
    public void clearMemberTokenCookies(HttpServletResponse response) {
        log.info("🗑️ Clearing member JWT token cookies...");

        ResponseCookie clearAccessCookie = createClearCookie(MEMBER_ACCESS_TOKEN);
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccessCookie.toString());
        log.info("🗑️ {} cookie cleared", MEMBER_ACCESS_TOKEN);

        ResponseCookie clearRefreshCookie = createClearCookie(MEMBER_REFRESH_TOKEN);
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString());
        log.info("🗑️ {} cookie cleared", MEMBER_REFRESH_TOKEN);

        log.info("✅ Member JWT token cookies cleared");
    }

    /**
     * 특정 쿠키값 추출
     */
    public String getCookieValue(HttpServletRequest request, String cookieName) {
        log.debug("🔍 Looking for cookie: {}", cookieName);

        if (request.getCookies() == null) {
            log.warn("❌ No cookies found in request");
            return null;
        }

        log.debug("🍪 Total cookies in request: {}", request.getCookies().length);

        for (Cookie cookie : request.getCookies()) {
            log.debug("🍪 Checking cookie: {} = {}",
                    cookie.getName(),
                    cookie.getValue() != null && cookie.getValue().length() > 30 ?
                            cookie.getValue().substring(0, 30) + "..." : cookie.getValue());

            if (cookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                boolean isValidValue = value != null && !value.trim().isEmpty();

                // JWT 토큰 형식 검증 추가
                if (isValidValue && (cookieName.equals(MEMBER_ACCESS_TOKEN) || cookieName.equals(MEMBER_REFRESH_TOKEN))) {
                    // JWT는 세 부분으로 구성: header.payload.signature
                    String[] parts = value.split("\\.");
                    if (parts.length != 3) {
                        log.error("❌ Invalid JWT format in cookie {}: expected 3 parts, got {}", cookieName, parts.length);
                        log.error("❌ Cookie value: {}", value);
                        return null;
                    }

                    // 각 부분이 Base64로 인코딩되어 있는지 확인
                    for (int i = 0; i < parts.length; i++) {
                        if (parts[i].isEmpty()) {
                            log.error("❌ Empty JWT part {} in cookie {}", i, cookieName);
                            return null;
                        }
                    }

                    log.debug("✅ JWT format validation passed for cookie: {}", cookieName);
                }

                log.info("✅ Cookie found: {} = {} (valid: {})",
                        cookieName,
                        value != null && value.length() > 30 ? value.substring(0, 30) + "..." : value,
                        isValidValue);

                return isValidValue ? value.trim() : null;
            }
        }

        log.warn("❌ Cookie not found: {}", cookieName);
        return null;
    }

    /**
     * 쿠키 생성 (공통 로직)
     */
    private ResponseCookie createTokenCookie(String name, String value, int maxAgeSeconds) {
        return ResponseCookie.from(name, value)
            .httpOnly(cookieHttpOnly)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path(cookiePath)
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .build();
    }

    /**
     * 쿠키 삭제용 빈 쿠키 생성
     */
    private ResponseCookie createClearCookie(String name) {
        return ResponseCookie.from(name, "")
            .httpOnly(cookieHttpOnly)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path(cookiePath)
            .maxAge(0)
            .build();
    }

    /**
     * 동적 보안 설정으로 쿠키 생성 (환경별 대응)
     */
    public void setMemberTokenCookiesWithDynamicSecurity(HttpServletResponse response,
                                                        HttpServletRequest request,
                                                        String accessToken,
                                                        String refreshToken) {
        log.debug("Setting member JWT token cookies with dynamic security");

        boolean isSecureRequest = isSecureRequest(request);

        if (accessToken != null) {
            ResponseCookie accessCookie = ResponseCookie.from(MEMBER_ACCESS_TOKEN, accessToken)
                .httpOnly(cookieHttpOnly)
                .secure(isSecureRequest)
                .sameSite(cookieSameSite)
                .path(cookiePath)
                .maxAge(Duration.ofSeconds(accessTokenExpiration / 1000))
                .build();
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        }

        if (refreshToken != null) {
            ResponseCookie refreshCookie = ResponseCookie.from(MEMBER_REFRESH_TOKEN, refreshToken)
                .httpOnly(cookieHttpOnly)
                .secure(isSecureRequest)
                .sameSite(cookieSameSite)
                .path(cookiePath)
                .maxAge(Duration.ofSeconds(refreshTokenExpiration / 1000))
                .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }

        log.debug("Member JWT token cookies set with secure: {}", isSecureRequest);
    }

    /**
     * 요청의 보안 상태 확인 (HTTPS 여부)
     */
    private boolean isSecureRequest(HttpServletRequest request) {
        return request.isSecure() ||
               "https".equalsIgnoreCase(request.getScheme()) ||
               "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    /**
     * 쿠키 설정 정보 로깅 (디버깅용)
     */
    public void logCookieSettings() {
        log.info("=== Cookie Settings ===");
        log.info("Secure: {}", cookieSecure);
        log.info("HttpOnly: {}", cookieHttpOnly);
        log.info("SameSite: {}", cookieSameSite);
        log.info("Path: {}", cookiePath);
        log.info("Access Token Expiration: {} seconds", accessTokenExpiration / 1000);
        log.info("Refresh Token Expiration: {} seconds", refreshTokenExpiration / 1000);
        log.info("=====================");
    }

    /**
     * Request에 토큰 갱신 정보 설정 (필터 → 컨트롤러 전달용)
     */
    public void setTokenRefreshAttributes(HttpServletRequest request, String accessToken, String refreshToken) {
        if (accessToken != null) {
            request.setAttribute("NEW_ACCESS_TOKEN", accessToken);
        }
        if (refreshToken != null) {
            request.setAttribute("NEW_REFRESH_TOKEN", refreshToken);
        }
    }

    /**
     * Request에서 토큰 갱신 정보 추출
     */
    public String getNewAccessToken(HttpServletRequest request) {
        return (String) request.getAttribute("NEW_ACCESS_TOKEN");
    }

    public String getNewRefreshToken(HttpServletRequest request) {
        return (String) request.getAttribute("NEW_REFRESH_TOKEN");
    }

    /**
     * 원자적 토큰 설정 - All or Nothing 방식
     */
    public TokenUpdateResult setMemberTokensAtomic(HttpServletResponse response,
                                                   String accessToken,
                                                   String refreshToken) {
        log.info("🔧 Starting atomic token update...");

        try {
            // 1단계: 검증
            if (!isValidTokenPair(accessToken, refreshToken)) {
                log.error("❌ Invalid token pair provided");
                return TokenUpdateResult.failure("Invalid tokens");
            }

            // 2단계: 원자적 설정
            setMemberTokensWithSync(response, accessToken, refreshToken);

            // 3단계: 성공 로그
            log.info("✅ Atomic token update completed successfully");
            return TokenUpdateResult.success();

        } catch (Exception e) {
            log.error("❌ Atomic token update failed - performing cleanup", e);

            // 실패 시 모든 토큰 정리
            clearAllMemberTokens(response);

            return TokenUpdateResult.failure(e.getMessage());
        }
    }

    /**
     * 완전한 토큰 정리 - 모든 관련 토큰/헤더 삭제
     */
    public void clearAllMemberTokens(HttpServletResponse response) {
        log.info("🗑️ Starting complete token cleanup...");

        // 1. 기존 쿠키 정리 메서드 호출
        clearMemberTokenCookies(response);

        // 2. 동기화 헤더 정리 신호
        response.setHeader("X-Token-Cleared", "true");
        response.setHeader("X-Clear-LocalStorage", "member_tokens");

        // 3. 기존 동기화 헤더 제거
        response.setHeader(HEADER_NEW_ACCESS_TOKEN, "");
        response.setHeader(HEADER_NEW_REFRESH_TOKEN, "");

        log.info("✅ Complete token cleanup finished");
    }

    /**
     * 토큰 쌍 유효성 검증
     */
    private boolean isValidTokenPair(String accessToken, String refreshToken) {
        if (accessToken == null || refreshToken == null) {
            return false;
        }

        // JWT 형식 검증
        return isValidJwtFormat(accessToken) && isValidJwtFormat(refreshToken);
    }

    /**
     * JWT 형식 검증
     */
    private boolean isValidJwtFormat(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        for (String part : parts) {
            if (part.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 토큰 상태 진단
     */
    public TokenStateInfo diagnoseTokenState(HttpServletRequest request) {
        String accessToken = getMemberAccessToken(request);
        String refreshToken = getMemberRefreshToken(request);

        return TokenStateInfo.builder()
                .hasAccessToken(accessToken != null)
                .hasRefreshToken(refreshToken != null)
                .accessTokenValid(isValidJwtFormat(accessToken))
                .refreshTokenValid(isValidJwtFormat(refreshToken))
                .isComplete(accessToken != null && refreshToken != null)
                .build();
    }
}