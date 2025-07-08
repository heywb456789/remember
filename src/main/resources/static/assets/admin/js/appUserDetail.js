/**
 * 회원 상세 정보 페이지 JavaScript
 */
import {adminAuthFetch} from '/js/commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
  'use strict';

  // DOM 요소
  const btnStatusChange = document.getElementById('btnStatusChange');
  const statusModal = new bootstrap.Modal(document.getElementById('statusModal'));
  const btnSaveStatus = document.getElementById('btnSaveStatus');
  const statusSelect = document.getElementById('status');
  const activityTypeSelect = document.getElementById('activityType');
  const loadMoreActivitiesBtn = document.getElementById('loadMoreActivities');
  const loadMoreHistoryBtn = document.getElementById('loadMoreHistory');

  // 현재 페이지 상태
  const userId = getUserIdFromUrl();
  let activitiesPage = 0;
  let loginHistoryPage = 0;
  const pageSize = 10;

  // 초기화
  initPage();

  /**
   * 페이지 초기화 함수
   */
  function initPage() {
    // 이벤트 리스너 등록
    initEventListeners();
  }

  /**
   * 이벤트 리스너 초기화
   */
  function initEventListeners() {
    // 상태 변경 버튼 클릭 이벤트
    if (btnStatusChange) {
      btnStatusChange.addEventListener('click', function() {
        statusModal.show();
      });
    }

    // 상태 저장 버튼 클릭 이벤트
    if (btnSaveStatus) {
      btnSaveStatus.addEventListener('click', saveUserStatus);
    }

    // 활동 유형 필터 변경 이벤트
    if (activityTypeSelect) {
      activityTypeSelect.addEventListener('change', function() {
        activitiesPage = 0; // 페이지 초기화
        loadUserActivities(true); // 활동 내역 새로 로드 (기존 내역 제거)
      });
    }

    // 더 보기 버튼 이벤트
    if (loadMoreActivitiesBtn) {
      loadMoreActivitiesBtn.addEventListener('click', function() {
        activitiesPage++;
        loadUserActivities(false); // 기존 내역 유지하고 추가 로드
      });
    }

    if (loadMoreHistoryBtn) {
      loadMoreHistoryBtn.addEventListener('click', function() {
        loginHistoryPage++;
        loadLoginHistory(false); // 기존 내역 유지하고 추가 로드
      });
    }

    // 탭 변경 이벤트
    const userDetailTabs = document.getElementById('userDetailTabs');
    if (userDetailTabs) {
      userDetailTabs.addEventListener('shown.bs.tab', function(e) {
        const targetTab = e.target.getAttribute('href');

        // 활동 내역 탭이 선택되었을 때
        if (targetTab === '#activity' && activitiesPage === 0) {
          loadUserActivities(true);
        }

        // 로그인 기록 탭이 선택되었을 때
        if (targetTab === '#login-history' && loginHistoryPage === 0) {
          loadLoginHistory(true);
        }
      });
    }
  }

  /**
   * URL에서 사용자 ID 추출
   * @returns {string} 사용자 ID
   */
  function getUserIdFromUrl() {
    const path = window.location.pathname;
    const matches = path.match(/\/admin\/users\/app\/user\/(\d+)$/);
    return matches ? matches[1] : null;
  }

  /**
   * 사용자 상태 변경 저장
   */
  async function saveUserStatus() {
    if (!userId) {
      showAlert('사용자 ID를 찾을 수 없습니다.', 'warning');
      return;
    }

    const newStatus = statusSelect.value;
    const reason = document.getElementById('reason').value.trim();

    btnSaveStatus.disabled = true;
    btnSaveStatus.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 저장 중...';

    try {
      const response = await adminAuthFetch(`/admin/users/app/user/${userId}/status`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          status: newStatus,
          reason: reason || null
        })
      });

      if (!response.ok) {
        throw new Error('서버 응답 오류: ' + response.status);
      }

      const result = await response.json();

      if (result.status.code === 'OK_0000') {
        // 상태 변경 성공
        statusModal.hide();
        showAlert('회원 상태가 성공적으로 변경되었습니다.', 'success');

        // 페이지 새로고침
        setTimeout(() => {
          window.location.reload();
        }, 1000);
      } else {
        throw new Error(result.status.message || '상태 변경 중 오류가 발생했습니다.');
      }
    } catch (error) {
      console.error('상태 변경 오류:', error);
      showAlert('상태 변경 중 오류가 발생했습니다: ' + error.message, 'danger');
    } finally {
      btnSaveStatus.innerHTML = '저장';
      btnSaveStatus.disabled = false;
    }
  }

  /**
   * 사용자 활동 내역 로드
   * @param {boolean} reset - 기존 내역 초기화 여부
   */
  async function loadUserActivities(reset = false) {
    if (!userId) {
      return;
    }

    const activityType = activityTypeSelect ? activityTypeSelect.value : '';
    const activityTimeline = document.querySelector('.activity-timeline');

    if (!activityTimeline) {
      return;
    }

    // 로딩 표시
    if (loadMoreActivitiesBtn) {
      loadMoreActivitiesBtn.disabled = true;
      loadMoreActivitiesBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 로딩 중...';
    }

    try {
      const response =
          await adminAuthFetch(`/admin/users/app/user/${userId}/activities?page=${activitiesPage}&size=${pageSize}&type=${activityType}`);

      if (!response.ok) {
        throw new Error('서버 응답 오류: ' + response.status);
      }

      const result = await response.json();

      // 기존 내역 초기화
      if (reset) {
        activityTimeline.innerHTML = '';
      }

      if (!result.response.activities || result.response.activities.length === 0) {
        // 활동 내역이 없는 경우
        if (activitiesPage === 0) {
          activityTimeline.innerHTML = `
            <div class="text-center py-5">
              <i class="fas fa-clipboard-list fa-3x text-muted mb-3"></i>
              <p class="mb-0">활동 내역이 없습니다.</p>
            </div>
          `;
        } else {
          showAlert('더 이상 표시할 활동 내역이 없습니다.', 'info');
        }

        // 더 보기 버튼 숨김
        if (loadMoreActivitiesBtn) {
          loadMoreActivitiesBtn.style.display = 'none';
        }
      } else {
        // 활동 내역 표시
        const activities = result.response.activities;

        activities.forEach(activity => {
          const timelineItem = document.createElement('div');
          timelineItem.className = 'timeline-item';

          const iconClass =
            activity.type === 'POST' ? 'icon-post' :
            activity.type === 'COMMENT' ? 'icon-comment' :
            activity.type === 'VOTE' ? 'icon-vote' : 'icon-like';

          const iconTag =
            activity.type === 'POST' ? 'fas fa-file-alt' :
            activity.type === 'COMMENT' ? 'fas fa-comment' :
            activity.type === 'VOTE' ? 'fas fa-vote-yea' : 'fas fa-heart';

          timelineItem.innerHTML = `
            <div class="timeline-icon ${iconClass}">
              <i class="${iconTag}"></i>
            </div>
            <div class="timeline-content">
              <h6 class="mb-1">${activity.title || '활동'}</h6>
              <p class="text-muted mb-1">${activity.description || '자세한 정보 없음'}</p>
              <small class="text-muted">
                <i class="fas fa-clock"></i> ${formatDate(activity.createdAt)}
                ${activity.link ? `<a href="${activity.link}" class="ms-2"><i class="fas fa-external-link-alt"></i> 바로가기</a>` : ''}
              </small>
            </div>
          `;

          activityTimeline.appendChild(timelineItem);
        });

        // 더 보기 버튼 표시 여부
        if (loadMoreActivitiesBtn) {
          loadMoreActivitiesBtn.style.display = result.hasMore ? 'inline-block' : 'none';
        }
      }
    } catch (error) {
      console.error('활동 내역 로드 오류:', error);
      showAlert('활동 내역을 로드하는 중 오류가 발생했습니다.', 'danger');
    } finally {
      if (loadMoreActivitiesBtn) {
        loadMoreActivitiesBtn.innerHTML = '더 보기';
        loadMoreActivitiesBtn.disabled = false;
      }
    }
  }

  /**
   * 로그인 기록 로드
   * @param {boolean} reset - 기존 내역 초기화 여부
   */
  async function loadLoginHistory(reset = false) {
  if (!userId) {
    return;
  }

  const loginHistoryTable = document.querySelector('#login-history table tbody');

  if (!loginHistoryTable) {
    return;
  }

  // 로딩 표시
  if (loadMoreHistoryBtn) {
    loadMoreHistoryBtn.disabled = true;
    loadMoreHistoryBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 로딩 중...';
  }

  try {
    const response = await adminAuthFetch(`/admin/users/app/user/${userId}/login-history?page=${loginHistoryPage}&size=${pageSize}`);

    if (!response.ok) {
      throw new Error('서버 응답 오류: ' + response.status);
    }

    const result = await response.json();

    // 기존 내역 초기화
    if (reset) {
      loginHistoryTable.innerHTML = '';
    }

    if (!result.response.history || result.response.history.length === 0) {
      // 로그인 기록이 없는 경우
      if (loginHistoryPage === 0) {
        loginHistoryTable.innerHTML = `
          <tr>
            <td colspan="4" class="text-center py-4">로그인 기록이 없습니다.</td>
          </tr>
        `;
      } else {
        showAlert('더 이상 표시할 로그인 기록이 없습니다.', 'info');
      }

      // 더 보기 버튼 숨김
      if (loadMoreHistoryBtn) {
        loadMoreHistoryBtn.style.display = 'none';
      }
    } else {
      // 로그인 기록 표시
      const history = result.response.history;

      history.forEach(login => {
        const row = document.createElement('tr');

        row.innerHTML = `
          <td>${formatDate(login.createdAt, true)}</td>
          <td>${login.ipAddress || '-'}</td>
          <td>
            <span>${login.userAgent || '알 수 없음'}</span>
            ${login.isMobile ? '<i class="fas fa-mobile-alt ms-1" title="모바일 기기"></i>' : ''}
          </td>
          <td>
            <span class="badge bg-success">성공</span>
          </td>
        `;

        loginHistoryTable.appendChild(row);
      });

      // 더 보기 버튼 표시 여부
      if (loadMoreHistoryBtn) {
        loadMoreHistoryBtn.style.display = result.response.hasMore ? 'inline-block' : 'none';
      }
    }
  } catch (error) {
    console.error('로그인 기록 로드 오류:', error);
    showAlert('로그인 기록을 로드하는 중 오류가 발생했습니다.', 'danger');
  } finally {
    if (loadMoreHistoryBtn) {
      loadMoreHistoryBtn.innerHTML = '더 보기';
      loadMoreHistoryBtn.disabled = false;
    }
  }
}

  /**
   * 날짜 포맷팅 함수
   * @param {string} dateString - 날짜 문자열
   * @param {boolean} includeSeconds - 초 포함 여부
   * @returns {string} - 포맷팅된 날짜 문자열
   */
  function formatDate(dateString, includeSeconds = false) {
    if (!dateString) return '-';

    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    if (includeSeconds) {
      const seconds = String(date.getSeconds()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    }

    return `${year}-${month}-${day} ${hours}:${minutes}`;
  }

  /**
   * 알림 표시 함수
   * @param {string} message - 알림 메시지
   * @param {string} type - 알림 타입 (success, danger, warning, info)
   */
  function showAlert(message, type = 'info') {
    // 커스텀 알림 시스템 사용
    if (window.CustomNotification) {
      window.CustomNotification.show(message, type);
      return;
    }

    // 기본 alert 사용
    alert(message);
  }
});