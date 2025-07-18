package com.tomato.remember.application.family.controller;

import com.tomato.remember.application.family.dto.FamilyMemberResponse;
import com.tomato.remember.application.family.dto.FamilyPageData;
import com.tomato.remember.application.family.dto.FamilyPageResponse;
import com.tomato.remember.application.family.dto.FamilySearchCondition;
import com.tomato.remember.application.family.dto.MemorialSummaryResponse;
import com.tomato.remember.application.family.dto.FamilyPermissionRequest;
import com.tomato.remember.application.family.repository.FamilyMemberRepository;
import com.tomato.remember.application.family.service.FamilyService;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.repository.MemorialRepository;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.dto.ListDTO;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.exception.APIException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 가족 관리 REST API 컨트롤러 - 앱 전용 데이터 API - 웹/앱 공통 액션 API (권한, 제거 등) - 초대 관련 API는 FamilyInviteApiController에서 처리
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

    @GetMapping
    public ResponseDTO<FamilyPageResponse> getFamilyList(
        @AuthenticationPrincipal MemberUserDetails currentUser,
        @RequestParam(required = false) Long memorialId) {

        log.info("가족 목록 API 요청 - 사용자: {}, 메모리얼ID: {}",
            currentUser.getMember().getId(), memorialId);

        try {
            Member member = currentUser.getMember();

            // 서비스에서 가족 페이지 데이터 조회
            FamilyPageData pageData = familyService.getFamilyPageData(member, memorialId);

            // API 응답 데이터로 변환
            FamilyPageResponse response = FamilyPageResponse.from(pageData);

            log.info("가족 목록 API 응답 완료 - 메모리얼: {}, 구성원: {}",
                response.getSelectedMemorial().getId(),
                response.getFamilyMembers().size());

            return ResponseDTO.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("가족 목록 API 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(ResponseStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("가족 목록 API 실패 - 사용자: {}", currentUser.getMember().getId(), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 특정 메모리얼의 가족 구성원 목록 조회 (페이징) GET /api/family/memorial/{memorialId}/members
     */
    @GetMapping("/memorial/{memorialId}/members")
    public ResponseDTO<ListDTO<FamilyMemberResponse>> getFamilyMembers(
        @AuthenticationPrincipal MemberUserDetails currentUser,
        @PathVariable Long memorialId,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("특정 메모리얼 가족 구성원 목록 조회 API - 메모리얼: {}, 사용자: {}",
            memorialId, currentUser.getMember().getId());

        try {
            Member member = currentUser.getMember();

            // 가족 구성원 조회 (페이징)
            Page<FamilyMemberResponse> familyMembers = familyService.getFamilyMembersWithOwnerPaged(
                member, memorialId, pageable);

            log.info("메모리얼 가족 구성원 조회 완료 - 메모리얼: {}, 구성원: {}/{}",
                memorialId, familyMembers.getNumberOfElements(), familyMembers.getTotalElements());

            return ResponseDTO.ok(ListDTO.of(familyMembers));

        } catch (IllegalArgumentException e) {
            log.warn("메모리얼 가족 구성원 목록 조회 실패 - 권한 없음: {}", e.getMessage());
            throw new APIException(ResponseStatus.MEMORIAL_ACCESS_DENIED);
        } catch (Exception e) {
            log.error("메모리얼 가족 구성원 목록 조회 실패 - 메모리얼: {}", memorialId, e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 내 메모리얼 목록 조회 API (페이징) GET /api/family/memorials
     */
    @GetMapping("/memorials")
    public ResponseDTO<ListDTO<MemorialSummaryResponse>> getMyMemorials(
        @AuthenticationPrincipal MemberUserDetails currentUser,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("내 메모리얼 목록 조회 API 요청 - 사용자: {}, 페이지: {}",
            currentUser.getMember().getId(), pageable.getPageNumber());

        try {
            Member member = currentUser.getMember();

            // 내 메모리얼 목록 조회 (페이징)
            Page<MemorialSummaryResponse> memorials = familyService.getMyMemorialSummariesPaged(member, pageable);

            log.info("내 메모리얼 목록 조회 완료 - 메모리얼 수: {}/{}",
                memorials.getNumberOfElements(), memorials.getTotalElements());

            return ResponseDTO.ok(ListDTO.of(memorials));

        } catch (Exception e) {
            log.error("내 메모리얼 목록 조회 실패 - 사용자: {}", currentUser.getMember().getId(), e);
            return ResponseDTO.internalServerError();
        }
    }

    /**
     * 가족 구성원 검색 API (페이징) GET /api/family/search
     */
    @GetMapping("/search")
    public ResponseDTO<ListDTO<FamilyMemberResponse>> searchFamilyMembers(
        @AuthenticationPrincipal MemberUserDetails currentUser,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long memorialId,
        @RequestParam(required = false) String relationship,
        @RequestParam(required = false) String inviteStatus,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("가족 구성원 검색 API 요청 - 사용자: {}, 키워드: {}, 메모리얼: {}",
            currentUser.getMember().getId(), keyword, memorialId);

        try {
            Member member = currentUser.getMember();

            // 검색 조건 구성
            FamilySearchCondition condition = FamilySearchCondition.builder()
                .keyword(keyword)
                .memorialId(memorialId)
                .relationship(relationship)
                .inviteStatus(inviteStatus)
                .build();

            // 가족 구성원 검색 (페이징)
            Page<FamilyMemberResponse> searchResults = familyService.searchFamilyMembers(
                member, condition, pageable);

            log.info("가족 구성원 검색 완료 - 검색 결과: {}/{}",
                searchResults.getNumberOfElements(), searchResults.getTotalElements());

            return ResponseDTO.ok(ListDTO.of(searchResults));

        } catch (Exception e) {
            log.error("가족 구성원 검색 실패 - 사용자: {}", currentUser.getMember().getId(), e);
            return ResponseDTO.internalServerError();
        }
    }

    /**
     * 가족 구성원 상세 조회 GET /api/family/{familyMemberId}
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
     * 가족 구성원 권한 설정 PUT /api/family/member/{memberId}/permissions
     */
    @PutMapping("/memorials/{memorialId}/members/{memberId}/permissions")
    public ResponseDTO<String> updateFamilyMemberPermissions(
        @PathVariable Long memorialId,
        @PathVariable Long memberId,
        @Valid @RequestBody FamilyPermissionRequest request,
        @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("가족 구성원 권한 수정 API 요청 - 사용자: {}, 메모리얼: {}, 회원: {}, 권한: {}",
            currentUser.getMember().getId(), memorialId, memberId, request.getSummary());

        try {
            Member member = currentUser.getMember();

            // 유효성 검사
            if (! request.isValid()) {
                throw new APIException(ResponseStatus.BAD_REQUEST);
            }

            // 권한 수정 처리
            familyService.updateFamilyMemberPermissions(member, memorialId, memberId, request);

            log.info("가족 구성원 권한 수정 완료 - 메모리얼: {}, 회원: {}, 권한: {}",
                memorialId, memberId, request.getSummary());

            return ResponseDTO.ok("권한이 성공적으로 수정되었습니다.");

        } catch (SecurityException e) {
            log.warn("권한 설정 실패 - 권한 없음: {}", e.getMessage());
            throw new APIException(ResponseStatus.MEMORIAL_OWNER_ONLY);
        } catch (IllegalArgumentException e) {
            log.warn("권한 설정 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(ResponseStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("권한 설정 실패 - 메모리얼: {}, 회원: {}", memorialId, memberId, e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 가족 구성원 제거 DELETE /api/family/member/{memberId}
     */
    @DeleteMapping("/memorials/{memorialId}/members/{memberId}")
    public ResponseDTO<String> removeFamilyMember(
        @PathVariable Long memorialId,
        @PathVariable Long memberId,
        @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("가족 구성원 제거 API 요청 - 사용자: {}, 메모리얼: {}, 회원: {}",
            currentUser.getMember().getId(), memorialId, memberId);

        try {
            Member member = currentUser.getMember();

            // 구성원 제거 처리
            String removedMemberName = familyService.removeFamilyMember(member, memorialId, memberId);

            log.info("가족 구성원 제거 완료 - 메모리얼: {}, 회원: {}, 이름: {}",
                memorialId, memberId, removedMemberName);

            return ResponseDTO.ok(String.format("%s님이 가족 구성원에서 제거되었습니다.", removedMemberName));

        } catch (SecurityException e) {
            log.warn("구성원 제거 실패 - 권한 없음: {}", e.getMessage());
            throw new APIException(ResponseStatus.MEMORIAL_OWNER_ONLY);
        } catch (IllegalArgumentException e) {
            log.warn("구성원 제거 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(ResponseStatus.DATA_NOT_FOUND);
        } catch (Exception e) {
            log.error("구성원 제거 실패 - 메모리얼: {}, 회원: {}", memorialId, memberId, e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }


    //초대링크 생성
    @GetMapping("/memorials/{memorialId}/members/{memberId}/invite-link")
    public ResponseDTO<Map<String, Object>> generateInviteLink(
        @PathVariable Long memorialId,
        @PathVariable Long memberId,
        @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("초대 링크 생성 API 요청 - 사용자: {}, 메모리얼: {}, 회원: {}",
            currentUser.getMember().getId(), memorialId, memberId);

        try {
            Member member = currentUser.getMember();

            // 초대 링크 생성
            String inviteLink = familyService.generateInviteLink(member, memorialId, memberId);

            Map<String, Object> response = new HashMap<>();
            response.put("inviteLink", inviteLink);
            response.put("memberId", memberId);
            response.put("memorialId", memorialId);

            log.info("초대 링크 생성 완료 - 메모리얼: {}, 회원: {}", memorialId, memberId);

            return ResponseDTO.ok(response);

        } catch (SecurityException e) {
            log.warn("초대 링크 생성 실패 - 권한 없음: {}", e.getMessage());
            throw new APIException(ResponseStatus.MEMORIAL_OWNER_ONLY);
        } catch (IllegalArgumentException e) {
            log.warn("초대 링크 생성 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(ResponseStatus.DATA_NOT_FOUND);
        } catch (Exception e) {
            log.error("초대 링크 생성 실패 - 메모리얼: {}, 회원: {}", memorialId, memberId, e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/member/{memberId}/memorial")
    public ResponseDTO<?> removeMember(@PathVariable Long memberId, @RequestParam Long memorialId,
        @AuthenticationPrincipal MemberUserDetails userDetails) {
        Member member = userDetails.getMember();
        familyService.deleteFamilyMemberByMemorialId(memberId, member, memorialId);
        return ResponseDTO.ok();
    }


    /**
     * 내가 받은 초대 목록 조회 GET /api/family/invitations/received
     */
    @Deprecated
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
     * 내가 보낸 초대 목록 조회 GET /api/family/invitations/sent
     */
    @Deprecated
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
}