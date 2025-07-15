package com.tomato.remember.application.family.service;

import com.tomato.remember.application.family.entity.FamilyInviteToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * SMS 관련 서비스
 * - SMS 텍스트 생성
 * - 앱 연동용 데이터 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {
    
    @Value("${spring.mail.domain:https://app.tomatoremember.com}")
    private String appDomain;
    
    @Value("${spring.mail.name:토마토리멤버}")
    private String appName;
    
    /**
     * 가족 초대 SMS 텍스트 생성
     */
    public String createInviteSmsText(FamilyInviteToken inviteToken) {
        log.info("가족 초대 SMS 텍스트 생성 - 토큰: {}", 
                inviteToken.getToken().substring(0, 8) + "...");
        
        try {
            StringBuilder message = new StringBuilder();
            
            // 인사말
            message.append(String.format("[%s] 가족 메모리얼 초대\n\n", appName));
            
            // 초대자 정보
            message.append(String.format("%s님이 ", inviteToken.getInviterName()));
            message.append(String.format("'%s(%s)' 메모리얼에 ", 
                    inviteToken.getMemorial().getNickname(), 
                    inviteToken.getMemorial().getName()));
            message.append(String.format("%s 관계로 초대했습니다.\n\n", 
                    inviteToken.getRelationshipDisplayName()));
            
            // 초대 메시지 (있는 경우)
            if (inviteToken.getInviteMessage() != null && !inviteToken.getInviteMessage().trim().isEmpty()) {
                message.append("💌 전달 메시지:\n");
                message.append(String.format("\"%s\"\n\n", inviteToken.getInviteMessage()));
            }
            
            // 초대 링크
            message.append("🔗 초대 수락하기:\n");
            message.append(createInviteLink(inviteToken.getToken()));
            message.append("\n\n");
            
            // 만료 안내
            message.append("⏰ 초대 만료: ");
            message.append(formatExpirationDate(inviteToken));
            message.append(String.format(" (남은 시간: %d시간)", inviteToken.getRemainingHours()));
            
            String smsText = message.toString();
            
            log.info("가족 초대 SMS 텍스트 생성 완료 - 토큰: {}, 길이: {}자", 
                    inviteToken.getToken().substring(0, 8) + "...", smsText.length());
            
            return smsText;
            
        } catch (Exception e) {
            log.error("가족 초대 SMS 텍스트 생성 실패 - 토큰: {}", 
                    inviteToken.getToken().substring(0, 8) + "...", e);
            
            // 실패 시 기본 메시지 반환
            return createFallbackSmsText(inviteToken);
        }
    }
    
    /**
     * 짧은 버전 SMS 텍스트 생성 (문자 길이 제한 고려)
     */
    public String createShortInviteSmsText(FamilyInviteToken inviteToken) {
        log.info("짧은 가족 초대 SMS 텍스트 생성 - 토큰: {}", 
                inviteToken.getToken().substring(0, 8) + "...");
        
        StringBuilder message = new StringBuilder();
        
        // 간단한 인사말
        message.append(String.format("[%s] ", appName));
        message.append(String.format("%s님이 메모리얼에 초대했습니다.\n\n", 
                inviteToken.getInviterName()));
        
        // 초대 링크
        message.append("수락하기: ");
        message.append(createInviteLink(inviteToken.getToken()));
        message.append("\n\n");
        
        // 만료 안내
        message.append(String.format("만료: %s", formatShortExpirationDate(inviteToken)));
        
        return message.toString();
    }
    
    /**
     * SMS 앱 연동용 데이터 생성
     */
    public Map<String, Object> createSmsAppData(FamilyInviteToken inviteToken) {
        log.info("SMS 앱 연동 데이터 생성 - 토큰: {}", 
                inviteToken.getToken().substring(0, 8) + "...");
        
        Map<String, Object> data = new HashMap<>();
        
        // 기본 정보
        data.put("phoneNumber", inviteToken.getContact());
        data.put("message", createInviteSmsText(inviteToken));
        data.put("shortMessage", createShortInviteSmsText(inviteToken));
        
        // 앱 실행 URL 생성
        data.put("smsUrl", createSmsAppUrl(inviteToken));
        data.put("shortSmsUrl", createShortSmsAppUrl(inviteToken));
        
        // 메타 정보
        data.put("messageLength", ((String) data.get("message")).length());
        data.put("shortMessageLength", ((String) data.get("shortMessage")).length());
        data.put("recommendShort", ((String) data.get("message")).length() > 80);
        
        return data;
    }
    
    /**
     * SMS 앱 실행 URL 생성 (일반 버전)
     */
    private String createSmsAppUrl(FamilyInviteToken inviteToken) {
        String message = createInviteSmsText(inviteToken);
        return createSmsUrl(inviteToken.getContact(), message);
    }
    
    /**
     * SMS 앱 실행 URL 생성 (짧은 버전)
     */
    private String createShortSmsAppUrl(FamilyInviteToken inviteToken) {
        String message = createShortInviteSmsText(inviteToken);
        return createSmsUrl(inviteToken.getContact(), message);
    }
    
    /**
     * SMS URL 생성
     */
    private String createSmsUrl(String phoneNumber, String message) {
        // Android: sms:전화번호?body=메시지
        // iOS: sms:전화번호&body=메시지
        
        // 전화번호 포맷 정리 (- 제거)
        String cleanPhoneNumber = phoneNumber.replaceAll("[^0-9]", "");
        
        // URL 인코딩
        String encodedMessage = java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
        
        return String.format("sms:%s?body=%s", cleanPhoneNumber, encodedMessage);
    }
    
    /**
     * 초대 링크 생성
     */
    private String createInviteLink(String token) {
        return appDomain + "/mobile/family/invite/" + token;
    }
    
    /**
     * 만료 날짜 포맷팅
     */
    private String formatExpirationDate(FamilyInviteToken inviteToken) {
        return inviteToken.getExpiresAt()
                .format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }
    
    /**
     * 짧은 만료 날짜 포맷팅
     */
    private String formatShortExpirationDate(FamilyInviteToken inviteToken) {
        return inviteToken.getExpiresAt()
                .format(DateTimeFormatter.ofPattern("MM/dd"));
    }
    
    /**
     * 폴백 SMS 텍스트 생성 (오류 시 사용)
     */
    private String createFallbackSmsText(FamilyInviteToken inviteToken) {
        return String.format("[%s] %s님이 가족 메모리얼에 초대했습니다. %s", 
                appName, 
                inviteToken.getInviterName(), 
                createInviteLink(inviteToken.getToken()));
    }
    
    /**
     * 전화번호 유효성 검사
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // 한국 휴대폰 번호 패턴 (010-xxxx-xxxx 또는 01xxxxxxxx)
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        return cleanNumber.matches("^010\\d{8}$");
    }
    
    /**
     * 전화번호 포맷팅 (010-xxxx-xxxx 형태로 변환)
     */
    public String formatPhoneNumber(String phoneNumber) {
        if (!isValidPhoneNumber(phoneNumber)) {
            return phoneNumber;
        }
        
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        return cleanNumber.substring(0, 3) + "-" + 
               cleanNumber.substring(3, 7) + "-" + 
               cleanNumber.substring(7);
    }
    
    /**
     * SMS 텍스트 길이 체크
     */
    public boolean isTextTooLong(String text) {
        // 일반적인 SMS 길이 제한 (한글 기준 약 70자)
        return text.length() > 70;
    }
    
    /**
     * 텍스트 길이 정보 반환
     */
    public Map<String, Object> getTextLengthInfo(String text) {
        Map<String, Object> info = new HashMap<>();
        
        info.put("length", text.length());
        info.put("isTooLong", isTextTooLong(text));
        info.put("maxLength", 70);
        info.put("remaining", Math.max(0, 70 - text.length()));
        
        return info;
    }
}