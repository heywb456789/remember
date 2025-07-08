/**
 * Spark Admin Panel - Common JS
 */

// 즉시 실행 함수를 사용하여 전역 네임스페이스를 오염시키지 않음
(() => {
  'use strict';
  
  // DOM 요소들
  const sidebar = document.getElementById('sidebar');
  const content = document.getElementById('content');
  const sidebarCollapse = document.getElementById('sidebarCollapse');

  /**
   * 문서 로드 완료 시 실행할 함수
   */
  const domReady = () => {
    initSidebar();
    initTooltips();
    handleResponsive();
  };

  /**
   * 사이드바 토글 및 이벤트 리스너 초기화
   */
  const initSidebar = () => {
    // 사이드바 토글 버튼 클릭 이벤트
    if (sidebarCollapse) {
      sidebarCollapse.addEventListener('click', toggleSidebar);
    }

    // 서브메뉴 토글
    document.querySelectorAll('.nav-link[data-bs-toggle="collapse"]').forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        const target = document.querySelector(link.getAttribute('data-bs-target'));
        if (target) {
          // 현재 메뉴가 활성화되어 있는지 확인
          const isActive = target.classList.contains('show');

          // 모든 서브메뉴 닫기
          if (!isActive) {
            document.querySelectorAll('.submenu.show').forEach(submenu => {
              if (submenu !== target) {
                submenu.classList.remove('show');
                const parentLink = document.querySelector(`[data-bs-target="#${submenu.id}"]`);
                if (parentLink) {
                  parentLink.setAttribute('aria-expanded', 'false');
                }
              }
            });
          }

          // 타겟 서브메뉴 토글
          target.classList.toggle('show');
          link.setAttribute('aria-expanded', target.classList.contains('show'));
        }
      });
    });

    // 서브메뉴 항목 클릭 시 링크 이동
    document.querySelectorAll('.submenu a').forEach(submenuLink => {
      submenuLink.addEventListener('click', (e) => {
        // 이미 active 클래스가 있는 링크라면 이벤트 중지 (중복 클릭 방지)
        if (submenuLink.classList.contains('active')) {
          e.preventDefault();
          return;
        }

        // 모바일에서 링크 클릭 시 사이드바 닫기
        if (window.innerWidth <= 768) {
          closeMobileSidebar();
        }
      });
    });

    // 모바일에서 사이드바 외부 클릭 시 닫기 - document에 직접 이벤트 등록하지 않음
    // (오버레이에 이벤트를 등록하는 방식으로 변경)
  };

  /**
   * 사이드바 토글 함수 - 별도 함수로 분리하여 재사용성 높임
   */
  const toggleSidebar = () => {
    const isMobile = window.innerWidth <= 768;

    if (isMobile) {
      // 모바일에서는 사이드바 상태 확인하여 토글
      if (sidebar.classList.contains('mobile-active')) {
        // 이미 열려 있으면 닫기
        closeMobileSidebar();
        console.log('토글: 사이드바 닫기 실행');
      } else {
        // 닫혀 있으면 열기
        openMobileSidebar();
        console.log('토글: 사이드바 열기 실행');
      }
    } else {
      // 데스크톱에서는 기존 동작 유지
      sidebar.classList.toggle('collapsed');
      content.classList.toggle('full-width');
    }
  };

  /**
   * 모바일 사이드바 열기
   */
  const openMobileSidebar = () => {
    // 모바일에서 사이드바 열 때 collapsed 클래스 제거 (충돌 방지)
    sidebar.classList.remove('collapsed');
    sidebar.classList.add('mobile-active');

    // 이미 존재하는 오버레이가 있다면 제거
    const existingOverlay = document.getElementById('sidebarOverlay');
    if (existingOverlay) {
      existingOverlay.parentNode.removeChild(existingOverlay);
    }

    // 새 배경 오버레이 추가
    const overlay = document.createElement('div');
    overlay.className = 'sidebar-overlay';
    overlay.id = 'sidebarOverlay'; // ID 추가하여 쉽게 찾을 수 있도록

    // 오버레이 클릭 시 사이드바 닫기
    overlay.addEventListener('click', function(e) {
      e.preventDefault();
      e.stopPropagation();
      closeMobileSidebar();
    });

    document.body.appendChild(overlay);

    // 디버깅을 위한 콘솔 로그
    console.log('사이드바 열기 실행됨', {
      'mobile-active': sidebar.classList.contains('mobile-active'),
      'collapsed': sidebar.classList.contains('collapsed'),
      'overlay': !!document.getElementById('sidebarOverlay')
    });
  };

  /**
   * 모바일 사이드바 닫기
   */
  const closeMobileSidebar = () => {
    // 먼저 collapsed 클래스 추가 (모바일에서는 숨김 효과)
    sidebar.classList.add('collapsed');

    // mobile-active 클래스 제거
    sidebar.classList.remove('mobile-active');

    // 오버레이 제거
    const overlay = document.getElementById('sidebarOverlay');
    if (overlay) {
      // 애니메이션을 위한 클래스 추가
      overlay.classList.add('fade-out');

      // 짧은 지연 후 제거 (애니메이션 시간 동안)
      setTimeout(() => {
        if (overlay && overlay.parentNode) {
          overlay.parentNode.removeChild(overlay);
        }
      }, 300);
    }

    // 디버깅을 위한 콘솔 로그
    console.log('사이드바 닫기 실행됨', {
      'mobile-active': sidebar.classList.contains('mobile-active'),
      'collapsed': sidebar.classList.contains('collapsed'),
      'overlay': !!overlay
    });
  };

  /**
   * 툴팁 초기화
   */
  const initTooltips = () => {
    // Bootstrap 툴팁 초기화
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
      return new bootstrap.Tooltip(tooltipTriggerEl);
    });
  };

  /**
   * 반응형 처리
   */
  const handleResponsive = () => {
    const mobileBreakpoint = 768;

    // 초기 화면 크기에 따라 사이드바 상태 설정
    const isMobile = window.innerWidth <= mobileBreakpoint;

    // 모바일 환경에서 사이드바 토글 동작 설정
    if (isMobile) {
      // 초기에 mobile-active 클래스가 있으면 제거
      sidebar.classList.remove('mobile-active');

      // collapsed 클래스 추가 (transform: translateX(-100%)를 적용하기 위해)
      sidebar.classList.add('collapsed');

      // 혹시 남아있는 오버레이 제거
      const overlay = document.getElementById('sidebarOverlay');
      if (overlay && overlay.parentNode) {
        overlay.parentNode.removeChild(overlay);
      }
    }

    // 화면 크기 변경 시 사이드바 상태 조정
    window.addEventListener('resize', () => {
      const isCurrentlyMobile = window.innerWidth <= mobileBreakpoint;

      if (isCurrentlyMobile) {
        // 모바일 뷰로 전환 시 사이드바가 열려있으면 닫기
        if (sidebar.classList.contains('mobile-active')) {
          closeMobileSidebar();
        }
      } else {
        // 데스크톱 사이즈로 변경 시 모바일 관련 클래스 정리
        sidebar.classList.remove('mobile-active');

        // 데스크톱에서는 collapsed 클래스로 토글 상태 관리

        // 오버레이 제거
        const overlay = document.getElementById('sidebarOverlay');
        if (overlay && overlay.parentNode) {
          overlay.parentNode.removeChild(overlay);
        }
      }
    });
  };

  /**
   * 새로고침 버튼 처리
   */
  const initRefreshButtons = () => {
    document.querySelectorAll('.refresh-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();

        // 새로고침 효과
        const card = btn.closest('.card');
        if (card) {
          const spinner = document.createElement('div');
          spinner.className = 'refresh-spinner';
          card.classList.add('refreshing');
          card.appendChild(spinner);

          // 가상의 로딩 시간 후 스피너 제거
          setTimeout(() => {
            card.classList.remove('refreshing');
            spinner.remove();
          }, 1000);
        }
      });
    });
  };

  /**
   * 카드 토글 버튼 처리
   */
  const initCardToggles = () => {
    document.querySelectorAll('.toggle-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();

        // 토글 메뉴 생성
        const menu = document.createElement('div');
        menu.className = 'card-toggle-menu';
        menu.innerHTML = `
          <ul>
            <li><a href="#"><i class="fas fa-expand"></i> Expand</a></li>
            <li><a href="#"><i class="fas fa-compress"></i> Compress</a></li>
            <li><a href="#"><i class="fas fa-times"></i> Remove</a></li>
          </ul>
        `;

        // 이미 메뉴가 있으면 제거, 없으면 추가
        const existingMenu = document.querySelector('.card-toggle-menu');
        if (existingMenu) {
          existingMenu.remove();
        } else {
          document.body.appendChild(menu);

          // 메뉴 위치 설정
          const rect = btn.getBoundingClientRect();
          menu.style.top = rect.bottom + 'px';
          menu.style.left = (rect.left - menu.offsetWidth + rect.width) + 'px';

          // 외부 클릭 시 메뉴 닫기
          document.addEventListener('click', function closeMenu(e) {
            if (!menu.contains(e.target) && e.target !== btn) {
              menu.remove();
              document.removeEventListener('click', closeMenu);
            }
          });
        }
      });
    });
  };

  /**
   * Ajax 요청 함수
   * @param {string} url - 요청 URL
   * @param {Object} options - 옵션 (method, data 등)
   * @returns {Promise} - Fetch Promise
   */
  const ajaxRequest = (url, options = {}) => {
    const defaultOptions = {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest'
      }
    };

    const fetchOptions = { ...defaultOptions, ...options };

    if (fetchOptions.data && fetchOptions.method !== 'GET') {
      fetchOptions.body = JSON.stringify(fetchOptions.data);
      delete fetchOptions.data;
    }

    return fetch(url, fetchOptions)
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! Status: ${response.status}`);
        }
        return response.json();
      });
  };

  /**
   * 알림 표시 함수
   * @param {string} message - 알림 메시지
   * @param {string} type - 알림 타입 (success, danger, warning, info)
   * @param {number} duration - 표시 시간 (ms)
   */
  const showNotification = (message, type = 'info', duration = 3000) => {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
      <div class="notification-icon">
        <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'danger' ? 'exclamation-circle' : type === 'warning' ? 'exclamation-triangle' : 'info-circle'}"></i>
      </div>
      <div class="notification-content">${message}</div>
      <button class="notification-close"><i class="fas fa-times"></i></button>
    `;

    // 알림 컨테이너 확인 또는 생성
    let container = document.querySelector('.notification-container');
    if (!container) {
      container = document.createElement('div');
      container.className = 'notification-container';
      document.body.appendChild(container);
    }

    container.appendChild(notification);

    // 닫기 버튼
    const closeBtn = notification.querySelector('.notification-close');
    if (closeBtn) {
      closeBtn.addEventListener('click', () => {
        notification.classList.add('notification-fade-out');
        setTimeout(() => {
          container.removeChild(notification);
          if (container.children.length === 0) {
            document.body.removeChild(container);
          }
        }, 300);
      });
    }

    // 자동 닫기
    if (duration > 0) {
      setTimeout(() => {
        if (notification.parentNode) {
          notification.classList.add('notification-fade-out');
          setTimeout(() => {
            if (notification.parentNode) {
              container.removeChild(notification);
              if (container.children.length === 0 && container.parentNode) {
                document.body.removeChild(container);
              }
            }
          }, 300);
        }
      }, duration);
    }
  };

  // 스파크 전역 객체 생성
  window.Spark = {
    initRefreshButtons,
    initCardToggles,
    ajaxRequest,
    showNotification,
    toggleSidebar,  // 전역으로 사용할 수 있도록 추가
    closeMobileSidebar // 전역으로 사용할 수 있도록 추가
  };
  
  // 문서 로드 완료 시 초기화
  document.addEventListener('DOMContentLoaded', () => {
    domReady();
    initRefreshButtons();
    initCardToggles();
  });
})();