package com.tomato.naraclub.admin.user.repository.custom;

import com.tomato.naraclub.admin.security.AdminUserDetails;
import com.tomato.naraclub.admin.user.dto.AppUserListRequest;
import com.tomato.naraclub.admin.user.dto.AppUserResponse;
import com.tomato.naraclub.common.dto.ListDTO;
import org.springframework.data.domain.Pageable;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.repository.custom
 * @fileName : AppUserCustomRepository
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
public interface AppUserCustomRepository {

    ListDTO<AppUserResponse> getAppUserList(AdminUserDetails user, AppUserListRequest request, Pageable pageable);
}
