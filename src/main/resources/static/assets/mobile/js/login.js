// src/main/resources/static/assets/mobile/js/login.js
// 새로운 JWT 쿠키 기반 인증 시스템용 로그인 JavaScript

import {
  authFetch,
  checkAuthAndRedirect,
  syncTokensFromPage,
  memberLogin
} from '/js/commonFetchV2.js';

// 초기화 함수
const initLoginPage = () => {
  console.log("Login page initializing with new JWT cookie authentication...");

  // 이미 로그인된 상태라면 메인 페이지로 리다이렉트
  checkAuthAndRedirect(window.PAGE_CONFIG?.mainUrl || '/mobile/main');

  // 페이지 로드 시 토큰 동기화 (서버에서 갱신된 토큰이 있는 경우)
  syncTokensFromPage();

  const elements = selectElements();
  registerEventListeners(elements);
  validateForm(elements);

  // 비밀번호 토글 기능 초기화
  initPasswordToggle();

  console.log('새로운 JWT 쿠키 인증 시스템으로 로그인 페이지 초기화 완료');
};

// 비밀번호 보기/숨기기 기능 초기화
const initPasswordToggle = () => {
  const passwordInput = document.getElementById('password');
  const passwordToggle = document.getElementById('passwordToggle');
  const eyeHidden = document.getElementById('eyeHidden');
  const eyeVisible = document.getElementById('eyeVisible');

  if (passwordToggle) {
    passwordToggle.addEventListener('click', function() {
      if (passwordInput.type === 'password') {
        // 비밀번호 보이기
        passwordInput.type = 'text';
        eyeHidden.style.display = 'none';
        eyeVisible.style.display = 'block';
      } else {
        // 비밀번호 숨기기
        passwordInput.type = 'password';
        eyeHidden.style.display = 'block';
        eyeVisible.style.display = 'none';
      }
    });
  }
};

const selectElements = () => ({
  phoneInput: document.querySelector('.phone-input'),
  passwordInput: document.getElementById('password'),
  loginButton: document.getElementById('verifyButton'),
  registerButton: document.getElementById('registerButton'),
  backButton: document.querySelector('.back-button'),
  autoLoginCheckbox: document.getElementById('autoLogin'),
  loginForm: document.getElementById('loginForm')
});

const registerEventListeners = (elements) => {
  elements.phoneInput.addEventListener('input',
      (e) => handlePhoneInput(e, elements));
  elements.passwordInput.addEventListener('input',
      () => handlePasswordInput(elements));

  // 🆕 버튼 클릭 이벤트로 변경 (폼 제출 대신)
  elements.loginButton.addEventListener('click', () => {
    handleLogin(elements);
  });

  // Enter 키 처리
  elements.passwordInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
      handleLogin(elements);
    }
  });

  elements.registerButton.addEventListener('click', handleRegister);
  elements.backButton.addEventListener('click', handleBack);
};

/**
 * 🆕 새로운 API 기반 로그인 처리
 *
 * 변경사항:
 * 1. JSON 형태로 API 호출
 * 2. 서버에서 쿠키에 JWT 토큰 설정
 * 3. 클라이언트에서 localStorage와 자동 동기화
 * 4. 성공/실패 처리를 클라이언트에서 완전히 처리
 */
const handleLogin = async (elements) => {
  const loginData = {
    phoneNumber: elements.phoneInput.value.trim(),
    password: elements.passwordInput.value,
    autoLogin: elements.autoLoginCheckbox.checked
  };

  console.log('🚀 API login attempt:', {
    phoneNumber: loginData.phoneNumber,
    autoLogin: loginData.autoLogin
  });

  // 로딩 상태 표시
  showLoadingState(elements.loginButton);

  try {
    // API 엔드포인트에서 가져오기
    const loginUrl = window.API_ENDPOINTS?.LOGIN || '/api/auth/login';

    // JSON으로 API 호출
    const response = await fetch(loginUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include', // 쿠키 포함
      body: JSON.stringify(loginData)
    });

    const data = await response.json();

    if (response.ok && data.status?.code === 'OK_0000') {
      // 🎉 로그인 성공
      console.log('✅ Login successful:', data.response.member);

      // localStorage에 토큰 저장 (서버에서 쿠키로도 설정됨)
      if (data.response.token) {
        localStorage.setItem('accessToken', data.response.token);
      }
      if (data.response.refreshToken) {
        localStorage.setItem('refreshToken', data.response.refreshToken);
      }

      // 성공 메시지 표시
      showSuccessMessage(`환영합니다, ${data.response.member.name}님!`);

      // 잠시 후 메인 페이지로 이동
      setTimeout(() => {
        const mainUrl = window.PAGE_CONFIG?.mainUrl || '/mobile/main';
        window.location.href = mainUrl;
      }, 1000);

    } else {
      // 로그인 실패
      const errorMessage = data.status?.message || '로그인에 실패했습니다.';
      console.error('❌ Login failed:', errorMessage);
      showErrorMessage(errorMessage);
      hideLoadingState(elements.loginButton);
    }

  } catch (err) {
    console.error('💥 Login error:', err);
    showErrorMessage('네트워크 오류가 발생했습니다. 다시 시도해주세요.');
    hideLoadingState(elements.loginButton);
  }
};

/**
 * 🆕 대안: commonFetchV2.js 사용한 로그인 (필요시 사용)
 */
const handleApiLoginAlternative = async (elements) => {
  const loginData = {
    phoneNumber: elements.phoneInput.value.trim(),
    password: elements.passwordInput.value,
    autoLogin: elements.autoLoginCheckbox.checked
  };

  showLoadingState(elements.loginButton);

  try {
    // commonFetchV2.js의 memberLogin 함수 사용
    const result = await memberLogin(
        loginData.phoneNumber,
        loginData.password,
        loginData.autoLogin
    );

    if (result.success) {
      console.log('API 로그인 성공:', result.data.member);

      // 성공 메시지 표시
      showSuccessMessage(`환영합니다, ${result.data.member.name}님!`);

      // 잠시 후 메인 페이지로 이동
      setTimeout(() => {
        const mainUrl = window.PAGE_CONFIG?.mainUrl || '/mobile/main';
        window.location.href = mainUrl;
      }, 1000);

    } else {
      showErrorMessage(result.error);
      hideLoadingState(elements.loginButton);
    }

  } catch (err) {
    console.error('API 로그인 실패:', err);
    showErrorMessage('네트워크 오류가 발생했습니다.');
    hideLoadingState(elements.loginButton);
  }
};

// 로딩 상태 표시
const showLoadingState = (button) => {
  const buttonText = button.querySelector('.button-text');
  const loadingSpinner = button.querySelector('.loading-spinner');

  if (buttonText && loadingSpinner) {
    buttonText.style.display = 'none';
    loadingSpinner.style.display = 'inline-block';
  }

  button.disabled = true;
  button.style.opacity = '0.7';
};

// 로딩 상태 해제
const hideLoadingState = (button) => {
  const buttonText = button.querySelector('.button-text');
  const loadingSpinner = button.querySelector('.loading-spinner');

  if (buttonText && loadingSpinner) {
    buttonText.style.display = 'inline';
    loadingSpinner.style.display = 'none';
  }

  button.disabled = false;
  button.style.opacity = '1';
};

// 에러 메시지 표시
const showErrorMessage = (message) => {
  const messageElement = document.getElementById('clientMessage');
  if (messageElement) {
    messageElement.textContent = message;
    messageElement.className = 'alert-message alert-error';
    messageElement.style.display = 'block';

    // 5초 후 메시지 숨김
    setTimeout(() => {
      messageElement.style.display = 'none';
    }, 5000);
  } else {
    alert(message);
  }
};

// 성공 메시지 표시
const showSuccessMessage = (message) => {
  const messageElement = document.getElementById('clientMessage');
  if (messageElement) {
    messageElement.textContent = message;
    messageElement.className = 'alert-message alert-success';
    messageElement.style.display = 'block';

    // 3초 후 메시지 숨김
    setTimeout(() => {
      messageElement.style.display = 'none';
    }, 3000);
  }
};

// 기타 이벤트 처리 (변경 없음)
const handleBack = () => window.history.back();

const handlePhoneInput = (e, elements) => {
  e.target.value = e.target.value.replace(/\D/g, '');
  validateForm(elements);
};

const handlePasswordInput = (elements) => validateForm(elements);

const handleRegister = () => {
  const registerUrl = window.PAGE_CONFIG?.registerUrl || '/mobile/register';
  window.location.href = registerUrl;
};

const validateForm = (elements) => {
  const isPhoneValid = /^01[0-9]{8,9}$/.test(elements.phoneInput.value);
  const isPasswordValid = elements.passwordInput.value.length >= 4;
  const isFormValid = isPhoneValid && isPasswordValid;

  elements.loginButton.disabled = !isFormValid;
  elements.loginButton.style.backgroundColor = isFormValid ? '#ff9999' : '#ffcccc';
};

// 토큰 동기화 이벤트 리스너 (새로운 기능)
document.addEventListener('tokenSynced', (event) => {
  console.log('🔄 토큰 동기화 감지:', event.detail);

  // 토큰이 동기화되었다면 이미 로그인된 상태이므로 메인 페이지로 이동
  if (event.detail.accessToken && event.detail.refreshToken) {
    console.log('✅ 로그인 상태 감지, 메인 페이지로 이동');
    setTimeout(() => {
      const mainUrl = window.PAGE_CONFIG?.mainUrl || '/mobile/main';
      window.location.href = mainUrl;
    }, 500);
  }
});

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', initLoginPage);