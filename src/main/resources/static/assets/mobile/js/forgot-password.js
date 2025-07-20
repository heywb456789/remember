// forgot-password.js - 비밀번호 찾기 페이지 로직

import { showToast } from './common.js';

// ===== 전역 변수 =====
let isProcessing = false;
let timerInterval = null;
let timerSeconds = 0;
let currentPhoneNumber = '';
let isPhoneVerified = false;

// ===== 초기화 함수 =====
const initForgotPasswordPage = () => {
    console.log("🚀 비밀번호 찾기 페이지 초기화");

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

    console.log('✅ 비밀번호 찾기 페이지 초기화 완료');
};

// ===== DOM 요소 초기화 =====
const initializeElements = () => {
    const elements = {
        // 입력 필드
        phoneNumber: document.getElementById('phoneNumber'),
        verificationCode: document.getElementById('verificationCode'),
        newPassword: document.getElementById('newPassword'),
        confirmPassword: document.getElementById('confirmPassword'),

        // 버튼
        requestCodeBtn: document.getElementById('requestCodeBtn'),
        verifyCodeBtn: document.getElementById('verifyCodeBtn'),
        submitBtn: document.getElementById('submitBtn'),

        // 섹션
        codeGroup: document.getElementById('codeGroup'),
        passwordGroup: document.getElementById('passwordGroup'),
        confirmPasswordGroup: document.getElementById('confirmPasswordGroup'),
        submitSection: document.getElementById('submitSection'),

        // 메시지 및 타이머
        messageElement: document.getElementById('messageElement'),
        timerDisplay: document.getElementById('timerDisplay'),
        passwordMatchMessage: document.getElementById('passwordMatchMessage'),

        // 모달
        timeoutModal: document.getElementById('timeoutModal'),
        successModal: document.getElementById('successModal'),
        timeoutConfirmBtn: document.getElementById('timeoutConfirmBtn'),
        successConfirmBtn: document.getElementById('successConfirmBtn')
    };

    // 필수 요소 체크
    const requiredElements = ['phoneNumber', 'requestCodeBtn', 'submitBtn'];
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

    // 1. 휴대폰 번호 입력 처리
    elements.phoneNumber.addEventListener('input', (e) => {
        handlePhoneInput(e, elements);
    });

    // 2. 인증번호 요청 버튼
    elements.requestCodeBtn.addEventListener('click', () => {
        handleRequestCode(elements);
    });

    // 3. 인증번호 입력 처리
    elements.verificationCode.addEventListener('input', (e) => {
        handleCodeInput(e, elements);
    });

    // 4. 인증번호 확인 버튼
    elements.verifyCodeBtn.addEventListener('click', () => {
        handleVerifyCode(elements);
    });

    // 5. 비밀번호 입력 처리
    elements.newPassword.addEventListener('input', () => {
        handlePasswordInput(elements);
    });

    // 6. 비밀번호 확인 입력 처리
    elements.confirmPassword.addEventListener('input', () => {
        handleConfirmPasswordInput(elements);
    });

    // 7. 최종 제출 버튼
    elements.submitBtn.addEventListener('click', () => {
        handleSubmit(elements);
    });

    // 8. 모달 버튼들
    elements.timeoutConfirmBtn.addEventListener('click', () => {
        handleTimeoutConfirm(elements);
    });

    elements.successConfirmBtn.addEventListener('click', () => {
        handleSuccessConfirm();
    });

    // Enter 키 처리
    elements.phoneNumber.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleRequestCode(elements);
        }
    });

    elements.verificationCode.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleVerifyCode(elements);
        }
    });

    [elements.newPassword, elements.confirmPassword].forEach(input => {
        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                handleSubmit(elements);
            }
        });
    });

    console.log('✅ 이벤트 리스너 등록 완료');
};

// ===== 폼 초기 상태 설정 =====
const initializeFormState = (elements) => {
    elements.phoneNumber.focus();
    updateRequestCodeButton(elements);
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
    updateRequestCodeButton(elements);
    hideMessage(elements);
};

const handleCodeInput = (e, elements) => {
    // 숫자만 허용
    let value = e.target.value.replace(/\D/g, '');

    // 6자리 제한
    if (value.length > 6) {
        value = value.slice(0, 6);
    }

    e.target.value = value;
    updateVerifyCodeButton(elements);
    hideMessage(elements);
};

const handlePasswordInput = (elements) => {
    validatePasswordStrength(elements);
    validatePasswordMatch(elements);
    updateSubmitButton(elements);
    hideMessage(elements);
};

const handleConfirmPasswordInput = (elements) => {
    validatePasswordMatch(elements);
    updateSubmitButton(elements);
    hideMessage(elements);
};

// ===== 1. 인증번호 요청 처리 =====
const handleRequestCode = async (elements) => {
    if (isProcessing) return;

    const phoneNumber = elements.phoneNumber.value.trim();
    if (!validatePhoneNumber(phoneNumber)) {
        showMessage(elements, '올바른 휴대폰 번호를 입력해주세요. (010으로 시작하는 11자리)', 'error');
        return;
    }

    console.log('📞 인증번호 요청 시작:', phoneNumber);
    isProcessing = true;
    showLoadingState(elements.requestCodeBtn);
    hideMessage(elements);

    try {
        const response = await fetch(window.API_ENDPOINTS.SEND_CODE, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ phoneNumber })
        });

        const data = await response.json();
        console.log('📡 인증번호 요청 응답:', data);

        if (response.ok && data.status?.code === 'OK_0000' && data.response?.success) {
            // 성공 처리
            currentPhoneNumber = phoneNumber;
            showMessage(elements, '인증번호가 발송되었습니다.', 'success');

            // UI 상태 변경
            elements.phoneNumber.disabled = true;
            elements.requestCodeBtn.disabled = true;
            elements.codeGroup.style.display = 'block';
            elements.verificationCode.focus();

            // 타이머 시작 (3분)
            startTimer(elements, 180);

        } else {
            // 실패 처리
            const errorMessage = data.response?.message || '인증번호 발송에 실패했습니다.';
            console.error('❌ 인증번호 요청 실패:', errorMessage);
            showMessage(elements, errorMessage, 'error');
        }

    } catch (error) {
        console.error('💥 인증번호 요청 오류:', error);
        showMessage(elements, '네트워크 오류가 발생했습니다. 다시 시도해주세요.', 'error');
    } finally {
        isProcessing = false;
        hideLoadingState(elements.requestCodeBtn);
    }
};

// ===== 2. 인증번호 확인 처리 =====
const handleVerifyCode = async (elements) => {
    if (isProcessing) return;

    const code = elements.verificationCode.value.trim();
    if (!validateVerificationCode(code)) {
        showMessage(elements, '6자리 인증번호를 입력해주세요.', 'error');
        return;
    }

    console.log('🔐 인증번호 확인 시작');
    isProcessing = true;
    showLoadingState(elements.verifyCodeBtn);
    hideMessage(elements);

    try {
        const response = await fetch(window.API_ENDPOINTS.VERIFY_CODE, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                phoneNumber: currentPhoneNumber,
                verificationCode: code
            })
        });

        const data = await response.json();
        console.log('📡 인증번호 확인 응답:', data);

        if (response.ok && data.status?.code === 'OK_0000' && data.response?.success) {
            // 인증 성공
            isPhoneVerified = true;
            showMessage(elements, '인증이 완료되었습니다.', 'success');

            // UI 상태 변경
            elements.verificationCode.disabled = true;
            elements.verifyCodeBtn.disabled = true;
            stopTimer();

            // 비밀번호 입력 필드 표시
            elements.passwordGroup.style.display = 'block';
            elements.confirmPasswordGroup.style.display = 'block';
            elements.submitSection.style.display = 'block';
            elements.newPassword.focus();

        } else {
            // 인증 실패
            console.error('❌ 인증번호 확인 실패');
            showMessage(elements, '인증번호가 일치하지 않습니다.', 'error');
            elements.verificationCode.select();
        }

    } catch (error) {
        console.error('💥 인증번호 확인 오류:', error);
        showMessage(elements, '네트워크 오류가 발생했습니다. 다시 시도해주세요.', 'error');
    } finally {
        isProcessing = false;
        hideLoadingState(elements.verifyCodeBtn);
    }
};

// ===== 3. 최종 비밀번호 재설정 처리 =====
const handleSubmit = async (elements) => {
    if (isProcessing || !isPhoneVerified) return;

    const newPassword = elements.newPassword.value;
    const confirmPassword = elements.confirmPassword.value;

    // 최종 검증
    if (!validatePasswordStrength(elements) || !validatePasswordMatch(elements)) {
        return;
    }

    console.log('🔒 비밀번호 재설정 시작');
    isProcessing = true;
    showLoadingState(elements.submitBtn);
    hideMessage(elements);

    try {
        const response = await fetch(window.API_ENDPOINTS.RESET_PASSWORD, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                phoneNumber: currentPhoneNumber,
                verificationCode: elements.verificationCode.value,
                newPassword: newPassword
            })
        });

        const data = await response.json();
        console.log('📡 비밀번호 재설정 응답:', data);

        if (response.ok && data.status?.code === 'OK_0000' && data.response?.success) {
            // 성공 - 완료 모달 표시
            console.log('✅ 비밀번호 재설정 성공');
            showSuccessModal(elements);

        } else {
            // 실패 처리
            const errorMessage = data.response?.message || '비밀번호 재설정에 실패했습니다.';
            console.error('❌ 비밀번호 재설정 실패:', errorMessage);
            showMessage(elements, errorMessage, 'error');
        }

    } catch (error) {
        console.error('💥 비밀번호 재설정 오류:', error);
        showMessage(elements, '네트워크 오류가 발생했습니다. 다시 시도해주세요.', 'error');
    } finally {
        isProcessing = false;
        hideLoadingState(elements.submitBtn);
    }
};

// ===== 타이머 관련 함수들 =====
const startTimer = (elements, seconds) => {
    timerSeconds = seconds;
    updateTimerDisplay(elements);

    timerInterval = setInterval(() => {
        timerSeconds--;
        updateTimerDisplay(elements);

        if (timerSeconds <= 0) {
            stopTimer();
            showTimeoutModal(elements);
        }
    }, 1000);

    console.log(`⏰ 타이머 시작: ${seconds}초`);
};

const stopTimer = () => {
    if (timerInterval) {
        clearInterval(timerInterval);
        timerInterval = null;
        console.log('⏰ 타이머 중지');
    }
};

const updateTimerDisplay = (elements) => {
    const minutes = Math.floor(timerSeconds / 60);
    const seconds = timerSeconds % 60;
    const timeString = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

    if (elements.timerDisplay) {
        elements.timerDisplay.textContent = timeString;
    }
};

// ===== 검증 함수들 =====
const validatePhoneNumber = (phone) => {
    return /^010\d{8}$/.test(phone);
};

const validateVerificationCode = (code) => {
    return /^\d{6}$/.test(code);
};

const validatePasswordStrength = (elements) => {
    const password = elements.newPassword.value;

    // 8~12자 체크
    if (password.length < 8 || password.length > 12) {
        return false;
    }

    // 영어 대소문자, 숫자, 특수문자 3개 이상 포함 체크
    const hasLower = /[a-z]/.test(password);
    const hasUpper = /[A-Z]/.test(password);
    const hasNumber = /\d/.test(password);
    const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(password);

    const typesCount = [hasLower, hasUpper, hasNumber, hasSpecial].filter(Boolean).length;

    return typesCount >= 3;
};

const validatePasswordMatch = (elements) => {
    const newPassword = elements.newPassword.value;
    const confirmPassword = elements.confirmPassword.value;

    if (confirmPassword.length === 0) {
        elements.passwordMatchMessage.style.display = 'none';
        return true;
    }

    const isMatch = newPassword === confirmPassword;

    if (isMatch) {
        elements.passwordMatchMessage.textContent = '비밀번호가 일치합니다.';
        elements.passwordMatchMessage.className = 'password-match-message success';
    } else {
        elements.passwordMatchMessage.textContent = '비밀번호가 일치하지 않습니다.';
        elements.passwordMatchMessage.className = 'password-match-message error';
    }

    elements.passwordMatchMessage.style.display = 'block';
    return isMatch;
};

// ===== 버튼 상태 업데이트 함수들 =====
const updateRequestCodeButton = (elements) => {
    const phoneValue = elements.phoneNumber.value.trim();
    const isValid = validatePhoneNumber(phoneValue);
    elements.requestCodeBtn.disabled = !isValid || isProcessing;
};

const updateVerifyCodeButton = (elements) => {
    const codeValue = elements.verificationCode.value.trim();
    const isValid = validateVerificationCode(codeValue);
    elements.verifyCodeBtn.disabled = !isValid || isProcessing;
};

const updateSubmitButton = (elements) => {
    const isPasswordValid = validatePasswordStrength(elements);
    const isPasswordMatch = validatePasswordMatch(elements);
    const canSubmit = isPhoneVerified && isPasswordValid && isPasswordMatch && !isProcessing;

    elements.submitBtn.disabled = !canSubmit;
};

// ===== 모달 처리 함수들 =====
const showTimeoutModal = (elements) => {
    console.log('⏰ 인증시간 초과 모달 표시');
    elements.timeoutModal.style.display = 'flex';
};

const handleTimeoutConfirm = (elements) => {
    elements.timeoutModal.style.display = 'none';

    // 폼 초기화
    resetForm(elements);
};

const showSuccessModal = (elements) => {
    console.log('✅ 비밀번호 재설정 완료 모달 표시');
    elements.successModal.style.display = 'flex';
};

const handleSuccessConfirm = () => {
    console.log('🔄 로그인 페이지로 이동');
    const loginUrl = window.PAGE_CONFIG?.loginUrl || '/mobile/login';
    window.location.href = loginUrl;
};

// ===== 폼 리셋 함수 =====
const resetForm = (elements) => {
    console.log('🔄 폼 초기화');

    // 입력 필드 초기화
    elements.phoneNumber.value = '';
    elements.phoneNumber.disabled = false;
    elements.verificationCode.value = '';
    elements.verificationCode.disabled = false;
    elements.newPassword.value = '';
    elements.confirmPassword.value = '';

    // 버튼 초기화
    elements.requestCodeBtn.disabled = true;
    elements.verifyCodeBtn.disabled = true;
    elements.submitBtn.disabled = true;

    // 섹션 숨김
    elements.codeGroup.style.display = 'none';
    elements.passwordGroup.style.display = 'none';
    elements.confirmPasswordGroup.style.display = 'none';
    elements.submitSection.style.display = 'none';

    // 메시지 숨김
    hideMessage(elements);
    elements.passwordMatchMessage.style.display = 'none';

    // 상태 초기화
    currentPhoneNumber = '';
    isPhoneVerified = false;
    stopTimer();

    // 포커스
    elements.phoneNumber.focus();
};

// ===== UI 헬퍼 함수들 =====
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

    button.style.opacity = '1';
};

const showMessage = (elements, message, type) => {
    if (elements.messageElement) {
        elements.messageElement.textContent = message;
        elements.messageElement.className = `alert-message alert-${type}`;
        elements.messageElement.style.display = 'block';

        // 자동 숨김 (에러가 아닌 경우)
        if (type !== 'error') {
            setTimeout(() => {
                hideMessage(elements);
            }, 5000);
        }
    }

    // 토스트도 함께 표시
    if (typeof showToast === 'function') {
        showToast(message, type);
    }
};

const hideMessage = (elements) => {
    if (elements.messageElement) {
        elements.messageElement.style.display = 'none';
    }
};

// ===== 페이지 로드 시 초기화 =====
document.addEventListener('DOMContentLoaded', initForgotPasswordPage);

// ===== 에러 처리 =====
window.addEventListener('error', (event) => {
    console.error('🚨 전역 에러:', event.error);
});

window.addEventListener('unhandledrejection', (event) => {
    console.error('🚨 처리되지 않은 Promise 거부:', event.reason);
});

// ===== 디버그 함수 (개발용) =====
if (window.location.search.includes('debug=true')) {
    window.forgotPasswordDebug = {
        getCurrentState: () => ({
            currentPhoneNumber,
            isPhoneVerified,
            timerSeconds,
            isProcessing
        }),
        resetForm: () => {
            const elements = initializeElements();
            if (elements) resetForm(elements);
        },
        showTestModal: (type) => {
            const elements = initializeElements();
            if (elements) {
                if (type === 'timeout') showTimeoutModal(elements);
                if (type === 'success') showSuccessModal(elements);
            }
        }
    };

    console.log('🔧 디버그 모드 활성화 - window.forgotPasswordDebug 사용 가능');
}