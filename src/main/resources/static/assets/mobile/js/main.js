// main.js - 토마토리멤버 메인 페이지 (수정된 버전)

import { authFetch, checkLoginStatus, handleFetchError } from './commonFetch.js';
import { showToast, showConfirm, showLoading } from './common.js';

// 메인 페이지 상태 관리
let mainPageState = {
  isLoading: false,
  isInitialized: false,
  memorialItems: [],
  isLoggedIn: false,
  currentUser: null,
  retryCount: 0,
  maxRetries: 3,
  refreshInterval: null
};

// 관계별 이모지 매핑
const RELATIONSHIP_EMOJIS = {
  '부': '👨',
  '모': '👩',
  '배우자부': '👨',
  '배우자모': '👩',
  '조부': '👴',
  '조모': '👵',
  '증조부': '👴',
  '증조모': '👵',
  '배우자': '💑',
  '자': '👶',
  '자부': '👰',
  '사위': '🤵',
  '형제/자매': '👫',
  '손': '👶',
  '증손': '👶',
  '본인': '😊',
  '동거인': '🏠',
  '기타': '👤'
};

/**
 * 메인 페이지 초기화
 */
function initializeMainPage() {
  console.log('메인 페이지 초기화 시작');

  if (mainPageState.isInitialized) {
    console.warn('메인 페이지가 이미 초기화되었습니다.');
    return;
  }

  try {
    // 1. 서버 데이터 로드
    loadServerData();

    // 2. 이벤트 바인딩
    bindAllEvents();

    // 3. 로그인 상태 UI 업데이트
    updateLoginUI();

    // 4. 로그인한 경우 추가 초기화
    if (mainPageState.isLoggedIn) {
      initializeLoggedInFeatures();
    }

    // 5. 초기화 완료 플래그 설정
    mainPageState.isInitialized = true;
    console.log('메인 페이지 초기화 완료');

  } catch (error) {
    console.error('메인 페이지 초기화 실패:', error);
    showToast('페이지 초기화 중 오류가 발생했습니다.', 'error');
  }
}

/**
 * 서버 데이터 로드
 */
function loadServerData() {
  console.log('서버 데이터 로드');

  if (window.serverData) {
    mainPageState.isLoggedIn = window.serverData.isLoggedIn || false;
    mainPageState.memorialItems = window.serverData.memorialList || [];
    mainPageState.currentUser = window.serverData.currentUser || null;

    console.log('서버 데이터 로드 완료:', {
      isLoggedIn: mainPageState.isLoggedIn,
      memorialCount: mainPageState.memorialItems.length,
      currentUser: mainPageState.currentUser?.name || 'None'
    });
  } else {
    console.warn('서버 데이터가 없습니다.');
  }
}

/**
 * 모든 이벤트 바인딩
 */
function bindAllEvents() {
  console.log('모든 이벤트 바인딩 시작');

  // 1. 새 메모리얼 등록 버튼들
  bindCreateMemorialButtons();

  // 2. 영상통화 버튼
  bindVideoCallButton();

  // 3. 무료체험 버튼
  bindFreeTrialButton();

  // 4. 메모리얼 아이템들
  bindMemorialItems();

  // 5. 기타 버튼들
  bindOtherButtons();

  console.log('모든 이벤트 바인딩 완료');
}

/**
 * 메모리얼 생성 버튼 바인딩
 */
function bindCreateMemorialButtons() {
  console.log('메모리얼 생성 버튼 바인딩');

  const createButtons = document.querySelectorAll(`
    .new-memorial-btn,
    .add-memorial-btn,
    .empty-state-create-btn
  `);

  createButtons.forEach(btn => {
    btn.removeEventListener('click', handleCreateMemorialClick);
    btn.addEventListener('click', handleCreateMemorialClick);
    console.log('생성 버튼 바인딩:', btn.className);
  });

  console.log('메모리얼 생성 버튼 바인딩 완료:', createButtons.length);
}

/**
 * 영상통화 버튼 바인딩
 */
function bindVideoCallButton() {
  console.log('영상통화 버튼 바인딩');

  const videoCallBtn = document.querySelector('.video-call-btn');
  if (videoCallBtn) {
    videoCallBtn.removeEventListener('click', handleVideoCallClick);
    videoCallBtn.addEventListener('click', handleVideoCallClick);
    console.log('영상통화 버튼 바인딩 완료');
  }
}

/**
 * 무료체험 버튼 바인딩
 */
function bindFreeTrialButton() {
  console.log('무료체험 버튼 바인딩');

  const freeTrialBtn = document.querySelector('.free-trial-btn');
  if (freeTrialBtn) {
    freeTrialBtn.removeEventListener('click', handleFreeTrialClick);
    freeTrialBtn.addEventListener('click', handleFreeTrialClick);
    console.log('무료체험 버튼 바인딩 완료');
  }
}

/**
 * 메모리얼 아이템 바인딩
 */
function bindMemorialItems() {
  console.log('메모리얼 아이템 바인딩');

  const memorialItems = document.querySelectorAll('.memorial-item');
  memorialItems.forEach(item => {
    item.removeEventListener('click', handleMemorialItemClick);
    item.addEventListener('click', handleMemorialItemClick);
  });

  console.log('메모리얼 아이템 바인딩 완료:', memorialItems.length);
}

/**
 * 기타 버튼들 바인딩
 */
function bindOtherButtons() {
  console.log('기타 버튼 바인딩');

  // 새로고침 버튼
  const refreshBtn = document.querySelector('.refresh-btn');
  if (refreshBtn) {
    refreshBtn.removeEventListener('click', handleRefreshClick);
    refreshBtn.addEventListener('click', handleRefreshClick);
  }

  // 에러 상태 재시도 버튼
  const retryBtn = document.querySelector('#errorState .btn');
  if (retryBtn) {
    retryBtn.removeEventListener('click', handleRetryClick);
    retryBtn.addEventListener('click', handleRetryClick);
  }
}

/**
 * 로그인 UI 업데이트
 */
function updateLoginUI() {
  console.log('로그인 UI 업데이트');

  const loggedInElements = document.querySelectorAll('.logged-in-only');
  const loggedOutElements = document.querySelectorAll('.logged-out-only');

  if (mainPageState.isLoggedIn) {
    loggedInElements.forEach(el => {
      el.style.display = 'block';
      el.classList.remove('d-none');
    });
    loggedOutElements.forEach(el => {
      el.style.display = 'none';
      el.classList.add('d-none');
    });
  } else {
    loggedInElements.forEach(el => {
      el.style.display = 'none';
      el.classList.add('d-none');
    });
    loggedOutElements.forEach(el => {
      el.style.display = 'block';
      el.classList.remove('d-none');
    });
  }

  console.log('로그인 UI 업데이트 완료');
}

/**
 * 로그인한 사용자 기능 초기화
 */
async function initializeLoggedInFeatures() {
  console.log('로그인한 사용자 기능 초기화');

  try {
    // 서버에서 이미 데이터를 받았으므로 바로 렌더링
    if (mainPageState.memorialItems.length > 0) {
      renderMemorialList(mainPageState.memorialItems);
    }

    // 주기적 새로고침은 비활성화 (서버사이드 렌더링 사용)
    // setupPeriodicRefresh();

  } catch (error) {
    console.error('로그인한 사용자 기능 초기화 실패:', error);
  }
}

/**
 * 이벤트 핸들러들
 */

// 메모리얼 생성 클릭 핸들러
async function handleCreateMemorialClick(e) {
  e.preventDefault();
  e.stopPropagation();

  console.log('메모리얼 생성 클릭');

  if (!mainPageState.isLoggedIn) {
    showLoginModal();
    return;
  }

  // 메모리얼 생성 페이지로 이동
  window.location.href = '/mobile/memorial/create';
}

// 영상통화 클릭 핸들러
async function handleVideoCallClick(e) {
  e.preventDefault();
  console.log('영상통화 클릭');

  if (mainPageState.memorialItems.length === 0) {
    showToast('먼저 메모리얼을 등록해주세요.', 'warning');
    return;
  }

  // 메모리얼이 1개인 경우 바로 영상통화 시작
  if (mainPageState.memorialItems.length === 1) {
    const memorial = mainPageState.memorialItems[0];
    startVideoCall(memorial.memorialId);
    return;
  }

  // 메모리얼이 여러개인 경우 선택 모달 표시
  showMemorialSelectionModal();
}

// 무료체험 클릭 핸들러
function handleFreeTrialClick(e) {
  e.preventDefault();
  console.log('무료체험 클릭');

  if (mainPageState.isLoggedIn) {
    showToast('이미 로그인된 상태입니다.', 'info');
    return;
  }

  // 회원가입 페이지로 이동
  window.location.href = '/mobile/register?trial=true';
}

// 메모리얼 아이템 클릭 핸들러
function handleMemorialItemClick(e) {
  const memorialId = e.currentTarget.dataset.memorialId;
  console.log('메모리얼 아이템 클릭:', memorialId);

  if (memorialId) {
    window.location.href = `/mobile/memorial/${memorialId}`;
  }
}

// 새로고침 클릭 핸들러
async function handleRefreshClick(e) {
  e.preventDefault();
  console.log('새로고침 클릭');

  // 서버사이드 렌더링 사용하므로 페이지 새로고침
  window.location.reload();
}

// 재시도 클릭 핸들러
async function handleRetryClick(e) {
  e.preventDefault();
  console.log('재시도 클릭');

  // 서버사이드 렌더링 사용하므로 페이지 새로고침
  window.location.reload();
}

/**
 * 메모리얼 목록 로드
 */
async function loadMemorialList() {
  console.log('메모리얼 목록 로드');

  if (mainPageState.isLoading || !mainPageState.isLoggedIn) {
    return;
  }

  try {
    mainPageState.isLoading = true;
    showLoadingState();

    const response = await authFetch('/api/memorial/my?size=5');
    const data = await response.json();

    if (data.status?.code === 'OK_0000') {
      mainPageState.memorialItems = data.response?.data || [];

      if (mainPageState.memorialItems.length > 0) {
        renderMemorialList(mainPageState.memorialItems);
      } else {
        showEmptyState();
      }
    } else {
      throw new Error(data.status?.message || '메모리얼 목록 로드 실패');
    }

  } catch (error) {
    console.error('메모리얼 목록 로드 실패:', error);
    showErrorState(error.message);
  } finally {
    mainPageState.isLoading = false;
    hideLoadingState();
  }
}

/**
 * 메모리얼 목록 렌더링 (서버 데이터 기반)
 */
function renderMemorialList(memorials) {
  console.log('메모리얼 목록 렌더링:', memorials.length);

  const container = document.getElementById('memorialList');
  if (!container) {
    console.log('메모리얼 리스트 컨테이너를 찾을 수 없음 - 서버에서 이미 렌더링됨');
    return;
  }

  // 서버에서 이미 렌더링된 경우 추가 처리만 수행
  bindMemorialItems();
  console.log('메모리얼 목록 이벤트 바인딩 완료');
}

/**
 * 메모리얼 아이템 생성
 */
function createMemorialItem(memorial) {
  const item = document.createElement('div');
  item.className = 'memorial-item';
  item.dataset.memorialId = memorial.memorialId;

  // 아바타 HTML 생성
  const avatarHtml = createAvatarHtml(memorial);

  item.innerHTML = `
    ${avatarHtml}
    <div class="memorial-info">
      <div class="memorial-name">${memorial.name}</div>
      <div class="memorial-relationship">${memorial.relationshipDescription || '관계 없음'}</div>
    </div>
    <div class="memorial-arrow">
      <i class="fas fa-chevron-right"></i>
    </div>
  `;

  return item;
}

/**
 * 아바타 HTML 생성 (이미지 또는 이모지)
 */
function createAvatarHtml(memorial) {
  if (memorial.mainProfileImageUrl) {
    return `
      <div class="memorial-avatar">
        <img src="${memorial.mainProfileImageUrl}" alt="${memorial.name}" class="avatar-img">
      </div>
    `;
  } else {
    const emoji = RELATIONSHIP_EMOJIS[memorial.relationshipDescription] || '👤';
    return `
      <div class="memorial-avatar">
        <span class="memorial-emoji">
          <span class="emoji">${emoji}</span>
        </span>
      </div>
    `;
  }
}

/**
 * 영상통화 시작
 */
function startVideoCall(memorialId) {
  console.log('영상통화 시작:', memorialId);
  showToast('영상통화 기능 준비 중입니다.', 'info');
  // TODO: 영상통화 로직 구현
}

/**
 * 메모리얼 선택 모달 표시
 */
function showMemorialSelectionModal() {
  console.log('메모리얼 선택 모달 표시');
  showToast('영상통화할 메모리얼을 선택해주세요.', 'info');
  // TODO: 메모리얼 선택 모달 구현
}

/**
 * 유틸리티 함수들
 */
function showLoadingState() {
  const skeleton = document.getElementById('loadingSkeleton');
  if (skeleton) skeleton.style.display = 'block';
}

function hideLoadingState() {
  const skeleton = document.getElementById('loadingSkeleton');
  if (skeleton) skeleton.style.display = 'none';
}

function showEmptyState() {
  const emptyState = document.querySelector('.empty-state');
  if (emptyState) emptyState.style.display = 'block';
}

function hideEmptyState() {
  const emptyState = document.querySelector('.empty-state');
  if (emptyState) emptyState.style.display = 'none';
}

function showErrorState(message) {
  const errorState = document.getElementById('errorState');
  const errorMessage = document.getElementById('errorMessage');

  if (errorState) errorState.style.display = 'block';
  if (errorMessage) errorMessage.textContent = message;

  hideLoadingState();
  hideEmptyState();
}

function hideErrorState() {
  const errorState = document.getElementById('errorState');
  if (errorState) errorState.style.display = 'none';
}

function showLoginModal() {
  const confirmLogin = confirm('이 기능을 사용하려면 로그인이 필요합니다.\n\n로그인 페이지로 이동하시겠습니까?');
  if (confirmLogin) {
    window.location.href = '/mobile/login';
  }
}

function setupPeriodicRefresh() {
  // 기존 인터벌 제거
  if (mainPageState.refreshInterval) {
    clearInterval(mainPageState.refreshInterval);
  }

  // 5분마다 새로고침
  mainPageState.refreshInterval = setInterval(() => {
    if (mainPageState.isLoggedIn && !document.hidden) {
      console.log('주기적 새로고침');
      loadMemorialList();
    }
  }, 5 * 60 * 1000);
}

/**
 * 정리 함수
 */
function destroyMainPage() {
  console.log('메인 페이지 정리');

  if (mainPageState.refreshInterval) {
    clearInterval(mainPageState.refreshInterval);
    mainPageState.refreshInterval = null;
  }

  mainPageState.isInitialized = false;
}

/**
 * 전역 함수들 (하위 호환성)
 */
window.mainPageManager = {
  initialize: initializeMainPage,
  destroy: destroyMainPage,
  getState: () => mainPageState
};

// 전역 함수들 (HTML에서 호출 가능)
window.handleCreateMemorial = handleCreateMemorialClick;
window.showVideoCall = handleVideoCallClick;
window.showLoginModal = showLoginModal;

/**
 * 자동 초기화
 */
console.log('수정된 main.js 로드 완료');

// DOM이 준비되면 즉시 초기화
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initializeMainPage);
} else {
  setTimeout(initializeMainPage, 100);
}

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', destroyMainPage);

// 모듈 익스포트
export {
  initializeMainPage,
  destroyMainPage,
  handleCreateMemorialClick,
  handleVideoCallClick
};