package com.tomato.naraclub.admin.user.service;

import com.tomato.naraclub.admin.security.AdminUserDetails;
import com.tomato.naraclub.admin.user.dto.AdminAuthorityRequest;
import com.tomato.naraclub.admin.user.dto.AdminUserListRequest;
import com.tomato.naraclub.admin.user.dto.AdminUserResponse;
import com.tomato.naraclub.admin.user.dto.AppUserResponse;
import com.tomato.naraclub.common.dto.ListDTO;
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
