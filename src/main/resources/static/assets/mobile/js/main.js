// main-simple.js - 간소화된 메인 페이지 JavaScript (이벤트 바인딩 타이밍 수정, FAB 초기화 주석)

import { showToast, showConfirm } from './common.js';
import { authFetch, memberLogout } from './commonFetch.js';

// 메인 페이지 상태 관리 (단순화)
let mainPageState = {
  isInitialized: false,
  selectedMemorialId: null,
  isLoggedIn: false,
  currentUser: null,
  memorialItems: []
};

// 체험하기 FAB 상태 (단순화) - 주석 처리 상태로 유지
let experienceFabState = {
  isExpanded: false,
  fab: null,
  options: null
};

/**
 * ===== 메인 초기화 함수 =====
 */
function initializeMainPage() {
  console.log('🚀 메인 페이지 초기화 시작 (간소화 버전)');

  if (mainPageState.isInitialized) {
    console.warn('⚠️ 메인 페이지가 이미 초기화되었습니다.');
    return;
  }

  try {
    // 1. 서버 데이터 로드
    loadServerData();

    // 2. 이벤트 바인딩 (FAB 포함)
    bindAllEvents();

    // 3. FAB 버튼 초기화 (DOM이 완전히 준비된 후) - 주석 처리
    // requestAnimationFrame으로 DOM 렌더링 완료 후 실행
    /*
    requestAnimationFrame(() => {
      setTimeout(() => {
        initializeFabButtons();
      }, 100); // 100ms 후 실행으로 DOM 완전 준비 보장
    });
    */

    // 3-1. 채팅 FAB만 초기화 (로그인 시에만 존재)
    requestAnimationFrame(() => {
      setTimeout(() => {
        initializeChatFabOnly();
      }, 100);
    });

    // 4. 초기화 완료
    mainPageState.isInitialized = true;
    console.log('✅ 메인 페이지 초기화 완료 (간소화 버전)');

  } catch (error) {
    console.error('❌ 메인 페이지 초기화 실패:', error);
    showToast('페이지 초기화 중 오류가 발생했습니다.', 'error');
  }
}

/**
 * ===== 서버 데이터 로드 =====
 */
function loadServerData() {
  console.log('📡 서버 데이터 로드');

  if (window.serverData) {
    mainPageState.isLoggedIn = window.serverData.isLoggedIn || false;
    mainPageState.memorialItems = window.serverData.memorialList || [];
    mainPageState.currentUser = window.serverData.currentUser || null;

    console.log('✅ 서버 데이터 로드 완료:', {
      isLoggedIn: mainPageState.isLoggedIn,
      memorialCount: mainPageState.memorialItems.length,
      currentUser: mainPageState.currentUser?.name || 'None'
    });
  } else {
    console.warn('⚠️ 서버 데이터가 없습니다.');
  }
}

/**
 * ===== 채팅 FAB만 초기화 (체험하기 FAB 제거) =====
 */
function initializeChatFabOnly() {
  console.log('🎯 채팅 FAB만 초기화');

  // 채팅 FAB 이벤트 바인딩 (로그인 시에만 존재)
  const chatFab = document.getElementById('chatFab');
  if (chatFab) {
    try {
      chatFab.addEventListener('click', handleChatFabClick);
      console.log('✅ 채팅 FAB 클릭 이벤트 바인딩 완료');
    } catch (error) {
      console.error('❌ 채팅 FAB 이벤트 바인딩 실패:', error);
    }
  } else {
    console.log('📝 채팅 FAB 없음 (로그인하지 않았거나 조건부 렌더링으로 숨김)');
  }

  console.log('✅ 채팅 FAB 초기화 완료');
}

/**
 * ===== FAB 버튼 초기화 (HTML에 이미 존재) - 주석 처리 =====
 */
/*
function initializeFabButtons() {
  console.log('🎯 FAB 버튼 초기화 (정적 생성 버전)');

  // 체험하기 FAB 요소 찾기 (재시도 로직 추가)
  experienceFabState.fab = document.getElementById('experienceFab');
  experienceFabState.options = document.getElementById('experienceOptions');

  if (!experienceFabState.fab) {
    console.error('❌ 체험하기 FAB 버튼을 찾을 수 없습니다. 재시도 중...');

    // 1초 후 재시도
    setTimeout(() => {
      initializeFabButtons();
    }, 1000);
    return;
  }

  console.log('✅ 체험하기 FAB 요소 찾음:', {
    fab: !!experienceFabState.fab,
    options: !!experienceFabState.options
  });

  // 체험하기 FAB 이벤트 바인딩
  try {
    experienceFabState.fab.addEventListener('click', handleExperienceFabClick);
    console.log('✅ 체험하기 FAB 클릭 이벤트 바인딩 완료');
  } catch (error) {
    console.error('❌ 체험하기 FAB 이벤트 바인딩 실패:', error);
  }

  // 체험하기 옵션 이벤트 바인딩
  if (experienceFabState.options) {
    try {
      const optionBtns = experienceFabState.options.querySelectorAll('.experience-option-btn');
      console.log('🎯 체험하기 옵션 버튼 개수:', optionBtns.length);

      optionBtns.forEach((btn, index) => {
        btn.addEventListener('click', handleExperienceOptionClick);
        console.log(`✅ 체험하기 옵션 ${index + 1} 이벤트 바인딩 완료`);
      });
    } catch (error) {
      console.error('❌ 체험하기 옵션 이벤트 바인딩 실패:', error);
    }
  }

  // 채팅 FAB 이벤트 바인딩 (로그인 시에만 존재)
  const chatFab = document.getElementById('chatFab');
  if (chatFab) {
    try {
      chatFab.addEventListener('click', handleChatFabClick);
      console.log('✅ 채팅 FAB 클릭 이벤트 바인딩 완료');
    } catch (error) {
      console.error('❌ 채팅 FAB 이벤트 바인딩 실패:', error);
    }
  } else {
    console.log('📝 채팅 FAB 없음 (로그인하지 않았거나 조건부 렌더링으로 숨김)');
  }

  // 외부 클릭 시 체험하기 옵션 닫기
  document.addEventListener('click', handleOutsideClick);

  console.log('✅ FAB 버튼 초기화 완료');
}
*/

/**
 * ===== 이벤트 바인딩 =====
 */
function bindAllEvents() {
  console.log('🔗 모든 이벤트 바인딩 시작');

  bindCreateMemorialButtons();
  bindVideoCallButton();
  bindFreeTrialButton();
  bindMemorialItems();
  bindOtherButtons();

  console.log('✅ 모든 이벤트 바인딩 완료');
}

function bindCreateMemorialButtons() {
  const createButtons = document.querySelectorAll(`
    .new-memorial-btn,
    .add-memorial-btn,
    .empty-state-create-btn
  `);

  createButtons.forEach(btn => {
    btn.removeEventListener('click', handleCreateMemorialClick);
    btn.addEventListener('click', handleCreateMemorialClick);
  });

  console.log('🔗 메모리얼 생성 버튼 바인딩 완료:', createButtons.length);
}

function bindVideoCallButton() {
  const videoCallBtn = document.querySelector('.video-call-btn');
  if (videoCallBtn) {
    videoCallBtn.removeEventListener('click', handleVideoCallClick);
    videoCallBtn.addEventListener('click', handleVideoCallClick);
    console.log('🔗 영상통화 버튼 바인딩 완료');
  }
}

function bindFreeTrialButton() {
  const freeTrialBtn = document.querySelector('.free-trial-btn');
  if (freeTrialBtn) {
    freeTrialBtn.removeEventListener('click', handleFreeTrialClick);
    freeTrialBtn.addEventListener('click', handleFreeTrialClick);
    console.log('🔗 무료체험 버튼 바인딩 완료');
  }
}

function bindMemorialItems() {
  const memorialItems = document.querySelectorAll('.memorial-item');
  memorialItems.forEach(item => {
    item.removeEventListener('click', handleMemorialItemClick);
    item.addEventListener('click', handleMemorialItemClick);
  });

  console.log('🔗 메모리얼 아이템 바인딩 완료:', memorialItems.length);
}

function bindOtherButtons() {
  const refreshBtn = document.querySelector('.refresh-btn');
  if (refreshBtn) {
    refreshBtn.removeEventListener('click', handleRefreshClick);
    refreshBtn.addEventListener('click', handleRefreshClick);
  }

  const retryBtn = document.querySelector('#errorState .btn');
  if (retryBtn) {
    retryBtn.removeEventListener('click', handleRetryClick);
    retryBtn.addEventListener('click', handleRetryClick);
  }
}

/**
 * ===== 이벤트 핸들러들 =====
 */

// 체험하기 FAB 클릭 - 주석 처리
/*
function handleExperienceFabClick(e) {
  e.preventDefault();
  e.stopPropagation();

  console.log('🚀 체험하기 FAB 클릭', {
    isExpanded: experienceFabState.isExpanded,
    fab: !!experienceFabState.fab,
    options: !!experienceFabState.options
  });

  if (experienceFabState.isExpanded) {
    closeExperienceOptions();
  } else {
    openExperienceOptions();
  }
}

function openExperienceOptions() {
  if (!experienceFabState.fab || !experienceFabState.options) {
    console.error('❌ 체험하기 버튼 또는 옵션을 찾을 수 없습니다.');
    return;
  }

  experienceFabState.isExpanded = true;

  // 아이콘 변경 (플레이 → X)
  experienceFabState.fab.innerHTML = '<i class="fas fa-times"></i>';
  experienceFabState.fab.classList.add('expanded');

  // 확장 옵션 표시
  experienceFabState.options.classList.add('show');

  // 접근성
  experienceFabState.fab.setAttribute('aria-expanded', 'true');
  experienceFabState.fab.setAttribute('aria-label', '체험 옵션 닫기');

  console.log('🎯 확장 옵션 열림');
}

function closeExperienceOptions() {
  if (!experienceFabState.fab || !experienceFabState.options) {
    console.error('❌ 체험하기 버튼 또는 옵션을 찾을 수 없습니다.');
    return;
  }

  experienceFabState.isExpanded = false;

  // 아이콘 변경 (X → 플레이)
  experienceFabState.fab.innerHTML = '<i class="fas fa-play"></i>';
  experienceFabState.fab.classList.remove('expanded');

  // 확장 옵션 숨김
  experienceFabState.options.classList.remove('show');

  // 접근성
  experienceFabState.fab.setAttribute('aria-expanded', 'false');
  experienceFabState.fab.setAttribute('aria-label', '체험하기');

  console.log('🎯 확장 옵션 닫힘');
}

// 체험하기 옵션 클릭
function handleExperienceOptionClick(e) {
  const btn = e.currentTarget;
  const href = btn.getAttribute('href');
  const name = btn.querySelector('span').textContent;

  console.log(`🎯 ${name} 버튼 클릭:`, href);

  // 클릭 효과
  btn.style.transform = 'scale(0.95)';
  setTimeout(() => {
    btn.style.transform = '';
  }, 150);

  // 토스트 메시지
  if (window.showToast) {
    showToast(`${name}으로 이동합니다.`, 'info', 2000);
  }

  // 확장 옵션 닫기
  setTimeout(() => {
    closeExperienceOptions();
  }, 300);
}
*/

// 채팅 FAB 클릭
function handleChatFabClick(e) {
  e.preventDefault();
  e.stopPropagation();

  console.log('💬 채팅 FAB 클릭');

  const chatFab = e.currentTarget;

  // 클릭 효과
  chatFab.style.transform = 'scale(0.95)';
  setTimeout(() => {
    chatFab.style.transform = 'scale(1)';
  }, 150);

  if (window.showToast) {
    showToast('채팅 기능 준비 중입니다.', 'info', 2000);
  } else {
    alert('채팅 기능 준비 중입니다.');
  }
}

// 외부 클릭시 닫기 - 주석 처리 (체험하기 FAB 없으므로)
/*
function handleOutsideClick(e) {
  if (!experienceFabState.isExpanded) return;

  const fab = experienceFabState.fab;
  const options = experienceFabState.options;

  if (fab && options &&
      !fab.contains(e.target) &&
      !options.contains(e.target)) {
    closeExperienceOptions();
  }
}
*/

// 메모리얼 생성 클릭
async function handleCreateMemorialClick(e) {
  e.preventDefault();
  e.stopPropagation();

  console.log('➕ 메모리얼 생성 클릭');

  if (!mainPageState.isLoggedIn) {
    showLoginModal();
    return;
  }

  window.location.href = '/mobile/memorial/create';
}

// 메모리얼 아이템 클릭
function handleMemorialItemClick(e) {
  e.preventDefault();
  e.stopPropagation();

  const memorialId = parseInt(e.currentTarget.dataset.memorialId);
  console.log('📋 메모리얼 아이템 클릭:', memorialId);

  if (!memorialId) return;

  // 선택/해제 토글
  if (mainPageState.selectedMemorialId === memorialId) {
    mainPageState.selectedMemorialId = null;
    console.log('❌ 메모리얼 선택 해제');
  } else {
    mainPageState.selectedMemorialId = memorialId;
    console.log('✅ 메모리얼 선택:', memorialId);
  }

  // UI 업데이트
  updateMemorialSelection();
  updateVideoCallButtonState();
}

// 영상통화 클릭
async function handleVideoCallClick(e) {
  e.preventDefault();
  console.log('📹 영상통화 클릭');

  if (mainPageState.memorialItems.length === 0) {
    alert('먼저 메모리얼을 등록해주세요.');
    return;
  }

  let selectedMemorial = null;

  // 메모리얼이 1개인 경우 자동 선택
  if (mainPageState.memorialItems.length === 1) {
    selectedMemorial = mainPageState.memorialItems[0];
  }
  // 여러개인 경우 선택된 메모리얼 확인
  else {
    if (!mainPageState.selectedMemorialId) {
      alert('영상통화할 메모리얼을 선택해주세요.');
      return;
    }
    selectedMemorial = mainPageState.memorialItems.find(
        item => item.memorialId === mainPageState.selectedMemorialId
    );
  }

  if (!selectedMemorial) {
    alert('선택된 메모리얼을 찾을 수 없습니다.');
    return;
  }

  console.log('✅ 선택된 메모리얼:', selectedMemorial);
  await checkVideoCallAvailability(selectedMemorial);
}

// 무료체험 클릭
function handleFreeTrialClick(e) {
  e.preventDefault();
  console.log('🎁 무료체험 클릭');

  if (mainPageState.isLoggedIn) {
    alert('이미 로그인된 상태입니다.');
    return;
  }

  window.location.href = '/mobile/register?trial=true';
}

// 새로고침 클릭
async function handleRefreshClick(e) {
  e.preventDefault();
  console.log('🔄 새로고침 클릭');
  window.location.reload();
}

// 재시도 클릭
async function handleRetryClick(e) {
  e.preventDefault();
  console.log('🔄 재시도 클릭');
  window.location.reload();
}

/**
 * ===== 영상통화 가능 여부 확인 =====
 */
async function checkVideoCallAvailability(memorial) {
  console.log('영상통화 가능 여부 확인:', memorial);

  try {
    // 1. 프로필 이미지 확인
    if (!memorial.hasRequiredProfileImages) {
      const confirmed = confirm(
          '영상통화 시작을 위해서는 프로필 사진을 등록해주세요.\n\n내정보 수정 페이지로 이동하시겠습니까?'
      );

      if (confirmed) {
        window.location.href = `/mobile/account/profile`;
      }
      return;
    }

    // 2-1. 소유자인 경우
    if (memorial.isOwner) {
      if (!memorial.aiTrainingCompleted) {
        alert('영상통화 준비를 위한 학습 중입니다. 잠시만 기다려주세요.');
        return;
      }

      startVideoCall(memorial.memorialId);
      return;
    }

    // 2-2. 가족 구성원인 경우
    if (!memorial.hasRequiredDeceasedInfo) {
      const confirmed = confirm(
          '영상통화 시작을 위해서는 고인에 대한 상세 정보를 입력해주세요.\n\n' +
          '(5개 항목을 모두 입력해야 합니다)\n\n' +
          '메모리얼 정보 입력 페이지로 이동하시겠습니까?'
      );

      if (confirmed) {
        window.location.href = `/mobile/memorial/family-info/${memorial.memorialId}`;
      }
      return;
    }

    // 3. AI 학습 완료 확인
    if (!memorial.aiTrainingCompleted) {
      alert('영상통화 준비를 위한 학습 중입니다. 잠시만 기다려주세요.');
      return;
    }

    startVideoCall(memorial.memorialId);

  } catch (error) {
    console.error('❌ 영상통화 가능 여부 확인 중 오류:', error);
    alert('영상통화 확인 중 오류가 발생했습니다.');
  }
}

/**
 * ===== UI 상태 업데이트 함수들 =====
 */
function updateMemorialSelection() {
  console.log('🎨 메모리얼 선택 UI 업데이트');

  const memorialItems = document.querySelectorAll('.memorial-item');

  memorialItems.forEach(item => {
    const memorialId = parseInt(item.dataset.memorialId);

    if (memorialId === mainPageState.selectedMemorialId) {
      item.classList.add('selected');
      console.log('✅ 메모리얼 선택 표시:', memorialId);
    } else {
      item.classList.remove('selected');
    }
  });
}

function updateVideoCallButtonState() {
  console.log('🎨 영상통화 버튼 상태 업데이트');

  const videoCallBtn = document.querySelector('.video-call-btn');
  if (!videoCallBtn) return;

  const hasMemorials = mainPageState.memorialItems.length > 0;
  const hasSelection = mainPageState.selectedMemorialId !== null;
  const isMultipleMemorials = mainPageState.memorialItems.length > 1;

  if (!hasMemorials) {
    videoCallBtn.disabled = true;
    videoCallBtn.innerHTML = '<i class="fas fa-video"></i> 영상통화';
    return;
  }

  let selectedMemorial = null;
  if (!isMultipleMemorials) {
    selectedMemorial = mainPageState.memorialItems[0];
  } else if (hasSelection) {
    selectedMemorial = mainPageState.memorialItems.find(
        item => item.memorialId === mainPageState.selectedMemorialId
    );
  }

  if (selectedMemorial) {
    const blockReason = getVideoCallBlockReason(selectedMemorial);
    if (blockReason) {
      videoCallBtn.disabled = false;
      videoCallBtn.innerHTML = `<i class="fas fa-video"></i> 영상통화 <small>(${getShortBlockReason(blockReason)})</small>`;
    } else {
      videoCallBtn.disabled = false;
      videoCallBtn.innerHTML = '<i class="fas fa-video"></i> 영상통화';
    }
  } else {
    videoCallBtn.disabled = false;
    videoCallBtn.innerHTML = '<i class="fas fa-video"></i> 영상통화';
  }
}

function getVideoCallBlockReason(memorial) {
  if (!memorial.hasRequiredProfileImages) {
    return '프로필 사진을 등록해주세요.';
  }

  if (!memorial.isOwner && !memorial.hasRequiredDeceasedInfo) {
    return '고인에 대한 상세 정보를 입력해주세요.';
  }

  if (!memorial.aiTrainingCompleted) {
    return 'AI 학습이 완료되지 않았습니다.';
  }

  if (!memorial.canAccess) {
    return '접근 권한이 없습니다.';
  }

  if (!memorial.isOwner && !memorial.canVideoCall) {
    return '영상통화 권한이 없습니다.';
  }

  return null;
}

function getShortBlockReason(fullReason) {
  const shortReasons = {
    '프로필 사진을 등록해주세요.': '프로필 필요',
    '고인에 대한 상세 정보를 입력해주세요.': '정보 입력 필요',
    'AI 학습이 완료되지 않았습니다.': '학습 중',
    '접근 권한이 없습니다.': '권한 없음',
    '영상통화 권한이 없습니다.': '권한 없음'
  };

  return shortReasons[fullReason] || '준비 중';
}

function startVideoCall(memorialId) {
  console.log('📹 영상통화 시작:', memorialId);
  window.location.href = `/mobile/videocall/${memorialId}`;
}

function showLoginModal() {
  const confirmLogin = confirm('이 기능을 사용하려면 로그인이 필요합니다.\n\n로그인 페이지로 이동하시겠습니까?');
  if (confirmLogin) {
    window.location.href = '/mobile/login';
  }
}

/**
 * ===== 전역 함수들 =====
 */
window.mainPageManager = {
  initialize: initializeMainPage,
  getState: () => mainPageState,
  selectMemorial: (memorialId) => {
    mainPageState.selectedMemorialId = memorialId;
    updateMemorialSelection();
    updateVideoCallButtonState();
  },
  // 체험하기 토글 함수 주석 처리
  /*
  toggleExperience: () => {
    if (experienceFabState.isExpanded) {
      closeExperienceOptions();
    } else {
      openExperienceOptions();
    }
  },
  */
  // 디버깅용 함수
  debugFab: () => {
    console.log('FAB 디버그 정보:', {
      /*
      experienceFab: {
        element: !!experienceFabState.fab,
        id: experienceFabState.fab?.id,
        expanded: experienceFabState.isExpanded
      },
      experienceOptions: {
        element: !!experienceFabState.options,
        id: experienceFabState.options?.id,
        buttons: experienceFabState.options?.querySelectorAll('.experience-option-btn').length || 0
      },
      */
      chatFab: {
        element: !!document.getElementById('chatFab')
      }
    });
  }
};

// HTML에서 호출 가능한 전역 함수들
window.handleCreateMemorial = handleCreateMemorialClick;
window.showVideoCall = handleVideoCallClick;
window.showLoginModal = showLoginModal;

/**
 * ===== 자동 초기화 =====
 */
console.log('🎉 토마토리멤버 main-simple.js 로드 완료 (정적 FAB 버전 - 이벤트 바인딩 수정, 체험하기 FAB 제거)');

// DOM이 준비되면 초기화
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initializeMainPage);
} else {
  // 이미 DOM이 준비된 경우 즉시 실행
  setTimeout(initializeMainPage, 50);
}

// 키보드 단축키 지원 - 주석 처리 (체험하기 FAB 제거)
/*
document.addEventListener('keydown', function(e) {
  // ESC 키로 체험하기 옵션 닫기
  if (e.key === 'Escape' && experienceFabState.isExpanded) {
    e.preventDefault();
    closeExperienceOptions();
  }
});
*/

// 모듈 익스포트
export {
  initializeMainPage,
  handleCreateMemorialClick,
  handleVideoCallClick,
  updateMemorialSelection,
  updateVideoCallButtonState,
  // 체험하기 관련 함수 주석 처리
  // openExperienceOptions,
  // closeExperienceOptions
};