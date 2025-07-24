/**
 * WebSocket 기반 영상통화 시스템 - 간소화된 설정 및 상수 (9개 상태)
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

    // 디버그 모드
    DEBUG: {
        ENABLED: true,
        LOG_LEVEL: 'info',
        SHOW_DEBUG_INFO: false
    },

    // 대기영상 URL
    DEFAULT_WAITING_VIDEO: 'https://remember.newstomato.com/static/waiting_no.mp4',

    // API 인증 설정
    AUTH: {
        TOKEN_CHECK_INTERVAL: 30000, // 30초마다 토큰 상태 확인
        AUTO_LOGOUT_ON_TOKEN_FAIL: true
    }
};

// ========== 간소화된 9개 상태 정의 ==========
window.WS_VIDEO_FLOW_STATES = {
    // === 초기화 ===
    INITIALIZING: {
        name: 'INITIALIZING',
        display: '초기화 중',
        description: '시스템을 준비하고 있습니다',
        icon: '⚙️',
        showLoading: true,
        allowRecording: false,
        allowUserInteraction: false
    },

    // === 권한 요청 ===
    PERMISSION_REQUESTING: {
        name: 'PERMISSION_REQUESTING',
        display: '권한 요청 중',
        description: '카메라와 마이크 권한을 요청합니다',
        icon: '🔒',
        showLoading: false,
        allowRecording: false,
        allowUserInteraction: true
    },

    // === 대기 (핵심 상태) ===
    WAITING: {
        name: 'WAITING',
        display: '대기 중',
        description: '대기영상이 재생되고 있습니다',
        icon: '⏳',
        showLoading: false,
        allowRecording: true,
        allowUserInteraction: true
    },

    // === 녹화 (핵심 상태) ===
    RECORDING: {
        name: 'RECORDING',
        display: '🔴 녹화 중',
        description: '음성이 녹화되고 있습니다',
        icon: '🔴',
        showLoading: false,
        allowRecording: false,
        allowUserInteraction: false
    },

    // === 처리 (핵심 상태) ===
    PROCESSING: {
        name: 'PROCESSING',
        display: '🤖 처리 중',
        description: 'AI가 응답을 생성하고 있습니다',
        icon: '🤖',
        showLoading: true,
        allowRecording: false,
        allowUserInteraction: false
    },

    // === 응답 재생 (핵심 상태) ===
    RESPONSE_PLAYING: {
        name: 'RESPONSE_PLAYING',
        display: '🎬 응답 재생 중',
        description: '응답영상이 재생되고 있습니다',
        icon: '🎬',
        showLoading: false,
        allowRecording: false,
        allowUserInteraction: false
    },

    // === 통화 종료 ===
    CALL_ENDING: {
        name: 'CALL_ENDING',
        display: '통화 종료 중',
        description: '통화를 종료하고 있습니다',
        icon: '👋',
        showLoading: true,
        allowRecording: false,
        allowUserInteraction: false
    },

    CALL_COMPLETED: {
        name: 'CALL_COMPLETED',
        display: '통화 완료',
        description: '통화가 완료되었습니다',
        icon: '✅',
        showLoading: false,
        allowRecording: false,
        allowUserInteraction: false
    },

    // === 오류 ===
    ERROR: {
        name: 'ERROR',
        display: '❌ 오류',
        description: '오류가 발생했습니다',
        icon: '❌',
        showLoading: false,
        allowRecording: false,
        allowUserInteraction: true
    }
};

// ========== 간소화된 메시지 타입 ==========
window.WS_MESSAGE_TYPES = {
    // === 인증 관련 ===
    AUTH: 'AUTH',
    AUTH_SUCCESS: 'AUTH_SUCCESS',
    TOKEN_INVALID: 'TOKEN_INVALID',
    TOKEN_MISSING: 'TOKEN_MISSING',
    SESSION_ACCESS_DENIED: 'SESSION_ACCESS_DENIED',
    AUTH_TIMEOUT: 'AUTH_TIMEOUT',
    TOKEN_REFRESHED: 'TOKEN_REFRESHED',
    TOKEN_REFRESH: 'TOKEN_REFRESH',

    // === 연결 관리 ===
    CONNECT: 'CONNECT',
    CONNECTED: 'CONNECTED',
    DISCONNECT: 'DISCONNECT',
    HEARTBEAT: 'HEARTBEAT',
    HEARTBEAT_RESPONSE: 'HEARTBEAT_RESPONSE',

    // === 상태 관리 (핵심) ===
    STATE_TRANSITION: 'STATE_TRANSITION',
    FORCE_STATE_CHANGE: 'FORCE_STATE_CHANGE',
    CLIENT_STATE_CHANGE: 'CLIENT_STATE_CHANGE',

    // === 영상 제어 명령 (서버 → 클라이언트) ===
    PLAY_WAITING_VIDEO: 'PLAY_WAITING_VIDEO',
    START_RECORDING: 'START_RECORDING',
    PLAY_RESPONSE_VIDEO: 'PLAY_RESPONSE_VIDEO',
    RESPONSE_VIDEO: 'RESPONSE_VIDEO',

    // === 간소화된 영상 이벤트 (클라이언트 → 서버) ===
    WAITING_VIDEO_EVENT: 'WAITING_VIDEO_EVENT', // eventType: "started" | "error"
    RESPONSE_VIDEO_EVENT: 'RESPONSE_VIDEO_EVENT', // eventType: "started" | "ended" | "error"

    // === 기존 개별 메시지들 (하위 호환성) ===
    RECORDING_STARTED: 'RECORDING_STARTED',
    RECORDING_STOPPED: 'RECORDING_STOPPED',
    RECORDING_ERROR: 'RECORDING_ERROR',
    VIDEO_UPLOAD_COMPLETE: 'VIDEO_UPLOAD_COMPLETE',

    // === 디바이스 관리 ===
    DEVICE_INFO: 'DEVICE_INFO',

    // === 진행 상황 ===
    PROCESSING_PROGRESS: 'PROCESSING_PROGRESS',

    // === 공통 메시지 ===
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

// ========== 전역 상태 (간소화) ==========
window.WS_VIDEO_STATE = {
    // 현재 플로우 상태 (9개 중 하나)
    currentFlowState: WS_VIDEO_FLOW_STATES.INITIALIZING,

    // 기본 세션 정보
    sessionKey: null,
    contactName: null,
    memberId: null,
    memorialId: null,
    deviceType: detectDeviceType(),
    deviceId: WS_VIDEO_UTILS.generateDeviceId(),

    // WebSocket 연결
    websocket: null,
    isConnected: false,
    reconnectAttempts: 0,
    lastHeartbeat: null,

    // 미디어 스트림
    userMediaStream: null,
    mediaRecorder: null,
    recordedChunks: [],
    isRecording: false,

    // 권한 상태
    cameraPermissionGranted: false,
    microphonePermissionGranted: false,

    // 영상 URL
    waitingVideoUrl: null,
    responseVideoUrl: null,

    // 기타
    sessionCreatedAt: null,
    statusMessage: '초기화 중...'
};

// ========== 간소화된 상태 관리 유틸리티 ==========
window.WS_VIDEO_STATE_UTILS = {
    // 현재 상태 정보 가져오기
    getCurrentState() {
        return WS_VIDEO_STATE.currentFlowState;
    },

    // 상태 전환 (클라이언트 사이드)
    transitionToState(newStateName) {
        const newState = WS_VIDEO_FLOW_STATES[newStateName];
        if (!newState) {
            WS_VIDEO_LOGGER.error('알 수 없는 상태:', newStateName);
            return false;
        }

        const previousState = WS_VIDEO_STATE.currentFlowState;
        WS_VIDEO_STATE.currentFlowState = newState;

        WS_VIDEO_LOGGER.info('클라이언트 상태 전환:', previousState.name, '→', newState.name);

        // UI 업데이트
        this.updateUI(newState, previousState);

        return true;
    },

    // UI 업데이트 (간소화)
    updateUI(newState, previousState) {
        // 1. 상태 텍스트 업데이트
        updateStatus(newState.display, this.getStatusType(newState));

        // 2. 로딩 표시 제어
        if (newState.showLoading) {
            showVideoLoadingOverlay();
        } else {
            hideVideoLoadingOverlay();
        }

        if (newState.name === 'WAITING' || newState.name === 'RESPONSE_PLAYING') {
            const mainVideo = document.getElementById('mainVideo');
            if (mainVideo && !mainVideo.muted) {
                mainVideo.volume = 0.8;  // 볼륨 보장
                WS_VIDEO_LOGGER.info('🔊 오디오 활성화 보장:', {
                    state: newState.name,
                    muted: mainVideo.muted,
                    volume: mainVideo.volume
                });
            }
        }

        // 3. 녹화 버튼 상태 제어
        const recordBtn = document.getElementById('recordBtn');
        if (recordBtn) {
            recordBtn.disabled = !newState.allowRecording;
            recordBtn.classList.toggle('disabled', !newState.allowRecording);
            recordBtn.title = newState.allowRecording ? '녹화하기' : '녹화할 수 없습니다';
        }

        // 4. 녹화 중 특별 처리
        if (newState.name === 'RECORDING') {
            updateRecordingUI(true);
            document.body.classList.add('recording-active');
        } else {
            updateRecordingUI(false);
            document.body.classList.remove('recording-active');
        }

        // 5. 연결 상태 처리
        if (newState.name === 'ERROR') {
            updateConnectionStatus('error');
        } else if (previousState && previousState.name === 'ERROR') {
            updateConnectionStatus('connected');
        }
    },

    // 상태별 표시 타입 결정
    getStatusType(state) {
        if (state.name === 'ERROR') return 'error';
        if (state.name === 'RECORDING') return 'recording';
        if (state.name === 'RESPONSE_PLAYING') return 'success';
        if (state.showLoading) return 'loading';
        return 'info';
    },

    // 녹화 가능 여부 확인
    canRecord() {
        return WS_VIDEO_STATE.currentFlowState.allowRecording &&
               WS_VIDEO_STATE.cameraPermissionGranted;
    },

    // 디버그 정보
    getDebugInfo() {
        return {
            currentState: WS_VIDEO_STATE.currentFlowState.name,
            display: WS_VIDEO_STATE.currentFlowState.display,
            description: WS_VIDEO_STATE.currentFlowState.description,
            allowRecording: WS_VIDEO_STATE.currentFlowState.allowRecording,
            showLoading: WS_VIDEO_STATE.currentFlowState.showLoading,
            isConnected: WS_VIDEO_STATE.isConnected,
            hasPermissions: WS_VIDEO_STATE.cameraPermissionGranted
        };
    }
};

// ========== 간소화된 에러 처리 ==========
window.WS_VIDEO_ERROR_HANDLER = {
    // 기본 에러 처리
    handleError(errorType, message) {
        WS_VIDEO_LOGGER.error(`❌ 에러 발생: ${errorType}`, message);

        // 오류 상태로 전환
        WS_VIDEO_STATE_UTILS.transitionToState('ERROR');

        // 사용자에게 메시지 표시
        showErrorMessage(message);

        // 5초 후 자동 복구 시도
        setTimeout(() => {
            if (WS_VIDEO_STATE.currentFlowState.name === 'ERROR') {
                WS_VIDEO_STATE_UTILS.transitionToState('WAITING');
                showInfoMessage('다시 시도할 수 있습니다');
            }
        }, 5000);
    },

    // 네트워크 에러
    handleNetworkError() {
        this.handleError('NETWORK_ERROR', '네트워크 연결을 확인해주세요');
        updateConnectionStatus('disconnected');
    },

    // 인증 에러
    handleAuthError() {
        this.handleError('AUTH_ERROR', '인증에 실패했습니다');
        setTimeout(() => {
            window.location.href = '/mobile/login?reason=auth_failed';
        }, 2000);
    },

    // 권한 에러
    handlePermissionError() {
        this.handleError('PERMISSION_ERROR', '카메라 권한이 필요합니다');
        showPermissionModal();
    }
};

// ========== 전역 에러 캐처 ==========
window.addEventListener('error', (event) => {
    WS_VIDEO_ERROR_HANDLER.handleError('JAVASCRIPT_ERROR', event.message);
});

window.addEventListener('unhandledrejection', (event) => {
    WS_VIDEO_ERROR_HANDLER.handleError('PROMISE_REJECTION', event.reason);
});

// ========== 전역 함수로 에러 발생 신고 ==========
window.reportError = (errorType, message) => {
    WS_VIDEO_ERROR_HANDLER.handleError(errorType, message);
};

// ========== 개발용 디버그 객체 ==========
if (WS_VIDEO_CONFIG.DEBUG.ENABLED) {
    window.WS_VIDEO_DEBUG = {
        getState: () => WS_VIDEO_STATE,
        getConfig: () => WS_VIDEO_CONFIG,
        getCurrentState: () => WS_VIDEO_STATE_UTILS.getCurrentState(),
        transitionTo: (stateName) => WS_VIDEO_STATE_UTILS.transitionToState(stateName),
        getDebugInfo: () => WS_VIDEO_STATE_UTILS.getDebugInfo(),
        simulateError: (type, message) => WS_VIDEO_ERROR_HANDLER.handleError(type, message)
    };
}

// 초기화 완료 로그
WS_VIDEO_LOGGER.info('간소화된 WebSocket 영상통화 설정 초기화 완료 (9개 상태)', {
    deviceType: WS_VIDEO_STATE.deviceType,
    deviceId: WS_VIDEO_STATE.deviceId,
    browserInfo: getBrowserInfo(),
    availableStates: Object.keys(WS_VIDEO_FLOW_STATES)
});