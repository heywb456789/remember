package com.tomato.remember.application.family.code;

public enum InviteMethod {
    SMS("문자메시지"),
    EMAIL("이메일");
    
    private final String displayName;
    
    InviteMethod(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}