// assets/admin/js/voteAdminList.js
import { adminAuthFetch } from '/js/commonFetch.js';

document.addEventListener('DOMContentLoaded', () => {
  const table       = document.getElementById('voteTable');
  const checkAll    = document.getElementById('checkAll');
  const deleteBtn   = document.getElementById('btnDelete');
  const createBtn   = document.getElementById('btnCreate');
  const updateBtn   = document.getElementById('btnEdit');
  const confirmModal= new bootstrap.Modal(document.getElementById('confirmModal'));
  let selectedIds   = [];

  // 1) 전체 선택 / 해제
  checkAll?.addEventListener('change', e => {
    const checked = e.target.checked;
    document.querySelectorAll('.row-check').forEach(cb => {
      cb.checked = checked;
      toggleRowHighlight(cb.closest('tr'), checked);
    });
    updateDeleteBtn();
  });

  // 2) 각 행 체크박스 클릭
  document.querySelectorAll('.row-check').forEach(cb => {
    cb.addEventListener('change', () => {
      toggleRowHighlight(cb.closest('tr'), cb.checked);
      updateDeleteBtn();
      syncCheckAll();
    });

    // 체크박스 클릭 시 이벤트 전파 중지 (행 클릭 이벤트와 겹치지 않도록)
    cb.addEventListener('click', (e) => {
      e.stopPropagation();
    });
  });

  // 3) 행 클릭 시 체크박스 토글
  table?.querySelectorAll('tbody tr').forEach(tr => {
    tr.addEventListener('click', (e) => {
      // 링크나 뱃지 클릭 시에는 동작하지 않음
      if (e.target.tagName === 'A' || e.target.closest('a') ||
          e.target.tagName === 'SPAN' || e.target.closest('span.badge')) {
        return;
      }

      const checkbox = tr.querySelector('.row-check');
      if (checkbox) {
        checkbox.checked = !checkbox.checked;
        toggleRowHighlight(tr, checkbox.checked);
        updateDeleteBtn();
        syncCheckAll();
      }
    });
  });

  // 4) 더블클릭 시 상세 페이지로 이동
  table?.querySelectorAll('tbody tr').forEach(tr => {
    tr.addEventListener('dblclick', () => {
      const id = tr.getAttribute('data-id');
      if (id) window.location.href = `/admin/vote/${id}`;
    });
  });

  // 5) 삭제 버튼 클릭 → 모달 띄우기
  deleteBtn?.addEventListener('click', () => {
    selectedIds = Array.from(
      document.querySelectorAll('.row-check:checked'),
      cb => cb.value
    );
    if (selectedIds.length) confirmModal.show();
  });

  createBtn?.addEventListener('click', () => {
    window.location.href = `/admin/vote/create`
  });

  updateBtn?.addEventListener('click', () => {
    selectedIds = Array.from(
      document.querySelectorAll('.row-check:checked'),
      cb => cb.value
    );
    if(selectedIds.length===0){
      showAlert('수정할 게시물을 선택해주세요', 'warning');
      return ;
    }

    if(selectedIds.length > 1){
      showAlert('1개만 선택이 가능합니다.', 'warning');
      return ;
    }

    if (selectedIds.length === 1) window.location.href = `/admin/vote/update/${selectedIds[0]}`
  });

  // 6) 모달에서 확인 클릭 → 삭제 API 호출 후 새로고침
  document.getElementById('btnConfirmDelete')?.addEventListener('click', async () => {
    try {
      await adminAuthFetch('/admin/vote/delete', {
        method: 'DELETE',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({ ids: selectedIds })
      });
      location.reload();
    } catch (err) {
      console.error(err);
      alert('삭제 중 오류가 발생했습니다.');
    }
  });

  // 7) 검색 기능 - 수정된 부분
  const searchForm = document.getElementById('searchForm');
  const searchTypeSelect = document.getElementById('searchType');
  const searchTextInput = document.getElementById('searchText');
  const searchButton = document.getElementById('btnSearch');

  // URL 파라미터에서 검색 조건 가져오기
  const urlParams = new URLSearchParams(window.location.search);
  const searchType = urlParams.get('searchType');
  const keyword = urlParams.get('searchText');

  // 검색 조건 설정
  if (searchType && searchTypeSelect) {
    searchTypeSelect.value = searchType;
  }

  if (keyword && searchTextInput) {
    searchTextInput.value = keyword;
  }

  // 폼 제출 이벤트 (HTML에서 form으로 변경된 경우)
  searchForm?.addEventListener('submit', (e) => {
    e.preventDefault();
    performSearch();
  });

  // 검색 버튼 클릭 이벤트
  searchButton?.addEventListener('click', (e) => {
    e.preventDefault();
    performSearch();
  });

  // 검색어 입력 필드에서 엔터 키 이벤트
  searchTextInput?.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      performSearch();
    }
  });

  // Helper: 행 강조
  function toggleRowHighlight(tr, on) {
    if (tr) tr.classList.toggle('table-active', on);
  }

  // Helper: 삭제 버튼 활성화
  function updateDeleteBtn() {
    const any = !!document.querySelector('.row-check:checked');
    deleteBtn.disabled = !any;
  }

  // Helper: 전체 체크박스 상태 동기화
  function syncCheckAll() {
    const all = Array.from(document.querySelectorAll('.row-check'));
    const anyChecked = all.some(cb => cb.checked);
    const allChecked = all.length > 0 && all.every(cb => cb.checked);

    checkAll.checked = allChecked;
    checkAll.indeterminate = anyChecked && !allChecked;
  }

  // Helper: 검색 실행 - 수정된 부분 (size=10 파라미터 추가)
  function performSearch() {
    const type = searchTypeSelect ? searchTypeSelect.value : 'VOTE_QUESTION';
    const keyword = searchTextInput ? searchTextInput.value.trim() : '';

    // URLSearchParams를 사용하여 파라미터 구성
    const params = new URLSearchParams();
    params.append('size', '10'); // size 고정

    if (keyword) {
      params.append('searchType', type);
      params.append('searchText', keyword);
    }

    // 검색 시 첫 페이지로 이동
    params.append('page', '0');

    window.location.href = `/admin/vote/list?${params.toString()}`;
  }
});

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