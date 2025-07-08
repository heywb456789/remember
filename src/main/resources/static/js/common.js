/**
 * 공통 기능 자바스크립트 파일
 * 사이드바, 헤더, 탭 메뉴 등 공통 요소에 대한 이벤트와 기능
 * 공통 컴포넌트 로드 및 관리
 */

// 페이지 로드 완료 시 실행
import {authFetch, optionalAuthFetch} from "/js/commonFetch.js";

document.addEventListener('DOMContentLoaded', async () => {
  const loaded = await loadCommonComponents();
  initSideMenu();
  initTabMenu();
  setActiveItems();

  // --- 헤더 검색 토글 & 실행 ---
  const searchBtn = document.querySelector('.search-button');
  const panel = document.querySelector('.header-search-panel');
  const submit = document.getElementById('header-search-submit');
  const input = document.getElementById('header-search-input');
  const selSec = document.getElementById('filter-section');
  const selFld = document.getElementById('filter-field');
  const searchIcon = searchBtn.querySelector('i');
  const myPageBtn = document.querySelector('.profile-button');

  // 패널 토글
  searchBtn.addEventListener('click', () => {
    const isOpen = panel.style.display !== 'flex';
    panel.style.display = isOpen ? 'flex' : 'none';
    // 아이콘 토글
    searchIcon.classList.toggle('fa-search', !isOpen);
    searchIcon.classList.toggle('fa-times', isOpen);
    if (isOpen) {
      input.focus();
    }
  });

  //마이페이지 클릭함수
  myPageBtn.addEventListener('click', () => {
    window.location.href = '/mypage/mypage.html'
  })

  // 검색 실행 함수
  function doHeaderSearch() {
    const section = selSec.value;   // all | original | board | vote
    const field = selFld.value;   // all | title | content | author
    const keyword = input.value.trim();
    if (!keyword) {
      return;
    }

    // 매핑 테이블
    const mapCategory = {
      all: 'ALL', video: 'ORIGINAL_VIDEO', shorts: 'ORIGINAL_SHORTS',
      article: 'ORIGINAL_NEWS', board: 'BOARD_POST', vote: 'VOTE_POST'
    };
    const mapSection = {
      all: 'ALL',
      title: 'TITLE',
      content: 'CONTENT',
      author: 'AUTHOR'
    };

    const params = new URLSearchParams({
      searchCategory: mapCategory[section] || 'ALL',
      searchSection: mapSection[field] || 'ALL',
      searchKeyword: keyword,
      page: '0',
      size: '10'
    });

    // 통합 검색 페이지로 리다이렉트
    window.location.href = `/main/search.html?${params.toString()}`;
  }

  submit.addEventListener('click', doHeaderSearch);
  input.addEventListener('keypress', e => {
    if (e.key === 'Enter') {
      doHeaderSearch();
    }
  });
});

/**
 * 공통 컴포넌트 로드 함수
 * 헤더, 사이드바, 탭 메뉴 등을 동적으로 삽입
 * @returns {Promise} 모든 컴포넌트 로드 완료 Promise
 */
async function loadCommonComponents() {
  try {
    // 사이드 메뉴 및 오버레이
    await loadComponent('side-menu-container', '/components/side-menu.html');

    // 헤더 (로고 및 아이콘)
    await loadComponent('header-container', '/components/header.html');

    // 탭 메뉴
    await loadComponent('tab-menu-container', '/components/tab-menu.html');

    // console.log('모든 공통 컴포넌트 로드 완료');
    return true;
  } catch (error) {
    console.error('공통 컴포넌트 로드 중 오류 발생:', error);

    // 오류 발생 시 기본 컴포넌트 직접 삽입
    injectDefaultComponents();
    return false;
  }
}

/**
 * 개별 컴포넌트 로드 함수
 * @param {string} containerId - 컴포넌트를 삽입할 컨테이너 ID
 * @param {string} componentUrl - 컴포넌트 HTML 파일 URL
 * @returns {Promise} 컴포넌트 로드 완료 Promise
 */
async function loadComponent(containerId, componentUrl) {
  return new Promise((resolve, reject) => {
    const container = document.getElementById(containerId);

    // 컨테이너가 없으면 무시
    if (!container) {
      console.warn(`${containerId} 컨테이너를 찾을 수 없습니다.`);
      resolve();
      return;
    }

    // 컴포넌트 HTML 가져오기
    fetch(componentUrl)
    .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP 오류: ${response.status}`);
      }
      return response.text();
    })
    .then(html => {
      // 컨테이너에 HTML 삽입
      container.innerHTML = html;
      resolve();
    })
    .catch(error => {
      console.error(`${componentUrl} 로드 중 오류:`, error);
      reject(error);
    });
  });
}

/**
 * 오류 시 기본 컴포넌트 삽입 함수
 * 컴포넌트 로드 실패 시 기본 HTML 삽입
 */
function injectDefaultComponents() {
  // 사이드 메뉴 & 오버레이 기본 HTML
  const sideMenuContainer = document.getElementById('side-menu-container');
  if (sideMenuContainer) {
    sideMenuContainer.innerHTML = `
      <div class="menu-overlay"></div>
      <div class="side-menu">
        <div class="side-menu-header">
          <div class="side-menu-logo">
            <img src="/images/logo.svg" alt="로고"/>
            <span>나라 걱정 클럽</span>
          </div>
        </div>
        <div class="side-menu-content">
          <div class="side-menu-item" data-page="home">클럽 홈</div>
          <div class="side-menu-item" data-page="rewards">리워드 정책</div>
          <div class="side-menu-item" data-page="link-x">계정인증(엑스)</div>
          <div class="side-menu-item" data-page="upload">활동내역 업로드</div>
<!--          <div class="side-menu-item" data-page="subscribe">정기 구독</div>-->
        </div>
      </div>
    `;
  }

  // 헤더 기본 HTML
  const headerContainer = document.getElementById('header-container');
  if (headerContainer) {
    headerContainer.innerHTML = `
      <div class="header">
        <button class="menu-button">
          <i class="fas fa-bars"></i>
        </button>
        <a href="/" class="logo">
          <img src="/images/logo.svg" alt="로고"/>
          <span>나라 걱정 클럽</span>
        </a>
        <div class="right-icons">
          <button class="search-button">
            <i class="fas fa-search"></i>
          </button>
          <button class="profile-button">
            <i class="fas fa-user"></i>
          </button>
        </div>
      </div>
    `;
  }

  // 탭 메뉴 기본 HTML
  const tabMenuContainer = document.getElementById('tab-menu-container');
  if (tabMenuContainer) {
    tabMenuContainer.innerHTML = `
      <div class="tab-menu">
        <div class="tab-item" data-page="home">클럽홈</div>
        <div class="tab-item" data-page="original">오리지널</div>
        <div class="tab-item" data-page="board">자유게시판</div>
        <div class="tab-item" data-page="vote">투표광장</div>
      </div>
    `;
  }
}

/**
 * 현재 페이지 URL에 따라 활성 탭/메뉴 설정
 */
function setActiveItems() {
  const currentPath = window.location.pathname;

  // 사이드 메뉴 활성화
  const sideMenuItems = document.querySelectorAll('.side-menu-item');
  sideMenuItems.forEach(item => {
    item.classList.remove('active');

    const page = item.getAttribute('data-page');
    if (matchPathToPage(currentPath, page)) {
      item.classList.add('active');
    }
  });

  // 탭 메뉴 활성화
  const tabItems = document.querySelectorAll('.tab-item');
  tabItems.forEach(item => {
    item.classList.remove('active');

    const page = item.getAttribute('data-page');
    if (matchPathToPage(currentPath, page)) {
      item.classList.add('active');
    }
  });
}

/**
 * 경로와 페이지 키 매칭 함수
 * @param {string} path - 현재 URL 경로
 * @param {string} page - 페이지 키
 * @returns {boolean} 매칭 여부
 */
function matchPathToPage(path, page) {
  const pathMap = {
    'home': ['/main/main.html'],
    'original': ['/original/original.html'],
    'board': ['/board/boardList.html'],
    'vote': ['/vote/voteList.html'],

    'rewards': ['/side/rewards.html'],
    'upload': ['/side/activity.html'],

    'write': ['/boardWrite.html'],
    'community': ['/boardList.html'],
    'exchange': ['/exchange.html']
  };

  return pathMap[page] && pathMap[page].some(p => path.endsWith(p));
}

/**
 * 사이드 메뉴 관련 기능 초기화
 */
function initSideMenu() {
  const menuButton = document.querySelector('.menu-button');
  const sideMenu = document.querySelector('.side-menu');
  const overlay = document.querySelector('.menu-overlay');

  // 햄버거 메뉴 클릭 이벤트
  if (menuButton && sideMenu && overlay) {
    menuButton.addEventListener('click', function () {
      sideMenu.classList.add('active');
      overlay.classList.add('active');
      document.body.style.overflow = 'hidden'; // 배경 스크롤 방지
    });

    // 오버레이 클릭 시 메뉴 닫기
    overlay.addEventListener('click', function () {
      sideMenu.classList.remove('active');
      overlay.classList.remove('active');
      document.body.style.overflow = '';
    });
  }

  // 메뉴 항목 클릭 이벤트
  initSideMenuItems();
}

/**
 * 사이드 메뉴 항목 클릭 이벤트 초기화
 */
function initSideMenuItems() {
  const menuItems = document.querySelectorAll('.side-menu-item');
  const sideMenu = document.querySelector('.side-menu');
  const overlay = document.querySelector('.menu-overlay');

  menuItems.forEach(item => {
    item.addEventListener('click', function () {
      const page = this.getAttribute('data-page');

      // 현재 활성화된 메뉴 아이템 비활성화
      document.querySelector('.side-menu-item.active')?.classList.remove(
          'active');

      // 클릭한 메뉴 아이템 활성화
      this.classList.add('active');

      // 페이지 이동
      navigateToPage(page);

      // 메뉴 닫기
      sideMenu.classList.remove('active');
      overlay.classList.remove('active');
      document.body.style.overflow = '';
    });
  });
}

/**
 * 탭 메뉴 초기화
 */
function initTabMenu() {
  const tabItems = document.querySelectorAll('.tab-item');

  tabItems.forEach(tab => {
    tab.addEventListener('click', function () {
      const page = this.getAttribute('data-page');

      // 모든 탭 비활성화
      tabItems.forEach(item => {
        item.classList.remove('active');
      });

      // 클릭한 탭 활성화
      this.classList.add('active');

      // 페이지 이동 또는 컨텐츠 변경
      handleTabChange(page);
    });
  });
}

/**
 * 페이지 이동 함수
 * @param {string} page - 이동할 페이지 키
 */
async function navigateToPage(page) {
  // 페이지별 URL 매핑
  const pageUrls = {
    'home': '/main/main.html',
    'rewards': '/side/rewards.html',
    'link-x': '/twitter/connect',
    'link-dc': '/side/link-dc.html',
    'upload': '/side/activity.html',
    'subscribe': '/side/subscriptionStatus.html',
  };

  if (page === 'link-x') {
    callTwitter();
    return;
  }

  // 인증 확인이 필요한 페이지 목록
  const authRequiredPages = ['subscribe', 'upload'];

  // 페이지가 인증이 필요하거나 명시적으로 인증 확인을 요청한 경우
  if (authRequiredPages.includes(page)) {
    try {
      // 인증 확인
      const isAuthenticated = await checkAuthentication();

      if (!isAuthenticated) {
        // 인증 실패 시 로그인 페이지로 리다이렉트
        redirectToLogin();
        return;
      }

      // 인증 성공 시 해당 페이지로 이동
      if (pageUrls[page]) {
        window.location.href = pageUrls[page];
      }
    } catch (error) {
      console.error('인증 확인 중 오류 발생:', error);
      redirectToLogin();
    }
  } else {
    // 인증이 필요하지 않은 페이지는 바로 이동
    if (pageUrls[page]) {
      window.location.href = pageUrls[page];
    }
  }
}

function redirectToLogin() {
  // 현재 페이지 URL을 세션 스토리지에 저장 (로그인 후 돌아오기 위함)
  // const currentPath = window.location.pathname + window.location.search;
  // sessionStorage.setItem('redirectAfterLogin', currentPath);

  // 사용자에게 알림
  alert('로그인이 필요한 서비스입니다.');

  // 로그인 페이지로 이동
  window.location.href = '/login/login.html';
}

/**
 * 인증 상태 확인
 * @returns {Promise<boolean>} 인증 성공 여부
 */
async function checkAuthentication() {
  try {
    const token = localStorage.getItem('accessToken');

    // 토큰이 없으면 인증 실패로 간주
    if (!token) {
      return false;
    }

    // 인증 상태 확인 API 호출
    const res = await fetch('/api/auth/validate', {
      method: 'GET',
      headers: {'Authorization': `Bearer ${token}`}
    });

    // 204 No Content면 인증 완료 상태
    return res.status === 204;
  } catch (error) {
    console.error('인증 확인 오류:', error);
    return false;
  }
}

async function callTwitter() {
  try {
    const response = await authFetch(`/twitter/connect`);

    const result = await response.json();

    window.location.href = result.response.connectUrl;

    return result;
  } catch (err) {
    console.error('오류 발생:', err);
    return null;
  }
}

/**
 * 탭 변경 처리
 * @param {string} page - 선택된 탭 페이지 키
 */
function handleTabChange(page) {
  // 페이지별 URL 매핑
  const pageUrls = {
    'home': '/main/main.html',
    'original': '/original/originalContent.html',
    'board': '/board/boardList.html',
    'vote': '/vote/voteList.html'
  };

  // 해당 페이지로 이동
  if (pageUrls[page]) {
    window.location.href = pageUrls[page];
  }
}

// 외부에서 사용할 수 있도록 일부 함수 노출
window.loadCommonComponents = loadCommonComponents;
window.initSideMenu = initSideMenu;
window.initTabMenu = initTabMenu;
