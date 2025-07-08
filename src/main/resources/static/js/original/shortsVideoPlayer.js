import {
  optionalAuthFetch,
  handleFetchError
} from '../commonFetch.js';

// 전역 변수
let videoData = null;
let videoPlayer = null;
let isPlaying = false;
let isYouTubeVideo = false;

// 자동재생 관련 변수
let userHasInteracted = false;
let autoplayQueue = [];

// URL 정리 함수 - type 파라미터 제거
(function cleanupUrl() {
  console.log('URL 정리 시작:', window.location.href);

  const urlParams = new URLSearchParams(window.location.search);
  let urlChanged = false;

  // type 파라미터 제거
  if (urlParams.has('type')) {
    console.log('⚠️ type 파라미터 발견 및 제거:', urlParams.get('type'));
    urlParams.delete('type');
    urlChanged = true;
  }

  // URL 업데이트
  if (urlChanged) {
    const cleanUrl = urlParams.toString()
      ? `${window.location.pathname}?${urlParams.toString()}`
      : window.location.pathname;

    window.history.replaceState({}, '', cleanUrl);
    console.log('✅ URL 정리 완료:', cleanUrl);
  }
})();

// 문서 로드 완료 시 실행
document.addEventListener('DOMContentLoaded', function () {
  // 요소 참조
  videoPlayer = document.getElementById('videoPlayer');

  // 초기화
  initBackButton();
  detectUserInteraction();
  loadVideoData();
  initMuteToggle();
});

/**
 * 사용자 상호작용 감지 및 자동재생 활성화
 */
function detectUserInteraction() {
  const events = ['click', 'touchstart', 'keydown'];

  function handleFirstInteraction() {
    userHasInteracted = true;
    console.log('사용자 상호작용 감지됨 - 자동재생 활성화');

    // 대기 중인 자동재생 실행
    autoplayQueue.forEach(callback => callback());
    autoplayQueue = [];

    // 이벤트 리스너 제거
    events.forEach(event => {
      document.removeEventListener(event, handleFirstInteraction);
    });
  }

  events.forEach(event => {
    document.addEventListener(event, handleFirstInteraction, { once: true });
  });
}

/**
 * Intersection Observer를 이용한 화면 진입 시 자동재생 -> 조회수 증가하지 않아서 썸네일 만 노출로 변경
 */
// function setupAutoplayObserver(iframe, thumbnailContainer, embedUrl, youtubeId) {
//   if (!('IntersectionObserver' in window)) {
//     setTimeout(() => {
//       tryAutoPlayYouTube(iframe, thumbnailContainer, embedUrl, youtubeId);
//     }, 1000);
//     return;
//   }
//
//   const observer = new IntersectionObserver((entries) => {
//     entries.forEach(entry => {
//       if (entry.isIntersecting && entry.intersectionRatio > 0.3) {
//         console.log('비디오가 화면에 보임 - 자동재생 시도');
//
//         if (userHasInteracted) {
//           tryAutoPlayYouTube(iframe, thumbnailContainer, embedUrl, youtubeId);
//         } else {
//           autoplayQueue.push(() => {
//             tryAutoPlayYouTube(iframe, thumbnailContainer, embedUrl, youtubeId);
//           });
//
//           setTimeout(() => {
//             tryAutoPlayYouTube(iframe, thumbnailContainer, embedUrl, youtubeId);
//           }, 500);
//         }
//
//         observer.unobserve(entry.target);
//       }
//     });
//   }, {
//     threshold: 0.3
//   });
//
//   const videoWrapper = document.querySelector('.video-wrapper');
//   if (videoWrapper) {
//     observer.observe(videoWrapper);
//   }
// }

/**
 * YouTube 자동재생 시도 - 조회수 증가하지 않기 때문에 주석 처리
 */
// function tryAutoPlayYouTube(iframe, thumbnailContainer, embedUrl, youtubeId) {
//   console.log('YouTube 자동재생 시도');
//
//   const videoWrapper = document.querySelector('.video-wrapper');
//
//   try {
//     if (thumbnailContainer) {
//       thumbnailContainer.classList.add('auto-playing');
//     }
//
//     hidePlayOverlay();
//
//     if (iframe) {
//       iframe.style.display = 'block';
//
//       const loadingTimeout = setTimeout(() => {
//         console.log('YouTube 로딩 타임아웃 - 로딩 상태 해제');
//         if (videoWrapper) {
//           videoWrapper.classList.remove('loading');
//         }
//         hideLoading();
//       }, 5000);
//
//       iframe.onload = function() {
//         console.log('YouTube iframe 로드 완료');
//         clearTimeout(loadingTimeout);
//
//         if (videoWrapper) {
//           videoWrapper.classList.remove('loading');
//           videoWrapper.classList.add('auto-playing');
//         }
//         hideLoading();
//
//         if (thumbnailContainer) {
//           thumbnailContainer.style.display = 'none';
//         }
//
//         isPlaying = true;
//       };
//
//       iframe.onerror = function() {
//         console.log('YouTube iframe 로드 실패 - 썸네일 표시');
//         clearTimeout(loadingTimeout);
//         handleAutoplayFailure(thumbnailContainer, videoWrapper);
//       };
//
//       console.log('YouTube 자동재생 iframe 설정 완료:', iframe.src);
//     }
//   } catch (error) {
//     console.log('YouTube 자동재생 실패:', error);
//     handleAutoplayFailure(thumbnailContainer, videoWrapper);
//   }
// }

/**
 * 자동재생 실패 시 처리
 */
function handleAutoplayFailure(thumbnailContainer, videoWrapper) {
  if (videoWrapper) {
    videoWrapper.classList.remove('loading', 'auto-playing');
  }
  hideLoading();

  if (thumbnailContainer) {
    thumbnailContainer.classList.remove('auto-playing');
    thumbnailContainer.style.display = 'flex';
  }

  const playOverlay = document.getElementById('playOverlay');
  if (playOverlay) {
    playOverlay.classList.add('autoplay-failed');
    showPlayOverlay();

    setTimeout(() => {
      playOverlay.classList.remove('autoplay-failed');
    }, 3000);
  }
}

/**
 * 뒤로가기 버튼 초기화
 */
function initBackButton() {
  const backButton = document.getElementById('backButton');
  if (backButton) {
    backButton.addEventListener('click', function () {
      if (window.history.length > 1) {
        window.history.back();
      } else {
        let tabType = 'shorts';
        if (videoData && videoData.type) {
          tabType = videoData.type === 'YOUTUBE_SHORTS' ? 'shorts' : 'video';
        }
        sessionStorage.setItem('currentContentTab', tabType);
        window.location.href = '/original/originalContent.html';
      }
    });
  }
}

/**
 * URL에서 비디오 ID 추출
 */
function getVideoIdFromUrl() {
  const urlParams = new URLSearchParams(window.location.search);
  return urlParams.get('id');
}

/**
 * 유튜브 동영상 ID 추출 함수
 */
function extractYouTubeId(url) {
  if (!url) return null;

  const cleanInput = url.trim();

  try {
    // 이미 ID만 있는 경우 (11자리)
    if (cleanInput.length === 11 && /^[a-zA-Z0-9_-]{11}$/.test(cleanInput)) {
      return cleanInput;
    }

    // 다양한 YouTube URL 패턴 처리
    const patterns = [
      /(?:youtube\.com\/watch\?v=)([a-zA-Z0-9_-]{11})/,
      /(?:youtu\.be\/)([a-zA-Z0-9_-]{11})/,
      /(?:youtube\.com\/embed\/)([a-zA-Z0-9_-]{11})/,
      /(?:m\.youtube\.com\/watch\?v=)([a-zA-Z0-9_-]{11})/,
      /(?:youtube\.com\/shorts\/)([a-zA-Z0-9_-]{11})/,
      /(?:youtu\.be\/shorts\/)([a-zA-Z0-9_-]{11})/
    ];

    for (const pattern of patterns) {
      const match = cleanInput.match(pattern);
      if (match && match[1]) {
        return match[1];
      }
    }
  } catch (error) {
    console.error('YouTube ID 추출 실패:', error);
  }

  return null;
}

/**
 * YouTube URL이 Shorts인지 확인
 */
function isYouTubeShorts(url) {
  if (!url) return false;
  const cleanUrl = url.trim().toLowerCase();
  return cleanUrl.includes('youtube.com/shorts/') || cleanUrl.includes('youtu.be/shorts/');
}

/**
 * YouTube embed URL 생성
 */
function createEmbedUrl(youtubeId) {
  if (!youtubeId) return null;
  return `https://www.youtube.com/embed/${youtubeId}`;
}

/**
 * YouTube iframe 생성
 */
function createYouTubeIframe(youtubeId, embedUrl, isShorts) {
  const youtubeIframe = document.createElement('iframe');
  youtubeIframe.id = 'youtubePlayer';
  youtubeIframe.className = 'youtube-player';

  const params = new URLSearchParams({
    rel: '0',
    modestbranding: '1',
    controls: '1',
    loop: '1',
    playlist: youtubeId,
    playsinline: '1',
    // autoplay: '1',  // 제거 - 자동재생 비활성화 (자동 재생시 조회수가 증가 하지 않음)
    // mute: '1',      // 제거 - 음소거 비활성화
    enablejsapi: '1'
  });

  if (isShorts) {
    params.set('iv_load_policy', '3');
    params.set('cc_load_policy', '0');
  }

  youtubeIframe.src = `${embedUrl}?${params.toString()}`;
  youtubeIframe.frameBorder = '0';
  youtubeIframe.allow = 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share';
  youtubeIframe.allowFullscreen = true;
  youtubeIframe.style.display = 'none';
  youtubeIframe.style.position = 'absolute';
  youtubeIframe.style.top = '0';
  youtubeIframe.style.left = '0';
  youtubeIframe.style.width = '100%';
  youtubeIframe.style.height = '100%';

  if (isShorts) {
    youtubeIframe.style.objectFit = 'cover';
    youtubeIframe.classList.add('youtube-shorts');

    const videoWrapper = document.querySelector('.video-wrapper');
    if (videoWrapper) {
      videoWrapper.classList.add('youtube-shorts-mode');
    }
  } else {
    youtubeIframe.style.objectFit = 'contain';
    youtubeIframe.classList.add('youtube-video');

    const videoWrapper = document.querySelector('.video-wrapper');
    if (videoWrapper) {
      videoWrapper.classList.add('youtube-video-mode');
    }
  }

  return youtubeIframe;
}

/**
 * 동영상 타입 판별 함수
 */
function determineVideoType(data) {
  console.log('비디오 타입 판별 시작:', data);

  // type이 YouTube 관련이면서 youtubeId가 존재하는 경우만 YouTube로 판단
  if (data.type && (data.type === 'YOUTUBE_VIDEO' || data.type === 'YOUTUBE_SHORTS')) {
    if (data.youtubeId || data.youtubeVideoId) {
      console.log('✅ YouTube 비디오 (type + youtubeId):', data.type, data.youtubeId);
      return true;
    } else {
      console.log('✅ 직접 업로드 비디오 (type은 YouTube이지만 youtubeId 없음):', data.type);
      return false;
    }
  }

  console.log('✅ 일반 업로드 비디오 (type:', data.type + ')');
  return false;
}

/**
 * Shorts 여부 판별
 */
function determineIfShorts(data) {
  // 1. type 필드 우선 확인
  if (data.type === 'YOUTUBE_SHORTS') {
    return true;
  }

  // 2. URL에서 확인
  const urlToCheck = data.videoUrl;
  if (urlToCheck && isYouTubeShorts(urlToCheck)) {
    return true;
  }

  return false;
}

/**
 * 비디오 데이터 로드
 */
async function loadVideoData() {
  const videoId = getVideoIdFromUrl();
  if (!videoId) {
    console.error('❌ 비디오 ID가 없습니다.');
    alert('잘못된 접근입니다.');
    return window.location.href = '/original/originalContent.html';
  }

  try {
    showLoading();

    // videoID에서 불필요한 파라미터 제거
    const cleanVideoId = videoId.toString().split('?')[0];

    // type 파라미터 없이 깔끔한 API 호출
    const apiUrl = `/api/videos/${cleanVideoId}`;
    console.log('📡 API 호출:', apiUrl);

    const response = await optionalAuthFetch(apiUrl);
    const data = await response.json();

    if (!data?.response) {
      throw new Error('Invalid video data');
    }

    videoData = data.response;
    console.log('✅ 비디오 데이터 로드 성공:', videoData);

    // 유튜브 영상인지 판별
    isYouTubeVideo = determineVideoType(videoData);
    console.log('📱 YouTube 비디오 여부:', isYouTubeVideo);

    // UI 업데이트는 상호작용 JS에서 처리
    if (window.updateVideoUI) {
      window.updateVideoUI(videoData, isYouTubeVideo);
    }

    // 플레이어 초기화
    if (isYouTubeVideo) {
      console.log('🎬 YouTube 플레이어 초기화');
      initYouTubePlayer(videoData);
    } else {
      console.log('🎬 일반 비디오 플레이어 초기화');
      initVideoPlayer();
    }

  } catch (error) {
    console.error('❌ 비디오 데이터 로드 오류:', error);
    handleFetchError(error);
  } finally {
    hideLoading();
  }
}

/**
 * 유튜브 플레이어 초기화
 */
function initYouTubePlayer(data) {
  const videoWrapper = document.querySelector('.video-wrapper');
  const thumbnailContainer = document.getElementById('thumbnailContainer');
  const videoPlayerElement = document.getElementById('videoPlayer');
  const muteToggleButton = document.getElementById('muteToggleButton');
  const playOverlay = document.getElementById('playOverlay');

  // 유튜브 ID 추출
  let youtubeId = null;

  if (data.youtubeId) {
    youtubeId = data.youtubeId;
  } else if (data.youtubeVideoId) {
    youtubeId = data.youtubeVideoId;
  } else if (data.videoUrl) {
    youtubeId = extractYouTubeId(data.videoUrl);
  } else if (data.videoId && data.videoId.length === 11) {
    youtubeId = data.videoId;
  }

  if (!youtubeId) {
    console.error('유튜브 ID를 찾을 수 없습니다. 데이터:', data);
    hideLoading();
    return;
  }

  console.log('YouTube ID 추출 성공:', youtubeId);

  // 기존 요소들 숨기기
  if (videoPlayerElement) {
    videoPlayerElement.style.display = 'none';
  }
  if (muteToggleButton) {
    muteToggleButton.style.display = 'none';
  }
  if (thumbnailContainer) {
    thumbnailContainer.style.display = 'none'; // 썸네일 숨김
  }
  if (playOverlay) {
    playOverlay.style.display = 'none'; // 재생 오버레이 숨김
  }

  // Shorts 여부 확인
  const isShorts = determineIfShorts(data);
  console.log('Shorts 여부:', isShorts, 'Type:', data.type);

  // embed URL 생성
  const embedUrl = createEmbedUrl(youtubeId);
  if (!embedUrl) {
    console.error('Embed URL 생성 실패');
    hideLoading();
    return;
  }

  // 유튜브 iframe 생성 - 자동재생 없음
  const youtubeIframe = document.createElement('iframe');
  youtubeIframe.id = 'youtubePlayer';
  youtubeIframe.className = 'youtube-player';

  const params = new URLSearchParams({
    rel: '0',
    modestbranding: '1',
    controls: '1',
    loop: '1',
    playlist: youtubeId,
    playsinline: '1',
    autoplay: '0',  // 자동재생 비활성화
    // mute 파라미터 제거 (사용자가 직접 재생)
    enablejsapi: '1'
  });

  if (isShorts) {
    params.set('iv_load_policy', '3');
    params.set('cc_load_policy', '0');
    youtubeIframe.classList.add('youtube-shorts');
    youtubeIframe.style.objectFit = 'cover';
    if (videoWrapper) {
      videoWrapper.classList.add('youtube-shorts-mode');
    }
  } else {
    youtubeIframe.classList.add('youtube-video');
    youtubeIframe.style.objectFit = 'contain';
    if (videoWrapper) {
      videoWrapper.classList.add('youtube-video-mode');
    }
  }

  youtubeIframe.src = `${embedUrl}?${params.toString()}`;
  youtubeIframe.frameBorder = '0';
  youtubeIframe.allow = 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share';
  youtubeIframe.allowFullscreen = true;
  youtubeIframe.style.position = 'absolute';
  youtubeIframe.style.top = '0';
  youtubeIframe.style.left = '0';
  youtubeIframe.style.width = '100%';
  youtubeIframe.style.height = '100%';
  youtubeIframe.style.display = 'block'; // 바로 표시

  // DOM에 추가
  if (videoWrapper) {
    videoWrapper.appendChild(youtubeIframe);
  }

  hideLoading();
  console.log('✅ YouTube iframe 바로 로드 완료 - 조회수 증가 보장');
}

/**
 * 유튜브 재생 시작 - 조회수 증가 목적으로 진입시 부터 Iframe을 생성하기 때문에 해당 기능 주석
 * 기존 방식 자체 썸네일 제시(+ 재생버튼) -> iframe 생성 -> 유튜브 재생  ==> 재생 버튼 두번
 */
// function startYouTubePlayback(iframe, thumbnailContainer, embedUrl, youtubeId) {
//   console.log('YouTube 수동 재생 시작:', { embedUrl, youtubeId });
//
//   hideLoading();
//   const videoWrapper = document.querySelector('.video-wrapper');
//   if (videoWrapper) {
//     videoWrapper.classList.remove('loading');
//   }
//
//   // 썸네일 숨기기
//   if (thumbnailContainer) {
//     thumbnailContainer.style.display = 'none';
//   }
//
//   // 재생 오버레이 숨기기
//   hidePlayOverlay();
//
//   // 유튜브 iframe 표시
//   if (iframe) {
//     iframe.style.display = 'block';
//     console.log('YouTube iframe 표시됨:', iframe.src);
//   }
//
//   isPlaying = true;
// }

/**
 * 일반 비디오 플레이어 초기화
 */
function initVideoPlayer() {
  const thumbnailContainer = document.getElementById('thumbnailContainer');
  const playOverlay = document.getElementById('playOverlay');

  console.log('일반 비디오 플레이어 초기화 시작:', videoData);

  // 썸네일 설정
  const thumbnailElement = document.getElementById('videoThumbnail');
  if (thumbnailElement && videoData) {
    const thumbnailUrl = videoData.thumbnailUrl || '/images/default-thumbnail.jpg';
    thumbnailElement.src = thumbnailUrl;
    thumbnailElement.alt = videoData.title;
    console.log('썸네일 설정:', thumbnailUrl);
  }

  // 비디오 소스 설정
  if (videoPlayer && videoData) {
    const videoUrl = videoData.videoUrl || videoData.url || videoData.src;
    if (videoUrl) {
      videoPlayer.src = videoUrl;
      videoPlayer.poster = videoData.thumbnailUrl;
      console.log('비디오 소스 설정:', videoUrl);
    } else {
      console.error('비디오 URL을 찾을 수 없습니다:', videoData);
      return;
    }
  }

  // 썸네일 클릭 시 비디오 재생
  if (thumbnailContainer) {
    thumbnailContainer.addEventListener('click', startPlayback);
  }

  // 재생 버튼 클릭 시 비디오 재생
  if (playOverlay) {
    playOverlay.addEventListener('click', startPlayback);
  }

  // 비디오 이벤트 리스너 등록
  if (videoPlayer) {
    // 자동 재생을 위한 속성 설정
    videoPlayer.muted = true;
    videoPlayer.loop = true;
    videoPlayer.autoplay = true;

    // 비디오 클릭 시 재생/일시정지
    videoPlayer.addEventListener('click', togglePlayPause);

    // 재생 시작 시
    videoPlayer.addEventListener('play', function () {
      console.log('비디오 재생 시작');
      isPlaying = true;
      hidePlayOverlay();
    });

    // 일시 정지 시
    videoPlayer.addEventListener('pause', function () {
      console.log('비디오 일시정지');
      isPlaying = false;
      showPlayOverlay();
    });

    // 재생 완료 시
    videoPlayer.addEventListener('ended', function () {
      console.log('비디오 재생 완료');
      if (!videoPlayer.loop) {
        videoPlayer.currentTime = 0;
        videoPlayer.play();
      }
    });

    // 로딩 시작
    videoPlayer.addEventListener('loadstart', function() {
      console.log('비디오 로딩 시작');
      showLoading();
    });

    // 재생 가능할 때 - 자동 재생 시도
    videoPlayer.addEventListener('canplay', function() {
      console.log('비디오 재생 가능');
      hideLoading();
      autoPlayVideo();
    });

    // 메타데이터 로드 완료 시에도 자동 재생 시도
    videoPlayer.addEventListener('loadedmetadata', function() {
      console.log('비디오 메타데이터 로드 완료');
      autoPlayVideo();
    });

    // 에러 처리
    videoPlayer.addEventListener('error', function(e) {
      console.error('비디오 로드 에러:', e);
      hideLoading();
      alert('동영상을 불러올 수 없습니다.');
    });
  }
}

/**
 * 자동 재생 시도 (직접 업로드 영상만)
 */
function autoPlayVideo() {
  if (!videoPlayer || isPlaying || isYouTubeVideo) return;

  // 썸네일 숨기고 비디오 표시
  const thumbnailContainer = document.getElementById('thumbnailContainer');
  if (thumbnailContainer) {
    thumbnailContainer.style.display = 'none';
  }

  if (videoPlayer) {
    videoPlayer.style.display = 'block';

    // 자동 재생 시도
    const playPromise = videoPlayer.play();

    if (playPromise !== undefined) {
      playPromise
        .then(() => {
          console.log('자동 재생 시작됨');
          isPlaying = true;
          hidePlayOverlay();
          updateMuteToggleButton();
        })
        .catch(error => {
          console.log('자동 재생 실패, 사용자 상호작용 필요:', error);
          showThumbnail();
          showPlayOverlay();
        });
    }
  }
}

/**
 * 재생 시작 (썸네일 클릭 시 - 직접 업로드 영상)
 */
function startPlayback(event) {
  event.preventDefault();
  event.stopPropagation();

  if (isYouTubeVideo) return;

  const thumbnailContainer = document.getElementById('thumbnailContainer');

  // 썸네일 숨기고 비디오 표시
  if (thumbnailContainer) {
    thumbnailContainer.style.display = 'none';
  }

  // 비디오 표시 및 재생
  if (videoPlayer) {
    videoPlayer.style.display = 'block';

    // 사용자가 직접 재생하는 경우 음소거 해제
    videoPlayer.muted = false;
    updateMuteToggleButton();

    videoPlayer.play()
      .then(() => {
        isPlaying = true;
        hidePlayOverlay();
      })
      .catch(error => {
        console.error('비디오 재생 실패:', error);
        alert('비디오 재생을 시작할 수 없습니다.');
        showThumbnail();
      });
  }
}

/**
 * 재생/일시정지 토글 (직접 업로드 영상만)
 */
function togglePlayPause(event) {
  event.preventDefault();
  event.stopPropagation();

  if (!videoPlayer || isYouTubeVideo) return;

  if (isPlaying) {
    videoPlayer.pause();
  } else {
    videoPlayer.play();
  }
}

/**
 * 썸네일 표시
 */
function showThumbnail() {
  const thumbnailContainer = document.getElementById('thumbnailContainer');
  const youtubePlayer = document.getElementById('youtubePlayer');

  // 유튜브 플레이어 숨기기
  if (youtubePlayer) {
    youtubePlayer.style.display = 'none';
  }

  // 일반 비디오 플레이어 숨기기
  if (videoPlayer && !isYouTubeVideo) {
    videoPlayer.style.display = 'none';
  }

  // 썸네일 표시
  if (thumbnailContainer) {
    thumbnailContainer.style.display = 'flex';
  }
}

/**
 * 재생 오버레이 표시/숨김
 */
function showPlayOverlay() {
  const playOverlay = document.getElementById('playOverlay');
  if (playOverlay) {
    playOverlay.classList.remove('hidden');
  }
}

function hidePlayOverlay() {
  const playOverlay = document.getElementById('playOverlay');
  if (playOverlay) {
    playOverlay.classList.add('hidden');
  }
}

/**
 * 로딩 스피너 표시/숨김
 */
function showLoading() {
  const loadingSpinner = document.getElementById('loadingSpinner');
  if (loadingSpinner) {
    loadingSpinner.style.display = 'block';
  }
}

function hideLoading() {
  const loadingSpinner = document.getElementById('loadingSpinner');
  if (loadingSpinner) {
    loadingSpinner.style.display = 'none';
  }
}

/**
 * 음소거 토글 버튼 초기화 (직접 업로드 영상만)
 */
function initMuteToggle() {
  const muteToggleButton = document.getElementById('muteToggleButton');

  if (muteToggleButton) {
    muteToggleButton.addEventListener('click', function() {
      if (!videoPlayer || isYouTubeVideo) return;

      videoPlayer.muted = !videoPlayer.muted;
      updateMuteToggleButton();
    });

    updateMuteToggleButton();
  }
}

/**
 * 음소거 토글 버튼 UI 업데이트 (직접 업로드 영상만)
 */
function updateMuteToggleButton() {
  const muteToggleButton = document.getElementById('muteToggleButton');

  if (muteToggleButton) {
    if (isYouTubeVideo) {
      muteToggleButton.style.display = 'none';
      return;
    }

    muteToggleButton.style.display = 'flex';

    if (videoPlayer) {
      const icon = muteToggleButton.querySelector('i');

      if (videoPlayer.muted) {
        muteToggleButton.classList.add('muted');
        icon.className = 'fas fa-volume-mute';
      } else {
        muteToggleButton.classList.remove('muted');
        icon.className = 'fas fa-volume-up';
      }
    }
  }
}

// 전역 함수로 내보내기 (다른 모듈에서 사용할 수 있도록)
window.getVideoIdFromUrl = getVideoIdFromUrl;
window.videoData = () => videoData;
window.isYouTubeVideo = () => isYouTubeVideo;