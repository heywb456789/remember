package com.tomato.remember.application.auth.dto;

import com.tomato.remember.application.MobileApiController.MemberInfo;
import com.tomato.remember.application.member.dto.MemberDTO;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private int expiresIn;
    private MemberDTO member;
}