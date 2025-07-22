/**
 * WebSocket 기반 영상통화 시스템 - WebSocket 통신 관리
 */

// ========== WebSocket 클라이언트 클래스 ==========
class WSVideoWebSocketClient {
    constructor() {
        this.websocket = null;
        this.heartbeatInterval = null;
        this.reconnectTimeout = null;
        this.isReconnecting = false;
        this.messageHandlers = new Map();

        // 메시지 핸들러 등록
        this.setupMessageHandlers();
    }

    // WebSocket 연결 시작
    async connect(sessionKey) {
        if (!sessionKey) {
            throw new Error('세션 키가 필요합니다');
        }

        const deviceType = WS_VIDEO_STATE.deviceType;
        const wsUrl = this.buildWebSocketUrl(deviceType, sessionKey);

        WS_VIDEO_LOGGER.info('WebSocket 연결 시도', { wsUrl, sessionKey, deviceType });

        try {
            this.websocket = new WebSocket(wsUrl);
            this.setupEventListeners();

            // 연결 완료 대기
            return new Promise((resolve, reject) => {
                const timeout = setTimeout(() => {
                    reject(new Error('WebSocket 연결 타임아웃'));
                }, WS_VIDEO_CONFIG.TIMERS.CONNECTION_TIMEOUT);

                this.websocket.onopen = () => {
                    clearTimeout(timeout);
                    WS_VIDEO_LOGGER.info('WebSocket 연결 성공');
                    resolve();
                };

                this.websocket.onerror = (error) => {
                    clearTimeout(timeout);
                    WS_VIDEO_LOGGER.error('WebSocket 연결 오류', error);
                    reject(new Error('WebSocket 연결 실패'));
                };
            });

        } catch (error) {
            WS_VIDEO_LOGGER.error('WebSocket 연결 실패', error);
            throw error;
        }
    }

    // WebSocket URL 생성
    buildWebSocketUrl(deviceType, sessionKey) {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.host;

        // 토큰을 WebSocket URL에 포함
        const accessToken = localStorage.getItem('accessToken');
        const tokenParam = accessToken ? `?token=${encodeURIComponent(accessToken)}` : '';

        const pathMap = {
            'WEB': `/ws/memorial-video/web/${sessionKey}${tokenParam}`,
            'MOBILE_WEB': `/ws/memorial-video/mobile-web/${sessionKey}${tokenParam}`,
            'IOS_APP': `/ws/memorial-video/ios/${sessionKey}${tokenParam}`,
            'ANDROID_APP': `/ws/memorial-video/android/${sessionKey}${tokenParam}`
        };

        const path = pathMap[deviceType] || pathMap['WEB'];
        return `${protocol}//${host}${path}`;
    }

    // 이벤트 리스너 설정
    setupEventListeners() {
        this.websocket.onopen = (event) => {
            WS_VIDEO_LOGGER.info('WebSocket 연결됨');
            WS_VIDEO_STATE.isConnected = true;
            WS_VIDEO_STATE.reconnectAttempts = 0;

            this.sendConnectMessage();
            this.startHeartbeat();

            // UI 업데이트
            updateConnectionStatus('connected');
        };

        this.websocket.onmessage = (event) => {
            this.handleMessage(event);
        };

        this.websocket.onerror = (error) => {
            WS_VIDEO_LOGGER.error('WebSocket 오류', error);
            updateConnectionStatus('error');
        };

        this.websocket.onclose = (event) => {
            WS_VIDEO_LOGGER.warn('WebSocket 연결 종료', {
                code: event.code,
                reason: event.reason
            });

            WS_VIDEO_STATE.isConnected = false;
            this.stopHeartbeat();

            if (!this.isReconnecting && event.code !== 1000) {
                this.attemptReconnection();
            }

            updateConnectionStatus('disconnected');
        };
    }

    // 연결 메시지 전송
    sendConnectMessage() {
        const accessToken = localStorage.getItem('accessToken');

        const message = {
            type: WS_MESSAGE_TYPES.CONNECT,
            contactName: WS_VIDEO_STATE.contactName || '연결 중...',
            deviceType: WS_VIDEO_STATE.deviceType,
            deviceId: WS_VIDEO_STATE.deviceId,
            memberId: WS_VIDEO_STATE.memberId, // 추가
            accessToken: accessToken, // 추가 (WebSocket 연결 시 인증)
            reconnect: WS_VIDEO_STATE.reconnectAttempts > 0
        };

        this.sendMessage(message);
        WS_VIDEO_LOGGER.info('연결 메시지 전송', message);
    }

    // 메시지 전송
    sendMessage(message) {
        if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
            this.websocket.send(JSON.stringify(message));
            WS_VIDEO_LOGGER.debug('메시지 전송', message);
        } else {
            WS_VIDEO_LOGGER.warn('WebSocket 연결되지 않음 - 메시지 전송 실패', message);
        }
    }

    // 메시지 수신 처리
    handleMessage(event) {
        try {
            const message = JSON.parse(event.data);
            WS_VIDEO_LOGGER.debug('메시지 수신', message);

            const handler = this.messageHandlers.get(message.type);
            if (handler) {
                handler(message);
            } else {
                WS_VIDEO_LOGGER.warn('알 수 없는 메시지 타입', message.type);
            }

        } catch (error) {
            WS_VIDEO_LOGGER.error('메시지 파싱 오류', error, event.data);
        }
    }

    // 메시지 핸들러 설정
    setupMessageHandlers() {
        // 연결 완료
        this.messageHandlers.set(WS_MESSAGE_TYPES.CONNECTED, (message) => {
            WS_VIDEO_LOGGER.info('서버 연결 확인', message);
            WS_VIDEO_STATE.sessionKey = message.sessionKey;
            WS_VIDEO_STATE.contactName = message.contactName;

            updateStatus(`${message.contactName}님과 연결됨`);
            updateSessionInfo(message.sessionKey);
        });

        // 하트비트
        this.messageHandlers.set(WS_MESSAGE_TYPES.HEARTBEAT, (message) => {
            WS_VIDEO_STATE.lastHeartbeat = Date.now();
            this.sendMessage({
                type: WS_MESSAGE_TYPES.HEARTBEAT_RESPONSE,
                timestamp: Date.now()
            });
        });

        // 상태 전환
        this.messageHandlers.set(WS_MESSAGE_TYPES.STATE_TRANSITION, (message) => {
            WS_VIDEO_LOGGER.info('상태 전환', {
                from: message.previousState,
                to: message.newState
            });

            this.handleStateTransition(message);
        });

        // 영상 제어
        this.messageHandlers.set(WS_MESSAGE_TYPES.PLAY_WAITING_VIDEO, (message) => {
            playWaitingVideo(message.waitingVideoUrl, message.loop);
        });

        this.messageHandlers.set(WS_MESSAGE_TYPES.START_RECORDING, (message) => {
            startRecordingCountdown(message.countdown, message.maxDuration);
        });

        this.messageHandlers.set(WS_MESSAGE_TYPES.PLAY_RESPONSE_VIDEO, (message) => {
            playResponseVideo(message.responseVideoUrl, message.autoReturn);
        });

        // 처리 진행률
        this.messageHandlers.set(WS_MESSAGE_TYPES.PROCESSING_PROGRESS, (message) => {
            updateProcessingProgress(message.progress, message.message);
        });

        // 오류
        this.messageHandlers.set(WS_MESSAGE_TYPES.ERROR, (message) => {
            WS_VIDEO_LOGGER.error('서버 오류', message);
            showErrorMessage(message.message || '서버 오류가 발생했습니다');
        });

        // 인증 실패 처리 추가
        this.messageHandlers.set('AUTHENTICATION_FAILED', (message) => {
            WS_VIDEO_LOGGER.error('WebSocket 인증 실패', message);

            showErrorMessage('인증이 만료되었습니다. 다시 로그인해주세요.');

            // 정리 후 로그인 페이지로 리다이렉트
            setTimeout(() => {
                if (typeof cleanup === 'function') {
                    cleanup();
                }
                window.location.href = '/mobile/login?reason=websocket_auth_failed';
            }, 2000);
        });

        // 토큰 만료 처리 추가
        this.messageHandlers.set('TOKEN_EXPIRED', (message) => {
            WS_VIDEO_LOGGER.warn('WebSocket 토큰 만료', message);

            // 토큰 갱신 시도
            this.handleTokenRefresh();
        });
    }

    // 토큰 갱신 처리 함수 추가
    async handleTokenRefresh() {
        try {
            await handleTokenRefresh(); // CommonFetch의 토큰 갱신 함수 사용

            // 새 토큰으로 재연결
            const newToken = localStorage.getItem('accessToken');
            if (newToken && WS_VIDEO_STATE.sessionKey) {
                this.attemptReconnection();
            }

        } catch (error) {
            WS_VIDEO_LOGGER.error('WebSocket 토큰 갱신 실패', error);
            handleFetchError(error);
        }
    }

    // 상태 전환 처리
    handleStateTransition(message) {
        const newState = message.newState;
        const previousState = WS_VIDEO_STATE.currentState;

        WS_VIDEO_STATE.currentState = newState;
        updateStatus(message.stateDisplayName || newState);

        // 상태별 특별 처리
        switch (newState) {
            case WS_VIDEO_STATES.PERMISSION_REQUESTING:
                // 권한 요청이 필요한 경우
                if (!WS_VIDEO_STATE.cameraPermissionGranted) {
                    showPermissionModal();
                }
                break;

            case WS_VIDEO_STATES.WAITING_PLAYING:
                // 대기영상 재생 시작
                break;

            case WS_VIDEO_STATES.RECORDING_COUNTDOWN:
                // 녹화 카운트다운 시작
                break;

            case WS_VIDEO_STATES.PROCESSING_AI:
                // AI 처리 진행률 모달 표시
                showProgressModal();
                break;

            case WS_VIDEO_STATES.RESPONSE_READY:
                hideProgressModal();
                break;

            case WS_VIDEO_STATES.CALL_COMPLETED:
                this.handleCallCompleted();
                break;

            case WS_VIDEO_STATES.ERROR_NETWORK:
            case WS_VIDEO_STATES.ERROR_PERMISSION:
            case WS_VIDEO_STATES.ERROR_PROCESSING:
            case WS_VIDEO_STATES.ERROR_TIMEOUT:
                this.handleError(newState, message);
                break;
        }
    }

    // 오류 처리
    handleError(errorState, message) {
        WS_VIDEO_LOGGER.error('상태 오류', { errorState, message });

        const errorMessages = {
            [WS_VIDEO_STATES.ERROR_NETWORK]: '네트워크 연결에 문제가 있습니다',
            [WS_VIDEO_STATES.ERROR_PERMISSION]: '카메라 또는 마이크 권한이 필요합니다',
            [WS_VIDEO_STATES.ERROR_PROCESSING]: '영상 처리 중 오류가 발생했습니다',
            [WS_VIDEO_STATES.ERROR_TIMEOUT]: '요청 시간이 초과되었습니다'
        };

        const errorMessage = errorMessages[errorState] || '알 수 없는 오류가 발생했습니다';
        showErrorMessage(errorMessage);

        // 특정 오류의 경우 재연결 시도
        if (errorState === WS_VIDEO_STATES.ERROR_NETWORK) {
            setTimeout(() => {
                this.attemptReconnection();
            }, 3000);
        }
    }

    // 통화 완료 처리
    handleCallCompleted() {
        WS_VIDEO_LOGGER.info('통화 완료');

        // 정리 작업
        this.stopHeartbeat();

        // 피드백 페이지로 이동
        setTimeout(() => {
            const memberId = WS_VIDEO_STATE.memberId;
            const memorialId = WS_VIDEO_STATE.memorialId;
            window.location.href = `/call/feedback?memberId=${memberId}&memorialId=${memorialId}`;
        }, 2000);
    }

    // 하트비트 시작
    startHeartbeat() {
        this.stopHeartbeat();

        this.heartbeatInterval = setInterval(() => {
            if (WS_VIDEO_STATE.lastHeartbeat) {
                const timeSinceLastHeartbeat = Date.now() - WS_VIDEO_STATE.lastHeartbeat;

                if (timeSinceLastHeartbeat > WS_VIDEO_CONFIG.HEARTBEAT_INTERVAL * 2) {
                    WS_VIDEO_LOGGER.warn('하트비트 타임아웃 - 재연결 필요');
                    this.attemptReconnection();
                    return;
                }
            }
        }, WS_VIDEO_CONFIG.HEARTBEAT_INTERVAL);
    }

    // 하트비트 중지
    stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
            this.heartbeatInterval = null;
        }
    }

    // 재연결 시도
    attemptReconnection() {
        if (this.isReconnecting) {
            return;
        }

        if (WS_VIDEO_STATE.reconnectAttempts >= WS_VIDEO_CONFIG.MAX_RECONNECT_ATTEMPTS) {
            WS_VIDEO_LOGGER.error('최대 재연결 시도 횟수 초과');
            showErrorMessage('서버 연결에 실패했습니다. 페이지를 새로고침해주세요.');
            return;
        }

        this.isReconnecting = true;
        WS_VIDEO_STATE.reconnectAttempts++;

        const delay = Math.min(
            WS_VIDEO_CONFIG.RECONNECT_DELAY_BASE * Math.pow(2, WS_VIDEO_STATE.reconnectAttempts - 1),
            30000
        );

        WS_VIDEO_LOGGER.info(`${delay / 1000}초 후 재연결 시도 (${WS_VIDEO_STATE.reconnectAttempts}/${WS_VIDEO_CONFIG.MAX_RECONNECT_ATTEMPTS})`);

        updateStatus(`${Math.round(delay / 1000)}초 후 재연결...`);
        updateConnectionStatus('connecting');

        this.reconnectTimeout = setTimeout(async () => {
            try {
                await this.connect(WS_VIDEO_STATE.sessionKey);
                this.isReconnecting = false;
                WS_VIDEO_LOGGER.info('재연결 성공');
            } catch (error) {
                this.isReconnecting = false;
                WS_VIDEO_LOGGER.error('재연결 실패', error);
                this.attemptReconnection();
            }
        }, delay);
    }

    // 연결 종료
    disconnect() {
        WS_VIDEO_LOGGER.info('WebSocket 연결 종료');

        this.isReconnecting = false;
        this.stopHeartbeat();

        if (this.reconnectTimeout) {
            clearTimeout(this.reconnectTimeout);
            this.reconnectTimeout = null;
        }

        if (this.websocket) {
            // 정상 종료 메시지 전송
            this.sendMessage({
                type: WS_MESSAGE_TYPES.DISCONNECT,
                reason: 'USER_ACTION'
            });

            this.websocket.close(1000, 'Normal closure');
            this.websocket = null;
        }

        WS_VIDEO_STATE.isConnected = false;
        WS_VIDEO_STATE.websocket = null;
    }

    // 상태 변경 알림
    notifyStateChange(newState, reason = 'USER_ACTION') {
        const message = {
            type: WS_MESSAGE_TYPES.CLIENT_STATE_CHANGE,
            newState: newState,
            reason: reason,
            timestamp: Date.now()
        };

        this.sendMessage(message);
        WS_VIDEO_LOGGER.info('클라이언트 상태 변경 알림', message);
    }

    // 권한 상태 보고
    notifyPermissionStatus(cameraGranted, microphoneGranted) {
        const message = {
            type: WS_MESSAGE_TYPES.PERMISSION_STATUS,
            cameraGranted: cameraGranted,
            microphoneGranted: microphoneGranted,
            timestamp: Date.now()
        };

        this.sendMessage(message);
        WS_VIDEO_LOGGER.info('권한 상태 보고', message);
    }

    // 디바이스 정보 전송
    sendDeviceInfo() {
        const browserInfo = getBrowserInfo();

        const message = {
            type: WS_MESSAGE_TYPES.DEVICE_INFO,
            deviceId: WS_VIDEO_STATE.deviceId,
            deviceType: WS_VIDEO_STATE.deviceType,
            userAgent: browserInfo.userAgent,
            screenResolution: `${screen.width}x${screen.height}`,
            platform: browserInfo.platform,
            timestamp: Date.now()
        };

        this.sendMessage(message);
        WS_VIDEO_LOGGER.info('디바이스 정보 전송', message);
    }

    // 영상 이벤트 알림
    notifyWaitingVideoStarted() {
        this.sendMessage({
            type: WS_MESSAGE_TYPES.WAITING_VIDEO_STARTED,
            timestamp: Date.now()
        });
    }

    notifyWaitingVideoError(error) {
        this.sendMessage({
            type: WS_MESSAGE_TYPES.WAITING_VIDEO_ERROR,
            error: error.message || error,
            timestamp: Date.now()
        });
    }

    notifyRecordingStarted() {
        this.sendMessage({
            type: WS_MESSAGE_TYPES.RECORDING_STARTED,
            timestamp: Date.now()
        });
    }

    notifyRecordingStopped(duration) {
        this.sendMessage({
            type: WS_MESSAGE_TYPES.RECORDING_STOPPED,
            duration: duration,
            timestamp: Date.now()
        });
    }

    notifyRecordingError(error) {
        this.sendMessage({
            type: WS_MESSAGE_TYPES.RECORDING_ERROR,
            error: error.message || error,
            timestamp: Date.now()
        });
    }

    notifyVideoUploadComplete(filePath) {
        this.sendMessage({
            type: WS_MESSAGE_TYPES.VIDEO_UPLOAD_COMPLETE,
            filePath: filePath,
            timestamp: Date.now()
        });
    }

    notifyVideoUploadError(error) {
        this.sendMessage({
            type: WS_MESSAGE_TYPES.VIDEO_UPLOAD_ERROR,
            error: error.message || error,
            timestamp: Date.now()
        });
    }

    notifyResponseVideoStarted() {
        this.sendMessage({
            type: WS_MESSAGE_TYPES.RESPONSE_VIDEO_STARTED,
            timestamp: Date.now()
        });
    }

    notifyResponseVideoEnded(watchedDuration) {
        this.sendMessage({
            type: WS_MESSAGE_TYPES.RESPONSE_VIDEO_ENDED,
            watchedDuration: watchedDuration,
            timestamp: Date.now()
        });
    }

    notifyResponseVideoError(error) {
        this.sendMessage({
            type: WS_MESSAGE_TYPES.RESPONSE_VIDEO_ERROR,
            error: error.message || error,
            timestamp: Date.now()
        });
    }
}

// ========== 전역 WebSocket 클라이언트 인스턴스 ==========
window.wsVideoClient = new WSVideoWebSocketClient();

// ========== WebSocket 관련 유틸리티 함수 ==========
window.connectWebSocket = async function(sessionKey) {
    try {
        await wsVideoClient.connect(sessionKey);
        WS_VIDEO_STATE.websocket = wsVideoClient;

        // 디바이스 정보 전송
        setTimeout(() => {
            wsVideoClient.sendDeviceInfo();
        }, 1000);

        return true;
    } catch (error) {
        WS_VIDEO_LOGGER.error('WebSocket 연결 실패', error);
        return false;
    }
};

window.disconnectWebSocket = function() {
    if (wsVideoClient) {
        wsVideoClient.disconnect();
    }
};

window.sendStateChange = function(newState, reason) {
    if (wsVideoClient) {
        wsVideoClient.notifyStateChange(newState, reason);
    }
};

window.sendPermissionStatus = function(cameraGranted, microphoneGranted) {
    if (wsVideoClient) {
        wsVideoClient.notifyPermissionStatus(cameraGranted, microphoneGranted);
    }
};

// ========== 네트워크 상태 관리 ==========
window.handleNetworkOnline = function() {
    WS_VIDEO_LOGGER.info('네트워크 온라인 상태');
    updateConnectionStatus('connecting');

    // 재연결 시도
    setTimeout(() => {
        if (WS_VIDEO_STATE.sessionKey && !WS_VIDEO_STATE.isConnected) {
            wsVideoClient.attemptReconnection();
        }
    }, 1000);
};

window.handleNetworkOffline = function() {
    WS_VIDEO_LOGGER.warn('네트워크 오프라인 상태');
    updateConnectionStatus('disconnected');
    updateStatus('네트워크 오프라인');
};

window.handleAppVisible = function() {
    WS_VIDEO_LOGGER.info('앱 활성화됨');

    // WebSocket 연결 상태 확인
    if (WS_VIDEO_STATE.sessionKey && !WS_VIDEO_STATE.isConnected) {
        setTimeout(() => {
            wsVideoClient.attemptReconnection();
        }, 500);
    }
};

window.handleAppHidden = function() {
    WS_VIDEO_LOGGER.info('앱 비활성화됨');
    // 녹화 중이면 중지
    if (WS_VIDEO_STATE.isRecording) {
        stopRecording();
    }
};

WS_VIDEO_LOGGER.info('WebSocket 클라이언트 초기화 완료');