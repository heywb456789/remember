package com.tomato.remember.common.security;

import com.tomato.remember.admin.security.AdminUserDetails;
import com.tomato.remember.admin.security.AdminUserDetailsService;
import com.tomato.remember.admin.user.entity.Admin;
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
public class AdminApiJwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final AdminUserDetailsService adminUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("AdminApiJwtFilter processing: {}", requestURI);

        try {
            String token = tokenProvider.extractBearerToken(request);

            if (token == null) {
                log.debug("No Bearer token found in admin API request: {}", requestURI);
                sendUnauthorizedResponse(response, "Missing Authorization token");
                return;
            }

            // 토큰 유효성 검증 및 인증 처리
            if (tokenProvider.validateAdminToken(token)) {
                authenticateAdmin(request, token);
                filterChain.doFilter(request, response);
            } else {
                log.debug("Invalid admin token for API request: {}", requestURI);
                sendUnauthorizedResponse(response, "Invalid admin token");
            }

        } catch (ExpiredJwtException ex) {
            log.debug("Admin API token expired for request: {}", requestURI);
            sendTokenExpiredResponse(response);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Admin API token validation failed for request: {}: {}", requestURI, ex.getMessage());
            sendUnauthorizedResponse(response, "Invalid token format");
        } catch (Exception ex) {
            log.error("Unexpected error in AdminApiJwtFilter for request: {}", requestURI, ex);
            sendUnauthorizedResponse(response, "Authentication error");
        }
    }

    private void authenticateAdmin(HttpServletRequest request, String token) {
        try {
            Map<String, Object> claims = tokenProvider.getAdminClaims(token);
            String username = (String) claims.get("username");
            if (username == null) {
                username = tokenProvider.getSubject(token);
            }

            UserDetails userDetails = adminUserDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Admin authenticated successfully via API: {}", username);

        } catch (Exception e) {
            log.error("Failed to authenticate admin via API", e);
            throw new RuntimeException("Authentication failed", e);
        }
    }

    /**
     * Admin API 401 Unauthorized 응답 - 리다이렉트 없이 JSON 응답
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

        log.debug("Admin API 401 response sent: {}", message);
    }

    /**
     * Admin API 토큰 만료 응답 - 리다이렉트 없이 JSON 응답
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
                    "message": "Admin token expired. Please refresh token."
                },
                "response": null,
                "timestamp": "%s"
            }
            """, java.time.LocalDateTime.now());

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();

        log.debug("Admin API token expired response sent");
    }

    // 정적 메서드들
    public static Admin getCurrentAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdminUserDetails) {
            return ((AdminUserDetails) authentication.getPrincipal()).getAdmin();
        }
        return null;
    }

    public static boolean hasAdminRole(String... roles) {
        Admin admin = getCurrentAdmin();
        if (admin == null) return false;

        String adminRole = admin.getRole().name();
        for (String role : roles) {
            if (role.equals(adminRole)) {
                return true;
            }
        }
        return false;
    }
}