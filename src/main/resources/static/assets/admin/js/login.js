// src/main/resources/static/js/admin-login.js

import {adminAuthFetch, handleTokenRefresh} from '../../../js/commonFetch.js';

document.addEventListener('DOMContentLoaded', () => {
  'use strict';

  // 비밀번호 표시/숨김 토글
  document.querySelectorAll('.toggle-password').forEach(btn => {
    btn.addEventListener('click', () => {
      const pwd = btn.previousElementSibling;
      const icon = btn.querySelector('i');
      if (pwd.type === 'password') {
        pwd.type = 'text';
        icon.classList.replace('fa-eye', 'fa-eye-slash');
      } else {
        pwd.type = 'password';
        icon.classList.replace('fa-eye-slash', 'fa-eye');
      }
    });
  });

  const loginBtn = document.getElementById('loginBtn');
  const recoverBtn = document.getElementById('btnRecoverPassword');
  const loginForm = document.getElementById('loginForm');


  loginBtn.addEventListener('click', onLoginSubmit);

  loginForm.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      onLoginSubmit(e);
    }
  });

  if (recoverBtn) {
    recoverBtn.addEventListener('click', onRecoverSubmit);
  }

});

async function onLoginSubmit(e) {
  e.preventDefault();
  clearError('username');
  clearError('password');
  clearError('general');

  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value.trim();
  const autoLogin = document.getElementById('remember-me').checked;  // boolean
  let valid = true;

  if (!username) {
    showError('username', '아이디를 입력해주세요.');
    valid = false;
  }
  if (!password) {
    showError('password', '비밀번호를 입력해주세요.');
    valid = false;
  }
  if (!valid) {
    return;
  }

  try {
    const res = await fetch('/admin/auth/login', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({username, password, autoLogin})
    });

    const json = await res.json();

    if (res.status === 401) {
      throw new Error(json.status.message || '로그인 정보가 올바르지 않습니다.');
    }
    if (!res.ok) {
      throw new Error('로그인에 실패했습니다.');
    }


    const token = json.response?.token;
    const refresh = json.response?.refreshToken;

    if (!token) {
      throw new Error('토큰이 전달되지 않았습니다.');
    }

    localStorage.setItem('adminAccessToken', token);
    if (refresh) {
      localStorage.setItem('adminRefreshToken', refresh);
    }

    // 1) 액세스 토큰 쿠키에 저장 (HttpOnly는 서버에서 Set-Cookie 헤더로 하는 게 보안상 좋습니다)
    // document.cookie = `ACCESS_TOKEN=${token}; Path=/; Secure; SameSite=Strict;`;
    // 2) 리프레시 토큰 쿠키에 저장
    // document.cookie = `REFRESH_TOKEN=${refresh}; Path=/; Secure; SameSite=Strict;`;

    // 3) 대시보드로 내비게이트
    window.location.href = '/admin/dashboard';
    // try{
    //   await adminAuthFetch(`/admin/dashboard`, {method: 'GET'})
    // }catch (err){
    //   console.error("리디렉션 실패 ")
    // }

  } catch (err) {
    showError('general', err.message);
  }
}

async function onRecoverSubmit(e) {
  e.preventDefault();
  clearError('recovery-username');
  clearError('recovery-phone');
  clearError('general');

  const username = document.getElementById('recovery-username').value.trim();
  const phone = document.getElementById('recovery-phone').value.trim();
  let valid = true;

  if (!username) {
    showError('recovery-username', '아이디를 입력해주세요.');
    valid = false;
  }
  if (!phone) {
    showError('recovery-phone', '휴대폰 번호를 입력해주세요.');
    valid = false;
  }
  if (!valid) {
    return;
  }

  try {
    let res = await fetch('/admin/auth/recover-password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('adminAccessToken')}`
      },
      body: JSON.stringify({username, phone})
    });

    // AccessToken 만료 시 리프레시 & 재요청
    if (res.status === 401) {
      await handleTokenRefresh('admin');
      res = await fetch('/admin/auth/recover-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('adminAccessToken')}`
        },
        body: JSON.stringify({username, phone})
      });
    }

    if (res.status === 404) {
      throw new Error('등록된 관리자 정보를 찾을 수 없습니다.');
    }
    if (!res.ok) {
      throw new Error('비밀번호 찾기에 실패했습니다.');
    }

    const json = await res.json();
    showGeneralSuccess(json.response?.message || '임시 비밀번호를 발송했습니다.');

  } catch (err) {
    showError('general', err.message);
  }
}

/**
 * fieldId: input 요소의 id 또는 'general'
 */
function showError(fieldId, msg) {
  if (fieldId === 'general') {
    const gen = document.getElementById('errorMsg');
    if (gen) {
      gen.textContent = msg;
      gen.classList.remove('d-none');
    } else {
      alert(msg);
    }
    return;
  }
  const field = document.getElementById(fieldId);
  if (!field) {
    return;
  }
  field.classList.add('is-invalid');
  let fb = field.nextElementSibling;
  if (!fb || !fb.classList.contains('invalid-feedback')) {
    fb = document.createElement('div');
    fb.classList.add('invalid-feedback');
    field.parentNode.appendChild(fb);
  }
  fb.textContent = msg;
}

function clearError(fieldId) {
  if (fieldId === 'general') {
    const gen = document.getElementById('errorMsg');
    if (gen) {
      gen.classList.add('d-none');
    }
    return;
  }
  const field = document.getElementById(fieldId);
  if (!field) {
    return;
  }
  field.classList.remove('is-invalid');
  const fb = field.nextElementSibling;
  if (fb && fb.classList.contains('invalid-feedback')) {
    fb.textContent = '';
  }
}

function showGeneralSuccess(msg) {
  const res = document.getElementById('recoverResult');
  const txt = document.getElementById('recoverMessage');
  if (res && txt) {
    res.classList.remove('alert-danger');
    res.classList.add('alert-success');
    txt.textContent = msg;
    res.classList.remove('d-none');
  } else {
    alert(msg);
  }
}
