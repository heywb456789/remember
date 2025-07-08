package com.tomato.remember.admin.auth.service;

import com.tomato.remember.admin.auth.dto.AdminAuthRequest;
import com.tomato.remember.admin.auth.dto.AdminAuthResponseDTO;
import jakarta.servlet.http.HttpServletRequest;


public interface AdminAuthService {

    AdminAuthResponseDTO createToken(AdminAuthRequest req, HttpServletRequest request);

    AdminAuthResponseDTO createUserAndToken(AdminAuthRequest req, HttpServletRequest request);

    Boolean checkUserName(String username);

    AdminAuthResponseDTO refreshToken(String refreshToken);
}
