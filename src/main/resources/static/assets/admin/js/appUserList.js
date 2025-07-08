/**
 * 회원 목록 페이지 JavaScript
 */
import {adminAuthFetch} from '/js/commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
  'use strict';

  // DOM 요소
  const searchForm = document.getElementById('searchForm');
  const btnViewList = document.getElementById('btnViewList');
  const btnViewCard = document.getElementById('btnViewCard');
  const listView = document.getElementById('listView');
  const cardView = document.getElementById('cardView');
  const statusChangeModalElement = document.getElementById('statusChangeModal');
  // 모달 객체 생성
  let statusChangeModal = null;
  if (statusChangeModalElement) {
    statusChangeModal = new bootstrap.Modal(statusChangeModalElement);
  }

  const btnConfirmStatusChange = document.getElementById('btnConfirmStatusChange');
  const currentStatusText = document.getElementById('currentStatusText');
  const newStatusSelect = document.getElementById('newStatus');
  const statusChangeReason = document.getElementById('statusChangeReason');
  const dateRangeInput = document.getElementById('dateRange');

  // 현재 상태 변경 대상 회원 ID와 원래 포커스 요소
  let currentUserId = null;
  let currentUserStatus = null;
  let lastFocusedElement = null;

  // 초기화
  initPage();

  normalizeDateRangeFormat();

  /** 페이지 초기화 */
  function initPage() {
    initEventListeners();
    initFlatpickrDateRange();

    // 뷰 모드 유지
    const savedViewMode = localStorage.getItem('userListViewMode') || 'list';
    setViewMode(savedViewMode);
  }

  /** 이벤트 리스너 등록 */
  function initEventListeners() {
    // 뷰 전환
    if (btnViewList) btnViewList.addEventListener('click', () => setViewMode('list'));
    if (btnViewCard) btnViewCard.addEventListener('click', () => setViewMode('card'));

    // 상태 변경 버튼 (위임)
    document.addEventListener('click', e => {
      const btn = e.target.closest('.btn-status-change');
      if (!btn) return;
      e.preventDefault();

      // 현재 버튼을 마지막 포커스 요소로 저장
      lastFocusedElement = btn;

      showStatusChangeModal(btn.dataset.userId, btn.dataset.userStatus);
    });

    // 상태 변경 확인
    if (btnConfirmStatusChange) {
      btnConfirmStatusChange.addEventListener('click', confirmStatusChange);
    }

    // 검색 폼 (날짜 범위 검증)
    if (searchForm) {
      searchForm.addEventListener('submit', e => {
        // dateRange 값이 있는 경우 포맷 확인 및 정리
        if (dateRangeInput && dateRangeInput.value) {
          try {
            const dateRange = dateRangeInput.value;
            const parts = dateRange.split(/\s*~\s*/);

            if (parts.length === 2) {
              const startDate = parts[0].trim();
              const endDate = parts[1].trim();

              // 날짜 형식이 YYYY-MM-DD인지 확인
              const datePattern = /^\d{4}-\d{2}-\d{2}$/;

              if (datePattern.test(startDate) && datePattern.test(endDate)) {
                // 형식이 맞으면 깔끔하게 포맷팅
                dateRangeInput.value = `${startDate} ~ ${endDate}`;
              }
            }
          } catch (err) {
            console.error('날짜 형식 검증 중 오류:', err);
          }
        }
      });
    }

    // 모달 이벤트
    if (statusChangeModalElement) {
      // 모달이 표시된 후 이벤트
      statusChangeModalElement.addEventListener('shown.bs.modal', function() {
        // 모달 내 첫 번째 폼 요소에 포커스
        if (newStatusSelect) {
          setTimeout(() => {
            newStatusSelect.focus();
          }, 100);
        }
      });

      // 모달이 닫힌 후 이벤트
      statusChangeModalElement.addEventListener('hidden.bs.modal', function() {
        // 원래 요소로 포커스 돌려주기
        if (lastFocusedElement) {
          setTimeout(() => {
            lastFocusedElement.focus();
          }, 50);
        }
      });
    }
  }

  /** 뷰 모드 적용 */
  function setViewMode(mode) {
    if (!listView || !cardView || !btnViewList || !btnViewCard) return;

    if (mode === 'card') {
      listView.classList.add('d-none');
      cardView.classList.remove('d-none');
      btnViewList.classList.remove('active');
      btnViewCard.classList.add('active');
    } else {
      listView.classList.remove('d-none');
      cardView.classList.add('d-none');
      btnViewList.classList.add('active');
      btnViewCard.classList.remove('active');
    }
    localStorage.setItem('userListViewMode', mode);
  }

  /**
 * Flatpickr 초기화 함수 수정 부분
 */
function initFlatpickrDateRange() {
  if (!dateRangeInput || typeof flatpickr === 'undefined') {
    console.error('Flatpickr가 로드되지 않았거나 dateRangeInput 요소가 없습니다.');
    return;
  }

  try {
    // 날짜 범위 값 파싱
    // 먼저 입력값의 형식 정규화
    const currentValue = dateRangeInput.value.trim();
    let initialDates = null;

    if (currentValue) {
      // '월월-날날' 패턴을 찾는 정규식으로 정리
      let normalizedValue = currentValue;
      const monthPattern = /(\d{4})-(\d{1,2})월\2월-(\d{1,2})(\d{1,2})/g;

      if (monthPattern.test(normalizedValue)) {
        normalizedValue = normalizedValue.replace(monthPattern, function(match, year, month, day1, day2) {
          const paddedMonth = month.padStart(2, '0');
          const paddedDay = (day1 + day2).padStart(2, '0');
          return `${year}-${paddedMonth}-${paddedDay}`;
        });
      }

      // 날짜 범위 분리
      const parts = normalizedValue.split(/\s*~\s*/);
      if (parts.length === 2) {
        const startDate = parts[0].trim();
        const endDate = parts[1].trim();

        // 날짜 객체로 변환 시도
        try {
          initialDates = [new Date(startDate), new Date(endDate)];
        } catch (e) {
          console.warn('날짜 변환 실패:', e);
        }
      }
    }

    // 기존 Flatpickr 초기화 코드
    const wrapper = dateRangeInput.closest('.input-group');
    if (!wrapper) return;

    // 클리어 버튼 생성
    let clearBtn = wrapper.querySelector('.date-clear-btn');
    if (!clearBtn) {
      clearBtn = document.createElement('button');
      clearBtn.type = 'button';
      clearBtn.className = 'date-clear-btn';
      clearBtn.innerHTML = '<i class="fas fa-times"></i>';
      wrapper.appendChild(clearBtn);
    }

    // 초기 클리어 버튼 표시 여부
    clearBtn.style.display = initialDates ? 'flex' : 'none';

    // 달력 아이콘 설정
    const calendarIcon = wrapper.querySelector('.input-group-text');
    if (calendarIcon) {
      calendarIcon.style.cursor = 'pointer';
    }

    // Flatpickr 설정
    const fpInstance = flatpickr(dateRangeInput, {
      mode: 'range',
      dateFormat: 'Y-m-d',  // 날짜 형식을 명확하게 지정
      locale: 'ko',
      disableMobile: true,
      defaultDate: initialDates,
      onChange: function(selectedDates) {
        if (selectedDates.length === 2) {
          // 날짜가 모두 선택된 경우, 명확한 형식으로 표시
          const formatted = selectedDates.map(date => formatDate(date));
          dateRangeInput.value = formatted.join(' ~ ');
          clearBtn.style.display = 'flex';
        }
      }
    });

    // 달력 아이콘 클릭 이벤트
    if (calendarIcon) {
      calendarIcon.addEventListener('click', () => fpInstance.toggle());
    }

    // 클리어 버튼 클릭 이벤트
    if (clearBtn) {
      clearBtn.addEventListener('click', e => {
        e.preventDefault();
        e.stopPropagation();
        fpInstance.clear();
        dateRangeInput.value = '';
        clearBtn.style.display = 'none';
      });
    }

    // 폼 제출 전 날짜 형식 정규화
    const searchForm = document.getElementById('searchForm');
    if (searchForm) {
      searchForm.addEventListener('submit', function(e) {
        // 날짜 범위 필드 값 정규화
        if (dateRangeInput.value) {
          try {
            // 월월-날날 패턴 수정
            const monthPattern = /(\d{4})-(\d{1,2})월\2월-(\d{1,2})(\d{1,2})/g;
            let normalizedValue = dateRangeInput.value;

            if (monthPattern.test(normalizedValue)) {
              normalizedValue = normalizedValue.replace(monthPattern, function(match, year, month, day1, day2) {
                const paddedMonth = month.padStart(2, '0');
                const paddedDay = (day1 + day2).padStart(2, '0');
                return `${year}-${paddedMonth}-${paddedDay}`;
              });
            }

            // 날짜 범위 분리 및 재조합
            const parts = normalizedValue.split(/\s*~\s*/);
            if (parts.length === 2) {
              dateRangeInput.value = `${parts[0].trim()} ~ ${parts[1].trim()}`;
            }
          } catch (err) {
            console.error('폼 제출 전 날짜 형식 정규화 오류:', err);
          }
        }
      });
    }

  } catch (error) {
    console.error('Flatpickr 초기화 중 오류가 발생했습니다:', error);
  }
}

  /**
   * 날짜 문자열 파싱하여 Date 객체 배열 반환
   * @param {string} dateRangeStr
   * @returns {Array|null}
   */
  function parseInitialDateRange(dateRangeStr) {
    if (!dateRangeStr) return null;

    try {
      const parts = dateRangeStr.split(/\s*~\s*/);
      if (parts.length !== 2) return null;

      // 시작일과 종료일 추출
      const startStr = parts[0].trim();
      const endStr = parts[1].trim();

      // YYYY-MM-DD 형식 검사
      const datePattern = /^\d{4}-\d{2}-\d{2}$/;

      if (!datePattern.test(startStr) || !datePattern.test(endStr)) {
        console.warn('날짜 형식이 맞지 않습니다:', startStr, endStr);
        return null;
      }

      // Date 객체로 변환
      return [new Date(startStr), new Date(endStr)];
    } catch (err) {
      console.error('날짜 범위 파싱 오류:', err);
      return null;
    }
  }

  /**
   * Date 객체를 YYYY-MM-DD 형식으로 변환
   * @param {Date} date
   * @returns {string}
   */
  function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * 날짜 범위 형식 정규화 함수
 * - URL 파라미터에서 가져온 데이터 형식 정리
 * - 페이지 로드 시 한 번 실행
 */
function normalizeDateRangeFormat() {
  const dateRangeInput = document.getElementById('dateRange');
  if (!dateRangeInput) return;

  try {
    // 현재 입력란에 있는 값 확인
    const currentValue = dateRangeInput.value.trim();
    if (!currentValue) return;

    // '월월-날날' 패턴을 찾는 정규식
    const monthPattern = /(\d{4})-(\d{1,2})월\2월-(\d{1,2})(\d{1,2})/g;

    // 정규식으로 잘못된 형식 변환
    if (monthPattern.test(currentValue)) {
      const correctedValue = currentValue.replace(monthPattern, function(match, year, month, day1, day2) {
        // 월과 일을 2자리로 패딩
        const paddedMonth = month.padStart(2, '0');
        const paddedDay = (day1 + day2).padStart(2, '0');
        return `${year}-${paddedMonth}-${paddedDay}`;
      });

      // 날짜 범위 형식 (~ 기호 기준)으로 분리
      const parts = correctedValue.split(/\s*~\s*/);
      if (parts.length === 2) {
        const formattedValue = `${parts[0].trim()} ~ ${parts[1].trim()}`;
        dateRangeInput.value = formattedValue;

        // 클리어 버튼 표시 업데이트
        const wrapper = dateRangeInput.closest('.input-group');
        const clearBtn = wrapper?.querySelector('.date-clear-btn');
        if (clearBtn) {
          clearBtn.style.display = 'flex';
        }
      }
    }
  } catch (err) {
    console.error('날짜 형식 정규화 중 오류:', err);
  }
}

  /**
   * 모달 열기
   * @param {string} userId
   * @param {string} userStatus  (예: 'ACTIVE', 'BLOCKED' 등)
   */
  function showStatusChangeModal(userId, userStatus) {
    if (!userId || !statusChangeModal) {
      console.error('회원 ID가 없거나 모달을 찾을 수 없습니다.');
      return;
    }

    currentUserId = userId;
    currentUserStatus = userStatus;

    // 뱃지 클래스/텍스트 적용
    if (currentStatusText) {
      currentStatusText.className = 'badge rounded-pill ' + getStatusBadgeClass(userStatus);
      currentStatusText.textContent = getStatusDisplayName(userStatus);
    }

    // 모달 필드 초기화
    if (newStatusSelect) newStatusSelect.value = userStatus;
    if (statusChangeReason) statusChangeReason.value = '';

    // 모달 열기
    statusChangeModal.show();
  }

  /** 상태 변경 요청 */
  async function confirmStatusChange() {
    if (!currentUserId) {
      showAlert('회원 ID가 없습니다.', 'warning');
      if (statusChangeModal) statusChangeModal.hide();
      return;
    }

    if (!newStatusSelect) {
      console.error('상태 선택 요소를 찾을 수 없습니다.');
      return;
    }

    const newStatus = newStatusSelect.value;
    const reason = statusChangeReason ? statusChangeReason.value.trim() : '';

    if (newStatus === currentUserStatus) {
      if (statusChangeModal) statusChangeModal.hide();
      showAlert("기존 상태와 동일한 값입니다. <br/> 다시 시도해주세요.",'info')
      return;
    }

    if (btnConfirmStatusChange) {
      btnConfirmStatusChange.disabled = true;
      btnConfirmStatusChange.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 처리 중...';
    }

    try {
      const res = await adminAuthFetch(`/admin/users/app/user/${currentUserId}/status`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: newStatus, reason: reason || null })
      });

      if (!res.ok) throw new Error(`서버 응답 오류: ${res.status}`);
      const result = await res.json();
      if (result.status.code !== 'OK_0000') throw new Error(result.status.message || '상태 변경 실패');

      showAlert('회원 상태가 변경되었습니다.', 'success');

      // UI 업데이트
      updateUserStatusUI(currentUserId, newStatus);

      // 모달 닫기
      if (statusChangeModal) statusChangeModal.hide();

    } catch (err) {
      console.error('상태 변경 오류:', err);
      showAlert('오류: ' + err.message, 'danger');
    } finally {
      if (btnConfirmStatusChange) {
        btnConfirmStatusChange.disabled = false;
        btnConfirmStatusChange.innerHTML = '변경';
      }
    }
  }

  /** UI에 새 상태 반영 */
  function updateUserStatusUI(userId, newStatus) {
    // 리스트 뷰 업데이트 - querySelector null 오류 방지
    const statusButtons = document.querySelectorAll(`[data-user-id="${userId}"]`);
    statusButtons.forEach(button => {
      try {
        // 먼저 상위 행(tr) 요소 확인
        const row = button.closest('tr');
        if (row) {
          const badge = row.querySelector('.badge');
          if (badge) {
            badge.className = 'badge rounded-pill ' + getStatusBadgeClass(newStatus);
            badge.textContent = getStatusDisplayName(newStatus);
          }
        }

        // 카드 뷰 요소 확인
        const card = button.closest('.user-card');
        if (card) {
          const cardBadge = card.querySelector('.badge');
          if (cardBadge) {
            cardBadge.className = 'badge rounded-pill ' + getStatusBadgeClass(newStatus);
            cardBadge.textContent = getStatusDisplayName(newStatus);
          }
        }

        // 버튼 자체의 데이터 속성 업데이트
        button.dataset.userStatus = newStatus;
      } catch (error) {
        console.error('UI 업데이트 중 오류:', error);
      }
    });
  }

  /**
   * 상태별 뱃지 클래스
   * @param {string} status
   * @returns {string}
   */
  function getStatusBadgeClass(status) {
    switch (status) {
      case 'ACTIVE':             // 활성
        return 'bg-success';
      case 'BLOCKED':            // 차단
        return 'bg-danger';
      case 'TEMPORARY_INVITE':   // 임시 초대
        return 'bg-warning';
      case 'TEMPORARY_PASS':     // 임시 통과
        return 'bg-info';
      case 'DELETED':            // 탈퇴
        return 'bg-secondary';
      default:
        return 'bg-secondary';
    }
  }

  /**
   * 상태별 표시 이름
   * @param {string} status
   * @returns {string}
   */
  function getStatusDisplayName(status) {
    switch (status) {
      case 'ACTIVE':
        return '활성';
      case 'BLOCKED':
        return '차단';
      case 'TEMPORARY_INVITE':
        return '임시 초대';
      case 'TEMPORARY_PASS':
        return '임시 통과';
      case 'DELETED':
        return '탈퇴';
      default:
        return '알 수 없음';
    }
  }

  /** 알림 */
  function showAlert(message, type = 'info') {
    if (window.CustomNotification) {
      window.CustomNotification.show(message, type);
    } else {
      alert(message);
    }
  }
});