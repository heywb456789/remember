package com.tomato.remember.application.member.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.member.entity.MemberAiProfileImage;
import com.tomato.remember.application.member.repository.MemberAiProfileImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 얼굴 인식 API 연동 서비스 (간단 버전)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FaceDetectionService {

    private final WebClient webClient;
    private final MemberAiProfileImageRepository profileImageRepository;

    @Value("${face.detection.api.url:http://192.168.20.64:8082/api/detectfaces}")
    private String faceDetectionApiUrl;

    /**
     * 얼굴 인식 처리
     */
    public void processProfileImages(Member member) {
        log.info("얼굴 인식 처리 시작 - 회원 ID: {}", member.getId());

        try {
            List<MemberAiProfileImage> profileImages =
                profileImageRepository.findByMemberOrderBySortOrderAsc(member);

            if (profileImages.size() != 5) {
                log.warn("프로필 이미지가 5장이 아닙니다 - 회원 ID: {}, 이미지 수: {}",
                    member.getId(), profileImages.size());
                return;
            }

            // API 요청 데이터 구성
            FaceDetectionRequest request = FaceDetectionRequest.builder()
                .images(profileImages.stream()
                    .map(img -> FaceDetectionImageData.builder()
                        .imageIdx(img.getSortOrder())
                        .memberId(member.getId())
                        .image(img.getImageUrl())
                        .build())
                    .collect(Collectors.toList()))
                .build();

            // API 호출
            FaceDetectionApiResponse response = webClient
                .post()
                .uri(faceDetectionApiUrl)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FaceDetectionApiResponse.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            if (response != null && response.isResult()) {
                // 결과 처리
                response.getData().getResults().forEach(result -> {
                    profileImages.stream()
                        .filter(img -> img.getSortOrder().equals(result.getImageIdx()))
                        .findFirst()
                        .ifPresent(img -> {
                            img.setAiProcessed(result.isDetectFace());
                            profileImageRepository.save(img);
                            log.info("얼굴 인식 결과 업데이트 - 순서: {}, 결과: {}",
                                result.getImageIdx(), result.isDetectFace());
                        });
                });
            }

        } catch (Exception e) {
            log.error("얼굴 인식 처리 실패 - 회원 ID: {}", member.getId(), e);
        }
    }

    // DTO 클래스들
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FaceDetectionRequest {
        private List<FaceDetectionImageData> images;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FaceDetectionImageData {
        @JsonProperty("imageIdx")
        private Integer imageIdx;
        @JsonProperty("memberId")
        private Long memberId;
        @JsonProperty("image")
        private String image;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FaceDetectionApiResponse {
        private boolean result;
        private int code;
        private String message;
        private FaceDetectionData data;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FaceDetectionData {
        private List<FaceDetectionResultData> results;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FaceDetectionResultData {
        @JsonProperty("imageIdx")
        private Integer imageIdx;
        @JsonProperty("memberId")
        private Long memberId;
        @JsonProperty("image_url")
        private String imageUrl;
        @JsonProperty("detect_face")
        private boolean detectFace;
        @JsonProperty("error")
        private String error;
    }
}