package com.tomato.remember.application.member.code;

public enum ActivityCategory {
    MEMORIAL("메모리얼"),
    VIDEO_CALL("영상통화"),
    FAMILY("가족"),
    PROFILE("프로필"),
    AUTH("인증"),
    FILE("파일"),
    SETTING("설정"),
    FEEDBACK("피드백"),
    ERROR("오류"),
    OTHER("기타");
    
    private final String displayName;
    
    ActivityCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}