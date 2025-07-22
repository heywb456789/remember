package com.tomato.remember.application.wsvideo.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 디바이스 타입 정의
 * 영상통화 기능 지원을 위해 확장됨
 */
@Getter
@RequiredArgsConstructor
public enum DeviceType {
    WEB("웹", true, true, true, "PC/태블릿 웹 브라우저"),
    MOBILE_WEB("모바일 웹", true, true, true, "모바일 웹 브라우저"),
    IOS_APP("iOS 앱", true, true, true, "iPhone/iPad 네이티브 앱"),
    ANDROID_APP("Android 앱", true, true, true, "Android 네이티브 앱");

    private final String displayName;

    // === 영상통화 기능 지원 여부 ===
    private final boolean supportsVideoCall;      // 영상통화 지원
    private final boolean supportsWebSocket;      // WebSocket 지원
    private final boolean supportsPrimaryControl; // 주 제어 디바이스 가능
    private final String description;              // 상세 설명

    /**
     * 영상통화 주 제어 디바이스로 사용 가능한지 확인
     */
    public boolean canBePrimaryDevice() {
        return supportsPrimaryControl;
    }

    /**
     * 영상통화 기능을 지원하는지 확인
     */
    public boolean supportsVideoCall() {
        return supportsVideoCall;
    }

    /**
     * WebSocket 연결을 지원하는지 확인
     */
    public boolean supportsWebSocket() {
        return supportsWebSocket;
    }

    /**
     * 모바일 디바이스인지 확인
     */
    public boolean isMobileDevice() {
        return this == MOBILE_WEB || this == IOS_APP || this == ANDROID_APP;
    }

    /**
     * 웹 기반 디바이스인지 확인
     */
    public boolean isWebBased() {
        return this == WEB || this == MOBILE_WEB;
    }

    /**
     * 네이티브 앱인지 확인
     */
    public boolean isNativeApp() {
        return this == IOS_APP || this == ANDROID_APP;
    }

    /**
     * 디바이스별 최적 영상 설정 반환
     */
    public VideoConstraints getOptimalVideoConstraints() {
        return switch (this) {
            case WEB -> new VideoConstraints(1920, 1080, 30, true);
            case MOBILE_WEB -> new VideoConstraints(1280, 720, 30, true);
            case IOS_APP -> new VideoConstraints(1280, 720, 30, true);
            case ANDROID_APP -> new VideoConstraints(1280, 720, 30, true);
        };
    }

    /**
     * 디바이스별 권한 요청 방식 반환
     */
    public PermissionStrategy getPermissionStrategy() {
        return switch (this) {
            case WEB, MOBILE_WEB -> PermissionStrategy.WEB_BROWSER_API;
            case IOS_APP, ANDROID_APP -> PermissionStrategy.NATIVE_PERMISSION;
        };
    }

    /**
     * 영상 품질 설정
     */
    @Getter
    @RequiredArgsConstructor
    public static class VideoConstraints {
        private final int width;
        private final int height;
        private final int frameRate;
        private final boolean supportsHighQuality;

        public String getResolution() {
            return width + "x" + height;
        }

        public boolean isHD() {
            return width >= 1280 && height >= 720;
        }

        public boolean isFullHD() {
            return width >= 1920 && height >= 1080;
        }
    }

    /**
     * 권한 요청 전략
     */
    public enum PermissionStrategy {
        WEB_BROWSER_API("웹 브라우저 API 사용"),
        NATIVE_PERMISSION("네이티브 권한 시스템");

        private final String description;

        PermissionStrategy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * User-Agent에서 디바이스 타입 추측
     */
    public static DeviceType detectFromUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return WEB;
        }

        String ua = userAgent.toLowerCase();

        // iOS 앱 체크
        if (ua.contains("tomato-remember-ios") || ua.contains("ios-app")) {
            return IOS_APP;
        }

        // Android 앱 체크
        if (ua.contains("tomato-remember-android") || ua.contains("android-app")) {
            return ANDROID_APP;
        }

        // 모바일 웹 체크
        if (ua.contains("mobile") || ua.contains("iphone") ||
            (ua.contains("android") && !ua.contains("tablet"))) {
            return MOBILE_WEB;
        }

        // 기본값은 웹
        return WEB;
    }

    /**
     * 디바이스별 WebSocket 엔드포인트 경로 반환
     */
    public String getWebSocketPath(String sessionKey) {
        return switch (this) {
            case WEB -> "/ws/memorial-video/web/" + sessionKey;
            case MOBILE_WEB -> "/ws/memorial-video/mobile-web/" + sessionKey;
            case IOS_APP -> "/ws/memorial-video/ios/" + sessionKey;
            case ANDROID_APP -> "/ws/memorial-video/android/" + sessionKey;
        };
    }

    /**
     * 디바이스별 UI 레이아웃 타입 반환
     */
    public String getUILayoutType() {
        return switch (this) {
            case WEB -> "desktop-web";
            case MOBILE_WEB -> "mobile-web";
            case IOS_APP -> "ios-native";
            case ANDROID_APP -> "android-native";
        };
    }

    /**
     * 디바이스가 특정 기능을 지원하는지 확인
     */
    public boolean supportsFeature(VideoCallFeature feature) {
        return switch (feature) {
            case CAMERA_RECORDING -> supportsVideoCall;
            case MICROPHONE_RECORDING -> supportsVideoCall;
            case FULLSCREEN_MODE -> true; // 모든 디바이스 지원
            case PICTURE_IN_PICTURE -> this == WEB; // 데스크톱 웹만
            case BACKGROUND_MODE -> isNativeApp(); // 네이티브 앱만
            case TOUCH_CONTROLS -> isMobileDevice(); // 모바일만
            case KEYBOARD_SHORTCUTS -> this == WEB; // 데스크톱 웹만
        };
    }

    /**
     * 영상통화 기능 목록
     */
    public enum VideoCallFeature {
        CAMERA_RECORDING,      // 카메라 녹화
        MICROPHONE_RECORDING,  // 마이크 녹음
        FULLSCREEN_MODE,       // 전체화면 모드
        PICTURE_IN_PICTURE,    // 화면 속 화면
        BACKGROUND_MODE,       // 백그라운드 실행
        TOUCH_CONTROLS,        // 터치 컨트롤
        KEYBOARD_SHORTCUTS     // 키보드 단축키
    }
}