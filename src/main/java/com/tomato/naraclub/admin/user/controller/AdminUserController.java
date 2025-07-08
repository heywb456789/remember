package com.tomato.naraclub.admin.user.controller;

import com.tomato.naraclub.admin.security.AdminUserDetails;
import com.tomato.naraclub.admin.user.code.AdminRole;
import com.tomato.naraclub.admin.user.code.AdminStatus;
import com.tomato.naraclub.admin.user.dto.AdminUserListRequest;
import com.tomato.naraclub.admin.user.dto.AdminUserResponse;
import com.tomato.naraclub.admin.user.dto.AppUserListRequest;
import com.tomato.naraclub.admin.user.dto.AppUserResponse;
import com.tomato.naraclub.admin.user.service.AdminUserService;
import com.tomato.naraclub.admin.user.service.AppUserService;
import com.tomato.naraclub.application.auth.entity.MemberLoginHistory;
import com.tomato.naraclub.common.dto.ListDTO;
import com.tomato.naraclub.common.dto.Pagination;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.controller
 * @fileName : AdminUserController
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AppUserService appUserService;
    private final AdminUserService adminUserService;

    @GetMapping("/app/user-list")
    public String userList(
        AppUserListRequest request,
        @AuthenticationPrincipal AdminUserDetails user,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size,
        Model model) {
        // 게시글 목록 조회
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        ListDTO<AppUserResponse> appUserList = appUserService.getAppUserList(user, request,
            pageable);

        // ListDTO에서 페이징 정보 추출
        Pagination pagination = appUserList.getPagination();
        List<AppUserResponse> userList = appUserList.getData();

        int totalPages = pagination.getTotalPages();
        int currentPage = pagination.getCurrentPage(); // 이미 1-based
        long totalCount = pagination.getTotalElements();

        // 페이지 범위 계산 (최대 10개 페이지 표시)
        int startPage = Math.max(1, currentPage - 5);
        int endPage = Math.min(startPage + 9, totalPages);

        // startPage 재조정 (끝 페이지 기준으로)
        if (endPage - startPage < 9 && startPage > 1) {
            startPage = Math.max(1, endPage - 9);
        }

        //모델에 데이터 추가
        model.addAttribute("userList", userList);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", currentPage); // 1-based
        model.addAttribute("currentPageZeroIndex", page); // 0-based (URL용)
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("pageSize", size);

        // 이전/다음 페이지 여부
        model.addAttribute("hasPrevious", page > 0);
        model.addAttribute("hasNext", page < totalPages - 1);

        // 페이지 제목 및 활성 메뉴 설정
        model.addAttribute("searchRequest", request);
        model.addAttribute("pageTitle", "앱 유저관리 - NaraSarang Admin");

        // 사용자 정보 설정 (공통)
        model.addAttribute("userName", user.getUsername());
        model.addAttribute("userRole", user.getAuthorities());
        model.addAttribute("userRoleDisplay", user.getAdmin().getRole().getDisplayName());
        model.addAttribute("userAvatar", null);

        return "admin/user/appUserList";
    }

    @GetMapping("/app/user/{id}")
    public String userDetail(
        @AuthenticationPrincipal AdminUserDetails user,
        @PathVariable long id,
        Model model) {
        AppUserResponse userResponse = appUserService.getAppUserDetail(id);
        // 첫 페이지 로그인 기록만 가져오기 (10개)
        Page<MemberLoginHistory> loginHistory = appUserService.getAppUserLoginHistory(id, 0, 10);

        // 첫 페이지 활동 내역도 가져오기 (옵션)
//        Page<UserActivityResponse> userActivities = appUserService.getUserActivities(id, 0, 10);

        model.addAttribute("user", userResponse);
        model.addAttribute("loginHistoryList", loginHistory.getContent());
        model.addAttribute("hasMoreLoginHistory", loginHistory.getTotalElements() > 10);

        model.addAttribute("pageTitle", "앱 유저관리 - NaraSarang Admin");
        model.addAttribute("activeMenu", "users");
        model.addAttribute("activeSubmenu", "app");

        // 사용자 정보 설정 (공통)
        model.addAttribute("userName", user.getUsername());
        model.addAttribute("userRole", user.getAuthorities());
        model.addAttribute("userRoleDisplay", user.getAdmin().getRole().getDisplayName());
        model.addAttribute("userAvatar", null);

        return "admin/user/appUserDetail";
    }

    @GetMapping("/admin/user-list")
    public String adminUserList(
        AdminUserListRequest request,
        @AuthenticationPrincipal AdminUserDetails user,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "50") int size,
        Model model) {
        // 게시글 목록 조회
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        ListDTO<AdminUserResponse> adminUserList = adminUserService.getAdminUserList(user, request,
            pageable);

        // 전체 리스트
        List<AdminUserResponse> all = adminUserList.getData();

        List<AdminUserResponse> superAdmins = all.stream()
            .filter(
                a -> a.getRole() == AdminRole.SUPER_ADMIN && a.getStatus() == AdminStatus.ACTIVE)
            .collect(Collectors.toList());
        List<AdminUserResponse> operators = all.stream()
            .filter(a -> a.getRole() == AdminRole.OPERATOR && a.getStatus() == AdminStatus.ACTIVE)
            .collect(Collectors.toList());
        List<AdminUserResponse> uploaders = all.stream()
            .filter(a -> a.getRole() == AdminRole.UPLOADER && a.getStatus() == AdminStatus.ACTIVE)
            .collect(Collectors.toList());
        List<AdminUserResponse> pendings = all.stream()
            .filter(a -> a.getRole() == AdminRole.COMMON)
            .collect(Collectors.toList());

        // 페이징 정보
        int totalPages = adminUserList.getPagination().getTotalPages();
        int currentPage = adminUserList.getPagination().getCurrentPage();
        int startPage = Math.max(1, currentPage - 4);
        int endPage = Math.min(startPage + 9, totalPages);

        //모델에 데이터 추가
        model.addAttribute("superAdmins", superAdmins);
        model.addAttribute("operators", operators);
        model.addAttribute("uploaders", uploaders);
        model.addAttribute("pendings", pendings);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        // 관리자로 부여 가능한 롤만 골라서 넘기기
        List<AdminRole> availableRoles = Arrays.stream(AdminRole.values())
            .filter(r -> r != AdminRole.COMMON)
            .collect(Collectors.toList());
        model.addAttribute("availableRoles", availableRoles);

        // 페이지 제목 및 활성 메뉴 설정
        model.addAttribute("searchRequest", request);
        model.addAttribute("pageTitle", "앱 유저관리 - NaraSarang Admin");
        model.addAttribute("activeMenu", "users");
        model.addAttribute("activeSubmenu", "admin");

        // 사용자 정보 설정 (공통)
        model.addAttribute("userName", user.getUsername());
        model.addAttribute("userRole", user.getAuthorities());
        model.addAttribute("userRoleDisplay", user.getAdmin().getRole().getDisplayName());
        model.addAttribute("userAvatar", null);

        return "admin/user/adminList";
    }
}
