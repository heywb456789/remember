package com.tomato.remember.application.family.code;

public enum FeedbackCategory {
    GENERAL("일반"),
    BUG_REPORT("버그 신고"),
    FEATURE_REQUEST("기능 요청"),
    IMPROVEMENT("개선 제안"),
    AI_QUALITY("AI 품질"),
    VIDEO_QUALITY("영상 품질"),
    AUDIO_QUALITY("음성 품질"),
    UI_UX("UI/UX"),
    PERFORMANCE("성능"),
    COMPATIBILITY("호환성"),
    SECURITY("보안"),
    OTHER("기타");
    
    private final String displayName;
    
    FeedbackCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}