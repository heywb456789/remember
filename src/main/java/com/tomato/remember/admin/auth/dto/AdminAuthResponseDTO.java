package com.tomato.remember.admin.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuthResponseDTO {

    private String token;
    private String refreshToken;
    private AdminDTO admin;
}
