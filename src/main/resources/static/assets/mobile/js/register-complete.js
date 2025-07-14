// register-complete.js - 회원가입 완료 페이지

// ===== 초기화 함수 =====
const initRegisterCompletePage = () => {
  console.log("🎉 회원가입 완료 페이지 초기화");

  // DOM 요소 초기화
  const elements = initializeElements();
  if (!elements) {
    console.error('❌ 필수 DOM 요소를 찾을 수 없습니다.');
    return;
  }

  // 이벤트 리스너 등록
  registerEventListeners(elements);

  // 데이터 정리
  cleanupRegistrationData();

  console.log('✅ 회원가입 완료 페이지 초기화 완료');
};

// ===== DOM 요소 초기화 =====
const initializeElements = () => {
  const elements = {
    loginButton: document.getElementById('loginButton')
  };

  // 필수 요소 체크
  if (!elements.loginButton) {
    console.error('❌ 로그인 버튼을 찾을 수 없습니다.');
    return null;
  }

  return elements;
};

// ===== 이벤트 리스너 등록 =====
const registerEventListeners = (elements) => {
  console.log('🔗 이벤트 리스너 등록');

  // 로그인 버튼 클릭
  elements.loginButton.addEventListener('click', () => {
    handleLoginRedirect();
  });

  // Enter 키 처리
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      handleLoginRedirect();
    }
  });

  console.log('✅ 이벤트 리스너 등록 완료');
};

// ===== 로그인 페이지로 이동 =====
const handleLoginRedirect = () => {
  console.log('🔐 로그인 페이지로 이동');

  // 로그인 URL 설정
  const loginUrl = window.PAGE_CONFIG?.loginUrl || '/mobile/login';

  // 부드러운 전환을 위한 애니메이션
  document.body.style.opacity = '0.8';
  document.body.style.transition = 'opacity 0.3s ease';

  setTimeout(() => {
    window.location.href = loginUrl;
  }, 200);
};

// ===== 회원가입 관련 데이터 정리 =====
const cleanupRegistrationData = () => {
  try {
    // 회원가입 과정에서 사용된 임시 데이터 정리
    sessionStorage.removeItem('phoneVerification');
    localStorage.removeItem('agreementData');

    console.log('🗑️ 회원가입 임시 데이터 정리 완료');
  } catch (error) {
    console.error('❌ 데이터 정리 중 오류:', error);
  }
};

// ===== 뒤로가기 방지 =====
const preventBackNavigation = () => {
  // 브라우저 뒤로가기 버튼 방지
  window.history.pushState(null, null, window.location.pathname);

  window.addEventListener('popstate', (e) => {
    console.log('🚫 뒤로가기 차단됨');
    window.history.pushState(null, null, window.location.pathname);

    // 사용자에게 안내 (선택사항)
    // alert('회원가입이 완료되었습니다. 로그인 버튼을 눌러주세요.');
  });
};

// ===== 페이지 로드 시 초기화 =====
document.addEventListener('DOMContentLoaded', () => {
  initRegisterCompletePage();

  // 뒤로가기 방지 활성화
  preventBackNavigation();

  // 페이지 진입 로그
  console.log('🎊 회원가입 완료 페이지 진입');
});

// ===== 페이지 가시성 변경 감지 =====
document.addEventListener('visibilitychange', () => {
  if (!document.hidden) {
    console.log('👁️ 회원가입 완료 페이지 가시성 복원');
  }
});

// ===== 키보드 이벤트 처리 =====
document.addEventListener('keydown', (e) => {
  // ESC 키 무시 (모달이 없으므로)
  if (e.key === 'Escape') {
    e.preventDefault();
  }

  // Space 키로도 로그인 가능
  if (e.key === ' ' || e.key === 'Spacebar') {
    e.preventDefault();
    handleLoginRedirect();
  }
});

// ===== 디버그 함수 (개발용) =====
if (window.location.search.includes('debug=true')) {
  window.registerCompleteDebug = {
    goToLogin: () => {
      handleLoginRedirect();
    },
    checkCleanup: () => {
      console.log('📊 정리 상태 확인:');
      console.log('  phoneVerification:', sessionStorage.getItem('phoneVerification'));
      console.log('  agreementData:', localStorage.getItem('agreementData'));
    },
    simulateError: () => {
      console.error('🧪 테스트 에러 시뮬레이션');
      alert('테스트용 에러입니다.');
    }
  };

  console.log('🔧 디버그 모드 활성화 - window.registerCompleteDebug 사용 가능');
  console.log('💡 사용법:');
  console.log('  - window.registerCompleteDebug.goToLogin() : 로그인 페이지로 이동');
  console.log('  - window.registerCompleteDebug.checkCleanup() : 데이터 정리 상태 확인');
}