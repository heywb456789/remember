package com.tomato.remember.application.memorial.controller;

import com.tomato.remember.application.memorial.dto.MemorialCreateRequestDTO;
import com.tomato.remember.application.memorial.dto.MemorialResponseDTO;
import com.tomato.remember.application.memorial.dto.MemorialUpdateRequestDTO;
import com.tomato.remember.application.memorial.service.MemorialService;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.dto.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 메모리얼 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/memorial")
@RequiredArgsConstructor
@Tag(name = "메모리얼 API", description = "메모리얼 관련 API")
public class MemorialRestController {

    private final MemorialService memorialService;

    /**
     * 내 메모리얼 목록 조회
     */
    @Operation(
            summary = "내 메모리얼 목록 조회",
            description = "현재 로그인한 사용자의 메모리얼 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메모리얼 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/my")
    public ResponseDTO<List<MemorialResponseDTO>> getMyMemorials(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member currentUser = userDetails.getMember();
        log.info("My memorial list request from user: {} (ID: {})",
                currentUser.getName(), currentUser.getId());

        List<MemorialResponseDTO> memorials = memorialService.getActiveMemorialListByMember(currentUser)
                .stream()
                .map(memorial -> memorial.toResponseDTO())
                .toList();

        log.info("Returned {} memorials for user: {} (ID: {})",
                memorials.size(), currentUser.getName(), currentUser.getId());

        return ResponseDTO.ok(memorials);
    }

    /**
     * 메모리얼 생성
     */
    @Operation(
            summary = "메모리얼 생성",
            description = "새로운 메모리얼을 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메모리얼 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    public ResponseDTO<MemorialResponseDTO> createMemorial(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @Valid @RequestBody MemorialCreateRequestDTO createRequest) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial creation API request from user: {} (ID: {}), memorial name: {}",
                currentUser.getName(), currentUser.getId(), createRequest.getName());

        MemorialResponseDTO createdMemorial = memorialService.createMemorial(createRequest, currentUser);

        log.info("Memorial created successfully via API: ID={}, Name={}, Owner={}",
                createdMemorial.getId(), createdMemorial.getName(), currentUser.getName());

        return ResponseDTO.ok(createdMemorial);
    }

    /**
     * 메모리얼 상세 조회
     */
    @Operation(
            summary = "메모리얼 상세 조회",
            description = "메모리얼 ID로 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메모리얼 조회 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/{memorialId}")
    public ResponseDTO<MemorialResponseDTO> getMemorial(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @PathVariable Long memorialId) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial detail API request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        MemorialResponseDTO memorial = memorialService.getMemorialById(memorialId, currentUser);

        log.info("Memorial detail returned successfully via API: ID={}, Name={}",
                memorial.getId(), memorial.getName());

        return ResponseDTO.ok(memorial);
    }

    /**
     * 메모리얼 수정
     */
    @Operation(
            summary = "메모리얼 수정",
            description = "메모리얼 정보를 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메모리얼 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{memorialId}")
    public ResponseDTO<MemorialResponseDTO> updateMemorial(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @PathVariable Long memorialId,
            @Valid @RequestBody MemorialUpdateRequestDTO updateRequest) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial update API request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        MemorialResponseDTO updatedMemorial = memorialService.updateMemorial(memorialId, updateRequest, currentUser);

        log.info("Memorial updated successfully via API: ID={}, Name={}, Owner={}",
                updatedMemorial.getId(), updatedMemorial.getName(), currentUser.getName());

        return ResponseDTO.ok(updatedMemorial);
    }

    /**
     * 메모리얼 수정 폼 데이터 조회
     */
    @Operation(
            summary = "메모리얼 수정 폼 데이터 조회",
            description = "메모리얼 수정을 위한 폼 데이터를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 폼 데이터 조회 성공"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/{memorialId}/edit")
    public ResponseDTO<MemorialUpdateRequestDTO> getMemorialForEdit(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @PathVariable Long memorialId) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial edit form data API request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        MemorialUpdateRequestDTO updateRequest = memorialService.getMemorialForUpdate(memorialId, currentUser);

        log.info("Memorial edit form data returned successfully via API: Memorial ID={}", memorialId);

        return ResponseDTO.ok(updateRequest);
    }

    /**
     * 메모리얼 방문 기록
     */
    @Operation(
            summary = "메모리얼 방문 기록",
            description = "메모리얼 방문을 기록합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "방문 기록 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/{memorialId}/visit")
    public ResponseDTO<Void> recordMemorialVisit(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @PathVariable Long memorialId) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial visit record API request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        memorialService.recordMemorialVisit(memorialId, currentUser);

        log.info("Memorial visit recorded successfully via API: Memorial ID={}", memorialId);

        return ResponseDTO.ok();
    }
    /**
     * 메모리얼 삭제 (소프트 삭제)
     */
    @Operation(
            summary = "메모리얼 삭제",
            description = "메모리얼을 삭제합니다. (소프트 삭제)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메모리얼 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{memorialId}")
    public ResponseDTO<Void> deleteMemorial(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @PathVariable Long memorialId) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial deletion API request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        memorialService.deleteMemorial(memorialId, currentUser);

        log.info("Memorial deleted successfully via API: Memorial ID={}", memorialId);

        return ResponseDTO.ok();
    }

    /**
     * 메모리얼 상태 토글 (활성화/비활성화)
     */
    @Operation(
            summary = "메모리얼 상태 토글",
            description = "메모리얼 상태를 활성화/비활성화 간 전환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "403", description = "상태 변경 권한 없음"),
            @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PatchMapping("/{memorialId}/toggle-status")
    public ResponseDTO<Void> toggleMemorialStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @PathVariable Long memorialId) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial status toggle API request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        memorialService.toggleMemorialStatus(memorialId, currentUser);

        log.info("Memorial status toggled successfully via API: Memorial ID={}", memorialId);

        return ResponseDTO.ok();
    }

    /**
     * 메모리얼 활성화
     */
    @Operation(
            summary = "메모리얼 활성화",
            description = "메모리얼을 활성화합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메모리얼 활성화 성공"),
            @ApiResponse(responseCode = "403", description = "활성화 권한 없음"),
            @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PatchMapping("/{memorialId}/activate")
    public ResponseDTO<Void> activateMemorial(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @PathVariable Long memorialId) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial activation API request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        memorialService.activateMemorial(memorialId, currentUser);

        log.info("Memorial activated successfully via API: Memorial ID={}", memorialId);

        return ResponseDTO.ok();
    }

    /**
     * 메모리얼 비활성화
     */
    @Operation(
            summary = "메모리얼 비활성화",
            description = "메모리얼을 비활성화합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메모리얼 비활성화 성공"),
            @ApiResponse(responseCode = "403", description = "비활성화 권한 없음"),
            @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PatchMapping("/{memorialId}/deactivate")
    public ResponseDTO<Void> deactivateMemorial(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @PathVariable Long memorialId) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial deactivation API request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        memorialService.deactivateMemorial(memorialId, currentUser);

        log.info("Memorial deactivated successfully via API: Memorial ID={}", memorialId);

        return ResponseDTO.ok();
    }

    /**
     * 메모리얼 복구
     */
    @Operation(
            summary = "메모리얼 복구",
            description = "삭제된 메모리얼을 복구합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메모리얼 복구 성공"),
            @ApiResponse(responseCode = "403", description = "복구 권한 없음"),
            @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PatchMapping("/{memorialId}/restore")
    public ResponseDTO<Void> restoreMemorial(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @PathVariable Long memorialId) {

        Member currentUser = userDetails.getMember();
        log.info("Memorial restoration API request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        memorialService.restoreMemorial(memorialId, currentUser);

        log.info("Memorial restored successfully via API: Memorial ID={}", memorialId);

        return ResponseDTO.ok();
    }

    /**
     * 메모리얼 완전 삭제
     */
    @Operation(
            summary = "메모리얼 완전 삭제",
            description = "메모리얼을 완전히 삭제합니다. (복구 불가)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메모리얼 완전 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "완전 삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{memorialId}/permanent")
    public ResponseDTO<Void> permanentlyDeleteMemorial(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
            @PathVariable Long memorialId) {

        Member currentUser = userDetails.getMember();
        log.warn("Memorial permanent deletion API request from user: {} (ID: {}), Memorial ID: {}",
                currentUser.getName(), currentUser.getId(), memorialId);

        memorialService.permanentlyDeleteMemorial(memorialId, currentUser);

        log.warn("Memorial permanently deleted successfully via API: Memorial ID={}", memorialId);

        return ResponseDTO.ok();
    }
}