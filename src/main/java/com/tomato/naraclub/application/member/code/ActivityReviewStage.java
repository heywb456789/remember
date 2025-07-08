package com.tomato.naraclub.application.member.code;

public enum ActivityReviewStage {
//    SUBMITTED("제출완료"),
    PENDING_REVIEW("지급검토중"),
    APPROVED("지급완료"),
    REJECTED("지급거절"),
    ;

    private final String displayName;

    ActivityReviewStage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
