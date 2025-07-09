// DOM이 로드된 후 실행될 함수
document.addEventListener('DOMContentLoaded', () => {
  const elements = {
    form: document.getElementById('signupForm'),
    fullName: document.getElementById('fullName'),
    phoneNumber: document.getElementById('phoneNumber'),
    verificationCode: document.getElementById('verificationCode'),
    sendCodeButton: document.getElementById('sendCodeButton'),
    verifyCodeButton: document.getElementById('verifyCodeButton'),
    password: document.getElementById('password'),
    confirmPassword: document.getElementById('confirmPassword'),
    email: document.getElementById('email'),
    agreeAll: document.getElementById('agreeAll'),
    agreeService: document.getElementById('agreeService'),
    agreePrivacy: document.getElementById('agreePrivacy'),
    agreeMarketing: document.getElementById('agreeMarketing'),
    signupButton: document.getElementById('signupButton'),
    backButton: document.querySelector('.back-button'),
    timerDisplay: document.getElementById('timerDisplay'), // 타이머를 보여줄 요소

    // 에러 메시지
    nameError: document.getElementById('nameError'),
    phoneError: document.getElementById('phoneError'),
    codeError: document.getElementById('codeError'),
    passwordError: document.getElementById('passwordError'),
    confirmPasswordError: document.getElementById('confirmPasswordError'),
    emailError: document.getElementById('emailError')
  };

  // 타이머 요소 참조 업데이트
  elements.timerDisplay = document.getElementById('timerDisplay');

  // 카운트다운 타이머 변수
  let countdownTimer = null;
  let remainingSeconds = 180; // 3분 = 180초

  // 타이머 시작 함수
  const startTimer = () => {
    clearInterval(countdownTimer); // 기존 타이머가 있다면 제거
    remainingSeconds = 180; // 3분으로 초기화
    updateTimerDisplay(); // 타이머 표시 업데이트

    countdownTimer = setInterval(() => {
      remainingSeconds--;
      updateTimerDisplay();

      if (remainingSeconds <= 0) {
        clearInterval(countdownTimer);
        elements.timerDisplay.textContent = '시간 초과';
        elements.sendCodeButton.disabled = false;
        elements.sendCodeButton.textContent = '재발송';
      }
    }, 1000);
  };

  // 타이머 표시 업데이트 함수
  const updateTimerDisplay = () => {
    const minutes = Math.floor(remainingSeconds / 60);
    const seconds = remainingSeconds % 60;
    elements.timerDisplay.textContent = `${minutes}:${seconds < 10 ? '0'
      : ''}${seconds}`;
  };

  // 타이머 중지 함수
  const stopTimer = () => {
    clearInterval(countdownTimer);
    elements.timerDisplay.textContent = '';
  };

  // 전화번호 입력 처리
  elements.phoneNumber.addEventListener('input', (e) => {
    // 숫자만 입력 가능하도록
    e.target.value = e.target.value.replace(/\D/g, '');
    validatePhone(elements);
  });

  // 1) 인증번호 발송
  let one_userKey = '';
  elements.sendCodeButton.addEventListener('click', async () => {
    if (!validatePhone(elements)) {
      return;
    }

    elements.sendCodeButton.disabled = true;
    elements.codeError.style.display = 'none';

    try {
      // 1-1) 타임스탬프 생성
      smsTimestamp = Math.floor(Date.now() / 1000).toString();

      // 1-2) API 호출
      const res = await fetch('/api/auth/smsCert/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phoneNumber: elements.phoneNumber.value })
      });

      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }
      const body = await res.json();
      console.log('cert req : {}', body);

      // 1-3) 서버 응답 확인
      if (body.status.code === 'OK_0000' && body.response.result) {
        // 성공: 인증번호 입력창 활성화 + 인증하기 버튼 활성화
        elements.verificationCode.disabled = false;
        elements.verifyCodeButton.disabled = false;

        one_userKey = body.response.value.userKey;

        // 버튼 텍스트 변경 및 타이머 시작
        elements.sendCodeButton.textContent = '재발송';
        startTimer();

        alert('인증번호가 발송되었습니다. 3분 내에 인증을 완료해주세요.');
      } else {
        let displayText = '';
        switch (body.response.code) {
          case 1034:
            displayText = '이미 가입된 번호 입니다.'
            break;
          default:
            displayText = body.response.message || '인증번호 발송에 실패했습니다.'
        }
        // 실패: 에러 메시지 표시
        elements.codeError.textContent = displayText;
        alert(displayText);
        elements.codeError.style.display = 'block';
        elements.sendCodeButton.disabled = false;
      }
    } catch (err) {
      console.error(err);
      elements.codeError.textContent = '네트워크 오류가 발생했습니다.';
      elements.codeError.style.display = 'block';
      elements.sendCodeButton.disabled = false;
    }
  });

  // 2) 인증번호 검증
  let one_certi_status = false;
  elements.verifyCodeButton.addEventListener('click', async () => {
    const code = elements.verificationCode.value.trim();
    if (code.length !== 6) {
      elements.codeError.textContent = '6자리 인증번호를 입력해주세요.';
      elements.codeError.style.display = 'block';
      return;
    }

    elements.verifyCodeButton.disabled = true;
    elements.codeError.style.display = 'none';

    try {
      // 2-1) 검증 API 호출
      const res = await fetch('/api/auth/smsCert/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          phoneNumber: elements.phoneNumber.value,
          verificationCode: code,
          userKey: one_userKey,
        })
      });

      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }
      const body = await res.json();

      console.log('certSuccess:{}', body);

      // 2-2) 검증 결과 처리
      if (body.status.code === 'OK_0000' && body.response.value) {
        // 최종 성공: 입력창·버튼 전부 비활성화 및 타이머 중지
        one_certi_status = body.response.value;

        // 인증 완료 상태 표시
        elements.verificationCode.disabled = true;
        elements.verifyCodeButton.disabled = true;
        elements.sendCodeButton.disabled = true;
        elements.phoneNumber.disabled = true; // 전화번호도 비활성화

        // 버튼 스타일 변경 - 회색으로
        elements.verifyCodeButton.style.backgroundColor = '#cccccc';
        elements.sendCodeButton.style.backgroundColor = '#cccccc';

        // 타이머 중지 및 성공 메시지 표시
        stopTimer();
        elements.timerDisplay.textContent = '인증 완료';
        elements.timerDisplay.style.color = '#28a745';

        alert('휴대폰 번호 인증이 완료되었습니다.');
      } else {
        elements.codeError.textContent = body.response.message || '인증 실패';
        elements.codeError.style.display = 'block';
        elements.verifyCodeButton.disabled = false;
      }
    } catch (err) {
      console.error(err);
      elements.codeError.textContent = '네트워크 오류가 발생했습니다.';
      elements.codeError.style.display = 'block';
      elements.verifyCodeButton.disabled = false;
    }
  });


  // 비밀번호 입력 처리
  elements.password.addEventListener('input', () => {
    validatePassword(elements);
    if (elements.confirmPassword.value) {
      validateConfirmPassword(elements);
    }
  });

  // 비밀번호 확인 입력 처리
  elements.confirmPassword.addEventListener('input', () => {
    validateConfirmPassword(elements);
  });

  // 이메일 입력 처리
  elements.email.addEventListener('input', () => {
    if (elements.email.value) {
      validateEmail(elements);
    } else {
      elements.emailError.style.display = 'none';
    }
  });

  // 전체 동의 체크박스 처리
  elements.agreeAll.addEventListener('change', () => {
    const isChecked = elements.agreeAll.checked;
    elements.agreeService.checked = isChecked;
    elements.agreePrivacy.checked = isChecked;
    elements.agreeMarketing.checked = isChecked;
    validateForm(elements);
  });

  // 개별 약관 체크박스 처리
  [elements.agreeService, elements.agreePrivacy,
  elements.agreeMarketing].forEach(checkbox => {
    checkbox.addEventListener('change', () => {
      // 모든 약관이 체크되었는지 확인
      elements.agreeAll.checked =
        elements.agreeService.checked &&
        elements.agreePrivacy.checked &&
        elements.agreeMarketing.checked;
      validateForm(elements);
    });
  });

  // 폼 제출 처리
  elements.form.addEventListener('submit', async (e) => {
    e.preventDefault();
    // 휴대폰 인증이 완료되었는지 확인
    if (!one_certi_status) {
      alert('휴대폰 인증을 완료해주세요.');
      return;
    }

    if (validateForm(elements)) {
      // 회원가입 데이터 구성
      const signupData = {
        name: elements.fullName.value,
        phoneNumber: elements.phoneNumber.value,
        verificationCode: elements.verificationCode.value,
        userKey: one_userKey,
        password: elements.password.value,
        email: elements.email.value,
        marketingAgree: elements.agreeMarketing.checked
      };

      console.log('회원가입 데이터:', signupData);

      try {
        // 2-1) 검증 API 호출
        const res = await fetch('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(signupData)
        });

        if (!res.ok) {
          throw new Error(`HTTP ${res.status}`);
        }
        const body = await res.json();

        console.log('registerSuccess:{}', body);

        // 2-2) 검증 결과 처리
        if (body.status.code === 'OK_0000') {
          localStorage.setItem('accessToken', res.response.token);
          localStorage.setItem('refreshToken', res.response.refreshToken);
          // 최종 성공: 화면 이동
          alert('휴대폰 번호 인증이 완료되었습니다.');
          window.location.href = '../main/main.html';
        } else {
          alert(body.response.message || '인증 실패');

        }
      } catch (err) {
        console.error(err);
        elements.codeError.textContent = '네트워크 오류가 발생했습니다.';
        elements.codeError.style.display = 'block';
        elements.verifyCodeButton.disabled = false;
      }
    }
  });

  // 뒤로가기 버튼 클릭 처리
  elements.backButton.addEventListener('click', () => {
    // 실제 구현 시 이전 페이지로 이동
    window.history.back();
  });

  // 입력 필드 이벤트 등록
  elements.fullName.addEventListener('input', () => validateName(elements));
  elements.verificationCode.addEventListener('input',
    () => validateVerificationCode(elements));

  // 모든 입력 필드에 대한 검증 이벤트 등록
  const allInputs = [
    elements.fullName, elements.phoneNumber, elements.verificationCode,
    elements.password, elements.confirmPassword, elements.email
  ];

  allInputs.forEach(input => {
    input.addEventListener('input', () => validateForm(elements));
  });
});

// 이름 검증 함수
const validateName = (elements) => {
  const isValid = elements.fullName.value.trim().length >= 2;
  elements.nameError.style.display = isValid ? 'none' : 'block';
  return isValid;
};

let smsTimestamp = null;

// 전화번호 유효성 검사 (기존 함수)
function validatePhone(elements) {
  const re = /^01[016789]\d{7,8}$/;
  const ok = re.test(elements.phoneNumber.value);
  elements.phoneError.style.display = ok ? 'none' : 'block';
  return ok;
}

// 인증번호 검증 함수
const validateVerificationCode = (elements) => {
  // 6자리 숫자인지 확인
  const isValid = /^\d{6}$/.test(elements.verificationCode.value);
  elements.codeError.style.display = isValid ? 'none' : 'block';
  return isValid;
};

// // 아이디 검증 함수
// const validateUserId = (elements) => {
//   // 영문, 숫자 조합 4-20자
//   const isValid = /^[a-zA-Z0-9]{4,20}$/.test(elements.userId.value);
//   elements.userIdError.style.display = isValid ? 'none' : 'block';
//   return isValid;
// };

// 비밀번호 검증 함수
const validatePassword = (elements) => {
  // 영문, 숫자, 특수문자 조합 8-20자
  const isValid = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*()]).{8,20}$/.test(
    elements.password.value);
  elements.passwordError.style.display = isValid ? 'none' : 'block';
  return isValid;
};

// 비밀번호 확인 검증 함수
const validateConfirmPassword = (elements) => {
  const isValid = elements.password.value === elements.confirmPassword.value;
  elements.confirmPasswordError.style.display = isValid ? 'none' : 'block';
  return isValid;
};

// 이메일 검증 함수
const validateEmail = (elements) => {
  // 이메일 형식 검사
  const isValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(elements.email.value);
  elements.emailError.style.display = isValid ? 'none' : 'block';
  return isValid || elements.email.value === '';
};

// 폼 전체 검증 함수
const validateForm = (elements) => {
  const isNameValid = validateName(elements);
  const isPhoneValid = validatePhone(elements);
  const isCodeValid = validateVerificationCode(elements);
  // const isUserIdValid = validateUserId(elements);
  const isPasswordValid = validatePassword(elements);
  const isConfirmPasswordValid = validateConfirmPassword(elements);
  const isEmailValid = validateEmail(elements);
  const isTermsValid = elements.agreeService.checked
    && elements.agreePrivacy.checked;

  // 모든 필드가 유효할 때만 버튼 활성화
  const isFormValid = isNameValid && isPhoneValid && isCodeValid &&
    isPasswordValid && isConfirmPasswordValid &&
    isEmailValid && isTermsValid;

  elements.signupButton.disabled = !isFormValid;

  if (isFormValid) {
    elements.signupButton.style.backgroundColor = '#ff9999';
  } else {
    elements.signupButton.style.backgroundColor = '#ffcccc';
  }

  return isFormValid;
};