// family-info.js - 고인 상세 정보 페이지 JavaScript (동적 질문 답변 방식)
import { authFetch } from './commonFetch.js';
import { showToast, showConfirm, showLoading, hideLoading } from './common.js';

// 페이지 상태 관리
let pageState = {
  isLoading: false,
  isViewMode: false,
  memorialId: null,
  familyInfo: null,
  questionAnswers: [] // MemorialQuestionAnswerDTO 구조
};

// 서버에서 전달된 질문 데이터
let questionsData = [];

/**
 * 페이지 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
  console.log('고인 상세 정보 페이지 초기화 시작');

  try {
    // 서버 데이터 로드
    loadServerData();

    // 이벤트 바인딩
    bindEvents();

    // 입력 모드인 경우 폼 초기화
    if (!pageState.isViewMode) {
      initializeForm();

      // 질문 이벤트 바인딩 (DOM 준비 후)
      setTimeout(() => {
        bindQuestionEvents();
        updateSubmitButton();
      }, 100);
    }

    console.log('페이지 초기화 완료');
  } catch (error) {
    console.error('페이지 초기화 실패:', error);
    showToast('페이지 초기화에 실패했습니다.', 'error');
  }
});

/**
 * 서버 데이터 로드
 */
function loadServerData() {
  if (window.familyInfoData) {
    pageState.memorialId = window.familyInfoData.memorialId;
    pageState.isViewMode = window.familyInfoData.isViewMode;
  }
  // fallback: hidden input 태그에서 가져오기
  if (!pageState.memorialId) {
    const hidden = document.getElementById('memorialId');
    if (hidden && hidden.value) {
      pageState.memorialId = hidden.value;
      console.log('DOM에서 memorialId 로드:', pageState.memorialId);
    }
  }

  // ── ② 질문 데이터 로드 ──
  if (window.familyInfoData && Array.isArray(window.familyInfoData.questions)) {
    questionsData = window.familyInfoData.questions;
    console.log('서버 질문 데이터 로드 완료 - 질문 수:', questionsData.length);
  } else {
    console.warn('서버 질문 데이터가 없습니다. DOM에서 로드합니다.');
    const textareas = document.querySelectorAll('textarea[data-question-id]');
    questionsData = Array.from(textareas).map(textarea => ({
      id:           parseInt(textarea.getAttribute('data-question-id'), 10),
      placeholderText: textarea.getAttribute('placeholder') || '',
      maxLength:    parseInt(textarea.getAttribute('maxlength'), 10) || 0,
      minLength:    parseInt(textarea.getAttribute('minlength'), 10) || 0,
      isRequired:   textarea.hasAttribute('required')
    }));
    console.log('DOM 기반 질문 데이터 로드 완료 - 질문 수:', questionsData.length);
  }

  // ── ③ 기존 답변 배열 세팅 ──
  pageState.questionAnswers = Array.isArray(window.familyInfoData?.familyInfo?.questionAnswers)
    ? window.familyInfoData.familyInfo.questionAnswers
    : [];
}


/**
 * 이벤트 바인딩
 */
function bindEvents() {
  // 통합된 클릭 이벤트 처리
  document.addEventListener('click', function(e) {
    const action = e.target.getAttribute('data-action') || e.target.closest('[data-action]')?.getAttribute('data-action');

    if (!action) return;

    switch(action) {
      case 'go-back':
        e.preventDefault();
        handleGoBack();
        break;
      case 'start-video-call':
        e.preventDefault();
        handleStartVideoCall();
        break;
    }
  });

  // 입력 모드인 경우에만 폼 이벤트 바인딩
  if (!pageState.isViewMode) {
    bindFormEvents();
  }
}

/**
 * 폼 이벤트 바인딩
 */
function bindFormEvents() {
  // 폼 제출
  const form = document.getElementById('familyInfoForm');
  if (form) {
    form.addEventListener('submit', handleFormSubmit);
  }
}

/**
 * 질문 이벤트 바인딩 (memorial-create.js 방식)
 */
function bindQuestionEvents() {
  console.log('질문 이벤트 바인딩 시작, 질문 수:', questionsData.length);

  questionsData.forEach(question => {
    const fieldId = `question_${question.id}`;
    const textarea = document.getElementById(fieldId);
    const countEl  = document.getElementById(`${fieldId}Count`);

    if (!textarea || !countEl) {
      console.warn(`질문 요소 없음: ${fieldId}`);
      return;
    }

    // 스타일 초기 적용
    applyTextareaStyles(textarea);
    updateQuestionCharacterCount(textarea, countEl, question);

    let debounce;
    textarea.addEventListener('input', e => {
      // 1) 앞 공백 제거
      e.target.value = e.target.value.replace(/^\s+/, '');

      const val = e.target.value;
      console.log(`질문 ${question.id} 입력:`, val.length, '자');

      // 2) 스타일·카운트·저장·검증
      applyTextareaStyles(e.target);
      updateQuestionAnswer(question.id, val.trim());
      updateQuestionCharacterCount(textarea, countEl, question);

      clearTimeout(debounce);
      debounce = setTimeout(() => performInstantValidation(question, val), 300);

      updateSubmitButton();
    });

    textarea.addEventListener('focus', e => {
      applyTextareaStyles(e.target);
      clearQuestionError(fieldId);
    });

    textarea.addEventListener('blur', e => {
      validateQuestion(question, e.target.value);
    });

    console.log(`질문 ${question.id} 이벤트 바인딩 완료`);
  });
}

/**
 * textarea 스타일 강제 적용
 */
function applyTextareaStyles(textarea) {
  textarea.style.textAlign = 'left';
  textarea.style.direction = 'ltr';
  textarea.style.unicodeBidi = 'embed';
  textarea.style.paddingLeft = '16px';
  textarea.style.paddingRight = '16px';
  textarea.style.textIndent = '0';
  textarea.style.margin = '0';
  textarea.style.whiteSpace = 'pre-wrap';
  textarea.style.wordBreak = 'break-word';
  textarea.style.overflowWrap = 'break-word';
}

/**
 * 폼 초기화
 */
/**
 * 폼 초기화
 */
function initializeForm() {
  console.log('폼 초기화 시작');
  loadServerData();

  // 기존 답변이 있으면 textarea 에 미리 채워넣기
  pageState.questionAnswers.forEach(ans => {
    const ta      = document.getElementById(`question_${ans.questionId}`);
    const countEl = document.getElementById(`question_${ans.questionId}Count`);
    const q       = questionsData.find(q => q.id === ans.questionId);
    if (ta && countEl && q) {
      const v = ans.answerText.trim();
      ta.value = v;
      updateQuestionCharacterCount(ta, countEl, q);
      updateQuestionAnswer(ans.questionId, v);
    }
  });

  // 질문 렌더링 후 즉시 이벤트 바인딩
  bindQuestionEvents();
  updateSubmitButton();
}


/**
 * 기존 답변 로드
 */
function loadExistingAnswers() {
  questionsData.forEach(question => {
    const textarea = document.getElementById(`question_${question.id}`);
    if (textarea && textarea.value.trim()) {
      updateQuestionAnswer(question.id, textarea.value.trim());
    }
  });
}

/**
 * 질문 답변 업데이트 (memorial-create.js 방식)
 */
function updateQuestionAnswer(questionId, answerText) {
  const existingIndex = pageState.questionAnswers.findIndex(
    answer => answer.questionId === questionId
  );

  const answerDTO = {
    questionId: questionId,
    answerText: answerText ? answerText.trim() : ''
  };

  if (existingIndex >= 0) {
    pageState.questionAnswers[existingIndex] = answerDTO;
  } else {
    pageState.questionAnswers.push(answerDTO);
  }

  console.log(`질문 ${questionId} 답변 업데이트:`, answerDTO);
}

/**
 * 글자 수 카운터 업데이트
 */
function updateQuestionCharacterCount(textarea, countElement, question) {
  const currentLength = textarea.value.trim().length;
  countElement.textContent = currentLength;

  const wrapper = countElement.parentElement;
  wrapper.classList.remove('warning', 'danger');

  if (currentLength > 0) {
    const pct = (currentLength / question.maxLength) * 100;
    if (pct >= 90)       wrapper.classList.add('danger');
    else if (pct >= 70)  wrapper.classList.add('warning');
  }
}

/**
 * 실시간 검증
 */
function performInstantValidation(question, value) {
  const fieldId = `question_${question.id}`;
  const textarea = document.getElementById(fieldId);
  const countElement = document.getElementById(`${fieldId}Count`);

  if (!textarea || !countElement) return;

  const currentLength = value.trim().length;
  const parentElement = countElement.parentElement;

  // 글자 수 색상 업데이트
  parentElement.classList.remove('warning', 'danger');

  if (currentLength > 0) {
    const percentage = (currentLength / question.maxLength) * 100;

    if (percentage >= 90) {
      parentElement.classList.add('danger');
    } else if (percentage >= 70) {
      parentElement.classList.add('warning');
    }
  }

  // 즉각적인 에러 체크
  if (currentLength > question.maxLength) {
    showQuestionError(fieldId, `최대 ${question.maxLength}자까지 입력 가능합니다.`);
  } else if (question.isRequired && currentLength === 0) {
    clearQuestionError(fieldId);
  } else {
    clearQuestionError(fieldId);
  }
}

/**
 * 질문 유효성 검사
 */
function validateQuestion(question, value) {
  const fieldId = `question_${question.id}`;
  const textarea = document.getElementById(fieldId);

  if (!textarea) return true;

  // 트림된 값으로 검사
  const trimmedValue = value ? value.trim() : '';
  const currentLength = trimmedValue.length;

  // 에러 초기화
  clearQuestionError(fieldId);

  // 1. 필수 필드 검사
  if (question.isRequired && currentLength === 0) {
    showQuestionError(fieldId, '이 질문은 필수 입력입니다.');
    return false;
  }

  // 2. 내용이 있는 경우만 길이 검사
  if (currentLength > 0) {
    // 최소 길이 검사
    if (currentLength < question.minLength) {
      showQuestionError(fieldId, `최소 ${question.minLength}자 이상 입력해주세요. (현재: ${currentLength}자)`);
      return false;
    }

    // 최대 길이 검사
    if (currentLength > question.maxLength) {
      showQuestionError(fieldId, `최대 ${question.maxLength}자까지 입력 가능합니다. (현재: ${currentLength}자)`);
      return false;
    }
  }

  return true;
}

/**
 * 질문 에러 표시
 */
function showQuestionError(fieldId, message) {
  const textarea = document.getElementById(fieldId);
  const errorElement = document.getElementById(`${fieldId}Error`);

  if (textarea) {
    textarea.classList.add('is-invalid');
  }

  if (errorElement) {
    errorElement.textContent = message;
    errorElement.style.display = 'block';
    errorElement.classList.add('show');
  }
}

/**
 * 질문 에러 초기화
 */
function clearQuestionError(fieldId) {
  const textarea = document.getElementById(fieldId);
  const errorElement = document.getElementById(`${fieldId}Error`);

  if (textarea) {
    textarea.classList.remove('is-invalid');
  }

  if (errorElement) {
    errorElement.style.display = 'none';
    errorElement.classList.remove('show');
    errorElement.textContent = '';
  }
}

/**
 * 모든 질문 유효성 검사
 */
function validateAllQuestions() {
  let isValid = true;
  let firstInvalidField = null;

  questionsData.forEach(question => {
    const fieldId = `question_${question.id}`;
    const textarea = document.getElementById(fieldId);
    const value = textarea ? textarea.value : '';

    const questionValid = validateQuestion(question, value);
    if (!questionValid) {
      isValid = false;
      if (!firstInvalidField) {
        firstInvalidField = textarea;
      }
    }
  });

  // 첫 번째 오류 필드로 스크롤
  if (!isValid && firstInvalidField) {
    firstInvalidField.scrollIntoView({
      behavior: 'smooth',
      block: 'center'
    });
    firstInvalidField.focus();
  }

  return isValid;
}

/**
 * 제출 버튼 상태 업데이트
 */
function updateSubmitButton() {
  const submitBtn = document.getElementById('submitBtn');
  if (!submitBtn) return;

  // 필수 질문 답변 여부 확인
  const questionsValid = checkRequiredQuestionsAnswered();

  submitBtn.disabled = !questionsValid;

  if (questionsValid) {
    submitBtn.innerHTML = '<i class="fas fa-save"></i> 저장하기';
    submitBtn.classList.remove('btn-disabled');
  } else {
    submitBtn.innerHTML = '<i class="fas fa-exclamation-triangle"></i> 필수질문 필요';
    submitBtn.classList.add('btn-disabled');
  }
}

/**
 * 필수 질문 답변 여부 확인
 */
function checkRequiredQuestionsAnswered() {
  const requiredQuestions = questionsData.filter(q => q.isRequired);

  for (const question of requiredQuestions) {
    const answer = pageState.questionAnswers.find(a => a.questionId === question.id);
    if (!answer || !answer.answerText || answer.answerText.trim().length < question.minLength) {
      return false;
    }
  }

  return true;
}

/**
 * 폼 제출 핸들러
 */
async function handleFormSubmit(event) {
  event.preventDefault();

  console.log('폼 제출 시작');
  console.log('현재 질문 답변:', pageState.questionAnswers);

  if (pageState.isLoading) {
    return;
  }

  try {
    // 최종 유효성 검사
    if (!validateAllQuestions()) {
      showToast('입력 정보를 확인해주세요.', 'warning');
      return;
    }

    // 확인 다이얼로그
    const confirmed = await showConfirm(
      '고인 상세 정보 저장',
      '입력하신 정보를 저장하시겠습니까?',
      '저장하기',
      '취소'
    );

    if (!confirmed) {
      return;
    }

    // 로딩 시작
    pageState.isLoading = true;
    const loading = showLoading('정보를 저장하는 중...');

    // 제출 버튼 상태 변경
    const submitBtn = document.getElementById('submitBtn');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 저장 중...';

    // 요청 데이터 준비 (빈 답변 제거)
    const requestData = {
      questionAnswers: pageState.questionAnswers.filter(answer =>
        answer.answerText && answer.answerText.trim().length > 0
      )
    };

    console.log('전송 데이터:', requestData);

    // API 호출
    const response = await authFetch(`/api/memorial/${pageState.memorialId}/family-info`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestData)
    });

    console.log('API 응답:', response);

    if (response.status?.code === 'OK_0000') {
      showToast('고인 상세 정보가 저장되었습니다.', 'success');

      // 3초 후 메인 페이지로 이동
      setTimeout(() => {
        window.location.href = '/mobile/home';
      }, 3000);
    } else {
      throw new Error(response.status?.message || '저장에 실패했습니다.');
    }

  } catch (error) {
    console.error('폼 제출 실패:', error);

    // FetchError인 경우 적절한 메시지 표시
    if (error.name === 'FetchError') {
      showToast(error.statusMessage || '서버 오류가 발생했습니다. 다시 시도해주세요.', 'error');
    } else {
      showToast(error.message || '저장 중 오류가 발생했습니다. 다시 시도해주세요.', 'error');
    }

  } finally {
    // 로딩 종료
    pageState.isLoading = false;
    hideLoading();

    // 제출 버튼 상태 복원
    updateSubmitButton();
  }
}

/**
 * 뒤로가기 핸들러
 */
function handleGoBack() {
  console.log('뒤로가기 클릭');

  // 입력 모드에서 내용이 있는 경우 확인
  if (!pageState.isViewMode) {
    const hasContent = pageState.questionAnswers.some(answer =>
      answer.answerText && answer.answerText.trim().length > 0
    );

    if (hasContent) {
      showConfirm(
        '페이지 나가기',
        '입력하신 내용이 저장되지 않습니다.\n\n정말 나가시겠습니까?',
        '나가기',
        '취소'
      ).then(confirmed => {
        if (confirmed) {
          window.history.back();
        }
      });
      return;
    }
  }

  window.history.back();
}

/**
 * 영상통화 시작 핸들러
 */
function handleStartVideoCall() {
  console.log('영상통화 시작 클릭');

  if (!pageState.memorialId) {
    showToast('메모리얼 정보를 찾을 수 없습니다.', 'error');
    return;
  }

  // 영상통화 페이지로 이동
  window.location.href = `/mobile/videocall/${pageState.memorialId}`;
}

console.log('family-info.js 로드 완료');