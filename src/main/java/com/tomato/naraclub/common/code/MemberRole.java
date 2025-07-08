package com.tomato.naraclub.common.code;

import java.util.Arrays;
import org.springframework.security.core.GrantedAuthority;

public enum MemberRole implements GrantedAuthority {
  USER_ACTIVE, USER_INACTIVE;

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

