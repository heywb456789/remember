// ApiJwtFilter.java - 다국어 제거 및 Enum 적용
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
            sendTokenExpiredResponse(response);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("API token validation failed: {}", ex.getMessage());
            sendUnauthorizedResponse(response, "Invalid token format");
        } catch (Exception ex) {
            log.error("Unexpected error in ApiJwtFilter", ex);
            sendUnauthorizedResponse(response, "Authentication error");
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
            log.debug("Member authenticated successfully via API: {}", memberId);

        } catch (Exception e) {
            log.error("Failed to authenticate member via API", e);
            throw new RuntimeException("Authentication failed", e);
        }
    }

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

    // 정적 메서드들 - Enum 사용으로 변경
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