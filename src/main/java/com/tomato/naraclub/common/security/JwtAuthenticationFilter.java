package com.tomato.naraclub.common.security;

import com.tomato.naraclub.admin.security.AdminUserDetails;
import com.tomato.naraclub.admin.security.AdminUserDetailsService;
import com.tomato.naraclub.admin.user.code.AdminRole;
import com.tomato.naraclub.admin.user.entity.Admin;
import com.tomato.naraclub.application.member.entity.Member;
import com.tomato.naraclub.application.security.MemberUserDetails;
import com.tomato.naraclub.application.security.MemberUserDetailsService;
import com.tomato.naraclub.common.util.CookieUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ADMIN_AUTH_LOGIN = "/admin/auth/login";       // ← 변경: 로그인 페이지 URI
    private static final String ADMIN_AUTH_LOGOUT = "/admin/auth/logout";

    private final CookieUtil cookieUtil;
    private final JwtTokenProvider tokenProvider;
    private final MemberUserDetailsService memberDetailsService;
    private final AdminUserDetailsService adminDetailsService;
    private final AuthenticationEntryPoint entryPoint =
        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

    public JwtAuthenticationFilter(
        JwtTokenProvider tokenProvider,
        MemberUserDetailsService memberDetailsService,
        AdminUserDetailsService adminDetailsService,
        CookieUtil cookieUtil
    ) {
        this.tokenProvider = tokenProvider;
        this.memberDetailsService = memberDetailsService;
        this.adminDetailsService = adminDetailsService;
        this.cookieUtil = cookieUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
        HttpServletResponse res,
        FilterChain chain)
        throws ServletException, IOException {
        String uri = req.getRequestURI();
        boolean isAdminPage = uri.startsWith("/admin")
            && !uri.startsWith("/admin/auth/");
        String token = resolveToken(req);

        // ← 변경: 1) 토큰이 없으면 바로 로그인 페이지로
        if (isAdminPage && token == null) {
            cookieUtil.clearTokenCookies(res);
            res.sendRedirect(ADMIN_AUTH_LOGIN);
            return;
        }

        try {
            if (token != null) {
                validateAndAuthenticate(token);
            }
            chain.doFilter(req, res);

        } catch (ExpiredJwtException ex) {
            // ← 변경: 2) 토큰 만료시에도 관리자 페이지는 로그인으로
            if (isAdminPage) {
                // 관리 페이지: refresh 쿠키로 재발급
                String refresh = resolveRefreshToken(req);
                if (refresh != null && tokenProvider.validateToken(refresh)) {
                    String role = tokenProvider.getClaims(refresh).get("role").toString();
                    UserDetails user = loadUserByRole(role, refresh);

                    String newAccessToken = issueNewAccessToken(user, role);
                    String newRefresh = issueNewRefreshToken(user, role);

                    // 1) 쿠키에도 새 토큰 세팅
                    cookieUtil.addTokenCookies(res, newAccessToken, newRefresh);

                    // 2) 응답 헤더에도 새 액세스 토큰 세팅 → 클라이언트가 읽어서 localStorage 동기화 가능
                    res.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken);

                    // 인증 컨텍스트 설정 후 계속 진행
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    chain.doFilter(req, res);
                } else {
                    cookieUtil.clearTokenCookies(res);
                    res.sendRedirect(ADMIN_AUTH_LOGIN);
                }

            } else {
                // API 요청이라면 그냥 401
                entryPoint.commence(req, res,
                    new InsufficientAuthenticationException("Token expired", ex));
            }

        } catch (JwtException | IllegalArgumentException | BadCredentialsException ex) {
            SecurityContextHolder.clearContext();
            if (isAdminPage) {
                // 토큰 형태 오류 등도 관리자용 로그인 리다이렉트
                cookieUtil.clearTokenCookies(res);
                res.sendRedirect(ADMIN_AUTH_LOGIN);
            } else {
                cookieUtil.clearTokenCookies(res);
                entryPoint.commence(req, res,
                    new InsufficientAuthenticationException("Invalid token", ex));
            }
        }
    }

    private String resolveToken(HttpServletRequest req) {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String t = header.substring(7).trim();
            return t.isEmpty() ? null : t;
        }
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("ACCESS_TOKEN".equals(c.getName())
                    && c.getValue() != null
                    && !c.getValue().trim().isEmpty()) {
                    return c.getValue().trim();
                }
            }
        }
        return null;
    }

    private String resolveRefreshToken(HttpServletRequest req) {
        if (req.getCookies() == null) {
            return null;
        }
        for (Cookie c : req.getCookies()) {
            if ("REFRESH_TOKEN".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private void validateAndAuthenticate(String token) {
        // 토큰 검증
        Jwts.parserBuilder()
            .setSigningKey(tokenProvider.getKey())
            .build()
            .parseClaimsJws(token);

        // 사용자 로딩
        String role = tokenProvider.getClaims(token).get("role").toString();
        UserDetails user = loadUserByRole(role, token);

        // 인증정보 세팅
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private UserDetails loadUserByRole(String role, String token) {
        try {
            AdminRole adminRole = AdminRole.of(role);
            String username = tokenProvider.getSubject(token);
            return adminDetailsService.loadUserByUsername(username);
        } catch (IllegalArgumentException e) {
            Long memberId = Long.valueOf(tokenProvider.getSubject(token));
            return memberDetailsService.loadUserByUsername(memberId.toString());
        }
    }

    private String issueNewAccessToken(UserDetails user, String role) {
        if (user instanceof AdminUserDetails) {
            Admin admin = ((AdminUserDetails) user).getAdmin();
            return tokenProvider.createAccessTokenForAdmin(admin);
        } else {
            Member member = ((MemberUserDetails) user).getMember();
            return tokenProvider.createAccessToken(member);
        }
    }

    private String issueNewRefreshToken(UserDetails user, String role) {
        if (user instanceof AdminUserDetails) {
            Admin admin = ((AdminUserDetails) user).getAdmin();
            return tokenProvider.createRefreshTokenForAdmin(admin, false);
        } else {
            Member member = ((MemberUserDetails) user).getMember();
            return tokenProvider.createRefreshToken(member, false);
        }
    }
}
