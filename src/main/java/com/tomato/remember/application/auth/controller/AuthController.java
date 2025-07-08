package com.tomato.remember.application.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
    public String loginPage(
            Model model,
            HttpServletRequest request) {

        // 기본 페이지 정보 설정
        model.addAttribute("pageTitle", "토마토 One-ID");
        model.addAttribute("appName", "토마토 One-ID");

        return "/mobile/login/login";
    }
}
