package com.tomato.remember.common.code;

public enum MemberStatus {
    BLOCKED("차단"),
    ACTIVE("활성"),
    DELETED("삭제");

    private final String displayName;

    MemberStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
