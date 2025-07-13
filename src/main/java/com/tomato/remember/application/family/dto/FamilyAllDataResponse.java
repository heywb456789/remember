package com.tomato.remember.application.family.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * 앱 전용: 전체 가족 관리 데이터 응답 DTO
 * SSR과 동일한 데이터를 JSON 형태로 제공
 */
@Getter
@Builder
@ToString
public class FamilyAllDataResponse {

    /**
     * 접근 가능한 모든 메모리얼 목록
     */
    private List<MemorialInfo> memorials;

    /**
     * 전체 가족 구성원 목록
     */
    private List<FamilyMemberResponse> familyMembers;

    /**
     * 통계 정보
     */
    private StatisticsInfo statistics;

    /**
     * 메모리얼 정보
     */
    @Getter
    @Builder
    public static class MemorialInfo {
        private Long id;
        private String name;
        private String nickname;
        private String mainProfileImageUrl;
        private boolean isActive;
        private long familyMemberCount;
    }

    /**
     * 통계 정보
     */
    @Getter
    @Builder
    public static class StatisticsInfo {
        private int totalMemorials;      // 전체 메모리얼 수
        private int totalMembers;        // 전체 가족 구성원 수
        private int activeMembers;       // 활성 가족 구성원 수
        private int pendingInvitations;  // 대기 중인 초대 수
    }

    // ===== 편의 메서드 =====

    /**
     * 빈 응답 생성
     */
    public static FamilyAllDataResponse empty() {
        return FamilyAllDataResponse.builder()
            .memorials(List.of())
            .familyMembers(List.of())
            .statistics(StatisticsInfo.builder()
                .totalMemorials(0)
                .totalMembers(0)
                .activeMembers(0)
                .pendingInvitations(0)
                .build())
            .build();
    }

    /**
     * 메모리얼 개수 조회
     */
    public int getMemorialCount() {
        return memorials != null ? memorials.size() : 0;
    }

    /**
     * 가족 구성원 개수 조회
     */
    public int getFamilyMemberCount() {
        return familyMembers != null ? familyMembers.size() : 0;
    }

    /**
     * 데이터 존재 여부 확인
     */
    public boolean hasData() {
        return getMemorialCount() > 0 || getFamilyMemberCount() > 0;
    }
}