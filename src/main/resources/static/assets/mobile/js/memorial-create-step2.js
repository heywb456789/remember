// memorial-create-step2.js - 메모리얼 등록 2단계 JavaScript

import { authFetch, checkLoginStatus, handleFetchError } from './commonFetch.js';
import { showToast, showConfirm, showLoading, hideLoading } from './common.js';

// 메모리얼 등록 2단계 상태 관리
let memorialStep2State = {
  currentStep: 2,
  maxStep: 4,
  tempMemorialId: null,
  isLoading: false,
  isInitialized: false,
  formData: {
    personality: '',
    favoriteWords: '',
    speakingStyle: '',
    interests: [],
    otherInterest: '',
    specialMemories: '',
    familyMessages: ''
  },
  characterLimits: {
    personality: 500,
    favoriteWords: 300,
    speakingStyle: 300,
    specialMemories: 500,
    familyMessages: 300,
    otherInterest: 50
  },
  validation: {
    errors: {},
    isValid: true // 2단계는 선택사항이므로 기본값 true
  }
};

/**
 * 메모리얼 등록 2단계 초기화
 */
function initializeMemorialStep2() {
  console.log('🚀 메모리얼 등록 2단계 초기화 시작 (수정된 버전)');

  if (memorialStep2State.isInitialized) {
    console.warn('⚠️ 메모리얼 등록 2단계가 이미 초기화되었습니다.');
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

    // 4. 이벤트 바인딩 (수정된 버전)
    bindAllEvents();

    // 5. 추가 보안 처리
    addGlobalKeyboardHandlers();
    preventBrowserDefaults();
    addEventLogging();

    // 6. 글자 수 카운터 초기화
    initializeCharacterCounters();

    // 7. 관심사 이벤트 초기화
    initializeInterestEvents();

    // 8. 초기화 완료
    memorialStep2State.isInitialized = true;
    console.log('✅ 메모리얼 등록 2단계 초기화 완료 (키보드 이벤트 수정 적용)');

  } catch (error) {
    console.error('❌ 메모리얼 등록 2단계 초기화 실패:', error);
    showToast('페이지 초기화 중 오류가 발생했습니다.', 'error');
  }
}

/**
 * 서버 데이터 로드
 */
function loadServerData() {
  console.log('📊 서버 데이터 로드');

  if (window.memorialStep2Data) {
    memorialStep2State.currentStep = window.memorialStep2Data.currentStep || 2;
    memorialStep2State.maxStep = window.memorialStep2Data.maxStep || 4;
    memorialStep2State.tempMemorialId = window.memorialStep2Data.tempMemorialId || sessionStorage.getItem('tempMemorialId');

    // 기존 폼 데이터가 있다면 복원
    if (window.memorialStep2Data.formData) {
      memorialStep2State.formData = { ...memorialStep2State.formData, ...window.memorialStep2Data.formData };
    }

    console.log('📊 서버 데이터 로드 완료:', {
      currentStep: memorialStep2State.currentStep,
      tempMemorialId: memorialStep2State.tempMemorialId
    });
  }
}

/**
 * 폼 초기화
 */
function initializeForm() {
  console.log('📝 폼 초기화');

  const form = document.getElementById('memorialStep2Form');
  if (!form) {
    throw new Error('메모리얼 등록 2단계 폼을 찾을 수 없습니다.');
  }

  // 기존 데이터 복원
  restoreFormData();

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

  // 2. 입력 필드 이벤트
  bindInputEvents();

  // 3. 텍스트 영역 이벤트
  bindTextareaEvents();

  // 4. 모달 이벤트
  bindModalEvents();

  console.log('✅ 이벤트 바인딩 완료');
}

/**
 * 폼 제출 이벤트 바인딩
 */
function bindFormSubmit() {
  const form = document.getElementById('memorialStep2Form');
  if (!form) return;

  // 기존 이벤트 리스너 제거 (중복 방지)
  form.removeEventListener('submit', handleFormSubmit);

  // 새로운 이벤트 리스너 추가
  form.addEventListener('submit', function(e) {
    e.preventDefault();
    e.stopPropagation();
    handleFormSubmit(e);
  });

  // Enter 키로 인한 폼 제출 방지
  form.addEventListener('keydown', function(e) {
    if (e.keyCode === 13) {
      e.preventDefault();
      e.stopPropagation();
    }
  });

  console.log('📝 폼 제출 이벤트 바인딩 완료 (강화된 이벤트 처리)');
}

function addGlobalKeyboardHandlers() {
  // 문서 전체에서 숫자 키 이벤트 모니터링
  document.addEventListener('keydown', function(e) {
    // 입력 필드에 포커스가 있을 때만 처리
    const activeElement = document.activeElement;
    if (activeElement &&
        (activeElement.classList.contains('form-input') ||
         activeElement.classList.contains('form-textarea'))) {

      // 숫자 키 감지 시 이벤트 전파 중단
      if ((e.keyCode >= 48 && e.keyCode <= 57) ||
          (e.keyCode >= 96 && e.keyCode <= 105)) {
        e.stopPropagation();
      }
    }
  }, true); // 캡처 단계에서 처리

  console.log('🌐 전역 키보드 이벤트 핸들러 추가');
}

function preventBrowserDefaults() {
  // 브라우저의 기본 단축키 방지
  document.addEventListener('keydown', function(e) {
    // F5 새로고침 방지 (필요시)
    if (e.keyCode === 116) {
      if (confirm('페이지를 새로고침하시겠습니까? 입력하신 내용이 사라질 수 있습니다.')) {
        return true;
      }
      e.preventDefault();
      e.stopPropagation();
    }
  });

  // 뒤로가기 버튼 처리
  window.addEventListener('popstate', function(e) {
    if (confirm('이전 페이지로 이동하시겠습니까? 입력하신 내용이 사라질 수 있습니다.')) {
      return true;
    }
    e.preventDefault();
    history.pushState(null, null, window.location.href);
  });
}

function addEventLogging() {
  if (window.debugMode) {
    document.addEventListener('keydown', function(e) {
      console.log('🔍 키다운 이벤트:', {
        keyCode: e.keyCode,
        key: e.key,
        target: e.target.tagName,
        className: e.target.className
      });
    });

    // 페이지 이동 감지
    let originalPushState = history.pushState;
    history.pushState = function() {
      console.log('📍 페이지 이동 감지:', arguments);
      return originalPushState.apply(history, arguments);
    };
  }
}

/**
 * 입력 필드 이벤트 바인딩
 */
function bindInputEvents() {
  const inputs = document.querySelectorAll('.form-input, .form-textarea');

  inputs.forEach(input => {
    // 기존 이벤트 핸들러들
    input.addEventListener('input', handleInputChange);
    input.addEventListener('change', handleInputChange);
    input.addEventListener('focus', handleInputFocus);
    input.addEventListener('blur', handleInputBlur);

    // 추가: 키다운 이벤트에서 숫자 키 이벤트 전파 방지
    input.addEventListener('keydown', function(e) {
      // 숫자 키 (0-9) 또는 넘패드 숫자 키 감지
      if ((e.keyCode >= 48 && e.keyCode <= 57) ||
          (e.keyCode >= 96 && e.keyCode <= 105)) {
        // 이벤트 전파 중단 (페이지 이동 방지)
        e.stopPropagation();
      }

      // Enter 키 처리 (폼 제출 방지)
      if (e.keyCode === 13) {
        e.preventDefault();
        e.stopPropagation();
      }
    });

    // 추가: 키업 이벤트에서도 처리
    input.addEventListener('keyup', function(e) {
      if ((e.keyCode >= 48 && e.keyCode <= 57) ||
          (e.keyCode >= 96 && e.keyCode <= 105)) {
        e.stopPropagation();
      }
    });
  });

  console.log('✅ 입력 필드 이벤트 바인딩 완료 (키보드 이벤트 수정 적용)');
}

/**
 * 텍스트 영역 이벤트 바인딩
 */
function bindTextareaEvents() {
  const textareas = document.querySelectorAll('.form-textarea');

  textareas.forEach(textarea => {
    textarea.addEventListener('input', handleTextareaChange);
    textarea.addEventListener('focus', handleInputFocus);
    textarea.addEventListener('blur', handleInputBlur);
  });

  console.log('📝 텍스트 영역 이벤트 바인딩 완료');
}

/**
 * 모달 이벤트 바인딩
 */
function bindModalEvents() {
  const confirmSkipBtn = document.getElementById('confirmSkipBtn');
  if (confirmSkipBtn) {
    confirmSkipBtn.addEventListener('click', handleSkipConfirm);
  }

  console.log('📝 모달 이벤트 바인딩 완료');
}

/**
 * 글자 수 카운터 초기화
 */
function initializeCharacterCounters() {
  console.log('🔢 글자 수 카운터 초기화');

  const textareas = document.querySelectorAll('.form-textarea');
  textareas.forEach(textarea => {
    const countElement = document.getElementById(`${textarea.name}Count`);
    if (countElement) {
      updateCharacterCount(textarea, countElement);
    }
  });

  console.log('✅ 글자 수 카운터 초기화 완료');
}

/**
 * 관심사 이벤트 초기화
 */
function initializeInterestEvents() {
  console.log('🎯 관심사 이벤트 초기화');

  const interestCheckboxes = document.querySelectorAll('input[name="interests"]');
  interestCheckboxes.forEach(checkbox => {
    checkbox.addEventListener('change', handleInterestChange);
  });

  // 기타 관심사 토글 확인
  toggleOtherInterestField();

  console.log('✅ 관심사 이벤트 초기화 완료');
}

/**
 * 이벤트 핸들러들
 */

// 폼 제출 핸들러
async function handleFormSubmit(e) {
  e.preventDefault();
  console.log('📝 2단계 폼 제출 시도');

  if (memorialStep2State.isLoading) {
    return;
  }

  try {
    // 로딩 시작
    memorialStep2State.isLoading = true;
    showLoadingOverlay();

    // 폼 데이터 수집
    const formData = collectFormData();

    // 유효성 검사 (선택사항이므로 기본적으로 통과)
    if (!validateForm()) {
      return;
    }

    // 2단계 데이터 저장
    await saveStep2Data(formData);

    // 다음 단계로 이동
    await goToNextStep();

  } catch (error) {
    console.error('❌ 2단계 폼 제출 실패:', error);
    showToast('정보 저장 중 오류가 발생했습니다.', 'error');
  } finally {
    memorialStep2State.isLoading = false;
    hideLoadingOverlay();
  }
}

// 입력 변경 핸들러
function handleInputChange(e) {
  e.stopPropagation(); // 이벤트 전파 중단 추가

  const { name, value } = e.target;

  // 상태 업데이트
  memorialStep2State.formData[name] = value;

  // 로컬 스토리지에 저장
  localStorage.setItem('memorialStep2Data', JSON.stringify(memorialStep2State.formData));

  console.log(`📝 입력 변경: ${name} = ${value}`);
}

function addUnloadProtection() {
  window.addEventListener('beforeunload', function(e) {
    // 입력된 데이터가 있는 경우에만 경고
    const hasData = Object.values(memorialStep2State.formData).some(value =>
      value && value.toString().trim().length > 0
    );

    if (hasData) {
      const message = '입력하신 내용이 저장되지 않을 수 있습니다. 정말 페이지를 떠나시겠습니까?';
      e.returnValue = message;
      return message;
    }
  });
}


// 텍스트 영역 변경 핸들러
function handleTextareaChange(e) {
  e.stopPropagation(); // 이벤트 전파 중단 추가

  const { name, value } = e.target;
  const limit = memorialStep2State.characterLimits[name];

  // 글자 수 제한
  if (limit && value.length > limit) {
    e.target.value = value.slice(0, limit);
    showToast(`${limit}자까지 입력 가능합니다.`, 'warning');
  }

  // 상태 업데이트
  memorialStep2State.formData[name] = e.target.value;

  // 로컬 스토리지에 저장
  localStorage.setItem('memorialStep2Data', JSON.stringify(memorialStep2State.formData));

  // 글자 수 카운터 업데이트
  const countElement = document.getElementById(`${name}Count`);
  if (countElement) {
    updateCharacterCount(e.target, countElement);
  }

  console.log(`📝 텍스트 영역 변경: ${name} = ${e.target.value.length}자`);
}

// 입력 포커스 핸들러
function handleInputFocus(e) {
  const field = e.target;
  field.parentElement.classList.add('focused');
}

// 입력 블러 핸들러
function handleInputBlur(e) {
  const field = e.target;
  field.parentElement.classList.remove('focused');
}

// 관심사 변경 핸들러
function handleInterestChange(e) {
  const value = e.target.value;
  const isChecked = e.target.checked;

  if (isChecked) {
    // 관심사 추가
    if (!memorialStep2State.formData.interests.includes(value)) {
      memorialStep2State.formData.interests.push(value);
    }
  } else {
    // 관심사 제거
    const index = memorialStep2State.formData.interests.indexOf(value);
    if (index > -1) {
      memorialStep2State.formData.interests.splice(index, 1);
    }
  }

  // 기타 관심사 필드 토글
  toggleOtherInterestField();

  // 로컬 스토리지에 저장
  localStorage.setItem('memorialStep2Data', JSON.stringify(memorialStep2State.formData));

  console.log(`🎯 관심사 변경: ${value} = ${isChecked}`, memorialStep2State.formData.interests);
}

// 건너뛰기 확인 핸들러
function handleSkipConfirm() {
  console.log('⏭️ 2단계 건너뛰기 확인');

  // 모달 닫기
  const modal = bootstrap.Modal.getInstance(document.getElementById('skipModal'));
  if (modal) {
    modal.hide();
  }

  // 다음 단계로 이동
  setTimeout(() => {
    window.location.href = '/mobile/memorial/create/step3?skip=true';
  }, 500);
}

/**
 * 유효성 검사 함수들
 */

// 전체 폼 유효성 검사 (2단계는 선택사항)
function validateForm() {
  console.log('✅ 2단계 폼 유효성 검사');

  const errors = {};
  let isValid = true;

  // 글자 수 제한 확인
  Object.keys(memorialStep2State.characterLimits).forEach(fieldName => {
    const value = memorialStep2State.formData[fieldName];
    const limit = memorialStep2State.characterLimits[fieldName];

    if (value && value.length > limit) {
      errors[fieldName] = `${limit}자 이하로 입력해주세요.`;
      isValid = false;
    }
  });

  // 상태 업데이트
  memorialStep2State.validation.errors = errors;
  memorialStep2State.validation.isValid = isValid;

  console.log('✅ 2단계 폼 유효성 검사 완료:', { isValid, errors });
  return isValid;
}

/**
 * UI 업데이트 함수들
 */

// 글자 수 카운터 업데이트
function updateCharacterCount(textarea, countElement) {
  const currentLength = textarea.value.length;
  const limit = memorialStep2State.characterLimits[textarea.name];

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

// 기타 관심사 필드 토글
function toggleOtherInterestField() {
  const otherInterestGroup = document.getElementById('otherInterestGroup');
  const otherCheckbox = document.querySelector('input[name="interests"][value="OTHER"]');

  if (otherInterestGroup && otherCheckbox) {
    if (otherCheckbox.checked) {
      otherInterestGroup.style.display = 'block';
    } else {
      otherInterestGroup.style.display = 'none';
      // 기타 관심사 입력값 초기화
      const otherInput = document.getElementById('otherInterest');
      if (otherInput) {
        otherInput.value = '';
        memorialStep2State.formData.otherInterest = '';
      }
    }
  }
}

// 진행 상태 업데이트
function updateProgressBar() {
  const progressFill = document.querySelector('.progress-fill');
  if (progressFill) {
    const percentage = (memorialStep2State.currentStep / memorialStep2State.maxStep) * 100;
    progressFill.style.width = `${percentage}%`;
  }

  // 활성 단계 업데이트
  document.querySelectorAll('.step').forEach((step, index) => {
    step.classList.remove('active', 'completed');

    if (index < memorialStep2State.currentStep - 1) {
      step.classList.add('completed');
    } else if (index === memorialStep2State.currentStep - 1) {
      step.classList.add('active');
    }
  });
}

/**
 * 데이터 관리 함수들
 */

// 폼 데이터 수집
function collectFormData() {
  const formData = {
    personality: document.getElementById('personality')?.value?.trim() || '',
    favoriteWords: document.getElementById('favoriteWords')?.value?.trim() || '',
    speakingStyle: document.getElementById('speakingStyle')?.value?.trim() || '',
    interests: [...memorialStep2State.formData.interests],
    otherInterest: document.getElementById('otherInterest')?.value?.trim() || '',
    specialMemories: document.getElementById('specialMemories')?.value?.trim() || '',
    familyMessages: document.getElementById('familyMessages')?.value?.trim() || ''
  };

  // 상태 업데이트
  memorialStep2State.formData = formData;

  console.log('📊 2단계 폼 데이터 수집:', formData);
  return formData;
}

// 폼 데이터 복원
function restoreFormData() {
  // 로컬 스토리지에서 데이터 복원
  const savedData = localStorage.getItem('memorialStep2Data');
  if (savedData) {
    try {
      const parsedData = JSON.parse(savedData);
      memorialStep2State.formData = { ...memorialStep2State.formData, ...parsedData };
    } catch (error) {
      console.warn('⚠️ 저장된 데이터 복원 실패:', error);
    }
  }

  // UI에 데이터 적용
  Object.keys(memorialStep2State.formData).forEach(key => {
    const value = memorialStep2State.formData[key];
    if (!value) return;

    if (key === 'interests' && Array.isArray(value)) {
      // 관심사 복원
      value.forEach(interest => {
        const checkbox = document.querySelector(`input[name="interests"][value="${interest}"]`);
        if (checkbox) {
          checkbox.checked = true;
        }
      });
      toggleOtherInterestField();
    } else {
      // 일반 입력 필드 복원
      const input = document.getElementById(key);
      if (input) {
        input.value = value;

        // 텍스트 영역인 경우 글자 수 카운터 업데이트
        if (input.classList.contains('form-textarea')) {
          const countElement = document.getElementById(`${key}Count`);
          if (countElement) {
            updateCharacterCount(input, countElement);
          }
        }
      }
    }
  });

  console.log('📊 2단계 폼 데이터 복원 완료');
}

/**
 * API 통신 함수들
 */

// 2단계 데이터 저장
async function saveStep2Data(formData) {
  console.log('💾 2단계 데이터 저장 시도');

  try {
    const requestData = {
      ...formData,
      tempMemorialId: memorialStep2State.tempMemorialId
    };

    const response = await authFetch('/api/memorial/create/step2', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestData)
    });

    if (!response.ok) {
      throw new Error('2단계 데이터 저장 실패');
    }

    const result = await response.json();
    console.log('✅ 2단계 데이터 저장 성공:', result);

    return result;

  } catch (error) {
    console.error('❌ 2단계 데이터 저장 실패:', error);
    throw error;
  }
}

// 다음 단계로 이동
async function goToNextStep() {
  console.log('➡️ 다음 단계로 이동');

  try {
    // 로컬 스토리지 정리
    localStorage.removeItem('memorialStep2Data');

    // 성공 메시지
    showToast('고인 정보가 저장되었습니다.', 'success');

    // 잠시 후 다음 페이지로 이동
    setTimeout(() => {
      window.location.href = '/mobile/memorial/create/step3';
    }, 1000);

  } catch (error) {
    console.error('❌ 다음 단계 이동 실패:', error);
    throw error;
  }
}

/**
 * 모달 및 오버레이 함수들
 */

// 로딩 오버레이 표시
function showLoadingOverlay() {
  const overlay = document.getElementById('loadingOverlay');
  if (overlay) {
    overlay.style.display = 'flex';
  }
}

// 로딩 오버레이 숨김
function hideLoadingOverlay() {
  const overlay = document.getElementById('loadingOverlay');
  if (overlay) {
    overlay.style.display = 'none';
  }
}

// 건너뛰기 모달 표시
function showSkipModal() {
  const modal = document.getElementById('skipModal');
  if (modal) {
    const bootstrapModal = new bootstrap.Modal(modal);
    bootstrapModal.show();
  }
}

// 정보 안내 모달 표시
function showInfoModal() {
  const modal = document.getElementById('infoModal');
  if (modal) {
    const bootstrapModal = new bootstrap.Modal(modal);
    bootstrapModal.show();
  }
}

/**
 * 정리 함수
 */
function destroyMemorialStep2() {
  console.log('🗑️ 메모리얼 등록 2단계 정리');

  memorialStep2State.isInitialized = false;
}

/**
 * 전역 함수들
 */
window.memorialStep2Manager = {
  initialize: initializeMemorialStep2,
  validateForm,
  collectFormData,
  showSkipModal,
  showInfoModal,
  destroy: destroyMemorialStep2,
  getState: () => memorialStep2State
};

// 전역 함수들 (HTML에서 호출 가능)
window.showMemorialInfoModal = showInfoModal;
window.showMemorialSkipModal = showSkipModal;
window.saveMemorialStep2 = saveStep2Data;

/**
 * 자동 초기화
 */
console.log('🌟 memorial-create-step2.js 로드 완료');

// DOM이 준비되면 초기화
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', function() {
    initializeMemorialStep2();
    addUnloadProtection();
  });
} else {
  setTimeout(function() {
    initializeMemorialStep2();
    addUnloadProtection();
  }, 100);
}

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', destroyMemorialStep2);

// 모듈 익스포트
export {
  initializeMemorialStep2,
  validateForm,
  collectFormData,
  saveStep2Data,
  destroyMemorialStep2
};