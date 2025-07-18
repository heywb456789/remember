// family-list-enhanced.js - 향상된 가족 목록 페이지 JavaScript
import { authFetch } from './commonFetch.js';
import { showToast, showConfirm, showLoading, hideLoading } from './common.js';

// 전역 상태
let pageState = {
    selectedMemorialId: null,
    familyMembers: [],
    currentMemberId: null // 현재 선택된 구성원 ID
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
            membersCount: pageState.familyMembers.length,
            members: pageState.familyMembers.map(m => ({
                id: m.id,
                name: m.member?.name,
                relationship: m.relationship,
                permissions: m.permissions,
                inviteStatus: m.inviteStatus
            }))
        });
    } else {
        console.warn('window.serverData가 없습니다.');
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
        showToast('구성원 정보를 찾을 수 없습니다.', 'error');
        return;
    }

    // 소유자는 권한 설정 불가
    if (member.relationship === 'SELF') {
        showToast('소유자는 권한 설정이 불가능합니다.', 'info');
        return;
    }

    // 현재 구성원 ID 저장
    pageState.currentMemberId = memberId;

    // 모달 정보 설정
    document.getElementById('permissionMemberName').textContent = member.member?.name?.substring(0, 1) || '?';
    document.getElementById('permissionMemberFullName').textContent = member.member?.name || '알 수 없음';
    document.getElementById('permissionMemberRelation').textContent = `고인과의 관계: ${member.relationshipDisplayName || '미설정'}`;

    // 현재 권한 상태 설정 (안전하게 처리)
    const memorialAccess = member.permissions?.memorialAccess === true;
    const videoCallAccess = member.permissions?.videoCallAccess === true;

    document.getElementById('memorialAccessSwitch').checked = memorialAccess;
    document.getElementById('videoCallSwitch').checked = videoCallAccess;

    console.log('권한 설정 모달 열기:', {
        memberId: memberId,
        memberName: member.member?.name,
        permissions: member.permissions,
        memorialAccess: memorialAccess,
        videoCallAccess: videoCallAccess
    });

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

        // 새로운 API 엔드포인트 사용
        const response = await authFetch(`/api/family/memorials/${pageState.selectedMemorialId}/members/${member.member.id}/permissions`, {
            method: 'PUT',
            body: JSON.stringify({
                memorialAccess: memorialAccess,
                videoCallAccess: videoCallAccess
            })
        });

        if (response.status?.code === 'OK_0000') {
            showToast('권한이 성공적으로 저장되었습니다.', 'success');

            // 모달 닫기
            bootstrap.Modal.getInstance(document.getElementById('permissionModal')).hide();

            // 페이지 새로고침
            setTimeout(() => {
                window.location.reload();
            }, 1000);
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
 * 구성원 메뉴 모달 열기
 */
function openMemberMenuModal(memberId, memberName) {
    const member = pageState.familyMembers.find(m => m.id == memberId);

    if (!member) {
        showToast('구성원 정보를 찾을 수 없습니다.', 'error');
        return;
    }

    // 소유자는 메뉴 사용 불가
    if (member.relationship === 'SELF') {
        showToast('소유자는 관리 메뉴를 사용할 수 없습니다.', 'info');
        return;
    }

    // 현재 구성원 ID 저장
    pageState.currentMemberId = memberId;

    // 모달 정보 설정
    document.getElementById('menuMemberName').textContent = member.member?.name?.substring(0, 1) || '?';
    document.getElementById('menuMemberFullName').textContent = member.member?.name || '알 수 없음';
    document.getElementById('menuMemberRelation').textContent = `고인과의 관계: ${member.relationshipDisplayName || '미설정'}`;
    document.getElementById('menuMemberStatus').textContent = `상태: ${member.inviteStatusDisplayName || '알 수 없음'}`;

    // 상태에 따른 버튼 활성화/비활성화 (주석 처리된 버튼들은 체크하지 않음)
    // const resendBtn = document.getElementById('resendInviteBtn');
    // const isAccepted = member.inviteStatus === 'ACCEPTED';

    // if (resendBtn) {
    //     resendBtn.disabled = isAccepted;
    //     resendBtn.classList.toggle('disabled', isAccepted);
    // }

    console.log('구성원 메뉴 모달 열기:', {
        memberId: memberId,
        memberName: member.member?.name,
        inviteStatus: member.inviteStatus,
        relationship: member.relationship
    });

    // 모달 열기
    const modal = new bootstrap.Modal(document.getElementById('memberMenuModal'));
    modal.show();
}

/**
 * 초대 재발송
 */
// async function resendInvite() {
//     const member = getCurrentMember();
//     if (!member) {
//         showToast('구성원 정보를 찾을 수 없습니다.', 'error');
//         return;
//     }

//     if (member.inviteStatus === 'ACCEPTED') {
//         showToast('이미 수락된 초대입니다.', 'info');
//         return;
//     }

//     const confirmed = await showConfirm(
//         '초대 재발송',
//         `${member.member?.name || '구성원'}님에게 초대를 다시 보내시겠습니까?`,
//         '재발송',
//         '취소'
//     );

//     if (!confirmed) return;

//     try {
//         const btn = document.getElementById('resendInviteBtn');
//         const originalText = btn.innerHTML;
//         btn.disabled = true;
//         btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 발송 중...';

//         const response = await authFetch(`/api/family/member/${member.id}/resend-invite`, {
//             method: 'POST'
//         });

//         if (response.status?.code === 'OK_0000') {
//             showToast('초대가 재발송되었습니다.', 'success');

//             // 모달 닫기
//             bootstrap.Modal.getInstance(document.getElementById('memberMenuModal')).hide();
//         } else {
//             throw new Error(response.status?.message || '초대 재발송에 실패했습니다.');
//         }
//     } catch (error) {
//         console.error('초대 재발송 실패:', error);
//         showToast('초대 재발송 중 오류가 발생했습니다.', 'error');
//     } finally {
//         const btn = document.getElementById('resendInviteBtn');
//         btn.disabled = false;
//         btn.innerHTML = originalText;
//     }
// }

/**
 * 관계 변경 모달 열기
 */
// function openChangeRelationModal() {
//     const member = getCurrentMember();
//     if (!member) {
//         showToast('구성원 정보를 찾을 수 없습니다.', 'error');
//         return;
//     }

//     // 현재 관계 선택
//     document.getElementById('newRelationship').value = member.relationship || '';

//     // 모달 열기
//     const modal = new bootstrap.Modal(document.getElementById('changeRelationModal'));
//     modal.show();
// }

/**
 * 관계 변경 저장
 */
// async function saveRelationChange() {
//     const member = getCurrentMember();
//     if (!member) {
//         showToast('구성원 정보를 찾을 수 없습니다.', 'error');
//         return;
//     }

//     const newRelationship = document.getElementById('newRelationship').value;

//     if (!newRelationship) {
//         showToast('관계를 선택해주세요.', 'warning');
//         return;
//     }

//     if (newRelationship === member.relationship) {
//         showToast('기존 관계와 동일합니다.', 'info');
//         return;
//     }

//     try {
//         const btn = document.getElementById('saveRelationBtn');
//         btn.disabled = true;
//         btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 저장 중...';

//         const response = await authFetch(`/api/family/member/${member.id}/relationship`, {
//             method: 'PUT',
//             body: JSON.stringify({
//                 relationship: newRelationship
//             })
//         });

//         if (response.status?.code === 'OK_0000') {
//             showToast('관계가 성공적으로 변경되었습니다.', 'success');

//             // 모달 닫기
//             bootstrap.Modal.getInstance(document.getElementById('changeRelationModal')).hide();
//             bootstrap.Modal.getInstance(document.getElementById('memberMenuModal')).hide();

//             // 페이지 새로고침
//             setTimeout(() => {
//                 window.location.reload();
//             }, 1000);
//         } else {
//             throw new Error(response.status?.message || '관계 변경에 실패했습니다.');
//         }
//     } catch (error) {
//         console.error('관계 변경 실패:', error);
//         showToast('관계 변경 중 오류가 발생했습니다.', 'error');
//     } finally {
//         const btn = document.getElementById('saveRelationBtn');
//         btn.disabled = false;
//         btn.innerHTML = '관계 변경';
//     }
// }

/**
 * 초대 링크 복사
 */
async function copyInviteLink() {
    const member = getCurrentMember();
    if (!member) {
        showToast('구성원 정보를 찾을 수 없습니다.', 'error');
        return;
    }

    if (!pageState.selectedMemorialId) {
        showToast('선택된 메모리얼이 없습니다.', 'error');
        return;
    }

    try {
        const btn = document.getElementById('copyInviteLinkBtn');
        const originalText = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 생성 중...';

        // 새로운 API 엔드포인트 사용
        const response = await authFetch(`/api/family/memorials/${pageState.selectedMemorialId}/members/${member.member.id}/invite-link`);

        if (response.status?.code === 'OK_0000') {
            const inviteLink = response.response?.inviteLink;

            if (inviteLink) {
                if (await copyToClipboard(inviteLink)) {
                    showToast('초대 링크가 클립보드에 복사되었습니다.', 'success');
                } else {
                    showToast('클립보드 복사에 실패했습니다.', 'error');
                }
            } else {
                showToast('초대 링크를 가져올 수 없습니다.', 'error');
            }
        } else {
            throw new Error(response.status?.message || '초대 링크 조회에 실패했습니다.');
        }
    } catch (error) {
        console.error('초대 링크 복사 실패:', error);
        showToast('초대 링크 복사 중 오류가 발생했습니다.', 'error');
    } finally {
        const btn = document.getElementById('copyInviteLinkBtn');
        btn.disabled = false;
        btn.innerHTML = originalText;
    }
}


/**
 * 구성원 제거
 */
async function removeMember() {
    const member = getCurrentMember();
    if (!member) {
        showToast('구성원 정보를 찾을 수 없습니다.', 'error');
        return;
    }

    const confirmed = await showConfirm(
        '구성원 제거',
        `${member.member?.name || '구성원'}님을 가족 구성원에서 제거하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.`,
        '제거',
        '취소'
    );

    if (!confirmed) return;

    try {
        const btn = document.getElementById('removeMemberBtn');
        const originalText = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 제거 중...';

        const response = await authFetch(`/api/family/memorials/${pageState.selectedMemorialId}/members/${member.member.id}`, {
            method: 'DELETE'
        });

        if (response.status?.code === 'OK_0000') {
            showToast('구성원이 성공적으로 제거되었습니다.', 'success');

            // 모달 닫기
            bootstrap.Modal.getInstance(document.getElementById('memberMenuModal')).hide();

            // 페이지 새로고침
            setTimeout(() => {
                window.location.reload();
            }, 1000);
        } else {
            throw new Error(response.status?.message || '구성원 제거에 실패했습니다.');
        }
    } catch (error) {
        console.error('구성원 제거 실패:', error);
        showToast('구성원 제거 중 오류가 발생했습니다.', 'error');
    } finally {
        const btn = document.getElementById('removeMemberBtn');
        btn.disabled = false;
        btn.innerHTML = originalText;
    }
}

/**
 * 클립보드 복사
 */
async function copyToClipboard(text) {
    try {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(text);
            return true;
        } else {
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
 * 가족 구성원 목록 새로고침
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

        if (response.status?.code === 'OK_0000') {
            // 페이지 새로고침으로 데이터 업데이트
            window.location.href = `/mobile/family?memorialId=${pageState.selectedMemorialId}`;
        } else {
            throw new Error(response.status?.message || '알 수 없는 오류');
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
                showToast('문자메시지가 발송되었습니다.', 'success');
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
 * 권한 부여 확인 다이얼로그
 */
async function showPermissionGrantConfirm(familyMemberId) {
    const member = pageState.familyMembers.find(m => m.id == familyMemberId);

    if (!member) {
        showToast('구성원 정보를 찾을 수 없습니다.', 'error');
        return;
    }

    const confirmed = await showConfirm(
        '권한 부여',
        `${member.member.name}님에게 메모리얼 접근 권한을 부여하시겠습니까?`,
        '권한 부여',
        '취소'
    );

    if (confirmed) {
        await grantMemberAccess(familyMemberId, true, false);
    }
}

/**
 * 구성원 권한 부여
 */
async function grantMemberAccess(familyMemberId, memorialAccess, videoCallAccess) {
    if (!pageState.selectedMemorialId) {
        showToast('선택된 메모리얼이 없습니다.', 'error');
        return;
    }

    // familyMemberId는 FamilyMember의 ID이므로, 실제 Member ID를 찾아야 함
    const member = pageState.familyMembers.find(m => m.id == familyMemberId);
    if (!member) {
        showToast('구성원 정보를 찾을 수 없습니다.', 'error');
        return;
    }

    try {
        const btn = document.querySelector(`[data-member-id="${familyMemberId}"].permission-btn`);
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 처리 중...';
        }

        // 새로운 API 엔드포인트 사용 - 실제 Member ID 사용
        const response = await authFetch(`/api/family/memorials/${pageState.selectedMemorialId}/members/${member.member.id}/permissions`, {
            method: 'PUT',
            body: JSON.stringify({
                memorialAccess: memorialAccess,
                videoCallAccess: videoCallAccess
            })
        });

        if (response.status?.code === 'OK_0000') {
            showToast('권한이 부여되었습니다.', 'success');

            // 페이지 새로고침
            setTimeout(() => {
                window.location.reload();
            }, 1000);
        } else {
            throw new Error(response.status?.message || '권한 부여에 실패했습니다.');
        }
    } catch (error) {
        console.error('권한 부여 실패:', error);
        showToast('권한 부여 중 오류가 발생했습니다.', 'error');
    } finally {
        const btn = document.querySelector(`[data-member-id="${familyMemberId}"].permission-btn`);
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '권한 없음';
        }
    }
}

/**
 * 구성원 액션 버튼 이벤트 바인딩
 */
function bindMemberActionEvents() {
    // 권한 설정 버튼
    document.querySelectorAll('.permission-btn:not(.owner-permission)').forEach(btn => {
        btn.addEventListener('click', function() {
            const familyMemberId = this.getAttribute('data-member-id'); // FamilyMember ID
            const realMemberId = this.getAttribute('data-real-member-id'); // 실제 Member ID
            const hasAccess = this.classList.contains('granted');

            if (hasAccess) {
                openPermissionModal(familyMemberId);
            } else {
                showPermissionGrantConfirm(familyMemberId);
            }
        });
    });

    // 메뉴 버튼
    document.querySelectorAll('.menu-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const familyMemberId = this.getAttribute('data-member-id'); // FamilyMember ID
            const realMemberId = this.getAttribute('data-real-member-id'); // 실제 Member ID
            const memberName = this.getAttribute('data-member-name');
            openMemberMenuModal(familyMemberId, memberName);
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

    // 권한 저장 버튼
    const savePermissionBtn = document.getElementById('savePermissionBtn');
    if (savePermissionBtn) {
        savePermissionBtn.addEventListener('click', savePermissions);
    }

    // 구성원 메뉴 액션 버튼들
    // const resendInviteBtn = document.getElementById('resendInviteBtn');
    // if (resendInviteBtn) {
    //     resendInviteBtn.addEventListener('click', resendInvite);
    // }

    // const changeRelationBtn = document.getElementById('changeRelationBtn');
    // if (changeRelationBtn) {
    //     changeRelationBtn.addEventListener('click', openChangeRelationModal);
    // }

    const copyInviteLinkBtn = document.getElementById('copyInviteLinkBtn');
    if (copyInviteLinkBtn) {
        copyInviteLinkBtn.addEventListener('click', copyInviteLink);
    }

    const removeMemberBtn = document.getElementById('removeMemberBtn');
    if (removeMemberBtn) {
        removeMemberBtn.addEventListener('click', removeMember);
    }

    // 관계 변경 저장 버튼
    // const saveRelationBtn = document.getElementById('saveRelationBtn');
    // if (saveRelationBtn) {
    //     saveRelationBtn.addEventListener('click', saveRelationChange);
    // }

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
window.openMemberMenuModal = openMemberMenuModal;
window.showPermissionGrantConfirm = showPermissionGrantConfirm;
window.grantMemberAccess = grantMemberAccess;
window.savePermissions = savePermissions;
// window.resendInvite = resendInvite;
// window.openChangeRelationModal = openChangeRelationModal;
// window.saveRelationChange = saveRelationChange;
window.copyInviteLink = copyInviteLink;
window.removeMember = removeMember;
window.copyToClipboard = copyToClipboard;

console.log('family-list-enhanced.js 로드 완료');