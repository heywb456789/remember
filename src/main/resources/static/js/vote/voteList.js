import { optionalAuthFetch } from '../commonFetch.js';

/**
 * 투표 목록을 API에서 불러와 동적으로 구성하는 함수
 * @param {number} page - 페이지 번호 (기본값: 0)
 * @param {number} size - 한 페이지당 게시글 수 (기본값: 10)
 * @param {string} keyword - 검색어 (기본값: 빈 문자열)
 */
async function loadVoteList(page = 0, size = 10, keyword = '') {
  // 투표 목록 컨테이너 확인
  const voteGrid = document.getElementById('voteGrid');
  if (!voteGrid) {
    return;
  }

  // 로딩 상태 표시
  voteGrid.innerHTML = `
  <div class="loading-state">
    <div class="loading-animation">
      <svg width="50" height="50" viewBox="0 0 50 50">
        <circle cx="25" cy="25" r="20" fill="none" stroke="#e0e0e0" stroke-width="4"></circle>
        <circle cx="25" cy="25" r="20" fill="none" stroke="#4263eb" stroke-width="4" stroke-dasharray="125" stroke-dashoffset="125" stroke-linecap="round">
          <animate attributeName="stroke-dashoffset" values="125;0" dur="1.5s" repeatCount="indefinite" />
        </circle>
      </svg>
    </div>
    <p class="loading-text">투표 목록을 불러오고 있습니다</p>
    <p class="loading-subtext">잠시만 기다려주세요...</p>
  </div>
`;
  const apiUrl = `/api/vote/posts?page=${page}&size=${size}` +
    (keyword ? `&keyword=${encodeURIComponent(keyword)}` : '');
  
  try {
    const res = await optionalAuthFetch(apiUrl);
    const data = await res.json();

    // 페이지네이션 정보 업데이트
    updatePagination(data.response.pagination);

    // 투표 목록이 없는 경우
    if (!data.response.data || data.response.data.length === 0) {
      voteGrid.innerHTML = `
        <div class="empty-state">
          <div class="empty-icon">
            <svg width="80" height="80" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M18 13H13V18H11V13H6V11H11V6H13V11H18V13Z" fill="#adb5bd"/>
              <circle cx="12" cy="12" r="9" stroke="#adb5bd" stroke-width="2" fill="none"/>
            </svg>
          </div>
          <p class="empty-title">투표가 없습니다</p>
        </div>
      `;
      
      // 비어있는 상태에서 투표 생성 버튼 이벤트 연결
      const createVoteEmptyBtn = document.getElementById('createVoteEmptyBtn');
      if (createVoteEmptyBtn) {
        createVoteEmptyBtn.addEventListener('click', handleCreateVoteClick);
      }
      
      return;
    }

    // 투표 목록 HTML 생성
    const voteListHTML = data.response.data.map(
      item => createVoteItemHTML(item)).join('');

    // 생성된 HTML을 컨테이너에 삽입
    voteGrid.innerHTML = voteListHTML;

    // 투표 아이템 클릭 이벤트 연결
    initVoteItemClick();

  } catch (error) {
    console.error('투표 목록을 불러오는 중 오류 발생:', error);

    // 오류 메시지 표시
    voteGrid.innerHTML = `
        <div class="error-state">
          <div class="error-icon">
            <svg width="60" height="60" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 4C7.58172 4 4 7.58172 4 12C4 16.4183 7.58172 20 12 20C16.4183 20 20 16.4183 20 12C20 7.58172 16.4183 4 12 4Z" stroke="#ff6b6b" stroke-width="2"/>
              <path d="M12 8V13" stroke="#ff6b6b" stroke-width="2" stroke-linecap="round"/>
              <circle cx="12" cy="16" r="1" fill="#ff6b6b"/>
            </svg>
          </div>
          <p class="error-title">투표 목록을 불러오는 데 실패했습니다</p>
          <p class="error-message">네트워크 연결을 확인하시거나 잠시 후 다시 시도해 주세요</p>
          <button class="retry-button">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4C7.58 4 4.01 7.58 4.01 12C4.01 16.42 7.58 20 12 20C15.73 20 18.84 17.45 19.73 14H17.65C16.83 16.33 14.61 18 12 18C8.69 18 6 15.31 6 12C6 8.69 8.69 6 12 6C13.66 6 15.14 6.69 16.22 7.78L13 11H20V4L17.65 6.35Z" fill="#4263eb"/>
            </svg>
            다시 시도하기
          </button>
        </div>
      `;

    // 다시 시도 버튼에 이벤트 연결
    const retryButton = voteGrid.querySelector('.retry-button');
    if (retryButton) {
      retryButton.addEventListener('click',
        () => loadVoteList(page, size, keyword));
    }
  }
}

/**
 * 투표 아이템 HTML 생성 함수
 * @param {Object} item - 투표 데이터
 * @returns {string} 투표 HTML
 */
function createVoteItemHTML(item) {
  // 배지 HTML 생성 함수
  const getBadgeHTML = () => {
    let badges = '';
    if (item.new) {
      badges += `<span class="badge badge-new">N</span>`;
    }
    return badges;
  };

  // 투표 옵션 HTML 생성
  const getOptionsHTML = () => {
    if (!item.voteOptions || item.voteOptions.length < 2) {
      return '<div class="vote-options"><div class="vote-option left">찬성</div><span class="vs-text">VS</span><div class="vote-option right">반대</div></div>';
    }

    return `
      <div class="vote-options">
        <div class="vote-option left">${item.voteOptions[0].optionName}</div>
        <span class="vs-text">VS</span>
        <div class="vote-option right">${item.voteOptions[1].optionName}</div>
      </div>
    `;
  };

  // 투표 HTML 생성
  return `
    <div class="vote-item" data-id="${item.votePostId}">
      <div class="vote-content">
        <p>${item.question}</p>
        ${getOptionsHTML()}
        <div class="vote-badge-container">
          ${getBadgeHTML()}
        </div>
      </div>
    </div>
  `;
}

/**
 * 페이지네이션 업데이트 함수
 * @param {Object} pagination - 페이지네이션 데이터
 */
function updatePagination(pagination) {
  const paginationContainer = document.querySelector('.pagination');
  if (!paginationContainer) {
    return;
  }

  // 현재 페이지
  const currentPage = pagination.currentPage;

  // 전체 페이지 수
  const totalPages = pagination.totalPages;

  // 표시할 페이지 버튼 개수
  const displayPageCount = 5;

  // 시작 페이지와 끝 페이지 계산
  let startPage = Math.max(1, currentPage - Math.floor(displayPageCount / 2));
  let endPage = Math.min(totalPages, startPage + displayPageCount - 1);

  // 시작 페이지 재조정
  if (endPage - startPage < displayPageCount - 1) {
    startPage = Math.max(1, endPage - displayPageCount + 1);
  }

  // 페이지네이션 HTML 생성
  let paginationHTML = `
    <span class="pagination-item prev ${currentPage === 1 ? 'disabled'
      : ''}" data-page="${currentPage - 1}">
      <i class="fas fa-chevron-left"></i>
    </span>
  `;

  // 첫 페이지가 1이 아닌 경우, 1 페이지 버튼 추가
  if (startPage > 1) {
    paginationHTML += `
      <span class="pagination-item" data-page="1">1</span>
      ${startPage > 2 ? '<span class="pagination-item dots">...</span>' : ''}
    `;
  }

  // 페이지 버튼 생성
  for (let i = startPage; i <= endPage; i++) {
    paginationHTML += `
      <span class="pagination-item ${i === currentPage ? 'active'
        : ''}" data-page="${i}">${i}</span>
    `;
  }

  // 마지막 페이지가 totalPages가 아닌 경우, 마지막 페이지 버튼 추가
  if (endPage < totalPages) {
    paginationHTML += `
      ${endPage < totalPages - 1
        ? '<span class="pagination-item dots">...</span>' : ''}
      <span class="pagination-item" data-page="${totalPages}">${totalPages}</span>
    `;
  }

  // 다음 페이지 버튼
  paginationHTML += `
    <span class="pagination-item next ${currentPage === totalPages ? 'disabled'
      : ''}" data-page="${currentPage + 1}">
      <i class="fas fa-chevron-right"></i>
    </span>
  `;

  // HTML 삽입
  paginationContainer.innerHTML = paginationHTML;

  // 페이지네이션 클릭 이벤트 추가
  initPaginationClick();
}

/**
 * 페이지네이션 클릭 이벤트 초기화
 */
function initPaginationClick() {
  const paginationItems = document.querySelectorAll(
    '.pagination-item:not(.dots):not(.disabled)');

  paginationItems.forEach(item => {
    item.addEventListener('click', function () {
      const page = parseInt(this.getAttribute('data-page'));
      if (isNaN(page)) {
        return;
      }

      // 현재 검색어 유지
      const searchInput = document.querySelector('.search-input');
      const keyword = searchInput ? searchInput.value.trim() : '';

      // 투표 목록 다시 로드
      loadVoteList(page - 1, 10, keyword);
    });
  });
}

/**
 * 투표 아이템 클릭 이벤트 초기화
 */
function initVoteItemClick() {
  const voteItems = document.querySelectorAll('.vote-item');

  voteItems.forEach(item => {
    item.addEventListener('click', function () {
      const voteId = this.getAttribute('data-id');
      if (!voteId) {
        return;
      }

      // 상세 페이지로 이동
      window.location.href = `/vote/voteDetail.html?id=${voteId}`;
    });
  });
}

/**
 * 투표 생성 버튼 클릭 핸들러
 */
function handleCreateVoteClick() {
  // 로그인 상태 확인
  const isLoggedIn = checkLoginStatus();

  if (isLoggedIn) {
    // 투표 생성 페이지로 이동
    window.location.href = '/vote/voteCreate.html';
  } else {
    // 로그인 필요 알림
    alert('투표 생성을 위해 로그인이 필요합니다.');
    // 로그인 페이지로 이동 또는 로그인 모달 표시
    // window.location.href = '/login.html';
  }
}

/**
 * 로그인 상태 확인 함수
 * @returns {boolean} 로그인 여부
 */
function checkLoginStatus() {
  // localStorage에서 토큰 또는 사용자 정보 확인
  const token = localStorage.getItem('accessToken');
  return !!token;
}

/**
 * 검색 이벤트 초기화
 */
function initSearch() {
  const searchInput = document.querySelector('.search-input');
  const searchForm = document.querySelector('.search-form');

  if (searchForm) {
    searchForm.addEventListener('submit', function (e) {
      e.preventDefault();
      const keyword = searchInput.value.trim();
      loadVoteList(0, 10, keyword);
    });
  }

  if (searchInput) {
    // 검색어 입력 후 엔터 키 이벤트
    searchInput.addEventListener('keypress', function (e) {
      if (e.key === 'Enter') {
        e.preventDefault();
        const keyword = this.value.trim();
        loadVoteList(0, 10, keyword);
      }
    });
  }
}

// 페이지 로드 시 투표 목록 초기화
document.addEventListener('DOMContentLoaded', function () {
  // 투표 목록 로드
  loadVoteList();

  // 검색 이벤트 초기화
  initSearch();

  // 투표 생성 버튼 이벤트 초기화
  const createVoteBtn = document.getElementById('createVoteBtn');
  if (createVoteBtn) {
    createVoteBtn.addEventListener('click', handleCreateVoteClick);
  }
});