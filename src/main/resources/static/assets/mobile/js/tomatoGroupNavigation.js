// tomatoNavigation.js - 토마토 네비게이션 최종 완성본

/**
 * 🍅 토마토 네비게이션 시스템
 * - 서버 API를 통한 안전한 데이터 로드
 * - 기존 앱바 완전 대체
 * - PC/모바일 반응형 지원
 * - 토마토 원본 디자인 100% 재현
 */

// 토마토 상태 관리
let tomatoState = {
  isInitialized: false,
  isLoading: false,
  apps: [],
  currentDevice: 'mobile',
  userOrder: null,
  isExpanded: false
};

// 설정
const TOMATO_CONFIG = {
  // 우리 서버 API 사용
  apiUrl: '/api/tomato/group',
  statusUrl: '/api/tomato/group/status',
  cacheUrl: '/api/tomato/group/cache',
  
  // 스타일 설정
  pc: {
    sidebarWidth: '73px',
    position: 'right',
    zIndex: 1100,
    iconSize: '32px'
  },
  mobile: {
    height: '70px',
    position: 'bottom',
    itemWidth: '65px',
    maxVisible: 6
  },
  
  // 암호화 설정
  encryption: {
    key: 'tomatogroup_pass',
    algorithm: 'base64' // 실제로는 AES 사용 권장
  }
};

/**
 * 토마토 네비게이션 초기화
 */
async function initializeTomatoNavigation() {
  console.log('🍅 토마토 네비게이션 초기화 시작');
  
  if (tomatoState.isInitialized) {
    console.warn('⚠️ 토마토 네비게이션이 이미 초기화되었습니다.');
    return;
  }
  
  try {
    tomatoState.isLoading = true;
    
    // 1. 기존 앱바 제거
    removeExistingAppNavigation();
    
    // 2. 디바이스 타입 감지
    detectDeviceType();
    
    // 3. 토마토 데이터 로드
    await loadTomatoData();
    
    // 4. UI 렌더링
    renderTomatoNavigation();
    
    // 5. 이벤트 바인딩
    bindTomatoEvents();
    
    // 6. 사용자 설정 로드
    loadUserSettings();
    
    tomatoState.isInitialized = true;
    tomatoState.isLoading = false;
    
    console.log('✅ 토마토 네비게이션 초기화 완료');
    
    // 성공 알림
    if (window.showToast) {
      window.showToast('토마토 그룹 네비게이션 로드 완료', 'success', 2000);
    }
    
  } catch (error) {
    console.error('❌ 토마토 네비게이션 초기화 실패:', error);
    tomatoState.isLoading = false;
    
    // 오류 알림
    if (window.showToast) {
      window.showToast('토마토 네비게이션 로드 실패', 'error', 3000);
    }
  }
}

/**
 * 기존 앱 네비게이션 완전 제거
 */
function removeExistingAppNavigation() {
  console.log('🗑️ 기존 앱 네비게이션 제거');
  
  const selectorsToRemove = [
    '.mobile-app-nav',
    '#mobileAppNav',
    '.app-nav-container',
    '.app-nav-scroll',
    '.app-nav-item:not(.tomato-app)',
    '.app-nav-loading',
    '.app-nav-error'
  ];
  
  selectorsToRemove.forEach(selector => {
    const elements = document.querySelectorAll(selector);
    elements.forEach(el => {
      el.remove();
      console.log(`✅ 제거 완료: ${selector}`);
    });
  });
  
  // 레이아웃 조정 (하단 네비게이션 제거로 인한)
  adjustLayoutForTomatoOnly();
}

/**
 * 레이아웃 조정
 */
function adjustLayoutForTomatoOnly() {
  const layout = document.querySelector('.mobile-layout');
  const main = document.querySelector('.mobile-main');
  const container = document.querySelector('.container');
  
  if (layout) {
    layout.style.paddingBottom = '30px';
    layout.style.marginBottom = '0';
  }
  
  if (main) {
    main.style.paddingBottom = '40px';
  }
  
  if (container) {
    container.style.paddingBottom = '50px';
    const lastChild = container.lastElementChild;
    if (lastChild) {
      lastChild.style.marginBottom = '60px';
    }
  }
  
  console.log('✅ 토마토 전용 레이아웃 조정 완료');
}

/**
 * 디바이스 타입 감지
 */
function detectDeviceType() {
  const width = window.innerWidth;
  
  if (width >= 1024) {
    tomatoState.currentDevice = 'desktop';
  } else if (width >= 768) {
    tomatoState.currentDevice = 'tablet';
  } else {
    tomatoState.currentDevice = 'mobile';
  }
  
  document.body.setAttribute('data-tomato-device', tomatoState.currentDevice);
  console.log('📱 디바이스 타입:', tomatoState.currentDevice);
}

/**
 * 토마토 데이터 로드 (서버 API 사용)
 */
async function loadTomatoData() {
  console.log('📡 토마토 데이터 로드 시작 (서버 API)');
  
  try {
    const response = await fetch(TOMATO_CONFIG.apiUrl, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'Cache-Control': 'no-cache'
      }
    });
    
    if (!response.ok) {
      throw new Error(`서버 응답 오류: ${response.status} ${response.statusText}`);
    }
    
    const result = await response.json();
    console.log('서버 응답:', result);
    
    if (!result.success) {
      throw new Error(result.error?.message || '서버에서 오류 응답');
    }
    
    // 서버에서 받은 데이터 처리 (문자열 또는 객체 모두 처리)
    let tomatoData;
    if (typeof result.data === 'string') {
      try {
        tomatoData = JSON.parse(result.data);
      } catch (parseError) {
        console.error('❌ JSON 파싱 오류:', parseError);
        throw new Error('서버 데이터 파싱 실패');
      }
    } else {
      tomatoData = result.data;
    }
    
    console.log('파싱된 토마토 데이터:', tomatoData);
    
    if (!tomatoData || !tomatoData.tomatogroup || !Array.isArray(tomatoData.tomatogroup)) {
      throw new Error('토마토 데이터 형식이 올바르지 않습니다');
    }
    
    if (tomatoData.tomatogroup.length === 0) {
      throw new Error('토마토 앱 목록이 비어있습니다');
    }
    
    tomatoState.apps = tomatoData.tomatogroup;
    console.log(`✅ 토마토 데이터 로드 성공: ${tomatoState.apps.length}개 앱`);
    
    // 각 앱 정보 로그
    tomatoState.apps.forEach((app, index) => {
      console.log(`  ${index + 1}. ${app.nameK} (${app.nameE}) - ID: ${app.id}`);
    });
    
  } catch (error) {
    console.error('❌ 토마토 데이터 로드 실패:', error);
    
    // 폴백 데이터 사용
    tomatoState.apps = getFallbackTomatoData();
    console.log(`🔄 폴백 데이터 사용: ${tomatoState.apps.length}개 앱`);
    
    // 오류를 다시 던지지 않고 폴백으로 계속 진행
    if (window.showToast) {
      window.showToast('토마토 서버 데이터 로드 실패, 기본 데이터 사용', 'warning', 3000);
    }
  }
}

/**
 * 폴백 토마토 데이터 (실제 토마토 데이터와 동일)
 */
function getFallbackTomatoData() {
  return [
    {
      id: 0,
      nameK: "이토마토",
      nameE: "etomato", 
      url: "https://www.etomato.com/home/SideBarBridge.aspx",
      image: "https://tomato.etomato.com/images/etomato.png"
    },
    {
      id: 1,
      nameK: "증권통",
      nameE: "stocktong",
      url: "https://www.stocktong.co.kr/Web/SideBarBridge.aspx", 
      image: "https://tomato.etomato.com/images/stocktong.png"
    },
    {
      id: 2,
      nameK: "뉴스통",
      nameE: "newstong",
      url: "https://www.newstong.co.kr/content/auto_Login.aspx",
      image: "https://tomato.etomato.com/images/newstong.png"
    },
    {
      id: 3,
      nameK: "뉴스토마토", 
      nameE: "newstomato",
      url: "https://www.newstomato.com/member/auto_login.aspx",
      image: "https://tomato.etomato.com/images/newstomato.png"
    },
    {
      id: 4,
      nameK: "토마토증권통",
      nameE: "tomatotv",
      url: "https://www.tomatostocktong.com/Account/AutoLogin",
      image: "https://tomato.etomato.com/images/tomatotv.jpg"
    },
    {
      id: 5,
      nameK: "토마토투자자문",
      nameE: "tomatoasset",
      url: "https://www.tomatoasset.com/sideBarBridge.aspx",
      image: "https://tomato.etomato.com/images/tomatoasset.png"
    },
    {
      id: 6,
      nameK: "IB토마토",
      nameE: "ibtomato",
      url: "https://www.ibtomato.com/Member/AutoLogin.aspx",
      image: "https://tomato.etomato.com/images/ibtomato.png"
    },
    {
      id: 7,
      nameK: "통통몰",
      nameE: "tongtongmall",
      url: "https://www.tongtongmall.net",
      image: "https://tomato.etomato.com/images/tongtongmall.png"
    },
    {
      id: 8,
      nameK: "통통마켓",
      nameE: "tongtongmarket",
      url: "https://www.tongtongmarket.com",
      image: "https://tomato.etomato.com/images/tongtongmarket.png"
    },
    {
      id: 9,
      nameK: "통통체인",
      nameE: "tongtonchain",
      url: "https://tongtongchain.io",
      image: "https://tomato.etomato.com/images/tongtongchain.png"
    },
    {
      id: 10,
      nameK: "집통",
      nameE: "jiptong",
      url: "http://m.jiptong.com/?autologin.aspx",
      image: "https://tomato.etomato.com/images/jiptong.png"
    },
    {
      id: 11,
      nameK: "스탁론",
      nameE: "stockloan",
      url: "https://loan.tomato.co.kr/SideBarBridge.aspx",
      image: "https://tomato.etomato.com/images/stockloan.png"
    },
    {
      id: 12,
      nameK: "통통사인",
      nameE: "tongtonsign",
      url: "https://tongtongsign.com",
      image: "https://tomato.etomato.com/images/tongtongsign.png"
    },
    {
      id: 13,
      nameK: "통통코인",
      nameE: "tongtoncoin",
      url: "https://ttcoin.io",
      image: "https://tomato.etomato.com/images/tongtongcoin.png"
    },
    {
      id: 14,
      nameK: "통통지갑",
      nameE: "tongtongwallet",
      url: "https://tongtongwallet.com",
      image: "https://tomato.etomato.com/images/tongtongwallet.png"
    },
    {
      id: 15,
      nameK: "통통브랜드",
      nameE: "tongtong",
      url: "https://tongtongmessenger.com/maintongtongbrand.aspx",
      image: "https://tomato.etomato.com/images/tongtong_brand.png"
    },
    {
      id: 16,
      nameK: "토마토페이",
      nameE: "tomatopay",
      url: "https://tomatopay.net",
      image: "https://tomato.etomato.com/images/tomatopay.png"
    },
    {
      id: 17,
      nameK: "토마토체인",
      nameE: "tomatochain",
      url: "https://tomatochain.net",
      image: "https://tomato.etomato.com/images/tomatochain.png"
    },
    {
      id: 18,
      nameK: "티켓통",
      nameE: "tomatoclassic",
      url: "https://www.tickettong.net/GroupLogin",
      image: "https://tomato.etomato.com/images/classic.png"
    },
    {
      id: 19,
      nameK: "토마토패스",
      nameE: "tomatoclassic",
      url: "https://www.tomatopass.com/account/oneIdAutoLogin.do",
      image: "https://tomato.etomato.com/images/tomatopass.jpg"
    },
    {
      id: 20,
      nameK: "차통",
      nameE: "chatong",
      url: "https://chatong.kr/intro_user",
      image: "https://tomato.etomato.com/images/chartong.png"
    },
    {
      id: 21,
      nameK: "서치통",
      nameE: "searchtong",
      url: "https://m.searchtong.com/Set/SignIn/Auto_Login.aspx",
      image: "https://tomato.etomato.com/images/searchtong.png"
    },
    {
      id: 22,
      nameK: "우리아이재단",
      nameE: "ourchildren",
      url: "https://ourchildren.or.kr/w_web.php",
      image: "https://tomato.etomato.com/images/ourchildren.jpg"
    }
  ];
}

/**
 * 토마토 네비게이션 렌더링
 */
function renderTomatoNavigation() {
  console.log('🎨 토마토 네비게이션 렌더링');
  
  // 기존 토마토 요소 제거
  removePreviousTomatoElements();
  
  if (tomatoState.currentDevice === 'desktop') {
    renderDesktopTomatoNavigation();
  } else {
    renderMobileTomatoNavigation();
  }
}

/**
 * 기존 토마토 요소 제거
 */
function removePreviousTomatoElements() {
  const selectors = [
    '.tomato-navigation',
    '.tomato-sidebar', 
    '.tomato-mobile-bar',
    '#tomatoSidebar',
    '#tomatoMobileBar'
  ];
  
  selectors.forEach(selector => {
    const elements = document.querySelectorAll(selector);
    elements.forEach(el => el.remove());
  });
}

/**
 * PC용 토마토 네비게이션 렌더링 (세로 스크롤 스타일)
 */
function renderDesktopTomatoNavigation() {
  console.log('🖥️ PC용 토마토 네비게이션 렌더링 (세로 스크롤)');
  
  const sidebar = document.createElement('div');
  sidebar.id = 'tomatoSidebar';
  sidebar.className = 'tomato-sidebar';
  
  sidebar.innerHTML = `
    <div class="tomato-scroll-container">
      <button class="tomato-scroll-btn tomato-up-btn" id="tomatoUpBtn">
        <i class="fas fa-chevron-up"></i>
      </button>
      <div class="tomato-apps-scroll" id="tomatoAppsScroll">
        ${renderTomatoApps()}
      </div>
      <button class="tomato-scroll-btn tomato-down-btn" id="tomatoDownBtn">
        <i class="fas fa-chevron-down"></i>
      </button>
    </div>
  `;
  
  document.body.appendChild(sidebar);
  addTomatoDesktopStyles();
  initializeTomatoVerticalScroll();
}

/**
 * 모바일용 토마토 네비게이션 렌더링 (Swiper 스타일)
 */
function renderMobileTomatoNavigation() {
  console.log('📱 모바일용 토마토 네비게이션 렌더링 (Swiper 스타일)');
  
  const mobileBar = document.createElement('div');
  mobileBar.id = 'tomatoMobileBar';
  mobileBar.className = 'tomato-mobile-bar';
  
  mobileBar.innerHTML = `
    <div class="tomato-swiper-container">
      <button class="tomato-nav-btn tomato-prev-btn" id="tomatoPrevBtn">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="tomato-mobile-scroll" id="tomatoMobileScroll">
        ${renderTomatoApps()}
      </div>
      <button class="tomato-nav-btn tomato-next-btn" id="tomatoNextBtn">
        <i class="fas fa-chevron-right"></i>
      </button>
    </div>
  `;
  
  document.body.appendChild(mobileBar);
  addTomatoMobileStyles();
  initializeTomatoSwiper();
}

/**
 * 토마토 앱들 렌더링 (더보기 버튼 제거)
 */
function renderTomatoApps() {
  const apps = getSortedTomatoApps();
  
  return apps.map(app => `
    <a href="#" class="tomato-app" 
       data-tomato-id="${app.id}"
       onclick="handleTomatoAppClick(${app.id}); return false;"
       title="${app.nameK}">
      <img src="${app.image}" alt="${app.nameK}" 
           onerror="this.src='${getDefaultTomatoIcon()}'">
      ${tomatoState.currentDevice !== 'desktop' ? `<span>${app.nameK}</span>` : ''}
    </a>
  `).join('');
}

/**
 * PC 세로 스크롤 초기화
 */
function initializeTomatoVerticalScroll() {
  const scrollContainer = document.getElementById('tomatoAppsScroll');
  const upBtn = document.getElementById('tomatoUpBtn');
  const downBtn = document.getElementById('tomatoDownBtn');
  
  if (!scrollContainer || !upBtn || !downBtn) {
    console.error('❌ 세로 스크롤 요소를 찾을 수 없습니다');
    return;
  }
  
  // 스크롤 버튼 상태 업데이트
  function updateScrollButtons() {
    const isScrollable = scrollContainer.scrollHeight > scrollContainer.clientHeight;
    const isAtTop = scrollContainer.scrollTop <= 5;
    const isAtBottom = scrollContainer.scrollTop >= scrollContainer.scrollHeight - scrollContainer.clientHeight - 5;
    
    upBtn.style.opacity = (isScrollable && !isAtTop) ? '1' : '0.3';
    downBtn.style.opacity = (isScrollable && !isAtBottom) ? '1' : '0.3';
    upBtn.disabled = !isScrollable || isAtTop;
    downBtn.disabled = !isScrollable || isAtBottom;
  }
  
  // 부드러운 세로 스크롤
  function smoothVerticalScroll(direction) {
    const itemHeight = 60; // 아이템 높이 + 간격
    const visibleItems = Math.floor(scrollContainer.clientHeight / itemHeight);
    const scrollAmount = itemHeight * Math.max(1, visibleItems - 1);
    
    const currentScroll = scrollContainer.scrollTop;
    const targetScroll = direction === 'down' 
      ? currentScroll + scrollAmount 
      : currentScroll - scrollAmount;
    
    scrollContainer.scrollTo({
      top: Math.max(0, targetScroll),
      behavior: 'smooth'
    });
  }
  
  // 이벤트 바인딩
  upBtn.addEventListener('click', (e) => {
    e.preventDefault();
    smoothVerticalScroll('up');
  });
  
  downBtn.addEventListener('click', (e) => {
    e.preventDefault();
    smoothVerticalScroll('down');
  });
  
  // 스크롤 이벤트
  scrollContainer.addEventListener('scroll', updateScrollButtons);
  
  // 초기 상태 설정
  setTimeout(updateScrollButtons, 100);
  
  // 윈도우 리사이즈 시 버튼 상태 업데이트
  window.addEventListener('resize', () => {
    setTimeout(updateScrollButtons, 300);
  });
  
  console.log('✅ 토마토 세로 스크롤 초기화 완료');
}

/**
 * 토마토 Swiper 초기화
 */
function initializeTomatoSwiper() {
  const scrollContainer = document.getElementById('tomatoMobileScroll');
  const prevBtn = document.getElementById('tomatoPrevBtn');
  const nextBtn = document.getElementById('tomatoNextBtn');
  
  if (!scrollContainer || !prevBtn || !nextBtn) {
    console.error('❌ Swiper 요소를 찾을 수 없습니다');
    return;
  }
  
  // 스크롤 가능 여부 확인
  function updateButtonStates() {
    const isScrollable = scrollContainer.scrollWidth > scrollContainer.clientWidth;
    const isAtStart = scrollContainer.scrollLeft <= 5;
    const isAtEnd = scrollContainer.scrollLeft >= scrollContainer.scrollWidth - scrollContainer.clientWidth - 5;
    
    prevBtn.style.opacity = (isScrollable && !isAtStart) ? '1' : '0.3';
    nextBtn.style.opacity = (isScrollable && !isAtEnd) ? '1' : '0.3';
    prevBtn.disabled = !isScrollable || isAtStart;
    nextBtn.disabled = !isScrollable || isAtEnd;
  }
  
  // 스크롤 애니메이션
  function smoothScroll(direction) {
    const itemWidth = 67; // 아이템 너비 + 간격
    const visibleItems = Math.floor(scrollContainer.clientWidth / itemWidth);
    const scrollAmount = itemWidth * Math.max(1, visibleItems - 1);
    
    const currentScroll = scrollContainer.scrollLeft;
    const targetScroll = direction === 'next' 
      ? currentScroll + scrollAmount 
      : currentScroll - scrollAmount;
    
    // 부드러운 스크롤
    scrollContainer.scrollTo({
      left: Math.max(0, targetScroll),
      behavior: 'smooth'
    });
  }
  
  // 이벤트 바인딩
  prevBtn.addEventListener('click', (e) => {
    e.preventDefault();
    smoothScroll('prev');
  });
  
  nextBtn.addEventListener('click', (e) => {
    e.preventDefault();
    smoothScroll('next');
  });
  
  // 스크롤 이벤트
  scrollContainer.addEventListener('scroll', updateButtonStates);
  
  // 초기 상태 설정
  setTimeout(updateButtonStates, 100);
  
  // 윈도우 리사이즈 시 버튼 상태 업데이트
  window.addEventListener('resize', () => {
    setTimeout(updateButtonStates, 300);
  });
  
  console.log('✅ 토마토 Swiper 초기화 완료');
}

/**
 * 정렬된 토마토 앱 가져오기
 */
function getSortedTomatoApps() {
  if (!tomatoState.userOrder) {
    return tomatoState.apps;
  }
  
  const orderArray = tomatoState.userOrder.split(',').map(id => parseInt(id));
  const sortedApps = [];
  const remainingApps = [...tomatoState.apps];
  
  // 사용자 순서대로 정렬
  orderArray.forEach(id => {
    const index = remainingApps.findIndex(app => app.id === id);
    if (index !== -1) {
      sortedApps.push(remainingApps.splice(index, 1)[0]);
    }
  });
  
  // 나머지 앱들 추가
  sortedApps.push(...remainingApps);
  
  return sortedApps;
}

/**
 * 토마토 앱 클릭 핸들러
 */
function handleTomatoAppClick(appId) {
  console.log('🍅 토마토 앱 클릭:', appId);
  
  const app = tomatoState.apps.find(a => a.id === appId);
  if (!app) {
    console.error('토마토 앱을 찾을 수 없습니다:', appId);
    return;
  }
  
  // 토스트 메시지
  if (window.showToast) {
    window.showToast(`${app.nameK}로 이동합니다.`, 'info', 2000);
  }
  
  // 암호화된 URL 생성 및 이동
  const encryptedUrl = generateTomatoUrl(app);
  window.open(encryptedUrl, '_blank', 'noopener,noreferrer');
}

/**
 * 암호화된 토마토 URL 생성
 */
function generateTomatoUrl(app) {
  try {
    const currentUser = getCurrentUser();
    const userOrder = tomatoState.userOrder || getDefaultOrder();
    const referrerNo = getCurrentReferrerNo();
    
    const params = {
      tomatoid: currentUser?.userKey || currentUser?.email || 'guest',
      tomatoGroupNo: userOrder,
      tomatoRefNo: referrerNo
    };
    
    const paramString = Object.entries(params)
      .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
      .join('&');
    
    // Base64 인코딩 (실제 환경에서는 AES 암호화)
    const encodedParams = btoa(unescape(encodeURIComponent(paramString)));
    
    return `${app.url}?tomatoEnc=${encodeURIComponent(encodedParams)}`;
    
  } catch (error) {
    console.error('토마토 URL 생성 실패:', error);
    return app.url;
  }
}

/**
 * 이벤트 바인딩
 */
function bindTomatoEvents() {
  // 윈도우 리사이즈 이벤트
  window.addEventListener('resize', debounce(() => {
    const prevDevice = tomatoState.currentDevice;
    detectDeviceType();
    
    if (prevDevice !== tomatoState.currentDevice) {
      renderTomatoNavigation();
      bindTomatoEvents();
    }
  }, 300));
}

/**
 * 사용자 설정 로드
 */
function loadUserSettings() {
  try {
    tomatoState.userOrder = localStorage.getItem('tomatoGroupOrder');
  } catch (error) {
    console.error('사용자 설정 로드 실패:', error);
  }
}

/**
 * 유틸리티 함수들
 */
function getCurrentUser() {
  return window.APP_CONFIG?.currentUser || 
         window.serverData?.currentUser || 
         null;
}

function getDefaultOrder() {
  return tomatoState.apps.map(app => app.id).join(',');
}

function getCurrentReferrerNo() {
  const currentHost = window.location.hostname;
  const cleanHost = currentHost.replace(/^(www\.|m\.|test\.)/, '');
  
  for (let i = 0; i < tomatoState.apps.length; i++) {
    const app = tomatoState.apps[i];
    if (app.url && app.url.includes(cleanHost)) {
      return i;
    }
  }
  return 0;
}

function getDefaultTomatoIcon() {
  return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzYiIGhlaWdodD0iMzYiIHZpZXdCb3g9IjAgMCAzNiAzNiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjM2IiBoZWlnaHQ9IjM2IiByeD0iOCIgZmlsbD0iI0ZGNkIzNSIvPgo8cGF0aCBkPSJNMTggMjZDMjIuNDE4MiAyNiAyNiAyMi40MTgyIDI2IDE4QzI2IDEzLjU4MTggMjIuNDE4MiAxMCAxOCAxMEMxMy41ODE4IDEwIDEwIDEzLjU4MTggMTAgMThDMTAgMjIuNDE4MiAxMy41ODE4IDI2IDE4IDI2WiIgZmlsbD0iI0ZGRkZGRiI+PC9wYXRoPgo8cGF0aCBkPSJNMTUuNSAxNS41SDE4VjIwLjVNMTggMTMuNUgxOC4wMDc1IiBzdHJva2U9IiNGRjZCMzUiIHN0cm9rZS13aWR0aD0iMS41IiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiLz4KPC9zdmc+Cg==';
}

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
 * 전역 함수들 (메뉴 컨트롤용)
 */
function toggleTomatoMenu() {
  const panel = document.getElementById('tomatoMenuPanel');
  if (panel) {
    tomatoState.isExpanded = !tomatoState.isExpanded;
    panel.classList.toggle('active', tomatoState.isExpanded);
  }
}

function closeTomatoMenu() {
  const panel = document.getElementById('tomatoMenuPanel');
  if (panel) {
    tomatoState.isExpanded = false;
    panel.classList.remove('active');
  }
}

function showTomatoModal() {
  // 모바일에서 전체 앱 목록 모달 표시
  if (window.showToast) {
    window.showToast('전체 토마토 앱 목록 기능 준비 중입니다.', 'info', 2000);
  }
}

/**
 * CSS 스타일 추가
 */
function addTomatoDesktopStyles() {
  if (document.getElementById('tomatoDesktopStyles')) return;
  
  const style = document.createElement('style');
  style.id = 'tomatoDesktopStyles';
  style.textContent = `
    .tomato-sidebar {
      position: fixed !important;
      top: 0 !important;
      right: 0 !important;
      width: ${TOMATO_CONFIG.pc.sidebarWidth} !important;
      height: 100vh !important;
      background: white !important;
      border-left: 1px solid #e0e0e0 !important;
      box-shadow: -2px 0 10px rgba(0,0,0,0.1) !important;
      z-index: ${TOMATO_CONFIG.pc.zIndex} !important;
      display: flex !important;
      flex-direction: column !important;
      padding: 8px 0 !important;
    }
    
    .tomato-scroll-container {
      display: flex !important;
      flex-direction: column !important;
      height: 100% !important;
      align-items: center !important;
    }
    
    .tomato-scroll-btn {
      background: #ff6b35 !important;
      border: none !important;
      color: white !important;
      width: 32px !important;
      height: 20px !important;
      border-radius: 10px !important;
      display: flex !important;
      align-items: center !important;
      justify-content: center !important;
      cursor: pointer !important;
      transition: all 0.3s ease !important;
      flex-shrink: 0 !important;
      font-size: 10px !important;
      margin: 3px 0 !important;
      box-shadow: 0 2px 6px rgba(255,107,53,0.3) !important;
    }
    
    .tomato-scroll-btn:hover:not(:disabled) {
      background: #e55a2b !important;
      transform: scale(1.05) !important;
    }
    
    .tomato-scroll-btn:disabled {
      cursor: not-allowed !important;
      transform: none !important;
    }
    
    .tomato-apps-scroll {
      flex: 1 !important;
      overflow-y: auto !important;
      overflow-x: hidden !important;
      padding: 6px 0 !important;
      margin: 3px 0 !important;
      width: 100% !important;
      display: flex !important;
      flex-direction: column !important;
      align-items: center !important;
      gap: 6px !important;
      scrollbar-width: none !important;
      -ms-overflow-style: none !important;
    }
    
    .tomato-apps-scroll::-webkit-scrollbar {
      display: none !important;
    }
    
    .tomato-app {
      display: flex !important;
      align-items: center !important;
      justify-content: center !important;
      width: 38px !important;
      height: 38px !important;
      border-radius: 6px !important;
      transition: all 0.3s ease !important;
      text-decoration: none !important;
      position: relative !important;
      flex-shrink: 0 !important;
      cursor: pointer !important;
    }
    
    .tomato-app:hover {
      background: rgba(255,107,53,0.1) !important;
      transform: scale(1.05) !important;
      text-decoration: none !important;
    }
    
    .tomato-app img {
      width: ${TOMATO_CONFIG.pc.iconSize} !important;
      height: ${TOMATO_CONFIG.pc.iconSize} !important;
      border-radius: 5px !important;
      object-fit: cover !important;
      transition: transform 0.2s ease !important;
    }
    
    .tomato-app:hover img {
      transform: scale(1.1) !important;
    }
    
    /* 툴팁 */
    .tomato-app::after {
      content: attr(title) !important;
      position: absolute !important;
      right: 100% !important;
      top: 50% !important;
      transform: translateY(-50%) !important;
      background: rgba(0,0,0,0.8) !important;
      color: white !important;
      padding: 6px 10px !important;
      border-radius: 6px !important;
      font-size: 12px !important;
      white-space: nowrap !important;
      opacity: 0 !important;
      visibility: hidden !important;
      transition: all 0.3s ease !important;
      z-index: 1000 !important;
      margin-right: 8px !important;
      pointer-events: none !important;
    }
    
    .tomato-app:hover::after {
      opacity: 1 !important;
      visibility: visible !important;
    }
    
    /* 큰 데스크톱 화면 */
    @media (min-width: 1400px) {
      .tomato-sidebar {
        width: 55px !important;
      }
      
      .tomato-app {
        width: 46px !important;
        height: 46px !important;
      }
      
      .tomato-app img {
        width: 32px !important;
        height: 32px !important;
      }
      
      .tomato-scroll-btn {
        width: 38px !important;
        height: 24px !important;
        font-size: 12px !important;
      }
    }
    
    /* 작은 데스크톱 화면 */
    @media (min-width: 1024px) and (max-width: 1399px) {
      .tomato-sidebar {
        width: 42px !important;
      }
      
      .tomato-app {
        width: 36px !important;
        height: 36px !important;
      }
      
      .tomato-app img {
        width: 26px !important;
        height: 26px !important;
      }
      
      .tomato-scroll-btn {
        width: 30px !important;
        height: 18px !important;
        font-size: 9px !important;
      }
    }
  `;
  document.head.appendChild(style);
}

function addTomatoMobileStyles() {
  if (document.getElementById('tomatoMobileStyles')) return;
  
  const style = document.createElement('style');
  style.id = 'tomatoMobileStyles';
  style.textContent = `
    .tomato-mobile-bar {
      position: fixed !important;
      bottom: 0 !important;
      left: 0 !important;
      right: 0 !important;
      height: ${TOMATO_CONFIG.mobile.height} !important;
      background: white !important;
      border-top: 1px solid #e0e0e0 !important;
      box-shadow: 0 -2px 10px rgba(0,0,0,0.1) !important;
      z-index: 1020 !important;
      padding: 8px 0 !important;
    }
    
    .tomato-swiper-container {
      display: flex !important;
      align-items: center !important;
      height: 100% !important;
      max-width: 375px !important;
      margin: 0 auto !important;
      padding: 0 8px !important;
    }
    
    .tomato-nav-btn {
      background: #ff6b35 !important;
      border: none !important;
      color: white !important;
      width: 28px !important;
      height: 28px !important;
      border-radius: 50% !important;
      display: flex !important;
      align-items: center !important;
      justify-content: center !important;
      cursor: pointer !important;
      transition: all 0.3s ease !important;
      flex-shrink: 0 !important;
      z-index: 10 !important;
      font-size: 12px !important;
      box-shadow: 0 2px 6px rgba(255,107,53,0.3) !important;
    }
    
    .tomato-nav-btn:hover:not(:disabled) {
      background: #e55a2b !important;
      transform: scale(1.1) !important;
    }
    
    .tomato-nav-btn:disabled {
      cursor: not-allowed !important;
      transform: none !important;
    }
    
    .tomato-prev-btn {
      margin-right: 8px !important;
    }
    
    .tomato-next-btn {
      margin-left: 8px !important;
    }
    
    .tomato-mobile-scroll {
      display: flex !important;
      gap: 12px !important;
      padding: 0 4px !important;
      overflow-x: auto !important;
      scrollbar-width: none !important;
      -ms-overflow-style: none !important;
      flex: 1 !important;
      scroll-behavior: smooth !important;
    }
    
    .tomato-mobile-scroll::-webkit-scrollbar {
      display: none !important;
    }
    
    .tomato-app {
      display: flex !important;
      flex-direction: column !important;
      align-items: center !important;
      min-width: ${TOMATO_CONFIG.mobile.itemWidth} !important;
      text-decoration: none !important;
      color: #666 !important;
      transition: all 0.3s ease !important;
      padding: 2px !important;
      border-radius: 6px !important;
      flex-shrink: 0 !important;
    }
    
    .tomato-app:hover {
      color: #ff6b35 !important;
      background: rgba(255,107,53,0.1) !important;
      transform: translateY(-1px) !important;
      text-decoration: none !important;
    }
    
    .tomato-app img {
      width: 30px !important;
      height: 30px !important;
      border-radius: 6px !important;
      margin-bottom: 2px !important;
      object-fit: cover !important;
      transition: transform 0.2s ease !important;
    }
    
    .tomato-app:hover img {
      transform: scale(1.05) !important;
    }
    
    .tomato-app span {
      font-size: 9px !important;
      font-weight: 500 !important;
      text-align: center !important;
      line-height: 1.1 !important;
      max-width: ${TOMATO_CONFIG.mobile.itemWidth} !important;
      overflow: hidden !important;
      text-overflow: ellipsis !important;
      white-space: nowrap !important;
      margin-top: 1px !important;
    }
    
    /* 더 작은 모바일 화면 */
    @media (max-width: 375px) {
      .tomato-swiper-container {
        padding: 0 4px !important;
      }
      
      .tomato-nav-btn {
        width: 24px !important;
        height: 24px !important;
        font-size: 10px !important;
      }
      
      .tomato-mobile-scroll {
        gap: 8px !important;
      }
      
      .tomato-app {
        min-width: 48px !important;
      }
      
      .tomato-app img {
        width: 26px !important;
        height: 26px !important;
      }
      
      .tomato-app span {
        font-size: 8px !important;
      }
    }
    
    /* 태블릿 */
    @media (min-width: 768px) and (max-width: 1023px) {
      .tomato-swiper-container {
        max-width: 768px !important;
        padding: 0 16px !important;
      }
      
      .tomato-nav-btn {
        width: 32px !important;
        height: 32px !important;
        font-size: 14px !important;
      }
      
      .tomato-app img {
        width: 34px !important;
        height: 34px !important;
      }
      
      .tomato-app span {
        font-size: 10px !important;
      }
    }
  `;
  document.head.appendChild(style);
}

// 전역 함수 등록
window.handleTomatoAppClick = handleTomatoAppClick;
window.toggleTomatoMenu = toggleTomatoMenu;
window.closeTomatoMenu = closeTomatoMenu;
window.showTomatoModal = showTomatoModal;
window.initializeTomatoNavigation = initializeTomatoNavigation;

// 디버깅용 함수들
window.debugTomato = async function() {
  console.group('🍅 토마토 네비게이션 디버그');
  
  console.log('상태:', tomatoState);
  console.log('앱 개수:', tomatoState.apps.length);
  console.log('디바이스:', tomatoState.currentDevice);
  
  // API 상태 확인
  try {
    const response = await fetch('/api/tomato/group/status');
    const status = await response.json();
    console.log('API 상태:', status);
  } catch (error) {
    console.error('API 상태 확인 실패:', error);
  }
  
  // DOM 요소 확인
  console.log('DOM 요소들:', {
    sidebar: !!document.getElementById('tomatoSidebar'),
    mobileBar: !!document.getElementById('tomatoMobileBar'),
    apps: document.querySelectorAll('.tomato-app').length
  });
  
  console.groupEnd();
};

window.reloadTomato = function() {
  console.log('🔄 토마토 네비게이션 재로드');
  tomatoState.isInitialized = false;
  initializeTomatoNavigation();
};

window.clearTomatoCache = async function() {
  try {
    const response = await fetch('/api/tomato/group/cache', {
      method: 'DELETE'
    });
    const result = await response.json();
    console.log('🗑️ 캐시 정리 결과:', result);
    window.reloadTomato();
  } catch (error) {
    console.error('캐시 정리 실패:', error);
  }
};

// DOM 로드 후 자동 초기화
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    setTimeout(initializeTomatoNavigation, 1000);
  });
} else {
  setTimeout(initializeTomatoNavigation, 1000);
}

console.log('🍅 토마토 네비게이션 스크립트 로드 완료 (서버 API 버전)');