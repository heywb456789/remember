package com.tomato.naraclub.admin.user.controller;

import com.tomato.naraclub.admin.security.AdminUserDetails;
import com.tomato.naraclub.admin.user.dto.AdminAuthorityRequest;
import com.tomato.naraclub.admin.user.dto.AdminUserResponse;
import com.tomato.naraclub.admin.user.dto.AppUserResponse;
import com.tomato.naraclub.admin.user.dto.UserActivityListResponse;
import com.tomato.naraclub.admin.user.dto.UserLoginHistoryResponse;
import com.tomato.naraclub.admin.user.dto.UserUpdateRequest;
import com.tomato.naraclub.admin.user.service.AdminUserService;
import com.tomato.naraclub.admin.user.service.AppUserService;
import com.tomato.naraclub.application.auth.entity.MemberLoginHistory;
import com.tomato.naraclub.common.dto.ListDTO;
import com.tomato.naraclub.common.dto.ResponseDTO;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.controller
 * @fileName : AdminUserRestController
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
@RestController
@RequestMapping("/admin/users/")
@RequiredArgsConstructor
public class AdminUserRestController {

    private final AppUserService appUserService;
    private final AdminUserService adminUserService;

    @PutMapping("/app/user/{id}/status")
    public ResponseDTO<AppUserResponse> updateUserVerified(
        @PathVariable Long id,
        @AuthenticationPrincipal AdminUserDetails userDetails,
        @RequestBody UserUpdateRequest request) {
        return ResponseDTO.ok(appUserService.updateUserVerified(id, userDetails, request));
    }

    @PostMapping("/admin/{id}/approve")
    public ResponseDTO<AdminUserResponse> approveUser(
        @PathVariable Long id,
        @AuthenticationPrincipal AdminUserDetails userDetails,
        @RequestBody AdminAuthorityRequest request) {
        return ResponseDTO.ok(adminUserService.approveUser(id, userDetails, request));
    }

    @PutMapping("/admin/{id}/role")
    public ResponseDTO<AdminUserResponse> updateAdminUserRole(
        @PathVariable Long id,
        @AuthenticationPrincipal AdminUserDetails userDetails,
        @RequestBody AdminAuthorityRequest request) {
        return ResponseDTO.ok(adminUserService.updateAdminUserRole(id, userDetails, request));
    }

    @PutMapping("/admin/{id}/status")
    public ResponseDTO<AdminUserResponse> updateAdminStatus(
        @PathVariable Long id,
        @AuthenticationPrincipal AdminUserDetails userDetails,
        @RequestBody AdminAuthorityRequest request) {
        return ResponseDTO.ok(adminUserService.updateAdminStatus(id, userDetails, request));
    }

    @GetMapping("/app/user/{id}/login-history")
    public ResponseDTO<Map<String, Object>> getLoginHistory(
        @PathVariable long id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {

        Page<MemberLoginHistory> loginHistory = appUserService.getAppUserLoginHistory(id, page,
            size);
        boolean hasMore = loginHistory.getTotalElements() > (size * (page + 1));

        Map<String, Object> response = new HashMap<>();
        response.put("history", loginHistory.getContent());
        response.put("hasMore", hasMore);

        return ResponseDTO.ok(response);
    }

    @GetMapping("/app/user/{id}/activities")
    public ResponseDTO<UserActivityListResponse> getActivities(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String type) {
        return ResponseDTO.ok(appUserService.getUserActivities(id, page, size, type));
    }
}
