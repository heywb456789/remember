// login.js - 토마토리멤버 로그인 페이지 (개선된 버전)
// 기존 API 구조 유지하면서 UI 개선

import { checkLoginStatus, syncTokensFromPage } from './commonFetch.js';
import { showToast } from './common.js';

// ===== 전역 변수 =====
let isSubmitting = false;

// ===== 초기화 함수 =====
const initLoginPage = () => {
  console.log("🚀 토마토리멤버 로그인 페이지 초기화");

  // 이미 로그인된 상태면 홈으로 리다이렉트
  if (checkLoginStatus()) {
    console.log('✅ 이미 로그인된 상태입니다.');
    showToast('이미 로그인되어 있습니다.', 'info');
    setTimeout(() => {
      window.location.href = '/mobile/home';
    }, 1000);
    return;
  }

  // 서버에서 갱신된 토큰 동기화
  syncTokensFromPage();

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

  // 비밀번호 토글 기능 초기화
  initPasswordToggle();

  console.log('✅ 로그인 페이지 초기화 완료');
};

// ===== DOM 요소 초기화 =====
const initializeElements = () => {
  const elements = {
    // 입력 필드
    countryCode: document.getElementById('countryCode'),
    phoneNumber: document.getElementById('phoneNumber'),
    password: document.getElementById('password'),
    autoLogin: document.getElementById('autoLogin'),

    // 버튼
    loginButton: document.getElementById('loginButton'),
    tongtongButton: document.getElementById('tongtongButton'),
    signupButton: document.getElementById('signupButton'),

    // 토글 요소
    passwordToggle: document.getElementById('passwordToggle'),
    eyeHidden: document.getElementById('eyeHidden'),
    eyeVisible: document.getElementById('eyeVisible'),

    // 기타
    messageElement: document.getElementById('clientMessage')
  };

  // 필수 요소 체크
  const requiredElements = ['phoneNumber', 'password', 'loginButton'];
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

  // 휴대폰 번호 입력 처리
  elements.phoneNumber.addEventListener('input', (e) => {
    handlePhoneInput(e, elements);
  });

  // 비밀번호 입력 처리
  elements.password.addEventListener('input', () => {
    handlePasswordInput(elements);
  });

  // 로그인 버튼 클릭
  elements.loginButton.addEventListener('click', (e) => {
    e.preventDefault();
    handleLogin(elements);
  });

  // Enter 키 처리
  elements.password.addEventListener('keypress', (e) => {
    if (e.key === 'Enter' && !isSubmitting) {
      e.preventDefault();
      handleLogin(elements);
    }
  });

  // 회원가입 버튼 클릭
  elements.signupButton.addEventListener('click', () => {
    handleSignup();
  });

  // 통통 로그인 버튼 클릭
  elements.tongtongButton.addEventListener('click', () => {
    handleTongtongLogin();
  });

  // 폼 필드 포커스 시 에러 메시지 숨김
  [elements.phoneNumber, elements.password].forEach(input => {
    input.addEventListener('focus', () => {
      hideMessage(elements);
    });
  });

  console.log('✅ 이벤트 리스너 등록 완료');
};

// ===== 폼 초기 상태 설정 =====
const initializeFormState = (elements) => {
  // 버튼 초기 비활성화
  updateFormValidation(elements);

  // 휴대폰 번호 입력 필드에 포커스
  elements.phoneNumber.focus();
};

// ===== 비밀번호 토글 기능 =====
const initPasswordToggle = () => {
  const passwordInput = document.getElementById('password');
  const passwordToggle = document.getElementById('passwordToggle');
  const eyeHidden = document.getElementById('eyeHidden');
  const eyeVisible = document.getElementById('eyeVisible');

  if (!passwordToggle || !eyeHidden || !eyeVisible) {
    console.warn('⚠️ 비밀번호 토글 요소를 찾을 수 없습니다.');
    return;
  }

  passwordToggle.addEventListener('click', () => {
    const isPassword = passwordInput.type === 'password';

    // 입력 타입 변경
    passwordInput.type = isPassword ? 'text' : 'password';

    // 아이콘 변경
    eyeHidden.style.display = isPassword ? 'none' : 'block';
    eyeVisible.style.display = isPassword ? 'block' : 'none';

    // 포커스 유지
    passwordInput.focus();

    console.log(`👁️ 비밀번호 ${isPassword ? '표시' : '숨김'}`);
  });
};

// ===== 입력 처리 함수들 =====
const handlePhoneInput = (e, elements) => {
  // 숫자만 허용
  let value = e.target.value.replace(/\D/g, '');

  // 11자리 제한
  if (value.length > 11) {
    value = value.slice(0, 11);
  }

  e.target.value = value;

  // 폼 검증 업데이트
  updateFormValidation(elements);

  // 자동 포맷팅 (선택사항)
  if (value.length >= 3) {
    updatePhoneDisplay(e.target, value);
  }
};

const handlePasswordInput = (elements) => {
  updateFormValidation(elements);
};

// ===== 휴대폰 번호 표시 포맷팅 =====
const updatePhoneDisplay = (input, value) => {
  // 실제 value는 숫자만 유지하고, placeholder에 포맷팅된 형태 표시
  if (value.length >= 11) {
    input.placeholder = `${value.slice(0, 3)} ${value.slice(3, 7)} ${value.slice(7, 11)}`;
  } else if (value.length >= 7) {
    input.placeholder = `${value.slice(0, 3)} ${value.slice(3, 7)} ${value.slice(7)}`;
  } else if (value.length >= 3) {
    input.placeholder = `${value.slice(0, 3)} ${value.slice(3)}`;
  }
};

// ===== 폼 검증 =====
const updateFormValidation = (elements) => {
  const phoneValue = elements.phoneNumber.value.trim();
  const passwordValue = elements.password.value;

  // 휴대폰 번호 검증 (010으로 시작하는 11자리)
  const isPhoneValid = /^010\d{8}$/.test(phoneValue);

  // 비밀번호 검증 (최소 4자리)
  const isPasswordValid = passwordValue.length >= 4;

  // 전체 폼 유효성
  const isFormValid = isPhoneValid && isPasswordValid;

  // 버튼 상태 업데이트
  elements.loginButton.disabled = !isFormValid || isSubmitting;

  // 입력 필드 시각적 피드백
  updateFieldValidation(elements.phoneNumber, phoneValue.length > 0 ? isPhoneValid : null);
  updateFieldValidation(elements.password, passwordValue.length > 0 ? isPasswordValid : null);

  return isFormValid;
};

// ===== 필드 검증 시각적 피드백 =====
const updateFieldValidation = (field, isValid) => {
  field.classList.remove('is-valid', 'is-invalid');

  if (isValid === true) {
    field.classList.add('is-valid');
  } else if (isValid === false) {
    field.classList.add('is-invalid');
  }
};

// ===== 핵심 로그인 처리 =====
const handleLogin = async (elements) => {
  if (isSubmitting) {
    console.log('⚠️ 이미 로그인 요청 처리 중');
    return;
  }

  // 폼 검증
  if (!updateFormValidation(elements)) {
    showToast('휴대폰 번호와 비밀번호를 올바르게 입력해주세요.', 'warning');
    return;
  }

  console.log('🚀 로그인 시도 시작');
  isSubmitting = true;

  // 로딩 상태 표시
  showLoadingState(elements.loginButton);
  hideMessage(elements);

  try {
    // 로그인 데이터 준비
    const loginData = {
      phoneNumber: elements.phoneNumber.value.trim(),
      password: elements.password.value,
      autoLogin: elements.autoLogin.checked
    };

    console.log('📞 로그인 API 호출:', {
      phoneNumber: loginData.phoneNumber,
      autoLogin: loginData.autoLogin
    });

    // API 호출
    const loginUrl = window.API_ENDPOINTS?.LOGIN || '/api/auth/login';
    const response = await fetch(loginUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include', // 쿠키 포함
      body: JSON.stringify(loginData)
    });

    const data = await response.json();
    console.log('📡 API 응답:', data);

    if (response.ok && data.status?.code === 'OK_0000') {
      // ✅ 로그인 성공
      console.log('✅ 로그인 성공:', data.response.member);

      // localStorage에 토큰 저장
      if (data.response.accessToken) {
        localStorage.setItem('accessToken', data.response.accessToken);
      }
      if (data.response.refreshToken) {
        localStorage.setItem('refreshToken', data.response.refreshToken);
      }

      // 성공 메시지 표시
      const memberName = data.response.member?.name || '사용자';
      showToast(`환영합니다, ${memberName}님!`, 'success');
      showSuccessMessage(elements, `로그인되었습니다.`);

      // 홈 페이지로 이동
      setTimeout(() => {
        const homeUrl = window.PAGE_CONFIG?.homeUrl || '/';
        window.location.href = homeUrl;
      }, 1500);

    } else {
      // ❌ 로그인 실패
      const errorMessage = data.status?.message || '로그인에 실패했습니다.';
      console.error('❌ 로그인 실패:', errorMessage);

      showToast('비밀번호가 틀렸습니다.', 'error');
      showErrorMessage(elements, errorMessage);

      // 비밀번호 필드 포커스 및 선택
      elements.password.focus();
      elements.password.select();
    }

  } catch (error) {
    console.error('💥 로그인 오류:', error);
    showToast('네트워크 오류가 발생했습니다. 다시 시도해주세요.', 'error');
    showErrorMessage(elements, '연결에 실패했습니다. 인터넷 연결을 확인해주세요.');
  } finally {
    isSubmitting = false;
    hideLoadingState(elements.loginButton);
  }
};

// ===== 회원가입 처리 =====
const handleSignup = () => {
  console.log('📝 One-ID 회원가입 페이지로 이동');
  const registerUrl = window.PAGE_CONFIG?.registerUrl || '/mobile/register';
  window.location.href = registerUrl;
};

// ===== 통통 로그인 처리 =====
const handleTongtongLogin = () => {
  console.log('🔗 통통 로그인 시도');
  showToast('앱에서 지원되는 기능입니다.', 'info');
};

// ===== UI 상태 관리 함수들 =====
const showLoadingState = (button) => {
  const buttonText = button.querySelector('.button-text');
  const buttonLoading = button.querySelector('.button-loading');

  if (buttonText && buttonLoading) {
    buttonText.style.display = 'none';
    buttonLoading.style.display = 'flex';
  }

  button.disabled = true;
  button.style.opacity = '0.8';
};

const hideLoadingState = (button) => {
  const buttonText = button.querySelector('.button-text');
  const buttonLoading = button.querySelector('.button-loading');

  if (buttonText && buttonLoading) {
    buttonText.style.display = 'block';
    buttonLoading.style.display = 'none';
  }

  button.disabled = false;
  button.style.opacity = '1';
};

// ===== 메시지 표시 함수들 =====
const showErrorMessage = (elements, message) => {
  if (elements.messageElement) {
    elements.messageElement.textContent = message;
    elements.messageElement.className = 'alert-message alert-error';
    elements.messageElement.style.display = 'block';

    // 자동 숨김
    setTimeout(() => {
      hideMessage(elements);
    }, 5000);
  }
};

const showSuccessMessage = (elements, message) => {
  if (elements.messageElement) {
    elements.messageElement.textContent = message;
    elements.messageElement.className = 'alert-message alert-success';
    elements.messageElement.style.display = 'block';
  }
};

const hideMessage = (elements) => {
  if (elements.messageElement) {
    elements.messageElement.style.display = 'none';
  }
};

// ===== 토큰 동기화 이벤트 처리 =====
document.addEventListener('tokenSynced', (event) => {
  console.log('🔄 토큰 동기화 감지:', event.detail);

  if (event.detail.accessToken && event.detail.refreshToken) {
    console.log('✅ 로그인 상태 감지됨');
    showToast('로그인되었습니다.', 'success');

    setTimeout(() => {
      window.location.href = '/';
    }, 1000);
  }
});

// ===== 페이지 로드 시 초기화 =====
document.addEventListener('DOMContentLoaded', initLoginPage);

// ===== 페이지 가시성 변경 시 처리 =====
document.addEventListener('visibilitychange', () => {
  if (!document.hidden && checkLoginStatus()) {
    console.log('👁️ 페이지 포커스 시 로그인 상태 감지');
    window.location.href = '/';
  }
});

// ===== 에러 처리 =====
window.addEventListener('error', (event) => {
  console.error('🚨 전역 에러:', event.error);
});

window.addEventListener('unhandledrejection', (event) => {
  console.error('🚨 처리되지 않은 Promise 거부:', event.reason);
});

// ===== 디버그 함수 (개발용) =====
if (window.location.search.includes('debug=true')) {
  window.loginDebug = {
    getFormData: () => {
      const elements = initializeElements();
      return {
        phoneNumber: elements?.phoneNumber?.value,
        password: elements?.password?.value,
        autoLogin: elements?.autoLogin?.checked,
        isSubmitting
      };
    },
    testValidation: () => {
      const elements = initializeElements();
      if (elements) {
        updateFormValidation(elements);
      }
    },
    showTestToast: (message, type) => {
      showToast(message, type || 'info');
    }
  };

  console.log('🔧 디버그 모드 활성화 - window.loginDebug 사용 가능');
}