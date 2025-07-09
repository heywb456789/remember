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
        log.info("🚀 === Mobile Login Page Access ===");
        log.info("📍 Request URI: {}", request.getRequestURI());
        log.info("🌐 Request URL: {}", request.getRequestURL());
        log.info("🔗 Query String: {}", request.getQueryString());
        log.info("👤 Remote Address: {}", request.getRemoteAddr());
        log.info("🎫 User Principal: {}", request.getUserPrincipal());

        // Authentication 정보 확인
        var auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("🔐 Current Authentication: {}", auth);
        log.info("✅ Is Authenticated: {}", auth != null ? auth.isAuthenticated() : "null");

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

        return "/mobile/login/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model, HttpServletRequest request) {
        log.info("🚀 === Mobile Register Page Access ===");

        // 기본 페이지 정보 설정
        model.addAttribute("pageTitle", "회원가입 - 토마토 One-ID");
        model.addAttribute("appName", "토마토 One-ID");

        // API 엔드포인트 정보 제공
        model.addAttribute("registerApiUrl", "/api/auth/register");
        model.addAttribute("loginUrl", "/mobile/login");
        model.addAttribute("smsVerifyApiUrl", "/api/auth/sms/send");
        model.addAttribute("smsConfirmApiUrl", "/api/auth/sms/verify");

        return "/mobile/register/register";
    }


    /**
     * 메모리얼 관련 뷰 페이지들
     */
    @GetMapping("/memorial/create")
    public String createMemorialPage(Model model) {
        log.info("🚀 === Create Memorial Page Access ===");

        model.addAttribute("pageTitle", "메모리얼 등록 - 토마토리멤버");
        model.addAttribute("createMemorialApiUrl", "/api/memorial");
        model.addAttribute("uploadApiUrl", "/api/upload");
        model.addAttribute("backUrl", "/mobile/home");

        return "/mobile/memorial/create";
    }

    @GetMapping("/memorial/{memorialId}")
    public String memorialDetailPage(@PathVariable Long memorialId, Model model) {
        log.info("🚀 === Memorial Detail Page Access - ID: {} ===", memorialId);

        model.addAttribute("pageTitle", "메모리얼 상세 - 토마토리멤버");
        model.addAttribute("memorialId", memorialId);
        model.addAttribute("memorialDetailApiUrl", "/api/memorial/" + memorialId);
        model.addAttribute("videoCallUrl", "/mobile/video-call/" + memorialId);
        model.addAttribute("tributeUrl", "/mobile/memorial/" + memorialId + "/tribute");

        return "/mobile/memorial/detail";
    }

    /**
     * 영상통화 관련 뷰 페이지들
     */
    @GetMapping("/video-call/{memorialId}")
    public String videoCallPage(@PathVariable Long memorialId, Model model) {
        log.info("🚀 === Video Call Page Access - Memorial ID: {} ===", memorialId);

        model.addAttribute("pageTitle", "영상통화 - 토마토리멤버");
        model.addAttribute("memorialId", memorialId);
        model.addAttribute("videoCallApiUrl", "/api/video-call/" + memorialId + "/process");
        model.addAttribute("feedbackUrl", "/mobile/video-call/" + memorialId + "/feedback");
        model.addAttribute("backUrl", "/mobile/memorial/" + memorialId);

        return "/mobile/video-call/room";
    }

    @GetMapping("/video-call/{memorialId}/feedback")
    public String feedbackPage(@PathVariable Long memorialId, Model model) {
        log.info("🚀 === Feedback Page Access - Memorial ID: {} ===", memorialId);

        model.addAttribute("pageTitle", "피드백 - 토마토리멤버");
        model.addAttribute("memorialId", memorialId);
        model.addAttribute("feedbackApiUrl", "/api/video-call/" + memorialId + "/feedback");
        model.addAttribute("backUrl", "/mobile/memorial/" + memorialId);

        return "/mobile/video-call/feedback";
    }

    /**
     * 추모하기 관련 뷰 페이지들
     */
    @GetMapping("/memorial/{memorialId}/tribute")
    public String tributePage(@PathVariable Long memorialId, Model model) {
        log.info("🚀 === Tribute Page Access - Memorial ID: {} ===", memorialId);

        model.addAttribute("pageTitle", "추모하기 - 토마토리멤버");
        model.addAttribute("memorialId", memorialId);
        model.addAttribute("tributeApiUrl", "/api/tribute/" + memorialId);
        model.addAttribute("paymentApiUrl", "/api/payment");
        model.addAttribute("backUrl", "/mobile/memorial/" + memorialId);

        return "/mobile/tribute/main";
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

        return "/mobile/mypage/profile";
    }

}
