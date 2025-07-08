package com.tomato.remember.common.util;

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
        log.debug("Setting member JWT token cookies");

        if (accessToken != null) {
            ResponseCookie accessCookie = createTokenCookie(
                MEMBER_ACCESS_TOKEN,
                accessToken,
                (int) (accessTokenExpiration / 1000)
            );
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            log.debug("MEMBER_ACCESS_TOKEN cookie set with expiration: {} seconds", accessTokenExpiration / 1000);
        }

        if (refreshToken != null) {
            ResponseCookie refreshCookie = createTokenCookie(
                MEMBER_REFRESH_TOKEN,
                refreshToken,
                (int) (refreshTokenExpiration / 1000)
            );
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
            log.debug("MEMBER_REFRESH_TOKEN cookie set with expiration: {} seconds", refreshTokenExpiration / 1000);
        }
    }

    /**
     * 회원 JWT 토큰 쿠키 + localStorage 동기화 헤더 설정
     */
    public void setMemberTokensWithSync(HttpServletResponse response, String accessToken, String refreshToken) {
        // 1. 쿠키 설정
        setMemberTokenCookies(response, accessToken, refreshToken);

        // 2. localStorage 동기화용 헤더 설정
        if (accessToken != null) {
            response.setHeader(HEADER_NEW_ACCESS_TOKEN, accessToken);
            log.debug("Set sync header for access token");
        }

        if (refreshToken != null) {
            response.setHeader(HEADER_NEW_REFRESH_TOKEN, refreshToken);
            log.debug("Set sync header for refresh token");
        }
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
        log.debug("Clearing member JWT token cookies");

        ResponseCookie clearAccessCookie = createClearCookie(MEMBER_ACCESS_TOKEN);
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccessCookie.toString());

        ResponseCookie clearRefreshCookie = createClearCookie(MEMBER_REFRESH_TOKEN);
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString());

        log.debug("Member JWT token cookies cleared");
    }

    /**
     * 특정 쿠키값 추출
     */
    public String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
            }
        }
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
}