// main.js - 토마토리멤버 메인 페이지 (Function 기반 + commonFetch.js 활용)

import { authFetch, optionalAuthFetch, checkLoginStatus, getUserId, handleFetchError } from './commonFetch.js';
import { showToast, showConfirm, showLoading, hideLoading, debounce, timeAgo, formatNumber } from './common.js';

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
 * 메인 페이지 초기화
 */
async function initializeMainPage() {
  console.log('🚀 메인 페이지 초기화 시작');

  if (mainPageState.isInitialized) {
    console.warn('⚠️ 메인 페이지가 이미 초기화되었습니다.');
    return;
  }

  try {
    // 서버 데이터 가져오기
    loadServerData();

    // 로그인 상태 확인
    await checkUserLoginStatus();

    // 이벤트 바인딩
    bindMainPageEvents();

    // 메모리얼 목록 로드
    if (mainPageState.isLoggedIn) {
      await loadMemorialList();
    }

    // 교차 관찰자 초기화
    initializeIntersectionObserver();

    // 풀 투 리프레시 초기화
    initializePullToRefresh();

    // 주기적 새로고침 설정
    setupPeriodicRefresh();

    mainPageState.isInitialized = true;
    console.log('✅ 메인 페이지 초기화 완료');

  } catch (error) {
    console.error('❌ 메인 페이지 초기화 실패:', error);
    showErrorState('페이지 초기화 중 오류가 발생했습니다.');
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
      currentUser: mainPageState.currentUser?.name
    });
  } else {
    console.warn('⚠️ 서버 데이터가 없습니다.');
  }
}

/**
 * 사용자 로그인 상태 확인
 */
async function checkUserLoginStatus() {
  console.log('🔐 사용자 로그인 상태 확인');

  try {
    // 클라이언트 토큰 확인
    const hasValidToken = checkLoginStatus();

    if (hasValidToken && !mainPageState.isLoggedIn) {
      // 토큰은 있지만 서버 데이터에 로그인 상태가 아닌 경우
      console.log('🔄 토큰 검증 및 사용자 정보 업데이트');
      await updateUserInfo();
    }

    // UI 상태 업데이트
    updateLoginUI();

  } catch (error) {
    console.error('❌ 로그인 상태 확인 실패:', error);
    mainPageState.isLoggedIn = false;
    updateLoginUI();
  }
}

/**
 * 사용자 정보 업데이트
 */
async function updateUserInfo() {
  console.log('👤 사용자 정보 업데이트');

  try {
    const response = await authFetch('/api/user/profile');
    const data = await response.json();

    if (data.status?.code === 'OK_0000') {
      mainPageState.currentUser = data.response;
      mainPageState.isLoggedIn = true;

      console.log('👤 사용자 정보 업데이트 완료:', mainPageState.currentUser.name);

      // UI 업데이트
      updateUserDisplay();

    } else {
      throw new Error(data.status?.message || '사용자 정보 조회 실패');
    }

  } catch (error) {
    console.error('❌ 사용자 정보 업데이트 실패:', error);
    throw error;
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
    loggedInElements.forEach(el => el.style.display = 'block');
    loggedOutElements.forEach(el => el.style.display = 'none');
  } else {
    loggedInElements.forEach(el => el.style.display = 'none');
    loggedOutElements.forEach(el => el.style.display = 'block');
  }
}

/**
 * 사용자 표시 업데이트
 */
function updateUserDisplay() {
  if (!mainPageState.currentUser) return;

  console.log('🎨 사용자 표시 업데이트:', mainPageState.currentUser.name);

  // 사이드 메뉴의 사용자 정보 업데이트
  const userAvatar = document.querySelector('.user-avatar img');
  const userName = document.querySelector('.user-name');
  const userEmail = document.querySelector('.user-email');

  if (userAvatar) {
    userAvatar.src = mainPageState.currentUser.profileImageUrl || '/assets/mobile/images/default-avatar.png';
  }
  if (userName) {
    userName.textContent = mainPageState.currentUser.name;
  }
  if (userEmail) {
    userEmail.textContent = mainPageState.currentUser.email;
  }
}

/**
 * 메인 페이지 이벤트 바인딩
 */
function bindMainPageEvents() {
  console.log('🔗 메인 페이지 이벤트 바인딩');

  // 새 메모리얼 등록 버튼들
  const createButtons = document.querySelectorAll('.new-memorial-btn, .add-memorial-btn');
  createButtons.forEach(btn => {
    btn.addEventListener('click', handleCreateMemorialClick);
  });

  // 하단 액션 버튼들
  const videoCallBtn = document.querySelector('.btn-video');
  const giftBtn = document.querySelector('.btn-gift');

  if (videoCallBtn) {
    videoCallBtn.addEventListener('click', handleVideoCallClick);
  }
  if (giftBtn) {
    giftBtn.addEventListener('click', handleGiftClick);
  }

  // 메모리얼 카드 클릭 이벤트
  bindMemorialCardEvents();

  // 페이지 가시성 변경 감지
  document.addEventListener('visibilitychange', handleVisibilityChange);

  console.log('✅ 메인 페이지 이벤트 바인딩 완료');
}

/**
 * 메모리얼 카드 이벤트 바인딩
 */
function bindMemorialCardEvents() {
  const memorialCards = document.querySelectorAll('.memorial-card');

  memorialCards.forEach(card => {
    card.addEventListener('click', function() {
      const memorialId = this.dataset.memorialId;
      if (memorialId) {
        handleMemorialCardClick(memorialId);
      }
    });
  });
}

/**
 * 메모리얼 목록 로드
 */
async function loadMemorialList() {
  console.log('📋 메모리얼 목록 로드 시작');

  if (mainPageState.isLoading) {
    console.log('⏳ 이미 로딩 중 - 스킵');
    return;
  }

  if (!mainPageState.isLoggedIn) {
    console.log('🔐 로그인 안됨 - 메모리얼 목록 로드 스킵');
    return;
  }

  try {
    mainPageState.isLoading = true;
    showLoadingState();

    console.log('🌐 메모리얼 목록 API 호출');
    const response = await authFetch('/api/memorial');
    const data = await response.json();

    if (data.status?.code === 'OK_0000') {
      const memorials = data.response || [];
      console.log('📋 메모리얼 개수:', memorials.length);

      mainPageState.memorialCards = memorials;
      mainPageState.retryCount = 0; // 성공 시 재시도 카운트 리셋

      if (memorials.length > 0) {
        renderMemorialList(memorials);
      } else {
        showEmptyState();
      }

    } else {
      throw new Error(data.status?.message || '메모리얼 목록을 불러올 수 없습니다.');
    }

  } catch (error) {
    console.error('❌ 메모리얼 목록 로드 실패:', error);
    handleLoadError(error);
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
  if (!container) {
    console.error('❌ #memorialList 컨테이너를 찾을 수 없음');
    return;
  }

  // 빈 상태 및 에러 상태 숨김
  hideEmptyState();
  hideErrorState();

  // 메모리얼 목록 섹션 표시
  const listSection = document.querySelector('.memorial-list-section');
  if (listSection) {
    listSection.style.display = 'block';
  }

  // 기존 내용 제거
  container.innerHTML = '';

  // 메모리얼 카드 생성
  memorials.forEach((memorial, index) => {
    const card = createMemorialCard(memorial);
    card.style.animationDelay = `${index * 0.1}s`;
    container.appendChild(card);
  });

  // 이벤트 바인딩
  bindMemorialCardEvents();

  console.log('✅ 메모리얼 목록 렌더링 완료');
}

/**
 * 메모리얼 카드 생성
 */
function createMemorialCard(memorial) {
  const card = document.createElement('div');
  card.className = 'memorial-card';
  card.dataset.memorialId = memorial.id;

  const lastVisit = memorial.lastVisitDate
    ? timeAgo(memorial.lastVisitDate)
    : '방문 기록 없음';

  const profileImage = memorial.profileImageUrl || '/assets/mobile/images/default-avatar.png';
  const onlineStatus = memorial.isOnline ? 'online' : 'offline';

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
 * 메모리얼 생성 클릭 처리
 */
async function handleCreateMemorialClick(e) {
  e.preventDefault();
  console.log('📝 메모리얼 생성 클릭');

  if (!mainPageState.isLoggedIn) {
    showLoginModal();
    return;
  }

  try {
    // 체험 상태 확인
    const trialStatus = await checkTrialStatus();

    if (trialStatus.isTrialUser && trialStatus.memorialCount >= trialStatus.maxMemorials) {
      console.log('⚠️ 무료체험 제한 - 모달 표시');
      showTrialLimitModal(trialStatus.maxMemorials);
    } else {
      console.log('✅ 메모리얼 생성 가능 - 생성 페이지 이동');
      window.location.href = window.serverData?.urls?.memorialCreate || '/mobile/memorial/create';
    }

  } catch (error) {
    console.error('❌ 메모리얼 생성 처리 실패:', error);
    // 에러 발생 시 일단 생성 페이지로 이동
    window.location.href = window.serverData?.urls?.memorialCreate || '/mobile/memorial/create';
  }
}

/**
 * 영상통화 클릭 처리
 */
async function handleVideoCallClick(e) {
  e.preventDefault();
  console.log('📹 영상통화 클릭');

  if (mainPageState.memorialCards.length === 0) {
    showToast('먼저 메모리얼을 등록해주세요.', 'warning');
    return;
  }

  try {
    // 로딩 상태 표시
    const loadingInstance = showLoading('영상통화 준비 중...');

    // 통화 가능 상태 확인 (첫 번째 메모리얼로 테스트)
    const firstMemorial = mainPageState.memorialCards[0];
    const response = await authFetch(`/api/memorial/${firstMemorial.id}/call-status`);
    const data = await response.json();

    loadingInstance.hide();

    if (data.status?.code === 'OK_0000') {
      const callStatus = data.response;

      if (callStatus.balance < callStatus.requiredTokens) {
        showInsufficientBalanceModal(callStatus.balance, callStatus.requiredTokens);
        return;
      }

      // 통화 시작 확인
      const confirmed = await showConfirm(
        '영상통화 시작',
        `${callStatus.memorialName}님과 영상통화를 시작하시겠습니까?\n\n` +
        `• 예상 비용: ${formatNumber(callStatus.costPerMinute)}원/분\n` +
        `• 현재 잔액: ${formatNumber(callStatus.balance)}원`,
        '통화 시작',
        '취소'
      );

      if (confirmed) {
        window.location.href = `/mobile/memorial/call/${firstMemorial.id}`;
      }
    } else {
      throw new Error(data.status?.message || '통화 상태 확인 실패');
    }

  } catch (error) {
    console.error('❌ 영상통화 처리 실패:', error);
    handleFetchError(error);
  }
}

/**
 * 선물하기 클릭 처리
 */
function handleGiftClick(e) {
  e.preventDefault();
  console.log('🎁 선물하기 클릭');

  showToast('선물하기 기능 준비 중입니다.', 'info');
}

/**
 * 메모리얼 카드 클릭 처리
 */
function handleMemorialCardClick(memorialId) {
  console.log('📱 메모리얼 카드 클릭:', memorialId);

  const memorial = mainPageState.memorialCards.find(m => m.id == memorialId);
  if (memorial) {
    console.log('🎯 메모리얼 정보:', memorial.name);
    // TODO: 메모리얼 상세 페이지로 이동
    window.location.href = `/mobile/memorial/detail/${memorialId}`;
  }
}

/**
 * 페이지 가시성 변경 처리
 */
function handleVisibilityChange() {
  if (!document.hidden && mainPageState.isLoggedIn) {
    console.log('👁️ 페이지 가시성 복원 - 메모리얼 상태 새로고침');
    refreshMemorialStatus();
  }
}

/**
 * 메모리얼 상태 새로고침
 */
async function refreshMemorialStatus() {
  if (!mainPageState.isLoggedIn || mainPageState.memorialCards.length === 0) {
    return;
  }

  console.log('🔄 메모리얼 상태 새로고침');

  try {
    const response = await authFetch('/api/memorial/status');
    const data = await response.json();

    if (data.status?.code === 'OK_0000') {
      updateMemorialStatus(data.response);
    }
  } catch (error) {
    console.error('❌ 메모리얼 상태 새로고침 실패:', error);
  }
}

/**
 * 메모리얼 상태 업데이트
 */
function updateMemorialStatus(statusList) {
  console.log('🔄 메모리얼 상태 업데이트:', statusList.length);

  statusList.forEach(status => {
    const card = document.querySelector(`[data-memorial-id="${status.id}"]`);
    if (card) {
      const statusIndicator = card.querySelector('.memorial-status');
      if (statusIndicator) {
        statusIndicator.className = `memorial-status ${status.isOnline ? 'online' : 'offline'}`;
      }

      const lastVisit = card.querySelector('.memorial-last-visit');
      if (lastVisit && status.lastVisitDate) {
        lastVisit.textContent = `마지막 방문: ${timeAgo(status.lastVisitDate)}`;
      }
    }
  });
}

/**
 * 체험 상태 확인
 */
async function checkTrialStatus() {
  console.log('🎁 체험 상태 확인');

  try {
    const response = await authFetch('/api/user/trial-status');
    const data = await response.json();

    if (data.status?.code === 'OK_0000') {
      return data.response;
    }

    return { isTrialUser: false, memorialCount: 0, maxMemorials: 0 };
  } catch (error) {
    console.error('체험 상태 확인 실패:', error);
    return { isTrialUser: false, memorialCount: 0, maxMemorials: 0 };
  }
}

/**
 * 로딩 상태 표시
 */
function showLoadingState() {
  console.log('⏳ 로딩 상태 표시');

  const skeleton = document.getElementById('loadingSkeleton');
  if (skeleton) {
    skeleton.style.display = 'block';
  }

  // 기존 목록 숨김
  const listSection = document.querySelector('.memorial-list-section');
  if (listSection) {
    listSection.style.display = 'none';
  }
}

/**
 * 로딩 상태 숨김
 */
function hideLoadingState() {
  console.log('⏳ 로딩 상태 숨김');

  const skeleton = document.getElementById('loadingSkeleton');
  if (skeleton) {
    skeleton.style.display = 'none';
  }
}

/**
 * 빈 상태 표시
 */
function showEmptyState() {
  console.log('📭 빈 상태 표시');

  const emptyState = document.querySelector('.empty-state');
  if (emptyState) {
    emptyState.style.display = 'block';
  }

  // 목록 섹션 숨김
  const listSection = document.querySelector('.memorial-list-section');
  if (listSection) {
    listSection.style.display = 'none';
  }
}

/**
 * 빈 상태 숨김
 */
function hideEmptyState() {
  const emptyState = document.querySelector('.empty-state');
  if (emptyState) {
    emptyState.style.display = 'none';
  }
}

/**
 * 에러 상태 표시
 */
function showErrorState(message) {
  console.log('❌ 에러 상태 표시:', message);

  const errorState = document.getElementById('errorState');
  const errorMessage = document.getElementById('errorMessage');

  if (errorState) {
    errorState.style.display = 'block';
  }
  if (errorMessage) {
    errorMessage.textContent = message;
  }

  // 다른 상태 숨김
  hideLoadingState();
  hideEmptyState();
}

/**
 * 에러 상태 숨김
 */
function hideErrorState() {
  const errorState = document.getElementById('errorState');
  if (errorState) {
    errorState.style.display = 'none';
  }
}

/**
 * 로드 에러 처리
 */
function handleLoadError(error) {
  mainPageState.retryCount++;

  if (mainPageState.retryCount < mainPageState.maxRetries) {
    console.log(`🔄 재시도 (${mainPageState.retryCount}/${mainPageState.maxRetries})`);

    setTimeout(() => {
      loadMemorialList();
    }, 1000 * mainPageState.retryCount);
  } else {
    console.error('❌ 최대 재시도 횟수 초과');
    showErrorState(error.message || '메모리얼 목록을 불러올 수 없습니다.');
  }
}

/**
 * 로그인 모달 표시
 */
function showLoginModal() {
  console.log('🔑 로그인 모달 표시');

  if (window.globalFunctions?.showLoginModal) {
    window.globalFunctions.showLoginModal();
  } else {
    alert('이 기능을 사용하려면\n먼저 로그인해 주세요.');
    setTimeout(() => {
      window.location.href = window.serverData?.urls?.login || '/mobile/login';
    }, 1000);
  }
}

/**
 * 잔액 부족 모달 표시
 */
function showInsufficientBalanceModal(balance, required) {
  console.log('💰 잔액 부족 모달 표시');

  if (window.globalFunctions?.showInsufficientBalanceModal) {
    window.globalFunctions.showInsufficientBalanceModal(balance, required);
  } else {
    alert(`잔액이 부족합니다.\n현재 잔액: ${formatNumber(balance)}원\n필요 금액: ${formatNumber(required)}원`);
  }
}

/**
 * 체험 제한 모달 표시
 */
function showTrialLimitModal(maxMemorials) {
  console.log('🎁 체험 제한 모달 표시');

  if (window.globalFunctions?.showTrialLimitModal) {
    window.globalFunctions.showTrialLimitModal(maxMemorials);
  } else {
    alert(`무료체험 제한\n최대 ${maxMemorials}개의 메모리얼만 등록할 수 있습니다.`);
  }
}

/**
 * 교차 관찰자 초기화
 */
function initializeIntersectionObserver() {
  if (!('IntersectionObserver' in window)) {
    console.warn('⚠️ IntersectionObserver를 지원하지 않는 브라우저');
    return;
  }

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
      }
    });
  }, {
    threshold: 0.1,
    rootMargin: '50px'
  });

  // 관찰 대상 요소들
  const targets = document.querySelectorAll('.memorial-card, .memorial-intro-card, .service-intro');
  targets.forEach(target => observer.observe(target));
}

/**
 * 풀 투 리프레시 초기화
 */
function initializePullToRefresh() {
  let startY = 0;
  let currentY = 0;
  let isPulling = false;
  let pullDistance = 0;
  const threshold = 100;

  document.addEventListener('touchstart', (e) => {
    if (window.scrollY === 0) {
      startY = e.touches[0].pageY;
      isPulling = true;
    }
  });

  document.addEventListener('touchmove', (e) => {
    if (!isPulling) return;

    currentY = e.touches[0].pageY;
    pullDistance = currentY - startY;

    if (pullDistance > threshold) {
      e.preventDefault();
      // 새로고침 표시
      showToast('새로고침 중...', 'info', 1000);

      if (mainPageState.isLoggedIn) {
        refreshMemorialStatus();
      }

      isPulling = false;
    }
  });

  document.addEventListener('touchend', () => {
    isPulling = false;
    pullDistance = 0;
  });
}

/**
 * 주기적 새로고침 설정
 */
function setupPeriodicRefresh() {
  // 5분마다 상태 새로고침
  if (mainPageState.refreshInterval) {
    clearInterval(mainPageState.refreshInterval);
  }

  mainPageState.refreshInterval = setInterval(() => {
    if (mainPageState.isLoggedIn && !document.hidden) {
      console.log('🔄 주기적 새로고침');
      refreshMemorialStatus();
    }
  }, 5 * 60 * 1000); // 5분
}

/**
 * 메인 페이지 정리
 */
function destroyMainPage() {
  console.log('🗑️ 메인 페이지 정리');

  // 주기적 새로고침 정리
  if (mainPageState.refreshInterval) {
    clearInterval(mainPageState.refreshInterval);
    mainPageState.refreshInterval = null;
  }

  // 상태 초기화
  mainPageState = {
    isLoading: false,
    isInitialized: false,
    memorialCards: [],
    isLoggedIn: false,
    currentUser: null,
    retryCount: 0,
    maxRetries: 3,
    refreshInterval: null
  };
}

// 전역 함수 등록
window.mainPageManager = {
  initialize: initializeMainPage,
  loadMemorialList,
  refreshMemorialStatus,
  destroy: destroyMainPage,
  getState: () => mainPageState
};

// 전역 함수들 (하위 호환성)
window.refreshMemorialList = loadMemorialList;
window.handleCreateMemorial = handleCreateMemorialClick;
window.showVideoCall = handleVideoCallClick;
window.showGiftInfo = handleGiftClick;

// 디버그 함수
window.debugMainPage = function() {
  console.group('🔍 메인 페이지 디버그 정보');
  console.log('상태:', mainPageState);
  console.log('로그인 여부:', mainPageState.isLoggedIn);
  console.log('현재 사용자:', mainPageState.currentUser);
  console.log('메모리얼 개수:', mainPageState.memorialCards.length);
  console.log('로딩 상태:', mainPageState.isLoading);
  console.log('초기화 상태:', mainPageState.isInitialized);
  console.log('재시도 횟수:', mainPageState.retryCount);
  console.groupEnd();
};

// 자동 초기화
console.log('🌟 main.js 스크립트 로드 완료');

// DOM 로드 완료 후 초기화
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    console.log('📱 DOM 로드 완료 - 메인 페이지 초기화');
    initializeMainPage();
  });
} else {
  console.log('📱 DOM 이미 로드됨 - 메인 페이지 즉시 초기화');
  initializeMainPage();
}

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', () => {
  destroyMainPage();
});

// 모듈 익스포트
export {
  initializeMainPage,
  loadMemorialList,
  refreshMemorialStatus,
  destroyMainPage,
  handleCreateMemorialClick,
  handleVideoCallClick,
  handleGiftClick
};