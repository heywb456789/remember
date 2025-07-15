package com.tomato.remember.application.family.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 가족 초대 상태
 */
@Getter
@RequiredArgsConstructor
public enum InviteStatus {
    /**
     * 초대 대기 중
     */
    PENDING("PENDING", "초대 대기 중", "초대 링크가 전송되었습니다"),

    /**
     * 초대 수락됨
     */
    ACCEPTED("ACCEPTED", "수락됨", "가족 구성원으로 등록되었습니다"),

    /**
     * 초대 거절됨
     */
    REJECTED("REJECTED", "거절됨", "초대가 거절되었습니다"),

    /**
     * 초대 만료됨
     */
    EXPIRED("EXPIRED", "만료됨", "초대 링크가 만료되었습니다"),

    /**
     * 초대 취소됨
     */
    CANCELLED("CANCELLED", "취소됨", "초대가 취소되었습니다");

    private final String code;
    private final String displayName;
    private final String description;

    // ===== 상태 확인 메서드 =====

    /**
     * 대기 중인지 확인
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * 활성 상태인지 확인 (수락된 상태)
     */
    public boolean isActive() {
        return this == ACCEPTED;
    }

    /**
     * 완료된 상태인지 확인 (수락 또는 거절)
     */
    public boolean isCompleted() {
        return this == ACCEPTED || this == REJECTED;
    }

    /**
     * 종료된 상태인지 확인 (만료 또는 취소)
     */
    public boolean isTerminated() {
        return this == EXPIRED || this == CANCELLED;
    }

    /**
     * 유효한 상태인지 확인 (대기 중이거나 수락됨)
     */
    public boolean isValid() {
        return this == PENDING || this == ACCEPTED;
    }

    /**
     * 응답 가능한 상태인지 확인 (대기 중)
     */
    public boolean canRespond() {
        return this == PENDING;
    }

    /**
     * 취소 가능한 상태인지 확인 (대기 중)
     */
    public boolean canCancel() {
        return this == PENDING;
    }

    // ===== 상태 변경 가능 여부 확인 =====

    /**
     * 수락으로 변경 가능한지 확인
     */
    public boolean canAccept() {
        return this == PENDING;
    }

    /**
     * 거절로 변경 가능한지 확인
     */
    public boolean canReject() {
        return this == PENDING;
    }

    /**
     * 만료로 변경 가능한지 확인
     */
    public boolean canExpire() {
        return this == PENDING;
    }

    /**
     * 취소로 변경 가능한지 확인
     */
    public boolean canBeCancelled() {
        return this == PENDING;
    }

    // ===== 코드 변환 메서드 =====

    /**
     * 코드로 InviteStatus 찾기
     */
    public static InviteStatus fromCode(String code) {
        for (InviteStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown invite status code: " + code);
    }

    /**
     * 표시명으로 InviteStatus 찾기
     */
    public static InviteStatus fromDisplayName(String displayName) {
        for (InviteStatus status : values()) {
            if (status.getDisplayName().equals(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown invite status display name: " + displayName);
    }

    // ===== 상태 그룹 확인 =====

    /**
     * 진행 중인 상태들 (처리 가능한 상태)
     */
    public static InviteStatus[] getActiveStatuses() {
        return new InviteStatus[]{PENDING, ACCEPTED};
    }

    /**
     * 완료된 상태들 (더 이상 처리 불가능한 상태)
     */
    public static InviteStatus[] getCompletedStatuses() {
        return new InviteStatus[]{ACCEPTED, REJECTED, EXPIRED, CANCELLED};
    }

    /**
     * 실패한 상태들 (부정적인 결과)
     */
    public static InviteStatus[] getFailedStatuses() {
        return new InviteStatus[]{REJECTED, EXPIRED, CANCELLED};
    }

    // ===== 상태별 CSS 클래스 =====

    /**
     * CSS 클래스명 반환 (UI 표시용)
     */
    public String getCssClass() {
        switch (this) {
            case PENDING:
                return "status-pending";
            case ACCEPTED:
                return "status-accepted";
            case REJECTED:
                return "status-rejected";
            case EXPIRED:
                return "status-expired";
            case CANCELLED:
                return "status-cancelled";
            default:
                return "status-unknown";
        }
    }

    /**
     * 부트스트rap 배지 클래스명 반환
     */
    public String getBadgeClass() {
        switch (this) {
            case PENDING:
                return "badge bg-warning";
            case ACCEPTED:
                return "badge bg-success";
            case REJECTED:
                return "badge bg-danger";
            case EXPIRED:
                return "badge bg-secondary";
            case CANCELLED:
                return "badge bg-dark";
            default:
                return "badge bg-light";
        }
    }

    // ===== 상태 전환 매트릭스 =====

    /**
     * 다음 가능한 상태 목록 반환
     */
    public InviteStatus[] getNextPossibleStatuses() {
        switch (this) {
            case PENDING:
                return new InviteStatus[]{ACCEPTED, REJECTED, EXPIRED, CANCELLED};
            case ACCEPTED:
            case REJECTED:
            case EXPIRED:
            case CANCELLED:
                return new InviteStatus[]{};
            default:
                return new InviteStatus[]{};
        }
    }

    /**
     * 특정 상태로 전환 가능한지 확인
     */
    public boolean canTransitionTo(InviteStatus targetStatus) {
        InviteStatus[] possibleStatuses = getNextPossibleStatuses();
        for (InviteStatus status : possibleStatuses) {
            if (status == targetStatus) {
                return true;
            }
        }
        return false;
    }
}