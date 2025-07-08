// src/main/resources/static/js/login.js
import {
  authFetch,
  checkAuthAndRedirect,
  handleTokenRefresh
} from '../../../js/commonFetch.js'

let inviteModal;
let isNoInviteFlow = false;
let inviteCodeToast;

// 초기화 함수
const initLoginPage = () => {
  console.log("Login page initializing...");
  checkAuthAndRedirect('../main/main.html');

  const elements = selectElements();
  registerEventListeners(elements);
  validateForm(elements);

  // 비밀번호 토글 기능 초기화
  initPasswordToggle();

  // 토스트 초기화
  inviteCodeToast = new bootstrap.Toast(
      document.getElementById('inviteCodeToast'));

  // URL에서 초대 코드 확인
  checkInviteCodeFromUrl();

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

// URL에서 초대 코드 파라미터 확인
const checkInviteCodeFromUrl = () => {
  const urlParams = new URLSearchParams(window.location.search);
  const inviteCode = urlParams.get('code');

  if (inviteCode) {
    // 초대 코드를 localStorage에 저장
    localStorage.setItem('pendingInviteCode', inviteCode);

    // 토스트 메시지 표시
    inviteCodeToast.show();

    // URL에서 초대 코드 파라미터 제거 (히스토리에 남지 않게)
    const newUrl = window.location.pathname;
    window.history.replaceState({}, document.title, newUrl);
  }
};

const selectElements = () => ({
  phoneInput: document.querySelector('.phone-input'),
  passwordInput: document.getElementById('password'),
  loginButton: document.getElementById('verifyButton'),
  registerButton: document.getElementById('registerButton'),
  backButton: document.querySelector('.back-button'),
  autoLoginCheckbox: document.getElementById('autoLogin'),
  inviteSubmitBtn: document.getElementById('inviteSubmitBtn'),
  inviteCodeInput: document.getElementById('inviteCodeInput'),
  inviteError: document.getElementById('inviteError'),
  noInviteLink: document.getElementById('noInviteLink'),
  inviteCancelBtn: document.getElementById('inviteCancelBtn'),
  inviteModalEl: document.getElementById('inviteModal')
});

const registerEventListeners = (elements) => {
  elements.phoneInput.addEventListener('input',
      (e) => handlePhoneInput(e, elements));
  elements.passwordInput.addEventListener('input',
      () => handlePasswordInput(elements));
  elements.loginButton.addEventListener('click', () => handleLogin(elements));
  elements.registerButton.addEventListener('click', handleRegister);
  elements.backButton.addEventListener('click', handleBack);
  elements.inviteSubmitBtn.addEventListener('click',
      () => handleInviteSubmit(elements));
  elements.noInviteLink.addEventListener('click', (e) => {
    e.preventDefault();
    handleNoInvite(elements);
  });
};

// 로그인 처리 (서버 응답에 따른 처리 변경)
const handleLogin = async (elements) => {
  const loginData = {
    phoneNumber: elements.phoneInput.value.trim(),
    password: elements.passwordInput.value,
    autoLogin: elements.autoLoginCheckbox.checked
  };
  try {
    const res = await jwtLogin(loginData);

    // 서버 응답 형식에 맞춰 토큰 저장
    localStorage.setItem('accessToken', res.response.token);
    localStorage.setItem('refreshToken', res.response.refreshToken);

    console.log(res.response.member)

    switch (res.response.member.status) {
      case 'TEMPORARY_INVITE':
        inviteModal = new bootstrap.Modal(elements.inviteModalEl);

        // localStorage에 저장된 초대 코드가 있는지 확인
        const savedInviteCode = localStorage.getItem('pendingInviteCode');
        if (savedInviteCode) {
          // 초대 코드 입력란에 자동으로 입력
          elements.inviteCodeInput.value = savedInviteCode;
        }

        inviteModal.show();
        break;
      case 'TEMPORARY_PASS':
        alert('신원 인증이 필요합니다.');
        alert('신원 인증이 필요합니다.');
        try {
          const res = await authFetch('/api/auth/pass/prepare',
              {method: 'POST'});
          const data = await res.json();
          const formData = {
            enc_data: data.response.encData,
            token_version_id: data.response.tokenVersionId,
            integrity_value: data.response.integrityValue,
            m: 'service'
          }
          submitFormToNice(data.response.requestUrl, formData);
        } catch (err) {
          alert('인증 요청 실패: ' + err.message);
        }
        break;
      case 'ACTIVE':
        window.location.href = '../../../main/main.html';
        break;
      default:
        alert("회원님의 계정이 삭제 되었거나. 임시 정지 되었습니다. 고객센터에 문의 부탁드립니다.");
        break;
    }

  } catch (err) {
    alert(err.message);
  }
};

// 초대 코드 확인 및 제출
const handleInviteSubmit = async (elements) => {
  if (isNoInviteFlow) {
    window.location.href = '../../../main/main.html';
    return;
  }
  const code = elements.inviteCodeInput.value.trim().toUpperCase();
  if (!/^[A-Z0-9]{7}$/.test(code)) {
    elements.inviteError.textContent = '7자리 대문자+숫자로 입력해주세요.';
    elements.inviteError.style.display = 'block';
    return;
  }
  elements.inviteError.style.display = 'none';
  try {
    const result = await submitInviteCode(code);
    // 초대 코드 제출 성공 시 localStorage에서 삭제
    localStorage.removeItem('pendingInviteCode');
    const updatedMemberStatus = result.response.status;

    if (updatedMemberStatus === 'TEMPORARY_PASS') {
      alert('신원 인증이 필요합니다.');
      try {
        const res = await authFetch('/api/auth/pass/prepare', {method: 'POST'});
        const data = await res.json();

        const formData = {
          enc_data: data.response.encData,
          token_version_id: data.response.tokenVersionId,
          integrity_value: data.response.integrityValue,
          m: 'service'
        }
        submitFormToNice(data.response.requestUrl, formData);
      } catch (err) {
        alert('인증 요청 실패: ' + err.message);
      }
    } else {
      alert('초대 코드 등록이 완료되었습니다. 메인 페이지로 이동합니다.');
      window.location.href = '../../../main/main.html';
    }
  } catch (err) {
    elements.inviteError.textContent = err.message;
    elements.inviteError.style.display = 'block';
  }
};

const handleNoInvite = (elements) => {
  isNoInviteFlow = true;
  const titleEl = elements.inviteModalEl.querySelector('.modal-title');
  titleEl.textContent = '운영진 검토가 진행중입니다.';
  elements.inviteCodeInput.style.display = 'none';
  elements.inviteError.style.display = 'none';
  elements.noInviteLink.style.display = 'none';
  elements.inviteCancelBtn.style.display = 'none';
  elements.inviteSubmitBtn.textContent = '확인';

  // '초대코드 없음' 선택 시에도 pendingInviteCode 삭제
  localStorage.removeItem('pendingInviteCode');
};

// JWT 로그인 API 호출 개선 (응답 구조 반영)
async function jwtLogin({phoneNumber, password, autoLogin}) {
  const res = await fetch(`/api/auth/login`, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({phoneNumber, password, autoLogin})
  });

  let result = await res.json();
  if (result.status.code !== 'OK_0000') {
    throw new Error(
        result.status.message || '로그인 정보가 올바르지 않습니다.');
  }
  if (!res.ok) {
    throw new Error('로그인 실패: ' + res.statusText);
  }
  return result; // 서버 응답 구조 전체 반환
}

function submitFormToNice(requestUrl, encodeData) {
  const form = document.createElement('form');
  form.method = 'POST';
  form.action = requestUrl;
  form.style.display = 'none';

  Object.entries(encodeData).forEach(([key, value]) => {
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = key;
      input.value = value;
      form.appendChild(input);
    });

  document.body.appendChild(form);
  form.submit();
}

// 초대 코드 제출 요청 (Refresh Token 활용하여 401 대응)
async function submitInviteCode(code) {
  const token = localStorage.getItem('accessToken');
  let res = await fetch('/api/members/invite', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({inviteCode: code})
  });

  // Access Token 만료 시 Refresh Token으로 재발급 후 재요청
  if (res.status === 401) {
    await handleTokenRefresh();
    const newToken = localStorage.getItem('accessToken');
    res = await fetch('/api/members/invite', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${newToken}`
      },
      body: JSON.stringify({inviteCode: code})
    });
  }

  if (!res.ok) {
    const errorData = await res.json();
    throw new Error(
        errorData.status?.message || '초대 코드 등록 실패: ' + res.statusText);
  }

  return await res.json();
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