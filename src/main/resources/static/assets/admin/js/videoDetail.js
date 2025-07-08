/**
 * 동영상 상세 페이지 JavaScript
 */
import {adminAuthFetch} from '../../../js/commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
  'use strict';

  // DOM 요소
  const btnTogglePublic = document.getElementById('btnTogglePublic');
  const btnConfirmDelete = document.getElementById('btnConfirmDelete');
  const deleteConfirmModal = new bootstrap.Modal(
      document.getElementById('deleteConfirmModal'));
  const viewTrendChartCanvas = document.getElementById('viewTrendChart');

  // 동영상 ID 가져오기 (URL에서 추출)
  const videoId = getVideoIdFromUrl();

  // 비디오 상세 데이터
  let videoData = null;

  // 초기화
  initPage();

  /**
   * 페이지 초기화 함수
   */
  function initPage() {
    // 이벤트 리스너 등록
    initEventListeners();

    // 조회 추세 차트 초기화
    initViewTrendChart();

    // 클립보드 복사 기능 초기화
    initClipboard();
  }

  /**
   * 이벤트 리스너 초기화
   */
  function initEventListeners() {
    // 공개/비공개 전환 버튼 이벤트
    if (btnTogglePublic) {
      btnTogglePublic.addEventListener('click', togglePublicStatus);
    }

    // 삭제 버튼 이벤트 (동적으로 추가될 수 있으므로 이벤트 위임 사용)
    document.addEventListener('click', function (e) {
      const deleteBtn = e.target.closest('.btn-delete');
      if (deleteBtn) {
        e.preventDefault();
        showDeleteConfirmModal(deleteBtn.dataset.videoId);
      }
    });

    // 삭제 확인 버튼 이벤트
    if (btnConfirmDelete) {
      btnConfirmDelete.addEventListener('click', confirmDelete);
    }
  }

  /**
   * URL에서 동영상 ID 추출
   * @returns {number} 동영상 ID
   */
  function getVideoIdFromUrl() {
    const path = window.location.pathname;
    const segments = path.split('/').filter(seg => seg.length > 0);
    const last = segments[segments.length - 1];
    const id = parseInt(last, 10);
    return Number.isNaN(id) ? 0 : id;
  }

  /**
   * 공개/비공개 상태 전환
   */
  async function togglePublicStatus() {
    if (!videoId) {
      showAlert('유효한 동영상 ID가 없습니다.', 'warning');
      return;
    }

    btnTogglePublic.disabled = true;
    const originalBtnText = btnTogglePublic.innerHTML;
    btnTogglePublic.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 처리 중...';

    try {
      const currentStatus = btnTogglePublic.classList.contains(
          'btn-outline-warning');
      const newStatus = !currentStatus; // 현재 공개 상태의 반대

      const res = await adminAuthFetch(
          `/admin/original/video/${videoId}/public`,
          {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({isPublic: newStatus})
          });

      if (!res.ok) {
        throw new Error('서버 응답 오류: ' + res.status);
      }

      const result = await res.json();

      if (result.status.code === 'OK_0000') {
        // UI 업데이트
        if (newStatus) {
          // 비공개 → 공개
          btnTogglePublic.classList.remove('btn-outline-success');
          btnTogglePublic.classList.add('btn-outline-warning');
          btnTogglePublic.innerHTML = '<i class="fas fa-eye-slash"></i> 비공개로 전환';

          // 뱃지 업데이트
          document.querySelectorAll('.badge').forEach(badge => {
            if (badge.textContent.trim() === '비공개') {
              badge.textContent = '공개';
              badge.classList.remove('bg-secondary');
              badge.classList.add('bg-success');
            }
          });

          showAlert('동영상이 공개 상태로 변경되었습니다.', 'success');
        } else {
          // 공개 → 비공개
          btnTogglePublic.classList.remove('btn-outline-warning');
          btnTogglePublic.classList.add('btn-outline-success');
          btnTogglePublic.innerHTML = '<i class="fas fa-eye"></i> 공개로 전환';

          // 뱃지 업데이트
          document.querySelectorAll('.badge').forEach(badge => {
            if (badge.textContent.trim() === '공개') {
              badge.textContent = '비공개';
              badge.classList.remove('bg-success');
              badge.classList.add('bg-secondary');
            }
          });

          showAlert('동영상이 비공개 상태로 변경되었습니다.', 'success');
        }
      } else {
        throw new Error(result.message || '상태 변경 중 오류가 발생했습니다.');
      }
    } catch (err) {
      console.error(err);
      showAlert('상태 변경 중 오류가 발생했습니다: ' + err.message, 'danger');
      btnTogglePublic.innerHTML = originalBtnText;
    } finally {
      btnTogglePublic.disabled = false;
    }
  }

  /**
   * 삭제 확인 모달 표시
   * @param {number} id - 삭제할 동영상 ID
   */
  function showDeleteConfirmModal(id) {
    // 삭제할 동영상 ID 설정
    btnConfirmDelete.dataset.videoId = id || videoId;

    // 모달 표시
    deleteConfirmModal.show();
  }

  /**
   * 삭제 확인 처리
   */
  async function confirmDelete() {
    const targetId = btnConfirmDelete.dataset.videoId;

    if (!targetId) {
      showAlert('삭제할 동영상 ID가 없습니다.', 'warning');
      deleteConfirmModal.hide();
      return;
    }

    btnConfirmDelete.disabled = true;
    btnConfirmDelete.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 삭제 중...';

    try {
      const res = await adminAuthFetch(`/admin/video/delete/${targetId}`, {
        method: 'DELETE'
      });

      if (!res.ok) {
        throw new Error('서버 응답 오류: ' + res.status);
      }

      const result = await res.json();

      if (result.status.code === 'OK_0000') {
        showAlert('동영상이 성공적으로 삭제되었습니다.', 'success');

        // 삭제 성공 시 목록 페이지로 이동 (1초 후)
        setTimeout(() => {
          window.location.href = '/admin/original/video/list';
        }, 1000);
      } else {
        throw new Error(result.message || '삭제 중 오류가 발생했습니다.');
      }
    } catch (err) {
      console.error(err);
      showAlert('삭제 중 오류가 발생했습니다: ' + err.message, 'danger');
      btnConfirmDelete.innerHTML = '삭제';
      btnConfirmDelete.disabled = false;
    } finally {
      deleteConfirmModal.hide();
    }
  }

  /**
   * 조회 추세 차트 초기화
   */
  async function initViewTrendChart() {
    if (!viewTrendChartCanvas) {
      return;
    }

    // 샘플 데이터 (실제로는 API에서 가져옴)
    const path = window.location.pathname.split('/');
    const videoId = path[path.length - 1];

    try {
      const res = await adminAuthFetch(
          `/admin/original/video/${videoId}/views/trend?days=7`);
      const json = await res.json();
      const {labels, values} = json.response;

      renderViewTrendChart(labels.reverse(), values.reverse());
    } catch (err) {
      console.error('조회수 트렌드 로드 실패:', err);
      // 실패 시 기본 차트나 에러 메시지 처리
    }
  }

  let viewTrendChartInstance = null;

  function renderViewTrendChart(labels, data) {
    const ctx = viewTrendChartCanvas.getContext('2d');

    // 기존 차트가 있으면 파괴
    if (viewTrendChartInstance) {
      viewTrendChartInstance.destroy();
    }

    viewTrendChartInstance = new Chart(ctx, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: '일별 조회수',
          data,
          tension: 0.3,
          fill: true,
          backgroundColor: 'rgba(54, 162, 235, 0.2)',
          borderColor: 'rgba(54, 162, 235, 1)',
          borderWidth: 2,
          pointRadius: 4,
          pointBackgroundColor: 'rgba(54, 162, 235, 1)',
          pointBorderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {display: false},
          tooltip: {mode: 'index', intersect: false}
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {precision: 0}
          }
        }
      }
    });
  }

  /**
   * 클립보드 복사 기능 초기화
   */
  function initClipboard() {
    // ClipboardJS 사용
    if (window.ClipboardJS) {
      const clipboard = new ClipboardJS('.btn-copy');

      clipboard.on('success', function (e) {
        showAlert('클립보드에 복사되었습니다.', 'success');
        e.clearSelection();
      });

      clipboard.on('error', function (e) {
        showAlert('복사 중 오류가 발생했습니다. 수동으로 선택하여 복사해주세요.', 'warning');
      });
    }
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