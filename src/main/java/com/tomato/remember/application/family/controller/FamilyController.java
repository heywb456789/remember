package com.tomato.remember.application.family.controller;

import com.tomato.remember.application.family.dto.FamilyMemberResponse;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.family.service.FamilyService;
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
import java.util.stream.Collectors;

/**
 * 가족 관리 SSR 컨트롤러
 * - 웹 브라우저용 Thymeleaf 페이지 렌더링
 * - 서버에서 모든 필요한 데이터를 준비해서 전달
 */
@Slf4j
@Controller
@RequestMapping("/mobile/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;
    private final MemorialService memorialService;

    /**
     * 가족 목록 페이지 (메인)
     * GET /mobile/family
     *
     * 전체 메모리얼 목록 + 전체 가족 구성원을 서버에서 준비해서 전달
     * JavaScript에서 추가 API 호출 없이 바로 사용 가능
     */
    @GetMapping
    public String familyListPage(
            @AuthenticationPrincipal MemberUserDetails currentUser,
            Model model) {

        log.info("가족 목록 페이지 요청 - 사용자: {}", currentUser.getMember().getId());

        try {
            Member member = currentUser.getMember();

            // 1. 접근 가능한 모든 메모리얼 조회
            List<Memorial> accessibleMemorials = getAccessibleMemorials(member);

            // 2. 전체 가족 구성원 조회 (모든 메모리얼의)
            List<FamilyMemberResponse> allFamilyMembers = familyService.getAllFamilyMembersForSSR(member);

            // 3. 통계 정보 계산
            int totalMemorials = accessibleMemorials.size();
            int totalMembers = allFamilyMembers.size();
            int activeMembers = (int) allFamilyMembers.stream()
                    .filter(FamilyMemberResponse::isActive)
                    .count();

            // 4. 모델에 데이터 추가
            model.addAttribute("currentUser", currentUser.getMember());
            model.addAttribute("memorials", accessibleMemorials);
            model.addAttribute("familyMembers", allFamilyMembers);

            // 통계 정보
            model.addAttribute("totalMemorials", totalMemorials);
            model.addAttribute("totalMembers", totalMembers);
            model.addAttribute("activeMembers", activeMembers);

            // 첫 번째 메모리얼 선택 (드롭다운 초기값용)
            model.addAttribute("selectedMemorial", accessibleMemorials.isEmpty() ? null : accessibleMemorials.get(0));

            log.info("가족 목록 데이터 준비 완료 - 메모리얼: {}, 가족 구성원: {}",
                    totalMemorials, totalMembers);

            return "mobile/family/family-list";

        } catch (Exception e) {
            log.error("가족 관리 페이지 로드 실패 - 사용자: {}", currentUser.getMember().getId(), e);
            model.addAttribute("error", "페이지를 불러올 수 없습니다. 잠시 후 다시 시도해주세요.");
            return "mobile/error/500";
        }
    }

    /**
     * 초대 응답 페이지 (초대 링크 클릭 시)
     * GET /mobile/family/invite/{inviteToken}
     */
    @GetMapping("/invite/{inviteToken}")
    public String inviteResponsePage(
            @PathVariable String inviteToken,
            @AuthenticationPrincipal MemberUserDetails currentUser,
            Model model) {

        log.info("초대 응답 페이지 요청 - 토큰: {}, 사용자: {}",
                inviteToken.substring(0, 8) + "...", currentUser != null ? currentUser.getMember().getId() : "guest");

        try {
            // 초대 정보 조회
            var inviteInfo = familyService.getInviteInfo(inviteToken);

            if (inviteInfo == null) {
                log.warn("유효하지 않은 초대 토큰: {}", inviteToken.substring(0, 8) + "...");
                model.addAttribute("error", "유효하지 않거나 만료된 초대 링크입니다.");
                return "mobile/family/invite-error";
            }

            // 이미 로그인된 사용자인 경우 초대 자동 처리
            if (currentUser != null) {
                try {
                    familyService.acceptInvite(inviteToken, currentUser.getMember());

                    model.addAttribute("success", "가족 구성원으로 등록되었습니다.");
                    model.addAttribute("memorial", inviteInfo.getMemorial());
                    model.addAttribute("relationship", inviteInfo.getRelationship().getDisplayName());

                    log.info("초대 자동 수락 완료 - 사용자: {}, 메모리얼: {}",
                            currentUser.getMember().getId(), inviteInfo.getMemorial().getId());

                    return "mobile/family/invite-success";

                } catch (Exception e) {
                    log.error("초대 자동 수락 실패 - 토큰: {}", inviteToken.substring(0, 8) + "...", e);
                    model.addAttribute("error", e.getMessage());
                    return "mobile/family/invite-error";
                }
            }

            // 비로그인 사용자는 가입/로그인 페이지로
            model.addAttribute("inviteToken", inviteToken);
            model.addAttribute("inviteInfo", inviteInfo);
            model.addAttribute("memorial", inviteInfo.getMemorial());
            model.addAttribute("inviter", inviteInfo.getInviter());
            model.addAttribute("relationship", inviteInfo.getRelationship().getDisplayName());

            return "mobile/family/invite-signup";

        } catch (Exception e) {
            log.error("초대 응답 페이지 로드 실패 - 토큰: {}", inviteToken.substring(0, 8) + "...", e);
            model.addAttribute("error", "초대 처리 중 오류가 발생했습니다.");
            return "mobile/family/invite-error";
        }
    }

    /**
     * 내가 받은 초대 목록 페이지
     * GET /mobile/family/invitations
     */
    @GetMapping("/invitations")
    public String myInvitationsPage(
            @AuthenticationPrincipal MemberUserDetails currentUser,
            Model model) {

        log.info("내 초대 목록 페이지 요청 - 사용자: {}", currentUser.getMember().getId());

        try {
            Member member = currentUser.getMember();

            // 받은 초대 목록 조회
            var receivedInvitations = familyService.getReceivedInvitationsForSSR(member);

            // 보낸 초대 목록 조회 (소유자인 경우)
            var sentInvitations = familyService.getSentInvitationsForSSR(member);

            // 통계 정보
            int pendingCount = (int) receivedInvitations.stream()
                    .filter(FamilyMemberResponse::isPending)
                    .count();

            model.addAttribute("currentUser", member);
            model.addAttribute("receivedInvitations", receivedInvitations);
            model.addAttribute("sentInvitations", sentInvitations);
            model.addAttribute("pendingCount", pendingCount);

            log.info("초대 목록 데이터 준비 완료 - 받은 초대: {}, 보낸 초대: {}",
                    receivedInvitations.size(), sentInvitations.size());

            return "mobile/family/my-invitations";

        } catch (Exception e) {
            log.error("초대 목록 페이지 로드 실패 - 사용자: {}", currentUser.getMember().getId(), e);
            model.addAttribute("error", "페이지를 불러올 수 없습니다.");
            return "mobile/error/500";
        }
    }

    /**
     * 가족 구성원 상세 페이지
     * GET /mobile/family/member/{memberId}
     */
    @GetMapping("/member/{memberId}")
    public String memberDetailPage(
            @PathVariable Long memberId,
            @AuthenticationPrincipal MemberUserDetails currentUser,
            Model model) {

        log.info("가족 구성원 상세 페이지 요청 - 구성원: {}, 사용자: {}",
                memberId, currentUser.getMember().getId());

        try {
            Member member = currentUser.getMember();

            // 가족 구성원 조회 및 권한 확인
            FamilyMemberResponse familyMember = familyService.getFamilyMemberForSSR(memberId, member);

            // 메모리얼 소유자 여부 확인
            boolean isOwner = familyMember.getMemorial().getId().equals(
                    familyMember.getMemorial().getId()); // TODO: Memorial 엔티티에서 소유자 정보 가져오기

            model.addAttribute("currentUser", member);
            model.addAttribute("familyMember", familyMember);
            model.addAttribute("memorial", familyMember.getMemorial());
            model.addAttribute("isOwner", isOwner);

            log.info("가족 구성원 상세 정보 준비 완료 - 구성원: {}", memberId);

            return "mobile/family/member-detail";

        } catch (IllegalArgumentException e) {
            log.warn("가족 구성원 접근 권한 없음 - 구성원: {}, 사용자: {}, 오류: {}",
                    memberId, currentUser.getMember().getId(), e.getMessage());
            return "redirect:/mobile/family?error=access_denied";
        } catch (Exception e) {
            log.error("가족 구성원 상세 페이지 로드 실패 - 구성원: {}", memberId, e);
            model.addAttribute("error", "페이지를 불러올 수 없습니다.");
            return "mobile/error/500";
        }
    }

    // ===== 내부 헬퍼 메서드 =====

    /**
     * 사용자가 접근 가능한 모든 메모리얼 조회
     * (소유한 메모리얼 + 가족 구성원으로 등록된 메모리얼)
     */
    private List<Memorial> getAccessibleMemorials(Member member) {
        log.debug("접근 가능한 메모리얼 조회 - 사용자: {}", member.getId());

        // 소유한 메모리얼
        List<Memorial> ownedMemorials = memorialService.findByOwner(member);

        // 가족 구성원으로 접근 가능한 메모리얼
        List<FamilyMember> accessibleFamilyMembers = familyService.getAccessibleFamilyMemberships(member);

        // 중복 제거하여 통합
        List<Memorial> allMemorials = new ArrayList<>(ownedMemorials);

        for (FamilyMember familyMember : accessibleFamilyMembers) {
            Memorial memorial = familyMember.getMemorial();
            if (!allMemorials.contains(memorial)) {
                allMemorials.add(memorial);
            }
        }

        log.debug("접근 가능한 메모리얼 조회 완료 - 사용자: {}, 개수: {}",
                member.getId(), allMemorials.size());

        return allMemorials;
    }
}