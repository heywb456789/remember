package com.tomato.remember.application.family.code;

public enum InviteStatus {
    PENDING("대기중"),
    ACCEPTED("수락됨"),
    REJECTED("거절됨"),
    EXPIRED("만료됨");
    
    private final String displayName;
    
    InviteStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}