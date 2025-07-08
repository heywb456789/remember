package com.tomato.remember.admin.user.dto;

import com.tomato.remember.admin.user.code.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.dto
 * @fileName : UserActivityResponse
 * @date : 2025-05-14
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityResponse {

    private Long memberId;

    private ActivityType type ;




}
