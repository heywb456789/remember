package com.tomato.remember.application.memorial.code;

public enum MemorialStatus {
    DRAFT("임시저장"),       // 👈 신규 추가
    ACTIVE("활성"),
    INACTIVE("비활성"),
    DELETED("삭제");
    
    private final String displayName;
    
    MemorialStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}