// 통합 API용 profile-settings.js (수정된 버전)
import { showToast, showConfirm, showLoading, hideLoading } from './common.js';
import { authFetch } from './commonFetch.js';

// 페이지 상태 관리
let pageState = {
    photos: [],                    // 현재 사진들 (최대 5장)
    photosToDelete: [],           // 삭제할 사진 인덱스들
    newPhotos: [],               // 새로 추가할 사진 파일들
    originalPhotoCount: 0,         // 페이지 로드 시 원본 사진 개수
    hasStartedPhotoUpload: false,  // 사진 업로드를 시작했는지 여부
    isUploading: false,            // 업로드 진행 중
    originalFormData: {},          // 변경사항 감지용
    maxPhotos: 5,                 // 최대 사진 개수
    redirectFromVideoCall: false   // 영상통화에서 리다이렉트 여부
};

/**
 * 서버 데이터 로드
 */
function loadServerData() {
    console.log('📊 서버 데이터 로드 시작');

    try {
        // window.serverData에서 현재 사용자 정보 로드
        const serverData = window.serverData || {};
        const currentUser = serverData.currentUser || {};

        // 페이지 상태에 반영
        pageState.redirectFromVideoCall = serverData.redirectFromVideoCall || false;
        pageState.maxPhotos = serverData.maxProfileImages || 5;

        // 프로필 통계 정보 로드
        const profileStats = serverData.profileStats || {};
        pageState.originalPhotoCount = profileStats.uploadedCount || 0;

        console.log('📊 서버 데이터 로드 완료:', {
            사용자명: currentUser.name,
            이미지개수: profileStats.uploadedCount,
            영상통화리다이렉트: pageState.redirectFromVideoCall,
            생년월일: currentUser.birthDate
        });

        // 영상통화에서 리다이렉트된 경우 알림 표시
        if (pageState.redirectFromVideoCall) {
            setTimeout(() => {
                showToast('영상통화를 위해 프로필 사진 5장이 필요합니다.', 'info', 5000);
            }, 1000);
        }

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

    try {
        // 뒤로가기 버튼
        const backBtn = document.getElementById('backBtn');
        if (backBtn) {
            backBtn.addEventListener('click', (e) => {
                e.preventDefault();
                console.log('🔙 뒤로가기 버튼 클릭');

                if (pageState.redirectFromVideoCall) {
                    window.location.href = '/mobile/home';
                } else {
                    history.back();
                }
            });
        }

        // 사진 업로드 버튼
        const photoUploadBtn = document.getElementById('photoUploadBtn');
        const photoInput = document.getElementById('photoInput');

        if (photoUploadBtn && photoInput) {
            photoUploadBtn.addEventListener('click', (e) => {
                e.preventDefault();
                console.log('📸 사진 업로드 버튼 클릭');
                photoInput.click();
            });

            photoInput.addEventListener('change', handlePhotoSelect);
        }

        // 저장 버튼
        const saveBtn = document.getElementById('saveBtn');
        if (saveBtn) {
            saveBtn.addEventListener('click', (e) => {
                e.preventDefault();
                console.log('💾 프로필 저장 버튼 클릭');
                saveProfile();
            });
        }

        // 회원탈퇴 버튼
        const deleteAccountBtn = document.getElementById('deleteAccountBtn');
        if (deleteAccountBtn) {
            deleteAccountBtn.addEventListener('click', (e) => {
                e.preventDefault();
                console.log('🗑️ 회원탈퇴 버튼 클릭');
                handleDeleteAccount();
            });
        }

        // 폼 필드 실시간 검증
        const formInputs = document.querySelectorAll('#profileForm input');
        formInputs.forEach(input => {
            input.addEventListener('input', debounce(() => {
                validateFormField(input);
                updateSaveButtonState();
            }, 300));

            input.addEventListener('blur', () => {
                validateFormField(input);
            });
        });

        console.log('✅ 이벤트 리스너 바인딩 완료');

    } catch (error) {
        console.error('이벤트 바인딩 실패:', error);
        showToast('페이지 기능 초기화에 실패했습니다.', 'error');
    }
}

/**
 * 폼 유효성 검사
 */
function validateForm() {
    const name = document.getElementById('userName')?.value?.trim();
    const email = document.getElementById('userEmail')?.value?.trim();
    const phone = document.getElementById('userPhone')?.value?.trim();
    const birthDate = document.getElementById('userBirthDate')?.value?.trim();

    // 이름 필수 검사
    if (!name) {
        return { isValid: false, message: '이름을 입력해주세요.' };
    }

    // 이메일 형식 검사
    if (email && !isValidEmail(email)) {
        return { isValid: false, message: '올바른 이메일 주소를 입력해주세요.' };
    }

    // 휴대폰 번호 형식 검사
    if (phone && !isValidPhoneNumber(phone)) {
        return { isValid: false, message: '올바른 휴대폰 번호를 입력해주세요.' };
    }

    // 생년월일 형식 검사
    if (birthDate && !isValidBirthDate(birthDate)) {
        return { isValid: false, message: '올바른 생년월일을 입력해주세요.' };
    }

    // 사진 업로드 검증
    const totalPhotoCount = getTotalValidPhotoCount();

    if (pageState.hasStartedPhotoUpload || totalPhotoCount > 0) {
        if (totalPhotoCount < pageState.maxPhotos) {
            const remaining = pageState.maxPhotos - totalPhotoCount;
            return {
                isValid: false,
                message: `프로필 사진이 완성되지 않았습니다. ${remaining}장 더 업로드해주세요. (현재 ${totalPhotoCount}/5)`
            };
        }
    }

    return { isValid: true };
}

/**
 * 생년월일 유효성 검사
 */
function isValidBirthDate(birthDate) {
    if (!birthDate) return true; // 선택 사항

    const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
    if (!dateRegex.test(birthDate)) {
        return false;
    }

    const date = new Date(birthDate);
    const today = new Date();
    const minDate = new Date(today.getFullYear() - 120, 0, 1); // 120년 전
    const maxDate = new Date(); // 오늘

    return date >= minDate && date <= maxDate;
}

/**
 * 프로필 저장 (통합 API 호출)
 */
async function saveProfile() {
    const saveBtn = document.getElementById('saveBtn');

    // 필수 필드 검증
    const validation = validateForm();
    if (!validation.isValid) {
        const totalPhotoCount = getTotalValidPhotoCount();
        if (pageState.hasStartedPhotoUpload || totalPhotoCount > 0) {
            const remaining = pageState.maxPhotos - totalPhotoCount;
            showToast(`사진 ${remaining}장을 더 업로드한 후 저장해주세요.`, 'warning', 4000);

            const photoSection = document.querySelector('.photo-section');
            if (photoSection) {
                photoSection.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        } else {
            showToast(validation.message, 'warning');
        }
        return;
    }

    try {
        // 로딩 시작
        saveBtn.disabled = true;
        saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 저장 중...';
        showLoading('프로필을 저장하고 있습니다...');

        // FormData 생성
        const formData = new FormData();

        // 기본 정보 추가
        const name = document.getElementById('userName')?.value?.trim();
        const email = document.getElementById('userEmail')?.value?.trim();
        const phone = document.getElementById('userPhone')?.value?.trim();
        const birthDate = document.getElementById('userBirthDate')?.value?.trim();

        // 알림 설정
        const pushNotification = document.getElementById('pushNotification')?.checked;
        const memorialNotification = document.getElementById('memorialNotification')?.checked;
        const paymentNotification = document.getElementById('paymentNotification')?.checked;
        const familyNotification = document.getElementById('familyNotification')?.checked;
        const marketingAgreed = document.getElementById('marketingAgreed')?.checked;

        // 필수 필드
        if (name) formData.append('name', name);

        // 선택 필드
        if (email) formData.append('email', email);
        if (phone) formData.append('phoneNumber', phone);
        if (birthDate) formData.append('birthDate', birthDate);

        // 알림 설정 추가
        if (pushNotification !== undefined) formData.append('pushNotification', pushNotification);
        if (memorialNotification !== undefined) formData.append('memorialNotification', memorialNotification);
        if (paymentNotification !== undefined) formData.append('paymentNotification', paymentNotification);
        if (familyNotification !== undefined) formData.append('familyNotification', familyNotification);
        if (marketingAgreed !== undefined) formData.append('marketingAgreed', marketingAgreed);

        // 이미지 관련 데이터 추가
        if (pageState.photosToDelete.length > 0) {
            pageState.photosToDelete.forEach(sortOrder => {
                formData.append('imagesToDelete', sortOrder);
            });
        }

        if (pageState.newPhotos.length > 0) {
            pageState.newPhotos.forEach(file => {
                formData.append('images', file);
            });
        }

        // 디버그 로그
        console.log('📤 전송 데이터:', {
            name,
            email,
            phone,
            birthDate,
            pushNotification,
            memorialNotification,
            paymentNotification,
            familyNotification,
            marketingAgreed,
            photosToDelete: pageState.photosToDelete,
            newPhotosCount: pageState.newPhotos.length
        });

        // 통합 API 호출
        const response = await authFetch('/api/profile/update', {
            method: 'PUT',
            body: formData  // JSON이 아닌 FormData로 전송
        });

        const result = await response.json();

        if (result.status?.code === 'OK_0000') {
            console.log('✅ 저장 성공:', result);

            // 응답 데이터 안전 처리
            const responseData = result.response || {};

            // 기본값 보정
            responseData.preferredLanguage = responseData.preferredLanguage || 'KO';
            responseData.updatedAt = responseData.updatedAt || new Date().toISOString();

            // notificationSettings null 처리
            if (!responseData.notificationSettings) {
                responseData.notificationSettings = {
                    pushNotification: true,
                    memorialNotification: true,
                    paymentNotification: true,
                    familyNotification: true,
                    marketingAgreed: false
                };
            }

            // profileCompletion null 처리
            if (!responseData.profileCompletion) {
                responseData.profileCompletion = {
                    uploadedImageCount: responseData.totalImages || 0,
                    validImageCount: responseData.totalImages || 0,
                    completionPercentage: responseData.totalImages ? (responseData.totalImages * 20) : 0,
                    canStartVideoCall: (responseData.totalImages || 0) >= 5
                };
            }

            // 성공 처리
            showToast('프로필이 성공적으로 저장되었습니다.', 'success');

            // 상태 초기화
            pageState.photosToDelete = [];
            pageState.newPhotos = [];

            // 이미지 정보 안전 처리
            const imageUrls = responseData.imageUrls || [];
            const totalImages = responseData.totalImages || 0;

            console.log('📸 이미지 정보 업데이트:', {
                imageUrls,
                totalImages,
                hasImages: imageUrls.length > 0,
                isA1Case: imageUrls.length === 0 && totalImages === 0
            });

            // A-1 케이스: 기본 정보만 수정 (이미지 변경 없음)
            if (imageUrls.length === 0 && totalImages === 0) {
                console.log('📷 A-1 케이스: 기본 정보만 수정됨 - 이미지 상태 유지');
                // 기존 이미지 상태 그대로 유지 (변경하지 않음)
            }
            // A-3 케이스: 이미지 포함 수정
            else if (imageUrls.length > 0 || totalImages > 0) {
                console.log('📷 이미지 포함 수정 - 사진 슬롯 업데이트');
                pageState.photos = imageUrls;
                pageState.originalPhotoCount = totalImages;
                updateAllPhotoSlots(imageUrls);
            }

            // 헤더 정보 안전 업데이트
            updateProfileHeader({
                name: responseData.name || document.getElementById('userName')?.value?.trim(),
                email: responseData.email || document.getElementById('userEmail')?.value?.trim(),
                phone: responseData.phoneNumber || document.getElementById('userPhone')?.value?.trim(),
                birthDate: responseData.birthDate || document.getElementById('userBirthDate')?.value?.trim()
            });

            // 폼 필드 업데이트
            updateFormFields(responseData);

            // 디버그 정보 로깅
            console.log('🔔 알림 설정:', responseData.notificationSettings);
            console.log('📊 프로필 완성도:', responseData.profileCompletion);

            // 원본 데이터 업데이트
            saveOriginalFormData();

            // 영상통화 리다이렉트 조건 - 안전한 체크
            if (pageState.redirectFromVideoCall) {
                const canStartVideoCall = responseData.profileCompletion.canStartVideoCall;

                if (canStartVideoCall) {
                    console.log('🎥 영상통화 조건 충족 - 리다이렉트 시작');
                    showToast('프로필 설정이 완료되었습니다. 영상통화 페이지로 이동합니다.', 'success');
                    setTimeout(() => {
                        window.location.href = '/mobile/video-call';
                    }, 2000);
                } else {
                    console.log('🎥 영상통화 조건 미충족 - 현재 페이지 유지');
                }
            }

        } else {
            console.log('❌ 저장 실패:', result.status);
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
 * 폼 필드 업데이트 (서버 응답 기준)
 */
function updateFormFields(responseData) {
    console.log('📝 폼 필드 업데이트:', responseData);

    // 기본 정보 업데이트
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

    // 알림 설정 업데이트
    const notificationSettings = responseData.notificationSettings || {};

    const pushNotificationField = document.getElementById('pushNotification');
    if (pushNotificationField) {
        pushNotificationField.checked = notificationSettings.pushNotification !== false;
    }

    const memorialNotificationField = document.getElementById('memorialNotification');
    if (memorialNotificationField) {
        memorialNotificationField.checked = notificationSettings.memorialNotification !== false;
    }

    const paymentNotificationField = document.getElementById('paymentNotification');
    if (paymentNotificationField) {
        paymentNotificationField.checked = notificationSettings.paymentNotification !== false;
    }

    const familyNotificationField = document.getElementById('familyNotification');
    if (familyNotificationField) {
        familyNotificationField.checked = notificationSettings.familyNotification !== false;
    }

    const marketingAgreedField = document.getElementById('marketingAgreed');
    if (marketingAgreedField) {
        marketingAgreedField.checked = notificationSettings.marketingAgreed === true;
    }
}

/**
 * 모든 사진 슬롯 업데이트 (서버 응답 기준)
 */
function updateAllPhotoSlots(imageUrls) {
    // 모든 슬롯 초기화
    for (let i = 0; i < pageState.maxPhotos; i++) {
        resetPhotoSlot(i);
    }

    // 새 이미지들로 업데이트
    imageUrls.forEach((url, index) => {
        if (index < pageState.maxPhotos) {
            updatePhotoSlot(index, url, false);
        }
    });

    updatePhotoUploadStatus();
}

/**
 * 휴대폰 번호 유효성 검사
 */
function isValidPhoneNumber(phone) {
    const phoneRegex = /^010[0-9]{8}$/;
    const cleanPhone = phone.replace(/[-\s]/g, '');
    return phoneRegex.test(cleanPhone);
}

/**
 * 개별 폼 필드 검증
 */
function validateFormField(field) {
    if (!field) return true;

    const fieldName = field.name || field.id;
    const value = field.value?.trim() || '';

    // 기존 에러 제거
    clearFieldError(field);

    // 필수 필드 검사
    if (field.hasAttribute('required') && !value) {
        showFieldError(field, '이 필드는 필수입니다.');
        return false;
    }

    // 이메일 검사
    if (field.type === 'email' && value) {
        if (!isValidEmail(value)) {
            showFieldError(field, '올바른 이메일 주소를 입력하세요.');
            return false;
        }
    }

    // 전화번호 검사
    if (field.type === 'tel' && value) {
        if (!isValidPhoneNumber(value)) {
            showFieldError(field, '올바른 휴대폰 번호를 입력하세요. (예: 010-1234-5678)');
            return false;
        }
    }

    // 생년월일 검사
    if (field.type === 'date' && value) {
        if (!isValidBirthDate(value)) {
            showFieldError(field, '올바른 생년월일을 입력하세요.');
            return false;
        }
    }

    return true;
}

/**
 * 필드 에러 표시
 */
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

/**
 * 필드 에러 제거
 */
function clearFieldError(field) {
    field.classList.remove('is-invalid');

    const errorElement = field.parentElement.querySelector('.invalid-feedback');
    if (errorElement) {
        errorElement.style.display = 'none';
    }
}

/**
 * 회원탈퇴 처리
 */
async function handleDeleteAccount() {
    console.log('🗑️ 회원탈퇴 처리 시작');

    try {
        const confirmed = await showConfirm(
            '회원탈퇴',
            '정말 회원탈퇴 하시겠습니까?\n\n탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.',
            '탈퇴하기',
            '취소'
        );

        if (!confirmed) {
            console.log('회원탈퇴 취소됨');
            return;
        }

        // 한 번 더 확인
        const finalConfirmed = await showConfirm(
            '최종 확인',
            '마지막 확인입니다.\n정말로 탈퇴하시겠습니까?',
            '예, 탈퇴합니다',
            '아니요'
        );

        if (!finalConfirmed) {
            console.log('회원탈퇴 최종 취소됨');
            return;
        }

        // 로딩 표시
        const loading = showLoading('회원탈퇴 처리 중...');

        try {
            const response = await authFetch('/api/profile/delete-account', {
                method: 'DELETE'
            });

            const result = await response.json();

            if (result.status?.code === 'OK_0000') {
                showToast('회원탈퇴가 완료되었습니다.', 'success');

                // 2초 후 로그인 페이지로 이동
                setTimeout(() => {
                    window.location.href = '/mobile/login?reason=account_deleted';
                }, 2000);

            } else {
                throw new Error(result.status?.message || '회원탈퇴 실패');
            }

        } catch (error) {
            console.error('회원탈퇴 API 오류:', error);
            showToast('회원탈퇴 처리 중 오류가 발생했습니다.', 'error');
        } finally {
            loading.hide();
        }

    } catch (error) {
        console.error('회원탈퇴 처리 실패:', error);
        showToast('회원탈퇴 처리에 실패했습니다.', 'error');
    }
}

/**
 * 기존 사진 로드
 */
function loadExistingPhotos() {
    const existingPhotos = window.serverData?.currentUser?.profileImages || [];

    if (existingPhotos.length > 0) {
        pageState.originalPhotoCount = existingPhotos.length;
        pageState.photos = [...existingPhotos];

        // 이미 5장이 있으면 업로드 시작된 것으로 간주
        if (existingPhotos.length === 5) {
            pageState.hasStartedPhotoUpload = true;
        }

        existingPhotos.forEach((photoUrl, index) => {
            if (index < pageState.maxPhotos) {
                updatePhotoSlot(index, photoUrl);
            }
        });

        console.log('기존 사진 로드 완료:', existingPhotos.length + '장');
        updatePhotoUploadStatus();
    }
}

/**
 * 사진 선택 처리
 */
function handlePhotoSelect(e) {
    const files = Array.from(e.target.files);

    if (files.length === 0) return;

    // 사진 업로드 시작 표시
    if (!pageState.hasStartedPhotoUpload && pageState.originalPhotoCount === 0) {
        pageState.hasStartedPhotoUpload = true;
        showPhotoUploadCommitment();
    }

    // 현재 상태 계산 (삭제 예정 제외)
    const currentValidCount = pageState.photos.filter((photo, index) =>
        photo && !pageState.photosToDelete.includes(index + 1)
    ).length;

    const newPhotoCount = pageState.newPhotos.length;
    const remainingSlots = pageState.maxPhotos - currentValidCount - newPhotoCount;

    if (remainingSlots === 0) {
        showToast('최대 5장까지만 업로드할 수 있습니다.', 'warning');
        return;
    }

    // 파일 검증 및 임시 저장
    const validFiles = files.slice(0, remainingSlots);
    addNewPhotos(validFiles);

    // 파일 인풋 리셋
    e.target.value = '';
}

/**
 * 새 사진 추가 (임시)
 */
function addNewPhotos(files) {
    files.forEach(file => {
        if (validateImageFile(file)) {
            // 임시 저장
            pageState.newPhotos.push(file);

            // 빈 슬롯 찾아서 미리보기 표시
            const emptySlotIndex = findNextEmptySlot();
            if (emptySlotIndex !== -1) {
                showImagePreview(emptySlotIndex, file);
            }
        }
    });

    updatePhotoUploadStatus();
}

/**
 * 빈 슬롯 찾기
 */
function findNextEmptySlot() {
    for (let i = 0; i < pageState.maxPhotos; i++) {
        const isOriginalEmpty = !pageState.photos[i];
        const isMarkedForDeletion = pageState.photosToDelete.includes(i + 1);
        const hasNewPhoto = document.querySelector(`.photo-slot[data-index="${i}"]`)?.classList.contains('new-photo');

        if (isOriginalEmpty || isMarkedForDeletion || !hasNewPhoto) {
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
        updatePhotoSlot(slotIndex, e.target.result, true); // true = 새 사진 표시
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
 * 사진 제거
 */
async function removePhoto(index) {
    const slot = document.querySelector(`.photo-slot[data-index="${index}"]`);
    const isNewPhoto = slot?.classList.contains('new-photo');
    const sortOrder = index + 1;

    if (isNewPhoto) {
        // 새로 추가된 사진 제거
        const newPhotoIndex = pageState.newPhotos.findIndex((_, i) => {
            // 이건 좀 복잡한 로직이 되는데, 간단하게 마지막 추가된 것부터 제거
            return i === pageState.newPhotos.length - 1;
        });

        if (newPhotoIndex !== -1) {
            pageState.newPhotos.splice(newPhotoIndex, 1);
        }
    } else if (pageState.photos[index]) {
        // 기존 사진을 삭제 예정으로 표시
        if (!pageState.photosToDelete.includes(sortOrder)) {
            pageState.photosToDelete.push(sortOrder);
        }
    }

    // 마지막 사진 삭제 시 상태 초기화 확인
    const remainingCount = getTotalValidPhotoCount();
    if (pageState.hasStartedPhotoUpload && remainingCount === 0) {
        const confirmed = await showConfirm(
            '사진 삭제 확인',
            '모든 사진을 삭제하면 프로필 사진 업로드를 처음부터 다시 시작해야 합니다. 정말 삭제하시겠습니까?',
            '삭제하기',
            '취소'
        );

        if (!confirmed) {
            // 삭제 취소
            if (isNewPhoto) {
                // 새 사진 복원은 복잡하므로 페이지 새로고침 권장
                showToast('삭제가 취소되었습니다.', 'info');
                return;
            } else {
                // 기존 사진 삭제 예정 취소
                const deleteIndex = pageState.photosToDelete.indexOf(sortOrder);
                if (deleteIndex !== -1) {
                    pageState.photosToDelete.splice(deleteIndex, 1);
                }
                return;
            }
        }

        // 모든 상태 초기화
        pageState.hasStartedPhotoUpload = false;
        pageState.photosToDelete = [];
        pageState.newPhotos = [];
        hidePhotoUploadCommitment();
    }

    // 슬롯 리셋
    resetPhotoSlot(index);
    updatePhotoUploadStatus();
    showToast('사진이 삭제되었습니다.', 'success');
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

        if (pageState.hasStartedPhotoUpload || totalPhotoCount > 0) {
            saveBtn.innerHTML = `
                <i class="fas fa-exclamation-triangle"></i>
                프로필 저장 (${totalPhotoCount}/5 완료)
            `;
        } else {
            saveBtn.innerHTML = '<i class="fas fa-save"></i> 프로필 저장';
        }
    } else {
        saveBtn.classList.remove('btn-warning');
        saveBtn.classList.add('btn-primary');

        if (totalPhotoCount === 5) {
            saveBtn.innerHTML = '<i class="fas fa-check"></i> 프로필 저장 (완료)';
        } else {
            saveBtn.innerHTML = '<i class="fas fa-save"></i> 프로필 저장';
        }
    }
}

/**
 * 사진 업로드 상태 업데이트
 */
function updatePhotoUploadStatus() {
    const totalCount = getTotalValidPhotoCount();
    const statusElement = document.getElementById('photoUploadStatus');

    if (!statusElement) {
        const photoSection = document.querySelector('.photo-section');
        const statusDiv = document.createElement('div');
        statusDiv.id = 'photoUploadStatus';
        statusDiv.className = 'photo-upload-status';
        photoSection.insertBefore(statusDiv, document.getElementById('photoGrid'));
    }

    const status = document.getElementById('photoUploadStatus');

    if (pageState.hasStartedPhotoUpload || totalCount > 0) {
        const remaining = pageState.maxPhotos - totalCount;

        if (totalCount === 5) {
            status.innerHTML = `
                <div class="status-complete">
                    <i class="fas fa-check-circle"></i>
                    <span>프로필 사진 업로드 완료! (${totalCount}/5)</span>
                </div>
            `;
            status.className = 'photo-upload-status complete';
        } else {
            status.innerHTML = `
                <div class="status-progress">
                    <i class="fas fa-clock"></i>
                    <span>프로필 사진 업로드 진행중 (${totalCount}/5) - ${remaining}장 더 필요</span>
                </div>
            `;
            status.className = 'photo-upload-status progress';
        }
        status.style.display = 'block';
    } else {
        status.style.display = 'none';
    }
}

// 나머지 헬퍼 함수들
function showPhotoUploadCommitment() {
    const alert = document.getElementById('photoAlert');
    if (alert) {
        alert.style.display = 'block';
        alert.innerHTML = `
            <i class="fas fa-info-circle"></i>
            <strong>중요:</strong> 프로필 사진 업로드를 시작하셨습니다. 
            반드시 5장을 모두 완성해야 저장할 수 있습니다.
        `;
        alert.className = 'alert alert-warning';
    }
    updatePhotoUploadStatus();
}

function hidePhotoUploadCommitment() {
    const alert = document.getElementById('photoAlert');
    if (alert) {
        alert.style.display = 'none';
    }

    const status = document.getElementById('photoUploadStatus');
    if (status) {
        status.style.display = 'none';
    }
}

function resetPhotoSlot(index) {
    const slot = document.querySelector(`.photo-slot[data-index="${index}"]`);
    if (!slot) return;

    slot.classList.remove('filled', 'new-photo', 'uploading', 'error');
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

function saveOriginalFormData() {
    pageState.originalFormData = {
        name: document.getElementById('userName')?.value || '',
        birthDate: document.getElementById('userBirthDate')?.value || '',
        phone: document.getElementById('userPhone')?.value || '',
        email: document.getElementById('userEmail')?.value || ''
    };
}

function updateProfileHeader(profileData) {
    const avatar = document.getElementById('profileAvatar');
    const name = document.getElementById('profileName');
    const email = document.getElementById('profileEmail');

    if (name && profileData.name) {
        name.textContent = profileData.name;
    }

    if (email) {
        email.textContent = profileData.email || profileData.phone || '정보 없음';
    }

    if (avatar && profileData.name) {
        avatar.textContent = profileData.name.charAt(0);
    }
}

function handleVideoCallRedirect() {
    console.log('🎥 영상통화 리다이렉트 처리');

    if (!pageState.redirectFromVideoCall) {
        console.log('영상통화 리다이렉트 아님 - 건너뜀');
        return;
    }

    console.log('영상통화에서 리다이렉트됨 - 특별 처리 시작');

    // 영상통화 관련 알림 표시
    const videoCallAlert = document.getElementById('videoCallAlert');
    if (videoCallAlert) {
        videoCallAlert.style.display = 'block';
        videoCallAlert.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    // 사진 섹션 강조
    const photoSection = document.querySelector('.photo-section');
    if (photoSection) {
        photoSection.classList.add('highlight-for-video-call');

        // 3초 후 강조 해제
        setTimeout(() => {
            photoSection.classList.remove('highlight-for-video-call');
        }, 3000);
    }

    // 페이지 제목 업데이트
    const pageTitle = document.querySelector('.page-title');
    if (pageTitle) {
        pageTitle.textContent = '영상통화용 프로필 설정';
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

// 이벤트 리스너
document.addEventListener('DOMContentLoaded', function() {
    try {
        loadServerData();
        saveOriginalFormData();
        bindEvents();
        loadExistingPhotos();
        handleVideoCallRedirect();

        // 실시간 업데이트
        setInterval(updateSaveButtonState, 1000);

        document.querySelectorAll('#profileForm input').forEach(input => {
            input.addEventListener('input', () => {
                setTimeout(updateSaveButtonState, 100);
            });
        });

        console.log('프로필 설정 페이지 초기화 완료');
    } catch (error) {
        console.error('프로필 설정 페이지 초기화 실패:', error);
        showToast('페이지 초기화에 실패했습니다.', 'error');
    }
});

// 전역 함수로 노출
window.loadServerData = loadServerData;
window.bindEvents = bindEvents;
window.handleVideoCallRedirect = handleVideoCallRedirect;
window.handleDeleteAccount = handleDeleteAccount;

console.log('통합 API용 프로필 설정 페이지 스크립트 로드 완료');