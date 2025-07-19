// layout.js - 토마토리멤버 완전 반응형 레이아웃 관리

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
  currentDevice: 'mobile', // 'mobile', 'tablet', 'desktop'
  resizeTimeout: null
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
  console.log('🚀 완전 반응형 레이아웃 매니저 초기화 시작');

  if (layoutState.isInitialized) {
    console.warn('⚠️ 레이아웃이 이미 초기화되었습니다.');
    return;
  }

  // DOM 로드 완료 확인
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
    // 디바이스 타입 감지
    detectDeviceType();

    // 핵심 요소 찾기
    findLayoutElements();

    // 이벤트 바인딩
    bindLayoutEvents();

    // 반응형 레이아웃 초기화
    initializeResponsiveLayout();

    // 네비게이션 초기화
    initializeNavigation();

    // 스크롤 동작 초기화
    initializeScrollBehavior();

    // 햄버거 메뉴 초기화 (모바일/태블릿만)
    if (layoutState.currentDevice !== 'desktop') {
      initializeHamburgerMenu();
    }

    // 뷰포트 높이 설정
    updateViewportHeight();

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

  // body에 디바이스 타입 속성 추가
  document.body.setAttribute('data-device', layoutState.currentDevice);

  console.log('📱 디바이스 타입:', layoutState.currentDevice, `(${width}px)`);
}

/**
 * 반응형 레이아웃 초기화
 */
function initializeResponsiveLayout() {
  console.log('🖥️ 반응형 레이아웃 초기화');

  // 디바이스별 초기화
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

  // 사이드바 표시
  const sidebar = document.querySelector('.pc-sidebar');
  if (sidebar) {
    sidebar.style.display = 'block';
  }

  // 햄버거 메뉴 숨김
  const menuBtn = document.querySelector('.header-menu-btn');
  if (menuBtn) {
    menuBtn.style.display = 'none';
  }

  // 모바일 메뉴 강제 닫기
  if (layoutState.isMenuOpen) {
    closeMenu();
  }

  // 콘텐츠 영역 조정
  adjustContentLayout('desktop');
}

/**
 * 태블릿 레이아웃 초기화
 */
function initializeTabletLayout() {
  console.log('📱 태블릿 레이아웃 초기화');

  // 사이드바 숨김
  const sidebar = document.querySelector('.pc-sidebar');
  if (sidebar) {
    sidebar.style.display = 'none';
  }

  // 햄버거 메뉴 표시
  const menuBtn = document.querySelector('.header-menu-btn');
  if (menuBtn) {
    menuBtn.style.display = 'flex';
  }

  // 콘텐츠 영역 조정
  adjustContentLayout('tablet');
}

/**
 * 모바일 레이아웃 초기화
 */
function initializeMobileLayout() {
  console.log('📱 모바일 레이아웃 초기화');

  // 사이드바 숨김
  const sidebar = document.querySelector('.pc-sidebar');
  if (sidebar) {
    sidebar.style.display = 'none';
  }

  // 햄버거 메뉴 표시
  const menuBtn = document.querySelector('.header-menu-btn');
  if (menuBtn) {
    menuBtn.style.display = 'flex';
  }

  // 콘텐츠 영역 조정
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
      // CSS 변수로 조정 (PC 레이아웃)
      layout.style.marginLeft = 'var(--sidebar-width-pc, 280px)';
      layout.style.paddingTop = 'var(--header-height-pc, 80px)';
      layout.style.paddingBottom = '0';

      if (header) {
        header.style.left = 'var(--sidebar-width-pc, 280px)';
        header.style.height = 'var(--header-height-pc, 80px)';
      }

      if (nav) {
        nav.style.position = 'static';
        nav.style.marginLeft = 'var(--sidebar-width-pc, 280px)';
      }
      break;

    case 'tablet':
    case 'mobile':
    default:
      // 모바일 레이아웃 (기존)
      layout.style.marginLeft = '0';
      layout.style.paddingTop = 'var(--header-height-mobile, 70px)';
      layout.style.paddingBottom = 'var(--nav-height-mobile, 80px)';

      if (header) {
        header.style.left = '0';
        header.style.height = 'var(--header-height-mobile, 70px)';
      }

      if (nav) {
        nav.style.position = 'fixed';
        nav.style.marginLeft = '0';
      }
      break;
  }
}

/**
 * 핵심 DOM 요소 찾기
 */
function findLayoutElements() {
  console.log('🔍 핵심 DOM 요소 찾기');

  // 햄버거 메뉴 관련 요소 (모바일/태블릿만)
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

  // 데스크톱이 아닌 경우에만 필수 요소 체크
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

  // 윈도우 리사이즈 이벤트 (디바운스 적용)
  window.addEventListener('resize', debounce(() => {
    handleWindowResize();
  }, 250));

  // 키보드 이벤트
  document.addEventListener('keydown', handleKeyboardEvents);

  // 네비게이션 아이템 클릭 이벤트
  const navItems = document.querySelectorAll('.nav-item, .sidebar-menu-link');
  navItems.forEach(item => {
    item.addEventListener('click', (e) => {
      handleNavItemClick(e, item);
    });
  });

  // 메뉴 링크 클릭 이벤트 (개선됨)
  bindMenuLinkEvents();

  // 사이드바 메뉴 링크 이벤트 (PC용)
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

      // 로그아웃 버튼 특별 처리
      if (link.classList.contains('logout-btn')) {
        e.preventDefault();
        handleLogout();
        return;
      }

      // disabled 링크 처리
      if (link.classList.contains('disabled')) {
        e.preventDefault();
        showToast('준비 중인 기능입니다.', 'info', 2000);
        return;
      }

      // 외부 링크 처리
      if (href && (href.startsWith('http') || href.startsWith('tel:') || href.startsWith('mailto:'))) {
        e.preventDefault();
        window.open(href, '_blank', 'noopener,noreferrer');
        return;
      }

      // 같은 페이지 링크 확인
      if (href === window.location.pathname) {
        e.preventDefault();
        showToast('현재 페이지입니다.', 'info', 2000);
        return;
      }

      // 페이지 로딩 표시
      showNavigationLoading(link);
    });
  });
}

/**
 * 메뉴 링크 이벤트 바인딩 (개선된 버전)
 */
function bindMenuLinkEvents() {
  const menuLinks = document.querySelectorAll('.menu-link');

  menuLinks.forEach(link => {
    link.addEventListener('click', (e) => {
      const href = link.getAttribute('href');
      const menuText = link.querySelector('.menu-text')?.textContent?.trim() || '알 수 없는 메뉴';

      console.log('🔗 메뉴 링크 클릭:', menuText, href);

      // 로그아웃 버튼 특별 처리
      if (link.classList.contains('logout-btn')) {
        e.preventDefault();
        handleLogout();
        return;
      }

      // disabled 링크 처리
      if (link.classList.contains('disabled')) {
        e.preventDefault();
        showToast('준비 중인 기능입니다.', 'info', 2000);
        return;
      }

      // 외부 링크 처리
      if (href && (href.startsWith('http') || href.startsWith('tel:') || href.startsWith('mailto:'))) {
        e.preventDefault();
        window.open(href, '_blank', 'noopener,noreferrer');
        return;
      }

      // 같은 페이지 링크 확인
      if (href === window.location.pathname) {
        e.preventDefault();
        closeMenu();
        showToast('현재 페이지입니다.', 'info', 2000);
        return;
      }

      // 일반 링크는 메뉴 닫기 후 이동
      setTimeout(() => closeMenu(), 150);

      // 페이지 로딩 표시
      showNavigationLoading(link);
    });
  });
}

/**
 * 네비게이션 초기화
 */
function initializeNavigation() {
  console.log('🧭 네비게이션 초기화');

  // 활성 네비게이션 업데이트
  updateActiveNavigation();

  // 접근성 향상
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
 * 햄버거 메뉴 초기화 (모바일/태블릿만)
 */
function initializeHamburgerMenu() {
  console.log('🍔 햄버거 메뉴 초기화');

  if (!layoutState.menuElement || !layoutState.menuButton || !layoutState.overlay) {
    console.warn('⚠️ 햄버거 메뉴 필수 요소가 없어 초기화를 건너뜁니다.');
    return;
  }

  // 메뉴 초기 상태 설정
  setMenuInitialState();

  // 메뉴 버튼 이벤트 바인딩
  bindMenuButtonEvents();

  // 메뉴 내부 이벤트 바인딩
  bindMenuInternalEvents();

  console.log('✅ 햄버거 메뉴 초기화 완료');
}

/**
 * 메뉴 초기 상태 설정
 */
function setMenuInitialState() {
  console.log('🎨 메뉴 초기 상태 설정');

  if (!layoutState.menuElement) return;

  // 메뉴를 완전히 숨김
  layoutState.menuElement.classList.remove('show');
  layoutState.menuElement.style.transform = 'translateX(100%)';
  layoutState.menuElement.style.visibility = 'hidden';

  // 오버레이 숨김
  if (layoutState.overlay) {
    layoutState.overlay.classList.remove('show');
  }

  // 초기 상태 설정
  layoutState.isMenuOpen = false;

  console.log('✅ 메뉴 초기 상태 설정 완료');
}

/**
 * 메뉴 버튼 이벤트 바인딩
 */
function bindMenuButtonEvents() {
  if (!layoutState.menuButton) {
    console.warn('⚠️ 메뉴 버튼이 없어서 이벤트 바인딩을 건너뜁니다.');
    return;
  }

  console.log('🔘 메뉴 버튼 이벤트 바인딩');

  // 클릭 이벤트 바인딩
  layoutState.menuButton.addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();

    console.log('🔘 메뉴 버튼 클릭');
    console.log('  현재 상태:', layoutState.isMenuOpen ? '열림' : '닫힌');
    console.log('  디바이스:', layoutState.currentDevice);

    // 데스크톱에서는 메뉴 토글 비활성화
    if (layoutState.currentDevice === 'desktop') {
      console.log('🖥️ 데스크톱에서는 햄버거 메뉴를 사용하지 않습니다.');
      return;
    }

    toggleMenu();
  });

  console.log('✅ 메뉴 버튼 이벤트 바인딩 완료');
}

/**
 * 메뉴 내부 이벤트 바인딩
 */
function bindMenuInternalEvents() {
  if (!layoutState.menuElement) return;

  console.log('🔗 메뉴 내부 이벤트 바인딩');

  // 닫기 버튼 이벤트
  const closeBtn = document.getElementById('menuCloseBtn');
  if (closeBtn) {
    closeBtn.addEventListener('click', (e) => {
      e.preventDefault();
      console.log('❌ 메뉴 닫기 버튼 클릭');
      closeMenu();
    });
  }

  // 오버레이 클릭 이벤트
  if (layoutState.overlay) {
    layoutState.overlay.addEventListener('click', () => {
      console.log('🌫️ 오버레이 클릭 - 메뉴 닫기');
      closeMenu();
    });
  }

  console.log('✅ 메뉴 내부 이벤트 바인딩 완료');
}

/**
 * 메뉴 토글
 */
function toggleMenu() {
  console.log('🔄 메뉴 토글');
  console.log('  현재 상태:', layoutState.isMenuOpen ? '열림' : '닫힌');
  console.log('  디바이스:', layoutState.currentDevice);

  // 데스크톱에서는 메뉴 토글 비활성화
  if (layoutState.currentDevice === 'desktop') {
    console.log('🖥️ 데스크톱에서는 사이드바를 사용합니다.');
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
  console.log('🔓 메뉴 열기');

  // 데스크톱에서는 메뉴 열기 비활성화
  if (layoutState.currentDevice === 'desktop') {
    console.log('🖥️ 데스크톱에서는 메뉴를 열 수 없습니다.');
    return;
  }

  if (!layoutState.menuElement || !layoutState.overlay) {
    console.error('❌ 메뉴 열기 실패 - 필수 요소 누락');
    return;
  }

  layoutState.isMenuOpen = true;

  // 햄버거 아이콘 애니메이션
  if (layoutState.menuButton) {
    layoutState.menuButton.classList.add('menu-open');
  }

  // 메뉴 표시
  layoutState.menuElement.classList.add('show');
  layoutState.menuElement.style.transform = 'translateX(0)';
  layoutState.menuElement.style.visibility = 'visible';

  // 오버레이 표시
  layoutState.overlay.classList.add('show');

  // 스크롤 방지
  document.body.classList.add('menu-open');

  // 포커스 관리
  setTimeout(() => {
    const firstFocusable = layoutState.menuElement.querySelector('a, button');
    if (firstFocusable) {
      firstFocusable.focus();
    }
  }, 100);

  console.log('✅ 메뉴 열기 완료');
}

/**
 * 메뉴 닫기
 */
function closeMenu() {
  console.log('🔒 메뉴 닫기');

  if (!layoutState.menuElement || !layoutState.overlay) {
    console.error('❌ 메뉴 닫기 실패 - 필수 요소 누락');
    return;
  }

  layoutState.isMenuOpen = false;

  // 햄버거 아이콘 애니메이션 리셋
  if (layoutState.menuButton) {
    layoutState.menuButton.classList.remove('menu-open');
  }

  // 메뉴 숨김
  layoutState.menuElement.classList.remove('show');
  layoutState.menuElement.style.transform = 'translateX(100%)';
  layoutState.menuElement.style.visibility = 'hidden';

  // 오버레이 숨김
  layoutState.overlay.classList.remove('show');

  // 스크롤 복구
  document.body.classList.remove('menu-open');

  // 포커스 복원
  if (layoutState.menuButton) {
    layoutState.menuButton.focus();
  }

  console.log('✅ 메뉴 닫기 완료');
}

/**
 * 윈도우 리사이즈 처리 (반응형 핵심)
 */
function handleWindowResize() {
  console.log('📱 윈도우 리사이즈');

  const prevDevice = layoutState.currentDevice;

  // 디바이스 타입 재감지
  detectDeviceType();

  const currentDevice = layoutState.currentDevice;

  console.log(`📱 디바이스 변경: ${prevDevice} → ${currentDevice}`);

  // 디바이스 타입이 변경된 경우
  if (prevDevice !== currentDevice) {
    handleDeviceTypeChange(prevDevice, currentDevice);
  }

  // 뷰포트 높이 업데이트
  updateViewportHeight();

  // 콘텐츠 레이아웃 조정
  adjustContentLayout(currentDevice);

  console.log('✅ 윈도우 리사이즈 처리 완료');
}

/**
 * 디바이스 타입 변경 처리
 */
function handleDeviceTypeChange(prevDevice, currentDevice) {
  console.log(`🔄 디바이스 타입 변경: ${prevDevice} → ${currentDevice}`);

  // 메뉴 상태 정리
  if (layoutState.isMenuOpen && currentDevice === 'desktop') {
    console.log('🖥️ 데스크톱으로 변경 - 모바일 메뉴 닫기');
    closeMenu();
  }

  // 디바이스별 레이아웃 초기화
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

  // 햄버거 메뉴 재초기화 (필요한 경우)
  if (currentDevice !== 'desktop' && (prevDevice === 'desktop' || !layoutState.menuElement)) {
    findLayoutElements();
    if (layoutState.menuElement && layoutState.menuButton && layoutState.overlay) {
      initializeHamburgerMenu();
    }
  }

  // 네비게이션 상태 업데이트
  updateActiveNavigation();
}

/**
 * 스크롤 처리 (앱 네비게이션만)
 */
function handleScroll() {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
  const header = document.getElementById('mobileHeader');

  // 헤더 스크롤 효과 (공통)
  if (header) {
    if (scrollTop > 50) {
      header.classList.add('scrolled');
    } else {
      header.classList.remove('scrolled');
    }
  }

  // 앱 네비게이션은 항상 고정 - 숨김/표시 로직 제거
  layoutState.lastScrollTop = scrollTop;

  // 스크롤 종료 감지
  clearTimeout(layoutState.scrollTimeout);
  layoutState.scrollTimeout = setTimeout(() => {
    layoutState.isScrolling = false;
  }, 150);
}

/**
 * 키보드 이벤트 처리
 */
function handleKeyboardEvents(e) {
  // ESC 키로 메뉴 닫기 (모바일/태블릿만)
  if (e.key === 'Escape' && layoutState.isMenuOpen && layoutState.currentDevice !== 'desktop') {
    console.log('⌨️ ESC 키로 메뉴 닫기');
    e.preventDefault();
    closeMenu();
  }

  // 단축키 지원 (데스크톱만)
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

  console.log('🧭 네비게이션 클릭:', itemText, href);

  // disabled 링크 처리
  if (item.classList.contains('disabled')) {
    e.preventDefault();
    showToast('준비 중인 기능입니다.', 'info', 2000);
    return;
  }

  // 같은 페이지 클릭 시 새로고침
  if (href === window.location.pathname) {
    e.preventDefault();
    showToast(`${itemText} 페이지를 새로고침합니다.`, 'info', 2000);
    setTimeout(() => {
      window.location.reload();
    }, 500);
    return;
  }

  // 로딩 상태 표시
  showNavigationLoading(item);

  // 페이지 전환 애니메이션
  animatePageTransition();
}

/**
 * 활성 네비게이션 업데이트
 */
function updateActiveNavigation() {
  const currentPath = window.location.pathname;

  // 사이드바 네비게이션 업데이트 (PC)
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

  // 하단 네비게이션 업데이트 (모바일/태블릿)
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
  // 사이드바 접근성
  const sidebarItems = document.querySelectorAll('.sidebar-menu-link');
  sidebarItems.forEach((item, index) => {
    const text = item.querySelector('span')?.textContent;
    if (text) {
      item.setAttribute('title', `${text} (Alt+${index + 1})`);
      item.setAttribute('aria-label', `${text} 페이지로 이동`);
    }
  });

  // 하단 네비게이션 접근성
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
  console.log('👋 로그아웃 처리');

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

    // 로딩 상태 표시
    showPageLoading();

    // 메뉴 닫기 (모바일/태블릿)
    if (layoutState.currentDevice !== 'desktop') {
      closeMenu();
    }

    // 토스트 메시지 표시
    showToast('로그아웃 중입니다...', 'info');

    // 로그아웃 처리
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

  // 2초 후 원래 아이콘 복원
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
 * 스로틀 유틸리티
 */
function throttle(func, limit) {
  let inThrottle;
  return function(...args) {
    if (!inThrottle) {
      func.apply(this, args);
      inThrottle = true;
      setTimeout(() => inThrottle = false, limit);
    }
  };
}

/**
 * 메뉴 상태 체크 (디버그용)
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

  // 이벤트 리스너 제거
  window.removeEventListener('resize', handleWindowResize);

  // 오버레이 제거
  if (layoutState.overlay && layoutState.overlay.parentNode) {
    layoutState.overlay.parentNode.removeChild(layoutState.overlay);
  }

  // 타이머 정리
  if (layoutState.scrollTimeout) {
    clearTimeout(layoutState.scrollTimeout);
  }

  if (layoutState.resizeTimeout) {
    clearTimeout(layoutState.resizeTimeout);
  }

  // 상태 초기화
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
    resizeTimeout: null
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

      // 네비게이션 상태 업데이트
      updateActiveNavigation();

      // 디바이스 타입 재확인
      const prevDevice = layoutState.currentDevice;
      detectDeviceType();

      if (prevDevice !== layoutState.currentDevice) {
        handleDeviceTypeChange(prevDevice, layoutState.currentDevice);
      }
    }
  });
}

// 전역 함수들 (하위 호환성)
window.openMenu = openMenu;
window.closeMenu = closeMenu;
window.toggleMenu = toggleMenu;
window.checkMenuState = checkMenuState;

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

  // 새로운 반응형 관련 메서드들
  isDesktop: () => layoutState.currentDevice === 'desktop',
  isTablet: () => layoutState.currentDevice === 'tablet',
  isMobile: () => layoutState.currentDevice === 'mobile',
  getBreakpoints: () => BREAKPOINTS
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
  console.groupEnd();
};

// 자동 초기화
console.log('🌟 완전 반응형 layout.js 스크립트 로드 완료');
initializeLayout();

// 추가 이벤트 핸들러 초기화
handleConnectionChange();
handleVisibilityChange();

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', () => {
  destroyLayout();
});

// 앱 네비게이션 설정 (기존 유지)
window.APP_CONFIG = window.APP_CONFIG || {};
window.APP_CONFIG.appNavigation = {
  apiUrl: 'https://api.otongtong.net/v1/api/etc/app/menu_list',
  params: {
    app_type: 'newstong',
    nation_code: 'KR',
    device: 'android',
    mode: 'dev'  // 운영환경에서는 'prod'로 변경
  }
};

// 모듈 익스포트 (필요한 경우)
export {
  initializeLayout,
  openMenu,
  closeMenu,
  toggleMenu,
  showPageLoading,
  hidePageLoading,
  checkMenuState,
  // 새로운 반응형 관련 함수들
  detectDeviceType,
  handleDeviceTypeChange,
  initializeResponsiveLayout
};