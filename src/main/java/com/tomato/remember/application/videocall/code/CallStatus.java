package com.tomato.remember.application.videocall.code;

public enum CallStatus {
    WAITING("대기중"),
    CONNECTING("연결중"),
    IN_PROGRESS("통화중"),
    COMPLETED("완료"),
    FAILED("실패"),
    CANCELLED("취소");
    
    private final String displayName;
    
    CallStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}