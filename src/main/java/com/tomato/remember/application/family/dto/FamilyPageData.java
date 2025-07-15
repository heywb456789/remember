package com.tomato.remember.application.family.dto;

import com.tomato.remember.application.memorial.entity.Memorial;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FamilyPageData {
    private List<Memorial> memorials;
    private Memorial selectedMemorial;
    private List<FamilyMemberResponse> familyMembers;
    private int totalMemorials;
    private int totalMembers;
}