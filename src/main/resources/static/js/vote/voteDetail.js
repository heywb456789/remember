import {
  authFetch,
  optionalAuthFetch,
  handleFetchError,
  getUserId
} from '../commonFetch.js'

let shareModal;

document.addEventListener('DOMContentLoaded', () => {
  initBackButton();
  loadVoteData();
  initShareFeatures();
});

// 뒤로가기 버튼
function initBackButton() {
  const backBtn = document.getElementById('backButton');
  if (!backBtn) {
    return;
  }
  backBtn.addEventListener('click', () => {
    window.history.length > 1 ? window.history.back()
        : window.location.href = 'voteList.html';
  });
}

// URL에서 ID 추출
function getVoteIdFromUrl() {
  return new URLSearchParams(window.location.search).get('id');
}

// 투표 데이터 로드
async function loadVoteData() {
  const voteId = getVoteIdFromUrl();
  if (!voteId) {
    return showError('투표 ID가 올바르지 않습니다.');
  }

  try {
    showLoading(true);
    const res = await optionalAuthFetch(`/api/vote/posts/${voteId}`);
    const {response: voteData} = await res.json();
    updateVoteUI(voteData);
  } catch (err) {
    console.error('투표 데이터 로드 오류:', err);
    handleFetchError(err);
    showError('투표 정보를 불러오는 중 오류가 발생했습니다.');
  } finally {
    showLoading(false);
  }
}

function showLoading(active) {
  document.getElementById('loadingState').style.display = active ? 'flex'
      : 'none';
  document.getElementById('voteContentContainer').style.display = active
      ? 'none' : 'block';
}

// UI 업데이트
function updateVoteUI({question, voteOptions, voted, votedId, votePostId}) {
  document.getElementById('voteQuestion').textContent = question;
  document.getElementById('totalVoteCount').textContent = getTotalVotes(
      voteOptions);

  renderVoteOptions(voteOptions, voted, votedId, votePostId);
  document.getElementById('voteMessage').textContent = voted
      ? '이미 투표에 참여하셨습니다.'
      : '투표에 참여해 주세요.';

  voted ? showVoteResults(voteOptions) : hideResults();
}

function getTotalVotes(options = []) {
  return options.reduce((sum, o) => sum + (o.voteCount || 0), 0);
}

function renderVoteOptions(options, voted, votedId, voteId) {
  const section = document.getElementById('voteOptionsSection');
  section.innerHTML = '';
  const isSelected = id => voted && votedId === id;

  if (options.length === 2) {
    const [l, r] = options;
    section.innerHTML = `
      <div class="vote-option-row">
        <button class="vote-option-button option-left ${isSelected(l.optionId)
        ? 'selected' : ''}" data-option-id="${l.optionId}">${l.optionName}</button>
        <span class="vs-text">VS</span>
        <button class="vote-option-button option-right ${isSelected(r.optionId)
        ? 'selected' : ''}" data-option-id="${r.optionId}">${r.optionName}</button>
      </div>`;
  } else {
    options.forEach(opt => {
      section.insertAdjacentHTML('beforeend', `
        <button class="vote-option-button ${isSelected(opt.optionId)
          ? 'selected' : ''}" data-option-id="${opt.optionId}">${opt.optionName}</button>
      `);
    });
  }

  if (!voted) {
    initVoteButtons(voteId);
  }
}

function initVoteButtons(voteId) {
  document.querySelectorAll('.vote-option-button').forEach(btn => {
    btn.addEventListener('click', async () => {
      const optionId = btn.dataset.optionId;
      disableButtons(true);
      try {
        await authFetch(`/api/vote/posts/${voteId}/options/${optionId}`,
            {method: 'POST'});
        document.getElementById(
            'voteMessage').textContent = '투표에 참여해 주셔서 감사합니다.';
        await loadVoteData();
      } catch (err) {
        console.error('투표 오류:', err);
        if (err.message.includes('Unauthorized')) {
          alert('로그인이 필요한 기능입니다.');
          window.location.href = '/login/login.html';
        } else {
          alert('투표 처리 중 오류가 발생했습니다.');
          window.location.reload();
        }
      } finally {
        disableButtons(false);
      }
    });
  });
}

function disableButtons(state) {
  document.querySelectorAll('.vote-option-button').forEach(
      b => b.disabled = state);
}

function hideResults() {
  document.getElementById('voteResultsSection').style.display = 'none';
}

function showVoteResults(options) {
  const container = document.getElementById('resultsContainer');
  container.innerHTML = '';
  const total = getTotalVotes(options);

  options.forEach((o, i) => {
    const pct = total ? Math.round((o.voteCount / total) * 100) : 0;
    container.insertAdjacentHTML('beforeend', `
      <div class="result-item">
        <div class="result-label">
          <span class="result-option-name">${o.optionName}</span>
          <span class="result-percentage">${pct}% (${o.voteCount}명)</span>
        </div>
        <div class="result-bar-container">
          <div class="result-bar ${i === 0 ? 'result-bar-left'
        : 'result-bar-right'}" style="width:${pct}%"></div>
        </div>
      </div>`);
  });

  document.getElementById('voteResultsSection').style.display = 'block';
}

function showError(msg) {
  showLoading(false);
  document.querySelector('.vote-detail-container').innerHTML = `
    <div class="error-state">
      <div class="error-icon">...</div>
      <p class="error-title">${msg}</p>
      <button class="retry-button" onclick="window.location.reload()">다시 시도하기</button>
    </div>`;
}

// 공유 기능 (모바일/웹 지원)
function initShareFeatures() {
  // Bootstrap Modal 인스턴스
  shareModal = new bootstrap.Modal(document.getElementById('shareModal'));

  document.getElementById('openShareModal')?.addEventListener('click', () => {
    shareModal.show();
    setupKakaoShare();
  });
  document.getElementById('copyUrl')?.addEventListener('click', copyCurrentUrl);
  // 통통 공유 버튼 이벤트 추가
  document.getElementById('shareTongtong')?.addEventListener('click', () => {
    shareTongtongApp();
  });

  document.getElementById('shareX')?.addEventListener('click', () => {
    shareToX();
  });
}

// 통통 앱 공유 함수
function shareTongtongApp() {
  const voteId = getVoteIdFromUrl();
  const shareUrl = `https://club1.newstomato.com/share/vote/${voteId}`;

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

async function setupKakaoShare() {
  const id = getVoteIdFromUrl();
  const userId = await getUserId();
  const title = document.getElementById('voteQuestion').textContent;
  const description = '토마토 뉴스 투표광장에서 참여해 보세요!';
  const shareUrl = `https://club1.newstomato.com/share/vote/${id}`;
  const imageUrl = 'https://image.newstomato.com/newstomato/club/share/voting.png';

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
        type: 'vote',      // 'board' | 'vote' | 'news' | 'video'
        id: id,        // 게시물 PK
        userId: userId // 로그인한 회원 ID
      }
    });
  } else {
    console.error('Kakao SDK 미초기화');
  }
}

function copyCurrentUrl() {
  const voteId = getVoteIdFromUrl();
  const shareUrl = `https://club1.newstomato.com/share/vote/${voteId}`;
  navigator.clipboard.writeText(shareUrl)
  .then(() => {
    alert('URL이 복사되었습니다.');
    shareModal.hide();
  })
  .catch(err => {
    console.error('복사 실패', err);
    alert('URL 복사에 실패했습니다.');
  });
}

/**
 * X(구 Twitter) 공유 함수
 */
async function shareToX() {
  const title = document.querySelector('.post-title')?.textContent.trim() || '';
  const postId = getVoteIdFromUrl();
  const shareUrl = `https://club1.newstomato.com/share/vote/${postId}`;

  try {
    const res = await authFetch('/twitter/share', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: title,
        shareUrl: shareUrl,
        type: 'VOTE',
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