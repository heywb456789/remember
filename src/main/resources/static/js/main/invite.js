// invite.js - 교환 버튼에만 로딩 표시 구현
import {
  optionalAuthFetch,
  handleFetchError,
  authFetch
} from '../commonFetch.js';

document.addEventListener('DOMContentLoaded', function() {
  // 초대 버튼 클릭 이벤트
  const inviteButton = document.getElementById('inviteButton');
  if (inviteButton) {
    inviteButton.addEventListener('click', function() {
      openInviteModal();
    });
  }

  // 교환 버튼 클릭 이벤트
  const exchangeButton = document.getElementById('exchangeButton');
  if (exchangeButton) {
    exchangeButton.addEventListener('click', function() {
      // 이미 처리 중이면 무시
      if (exchangeButton.disabled) {
        return;
      }
      exchangePoint(exchangeButton);
    });
  }

  // 초대 코드 복사 버튼 이벤트
  const copyCodeBtn = document.getElementById('copyCodeBtn');
  if (copyCodeBtn) {
    copyCodeBtn.addEventListener('click', copyInviteCode);
  }

  // 공유하기 버튼 이벤트
  const shareUrlBtn = document.getElementById('shareUrlBtn');
  if (shareUrlBtn) {
    shareUrlBtn.addEventListener('click', copyShareUrl);
  }

  // 로드 시 사용자 정보 가져오기 (포인트 & 초대 코드)
  fetchUserInfo();
});

/**
 * 포인트 교환 처리
 * @param {HTMLElement} button - 교환 버튼 요소
 */
async function exchangePoint(button){
  // 버튼 비활성화 및 로딩 아이콘 추가
  button.disabled = true;
  
  // 원래 내용 저장
  const originalText = button.textContent;
  
  // 로딩 아이콘 추가 (버튼 안에만 표시)
  button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> 교환중';

  try {
    const res = await authFetch('/api/user/points/exchange', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    const result = await res.json();

    if (res.ok) {
      updatePointsUI(result.response);
      alert('교환이 완료되었습니다.');
    } else {
      alert('실패: ' + (result.status?.message || '알 수 없는 오류가 발생했습니다.'));
    }
  } catch (err) {
    handleFetchError(err);
  } finally {
    // 버튼 상태 복원 (약간의 딜레이를 주어 UI 깜박임 방지)
    setTimeout(() => {
      button.disabled = false;
      button.textContent = originalText; // innerHTML 대신 textContent 사용하여 원래 텍스트만 복원
    }, 500);
  }
}

/**
 * API에서 사용자 정보 가져오기 (포인트 & 초대 코드)
 * @returns {Promise} 사용자 데이터를 담은 프로미스
 */
async function fetchUserInfo() {
  try {
    // /api/auth/me API 호출로 모든 정보 가져오기
    const response = await optionalAuthFetch('/api/auth/me');
    const data = await response.json();

    // 필요한 모든 UI 업데이트
    updateAllUI(data.response);

    // 데이터를 성공적으로 가져왔으므로 포인트 섹션 표시
    showPointSection();

    return data;
  } catch (error) {
    console.error('사용자 정보 로드 오류:', error);

    // 에러 발생 시 포인트 섹션 숨기기
    hidePointSection();
  }
}

/**
 * 포인트 섹션 컨테이너 표시
 */
function showPointSection() {
  const pointSectionContainer = document.querySelector('.point-section-container');
  if (pointSectionContainer) {
    pointSectionContainer.style.display = 'block';
  }
}

/**
 * 포인트 섹션 컨테이너 숨기기
 */
function hidePointSection() {
  const pointSectionContainer = document.querySelector('.point-section-container');
  if (pointSectionContainer) {
    pointSectionContainer.style.display = 'none';
  }
}

/**
 * 모든 UI 요소 업데이트 (포인트 & 초대 코드)
 * @param {Object} data - 사용자 데이터
 */
function updateAllUI(data) {
  // 포인트 업데이트
  updatePointsUI(data);

  // 초대 코드 업데이트
  updateInviteCodeUI(data);
}

/**
 * 포인트 UI 업데이트
 * @param {Object} data - 사용자 데이터
 */
function updatePointsUI(data) {
  const pointValueElement = document.querySelector('.point-value');
  if (pointValueElement && data.points !== undefined) {
    pointValueElement.textContent = `${data.points} TTR`;
  }
}

/**
 * 초대 코드 UI 업데이트
 * @param {Object} data - 사용자 데이터
 */
function updateInviteCodeUI(data) {
  const codeElement = document.getElementById('inviteCode');
  const urlElement = document.querySelector('.invite-url-preview span');

  if (codeElement && data.inviteCode) {
    codeElement.textContent = data.inviteCode;
  }

  if (urlElement) {
    // inviteUrl이 있으면 사용, 없으면 초대 코드로 URL 생성
    urlElement.textContent = `https://club1.newstomato.com/share/invite?code=${data.inviteCode}`;
  }
}

/**
 * 초대 모달 열기
 */
function openInviteModal() {
  const inviteModal = new bootstrap.Modal(document.getElementById('inviteModal'));
  inviteModal.show();
}

/**
 * 초대 코드 복사
 */
function copyInviteCode() {
  const inviteCode = document.getElementById('inviteCode').textContent;
  copyToClipboard(inviteCode);
  showToast();
}

/**
 * 공유 URL 복사
 */
function copyShareUrl() {
  const inviteUrlPreview = document.querySelector('.invite-url-preview span').textContent;
  copyToClipboard(inviteUrlPreview);
  showToast();
}

/**
 * 클립보드에 텍스트 복사
 * @param {string} text - 복사할 텍스트
 */
function copyToClipboard(text) {
  if (navigator.clipboard && window.isSecureContext) {
    // 보안 컨텍스트에서 Clipboard API 사용
    navigator.clipboard.writeText(text)
      .catch(err => {
        console.error('클립보드 복사 실패:', err);
        fallbackCopyToClipboard(text);
      });
  } else {
    // 대체 방법 사용
    fallbackCopyToClipboard(text);
  }
}

/**
 * 대체 클립보드 복사 방법
 * @param {string} text - 복사할 텍스트
 */
function fallbackCopyToClipboard(text) {
  try {
    const textArea = document.createElement('textarea');
    textArea.value = text;

    // 화면 밖으로 위치시키기
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);

    // 선택 및 복사
    textArea.focus();
    textArea.select();
    document.execCommand('copy');

    // 정리
    document.body.removeChild(textArea);
  } catch (err) {
    console.error('대체 클립보드 복사 실패:', err);
  }
}

/**
 * 토스트 메시지 표시
 */
function showToast() {
  const toastElement = document.getElementById('copyToast');
  const toast = new bootstrap.Toast(toastElement, {
    delay: 2000
  });

  toast.show();
}