// family-list-enhanced.js - SMS 앱 연동 강화 버전
import { authFetch } from './commonFetch.js';
import { showToast, showConfirm, showLoading, hideLoading } from './common.js';

// 전역 상태
let pageState = {
    selectedMemorialId: null,
    familyMembers: [],
    currentMemberId: null,
    currentInviteToken: null // SMS 초대 토큰 저장용
};

/**
 * 페이지 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('향상된 가족 목록 페이지 초기화 시작');

    try {
        // 서버 데이터 로드
        loadServerData();

        // 이벤트 바인딩
        bindEvents();

        console.log('향상된 가족 목록 페이지 초기화 완료');
    } catch (error) {
        console.error('페이지 초기화 실패:', error);
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

        console.log('서버 데이터 로드 완료:', {
            memorialId: pageState.selectedMemorialId,
            membersCount: pageState.familyMembers.length
        });
    } else {
        console.warn('window.serverData가 없습니다.');
    }
}

/**
 * 초대 보내기 - SMS 앱 연동 강화
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

        // 초대 데이터 구성
        const inviteData = {
            memorialId: pageState.selectedMemorialId,
            method: method,
            contact: method === 'email' ? email : phone,
            relationship: relationship,
            message: message
        };

        console.log('초대 발송 요청:', inviteData);

        // API 호출
        const response = await authFetch('/api/family/invite', {
            method: 'POST',
            body: JSON.stringify(inviteData)
        });

        console.log('초대 발송 응답:', response);

        if (response.status?.code === 'OK_0000') {
            if (method === 'email') {
                showToast('이메일이 발송되었습니다.', 'success');
            } else if (method === 'sms') {
                // SMS 앱 연동 처리
                await handleSmsInvite(inviteData);
            }

            // 모달 닫기
            bootstrap.Modal.getInstance(document.getElementById('inviteModal')).hide();
            resetInviteForm();

            // 3초 후 페이지 새로고침
            setTimeout(() => {
                window.location.href = `/mobile/family?memorialId=${pageState.selectedMemorialId}`;
            }, 3000);

        } else {
            const errorMessage = response.status?.message || '초대 발송에 실패했습니다.';
            showToast(errorMessage, 'error');
        }

    } catch (error) {
        console.error('초대 발송 실패:', error);
        showToast('초대 발송 중 오류가 발생했습니다. 다시 시도해주세요.', 'error');
    } finally {
        const btn = document.getElementById('sendInviteBtn');
        btn.disabled = false;
        btn.innerHTML = '초대 보내기';
    }
}

/**
 * SMS 초대 처리 - 앱 연동 강화
 */
async function handleSmsInvite(inviteData) {
    console.log('SMS 초대 처리 시작:', inviteData);

    try {
        // 1. 초대 토큰 생성 및 SMS 데이터 조회
        const smsResponse = await authFetch('/api/family/invite', {
            method: 'POST',
            body: JSON.stringify(inviteData)
        });

        if (smsResponse.status?.code !== 'OK_0000') {
            throw new Error('SMS 초대 토큰 생성 실패');
        }

        // 2. 토큰 추출 (응답에서 토큰 정보 필요)
        // TODO: 백엔드에서 토큰 정보를 응답에 포함하도록 수정 필요

        // 3. SMS 앱 실행 시도
        const phoneNumber = inviteData.contact;
        const smsContent = createSmsContent(inviteData);

        await openSmsApp(phoneNumber, smsContent);

        showToast('문자 앱이 실행되었습니다. 메시지를 확인하고 전송해 주세요.', 'success');

    } catch (error) {
        console.error('SMS 초대 처리 실패:', error);

        // 실패 시 대체 방법 제공
        showSmsAlternativeOptions(inviteData);
    }
}

/**
 * SMS 앱 실행
 */
async function openSmsApp(phoneNumber, message) {
    console.log('SMS 앱 실행 시도:', { phoneNumber: maskPhoneNumber(phoneNumber), messageLength: message.length });

    // 전화번호 정리 (하이픈 제거)
    const cleanPhoneNumber = phoneNumber.replace(/[^0-9]/g, '');

    // 메시지 URL 인코딩
    const encodedMessage = encodeURIComponent(message);

    // SMS URL 생성
    const smsUrl = `sms:${cleanPhoneNumber}?body=${encodedMessage}`;

    console.log('생성된 SMS URL:', smsUrl.substring(0, 50) + '...');

    try {
        // 1. 직접 링크 실행 시도
        const link = document.createElement('a');
        link.href = smsUrl;
        link.style.display = 'none';
        document.body.appendChild(link);

        // 사용자 액션으로 실행
        link.click();

        // 정리
        document.body.removeChild(link);

        console.log('SMS 앱 실행 완료');

        // 2. 추가 확인 - 실제로 앱이 실행되었는지 확인
        setTimeout(() => {
            // 앱 실행 실패 시 대체 방법 제공
            showSmsConfirmation(phoneNumber, message);
        }, 1000);

    } catch (error) {
        console.error('SMS 앱 실행 실패:', error);
        throw error;
    }
}

/**
 * SMS 내용 생성
 */
function createSmsContent(inviteData) {
    const appName = '토마토리멤버';
    const inviterName = '초대자'; // 현재 사용자 이름으로 교체 필요

    let message = `[${appName}] 가족 메모리얼 초대\n\n`;
    message += `${inviterName}님이 메모리얼에 초대했습니다.\n`;

    if (inviteData.message && inviteData.message.trim()) {
        message += `\n💌 "${inviteData.message}"\n`;
    }

    message += `\n초대 수락: [링크가 여기에 표시됩니다]\n`;
    message += `\n⏰ 초대는 7일 후 만료됩니다.`;

    return message;
}

/**
 * SMS 전송 확인 다이얼로그
 */
function showSmsConfirmation(phoneNumber, message) {
    const confirmed = confirm(
        `문자 앱이 실행되었습니다.\n\n` +
        `받는 사람: ${maskPhoneNumber(phoneNumber)}\n` +
        `메시지가 올바르게 입력되었는지 확인하고 전송해 주세요.\n\n` +
        `앱이 실행되지 않았다면 '취소'를 클릭하여 다른 방법을 시도하세요.`
    );

    if (!confirmed) {
        showSmsAlternativeOptions({ contact: phoneNumber, message });
    }
}

/**
 * SMS 대체 방법 제공
 */
function showSmsAlternativeOptions(inviteData) {
    const alternatives = [
        '1. 문자 앱을 직접 실행하여 수동으로 전송',
        '2. 이메일 방식으로 변경하여 재시도',
        '3. 나중에 다시 시도'
    ];

    const message = `문자 앱 실행이 실패했습니다.\n\n다음 방법을 시도해 보세요:\n\n${alternatives.join('\n')}`;

    showToast(message, 'warning', 8000);

    // 수동 복사 옵션 제공
    showManualSmsOption(inviteData);
}

/**
 * 수동 SMS 옵션 제공
 */
function showManualSmsOption(inviteData) {
    const message = createSmsContent(inviteData);

    // 클립보드 복사 시도
    if (navigator.clipboard) {
        navigator.clipboard.writeText(message).then(() => {
            showToast('메시지가 클립보드에 복사되었습니다. 문자 앱에서 붙여넣기 해주세요.', 'info');
        }).catch(err => {
            console.error('클립보드 복사 실패:', err);
            showManualCopyDialog(message);
        });
    } else {
        showManualCopyDialog(message);
    }
}

/**
 * 수동 복사 다이얼로그
 */
function showManualCopyDialog(message) {
    const modal = document.createElement('div');
    modal.className = 'modal fade';
    modal.innerHTML = `
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">메시지 수동 복사</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <p>아래 메시지를 복사하여 문자 앱에서 사용하세요:</p>
                    <textarea class="form-control" rows="6" readonly>${message}</textarea>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">닫기</button>
                    <button type="button" class="btn btn-primary" onclick="copyToClipboard('${message.replace(/'/g, "\\'")}')">복사</button>
                </div>
            </div>
        </div>
    `;

    document.body.appendChild(modal);

    const bsModal = new bootstrap.Modal(modal);
    bsModal.show();

    // 모달 닫힐 때 정리
    modal.addEventListener('hidden.bs.modal', () => {
        document.body.removeChild(modal);
    });
}

/**
 * 전화번호 마스킹
 */
function maskPhoneNumber(phoneNumber) {
    if (!phoneNumber || phoneNumber.length < 8) {
        return '****';
    }

    if (phoneNumber.includes('-')) {
        const parts = phoneNumber.split('-');
        if (parts.length === 3) {
            return parts[0] + '-****-' + parts[2];
        }
    }

    return phoneNumber.substring(0, 3) + '****' + phoneNumber.substring(phoneNumber.length - 4);
}

/**
 * 클립보드 복사
 */
async function copyToClipboard(text) {
    try {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(text);
            showToast('클립보드에 복사되었습니다.', 'success');
            return true;
        } else {
            // 폴백 방법
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

            if (result) {
                showToast('클립보드에 복사되었습니다.', 'success');
            } else {
                showToast('클립보드 복사에 실패했습니다.', 'error');
            }
            return result;
        }
    } catch (error) {
        console.error('클립보드 복사 실패:', error);
        showToast('클립보드 복사에 실패했습니다.', 'error');
        return false;
    }
}

// 나머지 기존 함수들은 그대로 유지...
// (권한 설정, 구성원 관리 등의 기존 함수들)

/**
 * 현재 구성원 정보 가져오기
 */
function getCurrentMember() {
    if (!pageState.currentMemberId) return null;
    return pageState.familyMembers.find(m => m.id == pageState.currentMemberId);
}

/**
 * 권한 설정 모달 열기
 */
function openPermissionModal(memberId) {
    const member = pageState.familyMembers.find(m => m.id == memberId);

    if (!member) {
        showToast('구성원 정보를 찾을 수 없습니다.', 'error');
        return;
    }

    if (member.relationship === 'SELF') {
        showToast('소유자는 권한 설정이 불가능합니다.', 'info');
        return;
    }

    pageState.currentMemberId = memberId;

    // 모달 정보 설정
    document.getElementById('permissionMemberName').textContent = member.member?.name?.substring(0, 1) || '?';
    document.getElementById('permissionMemberFullName').textContent = member.member?.name || '알 수 없음';
    document.getElementById('permissionMemberRelation').textContent = `고인과의 관계: ${member.relationshipDisplayName || '미설정'}`;

    // 현재 권한 상태 설정
    const memorialAccess = member.permissions?.memorialAccess === true;
    const videoCallAccess = member.permissions?.videoCallAccess === true;

    document.getElementById('memorialAccessSwitch').checked = memorialAccess;
    document.getElementById('videoCallSwitch').checked = videoCallAccess;

    // 모달 열기
    const modal = new bootstrap.Modal(document.getElementById('permissionModal'));
    modal.show();
}

/**
 * 권한 저장
 */
async function savePermissions() {
    const member = getCurrentMember();
    if (!member) {
        showToast('구성원 정보를 찾을 수 없습니다.', 'error');
        return;
    }

    if (!pageState.selectedMemorialId) {
        showToast('선택된 메모리얼이 없습니다.', 'error');
        return;
    }

    const memorialAccess = document.getElementById('memorialAccessSwitch').checked;
    const videoCallAccess = document.getElementById('videoCallSwitch').checked;

    try {
        const btn = document.getElementById('savePermissionBtn');
        const originalText = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 저장 중...';

        const response = await authFetch(`/api/family/memorials/${pageState.selectedMemorialId}/members/${member.member.id}/permissions`, {
            method: 'PUT',
            body: JSON.stringify({
                memorialAccess: memorialAccess,
                videoCallAccess: videoCallAccess
            })
        });

        if (response.status?.code === 'OK_0000') {
            showToast('권한이 성공적으로 저장되었습니다.', 'success');
            bootstrap.Modal.getInstance(document.getElementById('permissionModal')).hide();
            setTimeout(() => window.location.reload(), 1000);
        } else {
            throw new Error(response.status?.message || '권한 저장에 실패했습니다.');
        }
    } catch (error) {
        console.error('권한 저장 실패:', error);
        showToast('권한 저장 중 오류가 발생했습니다.', 'error');
    } finally {
        const btn = document.getElementById('savePermissionBtn');
        btn.disabled = false;
        btn.innerHTML = '권한 저장';
    }
}

/**
 * 이벤트 바인딩
 */
function bindEvents() {
    // 뒤로가기 버튼
    const backBtn = document.getElementById('backBtn');
    if (backBtn) {
        backBtn.addEventListener('click', () => window.history.back());
    }

    // 초대 버튼
    const inviteBtn = document.getElementById('inviteBtn');
    if (inviteBtn) {
        inviteBtn.addEventListener('click', openInviteModal);
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

    // 권한 저장 버튼
    const savePermissionBtn = document.getElementById('savePermissionBtn');
    if (savePermissionBtn) {
        savePermissionBtn.addEventListener('click', savePermissions);
    }

    // 기타 이벤트들...
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
 * 초대 폼 리셋
 */
function resetInviteForm() {
    document.getElementById('inviteForm').reset();
    document.getElementById('emailMethodGroup').classList.add('active');
    document.getElementById('smsMethodGroup').classList.remove('active');
    document.getElementById('emailGroup').style.display = 'block';
    document.getElementById('phoneGroup').style.display = 'none';
}

// 전역 함수 등록
window.openPermissionModal = openPermissionModal;
window.savePermissions = savePermissions;
window.copyToClipboard = copyToClipboard;

console.log('family-list-enhanced.js 로드 완료');