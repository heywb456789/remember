// memorial-create-complete.js - 메모리얼 등록 완료 페이지 JavaScript

import { authFetch, checkLoginStatus, handleFetchError } from './commonFetch.js';
import { showToast, showConfirm, showLoading, hideLoading } from './common.js';

// 메모리얼 완료 페이지 상태 관리
let memorialCompleteState = {
  currentStep: 4,
  maxStep: 4,
  memorial: null,
  aiLearningSteps: [],
  isInitialized: false,
  aiProgressInterval: null,
  aiProgressStartTime: null,
  currentProgressStep: 0,
  urls: {}
};

/**
 * 메모리얼 완료 페이지 초기화
 */
function initializeMemorialComplete() {
  console.log('🚀 메모리얼 완료 페이지 초기화 시작');

  if (memorialCompleteState.isInitialized) {
    console.warn('⚠️ 메모리얼 완료 페이지가 이미 초기화되었습니다.');
    return;
  }

  try {
    // 1. 로그인 상태 확인
    if (!checkLoginStatus()) {
      window.location.href = '/mobile/login';
      return;
    }

    // 2. 서버 데이터 로드
    loadServerData();

    // 3. 페이지 초기화
    initializePage();

    // 4. AI 학습 상태 모니터링 시작
    startAiLearningMonitoring();

    // 5. 초기화 완료
    memorialCompleteState.isInitialized = true;
    console.log('✅ 메모리얼 완료 페이지 초기화 완료');

  } catch (error) {
    console.error('❌ 메모리얼 완료 페이지 초기화 실패:', error);
    showToast('페이지 초기화 중 오류가 발생했습니다.', 'error');
  }
}

/**
 * 서버 데이터 로드
 */
function loadServerData() {
  console.log('📊 서버 데이터 로드');

  if (window.memorialCompleteData) {
    memorialCompleteState.memorial = window.memorialCompleteData.memorial;
    memorialCompleteState.aiLearningSteps = window.memorialCompleteData.aiLearningSteps;
    memorialCompleteState.urls = window.memorialCompleteData.urls;

    console.log('📊 서버 데이터 로드 완료:', {
      memorial: memorialCompleteState.memorial,
      aiTrainingStatus: memorialCompleteState.memorial.aiTrainingStatus
    });
  }
}

/**
 * 페이지 초기화
 */
function initializePage() {
  console.log('📝 페이지 초기화');

  // 1. 축하 애니메이션 실행
  playSuccessAnimation();

  // 2. 공유 링크 설정
  setupShareLink();

  // 3. 액션 버튼 상태 업데이트
  updateActionButtons();

  // 4. AI 학습 상태 초기화
  initializeAiLearningStatus();

  console.log('✅ 페이지 초기화 완료');
}

/**
 * 축하 애니메이션 실행
 */
function playSuccessAnimation() {
  console.log('🎉 축하 애니메이션 실행');

  // 성공 아이콘 애니메이션
  const successIcon = document.querySelector('.success-icon');
  if (successIcon) {
    successIcon.classList.add('animate-bounce');

    // 3초 후 애니메이션 제거
    setTimeout(() => {
      successIcon.classList.remove('animate-bounce');
    }, 3000);
  }

  // 완료 메시지 페이드 인
  const completionMessage = document.querySelector('.completion-message');
  if (completionMessage) {
    completionMessage.classList.add('fade-in');
  }

  // 액션 카드들 순차적 나타남
  const actionCards = document.querySelectorAll('.action-card');
  actionCards.forEach((card, index) => {
    setTimeout(() => {
      card.classList.add('slide-up');
    }, 500 + (index * 200));
  });
}

/**
 * 공유 링크 설정
 */
function setupShareLink() {
  console.log('🔗 공유 링크 설정');

  const shareLink = document.getElementById('shareLink');
  if (shareLink && memorialCompleteState.urls.share) {
    shareLink.value = memorialCompleteState.urls.share;
  }
}

/**
 * 액션 버튼 상태 업데이트
 */
function updateActionButtons() {
  console.log('🔘 액션 버튼 상태 업데이트');

  const videoCallAction = document.getElementById('videoCallAction');
  if (videoCallAction) {
    const isAiComplete = memorialCompleteState.memorial.aiTrainingCompleted;

    if (isAiComplete) {
      videoCallAction.classList.remove('disabled');
      videoCallAction.querySelector('p').textContent = '지금 바로 영상통화를 시작하세요!';
    } else {
      videoCallAction.classList.add('disabled');
      videoCallAction.querySelector('p').textContent = 'AI 학습 완료 후 가능합니다';
    }
  }
}

/**
 * AI 학습 상태 초기화
 */
function initializeAiLearningStatus() {
  console.log('🤖 AI 학습 상태 초기화');

  const aiStatus = memorialCompleteState.memorial.aiTrainingStatus;
  const isCompleted = memorialCompleteState.memorial.aiTrainingCompleted;

  console.log('AI 학습 상태:', aiStatus, '완료 여부:', isCompleted);

  if (isCompleted) {
    // 이미 완료된 경우
    showAiLearningComplete();
  } else {
    // 학습 중인 경우 상태에 따라 진행률 표시
    updateAiLearningProgress(aiStatus);
  }
}

/**
 * AI 학습 모니터링 시작
 */
function startAiLearningMonitoring() {
  console.log('👁️ AI 학습 모니터링 시작');

  // 이미 완료된 경우 모니터링 불필요
  if (memorialCompleteState.memorial.aiTrainingCompleted) {
    console.log('✅ AI 학습이 이미 완료되었습니다.');
    return;
  }

  // 주기적으로 AI 학습 상태 확인 (30초마다)
  memorialCompleteState.aiProgressInterval = setInterval(async () => {
    await checkAiLearningProgress();
  }, 30000);

  // 초기 상태 확인
  setTimeout(() => {
    checkAiLearningProgress();
  }, 5000);
}

/**
 * AI 학습 진행률 확인
 */
async function checkAiLearningProgress() {
  console.log('🔍 AI 학습 진행률 확인');

  try {
    const response = await authFetch(`/api/memorial/${memorialCompleteState.memorial.id}`, {
      method: 'GET'
    });

    if (!response.ok) {
      throw new Error('메모리얼 정보 조회 실패');
    }

    const result = await response.json();

    if (result.success && result.data) {
      const updatedMemorial = result.data;

      // 상태 업데이트
      memorialCompleteState.memorial.aiTrainingStatus = updatedMemorial.aiTrainingStatus;
      memorialCompleteState.memorial.aiTrainingCompleted = updatedMemorial.aiTrainingCompleted;

      // UI 업데이트
      updateAiLearningProgress(updatedMemorial.aiTrainingStatus);

      // 완료 확인
      if (updatedMemorial.aiTrainingCompleted) {
        handleAiLearningComplete();
      }
    }

  } catch (error) {
    console.error('❌ AI 학습 진행률 확인 실패:', error);
  }
}

/**
 * AI 학습 진행률 업데이트
 */
function updateAiLearningProgress(status) {
  console.log('📊 AI 학습 진행률 업데이트:', status);

  const progressBar = document.getElementById('aiProgressBar');
  const progressText = document.getElementById('aiProgressText');

  let progress = 0;
  let statusText = 'AI 학습 준비 중...';

  switch (status) {
    case 'PENDING':
      progress = 10;
      statusText = 'AI 학습 준비 중...';
      updateLearningStep('step-image', 'preparing', '준비 중...');
      break;
    case 'IN_PROGRESS':
      progress = 25;
      statusText = '이미지 분석 중...';
      updateLearningStep('step-image', 'active', '이미지 분석 중...');
      break;
    case 'VOICE_LEARNING':
      progress = 50;
      statusText = '음성 학습 중...';
      updateLearningStep('step-image', 'completed', '분석 완료');
      updateLearningStep('step-voice', 'active', '음성 학습 중...');
      break;
    case 'VIDEO_PROCESSING':
      progress = 75;
      statusText = '영상 분석 중...';
      updateLearningStep('step-voice', 'completed', '학습 완료');
      updateLearningStep('step-video', 'active', '영상 분석 중...');
      break;
    case 'COMPLETED':
      progress = 100;
      statusText = 'AI 학습 완료!';
      updateLearningStep('step-video', 'completed', '분석 완료');
      updateLearningStep('step-complete', 'completed', '학습 완료!');
      break;
    case 'FAILED':
      progress = 0;
      statusText = 'AI 학습 실패 (재시도 중...)';
      break;
  }

  // 진행률 바 업데이트
  if (progressBar) {
    progressBar.style.width = `${progress}%`;
  }

  // 진행률 텍스트 업데이트
  if (progressText) {
    progressText.textContent = statusText;
  }
}

/**
 * 개별 학습 단계 업데이트
 */
function updateLearningStep(stepId, status, statusText) {
  const stepElement = document.getElementById(stepId);
  if (!stepElement) return;

  const statusElement = stepElement.querySelector('.step-status i');
  const textElement = stepElement.querySelector(`#${stepId}-status`);

  // 상태 클래스 업데이트
  stepElement.className = `learning-step ${status}`;

  // 상태 아이콘 업데이트
  if (statusElement) {
    switch (status) {
      case 'preparing':
        statusElement.className = 'fas fa-clock text-muted';
        break;
      case 'active':
        statusElement.className = 'fas fa-spinner fa-spin text-primary';
        break;
      case 'completed':
        statusElement.className = 'fas fa-check text-success';
        break;
      case 'failed':
        statusElement.className = 'fas fa-times text-danger';
        break;
    }
  }

  // 상태 텍스트 업데이트
  if (textElement) {
    textElement.textContent = statusText;
  }
}

/**
 * AI 학습 완료 처리
 */
function handleAiLearningComplete() {
  console.log('🎉 AI 학습 완료 처리');

  // 모니터링 중지
  if (memorialCompleteState.aiProgressInterval) {
    clearInterval(memorialCompleteState.aiProgressInterval);
    memorialCompleteState.aiProgressInterval = null;
  }

  // UI 업데이트
  showAiLearningComplete();

  // 축하 모달 표시
  setTimeout(() => {
    showAiCompleteModal();
  }, 2000);
}

/**
 * AI 학습 완료 상태 표시
 */
function showAiLearningComplete() {
  console.log('✅ AI 학습 완료 상태 표시');

  // 진행률 100%로 설정
  updateAiLearningProgress('COMPLETED');

  // 액션 버튼 활성화
  updateActionButtons();

  // 성공 메시지 표시
  showToast('AI 학습이 완료되었습니다! 이제 영상통화를 시작할 수 있어요.', 'success');
}

/**
 * AI 완료 축하 모달 표시
 */
function showAiCompleteModal() {
  const modal = new bootstrap.Modal(document.getElementById('aiCompleteModal'));
  modal.show();
}

/**
 * 네비게이션 함수들
 */
function goToHome() {
  console.log('🏠 홈으로 이동');
  window.location.href = memorialCompleteState.urls.home || '/mobile/home';
}

function goToMemorialDetail() {
  console.log('👁️ 메모리얼 상세 페이지로 이동');
  window.location.href = memorialCompleteState.urls.memorial;
}

function goToVideoCall() {
  console.log('📹 영상통화 페이지로 이동');

  if (!memorialCompleteState.memorial.aiTrainingCompleted) {
    showToast('AI 학습이 완료된 후 영상통화를 시작할 수 있습니다.', 'warning');
    return;
  }

  window.location.href = memorialCompleteState.urls.videoCall;
}

function goToTribute() {
  console.log('💝 추모 페이지로 이동');
  window.location.href = memorialCompleteState.urls.tribute;
}

/**
 * 공유 관련 함수들
 */
function showShareModal() {
  console.log('📤 공유 모달 표시');
  const modal = new bootstrap.Modal(document.getElementById('shareModal'));
  modal.show();
}

function copyShareLink() {
  console.log('📋 공유 링크 복사');

  const shareLink = document.getElementById('shareLink');
  if (shareLink) {
    shareLink.select();
    shareLink.setSelectionRange(0, 99999);

    try {
      document.execCommand('copy');
      showToast('공유 링크가 복사되었습니다.', 'success');
    } catch (err) {
      console.error('복사 실패:', err);
      showToast('복사에 실패했습니다. 수동으로 복사해주세요.', 'error');
    }
  }
}

function shareToKakao() {
  console.log('📱 카카오톡 공유');

  if (typeof Kakao !== 'undefined' && Kakao.Link) {
    Kakao.Link.sendDefault({
      objectType: 'feed',
      content: {
        title: `${memorialCompleteState.memorial.name}님의 메모리얼`,
        description: '소중한 추억을 함께 나눠보세요.',
        imageUrl: memorialCompleteState.memorial.profileImageUrls?.[0] || '/assets/images/default-memorial.jpg',
        link: {
          mobileWebUrl: memorialCompleteState.urls.share,
          webUrl: memorialCompleteState.urls.share,
        },
      },
      buttons: [
        {
          title: '메모리얼 보기',
          link: {
            mobileWebUrl: memorialCompleteState.urls.share,
            webUrl: memorialCompleteState.urls.share,
          },
        },
      ],
    });
  } else {
    showToast('카카오톡 공유를 사용할 수 없습니다.', 'warning');
  }
}

function shareToFacebook() {
  console.log('📘 페이스북 공유');

  const url = encodeURIComponent(memorialCompleteState.urls.share);
  const title = encodeURIComponent(`${memorialCompleteState.memorial.name}님의 메모리얼`);

  window.open(`https://www.facebook.com/sharer/sharer.php?u=${url}&t=${title}`, '_blank', 'width=600,height=400');
}

function shareToTwitter() {
  console.log('🐦 트위터 공유');

  const url = encodeURIComponent(memorialCompleteState.urls.share);
  const text = encodeURIComponent(`${memorialCompleteState.memorial.name}님의 메모리얼 - 소중한 추억을 함께 나눠보세요.`);

  window.open(`https://twitter.com/intent/tweet?url=${url}&text=${text}`, '_blank', 'width=600,height=400');
}

function shareToInstagram() {
  console.log('📸 인스타그램 공유');

  // 인스타그램은 직접 공유 API가 없으므로 앱 열기 시도
  const instagramUrl = `instagram://camera`;

  try {
    window.open(instagramUrl, '_blank');
  } catch (error) {
    showToast('인스타그램 앱을 열 수 없습니다.', 'warning');
  }
}

/**
 * 정리 함수
 */
function destroyMemorialComplete() {
  console.log('🗑️ 메모리얼 완료 페이지 정리');

  // AI 진행률 모니터링 중지
  if (memorialCompleteState.aiProgressInterval) {
    clearInterval(memorialCompleteState.aiProgressInterval);
    memorialCompleteState.aiProgressInterval = null;
  }

  memorialCompleteState.isInitialized = false;
}

/**
 * 전역 함수들
 */
window.memorialCompleteManager = {
  initialize: initializeMemorialComplete,
  destroy: destroyMemorialComplete,
  getState: () => memorialCompleteState
};

// 전역 함수들 (HTML에서 호출 가능)
window.goToHome = goToHome;
window.goToMemorialDetail = goToMemorialDetail;
window.goToVideoCall = goToVideoCall;
window.goToTribute = goToTribute;
window.showShareModal = showShareModal;
window.copyShareLink = copyShareLink;
window.shareToKakao = shareToKakao;
window.shareToFacebook = shareToFacebook;
window.shareToTwitter = shareToTwitter;
window.shareToInstagram = shareToInstagram;

/**
 * 자동 초기화
 */
console.log('🌟 memorial-create-complete.js 로드 완료');

// DOM이 준비되면 초기화
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initializeMemorialComplete);
} else {
  setTimeout(initializeMemorialComplete, 100);
}

// 페이지 언로드 시 정리
window.addEventListener('beforeunload', destroyMemorialComplete);

// 모듈 익스포트
export {
  initializeMemorialComplete,
  goToHome,
  goToMemorialDetail,
  goToVideoCall,
  goToTribute,
  showShareModal,
  destroyMemorialComplete
};