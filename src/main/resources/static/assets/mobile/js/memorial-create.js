// memorial-create-step1.js - 메모리얼 등록 1단계 JavaScript

import { authFetch, checkLoginStatus, handleFetchError } from './commonFetch.js';
import { showToast, showConfirm, showLoading, hideLoading } from './common.js';

// 메모리얼 등록 1단계 상태 관리
let memorialStep1State = {
  currentStep: 1,
  maxStep: 4,
  isLoading: false,
  isInitialized: false,
  tempMemorialId: null,
  formData: {
    name: '',
    description: '',
    isPublic: false,
    deceasedName: '',
    gender: '',
    relationship: '',
    birthDate: '',
    deathDate: ''
  },
  validation: {
    errors: {},
    isValid: false
  }
};

/**
 * 메모리얼 등록 1단계 초기화
 */
function initializeMemorialStep1() {
  console.log('🚀 메모리얼 등록 1단계 초기화 시작');

  if (memorialStep1State.isInitialized) {
    console.warn('⚠️ 메모리얼 등록 1단계가 이미 초기화되었습니다.');
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

    // 5. 유효성 검사 규칙 설정
    setupValidationRules();

    // 6. 초기화 완료
    memorialStep1State.isInitialized = true;
    console.log('✅ 메모리얼 등록 1단계 초기화 완료');

  } catch (error) {
    console.error('❌ 메모리얼 등록 1단계 초기화 실패:', error);
    showToast('페이지 초기화 중 오류가 발생했습니다.', 'error');
  }
}

/**
 * 서버 데이터 로드
 */
function loadServerData() {
  console.log('📊 서버 데이터 로드');

  if (window.memorialStep1Data) {
    memorialStep1State.currentStep = window.memorialStep1Data.currentStep || 1;
    memorialStep1State.maxStep = window.memorialStep1Data.maxStep || 4;

    // 기존 폼 데이터가 있다면 복원
    if (window.memorialStep1Data.formData) {
      memorialStep1State.formData = { ...memorialStep1State.formData, ...window.memorialStep1Data.formData };
    }

    console.log('📊 서버 데이터 로드 완료:', {
      currentStep: memorialStep1State.currentStep,
      maxStep: memorialStep1State.maxStep
    });
  }
}

/**
 * 폼 초기화
 */
function initializeForm() {
  console.log('📝 폼 초기화');

  const form = document.getElementById('memorialCreateForm');
  if (!form) {
    throw new Error('메모리얼 등록 폼을 찾을 수 없습니다.');
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

  // 3. 실시간 유효성 검사
  bindValidationEvents();

  // 4. 날짜 유효성 검사
  bindDateValidation();

  console.log('✅ 이벤트 바인딩 완료');
}

/**
 * 폼 제출 이벤트 바인딩
 */
function bindFormSubmit() {
  const form = document.getElementById('memorialCreateForm');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    if (memorialStep1State.isLoading) {
      console.log('⏳ 이미 처리 중입니다.');
      return;
    }

    await handleFormSubmit();
  });

  console.log('📝 폼 제출 이벤트 바인딩 완료');
}

/**
 * 입력 필드 이벤트 바인딩
 */
function bindInputEvents() {
  // 텍스트 입력 필드
  const textInputs = ['memorialName', 'memorialDescription', 'deceasedName'];
  textInputs.forEach(id => {
    const input = document.getElementById(id);
    if (input) {
      input.addEventListener('input', handleInputChange);
      input.addEventListener('blur', handleInputBlur);
    }
  });

  // 라디오 버튼 (성별, 공개설정)
  const radioInputs = document.querySelectorAll('input[type="radio"]');
  radioInputs.forEach(radio => {
    radio.addEventListener('change', handleInputChange);
  });

  // 셀렉트 박스
  const selectInputs = ['relationship'];
  selectInputs.forEach(id => {
    const select = document.getElementById(id);
    if (select) {
      select.addEventListener('change', handleInputChange);
    }
  });

  // 날짜 입력
  const dateInputs = ['birthDate', 'deathDate'];
  dateInputs.forEach(id => {
    const input = document.getElementById(id);
    if (input) {
      input.addEventListener('change', handleInputChange);
    }
  });

  console.log('🔤 입력 필드 이벤트 바인딩 완료');
}

/**
 * 실시간 유효성 검사 바인딩
 */
function bindValidationEvents() {
  const requiredFields = ['memorialName', 'deceasedName', 'gender', 'relationship'];

  requiredFields.forEach(fieldName => {
    const field = document.querySelector(`[name="${fieldName}"]`);
    if (field) {
      field.addEventListener('blur', () => validateField(fieldName));
    }
  });

  console.log('✅ 실시간 유효성 검사 바인딩 완료');
}

/**
 * 날짜 유효성 검사 바인딩
 */
function bindDateValidation() {
  const birthDateInput = document.getElementById('birthDate');
  const deathDateInput = document.getElementById('deathDate');

  if (birthDateInput && deathDateInput) {
    birthDateInput.addEventListener('change', validateDateRange);
    deathDateInput.addEventListener('change', validateDateRange);
  }

  console.log('📅 날짜 유효성 검사 바인딩 완료');
}

/**
 * 입력 변경 이벤트 핸들러
 */
function handleInputChange(event) {
  const { name, value, type, checked } = event.target;

  if (type === 'radio') {
    memorialStep1State.formData[name] = value;
  } else if (type === 'checkbox') {
    memorialStep1State.formData[name] = checked;
  } else {
    memorialStep1State.formData[name] = value;
  }

  // 실시간 유효성 검사
  if (name && memorialStep1State.validation.errors[name]) {
    validateField(name);
  }

  console.log(`📝 입력 변경: ${name} = ${value}`);
}

/**
 * 입력 포커스 아웃 이벤트 핸들러
 */
function handleInputBlur(event) {
  const { name } = event.target;
  if (name) {
    validateField(name);
  }
}

/**
 * 폼 제출 핸들러
 */
async function handleFormSubmit() {
  console.log('📤 폼 제출 시작');

  try {
    memorialStep1State.isLoading = true;
    showLoadingOverlay();

    // 1. 폼 데이터 수집
    const formData = collectFormData();
    console.log('📊 수집된 폼 데이터:', formData);

    // 2. 유효성 검사
    if (!validateForm()) {
      console.log('❌ 유효성 검사 실패');
      return;
    }

    // 3. 1단계 데이터 저장
    await saveStep1Data(formData);

    // 4. 다음 단계로 이동
    await moveToNextStep();

  } catch (error) {
    console.error('❌ 폼 제출 실패:', error);
    handleFetchError(error);
  } finally {
    memorialStep1State.isLoading = false;
    hideLoadingOverlay();
  }
}

/**
 * 폼 데이터 수집
 */
function collectFormData() {
  const formData = {
    // 기본 정보
    name: getValue('memorialName'),
    description: getValue('memorialDescription'),
    isPublic: getRadioValue('isPublic') === 'true',

    // 고인 정보 (1단계에서 함께 수집)
    deceasedName: getValue('deceasedName'),
    gender: getRadioValue('gender'),
    relationship: getValue('relationship'),
    birthDate: getValue('birthDate') || null,
    deathDate: getValue('deathDate') || null
  };

  // 상태 업데이트
  memorialStep1State.formData = { ...memorialStep1State.formData, ...formData };

  return formData;
}

/**
 * 유효성 검사
 */
function validateForm() {
  console.log('✅ 전체 유효성 검사 시작');

  const requiredFields = ['name', 'deceasedName', 'gender', 'relationship'];
  let isValid = true;

  // 필수 필드 검사
  requiredFields.forEach(fieldName => {
    if (!validateField(fieldName)) {
      isValid = false;
    }
  });

  // 날짜 범위 검사
  if (!validateDateRange()) {
    isValid = false;
  }

  memorialStep1State.validation.isValid = isValid;
  console.log(`✅ 전체 유효성 검사 결과: ${isValid ? '성공' : '실패'}`);

  return isValid;
}

/**
 * 개별 필드 유효성 검사
 */
function validateField(fieldName) {
  const value = memorialStep1State.formData[fieldName] || getValue(fieldName);
  let isValid = true;
  let errorMessage = '';

  switch (fieldName) {
    case 'name':
      if (!value || value.trim().length < 2) {
        isValid = false;
        errorMessage = '메모리얼 이름은 2자 이상 입력해주세요.';
      } else if (value.length > 50) {
        isValid = false;
        errorMessage = '메모리얼 이름은 50자 이하로 입력해주세요.';
      }
      break;

    case 'deceasedName':
      if (!value || value.trim().length < 2) {
        isValid = false;
        errorMessage = '고인 이름은 2자 이상 입력해주세요.';
      } else if (value.length > 30) {
        isValid = false;
        errorMessage = '고인 이름은 30자 이하로 입력해주세요.';
      }
      break;

    case 'gender':
      if (!value) {
        isValid = false;
        errorMessage = '성별을 선택해주세요.';
      }
      break;

    case 'relationship':
      if (!value) {
        isValid = false;
        errorMessage = '관계를 선택해주세요.';
      }
      break;
  }

  // 에러 메시지 업데이트
  if (isValid) {
    delete memorialStep1State.validation.errors[fieldName];
    hideFieldError(fieldName);
  } else {
    memorialStep1State.validation.errors[fieldName] = errorMessage;
    showFieldError(fieldName, errorMessage);
  }

  return isValid;
}

/**
 * 날짜 범위 유효성 검사
 */
function validateDateRange() {
  const birthDate = getValue('birthDate');
  const deathDate = getValue('deathDate');

  if (!birthDate || !deathDate) {
    return true; // 선택사항이므로 비어있어도 유효
  }

  const birth = new Date(birthDate);
  const death = new Date(deathDate);
  const now = new Date();

  // 미래 날짜 검사
  if (birth > now) {
    showFieldError('birthDate', '생년월일은 현재 날짜 이전이어야 합니다.');
    return false;
  }

  if (death > now) {
    showFieldError('deathDate', '기일은 현재 날짜 이전이어야 합니다.');
    return false;
  }

  // 날짜 순서 검사
  if (birth >= death) {
    showFieldError('deathDate', '기일은 생년월일 이후여야 합니다.');
    return false;
  }

  hideFieldError('birthDate');
  hideFieldError('deathDate');
  return true;
}

/**
 * 1단계 데이터 저장
 */
async function saveStep1Data(formData) {
  console.log('💾 1단계 데이터 저장 시작');

  try {
    // API 호출 데이터 구성
    const requestData = {
      name: formData.name,
      description: formData.description,
      isPublic: formData.isPublic
    };

    const response = await authFetch('/api/memorial/create/step1', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestData)
    });

    if (!response.ok) {
      throw new Error('1단계 데이터 저장 실패');
    }

    const result = await response.json();

    if (result.success && result.data) {
      memorialStep1State.tempMemorialId = result.data.memorialId;

      // 세션 스토리지에 저장
      sessionStorage.setItem('tempMemorialId', memorialStep1State.tempMemorialId);
      sessionStorage.setItem('memorialStep1Data', JSON.stringify(formData));

      console.log('✅ 1단계 데이터 저장 성공:', result.data);
    } else {
      throw new Error(result.message || '1단계 데이터 저장 실패');
    }

  } catch (error) {
    console.error('❌ 1단계 데이터 저장 실패:', error);
    throw error;
  }
}

/**
 * 다음 단계로 이동
 */
async function moveToNextStep() {
  console.log('➡️ 다음 단계로 이동');

  try {
    showToast('기본 정보가 저장되었습니다.', 'success');

    // 잠시 후 다음 페이지로 이동
    setTimeout(() => {
      window.location.href = `/memorial/create/step2?memorialId=${memorialStep1State.tempMemorialId}`;
    }, 1000);

  } catch (error) {
    console.error('❌ 다음 단계 이동 실패:', error);
    throw error;
  }
}

/**
 * 유틸리티 함수들
 */

function getValue(id) {
  const element = document.getElementById(id);
  return element ? element.value.trim() : '';
}

function getRadioValue(name) {
  const radio = document.querySelector(`input[name="${name}"]:checked`);
  return radio ? radio.value : '';
}

function showFieldError(fieldName, message) {
  const errorElement = document.getElementById(fieldName + 'Error');
  if (errorElement) {
    errorElement.textContent = message;
    errorElement.style.display = 'block';
  }

  // 입력 필드에 에러 스타일 추가
  const field = document.getElementById(fieldName) || document.querySelector(`[name="${fieldName}"]`);
  if (field) {
    field.classList.add('error');
  }
}

function hideFieldError(fieldName) {
  const errorElement = document.getElementById(fieldName + 'Error');
  if (errorElement) {
    errorElement.style.display = 'none';
  }

  // 입력 필드에서 에러 스타일 제거
  const field = document.getElementById(fieldName) || document.querySelector(`[name="${fieldName}"]`);
  if (field) {
    field.classList.remove('error');
  }
}

function restoreFormData() {
  Object.keys(memorialStep1State.formData).forEach(key => {
    const value = memorialStep1State.formData[key];
    if (value) {
      const element = document.getElementById(key) || document.querySelector(`[name="${key}"]`);
      if (element) {
        if (element.type === 'radio') {
          const radio = document.querySelector(`input[name="${key}"][value="${value}"]`);
          if (radio) radio.checked = true;
        } else {
          element.value = value;
        }
      }
    }
  });
}

function updateProgressBar() {
  const progressFill = document.querySelector('.progress-fill');
  if (progressFill) {
    const progress = (memorialStep1State.currentStep / memorialStep1State.maxStep) * 100;
    progressFill.style.width = `${progress}%`;
  }
}

function setupValidationRules() {
  // 실시간 글자 수 제한
  const memorialName = document.getElementById('memorialName');
  const memorialDescription = document.getElementById('memorialDescription');
  const deceasedName = document.getElementById('deceasedName');

  if (memorialName) {
    memorialName.addEventListener('input', () => {
      const length = memorialName.value.length;
      const maxLength = 50;
      if (length > maxLength) {
        memorialName.value = memorialName.value.substring(0, maxLength);
      }
    });
  }

  if (memorialDescription) {
    memorialDescription.addEventListener('input', () => {
      const length = memorialDescription.value.length;
      const maxLength = 500;
      if (length > maxLength) {
        memorialDescription.value = memorialDescription.value.substring(0, maxLength);
      }
    });
  }

  if (deceasedName) {
    deceasedName.addEventListener('input', () => {
      const length = deceasedName.value.length;
      const maxLength = 30;
      if (length > maxLength) {
        deceasedName.value = deceasedName.value.substring(0, maxLength);
      }
    });
  }
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

/**
 * 정리 함수
 */
function destroyMemorialStep1() {
  console.log('🗑️ 메모리얼 등록 1단계 정리');
  memorialStep1State.isInitialized = false;
}

/**
 * 전역 함수들
 */
window.memorialStep1Manager = {
  initialize: initializeMemorialStep1,
  validateForm,
  collectFormData,
  destroy: destroyMemorialStep1,
  getState: () => memorialStep1State
};

/**
 * 자동 초기화
 */
console.log('🌟 memorial-create-step1.js 로드 완료');

// DOM이 준비되면 초기화
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initializeMemorialStep1);
} else {
  setTimeout(initializeMemorialStep1, 100);
}

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', destroyMemorialStep1);

// 모듈 익스포트
export {
  initializeMemorialStep1,
  validateForm,
  collectFormData,
  saveStep1Data,
  destroyMemorialStep1
};