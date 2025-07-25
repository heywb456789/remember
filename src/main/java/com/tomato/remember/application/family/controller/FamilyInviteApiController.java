package com.tomato.remember.application.family.controller;

import com.tomato.remember.application.family.dto.*;
import com.tomato.remember.application.family.service.FamilyInviteService;
import com.tomato.remember.application.security.MemberUserDetails;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.exception.APIException;
import com.tomato.remember.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 가족 초대 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/family/invite")
@RequiredArgsConstructor
public class FamilyInviteApiController {

    private final FamilyInviteService familyInviteService;

    /**
     * 가족 구성원 초대 발송
     * POST /api/family/invite
     */
    @PostMapping
    public ResponseDTO<FamilyInviteResponse> sendFamilyInvite(
            @Valid @RequestBody FamilyInviteRequest request,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("가족 구성원 초대 발송 API 요청 - 사용자: {}, 메모리얼: {}, 방법: {}, 연락처: {}",
                currentUser.getMember().getId(),
                request.getMemorialId(),
                request.getMethod(),
                StringUtil.maskContact(request.getContact()));

        try {
            // 서비스에서 비즈니스 로직 처리 - 예외 발생 시 적절한 ResponseStatus로 throw
            FamilyInviteResponse result = familyInviteService.sendFamilyInvite(currentUser.getMember(), request);

            // 방법에 따른 성공 응답 상태 결정
            ResponseStatus successStatus = determineSuccessStatus(request.getMethod());

            log.info("가족 구성원 초대 발송 API 완료 - 사용자: {}, 메모리얼: {}, 방법: {}",
                    currentUser.getMember().getId(),
                    request.getMemorialId(),
                    request.getMethod());

            return ResponseDTO.<FamilyInviteResponse>builder()
                    .status(successStatus)
                    .response(result)
                    .build();

        } catch (APIException e) {
            // APIException은 이미 적절한 ResponseStatus를 가지고 있으므로 재발생
            log.warn("가족 구성원 초대 발송 실패 - 사용자: {}, 메모리얼: {}, 에러: {}",
                    currentUser.getMember().getId(), request.getMemorialId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            // 예기치 못한 에러는 500으로 처리
            log.error("가족 구성원 초대 발송 중 예기치 못한 오류 - 사용자: {}, 메모리얼: {}",
                    currentUser.getMember().getId(), request.getMemorialId(), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * SMS 앱 실행 URL 생성
     * GET /api/family/invite/sms-url/{token}
     */
    @GetMapping("/sms-url/{token}")
    public ResponseDTO<String> createSmsUrl(@PathVariable String token) {
        log.info("SMS 앱 실행 URL 생성 API 요청 - 토큰: {}", StringUtil.maskToken(token));

        try {
            String smsUrl = familyInviteService.createSmsAppUrl(token);

            log.info("SMS 앱 실행 URL 생성 API 완료 - 토큰: {}", StringUtil.maskToken(token));

            return ResponseDTO.ok(smsUrl);

        } catch (APIException e) {
            log.warn("SMS 앱 실행 URL 생성 실패 - 토큰: {}, 에러: {}", StringUtil.maskToken(token), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("SMS 앱 실행 URL 생성 중 예기치 못한 오류 - 토큰: {}", StringUtil.maskToken(token), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 초대 토큰 처리 (수락)
     * POST /api/family/invite/process
     */
    @PostMapping("/process")
    public ResponseDTO<InviteProcessResponse> processInviteToken(
            @RequestBody TokenProcessRequest request,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        String token = request.getToken();
        log.info("초대 토큰 처리 API 요청 - 사용자: {}, 토큰: {}",
                currentUser.getMember().getId(), StringUtil.maskToken(token));

        try {
            if (StringUtil.isEmpty(token)) {
                throw new APIException("초대 토큰이 필요합니다.", ResponseStatus.BAD_REQUEST);
            }

            InviteProcessResponse result = familyInviteService.processInviteByToken(token, currentUser.getMember());

            log.info("초대 토큰 처리 API 완료 - 사용자: {}, 토큰: {}",
                    currentUser.getMember().getId(), StringUtil.maskToken(token));

            return ResponseDTO.<InviteProcessResponse>builder()
                    .status(ResponseStatus.FAMILY_INVITE_ACCEPTED)
                    .response(result)
                    .build();

        } catch (APIException e) {
            log.warn("초대 토큰 처리 실패 - 사용자: {}, 토큰: {}, 에러: {}",
                    currentUser.getMember().getId(), StringUtil.maskToken(token), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("초대 토큰 처리 중 예기치 못한 오류 - 사용자: {}, 토큰: {}",
                    currentUser.getMember().getId(), StringUtil.maskToken(token), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * SMS 앱 연동 데이터 조회
     * GET /api/family/invite/sms/{token}
     */
    @GetMapping("/sms/{token}")
    public ResponseDTO<SmsAppDataResponse> getSmsAppData(@PathVariable String token) {
        log.info("SMS 앱 연동 데이터 조회 API 요청 - 토큰: {}", StringUtil.maskToken(token));

        try {
            SmsAppDataResponse smsData = familyInviteService.getSmsAppData(token);

            log.info("SMS 앱 연동 데이터 조회 API 완료 - 토큰: {}, 메시지 길이: {}자",
                    StringUtil.maskToken(token), smsData.getMessageLength());

            return ResponseDTO.ok(smsData);

        } catch (APIException e) {
            log.warn("SMS 앱 데이터 조회 실패 - 토큰: {}, 에러: {}", StringUtil.maskToken(token), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("SMS 앱 연동 데이터 조회 중 예기치 못한 오류 - 토큰: {}", StringUtil.maskToken(token), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 초대 토큰 유효성 확인
     * GET /api/family/invite/validate/{token}
     */
    @GetMapping("/validate/{token}")
    public ResponseDTO<TokenValidationResponse> validateToken(@PathVariable String token) {
        log.info("초대 토큰 유효성 확인 API 요청 - 토큰: {}", StringUtil.maskToken(token));

        try {
            TokenValidationResponse validationResult = familyInviteService.validateToken(token);

            log.info("초대 토큰 유효성 확인 API 완료 - 토큰: {}, 유효성: {}",
                    StringUtil.maskToken(token), validationResult.getValid());

            return ResponseDTO.ok(validationResult);

        } catch (APIException e) {
            log.warn("초대 토큰 유효성 확인 실패 - 토큰: {}, 에러: {}", StringUtil.maskToken(token), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("초대 토큰 유효성 확인 중 예기치 못한 오류 - 토큰: {}", StringUtil.maskToken(token), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * SMS 앱 실행 테스트 (개발용)
     * POST /api/family/invite/test-sms
     */
    @PostMapping("/test-sms")
    public ResponseDTO<SmsTestResponse> testSmsApp(
            @RequestParam String phoneNumber,
            @RequestParam(required = false) String message,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("SMS 앱 실행 테스트 API 요청 - 사용자: {}, 전화번호: {}",
                currentUser.getMember().getId(), StringUtil.maskContact(phoneNumber));

        try {
            SmsTestResponse testResult = familyInviteService.createSmsTestData(phoneNumber, message);

            log.info("SMS 앱 실행 테스트 API 완료 - 사용자: {}, 전화번호: {}",
                    currentUser.getMember().getId(), StringUtil.maskContact(phoneNumber));

            return ResponseDTO.ok(testResult);

        } catch (APIException e) {
            log.warn("SMS 앱 실행 테스트 실패 - 사용자: {}, 전화번호: {}, 에러: {}",
                    currentUser.getMember().getId(), StringUtil.maskContact(phoneNumber), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("SMS 앱 실행 테스트 중 예기치 못한 오류 - 사용자: {}, 전화번호: {}",
                    currentUser.getMember().getId(), StringUtil.maskContact(phoneNumber), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 이메일 테스트 발송 (개발용)
     * POST /api/family/invite/test-email
     */
    @PostMapping("/test-email")
    public ResponseDTO<String> testEmail(
            @RequestParam String email,
            @AuthenticationPrincipal MemberUserDetails currentUser) {

        log.info("이메일 테스트 발송 API 요청 - 사용자: {}, 이메일: {}",
                currentUser.getMember().getId(), StringUtil.maskContact(email));

        try {
            // 이메일 유효성 검사
            if (!familyInviteService.canSendEmail(email)) {
                throw new APIException(ResponseStatus.FAMILY_INVITE_EMAIL_INVALID);
            }

            // 테스트 이메일 발송
            familyInviteService.sendTestEmail(email);

            log.info("이메일 테스트 발송 API 완료 - 사용자: {}, 이메일: {}",
                    currentUser.getMember().getId(), StringUtil.maskContact(email));

            return ResponseDTO.ok("테스트 이메일이 발송되었습니다.");

        } catch (APIException e) {
            log.warn("이메일 테스트 발송 실패 - 사용자: {}, 이메일: {}, 에러: {}",
                    currentUser.getMember().getId(), StringUtil.maskContact(email), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("이메일 테스트 발송 중 예기치 못한 오류 - 사용자: {}, 이메일: {}",
                    currentUser.getMember().getId(), StringUtil.maskContact(email), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 연락처 유효성 검사 (개발용)
     * GET /api/family/invite/validate-contact
     */
    @GetMapping("/validate-contact")
    public ResponseDTO<ContactValidationResponse> validateContact(
            @RequestParam String method,
            @RequestParam String contact) {

        log.info("연락처 유효성 검사 API 요청 - 방법: {}, 연락처: {}",
                method, StringUtil.maskContact(contact));

        try {
            boolean isValid = false;
            String message = "";

            if ("email".equals(method)) {
                isValid = familyInviteService.canSendEmail(contact);
                message = isValid ? "유효한 이메일 주소입니다." : "유효하지 않은 이메일 형식입니다.";
            } else if ("sms".equals(method)) {
                isValid = familyInviteService.isValidPhoneNumber(contact);
                message = isValid ? "유효한 전화번호입니다." : "유효하지 않은 전화번호 형식입니다.";

                // 전화번호인 경우 포맷팅된 버전도 제공
                if (isValid) {
                    String formatted = familyInviteService.formatPhoneNumber(contact);
                    message += " (형식: " + formatted + ")";
                }
            } else {
                throw new APIException(ResponseStatus.FAMILY_INVITE_METHOD_NOT_SUPPORTED);
            }

            ContactValidationResponse result = ContactValidationResponse.builder()
                    .method(method)
                    .contact(StringUtil.maskContact(contact))
                    .valid(isValid)
                    .message(message)
                    .build();

            log.info("연락처 유효성 검사 API 완료 - 방법: {}, 연락처: {}, 유효성: {}",
                    method, StringUtil.maskContact(contact), isValid);

            return ResponseDTO.ok(result);

        } catch (APIException e) {
            log.warn("연락처 유효성 검사 실패 - 방법: {}, 연락처: {}, 에러: {}",
                    method, StringUtil.maskContact(contact), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("연락처 유효성 검사 중 예기치 못한 오류 - 방법: {}, 연락처: {}",
                    method, StringUtil.maskContact(contact), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===== 헬퍼 메서드 =====

    /**
     * 초대 방법에 따른 성공 상태 결정
     */
    private ResponseStatus determineSuccessStatus(String method) {
        return switch (method) {
            case "email" -> ResponseStatus.FAMILY_INVITE_EMAIL_SENT;
            case "sms" -> ResponseStatus.FAMILY_INVITE_SMS_READY;
            default -> ResponseStatus.OK;
        };
    }

}