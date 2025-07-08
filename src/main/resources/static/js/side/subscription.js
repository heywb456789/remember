/**
 * 정기구독 처리를 위한 JavaScript 파일
 * subscription.js
 */
import {authFetch, handleFetchError} from '../commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
  // 상태 관리 객체
  const subscriptionState = {
    // 1단계: 신청자 정보
    step1: {
      name: '',
      phoneNumber: '',
    },
    // 2단계: 결제 정보
    step2: {
      planType: '정기구독',
      price: 33000,
      agreePayment: false
    },
    // 현재 단계 (1 또는 2)
    currentStep: getCurrentStep(),
    // 입력 완료 상태
    isStep1Valid: false,
    isStep2Valid: false
  };

  // 현재 페이지 단계 확인
  function getCurrentStep() {
    return window.location.pathname.includes('Step2') ? 2 : 1;
  }

  // 폼 요소 참조
  const elements = {
    // 1단계 요소
    step1Form: document.getElementById('step1Form'),
    nameInput: document.getElementById('name'),
    phoneInput: document.getElementById('phoneNumber'),
    step1NextButton: document.getElementById('step1NextBtn'),

    // 2단계 요소
    step2Form: document.getElementById('step2Form'),
    priceDisplay: document.getElementById('subscriptionPrice'),
    step2AgreeCheckbox: document.getElementById('agreePayment'),
    finalSubmitButton: document.getElementById('finalSubmitBtn')
  };

  // 초기화 함수
  function initSubscription() {
    // 세션에서 저장된 정보 복구
    restoreFromSession();

    // 1단계 폼 처리
    if (elements.step1Form) {
      // 입력 필드 이벤트
      if (elements.nameInput) {
        elements.nameInput.addEventListener('input', updateStep1Validation);
      }
      if (elements.phoneInput) {
        elements.phoneInput.addEventListener('input', updateStep1Validation);
      }

      // 폼 제출 이벤트
      elements.step1Form.addEventListener('submit', function (e) {
        e.preventDefault();
        if (validateStep1()) {
          goToStep2();
        }
      });
    }

    // 2단계 폼 처리
    if (elements.step2Form) {
      // 약관 동의 체크박스 이벤트
      if (elements.step2AgreeCheckbox) {
        elements.step2AgreeCheckbox.addEventListener('change',
            updateStep2Validation);
      }

      // 폼 제출 이벤트
      elements.step2Form.addEventListener('submit', function (e) {
        e.preventDefault();
        if (validateStep2()) {
          submitSubscription();
        }
      });

      // 뒤로가기 버튼 처리 (해당 페이지 내 뒤로가기 버튼이 있는 경우)
      const backButton = document.querySelector('.back-button');
      if (backButton) {
        backButton.addEventListener('click', function () {
          window.location.href = 'subscriptionStep1.html';
        });
      }

      // 브라우저 뒤로가기 처리
      window.addEventListener('popstate', function () {
        if (subscriptionState.currentStep === 2) {
          window.location.href = 'subscriptionStep1.html';
        }
      });
    }

    // 헤더의 뒤로가기 버튼 처리 (공통 헤더 컴포넌트가 로드된 후)
    setTimeout(() => {
      const headerBackButton = document.querySelector(
          '#header-container .menu-button');
      if (headerBackButton) {
        headerBackButton.addEventListener('click', function (e) {
          if (subscriptionState.currentStep === 2) {
            e.preventDefault();
            window.location.href = 'subscriptionStep1.html';
          }
        });
      }
    }, 100);

    // 버튼 상태 업데이트
    updateButtonStates();
  }

  // 입력값 변경 시 유효성 검사 (실시간 피드백을 위한 선택적 기능)
  function updateStep1Validation() {
    if (elements.step1Form) {
      // 입력값 가져오기
      subscriptionState.step1.name = elements.nameInput
          ? elements.nameInput.value.trim() : '';
      subscriptionState.step1.phoneNumber = elements.phoneInput
          ? elements.phoneInput.value.trim() : '';

      // 전화번호 입력 시 하이픈(-) 자동 제거
      if (elements.phoneInput && subscriptionState.step1.phoneNumber) {
        if (subscriptionState.step1.phoneNumber.includes('-')) {
          // 하이픈 제거
          const normalized = subscriptionState.step1.phoneNumber.replace(/-/g,
              '');
          // 숫자만 입력되었는지 확인
          if (/^\d+$/.test(normalized)) {
            elements.phoneInput.value = normalized;
            subscriptionState.step1.phoneNumber = normalized;
          }
        }
      }

      // 세션에 저장
      saveToSession();

      // 버튼 상태 업데이트
      updateButtonStates();
    }
  }

  // 2단계 유효성 검사 및 입력값 변경 처리
  function updateStep2Validation() {
    if (elements.step2Form) {
      // 약관 동의 상태 업데이트
      subscriptionState.step2.agreePayment = elements.step2AgreeCheckbox
          ? elements.step2AgreeCheckbox.checked : false;

      // 세션에 저장
      saveToSession();

      // 버튼 상태 업데이트
      updateButtonStates();
    }
  }

  // 1단계 완전한 유효성 검사 - 하이픈이 없는 전화번호만 허용하도록 수정
  function validateStep1() {
    const {name, phoneNumber} = subscriptionState.step1;

    // 이름 필수 검사
    if (!name || name.trim() === '') {
      alert('이름을 입력해주세요.');
      if (elements.nameInput) {
        elements.nameInput.focus();
      }
      return false;
    }

    // 이름 형식 검사 - 최소 2글자 이상 (한글, 영문 허용)
    if (name.length < 2) {
      alert('이름은 최소 2글자 이상 입력해주세요.');
      if (elements.nameInput) {
        elements.nameInput.focus();
      }
      return false;
    }

    // 전화번호 필수 검사
    if (!phoneNumber || phoneNumber.trim() === '') {
      alert('전화번호를 입력해주세요.');
      if (elements.phoneInput) {
        elements.phoneInput.focus();
      }
      return false;
    }

    // 하이픈이 포함되어 있는지 확인
    if (phoneNumber.includes('-')) {
      alert('전화번호는 하이픈(-) 없이 숫자만 입력해주세요.');
      if (elements.phoneInput) {
        elements.phoneInput.focus();
      }
      return false;
    }

    // 전화번호 형식 검사 (01로 시작, 총 10-11자리 숫자)
    const phoneRegex = /^01[016789]\d{7,8}$/;
    if (!phoneRegex.test(phoneNumber)) {
      // 자세한 오류 메시지 표시
      if (!phoneNumber.startsWith('01')) {
        alert('전화번호는 01로 시작해야 합니다.');
      } else if (phoneNumber.length < 10 || phoneNumber.length > 11) {
        alert('전화번호는 10~11자리여야 합니다.\n현재 ' + phoneNumber.length + '자리입니다.');
      } else {
        alert('유효한 전화번호 형식이 아닙니다. 올바른 전화번호를 입력해주세요.');
      }

      if (elements.phoneInput) {
        elements.phoneInput.focus();
      }
      return false;
    }

    // 모든 검증 통과
    subscriptionState.isStep1Valid = true;
    return true;
  }

// 전화번호 포맷팅 함수
  function formatPhoneNumber(phone) {
    if (phone.length === 10) {
      return phone.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3');
    } else if (phone.length === 11) {
      return phone.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
    }
    return phone;
  }

  // 2단계 완전한 유효성 검사
  function validateStep2() {
    // 2단계 진입 전 1단계 유효성 검사 확인
    if (!subscriptionState.isStep1Valid) {
      alert('이전 단계 정보가 유효하지 않습니다. 처음부터 다시 시도해주세요.');
      window.location.href = 'subscriptionStep1.html';
      return false;
    }

    // 약관 동의 확인
    if (!subscriptionState.step2.agreePayment) {
      alert('결제 진행에 동의해주세요.');
      return false;
    }

    subscriptionState.isStep2Valid = true;
    return true;
  }

  // 버튼 활성화/비활성화 상태 업데이트
  function updateButtonStates() {
    // 1단계 다음 버튼
    if (elements.step1NextButton) {
      const {name, phoneNumber} = subscriptionState.step1;
      const isStep1Ready = name && phoneNumber;

      elements.step1NextButton.disabled = !isStep1Ready;
    }

    // 2단계 최종 제출 버튼
    if (elements.finalSubmitButton) {
      const isStep2Ready = subscriptionState.step1.name
          && subscriptionState.step2.agreePayment;

      elements.finalSubmitButton.disabled = !isStep2Ready;
    }
  }

  // 2단계로 이동 함수 - 유효성 검사 강화
  function goToStep2() {
    // 2단계 이동 전 한 번 더 유효성 검사 실행
    if (!validateStep1()) {
      return; // 유효성 검사 실패 시 이동하지 않음
    }

    // 세션에 현재 상태 저장
    subscriptionState.isStep1Valid = true;
    saveToSession();

    // 2단계 페이지로 이동
    window.location.href = 'subscriptionStep2.html';
  }

  // 세션 스토리지에 현재 상태 저장
  function saveToSession() {
    try {
      sessionStorage.setItem('subscriptionState',
          JSON.stringify(subscriptionState));
    } catch (e) {
      console.error('세션 저장 실패:', e);
    }
  }

  // 세션 스토리지에서 상태 복원
  function restoreFromSession() {
    try {
      const savedState = sessionStorage.getItem('subscriptionState');
      if (savedState) {
        const parsedState = JSON.parse(savedState);

        // 저장된 상태 복원 (단계 제외)
        subscriptionState.step1 = parsedState.step1 || subscriptionState.step1;
        subscriptionState.step2 = parsedState.step2 || subscriptionState.step2;
        subscriptionState.isStep1Valid = parsedState.isStep1Valid || false;
        subscriptionState.isStep2Valid = parsedState.isStep2Valid || false;

        // 현재 단계는 URL로 판단
        subscriptionState.currentStep = getCurrentStep();

        // 폼 필드에 값 채우기
        if (subscriptionState.currentStep === 1) {
          if (elements.nameInput) {
            elements.nameInput.value = subscriptionState.step1.name || '';
          }
          if (elements.phoneInput) {
            elements.phoneInput.value = subscriptionState.step1.phoneNumber
                || '';
          }
        } else if (subscriptionState.currentStep === 2) {
          if (elements.step2AgreeCheckbox) {
            elements.step2AgreeCheckbox.checked = subscriptionState.step2.agreePayment
                || false;
          }

          // 1단계 완료 확인 (2단계 접근 제한)
          if (!subscriptionState.isStep1Valid && !isTestMode()) {
            alert('신청자 정보를 먼저 입력해주세요.');
            window.location.href = 'subscriptionStep1.html';
            return;
          }
        }

        // 버튼 상태 업데이트
        updateButtonStates();
      }
    } catch (e) {
      console.error('세션 복원 실패:', e);
    }
  }

  // 테스트 모드 확인 (개발 환경에서만 사용)
  function isTestMode() {
    return new URLSearchParams(window.location.search).has('test');
  }

  // 최종 구독 신청 제출
  async function submitSubscription() {
    try {
      // 버튼 비활성화 및 로딩 표시
      if (elements.finalSubmitButton) {
        elements.finalSubmitButton.disabled = true;
        elements.finalSubmitButton.innerHTML = '<span class="spinner-border"></span> 처리중...';
      }

      // 1. 내부 서버에 정보 저장 요청 및 seq 값 받기
      const internalRequestData = {
        name: subscriptionState.step1.name,
        phoneNumber: subscriptionState.step1.phoneNumber,
        planType: subscriptionState.step2.planType,
        price: subscriptionState.step2.price
      };

      // 내부 API 호출
      const internalResponse = await authFetch('/api/subscription/prepare', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(internalRequestData)
      });

      const internalResult = await internalResponse.json();

      if (!internalResponse.ok) {
        throw new Error(internalResult.message || '내부 서버 처리 중 오류가 발생했습니다.');
      }

      // 외부 결제 API에 보낼 데이터 준비
      const externalPaymentData = {
        mid: internalResult.response.mid,
        license: internalResult.response.licenseKey,
        userid: internalResult.response.memberId,
        prodname: internalResult.response.productName,
        phone: internalResult.response.memberPhone,
        username: internalResult.response.memberName,
        prodprice: internalResult.response.productPrice,
        returnurl: internalResult.response.returnUrl,
        requestUrl: internalResult.response.requestUrl,
        seq: internalResult.response.seq,
        subDate: internalResult.response.payStartDate,
        paymentLimit: internalResult.response.payMonth,
      };

      // 3. 외부 결제 API 폼 자동 제출 (POST 방식)
      submitFormToExternalAPI(externalPaymentData);

      // 세션 데이터 유지 (결제 취소 시 돌아올 가능성 대비)
      // 완료 페이지에서 세션 초기화 처리

    } catch (error) {
      console.error('결제 요청 오류:', error);

      // 에러 메시지 표시
      alert(error.message || '결제 준비 중 오류가 발생했습니다. 다시 시도해주세요.');

      // 버튼 상태 복원
      if (elements.finalSubmitButton) {
        elements.finalSubmitButton.disabled = false;
        elements.finalSubmitButton.textContent = '결제하기';
      }
    }
  }

  // 외부 API에 폼 자동 제출 함수
  function submitFormToExternalAPI(data) {
    // 동적 폼 생성
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = data.requestUrl;
    form.style.display = 'none';

    // 폼 필드 추가
    Object.entries(data).forEach(([key, value]) => {
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = key;
      input.value = value;
      form.appendChild(input);
    });

    // 폼을 body에 추가하고 제출
    document.body.appendChild(form);
    form.submit();
  }

  // 공통 헤더 로드 후 초기화 실행
  setTimeout(initSubscription, 100);
});