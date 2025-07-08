package com.tomato.naraclub.admin.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Stream;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

//@Component
public class AdminAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // 쿠키 삭제
        Stream.of("ACCESS_TOKEN", "REFRESH_TOKEN").forEach(name -> {
            Cookie cookie = new Cookie(name, "");
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        });
        // 리다이렉트
        response.sendRedirect("/admin/auth/login?expired");
    }
}