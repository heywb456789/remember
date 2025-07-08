package com.tomato.remember.common.security;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.application.security.MemberUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ApiJwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final MemberUserDetailsService memberUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("ApiJwtFilter processing: {}", requestURI);

        // 인증이 필요없는 경로는 패스
        if (isPublicPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = tokenProvider.extractBearerToken(request);
            
            if (token == null) {
                log.debug("No Bearer token found in API request");
                sendUnauthorizedResponse(response, "Missing Authorization token");
                return;
            }

            // 토큰 유효성 검증 및 인증 처리
            if (tokenProvider.validateMemberToken(token)) {
                authenticateMember(request, token);
                filterChain.doFilter(request, response);
            } else {
                sendUnauthorizedResponse(response, "Invalid member token");
            }

        } catch (ExpiredJwtException ex) {
            log.debug("API token expired: {}", ex.getMessage());
            // API는 자동 갱신하지 않고 401 응답 (클라이언트에서 처리)
            sendTokenExpiredResponse(response);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("API token validation failed: {}", ex.getMessage());
            sendUnauthorizedResponse(response, "Invalid token format");
        } catch (Exception ex) {
            log.error("Unexpected error in ApiJwtFilter", ex);
            sendUnauthorizedResponse(response, "Authentication error");
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
            log.debug("Member authenticated successfully via API: {}", memberId);
            
        } catch (Exception e) {
            log.error("Failed to authenticate member via API", e);
            throw new RuntimeException("Authentication failed", e);
        }
    }

    /**
     * 인증이 필요없는 공개 경로 확인
     */
    private boolean isPublicPath(String requestURI) {
        return requestURI.equals("/api/auth/login") ||
               requestURI.equals("/api/auth/refresh") ||
               requestURI.equals("/api/auth/register") ||
               requestURI.startsWith("/api/memorial/public/") ||
               requestURI.startsWith("/api/videos/public/") ||
               requestURI.startsWith("/api/news/public/") ||
               requestURI.startsWith("/api/share/") ||
               requestURI.equals("/api/auth/pass/callback") ||
               requestURI.startsWith("/api/subscription/confirm") ||
               requestURI.startsWith("/api/subscription/recurring");
    }

    /**
     * 401 Unauthorized 응답 전송
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("""
            {
                "status": {
                    "code": "UNAUTHORIZED_4001",
                    "message": "%s"
                },
                "response": null
            }
            """, message));
    }

    /**
     * 토큰 만료 응답 전송 (클라이언트에서 refresh 토큰으로 갱신하도록)
     */
    private void sendTokenExpiredResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
            {
                "status": {
                    "code": "TOKEN_EXPIRED_4011",
                    "message": "Access token expired. Please refresh token."
                },
                "response": null
            }
            """);
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

    /**
     * 회원 권한 확인 (유틸리티 메서드)
     */
    public static boolean hasMemberRole(String... roles) {
        Member member = getCurrentMember();
        if (member == null) return false;
        
        String memberRole = member.getRole().name();
        for (String role : roles) {
            if (role.equals(memberRole)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 회원 상태 확인 (유틸리티 메서드)
     */
    public static boolean isMemberActive() {
        Member member = getCurrentMember();
        return member != null && "ACTIVE".equals(member.getStatus().name());
    }
}