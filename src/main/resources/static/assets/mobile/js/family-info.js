// family-info.js - 고인 상세 정보 페이지 JavaScript (메모리얼 통일 스타일)
import { authFetch } from './commonFetch.js';
import { showToast, showConfirm, showLoading, hideLoading } from './common.js';

// 페이지 상태 관리
let pageState = {
  isLoading: false,
  isViewMode: false,
  memorialId: null,
  familyInfo: null,
  formData: {
    personality: '',
    hobbies: '',
    favoriteFood: '',
    specialMemories: '',
    speechHabits: ''
  }
};

// 필드 제한 설정
const fieldLimits = {
  personality: { max: 500, min: 10 },
  hobbies: { max: 300, min: 5 },
  favoriteFood: { max: 300, min: 5 },
  specialMemories: { max: 300, min: 5 },
  speechHabits: { max: 300, min: 5 }
};

/**
 * 페이지 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
  console.log('고인 상세 정보 페이지 초기화');

  try {
    // 서버 데이터 로드
    loadServerData();

    // 이벤트 바인딩
    bindEvents();

    // 입력 모드인 경우 폼 초기화
    if (!pageState.isViewMode) {
      initializeForm();
    }

    console.log('페이지 초기화 완료');
  } catch (error) {
    console.error('페이지 초기화 실패:', error);
    showToast('페이지 초기화에 실패했습니다.', 'error');
  }
});

/**
 * 서버 데이터 로드
 */
function loadServerData() {
  if (window.familyInfoData) {
    pageState.memorialId = window.familyInfoData.memorialId;
    pageState.isViewMode = window.familyInfoData.isViewMode;
    pageState.familyInfo = window.familyInfoData.familyInfo;

    console.log('서버 데이터 로드 완료:', {
      memorialId: pageState.memorialId,
      isViewMode: pageState.isViewMode
    });
  } else {
    console.warn('서버 데이터가 없습니다.');
  }
}

/**
 * 이벤트 바인딩 (memorial-create.js 방식)
 */
function bindEvents() {
  // 통합된 클릭 이벤트 처리
  document.addEventListener('click', function(e) {
    const action = e.target.getAttribute('data-action') || e.target.closest('[data-action]')?.getAttribute('data-action');

    if (!action) return;

    switch(action) {
      case 'go-back':
        e.preventDefault();
        handleGoBack();
        break;
      case 'start-video-call':
        e.preventDefault();
        handleStartVideoCall();
        break;
    }
  });

  // 입력 모드인 경우에만 폼 이벤트 바인딩
  if (!pageState.isViewMode) {
    bindFormEvents();
  }
}

/**
 * 폼 이벤트 바인딩
 */
function bindFormEvents() {
  // 폼 제출
  const form = document.getElementById('familyInfoForm');
  if (form) {
    form.addEventListener('submit', handleFormSubmit);
  }

  // 텍스트 영역 이벤트
  const textareas = ['personality', 'hobbies', 'favoriteFood', 'specialMemories', 'speechHabits'];
  textareas.forEach(fieldName => {
    const textarea = document.getElementById(fieldName);
    const countElement = document.getElementById(`${fieldName}Count`);

    if (textarea && countElement) {
      textarea.addEventListener('input', (e) => {
        handleTextareaChange(fieldName, e.target.value);
        updateCharacterCount(textarea, countElement);
      });

      textarea.addEventListener('blur', (e) => {
        validateField(fieldName, e.target.value);
      });
    }
  });
}

/**
 * 폼 초기화
 */
function initializeForm() {
  console.log('폼 초기화');

  // 가족 구성원은 빈 폼으로 시작 (기존 데이터 미반영)
  const fields = ['personality', 'hobbies', 'favoriteFood', 'specialMemories', 'speechHabits'];
  fields.forEach(fieldName => {
    const textarea = document.getElementById(fieldName);

    if (textarea) {
      // 빈 값으로 초기화
      textarea.value = '';
      pageState.formData[fieldName] = '';

      // 글자 수 카운터 초기화
      const countElement = document.getElementById(`${fieldName}Count`);
      if (countElement) {
        updateCharacterCount(textarea, countElement);
      }
    }
  });

  // 제출 버튼 상태 업데이트
  updateSubmitButton();
}

/**
 * 텍스트 영역 변경 핸들러
 */
function handleTextareaChange(fieldName, value) {
  pageState.formData[fieldName] = value;
  updateSubmitButton();
}

/**
 * 글자 수 카운터 업데이트 (memorial-create.js 방식)
 */
function updateCharacterCount(textarea, countElement) {
  const currentLength = textarea.value.length;
  const fieldName = textarea.name;
  const limits = fieldLimits[fieldName];

  if (!limits) return;

  countElement.textContent = currentLength;

  // 글자 수에 따른 색상 변경
  const parentElement = countElement.parentElement;
  parentElement.classList.remove('warning', 'danger');

  const percentage = (currentLength / limits.max) * 100;

  if (percentage >= 90) {
    parentElement.classList.add('danger');
  } else if (percentage >= 70) {
    parentElement.classList.add('warning');
  }
}

/**
 * 필드 유효성 검사
 */
function validateField(fieldName, value) {
  const limits = fieldLimits[fieldName];
  const textarea = document.getElementById(fieldName);
  const errorElement = document.getElementById(`${fieldName}Error`);

  // 에러 초기화
  if (errorElement) {
    errorElement.classList.remove('show');
    errorElement.textContent = '';
  }

  if (textarea) {
    textarea.classList.remove('is-invalid');
  }

  // 필수 필드 확인
  if (!value || value.trim().length === 0) {
    showFieldError(fieldName, '이 필드는 필수입니다.');
    return false;
  }

  // 최소 길이 확인
  if (value.trim().length < limits.min) {
    showFieldError(fieldName, `최소 ${limits.min}자 이상 입력해주세요.`);
    return false;
  }

  // 최대 길이 확인
  if (value.length > limits.max) {
    showFieldError(fieldName, `최대 ${limits.max}자까지 입력 가능합니다.`);
    return false;
  }

  return true;
}

/**
 * 필드 에러 표시
 */
function showFieldError(fieldName, message) {
  const textarea = document.getElementById(fieldName);
  const errorElement = document.getElementById(`${fieldName}Error`);

  if (textarea) {
    textarea.classList.add('is-invalid');
  }

  if (errorElement) {
    errorElement.textContent = message;
    errorElement.classList.add('show');
  }
}

/**
 * 전체 폼 유효성 검사
 */
function validateAllData() {
  const fields = ['personality', 'hobbies', 'favoriteFood', 'specialMemories', 'speechHabits'];
  let isValid = true;

  fields.forEach(fieldName => {
    const value = pageState.formData[fieldName];
    const fieldValid = validateField(fieldName, value);

    if (!fieldValid) {
      isValid = false;
    }
  });

  return isValid;
}

/**
 * 제출 버튼 상태 업데이트 (memorial-create.js 방식)
 */
function updateSubmitButton() {
  const submitBtn = document.getElementById('submitBtn');
  if (!submitBtn) return;

  const fields = ['personality', 'hobbies', 'favoriteFood', 'specialMemories', 'speechHabits'];
  const allFilled = fields.every(fieldName => {
    const value = pageState.formData[fieldName];
    return value && value.trim().length > 0;
  });

  const isFormValid = allFilled;

  submitBtn.disabled = !isFormValid;

  if (isFormValid) {
    submitBtn.innerHTML = '<i class="fas fa-save"></i> 저장하기';
    submitBtn.classList.remove('btn-disabled');
  } else {
    submitBtn.innerHTML = '<i class="fas fa-exclamation-triangle"></i> 모든 정보를 입력해주세요';
    submitBtn.classList.add('btn-disabled');
  }
}

/**
 * 폼 제출 핸들러 (memorial-create.js 방식)
 */
async function handleFormSubmit(event) {
  event.preventDefault();

  console.log('폼 제출 시작');

  if (pageState.isLoading) {
    return;
  }

  try {
    // 최종 유효성 검사
    if (!validateAllData()) {
      showToast('입력 정보를 확인해주세요.', 'warning');
      return;
    }

    // 확인 다이얼로그
    const confirmed = await showConfirm(
      '고인 상세 정보 저장',
      '입력하신 정보를 저장하시겠습니까?',
      '저장하기',
      '취소'
    );

    if (!confirmed) {
      return;
    }

    // 로딩 시작
    pageState.isLoading = true;
    const loading = showLoading('정보를 저장하는 중...');

    // 제출 버튼 상태 변경
    const submitBtn = document.getElementById('submitBtn');
    const originalText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 저장 중...';

    // API 호출
    const response = await authFetch(`/api/memorial/${pageState.memorialId}/family-info`, {
      method: 'POST',
      body: JSON.stringify(pageState.formData)
    });

    console.log('API 응답:', response);

    if (response.status?.code === 'OK_0000') {
      showToast('고인 상세 정보가 저장되었습니다.', 'success');

      // 3초 후 메인 페이지로 이동
      setTimeout(() => {
        window.location.href = '/mobile/home';
      }, 3000);
    } else {
      throw new Error(response.status?.message || '저장에 실패했습니다.');
    }

  } catch (error) {
    console.error('폼 제출 실패:', error);

    // FetchError인 경우 적절한 메시지 표시
    if (error.name === 'FetchError') {
      showToast(error.statusMessage || '서버 오류가 발생했습니다. 다시 시도해주세요.', 'error');
    } else {
      showToast(error.message || '저장 중 오류가 발생했습니다. 다시 시도해주세요.', 'error');
    }

  } finally {
    // 로딩 종료
    pageState.isLoading = false;
    hideLoading();

    // 제출 버튼 상태 복원
    updateSubmitButton();
  }
}

/**
 * 뒤로가기 핸들러 (memorial-create.js 방식)
 */
function handleGoBack() {
  console.log('뒤로가기 클릭');

  // 입력 모드에서 내용이 있는 경우 확인
  if (!pageState.isViewMode) {
    const hasContent = Object.values(pageState.formData).some(value => value && value.trim().length > 0);

    if (hasContent) {
      showConfirm(
        '페이지 나가기',
        '입력하신 내용이 저장되지 않습니다.\n\n정말 나가시겠습니까?',
        '나가기',
        '취소'
      ).then(confirmed => {
        if (confirmed) {
          window.history.back();
        }
      });
      return;
    }
  }

  window.history.back();
}

/**
 * 영상통화 시작 핸들러
 */
function handleStartVideoCall() {
  console.log('영상통화 시작 클릭');

  if (!pageState.memorialId) {
    showToast('메모리얼 정보를 찾을 수 없습니다.', 'error');
    return;
  }

  // 영상통화 페이지로 이동
  window.location.href = `/mobile/videocall/${pageState.memorialId}`;
}

// 전역 함수 등록
window.familyInfoManager = {
  getState: () => pageState,
  validateForm: validateAllData,
  handleFormSubmit,
  handleGoBack,
  handleStartVideoCall
};

console.log('family-info.js 로드 완료');