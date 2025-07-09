package com.tomato.remember.application.security;

import com.tomato.remember.application.member.entity.Member;
import com.tomato.remember.common.code.MemberRole;
import com.tomato.remember.common.code.MemberStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Getter
public class MemberUserDetails implements UserDetails {

    private final Member member;

    public MemberUserDetails(Member member) {
        this.member = member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getUserKey();
    }

    @Override
    public boolean isAccountNonExpired() {
        return member.getStatus() != MemberStatus.DELETED;
    }

    @Override
    public boolean isAccountNonLocked() {
        return member.getStatus() != MemberStatus.BLOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return member.getStatus() == MemberStatus.ACTIVE;
    }

    // 편의 메서드들
    public String getDisplayName() {
        return member.getName();
    }

    public String getMemberEmail() {
        return member.getEmail();
    }

    public String getPhoneNumber() {
        return member.getPhoneNumber();
    }

    public String getPreferredLanguage() {
        return member.getPreferredLanguage();
    }

    public MemberStatus getStatus() {
        return member.getStatus();
    }

    public MemberRole getRole() {
        return member.getRole();
    }

    // 권한 검증 메서드들
    public boolean hasRole(MemberRole role) {
        return role.equals(member.getRole());
    }

    public boolean hasAnyRole(MemberRole... roles) {
        MemberRole memberRole = member.getRole();
        for (MemberRole role : roles) {
            if (role.equals(memberRole)) {
                return true;
            }
        }
        return false;
    }

    public boolean isActive() {
        return member.getStatus() == MemberStatus.ACTIVE;
    }

    public boolean isBlocked() {
        return member.getStatus() == MemberStatus.BLOCKED;
    }

    public boolean isDeleted() {
        return member.getStatus() == MemberStatus.DELETED;
    }

    public boolean isUserActive() {
        return member.getRole() == MemberRole.USER_ACTIVE;
    }

    public boolean isUserInactive() {
        return member.getRole() == MemberRole.USER_INACTIVE;
    }

    // 무료 체험 관련
    public boolean isInFreeTrial() {
        return member.isInFreeTrial();
    }

    // 알림 설정 관련
    public boolean isPushNotificationEnabled() {
        return member.getPushNotification();
    }

    public boolean isMemorialNotificationEnabled() {
        return member.getMemorialNotification();
    }

    public boolean isPaymentNotificationEnabled() {
        return member.getPaymentNotification();
    }

    public boolean isFamilyNotificationEnabled() {
        return member.getFamilyNotification();
    }

    public boolean isMarketingAgreed() {
        return member.getMarketingAgreed();
    }

    public String getProfileImageUrl() {
        return member.getProfileImg();
    }

    @Override
    public String toString() {
        return "MemberUserDetails{" +
                "id=" + member.getId() +
                ", userKey='" + member.getUserKey() + '\'' +
                ", name='" + member.getName() + '\'' +
                ", role=" + member.getRole() +
                ", status=" + member.getStatus() +
                ", preferredLanguage='" + member.getPreferredLanguage() + '\'' +
                '}';
    }
}