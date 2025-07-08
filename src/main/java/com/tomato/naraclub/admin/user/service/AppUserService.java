package com.tomato.naraclub.admin.user.service;

import com.tomato.naraclub.admin.security.AdminUserDetails;
import com.tomato.naraclub.admin.user.dto.AppUserListRequest;
import com.tomato.naraclub.admin.user.dto.AppUserResponse;
import com.tomato.naraclub.admin.user.dto.UserActivityListResponse;
import com.tomato.naraclub.admin.user.dto.UserLoginHistoryResponse;
import com.tomato.naraclub.admin.user.dto.UserUpdateRequest;
import com.tomato.naraclub.application.auth.entity.MemberLoginHistory;
import com.tomato.naraclub.common.dto.ListDTO;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.service
 * @fileName : AppUserService
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
public interface AppUserService {

    ListDTO<AppUserResponse> getAppUserList(AdminUserDetails user, AppUserListRequest request, Pageable pageable);

    AppUserResponse updateUserVerified(Long id, AdminUserDetails userDetails, UserUpdateRequest request);

    AppUserResponse getAppUserDetail(long id);

    Page<MemberLoginHistory> getAppUserLoginHistory(long id, int page, int size);

    UserActivityListResponse getUserActivities(Long id, int page, int size, String type);
}
