// memorial-create-step3.js - 메모리얼 등록 3단계 JavaScript (다중 파일 업로드) - 수정 버전

import { authFetch, checkLoginStatus, handleFetchError } from './commonFetch.js';
import { showToast, showConfirm, showLoading, hideLoading } from './common.js';

// 메모리얼 등록 3단계 상태 관리
let memorialStep3State = {
  currentStep: 3,
  maxStep: 4,
  tempMemorialId: null,
  isLoading: false,
  isInitialized: false,
  uploadedFiles: {
    profileImages: [], // 사진 5장
    voiceFiles: [],   // 음성 3개
    videoFile: null,  // 영상 1개
    userImage: null   // 사용자 이미지 1개
  },
  uploadProgress: {
    current: 0,
    total: 0,
    filename: '',
    percentage: 0
  },
  mediaRecorder: null,
  recordingState: {
    isRecording: false,
    startTime: null,
    blob: null,
    duration: 0,
    recordedCount: 0
  },
  fileLimits: {
    profileImages: { maxCount: 5, maxSize: 5 * 1024 * 1024, types: ['image/jpeg', 'image/png'] },
    voiceFiles: { maxCount: 3, maxSize: 50 * 1024 * 1024, types: ['audio/mp3', 'audio/wav', 'audio/m4a'] },
    videoFile: { maxCount: 1, maxSize: 100 * 1024 * 1024, types: ['video/mp4', 'video/mov', 'video/avi'] },
    userImage: { maxCount: 1, maxSize: 3 * 1024 * 1024, types: ['image/jpeg', 'image/png'] }
  }
};

/**
 * 메모리얼 등록 3단계 초기화
 */
function initializeMemorialStep3() {
  console.log('🚀 메모리얼 등록 3단계 초기화 시작');

  if (memorialStep3State.isInitialized) {
    console.warn('⚠️ 메모리얼 등록 3단계가 이미 초기화되었습니다.');
    return;
  }

  try {
    // 1. 로그인 상태 확인
    if (!checkLoginStatus()) {
      window.location.href = '/mobile/login';
      return;
    }

    // 2. 서버 데이터 로드
    loadServerData();

    // 3. 폼 초기화
    initializeForm();

    // 4. 이벤트 바인딩
    bindAllEvents();

    // 5. 드래그 앤 드롭 초기화
    initializeDragAndDrop();

    // 6. 녹음 기능 초기화
    initializeRecording();

    // 7. 필수 파일 요약 표시
    updateRequiredFilesSummary();

    // 8. 초기화 완료
    memorialStep3State.isInitialized = true;
    console.log('✅ 메모리얼 등록 3단계 초기화 완료');

  } catch (error) {
    console.error('❌ 메모리얼 등록 3단계 초기화 실패:', error);
    showToast('페이지 초기화 중 오류가 발생했습니다.', 'error');
  }
}

/**
 * 서버 데이터 로드
 */
function loadServerData() {
  console.log('📊 서버 데이터 로드');

  if (window.memorialStep3Data) {
    memorialStep3State.currentStep = window.memorialStep3Data.currentStep || 3;
    memorialStep3State.maxStep = window.memorialStep3Data.maxStep || 4;
    memorialStep3State.tempMemorialId = window.memorialStep3Data.tempMemorialId || sessionStorage.getItem('tempMemorialId');

    // 파일 제한 정보 업데이트
    if (window.memorialStep3Data.fileLimits) {
      memorialStep3State.fileLimits = { ...memorialStep3State.fileLimits, ...window.memorialStep3Data.fileLimits };
    }

    console.log('📊 서버 데이터 로드 완료:', {
      currentStep: memorialStep3State.currentStep,
      tempMemorialId: memorialStep3State.tempMemorialId
    });
  }
}

/**
 * 폼 초기화
 */
function initializeForm() {
  console.log('📝 폼 초기화');

  const form = document.getElementById('memorialStep3Form');
  if (!form) {
    throw new Error('메모리얼 등록 3단계 폼을 찾을 수 없습니다.');
  }

  // 진행 상태 업데이트
  updateProgressBar();

  console.log('✅ 폼 초기화 완료');
}

/**
 * 모든 이벤트 바인딩
 */
function bindAllEvents() {
  console.log('🔗 이벤트 바인딩 시작');

  // 1. 폼 제출 이벤트
  bindFormSubmit();

  // 2. 파일 입력 이벤트
  bindFileInputEvents();

  // 3. 녹음 이벤트
  bindRecordingEvents();

  // 4. 모달 이벤트
  bindModalEvents();

  console.log('✅ 이벤트 바인딩 완료');
}

/**
 * 폼 제출 이벤트 바인딩
 */
function bindFormSubmit() {
  const form = document.getElementById('memorialStep3Form');
  if (!form) return;

  form.addEventListener('submit', handleFormSubmit);
  console.log('📝 폼 제출 이벤트 바인딩 완료');
}

/**
 * 파일 입력 이벤트 바인딩
 */
function bindFileInputEvents() {
  const fileInputs = [
    { id: 'profileImageInput', type: 'profileImages', multiple: true },
    { id: 'voiceFileInput', type: 'voiceFiles', multiple: true },
    { id: 'videoFileInput', type: 'videoFile', multiple: false },
    { id: 'userImageInput', type: 'userImage', multiple: false }
  ];

  fileInputs.forEach(({ id, type, multiple }) => {
    const input = document.getElementById(id);
    if (input) {
      input.addEventListener('change', (e) => handleFileSelect(e, type, multiple));
    }
  });

  console.log('📁 파일 입력 이벤트 바인딩 완료');
}

/**
 * 녹음 이벤트 바인딩
 */
function bindRecordingEvents() {
  // 녹음 관련 이벤트는 전역 함수로 처리
  console.log('🎤 녹음 이벤트 바인딩 완료');
}

/**
 * 모달 이벤트 바인딩
 */
function bindModalEvents() {
  // 모달 관련 이벤트는 전역 함수로 처리
  console.log('📱 모달 이벤트 바인딩 완료');
}

/**
 * 드래그 앤 드롭 초기화
 */
function initializeDragAndDrop() {
  console.log('🎯 드래그 앤 드롭 초기화');

  const dropZones = [
    { id: 'profileImageUpload', type: 'profileImages', multiple: true },
    { id: 'voiceFileUpload', type: 'voiceFiles', multiple: true },
    { id: 'videoFileUpload', type: 'videoFile', multiple: false },
    { id: 'userImageUpload', type: 'userImage', multiple: false }
  ];

  dropZones.forEach(({ id, type, multiple }) => {
    const zone = document.getElementById(id);
    if (zone) {
      setupDragAndDrop(zone, type, multiple);
    }
  });

  console.log('✅ 드래그 앤 드롭 초기화 완료');
}

/**
 * 드래그 앤 드롭 설정
 */
function setupDragAndDrop(zone, type, multiple) {
  zone.addEventListener('dragover', (e) => {
    e.preventDefault();
    zone.classList.add('drag-over');
  });

  zone.addEventListener('dragleave', (e) => {
    e.preventDefault();
    zone.classList.remove('drag-over');
  });

  zone.addEventListener('drop', (e) => {
    e.preventDefault();
    zone.classList.remove('drag-over');

    const files = Array.from(e.dataTransfer.files);
    handleFileSelect({ target: { files } }, type, multiple);
  });
}

/**
 * 녹음 기능 초기화
 */
function initializeRecording() {
  console.log('🎙️ 녹음 기능 초기화');

  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    console.warn('⚠️ 녹음 기능을 지원하지 않는 브라우저입니다.');
    return;
  }

  console.log('✅ 녹음 기능 초기화 완료');
}

/**
 * 파일 선택 핸들러
 */
function handleFileSelect(event, type, multiple) {
  console.log(`📁 파일 선택: ${type}, multiple: ${multiple}`);

  const files = Array.from(event.target.files);

  if (files.length === 0) {
    return;
  }

  // 파일 개수 제한 확인
  if (!checkFileLimit(files, type, multiple)) {
    return;
  }

  // 파일별 유효성 검사 및 업로드
  files.forEach(file => {
    if (validateFile(file, type)) {
      uploadFile(file, type);
    }
  });

  // 파일 입력 초기화
  event.target.value = '';
}

/**
 * 파일 개수 제한 확인
 */
function checkFileLimit(files, type, multiple) {
  const limits = memorialStep3State.fileLimits[type];
  if (!limits) {
    console.warn(`⚠️ 파일 타입 제한 정보 없음: ${type}`);
    return false;
  }

  const currentCount = Array.isArray(memorialStep3State.uploadedFiles[type])
    ? memorialStep3State.uploadedFiles[type].length
    : (memorialStep3State.uploadedFiles[type] ? 1 : 0);

  if (!multiple && files.length > 1) {
    showToast('파일을 1개만 선택해주세요.', 'warning');
    return false;
  }

  if (multiple && (currentCount + files.length) > limits.maxCount) {
    showToast(`최대 ${limits.maxCount}개까지 업로드할 수 있습니다.`, 'warning');
    return false;
  }

  if (!multiple && currentCount >= limits.maxCount) {
    showToast('이미 파일이 업로드되었습니다.', 'warning');
    return false;
  }

  return true;
}

/**
 * 파일 유효성 검사
 */
function validateFile(file, type) {
  const limits = memorialStep3State.fileLimits[type];
  if (!limits) {
    console.warn(`⚠️ 파일 타입 제한 정보 없음: ${type}`);
    return false;
  }

  // 파일 크기 확인
  if (file.size > limits.maxSize) {
    const maxSizeMB = (limits.maxSize / (1024 * 1024)).toFixed(1);
    showToast(`파일 크기가 ${maxSizeMB}MB를 초과합니다: ${file.name}`, 'error');
    return false;
  }

  // 파일 타입 확인
  const fileType = file.type.toLowerCase();
  const isValidType = limits.types.some(allowedType => {
    if (allowedType === 'image/jpeg') return fileType === 'image/jpeg' || fileType === 'image/jpg';
    if (allowedType === 'image/png') return fileType === 'image/png';
    if (allowedType === 'audio/mp3') return fileType === 'audio/mp3' || fileType === 'audio/mpeg';
    if (allowedType === 'audio/wav') return fileType === 'audio/wav';
    if (allowedType === 'audio/m4a') return fileType === 'audio/m4a';
    if (allowedType === 'video/mp4') return fileType === 'video/mp4';
    if (allowedType === 'video/mov') return fileType === 'video/mov' || fileType === 'video/quicktime';
    if (allowedType === 'video/avi') return fileType === 'video/avi';
    return fileType === allowedType;
  });

  if (!isValidType) {
    showToast(`지원하지 않는 파일 형식입니다: ${file.name}`, 'error');
    return false;
  }

  return true;
}

/**
 * 파일 업로드
 */
async function uploadFile(file, type) {
  console.log(`📤 파일 업로드 시작: ${type}`, file.name);

  try {
    showLoadingOverlay();
    updateUploadProgress(0, file.name);

    // FormData 생성
    const formData = new FormData();
    formData.append('files', file);
    formData.append('fileType', type);

    // 업로드 요청
    const response = await authFetch(`/api/memorial/${memorialStep3State.tempMemorialId}/upload`, {
      method: 'POST',
      body: formData
    });

    if (!response.ok) {
      throw new Error('파일 업로드 실패');
    }

    const result = await response.json();

    if (result.success && result.data) {
      // 성공 처리
      await handleUploadSuccess(file, type, result.data);
    } else {
      throw new Error(result.message || '파일 업로드 실패');
    }

  } catch (error) {
    console.error(`❌ 파일 업로드 실패: ${type}`, error);
    handleUploadError(file, type, error);
  } finally {
    hideLoadingOverlay();
  }
}

/**
 * 업로드 성공 처리
 */
async function handleUploadSuccess(file, type, uploadData) {
  console.log(`✅ 파일 업로드 성공: ${type}`, uploadData);

  // 업로드된 파일 정보 저장
  const fileInfo = {
    file: file,
    data: uploadData[0] || uploadData, // 업로드된 파일 데이터
    preview: await createFilePreview(file, type)
  };

  // 상태 업데이트
  if (Array.isArray(memorialStep3State.uploadedFiles[type])) {
    memorialStep3State.uploadedFiles[type].push(fileInfo);
  } else {
    memorialStep3State.uploadedFiles[type] = fileInfo;
  }

  // UI 업데이트
  updateFilePreview(type);
  updateRequiredFilesSummary();
  updateCompleteButton();

  showToast(`${file.name} 업로드 완료`, 'success');
}

/**
 * 업로드 실패 처리
 */
function handleUploadError(file, type, error) {
  console.error(`❌ 파일 업로드 실패: ${type}`, error);

  let message = '파일 업로드에 실패했습니다.';

  if (error.message.includes('File size')) {
    message = '파일 크기가 너무 큽니다.';
  } else if (error.message.includes('File type')) {
    message = '지원하지 않는 파일 형식입니다.';
  } else if (error.message.includes('File count')) {
    message = '업로드 가능한 파일 개수를 초과했습니다.';
  }

  showToast(`${file.name}: ${message}`, 'error');
}

/**
 * 파일 미리보기 생성
 */
function createFilePreview(file, type) {
  return new Promise((resolve) => {
    if (type === 'profileImages' || type === 'userImage') {
      // 이미지 미리보기
      const reader = new FileReader();
      reader.onload = (e) => resolve(e.target.result);
      reader.readAsDataURL(file);
    } else if (type === 'voiceFiles') {
      // 음성 파일 미리보기 (URL)
      resolve(URL.createObjectURL(file));
    } else if (type === 'videoFile') {
      // 비디오 파일 미리보기 (URL)
      resolve(URL.createObjectURL(file));
    } else {
      resolve(null);
    }
  });
}

/**
 * 파일 미리보기 업데이트
 */
function updateFilePreview(type) {
  const previewContainer = document.getElementById(`${type}Preview`);
  if (!previewContainer) return;

  const files = memorialStep3State.uploadedFiles[type];

  if (Array.isArray(files)) {
    // 다중 파일 미리보기
    previewContainer.innerHTML = files.map((fileInfo, index) =>
      createPreviewHtml(fileInfo, type, index)
    ).join('');
  } else if (files) {
    // 단일 파일 미리보기
    previewContainer.innerHTML = createPreviewHtml(files, type, 0);
  } else {
    previewContainer.innerHTML = '';
  }
}

/**
 * 미리보기 HTML 생성
 */
function createPreviewHtml(fileInfo, type, index) {
  const { file, preview } = fileInfo;

  if (type === 'profileImages' || type === 'userImage') {
    return `
      <div class="preview-item">
        <img src="${preview}" alt="${file.name}">
        <div class="preview-overlay">
          <button type="button" class="btn-remove" onclick="removeFile('${type}', ${index})">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="preview-info">
          <div class="file-name">${file.name}</div>
          <div class="file-size">${formatFileSize(file.size)}</div>
        </div>
      </div>
    `;
  } else if (type === 'voiceFiles') {
    return `
      <div class="preview-item audio-preview">
        <div class="audio-icon">
          <i class="fas fa-volume-up"></i>
        </div>
        <div class="preview-info">
          <div class="file-name">${file.name}</div>
          <div class="file-size">${formatFileSize(file.size)}</div>
        </div>
        <div class="audio-controls">
          <audio controls>
            <source src="${preview}" type="${file.type}">
          </audio>
        </div>
        <div class="preview-overlay">
          <button type="button" class="btn-remove" onclick="removeFile('${type}', ${index})">
            <i class="fas fa-times"></i>
          </button>
        </div>
      </div>
    `;
  } else if (type === 'videoFile') {
    return `
      <div class="preview-item video-preview">
        <video controls>
          <source src="${preview}" type="${file.type}">
        </video>
        <div class="preview-overlay">
          <button type="button" class="btn-remove" onclick="removeFile('${type}', ${index})">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="preview-info">
          <div class="file-name">${file.name}</div>
          <div class="file-size">${formatFileSize(file.size)}</div>
        </div>
      </div>
    `;
  }

  return '';
}

/**
 * 파일 제거
 */
function removeFile(type, index) {
  console.log(`🗑️ 파일 제거: ${type}, index: ${index}`);

  if (Array.isArray(memorialStep3State.uploadedFiles[type])) {
    memorialStep3State.uploadedFiles[type].splice(index, 1);
  } else {
    memorialStep3State.uploadedFiles[type] = null;
  }

  updateFilePreview(type);
  updateRequiredFilesSummary();
  updateCompleteButton();

  showToast('파일이 제거되었습니다.', 'info');
}

/**
 * 필수 파일 요약 업데이트
 */
function updateRequiredFilesSummary() {
  const summaryItems = [
    { id: 'profileImageStatus', type: 'profileImages', required: 5, label: '대표 사진' },
    { id: 'voiceFileStatus', type: 'voiceFiles', required: 3, label: '음성 파일' },
    { id: 'videoFileStatus', type: 'videoFile', required: 1, label: '영상 파일' },
    { id: 'userImageStatus', type: 'userImage', required: 1, label: '사용자 이미지' }
  ];

  summaryItems.forEach(({ id, type, required, label }) => {
    const element = document.getElementById(id);
    if (!element) return;

    const files = memorialStep3State.uploadedFiles[type];
    const currentCount = Array.isArray(files) ? files.length : (files ? 1 : 0);
    const isComplete = currentCount >= required;

    // 상태 업데이트
    element.className = `required-file-item ${isComplete ? 'completed' : 'pending'}`;

    // 아이콘 업데이트
    const icon = element.querySelector('.status-icon i');
    if (icon) {
      icon.className = isComplete ? 'fas fa-check' : 'fas fa-times';
    }

    // 텍스트 업데이트
    const span = element.querySelector('span');
    if (span) {
      span.textContent = `${label}: ${currentCount}/${required}개 (필수)`;
    }
  });
}

/**
 * 완료 버튼 업데이트
 */
function updateCompleteButton() {
  const completeBtn = document.getElementById('completeBtn');
  if (!completeBtn) return;

  const isAllComplete =
    memorialStep3State.uploadedFiles.profileImages.length >= 5 &&
    memorialStep3State.uploadedFiles.voiceFiles.length >= 3 &&
    memorialStep3State.uploadedFiles.videoFile &&
    memorialStep3State.uploadedFiles.userImage;

  completeBtn.disabled = !isAllComplete;

  if (isAllComplete) {
    completeBtn.innerHTML = '완료 <i class="fas fa-check"></i>';
  } else {
    completeBtn.innerHTML = '필수 파일을 업로드해주세요 <i class="fas fa-exclamation-triangle"></i>';
  }
}

/**
 * 폼 제출 핸들러
 */
async function handleFormSubmit(event) {
  event.preventDefault();
  console.log('📤 3단계 폼 제출 시작');

  if (memorialStep3State.isLoading) {
    return;
  }

  try {
    memorialStep3State.isLoading = true;
    showLoadingOverlay();

    // 완료 처리
    await completeMemorial();

    // 완료 페이지로 이동
    await moveToCompletePage();

  } catch (error) {
    console.error('❌ 3단계 폼 제출 실패:', error);
    handleFetchError(error);
  } finally {
    memorialStep3State.isLoading = false;
    hideLoadingOverlay();
  }
}

/**
 * 메모리얼 완료 처리
 */
async function completeMemorial() {
  console.log('🎯 메모리얼 완료 처리 시작');

  try {
    const response = await authFetch(`/api/memorial/${memorialStep3State.tempMemorialId}/complete`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      }
    });

    if (!response.ok) {
      throw new Error('메모리얼 완료 처리 실패');
    }

    const result = await response.json();

    if (result.success) {
      console.log('✅ 메모리얼 완료 처리 성공:', result.data);

      // 로컬 스토리지 정리
      sessionStorage.removeItem('tempMemorialId');
      sessionStorage.removeItem('memorialStep1Data');
      sessionStorage.removeItem('memorialStep2Data');

      return result.data;
    } else {
      throw new Error(result.message || '메모리얼 완료 처리 실패');
    }

  } catch (error) {
    console.error('❌ 메모리얼 완료 처리 실패:', error);
    throw error;
  }
}

/**
 * 완료 페이지로 이동
 */
async function moveToCompletePage() {
  console.log('🏁 완료 페이지로 이동');

  try {
    showToast('메모리얼이 성공적으로 등록되었습니다!', 'success');

    // 잠시 후 완료 페이지로 이동
    setTimeout(() => {
      window.location.href = `/memorial/create/complete?memorialId=${memorialStep3State.tempMemorialId}`;
    }, 1500);

  } catch (error) {
    console.error('❌ 완료 페이지 이동 실패:', error);
    throw error;
  }
}

/**
 * 녹음 관련 함수들
 */
async function startVoiceRecording() {
  console.log('🎙️ 음성 녹음 시작');

  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

    memorialStep3State.mediaRecorder = new MediaRecorder(stream);
    memorialStep3State.recordingState.isRecording = true;
    memorialStep3State.recordingState.startTime = Date.now();
    memorialStep3State.recordingState.duration = 0;

    // 녹음 데이터 저장
    const audioChunks = [];
    memorialStep3State.mediaRecorder.addEventListener('dataavailable', (event) => {
      audioChunks.push(event.data);
    });

    memorialStep3State.mediaRecorder.addEventListener('stop', () => {
      const audioBlob = new Blob(audioChunks, { type: 'audio/wav' });
      handleRecordingComplete(audioBlob);
    });

    memorialStep3State.mediaRecorder.start();

    // UI 업데이트
    showRecordingInterface();
    startRecordingTimer();

  } catch (error) {
    console.error('❌ 녹음 시작 실패:', error);
    showToast('마이크 접근 권한이 필요합니다.', 'error');
  }
}

function stopVoiceRecording() {
  console.log('⏹️ 음성 녹음 중지');

  if (memorialStep3State.mediaRecorder && memorialStep3State.recordingState.isRecording) {
    memorialStep3State.mediaRecorder.stop();
    memorialStep3State.recordingState.isRecording = false;

    // 스트림 정리
    memorialStep3State.mediaRecorder.stream.getTracks().forEach(track => track.stop());

    hideRecordingInterface();
  }
}

function cancelVoiceRecording() {
  console.log('❌ 음성 녹음 취소');

  if (memorialStep3State.mediaRecorder && memorialStep3State.recordingState.isRecording) {
    memorialStep3State.mediaRecorder.stop();
    memorialStep3State.recordingState.isRecording = false;

    // 스트림 정리
    memorialStep3State.mediaRecorder.stream.getTracks().forEach(track => track.stop());

    hideRecordingInterface();
    showToast('녹음이 취소되었습니다.', 'info');
  }
}

function handleRecordingComplete(audioBlob) {
  console.log('✅ 녹음 완료');

  // 녹음된 파일을 File 객체로 변환
  const recordedFile = new File([audioBlob], `recording_${Date.now()}.wav`, {
    type: 'audio/wav',
    lastModified: Date.now()
  });

  // 파일 업로드
  uploadFile(recordedFile, 'voiceFiles');

  memorialStep3State.recordingState.recordedCount++;
}

/**
 * UI 업데이트 함수들
 */
function showRecordingInterface() {
  const recordingInterface = document.getElementById('recordingInterface');
  if (recordingInterface) {
    recordingInterface.style.display = 'block';
  }
}

function hideRecordingInterface() {
  const recordingInterface = document.getElementById('recordingInterface');
  if (recordingInterface) {
    recordingInterface.style.display = 'none';
  }
}

function startRecordingTimer() {
  const timerInterval = setInterval(() => {
    if (!memorialStep3State.recordingState.isRecording) {
      clearInterval(timerInterval);
      return;
    }

    const elapsed = Date.now() - memorialStep3State.recordingState.startTime;
    const seconds = Math.floor(elapsed / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;

    const timerElement = document.querySelector('.recording-time');
    if (timerElement) {
      timerElement.textContent = `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
    }

    // 최대 녹음 시간 제한 (5분)
    if (elapsed > 300000) {
      stopVoiceRecording();
    }
  }, 1000);
}

function showLoadingOverlay() {
  const overlay = document.getElementById('loadingOverlay');
  if (overlay) {
    overlay.style.display = 'flex';
  }
}

function hideLoadingOverlay() {
  const overlay = document.getElementById('loadingOverlay');
  if (overlay) {
    overlay.style.display = 'none';
  }
}

function updateUploadProgress(percentage, filename) {
  const progressBar = document.getElementById('uploadProgress');
  const progressText = document.getElementById('uploadProgressText');

  if (progressBar) {
    progressBar.style.width = `${percentage}%`;
  }

  if (progressText) {
    progressText.textContent = `${Math.round(percentage)}%`;
  }

  if (filename) {
    const loadingText = document.querySelector('.loading-text');
    if (loadingText) {
      loadingText.textContent = `${filename} 업로드 중...`;
    }
  }
}

function updateProgressBar() {
  const progressFill = document.querySelector('.progress-fill');
  if (progressFill) {
    const progress = (memorialStep3State.currentStep / memorialStep3State.maxStep) * 100;
    progressFill.style.width = `${progress}%`;
  }
}

function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * 가이드 모달 함수들
 */
function showProfileImageGuide() {
  const modal = new bootstrap.Modal(document.getElementById('profileImageGuideModal'));
  modal.show();
}

function showVoiceFileGuide() {
  const modal = new bootstrap.Modal(document.getElementById('voiceFileGuideModal'));
  modal.show();
}

function showVideoFileGuide() {
  const modal = new bootstrap.Modal(document.getElementById('videoFileGuideModal'));
  modal.show();
}

function showUserImageGuide() {
  const modal = new bootstrap.Modal(document.getElementById('userImageGuideModal'));
  modal.show();
}

function showAiLearningInfo() {
  const modal = new bootstrap.Modal(document.getElementById('aiLearningInfoModal'));
  modal.show();
}

/**
 * 정리 함수
 */
function destroyMemorialStep3() {
  console.log('🗑️ 메모리얼 등록 3단계 정리');

  // 녹음 정리
  if (memorialStep3State.mediaRecorder) {
    memorialStep3State.mediaRecorder.stop();
    memorialStep3State.mediaRecorder = null;
  }

  // 파일 URL 정리
  Object.values(memorialStep3State.uploadedFiles).forEach(files => {
    if (Array.isArray(files)) {
      files.forEach(file => {
        if (file.preview && file.preview.startsWith('blob:')) {
          URL.revokeObjectURL(file.preview);
        }
      });
    } else if (files && files.preview && files.preview.startsWith('blob:')) {
      URL.revokeObjectURL(files.preview);
    }
  });

  memorialStep3State.isInitialized = false;
}

/**
 * 전역 함수들
 */
window.memorialStep3Manager = {
  initialize: initializeMemorialStep3,
  destroy: destroyMemorialStep3,
  getState: () => memorialStep3State
};

// 전역 함수들 (HTML에서 호출 가능)
window.startVoiceRecording = startVoiceRecording;
window.stopVoiceRecording = stopVoiceRecording;
window.cancelVoiceRecording = cancelVoiceRecording;
window.removeFile = removeFile;
window.showProfileImageGuide = showProfileImageGuide;
window.showVoiceFileGuide = showVoiceFileGuide;
window.showVideoFileGuide = showVideoFileGuide;
window.showUserImageGuide = showUserImageGuide;
window.showAiLearningInfo = showAiLearningInfo;

/**
 * 자동 초기화
 */
console.log('🌟 memorial-create-step3.js 로드 완료');

// DOM이 준비되면 초기화
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initializeMemorialStep3);
} else {
  setTimeout(initializeMemorialStep3, 100);
}

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', destroyMemorialStep3);

// 모듈 익스포트
export {
  initializeMemorialStep3,
  uploadFile,
  removeFile,
  startVoiceRecording,
  stopVoiceRecording,
  cancelVoiceRecording,
  destroyMemorialStep3
};