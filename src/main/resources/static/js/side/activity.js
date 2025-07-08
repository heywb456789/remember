/**
 * 활동 내역 업로드 페이지 JavaScript - 인증 및 페이징 적용 버전
 */

import {authFetch, FetchError, handleFetchError} from '../commonFetch.js';

// 페이징 관련 전역 변수
let currentPage = 0;
let totalPages = 0;
let pageSize = 10;
let totalItems = 0;

// 페이지 로드시 초기화
document.addEventListener('DOMContentLoaded', function () {
  // 페이지 진입 시 인증 확인
  checkAuthentication();

  // 업로드 버튼 클릭 이벤트
  document.getElementById('uploadButton')?.addEventListener('click',
      function () {
        // 모달 창 표시
        const modal = new bootstrap.Modal(
            document.getElementById('activityModal'));
        modal.show();
      });

  // 활동 저장 버튼 클릭 이벤트
  document.getElementById('saveActivity')?.addEventListener('click',
      createActivity);

  // 모달 창이 닫힐 때 입력 폼 초기화
  document.getElementById('activityModal')?.addEventListener('hidden.bs.modal',
      function () {
        document.getElementById('activityForm').reset();
      });

  // 페이지 이동 버튼 이벤트 등록
  document.addEventListener('click', function (e) {
    if (e.target.closest('.page-link')) {
      const pageBtn = e.target.closest('.page-link');

      if (pageBtn.classList.contains('disabled')) {
        return;
      }

      if (pageBtn.classList.contains('prev-page')) {
        // 이전 페이지로 이동
        changePage(currentPage - 1);
      } else if (pageBtn.classList.contains('next-page')) {
        // 다음 페이지로 이동
        changePage(currentPage + 1);
      } else {
        // 페이지 번호는 UI에서 1부터 시작하므로 내부 처리를 위해 -1 필요
        const pageNum = parseInt(pageBtn.textContent) - 1;
        changePage(pageNum);
      }
    }
  });
});

/**
 * 인증 상태 확인
 */
async function checkAuthentication() {
  // 로딩 표시
  showLoading(true);

  try {
    const token = localStorage.getItem('accessToken');

    // 인증 상태 확인 API 호출
    const res = await fetch('/api/auth/validate', {
      method: 'GET',
      headers: {'Authorization': `Bearer ${token}`}
    });

    // 204 No Content면 인증 완료 상태
    if (res.status === 204) {
      // 인증 성공 시 활동 내역 데이터 로드
      fetchActivities(currentPage);
    } else {
      // 401 등 인증 실패
      throw new Error('인증 실패');
    }
  } catch (error) {
    console.error('인증 확인 오류:', error);

    // 로딩 상태 숨김
    showLoading(false);

    // 인증 실패 시 로그인 페이지로 리디렉션
    alert('로그인이 필요합니다.');
    window.location.href = '/login/login.html';
  }
}

/**
 * 페이지 변경 처리
 * @param {number} page - 이동할 페이지 번호(0부터 시작)
 */
function changePage(page) {
  if (page < 0 || page >= totalPages) {
    return;
  }

  currentPage = page;
  fetchActivities(currentPage);

  // 페이지 상단으로 스크롤
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  });
}

/**
 * 활동 내역 데이터 가져오기 (Read)
 * @param {number} page - 페이지 번호(0부터 시작)
 */
async function fetchActivities(page = 0) {
  // 로딩 표시
  showLoading(true);

  try {
    // 서버에서 활동 내역 데이터 요청 (API도 페이지 번호를 0부터 시작)
    const response = await authFetch(
        `/api/members/activities?page=${page}&size=${pageSize}`);

    if (!response.ok) {
      if (response.status === 401) {
        // 인증 실패 시 로그인 페이지로 리디렉션
        alert('로그인이 필요합니다.');
        window.location.href = '/login/login.html';
        return;
      }

      throw new Error('서버 응답 오류: ' + response.status);
    }

    const data = await response.json();

    // 페이징 정보 업데이트 (API 응답의 currentPage는 1부터 시작하므로 변환 필요)
    currentPage = page; // 클라이언트는 0부터 시작하는 페이지 사용
    totalItems = data.response?.pagination?.totalElements || 0;
    totalPages = data.response?.pagination?.totalPages || 0;

    // 서버 응답의 페이지번호(1부터 시작)와 클라이언트의 페이지번호(0부터 시작) 일치 확인
    const serverCurrentPage = data.response?.pagination?.currentPage || 0;
    if (serverCurrentPage > 0 && page !== serverCurrentPage - 1) {
      console.warn(`페이지 번호 불일치: 클라이언트 ${page}, 서버 ${serverCurrentPage}`);
    }

    // 테이블 업데이트
    updateActivityTable(data.response?.data || []);
  } catch (error) {
    console.error('활동 내역 가져오기 오류:', error);

    if (error instanceof FetchError) {
      if (error.status === 401) {
        alert('로그인이 필요합니다.');
        window.location.href = '/login/login.html';
        return;
      }
    }

    showErrorMessage('활동 내역을 불러오는 중 오류가 발생했습니다.');

    // 오류 발생 시에도 페이지네이션 정보 초기화
    currentPage = page;
    totalItems = 0;
    totalPages = 0;

    // 빈 테이블 표시
    updateActivityTable([]);
  } finally {
    showLoading(false);
  }
}

/**
 * 페이지네이션 UI 업데이트
 */
function updatePagination() {
  const paginationEl = document.getElementById('pagination');
  const pageInfoEl = document.getElementById('pageInfo');

  if (!paginationEl) {
    return;
  }

  // 페이지 정보 업데이트
  if (pageInfoEl) {
    if (totalItems === 0) {
      pageInfoEl.textContent = '전체 0개';
    } else {
      // currentPage는 0부터 시작하므로 표시 시 +1 필요
      const startItem = totalItems === 0 ? 0 : currentPage * pageSize + 1;
      const endItem = Math.min(totalItems, (currentPage + 1) * pageSize);
      pageInfoEl.textContent = `전체 ${totalItems}개 중 ${startItem}-${endItem}`;
    }
  }

  // 데이터가 없거나 페이지가 1개 이하면 페이지네이션 숨김
  if (totalItems === 0 || totalPages <= 1) {
    paginationEl.style.display = 'none';
    return;
  }

  paginationEl.style.display = 'flex';

  // 최대 표시할 페이지 번호 개수
  const maxPageButtons = 5;

  // 표시할 시작 페이지와 끝 페이지 계산 (0부터 시작하는 페이지 번호 기준)
  let startPage = Math.max(0, currentPage - Math.floor(maxPageButtons / 2));
  let endPage = Math.min(totalPages - 1, startPage + maxPageButtons - 1);

  // 표시할 페이지 버튼이 maxPageButtons보다 적을 경우 시작 페이지 조정
  if (endPage - startPage + 1 < maxPageButtons) {
    startPage = Math.max(0, endPage - maxPageButtons + 1);
  }

  // 페이지네이션 HTML 생성 (화면에는 1부터 시작하는 페이지 번호 표시)
  // (클라이언트 내부에서는 0부터 시작하지만, UI에는 1부터 시작하는 번호 표시)
  let paginationHTML = `
    <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
      <a class="page-link prev-page" href="javascript:void(0)" aria-label="이전">
        <span aria-hidden="true">&laquo;</span>
      </a>
    </li>
  `;

  for (let i = startPage; i <= endPage; i++) {
    paginationHTML += `
      <li class="page-item ${i === currentPage ? 'active' : ''}">
        <a class="page-link" href="javascript:void(0)">${i + 1}</a>
      </li>
    `;
  }

  paginationHTML += `
    <li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
      <a class="page-link next-page" href="javascript:void(0)" aria-label="다음">
        <span aria-hidden="true">&raquo;</span>
      </a>
    </li>
  `;

  paginationEl.innerHTML = paginationHTML;
}

/**
 * 새 활동 데이터 생성 (Create)
 */
async function createActivity() {
  const titleInput = document.getElementById('activityTitle');
  const linkInput = document.getElementById('activityLink');

  const title = titleInput.value.trim();
  const link = linkInput.value.trim();

  // 입력 검증
  if (!title || !link) {
    showErrorMessage('제목과 링크를 모두 입력해주세요.');
    return;
  }

  // URL 검증
  if (!isValidUrl(link)) {
    showErrorMessage('유효한 URL을 입력해주세요.');
    return;
  }

  // 새 활동 데이터 객체 생성
  const newActivity = {
    title: title,
    shareLink: link // API 응답 형식에 맞게 수정
  };

  // 버튼 비활성화 및 로딩 표시
  const saveButton = document.getElementById('saveActivity');
  saveButton.disabled = true;
  saveButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 처리중...';

  try {
    // 서버에 데이터 전송
    const response = await authFetch('/api/members/activities', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(newActivity)
    });

    if (!response.ok) {
      if (response.status === 401) {
        // 인증 실패 시 로그인 페이지로 리디렉션
        alert('로그인이 필요합니다.');
        window.location.href = '/login/login.html';
        return;
      }
      throw new Error('서버 응답 오류: ' + response.status);
    }

    // 모달 창 닫기
    const modal = bootstrap.Modal.getInstance(
        document.getElementById('activityModal'));
    modal.hide();

    // 첫 페이지로 이동하여 테이블 데이터 새로고침
    currentPage = 0;
    fetchActivities(currentPage);

    // 완료 메시지 표시
    setTimeout(function () {
      const completionModal = new bootstrap.Modal(
          document.getElementById('completionModal'));
      completionModal.show();
    }, 300);
  } catch (error) {
    if (error instanceof FetchError) {
      console.error('[API Error]', error);
      showErrorMessage(error.statusMessage);
    } else {
      console.error('[Network/Error]', error);
      showErrorMessage('활동 등록중 오류가 발생했습니다.');
    }
  } finally {
    // 버튼 상태 복원
    saveButton.disabled = false;
    saveButton.innerHTML = '등록';
  }
}

/**
 * 활동 데이터 삭제 (Delete)
 * @param {number} id - 삭제할 활동 ID
 * @param {string} stage - 활동 상태
 */
async function deleteActivity(id, stage) {
  // '지급 검토중'(PENDING_REVIEW) 상태만 삭제 가능
  if (stage !== 'PENDING_REVIEW') {
    showErrorMessage('지급 검토중 상태의 기록만 삭제할 수 있습니다.');
    return;
  }

  // 삭제 확인
  if (!confirm('정말 이 활동 기록을 삭제하시겠습니까?')) {
    return;
  }

  try {
    // 서버에 삭제 요청
    const response = await authFetch(`/api/members/activities/${id}`, {
      method: 'DELETE'
    });

    if (!response.ok) {
      if (response.status === 401) {
        // 인증 실패 시 로그인 페이지로 리디렉션
        alert('로그인이 필요합니다.');
        window.location.href = '/login/login.html';
        return;
      }

      throw new Error('서버 응답 오류: ' + response.status);
    }

    // 현재 페이지 데이터 새로고침
    fetchActivities(0);

    // 삭제 완료 메시지
    showSuccessMessage('활동 기록이 삭제되었습니다.');
  } catch (error) {
    console.error('활동 삭제 오류:', error);

    if (error instanceof FetchError && error.status === 401) {
      alert('로그인이 필요합니다.');
      window.location.href = '/login/login.html';
      return;
    }

    showErrorMessage('활동 삭제 중 오류가 발생했습니다.');
  }
}

/**
 * 활동 내역 테이블 업데이트
 * @param {Array} activities - 활동 내역 데이터 배열
 */
function updateActivityTable(activities) {
  const tableBody = document.querySelector('#activityTable tbody');
  const emptyMessage = document.getElementById('emptyActivity');

  if (!tableBody || !emptyMessage) {
    return;
  }

  // 테이블 내용 초기화
  tableBody.innerHTML = '';

  if (!activities || activities.length === 0) {
    // 활동 내역이 없는 경우 메시지 표시
    emptyMessage.style.display = 'block';
    document.getElementById('activityTable').style.display = 'none';

    // 페이지네이션 숨김
    const paginationEl = document.getElementById('pagination');
    if (paginationEl) {
      paginationEl.style.display = 'none';
    }

    const pageInfoEl = document.getElementById('pageInfo');
    if (pageInfoEl) {
      pageInfoEl.textContent = '전체 0개';
    }
  } else {
    // 활동 내역이 있는 경우 테이블에 데이터 표시
    emptyMessage.style.display = 'none';
    document.getElementById('activityTable').style.display = 'table';

    activities.forEach(function (activity) {
      const row = document.createElement('tr');

      // 제목 셀
      const titleCell = document.createElement('td');
      titleCell.textContent = activity.title;
      row.appendChild(titleCell);

      // 링크 셀
      const linkCell = document.createElement('td');
      const link = document.createElement('a');
      link.href = activity.shareLink;
      link.textContent = activity.shareLink;
      link.target = '_blank';
      linkCell.appendChild(link);
      row.appendChild(linkCell);

      // 등록일 셀
      const dateCell = document.createElement('td');
      // 만약 createdAt이 없는 경우, 현재 날짜를 기본값으로 표시
      const createdDate = activity.createdAt || new Date().toISOString();
      dateCell.textContent = formatDate(createdDate);
      row.appendChild(dateCell);

      // 상태 셀
      const statusCell = document.createElement('td');

      // API 응답의 stage 값에 따라 상태 텍스트 설정
      let statusText = '지급 검토중';
      if (activity.stage === 'APPROVED') {
        statusText = '지급 완료';
        statusCell.classList.add('status-completed');
      } else if (activity.stage === 'REJECTED') {
        statusText = '지급 거절';
        statusCell.classList.add('status-rejected');
      } else if (activity.stage === 'PENDING_REVIEW') {
        statusText = '지급 검토중';
        statusCell.classList.add('status-pending');
      }

      statusCell.textContent = statusText;
      row.appendChild(statusCell);

      // 액션 셀 (삭제 버튼)
      const actionCell = document.createElement('td');

      // '지급 검토중'(PENDING_REVIEW) 상태일 때만 삭제 버튼 표시
      if (activity.stage === 'PENDING_REVIEW') {
        const deleteButton = document.createElement('button');
        deleteButton.classList.add('btn', 'btn-sm', 'btn-danger');
        deleteButton.innerHTML = '<i class="fas fa-trash"></i>';
        deleteButton.title = '삭제';
        deleteButton.addEventListener('click', function () {
          deleteActivity(activity.activityId, activity.stage);
        });
        actionCell.appendChild(deleteButton);
      }

      row.appendChild(actionCell);

      tableBody.appendChild(row);
    });
  }

  // 페이지 정보 업데이트 - 테이블 데이터 유무와 상관없이 갱신
  updatePagination();
}

/**
 * 날짜 포맷 변환
 * @param {string} dateString - 날짜 문자열
 * @returns {string} - 포맷된 날짜 문자열
 */
function formatDate(dateString) {
  if (!dateString) {
    return '';
  }

  const date = new Date(dateString);
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2,
      '0')}-${date.getDate().toString().padStart(2, '0')}`;
}

/**
 * URL 유효성 검사
 * @param {string} url - 검사할 URL
 * @returns {boolean} - URL 유효성 여부
 */
function isValidUrl(url) {
  try {
    new URL(url);
    return true;
  } catch (e) {
    return false;
  }
}

/**
 * 로딩 표시 함수
 * @param {boolean} isLoading - 로딩 상태
 */
function showLoading(isLoading) {
  const tableBody = document.querySelector('#activityTable tbody');
  const emptyMessage = document.getElementById('emptyActivity');

  if (!tableBody || !emptyMessage) {
    return;
  }

  if (isLoading) {
    emptyMessage.style.display = 'none';
    document.getElementById('activityTable').style.display = 'table';
    tableBody.innerHTML = '<tr><td colspan="5" class="text-center py-4"><i class="fas fa-spinner fa-spin me-2"></i>데이터를 불러오는 중...</td></tr>';
  }
}

/**
 * 오류 메시지 표시
 * @param {string} message - 오류 메시지
 */
function showErrorMessage(message) {
  alert(message);
}

/**
 * 성공 메시지 표시
 * @param {string} message - 성공 메시지
 */
function showSuccessMessage(message) {
  alert(message);
}