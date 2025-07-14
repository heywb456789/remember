package com.tomato.remember.application.member.controller;

import com.tomato.remember.application.member.dto.ProfileSettingsDTO;
import com.tomato.remember.application.member.dto.ProfileStats;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.member.service.MemberProfileService;
import com.tomato.remember.application.security.MemberUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.remember.application.member.controller
 * @fileName : MemberController
 * @date : 2025-07-14
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Slf4j
@Controller
@RequestMapping("/mobile/account")
@RequiredArgsConstructor
public class MemberController {

    private final MemberProfileService memberProfileService;

    /**
     * 프로필 설정 페이지
     *
     * @param redirectFromVideoCall 영상통화에서 리다이렉트 여부
     * @param model                 Thymeleaf 모델
     * @return 프로필 설정 페이지 뷰
     */
    @GetMapping("/profile")
    public String profileSettings(
        @RequestParam(name = "redirect", required = false) String redirectFromVideoCall,
        @AuthenticationPrincipal MemberUserDetails user,
        Model model) {

        log.info("프로필 설정 페이지 접근 - 영상통화 리다이렉트: {}", redirectFromVideoCall);

        try {
            // 현재 로그인 사용자 조회
            Member currentUser = user == null ? null : user.getMember();
            if (currentUser == null) {
                log.warn("인증되지 않은 사용자의 프로필 설정 페이지 접근 시도");
                return "redirect:/mobile/login";
            }

            // 프로필 정보 조회 (프로필 이미지 포함)
            ProfileSettingsDTO profileData = memberProfileService.getProfileSettingsData(currentUser.getId());

            model.addAttribute("currentUser", currentUser); // 기본 정보용
            model.addAttribute("profileData", profileData); // 이미지 관련 정보
            model.addAttribute("redirectFromVideoCall", "videocall".equals(redirectFromVideoCall));

            return "mobile/member/profile-settings";

        } catch (Exception e) {
            log.error("프로필 설정 페이지 로드 중 오류 발생", e);
            model.addAttribute("errorMessage", "프로필 정보를 불러오는 중 오류가 발생했습니다.");
            return "mobile/error/500";
        }
    }

}
