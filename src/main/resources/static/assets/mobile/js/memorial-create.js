// memorial-create.js - 메모리얼 등록 JavaScript

// 상태 관리
let memorialData = {
  // 기본 정보
  name: '',
  nickname: '',
  gender: '',
  birthDate: '',
  deathDate: '',
  relationship: '',

  // 고인 정보
  personality: '',
  favoriteWords: '',
  favoriteFoods: '',
  memories: '',
  habits: '',

  // 업로드된 파일들
  profileImages: [],
  voiceFiles: [],
  videoFile: null
};

// 파일 제한
const fileLimits = {
  profileImages: { maxCount: 5, maxSize: 5 * 1024 * 1024, types: ['image/jpeg', 'image/png', 'image/jpg'] },
  voiceFiles: { maxCount: 3, maxSize: 50 * 1024 * 1024, types: ['audio/mp3', 'audio/wav', 'audio/m4a', 'audio/mpeg'] },
  videoFile: { maxCount: 1, maxSize: 100 * 1024 * 1024, types: ['video/mp4', 'video/mov', 'video/avi', 'video/quicktime'] }
};

// 녹음 관련
let mediaRecorder = null;
let recordingState = {
  isRecording: false,
  startTime: null
};

/**
 * 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
  console.log('메모리얼 등록 페이지 초기화');

  // 이벤트 바인딩
  bindEvents();

  // 드래그 앤 드롭 초기화
  initializeDragAndDrop();

  console.log('초기화 완료');
});

/**
 * 이벤트 바인딩
 */
function bindEvents() {
  // 폼 제출
  const form = document.getElementById('memorialCreateForm');
  if (form) {
    form.addEventListener('submit', handleFormSubmit);
  }

  // 텍스트 입력 필드
  const textInputs = ['name', 'nickname', 'birthDate', 'deathDate'];
  textInputs.forEach(id => {
    const input = document.getElementById(id);
    if (input) {
      input.addEventListener('input', handleBasicInfoChange);
      input.addEventListener('blur', validateBasicInfo);
    }
  });

  // 성별 라디오 버튼
  const genderInputs = document.querySelectorAll('input[name="gender"]');
  genderInputs.forEach(radio => {
    radio.addEventListener('change', handleBasicInfoChange);
  });

  // 관계 선택
  const relationshipSelect = document.getElementById('relationship');
  if (relationshipSelect) {
    relationshipSelect.addEventListener('change', handleBasicInfoChange);
  }

  // 텍스트 영역들
  const textareas = ['personality', 'favoriteWords', 'favoriteFoods', 'memories', 'habits'];
  textareas.forEach(id => {
    const textarea = document.getElementById(id);
    if (textarea) {
      textarea.addEventListener('input', handlePersonalityChange);

      // 글자 수 카운터
      const countElement = document.getElementById(`${id}Count`);
      if (countElement) {
        textarea.addEventListener('input', () => updateCharacterCount(textarea, countElement));
      }
    }
  });

  // 파일 업로드
  const fileInputs = [
    { id: 'profileImageInput', type: 'profileImages', multiple: true },
    { id: 'voiceFileInput', type: 'voiceFiles', multiple: true },
    { id: 'videoFileInput', type: 'videoFile', multiple: false }
  ];

  fileInputs.forEach(({ id, type, multiple }) => {
    const input = document.getElementById(id);
    if (input) {
      input.addEventListener('change', (e) => handleFileSelect(e, type, multiple));
    }
  });
}

/**
 * 드래그 앤 드롭 초기화
 */
function initializeDragAndDrop() {
  const dropZones = [
    { id: 'profileImageUpload', type: 'profileImages', multiple: true },
    { id: 'voiceFileUpload', type: 'voiceFiles', multiple: true },
    { id: 'videoFileUpload', type: 'videoFile', multiple: false }
  ];

  dropZones.forEach(({ id, type, multiple }) => {
    const zone = document.getElementById(id);
    if (zone) {
      setupDragAndDrop(zone, type, multiple);
    }
  });
}

/**
 * 드래그 앤 드롭 설정
 */
function setupDragAndDrop(zone, type, multiple) {
  zone.addEventListener('dragover', (e) => {
    e.preventDefault();
    zone.classList.add('dragover');
  });

  zone.addEventListener('dragleave', (e) => {
    e.preventDefault();
    zone.classList.remove('dragover');
  });

  zone.addEventListener('drop', (e) => {
    e.preventDefault();
    zone.classList.remove('dragover');

    const files = Array.from(e.dataTransfer.files);
    handleFileSelect({ target: { files } }, type, multiple);
  });
}

/**
 * 기본 정보 변경 핸들러
 */
function handleBasicInfoChange(event) {
  const { name, value, type } = event.target;

  if (type === 'radio') {
    memorialData[name] = value;
  } else {
    memorialData[name] = value;
  }

  // 실시간 유효성 검사
  validateBasicInfo();
  updateSubmitButton();

  console.log(`기본 정보 변경: ${name} = ${value}`);
}

/**
 * 고인 정보 변경 핸들러
 */
function handlePersonalityChange(event) {
  const { name, value } = event.target;
  memorialData[name] = value;

  console.log(`고인 정보 변경: ${name} = ${value.length}자`);
}

/**
 * 파일 선택 핸들러
 */
function handleFileSelect(event, type, multiple) {
  const files = Array.from(event.target.files);

  if (files.length === 0) {
    return;
  }

  console.log(`파일 선택: ${type}, ${files.length}개`);

  // 파일 개수 제한 확인
  if (!checkFileLimit(files, type, multiple)) {
    return;
  }

  // 파일별 처리
  files.forEach(file => {
    if (validateFile(file, type)) {
      addFileToData(file, type);
    }
  });

  // 파일 입력 초기화
  event.target.value = '';

  // UI 업데이트
  updateFilePreview(type);
  updateSubmitButton();
}

/**
 * 파일 제한 확인
 */
function checkFileLimit(files, type, multiple) {
  const limits = fileLimits[type];
  if (!limits) return false;

  const currentFiles = memorialData[type];
  const currentCount = Array.isArray(currentFiles) ? currentFiles.length : (currentFiles ? 1 : 0);

  if (!multiple && files.length > 1) {
    alert('파일을 1개만 선택해주세요.');
    return false;
  }

  if (multiple && (currentCount + files.length) > limits.maxCount) {
    alert(`최대 ${limits.maxCount}개까지 업로드할 수 있습니다.`);
    return false;
  }

  if (!multiple && currentCount >= limits.maxCount) {
    alert('이미 파일이 업로드되었습니다.');
    return false;
  }

  return true;
}

/**
 * 파일 유효성 검사
 */
function validateFile(file, type) {
  const limits = fileLimits[type];
  if (!limits) return false;

  // 파일 크기 확인
  if (file.size > limits.maxSize) {
    const maxSizeMB = (limits.maxSize / (1024 * 1024)).toFixed(1);
    alert(`파일 크기가 ${maxSizeMB}MB를 초과합니다: ${file.name}`);
    return false;
  }

  // 파일 타입 확인
  const fileType = file.type.toLowerCase();
  const isValidType = limits.types.some(allowedType => {
    if (allowedType === 'image/jpeg') return fileType === 'image/jpeg' || fileType === 'image/jpg';
    if (allowedType === 'audio/mp3') return fileType === 'audio/mp3' || fileType === 'audio/mpeg';
    return fileType === allowedType;
  });

  if (!isValidType) {
    alert(`지원하지 않는 파일 형식입니다: ${file.name}`);
    return false;
  }

  return true;
}

/**
 * 파일을 데이터에 추가
 */
function addFileToData(file, type) {
  const fileInfo = {
    file: file,
    preview: null,
    id: Date.now() + Math.random()
  };

  // 미리보기 생성
  createFilePreview(file, type).then(preview => {
    fileInfo.preview = preview;
    updateFilePreview(type);
  });

  if (Array.isArray(memorialData[type])) {
    memorialData[type].push(fileInfo);
  } else {
    memorialData[type] = fileInfo;
  }
}

/**
 * 파일 미리보기 생성
 */
function createFilePreview(file, type) {
  return new Promise((resolve) => {
    if (type === 'profileImages') {
      const reader = new FileReader();
      reader.onload = (e) => resolve(e.target.result);
      reader.readAsDataURL(file);
    } else if (type === 'voiceFiles') {
      resolve(URL.createObjectURL(file));
    } else if (type === 'videoFile') {
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

  const files = memorialData[type];

  if (Array.isArray(files) && files.length > 0) {
    if (type === 'profileImages') {
      previewContainer.innerHTML = files.map((fileInfo, index) =>
        createImagePreviewHtml(fileInfo, type, index)
      ).join('');
    } else if (type === 'voiceFiles') {
      previewContainer.innerHTML = files.map((fileInfo, index) =>
        createAudioPreviewHtml(fileInfo, type, index)
      ).join('');
    }
  } else if (files && !Array.isArray(files)) {
    if (type === 'videoFile') {
      previewContainer.innerHTML = createVideoPreviewHtml(files, type, 0);
    }
  } else {
    previewContainer.innerHTML = '';
  }
}

/**
 * 이미지 미리보기 HTML 생성
 */
function createImagePreviewHtml(fileInfo, type, index) {
  const { file, preview } = fileInfo;

  return `
    <div class="preview-item">
      <img src="${preview || ''}" alt="${file.name}" loading="lazy">
      <div class="preview-info">
        <div class="file-name">${file.name}</div>
        <div class="file-size">${formatFileSize(file.size)}</div>
      </div>
    </div>
  `;
}

/**
 * 오디오 미리보기 HTML 생성
 */
function createAudioPreviewHtml(fileInfo, type, index) {
  const { file, preview } = fileInfo;

  return `
    <div class="audio-preview-item">
      <div class="audio-info">
        <i class="fas fa-volume-up"></i>
        <div class="audio-details">
          <div class="audio-name">${file.name}</div>
          <div class="audio-size">${formatFileSize(file.size)}</div>
        </div>
      </div>
      <div class="audio-controls">
        <audio controls>
          <source src="${preview}" type="${file.type}">
        </audio>
      </div>
    </div>
  `;
}

/**
 * 비디오 미리보기 HTML 생성
 */
function createVideoPreviewHtml(fileInfo, type, index) {
  const { file, preview } = fileInfo;

  return `
    <div class="preview-item video-preview">
      <video controls>
        <source src="${preview}" type="${file.type}">
      </video>
      <div class="preview-info">
        <div class="file-name">${file.name}</div>
        <div class="file-size">${formatFileSize(file.size)}</div>
      </div>
    </div>
  `;
}

/**
 * 글자 수 카운터 업데이트
 */
function updateCharacterCount(textarea, countElement) {
  const currentLength = textarea.value.length;
  const fieldName = textarea.name;
  const limits = {
    personality: 500,
    favoriteWords: 300,
    favoriteFoods: 300,
    memories: 300,
    habits: 300
  };

  const limit = limits[fieldName];
  countElement.textContent = currentLength;

  // 글자 수에 따른 색상 변경
  const parentElement = countElement.parentElement;
  parentElement.classList.remove('warning', 'danger');

  if (limit) {
    const percentage = (currentLength / limit) * 100;

    if (percentage >= 90) {
      parentElement.classList.add('danger');
    } else if (percentage >= 70) {
      parentElement.classList.add('warning');
    }
  }
}

/**
 * 기본 정보 유효성 검사
 */
function validateBasicInfo() {
  const required = ['name', 'nickname', 'gender', 'relationship'];
  let isValid = true;

  required.forEach(field => {
    const value = memorialData[field];
    if (!value || value.trim().length === 0) {
      isValid = false;
    }
  });

  // 이름 길이 검사
  if (memorialData.name && memorialData.name.length < 2) {
    isValid = false;
  }

  // 호칭 길이 검사
  if (memorialData.nickname && memorialData.nickname.length < 2) {
    isValid = false;
  }

  return isValid;
}

/**
 * 전체 데이터 유효성 검사
 */
function validateAllData() {
  // 기본 정보 검사
  if (!validateBasicInfo()) {
    alert('기본 정보를 모두 입력해주세요.');
    return false;
  }

  // 필수 파일 검사
  const requiredFiles = [
    { key: 'profileImages', min: 5, name: '대표 사진' },
    { key: 'voiceFiles', min: 3, name: '음성 파일' },
    { key: 'videoFile', min: 1, name: '영상 파일' }
  ];

  for (const { key, min, name } of requiredFiles) {
    const files = memorialData[key];
    const count = Array.isArray(files) ? files.length : (files ? 1 : 0);

    if (count < min) {
      alert(`${name} ${min}개를 업로드해주세요.`);
      return false;
    }
  }

  return true;
}

/**
 * 제출 버튼 업데이트
 */
function updateSubmitButton() {
  const submitBtn = document.getElementById('submitBtn');
  if (!submitBtn) return;

  const basicInfoValid = validateBasicInfo();
  const profileImagesValid = memorialData.profileImages.length >= 5;
  const voiceFilesValid = memorialData.voiceFiles.length >= 3;
  const videoFileValid = memorialData.videoFile !== null;

  const isAllValid = basicInfoValid && profileImagesValid && voiceFilesValid && videoFileValid;

  submitBtn.disabled = !isAllValid;

  if (isAllValid) {
    submitBtn.innerHTML = '<i class="fas fa-upload"></i> 등록하기';
    submitBtn.classList.remove('btn-disabled');
  } else {
    submitBtn.innerHTML = '<i class="fas fa-exclamation-triangle"></i> 필수 정보를 입력해주세요';
    submitBtn.classList.add('btn-disabled');
  }
}

/**
 * 폼 제출 핸들러
 */
async function handleFormSubmit(event) {
  event.preventDefault();

  console.log('폼 제출 시작');

  try {
    // 유효성 검사
    if (!validateAllData()) {
      return;
    }

    // 로딩 표시
    const submitBtn = document.getElementById('submitBtn');
    const originalText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 등록 중...';

    // FormData 생성
    const formData = new FormData();

    // 기본 정보 추가
    const basicInfo = {
      name: memorialData.name,
      nickname: memorialData.nickname,
      gender: memorialData.gender,
      birthDate: memorialData.birthDate || null,
      deathDate: memorialData.deathDate || null,
      relationship: memorialData.relationship,
      personality: memorialData.personality || '',
      favoriteWords: memorialData.favoriteWords || '',
      favoriteFoods: memorialData.favoriteFoods || '',
      memories: memorialData.memories || '',
      habits: memorialData.habits || ''
    };

    formData.append('memorialData', JSON.stringify(basicInfo));

    // 파일들 추가
    memorialData.profileImages.forEach((fileInfo, index) => {
      formData.append('profileImages', fileInfo.file);
    });

    memorialData.voiceFiles.forEach((fileInfo, index) => {
      formData.append('voiceFiles', fileInfo.file);
    });

    if (memorialData.videoFile) {
      formData.append('videoFile', memorialData.videoFile.file);
    }

    // API 호출 (실제 구현 시 수정 필요)
    console.log('메모리얼 데이터:', basicInfo);
    console.log('업로드 파일 개수:', {
      profileImages: memorialData.profileImages.length,
      voiceFiles: memorialData.voiceFiles.length,
      videoFile: memorialData.videoFile ? 1 : 0
    });

    // 임시 성공 처리
    setTimeout(() => {
      alert('메모리얼이 성공적으로 등록되었습니다!');
      // window.location.href = '/mobile/memorial';
    }, 2000);

  } catch (error) {
    console.error('폼 제출 실패:', error);
    alert('등록 중 오류가 발생했습니다. 다시 시도해주세요.');
  } finally {
    // 로딩 해제
    const submitBtn = document.getElementById('submitBtn');
    submitBtn.disabled = false;
    updateSubmitButton();
  }
}

/**
 * 가이드 모달 함수들
 */
function showProfileImageGuide() {
  const modal = document.getElementById('profileImageGuideModal');
  if (modal) {
    // 뒤쪽 스크롤 방지
    document.body.style.overflow = 'hidden';
    modal.classList.add('show');
    modal.style.display = 'flex';
  }
}

function showVoiceFileGuide() {
  const modal = document.getElementById('voiceFileGuideModal');
  if (modal) {
    // 뒤쪽 스크롤 방지
    document.body.style.overflow = 'hidden';
    modal.classList.add('show');
    modal.style.display = 'flex';
  }
}

function showVideoFileGuide() {
  const modal = document.getElementById('videoFileGuideModal');
  if (modal) {
    // 뒤쪽 스크롤 방지
    document.body.style.overflow = 'hidden';
    modal.classList.add('show');
    modal.style.display = 'flex';
  }
}

// 파일 선택 함수들
function selectProfileImages() {
  const input = document.getElementById('profileImageInput');
  if (input) {
    input.click();
  }
}

function selectVoiceFiles() {
  const input = document.getElementById('voiceFileInput');
  if (input) {
    input.click();
  }
}

function selectVideoFile() {
  const input = document.getElementById('videoFileInput');
  if (input) {
    input.click();
  }
}

// 모달 닫기 이벤트
document.addEventListener('click', function(e) {
  if (e.target.classList.contains('btn-close') || e.target.getAttribute('data-bs-dismiss') === 'modal') {
    const modal = e.target.closest('.modal');
    if (modal) {
      modal.classList.remove('show');
      modal.style.display = 'none';
      // 뒤쪽 스크롤 복원
      document.body.style.overflow = '';
    }
  }

  if (e.target.classList.contains('modal')) {
    e.target.classList.remove('show');
    e.target.style.display = 'none';
    // 뒤쪽 스크롤 복원
    document.body.style.overflow = '';
  }

  // 모달 푸터의 "파일 선택하기" 버튼 클릭 처리
  if (e.target.classList.contains('btn-primary') && e.target.getAttribute('data-bs-dismiss') === 'modal') {
    const modal = e.target.closest('.modal');
    if (modal) {
      // 모달 닫기
      modal.classList.remove('show');
      modal.style.display = 'none';
      document.body.style.overflow = '';

      // 해당 파일 입력 창 열기
      if (modal.id === 'profileImageGuideModal') {
        setTimeout(() => selectProfileImages(), 100);
      } else if (modal.id === 'voiceFileGuideModal') {
        setTimeout(() => selectVoiceFiles(), 100);
      } else if (modal.id === 'videoFileGuideModal') {
        setTimeout(() => selectVideoFile(), 100);
      }
    }
  }
});

/**
 * 유틸리티 함수들
 */
function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * 정리 함수
 */
window.addEventListener('beforeunload', function() {
  // 파일 URL 정리
  Object.values(memorialData).forEach(files => {
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
});

// CSS 애니메이션 추가
const style = document.createElement('style');
style.textContent = `
  @keyframes pulse {
    0% { transform: scale(1); }
    50% { transform: scale(1.1); }
    100% { transform: scale(1); }
  }
  
  .audio-preview-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    background: #f7fafc;
    border-radius: 8px;
    border: 1px solid #e2e8f0;
    margin-bottom: 8px;
  }
  
  .audio-info {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
  }
  
  .audio-details {
    flex: 1;
  }
  
  .audio-name {
    font-size: 14px;
    font-weight: 600;
    color: #2d3748;
    margin-bottom: 2px;
  }
  
  .audio-size {
    font-size: 12px;
    color: #718096;
  }
  
  .audio-controls audio {
    width: 200px;
    height: 32px;
  }
  
  .recording-indicator {
    text-align: center;
    padding: 20px;
  }
  
  .btn-disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
`;
document.head.appendChild(style);

console.log('memorial-create.js 로드 완료');