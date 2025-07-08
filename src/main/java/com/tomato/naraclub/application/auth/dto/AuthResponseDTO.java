package com.tomato.naraclub.application.auth.dto;

import com.tomato.naraclub.application.member.dto.MemberDTO;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String refreshToken;
    private MemberDTO member;
}