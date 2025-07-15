package com.tomato.remember.application.family.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FamilySearchCondition {
    private String keyword;
    private Long memorialId;
    private String relationship;
    private String inviteStatus;
}