package com.tomato.naraclub.admin.user.service;

import com.tomato.naraclub.admin.security.AdminUserDetails;
import com.tomato.naraclub.admin.user.code.AdminRole;
import com.tomato.naraclub.admin.user.code.AdminStatus;
import com.tomato.naraclub.admin.user.dto.AdminAuthorityRequest;
import com.tomato.naraclub.admin.user.dto.AdminUserListRequest;
import com.tomato.naraclub.admin.user.dto.AdminUserResponse;
import com.tomato.naraclub.admin.user.dto.AppUserResponse;
import com.tomato.naraclub.admin.user.entity.Admin;
import com.tomato.naraclub.admin.user.entity.AuthorityHistory;
import com.tomato.naraclub.admin.user.repository.AdminUserRepository;
import com.tomato.naraclub.admin.user.repository.AuthorityHistoryRepository;
import com.tomato.naraclub.common.code.ResponseStatus;
import com.tomato.naraclub.common.dto.ListDTO;
import com.tomato.naraclub.common.exception.APIException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.service
 * @fileName : AdminUserServiceImpl
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final AuthorityHistoryRepository authorityHistoryRepository;

    @Override
    public ListDTO<AdminUserResponse> getAdminUserList(AdminUserDetails user,
        AdminUserListRequest request, Pageable pageable) {
        return adminUserRepository.getAdminUserList(request, user, pageable);
    }

    // 승인 대기중인 유저만 승인
    @Override
    @Transactional
    public AdminUserResponse approveUser(Long id, AdminUserDetails userDetails,
        AdminAuthorityRequest request) {

        Admin admin = adminUserRepository.findByIdAndRole(id, AdminRole.COMMON).
            orElseThrow(() -> new APIException(ResponseStatus.USER_NOT_EXIST));

        //1. 승인 - 권한 변경 + active
        admin.updateRole(request.getRole());
        admin.approveStatus();

        //2. 히스토리
        authorityHistoryRepository.save(AuthorityHistory.builder()
            .reason(request.getReason())
            .adminId(admin.getId())
            .adminStatus(AdminStatus.ACTIVE)
            .adminRole(request.getRole())
            .updatedBy(userDetails.getId())
            .createdBy(userDetails.getId())
            .build());

        return admin.convertAdminUserResponse();
    }

    @Override
    @Transactional
    public AdminUserResponse updateAdminUserRole(Long id, AdminUserDetails userDetails,
        AdminAuthorityRequest request) {
        Admin admin = adminUserRepository.findByIdAndStatus(id, AdminStatus.ACTIVE).
            orElseThrow(() -> new APIException(ResponseStatus.USER_NOT_EXIST));


        long activeCount = adminUserRepository.countByRoleAndStatus(request.getRole(), AdminStatus.ACTIVE);

        switch (request.getRole()) {
            case AdminRole.SUPER_ADMIN:
                if(activeCount >= 3) throw new APIException(ResponseStatus.CANNOT_GRANT_SUPER_ADMIN);
                break;
            case AdminRole.OPERATOR:
                if(activeCount >= 10) throw new APIException(ResponseStatus.CANNOT_GRANT_OPERATOR);
                break;
            case AdminRole.UPLOADER:
                if(activeCount >= 10) throw new APIException(ResponseStatus.CANNOT_GRANT_UPLOADER);
                break;
            default:
                throw new APIException(ResponseStatus.BAD_REQUEST);
        }

        //1. 권한 변경
        admin.updateRole(request.getRole());

        //2.history
        authorityHistoryRepository.save(AuthorityHistory.builder()
            .reason(request.getReason())
            .adminId(admin.getId())
            .adminStatus(AdminStatus.ACTIVE)
            .adminRole(request.getRole())
            .updatedBy(userDetails.getId())
            .createdBy(userDetails.getId())
            .build());

        return admin.convertAdminUserResponse();
    }

    @Override
    public AdminUserResponse updateAdminStatus(Long id, AdminUserDetails userDetails,
        AdminAuthorityRequest request) {
        Admin admin = adminUserRepository.findById(id).
            orElseThrow(() -> new APIException(ResponseStatus.USER_NOT_EXIST));

        //1. 권한 변경
        admin.updateStatus(request.getStatus());

        //2.history
        authorityHistoryRepository.save(AuthorityHistory.builder()
            .reason(request.getReason())
            .adminId(admin.getId())
            .adminStatus(request.getStatus())
            .adminRole(request.getRole())
            .updatedBy(userDetails.getId())
            .createdBy(userDetails.getId())
            .build());

        return admin.convertAdminUserResponse();
    }
}
