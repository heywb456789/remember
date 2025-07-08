package com.tomato.remember.admin.auth.controller;

import com.tomato.remember.admin.auth.dto.AdminInfo;
import com.tomato.remember.admin.auth.dto.AdminLoginRequest;
import com.tomato.remember.admin.auth.dto.AdminLoginResponse;
import com.tomato.remember.admin.auth.dto.AdminRefreshResponse;
import com.tomato.remember.admin.auth.dto.AdminRegisterRequest;
import com.tomato.remember.admin.security.AdminUserDetails;
import com.tomato.remember.admin.security.AdminUserDetailsService;
import com.tomato.remember.admin.user.entity.Admin;
import com.tomato.remember.common.dto.ResponseDTO;
import com.tomato.remember.common.security.JwtTokenProvider;
import com.tomato.remember.common.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// com.tomato.naraclub.application.admin.controller.AdminAuthController.java
@Slf4j
@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
public class AdminAuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AdminUserDetailsService adminUserDetailsService;
    private final CookieUtil cookieUtil;

    /**
     * 관리자 로그인 API
     */
    @PostMapping("/auth/login")
    public ResponseDTO<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse response) {
        try {
            // 인증 처리
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            AdminUserDetails adminDetails = (AdminUserDetails) authentication.getPrincipal();
            Admin admin = adminDetails.getAdmin();

            // JWT 토큰 생성
            String accessToken = tokenProvider.createAdminAccessToken(admin);
            String refreshToken = tokenProvider.createAdminRefreshToken(admin, request.isAutoLogin());

            // 쿠키 설정 (필요시)
            // cookieUtil.setAdminTokenCookies(response, accessToken, refreshToken);

            AdminLoginResponse loginResponse = AdminLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600) // 1시간
                .adminInfo(AdminInfo.builder()
                    .id(admin.getId())
                    .username(admin.getUsername())
                    .role(admin.getRole().name())
                    .build())
                .build();

            log.info("Admin login successful: {}", admin.getUsername());
            return ResponseDTO.ok(loginResponse);

        } catch (Exception e) {
            log.error("Admin login failed: {}", e.getMessage());
            return ResponseDTO.ok();
        }
    }

    /**
     * 관리자 토큰 갱신 API
     */
    @PostMapping("/auth/refresh")
    public ResponseDTO<AdminRefreshResponse> refresh(@RequestBody Map<String, String> request,
                                                   HttpServletResponse response) {
        try {
            String refreshToken = request.get("refreshToken");

            // Refresh Token 검증
            if (!tokenProvider.validateAdminToken(refreshToken)) {
//                return ResponseDTO.badRequest("유효하지 않은 리프레시 토큰입니다.");
                return ResponseDTO.ok();
            }

            // 관리자 정보 추출
            String username = tokenProvider.getSubject(refreshToken);
            AdminUserDetails adminDetails = (AdminUserDetails) adminUserDetailsService.loadUserByUsername(username);
            Admin admin = adminDetails.getAdmin();

            // 새 토큰 생성
            String newAccessToken = tokenProvider.createAdminAccessToken(admin);
            String newRefreshToken = tokenProvider.createAdminRefreshToken(admin, false);

            // 쿠키 갱신 (필요시)
            // cookieUtil.setAdminTokenCookies(response, newAccessToken, newRefreshToken);

            AdminRefreshResponse refreshResponse = AdminRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();

            log.info("Admin token refreshed: {}", username);
            return ResponseDTO.ok(refreshResponse);

        } catch (Exception e) {
            log.error("Admin token refresh failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("토큰 갱신에 실패했습니다.");
            return ResponseDTO.ok();
        }
    }

    /**
     * 관리자 로그아웃 API
     */
    @PostMapping("/auth/logout")
    public ResponseDTO<Void> logout(@AuthenticationPrincipal AdminUserDetails adminDetails) {
        try {
            if (adminDetails != null) {
                log.info("Admin logout: {}", adminDetails.getUsername());
                // TODO: 토큰 블랙리스트 처리 (필요시)
            }

            return ResponseDTO.ok();
        } catch (Exception e) {
            log.error("Admin logout error: {}", e.getMessage());
            return ResponseDTO.ok(); // 로그아웃은 항상 성공으로 처리
        }
    }

    /**
     * 관리자 정보 조회 API
     */
    @GetMapping("/auth/me")
    public ResponseDTO<AdminInfo> getCurrentAdmin(@AuthenticationPrincipal AdminUserDetails adminDetails) {
        try {
            Admin admin = adminDetails.getAdmin();

            AdminInfo adminInfo = AdminInfo.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .role(admin.getRole().name())
                .build();

            return ResponseDTO.ok(adminInfo);
        } catch (Exception e) {
            log.error("Get current admin failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("관리자 정보 조회에 실패했습니다.");
            return ResponseDTO.ok();
        }
    }

    /**
     * 관리자 아이디 중복 확인 API
     */
    @GetMapping("/auth/check/username")
    public ResponseDTO<Boolean> checkUsername(@RequestParam String username) {
        try {
            // TODO: 아이디 중복 확인 로직
            // boolean isAvailable = adminService.isUsernameAvailable(username);
            boolean isAvailable = true; // 임시

            return ResponseDTO.ok(isAvailable);
        } catch (Exception e) {
            log.error("Check username failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("아이디 중복 확인에 실패했습니다.");
            return ResponseDTO.ok();
        }
    }

    /**
     * 관리자 회원가입 API (필요시)
     */
    @PostMapping("/auth/register")
    public ResponseDTO<AdminInfo> register(@Valid @RequestBody AdminRegisterRequest request) {
        try {
            // TODO: 관리자 회원가입 로직
            // Admin admin = adminService.registerAdmin(request);

            AdminInfo adminInfo = AdminInfo.builder()
                .id(1L) // 임시
                .username(request.getUsername())
                .role("OPERATOR")
                .build();

            log.info("Admin register successful: {}", request.getUsername());
            return ResponseDTO.ok(adminInfo);

        } catch (Exception e) {
            log.error("Admin register failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("관리자 회원가입에 실패했습니다.");
            return ResponseDTO.ok();
        }
    }

    /**
     * 메모리얼 목록 조회 API (관리자용)
     */
    @GetMapping("/memorials")
    public ResponseDTO<Map<String, Object>> getMemorials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AdminUserDetails adminDetails) {
        try {
            log.info("Admin {} requested memorial list", adminDetails.getUsername());

            // TODO: 메모리얼 목록 조회 로직
            Map<String, Object> result = Map.of(
                "content", "TODO: 메모리얼 목록",
                "page", page,
                "size", size,
                "totalElements", 0
            );

            return ResponseDTO.ok(result);
        } catch (Exception e) {
            log.error("Get memorials failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("메모리얼 목록 조회에 실패했습니다.");
            return ResponseDTO.ok();
        }
    }

    /**
     * 회원 목록 조회 API (관리자용)
     */
    @GetMapping("/members")
    public ResponseDTO<Map<String, Object>> getMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AdminUserDetails adminDetails) {
        try {
            log.info("Admin {} requested member list", adminDetails.getUsername());

            // TODO: 회원 목록 조회 로직
            Map<String, Object> result = Map.of(
                "content", "TODO: 회원 목록",
                "page", page,
                "size", size,
                "totalElements", 0
            );

            return ResponseDTO.ok(result);
        } catch (Exception e) {
            log.error("Get members failed: {}", e.getMessage());
//            return ResponseDTO.badRequest("회원 목록 조회에 실패했습니다.");
            return ResponseDTO.ok();
        }
    }

    /**
     * 디버깅용 API
     */
    @GetMapping("/auth/debug")
    public ResponseEntity<?> debug(@AuthenticationPrincipal AdminUserDetails adminDetails) {
        return ResponseEntity.ok(Map.of(
            "username", adminDetails.getUsername(),
            "role", adminDetails.getAdmin().getRole(),
            "authorities", adminDetails.getAuthorities()
        ));
    }
}
