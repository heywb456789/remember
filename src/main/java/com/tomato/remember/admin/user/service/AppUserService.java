package com.tomato.remember.admin.user.service;

import com.tomato.remember.admin.security.AdminUserDetails;
import com.tomato.remember.admin.user.dto.AppUserListRequest;
import com.tomato.remember.admin.user.dto.AppUserResponse;
import com.tomato.remember.admin.user.dto.UserActivityListResponse;
import com.tomato.remember.admin.user.dto.UserUpdateRequest;
import com.tomato.remember.application.auth.entity.MemberLoginHistory;
import com.tomato.remember.common.dto.ListDTO;
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
