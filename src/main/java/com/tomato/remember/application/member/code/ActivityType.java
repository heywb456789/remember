package com.tomato.remember.application.member.code;

public enum ActivityType {
    // 메모리얼 관련
    MEMORIAL_CREATED("메모리얼 생성"),
    MEMORIAL_UPDATED("메모리얼 수정"),
    MEMORIAL_DELETED("메모리얼 삭제"),
    MEMORIAL_VISITED("메모리얼 방문"),
    
    // 영상통화 관련
    VIDEO_CALL_STARTED("영상통화 시작"),
    VIDEO_CALL_ENDED("영상통화 종료"),
    VIDEO_CALL_FAILED("영상통화 실패"),
    VIDEO_CALL_CANCELLED("영상통화 취소"),
    
    // 가족 관련
    FAMILY_INVITED("가족 초대"),
    FAMILY_JOINED("가족 참여"),
    FAMILY_LEFT("가족 탈퇴"),
    FAMILY_REMOVED("가족 제거"),
    
    // 프로필 관련
    PROFILE_UPDATED("프로필 수정"),
    PROFILE_IMAGE_UPDATED("프로필 이미지 수정"),
    PASSWORD_CHANGED("비밀번호 변경"),
    
    // 인증 관련
    LOGIN("로그인"),
    LOGOUT("로그아웃"),
    REGISTER("회원가입"),
    PASSWORD_RESET("비밀번호 재설정"),
    
    // 파일 관련
    FILE_UPLOADED("파일 업로드"),
    FILE_DELETED("파일 삭제"),
    
    // 설정 관련
    NOTIFICATION_SETTING_CHANGED("알림 설정 변경"),
    LANGUAGE_CHANGED("언어 설정 변경"),
    
    // 기타
    FEEDBACK_SUBMITTED("피드백 제출"),
    ERROR_OCCURRED("오류 발생"),
    OTHER("기타");
    
    private final String displayName;
    
    ActivityType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}