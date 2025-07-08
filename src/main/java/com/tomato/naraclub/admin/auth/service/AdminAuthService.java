package com.tomato.naraclub.admin.auth.service;

import com.tomato.naraclub.admin.auth.dto.AdminAuthRequest;
import com.tomato.naraclub.admin.auth.dto.AdminAuthResponseDTO;
import com.tomato.naraclub.admin.security.AdminUserDetails;
import com.tomato.naraclub.application.auth.dto.AuthResponseDTO;
import jakarta.servlet.http.HttpServletRequest;


public interface AdminAuthService {

    AdminAuthResponseDTO createToken(AdminAuthRequest req, HttpServletRequest request);

    AdminAuthResponseDTO createUserAndToken(AdminAuthRequest req, HttpServletRequest request);

    Boolean checkUserName(String username);

    AdminAuthResponseDTO refreshToken(String refreshToken);
}
