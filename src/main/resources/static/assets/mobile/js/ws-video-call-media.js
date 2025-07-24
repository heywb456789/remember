/**
 * WebSocket 기반 영상통화 시스템 - 완전 수정된 미디어 관리
 * 🔧 중복 상태 변경 요청 완전 차단
 */
import { authFetch } from './commonFetch.js';

// ========== 권한 관리 (기존과 동일) ==========
class SimplePermissionManager {
    constructor() {
        this.permissionRequestInProgress = false;
    }

    // 기존 권한 확인
    async checkExistingPermissions() {
        WS_VIDEO_LOGGER.info('권한 확인 시작');

        try {
            const storedStatus = this.getStoredPermissionStatus();
            if (storedStatus === true) {
                const quickTest = await this.quickMediaTest();
                if (quickTest) {
                    WS_VIDEO_LOGGER.info('저장된 권한으로 빠른 검증 성공');
                    return true;
                } else {
                    this.clearStoredPermission();
                }
            }

            if (navigator.permissions) {
                const apiResult = await this.checkPermissionsAPI();
                if (apiResult !== null) return apiResult;
            }

            return await this.testMediaAccess();

        } catch (error) {
            WS_VIDEO_LOGGER.error('권한 확인 중 오류', error);
            return false;
        }
    }

    async quickMediaTest() {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({
                video: { width: 320, height: 240 },
                audio: true
            });

            stream.getTracks().forEach(track => track.stop());

            WS_VIDEO_STATE.cameraPermissionGranted = true;
            WS_VIDEO_STATE.microphonePermissionGranted = true;
            return true;
        } catch (error) {
            return false;
        }
    }

    async checkPermissionsAPI() {
        try {
            const [cameraPermission, micPermission] = await Promise.all([
                navigator.permissions.query({ name: 'camera' }),
                navigator.permissions.query({ name: 'microphone' })
            ]);

            const hasPermissions = cameraPermission.state === 'granted' &&
                                 micPermission.state === 'granted';

            if (hasPermissions) {
                this.updatePermissionStorage(true);
                WS_VIDEO_STATE.cameraPermissionGranted = true;
                WS_VIDEO_STATE.microphonePermissionGranted = true;
                return true;
            } else if (cameraPermission.state === 'denied' || micPermission.state === 'denied') {
                this.updatePermissionStorage(false);
                return false;
            }

            return null;

        } catch (error) {
            return null;
        }
    }

    async testMediaAccess() {
        try {
            const constraints = this.getMediaConstraints();
            const stream = await navigator.mediaDevices.getUserMedia(constraints);

            stream.getTracks().forEach(track => track.stop());

            this.updatePermissionStorage(true);
            WS_VIDEO_STATE.cameraPermissionGranted = true;
            WS_VIDEO_STATE.microphonePermissionGranted = true;
            return true;

        } catch (error) {
            if (error.name === 'NotAllowedError') {
                this.updatePermissionStorage(false);
                WS_VIDEO_STATE.cameraPermissionGranted = false;
                WS_VIDEO_STATE.microphonePermissionGranted = false;
                return false;
            }
            return false;
        }
    }

    getMediaConstraints() {
        const deviceType = WS_VIDEO_STATE.deviceType;
        const constraints = WS_VIDEO_CONFIG.VIDEO_CONSTRAINTS[deviceType] ||
                          WS_VIDEO_CONFIG.VIDEO_CONSTRAINTS.WEB;

        return {
            video: { ...constraints.video, facingMode: 'user' },
            audio: constraints.audio
        };
    }

    async requestPermissions() {
        if (this.permissionRequestInProgress) {
            WS_VIDEO_LOGGER.warn('권한 요청이 이미 진행 중');
            return false;
        }

        this.permissionRequestInProgress = true;

        try {
            WS_VIDEO_LOGGER.info('미디어 권한 요청 시작');
            updateStatus('권한 확인 중...');

            const constraints = this.getMediaConstraints();
            const browserInfo = getBrowserInfo();

            if (browserInfo?.isIOSSafari) {
                await this.requestPermissionsIOS(constraints);
            } else {
                await this.requestPermissionsStandard(constraints);
            }

            this.updatePermissionStorage(true);
            WS_VIDEO_STATE.cameraPermissionGranted = true;
            WS_VIDEO_STATE.microphonePermissionGranted = true;

            WS_VIDEO_LOGGER.info('권한 요청 성공');
            updateStatus('권한 설정 완료');
            return true;

        } catch (error) {
            WS_VIDEO_LOGGER.error('권한 요청 실패', error);
            this.updatePermissionStorage(false);
            this.handlePermissionError(error);
            return false;
        } finally {
            this.permissionRequestInProgress = false;
        }
    }

    async requestPermissionsIOS(constraints) {
        const videoStream = await navigator.mediaDevices.getUserMedia({
            video: constraints.video
        });

        const audioStream = await navigator.mediaDevices.getUserMedia({
            audio: constraints.audio
        });

        const tracks = [...videoStream.getTracks(), ...audioStream.getTracks()];
        WS_VIDEO_STATE.userMediaStream = new MediaStream(tracks);

        videoStream.getTracks().forEach(track => track.stop());
        audioStream.getTracks().forEach(track => track.stop());
    }

    async requestPermissionsStandard(constraints) {
        WS_VIDEO_STATE.userMediaStream = await navigator.mediaDevices.getUserMedia(constraints);
    }

    handlePermissionError(error) {
        let errorMessage = '미디어 권한 요청 중 오류가 발생했습니다';

        switch (error.name) {
            case 'NotAllowedError':
                errorMessage = '카메라 및 마이크 권한이 필요합니다.\n브라우저 설정에서 권한을 허용해주세요.';
                break;
            case 'NotFoundError':
                errorMessage = '카메라 또는 마이크를 찾을 수 없습니다.';
                break;
            case 'NotReadableError':
                errorMessage = '카메라 또는 마이크가 다른 앱에서 사용 중입니다.';
                break;
            case 'OverconstrainedError':
                errorMessage = '요청한 미디어 설정을 지원하지 않습니다.';
                break;
        }

        showErrorMessage(errorMessage);
        updateStatus('권한 오류');
    }

    updatePermissionStorage(hasPermission) {
        try {
            WS_VIDEO_UTILS.storage.set('mediaPermissionStatus', {
                camera: hasPermission,
                microphone: hasPermission,
                userAgent: navigator.userAgent.substring(0, 100)
            }, 24 * 60 * 60 * 1000);
        } catch (error) {
            WS_VIDEO_LOGGER.warn('권한 상태 저장 실패', error);
        }
    }

    getStoredPermissionStatus() {
        try {
            const data = WS_VIDEO_UTILS.storage.get('mediaPermissionStatus');
            if (!data) return null;

            if (!data.userAgent || !navigator.userAgent.includes(data.userAgent.substring(0, 50))) {
                WS_VIDEO_UTILS.storage.remove('mediaPermissionStatus');
                return null;
            }

            return data.camera && data.microphone;
        } catch (error) {
            return null;
        }
    }

    clearStoredPermission() {
        WS_VIDEO_UTILS.storage.remove('mediaPermissionStatus');
    }
}

// ========== 완전 수정된 녹화 관리 ==========
class SimpleRecordingManager {
    constructor() {
        this.isRecording = false;
        this.startTime = null;
        this.recordingTimeout = null;
        this.recordingStateRequested = false; // 🔧 중복 방지 플래그
        this.lastStateRequestTime = 0; // 🔧 시간 기반 중복 방지
    }

    // 🔧 녹화 상태 요청 가능 여부 확인
    canRequestRecordingState() {
        const now = Date.now();

        // 1. 이미 플래그가 설정되어 있으면 불가
        if (this.recordingStateRequested) {
            WS_VIDEO_LOGGER.warn('🚫 녹화 상태 요청 거부: 이미 요청됨 (플래그)');
            return false;
        }

        // 2. 3초 내 중복 요청 방지
        if (now - this.lastStateRequestTime < 3000) {
            WS_VIDEO_LOGGER.warn('🚫 녹화 상태 요청 거부: 너무 빠른 재요청 ({}ms 전)',
                                now - this.lastStateRequestTime);
            return false;
        }

        // 3. 이미 녹화 중이면 불가
        if (this.isRecording || WS_VIDEO_STATE.isRecording) {
            WS_VIDEO_LOGGER.warn('🚫 녹화 상태 요청 거부: 이미 녹화 중');
            return false;
        }

        return true;
    }

    // 🔧 녹화 상태 요청 완료 처리
    markStateRequestCompleted() {
        this.recordingStateRequested = false;
        WS_VIDEO_LOGGER.debug('✅ 녹화 상태 요청 플래그 해제');
    }

    // 녹화 준비
    async prepareRecording() {
        if (!WS_VIDEO_STATE.cameraPermissionGranted) {
            throw new Error('녹화를 위한 카메라 권한이 필요합니다');
        }

        if (WS_VIDEO_STATE.userMediaStream) {
            const videoTracks = WS_VIDEO_STATE.userMediaStream.getVideoTracks();
            const audioTracks = WS_VIDEO_STATE.userMediaStream.getAudioTracks();

            const hasActiveVideo = videoTracks.some(track => track.readyState === 'live');
            const hasActiveAudio = audioTracks.some(track => track.readyState === 'live');

            if (hasActiveVideo && hasActiveAudio) {
                WS_VIDEO_LOGGER.info('기존 스트림으로 녹화 준비 완료');
                return true;
            } else {
                WS_VIDEO_STATE.userMediaStream.getTracks().forEach(track => track.stop());
                WS_VIDEO_STATE.userMediaStream = null;
            }
        }

        try {
            const constraints = wsVideoPermissionManager.getMediaConstraints();
            WS_VIDEO_STATE.userMediaStream = await navigator.mediaDevices.getUserMedia(constraints);

            const myCamera = document.getElementById('myCamera');
            if (myCamera) {
                myCamera.srcObject = WS_VIDEO_STATE.userMediaStream;
            }

            WS_VIDEO_LOGGER.info('녹화 준비 완료 - 새 스트림 생성됨');
            return true;

        } catch (error) {
            WS_VIDEO_LOGGER.error('녹화용 스트림 생성 실패', error);
            throw new Error(`녹화용 미디어 스트림 생성 실패: ${error.message}`);
        }
    }

    // 🔧 완전 수정된 녹화 시작
    async startRecording(maxDuration = 10) {
        try {
            if (this.recordingStateRequested) {
                WS_VIDEO_LOGGER.warn('⚠️ 중복 startRecording 호출 무시');
                return;
            }
            // 🔧 녹화 상태 요청 가능 여부 확인
            if (!this.canRequestRecordingState()) {
                return; // 조용히 무시
            }

            // 🔧 플래그 설정 및 시간 기록
            this.recordingStateRequested = true;
            this.lastStateRequestTime = Date.now();

            WS_VIDEO_LOGGER.info('🔴 녹화 시작 - 상태 변경 요청 전송');

            // ✅ WebSocket으로 서버에 녹화 상태 변경 요청 (한 번만)
            if (wsVideoClient && wsVideoClient.websocket && wsVideoClient.websocket.readyState === WebSocket.OPEN) {
                const sent = wsVideoClient.sendMessage({
                    type: 'CLIENT_STATE_CHANGE',
                    newState: 'RECORDING',
                    reason: 'USER_REQUEST',
                    timestamp: Date.now()
                });

                if (!sent) {
                    WS_VIDEO_LOGGER.error('❌ 상태 변경 메시지 전송 실패');
                    this.markStateRequestCompleted();
                    throw new Error('상태 변경 메시지 전송 실패');
                }
            } else {
                WS_VIDEO_LOGGER.error('❌ WebSocket 연결되지 않음');
                this.markStateRequestCompleted();
                throw new Error('WebSocket 연결되지 않음');
            }

            // 🔧 실제 녹화 시작은 서버 응답 후에 처리
            // (handleServerRecordingCommand에서 처리)

        } catch (error) {
            WS_VIDEO_LOGGER.error('녹화 시작 오류', error);
            this.markStateRequestCompleted();
            this.resetRecordingState();
            throw error;
        }
    }

    // 🆕 서버 명령에 의한 실제 녹화 시작
    async handleServerRecordingCommand(maxDuration = 10) {
        try {
            WS_VIDEO_LOGGER.info('🔴 서버 명령으로 실제 녹화 시작');

            WS_VIDEO_STATE.recordedChunks = [];

            const options = this.getRecordingOptions();
            WS_VIDEO_STATE.mediaRecorder = new MediaRecorder(WS_VIDEO_STATE.userMediaStream, options);

            this.setupRecordingEventListeners();

            WS_VIDEO_STATE.mediaRecorder.start();
            this.isRecording = true;
            this.startTime = Date.now();
            WS_VIDEO_STATE.isRecording = true;

            // 🔧 상태 요청 완료 처리
            this.markStateRequestCompleted();

            updateRecordingUI(true);
            updateStatus('녹화 중...');

            // 최대 녹화 시간 설정
            this.recordingTimeout = setTimeout(() => {
                this.stopRecording();
            }, maxDuration * 1000);

            WS_VIDEO_LOGGER.info('✅ 실제 녹화 시작 완료', { maxDuration });

        } catch (error) {
            WS_VIDEO_LOGGER.error('실제 녹화 시작 오류', error);
            this.markStateRequestCompleted();
            this.resetRecordingState();
            throw error;
        }
    }

    // 녹화 옵션
    getRecordingOptions() {
        const mimeTypes = WS_VIDEO_CONFIG.RECORDING.MIME_TYPES;

        for (const mimeType of mimeTypes) {
            if (MediaRecorder.isTypeSupported(mimeType)) {
                return {
                    mimeType: mimeType,
                    videoBitsPerSecond: 2500000,
                    audioBitsPerSecond: 128000
                };
            }
        }

        return {};
    }

    // 녹화 이벤트 리스너
    setupRecordingEventListeners() {
        const mediaRecorder = WS_VIDEO_STATE.mediaRecorder;

        mediaRecorder.ondataavailable = (event) => {
            if (event.data.size > 0) {
                WS_VIDEO_STATE.recordedChunks.push(event.data);
            }
        };

        mediaRecorder.onstop = () => {
            WS_VIDEO_LOGGER.info('녹화 완료');
            this.processRecordedVideo();
        };

        mediaRecorder.onerror = (event) => {
            WS_VIDEO_LOGGER.error('녹화 중 오류', event.error);
            this.handleRecordingError(event.error);
        };
    }

    // 녹화 중지
    stopRecording() {
        if (!this.isRecording || !WS_VIDEO_STATE.mediaRecorder) {
            WS_VIDEO_LOGGER.warn('녹화 중이 아닙니다');
            return;
        }

        try {
            if (this.recordingTimeout) {
                clearTimeout(this.recordingTimeout);
                this.recordingTimeout = null;
            }

            WS_VIDEO_STATE.mediaRecorder.stop();
            this.isRecording = false;
            WS_VIDEO_STATE.isRecording = false;

            const duration = this.startTime ? Math.floor((Date.now() - this.startTime) / 1000) : 0;

            updateRecordingUI(false);
            updateStatus('녹화 완료');

            WS_VIDEO_LOGGER.info('녹화 중지됨', { duration });

        } catch (error) {
            WS_VIDEO_LOGGER.error('녹화 중지 오류', error);
            this.handleRecordingError(error);
        }
    }

    // 녹화 오류 처리
    handleRecordingError(error) {
        this.isRecording = false;
        WS_VIDEO_STATE.isRecording = false;
        this.markStateRequestCompleted();

        if (this.recordingTimeout) {
            clearTimeout(this.recordingTimeout);
        }

        updateRecordingUI(false);
        updateStatus('녹화 오류');

        if (wsVideoClient) {
            wsVideoClient.sendMessage({
                type: 'CLIENT_STATE_CHANGE',
                newState: 'ERROR',
                reason: 'RECORDING_ERROR',
                error: error.message || error
            });
        }

        showErrorMessage('녹화 중 오류가 발생했습니다');
    }

    // 🔧 상태 초기화 (강화됨)
    resetRecordingState() {
        this.isRecording = false;
        this.recordingStateRequested = false;
        this.lastStateRequestTime = 0;
        WS_VIDEO_STATE.isRecording = false;

        if (this.recordingTimeout) {
            clearTimeout(this.recordingTimeout);
            this.recordingTimeout = null;
        }

        WS_VIDEO_LOGGER.info('✅ 녹화 상태 완전 초기화');
    }

    // 녹화 영상 처리
    async processRecordedVideo() {
        if (!WS_VIDEO_STATE.recordedChunks || WS_VIDEO_STATE.recordedChunks.length === 0) {
            WS_VIDEO_LOGGER.error('녹화된 데이터가 없습니다');
            showErrorMessage('녹화된 데이터가 없습니다');
            return;
        }

        try {
            WS_VIDEO_LOGGER.info('녹화 영상 처리 시작');
            updateStatus('영상 처리 중...');

            const blob = new Blob(WS_VIDEO_STATE.recordedChunks, { type: 'video/webm' });
            const fileSize = WS_VIDEO_UTILS.formatFileSize(blob.size);

            WS_VIDEO_LOGGER.info('녹화 파일 생성 완료', { size: fileSize });

            await this.uploadRecordedVideo(blob);

        } catch (error) {
            WS_VIDEO_LOGGER.error('녹화 영상 처리 실패', error);

            if (wsVideoClient) {
                wsVideoClient.sendMessage({
                    type: 'CLIENT_STATE_CHANGE',
                    newState: 'ERROR',
                    reason: 'UPLOAD_ERROR',
                    error: error.message || error
                });
            }

            showErrorMessage('영상 처리 중 오류가 발생했습니다');
        }
    }

    // 영상 업로드
    async uploadRecordedVideo(blob) {
        if (!WS_VIDEO_STATE.sessionKey) {
            throw new Error('세션 키가 없습니다');
        }

        try {
            WS_VIDEO_LOGGER.info('서버로 영상 업로드 시작');
            updateStatus('업로드 중...');

            const formData = new FormData();
            const filename = `recorded_video_${WS_VIDEO_STATE.sessionKey}_${Date.now()}.webm`;
            formData.append('video', blob, filename);
            formData.append('contactKey', 'rohmoohyun');

            const response = await authFetch(
                `${WS_VIDEO_CONFIG.API_BASE_URL}/process/${WS_VIDEO_STATE.sessionKey}`,
                {
                    method: 'POST',
                    body: formData
                }
            );

            WS_VIDEO_LOGGER.info('영상 업로드 완료', response);
            updateStatus('AI 처리 대기 중...');

        } catch (error) {
            WS_VIDEO_LOGGER.error('영상 업로드 실패', error);
            throw error;
        }
    }
}

// ========== 전역 인스턴스 생성 ==========
window.wsVideoPermissionManager = new SimplePermissionManager();
window.wsVideoRecordingManager = new SimpleRecordingManager();

// ========== 전역 함수들 ==========
window.checkExistingPermissions = () => wsVideoPermissionManager.checkExistingPermissions();
window.requestPermissions = () => wsVideoPermissionManager.requestPermissions();

// 🔧 완전 수정된 녹화 시작 함수
window.startRecordingDirectly = async function() {
    try {
        await wsVideoRecordingManager.prepareRecording();
        await wsVideoRecordingManager.startRecording(10); // 상태 변경 요청만
        WS_VIDEO_LOGGER.info('✅ 녹화 상태 변경 요청 완료');
    } catch (error) {
        WS_VIDEO_LOGGER.error('녹화 시작 실패', error);
        wsVideoRecordingManager.resetRecordingState();
        throw error;
    }
};

// 🆕 서버 명령으로 실제 녹화 시작
window.startActualRecording = async function(maxDuration = 10) {
    try {
        await wsVideoRecordingManager.handleServerRecordingCommand(maxDuration);
        WS_VIDEO_LOGGER.info('✅ 실제 녹화 시작 완료');
    } catch (error) {
        WS_VIDEO_LOGGER.error('실제 녹화 시작 실패', error);
        throw error;
    }
};

window.stopRecording = () => wsVideoRecordingManager.stopRecording();

// 카메라 설정
window.setupCamera = async function() {
    const myCamera = document.getElementById('myCamera');
    const cameraPlaceholder = document.getElementById('cameraPlaceholder');

    try {
        if (WS_VIDEO_STATE.userMediaStream) {
            myCamera.srcObject = WS_VIDEO_STATE.userMediaStream;
            myCamera.style.display = 'block';
            cameraPlaceholder.style.display = 'none';
            WS_VIDEO_LOGGER.info('기존 스트림으로 카메라 설정 완료');
            return;
        }

        if (WS_VIDEO_STATE.cameraPermissionGranted) {
            const constraints = wsVideoPermissionManager.getMediaConstraints();
            WS_VIDEO_STATE.userMediaStream = await navigator.mediaDevices.getUserMedia(constraints);

            myCamera.srcObject = WS_VIDEO_STATE.userMediaStream;
            myCamera.style.display = 'block';
            cameraPlaceholder.style.display = 'none';

            WS_VIDEO_LOGGER.info('새 스트림으로 카메라 설정 완료');
        }

    } catch (error) {
        WS_VIDEO_LOGGER.error('카메라 설정 실패', error);
        myCamera.style.display = 'none';
        cameraPlaceholder.style.display = 'block';
    }
};

window.resetRecordingState = () => wsVideoRecordingManager.resetRecordingState();

WS_VIDEO_LOGGER.info('완전 수정된 미디어 관리자 초기화 완료');