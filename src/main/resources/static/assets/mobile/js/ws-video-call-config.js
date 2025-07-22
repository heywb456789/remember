/**
 * WebSocket 기반 영상통화 시스템 - 설정 및 상수
 */

// ========== 전역 설정 ==========
window.WS_VIDEO_CONFIG = {
    // API 엔드포인트
    API_BASE_URL: '/api/ws-video',

    // WebSocket 설정
    WEBSOCKET_BASE_URL: 'ws://localhost:8080/ws/memorial-video',
    HEARTBEAT_INTERVAL: 30000, // 30초
    RECONNECT_DELAY_BASE: 1000, // 1초
    MAX_RECONNECT_ATTEMPTS: 10,

    // 미디어 설정
    VIDEO_CONSTRAINTS: {
        WEB: {
            video: {
                width: { ideal: 1920, min: 1280 },
                height: { ideal: 1080, min: 720 },
                frameRate: { ideal: 30, min: 24 }
            },
            audio: {
                echoCancellation: true,
                noiseSuppression: true,
                sampleRate: 44100
            }
        },
        MOBILE_WEB: {
            video: {
                width: { ideal: 1280, min: 640 },
                height: { ideal: 720, min: 480 },
                frameRate: { ideal: 30, min: 20 }
            },
            audio: {
                echoCancellation: true,
                noiseSuppression: true
            }
        }
    },

    // 녹화 설정
    RECORDING: {
        MAX_DURATION: 10000, // 10초
        COUNTDOWN_DURATION: 3000, // 3초
        MIME_TYPES: [
            'video/webm;codecs=vp9,opus',
            'video/webm;codecs=vp8,opus',
            'video/webm',
            'video/mp4'
        ]
    },

    // 타이머 설정
    TIMERS: {
        SESSION_TTL: 3600000, // 1시간
        PERMISSION_TIMEOUT: 30000, // 30초
        VIDEO_LOAD_TIMEOUT: 15000, // 15초
        CONNECTION_TIMEOUT: 10000 // 10초
    },

    // 디버그 모드 (개발용)
    DEBUG: {
        ENABLED: true,
        LOG_LEVEL: 'info', // 'debug', 'info', 'warn', 'error'
        SHOW_DEBUG_INFO: false
    },

    // 대기영상 URL (개발용)
    DEFAULT_WAITING_VIDEO: 'https://remember.newstomato.com/static/waiting_no.mp4',

    // API 인증 설정 추가
    AUTH: {
        REQUIRED_ENDPOINTS: [
            '/api/ws-video/create-session',
            '/api/ws-video/process/{sessionKey}',
            '/api/ws-video/session/{sessionKey}/cleanup'
        ],
        TOKEN_CHECK_INTERVAL: 30000, // 30초마다 토큰 상태 확인
        AUTO_LOGOUT_ON_TOKEN_FAIL: true
    }
};

// ========== 상태 정의 ==========
window.WS_VIDEO_STATES = {
    // 초기화 단계
    INITIALIZING: 'INITIALIZING',
    PERMISSION_REQUESTING: 'PERMISSION_REQUESTING',
    PERMISSION_GRANTED: 'PERMISSION_GRANTED',

    // 대기 단계
    WAITING_READY: 'WAITING_READY',
    WAITING_PLAYING: 'WAITING_PLAYING',

    // 녹화 단계
    RECORDING_COUNTDOWN: 'RECORDING_COUNTDOWN',
    RECORDING_ACTIVE: 'RECORDING_ACTIVE',
    RECORDING_COMPLETE: 'RECORDING_COMPLETE',

    // 처리 단계
    PROCESSING_UPLOAD: 'PROCESSING_UPLOAD',
    PROCESSING_AI: 'PROCESSING_AI',
    PROCESSING_COMPLETE: 'PROCESSING_COMPLETE',

    // 응답 단계
    RESPONSE_READY: 'RESPONSE_READY',
    RESPONSE_PLAYING: 'RESPONSE_PLAYING',
    RESPONSE_COMPLETE: 'RESPONSE_COMPLETE',

    // 종료 단계
    CALL_ENDING: 'CALL_ENDING',
    CALL_COMPLETED: 'CALL_COMPLETED',

    // 오류 단계
    ERROR_NETWORK: 'ERROR_NETWORK',
    ERROR_PERMISSION: 'ERROR_PERMISSION',
    ERROR_PROCESSING: 'ERROR_PROCESSING',
    ERROR_TIMEOUT: 'ERROR_TIMEOUT'
};

// ========== 메시지 타입 ==========
window.WS_MESSAGE_TYPES = {
    // 클라이언트 → 서버
    CONNECT: 'CONNECT',
    DISCONNECT: 'DISCONNECT',
    HEARTBEAT_RESPONSE: 'HEARTBEAT_RESPONSE',
    CLIENT_STATE_CHANGE: 'CLIENT_STATE_CHANGE',
    PERMISSION_STATUS: 'PERMISSION_STATUS',
    DEVICE_INFO: 'DEVICE_INFO',

    // 영상 관련 (클라이언트 → 서버)
    WAITING_VIDEO_STARTED: 'WAITING_VIDEO_STARTED',
    WAITING_VIDEO_ERROR: 'WAITING_VIDEO_ERROR',
    RECORDING_READY: 'RECORDING_READY',
    RECORDING_STARTED: 'RECORDING_STARTED',
    RECORDING_STOPPED: 'RECORDING_STOPPED',
    RECORDING_ERROR: 'RECORDING_ERROR',
    VIDEO_UPLOAD_COMPLETE: 'VIDEO_UPLOAD_COMPLETE',
    VIDEO_UPLOAD_ERROR: 'VIDEO_UPLOAD_ERROR',
    RESPONSE_VIDEO_STARTED: 'RESPONSE_VIDEO_STARTED',
    RESPONSE_VIDEO_ENDED: 'RESPONSE_VIDEO_ENDED',
    RESPONSE_VIDEO_ERROR: 'RESPONSE_VIDEO_ERROR',

    // 서버 → 클라이언트
    CONNECTED: 'CONNECTED',
    HEARTBEAT: 'HEARTBEAT',
    STATE_TRANSITION: 'STATE_TRANSITION',
    FORCE_STATE_CHANGE: 'FORCE_STATE_CHANGE',

    // 영상 제어 (서버 → 클라이언트)
    PLAY_WAITING_VIDEO: 'PLAY_WAITING_VIDEO',
    STOP_WAITING_VIDEO: 'STOP_WAITING_VIDEO',
    START_RECORDING: 'START_RECORDING',
    STOP_RECORDING: 'STOP_RECORDING',
    PLAY_RESPONSE_VIDEO: 'PLAY_RESPONSE_VIDEO',
    STOP_RESPONSE_VIDEO: 'STOP_RESPONSE_VIDEO',

    // 진행 상황
    PROCESSING_PROGRESS: 'PROCESSING_PROGRESS',
    UPLOAD_PROGRESS: 'UPLOAD_PROGRESS',

    // 멀티 디바이스
    DEVICE_REGISTERED: 'DEVICE_REGISTERED',
    DEVICE_DISCONNECTED: 'DEVICE_DISCONNECTED',
    PRIORITY_CHANGED: 'PRIORITY_CHANGED',

    // 공통
    ERROR: 'ERROR',
    INFO: 'INFO',
    WARNING: 'WARNING',
    SUCCESS: 'SUCCESS'
};

// ========== 디바이스 타입 감지 ==========
window.detectDeviceType = function() {
    const userAgent = navigator.userAgent.toLowerCase();
    const isMobile = /mobile|android|iphone|ipad/.test(userAgent);
    const isTablet = /ipad|tablet/.test(userAgent);
    const isIOS = /iphone|ipad|ipod/.test(userAgent);
    const isAndroid = /android/.test(userAgent);

    // 앱에서 접근하는 경우 User-Agent를 통해 구분
    if (userAgent.includes('tomato-remember-ios')) {
        return 'IOS_APP';
    } else if (userAgent.includes('tomato-remember-android')) {
        return 'ANDROID_APP';
    } else if (isMobile && !isTablet) {
        return 'MOBILE_WEB';
    } else {
        return 'WEB';
    }
};

// ========== 브라우저 정보 ==========
window.getBrowserInfo = function() {
    const userAgent = navigator.userAgent;
    const isIOS = /iPad|iPhone|iPod/.test(userAgent);
    const isAndroid = /Android/.test(userAgent);
    const isSafari = /^((?!chrome|android).)*safari/i.test(userAgent);
    const isChrome = /Chrome/.test(userAgent);
    const isFirefox = /Firefox/.test(userAgent);

    return {
        userAgent,
        isIOS,
        isAndroid,
        isSafari,
        isChrome,
        isFirefox,
        isIOSSafari: isIOS && isSafari,
        isAndroidChrome: isAndroid && isChrome,
        deviceType: detectDeviceType(),
        platform: isIOS ? 'iOS' : (isAndroid ? 'Android' : 'Desktop'),
        supportsWebRTC: !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia)
    };
};

// ========== 유틸리티 함수 ==========
window.WS_VIDEO_UTILS = {
    // UUID 생성
    generateUUID: function() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            const r = Math.random() * 16 | 0;
            const v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    },

    // 디바이스 ID 생성
    generateDeviceId: function() {
        let deviceId = localStorage.getItem('ws-video-device-id');
        if (!deviceId) {
            deviceId = this.generateUUID();
            localStorage.setItem('ws-video-device-id', deviceId);
        }
        return deviceId;
    },

    // 시간 포맷팅
    formatTime: function(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    },

    // 파일 크기 포맷팅
    formatFileSize: function(bytes) {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },

    // 지연 함수
    delay: function(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    },

    // 재시도 로직
    retry: async function(fn, maxAttempts = 3, delayMs = 1000) {
        for (let attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return await fn();
            } catch (error) {
                if (attempt === maxAttempts) {
                    throw error;
                }
                console.warn(`시도 ${attempt} 실패, ${delayMs}ms 후 재시도:`, error.message);
                await this.delay(delayMs * attempt);
            }
        }
    },

    // 로컬 스토리지 관리
    storage: {
        set: function(key, value, ttlMs = null) {
            const item = {
                value: value,
                timestamp: Date.now(),
                ttl: ttlMs
            };
            localStorage.setItem(key, JSON.stringify(item));
        },

        get: function(key) {
            try {
                const item = JSON.parse(localStorage.getItem(key));
                if (!item) return null;

                if (item.ttl && (Date.now() - item.timestamp > item.ttl)) {
                    localStorage.removeItem(key);
                    return null;
                }

                return item.value;
            } catch (error) {
                console.warn('스토리지 조회 오류:', error);
                return null;
            }
        },

        remove: function(key) {
            localStorage.removeItem(key);
        }
    }
};

// ========== 로깅 시스템 ==========
window.WS_VIDEO_LOGGER = {
    debug: function(message, ...args) {
        if (WS_VIDEO_CONFIG.DEBUG.ENABLED &&
            ['debug'].includes(WS_VIDEO_CONFIG.DEBUG.LOG_LEVEL)) {
            console.debug(`[WS-VIDEO] ${message}`, ...args);
        }
    },

    info: function(message, ...args) {
        if (WS_VIDEO_CONFIG.DEBUG.ENABLED &&
            ['debug', 'info'].includes(WS_VIDEO_CONFIG.DEBUG.LOG_LEVEL)) {
            console.info(`[WS-VIDEO] ${message}`, ...args);
        }
    },

    warn: function(message, ...args) {
        if (WS_VIDEO_CONFIG.DEBUG.ENABLED &&
            ['debug', 'info', 'warn'].includes(WS_VIDEO_CONFIG.DEBUG.LOG_LEVEL)) {
            console.warn(`[WS-VIDEO] ${message}`, ...args);
        }
    },

    error: function(message, ...args) {
        console.error(`[WS-VIDEO] ${message}`, ...args);
    }
};

// ========== 전역 상태 ==========
window.WS_VIDEO_STATE = {
    // 현재 상태
    currentState: WS_VIDEO_STATES.INITIALIZING,

    // 세션 정보
    sessionKey: null,
    contactName: null,
    memberId: null,
    memorialId: null,
    deviceType: detectDeviceType(),
    deviceId: WS_VIDEO_UTILS.generateDeviceId(),

    // 연결 상태
    websocket: null,
    isConnected: false,
    reconnectAttempts: 0,
    lastHeartbeat: null,

    // 미디어 상태
    userMediaStream: null,
    mediaRecorder: null,
    recordedChunks: [],
    isRecording: false,

    // 권한 상태
    cameraPermissionGranted: false,
    microphonePermissionGranted: false,

    // 영상 상태
    waitingVideoUrl: null,
    responseVideoUrl: null,
    currentVideoState: 'WAITING',

    // UI 상태
    modalsOpen: [],
    statusMessage: '초기화 중...'
};

// 전역 변수 초기화 완료 로그
WS_VIDEO_LOGGER.info('설정 및 전역 변수 초기화 완료', {
    deviceType: WS_VIDEO_STATE.deviceType,
    deviceId: WS_VIDEO_STATE.deviceId,
    browserInfo: getBrowserInfo()
});