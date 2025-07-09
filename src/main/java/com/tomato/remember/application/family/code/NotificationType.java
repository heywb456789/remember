package com.tomato.remember.application.family.code;

public enum NotificationType {
    MEMORIAL_ACTIVITY("추모 활동"),
    PAYMENT_RELATED("결제 관련"),
    FAMILY_ACTIVITY("가족 활동"),
    MARKETING("마케팅"),
    SYSTEM_NOTICE("시스템 공지"),
    EMERGENCY("긴급");
    
    private final String displayName;
    
    NotificationType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}