package com.tomato.remember.application.wsvideo.service;

import com.tomato.remember.application.wsvideo.code.DeviceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 대기영상 관리 서비스
 * 디바이스별 최적화된 대기영상 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingVideoService {

    @Value("${app.video.base-url:https://remember.newstomato.com/static/}")
    private String baseVideoUrl;


    // 연락처별 대기영상 매핑
    private static final Map<String, ContactVideoInfo> CONTACT_VIDEOS = Map.of(
        "kimgeuntae", new ContactVideoInfo("김근태", "waiting_kt"),
        "rohmoohyun", new ContactVideoInfo("노무현", "waiting_no"),
        "default", new ContactVideoInfo("기본", "waiting_no")
    );

    // 디바이스별 영상 포맷 매핑
    private static final Map<DeviceType, VideoFormat> DEVICE_FORMATS = Map.of(
        DeviceType.WEB, new VideoFormat("mp4", "1920x1080", "high"),
        DeviceType.MOBILE_WEB, new VideoFormat("mp4", "1280x720", "medium"),
        DeviceType.IOS_APP, new VideoFormat("mp4", "1280x720", "medium"),
        DeviceType.ANDROID_APP, new VideoFormat("mp4", "1280x720", "medium")
    );

    // 재생 상태 캐시
    private final Map<String, WaitingVideoStatus> playingStatus = new ConcurrentHashMap<>();

    /**
     * 디바이스별 최적화된 대기영상 URL 반환
     */
    public String getWaitingVideoUrl(String contactKey, DeviceType deviceType) {
        try {
            ContactVideoInfo contactInfo = CONTACT_VIDEOS.getOrDefault(contactKey, CONTACT_VIDEOS.get("default"));
            VideoFormat format = DEVICE_FORMATS.get(deviceType);

            String filename = String.format("%s.%s",
                contactInfo.getVideoPrefix(),
                format.getExtension()
            );

            String fullUrl = String.format("%s%s", baseVideoUrl, filename);

            log.debug("📺 대기영상 URL 생성 - 연락처: {}, 디바이스: {}, URL: {}", 
                    contactKey, deviceType, fullUrl);

            return fullUrl;

        } catch (Exception e) {
            log.error("❌ 대기영상 URL 생성 실패 - 연락처: {}, 디바이스: {}", contactKey, deviceType, e);
            // 폴백 URL 반환
            return getFallbackVideoUrl(deviceType);
        }
    }

    /**
     * 대기영상 재생 시작 알림
     */
    public void notifyWaitingVideoStart(String sessionKey, String contactKey, DeviceType deviceType) {
        WaitingVideoStatus status = new WaitingVideoStatus(
            sessionKey, contactKey, deviceType, System.currentTimeMillis()
        );
        playingStatus.put(sessionKey, status);

        log.info("🎬 대기영상 재생 시작 - 세션: {}, 연락처: {}, 디바이스: {}", 
                sessionKey, contactKey, deviceType);
    }

    /**
     * 대기영상 재생 중지 알림
     */
    public void notifyWaitingVideoStop(String sessionKey) {
        WaitingVideoStatus status = playingStatus.remove(sessionKey);
        if (status != null) {
            long duration = System.currentTimeMillis() - status.getStartTime();
            log.info("⏹ 대기영상 재생 중지 - 세션: {}, 재생시간: {}ms", sessionKey, duration);
        }
    }

    /**
     * 연락처별 대기영상 플레이리스트 반환
     */
    public List<String> getWaitingVideoPlaylist(String contactKey, DeviceType deviceType) {
        ContactVideoInfo contactInfo = CONTACT_VIDEOS.getOrDefault(contactKey, CONTACT_VIDEOS.get("default"));
        VideoFormat format = DEVICE_FORMATS.get(deviceType);

        // 기본영상 + 추가 대기영상들
        return List.of(
            getWaitingVideoUrl(contactKey, deviceType),
            getAlternativeVideoUrl(contactInfo, format, "alt1"),
            getAlternativeVideoUrl(contactInfo, format, "alt2")
        );
    }

    /**
     * 대기영상 재생 상태 조회
     */
    public WaitingVideoStatus getPlayingStatus(String sessionKey) {
        return playingStatus.get(sessionKey);
    }

    /**
     * 모든 재생 중인 대기영상 상태 조회
     */
    public Map<String, WaitingVideoStatus> getAllPlayingStatus() {
        return Map.copyOf(playingStatus);
    }

    /**
     * 폴백 영상 URL 생성
     */
    private String getFallbackVideoUrl(DeviceType deviceType) {
        VideoFormat format = DEVICE_FORMATS.get(deviceType);
        return String.format("%s/default_fallback.%s", baseVideoUrl, format.getExtension());
    }

    /**
     * 대체 영상 URL 생성
     */
    private String getAlternativeVideoUrl(ContactVideoInfo contactInfo, VideoFormat format, String suffix) {
        String filename = String.format("%s_%s_%s_%s.%s",
            contactInfo.getVideoPrefix(),
            suffix,
            format.getResolution().replace("x", "_"),
            format.getQuality(),
            format.getExtension()
        );
        return String.format("%s/%s", baseVideoUrl, filename);
    }

    // ========== Inner Classes ==========

    public static class ContactVideoInfo {
        private final String displayName;
        private final String videoPrefix;

        public ContactVideoInfo(String displayName, String videoPrefix) {
            this.displayName = displayName;
            this.videoPrefix = videoPrefix;
        }

        public String getDisplayName() { return displayName; }
        public String getVideoPrefix() { return videoPrefix; }
    }

    public static class VideoFormat {
        private final String extension;
        private final String resolution;
        private final String quality;

        public VideoFormat(String extension, String resolution, String quality) {
            this.extension = extension;
            this.resolution = resolution;
            this.quality = quality;
        }

        public String getExtension() { return extension; }
        public String getResolution() { return resolution; }
        public String getQuality() { return quality; }
    }

    public static class WaitingVideoStatus {
        private final String sessionKey;
        private final String contactKey;
        private final DeviceType deviceType;
        private final long startTime;

        public WaitingVideoStatus(String sessionKey, String contactKey, DeviceType deviceType, long startTime) {
            this.sessionKey = sessionKey;
            this.contactKey = contactKey;
            this.deviceType = deviceType;
            this.startTime = startTime;
        }

        public String getSessionKey() { return sessionKey; }
        public String getContactKey() { return contactKey; }
        public DeviceType getDeviceType() { return deviceType; }
        public long getStartTime() { return startTime; }
        public long getDuration() { return System.currentTimeMillis() - startTime; }
    }
}