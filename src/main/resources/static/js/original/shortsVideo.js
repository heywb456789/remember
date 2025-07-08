import {
  optionalAuthFetch,
  authFetch,
  handleFetchError,
  FetchError,
  getUserId
} from '../commonFetch.js';

// 전역 변수
let shareModal = null;
let shareVideoType = null;
let isLiked = false;
let isBookmarked = false;
let isDescriptionExpanded = false;
let currentUserId = null;

// 문서 로드 완료 시 실행
document.addEventListener('DOMContentLoaded', function () {
  // 모달 초기화
  shareModal = new bootstrap.Modal(document.getElementById('shareModal'));

  // 초기화
  initCurrentUser();
  initActionButtons();
  initShareFeatures();
  initComments();
  initCommentsModal();
});

/**
 * 현재 사용자 정보 초기화
 */
async function initCurrentUser() {
  try {
    currentUserId = await getUserId();
    console.log('현재 사용자 ID:', currentUserId);
  } catch (error) {
    console.log('사용자 정보 없음 (비로그인 상태)');
    currentUserId = null;
  }
}

/**
 * 댓글 모달 초기화
 */
function initCommentsModal() {
  const commentsModal = document.getElementById('commentsModal');
  const commentsModalOverlay = document.getElementById('commentsModalOverlay');
  const commentsModalClose = document.getElementById('commentsModalClose');

  // 댓글 모달 열기
  function openCommentsModal() {
    commentsModal.classList.add('active');
    commentsModalOverlay.classList.add('active');
    // body 스크롤은 막지 않음 (유튜브 기능 유지를 위해)
  }

  // 댓글 모달 닫기
  function closeCommentsModal() {
    commentsModal.classList.remove('active');
    commentsModalOverlay.classList.remove('active');
  }

  // 이벤트 리스너
  if (commentsModalClose) {
    commentsModalClose.addEventListener('click', closeCommentsModal);
  }

  if (commentsModalOverlay) {
    commentsModalOverlay.addEventListener('click', closeCommentsModal);
  }

  // ESC 키로 모달 닫기
  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape' && commentsModal.classList.contains('active')) {
      closeCommentsModal();
    }
  });

  // 전역 함수로 등록
  window.openCommentsModal = openCommentsModal;
  window.closeCommentsModal = closeCommentsModal;
}

/**
 * 비디오 UI 업데이트 (플레이어 모듈에서 호출)
 */
function updateVideoUI(data, isYouTubeVideo) {
  // 공유용 비디오 타입 설정
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

  // 좋아요 수 업데이트
  const likeCountElement = document.getElementById('likeCount');
  if (likeCountElement) {
    likeCountElement.textContent = formatNumber(data.likeCount || 0);
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
 * 액션 버튼들 초기화
 */
function initActionButtons() {
  const likeButton = document.getElementById('likeButton');
  const commentButton = document.getElementById('commentButton');
  const shareButton = document.getElementById('shareButton');
  const bookmarkButton = document.getElementById('bookmarkButton');

  if (likeButton) {
    likeButton.addEventListener('click', toggleLike);
  }

  if (commentButton) {
    commentButton.addEventListener('click', function() {
      if (window.openCommentsModal) {
        window.openCommentsModal();
      }
    });
  }

  if (shareButton) {
    shareButton.addEventListener('click', openShareModal);
  }

  if (bookmarkButton) {
    bookmarkButton.addEventListener('click', toggleBookmark);
  }
}

/**
 * 좋아요 토글
 */
function toggleLike() {
  isLiked = !isLiked;
  const likeButton = document.getElementById('likeButton');
  const likeCountElement = document.getElementById('likeCount');

  if (likeButton) {
    const icon = likeButton.querySelector('i');
    if (isLiked) {
      likeButton.classList.add('liked');
      icon.className = 'fas fa-heart';
    } else {
      likeButton.classList.remove('liked');
      icon.className = 'far fa-heart';
    }
  }

  // 좋아요 수 업데이트
  if (likeCountElement) {
    const currentCount = parseInt(likeCountElement.textContent.replace(/,/g, '').replace('만', '0000')) || 0;
    const newCount = isLiked ? currentCount + 1 : Math.max(0, currentCount - 1);
    likeCountElement.textContent = formatNumber(newCount);
  }
}

/**
 * 북마크 토글
 */
function toggleBookmark() {
  isBookmarked = !isBookmarked;
  const bookmarkButton = document.getElementById('bookmarkButton');

  if (bookmarkButton) {
    const icon = bookmarkButton.querySelector('i');
    if (isBookmarked) {
      bookmarkButton.classList.add('bookmarked');
      icon.className = 'fas fa-bookmark';
    } else {
      bookmarkButton.classList.remove('bookmarked');
      icon.className = 'far fa-bookmark';
    }
  }
}

/**
 * 공유 모달 열기
 */
function openShareModal() {
  if (shareModal) {
    shareModal.show();
    setupKakaoShare();
  }
}

/**
 * 더보기 버튼 초기화
 */
function initMoreButton() {
  const moreButton = document.getElementById('moreButton');
  const descriptionElement = document.getElementById('videoDescription');

  if (moreButton && descriptionElement) {
    const isOverflowing = descriptionElement.scrollHeight > descriptionElement.clientHeight;

    moreButton.style.display = isOverflowing ? 'block' : 'none';

    moreButton.addEventListener('click', function () {
      isDescriptionExpanded = !isDescriptionExpanded;

      if (isDescriptionExpanded) {
        descriptionElement.classList.add('expanded');
        this.textContent = '접기';
      } else {
        descriptionElement.classList.remove('expanded');
        this.textContent = '더보기';
      }
    });
  }
}

/**
 * 공유 기능 초기화
 */
function initShareFeatures() {
  document.getElementById('copyUrl')?.addEventListener('click', () => {
    copyCurrentUrl();
  });

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
  const videoId = window.getVideoIdFromUrl();
  const shareUrl = `https://club1.newstomato.com/share/original/${videoId}`;

  const userAgent = navigator.userAgent.toLowerCase();
  let appScheme = '';
  let storeUrl = '';

  if (/iphone|ipad|ipod/.test(userAgent)) {
    appScheme = `tongtongios://tongtongiOS?url=${encodeURIComponent(shareUrl)}`;
    storeUrl = 'https://apps.apple.com/kr/app/통통-암호화-메신저/id982895719';
  } else if (/android/.test(userAgent)) {
    appScheme = `tongtong://m.etomato.com?url=${encodeURIComponent(shareUrl)}`;
    storeUrl = 'https://play.google.com/store/apps/details?id=tomato.solution.tongtong';
  } else {
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

  navigator.clipboard.writeText(shareUrl)
    .then(() => {
      console.log('URL이 클립보드에 복사되었습니다.');
    })
    .catch(err => {
      console.error('클립보드 복사 오류:', err);
    });

  const appCheckTimeout = 1500;
  const now = Date.now();

  window.location.href = appScheme;

  setTimeout(function() {
    if (document.hidden === false && Date.now() - now > appCheckTimeout) {
      if (confirm('통통 앱이 설치되어 있지 않은 것 같습니다. 앱 스토어로 이동하시겠습니까?')) {
        window.location.href = storeUrl;
      }
    }
    shareModal.hide();
  }, appCheckTimeout + 500);
}

/**
 * 카카오톡 공유 설정
 */
async function setupKakaoShare() {
  const id = window.getVideoIdFromUrl();
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
        type: shareVideoType,
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
  const title = document.getElementById('videoTitle')?.textContent.trim() || '';
  const postId = window.getVideoIdFromUrl();
  const shareUrl = `https://club1.newstomato.com/share/original/${postId}`;

  try {
    const res = await authFetch('/twitter/share', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: title,
        shareUrl: shareUrl,
        type: shareVideoType.toUpperCase(),
        targetId: postId
      })
    });
    await res.json();
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

/**
 * URL 복사
 */
function copyCurrentUrl() {
  const videoId = window.getVideoIdFromUrl();
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
  const videoId = window.getVideoIdFromUrl();
  const list = document.getElementById('commentsContainer');
  const noCommentsEl = document.getElementById('noComments');
  const modalInput = document.getElementById('modalCommentInput');
  const modalSubmitBtn = document.getElementById('modalCommentSubmit');
  const pageSize = 10;
  let page = 0;
  let loading = false;
  let done = false;

  // 초기 로드
  loadComments();

  // 댓글 등록 (모달 입력창만 사용)
  if (modalSubmitBtn && modalInput) {
    function updateModalSubmitButton() {
      const hasContent = modalInput.value.trim().length > 0;
      modalSubmitBtn.disabled = !hasContent;
    }

    modalInput.addEventListener('input', updateModalSubmitButton);
    updateModalSubmitButton();

    modalSubmitBtn.addEventListener('click', () => {
      const content = modalInput.value.trim();
      if (!content) return;
      submitComment(content);
      modalInput.value = '';
      updateModalSubmitButton();
    });

    modalInput.addEventListener('keypress', function (e) {
      if (e.key === 'Enter') {
        e.preventDefault();
        const content = this.value.trim();
        if (!content) return;
        submitComment(content);
        this.value = '';
        updateModalSubmitButton();
      }
    });
  }

  // 댓글 목록 로드
  async function loadComments() {
    if (!videoId || !list) return;

    loading = true;
    try {
      const cleanVideoId = videoId.toString().split('?')[0];
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

        // 댓글 렌더링 시 현재 사용자 ID 확인
        items.forEach(c => {
          const commentHtml = renderComment(c);
          list.insertAdjacentHTML('beforeend', commentHtml);
        });
        page++;
      }
    } catch (error) {
      console.error('댓글 로드 오류:', error);
      if (list) {
        list.innerHTML = '<p class="error-message" style="color: #888; text-align: center; padding: 20px;">댓글을 불러오는 데 실패했습니다.</p>';
      }
      if (noCommentsEl) {
        noCommentsEl.style.display = 'block';
      }
      done = true;
    } finally {
      loading = false;
    }
  }

  // 댓글 등록
  async function submitComment(content) {
    if (!videoId) return;

    try {
      const cleanVideoId = videoId.toString().split('?')[0];
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

      // 새 댓글 추가 시 현재 사용자 정보 확인
      if (list) {
        const commentHtml = renderComment(comment);
        list.insertAdjacentHTML('beforeend', commentHtml);
      }

      if (noCommentsEl) {
        noCommentsEl.style.display = 'none';
      }

      // 댓글 수 업데이트
      const commentCountEl = document.getElementById('commentCount');
      if (commentCountEl) {
        const currentCount = parseInt(commentCountEl.textContent.replace(/,/g, '').replace('만', '0000')) || 0;
        commentCountEl.textContent = formatNumber(currentCount + 1);
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

  // 수정/삭제 이벤트 처리
  if (list) {
    list.addEventListener('click', async e => {
      const item = e.target.closest('.comment-item');
      if (!item) return;

      const id = item.dataset.id;

      if (e.target.classList.contains('delete-btn')) {
        if (!confirm('댓글을 삭제하시겠습니까?')) return;

        try {
          const cleanVideoId = videoId.toString().split('?')[0];

          await authFetch(
            `/api/videos/${cleanVideoId}/comments/${id}`,
            {method: 'DELETE'}
          );
          item.remove();

          // 댓글 수 업데이트
          const commentCountEl = document.getElementById('commentCount');
          if (commentCountEl) {
            const currentCount = parseInt(commentCountEl.textContent.replace(/,/g, '').replace('만', '0000')) || 0;
            if (currentCount > 0) {
              commentCountEl.textContent = formatNumber(currentCount - 1);
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
        const contentEl = item.querySelector('.content');
        if (contentEl.isContentEditable) {
          // 수정 완료
          contentEl.contentEditable = false;
          contentEl.style.background = 'transparent';
          contentEl.style.border = 'none';
          contentEl.style.padding = '0';

          try {
            const cleanVideoId = videoId.toString().split('?')[0];

            await authFetch(
              `/api/videos/${cleanVideoId}/comments/${id}`,
              {
                method: 'PUT',
                headers: {
                  'Content-Type': 'application/json'
                },
                body: JSON.stringify({content: contentEl.textContent.trim()})
              }
            );

            // 수정 완료 표시
            e.target.style.color = '';
            e.target.title = '수정';

          } catch (error) {
            if (error instanceof FetchError && error.httpStatus === 401) {
              alert('수정을 위해 로그인 후 이용해주세요.');
              return window.location.href = '/login/login.html';
            }
            handleFetchError(error);
            console.error('수정 오류:', error);
          }
        } else {
          // 수정 시작
          contentEl.contentEditable = true;
          contentEl.style.background = 'rgba(255,255,255,0.1)';
          contentEl.style.border = '1px solid rgba(255,255,255,0.3)';
          contentEl.style.padding = '4px 8px';
          contentEl.style.borderRadius = '4px';
          contentEl.focus();

          // 편집 중 표시
          e.target.style.color = '#007bff';
          e.target.title = '저장';
        }
      }
    });
  }

  // 댓글 HTML 생성 - 수정/삭제 버튼 표시 로직 개선
  function renderComment(c) {
    console.log('댓글 렌더링:', {
      commentId: c.commentId,
      authorId: c.authorId,
      currentUserId: currentUserId,
      mine: c.mine
    });

    // 세 가지 방법으로 본인 댓글인지 확인
    let isMine = false;

    // 1. API에서 mine 필드 확인
    if (c.mine === true) {
      isMine = true;
    }
    // 2. authorId와 currentUserId 비교
    else if (currentUserId && c.authorId && currentUserId.toString() === c.authorId.toString()) {
      isMine = true;
    }
    // 3. userId와 currentUserId 비교 (백업)
    else if (currentUserId && c.userId && currentUserId.toString() === c.userId.toString()) {
      isMine = true;
    }

    console.log('최종 isMine 결과:', isMine);

    return `
      <div class="comment-item" data-id="${c.commentId}">
        <div class="comment-content">
          <div>
            <strong>${c.authorName || c.userName || '익명'}</strong>
            <small>${formatDate(c.createdAt)}</small>
          </div>
          <p class="content">${c.content}</p>
        </div>
        ${isMine ? `
          <div class="comment-actions">
            <i class="fas fa-pen edit-btn" title="수정"></i>
            <i class="fas fa-times delete-btn" title="삭제"></i>
          </div>
        ` : ''}
      </div>
    `;
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
  if (!dateString) return '';

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

// 전역 함수로 내보내기
window.updateVideoUI = updateVideoUI;