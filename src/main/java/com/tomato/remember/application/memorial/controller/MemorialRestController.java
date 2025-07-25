package com.tomato.remember.application.memorial.controller;

import com.tomato.remember.application.memorial.dto.*;
import com.tomato.remember.application.memorial.service.MemorialQuestionService;
import com.tomato.remember.application.memorial.service.MemorialService;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.dto.ListDTO;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.exception.APIException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 메모리얼 API 컨트롤러
 */
@Tag(name = "Memorial API", description = "메모리얼 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/memorials")
@RequiredArgsConstructor
public class MemorialRestController {

    private final MemorialService memorialService;
    private final MemorialQuestionService memorialQuestionService;

    /**
     * 메모리얼 등록
     *
     * @param memorialData 메모리얼 기본 정보 (JSON)
     * @param profileImages 프로필 이미지 파일들 (5장 필수)
     * @param voiceFiles 음성 파일들 (3개 필수)
     * @param videoFile 영상 파일 (1개 필수)
     * @param userDetails 현재 로그인된 사용자
     * @return 메모리얼 등록 결과
     */
    @Operation(summary = "메모리얼 생성", description = "기본 정보와 미디어 파일을 업로드하여 메모리얼을 생성합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "메모리얼 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseDTO<MemorialCreateResponseDTO> createMemorial(
            @RequestPart("memorialData") @Valid MemorialCreateRequestDTO memorialData,
            @RequestPart("profileImages") List<MultipartFile> profileImages,
            @RequestPart("voiceFiles") List<MultipartFile> voiceFiles,
            @RequestPart("videoFile") MultipartFile videoFile,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member member = userDetails.getMember();
        log.info("메모리얼 생성 요청 - 사용자: {} (ID: {}), 메모리얼명: {}",
                member.getName(), member.getId(), memorialData.getName());

        try {
            // 1. 파일 개수 유효성 검사
            validateFileCount(profileImages, voiceFiles, videoFile);

            // 2. 동적 질문 답변 유효성 검사
            validateQuestionAnswers(memorialData.getQuestionAnswers());

            // 3. 메모리얼 생성
            MemorialCreateResponseDTO response = memorialService.createMemorial(
                    memorialData, profileImages, voiceFiles, videoFile, member);

            log.info("메모리얼 생성 성공 - ID: {}, 이름: {}, 답변 수: {}",
                    response.getMemorialId(), response.getName(),
                    memorialData.getAnsweredQuestionCount());

            return ResponseDTO.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("메모리얼 생성 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(e.getMessage(), ResponseStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("메모리얼 생성 실패 - 사용자: {}, 오류: {}", member.getId(), e.getMessage(), e);
            throw new APIException("메모리얼 생성 중 오류가 발생했습니다.", ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 사용자의 메모리얼 목록 조회
     *
     * @param userDetails 현재 로그인된 사용자
     * @return 메모리얼 목록
     */
    @Operation(summary = "내 메모리얼 목록 조회", description = "현재 사용자가 소유한 메모리얼 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "메모리얼 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/my")
    public ResponseDTO<ListDTO<MemorialListResponseDTO>> getMyMemorials(
        @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size) {

        // 1. 인증된 사용자 확인
        if (userDetails == null) {
            throw new APIException(ResponseStatus.UNAUTHORIZED);
        }

        // 2. Member 객체 안전하게 추출
        Member member = userDetails.getMember();
        if (member == null) {
            throw new APIException(ResponseStatus.USER_NOT_EXIST);
        }

        // 3. 페이징 파라미터 유효성 검사 및 안전한 Pageable 생성
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) { // 최대 100개로 제한
            size = 10;
        }

        Pageable pageable = PageRequest.of(page, size);

        // 서비스 호출
        ListDTO<MemorialListResponseDTO> response = memorialService.getMyMemorials(member, pageable);

        // ResponseDTO로 감싸서 반환
        return ResponseDTO.ok(response);
    }

    /**
     * 메모리얼 상세 조회
     *
     * @param memorialId 메모리얼 ID
     * @param userDetails 현재 로그인된 사용자
     * @return 메모리얼 상세 정보
     */
    @Operation(summary = "메모리얼 상세 조회", description = "특정 메모리얼의 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "메모리얼 상세 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음")
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/{memorialId}")
    public ResponseDTO<MemorialListResponseDTO> getMemorial(
            @PathVariable Long memorialId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member member = userDetails.getMember();
        log.info("Getting memorial detail for member: {} (ID: {}), memorialId: {}",
                member.getName(), member.getId(), memorialId);

        MemorialListResponseDTO response = memorialService.getMemorial(memorialId, member);

        log.info("Memorial detail retrieved successfully: memorialId={}", memorialId);

        return ResponseDTO.ok(response);
    }

    /**
     * 내 메모리얼 목록 조회 (비페이징) - 가족 관리용
     * GET /api/memorial/my-memorials
     * JavaScript family-list.js에서 호출하는 경로
     */
    @Operation(summary = "내 메모리얼 목록 조회 (비페이징)", description = "가족 관리에서 사용하는 메모리얼 목록을 조회합니다.")
    @GetMapping("/memorial/my-memorials")
    public ResponseDTO<List<MemorialListResponseDTO>> getMyMemorialsForFamily(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        // 인증된 사용자 확인
        if (userDetails == null) {
            throw new APIException(ResponseStatus.UNAUTHORIZED);
        }

        Member member = userDetails.getMember();
        if (member == null) {
            throw new APIException(ResponseStatus.USER_NOT_EXIST);
        }

        log.info("내 메모리얼 목록 조회 (가족 관리용) - 사용자: {}", member.getId());

        try {
            // 비페이징으로 모든 메모리얼 조회
            List<MemorialListResponseDTO> result = memorialService.getMyMemorialsForApi(member);

            log.info("내 메모리얼 목록 조회 완료 - 사용자: {}, 메모리얼 수: {}",
                    member.getId(), result.size());

            return ResponseDTO.ok(result);

        } catch (Exception e) {
            log.error("내 메모리얼 목록 조회 실패 - 사용자: {}", member.getId(), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * 메모리얼 방문 기록 (통화 후 호출)
     *
     * @param memorialId 메모리얼 ID
     * @param userDetails 현재 로그인된 사용자
     * @return 방문 기록 결과
     */
    @Operation(summary = "메모리얼 방문 기록", description = "메모리얼 방문 기록을 저장합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "방문 기록 저장 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "메모리얼을 찾을 수 없음")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/{memorialId}/visit")
    public ResponseDTO<Void> recordVisit(
            @PathVariable Long memorialId,
            @Parameter(hidden = true) @AuthenticationPrincipal MemberUserDetails userDetails) {

        Member member = userDetails.getMember();
        log.info("Recording visit for member: {} (ID: {}), memorialId: {}",
                member.getName(), member.getId(), memorialId);

        memorialService.recordVisit(memorialId, member);

        log.info("Visit recorded successfully: memorialId={}", memorialId);

        return ResponseDTO.ok(null);
    }

    /**
     * 파일 개수 유효성 검사
     */
    private void validateFileCount(List<MultipartFile> profileImages,
                                   List<MultipartFile> voiceFiles,
                                   MultipartFile videoFile) {

        // 프로필 이미지 검사
        if (profileImages == null || profileImages.size() != 5) {
            throw new IllegalArgumentException("프로필 이미지는 정확히 5장이 필요합니다.");
        }

        // 음성 파일 검사
        if (voiceFiles == null || voiceFiles.size() != 3) {
            throw new IllegalArgumentException("음성 파일은 정확히 3개가 필요합니다.");
        }

        // 영상 파일 검사
        if (videoFile == null || videoFile.isEmpty()) {
            throw new IllegalArgumentException("영상 파일은 1개가 필요합니다.");
        }

        // 빈 파일 검사
        boolean hasEmptyProfileImage = profileImages.stream().anyMatch(MultipartFile::isEmpty);
        boolean hasEmptyVoiceFile = voiceFiles.stream().anyMatch(MultipartFile::isEmpty);

        if (hasEmptyProfileImage) {
            throw new IllegalArgumentException("업로드된 프로필 이미지 중 빈 파일이 있습니다.");
        }

        if (hasEmptyVoiceFile) {
            throw new IllegalArgumentException("업로드된 음성 파일 중 빈 파일이 있습니다.");
        }
    }

    private void validateQuestionAnswers(List<MemorialQuestionAnswerDTO> questionAnswers) {
        if (questionAnswers == null || questionAnswers.isEmpty()) {
            throw new IllegalArgumentException("질문 답변은 최소 1개 이상 필요합니다.");
        }

        log.info("질문 답변 유효성 검사 시작 - 답변 수: {}", questionAnswers.size());

        try {
            // 활성 질문 목록 조회
            List<MemorialQuestionResponse> activeQuestions = memorialQuestionService.getActiveQuestions();
            Map<Long, MemorialQuestionResponse> questionMap = activeQuestions.stream()
                    .collect(Collectors.toMap(MemorialQuestionResponse::getId, Function.identity()));

            // 필수 질문 답변 검사
            for (MemorialQuestionResponse question : activeQuestions) {
                if (question.getIsRequired()) {
                    boolean hasAnswer = questionAnswers.stream()
                            .anyMatch(dto -> dto.getQuestionId().equals(question.getId()) && dto.hasAnswer());

                    if (!hasAnswer) {
                        throw new IllegalArgumentException(
                                String.format("필수 질문에 답변이 없습니다: %s", question.getQuestionText()));
                    }
                }
            }

            // 각 답변의 유효성 검사
            for (MemorialQuestionAnswerDTO answerDTO : questionAnswers) {
                if (!answerDTO.hasAnswer()) {
                    continue; // 빈 답변은 건너뛰기
                }

                MemorialQuestionResponse question = questionMap.get(answerDTO.getQuestionId());
                if (question == null) {
                    throw new IllegalArgumentException(
                            String.format("존재하지 않는 질문입니다: %d", answerDTO.getQuestionId()));
                }

                // 답변 길이 검사
                int answerLength = answerDTO.getAnswerLength();
                if (answerLength < question.getMinLength()) {
                    throw new IllegalArgumentException(
                            String.format("답변이 너무 짧습니다. 최소 %d자 이상 입력해주세요: %s",
                                    question.getMinLength(), question.getQuestionText()));
                }

                if (answerLength > question.getMaxLength()) {
                    throw new IllegalArgumentException(
                            String.format("답변이 너무 깁니다. 최대 %d자 이하로 입력해주세요: %s",
                                    question.getMaxLength(), question.getQuestionText()));
                }
            }

            log.info("질문 답변 유효성 검사 완료 - 유효한 답변 수: {}",
                    questionAnswers.stream().filter(MemorialQuestionAnswerDTO::hasAnswer).count());

        } catch (Exception e) {
            log.error("질문 답변 유효성 검사 실패: {}", e.getMessage());
            throw new IllegalArgumentException("질문 답변 유효성 검사에 실패했습니다: " + e.getMessage());
        }
    }
}