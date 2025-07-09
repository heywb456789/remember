package com.tomato.remember.application.member.code;

public enum ActivityStatus {
    ACTIVE("활성"),
    ARCHIVED("보관됨"),
    DELETED("삭제됨");
    
    private final String displayName;
    
    ActivityStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}