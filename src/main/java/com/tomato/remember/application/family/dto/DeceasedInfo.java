package com.tomato.remember.application.family.dto;

import com.tomato.remember.application.family.entity.FamilyMember;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeceasedInfo {

    private String personality;
    private String hobbies;
    private String favoriteFood;
    private String specialMemories;
    private String speechHabits;
    private boolean hasDeceasedInfo;
    private boolean hasRequiredDeceasedInfo;
    private int filledFieldCount;

}