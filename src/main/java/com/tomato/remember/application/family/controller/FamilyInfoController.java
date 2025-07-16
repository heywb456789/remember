package com.tomato.remember.application.family.controller;

import com.tomato.remember.application.family.dto.FamilyInfoResponseDTO;
import com.tomato.remember.application.family.service.FamilyService;
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

import java.util.Map;

/**
 * 가족 구성원용 고인 상세 정보 SSR 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/mobile/memorial")
@RequiredArgsConstructor
public class FamilyInfoController {

    private final FamilyService familyService;

    /**
     * 가족 구성원용 고인 상세 정보 입력 페이지
     * GET /mobile/memorial/family-info/{memorialId}
     */
    @GetMapping("/family-info/{memorialId}")
    public String familyInfoPage(
            @PathVariable Long memorialId,
            @AuthenticationPrincipal MemberUserDetails userDetails,
            Model model) {

        log.info("가족 구성원 고인 상세 정보 입력 페이지 접근 - 메모리얼: {}, 사용자: {}", 
                memorialId, userDetails.getMember().getId());

        try {
            Member member = userDetails.getMember();

            // 1. 접근 권한 확인
            Map<String, Object> accessCheck = familyService.checkFamilyInfoAccess(memorialId, member);
            
            if (!(Boolean) accessCheck.get("canAccess")) {
                String errorMessage = (String) accessCheck.get("message");
                log.warn("가족 구성원 고인 상세 정보 페이지 접근 거부 - 메모리얼: {}, 사유: {}", 
                        memorialId, errorMessage);
                
                model.addAttribute("errorMessage", errorMessage);
                return "mobile/error/access-denied";
            }

            // 2. 이미 입력된 경우 조회 모드
            if ((Boolean) accessCheck.get("alreadySubmitted")) {
                FamilyInfoResponseDTO familyInfo = familyService.getFamilyInfo(memorialId, member);
                model.addAttribute("familyInfo", familyInfo);
                model.addAttribute("isViewMode", true);
                
                log.info("가족 구성원 고인 상세 정보 조회 모드 - 메모리얼: {}, 완성도: {}%", 
                        memorialId, familyInfo.getCompletionPercent());
                
                return "mobile/memorial/family-info";
            }

            // 3. 새로 입력하는 경우
            FamilyInfoResponseDTO familyInfo = familyService.getFamilyInfo(memorialId, member);
            model.addAttribute("familyInfo", familyInfo);
            model.addAttribute("isViewMode", false);

            log.info("가족 구성원 고인 상세 정보 입력 모드 - 메모리얼: {}, 완성도: {}%", 
                    memorialId, familyInfo.getCompletionPercent());

            return "mobile/memorial/family-info";

        } catch (IllegalArgumentException e) {
            log.warn("가족 구성원 고인 상세 정보 페이지 접근 실패 - 잘못된 요청: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "mobile/error/bad-request";
        } catch (SecurityException e) {
            log.warn("가족 구성원 고인 상세 정보 페이지 접근 실패 - 권한 없음: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "mobile/error/access-denied";
        } catch (Exception e) {
            log.error("가족 구성원 고인 상세 정보 페이지 접근 실패 - 메모리얼: {}", memorialId, e);
            model.addAttribute("errorMessage", "페이지를 불러오는 중 오류가 발생했습니다.");
            return "mobile/error/internal-error";
        }
    }
}