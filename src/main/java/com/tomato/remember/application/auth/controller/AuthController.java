package com.tomato.remember.application.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.auth.controller
 * @fileName : AuthController
 * @date : 2025-05-30
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Slf4j
@Controller
@RequestMapping("/mobile")
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("/login")
    public String loginPage(Model model, HttpServletRequest request) {
        // Authentication 정보 확인
        var auth = SecurityContextHolder.getContext().getAuthentication();

        // 기본 페이지 정보 설정
        model.addAttribute("pageTitle", "토마토 One-ID");
        model.addAttribute("appName", "토마토 One-ID");

        // 토큰 갱신 정보가 있다면 모델에 추가 (MobileJwtFilter에서 설정)
        String newAccessToken = (String) request.getAttribute("newAccessToken");
        String newRefreshToken = (String) request.getAttribute("newRefreshToken");

        if (newAccessToken != null && newRefreshToken != null) {
            model.addAttribute("newAccessToken", newAccessToken);
            model.addAttribute("newRefreshToken", newRefreshToken);
            log.debug("Token refresh info added to login page model");
        }

        // API 엔드포인트 정보 제공
        model.addAttribute("loginApiUrl", "/api/auth/login");
        model.addAttribute("registerUrl", "/mobile/register");
        model.addAttribute("mainUrl", "/mobile/home");

        return "mobile/login/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model, HttpServletRequest request) {

        // 기본 페이지 정보 설정
        model.addAttribute("pageTitle", "OneID 회원가입 - 토마토리멤버");
        model.addAttribute("appName", "토마토 OneID");

        // 페이지 네비게이션 URL들
        model.addAttribute("loginUrl", "/mobile/login");
        model.addAttribute("nextUrl", "/mobile/register/phone");

        // 디버그 모드 확인 (개발 환경에서만)
        if ("true".equals(request.getParameter("debug"))) {
            model.addAttribute("debugMode", true);
        }

        return "mobile/login/register";
    }

    @GetMapping("/register/phone")
    public String phoneVerifyPage(Model model) {
        model.addAttribute("pageTitle", "핸드폰 인증 - 토마토리멤버");
        model.addAttribute("smsVerifyApiUrl", "/api/auth/sms/send");
        model.addAttribute("smsConfirmApiUrl", "/api/auth/sms/verify");
        model.addAttribute("prevUrl", "/mobile/register");
        model.addAttribute("nextUrl", "/mobile/register/password");

        return "mobile/login/phone-verify";
    }

    @GetMapping("/register/password")
    public String registerPasswordPage(Model model) {
        log.info("📱 비밀번호 설정 페이지 요청");

        try {
            // 페이지 기본 정보 설정
            model.addAttribute("pageTitle", "비밀번호 설정 - 토마토리멤버");

            // API 엔드포인트 설정
            model.addAttribute("registerApiUrl", "/api/auth/register");

            // 네비게이션 URL 설정
            model.addAttribute("prevUrl", "/mobile/register/verify");
            model.addAttribute("homeUrl", "/mobile/home");

            // 메타 정보
            model.addAttribute("activeMenu", "register");
            model.addAttribute("activeSubMenu", "password");

            log.info("✅ 비밀번호 설정 페이지 렌더링 준비 완료");

            return "mobile/login/register-password";

        } catch (Exception e) {
            log.error("❌ 비밀번호 설정 페이지 오류", e);

            // 에러 발생 시 이전 단계로 리다이렉트
            return "redirect:/mobile/register/verify";
        }
    }

    /**
     * 회원가입 완료 페이지 표시
     */
    @GetMapping("/register/complete")
    public String registerCompletePage(Model model) {
        log.info("🎉 회원가입 완료 페이지 요청");

        try {
            // 페이지 기본 정보 설정
            model.addAttribute("pageTitle", "가입완료 - 토마토리멤버");

            // 네비게이션 URL 설정
            model.addAttribute("loginUrl", "/mobile/login");
            model.addAttribute("homeUrl", "/mobile/home");

            // 메타 정보
            model.addAttribute("activeMenu", "register");
            model.addAttribute("activeSubMenu", "complete");

            log.info("✅ 회원가입 완료 페이지 렌더링 준비 완료");

            return "mobile/login/register-complete";

        } catch (Exception e) {
            log.error("❌ 회원가입 완료 페이지 오류", e);

            // 에러 발생 시 로그인 페이지로 리다이렉트
            return "redirect:/mobile/login";
        }
    }

    /**
     * 마이페이지 관련 뷰 페이지들
     */
    @GetMapping("/mypage")
    public String mypagePage(Model model) {
        log.info("🚀 === MyPage Access ===");

        model.addAttribute("pageTitle", "마이페이지 - 토마토리멤버");
        model.addAttribute("profileApiUrl", "/api/auth/me");
        model.addAttribute("updateProfileApiUrl", "/api/auth/profile");
        model.addAttribute("paymentApiUrl", "/api/payment/history");

        return "mobile/mypage/profile";
    }

}
