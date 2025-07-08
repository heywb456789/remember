package com.tomato.remember.admin.security;

import com.tomato.remember.admin.user.code.AdminRole;
import com.tomato.remember.admin.user.code.AdminStatus;
import com.tomato.remember.admin.user.entity.Admin;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// com.tomato.naraclub.application.security.AdminUserDetails.java
public class AdminUserDetails implements UserDetails {

    private final Admin admin;

    public AdminUserDetails(Admin admin) {
        this.admin = admin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(admin.getRole());
    }

    @Override
    public String getPassword() {
        return admin.getPassword();
    }

    @Override
    public String getUsername() {
        return admin.getUsername();
    }

    // 나머지 메서드는 모두 true로
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
        return admin.getStatus() == AdminStatus.ACTIVE
            && admin.getRole() != AdminRole.COMMON;
    }

    public Admin getAdmin() {
        return admin;
    }

    public Long getId() {
        return admin.getId();
    }
}
