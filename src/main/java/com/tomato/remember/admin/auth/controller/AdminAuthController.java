package com.tomato.remember.admin.auth.controller;

import com.tomato.remember.admin.security.AdminUserDetails;
import com.tomato.remember.admin.user.entity.Admin;
import com.tomato.remember.common.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/admin/view/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final CookieUtil cookieUtil;

    /**
     * 관리자 로그인 페이지
     */
    @GetMapping("/login")
    public String loginPage() {

        // 이미 로그인되어 있으면 대시보드로 리다이렉트
        if (isAuthenticated()) {
            return "redirect:/admin/view/dashboard";
        }

        return "admin/login/login";
    }

    /**
     * 관리자 회원가입 페이지 (필요시)
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        // 이미 로그인되어 있으면 대시보드로 리다이렉트
        if (isAuthenticated()) {
            return "redirect:/admin/view/dashboard";
        }

        return "admin/login/register";
    }

    @GetMapping("/logout")
    public void logout(HttpServletRequest req, HttpServletResponse response) throws IOException {
//        cookieUtil.clearTokenCookies(response);
        // 로그인 페이지로 리디렉션 (302)
        response.sendRedirect("/admin/auth/login");
    }

    /**
     * 현재 인증된 관리자 정보 가져오기
     */
    private Admin getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdminUserDetails) {
            return ((AdminUserDetails) authentication.getPrincipal()).getAdmin();
        }
        throw new IllegalStateException("인증된 관리자 정보를 찾을 수 없습니다.");
    }

    /**
     * 로그인 상태 확인
     */
    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
               authentication.isAuthenticated() &&
               authentication.getPrincipal() instanceof AdminUserDetails;
    }
}
