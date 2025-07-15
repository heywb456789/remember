package com.tomato.remember.application.family.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * 권한 수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FamilyPermissionRequest {

    /**
     * 메모리얼 접근 권한
     */
    @NotNull(message = "메모리얼 접근 권한 설정은 필수입니다.")
    private Boolean memorialAccess;

    /**
     * 영상통화 접근 권한
     */
    @NotNull(message = "영상통화 접근 권한 설정은 필수입니다.")
    private Boolean videoCallAccess;

    /**
     * 권한 유효성 검사
     * 영상통화 권한이 true면 메모리얼 접근 권한도 true여야 함
     */
    public boolean isValid() {
        if (videoCallAccess && !memorialAccess) {
            return false;
        }
        return true;
    }

    /**
     * 권한 유효성 검사 메시지
     */
    public String getValidationMessage() {
        if (!isValid()) {
            return "영상통화 권한을 허용하려면 메모리얼 접근 권한도 허용해야 합니다.";
        }
        return null;
    }

    /**
     * 모든 권한 허용
     */
    public static FamilyPermissionRequest allowAll() {
        return new FamilyPermissionRequest(true, true);
    }

    /**
     * 모든 권한 차단
     */
    public static FamilyPermissionRequest denyAll() {
        return new FamilyPermissionRequest(false, false);
    }

    /**
     * 메모리얼 접근만 허용
     */
    public static FamilyPermissionRequest memorialOnly() {
        return new FamilyPermissionRequest(true, false);
    }

    /**
     * 권한 요약 문자열
     */
    public String getSummary() {
        if (memorialAccess && videoCallAccess) {
            return "모든 권한 허용";
        } else if (memorialAccess && !videoCallAccess) {
            return "메모리얼 접근만 허용";
        } else {
            return "모든 권한 차단";
        }
    }

    /**
     * 권한 변경 사항 확인
     */
    public boolean hasChanges(Boolean currentMemorialAccess, Boolean currentVideoCallAccess) {
        return !memorialAccess.equals(currentMemorialAccess) || 
               !videoCallAccess.equals(currentVideoCallAccess);
    }

    /**
     * 권한 레벨 비교 (높은 숫자가 더 많은 권한)
     */
    public int getPermissionLevel() {
        if (memorialAccess && videoCallAccess) {
            return 2; // 최고 권한
        } else if (memorialAccess) {
            return 1; // 중간 권한
        } else {
            return 0; // 최소 권한
        }
    }

    /**
     * 현재 권한보다 높은 권한인지 확인
     */
    public boolean isUpgrade(Boolean currentMemorialAccess, Boolean currentVideoCallAccess) {
        FamilyPermissionRequest current = new FamilyPermissionRequest(currentMemorialAccess, currentVideoCallAccess);
        return this.getPermissionLevel() > current.getPermissionLevel();
    }

    /**
     * 현재 권한보다 낮은 권한인지 확인
     */
    public boolean isDowngrade(Boolean currentMemorialAccess, Boolean currentVideoCallAccess) {
        FamilyPermissionRequest current = new FamilyPermissionRequest(currentMemorialAccess, currentVideoCallAccess);
        return this.getPermissionLevel() < current.getPermissionLevel();
    }
}