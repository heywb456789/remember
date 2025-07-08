package com.tomato.naraclub.admin.user.code;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@Getter
@AllArgsConstructor
public enum AdminRole implements GrantedAuthority {
    SUPER_ADMIN("슈퍼관리자"),
    OPERATOR("운영진"),
    UPLOADER("업로더"),
    COMMON("심사대기");

    private final String displayName;

    public static AdminRole of(String role) {
        return Arrays.stream(values())
            .filter(r -> r.name().equals(role))
            .findAny().orElseThrow(() ->
                new IllegalArgumentException("Unknown role: " + role));
    }

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
