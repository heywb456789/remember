package com.tomato.naraclub.admin.user.dto;

import com.tomato.naraclub.common.code.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.dto
 * @fileName : UserUpdateRequest
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserUpdateRequest {
    private String reason;
    private MemberStatus status;

}
