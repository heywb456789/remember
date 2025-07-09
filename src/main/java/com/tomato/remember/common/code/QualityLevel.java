package com.tomato.remember.common.code;

public enum QualityLevel {
    EXCELLENT(90, "최고"),
    GOOD(80, "좋음"),
    FAIR(70, "보통"),
    POOR(60, "나쁨"),
    VERY_POOR(0, "매우 나쁨");
    
    private final int threshold;
    private final String displayName;
    
    QualityLevel(int threshold, String displayName) {
        this.threshold = threshold;
        this.displayName = displayName;
    }
    
    public int getThreshold() {
        return threshold;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static QualityLevel fromScore(int score) {
        if (score >= EXCELLENT.threshold) {
            return EXCELLENT;
        }
        if (score >= GOOD.threshold) {
            return GOOD;
        }
        if (score >= FAIR.threshold) {
            return FAIR;
        }
        if (score >= POOR.threshold) {
            return POOR;
        }
        return VERY_POOR;
    }
}