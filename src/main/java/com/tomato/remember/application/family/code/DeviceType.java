package com.tomato.remember.application.family.code;

public enum DeviceType {
    MOBILE("모바일"),
    TABLET("태블릿"),
    DESKTOP("데스크톱"),
    OTHER("기타");
    
    private final String displayName;
    
    DeviceType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}