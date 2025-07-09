// main.js - 토마토리멤버 모바일 메인 페이지 (수정 버전)

import { authFetch, memberLogout, checkLoginStatus } from './commonFetch.js';
import { showModal, hideModal, showToast, showConfirm } from './common.js';

/**
 * 메인 페이지 관리 클래스
 */
class MainPageManager {
  constructor() {
    this.isLoading = false;
    this.memorialCards = [];
    this.isLoggedIn = false;
    this.init();
  }

  /**
   * 초기화
   */
  init() {
    console.log('🚀 MainPageManager 초기화 시작');

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
    console.log('🔧 메인 페이지 컴포넌트 초기화');

    this.bindEvents();
    this.checkLoginStatus();
    this.loadMemorialList();
    this.initIntersectionObserver();

    console.log('✅ 메인 페이지 초기화 완료');
  }

  /**
   * 이벤트 바인딩
   */
  bindEvents() {
    console.log('🔗 메인 페이지 이벤트 바인딩');

    // 새 메모리얼 등록 버튼
    const createButtons = document.querySelectorAll('.btn-create-memorial');
    console.log('📝 메모리얼 등록 버튼 개수:', createButtons.length);

    createButtons.forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        console.log('📝 메모리얼 등록 버튼 클릭');
        this.handleCreateMemorial();
      });
    });

    // 페이지 가시성 변경 감지
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) {
        console.log('👁️ 페이지 가시성 복원 - 메모리얼 상태 새로고침');
        this.refreshMemorialStatus();
      }
    });

    // 풀 투 리프레시
    this.initPullToRefresh();

    console.log('✅ 이벤트 바인딩 완료');
  }

  /**
   * 로그인 상태 확인
   */
  checkLoginStatus() {
    console.log('🔐 로그인 상태 확인');

    try {
      this.isLoggedIn = checkLoginStatus();
      console.log('🔐 로그인 상태:', this.isLoggedIn ? '로그인됨' : '로그인 안됨');

      if (this.isLoggedIn) {
        console.log('👤 로그인 사용자 - 정보 업데이트 시작');
        this.updateUserInfo();
      } else {
        console.log('👤 비로그인 사용자 - 로그인 안내 표시');
        this.showLoginGuide();
      }
    } catch (error) {
      console.error('❌ 로그인 상태 확인 중 오류:', error);
      this.isLoggedIn = false;
    }
  }

  /**
   * 로그인 안내 표시
   */
  showLoginGuide() {
    console.log('🔑 로그인 안내 표시');

    const loginSection = document.querySelector('.login-required-section');
    const memorialSection = document.querySelector('.memorial-list-section');
    const emptySection = document.querySelector('.memorial-empty-section');

    // 로그인 안내 섹션 표시
    if (loginSection) {
      loginSection.style.display = 'block';
    }

    // 메모리얼 관련 섹션 숨김
    if (memorialSection) {
      memorialSection.style.display = 'none';
    }
    if (emptySection) {
      emptySection.style.display = 'none';
    }
  }

  /**
   * 메모리얼 목록 로드
   */
  async loadMemorialList() {
    console.log('📋 메모리얼 목록 로드 시작');

    if (this.isLoading) {
      console.log('⏳ 이미 로딩 중 - 스킵');
      return;
    }

    // 로그인 상태 확인
    if (!this.isLoggedIn) {
      console.log('🔐 로그인 안됨 - 메모리얼 목록 로드 스킵');
      return;
    }

    try {
      this.isLoading = true;
      this.showLoadingState();

      console.log('🌐 메모리얼 목록 API 호출');
      const response = await authFetch('/api/memorial', {
        method: 'GET'
      });

      const data = await response.json();
      console.log('📋 메모리얼 목록 응답:', data);

      if (data.status?.code === 'OK_0000') {
        const memorials = data.response || [];
        console.log('📋 메모리얼 개수:', memorials.length);

        if (memorials.length > 0) {
          this.renderMemorialList(memorials);
        } else {
          this.showEmptyState();
        }
      } else {
        throw new Error(data.status?.message || '메모리얼 목록을 불러올 수 없습니다.');
      }

    } catch (error) {
      console.error('❌ 메모리얼 목록 로드 실패:', error);
      this.showErrorState(error.message);
    } finally {
      this.isLoading = false;
      this.hideLoadingState();
      console.log('📋 메모리얼 목록 로드 완료');
    }
  }

  /**
   * 메모리얼 목록 렌더링
   */
  renderMemorialList(memorials) {
    console.log('🎨 메모리얼 목록 렌더링:', memorials.length);

    const container = document.querySelector('.memorial-grid');
    if (!container) {
      console.error('❌ .memorial-grid 컨테이너를 찾을 수 없음');
      return;
    }

    // 빈 상태 섹션 숨김
    const emptySection = document.querySelector('.memorial-empty-section');
    if (emptySection) {
      emptySection.style.display = 'none';
    }

    // 메모리얼 목록 섹션 표시
    const listSection = document.querySelector('.memorial-list-section');
    if (listSection) {
      listSection.style.display = 'block';
    }

    container.innerHTML = '';

    memorials.forEach((memorial, index) => {
      const card = this.createMemorialCard(memorial);
      card.style.animationDelay = `${index * 0.1}s`;
      container.appendChild(card);
    });

    this.memorialCards = memorials;
    console.log('✅ 메모리얼 목록 렌더링 완료');
  }

  /**
   * 메모리얼 카드 생성
   */
  createMemorialCard(memorial) {
    const card = document.createElement('div');
    card.className = 'memorial-card';
    card.dataset.memorialId = memorial.id;

    const lastVisit = memorial.lastVisitDate ?
      new Date(memorial.lastVisitDate).toLocaleDateString('ko-KR') :
      '방문 기록 없음';

    card.innerHTML = `
      <div class="memorial-header">
        <div class="memorial-avatar">
          <img src="${memorial.profileImageUrl || '/assets/mobile/images/default-avatar.png'}" 
               alt="${memorial.name}" class="avatar-img">
          <div class="avatar-status ${memorial.isOnline ? 'online' : 'offline'}"></div>
        </div>
        <div class="memorial-info">
          <h4 class="memorial-name">${memorial.name}</h4>
          <span class="memorial-relationship">${memorial.relationship || '관계 없음'}</span>
          <p class="memorial-last-visit">마지막 방문: ${lastVisit}</p>
        </div>
      </div>
      
      <div class="memorial-actions">
        <a href="/mobile/memorial/call/${memorial.id}" class="btn btn-video-call"
           onclick="return handleVideoCall(this, ${memorial.id})">
          <i class="fas fa-video"></i>
          영상통화
        </a>
        <a href="/mobile/memorial/detail/${memorial.id}" class="btn btn-manage">
          <i class="fas fa-cog"></i>
          관리
        </a>
      </div>
    `;

    return card;
  }

  /**
   * 메모리얼 생성 처리
   */
  async handleCreateMemorial() {
    console.log('📝 메모리얼 생성 처리 시작');

    try {
      // 무료체험 기간 및 제한 확인
      const trialStatus = await this.checkTrialStatus();
      console.log('🎁 체험 상태:', trialStatus);

      if (trialStatus.isTrialUser && trialStatus.memorialCount >= trialStatus.maxMemorials) {
        console.log('⚠️ 무료체험 제한 - 모달 표시');
        this.showTrialLimitModal(trialStatus);
      } else {
        console.log('✅ 메모리얼 생성 가능 - 생성 페이지 이동');
        window.location.href = '/mobile/memorial/create';
      }
    } catch (error) {
      console.error('❌ 메모리얼 생성 처리 실패:', error);

      // 에러가 발생하면 일단 생성 페이지로 이동
      console.log('🔄 에러 발생으로 인한 생성 페이지 직접 이동');
      window.location.href = '/mobile/memorial/create';
    }
  }

  /**
   * 체험 제한 모달
   */
  showTrialLimitModal(trialStatus) {
    console.log('🎁 체험 제한 모달 표시');

    const modalHtml = `
      <div class="trial-limit-modal text-center">
        <div class="modal-icon mb-3">
          <i class="fas fa-info-circle text-primary" style="font-size: 3rem;"></i>
        </div>
        <h4>무료체험 제한</h4>
        <p class="mb-4">무료체험에서는 최대 ${trialStatus.maxMemorials}개의 메모리얼만 등록할 수 있습니다.</p>
        <div class="trial-upgrade-info mb-4">
          <div class="upgrade-item mb-2">
            <i class="fas fa-check text-success me-2"></i>
            <span>무제한 메모리얼 등록</span>
          </div>
          <div class="upgrade-item mb-2">
            <i class="fas fa-check text-success me-2"></i>
            <span>고급 AI 기능 이용</span>
          </div>
          <div class="upgrade-item">
            <i class="fas fa-check text-success me-2"></i>
            <span>가족 공유 기능</span>
          </div>
        </div>
        <div class="modal-actions">
          <a href="/mobile/payment/upgrade" class="btn btn-primary me-2">
            <i class="fas fa-crown"></i>
            프리미엄 업그레이드
          </a>
          <button class="btn btn-outline-secondary" onclick="hideTrialLimitModal()">
            나중에 업그레이드
          </button>
        </div>
      </div>
    `;

    this.showModalWithBootstrap('무료체험 제한', modalHtml, 'trialLimitModal');
  }

  /**
   * Bootstrap 모달 표시
   */
  showModalWithBootstrap(title, content, modalId) {
    const modalHtml = `
      <div class="modal fade" id="${modalId}" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">${title}</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
              ${content}
            </div>
          </div>
        </div>
      </div>
    `;

    // 기존 모달 제거
    const existingModal = document.getElementById(modalId);
    if (existingModal) {
      existingModal.remove();
    }

    // 새 모달 추가
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    // Bootstrap 모달 표시
    if (typeof bootstrap !== 'undefined') {
      const modal = new bootstrap.Modal(document.getElementById(modalId));
      modal.show();
      return modal;
    } else {
      console.error('❌ Bootstrap이 로드되지 않음');
      alert(title + '\n' + content.replace(/<[^>]*>/g, ''));
    }
  }

  /**
   * 체험 상태 확인
   */
  async checkTrialStatus() {
    try {
      const response = await authFetch('/api/user/trial-status');
      const data = await response.json();

      if (data.status?.code === 'OK_0000') {
        return data.response;
      }

      return { isTrialUser: false, memorialCount: 0, maxMemorials: 0 };
    } catch (error) {
      console.error('체험 상태 확인 실패:', error);
      return { isTrialUser: false, memorialCount: 0, maxMemorials: 0 };
    }
  }

  /**
   * 사용자 정보 업데이트
   */
  async updateUserInfo() {
    console.log('👤 사용자 정보 업데이트 시작');

    try {
      const response = await authFetch('/api/user/profile');
      const data = await response.json();

      if (data.status?.code === 'OK_0000') {
        console.log('👤 사용자 정보 업데이트 성공');
        this.updateUserDisplay(data.response);
      }
    } catch (error) {
      console.error('❌ 사용자 정보 업데이트 실패:', error);
    }
  }

  /**
   * 사용자 표시 업데이트
   */
  updateUserDisplay(user) {
    console.log('🎨 사용자 표시 업데이트:', user.name);

    // 사이드 메뉴의 사용자 정보 업데이트
    const userAvatar = document.querySelector('.user-avatar img');
    const userName = document.querySelector('.user-name');
    const userEmail = document.querySelector('.user-email');

    if (userAvatar) {
      userAvatar.src = user.profileImageUrl || '/assets/mobile/images/default-avatar.png';
    }
    if (userName) {
      userName.textContent = user.name;
    }
    if (userEmail) {
      userEmail.textContent = user.email;
    }
  }

  /**
   * 메모리얼 상태 새로고침
   */
  async refreshMemorialStatus() {
    if (!this.isLoggedIn) {
      console.log('🔐 로그인 안됨 - 메모리얼 상태 새로고침 스킵');
      return;
    }

    console.log('🔄 메모리얼 상태 새로고침');

    try {
      const response = await authFetch('/api/memorial/status');
      const data = await response.json();

      if (data.status?.code === 'OK_0000') {
        this.updateMemorialStatus(data.response);
      }
    } catch (error) {
      console.error('❌ 메모리얼 상태 새로고침 실패:', error);
    }
  }

  /**
   * 메모리얼 상태 업데이트
   */
  updateMemorialStatus(statusList) {
    console.log('🔄 메모리얼 상태 업데이트:', statusList.length);

    statusList.forEach(status => {
      const card = document.querySelector(`[data-memorial-id="${status.id}"]`);
      if (card) {
        const statusIndicator = card.querySelector('.avatar-status');
        if (statusIndicator) {
          statusIndicator.className = `avatar-status ${status.isOnline ? 'online' : 'offline'}`;
        }

        const lastVisit = card.querySelector('.memorial-last-visit');
        if (lastVisit && status.lastVisitDate) {
          const visitDate = new Date(status.lastVisitDate).toLocaleDateString('ko-KR');
          lastVisit.textContent = `마지막 방문: ${visitDate}`;
        }
      }
    });
  }

  /**
   * 로딩 상태 표시
   */
  showLoadingState() {
    console.log('⏳ 로딩 상태 표시');

    const container = document.querySelector('.memorial-grid');
    if (container) {
      container.innerHTML = `
        <div class="loading-skeleton">
          ${this.createSkeletonCard()}
          ${this.createSkeletonCard()}
          ${this.createSkeletonCard()}
        </div>
      `;
    }
  }

  /**
   * 스켈레톤 카드 생성
   */
  createSkeletonCard() {
    return `
      <div class="skeleton-card">
        <div class="skeleton-avatar"></div>
        <div class="skeleton-content">
          <div class="skeleton-line"></div>
          <div class="skeleton-line short"></div>
        </div>
      </div>
    `;
  }

  /**
   * 로딩 상태 숨김
   */
  hideLoadingState() {
    console.log('⏳ 로딩 상태 숨김');

    const skeletons = document.querySelectorAll('.loading-skeleton');
    skeletons.forEach(skeleton => skeleton.remove());
  }

  /**
   * 빈 상태 표시
   */
  showEmptyState() {
    console.log('📭 빈 상태 표시');

    // 목록 섹션 숨김
    const listSection = document.querySelector('.memorial-list-section');
    if (listSection) {
      listSection.style.display = 'none';
    }

    // 빈 상태 섹션 표시
    const emptySection = document.querySelector('.memorial-empty-section');
    if (emptySection) {
      emptySection.style.display = 'block';
    }
  }

  /**
   * 에러 상태 표시
   */
  showErrorState(message) {
    console.log('❌ 에러 상태 표시:', message);

    const container = document.querySelector('.memorial-grid');
    if (container) {
      container.innerHTML = `
        <div class="error-state">
          <div class="error-icon">
            <i class="fas fa-exclamation-triangle"></i>
          </div>
          <h4>오류가 발생했습니다</h4>
          <p>${message}</p>
          <button class="btn btn-outline-primary" onclick="mainPageManager.loadMemorialList()">
            다시 시도
          </button>
        </div>
      `;
    }
  }

  /**
   * 교차 관찰자 초기화
   */
  initIntersectionObserver() {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
        }
      });
    }, {
      threshold: 0.1,
      rootMargin: '50px'
    });

    // 관찰 대상 요소들
    const targets = document.querySelectorAll('.memorial-card, .empty-state-card, .login-guide-card');
    targets.forEach(target => observer.observe(target));
  }

  /**
   * 풀 투 리프레시 초기화
   */
  initPullToRefresh() {
    let startY = 0;
    let currentY = 0;
    let isPulling = false;

    document.addEventListener('touchstart', (e) => {
      if (window.scrollY === 0) {
        startY = e.touches[0].pageY;
        isPulling = true;
      }
    });

    document.addEventListener('touchmove', (e) => {
      if (!isPulling) return;

      currentY = e.touches[0].pageY;
      const pullDistance = currentY - startY;

      if (pullDistance > 100) {
        // 새로고침 트리거
        this.refreshMemorialStatus();
        showToast('새로고침 중...', 'info');
        isPulling = false;
      }
    });

    document.addEventListener('touchend', () => {
      isPulling = false;
    });
  }
}

// 전역 인스턴스 생성
let mainPageManager;

// DOM 로드 완료 시 초기화
console.log('🌟 main.js 스크립트 로드 완료');

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    console.log('📱 DOM 로드 완료 - 메인 페이지 매니저 초기화');
    mainPageManager = new MainPageManager();
    window.mainPageManager = mainPageManager;
  });
} else {
  console.log('📱 DOM 이미 로드됨 - 메인 페이지 매니저 즉시 초기화');
  mainPageManager = new MainPageManager();
  window.mainPageManager = mainPageManager;
}

// 전역 함수들 (HTML에서 호출)
window.hideTrialLimitModal = () => {
  const modal = bootstrap.Modal.getInstance(document.getElementById('trialLimitModal'));
  if (modal) {
    modal.hide();
  }
};

// 전역 디버깅 함수
window.debugMainPage = () => {
  console.group('🔍 메인 페이지 디버그 정보');
  console.log('메인 페이지 매니저:', window.mainPageManager);
  console.log('로그인 상태:', window.mainPageManager?.isLoggedIn);
  console.log('로딩 상태:', window.mainPageManager?.isLoading);
  console.log('메모리얼 카드 수:', window.mainPageManager?.memorialCards?.length || 0);
  console.groupEnd();
};

// 전역 새로고침 함수
window.refreshMemorials = () => {
  if (window.mainPageManager) {
    window.mainPageManager.loadMemorialList();
  }
};