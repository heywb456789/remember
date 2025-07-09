package com.tomato.remember.common.code;

import java.util.Arrays;
import org.springframework.security.core.GrantedAuthority;

public enum MemberRole implements GrantedAuthority {
  USER_ACTIVE("활성 사용자"),
  USER_INACTIVE("비활성 사용자");

  private final String displayName;

  MemberRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MemberRole parse(String role) {
        return Arrays.stream(values())
            .filter(memberRole -> memberRole.name().equals(role))
            .findAny()
            .orElse(null);
    }

    @Override
    public String getAuthority() {
        return this.name();
    }

}

