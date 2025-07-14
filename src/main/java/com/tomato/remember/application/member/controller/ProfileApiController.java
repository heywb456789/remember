package com.tomato.remember.application.member.controller;

import com.tomato.remember.application.member.dto.ProfileUpdateRequest;
import com.tomato.remember.application.member.dto.ProfileUpdateResponse;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.member.service.MemberProfileService;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.exception.APIException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 프로필 관리 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileApiController {

    private final MemberProfileService memberProfileService;

    /**
     * 통합 프로필 업데이트 API
     *
     * - 기본 정보만 수정
     * - 처음 사진 업로드 (5장 필수)
     * - 기존 사진 수정 (최종 5장 보장)
     */
    @PutMapping(value = "/update", consumes = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.MULTIPART_FORM_DATA_VALUE
    })
    public ResponseDTO<ProfileUpdateResponse> updateProfile(
        @AuthenticationPrincipal MemberUserDetails user,

        // 기본 정보
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String phoneNumber,
        @RequestParam(required = false) String birthDate,
        @RequestParam(required = false) String preferredLanguage,
        @RequestParam(required = false) Boolean marketingAgreed,

        // 알림 설정
        @RequestParam(required = false) Boolean pushNotification,
        @RequestParam(required = false) Boolean memorialNotification,
        @RequestParam(required = false) Boolean paymentNotification,
        @RequestParam(required = false) Boolean familyNotification,

        // 이미지 관련
        @RequestParam(value = "images", required = false) MultipartFile[] images,
        @RequestParam(value = "imagesToDelete", required = false) Integer[] imagesToDelete,
        @RequestParam(value = "imageOrders", required = false) Integer[] imageOrders
    ) {
        log.info("통합 프로필 업데이트 요청 - 이미지 개수: {}, 삭제할 이미지: {}",
            images != null ? images.length : 0,
            imagesToDelete != null ? imagesToDelete.length : 0);

        try {
            Member currentUser = user == null ? null : user.getMember();
            if (currentUser == null) {
                throw new APIException(ResponseStatus.UNAUTHORIZED);
            }

            // Request DTO 생성
            ProfileUpdateRequest request = buildProfileUpdateRequest(
                name, email, phoneNumber, birthDate, preferredLanguage, marketingAgreed,
                pushNotification, memorialNotification, paymentNotification, familyNotification,
                images, imagesToDelete, imageOrders
            );

            log.info("프로필 업데이트 요청 정보: {}", request);

            // 서비스 호출
            ProfileUpdateResponse response = memberProfileService.updateProfileUnified(
                currentUser.getId(), request
            );

            log.info("통합 프로필 업데이트 완료 - 사용자: {}, 최종 이미지 개수: {}",
                currentUser.getId(), response.getTotalImages());

            return ResponseDTO.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("프로필 업데이트 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(ResponseStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("프로필 업데이트 중 오류 발생", e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 회원 탈퇴 API
     */
    @DeleteMapping("/delete-account")
    public ResponseDTO<Void> deleteAccount(
        @AuthenticationPrincipal MemberUserDetails user
    ) {
        log.info("회원 탈퇴 요청");

        try {
            Member currentUser = user == null ? null : user.getMember();
            if (currentUser == null) {
                throw new APIException(ResponseStatus.UNAUTHORIZED);
            }

            memberProfileService.deleteAccount(currentUser.getId());

            log.info("회원 탈퇴 완료 - 사용자: {}", currentUser.getId());

            return ResponseDTO.ok();

        } catch (Exception e) {
            log.error("회원 탈퇴 중 오류 발생", e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 영상통화 가능 여부 확인 API
     */
    @GetMapping("/can-start-video-call")
    public ResponseDTO<Boolean> canStartVideoCall(
        @AuthenticationPrincipal MemberUserDetails user
    ) {
        try {
            Member currentUser = user == null ? null : user.getMember();
            if (currentUser == null) {
                throw new APIException(ResponseStatus.UNAUTHORIZED);
            }

            boolean canStart = memberProfileService.canStartVideoCall(currentUser.getId());

            return ResponseDTO.ok(canStart);

        } catch (Exception e) {
            log.error("영상통화 가능 여부 확인 중 오류 발생", e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===== 헬퍼 메서드 =====

    /**
     * ProfileUpdateRequest 객체 생성
     */
    private ProfileUpdateRequest buildProfileUpdateRequest(
        String name, String email, String phoneNumber, String birthDate, String preferredLanguage,
        Boolean marketingAgreed, Boolean pushNotification, Boolean memorialNotification,
        Boolean paymentNotification, Boolean familyNotification, MultipartFile[] images,
        Integer[] imagesToDelete, Integer[] imageOrders
    ) {
        ProfileUpdateRequest.ProfileUpdateRequestBuilder builder = ProfileUpdateRequest.builder();

        // 기본 정보 설정
        if (name != null && !name.trim().isEmpty()) {
            builder.name(name.trim());
        }

        if (email != null && !email.trim().isEmpty()) {
            builder.email(email.toLowerCase().trim());
        }

        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            builder.phoneNumber(phoneNumber.replaceAll("[-\\s]", ""));
        }

        // 생년월일 파싱
        if (birthDate != null && !birthDate.trim().isEmpty()) {
            try {
                LocalDate parsedDate = LocalDate.parse(birthDate, DateTimeFormatter.ISO_LOCAL_DATE);
                builder.birthDate(parsedDate);
            } catch (DateTimeParseException e) {
                log.warn("생년월일 파싱 실패: {}", birthDate);
                throw new IllegalArgumentException("올바른 생년월일 형식이 아닙니다. (YYYY-MM-DD)");
            }
        }

        // 설정 정보
        if (preferredLanguage != null && !preferredLanguage.trim().isEmpty()) {
            builder.preferredLanguage(preferredLanguage.toUpperCase());
        }

        builder.marketingAgreed(marketingAgreed);

        // 이미지 관련
        builder.images(images)
               .imagesToDelete(imagesToDelete)
               .imageOrders(imageOrders);

        return builder.build();
    }
}