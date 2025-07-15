// family-list-simple.js - 가족 목록 페이지 JavaScript (간단 버전)
import { authFetch } from './commonFetch.js';
import { showToast, showConfirm } from './common.js';

// 전역 상태
let pageState = {
    currentMemorialId: null,
    allMembers: [], // 전체 가족 구성원
    filteredMembers: [] // 현재 선택된 메모리얼의 구성원
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

        // 초기에는 전체 선택 상태로 시작
        pageState.currentMemorialId = 'all';
        pageState.filteredMembers = pageState.allMembers;

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
        pageState.allMembers = window.serverData.familyMembers || [];
        pageState.currentMemorialId = window.currentMemorialId;

        console.log('서버 데이터 로드 완료 - 전체 구성원:', pageState.allMembers.length);
    }
}

/**
 * 메모리얼 선택
 */
window.selectMemorial = function(memorialId) {
    console.log('메모리얼 선택:', memorialId);

    pageState.currentMemorialId = memorialId;

    if (memorialId === 'all') {
        // 전체 선택 - 모든 구성원 표시
        pageState.filteredMembers = pageState.allMembers;
        updateSelectedMemorialUI('all');
    } else {
        // 특정 메모리얼 선택 - 해당 메모리얼의 구성원만 필터링
        pageState.filteredMembers = pageState.allMembers.filter(
            member => member.memorial.id === memorialId
        );
        updateSelectedMemorialUI(memorialId);
    }

    // UI 업데이트
    updateFamilyMembersList(pageState.filteredMembers);
    closeMemorialDropdown();

    console.log('메모리얼 선택 완료 - 필터링된 구성원:', pageState.filteredMembers.length);
};

/**
 * 선택된 메모리얼 UI 업데이트
 */
function updateSelectedMemorialUI(memorialId) {
    const avatarElement = document.getElementById('selectedMemorialAvatar');
    const nameElement = document.getElementById('selectedMemorialName');
    const relationElement = document.getElementById('selectedMemorialRelation');

    if (memorialId === 'all') {
        // 전체 선택
        if (avatarElement) {
            avatarElement.innerHTML = '<i class="fas fa-users"></i>';
        }
        if (nameElement) {
            nameElement.textContent = '전체';
        }
        if (relationElement) {
            relationElement.textContent = '모든 메모리얼의 가족 구성원';
        }
    } else {
        // 특정 메모리얼 선택
        const memorial = window.serverData.memorials.find(m => m.id === memorialId);
        if (!memorial) return;

        if (avatarElement) {
            if (memorial.mainProfileImageUrl) {
                avatarElement.innerHTML = `<img src="${memorial.mainProfileImageUrl}" alt="${memorial.nickname}">`;
            } else {
                avatarElement.innerHTML = `<span>${memorial.nickname.charAt(0)}</span>`;
            }
        }

        if (nameElement) {
            nameElement.textContent = `${memorial.nickname} (${memorial.name})`;
        }

        if (relationElement) {
            relationElement.textContent = '선택된 메모리얼';
        }
    }

    // 드롭다운 선택 상태 업데이트
    document.querySelectorAll('.memorial-item').forEach(item => {
        const itemMemorialId = item.getAttribute('data-memorial-id');
        if (itemMemorialId === String(memorialId)) {
            item.classList.add('selected');
        } else {
            item.classList.remove('selected');
        }
    });
}

/**
 * 가족 구성원 목록 업데이트
 */
function updateFamilyMembersList(members) {
    const listContainer = document.getElementById('familyMembersList');
    const totalCountElement = document.getElementById('totalMembersCount');

    if (totalCountElement) {
        totalCountElement.textContent = members.length;
    }

    if (!members || members.length === 0) {
        listContainer.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-user-plus"></i>
                <h4>등록된 가족 구성원이 없습니다</h4>
                <p>새 가족 구성원을 초대하여<br>함께 소중한 추억을 나누세요</p>
            </div>
        `;
        return;
    }

    const membersHtml = members.map(member => {
        const memberName = member.member?.name || '알 수 없음';
        const isOwner = member.relationship === 'SELF';
        const memorialAccess = member.permissions?.memorialAccess || false;

        return `
            <div class="family-member-card ${isOwner ? 'owner-card' : ''}">
                <div class="member-content">
                    <div class="member-info">
                        <div class="member-avatar ${isOwner ? 'owner-avatar' : ''}">
                            <span>${memberName.charAt(0)}</span>
                        </div>
                        <div class="member-details">
                            <div class="member-name">
                                <span>${memberName}</span>
                                ${isOwner ? '<span class="owner-badge"><i class="fas fa-crown"></i> 소유자</span>' : ''}
                            </div>
                            <div class="member-relation">
                                메모리얼: ${member.memorial?.nickname || '알 수 없음'} • 고인과의 관계: ${member.relationshipDisplayName || '미설정'}
                            </div>
                            <div class="member-status ${isOwner ? 'status-owner' : 'status-' + (member.inviteStatus || 'pending').toLowerCase()}">
                                <span>상태: ${member.inviteStatusDisplayName || '알 수 없음'}</span>
                            </div>
                        </div>
                    </div>
                    <div class="member-actions">
                        ${isOwner ? `
                            <button class="permission-btn owner-permission" disabled>
                                모든 권한
                            </button>
                        ` : `
                            <button class="permission-btn ${memorialAccess ? 'granted' : 'denied'}"
                                    onclick="openPermissionModal(${member.id})">
                                ${memorialAccess ? '권한 설정' : '권한 없음'}
                            </button>
                            <button class="menu-btn" onclick="showMemberMenu(${member.id}, '${memberName}')" 
                                    aria-label="더보기">
                                <i class="fas fa-ellipsis-v"></i>
                            </button>
                        `}
                    </div>
                </div>
            </div>
        `;
    }).join('');

    listContainer.innerHTML = membersHtml;
}

/**
 * 가족 구성원 목록 새로고침 (API 호출)
 */
window.refreshFamilyMembersList = async function() {
    if (!pageState.currentMemorialId || pageState.currentMemorialId === 'all') {
        showToast('특정 메모리얼을 선택한 후 새로고침해주세요.', 'warning');
        return;
    }

    try {
        const response = await authFetch(`/api/family/memorial/${pageState.currentMemorialId}/members`);
        const result = await response.json();

        if (result.status?.code === 'OK_0000') {
            const newMembers = result.response || [];

            // 서버 데이터 업데이트 (해당 메모리얼만)
            pageState.allMembers = pageState.allMembers.filter(
                member => member.memorial.id !== pageState.currentMemorialId
            );
            pageState.allMembers.push(...newMembers);

            // 현재 필터링된 데이터 업데이트
            pageState.filteredMembers = newMembers;

            // UI 업데이트
            updateFamilyMembersList(pageState.filteredMembers);

            showToast('가족 구성원 목록이 새로고침되었습니다.', 'success');
        } else {
            throw new Error(result.status?.message || '알 수 없는 오류');
        }
    } catch (error) {
        console.error('가족 구성원 목록 새로고침 실패:', error);
        showToast('가족 구성원 목록을 불러올 수 없습니다.', 'error');
    }
};

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

// 전역 함수로 등록
window.toggleMemorialDropdown = toggleMemorialDropdown;

/**
 * 초대 모달 열기
 */
window.openInviteModal = function() {
    if (!pageState.currentMemorialId || pageState.currentMemorialId === 'all') {
        showToast('특정 메모리얼을 선택한 후 초대해주세요.', 'warning');
        return;
    }

    const modal = new bootstrap.Modal(document.getElementById('inviteModal'));
    modal.show();
};

/**
 * 초대 보내기
 */
window.sendInvite = async function() {
    const method = document.querySelector('input[name="inviteMethod"]:checked')?.value;
    const email = document.getElementById('inviteEmail')?.value;
    const phone = document.getElementById('invitePhone')?.value;
    const relationship = document.getElementById('inviteRelationship')?.value;

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

    try {
        const btn = event.target;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 전송 중...';

        const inviteData = {
            method: method,
            contact: method === 'email' ? email : phone,
            relationship: relationship,
            memorialId: pageState.currentMemorialId
        };

        const response = await authFetch('/api/family/invite', {
            method: 'POST',
            body: JSON.stringify(inviteData)
        });

        const result = await response.json();

        if (result.status?.code === 'OK_0000') {
            bootstrap.Modal.getInstance(document.getElementById('inviteModal')).hide();
            showToast('초대 링크가 전송되었습니다.', 'success');

            // 폼 리셋
            document.getElementById('inviteEmail').value = '';
            document.getElementById('invitePhone').value = '';
            document.getElementById('inviteRelationship').value = '';

            // 3초 후 새로고침
            setTimeout(() => {
                refreshFamilyMembersList();
            }, 3000);
        } else {
            showToast(result.status?.message || '초대 전송에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('초대 전송 실패:', error);
        showToast('네트워크 오류가 발생했습니다.', 'error');
    } finally {
        const btn = event.target;
        btn.disabled = false;
        btn.textContent = '초대 보내기';
    }
};

/**
 * 권한 설정 모달 (임시)
 */
window.openPermissionModal = function(memberId) {
    showToast('권한 설정 기능은 준비 중입니다.', 'info');
};

/**
 * 구성원 메뉴 (임시)
 */
window.showMemberMenu = function(memberId, memberName) {
    showToast(`${memberName}님의 메뉴 기능은 준비 중입니다.`, 'info');
};

/**
 * 이벤트 바인딩
 */
function bindEvents() {
    // 초대 방법 라디오 버튼
    const emailMethod = document.getElementById('emailMethod');
    const smsMethod = document.getElementById('smsMethod');
    const emailGroup = document.getElementById('emailGroup');
    const phoneGroup = document.getElementById('phoneGroup');

    if (emailMethod && smsMethod) {
        emailMethod.addEventListener('change', function() {
            if (this.checked) {
                if (emailGroup) emailGroup.style.display = 'block';
                if (phoneGroup) phoneGroup.style.display = 'none';
            }
        });

        smsMethod.addEventListener('change', function() {
            if (this.checked) {
                if (emailGroup) emailGroup.style.display = 'none';
                if (phoneGroup) phoneGroup.style.display = 'block';
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
}

console.log('family-list-simple.js 로드 완료');