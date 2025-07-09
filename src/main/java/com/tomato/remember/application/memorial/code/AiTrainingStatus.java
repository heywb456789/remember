package com.tomato.remember.application.memorial.code;

public enum AiTrainingStatus {
    PENDING("대기중"),
    IN_PROGRESS("진행중"),
    COMPLETED("완료"),
    FAILED("실패");
    
    private final String displayName;
    
    AiTrainingStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}