/**
 * 투표 등록/수정 페이지 JavaScript
 */
import { adminAuthFetch } from '../../../js/commonFetch.js';

document.addEventListener('DOMContentLoaded', function() {
  'use strict';

  // DOM 요소
  const voteForm = document.getElementById('voteForm');
  const btnSubmit = document.getElementById('btnSubmit');
  const voteOptionsContainer = document.querySelector('.vote-options');
    const addOptionBtn = document.getElementById('addOption');

  // Flatpickr 초기화 (날짜 선택기)
  initDatePickers();

  // 선택지 추가 버튼 이벤트
  if (addOptionBtn) {
    addOptionBtn.addEventListener('click', addVoteOption);
  }

  // 선택지 제거 버튼 이벤트 위임
  if (voteOptionsContainer) {
    voteOptionsContainer.addEventListener('click', function(e) {
      if (e.target.classList.contains('remove-option') || e.target.closest('.remove-option')) {
        removeVoteOption(e.target.closest('.vote-option'));
      }
    });
  }

  // 제출 버튼 이벤트
  if (btnSubmit) {
    btnSubmit.addEventListener('click', validateAndSubmit);
  }

  /**
   * Flatpickr 초기화 함수
   */
  function initDatePickers() {
    // 공통 옵션
    const datePickerOptions = {
      locale: 'ko',
      dateFormat: 'Y-m-d',
      allowInput: true,
      disableMobile: true
    };

    // 시작일 Flatpickr
    const startDatePicker = flatpickr('#startDate', {
      ...datePickerOptions,
      minDate: 'today',
      onChange: function(selectedDates, dateStr) {
        // 종료일 최소값을 시작일로 설정
        endDatePicker.set('minDate', dateStr);
      }
    });

    // 종료일 Flatpickr
    const endDatePicker = flatpickr('#endDate', {
      ...datePickerOptions,
      minDate: document.getElementById('startDate').value || 'today'
    });
  }

  /**
   * 투표 선택지 추가 함수
   */
  function addVoteOption() {
    const optionCount = document.querySelectorAll('.vote-option').length;

    // 최대 10개까지 선택지 추가 가능
    if (optionCount >= 10) {
      showAlert('최대 10개까지 선택지를 추가할 수 있습니다.', 'warning');
      return;
    }

    // 새 선택지 요소 생성
    const newOption = document.createElement('div');
    newOption.className = 'row mb-3 vote-option fade-in';

    // 수정 모드인지 확인
    const isEditing = !!document.getElementById('voteId');

    newOption.innerHTML = `
      <div class="col-md-11">
        <div class="input-group">
          <span class="input-group-text">옵션 ${optionCount + 1}</span>
          <input type="text" class="form-control" name="optionNames[]" placeholder="선택지를 입력하세요" required maxlength="50">
          ${isEditing ? '<input type="hidden" name="optionIds[]" value="">' : ''}
        </div>
      </div>
      <div class="col-md-1 d-flex align-items-center">
        <button type="button" class="btn btn-outline-danger remove-option">
          <i class="fas fa-times"></i>
        </button>
      </div>
    `;

    // 선택지 컨테이너에 추가
    voteOptionsContainer.appendChild(newOption);

    // 새 입력 필드에 포커스
    const newInput = newOption.querySelector('input');
    if (newInput) {
      setTimeout(() => {
        newInput.focus();
      }, 100);
    }
  }


  /**
   * 투표 선택지 제거 함수
   * @param {HTMLElement} optionElement - 제거할 선택지 요소
   */
  function removeVoteOption(optionElement) {
    const optionCount = document.querySelectorAll('.vote-option').length;

    // 최소 2개의 선택지는 유지
    if (optionCount <= 2) {
      showAlert('최소 2개의 선택지가 필요합니다.', 'warning');
      return;
    }

    // 애니메이션 후 요소 제거
    optionElement.classList.remove('fade-in');
    optionElement.classList.add('fade-out');

    setTimeout(() => {
      optionElement.remove();

      // 선택지 번호 재정렬
      document.querySelectorAll('.vote-option').forEach((option, index) => {
        option.querySelector('.input-group-text').innerHTML = `옵션 ${index + 1}`;
      });
    }, 300);
  }

  /**
   * 폼 유효성 검사 및 제출 함수
   */
  function validateAndSubmit() {
    // 투표 제목 검사
    const question = document.getElementById('question').value.trim();
    if (!question) {
      showAlert('투표 제목을 입력해주세요.', 'danger');
      document.getElementById('question').focus();
      return;
    }

    // 선택지 검사
    const options = document.querySelectorAll('input[name="optionNames[]"]');
    let emptyOption = false;

    options.forEach(option => {
      if (!option.value.trim()) {
        emptyOption = true;
      }
    });

    if (emptyOption) {
      showAlert('모든 선택지를 입력해주세요.', 'danger');
      return;
    }

    // 중복 선택지 확인
    const optionTexts = Array.from(options).map(option => option.value.trim());
    const uniqueOptions = new Set(optionTexts);

    if (uniqueOptions.size !== optionTexts.length) {
      showAlert('중복된 선택지가 있습니다.', 'danger');
      return;
    }

    // Fetch API로 전송
    submitFormWithFetch();
  }

  /**
   * Fetch API를 사용한 폼 제출
   */
  async function submitFormWithFetch() {
  btnSubmit.disabled = true;
  const originalBtnText = btnSubmit.innerHTML;
  btnSubmit.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 처리 중...';

  const isEditing = !!document.getElementById('voteId');
  const data = {
    question:  document.getElementById('question').value.trim(),
    startDate: document.getElementById('startDate').value.trim(),
    endDate:   document.getElementById('endDate').value.trim(),
    voteOptions: []
  };

  // ——— voteOptions 구성 ———
  const nameEls = document.querySelectorAll('input[name="optionNames[]"]');

  if (isEditing) {
    // 수정 모드: optionIds[] 와 짝을 맞춰서 둘 다 보냄
    const idEls = document.querySelectorAll('input[name="optionIds[]"]');
    nameEls.forEach((nameEl, idx) => {
      const rawId = idEls[idx]?.value;
      data.voteOptions.push({
        optionId: rawId ? Number(rawId) : null,
        optionName: nameEl.value.trim()
      });
    });

    data.voteId = Number(document.getElementById('voteId').value);
  } else {
    // 등록 모드: optionId 없이 optionName 만 보냄 (optionId 는 null)
    nameEls.forEach(nameEl => {
      data.voteOptions.push({
        optionId: null,
        optionName: nameEl.value.trim()
      });
    });
  }
  // ————————————————————

  const endpoint = isEditing
    ? {url : '/admin/vote/update', method:'PUT'}
    : {url : '/admin/vote/create', method: 'POST'};

  try {
    const res = await adminAuthFetch(endpoint.url, {
      method: endpoint.method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    if (!res.ok) throw new Error('서버 응답 오류: ' + res.status);
    const result = await res.json();

    if (result.status.code==='OK_0000') {
      showAlert(isEditing ? '투표가 수정되었습니다.' : '투표가 등록되었습니다.', 'success');
      setTimeout(() => window.location.href = '/admin/vote/list', 1000);
    } else {
      throw new Error(result.message || '처리 중 오류가 발생했습니다.');
    }
  } catch (err) {
    console.error(err);
    showAlert('요청 처리 중 오류가 발생했습니다: ' + err.message, 'danger');
    btnSubmit.disabled = false;
    btnSubmit.innerHTML = originalBtnText;
  }
}



  /**
   * 알림 표시 함수
   * @param {string} message - 알림 메시지
   * @param {string} type - 알림 타입 (success, danger, warning, info)
   */
  function showAlert(message, type = 'info') {
    // 커스텀 알림 시스템 사용
    if (window.CustomNotification) {
      window.CustomNotification.show(message, type);
      return;
    }

    // 기본 alert 사용
    alert(message);
  }
});