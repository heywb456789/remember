package com.tomato.naraclub.application.security;

import com.tomato.naraclub.application.member.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

/**
 * 사용 예시
 * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
 * MemberUserDetails userDetails = (MemberUserDetails) auth.getPrincipal();
 *
 * // Member 엔티티 전체 접근 가능
 * Member member = userDetails.getMember();
 * String email = member.getEmail();
 * MemberRole role = member.getRole();
 */
@Getter
public class MemberUserDetails implements UserDetails {

    private final Member member;

    public MemberUserDetails(Member member) {
        this.member = member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // MemberRole이 이미 GrantedAuthority를 구현했다면:
        return List.of(member.getRole());
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return member.getName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}