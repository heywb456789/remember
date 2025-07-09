package com.tomato.remember.application.videocall.code;

public enum CallType {
    NORMAL("일반통화"),
    EMERGENCY("긴급통화"),
    SCHEDULED("예약통화");
    
    private final String displayName;
    
    CallType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}