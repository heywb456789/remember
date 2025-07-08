package com.tomato.remember.admin.user.service;

import com.tomato.remember.admin.security.AdminUserDetails;
import com.tomato.remember.admin.user.dto.AdminAuthorityRequest;
import com.tomato.remember.admin.user.dto.AdminUserListRequest;
import com.tomato.remember.admin.user.dto.AdminUserResponse;
import com.tomato.remember.common.dto.ListDTO;
import org.springframework.data.domain.Pageable;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.service
 * @fileName : AdminUserService
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
public interface AdminUserService {

    ListDTO<AdminUserResponse> getAdminUserList(AdminUserDetails user, AdminUserListRequest request, Pageable pageable);

    AdminUserResponse approveUser(Long id, AdminUserDetails userDetails, AdminAuthorityRequest request);

    AdminUserResponse updateAdminUserRole(Long id, AdminUserDetails userDetails, AdminAuthorityRequest request);

    AdminUserResponse updateAdminStatus(Long id, AdminUserDetails userDetails, AdminAuthorityRequest request);
}
