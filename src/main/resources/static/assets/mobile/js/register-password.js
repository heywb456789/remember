// register-password.js - 비밀번호 설정 및 회원가입 완료

import { showToast } from './common.js';

// ===== 전역 변수 =====
let isSubmitting = false;
let phoneVerificationData = null;

// 비밀번호 검증 규칙
const passwordRules = {
  length: (password) => password.length >= 8,
  letter: (password) => /[a-zA-Z]/.test(password),
  number: (password) => /\d/.test(password),
  special: (password) => /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)
};

// ===== 초기화 함수 =====
const initRegisterPasswordPage = () => {
  console.log("🚀 비밀번호 설정 페이지 초기화");

  // 이전 단계 데이터 확인
  if (!loadPhoneVerificationData()) {
    console.error('❌ 인증 데이터가 없습니다. 이전 단계로 이동합니다.');
    redirectToPhoneVerify();
    return;
  }

  // DOM 요소 초기화
  const elements = initializeElements();
  if (!elements) {
    console.error('❌ 필수 DOM 요소를 찾을 수 없습니다.');
    return;
  }

  // 이벤트 리스너 등록
  registerEventListeners(elements);

  // 폼 초기 상태 설정
  initializeFormState(elements);

  // 모달 초기화
  initModals();

  console.log('✅ 비밀번호 설정 페이지 초기화 완료');
  console.log('📋 인증 데이터:', phoneVerificationData);
};

// ===== 이전 단계 데이터 로드 =====
const loadPhoneVerificationData = () => {
  try {
    const storedData = sessionStorage.getItem('phoneVerification');
    if (!storedData) {
      console.warn('⚠️ 세션 스토리지에 인증 데이터가 없습니다.');
      return false;
    }

    phoneVerificationData = JSON.parse(storedData);

    // 필수 데이터 확인
    const requiredFields = ['phoneNumber', 'userKey', 'verificationCode'];
    const missingFields = requiredFields.filter(field => !phoneVerificationData[field]);

    if (missingFields.length > 0) {
      console.error('❌ 필수 인증 데이터 누락:', missingFields);
      return false;
    }

    // 인증 데이터 유효성 확인 (3시간 이내)
    const verifiedAt = new Date(phoneVerificationData.verifiedAt);
    const now = new Date();
    const timeDiff = now - verifiedAt;
    const hoursDiff = timeDiff / (1000 * 60 * 60);

    if (hoursDiff > 3) {
      console.warn('⚠️ 인증 데이터가 만료되었습니다. (3시간 초과)');
      sessionStorage.removeItem('phoneVerification');
      return false;
    }

    return true;
  } catch (error) {
    console.error('❌ 인증 데이터 로드 실패:', error);
    return false;
  }
};

// ===== DOM 요소 초기화 =====
const initializeElements = () => {
  const elements = {
    // 입력 필드
    password: document.getElementById('password'),
    confirmPassword: document.getElementById('confirmPassword'),

    // 토글 버튼
    passwordToggle: document.getElementById('passwordToggle'),
    confirmPasswordToggle: document.getElementById('confirmPasswordToggle'),

    // 버튼
    registerButton: document.getElementById('registerButton'),

    // 상태 표시
    passwordRequirements: document.getElementById('passwordRequirements'),
    passwordMatchMessage: document.getElementById('passwordMatchMessage'),
    passwordErrorMessage: document.getElementById('passwordErrorMessage'),

    // 모달
    successModal: document.getElementById('successModal'),
    errorModal: document.getElementById('errorModal'),
    successConfirm: document.getElementById('successConfirm'),
    errorConfirm: document.getElementById('errorConfirm'),
    errorMessage: document.getElementById('errorMessage')
  };

  // 필수 요소 체크
  const requiredElements = ['password', 'confirmPassword', 'registerButton'];
  const missingElements = requiredElements.filter(key => !elements[key]);

  if (missingElements.length > 0) {
    console.error('❌ 필수 DOM 요소 누락:', missingElements);
    return null;
  }

  return elements;
};

// ===== 이벤트 리스너 등록 =====
const registerEventListeners = (elements) => {
  console.log('🔗 이벤트 리스너 등록');

  // 비밀번호 입력 처리
  elements.password.addEventListener('input', (e) => {
    handlePasswordInput(e, elements);
  });

  // 비밀번호 확인 입력 처리
  elements.confirmPassword.addEventListener('input', (e) => {
    handleConfirmPasswordInput(e, elements);
  });

  // 비밀번호 토글 버튼
  if (elements.passwordToggle) {
    elements.passwordToggle.addEventListener('click', () => {
      togglePasswordVisibility(elements.password, elements.passwordToggle);
    });
  }

  if (elements.confirmPasswordToggle) {
    elements.confirmPasswordToggle.addEventListener('click', () => {
      togglePasswordVisibility(elements.confirmPassword, elements.confirmPasswordToggle);
    });
  }

  // 회원가입 버튼
  elements.registerButton.addEventListener('click', () => {
    handleRegister(elements);
  });

  // Enter 키 처리
  elements.password.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
      elements.confirmPassword.focus();
    }
  });

  elements.confirmPassword.addEventListener('keypress', (e) => {
    if (e.key === 'Enter' && !elements.registerButton.disabled) {
      handleRegister(elements);
    }
  });

  console.log('✅ 이벤트 리스너 등록 완료');
};

// ===== 폼 초기 상태 설정 =====
const initializeFormState = (elements) => {
  // 비밀번호 입력 필드에 포커스
  elements.password.focus();

  // 버튼 상태 업데이트
  updateRegisterButton(elements);
};

// ===== 모달 초기화 =====
const initModals = () => {
  const elements = initializeElements();

  // 성공 모달 확인 버튼
  elements.successConfirm.addEventListener('click', () => {
    handleSuccessConfirm();
  });

  // 에러 모달 확인 버튼
  elements.errorConfirm.addEventListener('click', () => {
    hideErrorModal();
  });

  // 모달 배경 클릭 시 닫기
  elements.errorModal.addEventListener('click', (e) => {
    if (e.target === elements.errorModal) {
      hideErrorModal();
    }
  });
};

// ===== 입력 처리 함수들 =====
const handlePasswordInput = (e, elements) => {
  const password = e.target.value;

  // 비밀번호 요구사항 검증 및 UI 업데이트
  updatePasswordRequirements(password, elements);

  // 비밀번호 확인과 일치 여부 체크
  if (elements.confirmPassword.value) {
    updatePasswordMatch(password, elements.confirmPassword.value, elements);
  }

  // 버튼 상태 업데이트
  updateRegisterButton(elements);
};

const handleConfirmPasswordInput = (e, elements) => {
  const confirmPassword = e.target.value;
  const password = elements.password.value;

  // 비밀번호 일치 여부 체크
  updatePasswordMatch(password, confirmPassword, elements);

  // 버튼 상태 업데이트
  updateRegisterButton(elements);
};

// ===== 비밀번호 검증 함수들 =====
const updatePasswordRequirements = (password, elements) => {
  if (!elements.passwordRequirements) return;

  const requirements = elements.passwordRequirements.querySelectorAll('.requirement');

  requirements.forEach(requirement => {
    const ruleType = requirement.dataset.requirement;
    const isValid = passwordRules[ruleType] ? passwordRules[ruleType](password) : false;

    requirement.classList.toggle('valid', isValid);
  });
};

const updatePasswordMatch = (password, confirmPassword, elements) => {
  if (!confirmPassword) {
    // 확인 비밀번호가 비어있으면 메시지 숨김
    elements.passwordMatchMessage.style.display = 'none';
    elements.passwordErrorMessage.style.display = 'none';
    return;
  }

  if (password === confirmPassword) {
    // 일치
    elements.passwordMatchMessage.style.display = 'block';
    elements.passwordErrorMessage.style.display = 'none';
  } else {
    // 불일치
    elements.passwordMatchMessage.style.display = 'none';
    elements.passwordErrorMessage.style.display = 'block';
  }
};

const isPasswordValid = (password) => {
  return Object.values(passwordRules).every(rule => rule(password));
};

const isPasswordsMatch = (password, confirmPassword) => {
  return password === confirmPassword && confirmPassword.length > 0;
};

// ===== 버튼 상태 관리 =====
const updateRegisterButton = (elements) => {
  const password = elements.password.value;
  const confirmPassword = elements.confirmPassword.value;

  const isValid = isPasswordValid(password) &&
                  isPasswordsMatch(password, confirmPassword) &&
                  !isSubmitting;

  elements.registerButton.disabled = !isValid;
};

// ===== 비밀번호 표시/숨김 토글 =====
const togglePasswordVisibility = (inputElement, toggleButton) => {
  const isPassword = inputElement.type === 'password';
  const eyeIcon = toggleButton.querySelector('.eye-icon');
  const eyeOffIcon = toggleButton.querySelector('.eye-off-icon');

  if (isPassword) {
    inputElement.type = 'text';
    eyeIcon.style.display = 'none';
    eyeOffIcon.style.display = 'block';
  } else {
    inputElement.type = 'password';
    eyeIcon.style.display = 'block';
    eyeOffIcon.style.display = 'none';
  }
};

// ===== 회원가입 처리 (실제 API 연동) =====
const handleRegister = async (elements) => {
  if (isSubmitting) return;

  const password = elements.password.value;
  const confirmPassword = elements.confirmPassword.value;

  // 최종 검증
  if (!isPasswordValid(password)) {
    showErrorModal('비밀번호가 요구사항을 만족하지 않습니다.');
    return;
  }

  if (!isPasswordsMatch(password, confirmPassword)) {
    showErrorModal('비밀번호가 일치하지 않습니다.');
    return;
  }

  if (!phoneVerificationData) {
    showErrorModal('인증 정보가 없습니다. 다시 인증해주세요.');
    redirectToPhoneVerify();
    return;
  }

  console.log('🚀 회원가입 요청 시작');

  isSubmitting = true;
  showLoadingState(elements.registerButton);

  try {
    // 회원가입 데이터 구성
    const registerData = {
      name: phoneVerificationData.phoneNumber, // 핸드폰 번호를 name으로 사용
      phoneNumber: phoneVerificationData.phoneNumber,
      verificationCode: phoneVerificationData.verificationCode,
      userKey: phoneVerificationData.userKey,
      password: password,
      email: '', // 선택사항 - 빈 값
      marketingAgree: false // 기본값 false
    };

    console.log('📤 회원가입 데이터:', {
      ...registerData,
      password: '***masked***',
      verificationCode: '***masked***',
      userKey: '***masked***'
    });

    // 회원가입 API 호출
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(registerData)
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    console.log('📥 회원가입 응답:', data);

    if (data.status.code === 'OK_0000') {
      // 회원가입 성공
      console.log('✅ 회원가입 성공');

      // 토큰 저장 (JWT 쿠키 시스템과 연동)
      if (data.response.accessToken && data.response.refreshToken) {
        localStorage.setItem('accessToken', data.response.accessToken);
        localStorage.setItem('refreshToken', data.response.refreshToken);
        console.log('🔐 토큰 저장 완료');
      }

      // 인증 데이터 정리
      sessionStorage.removeItem('phoneVerification');

      // 회원가입 완료 페이지로 바로 이동
      console.log('🎉 회원가입 완료 페이지로 이동');
      window.location.href = '/mobile/register/complete';

    } else {
      // 회원가입 실패
      const errorMessage = data.response?.message || data.status?.message || '회원가입에 실패했습니다.';
      console.error('❌ 회원가입 실패:', errorMessage);
      showErrorModal(errorMessage);
    }

  } catch (error) {
    console.error('💥 회원가입 오류:', error);
    showErrorModal('네트워크 오류가 발생했습니다. 인터넷 연결을 확인하고 다시 시도해주세요.');
  } finally {
    isSubmitting = false;
    hideLoadingState(elements.registerButton);
    updateRegisterButton(elements);
  }
};

// ===== UI 상태 관리 =====
const showLoadingState = (button) => {
  const buttonText = button.querySelector('.button-text');
  const buttonLoading = button.querySelector('.button-loading');

  if (buttonText && buttonLoading) {
    buttonText.style.display = 'none';
    buttonLoading.style.display = 'block';
  }

  button.disabled = true;
};

const hideLoadingState = (button) => {
  const buttonText = button.querySelector('.button-text');
  const buttonLoading = button.querySelector('.button-loading');

  if (buttonText && buttonLoading) {
    buttonText.style.display = 'block';
    buttonLoading.style.display = 'none';
  }
};

// ===== 모달 관리 =====
const showSuccessModal = () => {
  const elements = initializeElements();

  elements.successModal.style.display = 'flex';
  setTimeout(() => {
    elements.successModal.classList.add('show');
  }, 10);
};

const hideSuccessModal = () => {
  const elements = initializeElements();

  elements.successModal.classList.remove('show');
  setTimeout(() => {
    elements.successModal.style.display = 'none';
  }, 300);
};

const showErrorModal = (message) => {
  const elements = initializeElements();

  elements.errorMessage.textContent = message;
  elements.errorModal.style.display = 'flex';
  setTimeout(() => {
    elements.errorModal.classList.add('show');
  }, 10);
};

const hideErrorModal = () => {
  const elements = initializeElements();

  elements.errorModal.classList.remove('show');
  setTimeout(() => {
    elements.errorModal.style.display = 'none';
  }, 300);
};

// ===== 성공 후 처리 =====
const handleSuccessConfirm = () => {
  console.log('🏠 홈페이지로 이동');

  hideSuccessModal();

  // 홈페이지로 이동
  const homeUrl = window.PAGE_CONFIG?.homeUrl || '/mobile/home';
  window.location.href = homeUrl;
};

// ===== 네비게이션 함수들 =====
const redirectToPhoneVerify = () => {
  const prevUrl = window.PAGE_CONFIG?.prevUrl || '/mobile/register/verify';

  setTimeout(() => {
    window.location.href = prevUrl;
  }, 1000);
};

// ===== 페이지 로드 시 초기화 =====
document.addEventListener('DOMContentLoaded', initRegisterPasswordPage);

// ===== 키보드 이벤트 처리 =====
document.addEventListener('keydown', (e) => {
  // ESC 키로 모달 닫기
  if (e.key === 'Escape') {
    const errorModal = document.getElementById('errorModal');
    if (errorModal && errorModal.classList.contains('show')) {
      hideErrorModal();
    }
  }
});

// ===== 페이지 언로드 시 경고 (입력 중일 때) =====
window.addEventListener('beforeunload', (e) => {
  const elements = initializeElements();
  if (elements && (elements.password.value || elements.confirmPassword.value) && !isSubmitting) {
    e.preventDefault();
    e.returnValue = '입력한 내용이 사라집니다. 정말 나가시겠습니까?';
  }
});

// ===== 디버그 함수 (개발용) =====
if (window.location.search.includes('debug=true')) {
  window.registerPasswordDebug = {
    getFormData: () => {
      const elements = initializeElements();
      return {
        password: elements?.password?.value ? '***masked***' : '',
        confirmPassword: elements?.confirmPassword?.value ? '***masked***' : '',
        phoneVerificationData: phoneVerificationData ? '***exists***' : null,
        isSubmitting
      };
    },
    getPhoneVerificationData: () => phoneVerificationData,
    simulateSuccess: () => {
      showSuccessModal();
    },
    testPasswordValidation: (password) => {
      const results = {};
      Object.keys(passwordRules).forEach(rule => {
        results[rule] = passwordRules[rule](password);
      });
      return results;
    }
  };

  console.log('🔧 디버그 모드 활성화 - window.registerPasswordDebug 사용 가능');
}