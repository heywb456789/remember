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
@RequestMapping("/memorial")
@RequiredArgsConstructor
public class MemorialController {

    private final MemorialService memorialService;

    /**
     * 메모리얼 생성 1단계 - 기본 정보 입력
     */
    @GetMapping("/create/step1")
    public String createStep1(Model model) {
        log.info("Accessing memorial create step1 page");
        return "mobile/memorial/create-step1";
    }

    /**
     * 메모리얼 생성 2단계 - 고인 정보 입력
     */
    @GetMapping("/create/step2")
    public String createStep2(@RequestParam("memorialId") Long memorialId,
                              Model model,
                              @AuthenticationPrincipal MemberUserDetails userDetails) {
        log.info("Accessing memorial create step2 page for memorialId: {}", memorialId);

        Member member = userDetails.getMember();

        // 메모리얼 존재 및 권한 검증
        try {
            memorialService.validateMemorialAccess(memorialId, member);
            model.addAttribute("memorialId", memorialId);
            return "mobile/memorial/create-step2";
        } catch (Exception e) {
            log.error("Error accessing step2 for memorial: {}", memorialId, e);
            return "redirect:/memorial/create/step1";
        }
    }

    /**
     * 메모리얼 생성 3단계 - 미디어 업로드
     */
    @GetMapping("/create/step3")
    public String createStep3(@RequestParam("memorialId") Long memorialId,
                              Model model,
                              @AuthenticationPrincipal MemberUserDetails userDetails) {
        log.info("Accessing memorial create step3 page for memorialId: {}", memorialId);

        Member member = userDetails.getMember();

        // 메모리얼 존재 및 권한 검증
        try {
            memorialService.validateMemorialAccess(memorialId, member);
            model.addAttribute("memorialId", memorialId);
            return "mobile/memorial/create-step3";
        } catch (Exception e) {
            log.error("Error accessing step3 for memorial: {}", memorialId, e);
            return "redirect:/memorial/create/step1";
        }
    }

    /**
     * 메모리얼 생성 완료 페이지
     */
    @GetMapping("/create/complete")
    public String createComplete(@RequestParam("memorialId") Long memorialId,
                                 Model model,
                                 @AuthenticationPrincipal MemberUserDetails userDetails) {
        log.info("Accessing memorial create complete page for memorialId: {}", memorialId);

        Member member = userDetails.getMember();

        try {
            // 완료된 메모리얼 정보 조회
            var memorial = memorialService.getMemorialById(memorialId, member);
            model.addAttribute("memorial", memorial);
            return "mobile/memorial/create-complete";
        } catch (Exception e) {
            log.error("Error accessing complete page for memorial: {}", memorialId, e);
            return "redirect:/memorial/my";
        }
    }

    /**
     * 내 메모리얼 목록 페이지
     */
    @GetMapping("/my")
    public String myMemorials(Model model, @AuthenticationPrincipal MemberUserDetails userDetails) {
        log.info("Accessing my memorials page");

        Member member = userDetails.getMember();
        var memorials = memorialService.getActiveMemorialListByMember(member);

        model.addAttribute("memorials", memorials);
        return "mobile/memorial/my-memorials";
    }

    /**
     * 메모리얼 상세 페이지
     */
    @GetMapping("/{memorialId}")
    public String memorialDetail(@PathVariable Long memorialId,
                                 Model model,
                                 @AuthenticationPrincipal MemberUserDetails userDetails) {
        log.info("Accessing memorial detail page for memorialId: {}", memorialId);

        Member member = userDetails.getMember();

        try {
            var memorial = memorialService.getMemorialById(memorialId, member);
            // 방문 기록
            memorialService.recordMemorialVisit(memorialId, member);

            model.addAttribute("memorial", memorial);
            return "mobile/memorial/detail";
        } catch (Exception e) {
            log.error("Error accessing memorial detail: {}", memorialId, e);
            return "redirect:/memorial/my";
        }
    }
}