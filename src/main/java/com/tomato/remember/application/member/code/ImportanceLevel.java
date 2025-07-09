package com.tomato.remember.application.member.code;

public enum ImportanceLevel {
    LOW(1, "낮음"),
    NORMAL(2, "보통"),
    HIGH(3, "높음"),
    CRITICAL(4, "중요"),
    URGENT(5, "긴급");
    
    private final int level;
    private final String displayName;
    
    ImportanceLevel(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static ImportanceLevel fromLevel(int level) {
        for (ImportanceLevel importance : values()) {
            if (importance.level == level) {
                return importance;
            }
        }
        return NORMAL;
    }
}