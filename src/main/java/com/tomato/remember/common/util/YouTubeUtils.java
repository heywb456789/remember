package com.tomato.remember.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class YouTubeUtils {

    /**
     * YouTube URL에서 비디오 ID 추출
     */
    public String extractVideoId(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String cleanInput = input.trim();

        try {
            // 1. 이미 ID만 있는 경우 (11자리)
            if (cleanInput.length() == 11 && isValidYouTubeId(cleanInput)) {
                return cleanInput;
            }

            // 2. youtube.com/watch?v= 형태
            if (cleanInput.contains("youtube.com/watch?v=")) {
                String[] parts = cleanInput.split("v=");
                if (parts.length > 1) {
                    String videoId = parts[1].split("&")[0]; // &로 다른 파라미터 제거
                    if (isValidYouTubeId(videoId)) {
                        return videoId;
                    }
                }
            }

            // 3. youtu.be/ 형태
            if (cleanInput.contains("youtu.be/")) {
                String[] parts = cleanInput.split("youtu.be/");
                if (parts.length > 1) {
                    String videoId = parts[1].split("\\?")[0]; // ?로 파라미터 제거
                    if (isValidYouTubeId(videoId)) {
                        return videoId;
                    }
                }
            }

            // 4. embed 형태
            if (cleanInput.contains("youtube.com/embed/")) {
                String[] parts = cleanInput.split("embed/");
                if (parts.length > 1) {
                    String videoId = parts[1].split("\\?")[0];
                    if (isValidYouTubeId(videoId)) {
                        return videoId;
                    }
                }
            }

            // 5. shorts 형태 (새로 추가)
            if (cleanInput.contains("youtube.com/shorts/") || cleanInput.contains("youtu.be/shorts/")) {
                String[] parts;
                if (cleanInput.contains("youtube.com/shorts/")) {
                    parts = cleanInput.split("shorts/");
                } else {
                    parts = cleanInput.split("youtu.be/shorts/");
                }

                if (parts.length > 1) {
                    String videoId = parts[1].split("\\?")[0]; // ?로 파라미터 제거
                    if (isValidYouTubeId(videoId)) {
                        return videoId;
                    }
                }
            }

        } catch (Exception e) {
            log.error("YouTube ID 추출 실패: {}", e.getMessage());
        }

        return null;
    }

    /**
     * YouTube ID 유효성 검사
     */
    private boolean isValidYouTubeId(String id) {
        if (id == null || id.length() != 11) {
            return false;
        }
        // YouTube ID는 영문, 숫자, -, _ 로만 구성
        return id.matches("^[a-zA-Z0-9_-]{11}$");
    }

    /**
     * 프론트엔드용 임베드 URL 생성 (재생 가능한 URL)
     * 저장된 URL을 실제 재생 가능한 embed URL로 변환
     */
    public String getEmbedUrl(String storedUrl) {
        if (storedUrl == null || storedUrl.trim().isEmpty()) {
            return null;
        }

        // 저장된 URL에서 비디오 ID 추출
        String videoId = extractVideoId(storedUrl);
        if (videoId == null) {
            return null;
        }

        // 기본 embed URL 생성
        return "https://www.youtube.com/embed/" + videoId;
    }

    /**
     * YouTube 시청 URL 생성
     */
    public String getWatchUrl(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            return null;
        }
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    /**
     * YouTube Shorts 시청 URL 생성
     */
    public String getShortsUrl(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            return null;
        }
        return "https://www.youtube.com/shorts/" + videoId;
    }

    /**
     * YouTube 썸네일 URL 생성 (기본 해상도)
     */
    public String getThumbnailUrl(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            return null;
        }
        return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
    }

    /**
     * YouTube 썸네일 URL 생성 (최대 해상도)
     */
    public String getMaxResThumbnailUrl(String videoId) {
        if (videoId == null || videoId.trim().isEmpty()) {
            return null;
        }
        return "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg";
    }

    /**
     * URL이 YouTube URL인지 확인
     */
    public boolean isYouTubeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String cleanUrl = url.trim().toLowerCase();
        return cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be");
    }

    /**
     * URL이 YouTube Shorts URL인지 확인
     */
    public boolean isYouTubeShortsUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String cleanUrl = url.trim().toLowerCase();
        return cleanUrl.contains("youtube.com/shorts/") || cleanUrl.contains("youtu.be/shorts/");
    }

    /**
     * 비디오 타입에 따른 최적 iframe 크기 반환
     */
    public static class EmbedSize {
        public final int width;
        public final int height;

        public EmbedSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}