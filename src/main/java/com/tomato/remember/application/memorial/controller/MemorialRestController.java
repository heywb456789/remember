package com.tomato.remember.application.memorial.controller;

import com.tomato.remember.application.memorial.dto.*;
import com.tomato.remember.application.memorial.service.MemorialService;
import com.tomato.remember.application.memorial.service.FileUploadService;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.dto.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
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
    private final FileUploadService fileUploadService;

    /**
     * 1단계: 메모리얼 임시 생성 (DRAFT 상태)
     */
    @Operation(summary = "메모리얼 1단계 생성", description = "기본 정보를 입력하여 메모리얼을 DRAFT 상태로 생성합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "메모리얼 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/create/step1")
    public ResponseDTO<MemorialCreateResponseDTO> createStep1(
            @Valid @RequestBody MemorialStep1RequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member member = userDetails.getMember();
        log.info("Creating memorial step1 for member: {} (ID: {})", member.getName(), member.getId());

        MemorialCreateResponseDTO response = memorialService.createMemorialStep1(request, member);

        log.info("Memorial step1 created successfully: memorialId={}", response.getMemorialId());
        return ResponseDTO.ok(response);
    }

    /**
     * 2단계: 고인 정보 업데이트
     */
    @Operation(summary = "메모리얼 2단계 업데이트", description = "메모리얼에 고인 정보를 업데이트합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{memorialId}/step2")
    public ResponseDTO<MemorialResponseDTO> updateStep2(
            @PathVariable Long memorialId,
            @Valid @RequestBody MemorialStep2RequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member member = userDetails.getMember();
        log.info("Updating memorial step2 for memorialId: {} by member: {} (ID: {})",
                memorialId, member.getName(), member.getId());

        MemorialResponseDTO response = memorialService.updateMemorialStep2(memorialId, request, member);

        log.info("Memorial step2 updated successfully: memorialId={}", memorialId);
        return ResponseDTO.ok(response);
    }

    /**
     * 3단계: 미디어 파일 업로드
     */
    @Operation(summary = "메모리얼 파일 업로드", description = "메모리얼에 미디어 파일을 업로드합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/{memorialId}/upload")
    public ResponseDTO<List<FileUploadResponseDTO>> uploadFiles(
            @PathVariable Long memorialId,
            @RequestParam("fileType") String fileType,
            @RequestParam("files") List<MultipartFile> files,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member member = userDetails.getMember();
        log.info("Uploading files for memorialId: {} by member: {} (ID: {}), fileType: {}, fileCount: {}",
                memorialId, member.getName(), member.getId(), fileType, files.size());

        // 메모리얼 접근 권한 검증
        memorialService.validateMemorialAccess(memorialId, member);

        List<FileUploadResponseDTO> responses = fileUploadService.uploadFiles(files, fileType, member);

        // 업로드된 파일 정보를 메모리얼에 연결
        memorialService.updateMemorialFiles(memorialId, fileType, responses, member);

        log.info("Files uploaded successfully for memorialId: {}, uploaded count: {}", memorialId, responses.size());
        return ResponseDTO.ok(responses);
    }

    /**
     * 메모리얼 완료 처리 (DRAFT → ACTIVE)
     */
    @Operation(summary = "메모리얼 완료 처리", description = "메모리얼을 DRAFT에서 ACTIVE 상태로 변경합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{memorialId}/complete")
    public ResponseDTO<MemorialResponseDTO> completeMemorial(
            @PathVariable Long memorialId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member member = userDetails.getMember();
        log.info("Completing memorial: memorialId={} by member: {} (ID: {})",
                memorialId, member.getName(), member.getId());

        MemorialResponseDTO response = memorialService.completeMemorial(memorialId, member);

        log.info("Memorial completed successfully: memorialId={}", memorialId);
        return ResponseDTO.ok(response);
    }

    /**
     * 내 메모리얼 목록 조회
     */
    @Operation(summary = "내 메모리얼 목록 조회", description = "현재 사용자의 메모리얼 목록을 조회합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/my")
    public ResponseDTO<List<MemorialResponseDTO>> getMyMemorials(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member member = userDetails.getMember();
        log.info("Getting memorial list for member: {} (ID: {})", member.getName(), member.getId());

        List<MemorialResponseDTO> memorials = memorialService.getActiveMemorialListByMember(member);

        log.info("Retrieved {} memorials for member: {} (ID: {})",
                memorials.size(), member.getName(), member.getId());
        return ResponseDTO.ok(memorials);
    }

    /**
     * 메모리얼 상세 조회
     */
    @Operation(summary = "메모리얼 상세 조회", description = "메모리얼 상세 정보를 조회합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/{memorialId}")
    public ResponseDTO<MemorialResponseDTO> getMemorial(
            @PathVariable Long memorialId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member member = userDetails.getMember();
        log.info("Getting memorial detail: memorialId={} by member: {} (ID: {})",
                memorialId, member.getName(), member.getId());

        MemorialResponseDTO memorial = memorialService.getMemorialById(memorialId, member);

        log.info("Retrieved memorial detail: memorialId={}", memorialId);
        return ResponseDTO.ok(memorial);
    }

    /**
     * 메모리얼 삭제
     */
    @Operation(summary = "메모리얼 삭제", description = "메모리얼을 삭제합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{memorialId}")
    public ResponseDTO<Void> deleteMemorial(
            @PathVariable Long memorialId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member member = userDetails.getMember();
        log.info("Deleting memorial: memorialId={} by member: {} (ID: {})",
                memorialId, member.getName(), member.getId());

        memorialService.deleteMemorial(memorialId, member);

        log.info("Memorial deleted successfully: memorialId={}", memorialId);
        return ResponseDTO.ok();
    }

    /**
     * 파일 삭제
     */
    @Operation(summary = "파일 삭제", description = "업로드된 파일을 삭제합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{memorialId}/files")
    public ResponseDTO<Void> deleteFile(
            @PathVariable Long memorialId,
            @RequestParam("fileUrl") String fileUrl,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member member = userDetails.getMember();
        log.info("Deleting file: memorialId={}, fileUrl={} by member: {} (ID: {})",
                memorialId, fileUrl, member.getName(), member.getId());

        // 메모리얼 접근 권한 검증
        memorialService.validateMemorialAccess(memorialId, member);

        fileUploadService.deleteFile(fileUrl, member);

        log.info("File deleted successfully: fileUrl={}", fileUrl);
        return ResponseDTO.ok();
    }
}