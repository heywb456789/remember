// MobileJwtFilter.java
package com.tomato.remember.common.security;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.application.security.MemberUserDetailsService;
import com.tomato.remember.common.util.CookieUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class MobileJwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final MemberUserDetailsService memberUserDetailsService;
    private final CookieUtil cookieUtil;

    // shouldNotFilter 메서드 제거 - SecurityConfig에서 securityMatcher로 처리

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("MobileJwtFilter processing: {}", requestURI);

        try {
            String accessToken = cookieUtil.getMemberAccessToken(request);

            if (accessToken == null) {
                log.debug("No access token found in cookies, redirecting to login");
                redirectToLogin(response);
                return;
            }

            // 토큰 유효성 검증 및 인증 처리
            if (tokenProvider.validateMemberToken(accessToken)) {
                authenticateMember(request, accessToken);
                filterChain.doFilter(request, response);
            } else {
                log.debug("Invalid access token, redirecting to login");
                cookieUtil.clearMemberTokenCookies(response);
                redirectToLogin(response);
            }

        } catch (ExpiredJwtException ex) {
            log.debug("Access token expired, attempting refresh");
            handleExpiredToken(request, response, filterChain, ex);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token validation failed: {}", ex.getMessage());
            cookieUtil.clearMemberTokenCookies(response);
            redirectToLogin(response);
        } catch (Exception ex) {
            log.error("Unexpected error in MobileJwtFilter", ex);
            cookieUtil.clearMemberTokenCookies(response);
            redirectToLogin(response);
        }
    }

    private void authenticateMember(HttpServletRequest request, String token) {
        try {
            Map<String, Object> claims = tokenProvider.getMemberClaims(token);
            String memberId = tokenProvider.getSubject(token);

            UserDetails userDetails = memberUserDetailsService.loadUserByUsername(memberId);

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

    private void handleExpiredToken(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain,
                                  ExpiredJwtException ex) throws IOException, ServletException {

        String refreshToken = cookieUtil.getMemberRefreshToken(request);

        if (refreshToken == null) {
            log.debug("No refresh token found, redirecting to login");
            cookieUtil.clearMemberTokenCookies(response);
            redirectToLogin(response);
            return;
        }

        try {
            if (!tokenProvider.validateMemberToken(refreshToken)) {
                log.debug("Invalid refresh token, redirecting to login");
                cookieUtil.clearMemberTokenCookies(response);
                redirectToLogin(response);
                return;
            }

            Map<String, Object> refreshClaims = tokenProvider.getMemberClaims(refreshToken);
            String memberId = tokenProvider.getSubject(refreshToken);

            UserDetails userDetails = memberUserDetailsService.loadUserByUsername(memberId);
            Member member = ((MemberUserDetails) userDetails).getMember();

            String newAccessToken = tokenProvider.createMemberAccessToken(member);
            String newRefreshToken = tokenProvider.createMemberRefreshToken(member, false);

            cookieUtil.setMemberTokensWithSync(response, newAccessToken, newRefreshToken);
            cookieUtil.setTokenRefreshAttributes(request, newAccessToken, newRefreshToken);

            authenticateMember(request, newAccessToken);

            log.info("Member token refreshed successfully for member: {}", memberId);
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException refreshEx) {
            log.debug("Refresh token also expired, redirecting to login");
            cookieUtil.clearMemberTokenCookies(response);
            redirectToLogin(response);
        } catch (Exception refreshEx) {
            log.error("Failed to refresh token", refreshEx);
            cookieUtil.clearMemberTokenCookies(response);
            redirectToLogin(response);
        }
    }

    private void redirectToLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/mobile/login");
    }

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