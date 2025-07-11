package com.tomato.remember.application.memorial.controller;

import com.tomato.remember.application.memorial.service.MemorialService;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 메모리얼 뷰 컨트롤러 (Thymeleaf 템플릿 전용)
 */
@Slf4j
@Controller
@RequestMapping("/mobile/memorial")
@RequiredArgsConstructor
public class MemorialController {

    private final MemorialService memorialService;


    @GetMapping("/create")
    public String create(Model model) {
        log.info("Accessing memorial create step1 page");
        return "mobile/memorial/create";
    }
}