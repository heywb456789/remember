import VideoService from "./video-service.js";
import NewsArticleService from "./news-service.js";

// 현재 활성화된 탭과 컨텐츠 타입 매핑
const TAB_CONTENT_TYPE = {
  'video': 'YOUTUBE_VIDEO',   // 롱폼
  'shorts': 'YOUTUBE_SHORTS', // 쇼츠
  'article': 'NEWS_ARTICLE'   // 뉴스토마토 기사
};

// 현재 활성화된 탭
let currentTab = 'video';
// 페이지당 아이템 수
const PAGE_SIZE = 4;
// 현재 페이지 번호 (0부터 시작)
let currentPage = 0;
// 모든 데이터를 로드했는지 여부
let isAllLoaded = false;
// 데이터 로딩 중 여부
let isLoading = false;
// 무한 스크롤 옵저버
let scrollObserver = null;

/**
 * 콘텐츠 목록을 API에서 불러와 동적으로 구성하는 함수
 * @param {string} tab - 탭 종류 (video, shorts, article)
 * @param {number} page - 페이지 번호 (기본값: 0)
 * @param {number} size - 한 페이지당 아이템 수 (기본값: 5)
 * @param {string} keyword - 검색어 (기본값: 빈 문자열)
 * @param {boolean} append - 기존 콘텐츠에 추가할지 여부
 */
async function loadContent(tab = 'video', page = 0, size = PAGE_SIZE,
    keyword = '', append = false) {
  // 이미 로딩 중이거나 모든 데이터를 로드한 경우 중단
  if (isLoading || (isAllLoaded && append)) {
    // console.log(`로드 중단: 로딩=${isLoading}, 모든데이터로드=${isAllLoaded}, 추가=${append}`);
    return;
  }

  // console.log(`콘텐츠 로드 시작: 탭=${tab}, 페이지=${page}, 크기=${size}, 추가=${append}`);

  // 컨텐츠 타입 가져오기
  const contentType = TAB_CONTENT_TYPE[tab];

  // 컨텐츠 리스트 컨테이너 확인
  const contentListElement = document.getElementById(`${tab}List`);
  if (!contentListElement) {
    return;
  }

  // 탭 변경 시 이전 탭 데이터 초기화
  if (!append) {
    // 콘텐츠 리스트 활성화 상태 변경
    const contentLists = document.querySelectorAll('.content-list');
    contentLists.forEach(list => {
      list.classList.remove('active');
      list.style.display = 'none'; // 명시적으로 display 속성 설정
    });
    contentListElement.classList.add('active');
    contentListElement.style.display = tab === 'shorts' ? 'grid' : 'block'; // shorts는 grid, 나머지는 block

    // 현재 탭 업데이트
    currentTab = tab;

    // 페이지 초기화
    currentPage = page;

    // 모든 데이터 로드 상태 초기화
    isAllLoaded = false;

    // 이전 내용 비우기 (확실하게 초기화)
    contentListElement.innerHTML = '';
  }

  // 로딩 상태 표시
  isLoading = true;
  showLoading(true);

  try {
    // console.log(`API 호출: 페이지=${page}, 크기=${size}, 타입=${contentType}`);
    // API 호출
    console.log(contentType);
    let response = '';
    if(contentType === 'NEWS_ARTICLE'){
      response = await NewsArticleService.getNewsList(page, 10, '', keyword);
    }else{
      response = await VideoService.getLatestVideos(page, size,
        contentType, keyword);
    }

    // 응답 데이터 확인
    if (!response || !response.response || !response.response.data) {
      throw new Error('Invalid response data');
    }

    const data = response.response;
    // console.log(`API 응답 데이터:`, data);

    // 데이터 없음 처리
    if (!data.data || data.data.length === 0) {
      // console.log(`데이터 없음: 추가=${append}`);
      // 추가 로드 시 모든 데이터 로드 완료 상태로 설정
      if (append) {
        isAllLoaded = true;
      } else {
        // 초기 로드 시 빈 상태 표시
        showEmptyState(contentListElement);
      }
      return;
    }

    // 페이지네이션 정보 업데이트
    if (data.pagination) {
      // console.log(`페이지네이션 정보:`, data.pagination);
      // 현재 페이지가 마지막 페이지면 모든 데이터 로드 완료
      if (data.pagination.currentPage >= data.pagination.totalPages) {
        isAllLoaded = true;
        // console.log(
        //     `마지막 페이지 도달: ${data.pagination.currentPage}/${data.pagination.totalPages
        //     - 1}`);
      }

      // 현재 페이지 업데이트
      currentPage = data.pagination.currentPage - 1;
    }

    // 콘텐츠 업데이트
    updateContent(tab, data, append);

    // 새 콘텐츠가 추가된 후 무한 스크롤 초기화
    initInfiniteScroll();

  } catch (error) {
    // console.error(`${tab} 콘텐츠 로드 오류:`, error);
    // handleFetchError(error);
    // 초기 로드에서만 오류 메시지 표시
    if (!append) {
      showError(tab, contentListElement);
    }
  } finally {
    // 로딩 상태 숨김
    isLoading = false;
    showLoading(false);
  }
}

/**
 * 콘텐츠 업데이트 함수
 * @param {string} tab - 탭 종류
 * @param {Object} data - API 응답 데이터
 * @param {boolean} append - 기존 콘텐츠에 추가할지 여부
 */
function updateContent(tab, data, append = false) {
  const contentList = document.getElementById(`${tab}List`);
  if (!contentList) {
    return;
  }

  // 데이터가 없는 경우
  if (!data.data || data.data.length === 0) {
    if (!append) {
      showEmptyState(contentList);
    }
    return;
  }

  // 컨텐츠 HTML 생성
  let contentHTML = '';

  if (tab === 'video') {
    contentHTML = createVideoListHTML(data.data);
  } else if (tab === 'shorts') {
    contentHTML = createShortsListHTML(data.data);
  } else if (tab === 'article') {
    contentHTML = createArticleListHTML(data.data);
  }

  // HTML 삽입 또는 추가
  if (append) {
    // 기존 내용에 추가
    contentList.insertAdjacentHTML('beforeend', contentHTML);
  } else {
    // 내용 교체
    contentList.innerHTML = contentHTML;
  }

  // 아이템 클릭 이벤트 초기화
  initContentItemsClick(tab);

  // 마우스 오버 및 터치 이벤트 초기화 (비디오와 쇼츠에만 적용)
  if (tab === 'video' || tab === 'shorts') {
    initVideoHoverPreview(tab);
  }
}

/**
 * 빈 상태 메시지 표시
 * @param {HTMLElement} container - 컨테이너 요소
 */
function showEmptyState(container) {
  container.innerHTML = `
    <div class="empty-state">
      <div class="empty-icon">
        <svg width="80" height="80" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M19 5V19H5V5H19ZM21 3H3V21H21V3Z" fill="#adb5bd"/>
          <path d="M14 17H6V15H14V17Z" fill="#adb5bd"/>
          <path d="M14 13H6V11H14V13Z" fill="#adb5bd"/>
          <path d="M18 9H6V7H18V9Z" fill="#adb5bd"/>
        </svg>
      </div>
      <p class="empty-title">콘텐츠가 없습니다</p>
      <p class="empty-message">새로운 콘텐츠가 곧 업데이트될 예정입니다</p>
    </div>
  `;
}

/**
 * 오류 상태 메시지 표시
 * @param {string} tab - 탭 종류
 * @param {HTMLElement} container - 컨테이너 요소
 */
function showError(tab, container) {
  container.innerHTML = `
    <div class="error-state">
      <div class="error-icon">
        <svg width="60" height="60" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 4C7.58172 4 4 7.58172 4 12C4 16.4183 7.58172 20 12 20C16.4183 20 20 16.4183 20 12C20 7.58172 16.4183 4 12 4Z" stroke="#ff6b6b" stroke-width="2"/>
          <path d="M12 8V13" stroke="#ff6b6b" stroke-width="2" stroke-linecap="round"/>
          <circle cx="12" cy="16" r="1" fill="#ff6b6b"/>
        </svg>
      </div>
      <p class="error-title">콘텐츠를 불러오는 데 실패했습니다</p>
      <p class="error-message">네트워크 연결을 확인하시거나 잠시 후 다시 시도해 주세요</p>
      <button class="retry-button" onclick="window.loadContent('${tab}', 0)">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4C7.58 4 4.01 7.58 4.01 12C4.01 16.42 7.58 20 12 20C15.73 20 18.84 17.45 19.73 14H17.65C16.83 16.33 14.61 18 12 18C8.69 18 6 15.31 6 12C6 8.69 8.69 6 12 6C13.66 6 15.14 6.69 16.22 7.78L13 11H20V4L17.65 6.35Z" fill="#4263eb"/>
        </svg>
        다시 시도하기
      </button>
    </div>
  `;

  // 재시도 버튼 이벤트 등록
  const retryButton = container.querySelector('.retry-button');
  if (retryButton) {
    retryButton.addEventListener('click', (e) => {
      e.preventDefault();
      loadContent(tab, 0);
    });
  }
}

/**
 * 무한 스크롤 초기화
 */
function initInfiniteScroll() {
  // 이전 옵저버가 있으면 해제
  if (scrollObserver) {
    scrollObserver.disconnect();
    scrollObserver = null;
  }

  // 현재 활성화된 리스트 컨테이너
  const contentList = document.getElementById(`${currentTab}List`);
  if (!contentList) {
    return;
  }

  // 마지막 아이템 찾기
  const items = contentList.querySelectorAll(`.${currentTab}-item`);
  if (items.length === 0) {
    return;
  } // 아이템이 없으면 종료

  const lastItem = items[items.length - 1];

  // console.log(`무한 스크롤 설정: ${currentTab} 탭, 마지막 아이템:`, lastItem);

  // 인터섹션 옵저버 설정
  const options = {
    root: null,
    rootMargin: '200px', // 더 일찍 로드되도록 마진 크게 설정
    threshold: 0.1 // 낮은 임계값으로 감도 높임
  };

  // 옵저버 생성
  scrollObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting && !isLoading && !isAllLoaded) {
        // console.log(`스크롤 감지: ${currentTab} 탭의 다음 페이지(${currentPage + 1}) 로드`);
        loadContent(currentTab, currentPage + 1, PAGE_SIZE, '', true);
      }
    });
  }, options);

  // 마지막 아이템 감시
  scrollObserver.observe(lastItem);
}

/**
 * 동영상 리스트 HTML 생성
 * @param {Array} items - 동영상 아이템 배열
 * @returns {string} HTML 문자열
 */
function createVideoListHTML(items) {
  return items.map(item => {
    const isNew = item.new;
    const videoId = item.videoId;
    const videoUrl = item.videoUrl;
    const videoType = getVideoType(item);

    // 유튜브 동영상 ID 추출
    const youtubeId = videoType === 'youtube' ?
      (item.youtubeVideoId || extractYouTubeId(videoUrl) || videoId) : null;

    return `
      <div class="video-item" data-id="${videoId}" data-url="${videoUrl}" data-type="${videoType}" data-youtube-id="${youtubeId || ''}">
        <div class="video-thumbnail-container">
          <img src="${item.thumbnailUrl}" alt="${item.title}" class="video-thumbnail">
          
          ${videoType === 'uploaded' ? `
            <!-- 직접 업로드 동영상만 hover-video 적용 -->
            <video class="hover-video" src="${videoUrl}" muted preload="none" loop></video>
          ` : `
            <!-- 유튜브 동영상은 iframe 미리보기 (선택사항) -->
            <div class="youtube-preview-container" style="display: none;">
              <iframe 
                class="youtube-preview-iframe" 
                src="https://www.youtube.com/embed/${youtubeId}?autoplay=1&mute=1&controls=0&rel=0&modestbranding=1" 
                frameborder="0" 
                allow="autoplay; encrypted-media">
              </iframe>
            </div>
          `}
          
          <span class="video-duration">${formatDuration(item.durationSec)}</span>
          ${isNew ? '<span class="video-badge badge-new">N</span>' : ''}
          
          <div class="play-overlay">
            <div class="play-button">
              <i class="fas fa-play"></i>
            </div>
          </div>
        </div>
        
        <div class="video-info" data-id="${videoId}">
          <h3 class="video-title">${item.title}</h3>
          <div class="video-meta">
            <span class="video-date">${formatDate(item.publishedAt || item.createdAt)}</span>
            <span class="video-views">조회수 ${formatViews(item.viewCount)}회</span>
            ${videoType === 'youtube' ? '<span class="video-source">YouTube</span>' : ''}
          </div>
        </div>
      </div>
    `;
  }).join('');
}

/**
 * 쇼츠 리스트 HTML 생성
 * @param {Array} items - 쇼츠 아이템 배열
 * @returns {string} HTML 문자열
 */
function createShortsListHTML(items) {
  return items.map(item => {
    const isNew = item.new;
    const videoId = item.videoId;
    const videoUrl = item.videoUrl;
    const videoType = getVideoType(item);

    // 유튜브 동영상 ID 추출
    const youtubeId = videoType === 'youtube' ?
      (item.youtubeVideoId || extractYouTubeId(videoUrl) || videoId) : null;

    return `
      <div class="shorts-item" data-id="${videoId}" data-url="${videoUrl}" data-type="${videoType}" data-youtube-id="${youtubeId || ''}">
        <img src="${item.thumbnailUrl}" alt="${item.title}" class="shorts-thumbnail">
        
        ${videoType === 'uploaded' ? `
          <!-- 직접 업로드 쇼츠만 hover-video 적용 -->
          <video class="hover-video" src="${videoUrl}" muted preload="none" loop></video>
        ` : ''}
        
        ${isNew ? '<span class="shorts-badge badge-new">N</span>' : ''}
        
        <div class="shorts-overlay" data-id="${videoId}">
          <h3 class="shorts-title">${item.title}</h3>
          <span class="shorts-views">조회수 ${formatViews(item.viewCount)}회</span>
        </div>
      </div>
    `;
  }).join('');
}

/**
 * 기사 리스트 HTML 생성
 * @param {Array} items - 기사 아이템 배열
 * @returns {string} HTML 문자열
 */
function createArticleListHTML(items) {
  return items.map(item => {
    const articleId = item.articleId || item.id;
    const isHot = item.hot === true; // hot 속성 확인
    const category = getCategoryDisplayName(item.category); // 카테고리 표시명 가져오기

    return `
      <div class="article-item" data-id="${articleId}">
        <img src="${item.thumbnailUrl}" alt="${item.title}" class="article-thumbnail">
        <div class="article-info">
          <div class="article-badges">
            ${category ? `<span class="article-badge badge-category">${category}</span>` : ''}
            ${item.new ? '<span class="article-badge badge-new">N</span>' : ''}
            ${isHot ? '<span class="article-badge badge-hot">HOT</span>' : ''}
          </div>
          <h3 class="article-title">${item.title}</h3>
          <div class="article-meta">
            <span class="article-date">${formatDate(item.publishedAt || item.createdAt)}</span>
            ${item.viewCount ? `<span class="article-views">조회 ${formatViews(item.viewCount)}</span>` : ''}
          </div>
        </div>
      </div>
    `;
  }).join('');
}

/**
 * 카테고리 코드를 표시용 이름으로 변환
 */
function getCategoryDisplayName(category) {
  const categoryMap = {
    'ECONOMY': '경제',
    'STOCK': '주식',
    'POLITICS': '정치',
    'SOCIETY': '사회',
    'GLOBAL': '국제',
    'TECH': 'IT/과학',
    'CULTURE': '문화',
    'SPORTS': '스포츠'
  };

  return categoryMap[category] || '';
}

/**
 * 콘텐츠 아이템 클릭 이벤트 초기화
 * @param {string} tab - 탭 종류
 */
/**
 * 콘텐츠 아이템 클릭 이벤트 초기화
 * @param {string} tab - 탭 종류
 */
function initContentItemsClick(tab) {
  // 아이템 전체 클릭 이벤트
  const items = document.querySelectorAll(`.${tab}-item`);
  items.forEach(item => {
    item.addEventListener('click', function(e) {
      const videoType = this.getAttribute('data-type');
      const contentId = this.getAttribute('data-id');
      const youtubeId = this.getAttribute('data-youtube-id');

      if (!contentId) {
        return;
      }

      // 비디오 영역 클릭 시 미리보기가 시작된 경우 이벤트 중지
      if (tab !== 'article' &&
          e.target.closest('.video-thumbnail-container') &&
          e.currentTarget.querySelector('.hover-video')?.style.opacity === '1') {
        return;
      }

      // 유튜브 동영상인 경우 처리 방식 선택
      if (videoType === 'youtube' && youtubeId) {
        // 옵션 1: 유튜브로 직접 이동
        // window.open(`https://www.youtube.com/watch?v=${youtubeId}`, '_blank');
        // return;

        // 옵션 2: 내 사이트의 상세 페이지로 이동 (유튜브 임베드)
        navigateToDetail(tab, contentId, videoType);
      } else {
        // 직접 업로드 동영상은 기존대로 처리
        navigateToDetail(tab, contentId, videoType);
      }
    });
  });

  // 비디오/쇼츠 탭에만 필요한 추가 이벤트 처리
  if (tab !== 'article') {
    // 제목 및 정보 영역 클릭 이벤트
    const infoItems = document.querySelectorAll(
        `.${tab}-info, .${tab}-title, .${tab}-overlay`);
    infoItems.forEach(info => {
      info.addEventListener('click', function(e) {
        e.stopPropagation(); // 상위 요소 이벤트 전파 방지

        const contentId = this.getAttribute('data-id');
        if (!contentId) {
          return;
        }

        const parentItem = this.closest(`.${tab}-item`);
        const videoType = parentItem?.getAttribute('data-type') || 'uploaded';

        // 콘텐츠 상세 페이지로 이동
        navigateToDetail(tab, contentId, videoType);
      });
    });
  }
}


/**
 * 콘텐츠 상세 페이지로 이동
 * @param {string} tab - 탭 종류
 * @param {string|number} contentId - 콘텐츠 ID
 * @param {string} videoType - 비디오 타입 ('youtube' | 'uploaded')
 */
function navigateToDetail(tab, contentId, videoType = 'uploaded') {
  let detailPage = '';

  switch (tab) {
    case 'video':
      detailPage = '/original/videoDetail.html';
      break;
    case 'shorts':
      detailPage = '/original/videoShortsDetail.html'; // videoShortsDetail.html 대신 shortsVideo.html 사용
      break;
    case 'article':
      detailPage = '/original/newsDetail.html';
      break;
  }

  saveCurrentTab(tab);

  // URL 파라미터 구성 수정
  const params = new URLSearchParams();
  params.set('id', contentId);

  // // 비디오/쇼츠인 경우에만 type 파라미터 추가
  // if (tab === 'video' || tab === 'shorts') {
  //   params.set('type', videoType);
  // }

  // 최종 URL 구성
  window.location.href = `${detailPage}?${params.toString()}`;
}

/**
 * 동영상 타입 판별 함수
 * @param {Object} item - 동영상 아이템
 * @returns {string} 'youtube' | 'uploaded'
 */
function getVideoType(item) {
  // videoUrl이 유튜브 링크인지 확인
  if (item.videoUrl && (
    item.videoUrl.includes('youtube.com') ||
    item.videoUrl.includes('youtu.be') ||
    item.videoUrl.includes('youtube.com/embed')
  )) {
    return 'youtube';
  }

  // youtubeVideoId가 있으면 유튜브
  if (item.youtubeVideoId || item.videoId?.length === 11) {
    return 'youtube';
  }

  // 그 외는 직접 업로드
  return 'uploaded';
}


/**
 * 유튜브 동영상 ID 추출 함수
 * @param {string} url - 유튜브 URL
 * @returns {string|null} 동영상 ID
 */
function extractYouTubeId(url) {
  if (!url) return null;

  const regex = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?\/\s]{11})/;
  const match = url.match(regex);
  return match ? match[1] : null;
}



/**
 * 비디오 마우스 오버 미리보기 초기화
 * @param {string} tab - 탭 종류 (video 또는 shorts)
 */
function initVideoHoverPreview(tab) {
  const videoContainers = document.querySelectorAll(`.${tab}-item`);

  videoContainers.forEach(container => {
    const videoType = container.getAttribute('data-type');
    const thumbnailImg = container.querySelector(`.${tab}-thumbnail`);
    const videoElement = container.querySelector('.hover-video');

    // 유튜브 동영상은 호버 미리보기 적용하지 않음
    if (videoType === 'youtube' || !videoElement || !thumbnailImg) {
      return;
    }

    // 호버 타이머 (지연 시작용)
    let hoverTimer;
    // 미리보기 타이머 (자동 종료용)
    let previewTimer;
    // 미리보기 시작 여부
    let previewStarted = false;

    // 데스크탑: mouseenter 이벤트 - 지연 시작
    container.addEventListener('mouseenter', function () {
      // 0.8초 지연 후 미리보기 시작
      hoverTimer = setTimeout(() => {
        startVideoPreview();
      }, 800);
    });

    // 모바일: touchstart 이벤트 - 길게 누르기
    let touchTimer;
    let touchStartY;
    let touchStartTime;

    container.addEventListener('touchstart', function (e) {
      touchStartY = e.touches[0].clientY;
      touchStartTime = Date.now();
      previewStarted = false;

      // 1초 동안 길게 터치하면 미리보기 시작
      touchTimer = setTimeout(() => {
        previewStarted = true;
        startVideoPreview();
      }, 1000);
    });

    container.addEventListener('touchmove', function (e) {
      const currentY = e.touches[0].clientY;
      const deltaY = touchStartY - currentY;

      // 위로 25px 이상 스와이프하면 미리보기 시작
      if (deltaY > 25) {
        clearTimeout(touchTimer);
        if (!previewStarted) {
          previewStarted = true;
          startVideoPreview();
        }
      }
    });

    // 비디오 미리보기 시작 함수
    function startVideoPreview() {
      // 비디오가 준비되지 않은 경우 로드
      if (videoElement.readyState === 0) {
        videoElement.load();
        // 로드 완료 후 재생 시작
        videoElement.addEventListener('loadeddata', playVideo, {once: true});
      } else {
        playVideo();
      }

      function playVideo() {
        // 이미지 숨기고 비디오 표시
        thumbnailImg.style.opacity = '0';
        videoElement.style.opacity = '1';

        // 비디오 재생
        videoElement.play().catch(err => {
          console.error('비디오 재생 오류:', err);
          // 재생 실패 시 이미지 다시 표시
          thumbnailImg.style.opacity = '1';
          videoElement.style.opacity = '0';
        });

        // 5초 후 비디오 정지 및 썸네일 다시 표시
        clearTimeout(previewTimer);
        previewTimer = setTimeout(() => {
          stopVideoPreview();
        }, 5000);
      }
    }

    // 데스크탑: mouseleave 이벤트
    container.addEventListener('mouseleave', function () {
      clearTimeout(hoverTimer); // 호버 타이머 취소
      stopVideoPreview();
    });

    // 모바일: touchend 이벤트
    container.addEventListener('touchend', function (e) {
      clearTimeout(touchTimer);

      const touchEndTime = Date.now();
      const touchDuration = touchEndTime - touchStartTime;

      if (previewStarted) {
        stopVideoPreview();
      }
    });

    // 비디오 미리보기 중지 함수
    function stopVideoPreview() {
      previewStarted = false;
      clearTimeout(previewTimer);
      videoElement.pause();
      // 가능하면 처음으로 되감기
      if (videoElement.readyState >= 2) {
        videoElement.currentTime = 0;
      }
      thumbnailImg.style.opacity = '1';
      videoElement.style.opacity = '0';
    }
  });
}

/**
 * 콘텐츠 탭 클릭 이벤트 초기화
 */
// 중복 이벤트 바인딩 방지
function initTabsEvent() {
  const tabs = document.querySelectorAll('.content-tab-item');
  tabs.forEach(tab => {
    tab.removeEventListener('click', handleTabClick);
    tab.addEventListener('click', handleTabClick);
  });
}

// 탭 클릭 핸들러 함수 수정
function handleTabClick(event) {
  const tabType = this.getAttribute('data-tab');

  // 이미 활성화된 탭이면 아무것도 하지 않음
  if (currentTab === tabType) {
    return;
  }

  // 모든 탭 비활성화 (명시적으로 모든 탭에서 active 클래스 제거)
  const allTabs = document.querySelectorAll('.content-tabs .content-tab-item');
  allTabs.forEach(item => item.classList.remove('active'));

  // 클릭한 탭만 활성화
  this.classList.add('active');

  // 스크롤 옵저버 해제
  if (scrollObserver) {
    scrollObserver.disconnect();
    scrollObserver = null;
  }

  // 상태 초기화
  isAllLoaded = false;
  currentPage = 0;

  // 모든 콘텐츠 리스트 숨기기 (이전 탭 내용 확실히 숨김)
  const contentLists = document.querySelectorAll('.content-list');
  contentLists.forEach(list => {
    list.classList.remove('active');
    list.style.display = 'none';
  });

  // 현재 탭 저장
  currentTab = tabType;
  sessionStorage.setItem('currentContentTab', tabType);

  // 새 콘텐츠 로드
  loadContent(tabType, 0, PAGE_SIZE);
}

/**
 * 공통 탭 메뉴 이벤트 초기화
 */
function initCommonTabMenu() {
  // 공통 탭 메뉴의 탭 아이템들
  const commonTabItems = document.querySelectorAll(
      '#tab-menu-container .tab-item');

  if (commonTabItems.length === 0) {
    console.warn('공통 탭 메뉴 아이템을 찾을 수 없습니다.');
    return;
  }

  commonTabItems.forEach(tab => {
    tab.addEventListener('click', function () {
      // 이미 active 클래스가 있는 경우에는 아무 작업도 하지 않음
      if (this.classList.contains('active')) {
        return;
      }

      const page = this.getAttribute('data-page') || '';

      // 페이지별 URL 매핑
      const pageUrls = {
        'home': '/main/main.html',
        'original': '/original/originalContent.html',
        'board': '/board/boardList.html',
        'vote': '/vote/voteList.html',
        'column': '/column/columnList.html',
        'notice': '/notice/noticeList.html'
      };

      // 해당 페이지로 이동
      if (pageUrls[page]) {
        window.location.href = pageUrls[page];
      } else {
        console.warn(`알 수 없는 탭 페이지: ${page}`);
      }
    });
  });
}

/**
 * 검색 이벤트 초기화
 */
function initSearch() {
  const searchInput = document.querySelector('.search-input');
  const searchForm = document.querySelector('.search-form');

  if (searchForm) {
    searchForm.addEventListener('submit', function (e) {
      e.preventDefault();
      const keyword = searchInput.value.trim();

      // 스크롤 옵저버 해제
      if (scrollObserver) {
        scrollObserver.disconnect();
        scrollObserver = null;
      }

      // 상태 초기화
      isAllLoaded = false;
      currentPage = 0;

      loadContent(currentTab, 0, PAGE_SIZE, keyword);
    });
  }

  if (searchInput) {
    // 검색어 입력 후 엔터 키 이벤트
    searchInput.addEventListener('keypress', function (e) {
      if (e.key === 'Enter') {
        e.preventDefault();
        const keyword = this.value.trim();

        // 스크롤 옵저버 해제
        if (scrollObserver) {
          scrollObserver.disconnect();
          scrollObserver = null;
        }

        // 상태 초기화
        isAllLoaded = false;
        currentPage = 0;

        loadContent(currentTab, 0, PAGE_SIZE, keyword);
      }
    });
  }
}

/**
 * 로딩 상태 표시 함수
 * @param {boolean} loading - 로딩 중 여부
 */
function showLoading(loading) {
  const loadingState = document.getElementById('loadingState');
  if (loadingState) {
    loadingState.style.display = loading ? 'flex' : 'none';
  }
}

/**
 * 아이템이 새로운지 확인 (48시간 이내 생성)
 * @param {string} dateString - 날짜 문자열
 * @returns {boolean} 새로운 아이템 여부
 */
function isItemNew(dateString) {
  if (!dateString) {
    return false;
  }

  const itemDate = new Date(dateString);
  const now = new Date();
  const diffTime = now.getTime() - itemDate.getTime();
  const diffHours = diffTime / (1000 * 60 * 60);

  return diffHours <= 48;
}

/**
 * 날짜 포맷 함수
 * @param {string} dateString - 날짜 문자열
 * @returns {string} 포맷된 날짜
 */
function formatDate(dateString) {
  if (!dateString) {
    return '';
  }

  const date = new Date(dateString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${year}.${month}.${day} ${hours}:${minutes}`;
}

/**
 * 조회수 포맷 함수
 * @param {number} views - 조회수
 * @returns {string} 포맷된 조회수
 */
function formatViews(views) {
  if (!views) {
    return '0';
  }

  if (views >= 10000) {
    return `${Math.floor(views / 10000)}만`;
  } else if (views >= 1000) {
    return `${Math.floor(views / 1000)}천`;
  }

  return views.toString();
}

/**
 * 영상 길이 포맷 함수
 * @param {number} seconds - 초 단위 길이
 * @returns {string} 포맷된 길이 (00:00)
 */
function formatDuration(seconds) {
  if (!seconds) {
    return '00:00';
  }

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = Math.floor(seconds % 60);

  return `${String(minutes).padStart(2, '0')}:${String(
      remainingSeconds).padStart(2, '0')}`;
}

function saveCurrentTab(tab) {
  sessionStorage.setItem('currentContentTab', tab);
}

// 전역 함수 노출 (retry 버튼 등에서 사용)
window.loadContent = loadContent;

// 페이지 로드 시 실행되는 코드 수정
document.addEventListener('DOMContentLoaded', function () {
  if (window.originalContentInitialized) {
    return;
  }
  window.originalContentInitialized = true;

  // 콘텐츠 전용 탭 이벤트 및 검색 이벤트 초기화
  initTabsEvent();
  initSearch();

  // 세션 스토리지에서 마지막으로 선택한 탭 가져오기
  const lastTab = sessionStorage.getItem('currentContentTab') || 'video';

  // 먼저 모든 탭의 활성화 상태를 제거 (중요: 중복 활성화 방지)
  const allTabs = document.querySelectorAll('.content-tabs .content-tab-item');
  allTabs.forEach(item => item.classList.remove('active'));

  // 해당 탭 활성화
  const tabToActivate = document.querySelector(`.content-tab-item[data-tab="${lastTab}"]`);
  if (tabToActivate) {
    tabToActivate.classList.add('active');
    currentTab = lastTab;
  } else {
    // 탭을 찾지 못한 경우 기본 탭(video) 활성화
    const defaultTab = document.querySelector('.content-tab-item[data-tab="video"]');
    if (defaultTab) {
      defaultTab.classList.add('active');
      currentTab = 'video';
    }
  }

  // 선택된 탭의 콘텐츠 로드
  loadContent(currentTab, 0, PAGE_SIZE);
});
