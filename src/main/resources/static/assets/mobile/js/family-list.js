// family-list.js - 가족 목록 페이지 JavaScript (SSR 기반)
import { authFetch } from './commonFetch.js';
import { showToast, showConfirm } from './common.js';

// 전역 상태 (간소화)
let pageState = {
    selectedMemorialId: null,
    familyMembers: []
};

/**
 * 페이지 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('가족 목록 페이지 초기화 시작');

    try {
        // 서버 데이터 로드
        loadServerData();

        // 이벤트 바인딩
        bindEvents();

        console.log('가족 목록 페이지 초기화 완료');
    } catch (error) {
        console.error('가족 목록 페이지 초기화 실패:', error);
        showToast('페이지 초기화에 실패했습니다.', 'error');
    }
});

/**
 * 서버 데이터 로드
 */
function loadServerData() {
    if (window.serverData) {
        pageState.selectedMemorialId = window.serverData.selectedMemorial?.id;
        pageState.familyMembers = window.serverData.familyMembers || [];

        console.log('서버 데이터 로드 완료 - 메모리얼:', pageState.selectedMemorialId, '구성원:', pageState.familyMembers.length);
    }
}

/**
 * 가족 구성원 목록 새로고침 (API 호출)
 */
async function refreshFamilyMembersList() {
    if (!pageState.selectedMemorialId) {
        showToast('선택된 메모리얼이 없습니다.', 'warning');
        return;
    }

    try {
        const refreshBtn = document.getElementById('refreshBtn');
        if (refreshBtn) {
            refreshBtn.classList.add('loading');
            refreshBtn.querySelector('i').classList.add('fa-spin');
        }

        const response = await authFetch(`/api/family/memorial/${pageState.selectedMemorialId}/members`);
        const result = await response.json();

        if (result.status?.code === 'OK_0000') {
            // 페이지 새로고침으로 데이터 업데이트
            window.location.href = `/mobile/family?memorialId=${pageState.selectedMemorialId}`;
        } else {
            throw new Error(result.status?.message || '알 수 없는 오류');
        }
    } catch (error) {
        console.error('가족 구성원 목록 새로고침 실패:', error);
        showToast('가족 구성원 목록을 불러올 수 없습니다.', 'error');
    } finally {
        const refreshBtn = document.getElementById('refreshBtn');
        if (refreshBtn) {
            refreshBtn.classList.remove('loading');
            refreshBtn.querySelector('i').classList.remove('fa-spin');
        }
    }
}

/**
 * 메모리얼 드롭다운 토글
 */
function toggleMemorialDropdown() {
    const dropdown = document.getElementById('memorialDropdownMenu');
    const button = document.getElementById('memorialSelectBtn');

    if (!dropdown || !button) return;

    if (dropdown.classList.contains('show')) {
        closeMemorialDropdown();
    } else {
        dropdown.classList.add('show');
        button.classList.add('active');
    }
}

/**
 * 메모리얼 드롭다운 닫기
 */
function closeMemorialDropdown() {
    const dropdown = document.getElementById('memorialDropdownMenu');
    const button = document.getElementById('memorialSelectBtn');

    if (dropdown) dropdown.classList.remove('show');
    if (button) button.classList.remove('active');
}

/**
 * 초대 모달 열기
 */
function openInviteModal() {
    if (!pageState.selectedMemorialId) {
        showToast('선택된 메모리얼이 없습니다.', 'warning');
        return;
    }

    const modal = new bootstrap.Modal(document.getElementById('inviteModal'));
    modal.show();
}

/**
 * 초대 보내기
 */
async function sendInvite() {
    const method = document.querySelector('input[name="inviteMethod"]:checked')?.value;
    const email = document.getElementById('inviteEmail')?.value;
    const phone = document.getElementById('invitePhone')?.value;
    const relationship = document.getElementById('inviteRelationship')?.value;
    const message = document.getElementById('inviteMessage')?.value;

    // 유효성 검사
    if (!relationship) {
        showToast('고인과의 관계를 선택해주세요.', 'warning');
        return;
    }

    if (method === 'email' && !email) {
        showToast('이메일 주소를 입력해주세요.', 'warning');
        return;
    }

    if (method === 'sms' && !phone) {
        showToast('전화번호를 입력해주세요.', 'warning');
        return;
    }

    if (!pageState.selectedMemorialId) {
        showToast('선택된 메모리얼이 없습니다.', 'error');
        return;
    }

    try {
        const btn = document.getElementById('sendInviteBtn');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 전송 중...';

        // 🔧 기존 코드 수정: contact 값 올바르게 설정
        const inviteData = {
            memorialId: pageState.selectedMemorialId,  // 🔧 순서 변경
            method: method,
            contact: method === 'email' ? email : phone,  // 🔧 sms → phone 수정
            relationship: relationship,
            message: message
        };

        console.log('초대 발송 요청:', inviteData);

        // 🎯 핵심 API 호출
        const response = await authFetch('/api/family/invite', {
            method: 'POST',
            body: JSON.stringify(inviteData)
        });

        console.log('초대 발송 응답:', response);

        if (response.status?.code === 'OK_0000') {
            // 성공 처리
            const responseData = response.response;

            if (method === 'email') {
                // 이메일 발송 완료
                showToast('이메일이 발송되었습니다.', 'success');

            } else if (method === 'sms') {
                // SMS 앱 연동 처리
                await handleSmsAppIntegration(responseData);
            }

            // 모달 닫기
            bootstrap.Modal.getInstance(document.getElementById('inviteModal')).hide();

            // 폼 리셋
            resetInviteForm();

            // 3초 후 페이지 새로고침
            setTimeout(() => {
                window.location.href = `/mobile/family?memorialId=${pageState.selectedMemorialId}`;
            }, 3000);

        } else {
            // 오류 처리
            const errorMessage = response.status?.message || '초대 발송에 실패했습니다.';
            showToast(errorMessage, 'error');
        }

    } catch (error) {
        console.error('초대 발송 실패:', error);

        // 네트워크 오류 vs API 오류 구분
        if (error.message && error.message.includes('Network')) {
            showToast('네트워크 오류가 발생했습니다. 인터넷 연결을 확인해주세요.', 'error');
        } else {
            showToast('초대 발송 중 오류가 발생했습니다. 다시 시도해주세요.', 'error');
        }
    } finally {
        const btn = document.getElementById('sendInviteBtn');
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-paper-plane"></i> 초대 보내기';
    }
}

/**
 * SMS 앱 연동 처리
 */
async function handleSmsAppIntegration(responseData) {
    try {
        console.log('SMS 앱 연동 시작:', responseData);

        // responseData에서 토큰 추출 (응답 구조에 따라 조정 필요)
        const token = responseData.token || responseData.smsToken || responseData.inviteToken;

        if (token) {
            // SMS 앱 데이터 조회
            const smsData = await getSmsAppData(token);

            if (smsData.smsUrl) {
                // SMS 앱 실행 시도
                console.log('SMS 앱 실행:', smsData.smsUrl);

                // iOS/Android SMS 앱 실행
                window.location.href = smsData.smsUrl;

                // 사용자 안내
                showToast('문자 앱이 실행됩니다. 메시지를 확인 후 전송해주세요.', 'info');

            } else if (smsData.message) {
                // 폴백: 클립보드 복사
                if (await copyToClipboard(smsData.message)) {
                    showToast('메시지가 클립보드에 복사되었습니다. 직접 문자를 보내주세요.', 'success');
                } else {
                    showToast('SMS 메시지가 준비되었습니다. 직접 문자를 보내주세요.', 'info');
                }
            }
        } else {
            // 토큰 없이 SMS 처리 완료
            showToast('SMS 초대가 준비되었습니다.', 'success');
        }

    } catch (error) {
        console.error('SMS 앱 연동 실패:', error);
        showToast('SMS 앱 연동에 실패했습니다. 수동으로 문자를 보내주세요.', 'warning');
    }
}

async function copyToClipboard(text) {
    try {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(text);
            return true;
        } else {
            // 폴백 방법 (HTTP 환경용)
            const textArea = document.createElement('textarea');
            textArea.value = text;
            textArea.style.position = 'fixed';
            textArea.style.left = '-999999px';
            textArea.style.top = '-999999px';
            document.body.appendChild(textArea);
            textArea.focus();
            textArea.select();

            const result = document.execCommand('copy');
            textArea.remove();
            return result;
        }
    } catch (error) {
        console.error('클립보드 복사 실패:', error);
        return false;
    }
}

/**
 * 초대 토큰 유효성 확인
 * 용도: 초대 링크 클릭 시 토큰이 유효한지 확인
 * 사용 시점: 초대 수락 페이지 진입 시
 */
async function validateInviteToken(token) {
    try {
        const response = await authFetch(`/api/family/invite/validate/${token}`);

        if (response.status?.code === 'OK_0000') {
            return response.response.valid;
        } else {
            return false;
        }

    } catch (error) {
        console.error('토큰 유효성 확인 실패:', error);
        return false;
    }
}

/**
 * SMS 앱 데이터 조회
 */
async function getSmsAppData(token) {
    try {
        console.log('SMS 데이터 조회 시작:', token);

        const response = await authFetch(`/api/family/invite/sms/${token}`);

        if (response.status?.code === 'OK_0000') {
            console.log('SMS 데이터 조회 성공:', response.response);
            return response.response;
        } else {
            throw new Error(response.status?.message || 'SMS 데이터 조회 실패');
        }

    } catch (error) {
        console.error('SMS 데이터 조회 실패:', error);
        throw error;
    }
}


/**
 * 초대 폼 리셋
 */
function resetInviteForm() {
    document.getElementById('inviteForm').reset();
    document.getElementById('emailMethodGroup').classList.add('active');
    document.getElementById('smsMethodGroup').classList.remove('active');
    document.getElementById('emailGroup').style.display = 'block';
    document.getElementById('phoneGroup').style.display = 'none';
}

/**
 * 권한 설정 모달 (준비 중)
 */
function openPermissionModal(memberId) {
    showToast('권한 설정 기능은 준비 중입니다.', 'info');
}

/**
 * 구성원 메뉴 (준비 중)
 */
function showMemberMenu(memberId, memberName) {
    showToast(`${memberName}님의 메뉴 기능은 준비 중입니다.`, 'info');
}

/**
 * 구성원 액션 버튼 이벤트 바인딩
 */
function bindMemberActionEvents() {
    // 권한 설정 버튼
    document.querySelectorAll('.permission-btn:not(.owner-permission)').forEach(btn => {
        btn.addEventListener('click', function() {
            const memberId = this.getAttribute('data-member-id');
            openPermissionModal(memberId);
        });
    });

    // 메뉴 버튼
    document.querySelectorAll('.menu-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const memberId = this.getAttribute('data-member-id');
            const memberName = this.getAttribute('data-member-name');
            showMemberMenu(memberId, memberName);
        });
    });
}

/**
 * 이벤트 바인딩
 */
function bindEvents() {
    // 뒤로가기 버튼
    const backBtn = document.getElementById('backBtn');
    if (backBtn) {
        backBtn.addEventListener('click', () => {
            window.history.back();
        });
    }

    // 메모리얼 선택 버튼
    const memorialSelectBtn = document.getElementById('memorialSelectBtn');
    if (memorialSelectBtn) {
        memorialSelectBtn.addEventListener('click', toggleMemorialDropdown);
    }

    // 초대 버튼
    const inviteBtn = document.getElementById('inviteBtn');
    if (inviteBtn) {
        inviteBtn.addEventListener('click', openInviteModal);
    }

    // 새로고침 버튼
    const refreshBtn = document.getElementById('refreshBtn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', refreshFamilyMembersList);
    }

    // 초대 방법 라디오 버튼
    const emailMethod = document.getElementById('emailMethod');
    const smsMethod = document.getElementById('smsMethod');
    const emailGroup = document.getElementById('emailGroup');
    const phoneGroup = document.getElementById('phoneGroup');
    const emailMethodGroup = document.getElementById('emailMethodGroup');
    const smsMethodGroup = document.getElementById('smsMethodGroup');

    if (emailMethod && smsMethod) {
        emailMethod.addEventListener('change', function() {
            if (this.checked) {
                if (emailGroup) emailGroup.style.display = 'block';
                if (phoneGroup) phoneGroup.style.display = 'none';
                if (emailMethodGroup) emailMethodGroup.classList.add('active');
                if (smsMethodGroup) smsMethodGroup.classList.remove('active');
            }
        });

        smsMethod.addEventListener('change', function() {
            if (this.checked) {
                if (emailGroup) emailGroup.style.display = 'none';
                if (phoneGroup) phoneGroup.style.display = 'block';
                if (emailMethodGroup) emailMethodGroup.classList.remove('active');
                if (smsMethodGroup) smsMethodGroup.classList.add('active');
            }
        });
    }

    // 전화번호 형식 자동 변환
    const phoneInput = document.getElementById('invitePhone');
    if (phoneInput) {
        phoneInput.addEventListener('input', function(e) {
            let value = e.target.value.replace(/[^0-9]/g, '');
            if (value.length > 3 && value.length <= 7) {
                value = value.replace(/(\d{3})(\d+)/, '$1-$2');
            } else if (value.length > 7) {
                value = value.replace(/(\d{3})(\d{4})(\d+)/, '$1-$2-$3');
            }
            e.target.value = value;
        });
    }

    // 초대 보내기 버튼
    const sendInviteBtn = document.getElementById('sendInviteBtn');
    if (sendInviteBtn) {
        sendInviteBtn.addEventListener('click', sendInvite);
    }

    // 드롭다운 외부 클릭 감지
    document.addEventListener('click', function(e) {
        const dropdown = document.getElementById('memorialDropdownMenu');
        const button = document.getElementById('memorialSelectBtn');

        if (dropdown && button) {
            if (!dropdown.contains(e.target) && !button.contains(e.target)) {
                closeMemorialDropdown();
            }
        }
    });

    // 구성원 액션 버튼 이벤트 바인딩
    bindMemberActionEvents();
}

// 전역 함수 등록 (HTML에서 사용)
window.openPermissionModal = openPermissionModal;
window.showMemberMenu = showMemberMenu;
window.handleSmsAppIntegration = handleSmsAppIntegration;
window.getSmsAppData = getSmsAppData;
window.copyToClipboard = copyToClipboard;
window.validateInviteToken = validateInviteToken;

console.log('family-list.js 로드 완료');