import {
  optionalAuthFetch,
  authFetch,
  handleFetchError,
  FetchError, getUserId
} from '../commonFetch.js';

// 전역 변수
let videoData = null;
let videoPlayer = null;
let progressUpdateInterval = null;
let isPlaying = false;
let isMuted = false;
let isFullscreen = false;
let shareModal = null;
let videoType = null; // API 호출용 (사용하지 않음)
let shareVideoType = null; // 공유 기능용
let isYouTubeVideo = false;

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
  // 비디오 플레이어 요소 참조
  videoPlayer = document.getElementById('videoPlayer');
  // 뒤로가기 버튼 초기화
  initBackButton();
  // 비디오 데이터 로드
  loadVideoData();
  // 공유 버튼 초기화
  initShareFeatures();
  // 댓글 영역은 별도로 처리
  initComments();
});

/**
 * 뒤로가기 버튼 초기화
 */
function initBackButton() {
  const backButton = document.getElementById('backButton');
  if (backButton) {
    backButton.addEventListener('click', function () {
      // 이전 페이지로 이동 (히스토리가 있는 경우)
      if (window.history.length > 1) {
        window.history.back();
      } else {
        // 히스토리가 없는 경우, videoData를 확인하기 전에 기본값으로 'video' 탭 설정
        let tabType = 'video';

        // videoData가 로드된 경우에만 콘텐츠 타입 확인
        if (videoData && videoData.type) {
          tabType = videoData.type === 'YOUTUBE_SHORTS' ? 'shorts' : 'video';
        }

        // 현재 탭 정보 저장
        sessionStorage.setItem('currentContentTab', tabType);

        // 목록 페이지로 이동
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

  const regex = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([^"&?\/\s]{11})/;
  const match = url.match(regex);
  return match ? match[1] : null;
}

/**
 * 동영상 타입 판별 함수 (YouTube vs 직접 업로드)
 */
function determineVideoType(data) {
  console.log('비디오 타입 판별 시작:', data);

  // type이 YouTube 관련이면서 youtubeId가 존재하는 경우만 YouTube로 판단
  if (data.type && (data.type === 'YOUTUBE_VIDEO' || data.type === 'YOUTUBE_SHORTS')) {
    if (data.youtubeVideoId || data.youtubeId || extractYouTubeId(data.videoUrl)) {
      console.log('✅ YouTube 비디오 (type + youtubeId):', data.type);
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
 * 비디오 데이터 로드
 */
async function loadVideoData() {
  const videoId = getVideoIdFromUrl();
  if (!videoId) {
    console.error('비디오 ID가 없습니다.');
    alert('잘못된 접근입니다.');
    return window.location.href = '/original/originalContent.html';
  }

  try {
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

    // 유튜브 영상인지 판별
    isYouTubeVideo = determineVideoType(videoData);
    console.log('📱 YouTube 비디오 여부:', isYouTubeVideo);

    updateVideoUI(videoData);

    // 유튜브 영상이면 유튜브 플레이어 초기화, 아니면 기본 플레이어 초기화
    if (isYouTubeVideo) {
      console.log('🎬 YouTube 플레이어 초기화');
      initYouTubePlayer(videoData);
    } else {
      console.log('🎬 일반 비디오 플레이어 초기화');
      initVideoPlayer();
      initVideoControls();
    }

  } catch (error) {
    console.error('비디오 데이터 로드 오류:', error);
    handleFetchError(error);
  }
}

/**
 * 비디오 UI 업데이트
 */
function updateVideoUI(data) {
  // 공유용 비디오 타입 설정 (공유 API에서 사용)
  shareVideoType = data.type || (isYouTubeVideo ? 'YOUTUBE_VIDEO' : 'UPLOADED');

  // 타이틀 업데이트
  const titleElement = document.getElementById('videoTitle');
  if (titleElement) {
    titleElement.textContent = data.title;
  }

  // 날짜 업데이트
  const dateElement = document.getElementById('videoDate');
  if (dateElement) {
    dateElement.textContent = formatDate(data.publishedAt || data.createdAt);
  }

  // 조회수 업데이트
  const viewCountElement = document.getElementById('viewCount');
  if (viewCountElement) {
    viewCountElement.textContent = formatNumber(data.viewCount || 0);
  }

  // 댓글 수 업데이트
  const commentCountElement = document.getElementById('commentCount');
  if (commentCountElement) {
    commentCountElement.textContent = formatNumber(data.commentCount || 0);
  }

  // 설명 업데이트
  const descriptionElement = document.getElementById('videoDescription');
  if (descriptionElement) {
    descriptionElement.textContent = data.description || '설명이 없습니다.';
  }

  // 더보기 버튼 초기화
  initMoreButton();
}

/**
 * 유튜브 플레이어 초기화
 */
function initYouTubePlayer(data) {
  const videoWrapper = document.getElementById('videoWrapper');
  const thumbnailContainer = document.getElementById('thumbnailContainer');
  const videoPlayerElement = document.getElementById('videoPlayer');
  const videoControls = document.getElementById('videoControls');

  // 유튜브 ID 추출
  const youtubeId = data.youtubeVideoId || data.youtubeId || extractYouTubeId(data.videoUrl) || data.videoId;

  if (!youtubeId) {
    console.error('유튜브 ID를 찾을 수 없습니다.');
    return;
  }

  // 기존 video 요소와 컨트롤 숨김
  if (videoPlayerElement) {
    videoPlayerElement.style.display = 'none';
  }
  if (videoControls) {
    videoControls.style.display = 'none';
  }

  // 썸네일 설정
  const thumbnailElement = document.getElementById('videoThumbnail');
  if (thumbnailElement) {
    thumbnailElement.src = data.thumbnailUrl || `https://img.youtube.com/vi/${youtubeId}/hqdefault.jpg`;
    thumbnailElement.alt = data.title;
  }

  // 유튜브 iframe 생성
  const youtubeIframe = document.createElement('iframe');
  youtubeIframe.id = 'youtubePlayer';
  youtubeIframe.className = 'youtube-player';
  youtubeIframe.src = `https://www.youtube.com/embed/${youtubeId}?rel=0&modestbranding=1&controls=1`;
  youtubeIframe.frameBorder = '0';
  youtubeIframe.allow = 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share';
  youtubeIframe.allowFullscreen = true;
  youtubeIframe.style.display = 'none';
  youtubeIframe.style.position = 'absolute';
  youtubeIframe.style.top = '0';
  youtubeIframe.style.left = '0';
  youtubeIframe.style.width = '100%';
  youtubeIframe.style.height = '100%';

  // iframe을 wrapper에 추가
  if (videoWrapper) {
    videoWrapper.appendChild(youtubeIframe);
  }

  // 썸네일 클릭 이벤트
  if (thumbnailContainer) {
    thumbnailContainer.addEventListener('click', function() {
      startYouTubePlayback(youtubeIframe, thumbnailContainer);
    });
  }

  // 재생 버튼 클릭 이벤트
  const playButton = document.getElementById('playButton');
  if (playButton) {
    playButton.addEventListener('click', function() {
      startYouTubePlayback(youtubeIframe, thumbnailContainer);
    });
  }
}

/**
 * 유튜브 재생 시작
 */
function startYouTubePlayback(iframe, thumbnailContainer) {
  // 썸네일 숨기기
  if (thumbnailContainer) {
    thumbnailContainer.style.display = 'none';
  }

  // 유튜브 iframe 표시
  if (iframe) {
    iframe.style.display = 'block';

    // 자동 재생을 위해 src에 autoplay 파라미터 추가
    const currentSrc = iframe.src;
    if (!currentSrc.includes('autoplay=1')) {
      iframe.src = currentSrc + '&autoplay=1';
    }
  }
}

/**
 * 일반 비디오 플레이어 초기화 (직접 업로드 영상용)
 */
function initVideoPlayer() {
  const videoWrapper = document.getElementById('videoWrapper');
  const thumbnailContainer = document.getElementById('thumbnailContainer');
  const playButton = document.getElementById('playButton');

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
  if (playButton) {
    playButton.addEventListener('click', startPlayback);
  }

  // 비디오 이벤트 리스너 등록
  if (videoPlayer) {
    // 메타데이터 로드 완료 시
    videoPlayer.addEventListener('loadedmetadata', function () {
      console.log('비디오 메타데이터 로드 완료');
      updateTotalTime();
    });

    // 재생 시작 시
    videoPlayer.addEventListener('play', function () {
      console.log('비디오 재생 시작');
      isPlaying = true;
      updatePlayPauseButton();
      startProgressInterval();
    });

    // 일시 정지 시
    videoPlayer.addEventListener('pause', function () {
      console.log('비디오 일시정지');
      isPlaying = false;
      updatePlayPauseButton();
      stopProgressInterval();
    });

    // 재생 완료 시
    videoPlayer.addEventListener('ended', function () {
      console.log('비디오 재생 완료');
      isPlaying = false;
      updatePlayPauseButton();
      stopProgressInterval();
      showThumbnail();
    });

    // 볼륨 변경 시
    videoPlayer.addEventListener('volumechange', function () {
      updateVolumeUI();
    });

    // 에러 처리
    videoPlayer.addEventListener('error', function(e) {
      console.error('비디오 로드 에러:', e);
      alert('동영상을 불러올 수 없습니다.');
    });
  }
}

/**
 * 일반 비디오 재생 시작 (직접 업로드 영상용)
 */
function startPlayback(event) {
  event.preventDefault();

  const thumbnailContainer = document.getElementById('thumbnailContainer');
  const videoControls = document.getElementById('videoControls');

  // 썸네일 숨기고 비디오 표시
  if (thumbnailContainer) {
    thumbnailContainer.style.display = 'none';
  }

  // 비디오 표시 및 재생
  if (videoPlayer) {
    videoPlayer.style.display = 'block';
    videoPlayer.play()
    .then(() => {
      // 재생 성공
      isPlaying = true;

      // 컨트롤 표시
      if (videoControls) {
        videoControls.style.display = 'block';
      }

      // 재생/일시정지 버튼 업데이트
      updatePlayPauseButton();

      // 프로그레스 바 업데이트 시작
      startProgressInterval();
    })
    .catch(error => {
      console.error('비디오 재생 실패:', error);
      alert('비디오 재생을 시작할 수 없습니다.');
      showThumbnail();
    });
  }
}

/**
 * 썸네일 표시
 */
function showThumbnail() {
  const thumbnailContainer = document.getElementById('thumbnailContainer');
  const videoControls = document.getElementById('videoControls');
  const youtubePlayer = document.getElementById('youtubePlayer');

  // 유튜브 플레이어 숨기기
  if (youtubePlayer) {
    youtubePlayer.style.display = 'none';
  }

  // 일반 비디오 플레이어 숨기기
  if (videoPlayer) {
    videoPlayer.style.display = 'none';
  }

  // 썸네일 표시
  if (thumbnailContainer) {
    thumbnailContainer.style.display = 'block';
  }

  // 컨트롤 숨김 (일반 비디오만)
  if (videoControls && !isYouTubeVideo) {
    videoControls.style.display = 'none';
  }
}

/**
 * 비디오 컨트롤 초기화 (직접 업로드 영상만)
 */
function initVideoControls() {
  // 유튜브 영상이면 컨트롤 초기화하지 않음
  if (isYouTubeVideo) {
    return;
  }

  const playPauseButton = document.getElementById('playPauseButton');
  const muteButton = document.getElementById('muteButton');
  const volumeSlider = document.getElementById('volumeSlider');
  const progressBar = document.getElementById('progressBar');
  const fullscreenButton = document.getElementById('fullscreenButton');

  // 재생/일시정지 버튼
  if (playPauseButton) {
    playPauseButton.addEventListener('click', togglePlayPause);
  }

  // 음소거 버튼
  if (muteButton) {
    muteButton.addEventListener('click', toggleMute);
  }

  // 볼륨 슬라이더
  if (volumeSlider) {
    volumeSlider.addEventListener('input', function () {
      videoPlayer.volume = this.value;

      // 볼륨이 0이면 음소거 상태로 변경
      if (parseFloat(this.value) === 0) {
        videoPlayer.muted = true;
        isMuted = true;
      } else {
        videoPlayer.muted = false;
        isMuted = false;
      }

      updateVolumeUI();
    });
  }

  // 프로그레스 바
  if (progressBar) {
    progressBar.addEventListener('click', function (e) {
      const rect = this.getBoundingClientRect();
      const pos = (e.clientX - rect.left) / rect.width;

      if (videoPlayer && videoPlayer.duration) {
        // 비디오 시간 업데이트
        videoPlayer.currentTime = pos * videoPlayer.duration;
        updateProgress();
      }
    });
  }

  // 전체화면 버튼
  if (fullscreenButton) {
    fullscreenButton.addEventListener('click', toggleFullscreen);
  }
}

/**
 * 재생/일시정지 토글
 */
function togglePlayPause() {
  if (!videoPlayer) {
    return;
  }

  if (isPlaying) {
    videoPlayer.pause();
  } else {
    videoPlayer.play();
  }
}

/**
 * 재생/일시정지 버튼 업데이트
 */
function updatePlayPauseButton() {
  const playPauseButton = document.getElementById('playPauseButton');

  if (playPauseButton) {
    const icon = playPauseButton.querySelector('i');

    if (icon) {
      if (isPlaying) {
        icon.className = 'fas fa-pause';
      } else {
        icon.className = 'fas fa-play';
      }
    }
  }
}

/**
 * 음소거 토글
 */
function toggleMute() {
  if (!videoPlayer) {
    return;
  }

  videoPlayer.muted = !videoPlayer.muted;
  isMuted = videoPlayer.muted;

  updateVolumeUI();
}

/**
 * 볼륨 UI 업데이트
 */
function updateVolumeUI() {
  const muteButton = document.getElementById('muteButton');
  const volumeSlider = document.getElementById('volumeSlider');

  if (muteButton) {
    const icon = muteButton.querySelector('i');

    if (icon) {
      if (isMuted || videoPlayer.volume === 0) {
        icon.className = 'fas fa-volume-mute';
      } else if (videoPlayer.volume < 0.5) {
        icon.className = 'fas fa-volume-down';
      } else {
        icon.className = 'fas fa-volume-up';
      }
    }
  }

  if (volumeSlider) {
    volumeSlider.value = isMuted ? 0 : videoPlayer.volume;
  }
}

/**
 * 프로그레스 바 업데이트 인터벌 시작
 */
function startProgressInterval() {
  if (progressUpdateInterval) {
    clearInterval(progressUpdateInterval);
  }

  progressUpdateInterval = setInterval(updateProgress, 200);
}

/**
 * 프로그레스 바 업데이트 인터벌 중지
 */
function stopProgressInterval() {
  if (progressUpdateInterval) {
    clearInterval(progressUpdateInterval);
    progressUpdateInterval = null;
  }
}

/**
 * 프로그레스 바 업데이트
 */
function updateProgress() {
  if (!videoPlayer) {
    return;
  }

  const progressFill = document.getElementById('progressFill');
  const currentTimeElement = document.getElementById('currentTime');

  if (progressFill && videoPlayer.duration) {
    const percentage = (videoPlayer.currentTime / videoPlayer.duration) * 100;
    progressFill.style.width = `${percentage}%`;
  }

  if (currentTimeElement) {
    currentTimeElement.textContent = formatTime(videoPlayer.currentTime);
  }
}

/**
 * 총 시간 업데이트
 */
function updateTotalTime() {
  if (!videoPlayer) {
    return;
  }

  const totalTimeElement = document.getElementById('totalTime');

  if (totalTimeElement && videoPlayer.duration) {
    totalTimeElement.textContent = formatTime(videoPlayer.duration);
  }
}

/**
 * 전체화면 토글
 */
function toggleFullscreen() {
  const videoWrapper = document.getElementById('videoWrapper');

  if (!videoWrapper) {
    return;
  }

  if (!isFullscreen) {
    // 전체화면 진입
    videoWrapper.classList.add('fullscreen-video');

    if (videoWrapper.requestFullscreen) {
      videoWrapper.requestFullscreen();
    } else if (videoWrapper.webkitRequestFullscreen) {
      videoWrapper.webkitRequestFullscreen();
    } else if (videoWrapper.msRequestFullscreen) {
      videoWrapper.msRequestFullscreen();
    }

    // 버튼 아이콘 변경
    const fullscreenButton = document.getElementById('fullscreenButton');
    if (fullscreenButton) {
      const icon = fullscreenButton.querySelector('i');
      if (icon) {
        icon.className = 'fas fa-compress';
      }
    }

    isFullscreen = true;
  } else {
    // 전체화면 종료
    videoWrapper.classList.remove('fullscreen-video');

    if (document.exitFullscreen) {
      document.exitFullscreen();
    } else if (document.webkitExitFullscreen) {
      document.webkitExitFullscreen();
    } else if (document.msExitFullscreen) {
      document.msExitFullscreen();
    }

    // 버튼 아이콘 변경
    const fullscreenButton = document.getElementById('fullscreenButton');
    if (fullscreenButton) {
      const icon = fullscreenButton.querySelector('i');
      if (icon) {
        icon.className = 'fas fa-expand';
      }
    }

    isFullscreen = false;
  }
}

/**
 * 더보기 버튼 초기화
 */
function initMoreButton() {
  const moreButton = document.getElementById('moreButton');
  const descriptionElement = document.getElementById('videoDescription');

  if (moreButton && descriptionElement) {
    // 내용이 줄임 표시가 필요한지 확인
    const isOverflowing = descriptionElement.scrollHeight > descriptionElement.clientHeight;

    // 내용이 넘치는 경우에만 더보기 버튼 표시
    moreButton.style.display = isOverflowing ? 'block' : 'none';

    moreButton.addEventListener('click', function () {
      // 설명 영역 확장 토글
      descriptionElement.classList.toggle('expanded');

      // 버튼 텍스트 변경
      this.textContent = descriptionElement.classList.contains('expanded') ? '접기' : '더보기';
    });
  }
}

/**
 * 공유 버튼 초기화
 */
// 공유 기능 초기화: 모달 열기 & 버튼 연결
function initShareFeatures() {
  // Bootstrap Modal 인스턴스
  shareModal = new bootstrap.Modal(document.getElementById('shareModal'));

  document.getElementById('shareButton')?.addEventListener('click', () => {
    shareModal.show();
    setupKakaoShare();
  });

  document.getElementById('copyUrl')?.addEventListener('click', () => {
    copyCurrentUrl();
  });

  // 통통 공유 버튼 이벤트 추가
  document.getElementById('shareTongtong')?.addEventListener('click', () => {
    shareTongtongApp();
  });

  document.getElementById('shareX')?.addEventListener('click', () => {
    shareToX();
  });
}

/**
 * 통통 앱 공유 함수
 */
function shareTongtongApp() {
  const videoId = getVideoIdFromUrl();
  const shareUrl = `https://club1.newstomato.com/share/original/${videoId}`;

  // 모바일 기기 확인
  const userAgent = navigator.userAgent.toLowerCase();
  let appScheme = '';
  let storeUrl = '';

  // iOS 기기 확인
  if (/iphone|ipad|ipod/.test(userAgent)) {
    appScheme = `tongtongios://tongtongiOS?url=${encodeURIComponent(shareUrl)}`;
    storeUrl = 'https://apps.apple.com/kr/app/통통-암호화-메신저/id982895719';
  }
  // 안드로이드 기기 확인
  else if (/android/.test(userAgent)) {
    appScheme = `tongtong://m.etomato.com?url=${encodeURIComponent(shareUrl)}`;
    storeUrl = 'https://play.google.com/store/apps/details?id=tomato.solution.tongtong';
  }
  // 데스크톱 또는 기타 기기
  else {
    navigator.clipboard.writeText(shareUrl)
      .then(() => {
        alert('URL이 클립보드에 복사되었습니다. 통통 앱에서 공유해주세요.');
      })
      .catch(err => {
        console.error('클립보드 복사 오류:', err);
        alert('복사에 실패했습니다.');
      });
    shareModal.hide();
    return;
  }

  // URL 자동 복사 (앱으로 전환되기 전에)
  navigator.clipboard.writeText(shareUrl)
    .then(() => {
      console.log('URL이 클립보드에 복사되었습니다.');
    })
    .catch(err => {
      console.error('클립보드 복사 오류:', err);
    });

  // 앱 실행 시도 시간
  const appCheckTimeout = 1500;
  const now = Date.now();

  // 앱 스킴 호출
  window.location.href = appScheme;

  // 앱 실행 확인
  setTimeout(function() {
    // 페이지가 숨겨지지 않았다면 (앱이 실행되지 않았다면)
    if (document.hidden === false && Date.now() - now > appCheckTimeout) {
      if (confirm('통통 앱이 설치되어 있지 않은 것 같습니다. 앱 스토어로 이동하시겠습니까?')) {
        window.location.href = storeUrl;
      }
    }
    shareModal.hide();
  }, appCheckTimeout + 500);
}

// 카카오톡 기본공유 버튼 생성
async function setupKakaoShare() {
  const id = getVideoIdFromUrl();
  const userId = await getUserId();
  const title = document.getElementById('videoTitle')?.textContent || '동영상 공유';
  const description = document.getElementById('videoDescription')?.textContent?.slice(0, 100) || '';
  const shareUrl = `https://club1.newstomato.com/share/original/${id}`;
  const imageUrl = document.getElementById('videoThumbnail')?.src || '';

  const container = document.getElementById('kakaotalk-sharing-btn');
  container.innerHTML = `
    <img src="/images/kakao.svg" alt="카카오톡" width="32"/>
    <span class="share-label">카카오톡</span>
  `;

  if (window.Kakao && Kakao.isInitialized()) {
    Kakao.Share.createDefaultButton({
      container: '#kakaotalk-sharing-btn',
      objectType: 'feed',
      content: {
        title,
        description,
        imageUrl,
        link: { mobileWebUrl: shareUrl, webUrl: shareUrl }
      },
      serverCallbackArgs: {
        type: shareVideoType, // 공유용 타입 사용
        id: id,
        userId: userId
      }
    });
  } else {
    Kakao.init('277f475e199d62bca2ac85a09b0d3045');
    Kakao.Share.createDefaultButton({
      container: '#kakaotalk-sharing-btn',
      objectType: 'feed',
      content: {
        title,
        description,
        imageUrl,
        link: { mobileWebUrl: shareUrl, webUrl: shareUrl }
      },
      serverCallbackArgs: {
        type: shareVideoType, // 공유용 타입 사용
        id: id,
        userId: userId
      }
    });
  }
}

/**
 * X(구 Twitter) 공유 함수
 */
async function shareToX() {
  const title = document.querySelector('.video-title')?.textContent.trim() ||
               document.getElementById('videoTitle')?.textContent.trim() || '';
  const postId = getVideoIdFromUrl();
  const shareUrl = `https://club1.newstomato.com/share/original/${postId}`;

  try {
    const res = await authFetch('/twitter/share', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: title,
        shareUrl: shareUrl,
        type: shareVideoType.toUpperCase(), // 공유용 타입 사용
        targetId: postId
      })
    });
    await res.json(); // 서버 메시지 무시해도 OK
    alert('X에 성공적으로 공유되었습니다!');
  } catch (err) {
    if (err instanceof FetchError && err.httpStatus === 401) {
      alert('트위터 연동이 필요합니다.');
      window.location.href = '/mypage/mypage.html';
    } else {
      console.error('트위터 공유 실패:', err);
      handleFetchError(err);
    }
  } finally {
    shareModal.hide();
  }
}

// URL 복사 후 모달 닫기
function copyCurrentUrl() {
  const videoId = getVideoIdFromUrl();
  const shareUrl = `https://club1.newstomato.com/share/original/${videoId}`;
  navigator.clipboard.writeText(shareUrl)
    .then(() => {
      alert('URL이 클립보드에 복사되었습니다.');
      shareModal.hide();
    })
    .catch(err => {
      console.error('클립보드 복사 오류:', err);
      alert('복사에 실패했습니다.');
    });
}

/**
 * 댓글 영역 초기화
 */
function initComments() {
  const videoId = getVideoIdFromUrl();
  const container = document.querySelector('.comments-section');
  const list = document.getElementById('commentsContainer');
  const noCommentsEl = document.getElementById('noComments');
  const input = document.getElementById('commentInput');
  const submitBtn = document.getElementById('commentSubmitButton');
  const pageSize = 10;
  let page = 0;
  let loading = false;
  let done = false;

  // 초기 로드
  loadComments();

  // 스크롤 이벤트: 상단 근처에서 추가 로드
  if (container) {
    container.addEventListener('scroll', () => {
      if (!loading && !done && container.scrollTop < 30) {
        const prevHeight = container.scrollHeight;
        loadComments().then(() => {
          // 로드 후 스크롤 위치 보정
          const addedHeight = container.scrollHeight - prevHeight;
          container.scrollTop += addedHeight;
        });
      }
    });
  }

  // 댓글 등록
  if (submitBtn && input) {
    submitBtn.addEventListener('click', () => {
      const content = input.value.trim();
      if (!content) {
        return;
      }
      submitComment(content);
      input.value = '';
    });

    // 엔터 키 제출
    input.addEventListener('keypress', function (e) {
      if (e.key === 'Enter') {
        e.preventDefault();
        const content = this.value.trim();
        if (!content) {
          return;
        }
        submitComment(content);
        this.value = '';
      }
    });
  }

  // 댓글 목록 로드 (인증 선택)
  async function loadComments() {
    if (!videoId || !list) {
      return;
    }

    loading = true;
    try {
      // videoId가 깨끗한지 확인하고 댓글 API 호출
      const cleanVideoId = videoId.toString().split('?')[0]; // ? 이후 제거
      const commentApiUrl = `/api/videos/${cleanVideoId}/comments?page=${page}&size=${pageSize}`;
      console.log('댓글 API 호출:', commentApiUrl);

      const res = await optionalAuthFetch(commentApiUrl);
      const data = await res.json();
      const items = data.response.data || [];

      if (items.length === 0) {
        if (page === 0) {
          list.innerHTML = '';
          if (noCommentsEl) {
            noCommentsEl.style.display = 'block';
          }
        }
        done = true;
      } else {
        if (noCommentsEl) {
          noCommentsEl.style.display = 'none';
        }
        items.forEach(
            c => list.insertAdjacentHTML('afterbegin', renderComment(c)));
        page++;
      }

      // 댓글 수 업데이트
      updateCommentCount(items.length);

    } catch (error) {
      console.error('댓글 로드 오류:', error);
      if (list) {
        list.innerHTML = '<p class="error-message">댓글을 불러오는 데 실패했습니다.</p>';
      }
      if (noCommentsEl) {
        noCommentsEl.style.display = 'block';
      }
      done = true;
    } finally {
      loading = false;
    }
  }

  // 댓글 등록 (인증 필수)
  async function submitComment(content) {
    if (!videoId) {
      return;
    }

    try {
      // videoId가 깨끗한지 확인
      const cleanVideoId = videoId.toString().split('?')[0]; // ? 이후 제거
      const submitApiUrl = `/api/videos/${cleanVideoId}/comments`;
      console.log('댓글 등록 API 호출:', submitApiUrl);

      const res = await authFetch(
          submitApiUrl,
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({content})
          }
      );

      const {response: comment} = await res.json();
      if (list) {
        list.insertAdjacentHTML('beforeend', renderComment(comment));
      }
      if (noCommentsEl) {
        noCommentsEl.style.display = 'none';
      }

      // 댓글 수 업데이트
      const commentCountEl = document.getElementById('commentCount');
      if (commentCountEl) {
        const currentCount = parseInt(commentCountEl.textContent) || 0;
        commentCountEl.textContent = currentCount + 1;
      }

    } catch (error) {
      console.error('댓글 등록 오류:', error);
      if (error instanceof FetchError && error.httpStatus === 401) {
        alert('댓글 등록을 위해 로그인 후 이용해주세요.');
        return window.location.href = '/login/login.html';
      }
      handleFetchError(error);
    }
  }

  // 수정/삭제 (인증 필수)
  if (list) {
    list.addEventListener('click', async e => {
      const item = e.target.closest('.comment-item');
      if (!item) {
        return;
      }

      const id = item.dataset.id;

      if (e.target.classList.contains('delete-btn')) {
        try {
          // videoId가 깨끗한지 확인
          const cleanVideoId = videoId.toString().split('?')[0]; // ? 이후 제거

          await authFetch(
              `/api/videos/${cleanVideoId}/comments/${id}`,
              {method: 'DELETE'}
          );
          item.remove();

          // 댓글 수 업데이트
          const commentCountEl = document.getElementById('commentCount');
          if (commentCountEl) {
            const currentCount = parseInt(commentCountEl.textContent) || 0;
            if (currentCount > 0) {
              commentCountEl.textContent = currentCount - 1;
            }
          }

        } catch (error) {
          if (error instanceof FetchError && error.httpStatus === 401) {
            alert('삭제를 위해 로그인 후 이용해주세요.');
            return window.location.href = '/login/login.html';
          }
          handleFetchError(error);
          console.error('삭제 오류:', error);
        }
      } else if (e.target.classList.contains('edit-btn')) {
        const p = item.querySelector('.content');
        if (p.isContentEditable) {
          p.contentEditable = false;
          try {
            // videoId가 깨끗한지 확인
            const cleanVideoId = videoId.toString().split('?')[0]; // ? 이후 제거

            await authFetch(
                `/api/videos/${cleanVideoId}/comments/${id}`,
                {
                  method: 'PUT',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  body: JSON.stringify({content: p.textContent.trim()})
                }
            );
          } catch (error) {
            if (error instanceof FetchError && error.httpStatus === 401) {
              alert('수정을 위해 로그인 후 이용해주세요.');
              return window.location.href = '/login/login.html';
            }
            handleFetchError(error);
            console.error('수정 오류:', error);
          }
        } else {
          p.contentEditable = true;
          p.focus();
        }
      }
    });
  }

  // 댓글 HTML 생성
  function renderComment(c) {
    return `
      <div class="comment-item" data-id="${c.commentId}">
        <div>
          <strong>${c.authorName}</strong>
          <small>${new Date(c.createdAt).toLocaleString('ko-KR')}</small>
          <p class="content">${c.content}</p>
        </div>
        ${c.mine ? `
          <div class="comment-actions">
            <i class="fas fa-pen edit-btn"></i>
            <i class="fas fa-times delete-btn"></i>
          </div>
        ` : ''}
      </div>
    `;
  }
}

/**
 * 댓글 수 업데이트
 */
function updateCommentCount(count) {
  const commentCountElement = document.getElementById('commentCount');

  if (commentCountElement) {
    commentCountElement.textContent = formatNumber(count);
  }
}

/**
 * 숫자 포맷 (천 단위 콤마)
 */
function formatNumber(num) {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '만';
  }
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

/**
 * 날짜 포맷
 */
function formatDate(dateString) {
  if (!dateString) {
    return '';
  }

  const now = new Date();
  const date = new Date(dateString);
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / (1000 * 60));
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffMins < 1) {
    return '방금 전';
  } else if (diffMins < 60) {
    return `${diffMins}분 전`;
  } else if (diffHours < 24) {
    return `${diffHours}시간 전`;
  } else if (diffDays < 7) {
    return `${diffDays}일 전`;
  } else {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}.${month}.${day}`;
  }
}

/**
 * 시간 포맷 (00:00 형식)
 */
function formatTime(seconds) {
  if (isNaN(seconds) || seconds === Infinity) {
    return '00:00';
  }

  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);

  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}