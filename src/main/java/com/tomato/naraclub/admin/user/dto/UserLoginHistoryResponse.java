package com.tomato.naraclub.admin.user.dto;

import com.tomato.naraclub.application.auth.code.LoginType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.dto
 * @fileName : UserLoginHistoryResponse
 * @date : 2025-05-13
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLoginHistoryResponse {

    private Long memberId;
    private LoginType type;
    private String ipAddress;
    private String deviceType;
    private String userAgent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
