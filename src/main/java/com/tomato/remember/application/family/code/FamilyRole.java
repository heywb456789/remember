package com.tomato.remember.application.family.code;

public enum FamilyRole {
    MAIN("주계정"),
    SUB("부계정");
    
    private final String displayName;
    
    FamilyRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}