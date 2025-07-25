// family-list-enhanced.js - 개선된 에러 처리 및 API 호출
import {
    apiPost,
    apiGet,
    apiPut,
    apiDelete,
    handleError,
    showSuccess,
    showInfo,
    showWarning,
    extractErrorMessage
} from './commonFetch.js';

// 전역 상태
let pageState = {
    selectedMemorialId: null,
    familyMembers: [],
    currentMemberId: null,
    currentRealMemberId: null,
    currentInviteToken: null
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

        // 메모리얼 드롭다운 초기화
        initMemorialDropdown();

        console.log('향상된 가족 목록 페이지 초기화 완료');
    } catch (error) {
        console.error('페이지 초기화 실패:', error);
        handleError(error, { errorPrefix: '페이지 초기화 실패' });
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
 * 메모리얼 드롭다운 초기화
 */
function initMemorialDropdown() {
    console.log('메모리얼 드롭다운 초기화 시작');

    const selectBtn = document.getElementById('memorialSelectBtn');
    const dropdownMenu = document.getElementById('memorialDropdownMenu');

    if (!selectBtn || !dropdownMenu) {
        console.warn('메모리얼 드롭다운 요소를 찾을 수 없습니다.');
        return;
    }

    // 드롭다운 토글 이벤트
    selectBtn.addEventListener('click', function(e) {
        e.preventDefault();
        e.stopPropagation();

        console.log('메모리얼 선택 버튼 클릭됨');

        // 드롭다운 메뉴 토글
        const isOpen = dropdownMenu.classList.contains('show');

        if (isOpen) {
            closeMemorialDropdown();
        } else {
            openMemorialDropdown();
        }
    });

    // 드롭다운 외부 클릭 시 닫기
    document.addEventListener('click', function(e) {
        if (!selectBtn.contains(e.target) && !dropdownMenu.contains(e.target)) {
            closeMemorialDropdown();
        }
    });

    // 메모리얼 항목 클릭 이벤트
    const memorialItems = dropdownMenu.querySelectorAll('.memorial-item');
    memorialItems.forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();

            const memorialId = this.getAttribute('data-memorial-id');
            console.log('메모리얼 선택됨:', memorialId);

            // 현재 선택된 메모리얼과 같으면 드롭다운만 닫기
            if (memorialId == pageState.selectedMemorialId) {
                closeMemorialDropdown();
                return;
            }

            // 다른 메모리얼 선택 시 페이지 이동
            selectMemorial(memorialId);
        });
    });

    console.log('메모리얼 드롭다운 초기화 완료');
}

/**
 * 메모리얼 드롭다운 열기
 */
function openMemorialDropdown() {
    const selectBtn = document.getElementById('memorialSelectBtn');
    const dropdownMenu = document.getElementById('memorialDropdownMenu');
    const arrow = selectBtn.querySelector('.dropdown-arrow i');

    console.log('메모리얼 드롭다운 열기');

    dropdownMenu.classList.add('show');
    selectBtn.classList.add('active');

    if (arrow) {
        arrow.style.transform = 'rotate(180deg)';
    }

    // 선택된 메모리얼로 스크롤
    const selectedItem = dropdownMenu.querySelector('.memorial-item.selected');
    if (selectedItem) {
        selectedItem.scrollIntoView({ block: 'nearest' });
    }
}

/**
 * 메모리얼 드롭다운 닫기
 */
function closeMemorialDropdown() {
    const selectBtn = document.getElementById('memorialSelectBtn');
    const dropdownMenu = document.getElementById('memorialDropdownMenu');
    const arrow = selectBtn.querySelector('.dropdown-arrow i');

    console.log('메모리얼 드롭다운 닫기');

    dropdownMenu.classList.remove('show');
    selectBtn.classList.remove('active');

    if (arrow) {
        arrow.style.transform = 'rotate(0deg)';
    }
}

/**
 * 메모리얼 선택
 */
function selectMemorial(memorialId) {
    console.log('메모리얼 선택 처리:', memorialId);

    if (!memorialId) {
        console.warn('메모리얼 ID가 없습니다.');
        return;
    }

    // 로딩 표시
    showInfo('메모리얼을 변경하고 있습니다...');

    // 페이지 이동
    setTimeout(() => {
        window.location.href = `/mobile/family?memorialId=${memorialId}`;
    }, 500);
}

/**
 * 초대 보내기 - 개선된 에러 처리
 */
async function sendInvite() {
    const method = document.querySelector('input[name="inviteMethod"]:checked')?.value;
    const email = document.getElementById('inviteEmail')?.value;
    const phone = document.getElementById('invitePhone')?.value;
    const relationship = document.getElementById('inviteRelationship')?.value;
    const message = document.getElementById('inviteMessage')?.value;

    // 유효성 검사
    if (!relationship) {
        showWarning('고인과의 관계를 선택해주세요.');
        return;
    }

    if (method === 'email' && !email) {
        showWarning('이메일 주소를 입력해주세요.');
        return;
    }

    if (method === 'sms' && !phone) {
        showWarning('전화번호를 입력해주세요.');
        return;
    }

    if (!pageState.selectedMemorialId) {
        handleError(new Error('선택된 메모리얼이 없습니다.'), {
            customMessage: '선택된 메모리얼이 없습니다.'
        });
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

        // API 호출 - 개선된 방식
        const response = await apiPost('/api/family/invite', inviteData, {
            showLoadingToast: true,
            loadingMessage: '초대를 발송하고 있습니다...',
            showErrorToast: false, // 커스텀 에러 처리
            errorPrefix: '초대 발송 실패'
        });

        console.log('초대 발송 응답:', response);

        if (response.status?.code === 'OK_0000') {
            if (method === 'email') {
                showSuccess('이메일이 발송되었습니다.');
            } else if (method === 'sms') {
                // SMS 앱 연동 처리
                await handleSmsInvite(response.response, inviteData);
            }

            // 모달 닫기
            const modal = bootstrap.Modal.getInstance(document.getElementById('inviteModal'));
            if (modal) modal.hide();
            resetInviteForm();

            // 3초 후 페이지 새로고침
            setTimeout(() => {
                window.location.href = `/mobile/family?memorialId=${pageState.selectedMemorialId}`;
            }, 3000);

        } else {
            handleError(new Error(response.status?.message || '초대 발송에 실패했습니다.'), {
                errorPrefix: '초대 발송 실패'
            });
        }

    } catch (error) {
        console.error('초대 발송 실패:', error);

        // 특정 에러 코드별 커스텀 메시지
        if (error.statusCode === 'ER_FI002') {
            handleError(error, {
                customMessage: '자기 자신은 초대할 수 없습니다. 다른 연락처를 입력해주세요.',
                errorPrefix: '초대 실패'
            });
        } else if (error.statusCode === 'ER_FI003') {
            handleError(error, {
                customMessage: '이미 가족 구성원으로 등록된 연락처입니다.',
                errorPrefix: '초대 실패'
            });
        } else if (error.statusCode === 'ER_FI004') {
            handleError(error, {
                customMessage: '이미 처리되지 않은 초대가 있습니다.',
                errorPrefix: '초대 실패'
            });
        } else {
            handleError(error, { errorPrefix: '초대 발송 실패' });
        }
    } finally {
        const btn = document.getElementById('sendInviteBtn');
        btn.disabled = false;
        btn.innerHTML = '초대 보내기';
    }
}

/**
 * SMS 초대 처리 - 서버 토큰 활용
 */
async function handleSmsInvite(responseData, inviteData) {
    console.log('SMS 초대 처리 시작:', responseData);

    try {
        // 서버에서 제공하는 SMS URL 사용
        if (responseData?.smsUrl) {
            showSuccess('문자 앱을 실행합니다...');

            // SMS 앱 실행
            await openSmsAppWithUrl(responseData.smsUrl, responseData.shortSmsUrl, responseData);

            showSuccess('문자 앱이 실행되었습니다. 메시지를 확인하고 전송해 주세요.');
        } else {
            // 폴백: 로컬에서 SMS 내용 생성 (토큰 포함)
            const phoneNumber = inviteData.contact;
            const smsContent = createSmsContent(inviteData, responseData?.token);
            await openSmsApp(phoneNumber, smsContent);
        }

    } catch (error) {
        console.error('SMS 초대 처리 실패:', error);
        handleError(error, {
            customMessage: 'SMS 앱 실행에 실패했습니다. 수동으로 문자를 보내주세요.',
            errorPrefix: 'SMS 실행 실패'
        });
        showSmsAlternativeOptions(inviteData, responseData?.token);
    }
}

/**
 * 서버 제공 SMS URL로 앱 실행
 */
async function openSmsAppWithUrl(smsUrl, shortSmsUrl, smsData) {
    console.log('서버 제공 SMS URL로 앱 실행:', {
        hasLongUrl: !!smsUrl,
        hasShortUrl: !!shortSmsUrl,
        recommendShort: smsData?.recommendShort
    });

    // 사용할 URL 결정 (길이가 긴 경우 짧은 버전 권장)
    const urlToUse = (smsData?.recommendShort && shortSmsUrl) ? shortSmsUrl : smsUrl;

    if (!urlToUse) {
        throw new Error('SMS URL이 제공되지 않았습니다.');
    }

    try {
        // SMS 앱 실행
        const link = document.createElement('a');
        link.href = urlToUse;
        link.style.display = 'none';
        document.body.appendChild(link);

        link.click();
        document.body.removeChild(link);

        console.log('서버 제공 SMS URL 실행 완료');

        // 추가 확인
        setTimeout(() => {
            showSmsConfirmation(smsData?.maskedPhoneNumber || '****', smsData);
        }, 1000);

    } catch (error) {
        console.error('서버 제공 SMS URL 실행 실패:', error);
        throw error;
    }
}

/**
 * SMS 앱 실행 (폴백)
 */
async function openSmsApp(phoneNumber, message) {
    console.log('SMS 앱 실행 시도:', {
        phoneNumber: maskPhoneNumber(phoneNumber),
        messageLength: message.length
    });

    // 전화번호 정리 (하이픈 제거)
    const cleanPhoneNumber = phoneNumber.replace(/[^0-9]/g, '');

    // 메시지 URL 인코딩
    const encodedMessage = encodeURIComponent(message);

    // SMS URL 생성
    const smsUrl = `sms:${cleanPhoneNumber}?body=${encodedMessage}`;

    console.log('생성된 SMS URL:', smsUrl.substring(0, 50) + '...');

    try {
        // 직접 링크 실행 시도
        const link = document.createElement('a');
        link.href = smsUrl;
        link.style.display = 'none';
        document.body.appendChild(link);

        link.click();
        document.body.removeChild(link);

        console.log('SMS 앱 실행 완료');

        // 추가 확인
        setTimeout(() => {
            showSmsConfirmation(phoneNumber, { message });
        }, 1000);

    } catch (error) {
        console.error('SMS 앱 실행 실패:', error);
        throw error;
    }
}

/**
 * SMS 내용 생성 (폴백용)
 */
// function createSmsContent(inviteData) {
//     const appName = '토마토리멤버';
//     const inviterName = '초대자'; // 현재 사용자 이름으로 교체 필요
//
//     let message = `[${appName}] 가족 메모리얼 초대\n\n`;
//     message += `${inviterName}님이 메모리얼에 초대했습니다.\n`;
//
//     if (inviteData.message && inviteData.message.trim()) {
//         message += `\n💌 "${inviteData.message}"\n`;
//     }
//
//     message += `\n초대 수락: [링크가 여기에 표시됩니다]\n`;
//     message += `\n⏰ 초대는 7일 후 만료됩니다.`;
//
//     return message;
// }
function createSmsContent(inviteData, token) {
  const appName = '토마토리멤버';
  const inviterName = '초대자'; // 현재 사용자 이름으로 교체 필요

  let message = `[${appName}] 가족 메모리얼 초대\n\n`;
  message += `${inviterName}님이 메모리얼에 초대했습니다.\n`;

  if (inviteData.message && inviteData.message.trim()) {
    message += `\n💌 "${inviteData.message}"\n`;
  }

  // 이메일과 동일한 초대 링크 생성
  if (token) {
    const baseUrl = window.location.origin || 'https://www.tomatoremember.com';
    const inviteLink = `${baseUrl}/mobile/family/invite/${token}`;
    message += `\n🔗 초대 수락하기:\n${inviteLink}\n`;
  } else {
    message += `\n🔗 초대 링크가 포함될 예정입니다.\n`;
  }

  message += `\n⏰ 초대는 7일 후 만료됩니다.`;

  return message;
}
/**
 * SMS 전송 확인 다이얼로그
 */
function showSmsConfirmation(phoneNumber, smsData) {
    const displayNumber = phoneNumber?.includes('*') ? phoneNumber : maskPhoneNumber(phoneNumber);

    const confirmed = confirm(
        `문자 앱이 실행되었습니다.\n\n` +
        `받는 사람: ${displayNumber}\n` +
        `메시지가 올바르게 입력되었는지 확인하고 전송해 주세요.\n\n` +
        `앱이 실행되지 않았다면 '취소'를 클릭하여 다른 방법을 시도하세요.`
    );

    if (!confirmed) {
        showSmsAlternativeOptions({
            contact: phoneNumber,
            message: smsData?.message || '메시지 내용을 확인할 수 없습니다.'
        }, smsData?.token);
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

    showWarning(message);

    // 수동 복사 옵션 제공
    showManualSmsOption(inviteData);
}

/**
 * 수동 SMS 옵션 제공
 */
function showManualSmsOption(inviteData) {
    const message = inviteData.message || createSmsContent(inviteData);

    // 클립보드 복사 시도
    if (navigator.clipboard) {
        navigator.clipboard.writeText(message).then(() => {
            showInfo('메시지가 클립보드에 복사되었습니다. 문자 앱에서 붙여넣기 해주세요.');
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
                    <textarea class="form-control" rows="6" readonly>${escapeHtml(message)}</textarea>
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
 * HTML 이스케이프
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
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
            showSuccess('클립보드에 복사되었습니다.');
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
                showSuccess('클립보드에 복사되었습니다.');
            } else {
                handleError(new Error('클립보드 복사에 실패했습니다.'));
            }
            return result;
        }
    } catch (error) {
        console.error('클립보드 복사 실패:', error);
        handleError(error, { customMessage: '클립보드 복사에 실패했습니다.' });
        return false;
    }
}

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
        handleError(new Error('구성원 정보를 찾을 수 없습니다.'));
        return;
    }

    if (member.relationship === 'SELF') {
        showInfo('소유자는 권한 설정이 불가능합니다.');
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
 * 권한 저장 - 개선된 에러 처리
 */
async function savePermissions() {
    const member = getCurrentMember();
    if (!member) {
        handleError(new Error('구성원 정보를 찾을 수 없습니다.'));
        return;
    }

    if (!pageState.selectedMemorialId) {
        handleError(new Error('선택된 메모리얼이 없습니다.'));
        return;
    }

    const memorialAccess = document.getElementById('memorialAccessSwitch').checked;
    const videoCallAccess = document.getElementById('videoCallSwitch').checked;

    try {
        const btn = document.getElementById('savePermissionBtn');
        const originalText = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 저장 중...';

        const response = await apiPut(
            `/api/family/memorials/${pageState.selectedMemorialId}/members/${member.member.id}/permissions`,
            {
                memorialAccess: memorialAccess,
                videoCallAccess: videoCallAccess
            },
            {
                showLoadingToast: true,
                loadingMessage: '권한을 저장하고 있습니다...',
                showSuccessToast: true,
                successMessage: '권한이 성공적으로 저장되었습니다.',
                errorPrefix: '권한 저장 실패'
            }
        );

        if (response.status?.code === 'OK_0000') {
            const modal = bootstrap.Modal.getInstance(document.getElementById('permissionModal'));
            if (modal) modal.hide();
            setTimeout(() => window.location.reload(), 1000);
        }

    } catch (error) {
        console.error('권한 저장 실패:', error);
        // 에러는 이미 apiPut에서 처리됨
    } finally {
        const btn = document.getElementById('savePermissionBtn');
        btn.disabled = false;
        btn.innerHTML = '권한 저장';
    }
}

/**
 * 구성원 메뉴 모달 열기
 */
function openMemberMenuModal(memberId, realMemberId, memberName) {
    console.log('구성원 메뉴 모달 열기:', { memberId, realMemberId, memberName });

    pageState.currentMemberId = memberId;
    pageState.currentRealMemberId = realMemberId;

    // 모달 정보 설정
    document.getElementById('menuMemberName').textContent = memberName?.substring(0, 1) || '?';
    document.getElementById('menuMemberFullName').textContent = memberName || '알 수 없음';

    // 구성원 정보 찾기
    const member = pageState.familyMembers.find(m => m.id == memberId);
    if (member) {
        document.getElementById('menuMemberRelation').textContent = `고인과의 관계: ${member.relationshipDisplayName || '미설정'}`;
        document.getElementById('menuMemberStatus').textContent = `상태: ${member.inviteStatusDisplayName || '알 수 없음'}`;
    }

    // 모달 열기
    const modal = new bootstrap.Modal(document.getElementById('memberMenuModal'));
    modal.show();
}

/**
 * 초대 링크 복사 - 개선된 에러 처리
 */
async function copyInviteLink() {
    if (!pageState.currentRealMemberId || !pageState.selectedMemorialId) {
        handleError(new Error('구성원 정보가 없습니다.'));
        return;
    }

    try {
        const response = await apiGet(
            `/api/family/memorials/${pageState.selectedMemorialId}/members/${pageState.currentRealMemberId}/invite-link`,
            {
                showLoadingToast: true,
                loadingMessage: '초대 링크를 생성하고 있습니다...',
                errorPrefix: '초대 링크 생성 실패'
            }
        );

        if (response.status?.code === 'OK_0000' && response.data?.inviteLink) {
            await copyToClipboard(response.data.inviteLink);
            const modal = bootstrap.Modal.getInstance(document.getElementById('memberMenuModal'));
            if (modal) modal.hide();
        } else {
            throw new Error(response.status?.message || '초대 링크 생성에 실패했습니다.');
        }
    } catch (error) {
        console.error('초대 링크 복사 실패:', error);
        // 에러는 이미 apiGet에서 처리됨
    }
}

/**
 * 구성원 제거 - 개선된 에러 처리
 */
async function removeMember() {
    const member = pageState.familyMembers.find(m => m.id == pageState.currentMemberId);
    if (!member) {
        handleError(new Error('구성원 정보를 찾을 수 없습니다.'));
        return;
    }

    const memberName = member.member?.name || '알 수 없음';

    const confirmed = confirm(
        `${memberName}님을 가족 구성원에서 제거하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.`
    );

    if (!confirmed) return;

    try {
        const response = await apiDelete(
            `/api/family/memorials/${pageState.selectedMemorialId}/members/${pageState.currentRealMemberId}`,
            {
                showLoadingToast: true,
                loadingMessage: '구성원을 제거하고 있습니다...',
                showSuccessToast: true,
                successMessage: `${memberName}님이 가족 구성원에서 제거되었습니다.`,
                errorPrefix: '구성원 제거 실패'
            }
        );

        if (response.status?.code === 'OK_0000') {
            const modal = bootstrap.Modal.getInstance(document.getElementById('memberMenuModal'));
            if (modal) modal.hide();
            setTimeout(() => window.location.reload(), 1000);
        }
    } catch (error) {
        console.error('구성원 제거 실패:', error);
        // 에러는 이미 apiDelete에서 처리됨
    }
}

/**
 * 이벤트 바인딩
 */
function bindEvents() {
    console.log('이벤트 바인딩 시작');

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

    // 새로고침 버튼
    const refreshBtn = document.getElementById('refreshBtn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => {
            showInfo('새로고침 중...');
            setTimeout(() => window.location.reload(), 500);
        });
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

    // 권한 버튼들
    document.querySelectorAll('.permission-btn').forEach(btn => {
        if (!btn.disabled) {
            btn.addEventListener('click', function() {
                const memberId = this.getAttribute('data-member-id');
                openPermissionModal(memberId);
            });
        }
    });

    // 메뉴 버튼들
    document.querySelectorAll('.menu-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const memberId = this.getAttribute('data-member-id');
            const realMemberId = this.getAttribute('data-real-member-id');
            const memberName = this.getAttribute('data-member-name');
            openMemberMenuModal(memberId, realMemberId, memberName);
        });
    });

    // 구성원 메뉴 액션들
    const copyInviteLinkBtn = document.getElementById('copyInviteLinkBtn');
    if (copyInviteLinkBtn) {
        copyInviteLinkBtn.addEventListener('click', copyInviteLink);
    }

    const removeMemberBtn = document.getElementById('removeMemberBtn');
    if (removeMemberBtn) {
        removeMemberBtn.addEventListener('click', removeMember);
    }

    console.log('이벤트 바인딩 완료');
}

/**
 * 초대 모달 열기
 */
function openInviteModal() {
    if (!pageState.selectedMemorialId) {
        showWarning('선택된 메모리얼이 없습니다.');
        return;
    }

    const modal = new bootstrap.Modal(document.getElementById('inviteModal'));
    modal.show();
}

/**
 * 초대 폼 리셋
 */
function resetInviteForm() {
    const form = document.getElementById('inviteForm');
    if (form) form.reset();

    const emailMethodGroup = document.getElementById('emailMethodGroup');
    const smsMethodGroup = document.getElementById('smsMethodGroup');
    const emailGroup = document.getElementById('emailGroup');
    const phoneGroup = document.getElementById('phoneGroup');

    if (emailMethodGroup) emailMethodGroup.classList.add('active');
    if (smsMethodGroup) smsMethodGroup.classList.remove('active');
    if (emailGroup) emailGroup.style.display = 'block';
    if (phoneGroup) phoneGroup.style.display = 'none';
}

// 전역 함수 등록 (기존 호환성 유지)
window.openPermissionModal = openPermissionModal;
window.savePermissions = savePermissions;
window.copyToClipboard = copyToClipboard;
window.openMemberMenuModal = openMemberMenuModal;

console.log('family-list-enhanced.js 로드 완료');