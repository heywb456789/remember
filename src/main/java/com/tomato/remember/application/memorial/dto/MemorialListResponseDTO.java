package com.tomato.remember.application.memorial.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorialListResponseDTO {
    
    private Long memorialId;
    private String name;
    private String nickname;
    private String mainProfileImageUrl;
    private LocalDate lastVisitAt;
    private Integer totalVisits;
    private Integer memoryCount;
    private Boolean aiTrainingCompleted;
    private String formattedAge;
    private String relationshipDescription;
    
    // 영상통화 가능 여부
    private Boolean canStartVideoCall;
    private Boolean hasRequiredProfileImages;
    
    // 파일 업로드 상태
    private Boolean hasRequiredFiles;
    private Integer profileImageCount;
    private Integer voiceFileCount;
    private Integer videoFileCount;

    // 접근 권한 관련 정보 (새로 추가)
    private Boolean isOwner;           // 소유자 여부
    private Boolean canAccess;         // 접근 권한 여부
    private String accessType;         // 접근 타입 (OWNER, FAMILY_MEMBER)
    private Boolean canVideoCall;      // 영상통화 권한 여부
    private Boolean canModify;         // 수정 권한 여부

    // 가족 구성원으로서의 관계 정보 (가족 구성원인 경우)
    private String familyRelationship;        // 가족 구성원으로서의 관계
    private String familyRelationshipDisplay; // 가족 구성원으로서의 관계 표시명

    /**
     * 접근 타입 설정
     */
    public void setAccessType(String accessType) {
        this.accessType = accessType;
        this.isOwner = "OWNER".equals(accessType);
        this.canModify = this.isOwner; // 소유자만 수정 가능
    }

    /**
     * 영상통화 가능 여부 확인 (메모리얼 상태 + 개인 권한)
     */
    public Boolean getCanVideoCall() {
        return canStartVideoCall && canAccess && (canVideoCall != null ? canVideoCall : true);
    }

    /**
     * 표시용 관계 정보 반환
     * - 소유자인 경우: 원래 관계 (relationshipDescription)
     * - 가족 구성원인 경우: 가족 구성원으로서의 관계 (familyRelationshipDisplay)
     */
    public String getDisplayRelationship() {
        if (isOwner) {
            return relationshipDescription;
        } else if (familyRelationshipDisplay != null) {
            return familyRelationshipDisplay;
        }
        return relationshipDescription;
    }

    /**
     * 접근 권한 상태 텍스트 반환
     */
    public String getAccessStatusText() {
        if (isOwner) {
            return "소유자";
        } else if (canAccess) {
            return "가족 구성원";
        } else {
            return "접근 불가";
        }
    }

    /**
     * 메모리얼 카드에 표시할 부제목 생성
     */
    public String getSubtitle() {
        StringBuilder subtitle = new StringBuilder();

        // 접근 타입 표시
        subtitle.append(getAccessStatusText());

        // 관계 정보 추가
        String relationship = getDisplayRelationship();
        if (relationship != null && !relationship.isEmpty()) {
            subtitle.append(" • ").append(relationship);
        }

        // 나이 정보 추가
        if (formattedAge != null && !formattedAge.contains("정보 없음")) {
            subtitle.append(" • ").append(formattedAge);
        }

        return subtitle.toString();
    }
}