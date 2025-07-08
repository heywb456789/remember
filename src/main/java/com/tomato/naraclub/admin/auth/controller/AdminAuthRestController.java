package com.tomato.naraclub.admin.auth.controller;

import com.tomato.naraclub.admin.auth.dto.AdminAuthRequest;
import com.tomato.naraclub.admin.auth.dto.AdminAuthResponseDTO;
import com.tomato.naraclub.admin.auth.service.AdminAuthService;
import com.tomato.naraclub.admin.security.AdminUserDetails;
import com.tomato.naraclub.application.auth.dto.AuthResponseDTO;
import com.tomato.naraclub.common.dto.ResponseDTO;
import com.tomato.naraclub.common.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// com.tomato.naraclub.application.admin.controller.AdminAuthController.java
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthRestController {

    private final AdminAuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/login")
    public ResponseDTO<AdminAuthResponseDTO> login(
        @RequestBody AdminAuthRequest req,
        HttpServletRequest request,
        HttpServletResponse response) {

        AdminAuthResponseDTO authResponse = authService.createToken(req, request);

        cookieUtil.addTokenCookies(response, authResponse.getToken(), authResponse.getRefreshToken());

        return ResponseDTO.ok(authResponse);
    }

    @PostMapping("/register")
    public ResponseDTO<AdminAuthResponseDTO> register(
        @RequestBody AdminAuthRequest req,
        HttpServletRequest request) {
        return ResponseDTO.ok(authService.createUserAndToken(req, request));
    }

    @GetMapping("/check/username")
    public ResponseDTO<Boolean> checkUsername(@RequestParam String username) {
        return ResponseDTO.ok(authService.checkUserName(username));
    }

    @PostMapping("/refresh")
    public ResponseDTO<AdminAuthResponseDTO> refreshToken(
        @RequestBody Map<String, String> request,
        HttpServletResponse response) {

        AdminAuthResponseDTO authResponse = authService.refreshToken(request.get("refreshToken"));

        cookieUtil.addTokenCookies(response, authResponse.getToken(), authResponse.getRefreshToken());
        return ResponseDTO.ok(authResponse);
    }

    @GetMapping("/debug")
    public ResponseEntity<?> debug(@AuthenticationPrincipal AdminUserDetails admin) {
        return ResponseEntity.ok(Map.of(
            "username", admin.getUsername(),
            "role", admin.getAdmin().getRole(),
            "authorities", admin.getAuthorities()
        ));
    }
}
