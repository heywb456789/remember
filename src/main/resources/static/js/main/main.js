// main.js
import VideoService from '../original/video-service.js';
import {
  optionalAuthFetch,
  handleFetchError,
  authFetch,
  FetchError
} from '../commonFetch.js';

document.addEventListener('DOMContentLoaded', async function () {

  initErrorAlert();
  await initAuthModalAndCheck();

  // 1) 최신 동영상
  await loadLatestVideo();

  // 2) 자유게시판 최신 4개 글
  await loadLatestPosts();

  // 3) 투표 공간 최신 4개
  await loadLatestPolls();

  // 4) '더보기' 클릭 이벤트
  initMoreLink();
  initPollMoreLink();     // 투표 공간

});

async function initAuthModalAndCheck() {
  let memberStatus = null;

  try {
    // authFetch → 액세스 토큰이 만료되면 handleTokenRefresh() 통해 자동 리프레시
    const response = await authFetch('/api/auth/me', {method: 'GET'});
    const data = await response.json();
    memberStatus = data.response?.status || null;
  } catch (err) {
    if (err instanceof FetchError && err.httpStatus === 401) {
      // 리프레시 실패 혹은 권한 문제일 경우 → 토큰만 지워둠 (commonFetch.js 내부에서 처리됨)
      memberStatus = null;
    } else {
      console.error('initAuthModalAndCheck: 네트워크/권한 오류', err);
      memberStatus = null;
    }
  }

  // 백엔드 enum값에 맞춰 “인증 미완료”에 해당하는 상태를 배열에 넣어주세요.
  const UNVERIFIED_STATUSES = ['TEMPORARY_PASS'];

  if (UNVERIFIED_STATUSES.includes(memberStatus)) {
    const identityModalEl = document.getElementById('identityModal');
    if (!identityModalEl) {
      return;
    }

    const identityModal = new bootstrap.Modal(identityModalEl);
    const skipKey = 'hideIdentityModal';
    const today = new Date().toISOString().split('T')[0]; // "YYYY-MM-DD"

    // 로컬 스토리지에 저장된 skip 날짜와 다르면 모달 띄우기
    if (localStorage.getItem(skipKey) !== today) {
      identityModal.show();
    }

    // “오늘 하루 보지 않기” 체크박스 처리
    const checkbox = document.getElementById('identitySkipToday');
    checkbox?.addEventListener('change', function () {
      if (this.checked) {
        localStorage.setItem(skipKey, today);
      } else {
        localStorage.removeItem(skipKey);
      }
    });

    // “인증하러 가기” 버튼 클릭 시 → /api/auth/pass/prepare 호출하여 패스 인증 준비
    const proceedBtn = document.getElementById('identityProceedBtn');
    proceedBtn?.addEventListener('click', async function () {
      try {
        // 1) 인증 준비 API 호출
        const res = await authFetch('/api/auth/pass/prepare', {method: 'POST'});
        const data = await res.json();
        const {
          requestUrl,
          encData,
          tokenVersionId,
          integrityValue
        } = data.response;

        // 2) Nice 인증 서버로 바로 POST할 Form 생성 및 제출
        const formData = {
          enc_data: encData,
          token_version_id: tokenVersionId,
          integrity_value: integrityValue,
          m: 'service'
        };
        submitFormToNice(requestUrl, formData);
      } catch (err) {
        alert('인증 요청 실패: ' + (err.message || '네트워크 오류'));
      }
    });
  }
}

function initErrorAlert() {
  const params = new URLSearchParams(window.location.search);
  if (params.get('error') === 'true') {
    alert('패스 인증이 실패하였습니다. 잠시 후 다시 시도해주세요.');
  }
}

/**
 * 최신 동영상을 로드하여 화면에 표시
 */
async function loadLatestVideo() {
  const hotTopicSection = document.querySelector('.hot-topic');
  try {
    hotTopicSection.innerHTML = '<div class="loading">동영상을 불러오는 중...</div>';

    //호출
    const latestVideo = await VideoService.getLatestVideo();

    if (!latestVideo) {
      hotTopicSection.innerHTML = '<div class="no-content">현재 표시할 동영상이 없습니다.</div>';
      return;
    }

    updateHotTopicSection(latestVideo);

  } catch (err) {
    // handleFetchError(err);  //주석 해제 하면 알럿 뜸
    hotTopicSection.innerHTML = '<div class="error">동영상을 불러오는 중 오류가 발생했습니다.</div>';
  }
}

/**
 * 최신 4개 게시글을 불러와 자유게시판 섹션을 업데이트
 */
async function loadLatestPosts() {
  const newsList = document.querySelectorAll('.news-section')[1]?.querySelector(
      '.news-list');
  if (!newsList) {
    return;
  }

  // 로딩 표시 (선택 사항)
  newsList.innerHTML = '<p class="loading-text">게시글을 불러오는 중...</p>';

  try {
    const res = await optionalAuthFetch('/api/board/posts?page=0&size=4');
    const {response} = await res.json();
    const items = response.data || [];

    if (items.length === 0) {
      newsList.innerHTML = '<p class="empty-title">게시글이 없습니다</p>';
      return;
    }

    newsList.innerHTML = items.map(item => {
      const badge = item.new
          ? `<div class="poll-icon poll-active">N</div>`
          : ``;
      return `
      <div class="news-item" data-id="${item.boardId}">
        <div class="news-title">${item.title}</div>
        <div class="news-comment">
          ${badge}
          <i class="far fa-comment comment-icon"></i>
          <span class="comment-count">${item.commentCount}</span>
        </div>
      </div>
    `
    }).join('');

    // 각 글 클릭 시 상세로 이동
    newsList.querySelectorAll('.news-item').forEach(el => {
      el.addEventListener('click', () => {
        window.location.href = `/board/boardDetail.html?id=${el.dataset.id}`;
      });
    });
  } catch (err) {
    // console.error('게시글 로드 오류:', err);
    // handleFetchError(err);
    newsList.innerHTML = '<p class="error">게시글을 불러오는 중 오류가 발생했습니다.</p>';
  }
}

/**
 * 자유게시판 ‘더보기’ 링크 클릭 시 목록 페이지로 이동
 */
function initMoreLink() {
  const section = document.querySelectorAll('.news-section')[1];
  const moreLink = section?.querySelector('.more-link');
  if (moreLink) {
    moreLink.addEventListener('click', e => {
      e.preventDefault();
      window.location.href = '/board/boardList.html';  // 실제 목록 페이지 경로에 맞춰주세요
    });
  }
}

/**
 * 최신 투표 4개를 불러와 투표 공간 섹션을 업데이트
 */
async function loadLatestPolls() {
  const pollList = document.querySelector('.poll-section .poll-list');
  if (!pollList) {
    return;
  }

  // 로딩 표시
  pollList.innerHTML = '<p class="loading-text">투표를 불러오는 중...</p>';

  try {
    // 페이지 0, size 4
    const res = await optionalAuthFetch('/api/vote/posts?page=0&size=4');
    const {response} = await res.json();
    const items = response.data || [];

    if (items.length === 0) {
      pollList.innerHTML = '<p class="empty-title">투표가 없습니다.</p>';
      return;
    }

    // 항목별 HTML 생성
    pollList.innerHTML = items.map(item => {
      const badge = item.new
          ? `<div class="poll-icon poll-active">N</div>`
          : ``;

      return `
        <div class="poll-item" data-id="${item.votePostId}">
          <div class="poll-title">${item.question}</div>
          <div class="poll-status">
            ${badge}
            <i class="far fa-comment comment-icon"></i>
            <span class="comment-count">${item.commentCount}</span>
          </div>
        </div>
      `;
    }).join('');

    // 클릭 시 상세 페이지로 이동
    pollList.querySelectorAll('.poll-item').forEach(el => {
      el.addEventListener('click', () => {
        window.location.href = `/vote/voteDetail.html?id=${el.dataset.id}`;
      });
    });

  } catch (err) {
    // console.error('투표 불러오기 오류:', err);
    // handleFetchError(err);
    pollList.innerHTML = '<p class="error">투표를 불러오는 중 오류가 발생했습니다.</p>';
  }
}

/**
 * 투표 공간 ‘더보기’ 클릭 시 투표 목록 페이지로 이동
 */
function initPollMoreLink() {
  const moreLink = document.querySelector('.poll-section .more-link');
  if (moreLink) {
    moreLink.addEventListener('click', e => {
      e.preventDefault();
      // 실제 투표 목록 페이지 경로로 수정하세요
      window.location.href = '/vote/voteList.html';
    });
  }
}

/**
 * 핫토픽 섹션 업데이트
 * @param {Object} videoData - 비디오 데이터 응답
 */
function updateHotTopicSection(videoData) {
  const hotTopicSection = document.querySelector('.hot-topic');

  // 데이터가 없는 경우 처리
  if (!videoData || !videoData.response || !videoData.response.data
      || videoData.response.data.length === 0) {
    hotTopicSection.innerHTML = '<div class="no-content">현재 표시할 동영상이 없습니다.</div>';
    return;
  }

  // 첫 번째 비디오 항목 사용
  const video = videoData.response.data[0];

  // 비디오 재생 시간 포맷팅
  const duration = formatDuration(video.durationSec);

  // 날짜 포맷팅
  const publishedDate = formatDate(video.publishedAt);

  // 비디오 타입에 따른 HTML 생성 (Youtube 또는 일반 비디오)
  const isYouTubeVideo = video.type === 'YOUTUBE_VIDEO';

  // 핫토픽 섹션 HTML 업데이트
  hotTopicSection.innerHTML = `
    <div class="video-container">
      <div class="video-thumbnail" data-id="${video.videoId}" data-url="${video.videoUrl}">
        <!-- 썸네일 이미지 -->
        <img src="${video.thumbnailUrl}" alt="${video.title}" class="thumbnail-img">
        
        <!-- 비디오 요소 (마우스 오버 시 재생) -->
        <video class="hover-video" src="${video.videoUrl}" muted preload="none" loop></video>
        
        <div class="play-overlay">
          <div class="play-button">
            <i class="fas fa-play"></i>
          </div>
          <div class="video-duration">${duration}</div>
        </div>
        ${video.hot ? '<div class="hot-badge">HOT</div>' : ''}
      </div>
      <div class="video-info" data-id="${video.videoId}">
        <h3 class="video-title">${video.title}</h3>
        <p class="video-description">${video.description || ''}</p>
        <div class="video-meta">
          <span class="video-date">${publishedDate}</span>
          <span class="video-views">조회수 ${formatViews(video.viewCount)}회</span>
        </div>
      </div>
    </div>
  `;

  // 비디오 마우스 오버 이벤트 초기화
  initVideoHoverPreview();

  // 썸네일 클릭 시 비디오 상세 페이지로 이동
  const videoThumbnail = hotTopicSection.querySelector('.video-thumbnail');
  if (videoThumbnail) {
    videoThumbnail.addEventListener('click', () => {
      const videoId = videoThumbnail.dataset.id;
      // 상세 페이지로 이동
      window.location.href = `/original/videoDetail.html?id=${videoId}`;
    });
  }

  // 비디오 정보 영역(제목, 설명 등) 클릭 시 상세 페이지로 이동
  const videoInfo = hotTopicSection.querySelector('.video-info');
  if (videoInfo) {
    videoInfo.addEventListener('click', () => {
      const videoId = videoInfo.dataset.id;
      window.location.href = `/original/videoDetail.html?id=${videoId}`;
    });
  }

  // 더보기 링크 이벤트 초기화
  initVideoMoreLink();
}

/**
 * 비디오 섹션 '더보기' 링크 클릭 시 리스트 페이지로 이동
 */
function initVideoMoreLink() {
  const section = document.querySelectorAll('.news-section')[0]; // 첫 번째 섹션 (비디오 섹션)
  const moreLink = section?.querySelector('.more-link');
  if (moreLink) {
    moreLink.addEventListener('click', e => {
      e.preventDefault();
      window.location.href = '/original/originalContent.html'; // 비디오 리스트 페이지 경로
    });
  }
}

/**
 * 비디오 마우스 오버 미리보기 초기화
 */
function initVideoHoverPreview() {
  const videoContainer = document.querySelector('.video-thumbnail');
  const thumbnailImg = videoContainer?.querySelector('.thumbnail-img');
  const videoElement = videoContainer?.querySelector('.hover-video');

  if (!videoContainer || !thumbnailImg || !videoElement) {
    return;
  }

  // 호버 타이머 (지연 시작용)
  let hoverTimer;
  // 미리보기 타이머 (자동 종료용)
  let previewTimer;
  // 미리보기 시작 여부
  let previewStarted = false;

  // 데스크탑: mouseenter 이벤트 - 지연 시작
  videoContainer.addEventListener('mouseenter', function () {
    // 0.8초 지연 후 미리보기 시작
    hoverTimer = setTimeout(() => {
      startVideoPreview();
    }, 800);
  });

  // 모바일: touchstart 이벤트 - 길게 누르기
  let touchTimer;
  let touchStartY;
  let touchStartTime;

  videoContainer.addEventListener('touchstart', function (e) {
    touchStartY = e.touches[0].clientY;
    touchStartTime = Date.now();
    previewStarted = false;

    // 1초 동안 길게 터치하면 미리보기 시작 (시간 증가)
    touchTimer = setTimeout(() => {
      previewStarted = true;
      startVideoPreview();
    }, 1000); // 1초로 증가
  });

  // 모바일: 위로 스와이프 감지
  videoContainer.addEventListener('touchmove', function (e) {
    const currentY = e.touches[0].clientY;
    const deltaY = touchStartY - currentY;

    // 위로 25px 이상 스와이프하면 미리보기 시작 (거리 증가)
    if (deltaY > 25) {
      clearTimeout(touchTimer); // 기존 터치 타이머 취소
      if (!previewStarted) {
        previewStarted = true;
        e.preventDefault(); // 스와이프로 미리보기 시작할 때만 기본 동작 방지
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
  videoContainer.addEventListener('mouseleave', function () {
    clearTimeout(hoverTimer); // 호버 타이머 취소
    stopVideoPreview();
  });

  // 모바일: touchend 이벤트
  videoContainer.addEventListener('touchend', function (e) {
    clearTimeout(touchTimer); // 터치 타이머 취소

    // 터치 지속 시간이 짧으면(300ms 미만) 클릭으로 간주
    const touchEndTime = Date.now();
    const touchDuration = touchEndTime - touchStartTime;

    if (previewStarted) {
      // 미리보기가 시작된 상태면 멈추고 클릭 이벤트 방지
      e.preventDefault();
      stopVideoPreview();
    } else if (touchDuration < 300) {
      // 짧은 터치는 클릭 이벤트 그대로 통과 (기본 동작 유지)
      // 아무 것도 하지 않음
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

  // 포커스 이벤트 (키보드 접근성)
  videoContainer.setAttribute('tabindex', '0'); // 포커스 가능하도록 설정
  videoContainer.addEventListener('focus', startVideoPreview);
  videoContainer.addEventListener('blur', stopVideoPreview);
}

/**
 * 시간(초)을 mm:ss 형식으로 변환
 * @param {number} seconds - 초 단위 시간
 * @returns {string} 포맷된 시간 문자열
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

/**
 * 날짜 문자열을 포맷팅
 * @param {string} dateString - ISO 형식 날짜 문자열
 * @returns {string} 포맷된 날짜 (YYYY.MM.DD)
 */
function formatDate(dateString) {
  if (!dateString) {
    return '';
  }

  const date = new Date(dateString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');

  return `${year}.${month}.${day}`;
}

/**
 * 조회수 숫자를 보기 좋은 형식으로 변환
 * @param {number} count - 조회수
 * @returns {string} 포맷된 조회수
 */
function formatViews(count) {
  if (!count) {
    return '0';
  }

  if (count >= 10000) {
    return `${Math.floor(count / 10000)}만`;
  } else if (count >= 1000) {
    return `${Math.floor(count / 1000)}천`;
  }

  return count.toString();
}

/**
 * Nice 인증 서버로 POST할 form을 생성하여 제출
 * @param {string} requestUrl - Nice 인증 서버 URL (data.response.requestUrl)
 * @param {object} encodeData   - { enc_data, token_version_id, integrity_value, m }
 */
function submitFormToNice(requestUrl, encodeData) {
  const form = document.createElement('form');
  form.method = 'POST';
  form.action = requestUrl;
  form.style.display = 'none';

  Object.entries(encodeData).forEach(([key, value]) => {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = key;
    input.value = value;
    form.appendChild(input);
  });

  document.body.appendChild(form);
  form.submit();
}
