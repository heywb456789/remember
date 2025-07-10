package com.tomato.remember.application.member.dto;

import com.tomato.remember.common.code.MemberRole;
import com.tomato.remember.common.code.MemberStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor
@ToString
public class MemberDTO {
    private Long id;
    private LocalDateTime createdAt;
    private String password;
    private String phoneNumber;
    private String inviteCode;
    private MemberStatus status;
    private MemberRole role;
    private String email;
    private String name;
    private LocalDateTime lastAccessAt;
    private String profileImg;
    private String profileImageUrl;
    private String preferredLanguage;
    private Boolean marketingAgreed;
    private Boolean pushNotification;
    private Boolean memorialNotification;
    private Boolean paymentNotification;
    private Boolean familyNotification;
    private LocalDateTime freeTrialStartAt;
    private LocalDateTime freeTrialEndAt;
}