// videocall-feedback.js - 비디오콜 피드백 JavaScript
import { authFetch } from './commonFetch.js';

// 피드백 상태 관리
let feedbackData = {
  reviewMessage: ''
};

/**
 * 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
  console.log('통화 피드백 페이지 초기화');

  // 이벤트 바인딩
  bindEvents();

  // 초기 상태 설정
  updateSubmitButton();

  console.log('초기화 완료');
});

/**
 * 이벤트 바인딩
 */
function bindEvents() {
  // 폼 제출
  const form = document.getElementById('feedbackForm');
  if (form) {
    form.addEventListener('submit', handleFormSubmit);
  }

  // 텍스트 입력
  const textarea = document.getElementById('reviewMessage');
  const countElement = document.getElementById('reviewMessageCount');

  if (textarea && countElement) {
    // 입력 이벤트
    textarea.addEventListener('input', (e) => {
      const value = e.target.value;
      feedbackData.reviewMessage = value;

      // 글자 수 업데이트
      updateCharacterCount(textarea, countElement);

      // 제출 버튼 상태 업데이트
      updateSubmitButton();

      // 에러 제거
      clearError();
    });

    // 포커스 시 에러 제거
    textarea.addEventListener('focus', () => {
      clearError();
    });
  }

  // 버튼 클릭 이벤트
  document.addEventListener('click', function(e) {
    const action = e.target.getAttribute('data-action') ||
                  e.target.closest('[data-action]')?.getAttribute('data-action');

    if (!action) return;

    switch(action) {
      case 'skip-feedback':
        e.preventDefault();
        handleSkipFeedback();
        break;
    }
  });
}

/**
 * 글자 수 카운터 업데이트
 */
function updateCharacterCount(textarea, countElement) {
  const currentLength = textarea.value.length;
  const maxLength = parseInt(textarea.getAttribute('maxlength'));

  countElement.textContent = currentLength;

  // 색상 변경
  const parentElement = countElement.parentElement;
  parentElement.classList.remove('warning', 'danger');

  const percentage = (currentLength / maxLength) * 100;
  if (percentage >= 90) {
    parentElement.classList.add('danger');
  } else if (percentage >= 70) {
    parentElement.classList.add('warning');
  }
}

/**
 * 제출 버튼 상태 업데이트
 */
function updateSubmitButton() {
  const submitBtn = document.getElementById('submitBtn');
  if (!submitBtn) return;

  const hasMessage = feedbackData.reviewMessage && feedbackData.reviewMessage.trim().length > 0;

  if (hasMessage) {
    submitBtn.innerHTML = '<i class="fas fa-heart"></i> 평가 등록';
    submitBtn.classList.remove('btn-disabled');
    submitBtn.disabled = false;
  } else {
    submitBtn.innerHTML = '<i class="fas fa-heart"></i> 평가 등록';
    submitBtn.disabled = false; // 빈 메시지도 등록 가능하도록
  }
}

/**
 * 에러 표시
 */
function showError(message) {
  const textarea = document.getElementById('reviewMessage');
  const errorElement = document.getElementById('reviewMessageError');

  if (textarea) {
    textarea.classList.add('is-invalid');
  }

  if (errorElement) {
    errorElement.textContent = message;
    errorElement.classList.add('show');
  }
}

/**
 * 에러 제거
 */
function clearError() {
  const textarea = document.getElementById('reviewMessage');
  const errorElement = document.getElementById('reviewMessageError');

  if (textarea) {
    textarea.classList.remove('is-invalid');
  }

  if (errorElement) {
    errorElement.classList.remove('show');
    errorElement.textContent = '';
  }
}

/**
 * 유효성 검사
 */
function validateForm() {
  const message = feedbackData.reviewMessage?.trim() || '';

  if (message.length > 500) {
    showError('최대 500자까지 입력 가능합니다.');
    return false;
  }

  return true;
}

/**
 * 폼 제출 핸들러
 */
async function handleFormSubmit(event) {
  event.preventDefault();

  console.log('=== 피드백 제출 시작 ===');
  console.log('피드백 데이터:', feedbackData);

  try {
    // 유효성 검사
    if (!validateForm()) {
      return;
    }

    // 제출 버튼 상태 변경
    const submitBtn = document.getElementById('submitBtn');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner loading-spinner"></i> 등록 중...';

    // URL에서 영문명 가져오기
    const urlParams = new URLSearchParams(window.location.search);
    const contactKey = urlParams.get('contact') || localStorage.getItem('selectedContactKey') || 'kimgeuntae';

    console.log('피드백 제출 정보:', {
      reviewMessage: feedbackData.reviewMessage?.trim() || '',
      contactKey: contactKey
    });

    // 서버로 데이터 전송
    const result = await authFetch('/api/video-call/sample/feedback', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        reviewMessage: feedbackData.reviewMessage?.trim() || '',
        contactKey: contactKey // 영문명 추가
      })
    });

    console.log('API 응답:', result);

    if (result.status?.code === 'OK_0000' && result.response?.success) {
      console.log('피드백 등록 성공:', result.response);

      // 성공 메시지 표시 후 홈으로 이동
      alert(result.response.message || '소중한 피드백을 남겨주셔서 감사합니다!');
      window.location.href = '/mobile/home';
    } else {
      const errorMessage = result.status?.message || result.response?.message || '피드백 등록 중 오류가 발생했습니다.';
      console.error('서버 응답 오류:', result);
      throw new Error(errorMessage);
    }

  } catch (error) {
    console.error('피드백 제출 실패:', error);

    if (error.name === 'FetchError') {
      showError(error.statusMessage || '서버 오류가 발생했습니다. 다시 시도해주세요.');
    } else {
      showError(error.message || '피드백 등록 중 오류가 발생했습니다. 다시 시도해주세요.');
    }
  } finally {
    // 버튼 상태 복원
    updateSubmitButton();
  }
}

/**
 * 건너뛰기 핸들러
 */
function handleSkipFeedback() {
  if (confirm('피드백을 건너뛰고 홈으로 이동하시겠습니까?')) {
    window.location.href = '/mobile/home';
  }
}

console.log('videocall-feedback.js 로드 완료');