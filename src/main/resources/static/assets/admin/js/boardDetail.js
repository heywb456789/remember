// assets/admin/js/boardDetail.js
import { adminAuthFetch } from '../../../js/commonFetch.js';

document.addEventListener('DOMContentLoaded', () => {
  // DOM 요소
  const boardId = getBoardIdFromUrl();
  const btnEdit = document.getElementById('btnEdit');
  const btnDelete = document.getElementById('btnDelete');
  const btnConfirmDelete = document.getElementById('btnConfirmDelete');
  const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
  const commentForm = document.getElementById('commentForm');
  const commentContent = document.getElementById('commentContent');
  const btnAddComment = document.getElementById('btnAddComment');
  const commentDeleteModal = new bootstrap.Modal(document.getElementById('commentDeleteModal'));
  const btnConfirmCommentDelete = document.getElementById('btnConfirmCommentDelete');

  let currentCommentId = null;

  // 이벤트 리스너 등록
  initEventListeners();

  /**
   * 이벤트 리스너 초기화
   */
  function initEventListeners() {
    // 게시글 수정 버튼
    // btnEdit?.addEventListener('click', () => {
    //   window.location.href = `/admin/board/edit/${boardId}`;
    // });

    // 게시글 삭제 버튼
    btnDelete?.addEventListener('click', () => {
      deleteModal.show();
    });

    // 게시글 삭제 확인 버튼
    btnConfirmDelete?.addEventListener('click', async () => {
      try {
        await deleteBoard();
        window.location.href = '/admin/board';
      } catch (error) {
        console.error('게시글 삭제 오류:', error);
        alert('게시글 삭제 중 오류가 발생했습니다.');
      } finally {
        deleteModal.hide();
      }
    });

    // 댓글 폼 제출
    commentForm?.addEventListener('submit', async (e) => {
      e.preventDefault();

      const content = commentContent.value.trim();
      if (!content) {
        alert('댓글 내용을 입력해주세요.');
        commentContent.focus();
        return;
      }

      try {
        await addComment(content);
        commentContent.value = '';
        location.reload(); // 댓글 추가 후 페이지 새로고침
      } catch (error) {
        console.error('댓글 등록 오류:', error);
        alert('댓글 등록 중 오류가 발생했습니다.');
      }
    });

    // 댓글 수정 버튼 클릭 이벤트 핸들러 (boardDetail.js 파일 내에서 수정)
document.querySelectorAll('.comment-edit').forEach(btn => {
  btn.addEventListener('click', (e) => {
    const commentId = e.currentTarget.getAttribute('data-id');
    const commentDiv = e.currentTarget.closest('.comment');
    const commentTextElement = commentDiv.querySelector('.comment-text');
    const originalText = commentTextElement.textContent;

    // 텍스트 영역을 수정 가능한 텍스트 입력으로 변경
    commentTextElement.innerHTML = `
      <div class="edit-comment-form">
        <textarea class="form-control">${originalText}</textarea>
        <div class="btn-group">
          <button type="button" class="btn btn-secondary cancel-edit">취소</button>
          <button type="button" class="btn btn-primary save-edit">저장</button>
        </div>
      </div>
    `;

    // 텍스트 영역에 포커스 주기
    const textarea = commentTextElement.querySelector('textarea');
    textarea.focus();
    textarea.setSelectionRange(textarea.value.length, textarea.value.length);

    // 취소 버튼
    commentDiv.querySelector('.cancel-edit').addEventListener('click', () => {
      commentTextElement.textContent = originalText;
    });

    // 저장 버튼
    commentDiv.querySelector('.save-edit').addEventListener('click', async () => {
      const newText = commentDiv.querySelector('textarea').value.trim();
      if (!newText) {
        alert('댓글 내용을 입력해주세요.');
        return;
      }

      try {
        // 저장 버튼 비활성화 및 로딩 상태 표시
        const saveButton = commentDiv.querySelector('.save-edit');
        saveButton.disabled = true;
        saveButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 저장 중...';

        await updateComment(commentId, newText);
        commentTextElement.textContent = newText;
      } catch (error) {
        console.error('댓글 수정 오류:', error);
        alert('댓글 수정 중 오류가 발생했습니다.');
        commentTextElement.textContent = originalText;
      }
    });
  });
});

    // 댓글 삭제 버튼
    document.querySelectorAll('.comment-delete').forEach(btn => {
      btn.addEventListener('click', (e) => {
        currentCommentId = e.currentTarget.getAttribute('data-id');
        commentDeleteModal.show();
      });
    });

    // 댓글 삭제 확인 버튼
    btnConfirmCommentDelete?.addEventListener('click', async () => {
      if (!currentCommentId) return;

      try {
        await deleteComment(currentCommentId);
        location.reload(); // 댓글 삭제 후 페이지 새로고침
      } catch (error) {
        console.error('댓글 삭제 오류:', error);
        alert('댓글 삭제 중 오류가 발생했습니다.');
      } finally {
        commentDeleteModal.hide();
        currentCommentId = null;
      }
    });

    // 이미지 클릭 시 원본 크기로 보기
    document.querySelectorAll('.image-link').forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        openLightbox(e.currentTarget.href);
      });
    });
  }

  /**
   * URL에서 게시글 ID 추출
   */
  function getBoardIdFromUrl() {
    const pathSegments = window.location.pathname.split('/');
    return pathSegments[pathSegments.length - 1];
  }

  /**
   * 게시글 삭제
   */
  async function deleteBoard() {
    return adminAuthFetch(`/admin/board/${boardId}`, {
      method: 'DELETE'
    });
  }

  /**
   * 댓글 추가
   */
  async function addComment(content) {
    return adminAuthFetch(`/admin/board/${boardId}/comment`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ content })
    });
  }

  /**
   * 댓글 수정
   */
  async function updateComment(commentId, content) {
    return adminAuthFetch(`/admin/board/comments/${commentId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ content })
    });
  }

  /**
   * 댓글 삭제
   */
  async function deleteComment(commentId) {
    return adminAuthFetch(`/admin/board/comments/${commentId}`, {
      method: 'DELETE'
    });
  }

  /**
   * 이미지 라이트박스 열기
   */
  function openLightbox(imageUrl) {
    // 라이트박스 컨테이너 생성
    const lightbox = document.createElement('div');
    lightbox.className = 'lightbox';
    lightbox.innerHTML = `
      <div class="lightbox-content">
        <img src="${imageUrl}" alt="원본 이미지">
        <button class="lightbox-close">&times;</button>
      </div>
    `;

    // 라이트박스에 스타일 추가
    const style = document.createElement('style');
    style.textContent = `
      .lightbox {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.8);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 9999;
      }
      .lightbox-content {
        position: relative;
        max-width: 90%;
        max-height: 90%;
      }
      .lightbox-content img {
        max-width: 100%;
        max-height: 90vh;
        display: block;
        margin: 0 auto;
      }
      .lightbox-close {
        position: absolute;
        top: -40px;
        right: -10px;
        font-size: 30px;
        color: white;
        background: transparent;
        border: none;
        cursor: pointer;
      }
    `;

    document.head.appendChild(style);
    document.body.appendChild(lightbox);

    // 닫기 버튼 및 외부 클릭 이벤트
    const closeBtn = lightbox.querySelector('.lightbox-close');
    closeBtn.addEventListener('click', () => {
      document.body.removeChild(lightbox);
    });

    lightbox.addEventListener('click', (e) => {
      if (e.target === lightbox) {
        document.body.removeChild(lightbox);
      }
    });
  }
});