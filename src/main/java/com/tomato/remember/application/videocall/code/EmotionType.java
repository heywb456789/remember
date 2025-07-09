package com.tomato.remember.application.videocall.code;

public enum EmotionType {
    HAPPY("기쁨"),
    SAD("슬픔"),
    ANGRY("분노"),
    SURPRISED("놀람"),
    FEAR("두려움"),
    DISGUST("혐오"),
    NEUTRAL("중립"),
    EXCITED("흥분"),
    CALM("평온"),
    NOSTALGIC("그리움"),
    GRATEFUL("감사"),
    WORRIED("걱정"),
    HOPEFUL("희망"),
    LONELY("외로움"),
    PEACEFUL("평화"),
    CONFUSED("혼란"),
    PROUD("자부심"),
    EMBARRASSED("당황"),
    DISAPPOINTED("실망"),
    RELIEVED("안도");
    
    private final String displayName;
    
    EmotionType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}