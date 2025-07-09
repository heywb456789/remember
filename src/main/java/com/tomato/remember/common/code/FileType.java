package com.tomato.remember.common.code;

public enum FileType {
    IMAGE("이미지"),
    AUDIO("음성"),
    VIDEO("영상"),
    DOCUMENT("문서"),
    OTHER("기타");
    
    private final String displayName;
    
    FileType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}