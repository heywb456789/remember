/**
 * WebSocket 기반 영상통화 시스템 - 미디어 관리 (권한, 녹화, 영상 재생)
 */

// ========== 권한 관리 ==========
class WSVideoPermissionManager {
    constructor() {
        this.permissionRequestInProgress = false;
    }

    // 기존 권한 확인
    async checkExistingPermissions() {
        WS_VIDEO_LOGGER.info('기존 권한 확인 시작');

        try {
            // 저장된 권한 상태 확인
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

            // Permissions API 확인 (지원하는 브라우저)
            if (navigator.permissions) {
                const apiResult = await this.checkPermissionsAPI();
                if (apiResult !== null) {
                    return apiResult;
                }
            }

            // 직접 미디어 접근 테스트
            return await this.testMediaAccess();

        } catch (error) {
            WS_VIDEO_LOGGER.error('권한 확인 중 오류', error);
            return false;
        }
    }

    // 빠른 미디어 테스트
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
            WS_VIDEO_LOGGER.debug('빠른 테스트 실패', error.name);
            return false;
        }
    }

    // Permissions API 확인
    async checkPermissionsAPI() {
        try {
            const [cameraPermission, micPermission] = await Promise.all([
                navigator.permissions.query({ name: 'camera' }),
                navigator.permissions.query({ name: 'microphone' })
            ]);

            WS_VIDEO_LOGGER.debug('Permissions API 결과', {
                camera: cameraPermission.state,
                microphone: micPermission.state
            });

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

            return null; // prompt 상태 - 직접 테스트 필요

        } catch (error) {
            WS_VIDEO_LOGGER.debug('Permissions API 미지원', error);
            return null;
        }
    }

    // 미디어 접근 테스트
    async testMediaAccess() {
        try {
            WS_VIDEO_LOGGER.info('미디어 접근 테스트 시작');

            const constraints = this.getMediaConstraints();
            const stream = await navigator.mediaDevices.getUserMedia(constraints);

            WS_VIDEO_LOGGER.info('미디어 접근 성공 - 권한 있음');

            // 스트림 정리
            stream.getTracks().forEach(track => {
                track.stop();
                WS_VIDEO_LOGGER.debug('트랙 정리', track.kind);
            });

            this.updatePermissionStorage(true);
            WS_VIDEO_STATE.cameraPermissionGranted = true;
            WS_VIDEO_STATE.microphonePermissionGranted = true;

            return true;

        } catch (error) {
            WS_VIDEO_LOGGER.info('미디어 접근 실패', error.name);

            if (error.name === 'NotAllowedError') {
                this.updatePermissionStorage(false);
                WS_VIDEO_STATE.cameraPermissionGranted = false;
                WS_VIDEO_STATE.microphonePermissionGranted = false;
                return false;
            } else if (error.name === 'NotFoundError') {
                WS_VIDEO_LOGGER.warn('미디어 장치 없음');
                return false;
            } else {
                // 기타 오류 - 권한 요청 필요
                return false;
            }
        }
    }

    // 미디어 제약조건 가져오기
    getMediaConstraints() {
        const deviceType = WS_VIDEO_STATE.deviceType;
        const constraints = WS_VIDEO_CONFIG.VIDEO_CONSTRAINTS[deviceType] ||
                          WS_VIDEO_CONFIG.VIDEO_CONSTRAINTS.WEB;

        return {
            video: {
                ...constraints.video,
                facingMode: 'user'
            },
            audio: constraints.audio
        };
    }

    // 권한 요청
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

            // iOS Safari는 단계별 권한 요청
            if (browserInfo.isIOSSafari) {
                await this.requestPermissionsIOS(constraints);
            } else {
                await this.requestPermissionsStandard(constraints);
            }

            this.updatePermissionStorage(true);
            WS_VIDEO_STATE.cameraPermissionGranted = true;
            WS_VIDEO_STATE.microphonePermissionGranted = true;

            // WebSocket으로 권한 상태 전송
            if (wsVideoClient) {
                wsVideoClient.notifyPermissionStatus(true, true);
            }

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

    // iOS Safari 권한 요청
    async requestPermissionsIOS(constraints) {
        WS_VIDEO_LOGGER.info('iOS Safari 단계별 권한 요청');

        // 1단계: 비디오 권한
        const videoStream = await navigator.mediaDevices.getUserMedia({
            video: constraints.video
        });
        WS_VIDEO_LOGGER.info('카메라 권한 획득');

        // 2단계: 오디오 권한
        const audioStream = await navigator.mediaDevices.getUserMedia({
            audio: constraints.audio
        });
        WS_VIDEO_LOGGER.info('마이크 권한 획득');

        // 통합 스트림 생성
        const tracks = [...videoStream.getTracks(), ...audioStream.getTracks()];
        WS_VIDEO_STATE.userMediaStream = new MediaStream(tracks);

        WS_VIDEO_LOGGER.info('iOS Safari 권한 요청 완료', {
            tracks: WS_VIDEO_STATE.userMediaStream.getTracks().map(t => t.kind)
        });
    }

    // 표준 권한 요청
    async requestPermissionsStandard(constraints) {
        WS_VIDEO_LOGGER.info('표준 권한 요청');

        WS_VIDEO_STATE.userMediaStream = await navigator.mediaDevices.getUserMedia(constraints);

        WS_VIDEO_LOGGER.info('표준 권한 요청 완료', {
            tracks: WS_VIDEO_STATE.userMediaStream.getTracks().map(t => t.kind)
        });
    }

    // 권한 오류 처리
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

        showPermissionErrorDialog(errorMessage);
        updateStatus('권한 오류');
    }

    // 권한 상태 저장/조회
    updatePermissionStorage(hasPermission) {
        try {
            WS_VIDEO_UTILS.storage.set('mediaPermissionStatus', {
                camera: hasPermission,
                microphone: hasPermission,
                userAgent: navigator.userAgent.substring(0, 100)
            }, 24 * 60 * 60 * 1000); // 24시간

            WS_VIDEO_LOGGER.debug('권한 상태 저장', hasPermission);
        } catch (error) {
            WS_VIDEO_LOGGER.warn('권한 상태 저장 실패', error);
        }
    }

    getStoredPermissionStatus() {
        try {
            const data = WS_VIDEO_UTILS.storage.get('mediaPermissionStatus');
            if (!data) return null;

            // User-Agent 검증 (브라우저 변경 감지)
            if (!data.userAgent || !navigator.userAgent.includes(data.userAgent.substring(0, 50))) {
                WS_VIDEO_UTILS.storage.remove('mediaPermissionStatus');
                return null;
            }

            return data.camera && data.microphone;
        } catch (error) {
            WS_VIDEO_LOGGER.warn('저장된 권한 상태 확인 실패', error);
            return null;
        }
    }

    clearStoredPermission() {
        WS_VIDEO_UTILS.storage.remove('mediaPermissionStatus');
    }
}

// ========== 녹화 관리 ==========
class WSVideoRecordingManager {
    constructor() {
        this.isRecording = false;
        this.startTime = null;
        this.countdownInterval = null;
        this.recordingTimeout = null;
    }

    // 녹화 준비 확인
    async prepareRecording() {
        if (!WS_VIDEO_STATE.cameraPermissionGranted || !WS_VIDEO_STATE.userMediaStream) {
            throw new Error('녹화를 위한 미디어 스트림이 필요합니다');
        }

        WS_VIDEO_LOGGER.info('녹화 준비 완료');
        return true;
    }

    // 녹화 시작 (카운트다운 포함)
    async startRecordingWithCountdown(countdownSeconds = 3, maxDuration = 10) {
        try {
            await this.prepareRecording();

            WS_VIDEO_LOGGER.info('녹화 카운트다운 시작', { countdownSeconds, maxDuration });

            // 카운트다운 UI 표시
            showCountdownModal();

            await this.runCountdown(countdownSeconds);

            // 실제 녹화 시작
            await this.startRecording(maxDuration);

        } catch (error) {
            WS_VIDEO_LOGGER.error('녹화 시작 실패', error);
            hideCountdownModal();

            if (wsVideoClient) {
                wsVideoClient.notifyRecordingError(error);
            }

            throw error;
        }
    }

    // 카운트다운 실행
    async runCountdown(seconds) {
        return new Promise((resolve) => {
            let remaining = seconds;

            const updateCountdown = () => {
                updateCountdownNumber(remaining);

                if (remaining <= 0) {
                    hideCountdownModal();
                    resolve();
                    return;
                }

                remaining--;
                this.countdownInterval = setTimeout(updateCountdown, 1000);
            };

            updateCountdown();
        });
    }

    // 실제 녹화 시작
    async startRecording(maxDuration) {
        try {
            WS_VIDEO_STATE.recordedChunks = [];

            // MediaRecorder 설정
            const options = this.getRecordingOptions();
            WS_VIDEO_STATE.mediaRecorder = new MediaRecorder(WS_VIDEO_STATE.userMediaStream, options);

            // 이벤트 리스너 설정
            this.setupRecordingEventListeners();

            // 녹화 시작
            WS_VIDEO_STATE.mediaRecorder.start();
            this.isRecording = true;
            this.startTime = Date.now();
            WS_VIDEO_STATE.isRecording = true;

            // UI 업데이트
            updateRecordingUI(true);
            updateStatus('녹화 중...');

            // 최대 녹화 시간 설정
            this.recordingTimeout = setTimeout(() => {
                this.stopRecording();
            }, maxDuration * 1000);

            // WebSocket 알림
            if (wsVideoClient) {
                wsVideoClient.notifyRecordingStarted();
            }

            WS_VIDEO_LOGGER.info('녹화 시작됨', { maxDuration });

        } catch (error) {
            WS_VIDEO_LOGGER.error('녹화 시작 오류', error);
            this.isRecording = false;
            WS_VIDEO_STATE.isRecording = false;
            throw error;
        }
    }

    // 녹화 옵션 설정
    getRecordingOptions() {
        const mimeTypes = WS_VIDEO_CONFIG.RECORDING.MIME_TYPES;

        for (const mimeType of mimeTypes) {
            if (MediaRecorder.isTypeSupported(mimeType)) {
                WS_VIDEO_LOGGER.debug('선택된 MIME 타입', mimeType);
                return {
                    mimeType: mimeType,
                    videoBitsPerSecond: 2500000, // 2.5 Mbps
                    audioBitsPerSecond: 128000   // 128 Kbps
                };
            }
        }

        WS_VIDEO_LOGGER.warn('지원되는 MIME 타입 없음 - 기본값 사용');
        return {};
    }

    // 녹화 이벤트 리스너 설정
    setupRecordingEventListeners() {
        const mediaRecorder = WS_VIDEO_STATE.mediaRecorder;

        mediaRecorder.ondataavailable = (event) => {
            if (event.data.size > 0) {
                WS_VIDEO_STATE.recordedChunks.push(event.data);
                WS_VIDEO_LOGGER.debug('녹화 데이터 청크 수신', event.data.size);
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
            // 타이머 정리
            if (this.recordingTimeout) {
                clearTimeout(this.recordingTimeout);
                this.recordingTimeout = null;
            }

            if (this.countdownInterval) {
                clearTimeout(this.countdownInterval);
                this.countdownInterval = null;
            }

            // 녹화 중지
            WS_VIDEO_STATE.mediaRecorder.stop();
            this.isRecording = false;
            WS_VIDEO_STATE.isRecording = false;

            // 녹화 시간 계산
            const duration = this.startTime ? Math.floor((Date.now() - this.startTime) / 1000) : 0;

            // UI 업데이트
            updateRecordingUI(false);
            updateStatus('녹화 완료');

            // WebSocket 알림
            if (wsVideoClient) {
                wsVideoClient.notifyRecordingStopped(duration);
            }

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

        // 타이머 정리
        if (this.recordingTimeout) {
            clearTimeout(this.recordingTimeout);
        }
        if (this.countdownInterval) {
            clearTimeout(this.countdownInterval);
        }

        // UI 업데이트
        updateRecordingUI(false);
        hideCountdownModal();
        updateStatus('녹화 오류');

        // WebSocket 알림
        if (wsVideoClient) {
            wsVideoClient.notifyRecordingError(error);
        }

        showErrorMessage('녹화 중 오류가 발생했습니다');
    }

    // 녹화된 영상 처리
    async processRecordedVideo() {
        if (!WS_VIDEO_STATE.recordedChunks || WS_VIDEO_STATE.recordedChunks.length === 0) {
            WS_VIDEO_LOGGER.error('녹화된 데이터가 없습니다');
            showErrorMessage('녹화된 데이터가 없습니다');
            return;
        }

        try {
            WS_VIDEO_LOGGER.info('녹화 영상 처리 시작');
            updateStatus('영상 처리 중...');

            // Blob 생성
            const blob = new Blob(WS_VIDEO_STATE.recordedChunks, { type: 'video/webm' });
            const fileSize = WS_VIDEO_UTILS.formatFileSize(blob.size);

            WS_VIDEO_LOGGER.info('녹화 파일 생성 완료', { size: fileSize });

            // 서버로 업로드
            await this.uploadRecordedVideo(blob);

        } catch (error) {
            WS_VIDEO_LOGGER.error('녹화 영상 처리 실패', error);

            if (wsVideoClient) {
                wsVideoClient.notifyVideoUploadError(error);
            }

            showErrorMessage('영상 처리 중 오류가 발생했습니다');
        }
    }

    // 녹화 영상 업로드
    async uploadRecordedVideo(blob) {
        if (!WS_VIDEO_STATE.sessionKey) {
            throw new Error('세션 키가 없습니다');
        }

        try {
            WS_VIDEO_LOGGER.info('서버로 영상 업로드 시작');
            updateStatus('업로드 중...');

            // FormData 생성
            const formData = new FormData();
            const filename = `recorded_video_${WS_VIDEO_STATE.sessionKey}_${Date.now()}.webm`;
            formData.append('video', blob, filename);

            // 추가 정보
            formData.append('contactKey', WS_VIDEO_STATE.memberId || 'default');

            // 업로드 요청
            const response = await authFetch(
                `${WS_VIDEO_CONFIG.API_BASE_URL}/process/${WS_VIDEO_STATE.sessionKey}`,
                {
                    method: 'POST',
                    body: formData
                }
            );

            WS_VIDEO_LOGGER.info('영상 업로드 완료', response);

            // WebSocket 알림
            if (wsVideoClient) {
                wsVideoClient.notifyVideoUploadComplete(
                    response.response?.filePath || 'uploaded'
                );
            }

            updateStatus('AI 처리 대기 중...');

        } catch (error) {
            WS_VIDEO_LOGGER.error('영상 업로드 실패', error);
            throw error;
        }
    }
}

// ========== 영상 재생 관리 ==========
class WSVideoPlayerManager {
    constructor() {
        this.currentVideoState = 'WAITING';
        this.transitionInProgress = false;
        this.browserInfo = getBrowserInfo();
    }

    // 대기영상 재생
    async playWaitingVideo(videoUrl, loop = true) {
        if (this.transitionInProgress) {
            WS_VIDEO_LOGGER.warn('영상 전환 중 - 대기영상 재생 지연');
            await WS_VIDEO_UTILS.delay(500);
        }

        try {
            this.transitionInProgress = true;
            WS_VIDEO_LOGGER.info('대기영상 재생 시작', videoUrl);

            WS_VIDEO_STATE.waitingVideoUrl = videoUrl;
            this.currentVideoState = 'WAITING';

            await this.playVideoSafely(videoUrl, loop, true);

            // WebSocket 알림
            if (wsVideoClient) {
                wsVideoClient.notifyWaitingVideoStarted();
            }

            WS_VIDEO_LOGGER.info('대기영상 재생 성공');

        } catch (error) {
            WS_VIDEO_LOGGER.error('대기영상 재생 실패', error);

            if (wsVideoClient) {
                wsVideoClient.notifyWaitingVideoError(error);
            }

            this.showVideoFallback();

        } finally {
            this.transitionInProgress = false;
        }
    }

    // 응답영상 재생
    async playResponseVideo(videoUrl, autoReturn = true) {
        if (this.transitionInProgress) {
            WS_VIDEO_LOGGER.warn('영상 전환 중 - 응답영상 재생 지연');
            await WS_VIDEO_UTILS.delay(500);
        }

        try {
            this.transitionInProgress = true;
            WS_VIDEO_LOGGER.info('응답영상 재생 시작', videoUrl);

            WS_VIDEO_STATE.responseVideoUrl = videoUrl;
            this.currentVideoState = 'RESPONSE';

            // 대기영상에서 응답영상으로 전환
            await this.fadeOutVideo();
            await this.playVideoSafely(videoUrl, false, true);
            await this.fadeInVideo();

            // WebSocket 알림
            if (wsVideoClient) {
                wsVideoClient.notifyResponseVideoStarted();
            }

            // 응답영상 종료 시 대기영상으로 복귀 설정
            if (autoReturn) {
                this.setupVideoEndedHandler();
            }

            WS_VIDEO_LOGGER.info('응답영상 재생 성공');

        } catch (error) {
            WS_VIDEO_LOGGER.error('응답영상 재생 실패', error);

            if (wsVideoClient) {
                wsVideoClient.notifyResponseVideoError(error);
            }

            // 대기영상으로 복귀
            await this.returnToWaitingVideo();

        } finally {
            this.transitionInProgress = false;
        }
    }

    // 안전한 영상 재생
    async playVideoSafely(videoUrl, loop = false, unmuted = false) {
        const mainVideo = document.getElementById('mainVideo');

        return new Promise((resolve, reject) => {
            // 영상 로딩 오버레이 표시
            showVideoLoadingOverlay();

            // 영상 설정
            mainVideo.src = videoUrl;
            mainVideo.loop = loop;
            mainVideo.muted = !unmuted;
            mainVideo.playsInline = true;

            if (unmuted) {
                mainVideo.volume = 0.8;
            }

            const cleanup = () => {
                mainVideo.removeEventListener('loadedmetadata', onLoaded);
                mainVideo.removeEventListener('error', onError);
                if (timeout) clearTimeout(timeout);
                hideVideoLoadingOverlay();
            };

            const onLoaded = async () => {
                try {
                    cleanup();
                    await mainVideo.play();
                    mainVideo.style.display = 'block';
                    resolve();
                } catch (playError) {
                    if (playError.name === 'NotAllowedError') {
                        this.showTouchToPlayGuide(resolve, reject);
                    } else {
                        reject(playError);
                    }
                }
            };

            const onError = (event) => {
                cleanup();
                reject(new Error('비디오 로딩 실패'));
            };

            mainVideo.addEventListener('loadedmetadata', onLoaded);
            mainVideo.addEventListener('error', onError);

            // 타임아웃 설정
            const timeout = setTimeout(() => {
                cleanup();
                reject(new Error('비디오 로딩 타임아웃'));
            }, WS_VIDEO_CONFIG.TIMERS.VIDEO_LOAD_TIMEOUT);

            mainVideo.load();
        });
    }

    // 터치하여 재생 가이드 표시
    showTouchToPlayGuide(resolve, reject) {
        const guide = document.createElement('div');
        guide.id = 'touchGuide';
        guide.className = 'touch-guide';
        guide.innerHTML = `
            <div class="touch-guide-icon">🎬</div>
            <div class="touch-guide-title">화면을 터치해주세요</div>
            <div class="touch-guide-message">영상을 시작하려면 터치가 필요합니다</div>
        `;

        guide.onclick = async () => {
            try {
                const mainVideo = document.getElementById('mainVideo');
                await mainVideo.play();
                guide.remove();
                mainVideo.style.display = 'block';
                WS_VIDEO_LOGGER.info('사용자 터치로 재생 시작');
                resolve();
            } catch (error) {
                guide.remove();
                reject(error);
            }
        };

        document.querySelector('.main-video-container').appendChild(guide);
    }

    // 영상 페이드 효과
    async fadeOutVideo() {
        if (this.browserInfo.isIOSSafari) return; // iOS Safari는 스킵

        const mainVideo = document.getElementById('mainVideo');
        mainVideo.style.transition = 'opacity 0.3s ease';
        mainVideo.style.opacity = '0';

        await WS_VIDEO_UTILS.delay(300);
    }

    async fadeInVideo() {
        if (this.browserInfo.isIOSSafari) return; // iOS Safari는 스킵

        const mainVideo = document.getElementById('mainVideo');
        mainVideo.style.transition = 'opacity 0.3s ease';
        mainVideo.style.opacity = '1';

        await WS_VIDEO_UTILS.delay(300);
        mainVideo.style.transition = '';
    }

    // 응답영상 종료 시 대기영상 복귀 설정
    setupVideoEndedHandler() {
        const mainVideo = document.getElementById('mainVideo');

        const onVideoEnded = async () => {
            WS_VIDEO_LOGGER.info('응답영상 재생 완료 - 대기영상으로 복귀');
            mainVideo.removeEventListener('ended', onVideoEnded);

            // WebSocket 알림
            if (wsVideoClient) {
                wsVideoClient.notifyResponseVideoEnded(Math.floor(mainVideo.currentTime));
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

            this.currentVideoState = 'WAITING';

            await this.fadeOutVideo();
            await this.playVideoSafely(WS_VIDEO_STATE.waitingVideoUrl, true, true);
            await this.fadeInVideo();

            updateStatus('대기 중');
            WS_VIDEO_LOGGER.info('대기영상 복귀 완료');

        } catch (error) {
            WS_VIDEO_LOGGER.error('대기영상 복귀 실패', error);
            this.showVideoFallback();
        }
    }

    // 영상 재생 실패 시 폴백 화면
    showVideoFallback() {
        WS_VIDEO_LOGGER.info('영상 폴백 화면 표시');

        const mainVideo = document.getElementById('mainVideo');
        mainVideo.style.display = 'none';

        const container = document.querySelector('.main-video-container');
        container.style.background = 'linear-gradient(135deg, #2c3e50, #34495e)';

        let fallback = document.getElementById('videoFallback');
        if (!fallback) {
            fallback = document.createElement('div');
            fallback.id = 'videoFallback';
            fallback.className = 'video-fallback';
            fallback.innerHTML = `
                <div class="video-fallback-icon">📹</div>
                <div class="video-fallback-title">연결을 준비하고 있습니다</div>
                <div class="video-fallback-message">잠시만 기다려주세요</div>
            `;
            container.appendChild(fallback);
        }
    }
}

// ========== 전역 인스턴스 생성 ==========
window.wsVideoPermissionManager = new WSVideoPermissionManager();
window.wsVideoRecordingManager = new WSVideoRecordingManager();
window.wsVideoPlayerManager = new WSVideoPlayerManager();

// ========== 전역 함수들 ==========
window.checkExistingPermissions = () => wsVideoPermissionManager.checkExistingPermissions();
window.requestPermissions = () => wsVideoPermissionManager.requestPermissions();

window.startRecordingCountdown = (countdown = 3, maxDuration = 10) =>
    wsVideoRecordingManager.startRecordingWithCountdown(countdown, maxDuration);
window.stopRecording = () => wsVideoRecordingManager.stopRecording();

window.playWaitingVideo = (url, loop = true) => wsVideoPlayerManager.playWaitingVideo(url, loop);
window.playResponseVideo = (url, autoReturn = true) => wsVideoPlayerManager.playResponseVideo(url, autoReturn);

// 카메라 설정
window.setupCamera = function() {
    if (WS_VIDEO_STATE.userMediaStream) {
        const myCamera = document.getElementById('myCamera');
        const cameraPlaceholder = document.getElementById('cameraPlaceholder');

        myCamera.srcObject = WS_VIDEO_STATE.userMediaStream;
        myCamera.style.display = 'block';
        cameraPlaceholder.style.display = 'none';

        WS_VIDEO_LOGGER.info('카메라 설정 완료');
    }
};

WS_VIDEO_LOGGER.info('미디어 관리자 초기화 완료');