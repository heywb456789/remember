package com.tomato.remember.common.security;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.application.security.MemberUserDetailsService;
import com.tomato.remember.common.code.MemberRole;
import com.tomato.remember.common.code.MemberStatus;
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

        try {
            String token = tokenProvider.extractBearerToken(request);

            if (token == null) {
                log.debug("No Bearer token found in API request: {}", requestURI);
                sendUnauthorizedResponse(response, "Missing Authorization token");
                return;
            }

            // 토큰 유효성 검증 및 인증 처리
            if (tokenProvider.validateMemberToken(token)) {
                authenticateMember(request, token);
                filterChain.doFilter(request, response);
            } else {
                log.debug("Invalid member token for API request: {}", requestURI);
                sendUnauthorizedResponse(response, "Invalid member token");
            }

        } catch (ExpiredJwtException ex) {
            log.debug("API token expired for request: {}", requestURI);
            sendTokenExpiredResponse(response);

        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("API token validation failed for request: {}: {}", requestURI, ex.getMessage());
            sendUnauthorizedResponse(response, "Invalid token format");

        } catch (Exception ex) {
            log.error("Unexpected error in ApiJwtFilter for request: {}", requestURI, ex);

            // 토큰 관련 오류가 아닌 일반적인 오류는 그대로 전달 (404 등)
            if (isTokenRelatedError(ex)) {
                sendUnauthorizedResponse(response, "Authentication error");
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
                    message.contains("JWT") ||
                    message.contains("authentication") ||
                    message.contains("Member ID");
        }

        return false;
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
            log.debug("Member authenticated successfully via API: {}", memberId);

        } catch (Exception e) {
            log.error("Failed to authenticate member via API", e);
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
     * API 401 Unauthorized 응답
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        String jsonResponse = String.format("""
            {
                "status": {
                    "code": "UNAUTHORIZED_4001",
                    "message": "%s"
                },
                "response": null,
                "timestamp": "%s"
            }
            """, message, java.time.LocalDateTime.now());

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();

        log.debug("API 401 response sent: {}", message);
    }

    /**
     * API 토큰 만료 응답
     */
    private void sendTokenExpiredResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        String jsonResponse = String.format("""
            {
                "status": {
                    "code": "TOKEN_EXPIRED_4011",
                    "message": "Access token expired. Please refresh token."
                },
                "response": null,
                "timestamp": "%s"
            }
            """, java.time.LocalDateTime.now());

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();

        log.debug("API token expired response sent");
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

    public static boolean hasMemberRole(MemberRole... roles) {
        Member member = getCurrentMember();
        if (member == null) return false;

        MemberRole memberRole = member.getRole();
        for (MemberRole role : roles) {
            if (role.equals(memberRole)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMemberActive() {
        Member member = getCurrentMember();
        return member != null && member.getStatus() == MemberStatus.ACTIVE;
    }

    public static boolean isMemberBlocked() {
        Member member = getCurrentMember();
        return member != null && member.getStatus() == MemberStatus.BLOCKED;
    }

    public static boolean isMemberDeleted() {
        Member member = getCurrentMember();
        return member != null && member.getStatus() == MemberStatus.DELETED;
    }

    public static boolean isUserActive() {
        Member member = getCurrentMember();
        return member != null && member.getRole() == MemberRole.USER_ACTIVE;
    }

    public static boolean isUserInactive() {
        Member member = getCurrentMember();
        return member != null && member.getRole() == MemberRole.USER_INACTIVE;
    }
}