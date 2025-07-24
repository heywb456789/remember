/**
 * WebSocket 기반 영상통화 시스템 - 간소화된 UI 관리
 * 핵심 기능만 남기고 불필요한 기능 제거
 */

// ========== DOM 요소 캐시 ==========
let domCache = {};

function cacheDOMElements() {
    domCache = {
        // 모달들
        permissionModal: document.getElementById('permissionModal'),
        callStartModal: document.getElementById('callStartModal'),
        loadingModal: document.getElementById('loadingModal'),

        // 상태 표시
        statusText: document.getElementById('statusText'),
        connectionStatus: document.getElementById('connectionStatus'),
        connectionIcon: document.getElementById('connectionIcon'),
        connectionText: document.getElementById('connectionText'),

        // 비디오
        mainVideo: document.getElementById('mainVideo'),
        myCamera: document.getElementById('myCamera'),
        cameraPlaceholder: document.getElementById('cameraPlaceholder'),
        videoLoadingOverlay: document.getElementById('videoLoadingOverlay'),

        // 컨트롤
        recordBtn: document.getElementById('recordBtn'),
        recordIcon: document.getElementById('recordIcon')
    };

    WS_VIDEO_LOGGER.info('DOM 요소 캐시 완료');
}

// ========== 간소화된 UI 매니저 ==========
class SimpleWSVideoUIManager {
    constructor() {
        this.openModals = [];
        this.lastRecordingToggle = 0; // 녹화 버튼 중복 클릭 방지
        this.recordingToggleThrottle = 2000;
    }

    // === 모달 관리 (3개만) ===
    showModal(modalElement) {
        if (!modalElement) return;
        modalElement.classList.add('show');
        this.openModals.push(modalElement.id);
    }

    hideModal(modalElement) {
        if (!modalElement) return;
        modalElement.classList.remove('show');
        const index = this.openModals.indexOf(modalElement.id);
        if (index > -1) this.openModals.splice(index, 1);
    }

    hideAllModals() {
        [domCache.permissionModal, domCache.callStartModal, domCache.loadingModal]
            .forEach(modal => modal && this.hideModal(modal));
        this.openModals = [];
    }

    // === 권한 모달 ===
    showPermissionModal() {
        this.showModal(domCache.permissionModal);
        this.updateStatus('권한 요청 중...');
    }

    hidePermissionModal() {
        this.hideModal(domCache.permissionModal);
    }

    // === 통화 시작 모달 ===
    showCallStartModal() {
        const contactName = WS_VIDEO_STATE.contactName || '연결 준비 중...';
        const display = document.getElementById('contactNameDisplay');
        if (display) display.textContent = contactName;

        this.showModal(domCache.callStartModal);
    }

    hideCallStartModal() {
        this.hideModal(domCache.callStartModal);
    }

    // === 로딩 모달 ===
    showLoadingModal(title = '연결 준비 중...', message = '잠시만 기다려주세요') {
        const titleEl = document.getElementById('loadingTitle');
        const messageEl = document.getElementById('loadingMessage');

        if (titleEl) titleEl.textContent = title;
        if (messageEl) messageEl.textContent = message;

        this.showModal(domCache.loadingModal);
    }

    hideLoadingModal() {
        this.hideModal(domCache.loadingModal);
    }

    // === 상태 업데이트 (간소화) ===
    updateStatus(newStatus, statusType = 'info') {
        if (!domCache.statusText) return;

        const statusConfig = {
            error: { color: '#e74c3c', icon: '❌' },
            recording: { color: '#e74c3c', icon: '🔴' },
            success: { color: '#27ae60', icon: '✅' },
            loading: { color: '#f39c12', icon: '⏳' },
            info: { color: '#3498db', icon: '' }
        };

        const config = statusConfig[statusType] || statusConfig.info;

        domCache.statusText.textContent = config.icon ? `${config.icon} ${newStatus}` : newStatus;
        domCache.statusText.style.color = config.color;

        // 녹화 상태일 때만 깜빡임
        if (statusType === 'recording') {
            domCache.statusText.classList.add('recording-blink');
        } else {
            domCache.statusText.classList.remove('recording-blink');
        }

        WS_VIDEO_LOGGER.debug('상태 업데이트:', newStatus, statusType);
    }

    // === 연결 상태 업데이트 ===
    updateConnectionStatus(status) {
        if (!domCache.connectionStatus) return;

        const statusConfig = {
            connecting: { icon: 'fa-wifi', text: '연결 중...', color: '#f39c12' },
            connected: { icon: 'fa-wifi', text: '연결됨', color: '#27ae60' },
            disconnected: { icon: 'fa-wifi-slash', text: '연결 끊김', color: '#e74c3c' },
            error: { icon: 'fa-exclamation-triangle', text: '연결 오류', color: '#e74c3c' }
        };

        const config = statusConfig[status] || statusConfig.disconnected;

        if (domCache.connectionIcon) {
            domCache.connectionIcon.className = `fas ${config.icon}`;
            domCache.connectionIcon.style.color = config.color;
        }

        if (domCache.connectionText) {
            domCache.connectionText.textContent = config.text;
            domCache.connectionText.style.color = config.color;
        }

        // 문제가 있는 상태만 표시
        if (status === 'connecting' || status === 'disconnected' || status === 'error') {
            domCache.connectionStatus.classList.add('show');
        } else {
            setTimeout(() => domCache.connectionStatus.classList.remove('show'), 2000);
        }
    }

    // === 녹화 UI 업데이트 ===
    updateRecordingUI = function(isRecording) {
    if (!domCache.recordBtn || !domCache.recordIcon) return;

    if (isRecording) {
        domCache.recordBtn.classList.add('recording', 'user-stop-enabled');
        domCache.recordBtn.disabled = false; // 🔧 중요: 녹화 중에도 버튼 활성화 (중지용)
        domCache.recordIcon.className = 'fas fa-stop';
        document.body.classList.add('recording-active');

        domCache.recordBtn.title = '녹화 중지하기 (클릭하여 중지)';

        // 🆕 중지 가능 시각적 표시 추가
        domCache.recordBtn.style.background = 'linear-gradient(45deg, #e74c3c, #c0392b)';
        domCache.recordBtn.style.animation = 'recordingPulse 2s ease-in-out infinite';

    } else {
        domCache.recordBtn.classList.remove('recording', 'user-stop-enabled');
        domCache.recordIcon.className = 'fas fa-microphone';
        document.body.classList.remove('recording-active');

        // 스타일 초기화
        domCache.recordBtn.style.background = '';
        domCache.recordBtn.style.animation = '';

        // 녹화 가능 여부에 따라 버튼 활성화
        setTimeout(() => {
            const canRecord = WS_VIDEO_STATE_UTILS?.canRecord() || false;
            domCache.recordBtn.disabled = !canRecord;
            domCache.recordBtn.classList.toggle('disabled', !canRecord);
            domCache.recordBtn.title = canRecord ? '녹화하기' : '녹화할 수 없습니다';
        }, 500); // 0.5초 후 버튼 상태 확인
    }
};

    // === 영상 로딩 오버레이 ===
    showVideoLoadingOverlay() {
        if (domCache.videoLoadingOverlay) {
            domCache.videoLoadingOverlay.classList.add('show');
        }
    }

    hideVideoLoadingOverlay() {
        if (domCache.videoLoadingOverlay) {
            domCache.videoLoadingOverlay.classList.remove('show');
        }
    }

    // === 영상 전환 로직 (OS별 최적화) ===
    async fadeOutVideo() {
        if (!domCache.mainVideo) return;

        const browserInfo = getBrowserInfo();
        // iOS Safari는 페이드 효과 스킵 (성능상 이유)
        if (browserInfo?.isIOSSafari) return;

        domCache.mainVideo.style.transition = 'opacity 0.3s ease';
        domCache.mainVideo.style.opacity = '0';
        await new Promise(resolve => setTimeout(resolve, 300));
    }

    async fadeInVideo() {
        if (!domCache.mainVideo) return;

        const browserInfo = getBrowserInfo();
        if (browserInfo?.isIOSSafari) return;

        domCache.mainVideo.style.transition = 'opacity 0.3s ease';
        domCache.mainVideo.style.opacity = '1';
        await new Promise(resolve => setTimeout(resolve, 300));
        domCache.mainVideo.style.transition = '';
    }

    // OS별 안전한 영상 교체
    async switchVideoSafely(newUrl, loop = false, unmuted = false) {
        if (!domCache.mainVideo || !newUrl) return false;

        try {
            this.showVideoLoadingOverlay();

            return new Promise((resolve, reject) => {
                const cleanup = () => {
                    domCache.mainVideo.removeEventListener('loadedmetadata', onLoaded);
                    domCache.mainVideo.removeEventListener('error', onError);
                    if (timeout) clearTimeout(timeout);
                    this.hideVideoLoadingOverlay();
                };

                const onLoaded = async () => {
                    try {
                        cleanup();
                        await domCache.mainVideo.play();
                        domCache.mainVideo.style.display = 'block';
                        WS_VIDEO_LOGGER.info('영상 교체 성공:', newUrl);
                        resolve(true);
                    } catch (playError) {
                        if (playError.name === 'NotAllowedError') {
                            this.showTouchToPlayGuide(resolve, reject);
                        } else {
                            WS_VIDEO_LOGGER.error('영상 재생 실패:', playError);
                            reject(playError);
                        }
                    }
                };

                const onError = () => {
                    cleanup();
                    WS_VIDEO_LOGGER.error('영상 로딩 실패:', newUrl);
                    reject(new Error('영상 로딩 실패'));
                };

                // 영상 설정
                domCache.mainVideo.src = newUrl;
                domCache.mainVideo.loop = loop;
                domCache.mainVideo.muted = !unmuted;
                domCache.mainVideo.playsInline = true;

                if (unmuted) {
                    domCache.mainVideo.volume = 0.8;
                }

                domCache.mainVideo.addEventListener('loadedmetadata', onLoaded);
                domCache.mainVideo.addEventListener('error', onError);

                // 타임아웃 설정
                const timeout = setTimeout(() => {
                    cleanup();
                    reject(new Error('영상 로딩 타임아웃'));
                }, WS_VIDEO_CONFIG?.TIMERS?.VIDEO_LOAD_TIMEOUT || 15000);

                // OS별 로드 방식
                const browserInfo = getBrowserInfo();
                if (browserInfo?.isIOSSafari || browserInfo?.isAndroid) {
                    // iOS/Android는 명시적 load() 호출 필요
                    domCache.mainVideo.load();
                    WS_VIDEO_LOGGER.debug('iOS/Android - 명시적 load() 호출');
                } else {
                    // 일반 브라우저는 src 설정만으로도 로딩 시작
                    WS_VIDEO_LOGGER.debug('데스크톱 브라우저 - 자동 로딩');
                }
            });

        } catch (error) {
            this.hideVideoLoadingOverlay();
            WS_VIDEO_LOGGER.error('영상 교체 실패:', error);
            return false;
        }
    }

    // 터치하여 재생 가이드 (iOS용)
    showTouchToPlayGuide(resolve, reject) {
        const guide = document.createElement('div');
        guide.id = 'touchGuide';
        guide.style.cssText = `
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: rgba(0, 0, 0, 0.85);
            color: white;
            padding: 32px;
            border-radius: 16px;
            text-align: center;
            z-index: 2500;
            cursor: pointer;
            backdrop-filter: blur(10px);
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
        `;

        guide.innerHTML = `
            <div style="font-size: 64px; margin-bottom: 20px;">🎬</div>
            <div style="font-size: 20px; font-weight: 600; margin-bottom: 12px;">화면을 터치해주세요</div>
            <div style="font-size: 14px; opacity: 0.8;">영상을 시작하려면 터치가 필요합니다</div>
        `;

        guide.onclick = async () => {
            try {
                await domCache.mainVideo.play();
                guide.remove();
                domCache.mainVideo.style.display = 'block';
                WS_VIDEO_LOGGER.info('사용자 터치로 영상 재생 시작');
                resolve(true);
            } catch (error) {
                guide.remove();
                WS_VIDEO_LOGGER.error('터치 가이드 재생 실패:', error);
                reject(error);
            }
        };

        document.querySelector('.main-video-container').appendChild(guide);

        // 10초 후 자동 제거
        setTimeout(() => {
            if (guide.parentNode) {
                guide.remove();
                reject(new Error('사용자 터치 타임아웃'));
            }
        }, 10000);
    }

    // 부드러운 영상 전환 (대기영상 ↔ 응답영상)
    async transitionVideo(newUrl, loop = false, unmuted = false) {
        try {
            WS_VIDEO_LOGGER.info('부드러운 영상 전환 시작:', newUrl);

            await this.fadeOutVideo();
            const success = await this.switchVideoSafely(newUrl, loop, unmuted);

            if (success) {
                await this.fadeInVideo();
                WS_VIDEO_LOGGER.info('영상 전환 완료');

                // 응답영상이고 자동 복귀가 필요한 경우
                if (!loop && unmuted) {
                    this.setupAutoReturnToWaiting();
                }

                return true;
            } else {
                WS_VIDEO_LOGGER.error('영상 전환 실패 - 대기영상으로 복귀');
                await this.returnToWaitingVideo();
                return false;
            }

        } catch (error) {
            WS_VIDEO_LOGGER.error('영상 전환 중 오류:', error);
            await this.returnToWaitingVideo();
            return false;
        }
    }

    // 응답영상 종료 후 대기영상 자동 복귀
    setupAutoReturnToWaiting() {
        const mainVideo = document.getElementById('mainVideo');
        if (!mainVideo) return;

        const onVideoEnded = async () => {
            WS_VIDEO_LOGGER.info('응답영상 종료 - 대기영상으로 복귀');
            mainVideo.removeEventListener('ended', onVideoEnded);

            // WebSocket 알림
            if (wsVideoClient) {
                wsVideoClient.notifyResponseVideoEvent('ended', {
                    duration: Math.floor(mainVideo.currentTime)
                });
            }

            await this.returnToWaitingVideo();
        };

        mainVideo.addEventListener('ended', onVideoEnded);
    }

    // 대기영상으로 복귀
    async returnToWaitingVideo() {
        if (!WS_VIDEO_STATE.waitingVideoUrl) {
            WS_VIDEO_LOGGER.warn('대기영상 URL이 없어 복귀 불가');
            return;
        }

        try {
            WS_VIDEO_LOGGER.info('대기영상으로 복귀 시작');
            updateStatus('대기영상으로 복귀 중...');

            await this.fadeOutVideo();
            const success = await this.switchVideoSafely(WS_VIDEO_STATE.waitingVideoUrl, true, true);

            if (success) {
                await this.fadeInVideo();
                updateStatus('대기 중');
                WS_VIDEO_LOGGER.info('대기영상 복귀 완료');
            } else {
                WS_VIDEO_LOGGER.error('대기영상 복귀 실패');
                updateStatus('영상 오류');
            }

        } catch (error) {
            WS_VIDEO_LOGGER.error('대기영상 복귀 실패', error);
            updateStatus('영상 오류');
        }
    }

    // === 간단한 토스트 메시지 ===
    showMessage(message, type = 'info', duration = 3000) {
        const toast = document.createElement('div');
        toast.className = `message-toast message-${type}`;
        toast.textContent = message;

        const colors = {
            info: '#3498db',
            success: '#27ae60',
            error: '#e74c3c',
            warning: '#f39c12'
        };

        toast.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: ${colors[type] || colors.info};
            color: white;
            padding: 12px 20px;
            border-radius: 8px;
            font-size: 14px;
            text-align: center;
            z-index: 3000;
            max-width: 80%;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
            animation: fadeInOut ${duration}ms ease-in-out;
        `;

        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), duration);
    }

    // === 초기화 ===
    initialize() {
        cacheDOMElements();
        this.addBasicStyles();
        WS_VIDEO_LOGGER.info('간소화된 UI 매니저 초기화 완료');
    }

    // === 기본 애니메이션 스타일 추가 ===
    addBasicStyles = function() {
    if (document.getElementById('ws-video-basic-styles')) return;

    const style = document.createElement('style');
        style.id = 'ws-video-basic-styles';
        style.textContent = `
            .recording-blink {
                animation: recordingBlink 1s ease-in-out infinite;
            }
            
            @keyframes recordingBlink {
                0%, 100% { opacity: 1; }
                50% { opacity: 0.3; }
            }
            
            .recording-active::before {
                content: '';
                position: fixed;
                top: 0; left: 0; right: 0; bottom: 0;
                border: 3px solid #e74c3c;
                pointer-events: none;
                z-index: 1000;
                animation: recordingBorder 2s ease-in-out infinite;
            }
            
            @keyframes recordingBorder {
                0%, 100% { opacity: 0.8; }
                50% { opacity: 0.3; }
            }
            
            @keyframes fadeInOut {
                0% { opacity: 0; transform: translate(-50%, -50%) scale(0.8); }
                20% { opacity: 1; transform: translate(-50%, -50%) scale(1); }
                80% { opacity: 1; transform: translate(-50%, -50%) scale(1); }
                100% { opacity: 0; transform: translate(-50%, -50%) scale(0.8); }
            }
    
            /* 🆕 녹화 버튼 애니메이션 */
            @keyframes recordingPulse {
                0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(231, 76, 60, 0.7); }
                50% { transform: scale(1.05); box-shadow: 0 0 0 10px rgba(231, 76, 60, 0); }
                100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(231, 76, 60, 0); }
            }
    
            /* 🆕 사용자 중지 가능 버튼 스타일 */
            .control-btn.user-stop-enabled {
                cursor: pointer !important;
                transition: all 0.3s ease;
            }
    
            .control-btn.user-stop-enabled:hover {
                transform: scale(1.1);
                box-shadow: 0 4px 15px rgba(231, 76, 60, 0.4);
            }
    
            .control-btn.user-stop-enabled::after {
                content: "중지";
                position: absolute;
                bottom: -25px;
                left: 50%;
                transform: translateX(-50%);
                font-size: 10px;
                color: #e74c3c;
                font-weight: bold;
            }
        `;

        document.head.appendChild(style);
    };
}

// ========== 전역 UI 매니저 인스턴스 ==========
window.wsVideoUIManager = new SimpleWSVideoUIManager();

// ========== 전역 함수들 (간소화) ==========
window.showPermissionModal = () => wsVideoUIManager.showPermissionModal();
window.hidePermissionModal = () => wsVideoUIManager.hidePermissionModal();
window.showCallStartModal = () => wsVideoUIManager.showCallStartModal();
window.hideCallStartModal = () => wsVideoUIManager.hideCallStartModal();
window.showLoadingModal = (title, message) => wsVideoUIManager.showLoadingModal(title, message);
window.hideLoadingModal = () => wsVideoUIManager.hideLoadingModal();

window.updateStatus = (status, type) => wsVideoUIManager.updateStatus(status, type);
window.updateConnectionStatus = (status) => wsVideoUIManager.updateConnectionStatus(status);
window.updateRecordingUI = (isRecording) => wsVideoUIManager.updateRecordingUI(isRecording);

window.showVideoLoadingOverlay = () => wsVideoUIManager.showVideoLoadingOverlay();
window.hideVideoLoadingOverlay = () => wsVideoUIManager.hideVideoLoadingOverlay();

// 영상 전환 관련 전역 함수들 추가
window.playWaitingVideo = (url, loop = true) => wsVideoUIManager.transitionVideo(url, loop, true);
window.playResponseVideo = (url, autoReturn = true) => wsVideoUIManager.transitionVideo(url, false, true);
window.switchVideoSafely = (url, loop, unmuted) => wsVideoUIManager.switchVideoSafely(url, loop, unmuted);

window.showSuccessMessage = (message) => wsVideoUIManager.showMessage(message, 'success');
window.showErrorMessage = (message) => wsVideoUIManager.showMessage(message, 'error');
window.showWarningMessage = (message) => wsVideoUIManager.showMessage(message, 'warning');
window.showInfoMessage = (message) => wsVideoUIManager.showMessage(message, 'info');

// ========== 버튼 이벤트 핸들러 (간소화) ==========
window.toggleRecording = async function() {
    try {
        // 중복 클릭 방지
        const now = Date.now();
        if (now - wsVideoUIManager.lastRecordingToggle < wsVideoUIManager.recordingToggleThrottle) {
            WS_VIDEO_LOGGER.warn('녹화 버튼 중복 클릭 방지 - throttle 적용');
            showWarningMessage('잠시 후 다시 시도해주세요');
            return;
        }
        wsVideoUIManager.lastRecordingToggle = now;

        // 🔧 현재 녹화 중인지 확인하여 시작/중지 결정
        if (WS_VIDEO_STATE.isRecording || wsVideoRecordingManager?.isRecording) {
            // 녹화 중이면 중지
            WS_VIDEO_LOGGER.info('🛑 사용자 요청으로 녹화 중지');
            await stopRecordingByUser();
            return;
        }

        // 현재 상태 확인
        const currentState = WS_VIDEO_STATE_UTILS?.getCurrentState();
        if (!currentState?.allowRecording) {
            showInfoMessage(`현재 상태에서는 녹화할 수 없습니다: ${currentState?.display || '알 수 없음'}`);
            return;
        }

        // 권한 확인
        if (!WS_VIDEO_STATE.cameraPermissionGranted) {
            showInfoMessage('녹화하려면 카메라 권한이 필요합니다');
            showPermissionModal();
            return;
        }

        // 녹화 버튼 즉시 비활성화
        if (domCache.recordBtn) {
            domCache.recordBtn.disabled = true;
            domCache.recordBtn.classList.add('disabled');
        }

        WS_VIDEO_LOGGER.info('🔴 녹화 시작 요청');
        await startRecordingDirectly();

    } catch (error) {
        WS_VIDEO_LOGGER.error('녹화 토글 오류:', error);
        showErrorMessage('녹화 중 오류가 발생했습니다');

        // 오류 시 버튼 상태 복원
        if (domCache.recordBtn) {
            domCache.recordBtn.disabled = false;
            domCache.recordBtn.classList.remove('disabled');
        }

        // 녹화 상태 초기화
        if (wsVideoRecordingManager) {
            wsVideoRecordingManager.resetRecordingState();
        }
    }
};

window.stopRecordingByUser = async function() {
    try {
        WS_VIDEO_LOGGER.info('🛑 사용자가 녹화 중지 요청');

        // 1. 클라이언트 녹화 즉시 중지
        if (wsVideoRecordingManager && wsVideoRecordingManager.isRecording) {
            wsVideoRecordingManager.forceStopRecording('USER_STOP');
        }

        // 2. 서버에 중지 알림
        if (wsVideoClient && wsVideoClient.websocket && wsVideoClient.websocket.readyState === WebSocket.OPEN) {
            wsVideoClient.sendMessage({
                type: 'CLIENT_STATE_CHANGE',
                newState: 'PROCESSING',
                reason: 'USER_STOP_RECORDING',
                timestamp: Date.now()
            });
        }

        // 3. UI 즉시 업데이트
        updateRecordingUI(false);
        updateStatus('사용자가 녹화를 중지했습니다');
        showInfoMessage('녹화가 중지되었습니다');

        WS_VIDEO_LOGGER.info('✅ 사용자 녹화 중지 완료');

    } catch (error) {
        WS_VIDEO_LOGGER.error('사용자 녹화 중지 중 오류:', error);
        showErrorMessage('녹화 중지 중 오류가 발생했습니다');
    }
};


window.requestPermissions = async function() {
    hidePermissionModal();

    try {
        const granted = await wsVideoPermissionManager?.requestPermissions();
        if (granted) {
            setupCamera();
            showCallStartModal();
        }
    } catch (error) {
        WS_VIDEO_LOGGER.error('권한 요청 실패', error);
    }
};

window.denyPermission = function() {
    hidePermissionModal();
    updateStatus('권한 없이 체험');
    showInfoMessage('체험 모드로 실행됩니다');
    initializeWithoutPermission();
};

window.cancelCall = function() {
    hideCallStartModal();
    const memberId = WS_VIDEO_STATE.memberId || '1';
    const memorialId = WS_VIDEO_STATE.memorialId || '1';
    window.location.href = `/memorial/${memorialId}?memberId=${memberId}`;
};

window.startCall = async function() {
    hideCallStartModal();

    try {
        if (typeof startVideoCallSession === 'function') {
            await startVideoCallSession();
        }
    } catch (error) {
        WS_VIDEO_LOGGER.error('통화 시작 실패', error);
        showErrorMessage('통화 시작에 실패했습니다');
    }
};

window.endCall = function() {
    if (confirm('영상통화를 종료하시겠습니까?')) {
        if (WS_VIDEO_STATE.isRecording) {
            stopRecording();
        }

        if (typeof cleanup === 'function') {
            cleanup();
        }

        const memberId = WS_VIDEO_STATE.memberId || '1';
        const memorialId = WS_VIDEO_STATE.memorialId || '1';
        window.location.href = `/call/feedback?memberId=${memberId}&memorialId=${memorialId}`;
    }
};

window.goBack = function() {

    // 녹화 중이면 먼저 중지
    if (WS_VIDEO_STATE.isRecording) {
        if (confirm('녹화가 진행 중입니다. 정말 나가시겠습니까?')) {
            stopRecording();
        } else {
            return;
        }
    }

    const memberId = WS_VIDEO_STATE.memberId || '1';
    const memorialId = WS_VIDEO_STATE.memorialId || '1';
    window.location.href = `/memorial/${memorialId}?memberId=${memberId}`;
};

// ========== 권한 없이 초기화 ==========
window.initializeWithoutPermission = async function() {
    updateStatus('체험 모드');

    try {
        await playWaitingVideo(WS_VIDEO_CONFIG?.DEFAULT_WAITING_VIDEO || 'default.mp4', true);
        setTimeout(() => showCallStartModal(), 2000);
    } catch (error) {
        WS_VIDEO_LOGGER.error('체험 모드 초기화 실패', error);
    }
};

// ========== 키보드 단축키 (간소화) ==========
document.addEventListener('keydown', function(event) {
    if (wsVideoUIManager.openModals.length > 0) return;

    switch (event.code) {
        case 'Space':
            event.preventDefault();
            toggleRecording();
            break;
        case 'Escape':
            event.preventDefault();
            if (WS_VIDEO_STATE.isRecording) {
                stopRecordingByUser(); // 녹화 중이면 중지
            } else {
                endCall(); // 아니면 통화 종료
            }
            break;
    }
});

// ========== 터치 이벤트 최적화 ==========
if ('ontouchstart' in window) {
    let lastTouchEnd = 0;
    document.addEventListener('touchend', function(event) {
        const now = Date.now();
        if (now - lastTouchEnd <= 300) {
            event.preventDefault();
        }
        lastTouchEnd = now;
    }, false);

    document.addEventListener('touchmove', function(event) {
        event.preventDefault();
    }, { passive: false });
}

WS_VIDEO_LOGGER.info('간소화된 UI 관리자 로드 완료');