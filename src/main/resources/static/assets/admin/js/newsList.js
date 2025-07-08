/**
 * 뉴스 목록 페이지 JavaScript
 */
import {adminAuthFetch} from '/js/commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
  'use strict';

  // DOM 요소
  const searchForm = document.getElementById('searchForm');
  const btnViewGrid = document.getElementById('btnViewGrid');
  const btnViewList = document.getElementById('btnViewList');
  const listView = document.getElementById('listView');
  const gridView = document.getElementById('gridView');
  const btnConfirmDelete = document.getElementById('btnConfirmDelete');
  const deleteConfirmModal = new bootstrap.Modal(
      document.getElementById('deleteConfirmModal'));
  const dateRangeInput = document.getElementById('dateRange');
  const btnExportCSV = document.getElementById('btnExportCSV');

  // 현재 삭제 대상 뉴스 ID
  let currentDeleteNewsId = null;

  // 초기화
  initPage();

  // 날짜 범위 값 정규화 (페이지 로딩 시 자동 실행)
  normalizeDateRangeFormat();

  /**
   * 페이지 초기화 함수
   */
  function initPage() {
    // 이벤트 리스너 등록
    initEventListeners();

    // Flatpickr 달력 초기화
    initFlatpickrDateRange();

    // 뷰 모드 설정 (로컬 스토리지에서 불러오기)
    const savedViewMode = localStorage.getItem('newsListViewMode') || 'list';
    setViewMode(savedViewMode);
  }

  /**
   * 이벤트 리스너 초기화
   */
  function initEventListeners() {
    // 목록/그리드 뷰 전환 버튼
    if (btnViewList && btnViewGrid) {
      btnViewList.addEventListener('click', function () {
        setViewMode('list');
      });

      btnViewGrid.addEventListener('click', function () {
        setViewMode('grid');
      });
    }

    // 삭제 버튼 이벤트 (동적으로 추가될 수 있으므로 이벤트 위임 사용)
    document.addEventListener('click', function (e) {
      const deleteBtn = e.target.closest('.btn-delete');
      if (deleteBtn) {
        e.preventDefault();
        showDeleteConfirmModal(deleteBtn.dataset.newsId);
      }
    });

    // 삭제 확인 버튼 이벤트
    if (btnConfirmDelete) {
      btnConfirmDelete.addEventListener('click', confirmDelete);
    }

    // 검색 폼 제출 이벤트
    if (searchForm) {
      searchForm.addEventListener('submit', function(e) {
        e.preventDefault();

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

        performSearch();
      });

      // 검색 버튼 클릭 이벤트
      const searchButton = searchForm.querySelector('button[type="submit"]');
      if (searchButton) {
        searchButton.addEventListener('click', function(e) {
          e.preventDefault();
          performSearch();
        });
      }

      // 검색어 입력 필드에서 엔터 키 이벤트
      const searchTextInput = document.getElementById('searchText');
      if (searchTextInput) {
        searchTextInput.addEventListener('keypress', function(e) {
          if (e.key === 'Enter') {
            e.preventDefault();
            performSearch();
          }
        });
      }
    }

    // CSV 내보내기 버튼 이벤트
    if (btnExportCSV) {
      btnExportCSV.addEventListener('click', exportToCSV);
    }
  }

  /**
   * 검색 실행
   */
  function performSearch() {
    if (searchForm) {
      searchForm.submit();
    }
  }

  /**
   * 목록/그리드 뷰 전환
   * @param {string} mode - 뷰 모드 ('list' 또는 'grid')
   */
  function setViewMode(mode) {
    if (mode === 'grid') {
      listView.classList.add('d-none');
      gridView.classList.remove('d-none');
      btnViewList.classList.remove('active');
      btnViewGrid.classList.add('active');
    } else {
      listView.classList.remove('d-none');
      gridView.classList.add('d-none');
      btnViewList.classList.add('active');
      btnViewGrid.classList.remove('active');
    }

    // 로컬 스토리지에 선택 저장
    localStorage.setItem('newsListViewMode', mode);
  }

  /**
   * Flatpickr 날짜 범위 선택기 초기화 함수
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

            // 유효하지 않은 날짜 확인
            if (isNaN(initialDates[0].getTime()) || isNaN(initialDates[1].getTime())) {
              console.warn('유효하지 않은 날짜 형식입니다:', startDate, endDate);
              initialDates = null;
            }
          } catch (e) {
            console.warn('날짜 변환 실패:', e);
            initialDates = null;
          }
        }
      }

      // 상위 요소에 특별한 클래스 추가
      const wrapper = dateRangeInput.closest('.input-group');
      if (!wrapper) return;

      wrapper.classList.add('date-range-input-group');

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
   * 날짜 범위 형식 정규화 함수
   * - URL 파라미터에서 가져온 데이터 형식 정리
   * - 페이지 로드 시 한 번 실행
   */
  function normalizeDateRangeFormat() {
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
   * 삭제 확인 모달 표시
   * @param {number} newsId - 삭제할 뉴스 ID
   */
  function showDeleteConfirmModal(newsId) {
    if (!newsId) {
      showAlert('삭제할 뉴스 ID가 없습니다.', 'warning');
      return;
    }

    // 현재 삭제 대상 ID 설정
    currentDeleteNewsId = newsId;

    // 모달 표시
    deleteConfirmModal.show();
  }

  /**
   * 삭제 확인 처리
   */
  async function confirmDelete() {
    if (!currentDeleteNewsId) {
      showAlert('삭제할 뉴스 ID가 없습니다.', 'warning');
      deleteConfirmModal.hide();
      return;
    }

    btnConfirmDelete.disabled = true;
    btnConfirmDelete.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 삭제 중...';

    try {
      const res = await adminAuthFetch(
          `/admin/original/news/delete/${currentDeleteNewsId}`, {
            method: 'DELETE'
          });

      if (!res.ok) {
        throw new Error('서버 응답 오류: ' + res.status);
      }

      const result = await res.json();

      if (result.status.code === 'OK_0000') {
        showAlert('뉴스가 성공적으로 삭제되었습니다.', 'success');

        // 목록에서 해당 항목 제거
        const listItem = document.querySelector(
            `[data-news-id="${currentDeleteNewsId}"]`).closest('tr');
        if (listItem) {
          listItem.classList.add('fade-out');

          // 애니메이션 후 제거
          setTimeout(() => {
            listItem.remove();

            // 항목이 없으면 "데이터 없음" 메시지 표시
            const tbody = document.querySelector('tbody');
            if (tbody && tbody.children.length === 0) {
              const emptyRow = document.createElement('tr');
              emptyRow.innerHTML = `
                <td colspan="7" class="text-center py-5">
                  <i class="fas fa-newspaper fa-3x text-muted mb-3"></i>
                  <p class="mb-0">조건에 맞는 뉴스가 없습니다.</p>
                </td>
              `;
              tbody.appendChild(emptyRow);
            }
          }, 300);
        }

        // 그리드 뷰에서도 제거
        const gridItem = document.querySelector(
            `.col-sm-6.col-md-4 [data-news-id="${currentDeleteNewsId}"]`).closest(
            '.col-sm-6.col-md-4');
        if (gridItem) {
          gridItem.classList.add('fade-out');

          // 애니메이션 후 제거
          setTimeout(() => {
            gridItem.remove();

            // 항목이 없으면 "데이터 없음" 메시지 표시
            const gridContainer = document.querySelector('#gridView .row');
            if (gridContainer && gridContainer.children.length === 0) {
              const emptyDiv = document.createElement('div');
              emptyDiv.className = 'text-center py-5';
              emptyDiv.innerHTML = `
                <i class="fas fa-newspaper fa-3x text-muted mb-3"></i>
                <p class="mb-0">조건에 맞는 뉴스가 없습니다.</p>
              `;
              gridView.innerHTML = '';
              gridView.appendChild(emptyDiv);
            }
          }, 300);
        }

        // 삭제 성공 후 현재 페이지 새로고침
        setTimeout(() => {
          window.location.reload();
        }, 1000);
      } else {
        throw new Error(result.message || '삭제 중 오류가 발생했습니다.');
      }
    } catch (err) {
      console.error(err);
      showAlert('삭제 중 오류가 발생했습니다: ' + err.message, 'danger');
    } finally {
      btnConfirmDelete.innerHTML = '삭제';
      btnConfirmDelete.disabled = false;
      deleteConfirmModal.hide();
    }
  }

  /**
   * CSV 파일로 내보내기
   */
  async function exportToCSV() {
    btnExportCSV.disabled = true;
    btnExportCSV.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 내보내는 중...';

    try {
      // 현재 검색 조건을 가져옴
      const searchParams = new URLSearchParams(window.location.search);
      searchParams.set('export', 'csv');

      const res = await adminAuthFetch(`/admin/news/export?${searchParams.toString()}`);

      if (!res.ok) {
        throw new Error('서버 응답 오류: ' + res.status);
      }

      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);

      // 가상 다운로드 링크 생성
      const a = document.createElement('a');
      a.style.display = 'none';
      a.href = url;
      a.download = `news_export_${new Date().toISOString().slice(0, 10)}.csv`;

      document.body.appendChild(a);
      a.click();

      // 정리
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      showAlert('CSV 파일이 성공적으로 내보내졌습니다.', 'success');
    } catch (err) {
      console.error(err);
      showAlert('CSV 내보내기 중 오류가 발생했습니다: ' + err.message, 'danger');
    } finally {
      btnExportCSV.innerHTML = '<i class="fas fa-download"></i> CSV 내보내기';
      btnExportCSV.disabled = false;
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