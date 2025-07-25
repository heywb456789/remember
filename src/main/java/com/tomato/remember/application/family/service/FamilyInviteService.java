package com.tomato.remember.application.family.service;

import com.tomato.remember.application.family.dto.*;
import com.tomato.remember.application.family.entity.FamilyInviteToken;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.family.repository.FamilyInviteTokenRepository;
import com.tomato.remember.application.family.repository.FamilyMemberRepository;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.repository.MemorialRepository;
import com.tomato.remember.common.code.ResponseStatus;
import com.tomato.remember.common.exception.APIException;
import com.tomato.remember.common.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 가족 초대 처리 서비스 - 완전한 최종 리팩터링 버전
 * ResponseStatus 기반 일관된 예외 처리 + StringUtil + 기존 서비스 완전 호환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyInviteService {

    private final FamilyInviteTokenRepository inviteTokenRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final MemorialRepository memorialRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    /**
     * 가족 구성원 초대 발송
     */
    @Transactional
    public FamilyInviteResponse sendFamilyInvite(Member inviter, FamilyInviteRequest request) {
        log.info("가족 구성원 초대 발송 시작 - 초대자: {}, 메모리얼: {}, 연락처: {}",
                inviter.getId(), request.getMemorialId(), StringUtil.maskContact(request.getContact()));

        // 1. 메모리얼 조회 및 권한 확인
        Memorial memorial = validateMemorialAccess(inviter, request.getMemorialId());

        // 2. 연락처 유효성 검사
        validateContact(request.getMethod(), request.getContact());

        // 3. 자기 자신 초대 방지
        validateNotSelfInvite(inviter, request.getContact());

        // 4. 이미 가족 구성원인지 확인
        validateNotExistingFamilyMember(memorial, request.getContact());

        // 5. 기존 유효한 토큰 확인 및 처리
        FamilyInviteToken inviteToken = handleExistingOrCreateNewToken(memorial, inviter, request);

        // 6. 초대 발송 처리
        FamilyInviteResponse result = processInviteSending(inviteToken, request);

        log.info("가족 구성원 초대 발송 완료 - 토큰: {}, 메모리얼: {}, 연락처: {}",
                StringUtil.maskToken(inviteToken.getToken()),
                memorial.getId(),
                StringUtil.maskContact(request.getContact()));

        return result;
    }

    /**
     * 초대 토큰으로 초대 수락 처리
     */
    @Transactional
    public InviteProcessResponse processInviteByToken(String token, Member acceptingMember) {
        log.info("초대 토큰 처리 시작 - 토큰: {}, 수락자: {}",
                StringUtil.maskToken(token), acceptingMember.getId());

        // 1. 토큰 유효성 검증
        FamilyInviteToken inviteToken = validateAndGetToken(token);

        // 2. 자기 자신 초대 방지 검사
        validateNotSelfAcceptance(inviteToken, acceptingMember);

        // 3. 중복 가족 구성원 확인
        validateNotDuplicateMember(inviteToken.getMemorial(), acceptingMember);

        // 4. 초대 수락 처리
        inviteToken.accept(acceptingMember);
        inviteTokenRepository.save(inviteToken);

        // 5. 가족 구성원 등록
        FamilyMember familyMember = createFamilyMemberFromToken(inviteToken, acceptingMember);
        familyMemberRepository.save(familyMember);

        log.info("초대 토큰 처리 완료 - 토큰: {}, 수락자: {}, 메모리얼: {}",
                StringUtil.maskToken(token),
                acceptingMember.getId(),
                inviteToken.getMemorial().getId());

        return InviteProcessResponse.builder()
            .memorialName(inviteToken.getMemorialName())
            .inviterName(inviteToken.getInviterName())
            .relationshipDisplayName(inviteToken.getRelationshipDisplayName())
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * SMS 앱 연동 데이터 조회
     */
    public SmsAppDataResponse getSmsAppData(String token) {
        log.info("SMS 앱 연동 데이터 조회 - 토큰: {}", StringUtil.maskToken(token));

        // 토큰 조회
        FamilyInviteToken inviteToken = inviteTokenRepository.findByToken(token)
                .orElseThrow(() -> new APIException(ResponseStatus.FAMILY_INVITE_TOKEN_INVALID));

        // SMS 방식 확인
        if (!inviteToken.isSmsMethod()) {
            throw new APIException(ResponseStatus.FAMILY_INVITE_NOT_SMS_METHOD);
        }

        // SMS 데이터 생성 (기존 SmsService 메서드 활용)
        return smsService.createSmsAppDataResponse(inviteToken);
    }

    /**
     * SMS 앱 실행 URL 생성
     */
    public String createSmsAppUrl(String token) {
        log.info("SMS 앱 실행 URL 생성 - 토큰: {}", StringUtil.maskToken(token));

        // 토큰 조회
        FamilyInviteToken inviteToken = inviteTokenRepository.findByToken(token)
                .orElseThrow(() -> new APIException(ResponseStatus.FAMILY_INVITE_TOKEN_INVALID));

        // SMS 방식 확인
        if (!inviteToken.isSmsMethod()) {
            throw new APIException(ResponseStatus.FAMILY_INVITE_NOT_SMS_METHOD);
        }

        return smsService.createSmsAppUrl(inviteToken);
    }

    /**
     * 초대 토큰 유효성 확인
     */
    public TokenValidationResponse validateToken(String token) {
        log.info("초대 토큰 유효성 확인 - 토큰: {}", StringUtil.maskToken(token));

        Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .expired(false)
                    .status("NOT_FOUND")
                    .remainingHours(0L)
                    .expiresAt("알 수 없음")
                    .build();
        }

        FamilyInviteToken inviteToken = tokenOpt.get();
        boolean isValid = inviteToken.isUsable();
        boolean isExpired = inviteToken.isExpired();

        return TokenValidationResponse.builder()
                .valid(isValid)
                .expired(isExpired)
                .status(inviteToken.getStatus().name())
                .remainingHours(inviteToken.getRemainingHours())
                .expiresAt(inviteToken.getExpiresAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }

    /**
     * SMS 테스트 데이터 생성
     */
    public SmsTestResponse createSmsTestData(String phoneNumber, String message) {
        log.info("SMS 테스트 데이터 생성 - 전화번호: {}", StringUtil.maskContact(phoneNumber));

        // 전화번호 유효성 검사
        if (!smsService.isValidPhoneNumber(phoneNumber)) {
            throw new APIException(ResponseStatus.FAMILY_INVITE_PHONE_INVALID);
        }

        // 기본 테스트 메시지
        String testMessage = StringUtil.isNotEmpty(message) ? message : "[토마토리멤버] 테스트 메시지입니다.";

        // SMS URL 생성
        String smsUrl = smsService.createTestSmsUrl(phoneNumber, testMessage);

        // 길이 정보 생성
        var lengthInfo = smsService.getTextLengthInfo(testMessage);

        return SmsTestResponse.builder()
                .maskedPhoneNumber(StringUtil.maskContact(phoneNumber))
                .smsUrl(smsUrl)
                .testMessage(testMessage)
                .messageLength(testMessage.length())
                .lengthInfo(SmsAppDataResponse.TextLengthInfo.builder()
                        .length((Integer) lengthInfo.get("length"))
                        .isTooLong((Boolean) lengthInfo.get("isTooLong"))
                        .maxLength((Integer) lengthInfo.get("maxLength"))
                        .remaining((Integer) lengthInfo.get("remaining"))
                        .build())
                .build();
    }

    // ===== 검증 메서드들 =====

    /**
     * 메모리얼 접근 권한 검증
     */
    private Memorial validateMemorialAccess(Member inviter, Long memorialId) {
        Memorial memorial = memorialRepository.findByIdAndOwner(memorialId, inviter)
                .orElseThrow(() -> new APIException(ResponseStatus.MEMORIAL_ACCESS_DENIED));

        if (!memorial.isActive()) {
            throw new APIException(ResponseStatus.MEMORIAL_STATUS_INVALID);
        }

        return memorial;
    }

    /**
     * 연락처 유효성 검사
     */
    private void validateContact(String method, String contact) {
        if ("email".equals(method)) {
            if (!emailService.isValidEmail(contact)) {
                throw new APIException(ResponseStatus.FAMILY_INVITE_EMAIL_INVALID);
            }
        } else if ("sms".equals(method)) {
            if (!smsService.isValidPhoneNumber(contact)) {
                throw new APIException(ResponseStatus.FAMILY_INVITE_PHONE_INVALID);
            }
        } else {
            throw new APIException(ResponseStatus.FAMILY_INVITE_METHOD_NOT_SUPPORTED);
        }
    }

    /**
     * 자기 자신 초대 방지
     */
    private void validateNotSelfInvite(Member inviter, String contact) {
        if (isSelfInvite(inviter, contact)) {
            throw new APIException(ResponseStatus.FAMILY_INVITE_SELF_INVITE);
        }
    }

    /**
     * 기존 가족 구성원 확인
     */
    private void validateNotExistingFamilyMember(Memorial memorial, String contact) {
        List<FamilyMember> existingMembers = familyMemberRepository.findByContact(contact, contact);

        boolean isExistingMember = existingMembers.stream()
                .anyMatch(fm -> fm.getMemorial().getId().equals(memorial.getId()));

        if (isExistingMember) {
            throw new APIException(ResponseStatus.FAMILY_INVITE_ALREADY_MEMBER);
        }
    }

    /**
     * 토큰 유효성 검증 및 조회
     */
    private FamilyInviteToken validateAndGetToken(String token) {
        Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findUsableToken(token, LocalDateTime.now());

        if (tokenOpt.isEmpty()) {
            throw new APIException(ResponseStatus.FAMILY_INVITE_TOKEN_INVALID);
        }

        return tokenOpt.get();
    }

    /**
     * 자기 자신 수락 방지
     */
    private void validateNotSelfAcceptance(FamilyInviteToken inviteToken, Member acceptingMember) {
        if (inviteToken.getInviter().getId().equals(acceptingMember.getId())) {
            throw new APIException(ResponseStatus.FAMILY_INVITE_SELF_INVITE);
        }

        if (inviteToken.getMemorial().getOwner().getId().equals(acceptingMember.getId())) {
            throw new APIException(ResponseStatus.FAMILY_INVITE_OWNER_SELF_INVITE);
        }
    }

    /**
     * 중복 가족 구성원 확인
     */
    private void validateNotDuplicateMember(Memorial memorial, Member member) {
        if (familyMemberRepository.existsByMemorialAndMember(memorial, member)) {
            throw new APIException(ResponseStatus.FAMILY_INVITE_ALREADY_MEMBER);
        }
    }

    // ===== 비즈니스 로직 메서드들 =====

    /**
     * 기존 토큰 처리 또는 새 토큰 생성
     */
    private FamilyInviteToken handleExistingOrCreateNewToken(Memorial memorial, Member inviter, FamilyInviteRequest request) {
        String contact = request.getContact();

        // 현재 유효한 토큰 조회
        List<FamilyInviteToken> pendingTokens = inviteTokenRepository
                .findPendingTokensByMemorialAndContact(memorial, contact, LocalDateTime.now());

        if (!pendingTokens.isEmpty()) {
            // 기존 유효한 토큰 재사용
            FamilyInviteToken existingToken = pendingTokens.get(0);

            log.info("기존 유효한 초대 토큰 재사용 - 토큰: {}", StringUtil.maskToken(existingToken.getToken()));

            // 메시지 업데이트
            if (StringUtil.isNotEmpty(request.getMessage())) {
                existingToken.updateInviteMessage(request.getMessage());
                inviteTokenRepository.save(existingToken);
            }

            return existingToken;
        }

        // 새로운 토큰 생성
        FamilyInviteToken newToken = FamilyInviteToken.createInviteToken(
                memorial,
                inviter,
                request.getRelationship(),
                request.getMethod(),
                contact,
                request.getMessage()
        );

        return inviteTokenRepository.save(newToken);
    }

    /**
     * 초대 발송 처리
     */
    private FamilyInviteResponse processInviteSending(FamilyInviteToken inviteToken, FamilyInviteRequest request) {
        try {
            if (inviteToken.isEmailMethod()) {
                return processEmailInvite(inviteToken, request);
            } else if (inviteToken.isSmsMethod()) {
                return processSmsInvite(inviteToken, request);
            } else {
                throw new APIException(ResponseStatus.FAMILY_INVITE_METHOD_NOT_SUPPORTED);
            }
        } catch (APIException e) {
            // APIException은 그대로 전파
            throw e;
        } catch (Exception e) {
            // 예기치 못한 에러는 500으로 래핑
            log.error("초대 발송 처리 중 예기치 못한 오류 - 토큰: {}", StringUtil.maskToken(inviteToken.getToken()), e);
            throw new APIException(ResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 이메일 초대 처리
     */
    private FamilyInviteResponse processEmailInvite(FamilyInviteToken inviteToken, FamilyInviteRequest request) {
        try {
            // 기존 EmailService 메서드 활용
            emailService.sendFamilyInviteEmail(inviteToken);

            return FamilyInviteResponse.forEmail(
                    StringUtil.maskContact(inviteToken.getContact()),
                    request.getMemorialId(),
                    request.getRelationship().getDisplayName()
            );

        } catch (APIException e) {
            // APIException은 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("이메일 초대 발송 실패 - 토큰: {}", StringUtil.maskToken(inviteToken.getToken()), e);
            throw new APIException(ResponseStatus.FAMILY_INVITE_EMAIL_FAILED);
        }
    }

    /**
     * SMS 초대 처리
     */
    private FamilyInviteResponse processSmsInvite(FamilyInviteToken inviteToken, FamilyInviteRequest request) {
        try {
            // 기존 SmsService 메서드 활용하여 SMS 앱 연동 데이터 생성
            SmsAppDataResponse smsData = smsService.createSmsAppDataResponse(inviteToken);

            return FamilyInviteResponse.forSms(
                    StringUtil.maskContact(inviteToken.getContact()),
                    request.getMemorialId(),
                    request.getRelationship().getDisplayName(),
                    inviteToken.getToken(),
                    smsData.getSmsUrl(),
                    smsData.getShortSmsUrl(),
                    smsData.getMessageLength(),
                    smsData.getShortMessageLength(),
                    smsData.getRecommendShort()
            );

        } catch (APIException e) {
            // APIException은 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("SMS 초대 데이터 생성 실패 - 토큰: {}", StringUtil.maskToken(inviteToken.getToken()), e);
            throw new APIException(ResponseStatus.FAMILY_INVITE_SMS_FAILED);
        }
    }

    /**
     * 초대 토큰에서 FamilyMember 생성
     */
    private FamilyMember createFamilyMemberFromToken(FamilyInviteToken inviteToken, Member acceptingMember) {
        return FamilyMember.builder()
                .memorial(inviteToken.getMemorial())
                .member(acceptingMember)
                .invitedBy(inviteToken.getInviter())
                .relationship(inviteToken.getRelationship())
                .inviteMessage(inviteToken.getInviteMessage())
                .inviteStatus(com.tomato.remember.application.family.code.InviteStatus.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .memorialAccess(false)  // 기본적으로 권한 없음
                .videoCallAccess(false) // 기본적으로 권한 없음
                .lastAccessAt(LocalDateTime.now())
                .build();
    }

    // ===== 유틸리티 메서드들 =====

    /**
     * 자기 자신 초대 여부 확인
     */
    private boolean isSelfInvite(Member inviter, String contact) {
        // 이메일 확인
        if (contact.contains("@")) {
            return contact.equals(inviter.getEmail());
        }

        // 전화번호 확인
        String cleanContact = contact.replaceAll("[^0-9]", "");
        String cleanInviterPhone = inviter.getPhoneNumber() != null ?
                inviter.getPhoneNumber().replaceAll("[^0-9]", "") : "";

        return cleanContact.equals(cleanInviterPhone);
    }

    /**
     * 만료된 토큰 정리 (스케줄러에서 호출)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("만료된 초대 토큰 정리 시작");

        try {
            List<FamilyInviteToken> expiredTokens = inviteTokenRepository.findExpiredTokens(LocalDateTime.now());

            int processedCount = 0;
            for (FamilyInviteToken token : expiredTokens) {
                token.expire();
                processedCount++;
            }

            if (processedCount > 0) {
                inviteTokenRepository.saveAll(expiredTokens);
            }

            log.info("만료된 초대 토큰 정리 완료 - 처리된 토큰 수: {}", processedCount);

        } catch (Exception e) {
            log.error("만료된 토큰 정리 중 오류 발생", e);
            // 스케줄러에서 호출되므로 예외를 던지지 않고 로그만 남김
        }
    }

    // ===== 백워드 호환성을 위한 추가 메서드들 =====

    /**
     * 이메일 발송 가능 여부 확인 (EmailService 위임)
     */
    public boolean canSendEmail(String email) {
        return emailService.isValidEmail(email);
    }

    /**
     * 전화번호 유효성 확인 (SmsService 위임)
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        return smsService.isValidPhoneNumber(phoneNumber);
    }

    /**
     * 전화번호 포맷팅 (SmsService 위임)
     */
    public String formatPhoneNumber(String phoneNumber) {
        return smsService.formatPhoneNumber(phoneNumber);
    }

    /**
     * SMS 텍스트 길이 체크 (SmsService 위임)
     */
    public boolean isTextTooLong(String text) {
        return smsService.isTextTooLong(text);
    }

    /**
     * 테스트 이메일 발송 (EmailService 위임)
     */
    public void sendTestEmail(String to) {
        emailService.sendTestEmail(to);
    }

    /**
     * SMS 텍스트 길이 정보 반환 (SmsService 위임)
     */
    public java.util.Map<String, Object> getTextLengthInfo(String text) {
        return smsService.getTextLengthInfo(text);
    }

    // ===== 레거시 호환 메서드들 (기존 코드와의 호환성) =====

    /**
     * 토큰 유효성 확인 (boolean 반환 - 레거시 호환)
     */
    public boolean isTokenValid(String token) {
        Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findUsableToken(token, LocalDateTime.now());
        return tokenOpt.isPresent();
    }

    /**
     * SMS 앱 연동 데이터 조회 (Map 반환 - 레거시 호환)
     */
    public java.util.Map<String, Object> getSmsAppDataAsMap(String token) {
        log.info("SMS 앱 연동 데이터 조회 (Map) - 토큰: {}", StringUtil.maskToken(token));

        Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return java.util.Map.of("error", "토큰을 찾을 수 없습니다.");
        }

        FamilyInviteToken inviteToken = tokenOpt.get();

        if (!inviteToken.isSmsMethod()) {
            return java.util.Map.of("error", "SMS 방식이 아닙니다.");
        }

        return smsService.createSmsAppData(inviteToken);
    }

    /**
     * 기존 Map 기반 초대 발송 (레거시 호환)
     */
    @Transactional
    public java.util.Map<String, Object> sendFamilyInviteAsMap(Member inviter, FamilyInviteRequest request) {
        try {
            FamilyInviteResponse response = sendFamilyInvite(inviter, request);

            // DTO를 Map으로 변환
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("success", true);
            result.put("method", response.getMethod());
            result.put("contact", response.getMaskedContact());
            result.put("memorialId", response.getMemorialId());
            result.put("relationshipDisplayName", response.getRelationshipDisplayName());
            result.put("timestamp", response.getTimestamp());

            if (response.getToken() != null) {
                result.put("token", response.getToken());
            }
            if (response.getSmsUrl() != null) {
                result.put("smsUrl", response.getSmsUrl());
            }
            if (response.getShortSmsUrl() != null) {
                result.put("shortSmsUrl", response.getShortSmsUrl());
            }
            if (response.getMessageLength() != null) {
                result.put("messageLength", response.getMessageLength());
            }
            if (response.getRecommendShort() != null) {
                result.put("recommendShort", response.getRecommendShort());
            }

            return result;

        } catch (Exception e) {
            log.error("레거시 Map 기반 초대 발송 실패", e);
            java.util.Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * 기존 String 기반 초대 처리 (레거시 호환)
     */
    @Transactional
    public String processInviteByTokenAsString(String token, Member acceptingMember) {
        try {
            InviteProcessResponse response = processInviteByToken(token, acceptingMember);
            return String.format("%s 메모리얼에 가족 구성원으로 등록되었습니다.", response.getMemorialName());
        } catch (Exception e) {
            log.error("레거시 String 기반 초대 처리 실패", e);
            throw e;
        }
    }
}