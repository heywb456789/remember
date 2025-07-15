package com.tomato.remember.application.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.remember.application.member.dto
 * @fileName : MyInfoDTO
 * @date : 2025-07-15
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyInfoDTO {
    private MemberDTO member;
    private ProfileSettingsDTO profile;
}
