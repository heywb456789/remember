package com.tomato.remember.application.family.dto;

import com.tomato.remember.application.member.code.Relationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * 가족 구성원 초대 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class FamilyInviteRequest {

    @NotNull(message = "메모리얼 ID는 필수입니다.")
    private Long memorialId;

    @NotBlank(message = "초대 방법은 필수입니다.")
    @Pattern(regexp = "^(email|sms)$", message = "초대 방법은 email 또는 sms만 가능합니다.")
    private String method;

    @NotBlank(message = "연락처는 필수입니다.")
    private String contact;

    @NotNull(message = "관계는 필수입니다.")
    private Relationship relationship;

    private String message;
}