// common.js - 토마토리멤버 모바일 공통 유틸리티
let currentLoading = null;

// export function showLoading(message = '로딩 중...') {
//   // 기존 코드...
//   currentLoading = loading; // 현재 로딩 인스턴스 저장
//   return {
//     hide: () => {
//       if (loading.parentElement) {
//         loading.classList.remove('show');
//         setTimeout(() => {
//           loading.remove();
//           currentLoading = null;
//         }, 300);
//       }
//     }
//   };
// }

// 새로 추가할 함수
export function hideLoading() {
  if (currentLoading) {
    currentLoading.hide();
    currentLoading = null;
  }
}

/**
 * 토스트 메시지 표시
 * @param {string} message - 메시지 내용
 * @param {string} type - 타입 (success, error, warning, info)
 * @param {number} duration - 표시 시간 (ms)
 */
export function showToast(message, type = 'info', duration = 3000) {
  // 기존 토스트 제거
  const existingToast = document.querySelector('.toast-message');
  if (existingToast) {
    existingToast.remove();
  }

  // 토스트 생성
  const toast = document.createElement('div');
  toast.className = `toast-message toast-${type}`;

  // 아이콘 설정
  const icons = {
    success: 'fas fa-check-circle',
    error: 'fas fa-exclamation-circle',
    warning: 'fas fa-exclamation-triangle',
    info: 'fas fa-info-circle'
  };

  toast.innerHTML = `
    <div class="toast-content">
      <i class="${icons[type]}"></i>
      <span class="toast-text">${message}</span>
      <button class="toast-close" onclick="this.parentElement.parentElement.remove()">
        <i class="fas fa-times"></i>
      </button>
    </div>
  `;

  // 페이지에 추가
  document.body.appendChild(toast);

  // 애니메이션으로 표시
  setTimeout(() => {
    toast.classList.add('show');
  }, 100);

  // 자동 제거
  setTimeout(() => {
    if (toast.parentElement) {
      toast.classList.remove('show');
      setTimeout(() => {
        toast.remove();
      }, 300);
    }
  }, duration);

  return toast;
}

/**
 * 확인 다이얼로그 표시
 * @param {string} title - 제목
 * @param {string} message - 메시지
 * @param {string} confirmText - 확인 버튼 텍스트
 * @param {string} cancelText - 취소 버튼 텍스트
 * @returns {Promise<boolean>} - 사용자 선택 결과
 */
export function showConfirm(title, message, confirmText = '확인', cancelText = '취소') {
  return new Promise((resolve) => {
    // 기존 모달 제거
    const existingModal = document.querySelector('.confirm-modal');
    if (existingModal) {
      existingModal.remove();
    }

    // 모달 생성
    const modal = document.createElement('div');
    modal.className = 'confirm-modal';
    modal.innerHTML = `
      <div class="confirm-backdrop"></div>
      <div class="confirm-dialog">
        <div class="confirm-header">
          <h4 class="confirm-title">${title}</h4>
        </div>
        <div class="confirm-body">
          <p class="confirm-message">${message}</p>
        </div>
        <div class="confirm-footer">
          <button class="btn btn-outline-secondary confirm-cancel">${cancelText}</button>
          <button class="btn btn-primary confirm-ok">${confirmText}</button>
        </div>
      </div>
    `;

    // 이벤트 바인딩
    const cancelBtn = modal.querySelector('.confirm-cancel');
    const okBtn = modal.querySelector('.confirm-ok');
    const backdrop = modal.querySelector('.confirm-backdrop');

    const handleClose = (result) => {
      modal.classList.remove('show');
      setTimeout(() => {
        modal.remove();
        resolve(result);
      }, 300);
    };

    cancelBtn.addEventListener('click', () => handleClose(false));
    okBtn.addEventListener('click', () => handleClose(true));
    backdrop.addEventListener('click', () => handleClose(false));

    // ESC 키 처리
    const handleKeydown = (e) => {
      if (e.key === 'Escape') {
        handleClose(false);
        document.removeEventListener('keydown', handleKeydown);
      }
    };
    document.addEventListener('keydown', handleKeydown);

    // 페이지에 추가
    document.body.appendChild(modal);

    // 애니메이션으로 표시
    setTimeout(() => {
      modal.classList.add('show');
    }, 100);

    // 확인 버튼에 포커스
    setTimeout(() => {
      okBtn.focus();
    }, 400);
  });
}

/**
 * 로딩 스피너 표시
 * @param {string} message - 로딩 메시지
 * @returns {Object} - 로딩 인스턴스 (hide 메서드 포함)
 */
export function showLoading(message = '로딩 중...') {
  // 기존 로딩 제거
  const existingLoading = document.querySelector('.loading-overlay');
  if (existingLoading) {
    existingLoading.remove();
  }

  // 로딩 오버레이 생성
  const loading = document.createElement('div');
  loading.className = 'loading-overlay';
  loading.innerHTML = `
    <div class="loading-content">
      <div class="loading-spinner">
        <div class="spinner-ring"></div>
      </div>
      <p class="loading-message">${message}</p>
    </div>
  `;

  // 페이지에 추가
  document.body.appendChild(loading);

  // 애니메이션으로 표시
  setTimeout(() => {
    loading.classList.add('show');
  }, 100);

  // ✅ 이 부분이 핵심 - currentLoading 설정
  currentLoading = {
    hide: () => {
      if (loading.parentElement) {
        loading.classList.remove('show');
        setTimeout(() => {
          loading.remove();
          currentLoading = null;  // ✅ 정리
        }, 300);
      }
    },
    updateMessage: (newMessage) => {
      const messageEl = loading.querySelector('.loading-message');
      if (messageEl) {
        messageEl.textContent = newMessage;
      }
    }
  };

  return currentLoading;  // ✅ currentLoading 반환
}

/**
 * 모달 표시
 * @param {string} id - 모달 ID
 * @param {Object} options - 모달 옵션
 */
export function showModal(id, options = {}) {
  const modal = document.getElementById(id);
  if (modal) {
    const bsModal = new bootstrap.Modal(modal, options);
    bsModal.show();
    return bsModal;
  }
}

/**
 * 모달 숨김
 * @param {string} id - 모달 ID
 */
export function hideModal(id) {
  const modal = document.getElementById(id);
  if (modal) {
    const bsModal = bootstrap.Modal.getInstance(modal);
    if (bsModal) {
      bsModal.hide();
    }
  }
}

/**
 * 폼 유효성 검사
 * @param {HTMLFormElement} form - 폼 요소
 * @returns {boolean} - 유효성 검사 결과
 */
export function validateForm(form) {
  if (!form) return false;

  let isValid = true;
  const formControls = form.querySelectorAll('.form-control, .form-select, .form-check-input');

  formControls.forEach(control => {
    const errorElement = form.querySelector(`[data-error="${control.name}"]`);

    // 기존 에러 제거
    control.classList.remove('is-invalid');
    if (errorElement) {
      errorElement.style.display = 'none';
    }

    // 필수 필드 검사
    if (control.hasAttribute('required') && !control.value.trim()) {
      showFieldError(control, '이 필드는 필수입니다.');
      isValid = false;
      return;
    }

    // 이메일 검사
    if (control.type === 'email' && control.value) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(control.value)) {
        showFieldError(control, '올바른 이메일 주소를 입력하세요.');
        isValid = false;
        return;
      }
    }

    // 전화번호 검사
    if (control.type === 'tel' && control.value) {
      const phoneRegex = /^010[0-9]{8}$/;
      if (!phoneRegex.test(control.value.replace(/[-\s]/g, ''))) {
        showFieldError(control, '올바른 휴대폰 번호를 입력하세요.');
        isValid = false;
        return;
      }
    }

    // 비밀번호 검사
    if (control.type === 'password' && control.value) {
      if (control.value.length < 8) {
        showFieldError(control, '비밀번호는 8자 이상이어야 합니다.');
        isValid = false;
        return;
      }
    }

    // 비밀번호 확인 검사
    if (control.dataset.passwordConfirm) {
      const passwordField = form.querySelector(`[name="${control.dataset.passwordConfirm}"]`);
      if (passwordField && control.value !== passwordField.value) {
        showFieldError(control, '비밀번호가 일치하지 않습니다.');
        isValid = false;
        return;
      }
    }
  });

  return isValid;
}

/**
 * 필드 에러 표시
 * @param {HTMLElement} field - 필드 요소
 * @param {string} message - 에러 메시지
 */
function showFieldError(field, message) {
  field.classList.add('is-invalid');

  let errorElement = field.parentElement.querySelector('.invalid-feedback');
  if (!errorElement) {
    errorElement = document.createElement('div');
    errorElement.className = 'invalid-feedback';
    field.parentElement.appendChild(errorElement);
  }

  errorElement.textContent = message;
  errorElement.style.display = 'block';
}

/**
 * 폼 리셋
 * @param {HTMLFormElement} form - 폼 요소
 */
export function resetForm(form) {
  if (!form) return;

  form.reset();

  // 에러 상태 제거
  const invalidFields = form.querySelectorAll('.is-invalid');
  invalidFields.forEach(field => {
    field.classList.remove('is-invalid');
  });

  const errorMessages = form.querySelectorAll('.invalid-feedback');
  errorMessages.forEach(error => {
    error.style.display = 'none';
  });
}

/**
 * 로컬 스토리지 안전 설정
 * @param {string} key - 키
 * @param {any} value - 값
 */
export function setLocalStorage(key, value) {
  try {
    const serializedValue = JSON.stringify(value);
    localStorage.setItem(key, serializedValue);
  } catch (error) {
    console.error('로컬 스토리지 설정 실패:', error);
  }
}

/**
 * 로컬 스토리지 안전 가져오기
 * @param {string} key - 키
 * @param {any} defaultValue - 기본값
 * @returns {any} - 저장된 값 또는 기본값
 */
export function getLocalStorage(key, defaultValue = null) {
  try {
    const item = localStorage.getItem(key);
    return item ? JSON.parse(item) : defaultValue;
  } catch (error) {
    console.error('로컬 스토리지 가져오기 실패:', error);
    return defaultValue;
  }
}

/**
 * 로컬 스토리지 제거
 * @param {string} key - 키
 */
export function removeLocalStorage(key) {
  try {
    localStorage.removeItem(key);
  } catch (error) {
    console.error('로컬 스토리지 제거 실패:', error);
  }
}

/**
 * 디바이스 타입 감지
 * @returns {string} - 디바이스 타입 (mobile, tablet, desktop)
 */
export function getDeviceType() {
  const width = window.innerWidth;

  if (width < 768) return 'mobile';
  if (width < 1024) return 'tablet';
  return 'desktop';
}

/**
 * 터치 디바이스 감지
 * @returns {boolean} - 터치 디바이스 여부
 */
export function isTouchDevice() {
  return 'ontouchstart' in window || navigator.maxTouchPoints > 0;
}

/**
 * iOS 감지
 * @returns {boolean} - iOS 여부
 */
export function isIOS() {
  return /iPad|iPhone|iPod/.test(navigator.userAgent);
}

/**
 * Android 감지
 * @returns {boolean} - Android 여부
 */
export function isAndroid() {
  return /Android/.test(navigator.userAgent);
}

/**
 * PWA 모드 감지
 * @returns {boolean} - PWA 모드 여부
 */
export function isPWA() {
  return window.matchMedia('(display-mode: standalone)').matches ||
         window.navigator.standalone === true;
}

/**
 * 클립보드에 텍스트 복사
 * @param {string} text - 복사할 텍스트
 * @returns {Promise<boolean>} - 복사 성공 여부
 */
export async function copyToClipboard(text) {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    } else {
      // 폴백 방법
      const textArea = document.createElement('textarea');
      textArea.value = text;
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      textArea.style.top = '-999999px';
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();

      const result = document.execCommand('copy');
      textArea.remove();
      return result;
    }
  } catch (error) {
    console.error('클립보드 복사 실패:', error);
    return false;
  }
}

/**
 * 파일 다운로드
 * @param {string} url - 다운로드 URL
 * @param {string} filename - 파일명
 */
export function downloadFile(url, filename) {
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.style.display = 'none';

  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

/**
 * 이미지 지연 로딩
 * @param {string} selector - 이미지 선택자
 */
export function initLazyLoading(selector = 'img[data-src]') {
  if ('IntersectionObserver' in window) {
    const imageObserver = new IntersectionObserver((entries, observer) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const img = entry.target;
          img.src = img.dataset.src;
          img.classList.remove('lazy');
          observer.unobserve(img);
        }
      });
    });

    document.querySelectorAll(selector).forEach(img => {
      imageObserver.observe(img);
    });
  } else {
    // 폴백: 즉시 로딩
    document.querySelectorAll(selector).forEach(img => {
      img.src = img.dataset.src;
      img.classList.remove('lazy');
    });
  }
}

/**
 * 디바운스 함수
 * @param {Function} func - 실행할 함수
 * @param {number} wait - 대기 시간 (ms)
 * @returns {Function} - 디바운스된 함수
 */
export function debounce(func, wait) {
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
 * 스로틀 함수
 * @param {Function} func - 실행할 함수
 * @param {number} limit - 제한 시간 (ms)
 * @returns {Function} - 스로틀된 함수
 */
export function throttle(func, limit) {
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
 * 날짜 포맷팅
 * @param {Date|string} date - 날짜
 * @param {string} format - 포맷 (YYYY-MM-DD, YYYY.MM.DD 등)
 * @returns {string} - 포맷된 날짜
 */
export function formatDate(date, format = 'YYYY-MM-DD') {
  const d = new Date(date);
  if (isNaN(d.getTime())) return '';

  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');

  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes);
}

/**
 * 상대 시간 표시
 * @param {Date|string} date - 날짜
 * @returns {string} - 상대 시간
 */
export function timeAgo(date) {
  const now = new Date();
  const diff = now - new Date(date);
  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days}일 전`;
  if (hours > 0) return `${hours}시간 전`;
  if (minutes > 0) return `${minutes}분 전`;
  return '방금 전';
}

/**
 * 숫자 포맷팅 (천 단위 콤마)
 * @param {number} num - 숫자
 * @returns {string} - 포맷된 숫자
 */
export function formatNumber(num) {
  return new Intl.NumberFormat('ko-KR').format(num);
}

/**
 * 바이트 크기 포맷팅
 * @param {number} bytes - 바이트 크기
 * @param {number} decimals - 소수점 자릿수
 * @returns {string} - 포맷된 크기
 */
export function formatFileSize(bytes, decimals = 2) {
  if (bytes === 0) return '0 Bytes';

  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];

  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

// CSS 스타일 추가 (토스트, 모달 등)
const commonStyles = `
  .toast-message {
    position: fixed;
    top: 90px;
    right: 20px;
    max-width: 350px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
    border-left: 4px solid #667eea;
    z-index: 9999;
    transform: translateX(100%);
    transition: transform 0.3s ease;
  }

  .toast-message.show {
    transform: translateX(0);
  }

  .toast-success { border-left-color: #48bb78; }
  .toast-error { border-left-color: #e53e3e; }
  .toast-warning { border-left-color: #ed8936; }
  .toast-info { border-left-color: #4299e1; }

  .toast-content {
    display: flex;
    align-items: center;
    padding: 16px;
    gap: 12px;
  }

  .toast-content i {
    font-size: 18px;
    flex-shrink: 0;
  }

  .toast-success i { color: #48bb78; }
  .toast-error i { color: #e53e3e; }
  .toast-warning i { color: #ed8936; }
  .toast-info i { color: #4299e1; }

  .toast-text {
    flex: 1;
    font-size: 14px;
    line-height: 1.4;
    color: #2d3748;
  }

  .toast-close {
    background: none;
    border: none;
    color: #a0aec0;
    cursor: pointer;
    padding: 4px;
    border-radius: 4px;
    transition: all 0.2s ease;
  }

  .toast-close:hover {
    background: #f7fafc;
    color: #4a5568;
  }

  .confirm-modal {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 9999;
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  .confirm-modal.show {
    opacity: 1;
  }

  .confirm-backdrop {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    backdrop-filter: blur(4px);
  }

  .confirm-dialog {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background: white;
    border-radius: 16px;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
    max-width: 400px;
    width: calc(100% - 40px);
    overflow: hidden;
  }

  .confirm-header {
    padding: 20px 20px 0;
  }

  .confirm-title {
    font-size: 18px;
    font-weight: 600;
    color: #2d3748;
    margin: 0;
  }

  .confirm-body {
    padding: 16px 20px;
  }

  .confirm-message {
    font-size: 14px;
    line-height: 1.5;
    color: #4a5568;
    margin: 0;
  }

  .confirm-footer {
    display: flex;
    gap: 12px;
    padding: 0 20px 20px;
    justify-content: flex-end;
  }

  .loading-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(4px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  .loading-overlay.show {
    opacity: 1;
  }

  .loading-content {
    text-align: center;
  }

  .loading-spinner {
    width: 40px;
    height: 40px;
    margin: 0 auto 16px;
  }

  .spinner-ring {
    width: 100%;
    height: 100%;
    border: 4px solid #e2e8f0;
    border-top: 4px solid #667eea;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  .loading-message {
    font-size: 14px;
    color: #4a5568;
    margin: 0;
  }

  .is-invalid {
    border-color: #e53e3e !important;
    box-shadow: 0 0 0 0.2rem rgba(229, 62, 62, 0.25) !important;
  }

  .invalid-feedback {
    display: none;
    font-size: 12px;
    color: #e53e3e;
    margin-top: 4px;
  }

  .lazy {
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  .lazy.loaded {
    opacity: 1;
  }

  @media (max-width: 480px) {
    .toast-message {
      right: 16px;
      left: 16px;
      max-width: none;
    }

    .confirm-dialog {
      width: calc(100% - 32px);
    }
  }
`;

// 동적 스타일 추가
if (!document.getElementById('common-dynamic-styles')) {
  const styleSheet = document.createElement('style');
  styleSheet.id = 'common-dynamic-styles';
  styleSheet.textContent = commonStyles;
  document.head.appendChild(styleSheet);
}