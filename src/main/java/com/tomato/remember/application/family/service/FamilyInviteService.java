package com.tomato.remember.application.family.service;

import com.tomato.remember.application.family.dto.FamilyInviteRequest;
import com.tomato.remember.application.family.entity.FamilyInviteToken;
import com.tomato.remember.application.family.entity.FamilyMember;
import com.tomato.remember.application.family.repository.FamilyInviteTokenRepository;
import com.tomato.remember.application.family.repository.FamilyMemberRepository;
import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.application.memorial.entity.Memorial;
import com.tomato.remember.application.memorial.repository.MemorialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 가족 초대 처리 서비스 - 트랜잭션 처리 개선 버전
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
     * 가족 구성원 초대 발송 - 트랜잭션 처리 개선
     */
    @Transactional
    public Map<String, Object> sendFamilyInvite(Member inviter, FamilyInviteRequest request) {
        log.info("가족 구성원 초대 발송 시작 - 초대자: {}, 메모리얼: {}, 연락처: {}",
                inviter.getId(), request.getMemorialId(), maskContact(request.getContact()));

        try {
            // 1. 메모리얼 조회 및 권한 확인
            Memorial memorial = validateMemorialAccess(inviter, request.getMemorialId());

            // 2. 연락처 유효성 검사
            validateContact(request.getMethod(), request.getContact());

            // 3. 자기 자신 초대 방지
            if (isSelfInvite(inviter, request.getContact())) {
                log.warn("자기 자신 초대 시도 차단 - 초대자: {}, 연락처: {}",
                        inviter.getId(), maskContact(request.getContact()));
                throw new IllegalArgumentException("자기 자신을 초대할 수 없습니다.");
            }

            // 4. 이미 가족 구성원인지 확인
            checkExistingFamilyMember(memorial, request.getContact());

            // 5. 기존 유효한 토큰 확인 및 처리
            FamilyInviteToken inviteToken = handleExistingOrCreateNewToken(memorial, inviter, request);

            // 6. 초대 발송 처리 (실패 시 토큰 삭제)
            Map<String, Object> result = processInviteSendingWithCleanup(inviteToken);

            log.info("가족 구성원 초대 발송 완료 - 토큰: {}, 메모리얼: {}, 연락처: {}",
                    inviteToken.getToken().substring(0, 8) + "...",
                    memorial.getId(),
                    maskContact(request.getContact()));

            return result;

        } catch (Exception e) {
            log.error("가족 구성원 초대 발송 실패 - 초대자: {}, 메모리얼: {}",
                    inviter.getId(), request.getMemorialId(), e);
            throw e;
        }
    }

    /**
     * 기존 토큰 처리 또는 새 토큰 생성
     */
    private FamilyInviteToken handleExistingOrCreateNewToken(Memorial memorial, Member inviter, FamilyInviteRequest request) {
        String contact = request.getContact();

        log.info("기존 토큰 확인 - 메모리얼: {}, 연락처: {}",
                memorial.getId(), maskContact(contact));

        // 1. 현재 유효한 (PENDING + 만료되지 않은) 토큰 조회
        List<FamilyInviteToken> pendingTokens = inviteTokenRepository
                .findPendingTokensByMemorialAndContact(memorial, contact, LocalDateTime.now());

        if (!pendingTokens.isEmpty()) {
            // 기존 유효한 토큰이 있으면 재사용
            FamilyInviteToken existingToken = pendingTokens.get(0);

            log.info("기존 유효한 초대 토큰 재사용 - 토큰: {}, 생성일: {}, 만료일: {}",
                    existingToken.getToken().substring(0, 8) + "...",
                    existingToken.getCreatedAt(),
                    existingToken.getExpiresAt());

            // 메시지 업데이트 (새로운 메시지로 덮어쓰기)
            if (request.getMessage() != null && !request.getMessage().trim().isEmpty()) {
                existingToken.updateInviteMessage(request.getMessage());
                inviteTokenRepository.save(existingToken);
                log.info("초대 메시지 업데이트 완료 - 토큰: {}", existingToken.getToken().substring(0, 8) + "...");
            }

            return existingToken;
        }

        // 2. 만료된 토큰이나 실패한 토큰 정리
        cleanupOldTokens(memorial, contact);

        // 3. 새로운 토큰 생성 (아직 DB에 저장하지 않음)
        FamilyInviteToken newToken = FamilyInviteToken.createInviteToken(
                memorial,
                inviter,
                request.getRelationship(),
                request.getMethod(),
                contact,
                request.getMessage()
        );

        log.info("새로운 초대 토큰 생성 준비 - 토큰: {}, 만료일: {}",
                newToken.getToken().substring(0, 8) + "...",
                newToken.getExpiresAt());

        return newToken;
    }

    /**
     * 초대 발송 처리 (실패 시 토큰 정리)
     */
    private Map<String, Object> processInviteSendingWithCleanup(FamilyInviteToken inviteToken) {
        try {
            // 1. 먼저 발송 테스트 (실제 발송 전 검증)
            if (inviteToken.isEmailMethod()) {
                validateEmailSending(inviteToken);
            } else if (inviteToken.isSmsMethod()) {
                validateSmsSending(inviteToken);
            }

            // 2. 검증 통과 시 토큰 DB 저장
            FamilyInviteToken savedToken = inviteTokenRepository.save(inviteToken);
            log.info("초대 토큰 DB 저장 완료 - 토큰: {}", savedToken.getToken().substring(0, 8) + "...");

            // 3. 실제 발송 처리
            Map<String, Object> result = processInviteSending(savedToken);

            log.info("초대 발송 성공 - 토큰: {}, 방법: {}",
                    savedToken.getToken().substring(0, 8) + "...", savedToken.getMethod());

            return result;

        } catch (Exception e) {
            log.error("초대 발송 실패 - 토큰: {}, 방법: {}",
                    inviteToken.getToken().substring(0, 8) + "...", inviteToken.getMethod(), e);

            // 실패 시 토큰이 이미 저장되어 있다면 삭제
            try {
                if (inviteToken.getId() != null) {
                    inviteTokenRepository.delete(inviteToken);
                    log.info("실패한 초대 토큰 삭제 완료 - 토큰: {}", inviteToken.getToken().substring(0, 8) + "...");
                }
            } catch (Exception deleteError) {
                log.error("실패한 토큰 삭제 중 오류 - 토큰: {}", inviteToken.getToken().substring(0, 8) + "...", deleteError);
            }

            throw e;
        }
    }

    /**
     * 이메일 발송 검증
     */
    private void validateEmailSending(FamilyInviteToken inviteToken) {
        if (!emailService.canSendEmail(inviteToken.getContact())) {
            throw new IllegalArgumentException("이메일 발송이 불가능한 주소입니다.");
        }
    }

    /**
     * SMS 발송 검증
     */
    private void validateSmsSending(FamilyInviteToken inviteToken) {
        if (!smsService.isValidPhoneNumber(inviteToken.getContact())) {
            throw new IllegalArgumentException("SMS 발송이 불가능한 전화번호입니다.");
        }
    }

    /**
     * 초대 발송 처리
     */
    private Map<String, Object> processInviteSending(FamilyInviteToken inviteToken) {
        if (inviteToken.isEmailMethod()) {
            return processEmailInvite(inviteToken);
        } else if (inviteToken.isSmsMethod()) {
            return processSmsInvite(inviteToken);
        } else {
            throw new IllegalArgumentException("지원하지 않는 초대 방법입니다.");
        }
    }

    /**
     * 이메일 초대 처리
     */
    private Map<String, Object> processEmailInvite(FamilyInviteToken inviteToken) {
        Map<String, Object> result = new HashMap<>();

        try {
            emailService.sendFamilyInviteEmail(inviteToken);

            result.put("success", true);
            result.put("message", "이메일로 초대 링크가 발송되었습니다.");
            result.put("method", "email");
            result.put("contact", inviteToken.getMaskedContact());

            return result;

        } catch (Exception e) {
            log.error("이메일 초대 발송 실패 - 토큰: {}",
                    inviteToken.getToken().substring(0, 8) + "...", e);
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * SMS 초대 처리
     */
    private Map<String, Object> processSmsInvite(FamilyInviteToken inviteToken) {
        Map<String, Object> result = new HashMap<>();

        try {
            // SMS 앱 연동 데이터 생성
            Map<String, Object> smsData = smsService.createSmsAppData(inviteToken);

            result.put("success", true);
            result.put("message", "SMS 초대 준비가 완료되었습니다.");
            result.put("method", "sms");
            result.put("contact", inviteToken.getMaskedContact());
            result.put("token", inviteToken.getToken());

            // SMS 앱 연동 정보 추가
            result.put("smsData", smsData);
            result.put("smsUrl", smsData.get("smsUrl"));
            result.put("shortSmsUrl", smsData.get("shortSmsUrl"));
            result.put("messageLength", smsData.get("messageLength"));
            result.put("recommendShort", smsData.get("recommendShort"));

            log.info("SMS 초대 데이터 생성 완료 - 토큰: {}, 메시지 길이: {}자",
                    inviteToken.getToken().substring(0, 8) + "...",
                    smsData.get("messageLength"));

            return result;

        } catch (Exception e) {
            log.error("SMS 초대 데이터 생성 실패 - 토큰: {}",
                    inviteToken.getToken().substring(0, 8) + "...", e);
            throw new RuntimeException("SMS 초대 데이터 생성에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 오래된 토큰 정리
     */
    private void cleanupOldTokens(Memorial memorial, String contact) {
        try {
            List<FamilyInviteToken> oldTokens = inviteTokenRepository
                    .findByMemorialAndContactOrderByCreatedAtDesc(memorial, contact);

            int cleanupCount = 0;
            for (FamilyInviteToken token : oldTokens) {
                if (token.getStatus().name().equals("PENDING") && token.isExpired()) {
                    token.expire();
                    cleanupCount++;
                } else if (token.getStatus().name().equals("REJECTED") || token.getStatus().name().equals("CANCELLED")) {
                    // 거절되거나 취소된 토큰은 그대로 유지 (히스토리용)
                    continue;
                }
            }

            if (cleanupCount > 0) {
                inviteTokenRepository.saveAll(oldTokens);
                log.info("오래된 토큰 정리 완료 - 메모리얼: {}, 연락처: {}, 정리된 토큰: {}개",
                        memorial.getId(), maskContact(contact), cleanupCount);
            }

        } catch (Exception e) {
            log.error("오래된 토큰 정리 실패 - 메모리얼: {}, 연락처: {}",
                    memorial.getId(), maskContact(contact), e);
        }
    }

    /**
     * 이미 가족 구성원인지 확인
     */
    private void checkExistingFamilyMember(Memorial memorial, String contact) {
        log.debug("기존 가족 구성원 확인 - 메모리얼: {}, 연락처: {}",
                memorial.getId(), maskContact(contact));

        // 이메일 또는 전화번호로 기존 가족 구성원 확인
        List<FamilyMember> existingMembers = familyMemberRepository.findByContact(contact, contact);

        // 같은 메모리얼의 가족 구성원인지 확인
        Optional<FamilyMember> existingMember = existingMembers.stream()
                .filter(fm -> fm.getMemorial().getId().equals(memorial.getId()))
                .findFirst();

        if (existingMember.isPresent()) {
            FamilyMember member = existingMember.get();
            String memberName = member.getMemberName();
            String statusName = member.getInviteStatusDisplayName();

            log.warn("이미 가족 구성원으로 등록된 연락처 - 메모리얼: {}, 연락처: {}, 구성원: {}, 상태: {}",
                    memorial.getId(), maskContact(contact), memberName, statusName);

            throw new IllegalArgumentException(
                    String.format("'%s'님은 이미 가족 구성원으로 등록되어 있습니다. (상태: %s)",
                            memberName, statusName));
        }
    }

    // ... 기존 헬퍼 메서드들 유지

    /**
     * 메모리얼 접근 권한 검증
     */
    private Memorial validateMemorialAccess(Member inviter, Long memorialId) {
        Memorial memorial = memorialRepository.findByIdAndOwner(memorialId, inviter)
                .orElseThrow(() -> new IllegalArgumentException("메모리얼을 찾을 수 없거나 접근 권한이 없습니다."));

        if (!memorial.isActive()) {
            throw new IllegalArgumentException("비활성화된 메모리얼입니다.");
        }

        return memorial;
    }

    /**
     * 연락처 유효성 검사
     */
    private void validateContact(String method, String contact) {
        if ("email".equals(method)) {
            if (!emailService.canSendEmail(contact)) {
                throw new IllegalArgumentException("유효하지 않은 이메일 주소입니다.");
            }
        } else if ("sms".equals(method)) {
            if (!smsService.isValidPhoneNumber(contact)) {
                throw new IllegalArgumentException("유효하지 않은 전화번호입니다.");
            }
        } else {
            throw new IllegalArgumentException("지원하지 않는 초대 방법입니다.");
        }
    }

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

    // ... 기존 나머지 메서드들 유지

    /**
     * 초대 토큰 상태 확인
     */
    public boolean isTokenValid(String token) {
        Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findUsableToken(token, LocalDateTime.now());
        return tokenOpt.isPresent();
    }

    /**
     * SMS 앱 연동 데이터 조회
     */
    public Map<String, Object> getSmsAppData(String token) {
        log.info("SMS 앱 연동 데이터 조회 - 토큰: {}", token.substring(0, 8) + "...");

        Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            log.warn("SMS 앱 데이터 조회 실패 - 토큰을 찾을 수 없음: {}", token.substring(0, 8) + "...");
            return Map.of("error", "토큰을 찾을 수 없습니다.");
        }

        FamilyInviteToken inviteToken = tokenOpt.get();

        if (!inviteToken.isSmsMethod()) {
            log.warn("SMS 앱 데이터 조회 실패 - SMS 방식이 아님: {}", token.substring(0, 8) + "...");
            return Map.of("error", "SMS 방식이 아닙니다.");
        }

        return smsService.createSmsAppData(inviteToken);
    }

    /**
     * SMS 앱 실행 URL 생성
     */
    public String createSmsAppUrl(String token) {
        log.info("SMS 앱 실행 URL 생성 - 토큰: {}", token.substring(0, 8) + "...");

        Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("토큰을 찾을 수 없습니다.");
        }

        FamilyInviteToken inviteToken = tokenOpt.get();

        if (!inviteToken.isSmsMethod()) {
            throw new IllegalArgumentException("SMS 방식이 아닙니다.");
        }

        Map<String, Object> smsData = smsService.createSmsAppData(inviteToken);
        return (String) smsData.get("smsUrl");
    }

    /**
     * 초대 토큰으로 초대 처리
     */
    @Transactional
    public String processInviteByToken(String token, Member acceptingMember) {
        log.info("초대 토큰 처리 시작 - 토큰: {}, 수락자: {}",
                token.substring(0, 8) + "...", acceptingMember.getId());

        try {
            // 1. 토큰 유효성 검증
            Optional<FamilyInviteToken> tokenOpt = inviteTokenRepository.findUsableToken(token, LocalDateTime.now());

            if (tokenOpt.isEmpty()) {
                throw new IllegalArgumentException("유효하지 않거나 만료된 초대 토큰입니다.");
            }

            FamilyInviteToken inviteToken = tokenOpt.get();

            // 2. 자기 자신 초대 방지 검사
            if (inviteToken.getInviter().getId().equals(acceptingMember.getId())) {
                log.warn("자기 자신 초대 시도 차단 - 초대자: {}, 수락자: {}",
                        inviteToken.getInviter().getId(), acceptingMember.getId());
                throw new IllegalArgumentException("자기 자신을 초대할 수 없습니다.");
            }

            // 3. 메모리얼 소유자 자기 자신 초대 방지
            if (inviteToken.getMemorial().getOwner().getId().equals(acceptingMember.getId())) {
                log.warn("메모리얼 소유자 자기 자신 초대 시도 차단 - 소유자: {}, 수락자: {}",
                        inviteToken.getMemorial().getOwner().getId(), acceptingMember.getId());
                throw new IllegalArgumentException("메모리얼 소유자는 자기 자신을 초대할 수 없습니다.");
            }

            // 4. 중복 가족 구성원 확인
            if (familyMemberRepository.existsByMemorialAndMember(inviteToken.getMemorial(), acceptingMember)) {
                throw new IllegalArgumentException("이미 해당 메모리얼의 가족 구성원입니다.");
            }

            // 5. 초대 수락 처리
            inviteToken.accept(acceptingMember);
            inviteTokenRepository.save(inviteToken);

            // 6. 가족 구성원 등록
            FamilyMember familyMember = createFamilyMemberFromToken(inviteToken, acceptingMember);
            familyMemberRepository.save(familyMember);

            log.info("초대 토큰 처리 완료 - 토큰: {}, 수락자: {}, 메모리얼: {}",
                    token.substring(0, 8) + "...",
                    acceptingMember.getId(),
                    inviteToken.getMemorial().getId());

            return String.format("%s 메모리얼에 가족 구성원으로 등록되었습니다.",
                    inviteToken.getMemorialName());

        } catch (Exception e) {
            log.error("초대 토큰 처리 실패 - 토큰: {}, 수락자: {}",
                    token.substring(0, 8) + "...", acceptingMember.getId(), e);
            throw e;
        }
    }

    /**
     * 초대 토큰에서 FamilyMember 생성
     */
    private FamilyMember createFamilyMemberFromToken(FamilyInviteToken inviteToken, Member acceptingMember) {
        log.info("FamilyMember 생성 - 메모리얼: {}, 구성원: {}, 관계: {}",
                inviteToken.getMemorial().getId(),
                acceptingMember.getId(),
                inviteToken.getRelationship());

        // 접근 권한 없음으로 초기 설정
        FamilyMember familyMember = FamilyMember.builder()
                .memorial(inviteToken.getMemorial())
                .member(acceptingMember)
                .invitedBy(inviteToken.getInviter())
                .relationship(inviteToken.getRelationship())
                .inviteMessage(inviteToken.getInviteMessage())
                .inviteStatus(com.tomato.remember.application.family.code.InviteStatus.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .memorialAccess(false)  // 메모리얼 접근 권한 없음
                .videoCallAccess(false) // 영상통화 권한 없음
                .lastAccessAt(LocalDateTime.now())
                .build();

        log.info("FamilyMember 생성 완료 - 메모리얼 접근: {}, 영상통화: {}, 관계: {}",
                familyMember.getMemorialAccess(),
                familyMember.getVideoCallAccess(),
                familyMember.getRelationshipDisplayName());

        return familyMember;
    }

    /**
     * 만료된 토큰 정리 (스케줄러에서 호출)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("만료된 초대 토큰 정리 시작");

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
    }
}