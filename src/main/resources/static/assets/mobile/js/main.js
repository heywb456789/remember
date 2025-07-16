// main.js - 토마토리멤버 메인 페이지 (초대 토큰 처리 추가)

import { authFetch, checkLoginStatus, handleFetchError } from './commonFetch.js';
import { showToast, showConfirm, showLoading, hideLoading } from './common.js';

// 메인 페이지 상태 관리
let mainPageState = {
  isLoading: false,
  isInitialized: false,
  memorialItems: [],
  selectedMemorialId: null,
  isLoggedIn: false,
  currentUser: null,
  retryCount: 0,
  maxRetries: 3,
  refreshInterval: null,
  inviteProcessing: false // 초대 처리 중 플래그 추가
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
 * 가족 구성원 배지 스타일 추가
 */
const familyMemberBadgeStyle = `
  .family-member-badge {
    background: #e3f2fd;
    color: #1976d2;
    font-size: 10px;
    padding: 2px 6px;
    border-radius: 4px;
    margin-left: 8px;
    font-weight: 500;
  }

  .memorial-meta {
    font-size: 12px;
    color: var(--text-light);
    margin-top: 4px;
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .memorial-meta .separator {
    opacity: 0.5;
  }

  .access-type {
    font-weight: 500;
    color: var(--text-secondary);
  }

  .status-icon.status-heart {
    color: #e91e63;
  }

  .status-icon.status-heart.status-ok {
    color: #4caf50;
  }

  .status-icon.status-heart.status-warning {
    color: #ff9800;
  }
`;


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

    // 2. 초대 토큰 처리 (최우선)
    if (mainPageState.isLoggedIn) {
      checkAndProcessInviteToken();
    }

    // 3. 이벤트 바인딩
    bindAllEvents();

    // 4. 로그인 상태 UI 업데이트
    updateLoginUI();

    // 5. 로그인한 경우 추가 초기화
    if (mainPageState.isLoggedIn) {
      initializeLoggedInFeatures();
    }

    // 6. 초기화 완료 플래그 설정
    mainPageState.isInitialized = true;
    console.log('메인 페이지 초기화 완료');

  } catch (error) {
    console.error('메인 페이지 초기화 실패:', error);
    showToast('페이지 초기화 중 오류가 발생했습니다.', 'error');
  }
}

/**
 * 초대 토큰 확인 및 처리 (sessionStorage 기반)
 */
async function checkAndProcessInviteToken() {
  console.log('초대 토큰 확인 시작');

  if (mainPageState.inviteProcessing) {
    console.log('초대 처리가 이미 진행 중입니다.');
    return;
  }

  try {
    // sessionStorage에서 초대 토큰 조회
    const inviteToken = sessionStorage.getItem('inviteToken');

    if (!inviteToken) {
      console.log('초대 토큰이 없습니다.');
      return;
    }

    console.log('초대 토큰 발견:', inviteToken.substring(0, 8) + '...');

    // 초대 처리 시작
    mainPageState.inviteProcessing = true;

    // 로딩 표시
    const loading = showLoading('초대 처리 중...');

    // 초대 토큰 처리 API 호출
    const response = await authFetch('/api/family/invite/process', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        token: inviteToken
      })
    });

    console.log('초대 처리 API 응답:', response);

    // 성공 처리
    if (response.status?.code === 'OK_0000') {
      const result = response.response;

      // 성공 토스트 표시
      showToast(result.message || '초대 처리가 완료되었습니다.', 'success');

      // sessionStorage에서 토큰 제거
      sessionStorage.removeItem('inviteToken');

      // 가족 목록 새로고침 (필요한 경우)
      setTimeout(() => {
        window.location.reload();
      }, 2000);

      console.log('초대 처리 완료');

    } else {
      // 실패 처리
      const errorMessage = response.status?.message || '초대 처리에 실패했습니다.';
      console.error('초대 처리 실패:', errorMessage);

      showToast(errorMessage, 'error');

      // 실패 시에도 토큰 제거 (보안상 이유)
      sessionStorage.removeItem('inviteToken');
    }

  } catch (error) {
    console.error('초대 토큰 처리 중 오류:', error);

    // 에러 메시지 표시
    const errorMessage = error.name === 'FetchError' ?
        error.statusMessage :
        '초대 처리 중 오류가 발생했습니다.';

    showToast(errorMessage, 'error');

    // 에러 시에도 토큰 제거
    sessionStorage.removeItem('inviteToken');

  } finally {
    // 로딩 숨김
    hideLoading();

    // 초대 처리 플래그 해제
    mainPageState.inviteProcessing = false;
  }
}

/**
 * 초대 토큰 유효성 검증 (선택적 사용)
 */
async function validateInviteToken(token) {
  console.log('초대 토큰 유효성 검증:', token.substring(0, 8) + '...');

  try {
    const response = await authFetch(`/api/family/invite/validate/${token}`);

    if (response.status?.code === 'OK_0000') {
      return response.response;
    } else {
      console.warn('토큰 유효성 검증 실패:', response.status?.message);
      return { valid: false, message: response.status?.message };
    }

  } catch (error) {
    console.error('토큰 유효성 검증 오류:', error);
    return { valid: false, message: '토큰 검증 중 오류가 발생했습니다.' };
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

  // 4. 메모리얼 아이템들 (선택 기능 추가)
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
 * 메모리얼 아이템 바인딩 (선택 기능으로 변경)
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

    // 영상통화 버튼 상태 업데이트
    updateVideoCallButtonState();

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

// 메모리얼 아이템 클릭 핸들러 (선택 기능으로 변경)
function handleMemorialItemClick(e) {
  e.preventDefault();
  e.stopPropagation();

  const memorialId = parseInt(e.currentTarget.dataset.memorialId);
  console.log('메모리얼 아이템 클릭:', memorialId);

  if (!memorialId) return;

  // 이미 선택된 메모리얼을 다시 클릭한 경우 선택 해제
  if (mainPageState.selectedMemorialId === memorialId) {
    mainPageState.selectedMemorialId = null;
    console.log('메모리얼 선택 해제');
  } else {
    mainPageState.selectedMemorialId = memorialId;
    console.log('메모리얼 선택:', memorialId);
  }

  // UI 업데이트
  updateMemorialSelection();
  updateVideoCallButtonState();
}

// 영상통화 클릭 핸들러 (수정된 로직)
async function handleVideoCallClick(e) {
  e.preventDefault();
  console.log('영상통화 클릭');

  // 메모리얼이 없는 경우
  if (mainPageState.memorialItems.length === 0) {
    alert('먼저 메모리얼을 등록해주세요.');
    return;
  }

  let selectedMemorial = null;

  // 메모리얼이 1개인 경우 자동 선택
  if (mainPageState.memorialItems.length === 1) {
    selectedMemorial = mainPageState.memorialItems[0];
  }
  // 메모리얼이 여러개인 경우 선택된 메모리얼 확인
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

  console.log('선택된 메모리얼:', selectedMemorial);

  // 영상통화 가능 여부 확인
  await checkVideoCallAvailability(selectedMemorial);
}

// 무료체험 클릭 핸들러
function handleFreeTrialClick(e) {
  e.preventDefault();
  console.log('무료체험 클릭');

  if (mainPageState.isLoggedIn) {
    alert('이미 로그인된 상태입니다.');
    return;
  }

  // 회원가입 페이지로 이동
  window.location.href = '/mobile/register?trial=true';
}

// 새로고침 클릭 핸들러
async function handleRefreshClick(e) {
  e.preventDefault();
  console.log('새로고침 클릭');

  if (mainPageState.isLoggedIn) {
    // 로그인 상태면 메모리얼 목록 다시 로드
    mainPageState.retryCount = 0;
    await loadMemorialList();
  } else {
    // 서버사이드 렌더링 사용하므로 페이지 새로고침
    window.location.reload();
  }
}

// 재시도 클릭 핸들러
async function handleRetryClick(e) {
  e.preventDefault();
  console.log('재시도 클릭');

  // 에러 상태 숨김
  hideErrorState();

  // 재시도 카운트 리셋
  mainPageState.retryCount = 0;

  if (mainPageState.isLoggedIn) {
    // 로그인 상태면 메모리얼 목록 다시 로드
    await loadMemorialList();
  } else {
    // 로그아웃 상태면 페이지 새로고침
    window.location.reload();
  }
}

/**
 * 영상통화 가능 여부 확인
 */
async function checkVideoCallAvailability(memorial) {
  console.log('영상통화 가능 여부 확인:', memorial);

  try {
    // 1. 프로필 이미지 확인 (공통)
    if (!memorial.hasRequiredProfileImages) {
      const confirmed = confirm(
          '영상통화 시작을 위해서는 프로필 사진을 등록해주세요.\n\n내정보 수정 페이지로 이동하시겠습니까?'
      );

      if (confirmed) {
        window.location.href = `/mobile/account/profile`;
      }
      return;
    }

    // 2-1. 소유자인 경우: AI 학습 완료 확인
    if (memorial.isOwner) {
      if (!memorial.aiTrainingCompleted) {
        alert('영상통화 준비를 위한 학습 중입니다. 잠시만 기다려주세요.');
        return;
      }

      // 소유자 - 모든 조건 만족 시 영상통화 시작
      startVideoCall(memorial.memorialId);
      return;
    }

    // 2-2. 가족 구성원인 경우: 고인 상세 정보 확인
    if (!memorial.hasRequiredDeceasedInfo) {
      const confirmed = confirm(
          '영상통화 시작을 위해서는 고인에 대한 상세 정보를 입력해주세요.\n\n' +
          '(5개 항목을 모두 입력해야 합니다)\n\n' +
          '메모리얼 정보 입력 페이지로 이동하시겠습니까?'
      );

      if (confirmed) {
        // 가족 구성원용 메모리얼 정보 입력 페이지로 이동
        window.location.href = `/mobile/memorial/family-info/${memorial.memorialId}`;
      }
      return;
    }

    // 3. AI 학습 완료 확인 (가족 구성원)
    if (!memorial.aiTrainingCompleted) {
      alert('영상통화 준비를 위한 학습 중입니다. 잠시만 기다려주세요.');
      return;
    }

    // 가족 구성원 - 모든 조건 만족 시 영상통화 시작
    startVideoCall(memorial.memorialId);

  } catch (error) {
    console.error('영상통화 가능 여부 확인 중 오류:', error);
    alert('영상통화 확인 중 오류가 발생했습니다.');
  }
}

async function confirmVideoCallStart(memorial) {
  console.log('영상통화 시작 최종 확인:', memorial);

  const memorialName = memorial.name || '메모리얼';
  const accessType = memorial.isOwner ? '소유자' : '가족 구성원';

  const confirmed = confirm(
      `${memorialName}님과 영상통화를 시작하시겠습니까?\n\n` +
      `접근 권한: ${accessType}\n` +
      `관계: ${memorial.getDisplayRelationship?.() || memorial.relationshipDescription}`
  );

  if (confirmed) {
    startVideoCall(memorial.memorialId);
  }
}

/**
 * 메모리얼 선택 UI 업데이트
 */
function updateMemorialSelection() {
  console.log('메모리얼 선택 UI 업데이트');

  const memorialItems = document.querySelectorAll('.memorial-item');

  memorialItems.forEach(item => {
    const memorialId = parseInt(item.dataset.memorialId);

    if (memorialId === mainPageState.selectedMemorialId) {
      item.classList.add('selected');
      console.log('메모리얼 선택 표시:', memorialId);
    } else {
      item.classList.remove('selected');
    }
  });
}

/**
 * 영상통화 버튼 상태 업데이트
 */
function updateVideoCallButtonState() {
  console.log('영상통화 버튼 상태 업데이트');

  const videoCallBtn = document.querySelector('.video-call-btn');
  if (!videoCallBtn) return;

  const hasMemorials = mainPageState.memorialItems.length > 0;
  const hasSelection = mainPageState.selectedMemorialId !== null;
  const isMultipleMemorials = mainPageState.memorialItems.length > 1;

  // 메모리얼이 없으면 비활성화
  if (!hasMemorials) {
    videoCallBtn.disabled = true;
    videoCallBtn.innerHTML = '<i class="fas fa-video"></i> 영상통화';
    return;
  }

  // 선택된 메모리얼 정보 가져오기
  let selectedMemorial = null;
  if (!isMultipleMemorials) {
    selectedMemorial = mainPageState.memorialItems[0];
  } else if (hasSelection) {
    selectedMemorial = mainPageState.memorialItems.find(
        item => item.memorialId === mainPageState.selectedMemorialId
    );
  }

  // 버튼 텍스트 업데이트
  if (selectedMemorial) {
    const blockReason = getVideoCallBlockReason(selectedMemorial);
    if (blockReason) {
      videoCallBtn.disabled = false; // 클릭은 가능 (설명을 위해)
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

/**
 * 영상통화 차단 사유 반환 (새로 추가)
 */
function getVideoCallBlockReason(memorial) {
  // 프로필 이미지 확인
  if (!memorial.hasRequiredProfileImages) {
    return '프로필 사진을 등록해주세요.';
  }

  // 가족 구성원인 경우 고인 상세 정보 확인
  if (!memorial.isOwner && !memorial.hasRequiredDeceasedInfo) {
    return '고인에 대한 상세 정보를 입력해주세요.';
  }

  // AI 학습 상태 확인
  if (!memorial.aiTrainingCompleted) {
    return 'AI 학습이 완료되지 않았습니다.';
  }

  // 접근 권한 확인
  if (!memorial.canAccess) {
    return '접근 권한이 없습니다.';
  }

  // 영상통화 권한 확인
  if (!memorial.isOwner && !memorial.canVideoCall) {
    return '영상통화 권한이 없습니다.';
  }

  return null;
}


/**
 * 짧은 차단 사유 텍스트 반환
 */
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

    // API 호출
    const data = await authFetch('/api/memorial/my?size=5');

    console.log('API 응답:', data);

    // 응답 구조에 따른 성공/실패 판단
    if (data.status?.code === 'OK_0000') {
      mainPageState.memorialItems = data.response?.data || [];

      if (mainPageState.memorialItems.length > 0) {
        renderMemorialList(mainPageState.memorialItems);
      } else {
        showEmptyState();
      }

      // 선택 상태 초기화
      mainPageState.selectedMemorialId = null;
      updateVideoCallButtonState();

      // 재시도 카운트 리셋
      mainPageState.retryCount = 0;

    } else {
      // 서버에서 실패 응답을 보낸 경우
      const errorMessage = data.status?.message || '메모리얼 목록 로드 실패';
      console.error('서버 응답 오류:', data);
      throw new Error(errorMessage);
    }

  } catch (error) {
    console.error('메모리얼 목록 로드 실패:', error);

    // 재시도 로직
    mainPageState.retryCount++;

    if (mainPageState.retryCount < mainPageState.maxRetries) {
      console.log(`재시도 ${mainPageState.retryCount}/${mainPageState.maxRetries}`);
      setTimeout(() => loadMemorialList(), 1000 * mainPageState.retryCount);
    } else {
      // 최대 재시도 초과 시 에러 상태 표시
      const errorMessage = error.name === 'FetchError' ?
          error.statusMessage :
          (error.message || '메모리얼 목록을 불러올 수 없습니다.');

      showErrorState(errorMessage);
    }

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
  updateVideoCallButtonState();
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

  // 상태 표시 아이콘 생성
  const statusIconsHtml = createStatusIconsHtml(memorial);

  item.innerHTML = `
    ${avatarHtml}
    <div class="memorial-info">
      <div class="memorial-name">
        ${memorial.name}
        ${memorial.isOwner ? '' : '<span class="family-member-badge">가족</span>'}
      </div>
      <div class="memorial-relationship">${memorial.getDisplayRelationship?.() || memorial.relationshipDescription}</div>
      <div class="memorial-meta">
        <span class="access-type">${memorial.getAccessStatusText?.() || (memorial.isOwner ? '소유자' : '가족 구성원')}</span>
        <span class="separator">•</span>
        <span class="age">${memorial.formattedAge}</span>
      </div>
    </div>
    <div class="memorial-status">
      ${statusIconsHtml}
      <div class="memorial-arrow">
        <i class="fas fa-chevron-right"></i>
        <i class="fas fa-check-circle selection-icon" style="display: none;"></i>
      </div>
    </div>
  `;

  return item;
}

/**
 * 상태 표시 아이콘 HTML 생성
 */
function createStatusIconsHtml(memorial) {
  const statusIcons = [];

  // 프로필 이미지 상태
  if (memorial.hasRequiredProfileImages) {
    statusIcons.push('<i class="fas fa-image status-icon status-ok" title="프로필 사진 등록 완료"></i>');
  } else {
    statusIcons.push('<i class="fas fa-image status-icon status-warning" title="프로필 사진 필요"></i>');
  }

  // 가족 구성원인 경우 고인 상세 정보 상태 추가
  if (!memorial.isOwner) {
    if (memorial.hasRequiredDeceasedInfo) {
      statusIcons.push('<i class="fas fa-heart status-icon status-ok" title="고인 상세 정보 입력 완료"></i>');
    } else {
      statusIcons.push('<i class="fas fa-heart status-icon status-warning" title="고인 상세 정보 입력 필요"></i>');
    }
  }

  // AI 학습 상태
  if (memorial.aiTrainingCompleted) {
    statusIcons.push('<i class="fas fa-brain status-icon status-ok" title="AI 학습 완료"></i>');
  } else {
    statusIcons.push('<i class="fas fa-brain status-icon status-warning" title="AI 학습 중"></i>');
  }

  return `
    <div class="status-indicators">
      ${statusIcons.join('')}
    </div>
  `;
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

  // 영상통화 페이지로 이동
  window.location.href = `/mobile/videocall/${memorialId}`;
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

  if (errorState) {
    errorState.style.display = 'block';
    errorState.classList.remove('d-none');
  }

  if (errorMessage) {
    errorMessage.textContent = message;
  }

  hideLoadingState();
  hideEmptyState();

  console.log('에러 상태 표시:', message);
}

function hideErrorState() {
  const errorState = document.getElementById('errorState');
  if (errorState) {
    errorState.style.display = 'none';
    errorState.classList.add('d-none');
  }
}

function showLoginModal() {
  const confirmLogin = confirm('이 기능을 사용하려면 로그인이 필요합니다.\n\n로그인 페이지로 이동하시겠습니까?');
  if (confirmLogin) {
    window.location.href = '/mobile/login';
  }
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
  mainPageState.selectedMemorialId = null;
}

/**
 * 전역 함수들 (하위 호환성)
 */
window.mainPageManager = {
  initialize: initializeMainPage,
  destroy: destroyMainPage,
  getState: () => mainPageState,
  selectMemorial: (memorialId) => {
    mainPageState.selectedMemorialId = memorialId;
    updateMemorialSelection();
    updateVideoCallButtonState();
  },
  // 초대 토큰 처리 함수 추가
  processInviteToken: checkAndProcessInviteToken,
  validateInviteToken: validateInviteToken
};

// 전역 함수들 (HTML에서 호출 가능)
window.handleCreateMemorial = handleCreateMemorialClick;
window.showVideoCall = handleVideoCallClick;
window.showLoginModal = showLoginModal;
window.processInviteToken = checkAndProcessInviteToken; // 초대 토큰 처리 함수 전역 등록

/**
 * 자동 초기화
 */
console.log('초대 토큰 처리 기능이 추가된 main.js 로드 완료');

// DOM이 준비되면 즉시 초기화
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initializeMainPage);
} else {
  setTimeout(initializeMainPage, 100);
}

// 스타일 동적 추가
if (!document.getElementById('family-member-styles')) {
  const styleSheet = document.createElement('style');
  styleSheet.id = 'family-member-styles';
  styleSheet.textContent = familyMemberBadgeStyle;
  document.head.appendChild(styleSheet);
}


// 페이지 언로드 시 정리
window.addEventListener('beforeunload', destroyMainPage);

// 모듈 익스포트
export {
  initializeMainPage,
  destroyMainPage,
  handleCreateMemorialClick,
  handleVideoCallClick,
  updateMemorialSelection,
  updateVideoCallButtonState,
  checkAndProcessInviteToken,
  validateInviteToken
};