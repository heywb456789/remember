// layout.js - 토마토리멤버 모바일 레이아웃 관리 (디버깅 강화)

/**
 * 모바일 레이아웃 관리 클래스
 */
class MobileLayoutManager {
  constructor() {
    this.isMenuOpen = false;
    this.scrollPosition = 0;
    this.lastScrollTop = 0;
    this.menuElement = null;
    this.overlay = null;
    this.menuButton = null;
    this.init();
  }

  /**
   * 초기화
   */
  init() {
    console.log('🚀 MobileLayoutManager 초기화 시작');

    // DOM 로드 완료 대기
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', () => {
        this.initializeComponents();
      });
    } else {
      this.initializeComponents();
    }
  }

  /**
   * 컴포넌트 초기화
   */
  initializeComponents() {
    console.log('🔧 컴포넌트 초기화 시작');

    this.bindEvents();
    this.initNavigation();
    this.initScrollBehavior();
    this.initOffcanvas();
    // this.updateActiveNavigation(); -> 알림 뱃지 기능 임시 주석
    // this.initPWAFeatures();

    console.log('✅ 컴포넌트 초기화 완료');
  }

  /**
   * 이벤트 바인딩
   */
  bindEvents() {
    console.log('🔗 이벤트 바인딩 시작');

    // 메뉴 버튼 이벤트는 initOffcanvas에서 처리
    // 나머지 이벤트들...

    // 네비게이션 아이템 클릭 이벤트
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
      item.addEventListener('click', (e) => {
        this.handleNavClick(e, item);
      });
    });

    // 윈도우 리사이즈 이벤트
    window.addEventListener('resize', () => {
      this.handleResize();
    });

    // 키보드 이벤트
    document.addEventListener('keydown', (e) => {
      this.handleKeydown(e);
    });

    console.log('✅ 이벤트 바인딩 완료');
  }

  /**
   * Offcanvas 메뉴 초기화 (강화된 디버깅)
   */
  initOffcanvas() {
    console.log('🔍 Offcanvas 초기화 시작');
    console.log('📊 현재 DOM 상태:', document.readyState);

    // DOM 요소 찾기
    const offcanvasElement = document.getElementById('mobileMenu');
    const menuButton = document.querySelector('.header-menu-btn');

    console.log('🔍 요소 검색 결과:');
    console.log('  📱 mobileMenu:', offcanvasElement);
    console.log('  🔘 header-menu-btn:', menuButton);

    // 모든 관련 요소 확인
    const allMenuElements = document.querySelectorAll('[id*="menu"], [class*="menu"]');
    const allHeaderElements = document.querySelectorAll('[class*="header"]');

    console.log('🔍 모든 메뉴 관련 요소:', allMenuElements);
    console.log('🔍 모든 헤더 관련 요소:', allHeaderElements);

    if (offcanvasElement && menuButton) {
      this.menuElement = offcanvasElement;
      this.menuButton = menuButton;

      console.log('✅ 요소 찾기 성공');

      // 커스텀 메뉴 초기화
      this.initCustomMenu();

      // 메뉴 버튼 이벤트 바인딩
      this.bindMenuButtonEvents();

      console.log('✅ Offcanvas 초기화 완료');
    } else {
      console.error('❌ 필수 요소를 찾을 수 없습니다');
      console.error('  📱 mobileMenu 존재:', !!offcanvasElement);
      console.error('  🔘 menu button 존재:', !!menuButton);

      // 대안 초기화 시도
      this.fallbackInitialization();
    }
  }

  /**
   * 메뉴 버튼 이벤트 바인딩
   */
  bindMenuButtonEvents() {
    if (!this.menuButton) {
      console.error('❌ 메뉴 버튼이 없어서 이벤트 바인딩 실패');
      return;
    }

    console.log('🔘 메뉴 버튼 이벤트 바인딩 시작');
    console.log('🔘 메뉴 버튼 요소:', this.menuButton);

    // 기존 이벤트 리스너 제거
    const existingHandler = this.menuButton.onclick;
    if (existingHandler) {
      console.log('🔄 기존 onclick 핸들러 제거');
      this.menuButton.onclick = null;
    }

    // 새 이벤트 리스너 추가
    this.handleMenuClick = (e) => {
      console.log('🔘 메뉴 버튼 클릭 이벤트 발생!');
      console.log('  📊 이벤트 타입:', e.type);
      console.log('  📊 이벤트 target:', e.target);
      console.log('  🔄 현재 메뉴 상태:', this.isMenuOpen ? '열림' : '닫힘');

      e.preventDefault();
      e.stopPropagation();

      this.toggleMenu();
    };

    // 여러 이벤트 타입으로 바인딩
    this.menuButton.addEventListener('click', this.handleMenuClick);
    this.menuButton.addEventListener('touchstart', this.handleMenuClick);

    // onclick 속성도 설정 (중복 방지)
    this.menuButton.onclick = (e) => {
      console.log('🔘 onclick 속성 이벤트 발생');
      this.handleMenuClick(e);
    };

    // 이벤트 바인딩 확인
    console.log('🔘 이벤트 리스너 바인딩 완료');
    console.log('  📊 click 이벤트 존재:', !!this.menuButton.onclick);
    console.log('  📊 addEventListener 완료');

    // 테스트 클릭 시뮬레이션
    setTimeout(() => {
      console.log('🧪 5초 후 테스트 클릭 시뮬레이션');
      console.log('  📊 메뉴 버튼 클릭 시뮬레이션 실행');
      // this.menuButton.click(); // 자동 테스트 (필요시 주석 해제)
    }, 5000);
  }

  /**
   * 커스텀 메뉴 초기화
   */
  initCustomMenu() {
    if (!this.menuElement) return;

    console.log('🎨 커스텀 메뉴 스타일 설정');

    // 메뉴 스타일 설정
    this.menuElement.style.cssText = `
      position: fixed;
      top: 0;
      right: 0;
      height: 100vh;
      width: 320px;
      background: white;
      box-shadow: -10px 0 30px rgba(0, 0, 0, 0.1);
      transform: translateX(100%);
      transition: transform 0.3s ease;
      z-index: 1050;
      overflow-y: auto;
      display: block;
    `;

    // 오버레이 생성
    this.createOverlay();

    // 메뉴 내부 이벤트 바인딩
    this.bindMenuInternalEvents();

    console.log('✅ 커스텀 메뉴 초기화 완료');
  }

  /**
   * 오버레이 생성
   */
  createOverlay() {
    // 기존 오버레이 제거
    const existingOverlay = document.querySelector('.menu-overlay');
    if (existingOverlay) {
      existingOverlay.remove();
    }

    // 새 오버레이 생성
    this.overlay = document.createElement('div');
    this.overlay.className = 'menu-overlay';
    this.overlay.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.5);
      z-index: 1040;
      opacity: 0;
      visibility: hidden;
      transition: all 0.3s ease;
    `;

    document.body.appendChild(this.overlay);

    // 오버레이 클릭 이벤트
    this.overlay.addEventListener('click', () => {
      console.log('🌫️ 오버레이 클릭 - 메뉴 닫기');
      this.closeMenu();
    });

    console.log('🌫️ 오버레이 생성 완료');
  }

  /**
   * 메뉴 내부 이벤트 바인딩
   */
  bindMenuInternalEvents() {
    if (!this.menuElement) return;

    // 닫기 버튼 이벤트
    const closeBtn = this.menuElement.querySelector('#menuCloseBtn');
    if (closeBtn) {
      closeBtn.addEventListener('click', (e) => {
        e.preventDefault();
        console.log('❌ 닫기 버튼 클릭');
        this.closeMenu();
      });
    }

    // 메뉴 링크 클릭 시 닫기
    const menuLinks = this.menuElement.querySelectorAll('.menu-link');
    menuLinks.forEach(link => {
      link.addEventListener('click', (e) => {
        console.log('🔗 메뉴 링크 클릭:', link.textContent);
        this.handleMenuClick(e, link);
        setTimeout(() => this.closeMenu(), 150);
      });
    });

    // ESC 키 이벤트
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && this.isMenuOpen) {
        console.log('⌨️ ESC 키로 메뉴 닫기');
        this.closeMenu();
      }
    });
  }

  /**
   * 메뉴 토글
   */
  toggleMenu() {
    console.log('🔄 메뉴 토글 시작');
    console.log('  📊 현재 상태:', this.isMenuOpen ? '열림' : '닫힘');
    console.log('  📊 메뉴 요소 존재:', !!this.menuElement);
    console.log('  📊 오버레이 존재:', !!this.overlay);

    if (!this.menuElement || !this.overlay) {
      console.error('❌ 메뉴 토글 실패 - 필수 요소 누락');
      console.error('  📊 menuElement:', this.menuElement);
      console.error('  📊 overlay:', this.overlay);
      return;
    }

    if (this.isMenuOpen) {
      console.log('🔒 메뉴 닫기 시도');
      this.closeMenu();
    } else {
      console.log('🔓 메뉴 열기 시도');
      this.openMenu();
    }
  }

  /**
   * 메뉴 열기
   */
  openMenu() {
    console.log('🔓 메뉴 열기 시작');

    if (!this.menuElement || !this.overlay) {
      console.error('❌ 메뉴 요소가 없어 열기 실패');
      return;
    }

    this.isMenuOpen = true;

    // 햄버거 아이콘 애니메이션
    if (this.menuButton) {
      this.menuButton.classList.add('menu-open');
      console.log('🔘 햄버거 아이콘 애니메이션 적용');
    }

    // 메뉴 표시
    this.menuElement.style.transform = 'translateX(0)';
    this.menuElement.style.visibility = 'visible';
    this.overlay.style.opacity = '1';
    this.overlay.style.visibility = 'visible';

    // 스크롤 방지
    document.body.classList.add('menu-open');

    console.log('✅ 메뉴 열기 완료');
  }

  /**
   * 메뉴 닫기
   */
  closeMenu() {
    console.log('🔒 메뉴 닫기 시작');

    if (!this.menuElement || !this.overlay) {
      console.error('❌ 메뉴 요소가 없어 닫기 실패');
      return;
    }

    this.isMenuOpen = false;

    // 햄버거 아이콘 애니메이션 리셋
    if (this.menuButton) {
      this.menuButton.classList.remove('menu-open');
      console.log('🔘 햄버거 아이콘 애니메이션 제거');
    }

    // 메뉴 숨김
    this.menuElement.style.transform = 'translateX(100%)';
    this.overlay.style.opacity = '0';
    this.overlay.style.visibility = 'hidden';

    // 스크롤 복구
    document.body.classList.remove('menu-open');

    console.log('✅ 메뉴 닫기 완료');
  }

  /**
   * 대안 초기화 (요소를 찾지 못했을 때)
   */
  fallbackInitialization() {
    console.log('🔄 대안 초기화 시도');

    // 0.5초 후 다시 시도
    setTimeout(() => {
      console.log('🔄 지연 초기화 시도');
      this.initOffcanvas();
    }, 500);

    // 1초 후 다시 시도
    setTimeout(() => {
      console.log('🔄 재시도 초기화');
      this.initOffcanvas();
    }, 1000);
  }

  /**
   * 나머지 기존 메서드들...
   */
  initNavigation() {
    this.updateActiveNavigation();
    this.updateNavigationBadges();
    this.enhanceNavigationAccessibility();
  }

  initScrollBehavior() {
    let ticking = false;
    window.addEventListener('scroll', () => {
      if (!ticking) {
        requestAnimationFrame(() => {
          this.handleScroll();
          ticking = false;
        });
        ticking = true;
      }
    });
  }

  initPWAFeatures() {
    this.initInstallPrompt();
    this.registerServiceWorker();
    this.checkForUpdates();
  }

  handleNavClick(e, item) {
    const href = item.getAttribute('href');
    if (href === window.location.pathname) {
      e.preventDefault();
      window.location.reload();
      return;
    }
    this.showNavigationLoading(item);
    this.animatePageTransition();
  }

  handleMenuClick(e, link) {
    if (link && link.classList.contains('logout-btn')) {
      e.preventDefault();
      this.handleLogout();
      return;
    }
    if (this.isMenuOpen) {
      this.closeMenu();
    }
    this.showMenuLoading(link);
  }

  handleScroll() {
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
    const header = document.getElementById('mobileHeader');
    const nav = document.getElementById('mobileNav');

    if (header) {
      if (scrollTop > 50) {
        header.classList.add('scrolled');
      } else {
        header.classList.remove('scrolled');
      }
    }

    if (Math.abs(scrollTop - this.lastScrollTop) > 5) {
      if (scrollTop > this.lastScrollTop && scrollTop > 100) {
        if (nav) nav.style.transform = 'translateY(100%)';
      } else {
        if (nav) nav.style.transform = 'translateY(0)';
      }
      this.lastScrollTop = scrollTop;
    }
  }

  handleResize() {
    this.updateViewportHeight();
    if (this.isMenuOpen && window.innerWidth > 768) {
      this.closeMenu();
    }
  }

  handleKeydown(e) {
    if (e.key === 'Escape' && this.isMenuOpen) {
      this.closeMenu();
    }
    if (e.key >= '1' && e.key <= '5' && !e.ctrlKey && !e.altKey) {
      const navItems = document.querySelectorAll('.nav-item');
      const index = parseInt(e.key) - 1;
      if (navItems[index]) {
        navItems[index].click();
      }
    }
  }

  updateActiveNavigation() {
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

  async updateNavigationBadges() {
    try {
      // 로그인 상태 확인
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
        this.renderNavigationBadges(data.response);
      } else {
        console.log('🔐 네비게이션 배지 API 인증 실패 - 스킵');
      }
    } catch (error) {
      console.error('네비게이션 배지 업데이트 실패:', error);
    }
  }

  renderNavigationBadges(badges) {
    Object.entries(badges).forEach(([menu, count]) => {
      const navItem = document.querySelector(`[href*="${menu}"]`);
      if (navItem && count > 0) {
        const existingBadge = navItem.querySelector('.nav-badge');
        if (existingBadge) {
          existingBadge.textContent = count > 99 ? '99+' : count;
        } else {
          const badge = document.createElement('span');
          badge.className = 'nav-badge';
          badge.textContent = count > 99 ? '99+' : count;
          navItem.appendChild(badge);
        }
      }
    });
  }

  enhanceNavigationAccessibility() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach((item, index) => {
      const text = item.querySelector('span')?.textContent;
      if (text) {
        item.setAttribute('aria-label', `${text} 페이지로 이동`);
      }
      item.setAttribute('title', `${text} (단축키: ${index + 1})`);
    });
  }

  async handleLogout() {
    try {
      const { showConfirm } = await import('./common.js');
      const confirmed = await showConfirm(
        '로그아웃 하시겠습니까?',
        '로그인 페이지로 이동합니다.'
      );
      if (!confirmed) return;

      const { memberLogout } = await import('./commonFetchV2.js');
      await memberLogout();
    } catch (error) {
      console.error('로그아웃 실패:', error);
      const { showToast } = await import('./common.js');
      showToast('로그아웃 중 오류가 발생했습니다.', 'error');
    }
  }

  showNavigationLoading(item) {
    const icon = item.querySelector('i');
    if (icon) {
      icon.className = 'fas fa-spinner fa-spin';
    }
    setTimeout(() => {
      if (icon) {
        icon.className = icon.dataset.originalClass || 'fas fa-home';
      }
    }, 2000);
  }

  showMenuLoading(link) {
    const icon = link.querySelector('i');
    if (icon) {
      icon.dataset.originalClass = icon.className;
      icon.className = 'fas fa-spinner fa-spin';
    }
  }

  animatePageTransition(direction = 'forward') {
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

  updateViewportHeight() {
    const vh = window.innerHeight * 0.01;
    document.documentElement.style.setProperty('--vh', `${vh}px`);
  }

  initInstallPrompt() {
    // PWA 설치 프롬프트 구현
  }

  registerServiceWorker() {
    // 개발 환경에서는 서비스 워커 등록하지 않음
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
      console.log('🔧 개발 환경에서는 서비스 워커 등록 스킵');
      return;
    }

    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('/sw.js')
        .then((registration) => {
          console.log('서비스 워커 등록 성공:', registration);
        })
        .catch((error) => {
          console.log('서비스 워커 등록 실패:', error);
        });
    }
  }

  checkForUpdates() {
    // 앱 업데이트 확인 구현
  }

  destroy() {
    // 이벤트 리스너 정리
    if (this.overlay && this.overlay.parentNode) {
      this.overlay.parentNode.removeChild(this.overlay);
    }
  }
}

// 전역 함수
window.logout = async () => {
  if (window.layoutManager) {
    await window.layoutManager.handleLogout();
  }
};

// 레이아웃 매니저 초기화
console.log('🌟 layout.js 스크립트 로드 완료');
let layoutManager;

// DOM 완전 로드 후 초기화
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    console.log('📱 DOM 로드 완료 - 레이아웃 매니저 초기화');
    layoutManager = new MobileLayoutManager();
    window.layoutManager = layoutManager;
  });
} else {
  console.log('📱 DOM 이미 로드됨 - 레이아웃 매니저 즉시 초기화');
  layoutManager = new MobileLayoutManager();
  window.layoutManager = layoutManager;
}

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', () => {
  if (window.layoutManager) {
    window.layoutManager.destroy();
  }
});

// 전역 디버깅 함수
window.debugMenu = () => {
  console.group('🔍 햄버거 메뉴 디버그 정보');
  console.log('레이아웃 매니저:', window.layoutManager);
  console.log('메뉴 요소:', window.layoutManager?.menuElement);
  console.log('메뉴 버튼:', window.layoutManager?.menuButton);
  console.log('오버레이:', window.layoutManager?.overlay);
  console.log('메뉴 상태:', window.layoutManager?.isMenuOpen ? '열림' : '닫힘');

  // 메뉴 버튼 클릭 테스트
  if (window.layoutManager?.menuButton) {
    console.log('🧪 메뉴 버튼 클릭 테스트 시작');
    window.layoutManager.menuButton.click();
  }

  console.groupEnd();
};

// 전역 메뉴 제어 함수
window.openMenu = () => {
  console.log('🔓 전역 메뉴 열기 호출');
  if (window.layoutManager) {
    window.layoutManager.openMenu();
  } else {
    console.error('❌ 레이아웃 매니저가 없습니다');
  }
};

window.closeMenu = () => {
  console.log('🔒 전역 메뉴 닫기 호출');
  if (window.layoutManager) {
    window.layoutManager.closeMenu();
  } else {
    console.error('❌ 레이아웃 매니저가 없습니다');
  }
};

window.toggleMenu = () => {
  console.log('🔄 전역 메뉴 토글 호출');
  if (window.layoutManager) {
    window.layoutManager.toggleMenu();
  } else {
    console.error('❌ 레이아웃 매니저가 없습니다');
  }
};