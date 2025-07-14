package com.tomato.remember.common.util;

import com.tomato.remember.common.dto.TokenStateInfo;
import com.tomato.remember.common.dto.TokenUpdateResult;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class CookieUtil {

    // JWT 쿠키 설정값들
    @Value("${spring.security.jwt.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${spring.security.jwt.cookie.http-only:true}")
    private boolean cookieHttpOnly;

    @Value("${spring.security.jwt.cookie.same-site:Strict}")
    private String cookieSameSite;

    @Value("${spring.security.jwt.cookie.path:/}")
    private String cookiePath;

    @Value("${spring.security.jwt.cookie.domain:}")
    private String cookieDomain;

    @Value("${spring.security.jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${spring.security.jwt.refresh-token-expiration:1209600000}")
    private long refreshTokenExpiration;

    // 쿠키 이름 상수
    private static final String MEMBER_ACCESS_TOKEN_COOKIE = "MEMBER_ACCESS_TOKEN";
    private static final String MEMBER_REFRESH_TOKEN_COOKIE = "MEMBER_REFRESH_TOKEN";

    @PostConstruct
    public void validateCookieConfiguration() {
        log.info("Cookie configuration validation starting");
        log.info("Cookie settings: secure={}, httpOnly={}, sameSite={}, path={}, domain='{}'",
                cookieSecure, cookieHttpOnly, cookieSameSite, cookiePath, cookieDomain);

        // SameSite=None + Secure=false 조합 경고
        if ("None".equalsIgnoreCase(cookieSameSite) && !cookieSecure) {
            log.error("INVALID COOKIE CONFIGURATION: SameSite=None requires Secure=true. Current: Secure={}", cookieSecure);
            log.error("Cookies may not be set in this configuration! Consider changing SameSite to 'Lax' for HTTP environments.");
        }

        // IP 환경에서 domain 설정 경고
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            log.warn("Domain is set to '{}'. For IP addresses, domain should be empty.", cookieDomain);
        }

        log.info("Cookie configuration validation completed");
    }

    /**
     * 회원 토큰을 쿠키에 설정하고 동기화용 속성 추가
     */
    public void setMemberTokensWithSync(HttpServletResponse response, String accessToken, String refreshToken) {
        log.info("Setting member tokens with sync attributes");
        log.debug("Token details: accessToken_length={}, refreshToken_length={}",
                accessToken != null ? accessToken.length() : 0,
                refreshToken != null ? refreshToken.length() : 0);

        try {
            // 쿠키 설정
            TokenUpdateResult result = setMemberTokensAtomic(response, accessToken, refreshToken);
            if (!result.isSuccess()) {
                log.error("Failed to set member tokens atomically: {}", result.getErrorMessage());
                throw new RuntimeException("Failed to set tokens: " + result.getErrorMessage());
            }

            log.info("Member tokens successfully set in cookies");

        } catch (Exception e) {
            log.error("Failed to set member tokens with sync", e);
            throw e;
        }
    }

    /**
     * 원자적 토큰 설정 - 모두 성공하거나 모두 실패
     */
    public TokenUpdateResult setMemberTokensAtomic(HttpServletResponse response, String accessToken, String refreshToken) {
        log.debug("Starting atomic token update");

        try {
            // 유효성 검증
            if (accessToken == null || accessToken.trim().isEmpty()) {
                log.error("Access token is null or empty");
                return TokenUpdateResult.failure("Access token is null or empty");
            }

            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                log.error("Refresh token is null or empty");
                return TokenUpdateResult.failure("Refresh token is null or empty");
            }

            // 쿠키 생성
            Cookie accessCookie = createMemberAccessTokenCookie(accessToken);
            Cookie refreshCookie = createMemberRefreshTokenCookie(refreshToken);

            // 쿠키 유효성 검증
            if (accessCookie == null) {
                log.error("Failed to create access token cookie");
                return TokenUpdateResult.failure("Failed to create access token cookie");
            }

            if (refreshCookie == null) {
                log.error("Failed to create refresh token cookie");
                return TokenUpdateResult.failure("Failed to create refresh token cookie");
            }

            // 원자적 설정
            response.addCookie(accessCookie);
            response.addCookie(refreshCookie);

            log.info("Tokens set atomically - Access cookie: name={}, secure={}, httpOnly={}, maxAge={}",
                    accessCookie.getName(), accessCookie.getSecure(), accessCookie.isHttpOnly(), accessCookie.getMaxAge());
            log.info("Tokens set atomically - Refresh cookie: name={}, secure={}, httpOnly={}, maxAge={}",
                    refreshCookie.getName(), refreshCookie.getSecure(), refreshCookie.isHttpOnly(), refreshCookie.getMaxAge());

            return TokenUpdateResult.success();

        } catch (Exception e) {
            log.error("Atomic token update failed", e);

            // 실패 시 정리 시도
            try {
                log.warn("Attempting to clear cookies after atomic update failure");
                clearMemberTokenCookies(response);
            } catch (Exception clearEx) {
                log.error("Failed to clear cookies after atomic update failure", clearEx);
            }

            return TokenUpdateResult.failure(e.getMessage());
        }
    }

    /**
     * Access Token 쿠키 생성
     */
    private Cookie createMemberAccessTokenCookie(String accessToken) {
        try {
            int maxAge = (int) (accessTokenExpiration / 1000); // JWT 만료 시간과 동일
            Cookie cookie = createTokenCookie(MEMBER_ACCESS_TOKEN_COOKIE, accessToken, maxAge);
            log.debug("Access token cookie created: maxAge={} seconds", maxAge);
            return cookie;
        } catch (Exception e) {
            log.error("Failed to create access token cookie", e);
            return null;
        }
    }

    /**
     * Refresh Token 쿠키 생성
     */
    private Cookie createMemberRefreshTokenCookie(String refreshToken) {
        try {
            int maxAge = (int) (refreshTokenExpiration / 1000);
            Cookie cookie = createTokenCookie(MEMBER_REFRESH_TOKEN_COOKIE, refreshToken, maxAge);
            log.debug("Refresh token cookie created: maxAge={} seconds", maxAge);
            return cookie;
        } catch (Exception e) {
            log.error("Failed to create refresh token cookie", e);
            return null;
        }
    }

    /**
     * 공통 토큰 쿠키 생성
     */
    private Cookie createTokenCookie(String name, String value, int maxAge) {
        log.debug("Creating cookie: name={}, value_length={}, maxAge={}", name, value.length(), maxAge);

        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(cookieHttpOnly);
        cookie.setSecure(cookieSecure);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(maxAge);

        // Domain 설정 (비어있지 않은 경우에만)
        if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
            cookie.setDomain(cookieDomain);
            log.debug("Cookie domain set to: {}", cookieDomain);
        } else {
            log.debug("Cookie domain not set (using default)");
        }

        log.debug("Cookie created with settings: secure={}, httpOnly={}, path={}",
                cookie.getSecure(), cookie.isHttpOnly(), cookie.getPath());

        return cookie;
    }

    /**
     * 요청에서 Access Token 추출
     */
    public String getMemberAccessToken(HttpServletRequest request) {
        String token = getCookieValue(request, MEMBER_ACCESS_TOKEN_COOKIE);
        log.debug("Access token retrieval: {}", token != null ? "found (length=" + token.length() + ")" : "not found");
        return token;
    }

    /**
     * 요청에서 Refresh Token 추출
     */
    public String getMemberRefreshToken(HttpServletRequest request) {
        String token = getCookieValue(request, MEMBER_REFRESH_TOKEN_COOKIE);
        log.debug("Refresh token retrieval: {}", token != null ? "found (length=" + token.length() + ")" : "not found");
        return token;
    }

    /**
     * 쿠키값 추출 유틸리티
     */
    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    log.debug("Cookie found: name={}, value_length={}", cookieName,
                            cookie.getValue() != null ? cookie.getValue().length() : 0);
                    return cookie.getValue();
                }
            }
        }
        log.debug("Cookie not found: {}", cookieName);
        return null;
    }

    /**
     * 회원 토큰 쿠키 삭제
     */
    public void clearMemberTokenCookies(HttpServletResponse response) {
        log.info("Clearing member token cookies");

        try {
            // Access Token 쿠키 삭제
            Cookie accessCookie = new Cookie(MEMBER_ACCESS_TOKEN_COOKIE, "");
            accessCookie.setHttpOnly(cookieHttpOnly);
            accessCookie.setSecure(cookieSecure);
            accessCookie.setPath(cookiePath);
            accessCookie.setMaxAge(0);

            if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
                accessCookie.setDomain(cookieDomain);
            }

            response.addCookie(accessCookie);
            log.debug("Access token cookie deletion scheduled");

            // Refresh Token 쿠키 삭제
            Cookie refreshCookie = new Cookie(MEMBER_REFRESH_TOKEN_COOKIE, "");
            refreshCookie.setHttpOnly(cookieHttpOnly);
            refreshCookie.setSecure(cookieSecure);
            refreshCookie.setPath(cookiePath);
            refreshCookie.setMaxAge(0);

            if (cookieDomain != null && !cookieDomain.trim().isEmpty()) {
                refreshCookie.setDomain(cookieDomain);
            }

            response.addCookie(refreshCookie);
            log.debug("Refresh token cookie deletion scheduled");

            log.info("Member token cookies cleared successfully");

        } catch (Exception e) {
            log.error("Failed to clear member token cookies", e);
            throw e;
        }
    }

    /**
     * 모든 회원 토큰 정리 (확장된 버전)
     */
    public void clearAllMemberTokens(HttpServletResponse response) {
        log.info("Clearing all member tokens");
        clearMemberTokenCookies(response);
        log.info("All member tokens cleared");
    }

    /**
     * 토큰 상태 진단
     */
    public TokenStateInfo diagnoseTokenState(HttpServletRequest request) {
        log.debug("Diagnosing token state");

        String accessToken = getMemberAccessToken(request);
        String refreshToken = getMemberRefreshToken(request);

        boolean hasAccess = accessToken != null && !accessToken.trim().isEmpty();
        boolean hasRefresh = refreshToken != null && !refreshToken.trim().isEmpty();
        boolean isPartiallyBroken = (hasAccess && !hasRefresh) || (!hasAccess && hasRefresh);

        log.debug("Token state: hasAccess={}, hasRefresh={}, isPartiallyBroken={}",
                hasAccess, hasRefresh, isPartiallyBroken);

        return TokenStateInfo.builder()
                .hasAccessToken(hasAccess)
                .hasRefreshToken(hasRefresh)
                .isPartiallyBroken(isPartiallyBroken)
                .build();
    }

    /**
     * 토큰 갱신용 Request 속성 설정 (Thymeleaf 동기화용)
     */
    public void setTokenRefreshAttributes(HttpServletRequest request, String newAccessToken, String newRefreshToken) {
        log.debug("Setting token refresh attributes for Thymeleaf sync");

        request.setAttribute("newAccessToken", newAccessToken);
        request.setAttribute("newRefreshToken", newRefreshToken);
        request.setAttribute("tokenRefreshedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        log.debug("Token refresh attributes set: accessToken_length={}, refreshToken_length={}",
                newAccessToken.length(), newRefreshToken.length());
    }

    /**
     * 쿠키 설정 정보 반환 (디버깅용)
     */
    public String getCookieConfigInfo() {
        return String.format("CookieConfig[secure=%s, httpOnly=%s, sameSite=%s, path=%s, domain='%s']",
                cookieSecure, cookieHttpOnly, cookieSameSite, cookiePath, cookieDomain);
    }

    /**
     * 현재 쿠키 상태 로깅 (디버깅용)
     */
    public void logCookieStatus(HttpServletRequest request) {
        log.info("=== Cookie Status Debug ===");
        log.info("Configuration: {}", getCookieConfigInfo());

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            log.info("Total cookies: {}", cookies.length);
            for (Cookie cookie : cookies) {
                if (cookie.getName().startsWith("MEMBER_")) {
                    log.info("Member cookie: name={}, value_length={}, maxAge={}, secure={}, httpOnly={}, path={}",
                            cookie.getName(),
                            cookie.getValue() != null ? cookie.getValue().length() : 0,
                            cookie.getMaxAge(),
                            cookie.getSecure(),
                            cookie.isHttpOnly(),
                            cookie.getPath());
                }
            }
        } else {
            log.info("No cookies found in request");
        }
        log.info("=== End Cookie Status ===");
    }
}