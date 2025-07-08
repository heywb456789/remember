package com.tomato.naraclub.admin.user.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.admin.user.dto
 * @fileName : UserActivityListResponse
 * @date : 2025-05-20
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityListResponse {
    private List<ActivityResponse> activities;
    private boolean                hasMore;
}
