package com.tomato.naraclub.common.util;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CookieUtil {

    // JWT 쿠키 설정값들
    @Value("${spring.security.jwt.cookie.secure:false}")
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
    public static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    public static final String REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN";

    /**
     * JWT 토큰 쿠키 설정 (로그인 시 사용)
     */
    public void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        log.debug("Setting JWT token cookies");

        // ACCESS_TOKEN 쿠키 설정
        if (accessToken != null) {
            ResponseCookie accessCookie = createTokenCookie(
                ACCESS_TOKEN_COOKIE,
                accessToken,
                (int) (accessTokenExpiration / 1000) // 밀리초를 초로 변환
            );
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            log.debug("ACCESS_TOKEN cookie set with expiration: {} seconds", accessTokenExpiration / 1000);
        }

        // REFRESH_TOKEN 쿠키 설정
        if (refreshToken != null) {
            ResponseCookie refreshCookie = createTokenCookie(
                REFRESH_TOKEN_COOKIE,
                refreshToken,
                (int) (refreshTokenExpiration / 1000) // 밀리초를 초로 변환
            );
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
            log.debug("REFRESH_TOKEN cookie set with expiration: {} seconds", refreshTokenExpiration / 1000);
        }
    }

    /**
     * JWT 토큰 쿠키 삭제 (로그아웃 시 사용)
     */
    public void clearTokenCookies(HttpServletResponse response) {
        log.debug("Clearing JWT token cookies");

        // ACCESS_TOKEN 쿠키 삭제
        ResponseCookie clearAccessCookie = createClearCookie(ACCESS_TOKEN_COOKIE);
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccessCookie.toString());

        // REFRESH_TOKEN 쿠키 삭제
        ResponseCookie clearRefreshCookie = createClearCookie(REFRESH_TOKEN_COOKIE);
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString());

        log.debug("JWT token cookies cleared");
    }

    /**
     * 특정 쿠키 삭제
     */
    public void clearCookie(HttpServletResponse response, String cookieName) {
        log.debug("Clearing cookie: {}", cookieName);

        ResponseCookie clearCookie = createClearCookie(cookieName);
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
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
     * 환경별 쿠키 보안 설정 동적 적용 (고급)
     */
    public void addTokenCookiesWithDynamicSecurity(HttpServletResponse response,
                                                  HttpServletRequest request,
                                                  String accessToken,
                                                  String refreshToken) {
        log.debug("Setting JWT token cookies with dynamic security");

        // 현재 요청의 보안 상태 확인
        boolean isSecureRequest = isSecureRequest(request);

        // ACCESS_TOKEN 쿠키 설정
        if (accessToken != null) {
            ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, accessToken)
                .httpOnly(cookieHttpOnly)
                .secure(isSecureRequest)  // 동적으로 결정
                .sameSite(cookieSameSite)
                .path(cookiePath)
                .maxAge(Duration.ofSeconds(accessTokenExpiration / 1000))
                .build();
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        }

        // REFRESH_TOKEN 쿠키 설정
        if (refreshToken != null) {
            ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(cookieHttpOnly)
                .secure(isSecureRequest)  // 동적으로 결정
                .sameSite(cookieSameSite)
                .path(cookiePath)
                .maxAge(Duration.ofSeconds(refreshTokenExpiration / 1000))
                .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }

        log.debug("JWT token cookies set with secure: {}", isSecureRequest);
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
}
