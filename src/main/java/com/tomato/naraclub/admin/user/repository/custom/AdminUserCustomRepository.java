package com.tomato.naraclub.admin.user.repository.custom;

import com.tomato.naraclub.admin.security.AdminUserDetails;
import com.tomato.naraclub.admin.user.dto.AdminUserListRequest;
import com.tomato.naraclub.admin.user.dto.AdminUserResponse;
import com.tomato.naraclub.common.dto.ListDTO;
import org.springframework.data.domain.Pageable;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.repository.custom
 * @fileName : AdminUserCustomRepository
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
public interface AdminUserCustomRepository {

    ListDTO<AdminUserResponse> getAdminUserList(AdminUserListRequest request, AdminUserDetails user, Pageable pageable);
}
