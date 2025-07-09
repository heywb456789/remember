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

    // shouldNotFilter 메서드 제거 - SecurityConfig에서 securityMatcher로 처리

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("AdminApiJwtFilter processing: {}", requestURI);

        try {
            String token = tokenProvider.extractBearerToken(request);

            if (token == null) {
                log.debug("No Bearer token found in request");
                sendUnauthorizedResponse(response, "Missing Authorization token");
                return;
            }

            // 토큰 유효성 검증 및 인증 처리
            if (tokenProvider.validateAdminToken(token)) {
                authenticateAdmin(request, token);
                filterChain.doFilter(request, response);
            } else {
                sendUnauthorizedResponse(response, "Invalid admin token");
            }

        } catch (ExpiredJwtException ex) {
            log.debug("Admin token expired: {}", ex.getMessage());
            handleExpiredToken(request, response, filterChain, ex);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Admin token validation failed: {}", ex.getMessage());
            sendUnauthorizedResponse(response, "Invalid token format");
        } catch (Exception ex) {
            log.error("Unexpected error in AdminApiJwtFilter", ex);
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
            log.debug("Admin authenticated successfully: {}", username);

        } catch (Exception e) {
            log.error("Failed to authenticate admin", e);
            throw new RuntimeException("Authentication failed", e);
        }
    }

    private void handleExpiredToken(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain,
                                  ExpiredJwtException ex) throws IOException {

        log.debug("Admin API token expired, sending 401");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
            {
                "status": {
                    "code": "UNAUTHORIZED_4001",
                    "message": "Admin token expired"
                },
                "response": null
            }
            """);
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