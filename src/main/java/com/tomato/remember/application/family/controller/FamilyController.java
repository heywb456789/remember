package com.tomato.remember.application.family.controller;

import com.tomato.remember.application.family.dto.FamilyMemberResponse;
import com.tomato.remember.application.family.dto.FamilyPageData;
import com.tomato.remember.application.family.service.FamilyService;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.service.MemorialService;
import com.tomato.remember.application.security.MemberUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 가족 관리 SSR 컨트롤러 (간단 버전)
 */
@Slf4j
@Controller
@RequestMapping("/mobile/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;
    private final MemorialService memorialService;

    /**
     * 가족 목록 페이지
     * GET /mobile/family
     */
    @GetMapping
    public String familyListPage(
            @AuthenticationPrincipal MemberUserDetails currentUser,
            @RequestParam(required = false) Long memorialId,
            Model model) {

        log.info("가족 목록 페이지 요청 - 사용자: {}, 메모리얼ID: {}",
                currentUser.getMember().getId(), memorialId);

        try {
            Member member = currentUser.getMember();

            // 서비스에서 모든 비즈니스 로직 처리
            FamilyPageData pageData = familyService.getFamilyPageData(member, memorialId);

            // 모델에 데이터만 설정
            model.addAttribute("currentUser", member);
            model.addAttribute("memorials", pageData.getMemorials());
            model.addAttribute("selectedMemorial", pageData.getSelectedMemorial());
            model.addAttribute("familyMembers", pageData.getFamilyMembers());
            model.addAttribute("totalMemorials", pageData.getTotalMemorials());
            model.addAttribute("totalMembers", pageData.getTotalMembers());

            return "mobile/family/family-list";

        } catch (Exception e) {
            log.error("가족 관리 페이지 로드 실패 - 사용자: {}", currentUser.getMember().getId(), e);
            model.addAttribute("error", "페이지를 불러올 수 없습니다.");
            return "mobile/error/500";
        }
    }

    /**
     * 내가 소유한 모든 메모리얼의 가족 구성원 조회 (소유자 포함)
     */
    private List<FamilyMemberResponse> getAllFamilyMembersWithOwner(List<Memorial> memorials, Member owner) {
        List<FamilyMemberResponse> allMembers = new ArrayList<>();

        for (Memorial memorial : memorials) {
            // 1. 소유자 정보 먼저 추가
            FamilyMemberResponse ownerInfo = createOwnerResponse(memorial, owner);
            allMembers.add(ownerInfo);

            // 2. 가족 구성원들 추가
            List<FamilyMemberResponse> familyMembers = familyService.getFamilyMembersByMemorial(memorial.getId());
            allMembers.addAll(familyMembers);
        }

        return allMembers;
    }

    /**
     * 소유자 정보를 FamilyMemberResponse로 생성
     */
    private FamilyMemberResponse createOwnerResponse(Memorial memorial, Member owner) {
        return FamilyMemberResponse.builder()
                .id(-1L) // 소유자는 음수 ID 사용
                .memorial(FamilyMemberResponse.MemorialInfo.builder()
                        .id(memorial.getId())
                        .name(memorial.getName())
                        .nickname(memorial.getNickname())
                        .mainProfileImageUrl(memorial.getMainProfileImageUrl())
                        .isActive(memorial.isActive())
                        .build())
                .member(FamilyMemberResponse.MemberInfo.builder()
                        .id(owner.getId())
                        .name(owner.getName())
                        .email(owner.getEmail())
                        .phoneNumber(owner.getPhoneNumber())
                        .profileImageUrl(owner.getProfileImageUrl())
                        .isActive(owner.isActive())
                        .build())
                .relationship(Relationship.SELF)
                .relationshipDisplayName("본인")
                .inviteStatus(com.tomato.remember.application.family.code.InviteStatus.ACCEPTED)
                .inviteStatusDisplayName("활성")
                .permissions(FamilyMemberResponse.PermissionInfo.builder()
                        .memorialAccess(true)
                        .videoCallAccess(true)
                        .canModify(true)
                        .build())
                .build();
    }
}