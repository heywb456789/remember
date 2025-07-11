package com.tomato.remember.application.memorial.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 메모리얼 파일 타입 enum
 */
@Getter
@RequiredArgsConstructor
public enum MemorialFileType {
    
    PROFILE_IMAGE("프로필 이미지", "image", 5),
    VOICE_FILE("음성 파일", "audio", 3),
    VIDEO_FILE("비디오 파일", "video", 1);

    private final String description;
    private final String category;
    private final int maxCount;

    /**
     * 파일 타입별 허용 MIME 타입 확인
     */
    public boolean isValidContentType(String contentType) {
        if (contentType == null) return false;
        
        return switch (this) {
            case PROFILE_IMAGE -> contentType.startsWith("image/") && 
                                  (contentType.equals("image/jpeg") || 
                                   contentType.equals("image/jpg") || 
                                   contentType.equals("image/png") || 
                                   contentType.equals("image/heic"));
            case VOICE_FILE -> contentType.startsWith("audio/") && 
                               (contentType.equals("audio/mp3") || 
                                contentType.equals("audio/mpeg") || 
                                contentType.equals("audio/wav") || 
                                contentType.equals("audio/m4a"));
            case VIDEO_FILE -> contentType.startsWith("video/") && 
                               (contentType.equals("video/mp4") || 
                                contentType.equals("video/mov") || 
                                contentType.equals("video/avi") || 
                                contentType.equals("video/quicktime"));
        };
    }

    /**
     * 파일 타입별 최대 파일 크기 (bytes)
     */
    public long getMaxFileSize() {
        return switch (this) {
            case PROFILE_IMAGE -> 5 * 1024 * 1024; // 5MB
            case VOICE_FILE -> 50 * 1024 * 1024;   // 50MB
            case VIDEO_FILE -> 100 * 1024 * 1024;  // 100MB
        };
    }

    /**
     * 파일 타입별 허용 확장자 목록
     */
    public String[] getAllowedExtensions() {
        return switch (this) {
            case PROFILE_IMAGE -> new String[]{"jpg", "jpeg", "png", "heic"};
            case VOICE_FILE -> new String[]{"mp3", "wav", "m4a"};
            case VIDEO_FILE -> new String[]{"mp4", "mov", "avi"};
        };
    }
}