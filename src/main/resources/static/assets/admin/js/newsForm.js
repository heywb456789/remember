/**
 * 뉴스 작성/수정 페이지 JavaScript - TinyMCE 에디터 적용 버전
 */
import {adminAuthFetch} from '/js/commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
  'use strict';

  // DOM 요소
  const newsForm = document.getElementById('newsForm');
  const titleInput = document.getElementById('title');
  const subtitleInput = document.getElementById('subtitle');
  const authorInput = document.getElementById('author');
  const categorySelect = document.getElementById('category');
  const tagsInput = document.getElementById('tags');
  const thumbnailUrlInput = document.getElementById('thumbnailUrl');
  const thumbnailPreview = document.getElementById('thumbnailPreview');
  const thumbnailPlaceholder = document.getElementById('thumbnailPlaceholder');

  const btnPublish = document.getElementById('btnPublish');
  // const btnPreview = document.getElementById('btnPreview');
  const btnUploadThumbnail = document.getElementById('btnUploadThumbnail');

  const btnMobileView = document.getElementById('btnMobileView');
  const mobilePreview = document.getElementById('mobilePreview');

  const previewTitle = document.getElementById('previewTitle');
  const previewSubtitle = document.getElementById('previewSubtitle');
  const previewCategory = document.getElementById('previewCategory');
  const previewAuthor = document.getElementById('previewAuthor');
  const previewDate = document.getElementById('previewDate');
  const previewImageContainer = document.getElementById(
      'previewImageContainer');
  const previewImage = document.getElementById('previewImage');
  const previewContent = document.getElementById('previewContent');
  const previewTags = document.getElementById('previewTags');
  const previewLoading = document.getElementById('previewLoading');

  // 모달 요소
  const uploadThumbnailModal = new bootstrap.Modal(
      document.getElementById('uploadThumbnailModal'));
  const confirmModal = new bootstrap.Modal(
      document.getElementById('confirmModal'));
  const saveProgressModal = new bootstrap.Modal(
      document.getElementById('saveProgressModal'));

  // 썸네일 업로드 관련 요소
  const btnConfirmUpload = document.getElementById('btnConfirmUpload');
  const thumbnailFile = document.getElementById('thumbnailFile');
  const uploadPreview = document.getElementById('uploadPreview');
  const uploadPlaceholder = document.getElementById('uploadPlaceholder');

  // 저장 진행 상태 관련 요소
  const saveProgressBar = document.getElementById('saveProgressBar');
  const saveProgressStatus = document.getElementById('saveProgressStatus');

  // 뉴스 ID와 모드 저장

  // 현재 작업 상태
  let isSubmitting = false;
  let isDirty = false;
  let currentAction = null;

  // TinyMCE 에디터 인스턴스
  let editor = null;

  // 임시 썸네일 저장
  let tempThumbnailFile = null;
  let tempThumbnailDataUrl = null;

  // 초기화
  initPage();

  /**
   * 페이지 초기화 함수
   */
  function initPage() {
    // TinyMCE 에디터 초기화
    initTinyMCEEditor();

    // 날짜 선택기 초기화
    initDatePicker();

    // 현재 날짜로 미리보기 날짜 설정
    updatePreviewDate();

    // 이벤트 리스너 등록
    initEventListeners();

    // 페이지 이탈 감지
    window.addEventListener('beforeunload', handleBeforeUnload);

    // 초기 미리보기 업데이트
    setTimeout(updatePreview, 500);
  }

  /**
   * TinyMCE 에디터 초기화
   */
  function initTinyMCEEditor() {
    // TinyMCE 초기화
    tinymce.init({
      language: 'ko_KR',
      language_url: '/assets/admin/tinymce/langs/ko_KR.js', // 언어 파일 경로 수정
      selector: '#content-editor',
      height: 500,
      menubar: true,
      plugins: [
        'advlist', 'autolink', 'lists', 'link', 'image', 'charmap', 'preview',
        'anchor', 'searchreplace', 'visualblocks', 'code', 'fullscreen',
        'insertdatetime', 'media', 'table', 'help', 'wordcount'
      ],
      toolbar: 'undo redo | blocks | ' +
          'bold italic underline strikethrough | alignleft aligncenter ' +
          'alignright alignjustify | bullist numlist outdent indent | ' +
          'removeformat | image link media | customCallout customSource | help',
      content_css: [
        '/assets/admin/css/newsEditor.css',
        'https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@300;400;500;700&family=Nanum+Gothic:wght@400;700&display=swap'
      ],
      content_style: `
      body { font-family: 'Noto Sans KR', sans-serif; font-size: 16px; }
      .news-callout { background-color: rgba(114, 124, 245, 0.1); border-left: 4px solid #727cf5; padding: 1rem; margin: 1rem 0; border-radius: 0 0.25rem 0.25rem 0; }
      .news-source { color: #6c757d; font-size: 0.9rem; font-style: italic; margin-top: 0.5rem; text-align: right; }
      img.image-wrap-right {
      float: right;
      margin: 0 0 10px 15px;
      max-width: 50%;
    }
    img.image-wrap-left {
      float: left;
      margin: 0 15px 10px 0;
      max-width: 50%;
    }
    img.image-align-center {
      display: block;
      margin: 10px auto;
      max-width: 100%;
    }
    /* 중요: 이미지 다음의 단락이 float 영향을 받지 않도록 */
    p { margin: 0 0 1em 0; }
    .clearfix::after {
      content: "";
      clear: both;
      display: table;
    }
    `,
      font_formats:
          '맑은 고딕=Malgun Gothic,Apple SD Gothic Neo,sans-serif;' +
          '나눔고딕=Nanum Gothic,sans-serif;' +
          'Noto Sans KR=Noto Sans KR,sans-serif;' +
          '굴림=Gulim,sans-serif;' +
          '돋움=Dotum,sans-serif;' +
          '바탕=Batang,serif;' +
          'Arial=arial,helvetica,sans-serif;' +
          'Arial Black=arial black,avant garde;' +
          'Georgia=georgia,palatino;' +
          'Tahoma=tahoma,arial,helvetica,sans-serif;' +
          'Times New Roman=times new roman,times;' +
          'Verdana=verdana,geneva;',
      fontsize_formats: '8pt 10pt 12pt 14pt 16pt 18pt 20pt 24pt 36pt',

      // 이미지 관련 설정 강화
      image_advtab: true,
      image_caption: true,
      image_title: true,
      image_dimensions: false,
      paste_data_images: true, // 붙여넣기로 이미지 업로드 활성화
      automatic_uploads: false, // 자동 업로드 활성화
      file_picker_types: 'image', // 파일 선택기에서 이미지만 선택 가능하도록
      // 다른 설정들...
      image_class_list: [
        {title: '없음', value: ''},
        {
          title: '오른쪽 정렬 (텍스트 감싸기)', value: 'image-wrap-right', styles: {
            'float': 'right',
            'margin': '0 0 10px 15px',
            'max-width': '50%'
          }
        },
        {
          title: '왼쪽 정렬 (텍스트 감싸기)', value: 'image-wrap-left', styles: {
            'float': 'left',
            'margin': '0 15px 10px 0',
            'max-width': '50%'
          }
        },
        {
          title: '가운데 정렬', value: 'image-align-center', styles: {
            'display': 'block',
            'margin': '10px auto',
            'max-width': '100%'
          }
        }
      ],

      // 이미지 삽입 전 설정 추가
      images_upload_handler: function (blobInfo, progress) {
        return new Promise((resolve, reject) => {
          // 파일 객체 생성
          const file = blobInfo.blob();

          // 파일 유형 검사
          if (!file.type.startsWith('image/')) {
            reject('이미지 파일만 업로드 가능합니다.');
            return;
          }

          // 파일 크기 검사 (10MB 제한)
          if (file.size > 10 * 1024 * 1024) {
            reject('이미지 크기가 10MB를 초과할 수 없습니다.');
            return;
          }

          // FormData 생성
          const formData = new FormData();
          formData.append('file', file, blobInfo.filename() || 'image.png');
          formData.append('type', 'news_content');

          // 서버에 업로드 요청
          adminAuthFetch('/admin/upload/image', {
            method: 'POST',
            body: formData
          })
          .then(response => {
            if (!response.ok) {
              throw new Error(`이미지 업로드 실패: ${response.status}`);
            }
            return response.json();
          })
          .then(result => {
            progress(100);

            if (result.status && result.status.code === 'OK_0000') {
              // 이미지 URL 얻기
              const imageUrl = result.data.url;

              // 이미지 URL 반환
              resolve(imageUrl);

              // 변경사항 감지
              isDirty = true;
            } else {
              reject(result.message || '이미지 업로드 중 오류가 발생했습니다.');
            }
          })
          .catch(error => {
            console.error('이미지 업로드 오류:', error);
            reject(error.message || '이미지 업로드 중 오류가 발생했습니다.');
          });
        });
      },

      // file_picker_callback는 제거하고 images_upload_handler에 통합

      // 에디터 설정
      setup: function (editor) {
        // 에디터 인스턴스 저장
        window.editor = editor;

        // 강조 상자 버튼 추가
        editor.ui.registry.addButton('customCallout', {
          icon: 'comment-add',
          tooltip: '강조 상자 삽입',
          onAction: function () {
            editor.execCommand('mceInsertContent', false,
                '<div class="news-callout">여기에 강조할 내용을 입력하세요.</div>'
            );
          }
        });

        // 출처 버튼 추가
        editor.ui.registry.addButton('customSource', {
          icon: 'cite',
          tooltip: '출처 삽입',
          onAction: function () {
            editor.execCommand('mceInsertContent', false,
                '<p class="news-source">출처: 출처를 입력하세요</p>'
            );
          }
        });

        // 에디터 내용 변경 이벤트
        editor.on('Change', function () {
          isDirty = true;
          debounce(updatePreview, 500)();
        });

        // 이미지 업로드 디버깅
        editor.on('init', function () {
          console.log('TinyMCE 초기화 완료');

          // 미리보기 초기 업데이트
          setTimeout(updatePreview, 500);
        });

        // 이미지 업로드 디버깅
        editor.on('UploadImage', function (e) {
          console.log('이미지 업로드 이벤트:', e);
        });

        // 이미지 삽입 후 이벤트
        editor.on('SetContent', function (e) {
          // 내용이 변경되었을 때의 로직
          if (e.content && e.content.indexOf('<img') !== -1) {
            // 이미지가 포함된 경우
            console.log('이미지 콘텐츠 감지됨');
          }
        });
      }
    });
  }

  /**
   * 이미지 업로드 핸들러
   * @param {Object} blobInfo - 이미지 파일 정보
   * @param {Function} success - 성공 콜백 함수
   * @param {Function} failure - 실패 콜백 함수
   */
  function handleImageUpload(blobInfo, success, failure) {
    // 파일 객체 생성
    const file = blobInfo.blob();

    // 파일 유형 검사
    if (!file.type.startsWith('image/')) {
      failure('이미지 파일만 업로드 가능합니다.');
      return;
    }

    // 파일 크기 검사 (10MB 제한)
    if (file.size > 10 * 1024 * 1024) {
      failure('이미지 크기가 10MB를 초과할 수 없습니다.');
      return;
    }

    // FormData 생성
    const formData = new FormData();
    formData.append('file', file, file.name || 'image.png');
    formData.append('type', 'news_content');

    // 프로그레스 표시
    updateSaveProgressUI(10, '이미지 업로드 중...');
    saveProgressModal.show();

    // 서버에 업로드 요청
    adminAuthFetch('/admin/upload/image', {
      method: 'POST',
      body: formData
    })
    .then(response => {
      if (!response.ok) {
        throw new Error(`이미지 업로드 실패: ${response.status}`);
      }
      return response.json();
    })
    .then(result => {
      updateSaveProgressUI(100, '이미지 업로드 완료!');
      setTimeout(() => {
        saveProgressModal.hide();
      }, 500);

      if (result.status.code === 'OK_0000') {
        // 성공 시 이미지 URL 반환
        success(result.data.url);

        // 미리보기 업데이트
        setTimeout(updatePreview, 500);

        // 변경사항 감지
        isDirty = true;
      } else {
        failure(result.message || '이미지 업로드 중 오류가 발생했습니다.');
      }
    })
    .catch(error => {
      updateSaveProgressUI(0, '이미지 업로드 실패');
      setTimeout(() => {
        saveProgressModal.hide();
      }, 500);

      console.error('이미지 업로드 오류:', error);
      failure(error.message || '이미지 업로드 중 오류가 발생했습니다.');
    });
  }

  /**
   * 날짜 선택기 초기화
   */
  function initDatePicker() {
    if (typeof flatpickr !== 'undefined') {
      flatpickr('#publishDate', {
        enableTime: true,
        dateFormat: 'Y-m-d H:i',
        time_24hr: true,
        locale: 'ko',
        allowInput: true,
        minDate: 'today'
      });
    }
  }

  /**
   * 이벤트 리스너 초기화
   */
  function initEventListeners() {
    // 폼 입력 변경 감지
    if (titleInput) {
      titleInput.addEventListener('input', function () {
        isDirty = true;
        debounce(updatePreview, 300)();
      });
    }

    if (subtitleInput) {
      subtitleInput.addEventListener('input', function () {
        isDirty = true;
        debounce(updatePreview, 300)();
      });
    }

    if (authorInput) {
      authorInput.addEventListener('input', function () {
        isDirty = true;
        debounce(updatePreview, 300)();
      });
    }

    if (categorySelect) {
      categorySelect.addEventListener('change', function () {
        isDirty = true;
        debounce(updatePreview, 300)();
      });
    }

    if (tagsInput) {
      tagsInput.addEventListener('input', function () {
        isDirty = true;
        debounce(updatePreview, 300)();
      });
    }


    if (btnPublish) {
      btnPublish.addEventListener('click', function () {
        if (validateForm()) {
          currentAction = 'publish';
          const confirmModalMessage = document.getElementById(
              'confirmModalMessage');
          const btnConfirmAction = document.getElementById('btnConfirmAction');

          if (confirmModalMessage && btnConfirmAction) {
            confirmModalMessage.textContent = '뉴스를 발행하시겠습니까? 발행 후에는 즉시 사용자에게 공개됩니다.';
            btnConfirmAction.textContent = '발행';
            btnConfirmAction.className = 'btn btn-primary';
            confirmModal.show();
          }
        }
      });
    }

    // 썸네일 업로드 관련
    if (btnUploadThumbnail) {
      btnUploadThumbnail.addEventListener('click', function () {
        uploadThumbnailModal.show();
      });
    }

    if (thumbnailFile) {
      thumbnailFile.addEventListener('change', function (e) {
        previewThumbnail(e.target);
      });
    }

    if (btnConfirmUpload) {
      btnConfirmUpload.addEventListener('click', function () {
        saveTempThumbnail();
      });
    }

    // 전체 페이지 미리보기
    // if (btnPreview) {
    //   btnPreview.addEventListener('click', function () {
    //     openFullPreview();
    //   });
    // }

    // 확인 모달 버튼 이벤트
    document.getElementById('btnConfirmAction')?.addEventListener('click',
        function () {
          if (currentAction === 'publish') {
            saveNews(true);
          }
          confirmModal.hide();
        });
  }

  /**
   * 폼 유효성 검사
   * @returns {boolean} 유효성 검사 통과 여부
   */
  function validateForm() {
    let isValid = true;

    // 제목 검사
    if (!titleInput.value.trim()) {
      titleInput.classList.add('is-invalid');
      isValid = false;
    } else {
      titleInput.classList.remove('is-invalid');
    }

    // 카테고리 검사
    if (!categorySelect.value) {
      categorySelect.classList.add('is-invalid');
      isValid = false;
    } else {
      categorySelect.classList.remove('is-invalid');
    }

    // 작성자 검사
    if (!authorInput.value.trim()) {
      authorInput.classList.add('is-invalid');
      isValid = false;
    } else {
      authorInput.classList.remove('is-invalid');
    }

    // 내용 검사
    const content = tinymce.get('content-editor').getContent();
    const contentText = tinymce.get('content-editor').getContent(
        {format: 'text'});

    if (!contentText.trim()) {
      document.querySelector('.tox-tinymce').classList.add('is-invalid');
      document.querySelector('.editor-feedback').style.display = 'block';
      isValid = false;
    } else {
      document.querySelector('.tox-tinymce').classList.remove('is-invalid');
      document.querySelector('.editor-feedback').style.display = 'none';
    }

    if (!isValid) {
      showAlert('필수 항목을 모두 입력해주세요.', 'warning');
    }

    return isValid;
  }

  /**
   * 전체 페이지 미리보기 창 열기
   */
  function openFullPreview() {
    // 에디터에서 현재 내용 가져오기
    const content = tinymce.get('content-editor').getContent();

    // 미리보기 URL 생성
    const previewUrl = `/preview/news/temp?` +
        `title=${encodeURIComponent(titleInput.value || '')}` +
        `&subtitle=${encodeURIComponent(subtitleInput?.value || '')}` +
        `&content=${encodeURIComponent(content || '')}` +
        `&thumbnail=${encodeURIComponent(
            thumbnailUrlInput.value || (tempThumbnailDataUrl || ''))}` +
        `&author=${encodeURIComponent(authorInput.value || '')}` +
        `&category=${encodeURIComponent(
            categorySelect.options[categorySelect.selectedIndex]?.text || '')}`
        +
        `&tags=${encodeURIComponent(tagsInput?.value || '')}`;

    // 새 창에서 미리보기 열기
    window.open(previewUrl, '_blank');
  }

  /**
   * 미리보기 업데이트
   */
  function updatePreview() {
    // 로딩 표시
    if (previewLoading) {
      previewLoading.style.display = 'flex';
    }

    // 카테고리 업데이트
    const categoryText = categorySelect.options[categorySelect.selectedIndex]?.text
        || '카테고리';
    if (previewCategory) {
      previewCategory.textContent = categoryText;
    }

    // 제목 업데이트
    const title = titleInput.value || '뉴스 제목';
    if (previewTitle) {
      previewTitle.textContent = title;
    }

    // 소제목 업데이트
    const subtitle = subtitleInput?.value || '';
    if (previewSubtitle) {
      if (subtitle) {
        previewSubtitle.textContent = subtitle;
        previewSubtitle.style.display = 'block';
      } else {
        previewSubtitle.style.display = 'none';
      }
    }

    // 작성자 업데이트
    const author = authorInput.value || '작성자';
    if (previewAuthor) {
      previewAuthor.textContent = author;
    }

    // 태그 업데이트
    const tags = tagsInput?.value || '';
    if (previewTags) {
      updatePreviewTags(tags, previewTags);
    }

    // 썸네일 업데이트
    // updatePreviewThumbnail();

    // 내용 업데이트
    updatePreviewContent();

    // 로딩 숨김
    setTimeout(() => {
      if (previewLoading) {
        previewLoading.style.display = 'none';
      }
    }, 300);
  }

  /**
   * 미리보기 썸네일 업데이트
   */
  function updatePreviewThumbnail() {
    // 썸네일 이미지 업데이트
    const thumbnailUrl = thumbnailUrlInput.value || tempThumbnailDataUrl || '';

    if (thumbnailUrl && previewImageContainer && previewImage) {
      previewImage.src = thumbnailUrl;
      previewImageContainer.style.display = 'block';
    } else if (previewImageContainer) {
      previewImageContainer.style.display = 'none';
    }
  }

  /**
   * 미리보기 내용 업데이트
   */
  function updatePreviewContent() {
    // TinyMCE에서 현재 내용 가져오기
    if (tinymce.get('content-editor')) {
      const content = tinymce.get('content-editor').getContent();

      // 내용이 있으면 미리보기 업데이트
      if (content && previewContent) {
        previewContent.innerHTML = content;
      } else if (previewContent) {
        previewContent.innerHTML = '<p>내용이 없습니다.</p>';
      }
    }
  }

  /**
   * 미리보기 태그 업데이트
   * @param {string} tagsString - 쉼표로 구분된 태그 문자열
   * @param {HTMLElement} container - 태그를 표시할 컨테이너
   */
  function updatePreviewTags(tagsString, container) {
    container.innerHTML = '';

    if (!tagsString) {
      container.style.display = 'none';
      return;
    }

    const tags = tagsString.split(',').map(tag => tag.trim()).filter(
        tag => tag);

    if (tags.length === 0) {
      container.style.display = 'none';
      return;
    }

    container.style.display = 'flex';

    tags.forEach(tag => {
      const tagElement = document.createElement('span');
      tagElement.classList.add('preview-tag');
      tagElement.textContent = tag;
      container.appendChild(tagElement);
    });
  }

  /**
   * 현재 날짜로 미리보기 날짜 업데이트
   */
  function updatePreviewDate() {
    const now = new Date();
    const formattedDate = now.getFullYear() + '-' +
        String(now.getMonth() + 1).padStart(2, '0') + '-' +
        String(now.getDate()).padStart(2, '0');

    if (previewDate) {
      previewDate.textContent = formattedDate;
    }
  }

  /**
   * 썸네일 이미지 미리보기
   * @param {HTMLInputElement} input - 파일 입력 요소
   */
  function previewThumbnail(input) {
    if (input.files && input.files[0]) {
      const file = input.files[0];

      // 파일 유형 검사
      if (!file.type.startsWith('image/')) {
        showAlert('이미지 파일만 업로드 가능합니다.', 'warning');
        input.value = '';
        return;
      }

      // 파일 크기 검사 (5MB 제한)
      if (file.size > 5 * 1024 * 1024) {
        showAlert('이미지 크기가 5MB를 초과할 수 없습니다.', 'warning');
        input.value = '';
        return;
      }

      const reader = new FileReader();

      reader.onload = function (e) {
        uploadPreview.src = e.target.result;
        uploadPreview.style.display = 'block';
        uploadPlaceholder.style.display = 'none';
        btnConfirmUpload.disabled = false;
      };

      reader.readAsDataURL(file);
    } else {
      uploadPreview.style.display = 'none';
      uploadPlaceholder.style.display = 'flex';
      btnConfirmUpload.disabled = true;
    }
  }

  /**
   * 썸네일 임시 저장
   */
  function saveTempThumbnail() {
    if (!thumbnailFile.files || !thumbnailFile.files[0]) {
      showAlert('업로드할 이미지를 선택해주세요.', 'warning');
      return;
    }

    // 썸네일 파일 임시 저장
    tempThumbnailFile = thumbnailFile.files[0];
    tempThumbnailDataUrl = uploadPreview.src;

    // 썸네일 미리보기 업데이트
    thumbnailPreview.src = tempThumbnailDataUrl;
    thumbnailPreview.style.display = 'block';
    thumbnailPlaceholder.style.display = 'none';

    // 모달 닫기
    uploadThumbnailModal.hide();

    // 미리보기 업데이트
    updatePreview();

    // 변경사항 감지
    isDirty = true;

    showAlert('썸네일이 추가되었습니다. 글 저장 시 서버에 업로드됩니다.', 'success');
  }

  /**
   * 뉴스 저장 (임시저장 또는 발행)
   * @param {boolean} isPublish - 발행 여부
   */
  /**
   * 뉴스 저장 (임시저장 또는 발행)
   * @param {boolean} isPublish - 발행 여부
   */
  async function saveNews(isPublish) {
  if (isSubmitting) {
    return;
  }
  if (isPublish && !validateForm()) {
    return;
  }

  isSubmitting = true;
  updateButtonStates(true);
  updateSaveProgressUI(10, '저장 준비 중...');
  saveProgressModal.show();

  try {
    // FormData 생성
    const formData = new FormData();

    // 기본 텍스트 필드 추가
    formData.append('title', titleInput.value);
    formData.append('subtitle', subtitleInput?.value || '');
    formData.append('category', categorySelect.value);
    formData.append('author', authorInput.value);
    formData.append('tags', tagsInput?.value || '');
    formData.append('publishDate',
        document.getElementById('publishDate')?.value || '');
    formData.append('isPublic', isPublish ? 'true' : 'false');
    formData.append('isHot',
        document.getElementById('isHot').checked ? 'true' : 'false');
    formData.append('type', 'NEWS_ARTICLE'); // 뉴스 타입 설정

    // 에디터 내용 가져오기 (Base64 이미지 포함)
    const content = tinymce.get('content-editor').getContent();

    // 원본 콘텐츠를 그대로 서버에 전송 (서버에서 Base64 이미지 추출)
    formData.append('content', content);

    // 이미지 처리 진행 알림
    updateSaveProgressUI(30, '이미지 처리 중...');

    // API 요청 설정
    let apiUrl;
    let method;

    const newsId = document.getElementById('newsId')?.value || '';
    const mode = document.getElementById('mode')?.value || 'create';

    // mode와 newsId에 따라 API URL과 메서드 결정
    if (mode === 'create') {
      apiUrl = '/admin/original/news/upload';
      method = 'POST';

      // 생성 모드에서는 썸네일이 필수
      if (tempThumbnailFile) {
        updateSaveProgressUI(40, '썸네일 처리 중...');
        formData.append('thumbnailFile', tempThumbnailFile);
      } else if (thumbnailUrlInput.value) {
        formData.append('thumbnailUrl', thumbnailUrlInput.value);
      } else {
        // 생성 모드에서 썸네일이 없는 경우 경고 표시
        showAlert('썸네일 이미지는 필수입니다.', 'warning');
        updateSaveProgressUI(0, '저장 실패');
        isSubmitting = false;
        updateButtonStates(false);
        setTimeout(() => {
          saveProgressModal.hide();
        }, 500);
        return;
      }
    } else {
      // 수정 모드
      apiUrl = `/admin/original/news/update/${newsId}`;
      method = 'PUT';

      if (newsId) {
        formData.append('newsId', newsId);
      }

      // 수정 모드에서 썸네일 처리
      if (tempThumbnailFile) {
        // 새로 업로드한 썸네일이 있는 경우
        updateSaveProgressUI(40, '썸네일 처리 중...');
        formData.append('thumbnailFile', tempThumbnailFile);
      } else if (thumbnailUrlInput.value) {
        // 입력된 썸네일 URL이 있는 경우
        formData.append('thumbnailUrl', thumbnailUrlInput.value);
      } else {
        // 썸네일을 변경하지 않는 경우 - 이미 서버에 있는 기존 이미지를 그대로 사용
        const currentThumbnailUrl = thumbnailPreview?.getAttribute('src') || '';
        if (currentThumbnailUrl && !currentThumbnailUrl.startsWith('data:')) {
          // 기존 썸네일 URL이 있으면 추가 (data URL이 아닌 경우)
          formData.append('thumbnailUrl', currentThumbnailUrl);
        }
        // 수정 모드에서는 썸네일이 없어도 별도의 경고 없이 진행
      }
    }

    updateSaveProgressUI(50, '저장 중...');

    const response = await adminAuthFetch(apiUrl, {
      method: method,
      body: formData
    });

    if (!response.ok) {
      throw new Error(`서버 응답 오류: ${response.status}`);
    }

    const result = await response.json();

    if (result.status && result.status.code === 'OK_0000') {
      updateSaveProgressUI(100, '저장 완료!');
      showAlert(`뉴스가 성공적으로 ${isPublish ? '발행' : '저장'}되었습니다.`, 'success');

      // 페이지 이동 또는 새로고침
      setTimeout(() => {
        if (mode === 'create') {
          window.location.href = `/admin/original/news/update/${result.response.articleId}`;
        } else {
          window.location.reload();
        }
      }, 1000);
    } else {
      throw new Error(result.message || `저장 중 오류가 발생했습니다.`);
    }
  } catch (err) {
    console.error('저장 오류:', err);
    updateSaveProgressUI(0, '저장 실패');
    showAlert(`저장 중 오류가 발생했습니다: ${err.message}`, 'danger');
  } finally {
    setTimeout(() => {
      saveProgressModal.hide();
      updateButtonStates(false);
      isSubmitting = false;
    }, 500);
  }
}

  function updateButtonStates(isDisabled) {
    btnPublish.disabled = isDisabled;
    // btnPreview.disabled = isDisabled;
  }

  /**
   * 저장 진행 상태 UI 업데이트
   * @param {number} progress - 진행률 (0-100)
   * @param {string} message - 상태 메시지
   */
  function updateSaveProgressUI(progress, message) {
    // 프로그레스바 업데이트
    if (saveProgressBar) {
      saveProgressBar.style.width = `${progress}%`;
      saveProgressBar.setAttribute('aria-valuenow', progress);
    }

    // 상태 메시지 업데이트
    if (saveProgressStatus) {
      saveProgressStatus.textContent = message;
    }
  }

  /**
   * 페이지 이탈 시 처리
   * @param {BeforeUnloadEvent} e - beforeunload 이벤트 객체
   */
  function handleBeforeUnload(e) {
    if (isDirty) {
      // 표준 메시지 (브라우저마다 다를 수 있음)
      const message = '변경사항이 저장되지 않았습니다. 정말로 페이지를 떠나시겠습니까?';
      e.returnValue = message;
      return message;
    }
  }

  /**
   * 디바운스 함수 (연속 호출 방지)
   * @param {Function} func - 실행할 함수
   * @param {number} wait - 대기 시간 (밀리초)
   * @returns {Function} 디바운스된 함수
   */
  function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
      const later = () => {
        clearTimeout(timeout);
        func(...args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
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

    // Toast 알림 사용
    if (window.bootstrap && typeof bootstrap.Toast !== 'undefined') {
      // Toast 요소가 존재하는지 확인
      let toastContainer = document.getElementById('toast-container');

      // 없으면 생성
      if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toast-container';
        toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        document.body.appendChild(toastContainer);
      }

      // Toast 요소 생성
      const toastId = 'toast-' + Date.now();
      const toast = document.createElement('div');
      toast.id = toastId;
      toast.className = `toast align-items-center text-white bg-${type} border-0`;
      toast.setAttribute('role', 'alert');
      toast.setAttribute('aria-live', 'assertive');
      toast.setAttribute('aria-atomic', 'true');

      // Toast 내용
      toast.innerHTML = `
        <div class="d-flex">
          <div class="toast-body">
            ${message}
          </div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
      `;

      // 컨테이너에 추가
      toastContainer.appendChild(toast);

      // Toast 초기화 및 표시
      const bsToast = new bootstrap.Toast(toast, {
        autohide: true,
        delay: 3000
      });
      bsToast.show();

      // 닫힌 후 DOM에서 제거
      toast.addEventListener('hidden.bs.toast', function () {
        toast.remove();
      });

      return;
    }

    // 기본 alert 사용
    alert(message);
  }
});