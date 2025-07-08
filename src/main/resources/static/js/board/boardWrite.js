// boardWrite.js
import {
  handleTokenRefresh,
  authFetch,
  handleFetchError,
  FetchError
} from '../commonFetch.js';

const MAX_IMAGE_COUNT = 3;
const MAX_IMAGE_SIZE = 5 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

let uploadedImages = [];

document.addEventListener('DOMContentLoaded', () => {
  initBackButton();
  initImageUpload();
  initFormSubmit();
  initFormInputs();
});

function initBackButton() {
  document.getElementById('backButton')?.addEventListener('click', () => {
    const title = document.getElementById('title').value.trim();
    const content = document.getElementById('content').value.trim();
    if (title || content || uploadedImages.length) {
      if (confirm('작성 중인 내용이 있습니다. 페이지를 나가시겠습니까?')) {
        location.href = 'boardList.html';
      }
    } else {
      location.href = 'boardList.html';
    }
  });
}

function initImageUpload() {
  const uploadBox = document.getElementById('uploadBox');
  const fileInput = document.getElementById('fileInput');
  const previewContainer = document.getElementById('imagePreviewContainer');

  uploadBox.addEventListener('click', () => {
    if (uploadedImages.length >= MAX_IMAGE_COUNT) {
      return alert(`이미지는 최대 ${MAX_IMAGE_COUNT}장까지 업로드 가능합니다.`);
    }
    fileInput.click();
  });

  fileInput.addEventListener('change', e => {
    const files = [...e.target.files];
    if (files.length + uploadedImages.length > MAX_IMAGE_COUNT) {
      return alert(`이미지는 최대 ${MAX_IMAGE_COUNT}장까지 가능합니다. 현재 ${uploadedImages.length}장 업로드됨.`);
    }
    files.forEach(file => {
      if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
        return alert('지원되지 않는 이미지 형식입니다.');
      }
      if (file.size > MAX_IMAGE_SIZE) {
        return alert(`이미지 크기는 최대 5MB까지 가능합니다.`);
      }
      const reader = new FileReader();
      reader.onload = ev => {
        uploadedImages.push(file);
        const idx = uploadedImages.length - 1;
        const div = document.createElement('div');
        div.className = 'image-preview';
        div.innerHTML = `
          <img src="${ev.target.result}" />
          <div class="delete-image" data-index="${idx}"><i class="fas fa-times"></i></div>
        `;
        div.querySelector('.delete-image').addEventListener('click', evt => {
          evt.stopPropagation();
          deleteImage(idx, div);
        });
        previewContainer.append(div);
        updateUploadBox();
      };
      reader.readAsDataURL(file);
    });
    fileInput.value = '';
  });
}

function deleteImage(index, el) {
  uploadedImages.splice(index, 1);
  el.remove();
  // 인덱스 재배치
  document.querySelectorAll('.delete-image').forEach((btn, i) => {
    btn.dataset.index = i;
  });
  updateUploadBox();
}

function updateUploadBox() {
  const uploadBox = document.getElementById('uploadBox');
  if (uploadedImages.length >= MAX_IMAGE_COUNT) {
    uploadBox.style.opacity = '0.5';
    uploadBox.style.cursor = 'not-allowed';
  } else {
    uploadBox.style.opacity = '1';
    uploadBox.style.cursor = 'pointer';
  }
}

function initFormSubmit() {
  document.getElementById('postForm').addEventListener('submit', async e => {
    e.preventDefault();
    const title = document.getElementById('title').value.trim();
    const content = document.getElementById('content').value.trim();
    if (!title) return alert('제목을 입력해주세요.');

    const formData = new FormData();
    formData.append('title', title);
    formData.append('content', content);
    uploadedImages.forEach(file => formData.append('images', file));

    try {
      // 인증 필수 API 호출
      const res = await authFetch('/api/board/posts', {
        method: 'POST',
        body: formData
      });
      const { response: post } = await res.json();
      // 작성 완료 후 상세로 이동
      location.href = `boardDetail.html?id=${post.boardId}`;
    } catch (err) {
      if (err instanceof FetchError && err.httpStatus === 401) {
        alert('로그인이 필요한 기능입니다.');
        location.href = '/login/login.html';
      } else {
        handleFetchError(err);
      }
    }
  });
}

function initFormInputs() {
  const title = document.getElementById('title');
  const content = document.getElementById('content');
  [title, content].forEach(el =>
    el.addEventListener('input', () => {
      const btn = document.getElementById('submitButton');
      btn.classList.toggle('active', title.value.trim() !== '');
    })
  );
}
