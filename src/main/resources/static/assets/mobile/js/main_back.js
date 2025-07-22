// main.js - 토마토리멤버 메인 페이지 (정리된 완전본)

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
  inviteProcessing: false
};

// 체험하기 플로팅 버튼 상태 관리 (채팅보다 위)
let experienceFabState = {
  isExpanded: false,
  fab: null,
  options: null,
  isHigherThanChat: true // 채팅보다 위에 위치
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
 * ===== 메인 초기화 함수 =====
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

    // 2. 초대 토큰 처리
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

    requestAnimationFrame(() => {
      initializeExperienceFab();
    });

    // 6. 초기화 완료
    mainPageState.isInitialized = true;
    console.log('✅ 메인 페이지 초기화 완료');

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
 * ===== 초대 토큰 처리 =====
 */
async function checkAndProcessInviteToken() {
  console.log('🎫 초대 토큰 확인 시작');

  if (mainPageState.inviteProcessing) {
    console.log('⚠️ 초대 처리가 이미 진행 중입니다.');
    return;
  }

  try {
    const inviteToken = sessionStorage.getItem('inviteToken');

    if (!inviteToken) {
      console.log('📭 초대 토큰이 없습니다.');
      return;
    }

    console.log('🎫 초대 토큰 발견:', inviteToken.substring(0, 8) + '...');

    mainPageState.inviteProcessing = true;
    const loading = showLoading('초대 처리 중...');

    const response = await authFetch('/api/family/invite/process', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: inviteToken })
    });

    if (response.status?.code === 'OK_0000') {
      const result = response.response;
      showToast(result.message || '초대 처리가 완료되었습니다.', 'success');
      sessionStorage.removeItem('inviteToken');

      setTimeout(() => {
        window.location.reload();
      }, 2000);

      console.log('✅ 초대 처리 완료');
    } else {
      const errorMessage = response.status?.message || '초대 처리에 실패했습니다.';
      console.error('❌ 초대 처리 실패:', errorMessage);
      showToast(errorMessage, 'error');
      sessionStorage.removeItem('inviteToken');
    }

  } catch (error) {
    console.error('❌ 초대 토큰 처리 중 오류:', error);
    const errorMessage = error.name === 'FetchError' ?
        error.statusMessage : '초대 처리 중 오류가 발생했습니다.';
    showToast(errorMessage, 'error');
    sessionStorage.removeItem('inviteToken');
  } finally {
    hideLoading();
    mainPageState.inviteProcessing = false;
  }
}

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
 * ===== UI 업데이트 =====
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
 * ===== 로그인한 사용자 기능 초기화 =====
 */
async function initializeLoggedInFeatures() {
  console.log('👤 로그인한 사용자 기능 초기화');

  try {
    if (mainPageState.memorialItems.length > 0) {
      renderMemorialList(mainPageState.memorialItems);
    }

    updateVideoCallButtonState();


  } catch (error) {
    console.error('❌ 로그인한 사용자 기능 초기화 실패:', error);
  }
}

/**
 * ===== 체험하기 플로팅 버튼 관리 (우선순위) =====
 */
function initializeExperienceFab() {
  console.log('🚀 체험하기 플로팅 버튼 초기화 (우선순위)');

  // 기존 버튼들 제거
  removeExistingExperienceFab();

  // 플로팅 버튼 생성 (채팅보다 위)
  createExperienceFabHigher();

  // 확장 옵션 생성
  createExperienceOptions();

  // 이벤트 바인딩
  bindExperienceFabEvents();

  console.log('✅ 체험하기 플로팅 버튼 초기화 완료 (우선위치)');
}

function removeExistingExperienceFab() {
  const existingFab = document.querySelector('.experience-fab');
  const existingOptions = document.querySelector('.experience-options');

  if (existingFab) {
    existingFab.remove();
    console.log('🗑️ 기존 체험하기 FAB 제거됨');
  }
  if (existingOptions) {
    existingOptions.remove();
    console.log('🗑️ 기존 체험하기 옵션 제거됨');
  }
}

function createExperienceFabHigher() {
  const fab = document.createElement('button');
  fab.className = 'experience-fab';
  fab.id = 'experienceFab';
  fab.setAttribute('aria-label', '체험하기');
  fab.setAttribute('title', '체험하기');
  fab.innerHTML = '<i class="fas fa-play"></i>';

  // 강제 위치 설정 (채팅보다 위)
  const deviceType = getDeviceType();
  let bottomPosition;

  switch (deviceType) {
    case 'desktop':
      bottomPosition = '130px'; // 데스크톱에서 바닥에서 130px (50px 줄임)
      break;
    case 'tablet':
      bottomPosition = 'calc(var(--nav-height-mobile, 80px) + 110px)'; // 태블릿에서 네비 위 110px (50px 줄임)
      break;
    case 'mobile':
    default:
      bottomPosition = 'calc(var(--nav-height-mobile, 80px) + 100px)'; // 모바일에서 네비 위 100px (50px 줄임)
      break;
  }

  // 강제 스타일 적용
  fab.style.cssText = `
    position: fixed !important;
    bottom: ${bottomPosition} !important;
    right: 20px !important;
    width: 56px !important;
    height: 56px !important;
    background: linear-gradient(135deg, #ff6b6b 0%, #ffa500 100%) !important;
    border-radius: 50% !important;
    border: none !important;
    color: white !important;
    font-size: 24px !important;
    cursor: pointer !important;
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3) !important;
    z-index: 1027 !important;
    transition: all 0.3s ease !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    visibility: visible !important;
    opacity: 1 !important;
    transform: none !important;
    margin: 0 !important;
    padding: 0 !important;
  `;

  // DOM에 추가
  document.body.appendChild(fab);
  experienceFabState.fab = fab;

  console.log('🚀 체험하기 플로팅 버튼 생성 완료 (위치 우선)');
}

function createExperienceOptions() {
  // 기존 옵션 제거
  const existingOptions = document.querySelector('.experience-options');
  if (existingOptions) {
    existingOptions.remove();
    console.log('🗑️ 기존 확장 옵션 제거');
  }

  const options = document.createElement('div');
  options.className = 'experience-options';
  options.id = 'experienceOptions';

  const deviceType = getDeviceType();
  let bottomPosition;

  switch (deviceType) {
    case 'desktop':
      bottomPosition = '210px';
      break;
    case 'tablet':
      bottomPosition = 'calc(var(--nav-height-mobile, 80px) + 180px)';
      break;
    case 'mobile':
    default:
      bottomPosition = 'calc(var(--nav-height-mobile, 80px) + 170px)';
      break;
  }

  // 기본 스타일 설정
  options.style.cssText = `
    position: fixed !important;
    bottom: ${bottomPosition} !important;
    right: 20px !important;
    z-index: 1026 !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
    opacity: 0 !important;
    visibility: hidden !important;
    transform: translateY(20px) !important;
    transition: all 0.3s ease !important;
    pointer-events: none !important;
  `;

  // HTML 콘텐츠 생성
  options.innerHTML = `
    <a href="/call/rohmoohyun" class="experience-option-btn" data-name="노무현 체험">
      <img src="/images/roh.png" alt="노무현" 
           onerror="this.style.display='none';" 
           style="width: 32px; height: 32px; border-radius: 50%; object-fit: cover; flex-shrink: 0;">
      <span>노무현 체험</span>
    </a>
    <a href="/call/kimgeuntae" class="experience-option-btn" data-name="김근태 체험">
      <img src="/images/kkt.png" alt="김근태" 
           onerror="this.style.display='none';" 
           style="width: 32px; height: 32px; border-radius: 50%; object-fit: cover; flex-shrink: 0;">
      <span>김근태 체험</span>
    </a>
  `;

  // DOM에 즉시 추가
  document.body.appendChild(options);

  // 생성 직후 강제 스타일 적용
  requestAnimationFrame(() => {
    // 확장 버튼들에 스타일 강제 적용
    const optionBtns = options.querySelectorAll('.experience-option-btn');
    console.log('🎯 확장 옵션 버튼 개수:', optionBtns.length);

    optionBtns.forEach((btn, index) => {
      btn.style.cssText = `
        display: flex !important;
        align-items: center !important;
        gap: 12px !important;
        padding: 12px 16px !important;
        background: white !important;
        border: 1px solid #e2e8f0 !important;
        border-radius: 28px !important;
        color: #333 !important;
        font-size: 14px !important;
        font-weight: 600 !important;
        cursor: pointer !important;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
        transition: all 0.3s ease !important;
        text-decoration: none !important;
        min-width: 140px !important;
        white-space: nowrap !important;
        visibility: visible !important;
        opacity: 1 !important;
      `;

      console.log(`  확장 버튼 ${index + 1} 스타일 적용:`, btn.textContent.trim());

      // 호버 이벤트 직접 바인딩
      btn.addEventListener('mouseenter', function() {
        this.style.transform = 'translateX(-8px) !important';
        this.style.boxShadow = '0 6px 20px rgba(0, 0, 0, 0.2) !important';
      });

      btn.addEventListener('mouseleave', function() {
        this.style.transform = 'translateX(0) !important';
        this.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.15) !important';
      });
    });
  });

  experienceFabState.options = options;

  console.log('✅ 확장 옵션들 생성 및 강제 스타일 적용 완료');

  // 생성 확인을 위한 디버그
  setTimeout(() => {
    const createdOptions = document.querySelector('.experience-options');
    const createdBtns = document.querySelectorAll('.experience-option-btn');
    console.log('🔍 생성 확인:', {
      옵션컨테이너: !!createdOptions,
      버튼개수: createdBtns.length,
      컨테이너위치: createdOptions ? createdOptions.style.bottom : 'N/A'
    });
  }, 100);
}

function getDeviceType() {
  const width = window.innerWidth;
  if (width < 768) return 'mobile';
  if (width < 1024) return 'tablet';
  return 'desktop';
}

function bindExperienceFabEvents() {
  if (!experienceFabState.fab) {
    console.error('❌ 체험하기 FAB 버튼을 찾을 수 없어 이벤트 바인딩을 건너뜁니다.');
    return;
  }

  // 메인 플로팅 버튼 클릭 이벤트
  experienceFabState.fab.removeEventListener('click', handleExperienceFabClick);
  experienceFabState.fab.addEventListener('click', handleExperienceFabClick);

  // 확장 옵션이 생성되었는지 확인하고 이벤트 바인딩
  const bindOptionEvents = () => {
    if (experienceFabState.options) {
      const optionBtns = experienceFabState.options.querySelectorAll('.experience-option-btn');
      console.log('🔗 확장 옵션 버튼 개수:', optionBtns.length);

      optionBtns.forEach((btn, index) => {
        btn.removeEventListener('click', handleExperienceOptionClick);
        btn.addEventListener('click', handleExperienceOptionClick);
        console.log(`  버튼 ${index + 1} 이벤트 바인딩:`, btn.textContent.trim());
      });

      // 외부 클릭시 닫기
      if (!document.experienceOutsideClickBound) {
        document.addEventListener('click', handleOutsideClick);
        document.experienceOutsideClickBound = true;
      }
    }
  };

  // 옵션이 이미 생성된 경우 즉시 바인딩
  bindOptionEvents();

  // 옵션이 나중에 생성될 수 있으므로 MutationObserver 사용
  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.type === 'childList') {
        mutation.addedNodes.forEach((node) => {
          if (node.nodeType === 1 && node.classList && node.classList.contains('experience-options')) {
            console.log('🔍 MutationObserver: 확장 옵션이 DOM에 추가됨');
            setTimeout(bindOptionEvents, 50);
          }
        });
      }
    });
  });

  observer.observe(document.body, {
    childList: true,
    subtree: false
  });

  // 윈도우 리사이즈 시 위치 재조정
  if (!window.experienceResizeListenerBound) {
    window.addEventListener('resize', () => {
      setTimeout(() => {
        repositionExperienceFab();
      }, 300);
    });
    window.experienceResizeListenerBound = true;
  }

  console.log('🔗 체험하기 버튼 이벤트 바인딩 완료');
}

function repositionExperienceFab() {
  if (!experienceFabState.fab || !experienceFabState.options) return;

  const deviceType = getDeviceType();
  let fabBottom, optionsBottom, fabRight, fabSize, fontSize;

  switch (deviceType) {
    case 'desktop':
      fabBottom = '130px';
      optionsBottom = '210px';
      fabRight = '40px';
      fabSize = '64px';
      fontSize = '28px';
      break;
    case 'tablet':
      fabBottom = 'calc(var(--nav-height-mobile, 80px) + 110px)';
      optionsBottom = 'calc(var(--nav-height-mobile, 80px) + 180px)';
      fabRight = '30px';
      fabSize = '60px';
      fontSize = '26px';
      break;
    case 'mobile':
    default:
      fabBottom = 'calc(var(--nav-height-mobile, 80px) + 100px)';
      optionsBottom = 'calc(var(--nav-height-mobile, 80px) + 170px)';
      fabRight = window.innerWidth <= 375 ? '16px' : '20px';
      fabSize = window.innerWidth <= 375 ? '50px' : '56px';
      fontSize = window.innerWidth <= 375 ? '22px' : '24px';
      break;
  }

  // FAB 위치 재조정
  experienceFabState.fab.style.bottom = fabBottom;
  experienceFabState.fab.style.right = fabRight;
  experienceFabState.fab.style.width = fabSize;
  experienceFabState.fab.style.height = fabSize;
  experienceFabState.fab.style.fontSize = fontSize;

  // 옵션들 위치 재조정
  experienceFabState.options.style.bottom = optionsBottom;
  experienceFabState.options.style.right = fabRight;

  console.log('📱 체험하기 버튼 위치 재조정:', deviceType);
}

/**
 * ===== 이벤트 핸들러들 =====
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

// 체험하기 플로팅 버튼 클릭
function handleExperienceFabClick(e) {
  e.preventDefault();
  e.stopPropagation();

  console.log('🚀 체험하기 플로팅 버튼 클릭', {
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
    console.error('❌ 체험하기 버튼 또는 옵션이 없어 열 수 없습니다.');
    return;
  }

  experienceFabState.isExpanded = true;

  // 아이콘 변경 (플레이 → X)
  experienceFabState.fab.innerHTML = '<i class="fas fa-times"></i>';
  experienceFabState.fab.classList.add('expanded');
  experienceFabState.fab.style.background = '#dc3545 !important';

  // ★ 여기서 show 클래스를 추가합니다
  experienceFabState.options.classList.add('show');

  // 확장 옵션 표시
  experienceFabState.options.style.opacity = '1';
  experienceFabState.options.style.visibility = 'visible';
  experienceFabState.options.style.transform = 'translateY(0)';
  experienceFabState.options.style.pointerEvents = 'all';

  // 접근성
  experienceFabState.fab.setAttribute('aria-expanded', 'true');
  experienceFabState.fab.setAttribute('aria-label', '체험 옵션 닫기');

  console.log('🎯 확장 옵션 열림');
}

function closeExperienceOptions() {
  if (!experienceFabState.fab || !experienceFabState.options) {
    console.error('❌ 체험하기 버튼 또는 옵션이 없어 닫을 수 없습니다.');
    return;
  }

  experienceFabState.isExpanded = false;

  // 아이콘 변경 (X → 플레이)
  experienceFabState.fab.innerHTML = '<i class="fas fa-play"></i>';
  experienceFabState.fab.classList.remove('expanded');
  experienceFabState.fab.style.background = 'linear-gradient(135deg, #ff6b6b 0%, #ffa500 100%) !important';

  // ★ 여기서 show 클래스를 제거합니다
  experienceFabState.options.classList.remove('show');

  // 확장 옵션 숨김
  experienceFabState.options.style.opacity = '0';
  experienceFabState.options.style.visibility = 'hidden';
  experienceFabState.options.style.transform = 'translateY(20px)';
  experienceFabState.options.style.pointerEvents = 'none';

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

// 외부 클릭시 닫기
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

  if (mainPageState.isLoggedIn) {
    mainPageState.retryCount = 0;
    await loadMemorialList();
  } else {
    window.location.reload();
  }
}

// 재시도 클릭
async function handleRetryClick(e) {
  e.preventDefault();
  console.log('🔄 재시도 클릭');

  hideErrorState();
  mainPageState.retryCount = 0;

  if (mainPageState.isLoggedIn) {
    await loadMemorialList();
  } else {
    window.location.reload();
  }
}

/**
 * ===== 영상통화 가능 여부 확인 =====
 */
async function checkVideoCallAvailability(memorial) {
  console.log('🔍 영상통화 가능 여부 확인:', memorial);

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

/**
 * ===== 데이터 로드 및 렌더링 =====
 */
function renderMemorialList(memorials) {
  console.log('🎨 메모리얼 목록 렌더링:', memorials.length);

  const container = document.getElementById('memorialList');
  if (!container) {
    console.log('📋 메모리얼 리스트 컨테이너를 찾을 수 없음 - 서버에서 이미 렌더링됨');
    return;
  }

  bindMemorialItems();
  updateVideoCallButtonState();
  console.log('✅ 메모리얼 목록 이벤트 바인딩 완료');
}

function startVideoCall(memorialId) {
  console.log('📹 영상통화 시작:', memorialId);
  window.location.href = `/mobile/videocall/${memorialId}`;
}

/**
 * ===== 유틸리티 함수들 =====
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

  console.log('❌ 에러 상태 표시:', message);
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
 * ===== 정리 함수 =====
 */
function cleanupExperienceFab() {
  console.log('🧹 체험하기 버튼 정리 시작');

  removeExistingExperienceFab();

  if (document.experienceOutsideClickBound) {
    document.removeEventListener('click', handleOutsideClick);
    document.experienceOutsideClickBound = false;
  }

  experienceFabState = {
    isExpanded: false,
    fab: null,
    options: null,
    isHigherThanChat: true
  };

  console.log('✅ 체험하기 버튼 정리 완료');
}

function destroyMainPage() {
  console.log('🧹 메인 페이지 정리');

  if (mainPageState.refreshInterval) {
    clearInterval(mainPageState.refreshInterval);
    mainPageState.refreshInterval = null;
  }

  cleanupExperienceFab();

  mainPageState.isInitialized = false;
  mainPageState.selectedMemorialId = null;
}

// 전역 디버깅 함수 추가
window.debugExperienceFab = function() {
  console.group('🔍 체험하기 FAB 디버그');

  const fab = document.querySelector('.experience-fab');
  const options = document.querySelector('.experience-options');
  const optionBtns = document.querySelectorAll('.experience-option-btn');

  console.log('FAB 버튼:', !!fab);
  console.log('옵션 컨테이너:', !!options);
  console.log('옵션 버튼 개수:', optionBtns.length);

  if (options) {
    console.log('옵션 컨테이너 스타일:', {
      display: options.style.display,
      visibility: options.style.visibility,
      opacity: options.style.opacity,
      transform: options.style.transform,
      bottom: options.style.bottom,
      right: options.style.right,
      zIndex: options.style.zIndex
    });
  }

  optionBtns.forEach((btn, index) => {
    const computedStyle = getComputedStyle(btn);
    console.log(`버튼 ${index + 1} (${btn.textContent.trim()}):`, {
      display: computedStyle.display,
      visibility: computedStyle.visibility,
      opacity: computedStyle.opacity,
      position: computedStyle.position
    });
  });

  console.log('experienceFabState:', experienceFabState);
  console.groupEnd();
};

// 강제로 확장 옵션 생성하는 함수
window.forceCreateExperienceOptions = function() {
  console.log('🔧 강제로 확장 옵션 생성');
  if (experienceFabState.fab) {
    createExperienceOptions();
    setTimeout(() => {
      const optionBtns = document.querySelectorAll('.experience-option-btn');
      console.log('🎯 강제 생성된 옵션 버튼:', optionBtns.length, '개');

      // 이벤트도 다시 바인딩
      optionBtns.forEach(btn => {
        btn.removeEventListener('click', handleExperienceOptionClick);
        btn.addEventListener('click', handleExperienceOptionClick);
      });
    }, 100);
  }
};
window.mainPageManager = {
  initialize: initializeMainPage,
  destroy: destroyMainPage,
  getState: () => mainPageState,
  selectMemorial: (memorialId) => {
    mainPageState.selectedMemorialId = memorialId;
    updateMemorialSelection();
    updateVideoCallButtonState();
  },
  processInviteToken: checkAndProcessInviteToken,
  toggleExperience: () => {
    if (experienceFabState.isExpanded) {
      closeExperienceOptions();
    } else {
      openExperienceOptions();
    }
  },
  openExperience: openExperienceOptions,
  closeExperience: closeExperienceOptions,
  repositionFab: repositionExperienceFab
};

// HTML에서 호출 가능한 전역 함수들
window.handleCreateMemorial = handleCreateMemorialClick;
window.showVideoCall = handleVideoCallClick;
window.showLoginModal = showLoginModal;
window.processInviteToken = checkAndProcessInviteToken;
window.forceCreateExperienceFab = function() {
  console.log('🔧 강제 체험하기 버튼 생성');
  mainPageState.isLoggedIn = true;
  initializeExperienceFab();
};

/**
 * ===== 자동 초기화 =====
 */
console.log('🎉 토마토리멤버 main.js 로드 완료 (체험하기 우선위치)');

// DOM이 준비되면 초기화
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
  handleVideoCallClick,
  updateMemorialSelection,
  updateVideoCallButtonState,
  checkAndProcessInviteToken,
  initializeExperienceFab,
  repositionExperienceFab
};