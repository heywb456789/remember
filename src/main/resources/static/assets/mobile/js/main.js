// main.js - 토마토리멤버 메인 페이지 (개선된 버전)

import { authFetch, checkLoginStatus, handleFetchError } from './commonFetch.js';
import { showToast, showConfirm, showLoading } from './common.js';

// 메인 페이지 상태 관리
let mainPageState = {
  isLoading: false,
  isInitialized: false,
  memorialCards: [],
  isLoggedIn: false,
  currentUser: null,
  retryCount: 0,
  maxRetries: 3,
  refreshInterval: null
};

/**
 * 메인 페이지 초기화 - 단순화된 버전
 */
function initializeMainPage() {
  console.log('🚀 메인 페이지 초기화 시작');

  if (mainPageState.isInitialized) {
    console.warn('⚠️ 메인 페이지가 이미 초기화되었습니다.');
    return;
  }

  try {
    // 1. 서버 데이터 로드
    loadServerData();

    // 2. 이벤트 바인딩 (가장 중요!)
    bindAllEvents();

    // 3. 로그인 상태 UI 업데이트
    updateLoginUI();

    // 4. 로그인한 경우 추가 초기화
    if (mainPageState.isLoggedIn) {
      initializeLoggedInFeatures();
    }

    // 5. 초기화 완료 플래그 설정
    mainPageState.isInitialized = true;
    console.log('✅ 메인 페이지 초기화 완료');

  } catch (error) {
    console.error('❌ 메인 페이지 초기화 실패:', error);
    showToast('페이지 초기화 중 오류가 발생했습니다.', 'error');
  }
}

/**
 * 서버 데이터 로드
 */
function loadServerData() {
  console.log('📊 서버 데이터 로드');

  if (window.serverData) {
    mainPageState.isLoggedIn = window.serverData.isLoggedIn || false;
    mainPageState.memorialCards = window.serverData.memorialList || [];
    mainPageState.currentUser = window.serverData.currentUser || null;

    console.log('📊 서버 데이터 로드 완료:', {
      isLoggedIn: mainPageState.isLoggedIn,
      memorialCount: mainPageState.memorialCards.length,
      currentUser: mainPageState.currentUser?.name || 'None'
    });
  } else {
    console.warn('⚠️ 서버 데이터가 없습니다.');
  }
}

/**
 * 모든 이벤트 바인딩 - 핵심 함수
 */
function bindAllEvents() {
  console.log('🔗 모든 이벤트 바인딩 시작');

  // 1. 새 메모리얼 등록 버튼들
  bindCreateMemorialButtons();

  // 2. 액션 버튼들
  bindActionButtons();

  // 3. 무료체험 버튼
  bindFreeTrialButton();

  // 4. 메모리얼 카드들
  bindMemorialCards();

  // 5. 기타 버튼들
  bindOtherButtons();

  console.log('✅ 모든 이벤트 바인딩 완료');
}

/**
 * 메모리얼 생성 버튼 바인딩
 */
function bindCreateMemorialButtons() {
  console.log('📝 메모리얼 생성 버튼 바인딩');

  // 선택자로 모든 생성 버튼 찾기
  const createButtons = document.querySelectorAll(`
    .new-memorial-btn,
    .add-memorial-btn,
    .empty-state-create-btn
  `);

  createButtons.forEach(btn => {
    // 기존 이벤트 제거 (중복 방지)
    btn.removeEventListener('click', handleCreateMemorialClick);

    // 새 이벤트 바인딩
    btn.addEventListener('click', handleCreateMemorialClick);

    console.log('📝 생성 버튼 바인딩:', btn.className);
  });

  console.log('✅ 메모리얼 생성 버튼 바인딩 완료:', createButtons.length);
}

/**
 * 액션 버튼들 바인딩
 */
function bindActionButtons() {
  console.log('🎬 액션 버튼 바인딩');

  // 영상통화 버튼
  const videoCallBtn = document.querySelector('.btn-video');
  if (videoCallBtn) {
    videoCallBtn.removeEventListener('click', handleVideoCallClick);
    videoCallBtn.addEventListener('click', handleVideoCallClick);
    console.log('📹 영상통화 버튼 바인딩 완료');
  }

  // 선물하기 버튼
  const giftBtn = document.querySelector('.btn-gift');
  if (giftBtn) {
    giftBtn.removeEventListener('click', handleGiftClick);
    giftBtn.addEventListener('click', handleGiftClick);
    console.log('🎁 선물하기 버튼 바인딩 완료');
  }
}

/**
 * 무료체험 버튼 바인딩
 */
function bindFreeTrialButton() {
  console.log('🎯 무료체험 버튼 바인딩');

  const freeTrialBtn = document.querySelector('.free-trial-btn');
  if (freeTrialBtn) {
    freeTrialBtn.removeEventListener('click', handleFreeTrialClick);
    freeTrialBtn.addEventListener('click', handleFreeTrialClick);
    console.log('🎁 무료체험 버튼 바인딩 완료');
  }
}

/**
 * 메모리얼 카드 바인딩
 */
function bindMemorialCards() {
  console.log('🎴 메모리얼 카드 바인딩');

  const memorialCards = document.querySelectorAll('.memorial-card');
  memorialCards.forEach(card => {
    card.removeEventListener('click', handleMemorialCardClick);
    card.addEventListener('click', handleMemorialCardClick);
  });

  console.log('✅ 메모리얼 카드 바인딩 완료:', memorialCards.length);
}

/**
 * 기타 버튼들 바인딩
 */
function bindOtherButtons() {
  console.log('🔘 기타 버튼 바인딩');

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
  console.log('🎨 로그인 UI 업데이트');

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

  console.log('✅ 로그인 UI 업데이트 완료');
}

/**
 * 로그인한 사용자 기능 초기화
 */
async function initializeLoggedInFeatures() {
  console.log('👤 로그인한 사용자 기능 초기화');

  try {
    // 메모리얼 목록이 비어있으면 서버에서 로드
    if (mainPageState.memorialCards.length === 0) {
      await loadMemorialList();
    }

    // 주기적 새로고침 설정
    setupPeriodicRefresh();

  } catch (error) {
    console.error('❌ 로그인한 사용자 기능 초기화 실패:', error);
  }
}

/**
 * 이벤트 핸들러들
 */

// 메모리얼 생성 클릭 핸들러
async function handleCreateMemorialClick(e) {
  e.preventDefault();
  e.stopPropagation();

  console.log('📝 메모리얼 생성 클릭');

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
  console.log('📹 영상통화 클릭');

  if (mainPageState.memorialCards.length === 0) {
    showToast('먼저 메모리얼을 등록해주세요.', 'warning');
    return;
  }

  showToast('영상통화 기능 준비 중입니다.', 'info');
}

// 선물하기 클릭 핸들러
function handleGiftClick(e) {
  e.preventDefault();
  console.log('🎁 선물하기 클릭');

  showToast('선물하기 기능 준비 중입니다.', 'info');
}

// 무료체험 클릭 핸들러
function handleFreeTrialClick(e) {
  e.preventDefault();
  console.log('🎯 무료체험 클릭');

  if (mainPageState.isLoggedIn) {
    showToast('이미 로그인된 상태입니다.', 'info');
    return;
  }

  // 회원가입 페이지로 이동
  window.location.href = '/mobile/register?trial=true';
}

// 메모리얼 카드 클릭 핸들러
function handleMemorialCardClick(e) {
  const memorialId = e.currentTarget.dataset.memorialId;
  console.log('🎴 메모리얼 카드 클릭:', memorialId);

  if (memorialId) {
    window.location.href = `/mobile/memorial/${memorialId}`;
  }
}

// 새로고침 클릭 핸들러
async function handleRefreshClick(e) {
  e.preventDefault();
  console.log('🔄 새로고침 클릭');

  if (mainPageState.isLoggedIn) {
    await loadMemorialList();
    showToast('목록이 새로고침되었습니다.', 'success');
  } else {
    window.location.reload();
  }
}

// 재시도 클릭 핸들러
async function handleRetryClick(e) {
  e.preventDefault();
  console.log('🔄 재시도 클릭');

  hideErrorState();
  await loadMemorialList();
}

/**
 * 메모리얼 목록 로드
 */
async function loadMemorialList() {
  console.log('📋 메모리얼 목록 로드');

  if (mainPageState.isLoading || !mainPageState.isLoggedIn) {
    return;
  }

  try {
    mainPageState.isLoading = true;
    showLoadingState();

    const response = await authFetch('/api/memorial/my');
    const data = await response.json();

    if (data.status?.code === 'OK_0000') {
      mainPageState.memorialCards = data.response || [];

      if (mainPageState.memorialCards.length > 0) {
        renderMemorialList(mainPageState.memorialCards);
      } else {
        showEmptyState();
      }
    } else {
      throw new Error(data.status?.message || '메모리얼 목록 로드 실패');
    }

  } catch (error) {
    console.error('❌ 메모리얼 목록 로드 실패:', error);
    showErrorState(error.message);
  } finally {
    mainPageState.isLoading = false;
    hideLoadingState();
  }
}

/**
 * 메모리얼 목록 렌더링
 */
function renderMemorialList(memorials) {
  console.log('🎨 메모리얼 목록 렌더링:', memorials.length);

  const container = document.getElementById('memorialList');
  if (!container) return;

  hideEmptyState();
  hideErrorState();

  // 기존 내용 제거
  container.innerHTML = '';

  // 메모리얼 카드 생성
  memorials.forEach(memorial => {
    const card = createMemorialCard(memorial);
    container.appendChild(card);
  });

  // 새로 생성된 카드들에 이벤트 바인딩
  bindMemorialCards();

  console.log('✅ 메모리얼 목록 렌더링 완료');
}

/**
 * 메모리얼 카드 생성
 */
function createMemorialCard(memorial) {
  const card = document.createElement('div');
  card.className = 'memorial-card';
  card.dataset.memorialId = memorial.id;

  const profileImage = memorial.profileImageUrl || '/assets/mobile/images/default-avatar.png';
  const onlineStatus = memorial.isOnline ? 'online' : 'offline';
  const lastVisit = memorial.lastVisitDate ?
    new Date(memorial.lastVisitDate).toLocaleDateString() :
    '방문 기록 없음';

  card.innerHTML = `
    <div class="memorial-header">
      <div class="memorial-avatar">
        <img src="${profileImage}" alt="${memorial.name}" class="avatar-img">
        <div class="memorial-status ${onlineStatus}"></div>
      </div>
      <div class="memorial-info">
        <div class="memorial-name">${memorial.name}</div>
        <div class="memorial-relationship">${memorial.relationship || '관계 없음'}</div>
        <div class="memorial-last-visit">마지막 방문: ${lastVisit}</div>
      </div>
    </div>
  `;

  return card;
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
      console.log('🔄 주기적 새로고침');
      loadMemorialList();
    }
  }, 5 * 60 * 1000);
}

/**
 * 정리 함수
 */
function destroyMainPage() {
  console.log('🗑️ 메인 페이지 정리');

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
  loadMemorialList,
  destroy: destroyMainPage,
  getState: () => mainPageState
};

// 전역 함수들 (HTML에서 호출 가능)
window.handleCreateMemorial = handleCreateMemorialClick;
window.showVideoCall = handleVideoCallClick;
window.showGiftInfo = handleGiftClick;
window.showLoginModal = showLoginModal;

/**
 * 자동 초기화
 */
console.log('🌟 개선된 main.js 로드 완료');

// DOM이 준비되면 즉시 초기화
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initializeMainPage);
} else {
  // DOM이 이미 로드된 경우 즉시 초기화
  setTimeout(initializeMainPage, 100);
}

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', destroyMainPage);

// 모듈 익스포트
export {
  initializeMainPage,
  loadMemorialList,
  destroyMainPage,
  handleCreateMemorialClick,
  handleVideoCallClick,
  handleGiftClick
};