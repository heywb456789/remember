/**
 * WebSocket 기반 영상통화 시스템 - UI 관리
 */

// ========== DOM 요소 참조 ==========
let domElements = {};

function initializeDOMElements() {
    domElements = {
        // 모달들
        permissionModal: document.getElementById('permissionModal'),
        callStartModal: document.getElementById('callStartModal'),
        loadingModal: document.getElementById('loadingModal'),
        countdownModal: document.getElementById('countdownModal'),
        progressModal: document.getElementById('progressModal'),

        // 모달 내용
        permissionIcon: document.getElementById('permissionIcon'),
        permissionTitle: document.getElementById('permissionTitle'),
        permissionMessage: document.getElementById('permissionMessage'),
        contactNameDisplay: document.getElementById('contactNameDisplay'),
        loadingTitle: document.getElementById('loadingTitle'),
        loadingMessage: document.getElementById('loadingMessage'),
        countdownNumber: document.getElementById('countdownNumber'),
        progressFill: document.getElementById('progressFill'),
        progressText: document.getElementById('progressText'),
        progressPercent: document.getElementById('progressPercent'),

        // 상태 표시
        statusIndicator: document.getElementById('statusIndicator'),
        statusText: document.getElementById('statusText'),
        sessionInfo: document.getElementById('sessionInfo'),
        sessionId: document.getElementById('sessionId'),
        connectionStatus: document.getElementById('connectionStatus'),
        connectionIcon: document.getElementById('connectionIcon'),
        connectionText: document.getElementById('connectionText'),

        // 비디오 및 카메라
        mainVideo: document.getElementById('mainVideo'),
        myCamera: document.getElementById('myCamera'),
        cameraPlaceholder: document.getElementById('cameraPlaceholder'),
        myCameraContainer: document.getElementById('myCameraContainer'),
        videoLoadingOverlay: document.getElementById('videoLoadingOverlay'),

        // 컨트롤
        recordBtn: document.getElementById('recordBtn'),
        recordIcon: document.getElementById('recordIcon'),
        controlBar: document.getElementById('controlBar'),

        // 디버그 (개발용)
        debugInfo: document.getElementById('debugInfo'),
        debugState: document.getElementById('debugState'),
        debugWS: document.getElementById('debugWS'),
        debugDevice: document.getElementById('debugDevice'),
        debugTTL: document.getElementById('debugTTL')
    };

    WS_VIDEO_LOGGER.info('DOM 요소 초기화 완료');
}

// ========== 모달 관리 ==========
class WSVideoUIManager {
    constructor() {
        this.openModals = [];
        this.touchGuideActive = false;
    }

    // 모달 표시
    showModal(modalElement) {
        if (!modalElement) return;

        modalElement.classList.add('show');
        this.openModals.push(modalElement.id);

        WS_VIDEO_LOGGER.debug('모달 표시', modalElement.id);
    }

    // 모달 숨김
    hideModal(modalElement) {
        if (!modalElement) return;

        modalElement.classList.remove('show');
        const index = this.openModals.indexOf(modalElement.id);
        if (index > -1) {
            this.openModals.splice(index, 1);
        }

        WS_VIDEO_LOGGER.debug('모달 숨김', modalElement.id);
    }

    // 모든 모달 숨김
    hideAllModals() {
        const modals = [
            domElements.permissionModal,
            domElements.callStartModal,
            domElements.loadingModal,
            domElements.countdownModal,
            domElements.progressModal
        ];

        modals.forEach(modal => {
            if (modal) {
                this.hideModal(modal);
            }
        });

        this.openModals = [];
        WS_VIDEO_LOGGER.debug('모든 모달 숨김');
    }

    // 권한 모달 관리
    showPermissionModal() {
        domElements.permissionIcon.textContent = '🎥';
        domElements.permissionTitle.textContent = '미디어 권한 필요';
        domElements.permissionMessage.textContent =
            '영상통화를 위해 카메라와 마이크 권한이 필요합니다.\n최적의 경험을 위해 권한을 허용해주세요.';

        this.showModal(domElements.permissionModal);
        updateStatus('권한 요청 중...');
    }

    showPermissionErrorDialog(message) {
        domElements.permissionIcon.textContent = '❌';
        domElements.permissionTitle.textContent = '권한 오류';
        domElements.permissionMessage.textContent = message;

        // 버튼 변경
        const buttons = domElements.permissionModal.querySelector('.permission-buttons');
        buttons.innerHTML = `
            <button class="permission-btn deny" onclick="goBack()">뒤로가기</button>
            <button class="permission-btn allow" onclick="requestPermissions()">다시 시도</button>
        `;

        this.showModal(domElements.permissionModal);
    }

    hidePermissionModal() {
        this.hideModal(domElements.permissionModal);
    }

    // 통화 시작 모달 관리
    showCallStartModal() {
        const contactName = WS_VIDEO_STATE.contactName || '연결 준비 중...';
        domElements.contactNameDisplay.textContent = contactName;
        this.showModal(domElements.callStartModal);

        WS_VIDEO_LOGGER.info('통화 시작 모달 표시', contactName);
    }

    hideCallStartModal() {
        this.hideModal(domElements.callStartModal);
    }

    // 로딩 모달 관리
    showLoadingModal(title = '연결 준비 중...', message = '잠시만 기다려주세요') {
        domElements.loadingTitle.textContent = title;
        domElements.loadingMessage.textContent = message;
        this.showModal(domElements.loadingModal);
    }

    hideLoadingModal() {
        this.hideModal(domElements.loadingModal);
    }

    // 카운트다운 모달 관리
    showCountdownModal() {
        this.showModal(domElements.countdownModal);
    }

    updateCountdownNumber(number) {
        if (domElements.countdownNumber) {
            domElements.countdownNumber.textContent = number;
            domElements.countdownNumber.style.animation = 'countdownPulse 1s ease-in-out';

            // 애니메이션 리셋
            setTimeout(() => {
                domElements.countdownNumber.style.animation = '';
            }, 1000);
        }
    }

    hideCountdownModal() {
        this.hideModal(domElements.countdownModal);
    }

    // 진행률 모달 관리
    showProgressModal() {
        this.showModal(domElements.progressModal);
        this.updateProcessingProgress(0, 'AI 분석을 시작합니다...');
    }

    updateProcessingProgress(progress, message) {
        if (domElements.progressFill) {
            domElements.progressFill.style.width = `${progress}%`;
        }
        if (domElements.progressText) {
            domElements.progressText.textContent = message;
        }
        if (domElements.progressPercent) {
            domElements.progressPercent.textContent = `${progress}%`;
        }
    }

    hideProgressModal() {
        this.hideModal(domElements.progressModal);
    }

    // 상태 업데이트
    updateStatus(newStatus, statusType = 'info') {
        if (domElements.statusText) {
            domElements.statusText.style.transition = 'opacity 0.3s ease';
            domElements.statusText.style.opacity = '0';

            setTimeout(() => {
                domElements.statusText.textContent = newStatus;
                domElements.statusText.style.opacity = '1';
            }, 150);
        }

        // 상태 점 색상 변경
        const statusDot = domElements.statusIndicator?.querySelector('.status-dot');
        if (statusDot) {
            statusDot.className = `status-dot ${statusType}`;
        }

        WS_VIDEO_STATE.statusMessage = newStatus;
        WS_VIDEO_LOGGER.debug('상태 업데이트', { status: newStatus, type: statusType });
    }

    // 세션 정보 업데이트
    updateSessionInfo(sessionKey) {
        if (domElements.sessionInfo && domElements.sessionId && sessionKey) {
            const shortKey = sessionKey.substring(sessionKey.length - 8);
            domElements.sessionId.textContent = `세션: ${shortKey}`;
            domElements.sessionInfo.style.display = 'block';
        }
    }

    // 연결 상태 업데이트
    updateConnectionStatus(status) {
        if (!domElements.connectionStatus) return;

        const statusConfig = {
            connecting: { icon: 'fa-wifi', text: '연결 중...', class: 'connecting' },
            connected: { icon: 'fa-wifi', text: '연결됨', class: 'connected' },
            disconnected: { icon: 'fa-wifi-slash', text: '연결 끊김', class: 'disconnected' },
            error: { icon: 'fa-exclamation-triangle', text: '연결 오류', class: 'disconnected' }
        };

        const config = statusConfig[status] || statusConfig.disconnected;

        if (domElements.connectionIcon) {
            domElements.connectionIcon.className = `fas ${config.icon}`;
        }
        if (domElements.connectionText) {
            domElements.connectionText.textContent = config.text;
        }

        domElements.connectionStatus.className = `connection-status ${config.class}`;

        // 연결 문제가 있을 때만 표시
        if (status === 'connecting' || status === 'disconnected' || status === 'error') {
            domElements.connectionStatus.classList.add('show');
        } else {
            setTimeout(() => {
                domElements.connectionStatus.classList.remove('show');
            }, 2000);
        }
    }

    // 녹화 UI 업데이트
    updateRecordingUI(isRecording) {
        if (!domElements.recordBtn || !domElements.recordIcon) return;

        if (isRecording) {
            domElements.recordBtn.classList.add('recording');
            domElements.recordIcon.className = 'fas fa-stop';
            domElements.myCameraContainer.classList.add('recording');
        } else {
            domElements.recordBtn.classList.remove('recording');
            domElements.recordIcon.className = 'fas fa-microphone';
            domElements.myCameraContainer.classList.remove('recording');
        }

        WS_VIDEO_LOGGER.debug('녹화 UI 업데이트', { isRecording });
    }

    // 영상 로딩 오버레이
    showVideoLoadingOverlay() {
        if (domElements.videoLoadingOverlay) {
            domElements.videoLoadingOverlay.classList.add('show');
        }
    }

    hideVideoLoadingOverlay() {
        if (domElements.videoLoadingOverlay) {
            domElements.videoLoadingOverlay.classList.remove('show');
        }
    }

    // 메시지 토스트
    showMessage(message, type = 'info', duration = 3000) {
        const toast = document.createElement('div');
        toast.className = `message-toast message-${type}`;
        toast.textContent = message;

        const colors = {
            info: 'rgba(52, 152, 219, 0.9)',
            success: 'rgba(39, 174, 96, 0.9)',
            error: 'rgba(231, 76, 60, 0.9)',
            warning: 'rgba(243, 156, 18, 0.9)'
        };

        toast.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: ${colors[type] || colors.info};
            color: white;
            padding: 16px 24px;
            border-radius: 8px;
            font-size: 14px;
            text-align: center;
            z-index: 3000;
            max-width: 80%;
            animation: fadeInOut ${duration}ms ease-in-out;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
        `;

        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), duration);

        WS_VIDEO_LOGGER.debug('메시지 표시', { message, type });
    }

    // 디버그 정보 업데이트 (개발용)
    updateDebugInfo() {
        if (!WS_VIDEO_CONFIG.DEBUG.SHOW_DEBUG_INFO || !domElements.debugInfo) {
            return;
        }

        const wsStatus = WS_VIDEO_STATE.isConnected ?
            `연결됨 (재시도: ${WS_VIDEO_STATE.reconnectAttempts})` : '연결 안됨';

        if (domElements.debugState) {
            domElements.debugState.textContent = WS_VIDEO_STATE.currentState;
        }
        if (domElements.debugWS) {
            domElements.debugWS.textContent = wsStatus;
        }
        if (domElements.debugDevice) {
            domElements.debugDevice.textContent = WS_VIDEO_STATE.deviceType;
        }
        if (domElements.debugTTL) {
            const sessionAge = WS_VIDEO_STATE.sessionKey ?
                Math.floor((Date.now() - (WS_VIDEO_STATE.sessionCreatedAt || Date.now())) / 1000 / 60) : 0;
            domElements.debugTTL.textContent = `${sessionAge}분`;
        }

        domElements.debugInfo.style.display = 'block';
    }

    // 터치 가이드 (iOS Safari용)
    showTouchGuide() {
        if (this.touchGuideActive) return;

        this.touchGuideActive = true;

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
            animation: pulseGlow 2s ease-in-out infinite;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
        `;

        guide.innerHTML = `
            <div style="font-size: 64px; margin-bottom: 20px;">🎬</div>
            <div style="font-size: 20px; font-weight: 600; margin-bottom: 12px;">화면을 터치해주세요</div>
            <div style="font-size: 14px; opacity: 0.8;">영상을 시작하려면 터치가 필요합니다</div>
        `;

        guide.onclick = async () => {
            try {
                if (domElements.mainVideo) {
                    await domElements.mainVideo.play();
                    guide.remove();
                    this.touchGuideActive = false;
                    WS_VIDEO_LOGGER.info('사용자 터치로 영상 재생 시작');
                }
            } catch (error) {
                WS_VIDEO_LOGGER.error('터치 가이드 재생 실패', error);
                guide.remove();
                this.touchGuideActive = false;
                this.showMessage('영상 재생에 실패했습니다', 'error');
            }
        };

        document.querySelector('.main-video-container').appendChild(guide);

        // 10초 후 자동 제거
        setTimeout(() => {
            if (guide.parentNode) {
                guide.remove();
                this.touchGuideActive = false;
            }
        }, 10000);
    }

    // CSS 클래스 동적 추가
    addDynamicStyles() {
        if (document.getElementById('ws-video-dynamic-styles')) return;

        const style = document.createElement('style');
        style.id = 'ws-video-dynamic-styles';
        style.textContent = `
            .touch-guide {
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
                animation: pulseGlow 2s ease-in-out infinite;
                box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
            }
            
            .touch-guide-icon {
                font-size: 64px;
                margin-bottom: 20px;
            }
            
            .touch-guide-title {
                font-size: 20px;
                font-weight: 600;
                margin-bottom: 12px;
            }
            
            .touch-guide-message {
                font-size: 14px;
                opacity: 0.8;
            }
            
            .video-fallback {
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                text-align: center;
                color: white;
                z-index: 100;
            }
            
            .video-fallback-icon {
                font-size: 64px;
                margin-bottom: 20px;
                animation: float 3s ease-in-out infinite;
            }
            
            .video-fallback-title {
                font-size: 18px;
                font-weight: 600;
                margin-bottom: 8px;
            }
            
            .video-fallback-message {
                font-size: 14px;
                opacity: 0.7;
            }
            
            .message-toast {
                position: fixed;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                padding: 16px 24px;
                border-radius: 8px;
                font-size: 14px;
                text-align: center;
                z-index: 3000;
                max-width: 80%;
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
                animation: fadeInOut 3000ms ease-in-out;
            }
            
            @media (max-width: 480px) {
                .touch-guide {
                    padding: 24px;
                }
                
                .touch-guide-icon {
                    font-size: 48px;
                    margin-bottom: 16px;
                }
                
                .touch-guide-title {
                    font-size: 18px;
                }
                
                .touch-guide-message {
                    font-size: 13px;
                }
                
                .video-fallback-icon {
                    font-size: 48px;
                    margin-bottom: 16px;
                }
                
                .video-fallback-title {
                    font-size: 16px;
                }
                
                .video-fallback-message {
                    font-size: 13px;
                }
            }
        `;

        document.head.appendChild(style);
    }

    // 반응형 UI 조정
    adjustResponsiveUI() {
        const isMobile = window.innerWidth <= 480;
        const isLandscape = window.innerWidth > window.innerHeight;

        // 모바일 가로모드에서 UI 조정
        if (isMobile && isLandscape) {
            document.body.classList.add('mobile-landscape');
        } else {
            document.body.classList.remove('mobile-landscape');
        }

        WS_VIDEO_LOGGER.debug('반응형 UI 조정', { isMobile, isLandscape });
    }

    // 초기화
    initialize() {
        initializeDOMElements();
        this.addDynamicStyles();
        this.adjustResponsiveUI();

        // 리사이즈 이벤트 리스너
        window.addEventListener('resize', () => {
            this.adjustResponsiveUI();
        });

        // 오리엔테이션 변경 이벤트
        window.addEventListener('orientationchange', () => {
            setTimeout(() => {
                this.adjustResponsiveUI();
            }, 100);
        });

        WS_VIDEO_LOGGER.info('UI 매니저 초기화 완료');
    }
}

// ========== 전역 UI 매니저 인스턴스 ==========
window.wsVideoUIManager = new WSVideoUIManager();

// ========== 전역 UI 함수들 ==========
// 모달 관리
window.showPermissionModal = () => wsVideoUIManager.showPermissionModal();
window.hidePermissionModal = () => wsVideoUIManager.hidePermissionModal();
window.showPermissionErrorDialog = (message) => wsVideoUIManager.showPermissionErrorDialog(message);

window.showCallStartModal = () => wsVideoUIManager.showCallStartModal();
window.hideCallStartModal = () => wsVideoUIManager.hideCallStartModal();

window.showLoadingModal = (title, message) => wsVideoUIManager.showLoadingModal(title, message);
window.hideLoadingModal = () => wsVideoUIManager.hideLoadingModal();

window.showCountdownModal = () => wsVideoUIManager.showCountdownModal();
window.hideCountdownModal = () => wsVideoUIManager.hideCountdownModal();
window.updateCountdownNumber = (number) => wsVideoUIManager.updateCountdownNumber(number);

window.showProgressModal = () => wsVideoUIManager.showProgressModal();
window.hideProgressModal = () => wsVideoUIManager.hideProgressModal();
window.updateProcessingProgress = (progress, message) => wsVideoUIManager.updateProcessingProgress(progress, message);

// 상태 관리
window.updateStatus = (status, type) => wsVideoUIManager.updateStatus(status, type);
window.updateSessionInfo = (sessionKey) => wsVideoUIManager.updateSessionInfo(sessionKey);
window.updateConnectionStatus = (status) => wsVideoUIManager.updateConnectionStatus(status);
window.updateRecordingUI = (isRecording) => wsVideoUIManager.updateRecordingUI(isRecording);

// 영상 관리
window.showVideoLoadingOverlay = () => wsVideoUIManager.showVideoLoadingOverlay();
window.hideVideoLoadingOverlay = () => wsVideoUIManager.hideVideoLoadingOverlay();

// 메시지
window.showSuccessMessage = (message) => wsVideoUIManager.showMessage(message, 'success');
window.showErrorMessage = (message) => wsVideoUIManager.showMessage(message, 'error');
window.showWarningMessage = (message) => wsVideoUIManager.showMessage(message, 'warning');
window.showInfoMessage = (message) => wsVideoUIManager.showMessage(message, 'info');

// 디버그
window.updateDebugInfo = () => wsVideoUIManager.updateDebugInfo();

// ========== 버튼 이벤트 핸들러 ==========
window.toggleRecording = async function() {
    try {
        if (!WS_VIDEO_STATE.isRecording) {
            // 녹화 시작
            if (!WS_VIDEO_STATE.cameraPermissionGranted) {
                showInfoMessage('녹화 기능을 사용하려면 카메라 권한이 필요합니다');
                showPermissionModal();
                return;
            }

            await startRecordingCountdown();
        } else {
            // 녹화 중지
            stopRecording();
        }
    } catch (error) {
        WS_VIDEO_LOGGER.error('녹화 토글 오류', error);
        showErrorMessage('녹화 기능 오류가 발생했습니다');
    }
};

window.requestPermissions = async function() {
    hidePermissionModal();

    try {
        const granted = await wsVideoPermissionManager.requestPermissions();
        if (granted) {
            setupCamera();
            showCallStartModal();
        }
    } catch (error) {
        WS_VIDEO_LOGGER.error('권한 요청 실패', error);
    }
};

window.denyPermission = function() {
    WS_VIDEO_LOGGER.info('사용자가 권한을 거부함');
    hidePermissionModal();

    updateStatus('권한 없이 체험');
    showInfoMessage('체험 모드로 실행됩니다.\n녹화 기능은 권한이 필요합니다.');

    // 권한 없이 초기화
    initializeWithoutPermission();
};

window.cancelCall = function() {
    WS_VIDEO_LOGGER.info('사용자가 통화를 취소함');
    hideCallStartModal();

    // 뒤로가기 처리
    const memberId = WS_VIDEO_STATE.memberId || '1';
    const memorialId = WS_VIDEO_STATE.memorialId || '1';
    window.location.href = `/memorial/${memorialId}?memberId=${memberId}`;
};

window.startCall = async function() {
    WS_VIDEO_LOGGER.info('통화 시작 버튼 클릭');
    hideCallStartModal();

    try {
        // 메인 초기화 함수 호출
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
        WS_VIDEO_LOGGER.info('통화 종료 요청');

        // 정리 작업
        if (typeof cleanup === 'function') {
            cleanup();
        }

        // 피드백 페이지로 이동
        const memberId = WS_VIDEO_STATE.memberId || '1';
        const memorialId = WS_VIDEO_STATE.memorialId || '1';
        window.location.href = `/call/feedback?memberId=${memberId}&memorialId=${memorialId}`;
    }
};

window.goBack = function() {
    WS_VIDEO_LOGGER.info('뒤로가기 요청');

    const memberId = WS_VIDEO_STATE.memberId || '1';
    const memorialId = WS_VIDEO_STATE.memorialId || '1';
    window.location.href = `/memorial/${memorialId}?memberId=${memberId}`;
};

// ========== 권한 없이 초기화 ==========
window.initializeWithoutPermission = async function() {
    WS_VIDEO_LOGGER.info('권한 없이 영상통화 초기화');

    updateStatus('체험 모드');

    try {
        // 대기영상만 재생
        await playWaitingVideo(WS_VIDEO_CONFIG.DEFAULT_WAITING_VIDEO, true);

        setTimeout(() => {
            showCallStartModal();
        }, 2000);

    } catch (error) {
        WS_VIDEO_LOGGER.error('체험 모드 초기화 실패', error);
        wsVideoPlayerManager.showVideoFallback();
    }
};

// ========== 키보드 단축키 (데스크톱용) ==========
document.addEventListener('keydown', function(event) {
    // 모달이 열려있으면 단축키 비활성화
    if (wsVideoUIManager.openModals.length > 0) return;

    switch (event.code) {
        case 'Space':
            event.preventDefault();
            toggleRecording();
            break;

        case 'Escape':
            event.preventDefault();
            endCall();
            break;

        case 'KeyD':
            if (event.ctrlKey && WS_VIDEO_CONFIG.DEBUG.ENABLED) {
                event.preventDefault();
                const debugInfo = document.getElementById('debugInfo');
                if (debugInfo) {
                    debugInfo.style.display = debugInfo.style.display === 'none' ? 'block' : 'none';
                    WS_VIDEO_CONFIG.DEBUG.SHOW_DEBUG_INFO = !WS_VIDEO_CONFIG.DEBUG.SHOW_DEBUG_INFO;
                }
            }
            break;
    }
});

// ========== 터치 이벤트 최적화 (모바일용) ==========
if ('ontouchstart' in window) {
    // 더블 탭 확대 방지
    let lastTouchEnd = 0;
    document.addEventListener('touchend', function(event) {
        const now = Date.now();
        if (now - lastTouchEnd <= 300) {
            event.preventDefault();
        }
        lastTouchEnd = now;
    }, false);

    // 터치 스크롤 방지
    document.addEventListener('touchmove', function(event) {
        event.preventDefault();
    }, { passive: false });
}

// ========== 디버그 정보 주기적 업데이트 ==========
if (WS_VIDEO_CONFIG.DEBUG.ENABLED) {
    setInterval(() => {
        updateDebugInfo();
    }, 2000);
}

WS_VIDEO_LOGGER.info('UI 관리자 초기화 완료');