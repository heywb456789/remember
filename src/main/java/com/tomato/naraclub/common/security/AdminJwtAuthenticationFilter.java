package com.tomato.naraclub.common.security;

import com.tomato.naraclub.admin.security.AdminUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final AdminUserDetailsService adminDetailsService;

    public AdminJwtAuthenticationFilter(
        JwtTokenProvider tokenProvider,
        AdminUserDetailsService adminDetailsService
    ) {
        this.tokenProvider = tokenProvider;
        this.adminDetailsService = adminDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (!tokenProvider.validateToken(token)) {
                throw new BadCredentialsException("Invalid or expired token");
            }
            String subject = tokenProvider.getSubject(token);
            // 관리자 ID 를 이용해 로드
            UserDetails admin = adminDetailsService.loadUserByUsername(subject);
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
