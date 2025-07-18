package com.tomato.remember.application.family.controller;

import com.tomato.remember.application.family.dto.FamilyInviteRequest;
import com.tomato.remember.application.family.service.FamilyInviteService;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.exception.APIException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 가족 초대 API 컨트롤러 - SMS 앱 연동 강화 버전
 */
@Slf4j
@RestController
@RequestMapping("/api/family/invite")
@RequiredArgsConstructor
public class FamilyInviteApiController {

    private final FamilyInviteService familyInviteService;

    /**
     * 가족 구성원 초대 발송 - SMS 정보 포함한 응답
     * POST /api/family/invite
     */
    @PostMapping
    public ResponseDTO<Map<String, Object>> sendFamilyInvite(
            @Valid @RequestBody FamilyInviteRequest request,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("가족 구성원 초대 발송 API 요청 - 사용자: {}, 메모리얼: {}, 방법: {}, 연락처: {}",
                currentUser.getMember().getId(),
                request.getMemorialId(),
                request.getMethod(),
                maskContact(request.getContact()));

        try {
            // 초대 발송 처리 - SMS 정보 포함한 응답 받기
            Map<String, Object> result = familyInviteService.sendFamilyInvite(currentUser.getMember(), request);

            // 추가 정보 포함
            result.put("memorialId", request.getMemorialId());
            result.put("relationship", request.getRelationship().getDisplayName());
            result.put("timestamp", System.currentTimeMillis());

            log.info("가족 구성원 초대 발송 API 완료 - 사용자: {}, 메모리얼: {}, 방법: {}, 성공: {}",
                    currentUser.getMember().getId(),
                    request.getMemorialId(),
                    request.getMethod(),
                    result.get("success"));

            return ResponseDTO.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("가족 구성원 초대 발송 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(e.getMessage(), ResponseStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("가족 구성원 초대 발송 실패 - 사용자: {}, 메모리얼: {}",
                    currentUser.getMember().getId(), request.getMemorialId(), e);
            throw new APIException("초대 발송 중 오류가 발생했습니다.", ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * SMS 앱 실행 URL 생성 (추가 API)
     * GET /api/family/invite/sms-url/{token}
     */
    @GetMapping("/sms-url/{token}")
    public ResponseDTO<Map<String, Object>> createSmsUrl(@PathVariable String token) {
        log.info("SMS 앱 실행 URL 생성 API 요청 - 토큰: {}", token.substring(0, 8) + "...");

        try {
            String smsUrl = familyInviteService.createSmsAppUrl(token);

            Map<String, Object> result = Map.of(
                    "smsUrl", smsUrl,
                    "success", true,
                    "message", "SMS 앱 실행 URL이 생성되었습니다."
            );

            log.info("SMS 앱 실행 URL 생성 API 완료 - 토큰: {}", token.substring(0, 8) + "...");

            return ResponseDTO.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("SMS 앱 실행 URL 생성 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(e.getMessage(), ResponseStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("SMS 앱 실행 URL 생성 실패 - 토큰: {}", token.substring(0, 8) + "...", e);
            throw new APIException("SMS 앱 실행 URL 생성 중 오류가 발생했습니다.", ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 초대 토큰 처리 API
     * POST /api/family/invite/process
     */
    @PostMapping("/process")
    public ResponseDTO<Map<String, Object>> processInviteToken(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        String token = request.get("token");

        log.info("초대 토큰 처리 API 요청 - 사용자: {}, 토큰: {}",
                currentUser.getMember().getId(),
                token != null ? token.substring(0, 8) + "..." : "null");

        try {
            if (token == null || token.trim().isEmpty()) {
                throw new IllegalArgumentException("초대 토큰이 필요합니다.");
            }

            // 초대 토큰 처리
            String result = familyInviteService.processInviteByToken(token, currentUser.getMember());

            // 성공 응답
            Map<String, Object> responseData = Map.of(
                    "success", true,
                    "message", result,
                    "timestamp", System.currentTimeMillis()
            );

            log.info("초대 토큰 처리 API 완료 - 사용자: {}, 토큰: {}",
                    currentUser.getMember().getId(),
                    token.substring(0, 8) + "...");

            return ResponseDTO.ok(responseData);

        } catch (IllegalArgumentException e) {
            log.warn("초대 토큰 처리 실패 - 잘못된 요청: {}", e.getMessage());
            throw new APIException(e.getMessage(), ResponseStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("초대 토큰 처리 실패 - 사용자: {}, 토큰: {}",
                    currentUser.getMember().getId(),
                    token != null ? token.substring(0, 8) + "..." : "null", e);
            throw new APIException("초대 처리 중 오류가 발생했습니다.", ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * SMS 앱 연동 데이터 조회
     * GET /api/family/invite/sms/{token}
     */
    @GetMapping("/sms/{token}")
    public ResponseDTO<Map<String, Object>> getSmsAppData(@PathVariable String token) {

        log.info("SMS 앱 연동 데이터 조회 API 요청 - 토큰: {}", token.substring(0, 8) + "...");

        try {
            Map<String, Object> smsData = familyInviteService.getSmsAppData(token);

            if (smsData.containsKey("error")) {
                log.warn("SMS 앱 데이터 조회 실패 - 토큰: {}, 오류: {}",
                        token.substring(0, 8) + "...", smsData.get("error"));
                throw new APIException(ResponseStatus.BAD_REQUEST);
            }

            log.info("SMS 앱 연동 데이터 조회 API 완료 - 토큰: {}, 메시지 길이: {}자",
                    token.substring(0, 8) + "...", smsData.get("messageLength"));

            return ResponseDTO.ok(smsData);

        } catch (APIException e) {
            throw e;
        } catch (Exception e) {
            log.error("SMS 앱 연동 데이터 조회 실패 - 토큰: {}", token.substring(0, 8) + "...", e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 초대 토큰 유효성 확인
     * GET /api/family/invite/validate/{token}
     */
    @GetMapping("/validate/{token}")
    public ResponseDTO<Map<String, Object>> validateToken(@PathVariable String token) {

        log.info("초대 토큰 유효성 확인 API 요청 - 토큰: {}", token.substring(0, 8) + "...");

        try {
            boolean isValid = familyInviteService.isTokenValid(token);

            Map<String, Object> responseData = Map.of(
                    "valid", isValid,
                    "message", isValid ? "유효한 토큰입니다." : "유효하지 않거나 만료된 토큰입니다."
            );

            log.info("초대 토큰 유효성 확인 API 완료 - 토큰: {}, 유효성: {}",
                    token.substring(0, 8) + "...", isValid);

            return ResponseDTO.ok(responseData);

        } catch (Exception e) {
            log.error("초대 토큰 유효성 확인 실패 - 토큰: {}", token.substring(0, 8) + "...", e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * SMS 앱 실행 테스트 (개발용)
     * POST /api/family/invite/test-sms
     */
    @PostMapping("/test-sms")
    public ResponseDTO<Map<String, Object>> testSmsApp(
            @RequestParam String phoneNumber,
            @RequestParam(required = false) String message,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("SMS 앱 실행 테스트 API 요청 - 사용자: {}, 전화번호: {}",
                currentUser.getMember().getId(), maskContact(phoneNumber));

        try {
            // 기본 테스트 메시지
            String testMessage = message != null ? message : "[토마토리멤버] 테스트 메시지입니다.";

            // 전화번호 정리
            String cleanPhoneNumber = phoneNumber.replaceAll("[^0-9]", "");

            // SMS URL 생성
            String smsUrl = String.format("sms:%s?body=%s",
                    cleanPhoneNumber,
                    java.net.URLEncoder.encode(testMessage, java.nio.charset.StandardCharsets.UTF_8));

            Map<String, Object> responseData = Map.of(
                    "success", true,
                    "message", "SMS 앱 실행 테스트 준비 완료",
                    "phoneNumber", maskContact(phoneNumber),
                    "smsUrl", smsUrl,
                    "testMessage", testMessage,
                    "messageLength", testMessage.length()
            );

            log.info("SMS 앱 실행 테스트 API 완료 - 사용자: {}, 전화번호: {}",
                    currentUser.getMember().getId(), maskContact(phoneNumber));

            return ResponseDTO.ok(responseData);

        } catch (Exception e) {
            log.error("SMS 앱 실행 테스트 실패 - 사용자: {}, 전화번호: {}",
                    currentUser.getMember().getId(), maskContact(phoneNumber), e);
            throw new APIException("SMS 앱 실행 테스트 중 오류가 발생했습니다.", ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===== 유틸리티 메서드 =====

    /**
     * 연락처 마스킹
     */
    private String maskContact(String contact) {
        if (contact == null || contact.length() < 4) {
            return "****";
        }

        if (contact.contains("@")) {
            // 이메일 마스킹
            int atIndex = contact.indexOf('@');
            if (atIndex > 2) {
                return contact.substring(0, 2) + "**" + contact.substring(atIndex);
            }
            return "**" + contact.substring(atIndex);
        } else {
            // 전화번호 마스킹
            return contact.substring(0, 3) + "****" + contact.substring(contact.length() - 4);
        }
    }
}