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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // /mobile/** 경로가 아니면 필터 실행 안함
        boolean shouldNotFilter = !path.startsWith("/mobile/");
        log.debug("MobileJwtFilter shouldNotFilter for {}: {}", path, shouldNotFilter);
        return shouldNotFilter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("MobileJwtFilter processing: {}", requestURI);

        // 인증이 필요없는 경로는 패스
        if (isPublicPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

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

    /**
     * 회원 인증 처리
     */
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

    /**
     * 만료된 토큰 처리 - 자동 갱신 시도
     */
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
            // Refresh Token 검증
            if (!tokenProvider.validateMemberToken(refreshToken)) {
                log.debug("Invalid refresh token, redirecting to login");
                cookieUtil.clearMemberTokenCookies(response);
                redirectToLogin(response);
                return;
            }

            // 새 토큰 발급
            Map<String, Object> refreshClaims = tokenProvider.getMemberClaims(refreshToken);
            String memberId = tokenProvider.getSubject(refreshToken);
            
            UserDetails userDetails = memberUserDetailsService.loadUserByUsername(memberId);
            Member member = ((MemberUserDetails) userDetails).getMember();
            
            String newAccessToken = tokenProvider.createMemberAccessToken(member);
            String newRefreshToken = tokenProvider.createMemberRefreshToken(member, false);
            
            // 쿠키 및 동기화 헤더 설정
            cookieUtil.setMemberTokensWithSync(response, newAccessToken, newRefreshToken);
            
            // Request에 갱신 정보 설정 (컨트롤러에서 사용할 수 있도록)
            cookieUtil.setTokenRefreshAttributes(request, newAccessToken, newRefreshToken);
            
            // 인증 설정 후 계속 진행
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

    /**
     * 인증이 필요없는 공개 경로 확인
     */
    private boolean isPublicPath(String requestURI) {
        return requestURI.equals("/mobile/login") ||
               requestURI.equals("/mobile/register") ||
               requestURI.equals("/mobile/auth/login") ||
               requestURI.equals("/mobile/auth/register") ||
               requestURI.equals("/mobile/auth/refresh") ||
               requestURI.startsWith("/mobile/public/") ||
               requestURI.startsWith("/mobile/static/");
    }

    /**
     * 로그인 페이지로 리다이렉트
     */
    private void redirectToLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/mobile/login");
    }

    /**
     * 현재 인증된 회원 정보 추출 (유틸리티 메서드)
     */
    public static Member getCurrentMember() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MemberUserDetails) {
            return ((MemberUserDetails) authentication.getPrincipal()).getMember();
        }
        return null;
    }

    /**
     * 현재 회원 ID 추출 (유틸리티 메서드)
     */
    public static Long getCurrentMemberId() {
        Member member = getCurrentMember();
        return member != null ? member.getId() : null;
    }

    /**
     * 로그인 상태 확인 (유틸리티 메서드)
     */
    public static boolean isAuthenticated() {
        return getCurrentMember() != null;
    }
}