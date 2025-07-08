package com.tomato.naraclub.admin.auth.controller;

import com.tomato.naraclub.common.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final CookieUtil cookieUtil;

    @GetMapping("/login")
    public String loginPage(){
        return "admin/login/login";
    }

    @GetMapping("/register")
    public String registerPage(){
        return "admin/login/register";
    }

    @GetMapping("/logout")
    public void logout(HttpServletRequest req, HttpServletResponse response) throws IOException {
        cookieUtil.clearTokenCookies(response);
        // 로그인 페이지로 리디렉션 (302)
        response.sendRedirect("/admin/auth/login");
    }
}
