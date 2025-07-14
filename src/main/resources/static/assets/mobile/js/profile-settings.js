// 개선된 profile-settings.js
import { showToast, showConfirm, showLoading, hideLoading } from './common.js';
import { authFetch } from './commonFetch.js';

// 페이지 상태 관리 (단순화)
let pageState = {
    photos: new Array(5).fill(null),  // 5개 슬롯을 null로 초기화
    photosToDelete: [],               // 삭제할 사진 순서들
    newPhotos: [],                   // 새로 추가할 사진 파일들
    hasStartedPhotoUpload: false,    // 사진 업로드 시작 여부
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

        // 삭제할 이미지
        pageState.photosToDelete.forEach(sortOrder => {
            formData.append('imagesToDelete', sortOrder);
        });

        // 새 이미지
        pageState.newPhotos.forEach(file => {
            formData.append('images', file);
        });

        const result = await authFetch('/api/profile/update', {
            method: 'PUT',
            body: formData
        });

        if (result.status?.code === 'OK_0000') {
            showToast('프로필이 성공적으로 저장되었습니다.', 'success');

            const responseData = result.response || {};
            const imageUrls = responseData.imageUrls || [];

            // 상태 초기화
            pageState.photos = new Array(5).fill(null);
            pageState.photosToDelete = [];
            pageState.newPhotos = [];

            // 이미지 업데이트
            updateAllPhotoSlots(imageUrls);

            // 폼 필드 업데이트
            updateFormFields(responseData);

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
 * 기존 사진 로드 (단순화)
 */
function loadExistingPhotos() {
    const existingPhotos = window.serverData?.currentUser?.profileImages || [];

    // 상태 초기화
    pageState.photos = new Array(5).fill(null);

    if (existingPhotos.length > 0) {
        // 기존 사진 설정
        existingPhotos.forEach((photoUrl, index) => {
            if (index < 5) {
                pageState.photos[index] = photoUrl;
                updatePhotoSlot(index, photoUrl, false);
            }
        });

        // 5장이면 업로드 시작된 것으로 간주
        if (existingPhotos.length === 5) {
            pageState.hasStartedPhotoUpload = true;
        }

        console.log('📸 기존 사진 로드 완료:', existingPhotos.length + '장');
    }

    updatePhotoUploadStatus();
}

/**
 * 사진 선택 처리 (단순화)
 */
function handlePhotoSelect(e) {
    const files = Array.from(e.target.files);
    console.log('📸 파일 선택:', files.length + '개');

    if (files.length === 0) return;

    // 현재 상태 로그
    console.log('📸 현재 상태:', {
        기존사진: pageState.photos.filter(p => p).length,
        새사진: pageState.newPhotos.length,
        삭제예정: pageState.photosToDelete.length
    });

    // 사진 업로드 시작 표시
    if (!pageState.hasStartedPhotoUpload && getTotalValidPhotoCount() === 0) {
        pageState.hasStartedPhotoUpload = true;
        showPhotoUploadCommitment();
    }

    // 남은 슬롯 계산
    const remainingSlots = 5 - getTotalValidPhotoCount();
    if (remainingSlots <= 0) {
        showToast('최대 5장까지만 업로드할 수 있습니다.', 'warning');
        return;
    }

    // 파일 검증 및 추가
    const validFiles = files.slice(0, remainingSlots);
    addNewPhotos(validFiles);

    // 파일 인풋 리셋
    e.target.value = '';
}

/**
 * 새 사진 추가 (단순화)
 */
function addNewPhotos(files) {
    files.forEach(file => {
        if (validateImageFile(file)) {
            const emptySlotIndex = findNextEmptySlot();

            if (emptySlotIndex !== -1) {
                // 임시 저장
                pageState.newPhotos.push(file);

                // 미리보기 표시
                showImagePreview(emptySlotIndex, file);

                console.log(`📸 새 사진 추가 - 슬롯: ${emptySlotIndex}, 파일: ${file.name}`);
            }
        }
    });

    updatePhotoUploadStatus();
}

/**
 * 빈 슬롯 찾기 (단순화)
 */
function findNextEmptySlot() {
    for (let i = 0; i < 5; i++) {
        const slot = document.querySelector(`.photo-slot[data-index="${i}"]`);

        // 슬롯이 비어있으면 (filled 클래스가 없으면)
        if (slot && !slot.classList.contains('filled')) {
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
        updatePhotoSlot(slotIndex, e.target.result, true);
        console.log(`📸 미리보기 표시 완료 - 슬롯: ${slotIndex}`);
    };
    reader.onerror = (e) => {
        console.error('📸 미리보기 생성 실패:', e);
        showToast('이미지 미리보기 생성에 실패했습니다.', 'error');
    };
    reader.readAsDataURL(file);
}

/**
 * 사진 슬롯 업데이트
 */
function updatePhotoSlot(index, imageUrl, isNewPhoto = false) {
    const slot = document.querySelector(`.photo-slot[data-index="${index}"]`);
    if (!slot) return;

    slot.classList.add('filled');
    if (isNewPhoto) {
        slot.classList.add('new-photo');
    }

    slot.innerHTML = `
        <img src="${imageUrl}" alt="프로필 사진 ${index + 1}">
        <button class="photo-remove" onclick="removePhoto(${index})" aria-label="사진 삭제">
            <i class="fas fa-times"></i>
        </button>
    `;
}

/**
 * 사진 제거 (단순화)
 */
async function removePhoto(index) {
    const slot = document.querySelector(`.photo-slot[data-index="${index}"]`);
    const isNewPhoto = slot?.classList.contains('new-photo');
    const sortOrder = index + 1;

    if (isNewPhoto) {
        // 새 사진 제거 - 마지막 추가된 것부터 제거
        pageState.newPhotos.pop();
    } else if (pageState.photos[index]) {
        // 기존 사진 삭제 예정으로 표시
        if (!pageState.photosToDelete.includes(sortOrder)) {
            pageState.photosToDelete.push(sortOrder);
        }
    }

    // 슬롯 리셋
    resetPhotoSlot(index);
    updatePhotoUploadStatus();
    showToast('사진이 삭제되었습니다.', 'success');
}

/**
 * 모든 사진 슬롯 업데이트
 */
function updateAllPhotoSlots(imageUrls) {
    // 모든 슬롯 초기화
    for (let i = 0; i < 5; i++) {
        resetPhotoSlot(i);
    }

    // 새 이미지로 업데이트
    imageUrls.forEach((url, index) => {
        if (index < 5) {
            pageState.photos[index] = url;
            updatePhotoSlot(index, url, false);
        }
    });

    updatePhotoUploadStatus();
}

/**
 * 현재 유효한 사진 개수 계산
 */
function getTotalValidPhotoCount() {
    const originalCount = pageState.photos.filter((photo, index) =>
        photo && !pageState.photosToDelete.includes(index + 1)
    ).length;

    return originalCount + pageState.newPhotos.length;
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
    let statusElement = document.getElementById('photoUploadStatus');

    if (!statusElement) {
        const photoSection = document.querySelector('.photo-section');
        statusElement = document.createElement('div');
        statusElement.id = 'photoUploadStatus';
        statusElement.className = 'photo-upload-status';
        photoSection.insertBefore(statusElement, document.getElementById('photoGrid'));
    }

    if (pageState.hasStartedPhotoUpload || totalCount > 0) {
        const remaining = 5 - totalCount;

        if (totalCount === 5) {
            statusElement.innerHTML = `
                <div class="status-complete">
                    <i class="fas fa-check-circle"></i>
                    <span>프로필 사진 업로드 완료! (${totalCount}/5)</span>
                </div>
            `;
            statusElement.className = 'photo-upload-status complete';
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

    slot.classList.remove('filled', 'new-photo');
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
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

function isValidPhoneNumber(phone) {
    const phoneRegex = /^010[0-9]{8}$/;
    const cleanPhone = phone.replace(/[-\s]/g, '');
    return phoneRegex.test(cleanPhone);
}

function isValidBirthDate(birthDate) {
    const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
    if (!dateRegex.test(birthDate)) return false;

    const date = new Date(birthDate);
    const today = new Date();
    const minDate = new Date(today.getFullYear() - 120, 0, 1);

    return date >= minDate && date <= today;
}

function validateFormField(field) {
    if (!field) return true;

    const value = field.value?.trim() || '';

    // 기존 에러 제거
    field.classList.remove('is-invalid');
    const errorElement = field.parentElement.querySelector('.invalid-feedback');
    if (errorElement) {
        errorElement.style.display = 'none';
    }

    // 검증 로직
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
        document.getElementById('userName').value = responseData.name;
    }
    if (responseData.email) {
        document.getElementById('userEmail').value = responseData.email;
    }
    if (responseData.phoneNumber) {
        document.getElementById('userPhone').value = responseData.phoneNumber;
    }
    if (responseData.birthDate) {
        document.getElementById('userBirthDate').value = responseData.birthDate;
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

console.log('📱 개선된 프로필 설정 스크립트 로드 완료');