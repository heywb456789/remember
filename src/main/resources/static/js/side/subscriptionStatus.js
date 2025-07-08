/**
 * 구독 랜딩 페이지 JavaScript
 * subscriptionLanding.js
 */
import { authFetch, handleFetchError } from '../commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
  // DOM 요소 참조
  const elements = {
    // 섹션들
    loadingSection: document.getElementById('loading-section'),
    nonSubscriberSection: document.getElementById('non-subscriber-section'),
    subscriberSection: document.getElementById('subscriber-section'),
    errorSection: document.getElementById('error-section'),

    // 비구독자 섹션 요소
    startSubscriptionBtn: document.getElementById('start-subscription-btn'),

    // 구독자 섹션 요소
    subscriptionStatusBadge: document.getElementById('subscription-status-badge'),
    subscriptionStartDate: document.getElementById('subscription-start-date'),
    nextPaymentDate: document.getElementById('next-payment-date'),
    monthlyAmount: document.getElementById('monthly-amount'),
    remainingMonths: document.getElementById('remaining-months'),

    // 결제 히스토리 요소
    refreshHistoryBtn: document.getElementById('refresh-history-btn'),
    paymentHistoryLoading: document.getElementById('payment-history-loading'),
    paymentHistoryList: document.getElementById('payment-history-list'),
    paymentHistoryEmpty: document.getElementById('payment-history-empty'),

    // 액션 버튼들
    manageSubscriptionBtn: document.getElementById('manage-subscription-btn'),
    contactSupportBtn: document.getElementById('contact-support-btn'),

    // 오류 섹션 요소
    errorMessage: document.getElementById('error-message'),
    retryBtn: document.getElementById('retry-btn')
  };

  // 구독 상태 데이터
  let subscriptionData = null;
  let paymentHistoryData = [];

  // 초기화
  initSubscriptionLanding();

  /**
   * 구독 랜딩 페이지 초기화
   */
  function initSubscriptionLanding() {
    // 이벤트 리스너 등록
    initEventListeners();

    // 구독 상태 조회
    loadSubscriptionStatus();
  }

  /**
   * 이벤트 리스너 등록
   */
  function initEventListeners() {
    // 구독 시작 버튼
    if (elements.startSubscriptionBtn) {
      elements.startSubscriptionBtn.addEventListener('click', function() {
        window.location.href = '/side/subscriptionStep1.html';
      });
    }

    // 결제 히스토리 새로고침 버튼
    if (elements.refreshHistoryBtn) {
      elements.refreshHistoryBtn.addEventListener('click', function() {
        loadPaymentHistory(true);
      });
    }

    // 구독 관리 버튼
    if (elements.manageSubscriptionBtn) {
      elements.manageSubscriptionBtn.addEventListener('click', function() {
        const modal = new bootstrap.Modal(document.getElementById('manageSubscriptionModal'));
        modal.show();
      });
    }

    // 고객지원 버튼
    if (elements.contactSupportBtn) {
      elements.contactSupportBtn.addEventListener('click', function() {
        // 고객지원 페이지로 이동 또는 연락처 정보 표시
        alert('고객지원 전화: 02-2128-3333');
      });
    }

    // 다시 시도 버튼
    if (elements.retryBtn) {
      elements.retryBtn.addEventListener('click', function() {
        loadSubscriptionStatus();
      });
    }
  }

  /**
   * 구독 상태 조회
   */
  async function loadSubscriptionStatus() {
    try {
      showLoading();

      // 구독 상태 조회 API 호출
      const response = await authFetch('/api/subscription/status');
      const result = await response.json();

      if (response.ok && result.status.code === 'OK_0000') {
        subscriptionData = result.response;

        if (subscriptionData && subscriptionData.status === 'PAYMENT_SUCCESS') {
          // 구독자인 경우
          showSubscriberSection();
          await loadPaymentHistory();
        } else {
          // 비구독자인 경우
          showNonSubscriberSection();
        }
      } else {
        throw new Error(result.status.message || '구독 정보를 불러올 수 없습니다.');
      }
    } catch (error) {
      showNonSubscriberSection();
    }
  }

  /**
   * 결제 히스토리 조회
   */
  async function loadPaymentHistory(showRefresh = false) {
    try {
      // 새로고침 버튼 클릭 시 로딩 표시
      if (showRefresh) {
        elements.paymentHistoryLoading.style.display = 'flex';
        elements.paymentHistoryList.style.display = 'none';
        elements.paymentHistoryEmpty.style.display = 'none';
      }

      // 결제 히스토리 조회 API 호출
      const response = await authFetch('/api/subscription/payment-history');
      const result = await response.json();

      if (response.ok && result.status.code === 'OK_0000') {
        paymentHistoryData = result.response || [];
        renderPaymentHistory();
      } else {
        throw new Error(result.status.message || '결제 내역을 불러올 수 없습니다.');
      }
    } catch (error) {
      console.error('결제 히스토리 조회 오류:', error);

      // 히스토리 조회 실패 시 빈 상태 표시
      elements.paymentHistoryLoading.style.display = 'none';
      elements.paymentHistoryList.style.display = 'none';
      elements.paymentHistoryEmpty.style.display = 'block';

      // 에러 메시지 표시
      showToast('결제 내역을 불러올 수 없습니다.', 'error');
    } finally {
      elements.paymentHistoryLoading.style.display = 'none';
    }
  }

  /**
   * 로딩 상태 표시
   */
  function showLoading() {
    hideAllSections();
    elements.loadingSection.style.display = 'flex';
  }

  /**
   * 비구독자 섹션 표시
   */
  function showNonSubscriberSection() {
    hideAllSections();
    elements.nonSubscriberSection.style.display = 'block';
  }

  /**
   * 구독자 섹션 표시
   */
  function showSubscriberSection() {
    hideAllSections();
    elements.subscriberSection.style.display = 'block';

    // 구독 정보 업데이트
    updateSubscriptionInfo();
  }

  /**
   * 오류 섹션 표시
   */
  function showErrorSection(message) {
    hideAllSections();
    elements.errorSection.style.display = 'block';
    elements.errorMessage.textContent = message;
  }

  /**
   * 모든 섹션 숨기기
   */
  function hideAllSections() {
    elements.loadingSection.style.display = 'none';
    elements.nonSubscriberSection.style.display = 'none';
    elements.subscriberSection.style.display = 'none';
    elements.errorSection.style.display = 'none';
  }

  /**
   * 구독 정보 업데이트
   */
  function updateSubscriptionInfo() {
    if (!subscriptionData) return;

    // 구독 상태 배지 업데이트
    const status = subscriptionData.status || 'ACTIVE';
    const statusText = getStatusText(status);
    const statusClass = getStatusClass(status);

    elements.subscriptionStatusBadge.textContent = statusText;
    elements.subscriptionStatusBadge.className = `status-badge ${statusClass}`;

    // 구독 시작일
    if (subscriptionData.payedDate) {
      elements.subscriptionStartDate.textContent = formatDate(subscriptionData.payedDate);
    }

    // 다음 결제일
    if (subscriptionData.nextPayDate) {
      elements.nextPaymentDate.textContent = formatDate(subscriptionData.nextPayDate);
    }

    // 월 결제금액
    if (subscriptionData.productPrice) {
      elements.monthlyAmount.textContent = formatCurrency(subscriptionData.productPrice);
    }

    // 잔여 개월
    if (subscriptionData.remainPayMonth !== undefined) {
      elements.remainingMonths.textContent = `${subscriptionData.remainPayMonth}개월`;
    }
  }

  /**
   * 결제 히스토리 렌더링
   */
  function renderPaymentHistory() {
    if (!paymentHistoryData || paymentHistoryData.length === 0) {
      elements.paymentHistoryList.style.display = 'none';
      elements.paymentHistoryEmpty.style.display = 'block';
      return;
    }

    elements.paymentHistoryEmpty.style.display = 'none';
    elements.paymentHistoryList.style.display = 'block';

    const historyHTML = paymentHistoryData.map(payment => {
      const statusClass = getPaymentStatusClass(payment.status);
      const statusText = getPaymentStatusText(payment.status);

      return `
        <div class="history-item">
          <div class="history-info">
            <div class="history-date">${formatDate(payment.createdAt)}</div>
            <div class="history-method">${payment.paymentMethod || '카드결제'}</div>
          </div>
          <div class="history-amount">
            <span class="amount">${formatCurrency(payment.productPrice)}</span>
          </div>
          <div class="history-status">
            <span class="payment-status ${statusClass}">${statusText}</span>
          </div>
        </div>
      `;
    }).join('');

    elements.paymentHistoryList.innerHTML = historyHTML;
  }

  /**
   * 구독 상태 텍스트 반환
   */
  function getStatusText(status) {
    const statusMap = {
      'PAYMENT_SUCCESS': '활성',
      'PAYMENT_FAILED': '활성(결제 실패)',
      'CANCELLED': '해지',
      'REFUNDED': '만료(환불 완료)'
    };
    return statusMap[status] || '알 수 없음';
  }

  /**
   * 구독 상태 CSS 클래스 반환
   */
  function getStatusClass(status) {
    const classMap = {
      'PAYMENT_SUCCESS': 'active',
      'PAYMENT_FAILED': 'paused',
      'CANCELLED': 'cancelled',
      'REFUNDED': 'expired'
    };
    return classMap[status] || 'unknown';
  }

  /**
   * 결제 상태 텍스트 반환
   */
  function getPaymentStatusText(status) {
    const statusMap = {
      'PAYMENT_SUCCESS': '결제완료',
      'PAYMENT_FAILED': '결제실패',
      'CANCELLED': '결제취소',
      'REFUNDED': '환불완료'
    };
    return statusMap[status] || '알 수 없음';
  }

  /**
   * 결제 상태 CSS 클래스 반환
   */
  function getPaymentStatusClass(status) {
    const classMap = {
      'PAYMENT_SUCCESS': 'success',
      'PAYMENT_FAILED': 'failed',
      'CANCELLED': 'cancelled',
      'REFUNDED': 'refunded'
    };
    return classMap[status] || 'unknown';
  }

  /**
   * 날짜 포맷팅
   */
  function formatDate(dateString) {
    if (!dateString) return '-';

    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      });
    } catch (error) {
      return dateString;
    }
  }

  /**
   * 통화 포맷팅
   */
  function formatCurrency(amount) {
    if (!amount) return '0원';

    try {
      return new Intl.NumberFormat('ko-KR').format(amount) + '원';
    } catch (error) {
      return amount + '원';
    }
  }

  /**
   * 토스트 메시지 표시
   */
  function showToast(message, type = 'info') {
    // 간단한 토스트 알림 (실제 구현에서는 더 세련된 토스트 라이브러리 사용 권장)
    const alertClass = type === 'error' ? 'alert-danger' : 'alert-info';
    const toastHTML = `
      <div class="toast-container position-fixed top-0 end-0 p-3">
        <div class="alert ${alertClass} alert-dismissible fade show" role="alert">
          ${message}
          <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
      </div>
    `;

    // 기존 토스트 제거
    const existingToast = document.querySelector('.toast-container');
    if (existingToast) {
      existingToast.remove();
    }

    // 새 토스트 추가
    document.body.insertAdjacentHTML('beforeend', toastHTML);

    // 3초 후 자동 제거
    setTimeout(() => {
      const toastElement = document.querySelector('.toast-container');
      if (toastElement) {
        toastElement.remove();
      }
    }, 3000);
  }

  // 전역 함수로 영수증 보기 기능 제공
  window.viewReceipt = function(paymentId) {
    // 영수증 보기 기능 구현
    alert(`결제 ID: ${paymentId}의 영수증을 표시합니다.`);
    // 실제 구현에서는 영수증 모달이나 새 창으로 표시
  };
});