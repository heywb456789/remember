package com.tomato.remember.application.family.controller;

import com.tomato.remember.application.family.dto.FamilyAllDataResponse;
import com.tomato.remember.application.family.dto.FamilyInviteRequest;
import com.tomato.remember.application.family.dto.FamilyMemberResponse;
import com.tomato.remember.application.family.dto.PermissionUpdateRequest;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.family.repository.FamilyMemberRepository;
import com.tomato.remember.application.family.service.FamilyService;
import com.tomato.remember.application.member.code.Relationship;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.member.service.MemberService;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.repository.MemorialRepository;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.dto.ListDTO;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.exception.APIException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 가족 관리 REST API 컨트롤러
 * - 앱 전용 데이터 API
 * - 웹/앱 공통 액션 API (초대, 권한, 제거 등)
 */
@Slf4j
@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
public class FamilyRestController {

    private final FamilyService familyService;
    private final FamilyMemberRepository familyMemberRepository;
    private final MemorialRepository memorialRepository;

    // ===== 앱 전용 데이터 API =====

    /**
     * 앱 전용: 전체 가족 관리 데이터 조회
     * GET /api/family/all-data
     *
     * SSR과 동일한 데이터를 JSON으로 제공
     * - 접근 가능한 모든 메모리얼
     * - 전체 가족 구성원
     * - 통계 정보
     */
    @GetMapping("/all-data")
    public ResponseDTO<FamilyAllDataResponse> getAllFamilyData(
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("앱 전용 전체 가족 데이터 조회 API - 사용자: {}", currentUser.getMember().getId());

        try {
            Member member = currentUser.getMember();

            // 🔥 SSR과 동일한 로직으로 전체 데이터 조회 (소유자 포함)
            FamilyAllDataResponse allData = familyService.getAllFamilyDataForApp(member);

            // 🔥 소유자 정보 존재 여부 검증
            long ownerCount = allData.getFamilyMembers().stream()
                    .filter(fm -> fm.getRelationship() == Relationship.SELF)
                    .count();

            log.info("📊 앱 API 응답 데이터: 메모리얼={}, 전체구성원={}, 소유자정보={}, 활성구성원={}",
                    allData.getMemorialCount(),
                    allData.getFamilyMemberCount(),
                    ownerCount,
                    allData.getStatistics().getActiveMembers());

            if (ownerCount == 0) {
                log.warn("⚠️ 앱 API: 소유자 정보가 없습니다! 사용자: {}", member.getId());
            }

            return ResponseDTO.ok(allData);

        } catch (Exception e) {
            log.error("❌ 앱 전용 전체 가족 데이터 조회 실패 - 사용자: {}", currentUser.getMember().getId(), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 메모리얼의 가족 구성원 목록 조회 (페이징)
     * GET /api/family/memorial/{memorialId}/members
     *
     * 앱에서 메모리얼별 상세 조회 시 사용
     */
    @GetMapping("/memorial/{memorialId}/members")
    public ResponseDTO<ListDTO<FamilyMemberResponse>> getFamilyMembers(
            @PathVariable Long memorialId,
            @AuthenticationPrincipal MemberUserDetails currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("특정 메모리얼 가족 구성원 목록 조회 API - 메모리얼: {}, 사용자: {}",
                memorialId, currentUser.getMember().getId());

        try {
            Member member = currentUser.getMember();

            // 🔥 메모리얼 접근 권한 확인
            Memorial memorial = memorialRepository.findById(memorialId)
                    .orElseThrow(() -> new IllegalArgumentException("메모리얼을 찾을 수 없습니다."));

            if (!memorial.canBeViewedBy(member)) {
                throw new IllegalArgumentException("메모리얼에 접근할 권한이 없습니다.");
            }

            // 🔥 해당 메모리얼의 가족 구성원 조회 (페이징)
            Page<FamilyMember> familyMembersPage = familyMemberRepository
                    .findByMemorialOrderByCreatedAtDesc(memorial, pageable);

            // 🔥 DTO 변환
            List<FamilyMemberResponse> familyMemberResponses = familyMembersPage.getContent().stream()
                    .map(FamilyMemberResponse::from)
                    .collect(Collectors.toList());

            // 🔥 소유자 정보 추가 (해당 메모리얼의 소유자인 경우에만)
            if (memorial.getOwner().equals(member)) {
                FamilyMemberResponse ownerInfo = familyService.createOwnerAsFamilyMember(memorial, member);
                familyMemberResponses.add(0, ownerInfo); // 맨 앞에 추가
                log.info("✅ 소유자 정보 추가: 메모리얼={}, 소유자={}", memorialId, member.getId());
            }

            // 🔥 새로운 Page 객체 생성 (소유자 포함)
            PageImpl<FamilyMemberResponse> resultPage = new PageImpl<>(
                    familyMemberResponses,
                    pageable,
                    familyMembersPage.getTotalElements() + (memorial.getOwner().equals(member) ? 1 : 0)
            );

            ListDTO<FamilyMemberResponse> result = ListDTO.of(resultPage);

            log.info("특정 메모리얼 가족 구성원 조회 완료 - 메모리얼: {}, 총구성원: {} (소유자포함)",
                    memorialId, result.getPagination().getTotalElements());

            return ResponseDTO.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("메모리얼 가족 구성원 목록 조회 실패 - 권한 없음: {}", e.getMessage());
            throw new APIException(ResponseStatus.MEMORIAL_ACCESS_DENIED);
        } catch (Exception e) {
            log.error("메모리얼 가족 구성원 목록 조회 실패 - 메모리얼: {}", memorialId, e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 내가 받은 초대 목록 조회
     * GET /api/family/invitations/received
     */
    @GetMapping("/invitations/received")
    public ResponseDTO<List<FamilyMemberResponse>> getReceivedInvitations(
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("받은 초대 목록 조회 API - 사용자: {}", currentUser.getMember().getId());

        try {
            List<FamilyMemberResponse> result = familyService.getReceivedInvitationsForApp(currentUser.getMember());

            log.info("받은 초대 목록 조회 완료 - 사용자: {}, 초대 수: {}",
                    currentUser.getMember().getId(), result.size());

            return ResponseDTO.ok(result);

        } catch (Exception e) {
            log.error("받은 초대 목록 조회 실패 - 사용자: {}", currentUser.getMember().getId(), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 내가 보낸 초대 목록 조회
     * GET /api/family/invitations/sent
     */
    @GetMapping("/invitations/sent")
    public ResponseDTO<List<FamilyMemberResponse>> getSentInvitations(
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("보낸 초대 목록 조회 API - 사용자: {}", currentUser.getMember().getId());

        try {
            List<FamilyMemberResponse> result = familyService.getSentInvitationsForApp(currentUser.getMember());

            log.info("보낸 초대 목록 조회 완료 - 사용자: {}, 초대 수: {}",
                    currentUser.getMember().getId(), result.size());

            return ResponseDTO.ok(result);

        } catch (Exception e) {
            log.error("보낸 초대 목록 조회 실패 - 사용자: {}", currentUser.getMember().getId(), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===== 공통 액션 API (웹/앱 공용) =====

    /**
     * 가족 구성원 초대
     * POST /api/family/invite
     */
    @PostMapping("/invite")
    public ResponseDTO<String> inviteFamilyMember(
            @Valid @RequestBody FamilyInviteRequest request,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("가족 구성원 초대 API - 메모리얼: {}, 초대자: {}, 방법: {}, 연락처: {}",
                request.getMemorialId(), currentUser.getMember().getId(),
                request.getMethod(), request.getMaskedContact());

        try {
            // 요청 유효성 검사
            if (!request.isValidContact()) {
                log.warn("가족 구성원 초대 실패 - 잘못된 연락처 형식: {}", request.getMaskedContact());
                throw new APIException(ResponseStatus.BAD_REQUEST);
            }

            String inviteToken = familyService.inviteFamilyMember(request, currentUser.getMember());

            log.info("가족 구성원 초대 완료 - 메모리얼: {}, 토큰: {}, 연락처: {}",
                    request.getMemorialId(), inviteToken.substring(0, 8) + "...", request.getMaskedContact());

            return ResponseDTO.ok("초대가 성공적으로 전송되었습니다.");

        } catch (IllegalArgumentException e) {
            log.warn("가족 구성원 초대 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(ResponseStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            log.warn("가족 구성원 초대 실패 - 권한 없음: {}", e.getMessage());
            throw new APIException(ResponseStatus.MEMORIAL_OWNER_ONLY);
        } catch (Exception e) {
            log.error("가족 구성원 초대 실패 - 메모리얼: {}", request.getMemorialId(), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 초대 수락
     * POST /api/family/invite/{inviteToken}/accept
     */
    @PostMapping("/invite/{inviteToken}/accept")
    public ResponseDTO<String> acceptInvite(
            @PathVariable String inviteToken,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("초대 수락 API - 토큰: {}, 사용자: {}",
                inviteToken.substring(0, 8) + "...", currentUser.getMember().getId());

        try {
            familyService.acceptInvite(inviteToken, currentUser.getMember());

            log.info("초대 수락 완료 - 토큰: {}, 사용자: {}",
                    inviteToken.substring(0, 8) + "...", currentUser.getMember().getId());

            return ResponseDTO.ok("가족 구성원으로 등록되었습니다.");

        } catch (IllegalArgumentException e) {
            log.warn("초대 수락 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(ResponseStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("초대 수락 실패 - 토큰: {}", inviteToken.substring(0, 8) + "...", e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 초대 거절
     * POST /api/family/invite/{inviteToken}/reject
     */
    @PostMapping("/invite/{inviteToken}/reject")
    public ResponseDTO<String> rejectInvite(
            @PathVariable String inviteToken,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("초대 거절 API - 토큰: {}, 사용자: {}",
                inviteToken.substring(0, 8) + "...", currentUser.getMember().getId());

        try {
            familyService.rejectInvite(inviteToken, currentUser.getMember());

            log.info("초대 거절 완료 - 토큰: {}, 사용자: {}",
                    inviteToken.substring(0, 8) + "...", currentUser.getMember().getId());

            return ResponseDTO.ok("초대를 거절했습니다.");

        } catch (IllegalArgumentException e) {
            log.warn("초대 거절 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(ResponseStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("초대 거절 실패 - 토큰: {}", inviteToken.substring(0, 8) + "...", e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 가족 구성원 상세 조회
     * GET /api/family/{familyMemberId}
     */
    @GetMapping("/{familyMemberId}")
    public ResponseDTO<FamilyMemberResponse> getFamilyMember(
            @PathVariable Long familyMemberId,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("가족 구성원 상세 조회 API - 구성원: {}, 사용자: {}",
                familyMemberId, currentUser.getMember().getId());

        try {
            FamilyMemberResponse result = familyService.getFamilyMemberForApp(
                    familyMemberId, currentUser.getMember());

            log.info("가족 구성원 상세 조회 완료 - 구성원: {}", familyMemberId);

            return ResponseDTO.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("가족 구성원 조회 실패 - 권한 없음: {}", e.getMessage());
            throw new APIException(ResponseStatus.MEMORIAL_ACCESS_DENIED);
        } catch (Exception e) {
            log.error("가족 구성원 조회 실패 - 구성원: {}", familyMemberId, e);
            throw new APIException(ResponseStatus.DATA_NOT_FOUND);
        }
    }

    /**
     * 가족 구성원 권한 설정
     * PUT /api/family/{familyMemberId}/permissions
     */
    @PutMapping("/{familyMemberId}/permissions")
    public ResponseDTO<String> updatePermissions(
            @PathVariable Long familyMemberId,
            @Valid @RequestBody PermissionUpdateRequest request,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("권한 설정 API - 구성원: {}, 사용자: {}, 권한: {}",
                familyMemberId, currentUser.getMember().getId(), request.getSummary());

        try {
            // 권한 유효성 검사
            if (!request.isValid()) {
                log.warn("권한 설정 실패 - 잘못된 권한 조합: {}", request.getValidationMessage());
                throw new APIException(ResponseStatus.BAD_REQUEST);
            }

            familyService.updatePermissions(familyMemberId, request, currentUser.getMember());

            log.info("권한 설정 완료 - 구성원: {}, 메모리얼 접근: {}, 영상통화: {}",
                    familyMemberId, request.getMemorialAccess(), request.getVideoCallAccess());

            return ResponseDTO.ok("권한이 성공적으로 변경되었습니다.");

        } catch (IllegalArgumentException e) {
            log.warn("권한 설정 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(ResponseStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            log.warn("권한 설정 실패 - 권한 없음: {}", e.getMessage());
            throw new APIException(ResponseStatus.MEMORIAL_OWNER_ONLY);
        } catch (Exception e) {
            log.error("권한 설정 실패 - 구성원: {}", familyMemberId, e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 가족 구성원 제거
     * DELETE /api/family/{familyMemberId}
     */
    @DeleteMapping("/{familyMemberId}")
    public ResponseDTO<String> removeFamilyMember(
            @PathVariable Long familyMemberId,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("가족 구성원 제거 API - 구성원: {}, 사용자: {}",
                familyMemberId, currentUser.getMember().getId());

        try {
            String memberName = familyService.removeFamilyMember(familyMemberId, currentUser.getMember());

            log.info("가족 구성원 제거 완료 - 구성원: {}, 이름: {}", familyMemberId, memberName);

            return ResponseDTO.ok(memberName + "님이 가족 구성원에서 제거되었습니다.");

        } catch (IllegalArgumentException e) {
            log.warn("가족 구성원 제거 실패 - 권한 없음: {}", e.getMessage());
            throw new APIException(ResponseStatus.MEMORIAL_OWNER_ONLY);
        } catch (Exception e) {
            log.error("가족 구성원 제거 실패 - 구성원: {}", familyMemberId, e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 초대 취소
     * DELETE /api/family/invitations/{familyMemberId}
     */
    @DeleteMapping("/invitations/{familyMemberId}")
    public ResponseDTO<String> cancelInvitation(
            @PathVariable Long familyMemberId,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("초대 취소 API - 구성원: {}, 사용자: {}", familyMemberId, currentUser.getMember().getId());

        try {
            familyService.cancelInvitation(familyMemberId, currentUser.getMember());

            log.info("초대 취소 완료 - 구성원: {}", familyMemberId);

            return ResponseDTO.ok("초대가 취소되었습니다.");

        } catch (IllegalArgumentException e) {
            log.warn("초대 취소 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(ResponseStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            log.warn("초대 취소 실패 - 권한 없음: {}", e.getMessage());
            throw new APIException(ResponseStatus.MEMORIAL_OWNER_ONLY);
        } catch (Exception e) {
            log.error("초대 취소 실패 - 구성원: {}", familyMemberId, e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
}