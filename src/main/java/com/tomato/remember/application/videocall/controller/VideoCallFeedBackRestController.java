package com.tomato.remember.application.videocall.controller;

import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.application.videocall.dto.VideoCallFeedbackRequestDTO;
import com.tomato.remember.application.videocall.dto.VideoCallFeedbackResponseDTO;
import com.tomato.remember.application.videocall.entity.VideoCallSampleReview;
import com.tomato.remember.application.videocall.service.VideoCallSampleReviewService;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.exception.APIException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.remember.application.videocall.controller
 * @fileName : VideoCallFeedBackRestController
 * @date : 2025-07-21
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Slf4j
@RestController
@RequestMapping("/api/video-call")
@RequiredArgsConstructor
public class VideoCallFeedBackRestController {
    private final VideoCallSampleReviewService videoCallSampleReviewService;

    @PostMapping("/sample/feedback")
    public ResponseDTO<VideoCallFeedbackResponseDTO> submitFeedback(
            @Valid @RequestBody VideoCallFeedbackRequestDTO requestDTO,
            @AuthenticationPrincipal MemberUserDetails userDetails) {

        try {
            log.info("비디오콜 피드백 제출 - 사용자: {}, 대상: {}",
                    userDetails.getMember().getName(), requestDTO.getContactKey());

            VideoCallSampleReview review = VideoCallSampleReview.builder()
                    .caller(userDetails.getMember())
                    .reviewMessage(requestDTO.getReviewMessage())
                    .contactKey(requestDTO.getContactKey())
                    .build();

            VideoCallSampleReview savedReview = videoCallSampleReviewService.save(review);

            VideoCallFeedbackResponseDTO result = VideoCallFeedbackResponseDTO.builder()
                    .success(true)
                    .message("소중한 피드백을 남겨주셔서 감사합니다!")
                    .feedbackId(savedReview.getId())
                    .build();

            log.info("비디오콜 피드백 저장 성공 - ID: {}", savedReview.getId());

            return ResponseDTO.ok(result);

        } catch (Exception e) {
            log.error("비디오콜 피드백 저장 실패", e);

            VideoCallFeedbackResponseDTO errorResponse = VideoCallFeedbackResponseDTO.builder()
                    .success(false)
                    .message("피드백 저장 중 오류가 발생했습니다.")
                    .build();

            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
