/**
 * WebSocket 기반 영상통화 시스템 - 완전 수정된 WebSocket 통신 관리
 * 🔧 중복 메시지 방지 및 누락된 메시지 타입 추가
 */

class WSVideoWebSocketClient {
    constructor() {
        this.websocket = null;
        this.heartbeatInterval = null;
        this.reconnectTimeout = null;
        this.isReconnecting = false;
        this.authenticated = false;
        this.authTimeout = null;
        this.messageHandlers = new Map();
        this.lastStateChangeTime = 0;
        this.stateChangeThrottle = 1000;

        this.setupMessageHandlers();
    }

    // ========== 완전 수정된 메시지 핸들러 ==========
    setupMessageHandlers() {
        // 1. 인증 성공
        this.messageHandlers.set('AUTH_SUCCESS', (message) => {
            WS_VIDEO_LOGGER.info('✅ WebSocket 인증 성공');

            this.authenticated = true;
            WS_VIDEO_STATE.isConnected = true;
            this.clearAuthTimeout();

            updateStatus('연결됨');
            updateConnectionStatus('connected');

            this.startHeartbeat();
            setTimeout(() => this.sendDeviceInfo(), 1000);
        });

        // 2. 상태 전환 (핵심 - 수정됨)
        this.messageHandlers.set('STATE_TRANSITION', (message) => {
            WS_VIDEO_LOGGER.info('🔄 상태 전환:', message.previousState, '→', message.newState);

            this.updateUIForState(message);
        });

        // 3. 대기영상 재생 명령
        this.messageHandlers.set('PLAY_WAITING_VIDEO', (message) => {
            WS_VIDEO_LOGGER.info('🎬 대기영상 재생 명령');
            playWaitingVideo(message.waitingVideoUrl, message.loop);
        });

        // 4. 녹화 시작 명령 (수정됨)
        this.messageHandlers.set('START_RECORDING', (message) => {
            WS_VIDEO_LOGGER.info('🔴 녹화 시작 명령 수신');

            // // 🔧 서버에서 명령이 오면 실제 녹화 시작
            // if (wsVideoRecordingManager && typeof startActualRecording === 'function') {
            //     const maxDuration = message.maxDuration || 10;
            //     startActualRecording(maxDuration);
            // }
        });

        // 5. 응답영상 재생 명령
        this.messageHandlers.set('PLAY_RESPONSE_VIDEO', (message) => {
            WS_VIDEO_LOGGER.info('🎬 응답영상 재생 명령');
            playResponseVideo(message.videoUrl);
        });

        // 6. 하트비트
        this.messageHandlers.set('HEARTBEAT', (message) => {
            WS_VIDEO_STATE.lastHeartbeat = Date.now();
            this.sendMessage({
                type: 'HEARTBEAT_RESPONSE',
                timestamp: Date.now()
            });
        });

        // 7. 🆕 처리 진행 상황 (누락된 메시지 타입 추가)
        this.messageHandlers.set('PROCESSING_PROGRESS', (message) => {
            WS_VIDEO_LOGGER.info('🤖 처리 진행 상황:', message.message);
            updateStatus(message.message || 'AI 처리 중...', 'loading');

        });

        // 8. 메시지들 (통합)
        this.messageHandlers.set('ERROR', (message) => {
            WS_VIDEO_LOGGER.error('❌ 서버 오류:', message.message);
            showErrorMessage(message.message);

            // 오류 시 녹화 상태 초기화
            if (wsVideoRecordingManager) {
                wsVideoRecordingManager.resetRecordingState();
            }
        });

        this.messageHandlers.set('INFO', (message) => {
            WS_VIDEO_LOGGER.info('ℹ️ 서버 정보:', message.message);
            if (message.showLoading) showVideoLoadingOverlay();
            showInfoMessage(message.message);
        });

        this.messageHandlers.set('SUCCESS', (message) => {
            showSuccessMessage(message.message);
            if (message.redirectUrl && message.redirectDelay) {
                setTimeout(() => {
                    window.location.href = message.redirectUrl;
                }, message.redirectDelay);
            }
        });

        // 9. 토큰 갱신 응답
        this.messageHandlers.set('TOKEN_REFRESHED', (message) => {
            WS_VIDEO_LOGGER.info('✅ 토큰 갱신 완료');
            showSuccessMessage('인증이 갱신되었습니다');
        });
    }

    // 상태별 UI 업데이트 (기존과 동일)
    updateUIForState(message) {
        const state = message.newState;
        const display = message.stateDisplayName;

        if (WS_VIDEO_STATE_UTILS && typeof WS_VIDEO_STATE_UTILS.transitionToState === 'function') {
            WS_VIDEO_STATE_UTILS.transitionToState(state);
            WS_VIDEO_LOGGER.info('클라이언트 상태 동기화:', state);
        }

        updateStatus(display, this.getStatusType(state));

        if (message.showLoading) {
            showVideoLoadingOverlay();
        } else {
            hideVideoLoadingOverlay();
        }

        const recordBtn = document.getElementById('recordBtn');
        if (recordBtn) {
            recordBtn.disabled = !message.canRecord;
            recordBtn.classList.toggle('disabled', !message.canRecord);
            recordBtn.title = message.canRecord ? '녹화하기' : '녹화할 수 없습니다';
        }

        if (state === 'RECORDING') {
            updateRecordingUI(true);
            document.body.classList.add('recording-active');
        } else {
            updateRecordingUI(false);
            document.body.classList.remove('recording-active');
        }

        updateConnectionStatus(message.isErrorState ? 'error' : 'connected');
    }

    getStatusType(state) {
        switch (state) {
            case 'RECORDING': return 'recording';
            case 'RESPONSE_PLAYING': return 'success';
            case 'PROCESSING': return 'loading';
            case 'ERROR': return 'error';
            default: return 'info';
        }
    }

    // 🔧 throttle된 메시지 전송 (수정됨)
    sendMessage(message, allowThrottle = false) {
        if (!this.websocket || this.websocket.readyState !== WebSocket.OPEN) {
            WS_VIDEO_LOGGER.warn('WebSocket 연결되지 않음 - 메시지 전송 실패', message);
            return false;
        }

        // 상태 변경 메시지에 대한 throttle 적용
        if (allowThrottle && message.type === 'CLIENT_STATE_CHANGE') {
            const now = Date.now();
            if (now - this.lastStateChangeTime < this.stateChangeThrottle) {
                WS_VIDEO_LOGGER.warn('🚫 상태 변경 메시지 throttle - 중복 방지', message);
                return false;
            }
            this.lastStateChangeTime = now;
        }

        try {
            this.websocket.send(JSON.stringify(message));
            WS_VIDEO_LOGGER.debug('📤 메시지 전송:', message.type);
            return true;
        } catch (error) {
            WS_VIDEO_LOGGER.error('메시지 전송 실패', error, message);
            return false;
        }
    }

    sendStateChangeMessage(newState, reason, additionalData = {}) {
        const message = {
            type: 'CLIENT_STATE_CHANGE',
            newState: newState,
            reason: reason,
            timestamp: Date.now(),
            ...additionalData
        };

        const sent = this.sendMessage(message, true); // throttle 적용
        if (sent) {
            WS_VIDEO_LOGGER.info(`🔄 상태 변경 요청: ${newState} (${reason})`);
        }

        return sent;
    }

    // 메시지 수신 처리
    handleMessage(event) {
        try {
            const message = JSON.parse(event.data);
            WS_VIDEO_LOGGER.debug('📨 메시지 수신:', message.type);

            const handler = this.messageHandlers.get(message.type);
            if (handler) {
                handler(message);
            } else {
                WS_VIDEO_LOGGER.warn('⚠️ 알 수 없는 메시지 타입:', message.type);
            }

        } catch (error) {
            WS_VIDEO_LOGGER.error('메시지 파싱 오류', error, event.data);
        }
    }

    // ========== 간소화된 알림 메서드들 ==========

    notifyWaitingVideoEvent(eventType, data = {}) {
        this.sendMessage({
            type: 'WAITING_VIDEO_EVENT',
            eventType: eventType,
            timestamp: Date.now(),
            ...data
        });
    }

    notifyResponseVideoEvent(eventType, data = {}) {
        this.sendMessage({
            type: 'RESPONSE_VIDEO_EVENT',
            eventType: eventType,
            timestamp: Date.now(),
            ...data
        });
    }

    notifyRecordingError(error) {
        this.sendStateChangeMessage('ERROR', 'RECORDING_ERROR', {
            error: error.message || error
        });
    }

    notifyVideoUploadComplete(filePath) {
        this.sendMessage({
            type: 'VIDEO_UPLOAD_COMPLETE',
            filePath: filePath,
            timestamp: Date.now()
        });
    }

    notifyVideoUploadError(error) {
        this.sendStateChangeMessage('ERROR', 'UPLOAD_ERROR', {
            error: error.message || error
        });
    }

    // 편의 메서드들
    notifyWaitingVideoStarted() {
        this.notifyWaitingVideoEvent('started');
    }

    notifyWaitingVideoError(error) {
        this.notifyWaitingVideoEvent('error', { error: error.message || error });
    }

    notifyResponseVideoStarted() {
        this.notifyResponseVideoEvent('started');
    }

    notifyResponseVideoEnded(duration) {
        this.notifyResponseVideoEvent('ended', { duration });
    }

    notifyResponseVideoError(error) {
        this.notifyResponseVideoEvent('error', { error: error.message || error });
    }

    // ========== 연결 관리 (기존과 동일) ==========

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

            return new Promise((resolve, reject) => {
                const timeout = setTimeout(() => {
                    reject(new Error('WebSocket 연결 타임아웃'));
                }, WS_VIDEO_CONFIG.TIMERS.CONNECTION_TIMEOUT);

                const originalOnOpen = this.websocket.onopen;
                this.websocket.onopen = (event) => {
                    clearTimeout(timeout);
                    if (originalOnOpen) {
                        originalOnOpen.call(this.websocket, event);
                    }
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

    buildWebSocketUrl(deviceType, sessionKey) {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.host;

        const pathMap = {
            'WEB': `/ws/memorial-video/web/${sessionKey}`,
            'MOBILE_WEB': `/ws/memorial-video/mobile-web/${sessionKey}`,
            'IOS_APP': `/ws/memorial-video/ios/${sessionKey}`,
            'ANDROID_APP': `/ws/memorial-video/android/${sessionKey}`
        };

        const path = pathMap[deviceType] || pathMap['WEB'];
        return `${protocol}//${host}${path}`;
    }

    setupEventListeners() {
        this.websocket.onopen = (event) => {
            WS_VIDEO_LOGGER.info('WebSocket 연결됨 - AUTH 메시지 전송');
            this.authenticated = false;

            this.sendAuthMessage();

            this.authTimeout = setTimeout(() => {
                if (!this.authenticated) {
                    WS_VIDEO_LOGGER.error('WebSocket 인증 타임아웃');
                    showErrorMessage('인증 시간이 초과되었습니다');
                    this.websocket.close(4001, 'Authentication timeout');
                }
            }, 5000);

            updateConnectionStatus('connecting');
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

            this.authenticated = false;
            WS_VIDEO_STATE.isConnected = false;
            this.stopHeartbeat();
            this.clearAuthTimeout();

            // 🔧 연결 종료 시 녹화 상태 초기화
            if (wsVideoRecordingManager) {
                wsVideoRecordingManager.resetRecordingState();
            }

            if (event.code >= 4001 && event.code <= 4003) {
                this.handleAuthenticationError(event);
                return;
            }

            if (!this.isReconnecting && event.code !== 1000) {
                this.attemptReconnection();
            }

            updateConnectionStatus('disconnected');
        };
    }

    sendAuthMessage() {
        if (!this.websocket || this.websocket.readyState !== WebSocket.OPEN) {
            WS_VIDEO_LOGGER.error('WebSocket 연결되지 않음');
            return;
        }

        const accessToken = localStorage.getItem('accessToken');
        if (!accessToken) {
            WS_VIDEO_LOGGER.error('AccessToken 없음');
            showErrorMessage('인증 토큰이 없습니다. 다시 로그인해주세요.');
            setTimeout(() => {
                window.location.href = '/mobile/login?reason=no_token';
            }, 2000);
            return;
        }

        const message = {
            type: 'AUTH',
            token: accessToken,
            sessionKey: WS_VIDEO_STATE.sessionKey,
            deviceType: WS_VIDEO_STATE.deviceType
        };

        this.sendMessage(message);
        WS_VIDEO_LOGGER.info('AUTH 메시지 전송 완료');
    }

    clearAuthTimeout() {
        if (this.authTimeout) {
            clearTimeout(this.authTimeout);
            this.authTimeout = null;
        }
    }

    handleAuthenticationError(event) {
        let errorMessage = '인증에 실패했습니다';

        switch (event.code) {
            case 4001:
                errorMessage = '인증 시간이 초과되었습니다';
                break;
            case 4002:
                errorMessage = '유효하지 않은 토큰입니다';
                break;
            case 4003:
                errorMessage = '세션 접근 권한이 없습니다';
                break;
        }

        showErrorMessage(errorMessage + '. 다시 로그인해주세요.');

        setTimeout(() => {
            if (typeof cleanup === 'function') {
                cleanup();
            }
            window.location.href = '/mobile/login?reason=websocket_auth_failed';
        }, 2000);
    }

    attemptReconnection() {
        if (this.isReconnecting) return;

        if (WS_VIDEO_STATE.reconnectAttempts >= WS_VIDEO_CONFIG.MAX_RECONNECT_ATTEMPTS) {
            WS_VIDEO_LOGGER.error('최대 재연결 시도 횟수 초과');
            showErrorMessage('서버 연결에 실패했습니다. 페이지를 새로고침해주세요.');
            return;
        }

        this.isReconnecting = true;
        this.authenticated = false;
        WS_VIDEO_STATE.reconnectAttempts++;

        const delay = Math.min(
            WS_VIDEO_CONFIG.RECONNECT_DELAY_BASE * Math.pow(2, WS_VIDEO_STATE.reconnectAttempts - 1),
            30000
        );

        WS_VIDEO_LOGGER.info(`${delay/1000}초 후 재연결 시도 (${WS_VIDEO_STATE.reconnectAttempts}/${WS_VIDEO_CONFIG.MAX_RECONNECT_ATTEMPTS})`);

        updateStatus(`${Math.round(delay/1000)}초 후 재연결...`);
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

    refreshToken(newToken) {
        if (!this.authenticated || !this.websocket || this.websocket.readyState !== WebSocket.OPEN) {
            WS_VIDEO_LOGGER.warn('토큰 갱신 불가 - WebSocket 미연결 또는 미인증');
            return;
        }

        this.sendMessage({
            type: 'TOKEN_REFRESH',
            accessToken: newToken,
            timestamp: Date.now()
        });

        WS_VIDEO_LOGGER.info('토큰 갱신 메시지 전송');
    }

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

    stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
            this.heartbeatInterval = null;
        }
    }

    sendDeviceInfo() {
        const browserInfo = getBrowserInfo();

        this.sendMessage({
            type: 'DEVICE_INFO',
            deviceId: WS_VIDEO_STATE.deviceId,
            deviceType: WS_VIDEO_STATE.deviceType,
            userAgent: browserInfo.userAgent,
            screenResolution: `${screen.width}x${screen.height}`,
            platform: browserInfo.platform,
            timestamp: Date.now()
        });
    }

    disconnect() {
        WS_VIDEO_LOGGER.info('WebSocket 연결 종료');

        this.isReconnecting = false;
        this.authenticated = false;
        this.stopHeartbeat();
        this.clearAuthTimeout();

        if (this.reconnectTimeout) {
            clearTimeout(this.reconnectTimeout);
            this.reconnectTimeout = null;
        }

        if (this.websocket) {
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
}

// 전역 WebSocket 클라이언트 인스턴스
window.wsVideoClient = new WSVideoWebSocketClient();

// 전역 함수들 (기존과 동일)
window.connectWebSocket = async function(sessionKey) {
    try {
        await wsVideoClient.connect(sessionKey);
        WS_VIDEO_STATE.websocket = wsVideoClient;
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

window.refreshWebSocketToken = function(newToken) {
    if (wsVideoClient) {
        wsVideoClient.refreshToken(newToken);
    }
};

window.handleNetworkOnline = function() {
    WS_VIDEO_LOGGER.info('네트워크 온라인 상태');
    updateConnectionStatus('connecting');

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
    if (WS_VIDEO_STATE.sessionKey && !WS_VIDEO_STATE.isConnected) {
        setTimeout(() => {
            wsVideoClient.attemptReconnection();
        }, 500);
    }
};

window.handleAppHidden = function() {
    WS_VIDEO_LOGGER.info('앱 비활성화됨');
    if (WS_VIDEO_STATE.isRecording) {
        stopRecording();
    }
};

WS_VIDEO_LOGGER.info('완전 수정된 WebSocket 클라이언트 초기화 완료 - 중복 방지 및 누락 메시지 추가');