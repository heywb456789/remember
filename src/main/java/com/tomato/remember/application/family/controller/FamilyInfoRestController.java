package com.tomato.remember.application.family.controller;

import com.tomato.remember.application.family.dto.FamilyInfoRequestDTO;
import com.tomato.remember.application.family.dto.FamilyInfoResponseDTO;
import com.tomato.remember.application.family.service.FamilyService;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.exception.APIException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 가족 구성원용 고인 상세 정보 API 컨트롤러
 */
@Tag(name = "Family Info API", description = "가족 구성원용 고인 상세 정보 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/memorial")
@RequiredArgsConstructor
public class FamilyInfoRestController {

    private final FamilyService familyService;

    /**
     * 가족 구성원용 고인 상세 정보 조회 GET /api/memorial/{memorialId}/family-info
     */
    @Operation(summary = "고인 상세 정보 조회", description = "가족 구성원이 고인에 대한 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음")
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/{memorialId}/family-info")
    public ResponseDTO<FamilyInfoResponseDTO> getFamilyInfo(
        @PathVariable Long memorialId,
        @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        log.info("가족 구성원 고인 상세 정보 조회 API - 메모리얼: {}, 사용자: {}",
            memorialId, userDetails.getMember().getId());

        try {
            Member member = userDetails.getMember();
            FamilyInfoResponseDTO response = familyService.getFamilyInfo(memorialId, member);

            log.info("가족 구성원 고인 상세 정보 조회 완료 - 메모리얼: {}, 완성도: {}%",
                memorialId, response.getCompletionPercent());

            return ResponseDTO.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("가족 구성원 고인 상세 정보 조회 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(e.getMessage(), ResponseStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            log.warn("가족 구성원 고인 상세 정보 조회 실패 - 권한 없음: {}", e.getMessage());
            throw new APIException(e.getMessage(), ResponseStatus.MEMORIAL_ACCESS_DENIED);
        } catch (Exception e) {
            log.error("가족 구성원 고인 상세 정보 조회 실패 - 메모리얼: {}", memorialId, e);
            throw new APIException("고인 상세 정보 조회 중 오류가 발생했습니다.", ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 가족 구성원용 고인 상세 정보 등록 POST /api/memorial/{memorialId}/family-info
     */
    @Operation(summary = "고인 상세 정보 등록", description = "가족 구성원이 고인에 대한 상세 정보를 등록합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "등록 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 등록된 정보"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/{memorialId}/family-info")
    public ResponseDTO<Map<String, Object>> saveFamilyInfo(
        @PathVariable Long memorialId,
        @Valid @RequestBody FamilyInfoRequestDTO request,
        @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        try {
            Member member = userDetails.getMember();

            familyService.saveFamilyAnswers(memorialId, member, request.getQuestionAnswers());

            Map<String, Object> data = Map.of(
                "success", true,
                "message", "답변이 저장되었습니다.",
                "memorialId", memorialId
            );
            return ResponseDTO.ok(data);

        } catch (IllegalArgumentException e) {
            log.warn("가족 구성원 고인 상세 정보 등록 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(e.getMessage(), ResponseStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            log.warn("가족 구성원 고인 상세 정보 등록 실패 - 권한 없음: {}", e.getMessage());
            throw new APIException(e.getMessage(), ResponseStatus.MEMORIAL_ACCESS_DENIED);
        } catch (Exception e) {
            log.error("가족 구성원 고인 상세 정보 등록 실패 - 메모리얼: {}", memorialId, e);
            throw new APIException("고인 상세 정보 등록 중 오류가 발생했습니다.", ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 가족 구성원용 고인 상세 정보 등록 가능 여부 확인 GET /api/memorial/{memorialId}/family-info/check
     */
    @Operation(summary = "고인 상세 정보 등록 가능 여부 확인", description = "가족 구성원이 고인 상세 정보를 등록할 수 있는지 확인합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/{memorialId}/family-info/check")
    public ResponseDTO<Map<String, Object>> checkFamilyInfoAccess(
        @PathVariable Long memorialId,
        @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        log.info("가족 구성원 고인 상세 정보 등록 가능 여부 확인 API - 메모리얼: {}, 사용자: {}",
            memorialId, userDetails.getMember().getId());

        try {
            Member member = userDetails.getMember();
            Map<String, Object> checkResult = familyService.checkFamilyInfoAccess(memorialId, member);

            log.info("가족 구성원 고인 상세 정보 등록 가능 여부 확인 완료 - 메모리얼: {}, 접근 가능: {}",
                memorialId, checkResult.get("canAccess"));

            return ResponseDTO.ok(checkResult);

        } catch (IllegalArgumentException e) {
            log.warn("가족 구성원 고인 상세 정보 등록 가능 여부 확인 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(e.getMessage(), ResponseStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("가족 구성원 고인 상세 정보 등록 가능 여부 확인 실패 - 메모리얼: {}", memorialId, e);
            throw new APIException("접근 권한 확인 중 오류가 발생했습니다.", ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
}