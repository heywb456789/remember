package com.tomato.naraclub.admin.user.dto;

import com.tomato.naraclub.admin.user.code.AdminRole;
import com.tomato.naraclub.admin.user.code.AdminStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.dto
 * @fileName : AdminAuthorityRequest
 * @date : 2025-05-13
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminAuthorityRequest {

    private Long adminId;
    private String reason;
    private AdminRole role;
    private AdminStatus status;
}
