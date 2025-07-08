// src/main/resources/static/js/login.js
import {
  authFetch,
  checkAuthAndRedirect,
  handleTokenRefresh
} from '../../../js/commonFetch.js'

// 초기화 함수
const initLoginPage = () => {
  console.log("Login page initializing...");
  checkAuthAndRedirect('../main/main.html');

  const elements = selectElements();
  registerEventListeners(elements);
  validateForm(elements);

  // 비밀번호 토글 기능 초기화
  initPasswordToggle();

  console.log('로그인 페이지가 초기화되었습니다.');
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
  autoLoginCheckbox: document.getElementById('autoLogin')
});

const registerEventListeners = (elements) => {
  elements.phoneInput.addEventListener('input',
      (e) => handlePhoneInput(e, elements));
  elements.passwordInput.addEventListener('input',
      () => handlePasswordInput(elements));
  elements.loginButton.addEventListener('click', () => handleLogin(elements));
  elements.registerButton.addEventListener('click', handleRegister);
  elements.backButton.addEventListener('click', handleBack);
};

// 로그인 처리 (수정된 로직)
const handleLogin = async (elements) => {
  const loginData = {
    phoneNumber: elements.phoneInput.value.trim(),
    password: elements.passwordInput.value,
    autoLogin: elements.autoLoginCheckbox.checked
  };

  // 로딩 상태 표시
  showLoadingState(elements.loginButton);

  try {
    const res = await jwtLogin(loginData);

    // 서버 응답 형식에 맞춰 토큰 저장
    localStorage.setItem('accessToken', res.response.token);
    localStorage.setItem('refreshToken', res.response.refreshToken);

    console.log('로그인 성공:', res.response.member);

    // 로그인 성공 시 바로 메인 페이지로 이동
    window.location.href = '../../../main/main.html';

  } catch (err) {
    console.error('로그인 실패:', err);
    showErrorMessage(err.message);
  } finally {
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

    // 3초 후 메시지 숨김
    setTimeout(() => {
      messageElement.style.display = 'none';
    }, 3000);
  } else {
    alert(message);
  }
};

// JWT 로그인 API 호출 (수정된 응답 구조 반영)
async function jwtLogin({phoneNumber, password, autoLogin}) {
  const res = await fetch(`/api/auth/login`, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({phoneNumber, password, autoLogin})
  });

  let result = await res.json();

  if (!res.ok || result.status.code !== 'OK_0000') {
    throw new Error(
        result.status.message || '로그인 정보가 올바르지 않습니다.');
  }

  return result; // 서버 응답 구조 전체 반환
}

// 기타 이벤트 처리 (변경 없음)
const handleBack = () => window.history.back();

const handlePhoneInput = (e, elements) => {
  e.target.value = e.target.value.replace(/\D/g, '');
  validateForm(elements);
};

const handlePasswordInput = (elements) => validateForm(elements);

const handleRegister = () => window.location.href = '../../../login/signup.html';

const validateForm = (elements) => {
  const isPhoneValid = /^01[0-9]{8,9}$/.test(elements.phoneInput.value);
  const isPasswordValid = elements.passwordInput.value.length >= 4;
  const isFormValid = isPhoneValid && isPasswordValid;
  elements.loginButton.disabled = !isFormValid;
  elements.loginButton.style.backgroundColor = isFormValid ? '#ff9999'
      : '#ffcccc';
};

document.addEventListener('DOMContentLoaded', initLoginPage);