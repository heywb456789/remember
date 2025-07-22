/**
 * WebSocket 기반 영상통화 시스템 - 메인 초기화 로직
 */
import { authFetch, handleFetchError, checkLoginStatus } from './commonFetch.js';

// ========== 메인 초기화 클래스 ==========
class WSVideoCallMainController {
    constructor() {
        this.initializationInProgress = false;
        this.sessionCreated = false;
        this.authCheckInterval = null; // 추가
    }

    // 전체 시스템 초기화
    async initialize() {
        if (this.initializationInProgress) {
            WS_VIDEO_LOGGER.warn('초기화가 이미 진행 중입니다');
            return;
        }

        this.initializationInProgress = true;

        try {
            WS_VIDEO_LOGGER.info('WebSocket 영상통화 시스템 초기화 시작');
            // 0. 로그인 상태 확인 추가
            if (!checkLoginStatus()) {
                throw new Error('로그인이 필요합니다');
            }

            // 1. UI 매니저 초기화
            wsVideoUIManager.initialize();

            // 2. 초기 파라미터 설정
            this.setupInitialParameters();

            // 3. 외부 API 호출하여 대기영상 및 연락처 정보 받기
            await this.fetchInitialData();

            // 4. 권한 확인
            await this.checkPermissions();

            // 5. 토큰 상태 모니터링 시작 추가
            this.startAuthMonitoring();

            WS_VIDEO_LOGGER.info('시스템 초기화 완료');

        } catch (error) {
            WS_VIDEO_LOGGER.error('시스템 초기화 실패', error);
            this.handleInitializationError(error);
        } finally {
            this.initializationInProgress = false;
        }
    }

    // 토큰 상태 모니터링 시작
    startAuthMonitoring() {
        this.authCheckInterval = setInterval(() => {
            if (!checkLoginStatus()) {
                WS_VIDEO_LOGGER.warn('토큰 만료 감지 - 통화 종료');
                this.handleAuthFailure();
            }
        }, WS_VIDEO_CONFIG.AUTH.TOKEN_CHECK_INTERVAL);
    }

    // 인증 실패 처리
    handleAuthFailure() {
        if (this.authCheckInterval) {
            clearInterval(this.authCheckInterval);
            this.authCheckInterval = null;
        }

        showErrorMessage('로그인이 만료되었습니다. 다시 로그인해주세요.');

        // 정리 작업
        this.cleanup();

        // 로그인 페이지로 리다이렉트
        setTimeout(() => {
            window.location.href = '/mobile/login?reason=session_expired';
        }, 2000);
    }

    // 초기 파라미터 설정
    setupInitialParameters() {
        const params = window.INITIAL_PARAMS || {};

        WS_VIDEO_STATE.memberId = params.memberId || '1';
        WS_VIDEO_STATE.memorialId = params.memorialId || '1';
        WS_VIDEO_STATE.sessionCreatedAt = Date.now();

        WS_VIDEO_LOGGER.info('초기 파라미터 설정 완료', {
            memberId: WS_VIDEO_STATE.memberId,
            memorialId: WS_VIDEO_STATE.memorialId,
            deviceType: WS_VIDEO_STATE.deviceType,
            deviceId: WS_VIDEO_STATE.deviceId
        });
    }

    // 외부 API 호출하여 초기 데이터 받기
    async fetchInitialData() {
        try {
            WS_VIDEO_LOGGER.info('외부 API 호출 - 초기 데이터 요청');
            updateStatus('연결 정보 확인 중...');
            showLoadingModal('연결 준비 중...', '정보를 확인하고 있습니다');

            const requestData = {
                memberId: WS_VIDEO_STATE.memberId,
                memorialId: WS_VIDEO_STATE.memorialId,
                deviceType: WS_VIDEO_STATE.deviceType,
                deviceId: WS_VIDEO_STATE.deviceId
            };

            const response = await authFetch('/api/ws-video/initial-data', {
                method: 'POST',
                body: JSON.stringify(requestData)
            });

            // 응답 처리 (authFetch는 이미 JSON 파싱됨)
            if (response.status?.code === 'OK_0000') {
                const initialData = response.response;
                WS_VIDEO_STATE.contactName = initialData.contactName;
                WS_VIDEO_STATE.waitingVideoUrl = initialData.waitingVideoUrl;

                hideLoadingModal();
                updateStatus('연결 정보 확인 완료');

                WS_VIDEO_LOGGER.info('초기 데이터 수신 완료', initialData);
            } else {
                throw new Error(response.status?.message || '초기 데이터 요청 실패');
            }

        } catch (error) {
            hideLoadingModal();
            WS_VIDEO_LOGGER.error('초기 데이터 요청 실패', error);

            // 폴백 데이터 사용
            WS_VIDEO_STATE.contactName = '김근태';
            WS_VIDEO_STATE.waitingVideoUrl = WS_VIDEO_CONFIG.DEFAULT_WAITING_VIDEO;

            showWarningMessage('일부 정보를 기본값으로 설정했습니다');
        }
    }

    // 외부 API 호출 (개발 단계)
    async callExternalAPI() {
        // TODO: 실제 API 엔드포인트 연결
        // 현재는 모의 응답 반환

        WS_VIDEO_LOGGER.info('외부 API 모의 호출', {
            memberId: WS_VIDEO_STATE.memberId,
            memorialId: WS_VIDEO_STATE.memorialId
        });

        // 실제 구현시 사용할 코드 예시:
        /*
        const response = await fetch('/api/external/memorial-info', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                memberId: WS_VIDEO_STATE.memberId,
                memorialId: WS_VIDEO_STATE.memorialId
            })
        });

        if (!response.ok) {
            throw new Error(`외부 API 오류: ${response.status}`);
        }

        return await response.json();
        */

        // 개발용 모의 응답
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({
                    contactName: '김근태',
                    waitingVideoUrl: WS_VIDEO_CONFIG.DEFAULT_WAITING_VIDEO,
                    success: true
                });
            }, 1500); // 1.5초 지연으로 로딩 시뮬레이션
        });
    }

    // 권한 확인
    async checkPermissions() {
        try {
            WS_VIDEO_LOGGER.info('권한 확인 시작');
            updateStatus('권한 확인 중...');

            const hasPermissions = await wsVideoPermissionManager.checkExistingPermissions();

            if (hasPermissions) {
                WS_VIDEO_LOGGER.info('기존 권한 확인됨');
                setupCamera();
                await this.initializeWithPermissions();
            } else {
                WS_VIDEO_LOGGER.info('권한 요청 필요');
                showPermissionModal();
            }

        } catch (error) {
            WS_VIDEO_LOGGER.error('권한 확인 중 오류', error);
            showPermissionModal();
        }
    }

    // 권한이 있을 때 초기화
    async initializeWithPermissions() {
        try {
            WS_VIDEO_LOGGER.info('권한 기반 초기화 시작');

            // 대기영상 재생 시작
            await wsVideoPlayerManager.playWaitingVideo(WS_VIDEO_STATE.waitingVideoUrl, true);

            updateStatus('준비 완료');

            // 통화 시작 모달 표시
            setTimeout(() => {
                showCallStartModal();
            }, 1000);

        } catch (error) {
            WS_VIDEO_LOGGER.error('권한 기반 초기화 실패', error);
            this.handleInitializationError(error);
        }
    }

    // 실제 영상통화 세션 시작
    async startVideoCallSession() {
        if (this.sessionCreated) {
            WS_VIDEO_LOGGER.warn('세션이 이미 생성됨');
            return;
        }

        try {
            WS_VIDEO_LOGGER.info('영상통화 세션 시작');
            showLoadingModal('세션 생성 중...', '서버와 연결하고 있습니다');

            // 1. 세션 생성 API 호출
            const sessionData = await this.createVideoCallSession();

            // 2. WebSocket 연결
            const connected = await connectWebSocket(sessionData.sessionKey);
            if (!connected) {
                throw new Error('WebSocket 연결 실패');
            }

            // 3. 세션 상태 업데이트
            WS_VIDEO_STATE.sessionKey = sessionData.sessionKey;
            this.sessionCreated = true;

            hideLoadingModal();
            updateStatus(`${WS_VIDEO_STATE.contactName}님과 연결됨`);
            updateSessionInfo(sessionData.sessionKey);

            // 4. 오디오 활성화
            await this.enableAudio();

            WS_VIDEO_LOGGER.info('영상통화 세션 시작 완료', {
                sessionKey: sessionData.sessionKey
            });

        } catch (error) {
            hideLoadingModal();
            WS_VIDEO_LOGGER.error('영상통화 세션 시작 실패', error);
            showErrorMessage('세션 생성에 실패했습니다');

            // 재시도 옵션 제공
            setTimeout(() => {
                if (confirm('다시 시도하시겠습니까?')) {
                    this.startVideoCallSession();
                }
            }, 2000);
        }
    }

    // 영상통화 세션 생성 API 호출
    async createVideoCallSession() {
        const requestData = {
            contactName: WS_VIDEO_STATE.contactName,
            contactKey: WS_VIDEO_STATE.memberId, // contactKey로 memberId 사용
            memorialId: parseInt(WS_VIDEO_STATE.memorialId),
            callerId: parseInt(WS_VIDEO_STATE.memberId),
            deviceType: WS_VIDEO_STATE.deviceType,
            deviceId: WS_VIDEO_STATE.deviceId
        };

        WS_VIDEO_LOGGER.info('세션 생성 요청', requestData);

        try {
            // authFetch 사용으로 변경
            const response = await authFetch(`${WS_VIDEO_CONFIG.API_BASE_URL}/create-session`, {
                method: 'POST',
                body: JSON.stringify(requestData)
            });

            if (response.status?.code !== 'OK_0000') {
                throw new Error(response.status?.message || '세션 생성 실패');
            }

            WS_VIDEO_LOGGER.info('세션 생성 성공', response.response);
            return response.response;

        } catch (error) {
            WS_VIDEO_LOGGER.error('세션 생성 실패', error);
            handleFetchError(error);
            throw error;
        }
    }

    // 오디오 활성화
    async enableAudio() {
        try {
            const mainVideo = document.getElementById('mainVideo');
            if (mainVideo) {
                mainVideo.muted = false;
                mainVideo.volume = 0.8;

                if (!mainVideo.paused) {
                    await mainVideo.play();
                }

                WS_VIDEO_LOGGER.info('오디오 활성화 완료');
            }
        } catch (error) {
            WS_VIDEO_LOGGER.error('오디오 활성화 실패', error);
        }
    }

    // 초기화 오류 처리
    handleInitializationError(error) {
        WS_VIDEO_LOGGER.error('초기화 오류', error);

        updateStatus('초기화 실패');
        showErrorMessage('시스템 초기화에 실패했습니다');

        // 5초 후 페이지 새로고침
        setTimeout(() => {
            if (confirm('페이지를 새로고침하시겠습니까?')) {
                window.location.reload();
            } else {
                // 뒤로가기
                goBack();
            }
        }, 5000);
    }

    // 리소스 정리
    cleanup() {
        WS_VIDEO_LOGGER.info('리소스 정리 시작');

        try {

            // 토큰 모니터링 중지 추가
            if (this.authCheckInterval) {
                clearInterval(this.authCheckInterval);
                this.authCheckInterval = null;
            }

            // WebSocket 연결 종료
            disconnectWebSocket();

            // 미디어 스트림 정리
            if (WS_VIDEO_STATE.userMediaStream) {
                WS_VIDEO_STATE.userMediaStream.getTracks().forEach(track => {
                    track.stop();
                    WS_VIDEO_LOGGER.debug('미디어 트랙 정리', track.kind);
                });
                WS_VIDEO_STATE.userMediaStream = null;
            }

            // 녹화 중지
            if (WS_VIDEO_STATE.isRecording) {
                wsVideoRecordingManager.stopRecording();
            }

            // 타이머 정리
            if (wsVideoRecordingManager.recordingTimeout) {
                clearTimeout(wsVideoRecordingManager.recordingTimeout);
            }
            if (wsVideoRecordingManager.countdownInterval) {
                clearTimeout(wsVideoRecordingManager.countdownInterval);
            }

            // 전역 상태 초기화
            WS_VIDEO_STATE.sessionKey = null;
            WS_VIDEO_STATE.isConnected = false;
            WS_VIDEO_STATE.isRecording = false;
            WS_VIDEO_STATE.recordedChunks = [];

            // 모달 닫기
            wsVideoUIManager.hideAllModals();

            WS_VIDEO_LOGGER.info('리소스 정리 완료');

        } catch (error) {
            WS_VIDEO_LOGGER.error('리소스 정리 중 오류', error);
        }
    }
}

// ========== 전역 메인 컨트롤러 인스턴스 ==========
window.wsVideoMainController = new WSVideoCallMainController();

// ========== 전역 함수 내보내기 ==========
window.initializeVideoCall = () => wsVideoMainController.initialize();
window.startVideoCallSession = () => wsVideoMainController.startVideoCallSession();
window.cleanup = () => wsVideoMainController.cleanup();

// ========== 네트워크 연결 상태 모니터링 ==========
class NetworkMonitor {
    constructor() {
        this.isOnline = navigator.onLine;
        this.setupEventListeners();
    }

    setupEventListeners() {
        window.addEventListener('online', () => {
            this.isOnline = true;
            WS_VIDEO_LOGGER.info('네트워크 온라인');
            updateConnectionStatus('connected');

            // WebSocket 재연결 시도
            if (WS_VIDEO_STATE.sessionKey && !WS_VIDEO_STATE.isConnected) {
                setTimeout(() => {
                    handleNetworkOnline();
                }, 1000);
            }
        });

        window.addEventListener('offline', () => {
            this.isOnline = false;
            WS_VIDEO_LOGGER.warn('네트워크 오프라인');
            updateConnectionStatus('disconnected');
            updateStatus('네트워크 연결 없음');

            handleNetworkOffline();
        });
    }

    getStatus() {
        return this.isOnline;
    }
}

// ========== 세션 상태 모니터링 ==========
class SessionMonitor {
    constructor() {
        this.monitoringInterval = null;
        this.startMonitoring();
    }

    startMonitoring() {
        // 30초마다 세션 상태 체크
        this.monitoringInterval = setInterval(() => {
            this.checkSessionHealth();
        }, 30000);
    }

    checkSessionHealth() {
        if (!WS_VIDEO_STATE.sessionKey) return;

        const sessionAge = Date.now() - (WS_VIDEO_STATE.sessionCreatedAt || Date.now());
        const maxAge = WS_VIDEO_CONFIG.TIMERS.SESSION_TTL;

        if (sessionAge > maxAge) {
            WS_VIDEO_LOGGER.warn('세션 TTL 초과', {
                age: Math.floor(sessionAge / 1000 / 60),
                maxAge: Math.floor(maxAge / 1000 / 60)
            });

            showWarningMessage('세션이 곧 만료됩니다');
        }

        // WebSocket 연결 상태 체크
        if (!WS_VIDEO_STATE.isConnected) {
            WS_VIDEO_LOGGER.warn('WebSocket 연결 끊김 감지');
            updateConnectionStatus('disconnected');
        }
    }

    stopMonitoring() {
        if (this.monitoringInterval) {
            clearInterval(this.monitoringInterval);
            this.monitoringInterval = null;
        }
    }
}

// ========== 전역 모니터 인스턴스 ==========
window.networkMonitor = new NetworkMonitor();
window.sessionMonitor = new SessionMonitor();

// ========== 페이지 생명주기 이벤트 ==========
window.addEventListener('beforeunload', (event) => {
    WS_VIDEO_LOGGER.info('페이지 언로드 - 정리 작업 시작');

    // 동기적 정리 작업
    cleanup();

    // 서버에 세션 종료 알림 (beacon 사용)
    if (WS_VIDEO_STATE.sessionKey) {
        try {
            const accessToken = localStorage.getItem('accessToken');
            const cleanupData = JSON.stringify({
                reason: 'PAGE_UNLOAD',
                sessionKey: WS_VIDEO_STATE.sessionKey
            });

            // beacon에 Authorization 헤더 추가는 불가능하므로 URL에 토큰 포함
            const cleanupUrl = `${WS_VIDEO_CONFIG.API_BASE_URL}/session/${WS_VIDEO_STATE.sessionKey}/cleanup?token=${encodeURIComponent(accessToken)}`;

            navigator.sendBeacon(cleanupUrl, cleanupData);
        } catch (error) {
            WS_VIDEO_LOGGER.warn('세션 종료 알림 실패', error);
        }
    }
});

// 토큰 관련 이벤트 리스너 추가
window.addEventListener('tokenSynced', (event) => {
    WS_VIDEO_LOGGER.info('토큰 동기화됨', event.detail);

    // WebSocket이 연결되어 있다면 새 토큰으로 재인증
    if (WS_VIDEO_STATE.isConnected && wsVideoClient) {
        wsVideoClient.sendMessage({
            type: 'TOKEN_REFRESH',
            accessToken: event.detail.accessToken,
            timestamp: Date.now()
        });
    }
});

window.addEventListener('tokenCleared', (event) => {
    WS_VIDEO_LOGGER.warn('토큰이 정리됨', event.detail);

    // 통화 종료 처리
    if (wsVideoMainController) {
        wsVideoMainController.handleAuthFailure();
    }
});

window.addEventListener('pagehide', (event) => {
    if (event.persisted) {
        WS_VIDEO_LOGGER.info('페이지 캐시됨');
    } else {
        WS_VIDEO_LOGGER.info('페이지 완전히 언로드됨');
    }
});

window.addEventListener('pageshow', (event) => {
    if (event.persisted) {
        WS_VIDEO_LOGGER.info('페이지 캐시에서 복원됨 - 새로고침 필요');
        // 캐시된 페이지는 상태가 일관되지 않을 수 있으므로 새로고침
        window.location.reload();
    }
});

// ========== 에러 캐칭 및 로깅 ==========
window.addEventListener('error', (event) => {
    WS_VIDEO_LOGGER.error('전역 JavaScript 오류', {
        message: event.message,
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno,
        error: event.error
    });
});

window.addEventListener('unhandledrejection', (event) => {
    WS_VIDEO_LOGGER.error('처리되지 않은 Promise 거부', {
        reason: event.reason,
        promise: event.promise
    });

    // 사용자에게 알림
    if (WS_VIDEO_CONFIG.DEBUG.ENABLED) {
        showWarningMessage('예상치 못한 오류가 발생했습니다');
    }
});

// ========== 개발용 디버그 함수들 ==========
if (WS_VIDEO_CONFIG.DEBUG.ENABLED) {
    // 전역 디버그 객체
    window.WS_VIDEO_DEBUG = {
        getState: () => WS_VIDEO_STATE,
        getConfig: () => WS_VIDEO_CONFIG,
        forceReconnect: () => {
            if (wsVideoClient) {
                wsVideoClient.attemptReconnection();
            }
        },
        simulateError: (type) => {
            switch (type) {
                case 'network':
                    window.dispatchEvent(new Event('offline'));
                    break;
                case 'websocket':
                    if (WS_VIDEO_STATE.websocket) {
                        WS_VIDEO_STATE.websocket.close();
                    }
                    break;
                default:
                    throw new Error('Simulated error');
            }
        },
        showAllModals: () => {
            showLoadingModal('테스트', '모든 모달 테스트');
            setTimeout(() => showPermissionModal(), 1000);
            setTimeout(() => showCountdownModal(), 2000);
            setTimeout(() => showProgressModal(), 3000);
        }
    };

    WS_VIDEO_LOGGER.info('디버그 모드 활성화 - window.WS_VIDEO_DEBUG 사용 가능');
}

WS_VIDEO_LOGGER.info('메인 컨트롤러 초기화 완료');