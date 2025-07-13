package com.tomato.remember.application.family.controller;

import com.tomato.remember.application.family.dto.FamilyMemberResponse;
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
    public String familyListPage(@AuthenticationPrincipal MemberUserDetails currentUser, Model model) {
        log.info("가족 목록 페이지 요청 - 사용자: {}", currentUser.getMember().getId());

        try {
            Member member = currentUser.getMember();

            // 1. 내가 소유한 메모리얼만 조회
            List<Memorial> myMemorials = memorialService.findByOwner(member);
            log.info("소유한 메모리얼 수: {} (사용자: {})", myMemorials.size(), member.getId());

            // 2. 전체 가족 구성원 조회 (내가 소유한 모든 메모리얼 기준)
            List<FamilyMemberResponse> allFamilyMembers = getAllFamilyMembersWithOwner(myMemorials, member);

            // 3. 기본 선택은 "전체" (selectedMemorial = null)
            Memorial selectedMemorial = null;

            // 4. 모델에 데이터 추가
            model.addAttribute("currentUser", member);
            model.addAttribute("memorials", myMemorials);
            model.addAttribute("familyMembers", allFamilyMembers); // 전체 가족 구성원
            model.addAttribute("selectedMemorial", selectedMemorial); // null = 전체 선택
            model.addAttribute("totalMemorials", myMemorials.size());
            model.addAttribute("totalMembers", allFamilyMembers.size());

            log.info("가족 목록 데이터 준비 완료 - 메모리얼: {}, 전체 구성원: {}", myMemorials.size(), allFamilyMembers.size());

            return "mobile/family/family-list";

        } catch (Exception e) {
            log.error("가족 관리 페이지 로드 실패 - 사용자: {}", currentUser.getMember().getId(), e);
            model.addAttribute("error", "페이지를 불러올 수 없습니다.");
            return "mobile/error/500";
        }
    }

    /**
     * 초대 응답 페이지
     * GET /mobile/family/invite/{inviteToken}
     */
    @GetMapping("/invite/{inviteToken}")
    public String inviteResponsePage(@PathVariable String inviteToken,
                                     @AuthenticationPrincipal MemberUserDetails currentUser,
                                     Model model) {
        log.info("초대 응답 페이지 요청 - 토큰: {}", inviteToken.substring(0, 8) + "...");

        try {
            // 초대 정보 조회
            var inviteInfo = familyService.getInviteInfo(inviteToken);

            if (inviteInfo == null) {
                model.addAttribute("error", "유효하지 않거나 만료된 초대 링크입니다.");
                return "mobile/family/invite-error";
            }

            // 로그인된 사용자면 자동 처리
            if (currentUser != null) {
                try {
                    familyService.acceptInvite(inviteToken, currentUser.getMember());
                    model.addAttribute("success", "가족 구성원으로 등록되었습니다.");
                    model.addAttribute("memorial", inviteInfo.getMemorial());
                    return "mobile/family/invite-success";
                } catch (Exception e) {
                    model.addAttribute("error", e.getMessage());
                    return "mobile/family/invite-error";
                }
            }

            // 비로그인 사용자는 가입 페이지로
            model.addAttribute("inviteToken", inviteToken);
            model.addAttribute("inviteInfo", inviteInfo);
            model.addAttribute("memorial", inviteInfo.getMemorial());
            model.addAttribute("inviter", inviteInfo.getInviter());

            return "mobile/family/invite-signup";

        } catch (Exception e) {
            log.error("초대 응답 페이지 로드 실패 - 토큰: {}", inviteToken.substring(0, 8) + "...", e);
            model.addAttribute("error", "초대 처리 중 오류가 발생했습니다.");
            return "mobile/family/invite-error";
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