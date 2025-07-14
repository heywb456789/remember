package com.tomato.remember.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 파일 저장 카테고리
 */
@Getter
@RequiredArgsConstructor
public enum StorageCategory {
    
    /**
     * 프로필 이미지 (회원 프로필)
     */
    PROFILE("profile", "프로필 이미지"),
    
    /**
     * 메모리얼 관련 파일 (프로필 이미지, 음성, 비디오)
     */
    MEMORIAL("memorial", "메모리얼 파일"),
    
    /**
     * 추모 게시물 관련 파일
     */
    TRIBUTE("tribute", "추모 게시물"),
    
    /**
     * 영상통화 관련 파일
     */
    VIDEO_CALL("videocall", "영상통화"),
    
    /**
     * 임시 파일
     */
    TEMP("temp", "임시 파일"),
    
    /**
     * 시스템 파일
     */
    SYSTEM("system", "시스템 파일");

    /**
     * 폴더명
     */
    private final String folder;
    
    /**
     * 설명
     */
    private final String description;
    
    /**
     * 폴더 경로 반환
     */
    public String getFolderPath() {
        return folder;
    }
    
    /**
     * 전체 경로 생성 (yyyy/MM 형태 추가)
     */
    public String createPath(String yearMonth) {
        return folder + "/" + yearMonth;
    }
}