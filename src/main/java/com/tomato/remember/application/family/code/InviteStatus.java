package com.tomato.remember.application.family.code;

/**
 * 가족 초대 상태
 */
public enum InviteStatus {
    PENDING("대기중", "초대가 발송되었지만 아직 응답하지 않은 상태"),
    ACCEPTED("수락", "초대를 수락하여 가족 구성원으로 등록된 상태"),
    REJECTED("거절", "초대를 거절한 상태"),
    EXPIRED("만료", "초대 기간이 만료된 상태"),
    CANCELLED("취소", "초대를 보낸 사람이 취소한 상태");

    private final String displayName;
    private final String description;

    InviteStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 활성 상태인지 확인 (가족 구성원으로 인정되는 상태)
     */
    public boolean isActive() {
        return this == ACCEPTED;
    }

    /**
     * 응답 대기 상태인지 확인
     */
    public boolean isPending() {
        return this == PENDING;
    }

    /**
     * 완료된 상태인지 확인 (더 이상 변경 불가)
     */
    public boolean isCompleted() {
        return this == ACCEPTED || this == REJECTED || this == EXPIRED || this == CANCELLED;
    }
}