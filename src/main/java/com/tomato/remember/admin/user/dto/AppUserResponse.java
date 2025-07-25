package com.tomato.remember.admin.user.dto;

import com.tomato.remember.common.code.MemberRole;
import com.tomato.remember.common.code.MemberStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.dto
 * @fileName : AppUserResponse
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUserResponse {
    private Long userId;
    private String userName;
    private String userKey;
    private String phoneNumber;
    private String inviteCode;
    private String profileImageUrl;
    private MemberStatus status;
    private MemberRole role;
    private String email;
    private String preferredLanguage;
    private Boolean marketingAgreed;
    private Boolean pushNotification;
    private Boolean memorialNotification;
    private Boolean paymentNotification;
    private Boolean familyNotification;
    private LocalDateTime freeTrialStartAt;
    private LocalDateTime freeTrialEndAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
