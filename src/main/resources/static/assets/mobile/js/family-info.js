// family-info.js - 가족 구성원용 고인 상세 정보 입력 페이지

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
  },
  validation: {
    personality: false,
    hobbies: false,
    favoriteFood: false,
    specialMemories: false,
    speechHabits: false
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
  console.log('가족 구성원 고인 상세 정보 페이지 초기화');

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
      isViewMode: pageState.isViewMode,
      completionPercent: pageState.familyInfo?.completionPercent
    });
  } else {
    console.warn('서버 데이터가 없습니다.');
  }
}

/**
 * 이벤트 바인딩
 */
function bindEvents() {
  // 뒤로가기 버튼
  const backBtns = document.querySelectorAll('[data-action="go-back"]');
  backBtns.forEach(btn => {
    btn.addEventListener('click', handleGoBack);
  });

  // 입력 모드인 경우에만 폼 이벤트 바인딩
  if (!pageState.isViewMode) {
    bindFormEvents();
  }

  // 조회 모드인 경우 영상통화 버튼 바인딩
  if (pageState.isViewMode) {
    const videoCallBtn = document.querySelector('[data-action="start-video-call"]');
    if (videoCallBtn) {
      videoCallBtn.addEventListener('click', handleStartVideoCall);
    }
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
        handleTextareaInput(fieldName, e.target.value);
        updateCharacterCount(fieldName, e.target.value, countElement);
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

  // 기존 데이터가 있는 경우 폼에 설정
  if (pageState.familyInfo) {
    const fields = ['personality', 'hobbies', 'favoriteFood', 'specialMemories', 'speechHabits'];
    fields.forEach(fieldName => {
      const textarea = document.getElementById(fieldName);
      const value = pageState.familyInfo[fieldName] || '';

      if (textarea) {
        textarea.value = value;
        pageState.formData[fieldName] = value;

        // 글자 수 업데이트
        const countElement = document.getElementById(`${fieldName}Count`);
        if (countElement) {
          updateCharacterCount(fieldName, value, countElement);
        }

        // 유효성 검사
        validateField(fieldName, value);
      }
    });
  }

  // 제출 버튼 상태 업데이트
  updateSubmitButton();
}

/**
 * 텍스트 영역 입력 핸들러
 */
function handleTextareaInput(fieldName, value) {
  pageState.formData[fieldName] = value;

  // 실시간 유효성 검사
  const isValid = validateField(fieldName, value);
  pageState.validation[fieldName] = isValid;

  // 제출 버튼 상태 업데이트
  updateSubmitButton();
}

/**
 * 글자 수 카운터 업데이트
 */
function updateCharacterCount(fieldName, value, countElement) {
  const currentLength = value.length;
  const maxLength = fieldLimits[fieldName].max;

  countElement.textContent = currentLength;

  // 글자 수에 따른 색상 변경
  const parentElement = countElement.parentElement;
  parentElement.classList.remove('warning', 'danger');

  const percentage = (currentLength / maxLength) * 100;

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
    textarea.classList.remove('error');
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
    textarea.classList.add('error');
  }

  if (errorElement) {
    errorElement.textContent = message;
    errorElement.classList.add('show');
  }
}

/**
 * 전체 폼 유효성 검사
 */
function validateForm() {
  const fields = ['personality', 'hobbies', 'favoriteFood', 'specialMemories', 'speechHabits'];
  let isValid = true;

  fields.forEach(fieldName => {
    const value = pageState.formData[fieldName];
    const fieldValid = validateField(fieldName, value);

    if (!fieldValid) {
      isValid = false;
    }

    pageState.validation[fieldName] = fieldValid;
  });

  return isValid;
}

/**
 * 제출 버튼 상태 업데이트
 */
function updateSubmitButton() {
  const submitBtn = document.getElementById('submitBtn');
  if (!submitBtn) return;

  const allValid = Object.values(pageState.validation).every(valid => valid === true);
  const allFilled = Object.values(pageState.formData).every(value => value && value.trim().length > 0);

  const isFormValid = allValid && allFilled;

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
 * 폼 제출 핸들러
 */
async function handleFormSubmit(event) {
  event.preventDefault();

  console.log('폼 제출 시작');

  if (pageState.isLoading) {
    return;
  }

  try {
    // 최종 유효성 검사
    if (!validateForm()) {
      showToast('입력 정보를 확인해주세요.', 'warning');
      return;
    }

    // 확인 다이얼로그
    const confirmed = await showConfirm(
      '고인 상세 정보 저장',
      '입력하신 정보를 저장하시겠습니까?\n\n저장 후에는 수정할 수 없습니다.',
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
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 저장 중...';
    }

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

    const errorMessage = error.name === 'FetchError' ?
      error.statusMessage :
      (error.message || '저장 중 오류가 발생했습니다.');

    showToast(errorMessage, 'error');

  } finally {
    // 로딩 종료
    pageState.isLoading = false;
    hideLoading();

    // 제출 버튼 상태 복원
    updateSubmitButton();
  }
}

/**
 * 뒤로가기 핸들러
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

/**
 * 진행 상황 업데이트
 */
function updateProgress() {
  const progressBar = document.querySelector('.progress-fill');
  const progressCount = document.querySelector('.progress-count');

  if (!progressBar || !progressCount) return;

  const filledCount = Object.values(pageState.formData).filter(value => value && value.trim().length > 0).length;
  const totalCount = 5;
  const percentage = (filledCount / totalCount) * 100;

  progressBar.style.width = `${percentage}%`;
  progressCount.textContent = `${filledCount}/${totalCount}`;
}

/**
 * 실시간 진행 상황 업데이트
 */
function updateProgressInRealTime() {
  if (!pageState.isViewMode) {
    updateProgress();
  }
}

// 전역 함수 등록
window.familyInfoManager = {
  getState: () => pageState,
  validateForm,
  updateProgress: updateProgressInRealTime
};

console.log('family-info.js 로드 완료');