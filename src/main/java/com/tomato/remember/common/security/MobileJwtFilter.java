package com.tomato.remember.common.security;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.application.security.MemberUserDetailsService;
import com.tomato.remember.common.dto.TokenRefreshResult;
import com.tomato.remember.common.dto.TokenStateInfo;
import com.tomato.remember.common.dto.TokenUpdateResult;
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
            "/mobile/mypage",
            "/mobile/family"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String requestMethod = request.getMethod();
        log.debug("MobileJwtFilter processing: {} {}", requestMethod, requestURI);

        // 로그인 관련 경로는 필터에서 제외
        if (shouldSkipFilter(requestURI)) {
            log.debug("Skipping filter for: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        boolean isAuthRequired = isAuthRequiredPath(requestURI);
        log.debug("Auth required for {}: {}", requestURI, isAuthRequired);

        // 현재 쿠키 상태 로깅
        logCurrentCookieState(request);

        try {
            String accessToken = cookieUtil.getMemberAccessToken(request);
            log.debug("Access token from cookie: {}", accessToken != null ? "PRESENT (length=" + accessToken.length() + ")" : "NULL");

            if (accessToken == null) {
                log.info("No access token found in cookies for request: {}", requestURI);
                handleNoToken(request, response, filterChain, isAuthRequired);
                return;
            }

            // 토큰 유효성 검증
            if (tokenProvider.validateMemberToken(accessToken)) {
                log.debug("Valid token found, authenticating member for request: {}", requestURI);
                authenticateMember(request, accessToken);
                filterChain.doFilter(request, response);
            } else {
                log.warn("Invalid access token found for request: {}", requestURI);
                handleInvalidToken(request, response, filterChain, isAuthRequired);
            }

        } catch (ExpiredJwtException ex) {
            log.info("Access token expired for request: {}, attempting refresh", requestURI);
            handleExpiredToken(request, response, filterChain, isAuthRequired);

        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT validation failed for request: {}, error: {}", requestURI, ex.getMessage());
            handleInvalidToken(request, response, filterChain, isAuthRequired);

        } catch (Exception ex) {
            log.error("Unexpected error in MobileJwtFilter for request: {}", requestURI, ex);

            // 토큰 관련 오류가 아닌 일반적인 오류는 그대로 전달
            if (isTokenRelatedError(ex)) {
                log.warn("Token related error detected, treating as invalid token");
                handleInvalidToken(request, response, filterChain, isAuthRequired);
            } else {
                log.debug("Non-token related error, continuing filter chain");
                filterChain.doFilter(request, response);
            }
        }
    }

    /**
     * 현재 쿠키 상태 로깅
     */
    private void logCurrentCookieState(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            log.debug("Current cookies count: {}", cookies.length);
            for (Cookie cookie : cookies) {
                if (cookie.getName().startsWith("MEMBER_")) {
                    log.debug("Cookie found: name={}, value_length={}, maxAge={}, secure={}, httpOnly={}",
                            cookie.getName(),
                            cookie.getValue() != null ? cookie.getValue().length() : 0,
                            cookie.getMaxAge(),
                            cookie.getSecure(),
                            cookie.isHttpOnly());
                }
            }
        } else {
            log.debug("No cookies found in request");
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
    private void handleNoToken(HttpServletRequest request, HttpServletResponse response,
                               FilterChain filterChain, boolean isAuthRequired) throws IOException, ServletException {

        String requestURI = request.getRequestURI();
        log.debug("Handling no token scenario for: {}, authRequired: {}", requestURI, isAuthRequired);

        // 🆕 RefreshToken이 있으면 갱신 시도
        String refreshToken = cookieUtil.getMemberRefreshToken(request);
        if (refreshToken != null) {
            log.info("No access token but refresh token found, attempting refresh for: {}", requestURI);
            try {
                if (tokenProvider.validateMemberToken(refreshToken)) {
                    handleTokenRefreshFromNoAccess(request, response, filterChain, refreshToken, isAuthRequired);
                    return;
                } else {
                    log.warn("Refresh token invalid, clearing all tokens");
                    cookieUtil.clearAllMemberTokens(response);
                }
            } catch (Exception e) {
                log.error("Error during refresh token validation", e);
                cookieUtil.clearAllMemberTokens(response);
            }
        }

        // 기존 로직
        if (isAuthRequired) {
            log.info("Redirecting to login for auth required path: {}", requestURI);
            redirectToLogin(response);
        } else {
            log.debug("Continuing as guest for optional auth path: {}", requestURI);
            filterChain.doFilter(request, response);
        }
    }

    private void handleTokenRefreshFromNoAccess(HttpServletRequest request, HttpServletResponse response,
                                                FilterChain filterChain, String refreshToken, boolean isAuthRequired) throws ServletException, IOException {
        try {
            TokenRefreshResult refreshResult = performAtomicTokenRefresh(request, response, refreshToken);

            if (refreshResult.isSuccess()) {
                log.info("Token refresh successful from no-access scenario");
                authenticateMember(request, refreshResult.getNewAccessToken());
                filterChain.doFilter(request, response);
            } else {
                log.error("Token refresh failed from no-access scenario: {}", refreshResult.getErrorMessage());
                cookieUtil.clearAllMemberTokens(response);
                handlePostClearAction(request, response, filterChain, isAuthRequired);
            }
        } catch (Exception e) {
            log.error("Error during token refresh from no-access scenario", e);
            cookieUtil.clearAllMemberTokens(response);
            handlePostClearAction(request, response, filterChain, isAuthRequired);
        }
    }

    /**
     * 유효하지 않은 토큰 처리 (토큰 만료 제외)
     */
    private void handleInvalidToken(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain, boolean isAuthRequired) throws IOException, ServletException {

        String requestURI = request.getRequestURI();
        log.info("Handling invalid token for: {}, clearing all cookies", requestURI);

        // 쿠키 정리 전 상태 로깅
        logCurrentCookieState(request);

        cookieUtil.clearMemberTokenCookies(response);
        log.info("Member token cookies cleared for request: {}", requestURI);

        if (isAuthRequired) {
            log.info("Redirecting to login for invalid token on auth required path: {}", requestURI);
            redirectToLogin(response);
        } else {
            log.debug("Continuing as guest after token invalidation for path: {}", requestURI);
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 토큰 만료 처리 - 리프레시 토큰으로 갱신 시도
     */
    private void handleExpiredToken(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain,
                                    boolean isAuthRequired) throws IOException, ServletException {

        String requestURI = request.getRequestURI();
        log.info("Handling expired access token for request: {}", requestURI);

        // 1. 토큰 상태 진단
        TokenStateInfo tokenState = cookieUtil.diagnoseTokenState(request);
        log.info("Token state diagnosis: hasAccess={}, hasRefresh={}, isPartiallyBroken={}",
                tokenState.isHasAccessToken(), tokenState.isHasRefreshToken(), tokenState.isPartiallyBroken());

        // 2. 부분 손상 감지
        if (tokenState.isPartiallyBroken()) {
            log.warn("Partial token corruption detected for request: {}, cleaning all tokens", requestURI);
            cookieUtil.clearAllMemberTokens(response);
            handlePostClearAction(request, response, filterChain, isAuthRequired);
            return;
        }

        String refreshToken = cookieUtil.getMemberRefreshToken(request);
        if (refreshToken == null) {
            log.warn("No refresh token found for expired access token, request: {}", requestURI);
            cookieUtil.clearAllMemberTokens(response);
            handlePostClearAction(request, response, filterChain, isAuthRequired);
            return;
        }

        log.debug("Refresh token found (length={}), attempting validation", refreshToken.length());

        try {
            // RefreshToken 검증
            if (!tokenProvider.validateMemberToken(refreshToken)) {
                log.warn("Refresh token validation failed for request: {}", requestURI);
                cookieUtil.clearAllMemberTokens(response);
                handlePostClearAction(request, response, filterChain, isAuthRequired);
                return;
            }

            log.info("Refresh token valid, attempting atomic token refresh for request: {}", requestURI);

            // 원자적 토큰 갱신
            TokenRefreshResult refreshResult = performAtomicTokenRefresh(request, response, refreshToken);

            if (refreshResult.isSuccess()) {
                log.info("Token refresh successful for member ID: {}, request: {}",
                        refreshResult.getMemberId(), requestURI);
                authenticateMember(request, refreshResult.getNewAccessToken());
                filterChain.doFilter(request, response);
            } else {
                log.error("Token refresh failed for request: {}, error: {}", requestURI, refreshResult.getErrorMessage());
                // 갱신 실패 시 반드시 모든 토큰 정리
                cookieUtil.clearAllMemberTokens(response);
                log.info("All member tokens cleared after refresh failure for request: {}", requestURI);
                handlePostClearAction(request, response, filterChain, isAuthRequired);
            }

        } catch (ExpiredJwtException ex) {
            log.warn("Refresh token also expired for request: {}", requestURI);
            cookieUtil.clearAllMemberTokens(response);
            handlePostClearAction(request, response, filterChain, isAuthRequired);

        } catch (Exception ex) {
            log.error("Error during token refresh for request: {}", requestURI, ex);
            cookieUtil.clearAllMemberTokens(response);
            handlePostClearAction(request, response, filterChain, isAuthRequired);
        }
    }

    /**
     * 원자적 토큰 갱신
     */
    private TokenRefreshResult performAtomicTokenRefresh(HttpServletRequest request,
                                                         HttpServletResponse response,
                                                         String refreshToken) {
        try {
            log.debug("Starting atomic token refresh process");

            // 1. 회원 정보 조회
            String memberIdStr = tokenProvider.getSubject(refreshToken);
            Long memberId = Long.parseLong(memberIdStr);
            log.debug("Token refresh for member ID: {}", memberId);

            UserDetails userDetails = memberUserDetailsService.loadUserById(memberId);
            Member member = ((MemberUserDetails) userDetails).getMember();
            log.debug("Member loaded: name={}, status={}", member.getName(), member.getStatus());

            // 2. 새 토큰 쌍 생성
            String newAccessToken = tokenProvider.createMemberAccessToken(member);
            String newRefreshToken = tokenProvider.createMemberRefreshToken(member, false);
            log.debug("New tokens generated: accessToken_length={}, refreshToken_length={}",
                    newAccessToken.length(), newRefreshToken.length());

            // 3. 원자적 쿠키 설정
            TokenUpdateResult updateResult = cookieUtil.setMemberTokensAtomic(
                    response, newAccessToken, newRefreshToken
            );

            if (!updateResult.isSuccess()) {
                log.error("Cookie atomic update failed: {}", updateResult.getErrorMessage());
                return TokenRefreshResult.failure("Cookie update failed: " + updateResult.getErrorMessage());
            }

            // 4. Request 속성 설정
            cookieUtil.setTokenRefreshAttributes(request, newAccessToken, newRefreshToken);
            log.debug("Token refresh attributes set in request");

            log.info("Atomic token refresh completed successfully for member: {}", member.getName());
            return TokenRefreshResult.success(newAccessToken, newRefreshToken, memberId);

        } catch (Exception e) {
            log.error("Atomic token refresh failed", e);
            return TokenRefreshResult.failure(e.getMessage());
        }
    }

    /**
     * 쿠키 클리어 후 후속 처리
     */
    private void handlePostClearAction(HttpServletRequest request,
                                       HttpServletResponse response,
                                       FilterChain filterChain,
                                       boolean isAuthRequired) throws IOException, ServletException {

        String requestURI = request.getRequestURI();
        SecurityContextHolder.clearContext();
        log.debug("Security context cleared for request: {}", requestURI);

        if (isAuthRequired) {
            log.info("Auth required - redirecting to login for request: {}", requestURI);
            response.sendRedirect("/mobile/login?reason=token_expired");
        } else {
            log.debug("Guest access allowed - continuing for request: {}", requestURI);
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 회원 인증 처리
     */
    private void authenticateMember(HttpServletRequest request, String token) {
        try {
            log.debug("Starting member authentication process");

            Map<String, Object> claims = tokenProvider.getMemberClaims(token);
            Long memberId = extractMemberId(claims, token);
            log.debug("Extracted member ID from token: {}", memberId);

            UserDetails userDetails = memberUserDetailsService.loadUserById(memberId);
            Member member = ((MemberUserDetails) userDetails).getMember();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("Member authenticated successfully: ID={}, name={}, role={}",
                    member.getId(), member.getName(), member.getRole());

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
            log.debug("Member ID extracted from Number claim: {}", memberId);
        } else if (memberIdClaim instanceof String) {
            try {
                memberId = Long.parseLong((String) memberIdClaim);
                log.debug("Member ID extracted from String claim: {}", memberId);
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
                log.debug("Member ID extracted from token subject: {}", memberId);
            } catch (NumberFormatException e) {
                log.error("Invalid member ID format in subject: {}", memberIdStr);
                throw new RuntimeException("Invalid member ID format in subject", e);
            }
        }

        if (memberId == null) {
            log.error("Member ID not found in token claims or subject");
            throw new RuntimeException("Member ID not found in token");
        }

        return memberId;
    }

    /**
     * 로그인 페이지로 리다이렉션
     */
    private void redirectToLogin(HttpServletResponse response) throws IOException {
        String redirectUrl = "/mobile/login";
        log.info("Redirecting to login page: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    /**
     * 인증이 필요한 경로인지 확인
     */
    private boolean isAuthRequiredPath(String requestURI) {
        for (String path : AUTH_REQUIRED_PATHS) {
            if (path.contains("*")) {
                String pattern = path.replace("*", "[^/]+");
                if (requestURI.matches(pattern)) {
                    log.debug("Auth required path matched (pattern): {} -> {}", path, requestURI);
                    return true;
                }
            } else if (requestURI.equals(path) || requestURI.startsWith(path + "/")) {
                log.debug("Auth required path matched (exact): {} -> {}", path, requestURI);
                return true;
            }
        }
        log.debug("Auth not required for path: {}", requestURI);
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
                "/mobile/images/",
                "/mobile/family/invite/",
                "/mobile/video-call"
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