package com.tomato.remember.application.family.dto;

import com.tomato.remember.application.memorial.entity.Memorial;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * @author : MinjaeKim
 * @packageName : com.tomato.remember.application.family.dto
 * @fileName : MemorialSummaryResponse
 * @date : 2025-07-15
 * @description :
 * @AUTHOR : MinjaeKim
 */
@Getter
@Builder
@AllArgsConstructor
public class MemorialSummaryResponse {

    private Long id;
    private String name;
    private String nickname;
    private String mainProfileImageUrl;
    private String relationship;
    private String relationshipDisplayName;
    private LocalDate birthDate;
    private LocalDate deathDate;
    private boolean isActive;
    private int familyMemberCount;
    private int activeFamilyMemberCount;

    /**
     * Memorial 엔티티에서 요약 정보 생성
     */
    public static MemorialSummaryResponse from(Memorial memorial) {
        return MemorialSummaryResponse.builder()
                .id(memorial.getId())
                .name(memorial.getName())
                .nickname(memorial.getNickname())
                .mainProfileImageUrl(memorial.getMainProfileImageUrl())
                .relationship(memorial.getRelationship().name())
                .relationshipDisplayName(memorial.getRelationship().getDisplayName())
                .birthDate(memorial.getBirthDate())
                .deathDate(memorial.getDeathDate())
                .isActive(memorial.isActive())
                .familyMemberCount(memorial.getFamilyMembers().size())
                .activeFamilyMemberCount(memorial.getActiveFamilyMemberCount())
                .build();
    }
}
