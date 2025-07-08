package com.tomato.remember.admin.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.remember.admin.auth.dto
 * @fileName : AdminInfo
 * @date : 2025-07-08
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@Builder
public class AdminInfo {

    private Long id;
    private String username;
    private String role;
}
