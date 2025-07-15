// memorial-create.js - 메모리얼 등록 JavaScript (완전 수정본)
import { authFetch } from './commonFetch.js';

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

/**
 * 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
  console.log('메모리얼 등록 페이지 초기화');

  // 이벤트 바인딩
  bindEvents();

  // 드래그 앤 드롭 초기화
  initializeDragAndDrop();

  // 파일 목록 컨테이너 초기 상태 설정
  initializeFileContainers();

  // 제출 버튼 초기 상태 설정
  updateSubmitButton();

  console.log('초기화 완료');
});

/**
 * 파일 목록 컨테이너 초기화
 */
function initializeFileContainers() {
  const containers = ['profileImagesList', 'voiceFilesList', 'videoFileList'];
  containers.forEach(id => {
    const container = document.getElementById(id);
    if (container) {
      container.style.display = 'none';
    }
  });
}

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

  // 통합된 클릭 이벤트 처리
  document.addEventListener('click', function(e) {
    const action = e.target.getAttribute('data-action') || e.target.closest('[data-action]')?.getAttribute('data-action');

    if (!action) return;

    switch(action) {
      case 'show-profile-guide':
        e.preventDefault();
        showProfileImageGuide();
        break;
      case 'show-voice-guide':
        e.preventDefault();
        showVoiceFileGuide();
        break;
      case 'show-video-guide':
        e.preventDefault();
        showVideoFileGuide();
        break;
      case 'close-modal':
        e.preventDefault();
        closeModal(e.target);
        break;
      case 'select-profile-files':
        e.preventDefault();
        closeModalAndSelectFiles(e.target, 'profileImageInput');
        break;
      case 'select-voice-files':
        e.preventDefault();
        closeModalAndSelectFiles(e.target, 'voiceFileInput');
        break;
      case 'select-video-files':
        e.preventDefault();
        closeModalAndSelectFiles(e.target, 'videoFileInput');
        break;
      case 'remove-file':
        e.preventDefault();
        const type = e.target.getAttribute('data-type') || e.target.closest('[data-type]')?.getAttribute('data-type');
        const index = parseInt(e.target.getAttribute('data-index') || e.target.closest('[data-index]')?.getAttribute('data-index'));
        removeFile(type, index);
        break;
      case 'go-back':
        e.preventDefault();
        history.back();
        break;
    }

    // 기존 모달 관련 처리 (data-bs-dismiss 등)
    if (e.target.classList.contains('btn-close') || e.target.getAttribute('data-bs-dismiss') === 'modal') {
      const modal = e.target.closest('.modal');
      if (modal) {
        modal.classList.remove('show');
        modal.style.display = 'none';
        document.body.style.overflow = '';
      }
    }

    // 모달 배경 클릭시 닫기
    if (e.target.classList.contains('modal')) {
      e.target.classList.remove('show');
      e.target.style.display = 'none';
      document.body.style.overflow = '';
    }

    // 기존 모달 푸터 버튼 처리 (하위 호환성)
    if (e.target.classList.contains('btn-primary') && e.target.getAttribute('data-bs-dismiss') === 'modal') {
      const modal = e.target.closest('.modal');
      if (modal) {
        modal.classList.remove('show');
        modal.style.display = 'none';
        document.body.style.overflow = '';

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
 * 모달 닫기 헬퍼 함수
 */
function closeModal(element) {
  const modal = element.closest('.modal');
  if (modal) {
    modal.classList.remove('show');
    modal.style.display = 'none';
    document.body.style.overflow = '';
  }
}

/**
 * 모달 닫고 파일 선택 헬퍼 함수
 */
function closeModalAndSelectFiles(element, inputId) {
  const modal = element.closest('.modal');
  if (modal) {
    modal.classList.remove('show');
    modal.style.display = 'none';
    document.body.style.overflow = '';

    setTimeout(() => {
      const input = document.getElementById(inputId);
      if (input) {
        input.click();
      }
    }, 100);
  }
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
  updateFileList(type);
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
    id: Date.now() + Math.random(),
    name: file.name,
    size: file.size,
    type: file.type
  };

  if (Array.isArray(memorialData[type])) {
    memorialData[type].push(fileInfo);
  } else {
    memorialData[type] = fileInfo;
  }
}

/**
 * 파일 목록 업데이트
 */
function updateFileList(type) {
  // 파일 타입에 따른 올바른 컨테이너 ID 매핑
  const containerIds = {
    'profileImages': 'profileImagesList',
    'voiceFiles': 'voiceFilesList',
    'videoFile': 'videoFileList'
  };

  const listContainer = document.getElementById(containerIds[type]);
  if (!listContainer) {
    console.warn(`파일 목록 컨테이너를 찾을 수 없습니다: ${containerIds[type]}`);
    return;
  }

  const files = memorialData[type];

  if (Array.isArray(files) && files.length > 0) {
    listContainer.innerHTML = files.map((fileInfo, index) =>
        createFileListItemHtml(fileInfo, type, index)
    ).join('');
    listContainer.style.display = 'block';
  } else if (files && !Array.isArray(files)) {
    listContainer.innerHTML = createFileListItemHtml(files, type, 0);
    listContainer.style.display = 'block';
  } else {
    listContainer.innerHTML = '';
    listContainer.style.display = 'none';
  }

  // 업로드 영역 상태 업데이트
  updateUploadAreaState(type);
}

/**
 * 파일 목록 아이템 HTML 생성
 */
function createFileListItemHtml(fileInfo, type, index) {
  const { file, name, size } = fileInfo;
  const fileIcon = getFileIcon(type);

  return `
    <div class="file-item" data-type="${type}" data-index="${index}">
      <div class="file-info">
        <div class="file-icon">
          <i class="${fileIcon}"></i>
        </div>
        <div class="file-details">
          <div class="file-name">${name}</div>
          <div class="file-size">${formatFileSize(size)}</div>
        </div>
      </div>
      <button type="button" class="file-remove-btn" data-action="remove-file" data-type="${type}" data-index="${index}">
        <i class="fas fa-times"></i>
      </button>
    </div>
  `;
}

/**
 * 파일 타입별 아이콘 반환
 */
function getFileIcon(type) {
  switch(type) {
    case 'profileImages': return 'fas fa-image text-primary';
    case 'voiceFiles': return 'fas fa-volume-up text-success';
    case 'videoFile': return 'fas fa-video text-warning';
    default: return 'fas fa-file';
  }
}

/**
 * 업로드 영역 상태 업데이트
 */
function updateUploadAreaState(type) {
  const uploadArea = document.getElementById(`${type}Upload`);
  const placeholder = uploadArea?.querySelector('.upload-placeholder');

  if (!uploadArea || !placeholder) return;

  const files = memorialData[type];
  const fileCount = Array.isArray(files) ? files.length : (files ? 1 : 0);
  const limits = fileLimits[type];

  if (fileCount > 0) {
    uploadArea.classList.add('has-files');
    updatePlaceholderText(placeholder, type, fileCount, limits.maxCount);
  } else {
    uploadArea.classList.remove('has-files');
    resetPlaceholderText(placeholder, type);
  }
}

/**
 * 플레이스홀더 텍스트 업데이트
 */
function updatePlaceholderText(placeholder, type, currentCount, maxCount) {
  const titleElement = placeholder.querySelector('h4');
  const descElement = placeholder.querySelector('p');

  if (titleElement && descElement) {
    switch(type) {
      case 'profileImages':
        titleElement.textContent = `사진 ${currentCount}/${maxCount}개 선택됨`;
        descElement.innerHTML = currentCount < maxCount ?
            '추가로 사진을 선택하거나<br>이곳에 드래그하세요' :
            '최대 개수에 도달했습니다';
        break;
      case 'voiceFiles':
        titleElement.textContent = `음성 ${currentCount}/${maxCount}개 선택됨`;
        descElement.innerHTML = currentCount < maxCount ?
            '추가로 음성 파일을 선택하거나<br>이곳에 드래그하세요' :
            '최대 개수에 도달했습니다';
        break;
      case 'videoFile':
        titleElement.textContent = `영상 파일 선택됨`;
        descElement.innerHTML = '다른 영상으로 교체하려면<br>새 파일을 선택하세요';
        break;
    }
  }
}

/**
 * 플레이스홀더 텍스트 리셋
 */
function resetPlaceholderText(placeholder, type) {
  const titleElement = placeholder.querySelector('h4');
  const descElement = placeholder.querySelector('p');

  if (titleElement && descElement) {
    switch(type) {
      case 'profileImages':
        titleElement.textContent = '사진 5장 선택';
        descElement.innerHTML = 'JPG, PNG 파일을 선택하거나<br>이곳에 드래그하세요';
        break;
      case 'voiceFiles':
        titleElement.textContent = '음성 파일 3개 선택';
        descElement.innerHTML = 'MP3, WAV, M4A 파일을 선택하거나<br>이곳에 드래그하세요';
        break;
      case 'videoFile':
        titleElement.textContent = '영상 파일 1개 선택';
        descElement.innerHTML = 'MP4, MOV, AVI 파일을 선택하거나<br>이곳에 드래그하세요';
        break;
    }
  }
}

/**
 * 파일 제거
 */
function removeFile(type, index) {
  console.log(`파일 제거: ${type}[${index}]`);

  if (Array.isArray(memorialData[type])) {
    memorialData[type].splice(index, 1);
  } else {
    memorialData[type] = null;
  }

  // UI 업데이트
  updateFileList(type);
  updateSubmitButton();
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

    const blob = new Blob([JSON.stringify(basicInfo)], {
      type: 'application/json'
    });
    formData.append('memorialData', blob);

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

    // API 호출 - 수정된 부분
    const result = await authFetch('/api/memorials', {
      method: 'POST',
      body: formData
    });

    console.log('API 응답:', result);

    // 응답 구조에 따른 성공/실패 판단
    if (result.status?.code === 'OK_0000' && result.response?.success) {
      console.log('메모리얼 등록 성공:', result.response);

      // 성공 메시지 표시
      alert(result.response.message || '메모리얼이 성공적으로 등록되었습니다!');

      // 메모리얼 목록 페이지로 이동
      window.location.href = '/mobile/home';

    } else {
      // 서버에서 실패 응답을 보낸 경우
      const errorMessage = result.status?.message || result.response?.message || '등록 중 오류가 발생했습니다.';
      console.error('서버 응답 오류:', result);
      throw new Error(errorMessage);
    }

  } catch (error) {
    console.error('폼 제출 실패:', error);

    // FetchError인 경우 적절한 메시지 표시
    if (error.name === 'FetchError') {
      alert(error.statusMessage || '서버 오류가 발생했습니다. 다시 시도해주세요.');
    } else {
      alert(error.message || '등록 중 오류가 발생했습니다. 다시 시도해주세요.');
    }
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
    document.body.style.overflow = 'hidden';
    modal.classList.add('show');
    modal.style.display = 'flex';
  }
}

function showVoiceFileGuide() {
  const modal = document.getElementById('voiceFileGuideModal');
  if (modal) {
    document.body.style.overflow = 'hidden';
    modal.classList.add('show');
    modal.style.display = 'flex';
  }
}

function showVideoFileGuide() {
  const modal = document.getElementById('videoFileGuideModal');
  if (modal) {
    document.body.style.overflow = 'hidden';
    modal.classList.add('show');
    modal.style.display = 'flex';
  }
}

// 파일 선택 함수들 (하위 호환성)
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

console.log('memorial-create.js 로드 완료');