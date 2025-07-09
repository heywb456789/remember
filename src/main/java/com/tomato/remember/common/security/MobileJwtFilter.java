package com.tomato.remember.common.security;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.application.security.MemberUserDetailsService;
import com.tomato.remember.common.util.CookieUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class MobileJwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final MemberUserDetailsService memberUserDetailsService;
    private final CookieUtil cookieUtil;

    // 인증이 필요한 경로들
    private static final String[] AUTH_REQUIRED_PATHS = {
            "/mobile/dashboard",
            "/mobile/memorial/create",
            "/mobile/memorial/*/edit",
            "/mobile/video-call",
            "/mobile/mypage"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("MobileJwtFilter processing: {}", requestURI);

        // 로그인 관련 경로는 필터에서 제외
        if (shouldSkipFilter(requestURI)) {
            log.debug("Skipping filter for: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        boolean isAuthRequired = isAuthRequiredPath(requestURI);
        log.debug("Auth required for {}: {}", requestURI, isAuthRequired);

        try {
            String accessToken = cookieUtil.getMemberAccessToken(request);
            log.debug("Access token from cookie: {}", accessToken != null ? "PRESENT" : "NULL");

            if (accessToken == null) {
                log.debug("No access token found in cookies");
                handleNoToken(request, response, filterChain, isAuthRequired);
                return;
            }

            // 토큰 유효성 검증
            if (tokenProvider.validateMemberToken(accessToken)) {
                log.debug("Valid token found, authenticating member");
                authenticateMember(request, accessToken);
                filterChain.doFilter(request, response);
            } else {
                log.debug("Invalid access token found");
                handleInvalidToken(request, response, filterChain, isAuthRequired);
            }

        } catch (ExpiredJwtException ex) {
            log.debug("Access token expired: {}", ex.getMessage());
            handleExpiredToken(request, response, filterChain, isAuthRequired);

        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            handleInvalidToken(request, response, filterChain, isAuthRequired);

        } catch (Exception ex) {
            log.error("Unexpected error in MobileJwtFilter for request: {}", requestURI, ex);

            // 토큰 관련 오류가 아닌 일반적인 오류는 그대로 전달
            if (isTokenRelatedError(ex)) {
                handleInvalidToken(request, response, filterChain, isAuthRequired);
            } else {
                filterChain.doFilter(request, response);
            }
        }
    }

    /**
     * 토큰과 관련된 오류인지 확인
     */
    private boolean isTokenRelatedError(Exception ex) {
        if (ex instanceof JwtException || ex instanceof IllegalArgumentException) {
            return true;
        }

        String message = ex.getMessage();
        if (message != null) {
            return message.contains("token") ||
                    message.contains("authentication") ||
                    message.contains("JWT") ||
                    message.contains("Member ID");
        }

        return false;
    }

    /**
     * 토큰이 없는 경우 처리
     */
    private void handleNoToken(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, boolean isAuthRequired)
            throws IOException, ServletException {
        if (isAuthRequired) {
            log.debug("Redirecting to login for auth required path");
            redirectToLogin(response);
        } else {
            log.debug("Continuing as guest for optional auth path");
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 유효하지 않은 토큰 처리 (토큰 만료 제외)
     */
    private void handleInvalidToken(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain, boolean isAuthRequired) throws IOException, ServletException {
        log.debug("Handling invalid token - clearing cookies");
        cookieUtil.clearMemberTokenCookies(response);

        if (isAuthRequired) {
            log.debug("Redirecting to login for invalid token");
            redirectToLogin(response);
        } else {
            log.debug("Continuing as guest after token invalidation");
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 토큰 만료 처리 - 리프레시 토큰으로 갱신 시도
     */
    private void handleExpiredToken(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain, boolean isAuthRequired) throws IOException, ServletException {
        log.debug("Handling expired token");

        String refreshToken = cookieUtil.getMemberRefreshToken(request);
        log.debug("Refresh token: {}", refreshToken != null ? "PRESENT" : "NULL");

        if (refreshToken == null) {
            log.debug("No refresh token found - clearing cookies");
            cookieUtil.clearMemberTokenCookies(response);
            handlePostClearAction(request, response, filterChain, isAuthRequired);
            return;
        }

        try {
            if (!tokenProvider.validateMemberToken(refreshToken)) {
                log.debug("Invalid refresh token - clearing cookies");
                cookieUtil.clearMemberTokenCookies(response);
                handlePostClearAction(request, response, filterChain, isAuthRequired);
                return;
            }

            log.debug("Attempting to refresh tokens");
            String newAccessToken = refreshTokens(request, response, refreshToken);
            authenticateMember(request, newAccessToken);

            log.debug("Token refresh successful");
            filterChain.doFilter(request, response);

        } catch (Exception refreshEx) {
            log.error("Token refresh failed - clearing cookies", refreshEx);
            cookieUtil.clearMemberTokenCookies(response);
            handlePostClearAction(request, response, filterChain, isAuthRequired);
        }
    }

    /**
     * 쿠키 클리어 후 후속 처리
     */
    private void handlePostClearAction(HttpServletRequest request, HttpServletResponse response,
                                       FilterChain filterChain, boolean isAuthRequired) throws IOException, ServletException {
        if (isAuthRequired) {
            redirectToLogin(response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 회원 인증 처리
     */
    private void authenticateMember(HttpServletRequest request, String token) {
        try {
            Map<String, Object> claims = tokenProvider.getMemberClaims(token);
            Long memberId = extractMemberId(claims, token);

            UserDetails userDetails = memberUserDetailsService.loadUserById(memberId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Member authenticated successfully: {}", memberId);

        } catch (Exception e) {
            log.error("Failed to authenticate member", e);
            throw new RuntimeException("Authentication failed", e);
        }
    }

    /**
     * 토큰에서 Member ID 추출
     */
    private Long extractMemberId(Map<String, Object> claims, String token) {
        Long memberId = null;
        Object memberIdClaim = claims.get("memberId");

        if (memberIdClaim instanceof Number) {
            memberId = ((Number) memberIdClaim).longValue();
        } else if (memberIdClaim instanceof String) {
            try {
                memberId = Long.parseLong((String) memberIdClaim);
            } catch (NumberFormatException e) {
                log.error("Invalid memberId format in claim: {}", memberIdClaim);
                throw new RuntimeException("Invalid memberId format in claim", e);
            }
        }

        // Subject에서도 시도 (fallback)
        if (memberId == null) {
            String memberIdStr = tokenProvider.getSubject(token);
            try {
                memberId = Long.parseLong(memberIdStr);
            } catch (NumberFormatException e) {
                log.error("Invalid member ID format in subject: {}", memberIdStr);
                throw new RuntimeException("Invalid member ID format in subject", e);
            }
        }

        if (memberId == null) {
            throw new RuntimeException("Member ID not found in token");
        }

        return memberId;
    }

    /**
     * 토큰 갱신
     */
    private String refreshTokens(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        try {
            String memberIdStr = tokenProvider.getSubject(refreshToken);
            Long memberId = Long.parseLong(memberIdStr);

            log.debug("Refreshing tokens for member ID: {}", memberId);

            UserDetails userDetails = memberUserDetailsService.loadUserById(memberId);
            Member member = ((MemberUserDetails) userDetails).getMember();

            String newAccessToken = tokenProvider.createMemberAccessToken(member);
            String newRefreshToken = tokenProvider.createMemberRefreshToken(member, false);

            cookieUtil.setMemberTokensWithSync(response, newAccessToken, newRefreshToken);
            cookieUtil.setTokenRefreshAttributes(request, newAccessToken, newRefreshToken);

            log.debug("Tokens refreshed successfully for member: {}", member.getName());
            return newAccessToken;

        } catch (Exception e) {
            log.error("Failed to refresh tokens", e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    /**
     * 로그인 페이지로 리다이렉션
     */
    private void redirectToLogin(HttpServletResponse response) throws IOException {
        log.debug("Redirecting to login page");
        response.sendRedirect("/mobile/login");
    }

    /**
     * 인증이 필요한 경로인지 확인
     */
    private boolean isAuthRequiredPath(String requestURI) {
        for (String path : AUTH_REQUIRED_PATHS) {
            if (path.contains("*")) {
                String pattern = path.replace("*", "[^/]+");
                if (requestURI.matches(pattern)) {
                    return true;
                }
            } else if (requestURI.equals(path) || requestURI.startsWith(path + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 필터를 건너뛸 경로인지 확인
     */
    private boolean shouldSkipFilter(String requestURI) {
        String[] skipPaths = {
                "/mobile/login",
                "/mobile/register",
                "/mobile/auth/",
                "/mobile/forgot-password",
                "/mobile/reset-password",
                "/mobile/verify",
                "/mobile/public/",
                "/mobile/static/",
                "/mobile/assets/",
                "/mobile/css/",
                "/mobile/js/",
                "/mobile/images/"
        };

        for (String skipPath : skipPaths) {
            if (requestURI.equals(skipPath) || requestURI.startsWith(skipPath)) {
                return true;
            }
        }

        return false;
    }

    // 정적 메서드들
    public static Member getCurrentMember() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MemberUserDetails) {
            return ((MemberUserDetails) authentication.getPrincipal()).getMember();
        }
        return null;
    }

    public static Long getCurrentMemberId() {
        Member member = getCurrentMember();
        return member != null ? member.getId() : null;
    }

    public static boolean isAuthenticated() {
        return getCurrentMember() != null;
    }
}