package com.tomato.remember.application.family.dto;

import com.tomato.remember.application.member.code.Relationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * 가족 구성원 초대 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class FamilyInviteRequest {

    /**
     * 메모리얼 ID
     */
    @NotNull(message = "메모리얼 ID는 필수입니다.")
    private Long memorialId;

    /**
     * 초대 방법 (email 또는 sms)
     */
    @NotBlank(message = "초대 방법은 필수입니다.")
    @Pattern(regexp = "^(email|sms)$", message = "초대 방법은 email 또는 sms만 가능합니다.")
    private String method;

    /**
     * 연락처 (이메일 또는 전화번호)
     */
    @NotBlank(message = "연락처는 필수입니다.")
    private String contact;

    /**
     * 고인과의 관계
     */
    @NotNull(message = "고인과의 관계는 필수입니다.")
    private Relationship relationship;

    /**
     * 초대 메시지 (선택사항)
     */
    private String message;

    /**
     * 이메일 유효성 검사
     */
    public boolean isValidEmail() {
        if (!"email".equals(method)) {
            return true; // 이메일 방식이 아니면 검사 안함
        }
        
        String emailPattern = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        return contact != null && contact.matches(emailPattern);
    }

    /**
     * 전화번호 유효성 검사
     */
    public boolean isValidPhone() {
        if (!"sms".equals(method)) {
            return true; // SMS 방식이 아니면 검사 안함
        }
        
        String phonePattern = "^010-\\d{4}-\\d{4}$";
        return contact != null && contact.matches(phonePattern);
    }

    /**
     * 연락처 유효성 검사
     */
    public boolean isValidContact() {
        return isValidEmail() && isValidPhone();
    }

    /**
     * 이메일인지 확인
     */
    public boolean isEmailMethod() {
        return "email".equals(method);
    }

    /**
     * SMS인지 확인
     */
    public boolean isSmsMethod() {
        return "sms".equals(method);
    }

    /**
     * 전화번호 포맷팅 (하이픈 제거)
     */
    public String getFormattedPhone() {
        if (contact == null) {
            return null;
        }
        return contact.replaceAll("-", "");
    }

    /**
     * 연락처 마스킹 (로그용)
     */
    public String getMaskedContact() {
        if (contact == null || contact.length() < 4) {
            return "****";
        }
        
        if (isEmailMethod()) {
            // 이메일 마스킹: te**@example.com
            int atIndex = contact.indexOf('@');
            if (atIndex > 2) {
                return contact.substring(0, 2) + "**" + contact.substring(atIndex);
            }
            return "**" + contact.substring(contact.length() - 4);
        } else {
            // 전화번호 마스킹: 010-****-5678
            return contact.substring(0, 4) + "****" + contact.substring(contact.length() - 4);
        }
    }
}