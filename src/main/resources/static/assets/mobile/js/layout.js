// layout.js - 토마토리멤버 모바일 레이아웃 관리 (Function 기반)

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
  isInitialized: false
};

/**
 * 레이아웃 매니저 초기화
 */
function initializeLayout() {
  console.log('🚀 레이아웃 매니저 초기화 시작');

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
    // 핵심 요소 찾기
    findLayoutElements();

    // 이벤트 바인딩
    bindLayoutEvents();

    // 네비게이션 초기화
    initializeNavigation();

    // 스크롤 동작 초기화
    initializeScrollBehavior();

    // 햄버거 메뉴 초기화
    initializeHamburgerMenu();

    // 뷰포트 높이 설정
    updateViewportHeight();

    layoutState.isInitialized = true;
    console.log('✅ 레이아웃 컴포넌트 초기화 완료');

  } catch (error) {
    console.error('❌ 레이아웃 초기화 실패:', error);
  }
}

/**
 * 핵심 DOM 요소 찾기
 */
function findLayoutElements() {
  console.log('🔍 핵심 DOM 요소 찾기');

  // 햄버거 메뉴 관련 요소
  layoutState.menuElement = document.getElementById('mobileMenu');
  layoutState.menuButton = document.getElementById('menuToggleBtn');
  layoutState.overlay = document.getElementById('menuOverlay');

  console.log('📱 요소 검색 결과:');
  console.log('  메뉴 요소:', !!layoutState.menuElement);
  console.log('  메뉴 버튼:', !!layoutState.menuButton);
  console.log('  오버레이:', !!layoutState.overlay);

  // 필수 요소 누락 시 경고
  if (!layoutState.menuElement || !layoutState.menuButton || !layoutState.overlay) {
    console.warn('⚠️ 일부 필수 요소를 찾을 수 없습니다. 햄버거 메뉴 기능이 제한될 수 있습니다.');
  }
}

/**
 * 이벤트 바인딩
 */
function bindLayoutEvents() {
  console.log('🔗 레이아웃 이벤트 바인딩');

  // 윈도우 리사이즈 이벤트
  window.addEventListener('resize', debounce(() => {
    handleWindowResize();
  }, 250));

  // 키보드 이벤트
  document.addEventListener('keydown', handleKeyboardEvents);

  // 네비게이션 아이템 클릭 이벤트
  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach(item => {
    item.addEventListener('click', (e) => {
      handleNavItemClick(e, item);
    });
  });

  console.log('✅ 레이아웃 이벤트 바인딩 완료');
}

/**
 * 네비게이션 초기화
 */
function initializeNavigation() {
  console.log('🧭 네비게이션 초기화');

  // 활성 네비게이션 업데이트
  updateActiveNavigation();

  // 네비게이션 배지 업데이트
  updateNavigationBadges();

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
 * 햄버거 메뉴 초기화
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
    console.log('  현재 상태:', layoutState.isMenuOpen ? '열림' : '닫힘');

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

  // 메뉴 링크 클릭 시 메뉴 닫기
  const menuLinks = layoutState.menuElement.querySelectorAll('.menu-link');
  menuLinks.forEach(link => {
    link.addEventListener('click', (e) => {
      console.log('🔗 메뉴 링크 클릭:', link.textContent?.trim());

      // 로그아웃 버튼이면 특별 처리
      if (link.classList.contains('logout-btn')) {
        e.preventDefault();
        handleLogout();
        return;
      }

      // 일반 링크는 메뉴 닫기
      setTimeout(() => closeMenu(), 150);
    });
  });

  console.log('✅ 메뉴 내부 이벤트 바인딩 완료');
}

/**
 * 메뉴 토글
 */
function toggleMenu() {
  console.log('🔄 메뉴 토글');
  console.log('  현재 상태:', layoutState.isMenuOpen ? '열림' : '닫힘');

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
 * 스크롤 처리
 */
function handleScroll() {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
  const header = document.getElementById('mobileHeader');
  const nav = document.getElementById('mobileNav');

  // 헤더 스크롤 효과
  if (header) {
    if (scrollTop > 50) {
      header.classList.add('scrolled');
    } else {
      header.classList.remove('scrolled');
    }
  }

  // 네비게이션 자동 숨김/표시
  if (Math.abs(scrollTop - layoutState.lastScrollTop) > 5) {
    if (scrollTop > layoutState.lastScrollTop && scrollTop > 100) {
      // 아래로 스크롤 시 네비게이션 숨김
      if (nav) nav.classList.add('hidden');
    } else {
      // 위로 스크롤 시 네비게이션 표시
      if (nav) nav.classList.remove('hidden');
    }
    layoutState.lastScrollTop = scrollTop;
  }
}

/**
 * 윈도우 리사이즈 처리
 */
function handleWindowResize() {
  console.log('📱 윈도우 리사이즈');

  // 뷰포트 높이 업데이트
  updateViewportHeight();

  // 데스크톱 크기에서 메뉴 자동 닫기
  if (layoutState.isMenuOpen && window.innerWidth > 768) {
    closeMenu();
  }

  // 네비게이션 배지 업데이트
  updateNavigationBadges();
}

/**
 * 키보드 이벤트 처리
 */
function handleKeyboardEvents(e) {
  // ESC 키로 메뉴 닫기
  if (e.key === 'Escape' && layoutState.isMenuOpen) {
    console.log('⌨️ ESC 키로 메뉴 닫기');
    closeMenu();
  }
}

/**
 * 네비게이션 아이템 클릭 처리
 */
function handleNavItemClick(e, item) {
  const href = item.getAttribute('href');

  // 같은 페이지 클릭 시 새로고침
  if (href === window.location.pathname) {
    e.preventDefault();
    window.location.reload();
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
  const navItems = document.querySelectorAll('.nav-item');

  navItems.forEach(item => {
    const href = item.getAttribute('href');

    if (href === currentPath || (href !== '/' && currentPath.startsWith(href))) {
      item.classList.add('active');
    } else {
      item.classList.remove('active');
    }
  });
}

/**
 * 네비게이션 배지 업데이트
 */
async function updateNavigationBadges() {
  try {
    const accessToken = localStorage.getItem('accessToken');
    if (!accessToken) {
      console.log('🔐 로그인 안됨 - 네비게이션 배지 업데이트 스킵');
      return;
    }

    const response = await fetch('/api/navigation/badges', {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    });

    if (response.ok) {
      const data = await response.json();
      renderNavigationBadges(data.response);
    }
  } catch (error) {
    console.error('네비게이션 배지 업데이트 실패:', error);
  }
}

/**
 * 네비게이션 배지 렌더링
 */
function renderNavigationBadges(badges) {
  Object.entries(badges).forEach(([menu, count]) => {
    const navItem = document.querySelector(`[href*="${menu}"]`);
    if (navItem && count > 0) {
      let badge = navItem.querySelector('.nav-badge');

      if (!badge) {
        badge = document.createElement('span');
        badge.className = 'nav-badge';
        navItem.appendChild(badge);
      }

      badge.textContent = count > 99 ? '99+' : count;
      badge.style.cssText = `
        position: absolute;
        top: -2px;
        right: -2px;
        background: #e53e3e;
        color: white;
        border-radius: 50%;
        width: 18px;
        height: 18px;
        font-size: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 600;
      `;
    }
  });
}

/**
 * 네비게이션 접근성 향상
 */
function enhanceNavigationAccessibility() {
  const navItems = document.querySelectorAll('.nav-item');

  navItems.forEach((item, index) => {
    const text = item.querySelector('span')?.textContent;
    if (text) {
      item.setAttribute('title', `${text} (단축키: ${index + 1})`);
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
      '로그아웃 하시겠습니까?',
      '로그아웃',
      '취소'
    );

    if (!confirmed) {
      return;
    }

    // 로딩 상태 표시
    showPageLoading();

    // 메뉴 닫기
    closeMenu();

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

    setTimeout(() => {
      main.style.opacity = '';
      main.style.transform = '';
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

  // 이벤트 리스너 제거
  if (layoutState.overlay && layoutState.overlay.parentNode) {
    layoutState.overlay.parentNode.removeChild(layoutState.overlay);
  }

  // 상태 초기화
  layoutState = {
    isMenuOpen: false,
    lastScrollTop: 0,
    isScrolling: false,
    menuElement: null,
    menuButton: null,
    overlay: null,
    isInitialized: false
  };
}

// 전역 함수들 (하위 호환성)
window.openMenu = openMenu;
window.closeMenu = closeMenu;
window.toggleMenu = toggleMenu;
window.layoutManager = {
  openMenu,
  closeMenu,
  toggleMenu,
  isMenuOpen: () => layoutState.isMenuOpen,
  destroy: destroyLayout
};

// 전역 디버깅 함수
window.debugLayout = function() {
  console.group('🔍 레이아웃 디버그 정보');
  console.log('레이아웃 상태:', layoutState);
  console.log('메뉴 요소:', layoutState.menuElement);
  console.log('메뉴 버튼:', layoutState.menuButton);
  console.log('오버레이:', layoutState.overlay);
  console.log('메뉴 열림 상태:', layoutState.isMenuOpen);
  console.log('초기화 상태:', layoutState.isInitialized);
  console.groupEnd();
};

// 자동 초기화
console.log('🌟 layout.js 스크립트 로드 완료');
initializeLayout();

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', () => {
  destroyLayout();
});

// 모듈 익스포트 (필요한 경우)
export {
  initializeLayout,
  openMenu,
  closeMenu,
  toggleMenu,
  updateNavigationBadges,
  showPageLoading,
  hidePageLoading
};