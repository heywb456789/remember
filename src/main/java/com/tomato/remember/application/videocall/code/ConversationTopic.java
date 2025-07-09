package com.tomato.remember.application.videocall.code;

public enum ConversationTopic {
    GREETING("인사"),
    DAILY_LIFE("일상"),
    MEMORY("추억"),
    FAMILY("가족"),
    HEALTH("건강"),
    WEATHER("날씨"),
    FOOD("음식"),
    HOBBY("취미"),
    TRAVEL("여행"),
    WORK("일"),
    EMOTION("감정"),
    ADVICE("조언"),
    GRATITUDE("감사"),
    WORRY("걱정"),
    CELEBRATION("축하"),
    COMFORT("위로"),
    ENCOURAGEMENT("격려"),
    NEWS("근황"),
    PLAN("계획"),
    DREAM("꿈"),
    REGRET("후회"),
    HOPE("희망"),
    LOVE("사랑"),
    MISSING("그리움"),
    PRAYER("기도"),
    BLESSING("축복"),
    GOODBYE("작별"),
    OTHER("기타");
    
    private final String displayName;
    
    ConversationTopic(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}