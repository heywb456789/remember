/**
 * 동영상 업로드/수정 페이지 JavaScript (수정 버전)
 */
import {adminAuthFetch} from '../../../js/commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    // DOM 요소
    const videoForm = document.getElementById('videoForm');
    const btnSubmit = document.getElementById('btnSubmit');

    // 파일 업로드 관련 요소
    const videoUploader = document.getElementById('videoUploader');
    const uploadPlaceholder = document.getElementById('uploadPlaceholder');
    const uploadPreview = document.getElementById('uploadPreview');
    const videoPreview = document.getElementById('videoPreview');
    const videoFile = document.getElementById('videoFile');
    const videoFileName = document.getElementById('videoFileName');
    const videoFileSize = document.getElementById('videoFileSize');
    const removeVideo = document.getElementById('removeVideo');

    // 썸네일 관련 요소
    const thumbnailUploader = document.getElementById('thumbnailUploader');
    const thumbnailPlaceholder = document.getElementById('thumbnailPlaceholder');
    const thumbnailPreview = document.getElementById('thumbnailPreview');
    const thumbnailImage = document.getElementById('thumbnailImage');
    const thumbnailFile = document.getElementById('thumbnailFile');
    const thumbnailFileName = document.getElementById('thumbnailFileName');
    const thumbnailFileSize = document.getElementById('thumbnailFileSize');
    const removeThumbnail = document.getElementById('removeThumbnail');

    // 유튜브 관련 요소
    const youtubeUrl = document.getElementById('youtubeUrl');
    const previewYoutube = document.getElementById('previewYoutube');
    const youtubePreviewContainer = document.getElementById('youtubePreviewContainer');
    const youtubePreviewFrame = document.getElementById('youtubePreviewFrame');

    // 업로드 타입 탭 관련 요소
    const fileTabs = document.getElementById('uploadTypeTabs');
    const durationSec = document.getElementById('durationSec');

    // Flatpickr 초기화 (날짜/시간 선택기)
    initDateTimePicker();

    // 수정 모드인지 확인
    const isEditing = !!document.getElementById('videoId');

    // 이벤트 리스너 등록
    initEventListeners();

    // 수정 모드에서 유튜브 탭 처리
    handleEditModeYouTubeTab();

    /**
     * 이벤트 리스너 초기화
     */
    function initEventListeners() {
        // 동영상 파일 업로드 이벤트
        if (videoUploader) {
            videoUploader.addEventListener('click', function () {
                videoFile.click();
            });

            videoUploader.addEventListener('dragover', function (e) {
                e.preventDefault();
                videoUploader.classList.add('dragover');
            });

            videoUploader.addEventListener('dragleave', function () {
                videoUploader.classList.remove('dragover');
            });

            videoUploader.addEventListener('drop', function (e) {
                e.preventDefault();
                videoUploader.classList.remove('dragover');

                if (e.dataTransfer.files.length > 0) {
                    handleVideoFileSelect(e.dataTransfer.files[0]);
                }
            });

            if (videoFile) {
                videoFile.addEventListener('change', function () {
                    if (this.files.length > 0) {
                        handleVideoFileSelect(this.files[0]);
                    }
                });
            }

            if (removeVideo) {
                removeVideo.addEventListener('click', function (e) {
                    e.stopPropagation();
                    resetVideoUploader();
                });
            }
        }

        // 썸네일 이미지 업로드 이벤트
        if (thumbnailUploader) {
            thumbnailUploader.addEventListener('click', function () {
                if (thumbnailUploader.style.pointerEvents !== 'none') {
                    thumbnailFile.click();
                }
            });

            thumbnailUploader.addEventListener('dragover', function (e) {
                e.preventDefault();
                if (thumbnailUploader.style.pointerEvents !== 'none') {
                    thumbnailUploader.classList.add('dragover');
                }
            });

            thumbnailUploader.addEventListener('dragleave', function () {
                thumbnailUploader.classList.remove('dragover');
            });

            thumbnailUploader.addEventListener('drop', function (e) {
                e.preventDefault();
                thumbnailUploader.classList.remove('dragover');

                if (thumbnailUploader.style.pointerEvents !== 'none' && e.dataTransfer.files.length > 0) {
                    handleThumbnailFileSelect(e.dataTransfer.files[0]);
                }
            });

            if (thumbnailFile) {
                thumbnailFile.addEventListener('change', function () {
                    if (this.files.length > 0) {
                        handleThumbnailFileSelect(this.files[0]);
                    }
                });
            }

            if (removeThumbnail) {
                removeThumbnail.addEventListener('click', function (e) {
                    e.stopPropagation();
                    resetThumbnailUploader();
                });
            }
        }

        // 유튜브 미리보기 버튼
        if (previewYoutube) {
            previewYoutube.addEventListener('click', previewYoutubeVideo);
        }

        // 유튜브 URL 실시간 검증
        setupRealTimeYouTubeValidation();

        // 업로드 타입 탭 전환 이벤트
        if (fileTabs) {
            const tabButtons = fileTabs.querySelectorAll('.nav-link');
            tabButtons.forEach(button => {
                button.addEventListener('click', function () {
                    // 이미 활성화된 탭이면 무시
                    if (this.classList.contains('active')) {
                        return;
                    }

                    // 다른 탭 비활성화
                    tabButtons.forEach(btn => {
                        btn.classList.remove('active');
                        btn.setAttribute('aria-selected', 'false');
                    });

                    // 현재 탭 활성화
                    this.classList.add('active');
                    this.setAttribute('aria-selected', 'true');

                    // 탭 내용 전환
                    const targetId = this.getAttribute('data-bs-target');
                    const targetContent = document.querySelector(targetId);

                    document.querySelectorAll('.tab-pane').forEach(pane => {
                        pane.classList.remove('show', 'active');
                    });

                    targetContent.classList.add('show', 'active');

                    // 썸네일 영역 처리
                    handleThumbnailArea(targetId === '#youtube-content');
                });
            });
        }

        // 제출 버튼 이벤트
        if (btnSubmit) {
            btnSubmit.addEventListener('click', validateAndSubmit);
        }
    }

    /**
     * 수정 모드에서 유튜브 탭 처리
     */
    function handleEditModeYouTubeTab() {
        if (isEditing && youtubeUrl && youtubeUrl.value.trim()) {
            // 유튜브 탭 활성화
            const youtubeTab = document.getElementById('youtube-tab');
            const fileTab = document.getElementById('file-tab');
            const youtubeContent = document.getElementById('youtube-content');
            const fileContent = document.getElementById('file-content');

            if (youtubeTab && fileTab && youtubeContent && fileContent) {
                // 파일 탭 비활성화
                fileTab.classList.remove('active');
                fileTab.setAttribute('aria-selected', 'false');
                fileContent.classList.remove('show', 'active');

                // 유튜브 탭 활성화
                youtubeTab.classList.add('active');
                youtubeTab.setAttribute('aria-selected', 'true');
                youtubeContent.classList.add('show', 'active');

                // 미리보기 버튼 활성화 및 썸네일 영역 처리
                previewYoutube.disabled = false;
                handleThumbnailArea(true);

                // 기존 유튜브 영상 미리보기
                previewYoutubeVideo();
            }
        }
    }

    /**
     * 썸네일 영역 처리 (유튜브/파일 탭에 따른)
     */
    function handleThumbnailArea(isYoutubeTab) {
        const thumbnailUploader = document.getElementById('thumbnailUploader');

        if (isYoutubeTab) {
            // 유튜브 탭일 때 썸네일 영역 비활성화
            if (thumbnailUploader) {
                thumbnailUploader.style.opacity = '0.5';
                thumbnailUploader.style.pointerEvents = 'none';

                // 안내 메시지 추가
                let youtubeNotice = document.getElementById('youtubeNotice');
                if (!youtubeNotice) {
                    youtubeNotice = document.createElement('div');
                    youtubeNotice.id = 'youtubeNotice';
                    youtubeNotice.className = 'alert alert-info mt-2';
                    youtubeNotice.innerHTML = `
                        <i class="fas fa-info-circle"></i> 
                        유튜브 영상의 썸네일은 자동으로 설정됩니다.
                    `;
                    thumbnailUploader.parentNode.insertBefore(youtubeNotice, thumbnailUploader.nextSibling);
                }
            }
        } else {
            // 파일 업로드 탭일 때 썸네일 영역 활성화
            if (thumbnailUploader) {
                thumbnailUploader.style.opacity = '';
                thumbnailUploader.style.pointerEvents = '';

                // 안내 메시지 제거
                const youtubeNotice = document.getElementById('youtubeNotice');
                if (youtubeNotice) {
                    youtubeNotice.remove();
                }
            }
        }
    }

    /**
     * YouTube URL 실시간 검증 및 미리보기
     */
    function setupRealTimeYouTubeValidation() {
        if (youtubeUrl) {
            youtubeUrl.addEventListener('input', function() {
                const value = this.value.trim();
                const videoId = extractYouTubeId(value);

                // URL 타입 힌트 업데이트
                updateUrlTypeHint(value);

                // 입력 필드 스타일 업데이트
                if (value === '') {
                    // 빈 값
                    this.classList.remove('is-valid', 'is-invalid');
                    previewYoutube.disabled = true;
                    hideYoutubePreview();
                } else if (videoId) {
                    // 유효한 URL/ID
                    this.classList.remove('is-invalid');
                    this.classList.add('is-valid');
                    previewYoutube.disabled = false;
                } else {
                    // 잘못된 URL/ID
                    this.classList.remove('is-valid');
                    this.classList.add('is-invalid');
                    previewYoutube.disabled = true;
                    hideYoutubePreview();
                }
            });

            // Enter 키로 미리보기 실행
            youtubeUrl.addEventListener('keypress', function(e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    if (!previewYoutube.disabled) {
                        previewYoutubeVideo();
                    }
                }
            });

            // 페이지 로드 시 기존 유튜브 영상 있으면 미리보기 설정
            if (youtubeUrl.value.trim()) {
                const videoId = extractYouTubeId(youtubeUrl.value.trim());
                if (videoId) {
                    youtubeUrl.classList.add('is-valid');
                    previewYoutube.disabled = false;
                    updateUrlTypeHint(youtubeUrl.value.trim());
                } else {
                    previewYoutube.disabled = true;
                }
            } else {
                previewYoutube.disabled = true;
            }
        }
    }

    /**
     * URL 타입 힌트 표시
     */
    function updateUrlTypeHint(url) {
        const hintElement = document.getElementById('urlTypeHint');

        if (!hintElement) return;

        if (!url) {
            hintElement.textContent = '';
            hintElement.className = '';
            return;
        }

        if (url.includes('youtube.com/shorts/') || url.includes('youtu.be/shorts/')) {
            hintElement.textContent = '🩳 YouTube Shorts로 감지됨';
            hintElement.className = 'url-hint shorts text-warning';
        } else if (url.includes('youtube.com') || url.includes('youtu.be')) {
            hintElement.textContent = '🎬 YouTube 동영상으로 감지됨';
            hintElement.className = 'url-hint video text-info';
        } else if (extractYouTubeId(url)) {
            hintElement.textContent = '🆔 YouTube ID로 인식됨';
            hintElement.className = 'url-hint id text-success';
        } else {
            hintElement.textContent = '❌ 올바른 YouTube URL 또는 ID를 입력하세요';
            hintElement.className = 'url-hint invalid text-danger';
        }
    }

    /**
     * 동영상 파일 선택 처리
     */
    function handleVideoFileSelect(file) {
        // 파일 유형 검증
        const validTypes = [
            'video/mp4', 'video/quicktime', 'video/x-msvideo', 'video/x-ms-wmv',
            'video/x-matroska', 'video/webm', 'video/x-flv', 'video/mpeg', 'video/x-m4v'
        ];

        const fileName = file.name.toLowerCase();
        const isValidExtension = ['.mp4', '.mov', '.avi', '.wmv', '.mkv', '.webm', '.flv', '.mpg', '.mpeg', '.m4v']
            .some(ext => fileName.endsWith(ext));

        if (!validTypes.includes(file.type) && !isValidExtension) {
            showAlert('지원하지 않는 파일 형식입니다. MP4, MOV, AVI, WMV, MKV, WEBM, FLV, MPG, M4V 파일만 허용됩니다.', 'danger');
            return;
        }

        // 파일 크기 검증 (500MB)
        const maxSize = 500 * 1024 * 1024;
        if (file.size > maxSize) {
            showAlert('파일 크기가 너무 큽니다. 최대 500MB까지 허용됩니다.', 'danger');
            return;
        }

        // 파일 정보 설정
        videoFileName.textContent = file.name;
        videoFileSize.textContent = formatFileSize(file.size);

        // 비디오 미리보기 설정
        const videoURL = URL.createObjectURL(file);
        videoPreview.src = videoURL;

        // 비디오 로드 이벤트 추가
        videoPreview.onloadedmetadata = function () {
            const duration = Math.round(videoPreview.duration);
            durationSec.value = duration;
        };

        // 비디오 로드 에러 처리
        videoPreview.onerror = function () {
            videoPreview.style.display = 'none';
            const placeholderMsg = document.createElement('div');
            placeholderMsg.className = 'video-format-notice alert alert-warning';
            placeholderMsg.innerHTML = `
                <i class="fas fa-exclamation-circle"></i> 
                미리보기를 지원하지 않는 형식입니다.<br>
                파일: ${file.name}
            `;
            uploadPreview.insertBefore(placeholderMsg, videoPreview.nextSibling);

            if (!durationSec.value) {
                durationSec.focus();
                showAlert('이 비디오 형식은 자동으로 길이를 계산할 수 없습니다. 영상 길이(초)를 직접 입력해주세요.', 'warning');
            }
        };

        // 미리보기 표시
        uploadPlaceholder.classList.add('d-none');
        uploadPreview.classList.remove('d-none');
    }

    /**
     * 썸네일 이미지 선택 처리
     */
    function handleThumbnailFileSelect(file) {
        const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
        if (!validTypes.includes(file.type)) {
            showAlert('지원하지 않는 파일 형식입니다. JPG, PNG, WEBP 파일만 허용됩니다.', 'danger');
            return;
        }

        const maxSize = 10 * 1024 * 1024;
        if (file.size > maxSize) {
            showAlert('파일 크기가 너무 큽니다. 최대 10MB까지 허용됩니다.', 'danger');
            return;
        }

        thumbnailFileName.textContent = file.name;
        thumbnailFileSize.textContent = formatFileSize(file.size);

        const imageURL = URL.createObjectURL(file);
        thumbnailImage.src = imageURL;

        thumbnailPlaceholder.classList.add('d-none');
        thumbnailPreview.classList.remove('d-none');
    }

    /**
     * 동영상 업로더 초기화
     */
    function resetVideoUploader() {
        videoFile.value = '';
        videoPreview.src = '';
        videoPreview.style.display = '';

        const formatNotice = uploadPreview.querySelector('.video-format-notice');
        if (formatNotice) {
            formatNotice.remove();
        }

        uploadPreview.classList.add('d-none');
        uploadPlaceholder.classList.remove('d-none');
    }

    /**
     * 썸네일 업로더 초기화
     */
    function resetThumbnailUploader() {
        thumbnailFile.value = '';
        thumbnailImage.src = '';
        thumbnailPreview.classList.add('d-none');
        thumbnailPlaceholder.classList.remove('d-none');
    }

    /**
     * 유튜브 영상 미리보기
     */
    function previewYoutubeVideo() {
        const urlOrId = youtubeUrl.value.trim();

        if (!urlOrId) {
            showAlert('유튜브 URL 또는 ID를 입력해주세요.', 'warning');
            return;
        }

        const videoId = extractYouTubeId(urlOrId);

        if (!videoId) {
            showAlert('올바른 유튜브 URL 또는 ID를 입력해주세요.', 'warning');
            return;
        }

        // embed URL로 변환 (모든 YouTube URL을 embed 형태로 변환)
        const embedUrl = `https://www.youtube.com/embed/${videoId}`;

        // 아이프레임 소스 설정
        youtubePreviewFrame.src = embedUrl;
        youtubePreviewContainer.classList.remove('d-none');

        // Shorts인지 일반 비디오인지 확인하여 크기 조정
        const isShorts = urlOrId.includes('/shorts/');
        if (isShorts) {
            // Shorts용 크기 (9:16)
            youtubePreviewContainer.querySelector('.ratio').className = 'ratio';
            youtubePreviewContainer.querySelector('.ratio').style.aspectRatio = '9/16';
            youtubePreviewContainer.querySelector('.ratio').style.maxWidth = '315px';
            youtubePreviewContainer.querySelector('.ratio').style.margin = '0 auto';
        } else {
            // 일반 비디오용 크기 (16:9)
            youtubePreviewContainer.querySelector('.ratio').className = 'ratio ratio-16x9';
            youtubePreviewContainer.querySelector('.ratio').style.aspectRatio = '';
            youtubePreviewContainer.querySelector('.ratio').style.maxWidth = '';
            youtubePreviewContainer.querySelector('.ratio').style.margin = '';
        }

        // 기본 동영상 길이 설정
        if (!durationSec.value) {
            durationSec.value = 180;
        }
    }

    /**
     * 유튜브 미리보기 숨기기
     */
    function hideYoutubePreview() {
        youtubePreviewFrame.src = '';
        youtubePreviewContainer.classList.add('d-none');
    }

    /**
     * YouTube URL에서 비디오 ID 추출
     */
    function extractYouTubeId(input) {
        if (!input) return null;

        const cleanInput = input.trim();

        // 1. 이미 ID만 있는 경우 (11자리)
        if (cleanInput.length === 11 && /^[a-zA-Z0-9_-]{11}$/.test(cleanInput)) {
            return cleanInput;
        }

        // 2. 다양한 YouTube URL 패턴 처리
        const patterns = [
            /(?:youtube\.com\/watch\?v=)([a-zA-Z0-9_-]{11})/,
            /(?:youtu\.be\/)([a-zA-Z0-9_-]{11})/,
            /(?:youtube\.com\/embed\/)([a-zA-Z0-9_-]{11})/,
            /(?:m\.youtube\.com\/watch\?v=)([a-zA-Z0-9_-]{11})/,
            /(?:youtube\.com\/shorts\/)([a-zA-Z0-9_-]{11})/,
            /(?:youtu\.be\/shorts\/)([a-zA-Z0-9_-]{11})/
        ];

        for (const pattern of patterns) {
            const match = cleanInput.match(pattern);
            if (match && match[1]) {
                return match[1];
            }
        }

        return null;
    }

    /**
     * 파일 크기 포맷팅
     */
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';

        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    /**
     * Flatpickr 날짜/시간 선택기 초기화
     */
    function initDateTimePicker() {
        const publishedAtInput = document.getElementById('publishedAt');

        if (publishedAtInput) {
            flatpickr(publishedAtInput, {
                locale: 'ko',
                enableTime: true,
                dateFormat: 'Y-m-d H:i',
                time_24hr: true,
                minuteIncrement: 5,
                allowInput: true,
                disableMobile: true,
                defaultDate: publishedAtInput.value || new Date()
            });
        }
    }

    /**
     * 폼 유효성 검사 및 제출
     */
    function validateAndSubmit() {
        // 기본 필드 검증
        const title = document.getElementById('title').value.trim();
        const category = document.getElementById('category').value;
        const type = document.getElementById('type').value;
        const durationSecValue = durationSec.value.trim();

        if (!title) {
            showAlert('제목을 입력해주세요.', 'danger');
            document.getElementById('title').focus();
            return;
        }

        if (!category) {
            showAlert('카테고리를 선택해주세요.', 'danger');
            document.getElementById('category').focus();
            return;
        }

        if (!type) {
            showAlert('타입을 선택해주세요.', 'danger');
            document.getElementById('type').focus();
            return;
        }

        if (!durationSecValue || isNaN(durationSecValue) || parseInt(durationSecValue) <= 0) {
            showAlert('올바른 영상 길이를 입력해주세요.', 'danger');
            durationSec.focus();
            return;
        }

        const isYoutubeTab = document.getElementById('youtube-tab').classList.contains('active');

        if (isYoutubeTab) {
            // 유튜브 URL 검증
            const youtubeUrlValue = youtubeUrl.value.trim();
            if (!youtubeUrlValue) {
                showAlert('유튜브 URL 또는 ID를 입력해주세요.', 'danger');
                youtubeUrl.focus();
                return;
            }

            const extractedId = extractYouTubeId(youtubeUrlValue);
            if (!extractedId) {
                showAlert('올바른 유튜브 URL 또는 ID를 입력해주세요.', 'warning');
                youtubeUrl.focus();
                return;
            }
        } else {
            // 파일 업로드 검증
            if (!isEditing && !videoFile.files.length) {
                showAlert('동영상 파일을 업로드해주세요.', 'danger');
                return;
            }

            if (!isEditing && !thumbnailFile.files.length) {
                showAlert('썸네일 이미지를 업로드해주세요.', 'danger');
                return;
            }
        }

        // 폼 제출
        submitForm();
    }

    /**
     * 폼 제출 처리
     */
    function submitForm() {
        btnSubmit.disabled = true;
        const originalBtnText = btnSubmit.innerHTML;
        btnSubmit.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 처리 중...';

        const formData = new FormData(videoForm);

        const endpoint = isEditing
            ? {url: '/admin/original/video/update', method: 'POST'}
            : {url: '/admin/original/video/upload', method: 'POST'};

        // 체크박스 상태 수동 추가
        formData.set('isPublic', document.getElementById('isPublic').checked);
        formData.set('isHot', document.getElementById('isHot').checked);

        // 업로드 방식에 따른 처리
        const isYoutubeTab = document.getElementById('youtube-tab').classList.contains('active');
        formData.set('uploadType', isYoutubeTab ? 'youtube' : 'file');

        // API 요청 전송
        adminAuthFetch(endpoint.url, {
            method: endpoint.method,
            body: formData
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('서버 응답 오류: ' + response.status);
            }
            return response.json();
        })
        .then(result => {
            if (result.status && result.status.code === 'OK_0000') {
                showAlert(isEditing ? '동영상이 성공적으로 수정되었습니다.' : '동영상이 성공적으로 업로드되었습니다.', 'success');
                setTimeout(() => {
                    window.location.href = '/admin/original/video/list';
                }, 1000);
            } else {
                throw new Error(result.message || '처리 중 오류가 발생했습니다.');
            }
        })
        .catch(error => {
            console.error('요청 오류:', error);
            showAlert('요청 처리 중 오류가 발생했습니다: ' + error.message, 'danger');
            btnSubmit.disabled = false;
            btnSubmit.innerHTML = originalBtnText;
        });
    }

    /**
     * 알림 표시 함수
     */
    function showAlert(message, type = 'info') {
        if (window.CustomNotification) {
            window.CustomNotification.show(message, type);
            return;
        }
        alert(message);
    }
});