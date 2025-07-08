/**
 * 뉴스 상세 페이지 JavaScript
 */
import {adminAuthFetch} from '/js/commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
  'use strict';

  // DOM 요소
  const btnDelete = document.getElementById('btnDelete');
  const btnConfirmDelete = document.getElementById('btnConfirmDelete');
  const deleteConfirmModal = new bootstrap.Modal(
      document.getElementById('deleteConfirmModal'));
  const btnOpenPreview = document.getElementById('btnOpenPreview');
  const previewFrame = document.getElementById('previewFrame');

  // 뉴스 ID 가져오기 (URL에서 추출)
  const newsId = getNewsIdFromUrl();

  // 초기화
  initPage();

  /**
   * 페이지 초기화 함수
   */
  function initPage() {
    // 이벤트 리스너 등록
    initEventListeners();

    // 페이지 로드 애니메이션
    animatePageLoad();
  }

  /**
   * 이벤트 리스너 초기화
   */
  function initEventListeners() {
    // 삭제 버튼 이벤트
    if (btnDelete) {
      btnDelete.addEventListener('click', function() {
        deleteConfirmModal.show();
      });
    }

    // 삭제 확인 버튼 이벤트
    if (btnConfirmDelete) {
      btnConfirmDelete.addEventListener('click', confirmDelete);
    }

    // 새 창에서 미리보기 버튼 이벤트
    if (btnOpenPreview && previewFrame) {
      btnOpenPreview.addEventListener('click', function() {
        const previewUrl = previewFrame.getAttribute('src');
        if (previewUrl) {
          window.open(previewUrl, '_blank', 'width=375,height=667');
        }
      });
    }
  }

  /**
   * URL에서 뉴스 ID 추출
   * @returns {string|null} 뉴스 ID 또는 null
   */
  function getNewsIdFromUrl() {
    const pathSegments = window.location.pathname.split('/');
    const idIndex = pathSegments.findIndex(segment => segment === 'news') + 1;
    return idIndex > 0 && idIndex < pathSegments.length ? pathSegments[idIndex] : null;
  }

  /**
   * 삭제 확인 처리
   */
  async function confirmDelete() {
    if (!newsId) {
      showNotification('삭제할 뉴스 ID가 없습니다.', 'warning');
      deleteConfirmModal.hide();
      return;
    }

    // 버튼 상태 업데이트
    btnConfirmDelete.disabled = true;
    btnConfirmDelete.innerHTML = '<i class="mdi mdi-loading mdi-spin me-1"></i> 삭제 중...';

    try {
      const res = await adminAuthFetch(`/admin/news/delete/${newsId}`, {
        method: 'DELETE'
      });

      if (!res.ok) {
        throw new Error('서버 응답 오류: ' + res.status);
      }

      const result = await res.json();

      if (result.status.code === 'OK_0000') {
        showNotification('뉴스가 성공적으로 삭제되었습니다.', 'success');

        // 삭제 성공 후 목록 페이지로 이동
        setTimeout(() => {
          // 페이드아웃 효과 후 이동
          document.body.classList.add('fade-out');
          setTimeout(() => {
            window.location.href = '/admin/original/news/list';
          }, 300);
        }, 1000);
      } else {
        throw new Error(result.message || '삭제 중 오류가 발생했습니다.');
      }
    } catch (err) {
      console.error(err);
      showNotification('삭제 중 오류가 발생했습니다: ' + err.message, 'danger');
    } finally {
      btnConfirmDelete.innerHTML = '<i class="mdi mdi-trash-can me-1"></i> 삭제';
      btnConfirmDelete.disabled = false;
      deleteConfirmModal.hide();
    }
  }

  /**
   * 알림 표시 함수
   * @param {string} message - 알림 메시지
   * @param {string} type - 알림 타입 (success, danger, warning, info)
   */
  function showNotification(message, type = 'info') {
    // Hyper 테마 토스트 알림 사용 (Hyper 템플릿에 포함된 경우)
    if (window.Hyper && window.Hyper.Toast) {
      window.Hyper.Toast.show({
        text: message,
        position: 'top-right',
        hideAfter: 5000,
        type: type
      });
      return;
    }

    // 커스텀 알림 시스템 사용
    if (window.CustomNotification) {
      window.CustomNotification.show(message, type);
      return;
    }

    // 기본 alert 사용
    alert(message);
  }

  /**
   * 페이지 로드 시 애니메이션 효과
   */
  function animatePageLoad() {
    // 카드 요소들을 순차적으로 페이드인
    const cards = document.querySelectorAll('.card');
    cards.forEach((card, index) => {
      card.style.opacity = '0';
      card.style.transform = 'translateY(20px)';
      card.style.transition = 'opacity 0.3s ease, transform 0.3s ease';

      setTimeout(() => {
        card.style.opacity = '1';
        card.style.transform = 'translateY(0)';
      }, 100 * (index + 1));
    });
  }

  // 페이지 떠날 때 페이드 아웃 효과를 위한 스타일 추가
  const style = document.createElement('style');
  style.textContent = `
    body.fade-out {
      opacity: 0;
      transition: opacity 0.3s ease;
    }
  `;
  document.head.appendChild(style);
});