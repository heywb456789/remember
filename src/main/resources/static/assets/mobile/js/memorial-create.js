// memorial-create.js - 메모리얼 등록 JavaScript (수정된 버전)
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

  // 질문 답변들 (동적 질문) - List<DTO> 형태
  questionAnswers: [],

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

// 서버에서 전달된 질문 데이터
let questionsData = [];

/**
 * 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
  console.log('메모리얼 등록 페이지 초기화');

  // 서버 데이터 로드
  loadServerData();

  // DOM 준비 확인 후 이벤트 바인딩
  setTimeout(() => {
    // 이벤트 바인딩
    bindEvents();

    // 드래그 앤 드롭 초기화
    initializeDragAndDrop();

    // 파일 목록 컨테이너 초기 상태 설정
    initializeFileContainers();

    // 질문 이벤트 바인딩 (DOM 준비 후)
    bindQuestionEvents();

    // 기존 폼 데이터 로드
    loadExistingFormData();

    // 제출 버튼 초기 상태 설정
    updateSubmitButton();

    console.log('초기화 완료');
  }, 100);
});

/**
 * 서버 데이터 로드
 */
function loadServerData() {
  if (window.serverData && Array.isArray(window.serverData.questions) && window.serverData.questions.length > 0) {
    questionsData = window.serverData.questions;
    console.log('서버 데이터 로드 완료 - 질문 수:', questionsData.length);
  } else {
    // 서버 데이터가 없을 경우 DOM에서 질문 정보 추출
    const textareas = document.querySelectorAll('textarea[data-question-id]');
    questionsData = Array.from(textareas).map(textarea => ({
      id: parseInt(textarea.getAttribute('data-question-id'), 10),
      placeholderText: textarea.getAttribute('placeholder') || '',
      maxLength: parseInt(textarea.getAttribute('maxlength'), 10) || 0,
      minLength: parseInt(textarea.getAttribute('minlength'), 10) || 0,
      isRequired: textarea.hasAttribute('required')
    }));
    console.warn('서버 데이터 없음—DOM에서 로드된 질문 수:', questionsData.length);
  }
}

/**
 * ✅ 기존 폼 데이터 로드 (새로 추가)
 */
function loadExistingFormData() {
  // 기본 정보 로드
  const basicFields = ['name', 'nickname', 'birthDate', 'deathDate', 'relationship'];
  basicFields.forEach(field => {
    const element = document.getElementById(field);
    if (element && element.value) {
      memorialData[field] = element.value;
    }
  });

  // 성별 라디오 버튼 로드
  const genderRadio = document.querySelector('input[name="gender"]:checked');
  if (genderRadio) {
    memorialData.gender = genderRadio.value;
  }

  // 질문 답변 로드
  questionsData.forEach(question => {
    const textarea = document.getElementById(`question_${question.id}`);
    if (textarea && textarea.value.trim()) {
      updateQuestionAnswer(question.id, textarea.value);
    }
  });

  console.log('기존 폼 데이터 로드 완료:', memorialData);
}

/**
 * ✅ textarea 스타일 강제 적용 (간격 제거)
 */
function applyTextareaStyles(textarea) {
  textarea.style.textAlign = 'left';
  textarea.style.direction = 'ltr';
  textarea.style.unicodeBidi = 'embed';
  textarea.style.paddingLeft = '16px';
  textarea.style.paddingRight = '16px';
  textarea.style.textIndent = '0';
  textarea.style.margin = '0';
  textarea.style.whiteSpace = 'pre-wrap';
  textarea.style.wordBreak = 'break-word';
  textarea.style.overflowWrap = 'break-word';
}

/**
 * ✅ 실시간 검증 (부드러운 피드백)
 */
function performInstantValidation(question, value) {
  const fieldId = `question_${question.id}`;
  const textarea = document.getElementById(fieldId);
  const countElement = document.getElementById(`${fieldId}Count`);

  if (!textarea || !countElement) return;

  const currentLength = value.trim().length;
  const parentElement = countElement.parentElement;

  // 글자 수 색상 업데이트
  parentElement.classList.remove('warning', 'danger');

  if (currentLength > 0) {
    const percentage = (currentLength / question.maxLength) * 100;

    if (percentage >= 90) {
      parentElement.classList.add('danger');
    } else if (percentage >= 70) {
      parentElement.classList.add('warning');
    }
  }

  // 즉각적인 에러 체크
  if (currentLength > question.maxLength) {
    showQuestionError(fieldId, `최대 ${question.maxLength}자까지 입력 가능합니다.`);
  } else if (question.isRequired && currentLength === 0) {
    clearQuestionError(fieldId);
  } else {
    clearQuestionError(fieldId);
  }
}

/**
 * ✅ 질문 에러 초기화
 */
function clearQuestionError(fieldId) {
  const textarea = document.getElementById(fieldId);
  const errorElement = document.getElementById(`${fieldId}Error`);

  if (textarea) {
    textarea.classList.remove('is-invalid');
  }

  if (errorElement) {
    errorElement.style.display = 'none';
    errorElement.classList.remove('show');
    errorElement.textContent = '';
  }
}

/**
 * ✅ 질문 이벤트 바인딩 (강화된 버전)
 */
function bindQuestionEvents() {
  console.log('질문 이벤트 바인딩 시작, 질문 수:', questionsData.length);

  questionsData.forEach(question => {
    const fieldId = `question_${question.id}`;
    const textarea = document.getElementById(fieldId);
    const countElement = document.getElementById(`${fieldId}Count`);

    console.log(`질문 ${question.id} 바인딩 시도:`, { textarea: !!textarea, countElement: !!countElement });

    if (textarea && countElement) {
      // 스타일 강제 적용 (패딩·인덴트 초기화)
      applyTextareaStyles(textarea);

      // 초기 글자 수 표시
      updateQuestionCharacterCount(textarea, countElement, question);

      // 실시간 입력 이벤트 (디바운스 포함)
      let inputTimeout;
      textarea.addEventListener('input', e => {
        // 1) 선행 공백 제거
        e.target.value = e.target.value.replace(/^\s+/, '');

        // 2) trimmed 값을 기준으로 로깅 및 처리
        const value = e.target.value;
        console.log(`질문 ${question.id} 입력 이벤트:`, value.length, '자 (trimmed)');

        // 스타일 재적용
        applyTextareaStyles(e.target);

        // 답변 저장 (trimmed 값)
        updateQuestionAnswer(question.id, value.trim());

        // 글자 수 카운터 업데이트
        updateQuestionCharacterCount(textarea, countElement, question);

        // 디바운스된 실시간 검증
        clearTimeout(inputTimeout);
        inputTimeout = setTimeout(() => {
          performInstantValidation(question, value);
        }, 300);

        // 제출 버튼 상태 업데이트
        updateSubmitButton();
      });

      // 포커스 시 스타일·에러 초기화
      textarea.addEventListener('focus', e => {
        applyTextareaStyles(e.target);
        clearQuestionError(fieldId);
      });

      // 블러 시 유효성 검사
      textarea.addEventListener('blur', e => {
        validateQuestion(question, e.target.value);
      });

      console.log(`질문 ${question.id} 이벤트 바인딩 완료`);
    } else {
      console.warn(`질문 ${question.id} 요소를 찾을 수 없습니다:`, fieldId);
    }
  });
}

/**
 * 질문 글자 수 카운터 업데이트
 */
function updateQuestionCharacterCount(textarea, countElement, question) {
  // trim only leading/trailing whitespace so internal spaces still count
  const trimmedValue = textarea.value.trim();
  const currentLength = trimmedValue.length;
  countElement.textContent = currentLength;

  // (optional) if you want to also remove those leading spaces from the textarea itself:
  // textarea.value = trimmedValue;

  // 글자 수에 따른 색상 변경
  const parentElement = countElement.parentElement;
  parentElement.classList.remove('warning', 'danger');

  if (currentLength > 0) {
    const percentage = (currentLength / question.maxLength) * 100;

    if (percentage >= 90) {
      parentElement.classList.add('danger');
    } else if (percentage >= 70) {
      parentElement.classList.add('warning');
    }
  }
}

/**
 * ✅ 질문 답변 업데이트 (로깅 추가)
 */
function updateQuestionAnswer(questionId, answerText) {
  const existingIndex = memorialData.questionAnswers.findIndex(
    answer => answer.questionId === questionId
  );

  const answerDTO = {
    questionId: questionId,
    answerText: answerText ? answerText.trim() : ''
  };

  if (existingIndex >= 0) {
    memorialData.questionAnswers[existingIndex] = answerDTO;
  } else {
    memorialData.questionAnswers.push(answerDTO);
  }

  console.log(`질문 ${questionId} 답변 업데이트:`, answerDTO);
  console.log('현재 모든 답변:', memorialData.questionAnswers);
}

/**
 * 특정 질문의 답변 조회
 */
function getQuestionAnswer(questionId) {
  const answer = memorialData.questionAnswers.find(
    answer => answer.questionId === questionId
  );
  return answer ? answer.answerText : '';
}

/**
 * ✅ 질문 유효성 검사 (강화된 버전)
 */
function validateQuestion(question, value) {
  const fieldId = `question_${question.id}`;
  const textarea = document.getElementById(fieldId);

  if (!textarea) return true;

  // 트림된 값으로 검사
  const trimmedValue = value ? value.trim() : '';
  const currentLength = trimmedValue.length;

  // 에러 초기화
  clearQuestionError(fieldId);

  // 1. 필수 필드 검사
  if (question.isRequired && currentLength === 0) {
    showQuestionError(fieldId, '이 질문은 필수 입력입니다.');
    return false;
  }

  // 2. 내용이 있는 경우만 길이 검사
  if (currentLength > 0) {
    // 최소 길이 검사
    if (currentLength < question.minLength) {
      showQuestionError(fieldId, `최소 ${question.minLength}자 이상 입력해주세요. (현재: ${currentLength}자)`);
      return false;
    }

    // 최대 길이 검사
    if (currentLength > question.maxLength) {
      showQuestionError(fieldId, `최대 ${question.maxLength}자까지 입력 가능합니다. (현재: ${currentLength}자)`);
      return false;
    }
  }

  return true;
}

/**
 * ✅ 질문 에러 표시 (강화된 버전)
 */
function showQuestionError(fieldId, message) {
  const textarea = document.getElementById(fieldId);
  const errorElement = document.getElementById(`${fieldId}Error`);

  if (textarea) {
    textarea.classList.add('is-invalid');
  }

  if (errorElement) {
    errorElement.textContent = message;
    errorElement.style.display = 'block';
    errorElement.classList.add('show');
  }
}

/**
 * ✅ 모든 질문 유효성 검사 (강화된 버전)
 */
function validateAllQuestions() {
  let isValid = true;
  let firstInvalidField = null;

  questionsData.forEach(question => {
    const fieldId = `question_${question.id}`;
    const textarea = document.getElementById(fieldId);
    const value = textarea ? textarea.value : '';

    const questionValid = validateQuestion(question, value);
    if (!questionValid) {
      isValid = false;
      if (!firstInvalidField) {
        firstInvalidField = textarea;
      }
    }
  });

  // 첫 번째 오류 필드로 스크롤
  if (!isValid && firstInvalidField) {
    firstInvalidField.scrollIntoView({
      behavior: 'smooth',
      block: 'center'
    });
    firstInvalidField.focus();
  }

  return isValid;
}

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

  const basicInfoFields = [
    { id: 'name', type: 'input' },
    { id: 'nickname', type: 'input' },
    { id: 'birthDate', type: 'input' },
    { id: 'deathDate', type: 'input' },
    { id: 'relationship', type: 'select' }
  ];

  basicInfoFields.forEach(({ id, type }) => {
    const element = document.getElementById(id);
    if (element) {
      element.addEventListener('input', handleBasicInfoChange);
      element.addEventListener('change', handleBasicInfoChange);
      if (type === 'input') {
        element.addEventListener('blur', handleBasicInfoChange);
      }
    }
  });

  // 성별 라디오 버튼
  const genderInputs = document.querySelectorAll('input[name="gender"]');
  genderInputs.forEach(radio => {
    radio.addEventListener('change', handleBasicInfoChange);
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

    // 기존 모달 관련 처리
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

    // 기존 모달 푸터 버튼 처리
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
 * ✅ 기본 정보 변경 핸들러 (로깅 추가)
 */
function handleBasicInfoChange(event) {
  const { name, value, type } = event.target;

  if (type === 'radio') {
    memorialData[name] = value;
  } else {
    memorialData[name] = value;
  }

  console.log('기본 정보 변경:', name, '=', value);
  console.log('현재 memorialData:', memorialData);

  validateBasicInfo();
  updateSubmitButton();
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

  if (!checkFileLimit(files, type, multiple)) {
    return;
  }

  files.forEach(file => {
    if (validateFile(file, type)) {
      addFileToData(file, type);
    }
  });

  event.target.value = '';
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

  if (file.size > limits.maxSize) {
    const maxSizeMB = (limits.maxSize / (1024 * 1024)).toFixed(1);
    alert(`파일 크기가 ${maxSizeMB}MB를 초과합니다: ${file.name}`);
    return false;
  }

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
  const uploadAreaId = type === 'profileImages' ? 'profileImageUpload' :
      type === 'voiceFiles' ? 'voiceFileUpload' : 'videoFileUpload';
  const uploadArea = document.getElementById(uploadAreaId);
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

  updateFileList(type);
  updateSubmitButton();
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

  if (memorialData.name && memorialData.name.length < 2) {
    isValid = false;
  }

  if (memorialData.nickname && memorialData.nickname.length < 2) {
    isValid = false;
  }

  return isValid;
}

/**
 * 전체 데이터 유효성 검사
 */
function validateAllData() {
  if (!validateBasicInfo()) {
    alert('기본 정보를 모두 입력해주세요.');
    return false;
  }

  if (!validateAllQuestions()) {
    alert('필수 질문에 답변해주세요.');
    return false;
  }

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
  // 수정: 검증 대신 답변 존재 여부 확인
  const questionsValid = checkRequiredQuestionsAnswered();
  const profileImagesValid = memorialData.profileImages.length >= 5;
  const voiceFilesValid = memorialData.voiceFiles.length >= 3;
  const videoFileValid = memorialData.videoFile !== null;

  const isAllValid = basicInfoValid && questionsValid && profileImagesValid && voiceFilesValid && videoFileValid;

  submitBtn.disabled = !isAllValid;

  if (isAllValid) {
    submitBtn.innerHTML = '<i class="fas fa-upload"></i> 등록하기';
    submitBtn.classList.remove('btn-disabled');
  } else {
    let missingItems = [];
    if (!basicInfoValid) missingItems.push('기본정보');
    if (!questionsValid) missingItems.push('필수질문');
    if (!profileImagesValid) missingItems.push('사진');
    if (!voiceFilesValid) missingItems.push('음성');
    if (!videoFileValid) missingItems.push('영상');

    submitBtn.innerHTML = `<i class="fas fa-exclamation-triangle"></i> ${missingItems.join(', ')} 필요`;
    submitBtn.classList.add('btn-disabled');
  }
}

/**
 * 필수 질문 답변 여부 확인 (검증 없이 존재만 확인)
 */
function checkRequiredQuestionsAnswered() {
  const requiredQuestions = questionsData.filter(q => q.isRequired);

  for (const question of requiredQuestions) {
    const answer = memorialData.questionAnswers.find(a => a.questionId === question.id);
    if (!answer || !answer.answerText || answer.answerText.trim().length < question.minLength) {
      return false;
    }
  }

  return true;
}

/**
 * ✅ 최종 검증 수행
 */
function performFinalValidation() {
  const requiredQuestions = questionsData.filter(q => q.isRequired);

  for (const question of requiredQuestions) {
    const answer = memorialData.questionAnswers.find(a => a.questionId === question.id);
    if (!answer || !answer.answerText || answer.answerText.trim().length < question.minLength) {
      return {
        isValid: false,
        message: `"${question.questionText}" 질문에 최소 ${question.minLength}자 이상 답변해주세요.`
      };
    }
  }

  if (memorialData.profileImages.length !== 5) {
    return {
      isValid: false,
      message: '프로필 사진을 정확히 5장 업로드해주세요.'
    };
  }

  if (memorialData.voiceFiles.length !== 3) {
    return {
      isValid: false,
      message: '음성 파일을 정확히 3개 업로드해주세요.'
    };
  }

  if (!memorialData.videoFile) {
    return {
      isValid: false,
      message: '영상 파일을 1개 업로드해주세요.'
    };
  }

  return { isValid: true };
}

/**
 * ✅ FormData 생성 (로깅 강화)
 */
function createFormData() {
  const formData = new FormData();

  const basicInfo = {
    name: memorialData.name?.trim() || '',
    nickname: memorialData.nickname?.trim() || '',
    gender: memorialData.gender || '',
    birthDate: memorialData.birthDate || null,
    deathDate: memorialData.deathDate || null,
    relationship: memorialData.relationship || '',
    questionAnswers: memorialData.questionAnswers
      .filter(answer => answer.answerText && answer.answerText.trim().length > 0)
      .map(answer => ({
        questionId: answer.questionId,
        answerText: answer.answerText.trim()
      }))
  };

  console.log('FormData 생성 - 기본 정보:', basicInfo);
  console.log('FormData 생성 - 질문 답변:', basicInfo.questionAnswers);

  const blob = new Blob([JSON.stringify(basicInfo)], {
    type: 'application/json'
  });
  formData.append('memorialData', blob);

  memorialData.profileImages.forEach((fileInfo) => {
    formData.append('profileImages', fileInfo.file);
  });

  memorialData.voiceFiles.forEach((fileInfo) => {
    formData.append('voiceFiles', fileInfo.file);
  });

  if (memorialData.videoFile) {
    formData.append('videoFile', memorialData.videoFile.file);
  }

  return formData;
}

/**
 * ✅ 폼 제출 핸들러 (로깅 강화)
 */
async function handleFormSubmit(event) {
  event.preventDefault();

  console.log('=== 폼 제출 시작 ===');
  console.log('현재 memorialData:', memorialData);

  try {
    if (!validateAllData()) {
      return;
    }

    const finalValidation = performFinalValidation();
    if (!finalValidation.isValid) {
      alert(finalValidation.message);
      return;
    }

    const submitBtn = document.getElementById('submitBtn');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 등록 중...';

    const formData = createFormData();

    console.log('전송 데이터:', {
      basicInfo: memorialData,
      questionAnswers: memorialData.questionAnswers.length,
      profileImages: memorialData.profileImages.length,
      voiceFiles: memorialData.voiceFiles.length,
      videoFile: memorialData.videoFile ? 'Yes' : 'No'
    });

    const result = await authFetch('/api/memorials', {
      method: 'POST',
      body: formData
    });

    console.log('API 응답:', result);

    if (result.status?.code === 'OK_0000' && result.response?.success) {
      console.log('메모리얼 등록 성공:', result.response);
      alert(result.response.message || '메모리얼이 성공적으로 등록되었습니다!');
      window.location.href = '/mobile/home';
    } else {
      const errorMessage = result.status?.message || result.response?.message || '등록 중 오류가 발생했습니다.';
      console.error('서버 응답 오류:', result);
      throw new Error(errorMessage);
    }

  } catch (error) {
    console.error('폼 제출 실패:', error);

    if (error.name === 'FetchError') {
      alert(error.statusMessage || '서버 오류가 발생했습니다. 다시 시도해주세요.');
    } else {
      alert(error.message || '등록 중 오류가 발생했습니다. 다시 시도해주세요.');
    }
  } finally {
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