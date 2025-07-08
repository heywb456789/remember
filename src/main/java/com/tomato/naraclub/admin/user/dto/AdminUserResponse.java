package com.tomato.naraclub.admin.user.dto;

import com.tomato.naraclub.admin.user.code.AdminRole;
import com.tomato.naraclub.admin.user.code.AdminStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.dto
 * @fileName : AdminUserResponse
 * @date : 2025-05-12
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {

    private Long adminId;
    private String loginId;
    private String name;
    private String email;
    private String phoneNumber;
    private AdminRole role;
    private AdminStatus status;
    private String profileImage;
    private LocalDateTime lastAccessAt;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;

}
