// 수정된 profile-settings.js
import { showToast, showConfirm, showLoading, hideLoading } from './common.js';
import { authFetch } from './commonFetch.js';

// 페이지 상태 관리 (단순화)
let pageState = {
    photos: new Array(5).fill(null),      // 기존 이미지 URL들
    photoDetails: new Array(5).fill(null), // sortOrder 포함 상세 정보
    photosToDelete: [],                   // 삭제할 사진 sortOrder들
    newPhotos: [],                       // 새로 추가할 사진 파일들
    hasStartedPhotoUpload: false,        // 사진 업로드 시작 여부
    maxPhotos: 5,
    redirectFromVideoCall: false
};

/**
 * 서버 데이터 로드
 */
function loadServerData() {
    console.log('📊 서버 데이터 로드 시작');

    try {
        const serverData = window.serverData || {};
        const currentUser = serverData.currentUser || {};

        // 페이지 상태 설정
        pageState.redirectFromVideoCall = serverData.redirectFromVideoCall || false;

        // 생년월일 초기화
        if (currentUser.birthDate) {
            const birthDateField = document.getElementById('userBirthDate');
            if (birthDateField) {
                birthDateField.value = currentUser.birthDate;
            }
        }

        // 영상통화 리다이렉트 알림
        if (pageState.redirectFromVideoCall) {
            setTimeout(() => {
                showToast('영상통화를 위해 프로필 사진 5장이 필요합니다.', 'info', 5000);
            }, 1000);
        }

        console.log('📊 서버 데이터 로드 완료');

    } catch (error) {
        console.error('서버 데이터 로드 실패:', error);
        showToast('페이지 데이터 로드에 실패했습니다.', 'error');
    }
}

/**
 * 이벤트 리스너 바인딩
 */
function bindEvents() {
    console.log('🔗 이벤트 리스너 바인딩 시작');

    // 뒤로가기
    const backBtn = document.getElementById('backBtn');
    if (backBtn) {
        backBtn.addEventListener('click', (e) => {
            e.preventDefault();
            if (pageState.redirectFromVideoCall) {
                window.location.href = '/mobile/home';
            } else {
                history.back();
            }
        });
    }

    // 사진 업로드
    const photoUploadBtn = document.getElementById('photoUploadBtn');
    const photoInput = document.getElementById('photoInput');
    if (photoUploadBtn && photoInput) {
        photoUploadBtn.addEventListener('click', (e) => {
            e.preventDefault();
            photoInput.click();
        });
        photoInput.addEventListener('change', handlePhotoSelect);
    }

    // 저장 버튼
    const saveBtn = document.getElementById('saveBtn');
    if (saveBtn) {
        saveBtn.addEventListener('click', (e) => {
            e.preventDefault();
            saveProfile();
        });
    }

    // 회원탈퇴
    const deleteAccountBtn = document.getElementById('deleteAccountBtn');
    if (deleteAccountBtn) {
        deleteAccountBtn.addEventListener('click', (e) => {
            e.preventDefault();
            handleDeleteAccount();
        });
    }

    // 폼 필드 실시간 검증
    document.querySelectorAll('#profileForm input').forEach(input => {
        input.addEventListener('input', debounce(() => {
            validateFormField(input);
            updateSaveButtonState();
        }, 300));
    });

    console.log('✅ 이벤트 리스너 바인딩 완료');
}

/**
 * 프로필 저장
 */
async function saveProfile() {
    const saveBtn = document.getElementById('saveBtn');
    const validation = validateForm();

    if (!validation.isValid) {
        showToast(validation.message, 'warning');
        return;
    }

    try {
        saveBtn.disabled = true;
        saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 저장 중...';
        showLoading('프로필을 저장하고 있습니다...');

        const formData = new FormData();
        const name = document.getElementById('userName')?.value?.trim();
        const email = document.getElementById('userEmail')?.value?.trim();
        const phone = document.getElementById('userPhone')?.value?.trim();
        const birthDate = document.getElementById('userBirthDate')?.value?.trim();

        if (name) formData.append('name', name);
        if (email) formData.append('email', email);
        if (phone) formData.append('phoneNumber', phone);
        if (birthDate) formData.append('birthDate', birthDate);

        // 삭제할 이미지 추가
        pageState.photosToDelete.forEach(sortOrder => {
            formData.append('imagesToDelete', sortOrder);
        });

        // 새로운 이미지 추가
        pageState.newPhotos.forEach(file => {
            formData.append('images', file);
        });

        console.log('📸 전송 데이터:', {
            삭제할이미지: pageState.photosToDelete,
            새이미지개수: pageState.newPhotos.length,
            새이미지파일명: pageState.newPhotos.map(f => f.name)
        });

        const result = await authFetch('/api/profile/update', {
            method: 'PUT',
            body: formData
        });

        console.log('📸 서버 응답:', result);

        if (result.status?.code === 'OK_0000') {
            const responseData = result.response || {};

            showToast('프로필이 성공적으로 저장되었습니다.', 'success');

            // 🔧 핵심 수정: 상태 초기화를 응답 처리 후에 실행
            await handleSaveSuccess(responseData);

        } else {
            throw new Error(result.status?.message || '저장 실패');
        }

    } catch (error) {
        console.error('프로필 저장 실패:', error);
        showToast('프로필 저장에 실패했습니다.', 'error');
    } finally {
        hideLoading();
        saveBtn.disabled = false;
        updateSaveButtonState();
    }
}

/**
 * 저장 성공 후 처리
 */
async function handleSaveSuccess(responseData) {
    console.log('📸 저장 성공 처리 시작:', responseData);

    // 🔧 먼저 기존 경고 메시지 제거
    clearFaceDetectionWarning();

    // 상태 초기화
    pageState.photos = new Array(5).fill(null);
    pageState.photoDetails = new Array(5).fill(null);
    pageState.photosToDelete = [];
    pageState.newPhotos = [];

    // 폼 필드 업데이트
    updateFormFields(responseData);

    // 이미지 업데이트
    const images = responseData.imageUrls || [];
    const invalidImageNumbers = responseData.invalidImageNumbers || [];

    console.log('📸 이미지 업데이트:', {
        총이미지수: images.length,
        재인증필요: invalidImageNumbers
    });

    if (images && images.length > 0) {
        // 🔧 핵심 수정: 응답 데이터 구조에 맞게 업데이트
        updatePhotoSlotsFromResponse(images, invalidImageNumbers);
    } else {
        // 이미지가 없는 경우 모든 슬롯 초기화
        resetAllPhotoSlots();
    }

    // 재인증 필요한 이미지가 있으면 알림 표시
    if (invalidImageNumbers.length > 0) {
        setTimeout(() => {
            showFaceDetectionWarning(invalidImageNumbers);
        }, 1000);
    }

    console.log('📸 저장 성공 처리 완료');
}

/**
 * 서버 응답 데이터로부터 사진 슬롯 업데이트
 */
function updatePhotoSlotsFromResponse(images, invalidImageNumbers = []) {
    console.log('📸 서버 응답 데이터로 슬롯 업데이트:', {
        images: images.length,
        invalidImageNumbers
    });

    // 🔧 모든 슬롯 초기화 (경고 메시지도 제거됨)
    resetAllPhotoSlots();

    // 이미지 배치 (sortOrder 기준)
    images.forEach(imageDetail => {
        if (imageDetail && imageDetail.sortOrder && imageDetail.imageUrl) {
            const slotIndex = imageDetail.sortOrder - 1; // sortOrder는 1부터 시작
            if (slotIndex >= 0 && slotIndex < 5) {
                pageState.photos[slotIndex] = imageDetail.imageUrl;
                pageState.photoDetails[slotIndex] = imageDetail;

                updatePhotoSlot(slotIndex, imageDetail.imageUrl, false, imageDetail.sortOrder);

                // 얼굴 인식 실패한 이미지 표시
                if (invalidImageNumbers.includes(imageDetail.sortOrder)) {
                    markPhotoAsInvalid(slotIndex);
                }
            }
        }
    });

    // 5장 완료 상태 설정
    if (images.length === 5) {
        pageState.hasStartedPhotoUpload = true;
    }

    updatePhotoUploadStatus();
}

/**
 * 모든 사진 슬롯 리셋
 */
function resetAllPhotoSlots() {
    console.log('📸 모든 슬롯 리셋');

    // 🔧 경고 메시지도 함께 제거
    clearFaceDetectionWarning();

    for (let i = 0; i < 5; i++) {
        resetPhotoSlot(i);
    }
}

/**
 * 얼굴 인식 경고 메시지 제거
 */
function clearFaceDetectionWarning() {
    const existingWarning = document.getElementById('faceDetectionWarning');
    if (existingWarning) {
        existingWarning.remove();
        console.log('📸 얼굴 인식 경고 메시지 제거');
    }
}

/**
 * 얼굴 인식 경고 표시
 */
function showFaceDetectionWarning(invalidImageNumbers) {
    const photoSection = document.querySelector('.photo-section');
    if (!photoSection) return;

    // 🔧 기존 경고 확실히 제거
    clearFaceDetectionWarning();

    const warningDiv = document.createElement('div');
    warningDiv.id = 'faceDetectionWarning';
    warningDiv.className = 'alert alert-warning';
    warningDiv.innerHTML = `
        <i class="fas fa-exclamation-triangle"></i>
        <strong>재인증 필요:</strong> ${invalidImageNumbers.join(', ')}번째 사진에서 얼굴이 인식되지 않았습니다. 
        해당 사진을 다시 업로드해주세요.
    `;

    const photoGrid = document.getElementById('photoGrid');
    if (photoGrid) {
        photoSection.insertBefore(warningDiv, photoGrid);
    }

    console.log('📸 얼굴 인식 경고 표시:', invalidImageNumbers);
}

/**
 * 얼굴 인식 실패 사진 표시
 */
function markPhotoAsInvalid(slotIndex) {
    const slot = document.querySelector(`.photo-slot[data-index="${slotIndex}"]`);
    if (slot) {
        slot.classList.add('face-detection-failed');

        // 기존 info에 경고 아이콘 추가
        const photoInfo = slot.querySelector('.photo-info');
        if (photoInfo) {
            photoInfo.innerHTML += ' <i class="fas fa-exclamation-triangle" style="color: #dc3545;" title="얼굴 인식 실패"></i>';
        }
    }
}

/**
 * 기존 사진 로드 (페이지 로드 시)
 */
function loadExistingPhotos() {
    console.log('📸 기존 사진 로드 시작');

    const serverData = window.serverData || {};
    const profileData = serverData.profileData || {};
    const profileImages = profileData.imageUrls || [];

    console.log('📸 서버 데이터 확인:', {
        serverData: !!serverData,
        profileData: !!profileData,
        profileImages: profileImages.length
    });

    // 상태 초기화
    pageState.photos = new Array(5).fill(null);
    pageState.photoDetails = new Array(5).fill(null);

    if (profileImages && profileImages.length > 0) {
        // 재인증 필요한 이미지 찾기
        const invalidImageNumbers = profileImages
            .filter(img => img.aiProcessed === false)
            .map(img => img.sortOrder);

        console.log('📸 재인증 필요한 이미지:', invalidImageNumbers);

        updatePhotoSlotsFromResponse(profileImages, invalidImageNumbers);

        // 5장이고 재인증 필요한 이미지가 있으면 경고 표시
        if (profileImages.length === 5 && invalidImageNumbers.length > 0) {
            setTimeout(() => {
                showFaceDetectionWarning(invalidImageNumbers);
            }, 1000);
        }

        if (profileImages.length === 5) {
            pageState.hasStartedPhotoUpload = true;
        }
    }

    updatePhotoUploadStatus();
    console.log('📸 기존 사진 로드 완료');
}

/**
 * 사진 선택 처리
 */
function handlePhotoSelect(e) {
    const files = Array.from(e.target.files);
    console.log('📸 파일 선택:', files.length + '개');

    if (files.length === 0) return;

    // 현재 상태 확인
    const currentValid = getTotalValidPhotoCount();
    console.log('📸 현재 유효한 사진:', currentValid);

    // 사진 업로드 시작 표시
    if (!pageState.hasStartedPhotoUpload && currentValid === 0) {
        pageState.hasStartedPhotoUpload = true;
        showPhotoUploadCommitment();
    }

    // 남은 슬롯 계산
    const remainingSlots = 5 - currentValid;
    if (remainingSlots <= 0) {
        showToast('최대 5장까지만 업로드할 수 있습니다.', 'warning');
        return;
    }

    // 파일 검증 및 추가
    const validFiles = files.slice(0, remainingSlots);
    console.log(`📸 처리할 파일: ${validFiles.length}개`);

    addNewPhotos(validFiles);

    // 파일 인풋 리셋
    e.target.value = '';
}

/**
 * 새 사진 추가
 */
function addNewPhotos(files) {
    console.log('📸 새 사진 추가 시작:', files.length);

    let addedCount = 0;

    files.forEach((file, fileIndex) => {
        if (validateImageFile(file)) {
            const emptySlotIndex = findNextEmptySlot();

            if (emptySlotIndex !== -1) {
                // 즉시 슬롯 예약
                const slot = document.querySelector(`.photo-slot[data-index="${emptySlotIndex}"]`);
                if (slot) {
                    slot.classList.add('filled', 'uploading');
                    slot.innerHTML = `
                        <div class="upload-placeholder">
                            <i class="fas fa-spinner fa-spin"></i>
                            <span>업로드 중...</span>
                        </div>
                    `;
                }

                // 파일 추가
                pageState.newPhotos.push(file);

                // 미리보기 표시
                showImagePreview(emptySlotIndex, file);

                addedCount++;
            }
        }
    });

    console.log(`📸 총 ${addedCount}개 파일 추가 완료`);
    updatePhotoUploadStatus();
}

/**
 * 빈 슬롯 찾기
 */
function findNextEmptySlot() {
    for (let i = 0; i < 5; i++) {
        const slot = document.querySelector(`.photo-slot[data-index="${i}"]`);
        if (slot && !slot.classList.contains('filled') && !slot.classList.contains('uploading')) {
            return i;
        }
    }
    return -1;
}

/**
 * 이미지 미리보기 표시
 */
function showImagePreview(slotIndex, file) {
    const reader = new FileReader();

    reader.onload = (e) => {
        const slot = document.querySelector(`.photo-slot[data-index="${slotIndex}"]`);
        if (slot) {
            slot.classList.remove('uploading');
            slot.classList.add('filled', 'new-photo');

            const displaySortOrder = slotIndex + 1;
            slot.innerHTML = `
                <img src="${e.target.result}" alt="프로필 사진 ${displaySortOrder}">
                <div class="photo-info">
                    <span class="photo-order">${displaySortOrder}</span>
                </div>
                <button class="photo-remove" onclick="removePhoto(${slotIndex}, ${displaySortOrder})" aria-label="사진 삭제">
                    <i class="fas fa-times"></i>
                </button>
            `;
            slot.setAttribute('data-sort-order', displaySortOrder);
        }
    };

    reader.onerror = () => {
        console.error('미리보기 생성 실패');
        resetPhotoSlot(slotIndex);
        pageState.newPhotos.pop();
        showToast(`이미지 미리보기 생성 실패: ${file.name}`, 'error');
        updatePhotoUploadStatus();
    };

    reader.readAsDataURL(file);
}

/**
 * 사진 슬롯 업데이트
 */
function updatePhotoSlot(index, imageUrl, isNewPhoto = false, sortOrder = null) {
    const slot = document.querySelector(`.photo-slot[data-index="${index}"]`);
    if (!slot) return;

    slot.classList.add('filled');
    if (isNewPhoto) {
        slot.classList.add('new-photo');
    }

    const displaySortOrder = sortOrder || (index + 1);
    slot.innerHTML = `
        <img src="${imageUrl}" alt="프로필 사진 ${displaySortOrder}">
        <div class="photo-info">
            <span class="photo-order">${displaySortOrder}</span>
        </div>
        <button class="photo-remove" onclick="removePhoto(${index}, ${displaySortOrder})" aria-label="사진 삭제">
            <i class="fas fa-times"></i>
        </button>
    `;
    slot.setAttribute('data-sort-order', displaySortOrder);
}

/**
 * 사진 제거
 */
async function removePhoto(index, sortOrder = null) {
    const slot = document.querySelector(`.photo-slot[data-index="${index}"]`);
    const isNewPhoto = slot?.classList.contains('new-photo');

    // sortOrder 결정
    const actualSortOrder = sortOrder ||
                           slot?.getAttribute('data-sort-order') ||
                           (index + 1);

    console.log('📸 사진 제거:', { index, actualSortOrder, isNewPhoto });

    if (isNewPhoto) {
        // 새 사진 제거
        const newPhotoIndex = findNewPhotoIndex(index);
        if (newPhotoIndex !== -1) {
            pageState.newPhotos.splice(newPhotoIndex, 1);
        }
    } else if (pageState.photos[index]) {
        // 기존 사진 제거
        const sortOrderNum = parseInt(actualSortOrder);
        if (!pageState.photosToDelete.includes(sortOrderNum)) {
            pageState.photosToDelete.push(sortOrderNum);
        }
        pageState.photos[index] = null;
        pageState.photoDetails[index] = null;
    }

    // 슬롯 리셋
    resetPhotoSlot(index);
    updatePhotoUploadStatus();
    showToast(`사진 ${actualSortOrder}번이 삭제되었습니다.`, 'success');
}

/**
 * 새 사진 배열에서 해당 슬롯의 파일 인덱스 찾기
 */
function findNewPhotoIndex(slotIndex) {
    let newPhotoCount = 0;
    for (let i = 0; i < slotIndex; i++) {
        const slot = document.querySelector(`.photo-slot[data-index="${i}"]`);
        if (slot && slot.classList.contains('new-photo')) {
            newPhotoCount++;
        }
    }

    const currentSlot = document.querySelector(`.photo-slot[data-index="${slotIndex}"]`);
    if (currentSlot && currentSlot.classList.contains('new-photo')) {
        return newPhotoCount;
    }
    return -1;
}

/**
 * 현재 유효한 사진 개수 계산
 */
function getTotalValidPhotoCount() {
    // 기존 사진 중 삭제 예정이 아닌 것들
    const validExistingPhotos = pageState.photos.filter((photo, index) => {
        if (!photo) return false;
        const sortOrder = pageState.photoDetails[index]?.sortOrder || (index + 1);
        return !pageState.photosToDelete.includes(sortOrder);
    }).length;

    // 새 사진 개수
    const newPhotoSlots = document.querySelectorAll('.photo-slot.new-photo, .photo-slot.uploading').length;

    return validExistingPhotos + newPhotoSlots;
}

/**
 * 폼 유효성 검사
 */
function validateForm() {
    const name = document.getElementById('userName')?.value?.trim();
    const email = document.getElementById('userEmail')?.value?.trim();
    const phone = document.getElementById('userPhone')?.value?.trim();
    const birthDate = document.getElementById('userBirthDate')?.value?.trim();

    if (!name) {
        return { isValid: false, message: '이름을 입력해주세요.' };
    }

    if (email && !isValidEmail(email)) {
        return { isValid: false, message: '올바른 이메일 주소를 입력해주세요.' };
    }

    if (phone && !isValidPhoneNumber(phone)) {
        return { isValid: false, message: '올바른 휴대폰 번호를 입력해주세요.' };
    }

    if (birthDate && !isValidBirthDate(birthDate)) {
        return { isValid: false, message: '올바른 생년월일을 입력해주세요.' };
    }

    // 사진 검증
    const totalPhotoCount = getTotalValidPhotoCount();
    if (pageState.hasStartedPhotoUpload && totalPhotoCount < 5) {
        const remaining = 5 - totalPhotoCount;
        return {
            isValid: false,
            message: `프로필 사진 ${remaining}장을 더 업로드해주세요. (현재 ${totalPhotoCount}/5)`
        };
    }

    return { isValid: true };
}

/**
 * 저장 버튼 상태 업데이트
 */
function updateSaveButtonState() {
    const saveBtn = document.getElementById('saveBtn');
    if (!saveBtn) return;

    const validation = validateForm();
    const totalPhotoCount = getTotalValidPhotoCount();

    if (!validation.isValid) {
        saveBtn.classList.add('btn-warning');
        saveBtn.classList.remove('btn-primary');
        saveBtn.innerHTML = `<i class="fas fa-exclamation-triangle"></i> 프로필 저장 (${totalPhotoCount}/5)`;
    } else {
        saveBtn.classList.remove('btn-warning');
        saveBtn.classList.add('btn-primary');
        saveBtn.innerHTML = '<i class="fas fa-check"></i> 프로필 저장 (완료)';
    }
}

/**
 * 사진 업로드 상태 업데이트
 */
function updatePhotoUploadStatus() {
    const totalCount = getTotalValidPhotoCount();
    const filledSlots = document.querySelectorAll('.photo-slot.filled').length;
    const uploadingSlots = document.querySelectorAll('.photo-slot.uploading').length;

    let statusElement = document.getElementById('photoUploadStatus');

    if (!statusElement) {
        const photoSection = document.querySelector('.photo-section');
        if (photoSection) {
            statusElement = document.createElement('div');
            statusElement.id = 'photoUploadStatus';
            statusElement.className = 'photo-upload-status';
            const photoGrid = document.getElementById('photoGrid');
            if (photoGrid) {
                photoSection.insertBefore(statusElement, photoGrid);
            }
        }
    }

    if (!statusElement) return;

    if (pageState.hasStartedPhotoUpload || totalCount > 0) {
        const remaining = 5 - totalCount;

        if (totalCount === 5 && uploadingSlots === 0) {
            statusElement.innerHTML = `
                <div class="status-complete">
                    <i class="fas fa-check-circle"></i>
                    <span>프로필 사진 업로드 완료! (${totalCount}/5)</span>
                </div>
            `;
            statusElement.className = 'photo-upload-status complete';
        } else if (uploadingSlots > 0) {
            statusElement.innerHTML = `
                <div class="status-progress">
                    <i class="fas fa-spinner fa-spin"></i>
                    <span>사진 처리 중... (${totalCount}/5)</span>
                </div>
            `;
            statusElement.className = 'photo-upload-status progress';
        } else {
            statusElement.innerHTML = `
                <div class="status-progress">
                    <i class="fas fa-clock"></i>
                    <span>프로필 사진 업로드 진행중 (${totalCount}/5) - ${remaining}장 더 필요</span>
                </div>
            `;
            statusElement.className = 'photo-upload-status progress';
        }
        statusElement.style.display = 'block';
    } else {
        statusElement.style.display = 'none';
    }

    updateSaveButtonState();
}

/**
 * 회원탈퇴 처리
 */
async function handleDeleteAccount() {
    const confirmed = await showConfirm(
        '회원탈퇴',
        '정말 회원탈퇴 하시겠습니까?\n\n탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.',
        '탈퇴하기',
        '취소'
    );

    if (!confirmed) return;

    try {
        showLoading('회원탈퇴 처리 중...');

        const result = await authFetch('/api/profile/delete-account', {
            method: 'DELETE'
        });

        if (result.status?.code === 'OK_0000') {
            showToast('회원탈퇴가 완료되었습니다.', 'success');
            setTimeout(() => {
                window.location.href = '/mobile/login?reason=account_deleted';
            }, 2000);
        } else {
            throw new Error(result.status?.message || '회원탈퇴 실패');
        }

    } catch (error) {
        console.error('회원탈퇴 실패:', error);
        showToast('회원탈퇴 처리 중 오류가 발생했습니다.', 'error');
    } finally {
        hideLoading();
    }
}

// === 헬퍼 함수들 ===

function showPhotoUploadCommitment() {
    const alert = document.getElementById('photoAlert');
    if (alert) {
        alert.style.display = 'block';
        alert.innerHTML = `
            <i class="fas fa-info-circle"></i>
            <strong>중요:</strong> 프로필 사진 업로드를 시작하셨습니다. 
            반드시 5장을 모두 완성해야 저장할 수 있습니다.
        `;
    }
}

function resetPhotoSlot(index) {
    const slot = document.querySelector(`.photo-slot[data-index="${index}"]`);
    if (!slot) return;

    // 🔧 얼굴 인식 실패 클래스도 제거
    slot.classList.remove('filled', 'new-photo', 'uploading', 'face-detection-failed');
    slot.removeAttribute('data-sort-order');
    slot.innerHTML = `
        <i class="fas fa-camera"></i>
        <span>${getSlotLabel(index)}</span>
    `;
}

function getSlotLabel(index) {
    const labels = ['첫 번째<br>사진', '두 번째<br>사진', '세 번째<br>사진', '네 번째<br>사진', '다섯 번째<br>사진'];
    return labels[index];
}

function validateImageFile(file) {
    if (!file.type.startsWith('image/')) {
        showToast(`이미지 파일만 업로드 가능합니다: ${file.name}`, 'warning');
        return false;
    }

    const maxSize = 5 * 1024 * 1024; // 5MB
    if (file.size > maxSize) {
        showToast(`파일 크기는 5MB 이하여야 합니다: ${file.name}`, 'warning');
        return false;
    }

    return true;
}

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function isValidPhoneNumber(phone) {
    const cleanPhone = phone.replace(/[-\s]/g, '');
    return /^010[0-9]{8}$/.test(cleanPhone);
}

function isValidBirthDate(birthDate) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(birthDate)) return false;
    const date = new Date(birthDate);
    const today = new Date();
    const minDate = new Date(today.getFullYear() - 120, 0, 1);
    return date >= minDate && date <= today;
}

function validateFormField(field) {
    if (!field) return true;

    const value = field.value?.trim() || '';

    field.classList.remove('is-invalid');
    const errorElement = field.parentElement.querySelector('.invalid-feedback');
    if (errorElement) {
        errorElement.style.display = 'none';
    }

    if (field.hasAttribute('required') && !value) {
        showFieldError(field, '이 필드는 필수입니다.');
        return false;
    }

    if (field.type === 'email' && value && !isValidEmail(value)) {
        showFieldError(field, '올바른 이메일 주소를 입력하세요.');
        return false;
    }

    if (field.type === 'tel' && value && !isValidPhoneNumber(value)) {
        showFieldError(field, '올바른 휴대폰 번호를 입력하세요.');
        return false;
    }

    if (field.type === 'date' && value && !isValidBirthDate(value)) {
        showFieldError(field, '올바른 생년월일을 입력하세요.');
        return false;
    }

    return true;
}

function showFieldError(field, message) {
    field.classList.add('is-invalid');

    let errorElement = field.parentElement.querySelector('.invalid-feedback');
    if (!errorElement) {
        errorElement = document.createElement('div');
        errorElement.className = 'invalid-feedback';
        field.parentElement.appendChild(errorElement);
    }

    errorElement.textContent = message;
    errorElement.style.display = 'block';
}

function updateFormFields(responseData) {
    if (responseData.name) {
        const nameField = document.getElementById('userName');
        if (nameField) nameField.value = responseData.name;
    }
    if (responseData.email) {
        const emailField = document.getElementById('userEmail');
        if (emailField) emailField.value = responseData.email;
    }
    if (responseData.phoneNumber) {
        const phoneField = document.getElementById('userPhone');
        if (phoneField) phoneField.value = responseData.phoneNumber;
    }
    if (responseData.birthDate) {
        const birthDateField = document.getElementById('userBirthDate');
        if (birthDateField) birthDateField.value = responseData.birthDate;
    }
}

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

// 전역 함수로 노출
window.removePhoto = removePhoto;

// 페이지 초기화
document.addEventListener('DOMContentLoaded', function() {
    try {
        loadServerData();
        bindEvents();
        loadExistingPhotos();

        // 실시간 업데이트
        setInterval(updateSaveButtonState, 1000);

        console.log('✅ 프로필 설정 페이지 초기화 완료');
    } catch (error) {
        console.error('프로필 설정 페이지 초기화 실패:', error);
        showToast('페이지 초기화에 실패했습니다.', 'error');
    }
});

console.log('📱 수정된 프로필 설정 스크립트 로드 완료');