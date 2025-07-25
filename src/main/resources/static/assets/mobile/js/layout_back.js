// layout.js - 토마토리멤버 완전 반응형 레이아웃 관리 (체험하기 우선, 채팅 아래)

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
  contentMonitor: null,
  chatFab: null
};

// 디바이스 브레이크포인트
const BREAKPOINTS = {
  MOBILE: 768,
  TABLET: 1024,
  DESKTOP: 1200
};

/**
 * 레이아웃 매니저 초기화
 */
function initializeLayout() {
  console.log('🚀 토마토 사이드바 적용 레이아웃 매니저 초기화 시작 (채팅 FAB 아래 위치)');

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
    // initializeNavigation();
    initializeScrollBehavior();

    if (layoutState.currentDevice !== 'desktop') {
      initializeHamburgerMenu();
    }

    updateViewportHeight();

    // 🔧 콘텐츠 레이아웃 최적화 (하단 네비게이션 제거로 인한)
    setTimeout(() => {
      console.log('🔧 하단 네비게이션 제거에 따른 레이아웃 최적화 시작');
      optimizeLayoutForSidebar();
      startContentMonitor();
    }, 1000);

    // 🔧 채팅 FAB 초기화 (체험하기보다 아래 위치, DOM 준비 후)
    requestAnimationFrame(() => {
      setTimeout(() => {
        initializeChatFabBelowExperience();
      }, 300); // 체험하기 후 300ms
    });

    setTimeout(async () => {
      console.log('🍅 토마토 그룹 네비게이션 초기화 시작');

      if (window.initializeTomatoGroupNavigation) {
        try {
          await window.initializeTomatoGroupNavigation();
          console.log('✅ 토마토 그룹 네비게이션 초기화 완료');
        } catch (error) {
          console.error('❌ 토마토 그룹 네비게이션 초기화 실패:', error);
        }
      } else {
        console.warn('⚠️ 토마토 그룹 네비게이션 함수를 찾을 수 없습니다.');

        // 폴백: 스크립트가 아직 로드되지 않은 경우 재시도
        setTimeout(() => {
          if (window.initializeTomatoGroupNavigation) {
            console.log('🔄 토마토 그룹 네비게이션 재시도');
            window.initializeTomatoGroupNavigation();
          }
        }, 2000);
      }
    }, 800);

    layoutState.isInitialized = true;
    console.log('✅ 토마토 사이드바 적용 레이아웃 초기화 완료 (채팅 아래 위치)');

  } catch (error) {
    console.error('❌ 레이아웃 초기화 실패:', error);
  }
}

/**
 * 🔧 채팅 FAB 초기화 (체험하기보다 아래 위치)
 */
function initializeChatFabBelowExperience() {
  console.log('💬 채팅 FAB 초기화 (체험하기보다 아래 위치)');

  const isLoggedIn = window.APP_CONFIG?.isLoggedIn ||
      window.serverData?.isLoggedIn ||
      !!localStorage.getItem('accessToken');

  if (!isLoggedIn) {
    console.log('📝 로그인하지 않은 상태이므로 채팅 FAB을 초기화하지 않습니다.');
    return;
  }

  // 체험하기 버튼이 생성될 때까지 대기 (즉시 체크)
  const checkExperienceFab = () => {
    const experienceFab = document.querySelector('.experience-fab');

    if (experienceFab) {
      console.log('🚀 체험하기 버튼 확인됨, 채팅 FAB 생성');
      // DOM이 완전히 준비된 후 채팅 FAB 생성
      requestAnimationFrame(() => {
        createChatFabBelow();
      });
    } else {
      // console.log('⏳ 체험하기 버튼 대기 중...');
      // 더 빠른 체크
      requestAnimationFrame(checkExperienceFab);
    }
  };

  checkExperienceFab();
}

/**
 * 🔧 채팅 FAB 버튼 생성 (체험하기 아래)
 */
function createChatFabBelow() {
  try {
    // 기존 채팅 FAB 제거
    const existingChatFab = document.querySelector('.chat-fab');
    if (existingChatFab) {
      existingChatFab.remove();
      console.log('🗑️ 기존 채팅 FAB 제거');
    }

    const chatFab = document.createElement('button');
    chatFab.className = 'chat-fab';
    chatFab.id = 'chatFab';
    chatFab.setAttribute('aria-label', '채팅 열기');
    chatFab.innerHTML = '<i class="fas fa-comments"></i>';

    // 위치 계산 (체험하기보다 아래)
    const deviceType = layoutState.currentDevice || detectDeviceType();
    let bottomPosition, rightPosition, fabSize, fontSize;

    // 체험하기 버튼 존재 여부 확인
    const experienceFab = document.querySelector('.experience-fab');
    const hasExperienceFab = !!experienceFab;

    switch (deviceType) {
      case 'desktop':
        bottomPosition = hasExperienceFab ? '50px' : '50px'; // 체험하기 아래 130px 차이
        rightPosition = '40px';
        fabSize = '64px';
        fontSize = '28px';
        break;
      case 'tablet':
        bottomPosition = hasExperienceFab ?
          'calc(var(--nav-height-mobile, 80px) + 35px)' :
          'calc(var(--nav-height-mobile, 80px) + 35px)';
        rightPosition = '30px';
        fabSize = '60px';
        fontSize = '26px';
        break;
      case 'mobile':
      default:
        const mobileRight = window.innerWidth <= 375 ? '16px' : '20px';
        const mobileSize = window.innerWidth <= 375 ? '50px' : '56px';
        const mobileFontSize = window.innerWidth <= 375 ? '22px' : '24px';

        bottomPosition = hasExperienceFab ?
          'calc(var(--nav-height-mobile, 80px) + 30px)' : // 체험하기 아래, 네비 위 30px
          'calc(var(--nav-height-mobile, 80px) + 30px)';
        rightPosition = mobileRight;
        fabSize = mobileSize;
        fontSize = mobileFontSize;
        break;
    }

    // 강제 스타일 적용 (체험하기보다 낮은 z-index)
    chatFab.style.cssText = `
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
      z-index: 1024 !important;
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

    // 이벤트 바인딩
    chatFab.addEventListener('click', handleChatFabClick);

    // DOM에 추가
    document.body.appendChild(chatFab);
    layoutState.chatFab = chatFab;

    console.log('✅ 채팅 FAB 버튼 생성 완료 (체험하기 아래)', {
      device: deviceType,
      hasExperienceFab: hasExperienceFab,
      bottomPosition: bottomPosition
    });

    return chatFab;

  } catch (error) {
    console.error('❌ 채팅 FAB 버튼 생성 실패:', error);
    return null;
  }
}

/**
 * 🔧 채팅 FAB 클릭 핸들러
 */
function handleChatFabClick(e) {
  e.preventDefault();
  e.stopPropagation();

  console.log('💬 채팅 FAB 클릭 (체험하기 아래)');

  const chatFab = e.currentTarget;

  // 클릭 효과
  chatFab.style.transform = 'scale(0.95)';
  setTimeout(() => {
    chatFab.style.transform = 'scale(1)';
  }, 150);

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

  if (window.showToast) {
    showToast('채팅 기능 준비 중입니다.', 'info', 2000);
  } else {
    alert('채팅 기능 준비 중입니다.');
  }
}

/**
 * 🔧 채팅 FAB 위치 재조정 (체험하기 기준)
 */
function repositionChatFab() {
  if (!layoutState.chatFab) return;

  const deviceType = layoutState.currentDevice || detectDeviceType();
  const experienceFab = document.querySelector('.experience-fab');
  const hasExperienceFab = !!experienceFab;

  let bottomPosition, rightPosition, fabSize, fontSize;

  switch (deviceType) {
    case 'desktop':
      bottomPosition = hasExperienceFab ? '50px' : '50px';
      rightPosition = '40px';
      fabSize = '64px';
      fontSize = '28px';
      break;
    case 'tablet':
      bottomPosition = hasExperienceFab ?
        'calc(var(--nav-height-mobile, 80px) + 35px)' :
        'calc(var(--nav-height-mobile, 80px) + 35px)';
      rightPosition = '30px';
      fabSize = '60px';
      fontSize = '26px';
      break;
    case 'mobile':
    default:
      const mobileRight = window.innerWidth <= 375 ? '16px' : '20px';
      const mobileSize = window.innerWidth <= 375 ? '50px' : '56px';
      const mobileFontSize = window.innerWidth <= 375 ? '22px' : '24px';

      bottomPosition = hasExperienceFab ?
        'calc(var(--nav-height-mobile, 80px) + 30px)' :
        'calc(var(--nav-height-mobile, 80px) + 30px)';
      rightPosition = mobileRight;
      fabSize = mobileSize;
      fontSize = mobileFontSize;
      break;
  }

  // 위치 재조정
  layoutState.chatFab.style.bottom = bottomPosition;
  layoutState.chatFab.style.right = rightPosition;
  layoutState.chatFab.style.width = fabSize;
  layoutState.chatFab.style.height = fabSize;
  layoutState.chatFab.style.fontSize = fontSize;

  console.log('📱 채팅 FAB 위치 재조정 (체험하기 기준):', {
    device: deviceType,
    hasExperienceFab: hasExperienceFab,
    bottom: bottomPosition
  });
}

/**
 * 🔧 사이드바 적용에 따른 레이아웃 최적화
 */
function optimizeLayoutForSidebar() {
  try {
    const elements = {
      layout: document.querySelector('.mobile-layout'),
      main: document.querySelector('.mobile-main'),
      container: document.querySelector('.container'),
      header: document.querySelector('.mobile-header')
    };

    if (!elements.layout || !elements.main) {
      console.error('❌ 필수 레이아웃 요소를 찾을 수 없습니다.');
      return false;
    }

    const headerHeight = elements.header ? elements.header.offsetHeight : 70;

    // 하단 네비게이션 제거로 인한 패딩 조정
    elements.layout.style.paddingTop = `${headerHeight + 10}px`;
    elements.layout.style.paddingBottom = '30px'; // 기존 네비게이션 높이 제거

    // 메인 콘텐츠 여백 조정
    elements.main.style.padding = '15px 0 30px 0';
    elements.main.style.minHeight = `calc(100vh - ${headerHeight}px - 40px)`;

    // 컨테이너 하단 여백 확보
    if (elements.container) {
      elements.container.style.paddingBottom = '30px';
      elements.container.style.minHeight = `calc(100vh - ${headerHeight + 40}px)`;

      const lastChild = elements.container.lastElementChild;
      if (lastChild) {
        lastChild.style.marginBottom = '50px';
      }
    }

    console.log('✅ 사이드바 적용에 따른 레이아웃 최적화 완료');
    return true;

  } catch (error) {
    console.error('❌ 레이아웃 최적화 실패:', error);
    return false;
  }
}

/**
 * 🔧 콘텐츠 모니터링 (하단 네비게이션 제거로 인한 이슈 감지)
 */
function startContentMonitor() {
  if (layoutState.contentMonitor) return;

  const monitor = () => {
    const issues = diagnoseContentLayout();
    if (issues.length > 0) {
      console.warn('⚠️ 콘텐츠 레이아웃 문제 감지:', issues);
      optimizeLayoutForSidebar();

      // 채팅 FAB 재생성
      if (issues.some(issue => issue.type === 'chat-fab-missing' || issue.type === 'chat-fab-hidden')) {
        setTimeout(() => {
          initializeChatFabBelowExperience();
        }, 500);
      }
    }
  };

  layoutState.contentMonitor = setInterval(monitor, 10000);
  console.log('👁️ 콘텐츠 모니터링 시작');
}

/**
 * 🔧 콘텐츠 레이아웃 문제 진단
 */
function diagnoseContentLayout() {
  const elements = {
    header: document.querySelector('.mobile-header'),
    main: document.querySelector('.mobile-main'),
    container: document.querySelector('.container'),
    chatFab: document.querySelector('.chat-fab'),
    experienceFab: document.querySelector('.experience-fab')
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
  } else {
    issues.push({
      type: 'chat-fab-missing',
      message: '채팅 FAB 버튼 요소가 없음'
    });
  }

  // 체험하기 FAB 위치 확인
  if (elements.experienceFab && elements.chatFab) {
    const expRect = elements.experienceFab.getBoundingClientRect();
    const chatRect = elements.chatFab.getBoundingClientRect();

    // 체험하기가 채팅보다 위에 있어야 함 (더 큰 bottom 값)
    if (expRect.bottom <= chatRect.bottom) {
      issues.push({
        type: 'fab-position-conflict',
        message: '체험하기 버튼이 채팅 버튼보다 아래에 위치함'
      });
    }
  }

  return issues;
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

  return layoutState.currentDevice;
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
 * 콘텐츠 레이아웃 조정 (하단 네비게이션 제거 반영)
 */
function adjustContentLayout(deviceType) {
  const layout = document.querySelector('.mobile-layout');
  const header = document.querySelector('.mobile-header');
  const main = document.querySelector('.mobile-main');

  if (!layout || !header || !main) return;

  switch (deviceType) {
    case 'desktop':
      layout.style.marginLeft = 'var(--sidebar-width-pc, 280px)';
      layout.style.paddingTop = 'var(--header-height-pc, 80px)';
      layout.style.paddingBottom = '50px'; // 하단 네비게이션 제거 반영

      if (header) {
        header.style.left = 'var(--sidebar-width-pc, 280px)';
        header.style.height = 'var(--header-height-pc, 80px)';
      }
      break;

    case 'tablet':
    case 'mobile':
    default:
      layout.style.marginLeft = '0';
      layout.style.paddingTop = 'var(--header-height-mobile, 70px)';
      layout.style.paddingBottom = '30px'; // 하단 네비게이션 제거 반영
      layout.style.marginBottom = '0';

      if (header) {
        header.style.left = '0';
        header.style.height = 'var(--header-height-mobile, 70px)';
      }
      break;
  }
}

/**
 * 핵심 DOM 요소 찾기
 */
function findLayoutElements() {
  console.log('핵심 DOM 요소 찾기');

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
    optimizeLayoutForSidebar();

    // 채팅 FAB 위치 재조정
    if (layoutState.chatFab) {
      repositionChatFab();
    }
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
    optimizeLayoutForSidebar();

    // 채팅 FAB 재배치
    if (layoutState.chatFab) {
      repositionChatFab();
    } else {
      // 채팅 FAB이 없으면 재생성
      initializeChatFabBelowExperience();
    }
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
 * 레이아웃 정리
 */
function destroyLayout() {
  console.log('🗑️ 레이아웃 정리');

  if (layoutState.contentMonitor) {
    clearInterval(layoutState.contentMonitor);
    layoutState.contentMonitor = null;
  }

  window.removeEventListener('resize', handleWindowResize);

  if (layoutState.overlay && layoutState.overlay.parentNode) {
    layoutState.overlay.parentNode.removeChild(layoutState.overlay);
  }

  if (layoutState.chatFab && layoutState.chatFab.parentNode) {
    layoutState.chatFab.parentNode.removeChild(layoutState.chatFab);
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
    contentMonitor: null,
    chatFab: null
  };
}

// 전역 함수들
window.openMenu = openMenu;
window.closeMenu = closeMenu;
window.toggleMenu = toggleMenu;

// 레이아웃 매니저 객체
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
  isDesktop: () => layoutState.currentDevice === 'desktop',
  isTablet: () => layoutState.currentDevice === 'tablet',
  isMobile: () => layoutState.currentDevice === 'mobile',
  getBreakpoints: () => BREAKPOINTS,
  optimizeLayout: optimizeLayoutForSidebar,
  initializeChatFab: initializeChatFabBelowExperience,
  repositionChatFab: repositionChatFab
};

// 자동 초기화
console.log('🌟 토마토 사이드바 적용 layout.js 스크립트 로드 완료 (채팅 FAB 아래)');
initializeLayout();

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', () => {
  destroyLayout();
});

// 모듈 익스포트
export {
  initializeLayout,
  openMenu,
  closeMenu,
  toggleMenu,
  showPageLoading,
  hidePageLoading,
  detectDeviceType,
  handleDeviceTypeChange,
  initializeResponsiveLayout,
  optimizeLayoutForSidebar,
  initializeChatFabBelowExperience,
  createChatFabBelow,
  handleChatFabClick,
  repositionChatFab,
  startContentMonitor
};