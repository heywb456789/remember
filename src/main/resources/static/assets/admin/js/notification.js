/**
 * 커스텀 알림 시스템
 */
(function() {
  'use strict';

  // 알림 컨테이너 생성
  let toastContainer;

  // 초기화 함수
  function initToastContainer() {
    if (!toastContainer) {
      toastContainer = document.createElement('div');
      toastContainer.className = 'toast-container';
      document.body.appendChild(toastContainer);
    }
    return toastContainer;
  }

  /**
   * 알림 표시 함수
   * @param {string} message - 알림 메시지
   * @param {string} type - 알림 타입 (success, danger, warning, info)
   * @param {number} duration - 표시 시간 (ms)
   */
  function showNotification(message, type = 'info', duration = 3000) {
    const container = initToastContainer();

    // 타입에 따른 타이틀과 아이콘 설정
    const typeConfig = {
      success: {
        title: '성공',
        icon: 'check-circle'
      },
      danger: {
        title: '오류',
        icon: 'exclamation-circle'
      },
      warning: {
        title: '주의',
        icon: 'exclamation-triangle'
      },
      info: {
        title: '안내',
        icon: 'info-circle'
      }
    };

    const config = typeConfig[type] || typeConfig.info;

    // 토스트 요소 생성
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
      <div class="toast-icon">
        <i class="fas fa-${config.icon}"></i>
      </div>
      <div class="toast-content">
        <div class="toast-title">${config.title}</div>
        <div class="toast-message">${message}</div>
      </div>
      <button type="button" class="toast-close">
        <i class="fas fa-times"></i>
      </button>
    `;

    // 닫기 버튼 이벤트
    const closeBtn = toast.querySelector('.toast-close');
    closeBtn.addEventListener('click', () => {
      hideToast(toast);
    });

    // 컨테이너에 추가
    container.appendChild(toast);

    // 표시 애니메이션 (비동기적으로 진행)
    setTimeout(() => {
      toast.classList.add('show');
    }, 10);

    // 자동 닫기
    if (duration > 0) {
      setTimeout(() => {
        hideToast(toast);
      }, duration);
    }

    // 토스트 반환 (필요시 참조 가능)
    return toast;
  }

  /**
   * 알림 숨김 함수
   * @param {HTMLElement} toast - 숨길 토스트 요소
   */
  function hideToast(toast) {
    if (!toast || !toast.parentNode) return;

    // 숨김 애니메이션
    toast.classList.remove('show');
    toast.classList.add('hide');

    // 애니메이션 완료 후 제거
    setTimeout(() => {
      if (toast.parentNode) {
        toast.parentNode.removeChild(toast);

        // 컨테이너에 더 이상 토스트가 없으면 컨테이너도 제거
        if (toastContainer && toastContainer.children.length === 0) {
          document.body.removeChild(toastContainer);
          toastContainer = null;
        }
      }
    }, 300);
  }

  /**
   * 알림 표시 함수
   * @param {string} message - 알림 메시지
   * @param {string} type - 알림 타입 (success, danger, warning, info)
   */
  function showAlert(message, type = 'info') {
    // 커스텀 알림 시스템 사용
    if (window.CustomNotification) {
      window.CustomNotification.show(message, type);
      return;
    }

    // 기본 alert 사용
    alert(message);
  }

  /**
   * 모든 알림 제거 함수
   */
  function clearAllNotifications() {
    if (toastContainer) {
      const toasts = toastContainer.querySelectorAll('.toast');
      toasts.forEach(toast => {
        hideToast(toast);
      });
    }
  }

  // 전역 객체에 함수 노출
  window.CustomNotification = {
    show: showNotification,
    hide: hideToast,
    clearAll: clearAllNotifications
  };
})();