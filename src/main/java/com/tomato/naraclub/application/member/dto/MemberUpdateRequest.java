package com.tomato.naraclub.application.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.naraclub.application.member.dto
 * @fileName : MemberUpdateRequest
 * @date : 2025-05-19
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberUpdateRequest {

    private String name;
}
