// layout.js - 토마토리멤버 완전 반응형 레이아웃 관리 (최종 완성본)

import { showToast, showConfirm } from './common.js';
import { memberLogout } from './commonFetch.js';

// 레이아웃 상태 관리
let layoutState = {
  isMenuOpen: false,
  lastScrollTop: 0,
  isScrolling: false,
  menuElement: null,
  menuButton: null,
  overlay: null,
  isInitialized: false,
  navigationBadges: {},
  scrollTimeout: null,
  currentDevice: 'mobile',
  resizeTimeout: null,
  spacingMonitor: null,
  contentMonitor: null
};

// 디바이스 브레이크포인트
const BREAKPOINTS = {
  MOBILE: 768,
  TABLET: 1024,
  DESKTOP: 1200
};

/**
 * 🔧 레이아웃 간격 문제 진단
 */
function diagnoseLayoutSpacing() {
  const elements = {
    layout: document.querySelector('.mobile-layout'),
    header: document.querySelector('.mobile-header'),
    main: document.querySelector('.mobile-main'),
    appNav: document.querySelector('.mobile-app-nav'),
    container: document.querySelector('.container')
  };

  if (elements.main && elements.appNav) {
    const mainRect = elements.main.getBoundingClientRect();
    const navRect = elements.appNav.getBoundingClientRect();
    const gap = navRect.top - mainRect.bottom;

    if (gap > 5) {
      console.warn(`⚠️ 앱 네비게이션 간격이 너무 큽니다: ${gap}px`);
      return gap;
    } else if (gap < 0) {
      console.warn(`⚠️ 요소가 겹치고 있습니다: ${gap}px`);
      return gap;
    }
  }

  return null;
}

/**
 * 🔧 콘텐츠 잘림 문제 진단
 */
function diagnoseContentOverlap() {
  const elements = {
    header: document.querySelector('.mobile-header'),
    main: document.querySelector('.mobile-main'),
    container: document.querySelector('.container'),
    appNav: document.querySelector('.mobile-app-nav'),
    chatFab: document.querySelector('.chat-fab')
  };

  const issues = [];

  // 헤더와 메인 콘텐츠 겹침 체크
  if (elements.header && elements.main) {
    const headerRect = elements.header.getBoundingClientRect();
    const mainRect = elements.main.getBoundingClientRect();

    if (headerRect.bottom > mainRect.top) {
      const overlap = headerRect.bottom - mainRect.top;
      issues.push({
        type: 'header-content-overlap',
        overlap: overlap,
        message: `헤더가 콘텐츠와 ${overlap}px 겹침`
      });
    }
  }

  // 앱 네비게이션과 콘텐츠 겹침 체크
  if (elements.appNav && elements.main) {
    const navRect = elements.appNav.getBoundingClientRect();
    const mainRect = elements.main.getBoundingClientRect();

    if (navRect.top < mainRect.bottom) {
      const overlap = mainRect.bottom - navRect.top;
      issues.push({
        type: 'nav-content-overlap',
        overlap: overlap,
        message: `앱 네비게이션이 콘텐츠와 ${overlap}px 겹침`
      });
    }
  }

  // 채팅 FAB 표시 여부 체크
  if (elements.chatFab) {
    const fabStyles = getComputedStyle(elements.chatFab);
    const isVisible = fabStyles.display !== 'none' &&
        fabStyles.visibility !== 'hidden' &&
        fabStyles.opacity !== '0';

    if (!isVisible) {
      issues.push({
        type: 'chat-fab-hidden',
        message: '채팅 FAB 버튼이 숨겨져 있음'
      });
    }

    if (elements.appNav && isVisible) {
      const fabRect = elements.chatFab.getBoundingClientRect();
      const navRect = elements.appNav.getBoundingClientRect();

      if (fabRect.bottom > navRect.top && fabRect.top < navRect.bottom) {
        issues.push({
          type: 'chat-fab-nav-overlap',
          message: '채팅 FAB가 앱 네비게이션과 겹침'
        });
      }
    }
  } else {
    issues.push({
      type: 'chat-fab-missing',
      message: '채팅 FAB 버튼 요소가 없음'
    });
  }

  return issues;
}

/**
 * 🔧 간격 문제 자동 수정
 */
function fixLayoutSpacing() {
  const appNav = document.querySelector('.mobile-app-nav');
  const main = document.querySelector('.mobile-main');
  const layout = document.querySelector('.mobile-layout');

  if (!appNav) return false;

  try {
    const forceBottomStyles = `
      position: fixed !important;
      bottom: 0 !important;
      left: 0 !important;
      right: 0 !important;
      width: 100% !important;
      max-width: none !important;
      margin: 0 !important;
      transform: none !important;
      z-index: 1020 !important;
    `;

    appNav.style.cssText += forceBottomStyles;

    if (main) {
      main.style.marginBottom = '0';
      main.style.paddingBottom = '0';
    }

    if (layout) {
      layout.style.marginBottom = '0';
      layout.style.paddingBottom = 'var(--nav-height-mobile, 80px)';
    }

    const container = document.querySelector('.container');
    if (container) {
      container.style.marginBottom = '0';
      container.style.paddingBottom = '0';

      const lastChild = container.lastElementChild;
      if (lastChild) {
        lastChild.style.marginBottom = '20px';
      }
    }

    console.log('✅ 레이아웃 간격 수정 완료');
    return true;

  } catch (error) {
    console.error('❌ 레이아웃 간격 수정 실패:', error);
    return false;
  }
}

/**
 * 🔧 콘텐츠 레이아웃 자동 수정
 */
function fixContentLayout() {
  try {
    const layout = document.querySelector('.mobile-layout');
    const main = document.querySelector('.mobile-main');
    const container = document.querySelector('.container');
    const header = document.querySelector('.mobile-header');
    const appNav = document.querySelector('.mobile-app-nav');

    if (!layout || !main) {
      console.error('❌ 필수 레이아웃 요소를 찾을 수 없습니다.');
      return false;
    }

    // 레이아웃 패딩 조정
    const headerHeight = header ? header.offsetHeight : 70;
    const navHeight = appNav ? appNav.offsetHeight : 80;

    layout.style.paddingTop = `${headerHeight + 10}px`;
    layout.style.paddingBottom = `${navHeight + 10}px`;

    // 메인 콘텐츠 여백 조정
    main.style.padding = '15px 0 20px 0';
    main.style.overflowY = 'auto';

    // 컨테이너 하단 여백 확보
    if (container) {
      container.style.paddingBottom = '20px';

      const lastChild = container.lastElementChild;
      if (lastChild) {
        lastChild.style.marginBottom = '40px';
      }
    }

    console.log('✅ 콘텐츠 레이아웃 수정 완료');
    return true;

  } catch (error) {
    console.error('❌ 콘텐츠 레이아웃 수정 실패:', error);
    return false;
  }
}

/**
 * 🔧 채팅 FAB 버튼 생성
 */
function createChatFab() {
  try {
    // 로그인 상태 확인
    const isLoggedIn = window.APP_CONFIG?.isLoggedIn ||
        window.serverData?.isLoggedIn ||
        !!localStorage.getItem('accessToken');

    if (!isLoggedIn) {
      console.log('📝 로그인하지 않은 상태이므로 채팅 FAB을 생성하지 않습니다.');
      return null;
    }

    const chatFab = document.createElement('button');
    chatFab.className = 'chat-fab';
    chatFab.setAttribute('aria-label', '채팅 열기');
    chatFab.innerHTML = '<i class="fas fa-comments"></i>';

    document.body.appendChild(chatFab);

    console.log('✅ 채팅 FAB 버튼 생성 완료');
    return chatFab;

  } catch (error) {
    console.error('❌ 채팅 FAB 버튼 생성 실패:', error);
    return null;
  }
}

/**
 * 🔧 채팅 FAB 버튼 표시 및 위치 조정
 */
function fixChatFab() {
  try {
    let chatFab = document.querySelector('.chat-fab');

    // 채팅 FAB이 없으면 생성
    if (!chatFab) {
      console.log('💬 채팅 FAB 버튼이 없어서 새로 생성합니다.');
      chatFab = createChatFab();
    }

    if (!chatFab) {
      console.error('❌ 채팅 FAB 버튼을 생성할 수 없습니다.');
      return false;
    }

    // 현재 디바이스에 따른 위치 조정
    const currentDevice = layoutState.currentDevice || detectDeviceType();
    const appNav = document.querySelector('.mobile-app-nav');
    const navHeight = appNav ? appNav.offsetHeight : 80;

    // 강제 스타일 적용
    const fabStyles = `
      position: fixed !important;
      display: flex !important;
      visibility: visible !important;
      opacity: 1 !important;
      z-index: 1025 !important;
      background: var(--primary-color, #ff6b35) !important;
      border-radius: 50% !important;
      border: none !important;
      color: white !important;
      cursor: pointer !important;
      align-items: center !important;
      justify-content: center !important;
      box-shadow: 0 4px 12px rgba(255, 107, 53, 0.3) !important;
      transition: all 0.3s ease !important;
    `;

    // 디바이스별 위치 설정
    let bottomPosition, rightPosition, fabSize, fontSize;

    switch (currentDevice) {
      case 'desktop':
        bottomPosition = '110px';
        rightPosition = '40px';
        fabSize = '64px';
        fontSize = '28px';
        break;
      case 'tablet':
        bottomPosition = `${navHeight + 20}px`;
        rightPosition = '30px';
        fabSize = '60px';
        fontSize = '26px';
        break;
      case 'mobile':
      default:
        bottomPosition = `${navHeight + 15}px`;
        rightPosition = '20px';
        fabSize = '56px';
        fontSize = '24px';
        break;
    }

    // 스타일 적용
    chatFab.style.cssText = fabStyles + `
      bottom: ${bottomPosition} !important;
      right: ${rightPosition} !important;
      width: ${fabSize} !important;
      height: ${fabSize} !important;
      font-size: ${fontSize} !important;
    `;

    // 클릭 이벤트가 없으면 추가
    if (!chatFab.hasAttribute('data-event-bound')) {
      chatFab.addEventListener('click', handleChatFabClick);
      chatFab.setAttribute('data-event-bound', 'true');
    }

    console.log('✅ 채팅 FAB 버튼 수정 완료');
    return true;

  } catch (error) {
    console.error('❌ 채팅 FAB 버튼 수정 실패:', error);
    return false;
  }
}

/**
 * 🔧 채팅 FAB 클릭 핸들러
 */
function handleChatFabClick(e) {
  e.preventDefault();
  e.stopPropagation();

  console.log('💬 채팅 FAB 클릭');

  // 클릭 효과 애니메이션
  const chatFab = e.currentTarget;
  chatFab.style.transform = 'scale(0.95)';
  setTimeout(() => {
    chatFab.style.transform = 'scale(1)';
  }, 150);

  // 로그인 상태 확인
  const isLoggedIn = window.APP_CONFIG?.isLoggedIn ||
      window.serverData?.isLoggedIn ||
      !!localStorage.getItem('accessToken');

  if (!isLoggedIn) {
    if (window.showToast) {
      showToast('채팅 기능을 사용하려면 로그인이 필요합니다.', 'info');
    } else {
      alert('채팅 기능을 사용하려면 로그인이 필요합니다.');
    }
    setTimeout(() => {
      window.location.href = '/mobile/login';
    }, 1500);
    return;
  }

  // 현재는 준비 중 메시지 표시
  if (window.showToast) {
    showToast('채팅 기능 준비 중입니다.', 'info', 2000);
  } else {
    alert('채팅 기능 준비 중입니다.');
  }

  // TODO: 실제 채팅 기능 구현 시 주석 해제
  // window.location.href = '/mobile/chat';
}

/**
 * 🔧 채팅 FAB 지속적 모니터링 및 복구
 */
function startChatFabMonitoring() {
  // 로그인 상태가 아니면 모니터링하지 않음
  const isLoggedIn = window.APP_CONFIG?.isLoggedIn ||
      window.serverData?.isLoggedIn ||
      !!localStorage.getItem('accessToken');

  if (!isLoggedIn) {
    return;
  }

  const monitorInterval = setInterval(() => {
    const chatFab = document.querySelector('.chat-fab');

    if (!chatFab) {
      console.warn('⚠️ 채팅 FAB이 사라짐 - 재생성');
      createAndShowChatFab();
      return;
    }

    // 스타일 검증
    const computedStyle = getComputedStyle(chatFab);
    const isVisible = computedStyle.display !== 'none' &&
        computedStyle.visibility !== 'hidden' &&
        computedStyle.opacity !== '0';

    if (!isVisible) {
      console.warn('⚠️ 채팅 FAB이 숨겨짐 - 스타일 복구');
      forceChatFabStyles(chatFab);
    }

    // 위치 검증
    const bottom = parseInt(computedStyle.bottom);
    const right = parseInt(computedStyle.right);

    if (bottom < 50 || right < 10) {
      console.warn('⚠️ 채팅 FAB 위치 이상 - 위치 복구');
      forceChatFabStyles(chatFab);
    }

  }, 5000); // 5초마다 검사

  // 페이지 언로드 시 정리
  window.addEventListener('beforeunload', () => {
    clearInterval(monitorInterval);
  });

  console.log('👁️ 채팅 FAB 모니터링 시작');
}

/**
 * 🔧 채팅 FAB 초기화 통합 함수
 */
function initializeChatFab() {
  console.log('💬 채팅 FAB 초기화');

  // 로그인 상태 확인
  const isLoggedIn = window.APP_CONFIG?.isLoggedIn ||
      window.serverData?.isLoggedIn ||
      !!localStorage.getItem('accessToken');

  if (!isLoggedIn) {
    console.log('📝 로그인하지 않은 상태이므로 채팅 FAB을 초기화하지 않습니다.');
    return;
  }

  // 1. 채팅 FAB 생성 및 표시
  createAndShowChatFab();

  // 2. 지속적 모니터링 시작
  startChatFabMonitoring();

  // 3. 윈도우 리사이즈 시 위치 재조정
  const resizeHandler = () => {
    setTimeout(() => {
      forceChatFabStyles();
    }, 300);
  };

  window.addEventListener('resize', resizeHandler);
  window.addEventListener('orientationchange', () => {
    setTimeout(resizeHandler, 500);
  });

  // 4. 페이지 가시성 변경 시 복구
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) {
      setTimeout(() => {
        const chatFab = document.querySelector('.chat-fab');
        if (!chatFab) {
          createAndShowChatFab();
        } else {
          forceChatFabStyles(chatFab);
        }
      }, 1000);
    }
  });

  console.log('✅ 채팅 FAB 초기화 완료');
}


/**
 * 🔧 브라우저별 핫픽스 적용
 */
function applyBrowserSpecificFixes() {
  const userAgent = navigator.userAgent;
  const appNav = document.querySelector('.mobile-app-nav');

  if (!appNav) return;

  // iOS Safari 특별 처리
  if (/iPad|iPhone|iPod/.test(userAgent)) {
    appNav.style.paddingBottom = 'max(20px, env(safe-area-inset-bottom))';
  }

  // Android Chrome 특별 처리
  if (/Android/.test(userAgent) && /Chrome/.test(userAgent)) {
    appNav.style.bottom = '0';
    appNav.style.position = 'fixed';
  }

  // Samsung Internet 특별 처리
  if (/SamsungBrowser/.test(userAgent)) {
    appNav.style.zIndex = '9999';
  }
}

/**
 * 🔧 실시간 간격 모니터링
 */
function startSpacingMonitor() {
  if (layoutState.spacingMonitor) return;

  const monitor = () => {
    const gap = diagnoseLayoutSpacing();
    if (gap !== null && gap > 5) {
      console.warn(`⚠️ 간격 문제 감지: ${gap}px - 자동 수정 시도`);
      fixLayoutSpacing();
    }
  };

  layoutState.spacingMonitor = setInterval(monitor, 5000);
  window.addEventListener('resize', monitor);

  console.log('👁️ 실시간 간격 모니터링 시작');
}

/**
 * 🔧 실시간 콘텐츠 모니터링
 */
function startContentMonitor() {
  if (layoutState.contentMonitor) return;

  const monitor = () => {
    const issues = diagnoseContentOverlap();
    if (issues.length > 0) {
      console.warn('⚠️ 콘텐츠 문제 감지:', issues);
      fixContentLayout();
      fixChatFab();
    }
  };

  layoutState.contentMonitor = setInterval(monitor, 10000);
  console.log('👁️ 실시간 콘텐츠 모니터링 시작');
}

/**
 * 🔧 간격 모니터링 중지
 */
function stopSpacingMonitor() {
  if (layoutState.spacingMonitor) {
    clearInterval(layoutState.spacingMonitor);
    layoutState.spacingMonitor = null;
    console.log('👁️ 실시간 간격 모니터링 중지');
  }
}

/**
 * 🔧 콘텐츠 모니터링 중지
 */
function stopContentMonitor() {
  if (layoutState.contentMonitor) {
    clearInterval(layoutState.contentMonitor);
    layoutState.contentMonitor = null;
    console.log('👁️ 실시간 콘텐츠 모니터링 중지');
  }
}

/**
 * 🔧 뷰포트 변경 시 레이아웃 재조정
 */
function handleViewportChange() {
  console.log('📱 뷰포트 변경 감지');

  const prevDevice = layoutState.currentDevice;
  detectDeviceType();

  fixContentLayout();
  fixChatFab();

  if (prevDevice !== layoutState.currentDevice) {
    console.log(`📱 디바이스 변경: ${prevDevice} → ${layoutState.currentDevice}`);

    setTimeout(() => {
      fixLayoutSpacing();
    }, 300);
  }
}

/**
 * 🔧 전체 문제 해결
 */
function resolveAllIssues() {
  console.group('🎯 전체 레이아웃 문제 해결');

  try {
    console.log('1️⃣ 간격 문제 진단');
    const gap = diagnoseLayoutSpacing();

    console.log('2️⃣ 콘텐츠 문제 진단');
    const contentIssues = diagnoseContentOverlap();

    console.log('3️⃣ 브라우저별 핫픽스 적용');
    applyBrowserSpecificFixes();

    console.log('4️⃣ 간격 문제 수정');
    const spacingFixed = fixLayoutSpacing();

    console.log('5️⃣ 콘텐츠 레이아웃 수정');
    const contentFixed = fixContentLayout();

    console.log('6️⃣ 채팅 FAB 버튼 수정');
    const fabFixed = fixChatFab();

    console.log('7️⃣ 최종 확인');
    setTimeout(() => {
      const finalGap = diagnoseLayoutSpacing();
      const finalContentIssues = diagnoseContentOverlap();

      if ((finalGap === null || finalGap <= 5) && finalContentIssues.length === 0) {
        console.log('✅ 모든 문제가 해결되었습니다!');
      } else {
        console.warn('⚠️ 일부 문제가 남아있을 수 있습니다.');
      }
    }, 1000);

    return { gap, contentIssues, spacingFixed, contentFixed, fabFixed };

  } catch (error) {
    console.error('❌ 문제 해결 중 오류:', error);
    return false;
  } finally {
    console.groupEnd();
  }
}

/**
 * 레이아웃 매니저 초기화
 */
function initializeLayout() {
  console.log('🚀 완전 반응형 레이아웃 매니저 초기화 시작');

  if (layoutState.isInitialized) {
    console.warn('⚠️ 레이아웃이 이미 초기화되었습니다.');
    return;
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      initializeLayoutComponents();
    });
  } else {
    initializeLayoutComponents();
  }
}

/**
 * 레이아웃 컴포넌트 초기화
 */
function initializeLayoutComponents() {
  console.log('🔧 레이아웃 컴포넌트 초기화 시작');

  try {
    detectDeviceType();
    findLayoutElements();
    bindLayoutEvents();
    initializeResponsiveLayout();
    initializeNavigation();
    initializeScrollBehavior();

    if (layoutState.currentDevice !== 'desktop') {
      initializeHamburgerMenu();
    }

    updateViewportHeight();

    // 🔧 채팅 FAB 초기화 추가
    setTimeout(() => {
      initializeChatFab();
    }, 500);

    // 🔧 모든 문제 해결 로직 추가
    setTimeout(() => {
      console.log('🔧 전체 문제 해결 로직 시작');

      // 전체 문제 해결
      resolveAllIssues();

      // 실시간 모니터링 시작
      startSpacingMonitor();
      startContentMonitor();

      // 뷰포트 변경 감지
      window.addEventListener('resize', debounce(handleViewportChange, 250));
      window.addEventListener('orientationchange', () => {
        setTimeout(handleViewportChange, 500);
      });

    }, 1500);

    layoutState.isInitialized = true;
    console.log('✅ 완전 반응형 레이아웃 초기화 완료');

  } catch (error) {
    console.error('❌ 레이아웃 초기화 실패:', error);
  }
}

/**
 * 디바이스 타입 감지
 */
function detectDeviceType() {
  const width = window.innerWidth;

  if (width < BREAKPOINTS.MOBILE) {
    layoutState.currentDevice = 'mobile';
  } else if (width < BREAKPOINTS.TABLET) {
    layoutState.currentDevice = 'tablet';
  } else {
    layoutState.currentDevice = 'desktop';
  }

  document.body.setAttribute('data-device', layoutState.currentDevice);
  console.log('📱 디바이스 타입:', layoutState.currentDevice, `(${width}px)`);
}

/**
 * 반응형 레이아웃 초기화
 */
function initializeResponsiveLayout() {
  console.log('🖥️ 반응형 레이아웃 초기화');

  switch (layoutState.currentDevice) {
    case 'desktop':
      initializeDesktopLayout();
      break;
    case 'tablet':
      initializeTabletLayout();
      break;
    case 'mobile':
    default:
      initializeMobileLayout();
      break;
  }
}

/**
 * 데스크톱 레이아웃 초기화
 */
function initializeDesktopLayout() {
  console.log('🖥️ 데스크톱 레이아웃 초기화');

  const sidebar = document.querySelector('.pc-sidebar');
  if (sidebar) {
    sidebar.style.display = 'block';
  }

  const menuBtn = document.querySelector('.header-menu-btn');
  if (menuBtn) {
    menuBtn.style.display = 'none';
  }

  if (layoutState.isMenuOpen) {
    closeMenu();
  }

  adjustContentLayout('desktop');
}

/**
 * 태블릿 레이아웃 초기화
 */
function initializeTabletLayout() {
  console.log('📱 태블릿 레이아웃 초기화');

  const sidebar = document.querySelector('.pc-sidebar');
  if (sidebar) {
    sidebar.style.display = 'none';
  }

  const menuBtn = document.querySelector('.header-menu-btn');
  if (menuBtn) {
    menuBtn.style.display = 'flex';
  }

  adjustContentLayout('tablet');
}

/**
 * 모바일 레이아웃 초기화
 */
function initializeMobileLayout() {
  console.log('📱 모바일 레이아웃 초기화');

  const sidebar = document.querySelector('.pc-sidebar');
  if (sidebar) {
    sidebar.style.display = 'none';
  }

  const menuBtn = document.querySelector('.header-menu-btn');
  if (menuBtn) {
    menuBtn.style.display = 'flex';
  }

  adjustContentLayout('mobile');
}

/**
 * 콘텐츠 레이아웃 조정
 */
function adjustContentLayout(deviceType) {
  const layout = document.querySelector('.mobile-layout');
  const header = document.querySelector('.mobile-header');
  const main = document.querySelector('.mobile-main');
  const nav = document.querySelector('.mobile-nav, .mobile-app-nav');

  if (!layout || !header || !main) return;

  switch (deviceType) {
    case 'desktop':
      layout.style.marginLeft = 'var(--sidebar-width-pc, 280px)';
      layout.style.paddingTop = 'var(--header-height-pc, 80px)';
      layout.style.paddingBottom = '0';

      if (header) {
        header.style.left = 'var(--sidebar-width-pc, 280px)';
        header.style.height = 'var(--header-height-pc, 80px)';
      }

      if (nav) {
        nav.style.cssText += `
          position: fixed !important;
          bottom: 0 !important;
          left: var(--sidebar-width-pc, 280px) !important;
          right: 0 !important;
          transform: none !important;
          max-width: none !important;
          width: auto !important;
          margin: 0 !important;
        `;
      }
      break;

    case 'tablet':
    case 'mobile':
    default:
      layout.style.marginLeft = '0';
      layout.style.paddingTop = 'var(--header-height-mobile, 70px)';
      layout.style.paddingBottom = 'var(--nav-height-mobile, 80px)';
      layout.style.marginBottom = '0';

      if (header) {
        header.style.left = '0';
        header.style.height = 'var(--header-height-mobile, 70px)';
      }

      if (nav) {
        nav.style.cssText += `
          position: fixed !important;
          bottom: 0 !important;
          left: 0 !important;
          right: 0 !important;
          width: 100% !important;
          max-width: none !important;
          margin: 0 !important;
          transform: none !important;
        `;
      }
      break;
  }
}

/**
 * 핵심 DOM 요소 찾기
 */
function findLayoutElements() {
  console.log('🔍 핵심 DOM 요소 찾기');

  if (layoutState.currentDevice !== 'desktop') {
    layoutState.menuElement = document.getElementById('mobileMenu');
    layoutState.menuButton = document.getElementById('menuToggleBtn');
    layoutState.overlay = document.getElementById('menuOverlay');
  }

  console.log('📱 요소 검색 결과:');
  console.log('  디바이스 타입:', layoutState.currentDevice);
  console.log('  메뉴 요소:', !!layoutState.menuElement);
  console.log('  메뉴 버튼:', !!layoutState.menuButton);
  console.log('  오버레이:', !!layoutState.overlay);

  if (layoutState.currentDevice !== 'desktop') {
    if (!layoutState.menuElement || !layoutState.menuButton || !layoutState.overlay) {
      console.warn('⚠️ 일부 필수 요소를 찾을 수 없습니다. 햄버거 메뉴 기능이 제한될 수 있습니다.');
    }
  }
}

/**
 * 이벤트 바인딩
 */
function bindLayoutEvents() {
  console.log('🔗 반응형 레이아웃 이벤트 바인딩');

  window.addEventListener('resize', debounce(() => {
    handleWindowResize();
  }, 250));

  document.addEventListener('keydown', handleKeyboardEvents);

  const navItems = document.querySelectorAll('.nav-item, .sidebar-menu-link');
  navItems.forEach(item => {
    item.addEventListener('click', (e) => {
      handleNavItemClick(e, item);
    });
  });

  bindMenuLinkEvents();
  bindSidebarEvents();

  console.log('✅ 반응형 레이아웃 이벤트 바인딩 완료');
}

/**
 * 사이드바 이벤트 바인딩 (PC전용)
 */
function bindSidebarEvents() {
  const sidebarLinks = document.querySelectorAll('.sidebar-menu-link');

  sidebarLinks.forEach(link => {
    link.addEventListener('click', (e) => {
      const href = link.getAttribute('href');
      const linkText = link.querySelector('span')?.textContent?.trim() || '알 수 없는 메뉴';

      console.log('🖥️ 사이드바 링크 클릭:', linkText, href);

      if (link.classList.contains('logout-btn')) {
        e.preventDefault();
        handleLogout();
        return;
      }

      if (link.classList.contains('disabled')) {
        e.preventDefault();
        showToast('준비 중인 기능입니다.', 'info', 2000);
        return;
      }

      if (href && (href.startsWith('http') || href.startsWith('tel:') || href.startsWith('mailto:'))) {
        e.preventDefault();
        window.open(href, '_blank', 'noopener,noreferrer');
        return;
      }

      if (href === window.location.pathname) {
        e.preventDefault();
        showToast('현재 페이지입니다.', 'info', 2000);
        return;
      }

      showNavigationLoading(link);
    });
  });
}

/**
 * 메뉴 링크 이벤트 바인딩
 */
function bindMenuLinkEvents() {
  const menuLinks = document.querySelectorAll('.menu-link');

  menuLinks.forEach(link => {
    link.addEventListener('click', (e) => {
      const href = link.getAttribute('href');
      const menuText = link.querySelector('.menu-text')?.textContent?.trim() || '알 수 없는 메뉴';

      console.log('🔗 메뉴 링크 클릭:', menuText, href);

      if (link.classList.contains('logout-btn')) {
        e.preventDefault();
        handleLogout();
        return;
      }

      if (link.classList.contains('disabled')) {
        e.preventDefault();
        showToast('준비 중인 기능입니다.', 'info', 2000);
        return;
      }

      if (href && (href.startsWith('http') || href.startsWith('tel:') || href.startsWith('mailto:'))) {
        e.preventDefault();
        window.open(href, '_blank', 'noopener,noreferrer');
        return;
      }

      if (href === window.location.pathname) {
        e.preventDefault();
        closeMenu();
        showToast('현재 페이지입니다.', 'info', 2000);
        return;
      }

      setTimeout(() => closeMenu(), 150);
      showNavigationLoading(link);
    });
  });
}

/**
 * 네비게이션 초기화
 */
function initializeNavigation() {
  console.log('🧭 네비게이션 초기화');
  updateActiveNavigation();
  enhanceNavigationAccessibility();
  console.log('✅ 네비게이션 초기화 완료');
}

/**
 * 스크롤 동작 초기화
 */
function initializeScrollBehavior() {
  console.log('📜 스크롤 동작 초기화');

  let ticking = false;

  window.addEventListener('scroll', () => {
    if (!ticking) {
      requestAnimationFrame(() => {
        handleScroll();
        ticking = false;
      });
      ticking = true;
    }
  });

  console.log('✅ 스크롤 동작 초기화 완료');
}

/**
 * 햄버거 메뉴 초기화
 */
function initializeHamburgerMenu() {
  console.log('🍔 햄버거 메뉴 초기화');

  if (!layoutState.menuElement || !layoutState.menuButton || !layoutState.overlay) {
    console.warn('⚠️ 햄버거 메뉴 필수 요소가 없어 초기화를 건너뜁니다.');
    return;
  }

  setMenuInitialState();
  bindMenuButtonEvents();
  bindMenuInternalEvents();

  console.log('✅ 햄버거 메뉴 초기화 완료');
}

/**
 * 메뉴 초기 상태 설정
 */
function setMenuInitialState() {
  if (!layoutState.menuElement) return;

  layoutState.menuElement.classList.remove('show');
  layoutState.menuElement.style.transform = 'translateX(100%)';
  layoutState.menuElement.style.visibility = 'hidden';

  if (layoutState.overlay) {
    layoutState.overlay.classList.remove('show');
  }

  layoutState.isMenuOpen = false;
}

/**
 * 메뉴 버튼 이벤트 바인딩
 */
function bindMenuButtonEvents() {
  if (!layoutState.menuButton) return;

  layoutState.menuButton.addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();

    if (layoutState.currentDevice === 'desktop') {
      return;
    }

    toggleMenu();
  });
}

/**
 * 메뉴 내부 이벤트 바인딩
 */
function bindMenuInternalEvents() {
  if (!layoutState.menuElement) return;

  const closeBtn = document.getElementById('menuCloseBtn');
  if (closeBtn) {
    closeBtn.addEventListener('click', (e) => {
      e.preventDefault();
      closeMenu();
    });
  }

  if (layoutState.overlay) {
    layoutState.overlay.addEventListener('click', () => {
      closeMenu();
    });
  }
}

/**
 * 메뉴 토글
 */
function toggleMenu() {
  if (layoutState.currentDevice === 'desktop') {
    return;
  }

  if (!layoutState.menuElement || !layoutState.overlay) {
    console.error('❌ 메뉴 토글 실패 - 필수 요소 누락');
    return;
  }

  if (layoutState.isMenuOpen) {
    closeMenu();
  } else {
    openMenu();
  }
}

/**
 * 메뉴 열기
 */
function openMenu() {
  if (layoutState.currentDevice === 'desktop') {
    return;
  }

  if (!layoutState.menuElement || !layoutState.overlay) {
    console.error('❌ 메뉴 열기 실패 - 필수 요소 누락');
    return;
  }

  layoutState.isMenuOpen = true;

  if (layoutState.menuButton) {
    layoutState.menuButton.classList.add('menu-open');
  }

  layoutState.menuElement.classList.add('show');
  layoutState.menuElement.style.transform = 'translateX(0)';
  layoutState.menuElement.style.visibility = 'visible';

  layoutState.overlay.classList.add('show');
  document.body.classList.add('menu-open');

  setTimeout(() => {
    const firstFocusable = layoutState.menuElement.querySelector('a, button');
    if (firstFocusable) {
      firstFocusable.focus();
    }
  }, 100);
}

/**
 * 메뉴 닫기
 */
function closeMenu() {
  if (!layoutState.menuElement || !layoutState.overlay) {
    console.error('❌ 메뉴 닫기 실패 - 필수 요소 누락');
    return;
  }

  layoutState.isMenuOpen = false;

  if (layoutState.menuButton) {
    layoutState.menuButton.classList.remove('menu-open');
  }

  layoutState.menuElement.classList.remove('show');
  layoutState.menuElement.style.transform = 'translateX(100%)';
  layoutState.menuElement.style.visibility = 'hidden';

  layoutState.overlay.classList.remove('show');
  document.body.classList.remove('menu-open');

  if (layoutState.menuButton) {
    layoutState.menuButton.focus();
  }
}

/**
 * 윈도우 리사이즈 처리
 */
function handleWindowResize() {
  const prevDevice = layoutState.currentDevice;
  detectDeviceType();
  const currentDevice = layoutState.currentDevice;

  if (prevDevice !== currentDevice) {
    handleDeviceTypeChange(prevDevice, currentDevice);
  }

  updateViewportHeight();
  adjustContentLayout(currentDevice);

  setTimeout(() => {
    fixLayoutSpacing();
    fixContentLayout();
    fixChatFab();
  }, 500);
}

/**
 * 디바이스 타입 변경 처리
 */
function handleDeviceTypeChange(prevDevice, currentDevice) {
  console.log(`🔄 디바이스 타입 변경: ${prevDevice} → ${currentDevice}`);

  if (layoutState.isMenuOpen && currentDevice === 'desktop') {
    closeMenu();
  }

  switch (currentDevice) {
    case 'desktop':
      initializeDesktopLayout();
      break;
    case 'tablet':
      initializeTabletLayout();
      break;
    case 'mobile':
    default:
      initializeMobileLayout();
      break;
  }

  if (currentDevice !== 'desktop' && (prevDevice === 'desktop' || !layoutState.menuElement)) {
    findLayoutElements();
    if (layoutState.menuElement && layoutState.menuButton && layoutState.overlay) {
      initializeHamburgerMenu();
    }
  }

  updateActiveNavigation();

  setTimeout(() => {
    fixLayoutSpacing();
    fixContentLayout();
    fixChatFab();
  }, 300);
}

/**
 * 스크롤 처리
 */
function handleScroll() {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
  const header = document.getElementById('mobileHeader');

  if (header) {
    if (scrollTop > 50) {
      header.classList.add('scrolled');
    } else {
      header.classList.remove('scrolled');
    }
  }

  layoutState.lastScrollTop = scrollTop;

  clearTimeout(layoutState.scrollTimeout);
  layoutState.scrollTimeout = setTimeout(() => {
    layoutState.isScrolling = false;
  }, 150);
}

/**
 * 키보드 이벤트 처리
 */
function handleKeyboardEvents(e) {
  if (e.key === 'Escape' && layoutState.isMenuOpen && layoutState.currentDevice !== 'desktop') {
    e.preventDefault();
    closeMenu();
  }

  if (e.altKey && layoutState.currentDevice === 'desktop' && !layoutState.isMenuOpen) {
    const key = parseInt(e.key);
    if (key >= 1 && key <= 6) {
      e.preventDefault();
      const sidebarLinks = document.querySelectorAll('.sidebar-menu-link');
      if (sidebarLinks[key - 1]) {
        sidebarLinks[key - 1].click();
      }
    }
  }
}

/**
 * 네비게이션 아이템 클릭 처리
 */
function handleNavItemClick(e, item) {
  const href = item.getAttribute('href');
  const itemText = item.querySelector('span')?.textContent?.trim() || '알 수 없는 메뉴';

  if (item.classList.contains('disabled')) {
    e.preventDefault();
    showToast('준비 중인 기능입니다.', 'info', 2000);
    return;
  }

  if (href === window.location.pathname) {
    e.preventDefault();
    showToast(`${itemText} 페이지를 새로고침합니다.`, 'info', 2000);
    setTimeout(() => {
      window.location.reload();
    }, 500);
    return;
  }

  showNavigationLoading(item);
  animatePageTransition();
}

/**
 * 활성 네비게이션 업데이트
 */
function updateActiveNavigation() {
  const currentPath = window.location.pathname;

  const sidebarItems = document.querySelectorAll('.sidebar-menu-link');
  sidebarItems.forEach(item => {
    const href = item.getAttribute('href');
    item.classList.remove('active');

    if (href === currentPath) {
      item.classList.add('active');
    } else if (href !== '/mobile/home' && currentPath.startsWith(href)) {
      item.classList.add('active');
    } else if (href === '/mobile/home' && (currentPath === '/mobile' || currentPath === '/mobile/')) {
      item.classList.add('active');
    }
  });

  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach(item => {
    const href = item.getAttribute('href');
    item.classList.remove('active');

    if (href === currentPath) {
      item.classList.add('active');
    } else if (href !== '/mobile/home' && currentPath.startsWith(href)) {
      item.classList.add('active');
    } else if (href === '/mobile/home' && (currentPath === '/mobile' || currentPath === '/mobile/')) {
      item.classList.add('active');
    }
  });
}

/**
 * 네비게이션 접근성 향상
 */
function enhanceNavigationAccessibility() {
  const sidebarItems = document.querySelectorAll('.sidebar-menu-link');
  sidebarItems.forEach((item, index) => {
    const text = item.querySelector('span')?.textContent;
    if (text) {
      item.setAttribute('title', `${text} (Alt+${index + 1})`);
      item.setAttribute('aria-label', `${text} 페이지로 이동`);
    }
  });

  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach((item, index) => {
    const text = item.querySelector('span')?.textContent;
    if (text) {
      item.setAttribute('title', text);
      item.setAttribute('aria-label', `${text} 페이지로 이동`);
    }
  });
}

/**
 * 로그아웃 처리
 */
async function handleLogout() {
  try {
    const confirmed = await showConfirm(
        '로그아웃',
        '정말 로그아웃 하시겠습니까?',
        '로그아웃',
        '취소'
    );

    if (!confirmed) {
      return;
    }

    showPageLoading();

    if (layoutState.currentDevice !== 'desktop') {
      closeMenu();
    }

    showToast('로그아웃 중입니다...', 'info');
    await memberLogout();

  } catch (error) {
    console.error('로그아웃 실패:', error);
    hidePageLoading();
    showToast('로그아웃 중 오류가 발생했습니다.', 'error');
  }
}

/**
 * 네비게이션 로딩 표시
 */
function showNavigationLoading(item) {
  const icon = item.querySelector('i');
  if (icon) {
    icon.dataset.originalClass = icon.className;
    icon.className = 'fas fa-spinner fa-spin';
  }

  setTimeout(() => {
    if (icon && icon.dataset.originalClass) {
      icon.className = icon.dataset.originalClass;
      delete icon.dataset.originalClass;
    }
  }, 2000);
}

/**
 * 페이지 전환 애니메이션
 */
function animatePageTransition(direction = 'forward') {
  const main = document.getElementById('mobileMain');
  if (main) {
    main.style.opacity = '0.7';
    main.style.transform = direction === 'back' ? 'translateX(-10px)' : 'translateX(10px)';
    main.style.transition = 'all 0.3s ease';

    setTimeout(() => {
      main.style.opacity = '';
      main.style.transform = '';
      main.style.transition = '';
    }, 300);
  }
}

/**
 * 뷰포트 높이 업데이트
 */
function updateViewportHeight() {
  const vh = window.innerHeight * 0.01;
  document.documentElement.style.setProperty('--vh', `${vh}px`);
}

/**
 * 페이지 로딩 상태 표시
 */
function showPageLoading() {
  document.body.classList.add('loading');
}

/**
 * 페이지 로딩 상태 숨김
 */
function hidePageLoading() {
  document.body.classList.remove('loading');
}

/**
 * 디바운스 유틸리티
 */
function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

/**
 * 메뉴 상태 체크
 */
function checkMenuState() {
  console.log('🔍 레이아웃 상태 체크:', {
    currentDevice: layoutState.currentDevice,
    isMenuOpen: layoutState.isMenuOpen,
    menuElement: !!layoutState.menuElement,
    menuButton: !!layoutState.menuButton,
    overlay: !!layoutState.overlay,
    bodyClasses: document.body.className,
    windowSize: {
      width: window.innerWidth,
      height: window.innerHeight
    }
  });
}

/**
 * 레이아웃 정리
 */
function destroyLayout() {
  console.log('🗑️ 레이아웃 정리');

  stopSpacingMonitor();
  stopContentMonitor();

  window.removeEventListener('resize', handleWindowResize);

  if (layoutState.overlay && layoutState.overlay.parentNode) {
    layoutState.overlay.parentNode.removeChild(layoutState.overlay);
  }

  if (layoutState.scrollTimeout) {
    clearTimeout(layoutState.scrollTimeout);
  }

  if (layoutState.resizeTimeout) {
    clearTimeout(layoutState.resizeTimeout);
  }

  layoutState = {
    isMenuOpen: false,
    lastScrollTop: 0,
    isScrolling: false,
    menuElement: null,
    menuButton: null,
    overlay: null,
    isInitialized: false,
    navigationBadges: {},
    scrollTimeout: null,
    currentDevice: 'mobile',
    resizeTimeout: null,
    spacingMonitor: null,
    contentMonitor: null
  };
}

/**
 * 온라인/오프라인 상태 처리
 */
function handleConnectionChange() {
  window.addEventListener('online', () => {
    console.log('🌐 온라인 상태 복원');
    showToast('인터넷 연결이 복원되었습니다.', 'success', 3000);
  });

  window.addEventListener('offline', () => {
    console.log('📴 오프라인 상태');
    showToast('인터넷 연결이 끊어졌습니다.', 'warning', 5000);
  });
}

/**
 * 페이지 가시성 변경 처리
 */
function handleVisibilityChange() {
  document.addEventListener('visibilitychange', () => {
    if (!document.hidden && layoutState.isInitialized) {
      console.log('👁️ 페이지 가시성 복원');

      updateActiveNavigation();

      const prevDevice = layoutState.currentDevice;
      detectDeviceType();

      if (prevDevice !== layoutState.currentDevice) {
        handleDeviceTypeChange(prevDevice, layoutState.currentDevice);
      }

      setTimeout(() => {
        fixLayoutSpacing();
        fixContentLayout();
        fixChatFab();
      }, 1000);
    }
  });
}

// 전역 함수들
window.openMenu = openMenu;
window.closeMenu = closeMenu;
window.toggleMenu = toggleMenu;
window.checkMenuState = checkMenuState;

// 전역 함수로 채팅 FAB 관련 함수들 등록
window.createAndShowChatFab = createAndShowChatFab;
window.forceChatFabStyles = forceChatFabStyles;
window.handleChatFabClick = handleChatFabClick;
window.initializeChatFab = initializeChatFab;

// 완전 반응형 레이아웃 매니저 객체
window.layoutManager = {
  openMenu,
  closeMenu,
  toggleMenu,
  isMenuOpen: () => layoutState.isMenuOpen,
  getCurrentDevice: () => layoutState.currentDevice,
  updateResponsiveLayout: () => handleWindowResize(),
  showPageLoading,
  hidePageLoading,
  destroy: destroyLayout,
  checkState: checkMenuState,
  isDesktop: () => layoutState.currentDevice === 'desktop',
  isTablet: () => layoutState.currentDevice === 'tablet',
  isMobile: () => layoutState.currentDevice === 'mobile',
  getBreakpoints: () => BREAKPOINTS,
  diagnoseSpacing: diagnoseLayoutSpacing,
  fixSpacing: fixLayoutSpacing,
  startSpacingMonitor: startSpacingMonitor,
  stopSpacingMonitor: stopSpacingMonitor,
  diagnoseContent: diagnoseContentOverlap,
  fixContent: fixContentLayout,
  fixChatFab: fixChatFab,
  resolveAll: resolveAllIssues
};

// 전역 유틸리티
window.layoutSpacingUtils = {
  diagnose: diagnoseLayoutSpacing,
  fix: fixLayoutSpacing,
  monitor: startSpacingMonitor,
  stopMonitor: stopSpacingMonitor,
  diagnoseContent: diagnoseContentOverlap,
  fixContent: fixContentLayout,
  fixChatFab: fixChatFab,
  resolveAll: resolveAllIssues,
  createChatFab: createAndShowChatFab,
  fixChatFabStyles: forceChatFabStyles,
  initChatFab: initializeChatFab,
  toggleDebug: function() {
    const isDebug = document.body.classList.contains('debug-spacing');
    if (isDebug) {
      document.body.classList.remove('debug-spacing');
      console.log('🔧 디버그 모드 비활성화');
    } else {
      document.body.classList.add('debug-spacing');
      console.log('🔧 디버그 모드 활성화 - 요소 경계선 표시');
    }
    return !isDebug;
  }
};

// 전역 디버깅 함수
window.debugLayout = function() {
  console.group('🔍 완전 반응형 레이아웃 디버그 정보');
  console.log('레이아웃 상태:', layoutState);
  console.log('현재 디바이스:', layoutState.currentDevice);
  console.log('브레이크포인트:', BREAKPOINTS);
  console.log('윈도우 크기:', {
    width: window.innerWidth,
    height: window.innerHeight
  });
  console.log('메뉴 요소:', layoutState.menuElement);
  console.log('메뉴 버튼:', layoutState.menuButton);
  console.log('오버레이:', layoutState.overlay);
  console.log('메뉴 열림 상태:', layoutState.isMenuOpen);
  console.log('초기화 상태:', layoutState.isInitialized);

  const gap = diagnoseLayoutSpacing();
  console.log('앱 네비게이션 간격:', gap ? `${gap}px` : '정상');

  const contentIssues = diagnoseContentOverlap();
  console.log('콘텐츠 문제:', contentIssues.length ? contentIssues : '없음');

  console.groupEnd();
};

/**
 * 🔧 채팅 FAB 버튼 생성 및 강제 표시
 */
function createAndShowChatFab() {
  console.log('💬 채팅 FAB 버튼 생성 및 표시 시작');

  try {
    // 로그인 상태 확인
    const isLoggedIn = window.APP_CONFIG?.isLoggedIn ||
        window.serverData?.isLoggedIn ||
        !!localStorage.getItem('accessToken');

    if (!isLoggedIn) {
      console.log('📝 로그인하지 않은 상태이므로 채팅 FAB을 생성하지 않습니다.');
      return null;
    }

    // 기존 채팅 FAB 제거
    const existingFab = document.querySelector('.chat-fab');
    if (existingFab) {
      existingFab.remove();
      console.log('🗑️ 기존 채팅 FAB 제거');
    }

    // 새 채팅 FAB 생성
    const chatFab = document.createElement('button');
    chatFab.className = 'chat-fab';
    chatFab.id = 'chatFab';
    chatFab.setAttribute('aria-label', '채팅 열기');
    chatFab.innerHTML = '<i class="fas fa-comments"></i>';

    // 클릭 이벤트 추가
    chatFab.addEventListener('click', handleChatFabClick);

    // body에 추가
    document.body.appendChild(chatFab);

    // 강제 스타일 적용
    setTimeout(() => {
      forceChatFabStyles(chatFab);
    }, 100);

    console.log('✅ 채팅 FAB 버튼 생성 및 표시 완료');
    return chatFab;

  } catch (error) {
    console.error('❌ 채팅 FAB 버튼 생성 실패:', error);
    return null;
  }
}

/**
 * 🔧 채팅 FAB 스타일 강제 적용
 */
function forceChatFabStyles(chatFab) {
  if (!chatFab) {
    chatFab = document.querySelector('.chat-fab');
  }

  if (!chatFab) {
    console.warn('⚠️ 채팅 FAB 요소를 찾을 수 없습니다.');
    return;
  }

  // 현재 디바이스에 따른 위치 설정
  const currentDevice = layoutState.currentDevice || detectDeviceType();
  const appNav = document.querySelector('.mobile-app-nav');
  const navHeight = appNav ? appNav.offsetHeight : 80;

  // 디바이스별 위치 설정
  let bottomPosition, rightPosition, fabSize, fontSize;

  switch (currentDevice) {
    case 'desktop':
      bottomPosition = '110px';
      rightPosition = '40px';
      fabSize = '64px';
      fontSize = '28px';
      break;
    case 'tablet':
      bottomPosition = `${navHeight + 20}px`;
      rightPosition = '30px';
      fabSize = '60px';
      fontSize = '26px';
      break;
    case 'mobile':
    default:
      bottomPosition = `${navHeight + 15}px`;
      rightPosition = '20px';
      fabSize = '56px';
      fontSize = '24px';
      break;
  }

  // 강제 스타일 적용
  const forceStyles = `
    position: fixed !important;
    bottom: ${bottomPosition} !important;
    right: ${rightPosition} !important;
    width: ${fabSize} !important;
    height: ${fabSize} !important;
    background: var(--primary-color, #ff6b35) !important;
    border-radius: 50% !important;
    border: none !important;
    color: white !important;
    font-size: ${fontSize} !important;
    cursor: pointer !important;
    box-shadow: 0 4px 12px rgba(255, 107, 53, 0.3) !important;
    z-index: 1025 !important;
    transition: all 0.3s ease !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    visibility: visible !important;
    opacity: 1 !important;
    transform: none !important;
    margin: 0 !important;
    padding: 0 !important;
    -webkit-appearance: none !important;
    -webkit-tap-highlight-color: transparent !important;
    -webkit-touch-callout: none !important;
    -webkit-user-select: none !important;
    user-select: none !important;
  `;

  chatFab.style.cssText = forceStyles;

  // 추가 보장을 위한 속성 설정
  chatFab.style.setProperty('display', 'flex', 'important');
  chatFab.style.setProperty('visibility', 'visible', 'important');
  chatFab.style.setProperty('opacity', '1', 'important');

  console.log('🔧 채팅 FAB 스타일 강제 적용 완료:', {
    device: currentDevice,
    bottom: bottomPosition,
    right: rightPosition,
    size: fabSize
  });
}




// 자동 초기화
console.log('🌟 완전 반응형 layout.js (최종 완성본) 스크립트 로드 완료');
initializeLayout();

// 추가 이벤트 핸들러 초기화
handleConnectionChange();
handleVisibilityChange();

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', () => {
  destroyLayout();
});

// 앱 네비게이션 설정
window.APP_CONFIG = window.APP_CONFIG || {};
window.APP_CONFIG.appNavigation = {
  apiUrl: 'https://api.otongtong.net/v1/api/etc/app/menu_list',
  params: {
    app_type: 'newstong',
    nation_code: 'KR',
    device: 'android',
    mode: 'dev'
  }
};

// 개발자 도구 명령어 안내
console.log(`
🔧 개발자 도구에서 사용 가능한 명령어:
- layoutSpacingUtils.resolveAll() : 모든 문제 한번에 해결
- layoutSpacingUtils.diagnose() : 간격 문제 진단
- layoutSpacingUtils.diagnoseContent() : 콘텐츠 문제 진단
- layoutSpacingUtils.fix() : 간격 문제 수정
- layoutSpacingUtils.fixContent() : 콘텐츠 레이아웃 수정
- layoutSpacingUtils.fixChatFab() : 채팅 FAB 버튼 수정
- layoutSpacingUtils.toggleDebug() : 디버그 모드 토글
- debugLayout() : 전체 레이아웃 디버그 정보
`);

// 모듈 익스포트
export {
  initializeLayout,
  openMenu,
  closeMenu,
  toggleMenu,
  showPageLoading,
  hidePageLoading,
  checkMenuState,
  detectDeviceType,
  handleDeviceTypeChange,
  initializeResponsiveLayout,
  diagnoseLayoutSpacing,
  diagnoseContentOverlap,
  fixLayoutSpacing,
  fixContentLayout,
  fixChatFab,
  createChatFab,
  handleChatFabClick,
  startSpacingMonitor,
  stopSpacingMonitor,
  startContentMonitor,
  stopContentMonitor,
  resolveAllIssues
};