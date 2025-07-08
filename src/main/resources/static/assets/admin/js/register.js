/**
 * 나라걱정 클럽 - 회원가입 페이지 JavaScript
 */

document.addEventListener('DOMContentLoaded', function() {
  'use strict';

  // ---- 토큰 자동 갱신 헬퍼 (commonFetch.js에서 export) ----
  // import { handleTokenRefresh } from '../js/commonFetch.js';
  // (만약 모듈로 관리하신다면 위처럼 import 하고, fetch 호출 시 401 처리에 이용)

  // 비밀번호 표시/숨김 토글
  document.querySelectorAll('.toggle-password').forEach(button => {
    button.addEventListener('click', function() {
      const input = this.previousElementSibling;
      const icon = this.querySelector('i');
      if (input.type === 'password') {
        input.type = 'text';
        icon.classList.replace('fa-eye', 'fa-eye-slash');
      } else {
        input.type = 'password';
        icon.classList.replace('fa-eye-slash', 'fa-eye');
      }
    });
  });

  // 폼 요소 가져오기
  const form        = document.getElementById('registerForm');
  const btnCheckId  = document.getElementById('checkUsername');
  const fldUsername = document.getElementById('username');
  const msgUsername = document.getElementById('usernameResult');
  const fldPassword = document.getElementById('password');
  const fldConfirm  = document.getElementById('confirmPassword');
  const msgPass     = document.getElementById('passwordMatch');
  const fldPhone    = document.getElementById('phoneNumber');
  const fldEmail    = document.getElementById('email');
  const chkAgree    = document.getElementById('agree');
  const btnSubmit   = document.getElementById('registerButton');
  const msgError    = document.getElementById('errorMsg');

  const pwComplexityRe = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*]).{8,}$/;

  let isUsernameAvailable = false;
  let isPasswordMatch     = false;

  // 아이디 중복 확인 버튼
  btnCheckId.addEventListener('click', async () => {
    const username = fldUsername.value.trim();
    const re       = /^[a-zA-Z0-9]{4,20}$/;
    if (!re.test(username)) {
      msgUsername.textContent = '아이디는 영문/숫자 4~20자이어야 합니다.';
      msgUsername.className   = 'form-text text-danger';
      isUsernameAvailable     = false;
      updateSubmitButton();
      return;
    }

    try {
      const res = await fetch(`/admin/auth/check/username?username=${encodeURIComponent(username)}`);
      if (!res.ok) throw new Error('네트워크 오류');
      const { response } = await res.json();

      if (response) {
        msgUsername.textContent = '사용 가능한 아이디입니다.';
        msgUsername.className   = 'form-text text-success';
      } else {
        msgUsername.textContent = '이미 사용 중인 아이디입니다.';
        msgUsername.className   = 'form-text text-danger';
      }
      isUsernameAvailable = response;
    } catch (err) {
      msgUsername.textContent = '중복 확인 중 오류가 발생했습니다.';
      msgUsername.className   = 'form-text text-danger';
      isUsernameAvailable     = false;
    }
    updateSubmitButton();
  });

  // 아이디 입력 시 다시 확인 필요
  fldUsername.addEventListener('input', () => {
    isUsernameAvailable = false;
    msgUsername.textContent = '아이디 중복 확인이 필요합니다.';
    msgUsername.className   = 'form-text text-muted';
    updateSubmitButton();
  });

  // 비밀번호/확인 일치 검사
  [fldPassword, fldConfirm].forEach(el =>
    el.addEventListener('input', () => {
      const pw = fldPassword.value;
      const cf = fldConfirm.value;

      // 1) 복잡도 체크
      if (pw && !pwComplexityRe.test(pw)) {
        isPasswordMatch = false;
        msgPass.textContent = '영문, 숫자, 특수문자를 포함하여 8자 이상으로 입력해주세요.';
        msgPass.className   = 'form-text text-danger';
        fldPassword.classList.add('is-invalid');
        updateSubmitButton();
        return;
      } else {
        fldPassword.classList.remove('is-invalid');
      }

      //2) 일치 검사
      const ok = pw !== '' && pw === fldConfirm.value;
      isPasswordMatch = ok;

      if (fldConfirm.value === '') {
        msgPass.textContent = '';
      } else if (ok) {
        msgPass.textContent = '비밀번호가 일치합니다.';
        msgPass.className   = 'form-text text-success';
        fldConfirm.classList.remove('is-invalid');
      } else {
        msgPass.textContent = '비밀번호가 일치하지 않습니다.';
        msgPass.className   = 'form-text text-danger';
        fldConfirm.classList.add('is-invalid');
      }

      updateSubmitButton();
    })
  );

  // 약관 동의 시 버튼 상태 갱신
  chkAgree.addEventListener('change', updateSubmitButton);

  // 모든 조건 갖춰졌는지 확인하여 버튼 활성화
  function updateSubmitButton() {
    btnSubmit.disabled = !(
      isUsernameAvailable &&
      isPasswordMatch &&
      chkAgree.checked
    );
  }

  // 실제 회원가입 API 요청
  async function register(data) {
    const res = await fetch('/admin/auth/register', {
      method: 'POST',
      headers: {'Content-Type':'application/json'},
      body: JSON.stringify(data)
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || '회원가입에 실패했습니다.');
    }
    const json = await res.json();
    const token = json.response?.token;
    const refresh = json.response?.refreshToken;

    if (!token) {
      throw new Error('토큰이 전달되지 않았습니다.');
    }

    localStorage.setItem('adminAccessToken', token);
    if (refresh) {
      localStorage.setItem('adminRefreshToken', refresh);
    }
    alert("회원가입이 완료 되었습니다.")
    // 로그인 후 이동
    window.location.href = '/admin/dashboard';
  }

  // 폼 제출
  form.addEventListener('submit', async e => {
    e.preventDefault();
    msgError.classList.add('d-none');

    // 기본 HTML5 검증
    if (!form.checkValidity()) {
      form.querySelectorAll(':invalid').forEach(f => f.classList.add('is-invalid'));
      return;
    }

    // 추가 유효성
    if (!isUsernameAvailable || !isPasswordMatch) {
      return;
    }

    // 페이로드
    const payload = {
      username:    fldUsername.value.trim(),
      password:    fldPassword.value,
      phoneNumber: fldPhone.value.trim(),
      email:       fldEmail.value.trim()
    };

    try {
      await register(payload);

    } catch (err) {
      msgError.textContent = err.message;
      msgError.classList.remove('d-none');
    }
  });

  // 초기 버튼 상태
  updateSubmitButton();
});
