package com.tomato.remember.application.family.dto;

import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FamilyPageResponse {
    
    private List<MemorialSummaryResponse> memorials;
    private MemorialSummaryResponse selectedMemorial;
    private List<FamilyMemberResponse> familyMembers;
    private int totalMemorials;
    private int totalMembers;
    private String message;

    /**
     * FamilyPageData에서 API 응답 생성
     */
    public static FamilyPageResponse from(FamilyPageData pageData) {
        return FamilyPageResponse.builder()
                .memorials(pageData.getMemorials().stream()
                        .map(MemorialSummaryResponse::from)
                        .collect(Collectors.toList()))
                .selectedMemorial(pageData.getSelectedMemorial() != null 
                        ? MemorialSummaryResponse.from(pageData.getSelectedMemorial()) 
                        : null)
                .familyMembers(pageData.getFamilyMembers())
                .totalMemorials(pageData.getTotalMemorials())
                .totalMembers(pageData.getTotalMembers())
                .message(pageData.getMemorials().isEmpty() 
                        ? "등록된 메모리얼이 없습니다." 
                        : null)
                .build();
    }
}