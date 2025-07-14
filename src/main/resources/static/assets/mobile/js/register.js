// register.js - OneID 회원가입 약관 동의 페이지

import { checkLoginStatus } from './commonFetch.js';
import { showToast } from './common.js';

// ===== 전역 변수 =====
let currentAgreementType = null;
let isScrolledToBottom = false;

// ===== 약관 내용 데이터 =====
const agreementContents = {
  terms: {
    title: '이용 약관',
    content: `제1조 (목적)
이 약관은 토마토그룹이 제공하는 OneID 서비스의 이용조건 및 절차에 관한 사항과 기타 필요한 사항을 규정함을 목적으로 합니다.

제2조 (정의)
1. "서비스"라 함은 토마토그룹이 제공하는 OneID 통합 인증 서비스를 의미합니다.
2. "이용자"라 함은 이 약관에 따라 회사가 제공하는 서비스를 받는 회원 및 비회원을 말합니다.
3. "회원"이라 함은 회사에 개인정보를 제공하여 회원등록을 한 자로서, 회사의 정보를 지속적으로 제공받으며, 회사가 제공하는 서비스를 계속적으로 이용할 수 있는 자를 말합니다.

제3조 (약관의 효력 및 변경)
1. 이 약관은 서비스 화면에 게시하거나 기타의 방법으로 회원에게 공지함으로써 효력을 발생합니다.
2. 회사는 합리적인 사유가 발생할 경우에는 이 약관을 변경할 수 있으며, 약관이 변경되는 경우 변경된 약관의 내용과 시행일을 정하여, 그 시행일로부터 최소 7일 이전에 공지합니다.

제4조 (서비스의 제공 및 변경)
1. 회사는 다음과 같은 업무를 수행합니다.
   - OneID 통합 인증 서비스 제공
   - 토마토그룹 계열사 서비스 연동
   - 개인정보 보호 및 보안 서비스
   - 기타 회사가 정하는 업무

제5조 (서비스의 중단)
1. 회사는 컴퓨터 등 정보통신설비의 보수점검, 교체 및 고장, 통신의 두절 등의 사유가 발생한 경우에는 서비스의 제공을 일시적으로 중단할 수 있습니다.
2. 회사는 제1항의 사유로 서비스의 제공이 일시적으로 중단됨으로 인하여 이용자 또는 제3자가 입은 손해에 대하여 배상하지 않습니다.

제6조 (회원가입)
1. 이용자는 회사가 정한 가입 양식에 따라 회원정보를 기입한 후 이 약관에 동의한다는 의사표시를 함으로서 회원가입을 신청합니다.
2. 회사는 제1항과 같이 회원으로 가입할 것을 신청한 이용자 중 다음 각 호에 해당하지 않는 한 회원으로 등록합니다.

제7조 (회원탈퇴 및 자격 상실)
1. 회원은 회사에 언제든지 탈퇴를 요청할 수 있으며 회사는 즉시 회원탈퇴를 처리합니다.
2. 회원이 다음 각 호의 사유에 해당하는 경우, 회사는 회원자격을 제한 및 정지시킬 수 있습니다.

본 약관에 동의하시면 OneID 서비스를 이용하실 수 있습니다.`
  },
  privacy: {
    title: '개인정보처리방침',
    content: `토마토그룹 개인정보처리방침

토마토그룹(이하 "회사")는 개인정보보호법에 따라 이용자의 개인정보 보호 및 권익을 보호하고 개인정보와 관련한 이용자의 고충을 원활하게 처리할 수 있도록 다음과 같은 처리방침을 두고 있습니다.

1. 개인정보의 처리목적
회사는 다음의 목적을 위하여 개인정보를 처리하고 있으며, 다음의 목적 이외의 용도로는 이용하지 않습니다.
- OneID 회원가입 및 관리
- 토마토그룹 서비스 제공
- 본인확인 및 인증
- 고객상담 및 불만처리
- 마케팅 및 광고에의 활용

2. 개인정보의 처리 및 보유기간
① 회사는 정보주체로부터 개인정보를 수집할 때 동의받은 개인정보 보유·이용기간 또는 법령에 따른 개인정보 보유·이용기간 내에서 개인정보를 처리·보유합니다.
② 구체적인 개인정보 처리 및 보유 기간은 다음과 같습니다.
- 회원가입 및 관리: 회원탈퇴 시까지
- 서비스 제공: 서비스 종료 시까지
- 마케팅 활용: 동의철회 시까지

3. 개인정보의 제3자 제공
① 회사는 정보주체의 개인정보를 1조에서 명시한 범위 내에서만 처리하며, 정보주체의 동의, 법률의 특별한 규정 등 개인정보보호법 제17조 및 제18조에 해당하는 경우에만 개인정보를 제3자에게 제공합니다.

4. 개인정보처리 위탁
① 회사는 원활한 개인정보 업무처리를 위하여 다음과 같이 개인정보 처리업무를 위탁하고 있습니다.
- 위탁받는 자: 클라우드 서비스 제공업체
- 위탁하는 업무의 내용: 데이터 저장 및 관리

5. 정보주체의 권리
정보주체는 개인정보보호법에 따라 다음과 같은 권리를 행사할 수 있습니다.
- 개인정보 처리정지 요구권
- 개인정보 열람요구권
- 개인정보 정정·삭제요구권
- 손해배상청구권

6. 개인정보의 안전성 확보조치
회사는 개인정보보호법 제29조에 따라 다음과 같이 안전성 확보에 필요한 기술적/관리적 및 물리적 조치를 하고 있습니다.
- 개인정보 취급 직원의 최소화 및 교육
- 개인정보에 대한 접근 제한
- 접속기록의 보관 및 위변조 방지
- 개인정보의 암호화
- 보안프로그램 설치 및 갱신

본 방침은 2024년 1월 1일부터 시행됩니다.`
  },
  location: {
    title: '위치정보 약관',
    content: `위치정보 서비스 이용약관

제1조 (목적)
이 약관은 토마토그룹(이하 "회사")이 제공하는 위치정보서비스와 관련하여 회사와 개인위치정보주체와의 권리, 의무 및 책임사항, 기타 필요한 사항을 규정함을 목적으로 합니다.

제2조 (약관의 효력 및 변경)
① 이 약관은 개인위치정보주체가 동의버튼을 클릭하거나 위치정보의 수집·이용·제공에 동의함으로써 효력이 발생합니다.
② 회사는 위치정보의 보호 및 이용 등에 관한 법률, 정보통신망 이용촉진 및 정보보호 등에 관한 법률, 전기통신사업법, 개인정보보호법 등 관련 법령을 준수합니다.

제3조 (서비스 내용 및 요금)
① 회사는 직접 수집하거나 위치정보사업자로부터 수집한 개인위치정보를 이용하여 다음과 같은 서비스를 제공합니다.
- 현재 위치 기반 서비스 제공
- 길 찾기 및 교통정보 제공
- 주변 시설 정보 제공
- 위치 기반 맞춤형 콘텐츠 제공

제4조 (개인위치정보주체의 권리)
① 개인위치정보주체는 개인위치정보 수집·이용·제공에 대한 동의를 언제든지 철회할 수 있습니다.
② 개인위치정보주체는 언제든지 개인위치정보의 수집·이용·제공의 일시중지를 요구할 수 있습니다.
③ 개인위치정보주체는 다음 각 호의 자료에 대하여 열람 또는 고지를 요구할 수 있습니다.

제5조 (개인위치정보의 이용·제공)
① 회사는 개인위치정보를 이용하여 서비스를 제공하고자 하는 경우에는 미리 약관에 명시하거나 개인위치정보주체에게 고지하고 동의를 받습니다.
② 회사는 개인위치정보주체의 동의 없이 개인위치정보를 제3자에게 제공하지 않으며, 제3자 제공 서비스를 제공하는 경우에는 제공받는 자 및 제공목적을 사전에 개인위치정보주체에게 고지하고 동의를 받습니다.

제6조 (개인위치정보의 보관·이용기간)
① 회사는 위치정보의 보호 및 이용 등에 관한 법률 제16조 제2항에 근거하여 개인위치정보를 자동으로 기록·보존하며, 해당 개인위치정보는 6개월간 보관합니다.
② 개인위치정보주체가 개인위치정보의 수집·이용·제공에 동의한 경우에는 개인위치정보를 해당 서비스 제공을 위해 필요한 최소한의 기간 동안 보관·이용합니다.

제7조 (손해배상)
① 회사가 위치정보의 보호 및 이용 등에 관한 법률 제15조 내지 제26조의 규정을 위반한 행위로 개인위치정보주체에게 손해가 발생한 경우 개인위치정보주체는 회사에 대하여 손해배상을 청구할 수 있습니다.

본 약관은 2024년 1월 1일부터 시행됩니다.`
  },
  marketing: {
    title: '마케팅 정보 수신 동의',
    content: `마케팅 정보 수신 동의 약관

1. 수집하는 개인정보 항목
회사는 마케팅 정보 제공을 위해 다음과 같은 개인정보를 수집합니다.
- 필수항목: 이름, 휴대폰번호, 이메일주소
- 선택항목: 생년월일, 성별, 관심분야

2. 개인정보의 수집 및 이용목적
- 신규 서비스 및 이벤트 정보 제공
- 맞춤형 광고 및 마케팅 콘텐츠 제공
- 고객만족도 조사 및 마케팅 분석
- 프로모션 및 이벤트 참여기회 제공

3. 개인정보의 보유 및 이용기간
- 보유기간: 동의철회 시 또는 회원탈퇴 시까지
- 동의철회 방법: 
  * 이메일 수신거부 링크 클릭
  * 고객센터 전화 (1588-0000)
  * 마이페이지에서 직접 설정 변경

4. 마케팅 정보 발송 방법
- 이메일을 통한 뉴스레터 발송
- SMS/MMS를 통한 이벤트 정보 발송
- 앱 푸시 알림을 통한 신규 서비스 안내
- 우편을 통한 카탈로그 및 안내자료 발송

5. 제3자 제공
회사는 마케팅 목적으로 수집한 개인정보를 원칙적으로 제3자에게 제공하지 않습니다. 다만, 다음의 경우에는 예외로 합니다.
- 이용자들이 사전에 동의한 경우
- 법령의 규정에 의거하거나, 수사 목적으로 법령에 정해진 절차와 방법에 따라 수사기관의 요구가 있는 경우

6. 동의거부권 및 불이익
마케팅 정보 수신에 대한 동의는 선택사항입니다. 동의를 거부하셔도 OneID 서비스 이용에는 제한이 없으나, 각종 이벤트 및 프로모션 정보를 받아보실 수 없습니다.

7. 개인정보 처리위탁
원활한 마케팅 서비스 제공을 위하여 다음과 같이 개인정보 처리를 위탁하고 있습니다.
- 위탁받는 자: 이메일/SMS 발송 전문업체
- 위탁업무 내용: 마케팅 이메일 및 SMS 발송 대행

본 동의는 2024년 1월 1일부터 시행됩니다.

마케팅 정보 수신에 동의하시면 토마토그룹의 다양한 혜택과 최신 정보를 가장 먼저 받아보실 수 있습니다.`
  }
};

// ===== 초기화 함수 =====
const initRegisterPage = () => {
  console.log("🚀 OneID 회원가입 약관 동의 페이지 초기화");

  // 이미 로그인된 상태면 홈으로 리다이렉트
  if (checkLoginStatus()) {
    console.log('✅ 이미 로그인된 상태입니다.');
    showToast('이미 로그인되어 있습니다.', 'info');
    setTimeout(() => {
      window.location.href = '/';
    }, 1000);
    return;
  }

  // DOM 요소 초기화
  const elements = initializeElements();
  if (!elements) {
    console.error('❌ 필수 DOM 요소를 찾을 수 없습니다.');
    return;
  }

  // 이벤트 리스너 등록
  registerEventListeners(elements);

  // 약관 동의 초기화
  initAgreements(elements);

  // 모달 초기화
  initModals();

  console.log('✅ OneID 회원가입 페이지 초기화 완료');
};

// ===== DOM 요소 초기화 =====
const initializeElements = () => {
  const elements = {
    // 약관 체크박스
    agreeAll: document.getElementById('agreeAll'),
    agreeTerms: document.getElementById('agreeTerms'),
    agreePrivacy: document.getElementById('agreePrivacy'),
    agreeLocation: document.getElementById('agreeLocation'),
    agreeMarketing: document.getElementById('agreeMarketing'),

    // 버튼
    nextButton: document.getElementById('nextButton'),

    // 모달 요소들
    agreementModal: document.getElementById('agreementModal'),
    modalTitle: document.getElementById('modalTitle'),
    modalBody: document.getElementById('modalBody'),
    modalClose: document.getElementById('modalClose'),
    modalConfirm: document.getElementById('modalConfirm'),

    // 약관 동의 요청 모달
    agreementRequestModal: document.getElementById('agreementRequestModal'),
    agreementRequestConfirm: document.getElementById('agreementRequestConfirm')
  };

  // 필수 요소 체크
  const requiredElements = ['agreeAll', 'agreeTerms', 'agreePrivacy', 'agreeLocation', 'nextButton'];
  const missingElements = requiredElements.filter(key => !elements[key]);

  if (missingElements.length > 0) {
    console.error('❌ 필수 DOM 요소 누락:', missingElements);
    return null;
  }

  return elements;
};

// ===== 이벤트 리스너 등록 =====
const registerEventListeners = (elements) => {
  console.log('🔗 이벤트 리스너 등록');

  // 다음 버튼 클릭
  elements.nextButton.addEventListener('click', () => {
    handleNext(elements);
  });

  // 약관 보기 링크들
  document.querySelectorAll('.agreement-link').forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      const agreementType = link.getAttribute('data-agreement');
      showAgreementModal(agreementType);
    });
  });

  console.log('✅ 이벤트 리스너 등록 완료');
};

// ===== 약관 동의 초기화 =====
const initAgreements = (elements) => {
  // 전체 동의 체크박스 처리
  elements.agreeAll.addEventListener('change', () => {
    const isChecked = elements.agreeAll.checked;
    elements.agreeTerms.checked = isChecked;
    elements.agreePrivacy.checked = isChecked;
    elements.agreeLocation.checked = isChecked;
    elements.agreeMarketing.checked = isChecked;
    updateNextButton(elements);
  });

  // 개별 체크박스 처리
  [elements.agreeTerms, elements.agreePrivacy, elements.agreeLocation, elements.agreeMarketing].forEach(checkbox => {
    checkbox.addEventListener('change', () => {
      // 전체 동의 상태 업데이트
      const allRequired = elements.agreeTerms.checked && elements.agreePrivacy.checked && elements.agreeLocation.checked;
      const allChecked = allRequired && elements.agreeMarketing.checked;
      elements.agreeAll.checked = allChecked;

      updateNextButton(elements);
    });
  });
};

// ===== 모달 초기화 =====
const initModals = () => {
  const elements = initializeElements();

  // 약관 모달 닫기
  elements.modalClose.addEventListener('click', () => {
    hideAgreementModal();
  });

  // 약관 모달 확인 버튼
  elements.modalConfirm.addEventListener('click', () => {
    if (currentAgreementType) {
      // 해당 약관 체크박스 체크
      const checkboxId = `agree${currentAgreementType.charAt(0).toUpperCase() + currentAgreementType.slice(1)}`;
      const checkbox = document.getElementById(checkboxId);
      if (checkbox) {
        checkbox.checked = true;

        // 전체 동의 상태 업데이트
        updateAgreements();
        updateNextButton(elements);
      }
    }
    hideAgreementModal();
  });

  // 약관 동의 요청 모달 확인
  elements.agreementRequestConfirm.addEventListener('click', () => {
    hideAgreementRequestModal();
  });

  // 모달 배경 클릭 시 닫기
  elements.agreementModal.addEventListener('click', (e) => {
    if (e.target === elements.agreementModal) {
      hideAgreementModal();
    }
  });

  elements.agreementRequestModal.addEventListener('click', (e) => {
    if (e.target === elements.agreementRequestModal) {
      hideAgreementRequestModal();
    }
  });
};

// ===== 다음 버튼 상태 업데이트 =====
const updateNextButton = (elements) => {
  const requiredChecked = elements.agreeTerms.checked &&
                         elements.agreePrivacy.checked &&
                         elements.agreeLocation.checked;

  elements.nextButton.disabled = !requiredChecked;
};

// ===== 전체 동의 상태 업데이트 =====
const updateAgreements = () => {
  const elements = initializeElements();
  const allRequired = elements.agreeTerms.checked && elements.agreePrivacy.checked && elements.agreeLocation.checked;
  const allChecked = allRequired && elements.agreeMarketing.checked;
  elements.agreeAll.checked = allChecked;
};

// ===== 약관 모달 표시 =====
const showAgreementModal = (agreementType) => {
  const elements = initializeElements();
  const agreement = agreementContents[agreementType];

  if (!agreement) {
    console.error('❌ 약관 내용을 찾을 수 없습니다:', agreementType);
    return;
  }

  currentAgreementType = agreementType;
  isScrolledToBottom = false;

  // 모달 내용 설정
  elements.modalTitle.textContent = agreement.title;
  elements.modalBody.innerHTML = `<div class="agreement-content">${agreement.content}</div>`;

  // 확인 버튼 비활성화
  elements.modalConfirm.disabled = true;

  // 스크롤 이벤트 리스너 추가
  elements.modalBody.addEventListener('scroll', handleModalScroll);

  // 모달 표시
  elements.agreementModal.style.display = 'flex';
  setTimeout(() => {
    elements.agreementModal.classList.add('show');
  }, 10);

  console.log(`📄 약관 모달 표시: ${agreement.title}`);
};

// ===== 약관 모달 숨김 =====
const hideAgreementModal = () => {
  const elements = initializeElements();

  elements.agreementModal.classList.remove('show');
  setTimeout(() => {
    elements.agreementModal.style.display = 'none';
    currentAgreementType = null;
    isScrolledToBottom = false;
  }, 300);
};

// ===== 약관 동의 요청 모달 표시 =====
const showAgreementRequestModal = () => {
  const elements = initializeElements();

  elements.agreementRequestModal.style.display = 'flex';
  setTimeout(() => {
    elements.agreementRequestModal.classList.add('show');
  }, 10);
};

// ===== 약관 동의 요청 모달 숨김 =====
const hideAgreementRequestModal = () => {
  const elements = initializeElements();

  elements.agreementRequestModal.classList.remove('show');
  setTimeout(() => {
    elements.agreementRequestModal.style.display = 'none';
  }, 300);
};

// ===== 모달 스크롤 처리 =====
const handleModalScroll = () => {
  const elements = initializeElements();
  const modalBody = elements.modalBody;

  // 스크롤이 끝까지 내려갔는지 확인
  const isAtBottom = modalBody.scrollTop + modalBody.clientHeight >= modalBody.scrollHeight - 5;

  if (isAtBottom && !isScrolledToBottom) {
    isScrolledToBottom = true;
    elements.modalConfirm.disabled = false;
    console.log('📜 약관을 끝까지 읽었습니다. 확인 버튼 활성화');
  }
};

// ===== 다음 버튼 처리 =====
const handleNext = (elements) => {
  // 필수 약관 동의 확인
  const requiredChecked = elements.agreeTerms.checked &&
                         elements.agreePrivacy.checked &&
                         elements.agreeLocation.checked;

  if (!requiredChecked) {
    console.log('⚠️ 필수 약관 미동의');
    showAgreementRequestModal();
    return;
  }

  console.log('🚀 다음 단계로 이동');

  // 동의 정보를 세션 또는 로컬스토리지에 저장
  const agreementData = {
    terms: elements.agreeTerms.checked,
    privacy: elements.agreePrivacy.checked,
    location: elements.agreeLocation.checked,
    marketing: elements.agreeMarketing.checked,
    timestamp: new Date().toISOString()
  };

  try {
    localStorage.setItem('agreementData', JSON.stringify(agreementData));
    console.log('✅ 약관 동의 정보 저장 완료');
  } catch (error) {
    console.error('❌ 약관 동의 정보 저장 실패:', error);
  }

  // 다음 페이지로 이동 (핸드폰 번호 인증)
  const nextUrl = window.PAGE_CONFIG?.nextUrl || '/mobile/register/phone';
  window.location.href = nextUrl;
};

// ===== 페이지 로드 시 초기화 =====
document.addEventListener('DOMContentLoaded', initRegisterPage);

// ===== 페이지 가시성 변경 시 처리 =====
document.addEventListener('visibilitychange', () => {
  if (!document.hidden && checkLoginStatus()) {
    console.log('👁️ 페이지 포커스 시 로그인 상태 감지');
    window.location.href = '/';
  }
});

// ===== 키보드 이벤트 처리 =====
document.addEventListener('keydown', (e) => {
  // ESC 키로 모달 닫기
  if (e.key === 'Escape') {
    const agreementModal = document.getElementById('agreementModal');
    const requestModal = document.getElementById('agreementRequestModal');

    if (agreementModal && agreementModal.classList.contains('show')) {
      hideAgreementModal();
    } else if (requestModal && requestModal.classList.contains('show')) {
      hideAgreementRequestModal();
    }
  }
});

// ===== 디버그 함수 (개발용) =====
if (window.location.search.includes('debug=true')) {
  window.registerDebug = {
    getAgreementData: () => {
      const elements = initializeElements();
      return {
        terms: elements?.agreeTerms?.checked,
        privacy: elements?.agreePrivacy?.checked,
        location: elements?.agreeLocation?.checked,
        marketing: elements?.agreeMarketing?.checked,
        all: elements?.agreeAll?.checked
      };
    },
    showModal: (type) => {
      showAgreementModal(type);
    },
    testScrollToBottom: () => {
      isScrolledToBottom = true;
      const elements = initializeElements();
      elements.modalConfirm.disabled = false;
    }
  };

  console.log('🔧 디버그 모드 활성화 - window.registerDebug 사용 가능');
}