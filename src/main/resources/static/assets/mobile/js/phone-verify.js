// phone-verify.js - 핸드폰 번호 인증 페이지 (API 명세 적용)

import { showToast } from './common.js';

// ===== 전역 변수 =====
let isSubmitting = false;
let timerInterval = null;
let timeLeft = 180; // 3분 (180초)
let verificationSent = false;
let verificationConfirmed = false;
let userKey = ''; // SMS 발송 시 받은 userKey

// ===== 초기화 함수 =====
const initPhoneVerifyPage = () => {
  console.log("🚀 핸드폰 번호 인증 페이지 초기화");

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

  console.log('✅ 핸드폰 번호 인증 페이지 초기화 완료');
};

// ===== DOM 요소 초기화 =====
const initializeElements = () => {
  const elements = {
    // 입력 필드
    countryCode: document.getElementById('countryCode'),
    phoneNumber: document.getElementById('phoneNumber'),
    verificationCode: document.getElementById('verificationCode'),

    // 버튼
    verifyRequestButton: document.getElementById('verifyRequestButton'),
    confirmButton: document.getElementById('confirmButton'),
    nextButton: document.getElementById('nextButton'),

    // 섹션
    verificationSection: document.getElementById('verificationSection'),
    timer: document.getElementById('timer'),

    // 모달
    alertModal: document.getElementById('alertModal'),
    alertMessage: document.getElementById('alertMessage'),
    alertConfirm: document.getElementById('alertConfirm')
  };

  // 필수 요소 체크
  const requiredElements = ['phoneNumber', 'verifyRequestButton', 'nextButton'];
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

  // 핸드폰 번호 입력 처리
  elements.phoneNumber.addEventListener('input', (e) => {
    handlePhoneInput(e, elements);
  });

  // 인증번호 입력 처리
  if (elements.verificationCode) {
    elements.verificationCode.addEventListener('input', (e) => {
      handleVerificationCodeInput(e, elements);
    });
  }

  // 인증 버튼 클릭
  elements.verifyRequestButton.addEventListener('click', () => {
    handleVerifyRequest(elements);
  });

  // 확인 버튼 클릭
  if (elements.confirmButton) {
    elements.confirmButton.addEventListener('click', () => {
      handleConfirm(elements);
    });
  }

  // 다음 버튼 클릭
  elements.nextButton.addEventListener('click', () => {
    handleNext(elements);
  });

  // Enter 키 처리
  elements.phoneNumber.addEventListener('keypress', (e) => {
    if (e.key === 'Enter' && !elements.verifyRequestButton.disabled) {
      handleVerifyRequest(elements);
    }
  });

  if (elements.verificationCode) {
    elements.verificationCode.addEventListener('keypress', (e) => {
      if (e.key === 'Enter' && elements.confirmButton && !elements.confirmButton.disabled) {
        handleConfirm(elements);
      }
    });
  }

  console.log('✅ 이벤트 리스너 등록 완료');
};

// ===== 폼 초기 상태 설정 =====
const initializeFormState = (elements) => {
  // 핸드폰 번호 입력 필드에 포커스
  elements.phoneNumber.focus();

  // 버튼 상태 업데이트
  updateVerifyRequestButton(elements);
  updateNextButton(elements);
};

// ===== 모달 초기화 =====
const initModals = () => {
  const elements = initializeElements();

  // 알림 모달 확인 버튼
  elements.alertConfirm.addEventListener('click', () => {
    hideAlertModal();
  });

  // 모달 배경 클릭 시 닫기
  elements.alertModal.addEventListener('click', (e) => {
    if (e.target === elements.alertModal) {
      hideAlertModal();
    }
  });
};

// ===== 입력 처리 함수들 =====
const handlePhoneInput = (e, elements) => {
  let value = e.target.value.replace(/\D/g, '');

  // 11자리 제한
  if (value.length > 11) {
    value = value.slice(0, 11);
  }

  e.target.value = value;

  // 버튼 상태 업데이트
  updateVerifyRequestButton(elements);
};

const handleVerificationCodeInput = (e, elements) => {
  // 숫자만 허용
  let value = e.target.value.replace(/\D/g, '');

  // 6자리 제한
  if (value.length > 6) {
    value = value.slice(0, 6);
  }

  e.target.value = value;

  // 6자리 입력 완료 시 자동으로 확인 버튼 활성화
  updateConfirmButton(elements);
};

// ===== 버튼 상태 업데이트 =====
const updateVerifyRequestButton = (elements) => {
  const phoneValue = elements.phoneNumber.value.replace(/\D/g, '');
  const isPhoneValid = phoneValue.length >= 10; // 최소 10자리

  elements.verifyRequestButton.disabled = !isPhoneValid || isSubmitting;

  // 인증번호가 이미 발송된 경우 버튼 텍스트 변경
  if (verificationSent) {
    const buttonText = elements.verifyRequestButton.querySelector('.button-text');
    if (buttonText) {
      buttonText.textContent = '재발송';
    }
  }
};

const updateConfirmButton = (elements) => {
  if (!elements.confirmButton) return;

  const codeValue = elements.verificationCode.value.trim();
  const isCodeValid = codeValue.length === 6;

  elements.confirmButton.disabled = !isCodeValid || isSubmitting;
};

const updateNextButton = (elements) => {
  const shouldEnable = verificationConfirmed && !isSubmitting;

  console.log('🔄 다음 버튼 상태 업데이트:', {
    verificationConfirmed,
    isSubmitting,
    shouldEnable
  });

  elements.nextButton.disabled = !shouldEnable;

  if (shouldEnable) {
    elements.nextButton.classList.remove('disabled');
    elements.nextButton.classList.add('enabled');
  } else {
    elements.nextButton.classList.remove('enabled');
    elements.nextButton.classList.add('disabled');
  }

  console.log('✅ 다음 버튼 최종 상태 - disabled:', elements.nextButton.disabled);
};

// ===== 인증요청 처리 (실제 API 연동) =====
const handleVerifyRequest = async (elements) => {
  if (isSubmitting) return;

  const phoneNumber = elements.phoneNumber.value.replace(/\D/g, '');
  console.log('📱 인증번호 요청:', phoneNumber);

  if (!phoneNumber || phoneNumber.length < 10) {
    showAlert('올바른 핸드폰 번호를 입력해주세요.');
    return;
  }

  isSubmitting = true;
  showLoadingState(elements.verifyRequestButton);

  try {
    // SMS 인증번호 발송 API 호출
    const response = await fetch('/api/auth/smsCert/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        phoneNumber: phoneNumber
      })
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    console.log('SMS 발송 응답:', data);

    if (data.status.code === 'OK_0000' && data.response.result) {
      // 성공: userKey 저장 및 인증번호 입력 섹션 표시
      userKey = data.response.value.userKey;
      verificationSent = true;

      showVerificationSection(elements);
      startTimer(elements);
      showAlert('인증번호가 발송되었습니다. 3분 내에 인증을 완료해주세요.');

      // 핸드폰 번호 입력 비활성화
      elements.phoneNumber.disabled = true;
      updateVerifyRequestButton(elements);

      // 인증번호 입력 필드에 포커스
      setTimeout(() => {
        if (elements.verificationCode) {
          elements.verificationCode.focus();
        }
      }, 500);

    } else {
      // 실패: 에러 메시지 표시
      let errorMessage = '인증번호 발송에 실패했습니다.';

      if (data.response.code === 1034) {
        errorMessage = '이미 가입된 번호입니다.';
      } else if (data.response.message) {
        errorMessage = data.response.message;
      }

      showAlert(errorMessage);
    }

  } catch (error) {
    console.error('💥 인증번호 발송 오류:', error);
    showAlert('네트워크 오류가 발생했습니다. 다시 시도해주세요.');
  } finally {
    isSubmitting = false;
    hideLoadingState(elements.verifyRequestButton);
  }
};

// ===== 확인 버튼 처리 (실제 API 연동) =====
const handleConfirm = async (elements) => {
  if (isSubmitting) return;

  const verificationCode = elements.verificationCode.value.trim();
  const phoneNumber = elements.phoneNumber.value.replace(/\D/g, '');

  console.log('🔐 인증번호 확인:', verificationCode);

  if (!verificationCode || verificationCode.length !== 6) {
    showAlert('6자리 인증번호를 입력해주세요.');
    return;
  }

  if (!userKey) {
    showAlert('인증번호를 다시 요청해주세요.');
    return;
  }

  isSubmitting = true;
  showLoadingState(elements.confirmButton);

  try {
    // SMS 인증번호 확인 API 호출
    const response = await fetch('/api/auth/smsCert/verify', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        phoneNumber: phoneNumber,
        verificationCode: verificationCode,
        userKey: userKey
      })
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    console.log('SMS 인증 응답:', data);

    if (data.status.code === 'OK_0000' && data.response.value) {
      // 인증 성공
      console.log('✅ 인증번호 확인 성공');
      verificationConfirmed = true;
      stopTimer();

      // 인증 완료 상태로 UI 업데이트
      elements.verificationCode.disabled = true;
      if (elements.confirmButton) {
        elements.confirmButton.disabled = true;
      }
      if (elements.timer) {
        elements.timer.textContent = '인증 완료';
        elements.timer.style.color = 'var(--success)';
      }

      // 🔥 인증 완료 즉시 sessionStorage에 저장
      const verificationData = {
        phoneNumber: phoneNumber,
        countryCode: elements.countryCode.value,
        verificationCode: verificationCode,
        userKey: userKey,
        verifiedAt: new Date().toISOString()
      };

      try {
        sessionStorage.setItem('phoneVerification', JSON.stringify(verificationData));
        console.log('📝 인증 정보 즉시 저장 완료:', verificationData);
      } catch (storageError) {
        console.error('❌ 인증 정보 저장 실패:', storageError);
        showAlert('인증 정보 저장에 실패했습니다. 다시 시도해주세요.');
        return;
      }

      // 다음 버튼 활성화 - 강제로 상태 업데이트
      console.log('🔄 다음 버튼 활성화 중...');
      elements.nextButton.disabled = false;

      // CSS 클래스로도 상태 업데이트
      elements.nextButton.classList.remove('disabled');
      elements.nextButton.classList.add('enabled');

      console.log('✅ 다음 버튼 활성화 완료. disabled:', elements.nextButton.disabled);

      showAlert('인증이 완료되었습니다. 다음 단계로 진행해주세요.');

    } else {
      // 인증 실패
      const errorMessage = data.response.message || '인증번호가 일치하지 않습니다.';
      showAlert(errorMessage);

      // 인증번호 입력 필드 포커스 및 선택
      if (elements.verificationCode) {
        elements.verificationCode.focus();
        elements.verificationCode.select();
      }
    }

  } catch (error) {
    console.error('💥 인증번호 확인 오류:', error);
    showAlert('네트워크 오류가 발생했습니다. 다시 시도해주세요.');
  } finally {
    isSubmitting = false;
    hideLoadingState(elements.confirmButton);
  }
};

// ===== 다음 버튼 처리 =====
const handleNext = async (elements) => {
  if (isSubmitting || !verificationConfirmed) return;

  console.log('🚀 다음 단계로 이동');

  // sessionStorage에 데이터가 이미 저장되어 있는지 확인
  const existingData = sessionStorage.getItem('phoneVerification');
  if (!existingData) {
    console.error('❌ sessionStorage에 인증 데이터가 없습니다. 다시 인증해주세요.');
    showAlert('인증 정보가 없습니다. 다시 인증해주세요.');

    // 폼 리셋하고 다시 인증 받도록
    resetForm(elements);
    return;
  }

  try {
    const verificationData = JSON.parse(existingData);
    console.log('📋 저장된 인증 정보 확인:', verificationData);

    // 다음 페이지로 이동 (비밀번호 입력 페이지)
    const nextUrl = window.PAGE_CONFIG?.nextUrl || '/mobile/register/password';
    console.log('🔗 이동할 URL:', nextUrl);

    window.location.href = nextUrl;

  } catch (error) {
    console.error('❌ 인증 데이터 확인 실패:', error);
    showAlert('인증 데이터 확인에 실패했습니다. 다시 시도해주세요.');
  }
};

// ===== 인증번호 섹션 표시 =====
const showVerificationSection = (elements) => {
  if (elements.verificationSection) {
    elements.verificationSection.style.display = 'block';
  }
  if (elements.verificationCode) {
    elements.verificationCode.value = '';
  }
  verificationConfirmed = false;
  updateConfirmButton(elements);
  updateNextButton(elements);
};

// ===== 타이머 관련 함수들 =====
const startTimer = (elements) => {
  timeLeft = 180; // 3분 리셋
  updateTimerDisplay(elements);

  timerInterval = setInterval(() => {
    timeLeft--;
    updateTimerDisplay(elements);

    if (timeLeft <= 0) {
      stopTimer();
      showAlert('인증시간이 만료되었습니다. 다시 요청해주세요.');
      resetVerificationSection(elements);
    }
  }, 1000);
};

const stopTimer = () => {
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
};

const updateTimerDisplay = (elements) => {
  if (!elements.timer) return;

  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;
  elements.timer.textContent = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

  // 30초 이하일 때 빨간색으로 표시
  if (timeLeft <= 30) {
    elements.timer.style.color = 'var(--error)';
  } else {
    elements.timer.style.color = 'var(--warning)';
  }
};

// ===== 인증 섹션 리셋 =====
const resetVerificationSection = (elements) => {
  if (elements.verificationSection) {
    elements.verificationSection.style.display = 'none';
  }
  if (elements.verificationCode) {
    elements.verificationCode.value = '';
    elements.verificationCode.disabled = false;
  }
  if (elements.confirmButton) {
    elements.confirmButton.disabled = true;
  }
  verificationSent = false;
  verificationConfirmed = false;
  userKey = '';
  updateNextButton(elements);
};

// ===== 폼 완전 리셋 =====
const resetForm = (elements) => {
  elements.phoneNumber.disabled = false;
  elements.phoneNumber.value = '';
  resetVerificationSection(elements);
  updateVerifyRequestButton(elements);

  // 버튼 텍스트 원래대로
  const buttonText = elements.verifyRequestButton.querySelector('.button-text');
  if (buttonText) {
    buttonText.textContent = '인증번호 받기';
  }
};

// ===== UI 상태 관리 =====
const showLoadingState = (button) => {
  if (!button) return;

  const buttonText = button.querySelector('.button-text');
  const buttonLoading = button.querySelector('.button-loading');

  if (buttonText && buttonLoading) {
    buttonText.style.display = 'none';
    buttonLoading.style.display = 'flex';
  }

  button.disabled = true;
};

const hideLoadingState = (button) => {
  if (!button) return;

  const buttonText = button.querySelector('.button-text');
  const buttonLoading = button.querySelector('.button-loading');

  if (buttonText && buttonLoading) {
    buttonText.style.display = 'block';
    buttonLoading.style.display = 'none';
  }
};

// ===== 알림 모달 =====
const showAlert = (message, callback = null) => {
  const elements = initializeElements();
  if (!elements.alertModal) return;

  elements.alertMessage.textContent = message;
  elements.alertModal.style.display = 'flex';

  setTimeout(() => {
    elements.alertModal.classList.add('show');
  }, 10);

  // 콜백이 있다면 확인 버튼 클릭 시 실행
  if (callback) {
    elements.alertConfirm.onclick = () => {
      hideAlertModal();
      callback();
    };
  } else {
    elements.alertConfirm.onclick = () => {
      hideAlertModal();
    };
  }
};

const hideAlertModal = () => {
  const elements = initializeElements();
  if (!elements.alertModal) return;

  elements.alertModal.classList.remove('show');
  setTimeout(() => {
    elements.alertModal.style.display = 'none';
  }, 300);
};

// ===== 페이지 로드 시 초기화 =====
document.addEventListener('DOMContentLoaded', initPhoneVerifyPage);

// ===== 페이지 언로드 시 정리 =====
window.addEventListener('beforeunload', () => {
  stopTimer();
});

// ===== 키보드 이벤트 처리 =====
document.addEventListener('keydown', (e) => {
  // ESC 키로 모달 닫기
  if (e.key === 'Escape') {
    const alertModal = document.getElementById('alertModal');
    if (alertModal && alertModal.classList.contains('show')) {
      hideAlertModal();
    }
  }
});

// ===== 디버그 함수 (개발용) =====
if (window.location.search.includes('debug=true')) {
  window.phoneVerifyDebug = {
    getFormData: () => {
      const elements = initializeElements();
      return {
        phoneNumber: elements?.phoneNumber?.value,
        verificationCode: elements?.verificationCode?.value,
        userKey,
        verificationSent,
        verificationConfirmed,
        timeLeft
      };
    },
    simulateSuccess: () => {
      const elements = initializeElements();
      if (elements.verificationCode) {
        elements.verificationCode.value = '123456';
      }
      verificationConfirmed = true;
      userKey = 'test-user-key';
      stopTimer();

      // sessionStorage에 테스트 데이터 저장
      const testData = {
        phoneNumber: elements.phoneNumber.value.replace(/\D/g, '') || '01012345678',
        countryCode: elements.countryCode.value || '+82',
        verificationCode: '123456',
        userKey: 'test-user-key',
        verifiedAt: new Date().toISOString()
      };
      sessionStorage.setItem('phoneVerification', JSON.stringify(testData));

      // 다음 버튼 강제 활성화
      elements.nextButton.disabled = false;
      elements.nextButton.classList.remove('disabled');
      elements.nextButton.classList.add('enabled');

      console.log('🧪 테스트 성공 시뮬레이션 완료');
      console.log('💾 sessionStorage 저장됨:', testData);
    },
    checkSessionStorage: () => {
      const data = sessionStorage.getItem('phoneVerification');
      if (data) {
        console.log('📱 sessionStorage phoneVerification:', JSON.parse(data));
      } else {
        console.log('❌ sessionStorage에 phoneVerification 없음');
      }

      const agreement = localStorage.getItem('agreementData');
      if (agreement) {
        console.log('📋 localStorage agreementData:', JSON.parse(agreement));
      } else {
        console.log('❌ localStorage에 agreementData 없음');
      }
    },
    clearStorage: () => {
      sessionStorage.removeItem('phoneVerification');
      localStorage.removeItem('agreementData');
      console.log('🗑️ 모든 저장 데이터 삭제');
    },
    resetForm: () => {
      const elements = initializeElements();
      resetForm(elements);
    },
    // 테스트용 인증번호 자동 입력
    useTestCode: () => {
      const elements = initializeElements();
      if (elements.verificationCode) {
        elements.verificationCode.value = '123456';
        handleVerificationCodeInput({ target: elements.verificationCode }, elements);
      }
    }
  };

  console.log('🔧 디버그 모드 활성화 - window.phoneVerifyDebug 사용 가능');
  console.log('💡 사용법:');
  console.log('  - window.phoneVerifyDebug.simulateSuccess() : 인증 성공 시뮬레이션');
  console.log('  - window.phoneVerifyDebug.useTestCode() : 테스트 인증번호 입력');
  console.log('  - window.phoneVerifyDebug.checkSessionStorage() : 저장 데이터 확인');
  console.log('  - window.phoneVerifyDebug.clearStorage() : 저장 데이터 삭제');
}