package com.tomato.remember.application.memorial.code;

public enum InterestType {
    TRAVEL("여행"),
    MUSIC("음악"),
    SPORTS("운동"),
    COOKING("요리"),
    READING("독서"),
    GARDENING("정원가꾸기"),
    PHOTOGRAPHY("사진"),
    PAINTING("그림"),
    FISHING("낚시"),
    HIKING("등산"),
    DANCING("춤"),
    SINGING("노래"),
    KNITTING("뜨개질"),
    CHESS("바둑/체스"),
    MOVIES("영화"),
    DRAMA("드라마"),
    NEWS("뉴스"),
    RELIGION("종교"),
    VOLUNTEER("봉사활동"),
    FAMILY("가족"),
    PETS("반려동물"),
    FASHION("패션"),
    TECHNOLOGY("기술"),
    BUSINESS("사업"),
    EDUCATION("교육"),
    HEALTH("건강"),
    FOOD("음식"),
    NATURE("자연"),
    HISTORY("역사"),
    CULTURE("문화"),
    ART("예술"),
    CRAFT("공예"),
    GAME("게임"),
    SHOPPING("쇼핑"),
    SOCIAL("사교활동"),
    OTHER("기타");
    
    private final String displayName;
    
    InterestType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}