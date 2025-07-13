// family-list.js - 가족 목록 페이지 JavaScript (완전 개선 버전)
import { authFetch } from './commonFetch.js';
import { showToast, showConfirm } from './common.js';

// 전역 상태 관리
let familyPageState = {
    // 서버에서 받은 원본 데이터
    serverMemorials: [],
    serverFamilyMembers: [],

    // 현재 선택된 메모리얼
    selectedMemorialId: null,

    // 필터링된 데이터
    filteredFamilyMembers: [],

    // UI 상태
    isInitialized: false,
    currentEditingMemberId: null,
    currentRemovingMemberId: null,
    currentMemberName: null,

    // 통계
    statistics: {
        totalMemorials: 0,
        totalMembers: 0,
        activeMembers: 0
    }
};

/**
 * 페이지 초기화 - SSR 데이터 활용
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 가족 목록 페이지 초기화 시작');

    try {
        // 1. 서버 데이터 로드
        initializeWithServerData();

        // 2. 이벤트 바인딩
        bindEvents();

        // 3. 권한 스위치 연동 설정
        setupPermissionSwitches();

        // 4. 드롭다운 외부 클릭 감지
        setupDropdownOutsideClick();

        // 5. 브라우저 히스토리 네비게이션 설정
        setupHistoryNavigation();

        // 6. 초기 메모리얼 선택
        selectInitialMemorial();

        familyPageState.isInitialized = true;
        console.log('✅ 가족 목록 페이지 초기화 완료');

    } catch (error) {
        console.error('❌ 가족 목록 페이지 초기화 실패:', error);
        showToast('페이지 초기화에 실패했습니다.', 'error');
    }
});

/**
 * 서버 데이터로 초기화 (SSR 데이터 활용)
 */
function initializeWithServerData() {
    console.log('📡 서버 데이터 로드 시작');

    try {
        // window.serverData는 Thymeleaf에서 설정
        if (window.serverData) {
            familyPageState.serverMemorials = window.serverData.memorials || [];
            familyPageState.serverFamilyMembers = window.serverData.familyMembers || [];
            familyPageState.statistics = window.serverData.statistics || {};

            console.log('📊 서버 데이터 로드 완료:', {
                memorials: familyPageState.serverMemorials.length,
                familyMembers: familyPageState.serverFamilyMembers.length,
                statistics: familyPageState.statistics
            });

            // 메모리얼 드롭다운 렌더링
            renderMemorialDropdown();

            // 통계 정보 업데이트
            updateStatistics();

        } else {
            console.warn('⚠️ 서버 데이터가 없습니다. 앱 API 사용으로 폴백');

            // 서버 데이터가 없으면 앱 API 사용 (앱 환경)
            loadDataFromApi();
        }

    } catch (error) {
        console.error('❌ 서버 데이터 로드 실패:', error);
        // 서버 데이터 로드 실패 시 API 폴백
        loadDataFromApi();
    }
}

/**
 * API에서 데이터 로드 (앱 환경 또는 폴백용)
 */
async function loadDataFromApi() {
    console.log('🔄 API에서 데이터 로드 시작');

    try {
        showMemorialLoading(true);

        const response = await authFetch('/api/family/all-data');
        const result = await response.json();

        if (result.status?.code === 'OK_0000' && result.response) {
            familyPageState.serverMemorials = result.response.memorials || [];
            familyPageState.serverFamilyMembers = result.response.familyMembers || [];
            familyPageState.statistics = result.response.statistics || {};

            console.log('📊 API 데이터 로드 완료:', {
                memorials: familyPageState.serverMemorials.length,
                familyMembers: familyPageState.serverFamilyMembers.length
            });

            // 메모리얼 드롭다운 렌더링
            renderMemorialDropdown();

            // 통계 정보 업데이트
            updateStatistics();

        } else {
            throw new Error(result.status?.message || 'API 응답 오류');
        }

    } catch (error) {
        console.error('❌ API 데이터 로드 실패:', error);
        showToast('데이터를 불러올 수 없습니다.', 'error');
        showFamilyError('네트워크 오류가 발생했습니다.');
    } finally {
        showMemorialLoading(false);
    }
}

/**
 * 메모리얼 드롭다운 렌더링
 */
function renderMemorialDropdown() {
    console.log('🎨 메모리얼 드롭다운 렌더링');

    const memorialList = document.getElementById('memorialList');
    const emptyMemorials = document.getElementById('emptyMemorials');

    if (!memorialList) {
        console.warn('⚠️ 메모리얼 리스트 요소를 찾을 수 없습니다.');
        return;
    }

    if (familyPageState.serverMemorials.length === 0) {
        memorialList.innerHTML = '';
        if (emptyMemorials) emptyMemorials.style.display = 'block';
        return;
    }

    if (emptyMemorials) emptyMemorials.style.display = 'none';

    const memorialsHtml = familyPageState.serverMemorials.map(memorial => {
        const familyCount = memorial.familyMemberCount || 0;

        return `
            <button class="memorial-item" onclick="selectMemorial(${memorial.id})" data-memorial-id="${memorial.id}">
                <div class="memorial-avatar">
                    ${memorial.mainProfileImageUrl ?
            `<img src="${memorial.mainProfileImageUrl}" alt="${memorial.nickname}">` :
            memorial.nickname.charAt(0)
        }
                </div>
                <div class="memorial-info">
                    <div class="memorial-name">${memorial.nickname} (${memorial.name})</div>
                    <div class="memorial-relation">접근 가능한 메모리얼</div>
                </div>
                <div class="memorial-stats">
                    <i class="fas fa-users"></i>
                    <span>${familyCount}</span>
                </div>
            </button>
        `;
    }).join('');

    memorialList.innerHTML = memorialsHtml;
    console.log('✅ 메모리얼 드롭다운 렌더링 완료');
}

/**
 * 초기 메모리얼 선택
 */
function selectInitialMemorial() {
    console.log('🎯 초기 메모리얼 선택');

    // 1순위: URL에서 메모리얼 ID 추출
    const urlMemorialId = getMemorialIdFromURL();
    if (urlMemorialId && familyPageState.serverMemorials.find(m => m.id === urlMemorialId)) {
        selectMemorial(urlMemorialId);
        return;
    }

    // 2순위: 서버에서 전달된 현재 메모리얼 ID
    const serverMemorialId = window.currentMemorialId || window.serverData?.selectedMemorial?.id;
    if (serverMemorialId && familyPageState.serverMemorials.find(m => m.id === serverMemorialId)) {
        selectMemorial(serverMemorialId);
        return;
    }

    // 3순위: 첫 번째 사용 가능한 메모리얼
    if (familyPageState.serverMemorials.length > 0) {
        selectMemorial(familyPageState.serverMemorials[0].id);
        return;
    }

    // 메모리얼이 없는 경우
    console.log('ℹ️ 선택 가능한 메모리얼이 없습니다.');
    showEmptyMemorialState();
}

/**
 * 메모리얼 선택 (클라이언트 사이드 필터링)
 */
window.selectMemorial = function(memorialId) {
    console.log('🎯 메모리얼 선택:', memorialId);

    const memorial = familyPageState.serverMemorials.find(m => m.id === memorialId);
    if (!memorial) {
        console.error('❌ 메모리얼을 찾을 수 없음:', memorialId);
        return;
    }

    // 이미 선택된 메모리얼인 경우 중복 처리 방지
    if (familyPageState.selectedMemorialId === memorialId) {
        console.log('ℹ️ 이미 선택된 메모리얼:', memorialId);
        closeMemorialDropdown();
        return;
    }

    // 선택된 메모리얼 업데이트
    familyPageState.selectedMemorialId = memorialId;

    // UI 업데이트
    updateSelectedMemorialUI(memorial);
    updateDropdownSelection(memorialId);
    closeMemorialDropdown();

    // URL 업데이트 (브라우저 히스토리)
    if (!window.isHistoryNavigation) {
        updateURL(memorialId);
    }

    // 가족 구성원 목록 필터링 (서버 데이터 기반)
    filterFamilyMembersByMemorial(memorialId);

    console.log('✅ 메모리얼 선택 완료:', memorial.name);
};

/**
 * 가족 구성원 필터링 (클라이언트 사이드)
 */
function filterFamilyMembersByMemorial(memorialId) {
    console.log('🔍 가족 구성원 필터링:', memorialId);

    // 서버 데이터에서 해당 메모리얼의 가족 구성원만 필터링
    familyPageState.filteredFamilyMembers = familyPageState.serverFamilyMembers.filter(
        member => member.memorial.id === memorialId
    );

    console.log('📊 필터링 결과:', {
        memorialId,
        filteredCount: familyPageState.filteredFamilyMembers.length,
        totalCount: familyPageState.serverFamilyMembers.length
    });

    // UI 업데이트
    updateFamilyMembersList(familyPageState.filteredFamilyMembers);
    updateFamilyStats(familyPageState.filteredFamilyMembers);
}

/**
 * 선택된 메모리얼 UI 업데이트
 */
function updateSelectedMemorialUI(memorial) {
    const avatarElement = document.getElementById('selectedMemorialAvatar');
    const nameElement = document.getElementById('selectedMemorialName');
    const relationElement = document.getElementById('selectedMemorialRelation');

    if (avatarElement) {
        if (memorial.mainProfileImageUrl) {
            avatarElement.innerHTML = `<img src="${memorial.mainProfileImageUrl}" alt="${memorial.nickname}">`;
        } else {
            avatarElement.innerHTML = memorial.nickname.charAt(0);
        }
    }

    if (nameElement) {
        nameElement.textContent = `${memorial.nickname} (${memorial.name})`;
    }

    if (relationElement) {
        relationElement.textContent = '선택된 메모리얼';
    }
}

/**
 * 드롭다운 선택 상태 업데이트
 */
function updateDropdownSelection(memorialId) {
    const items = document.querySelectorAll('.memorial-item');
    items.forEach(item => {
        const itemMemorialId = parseInt(item.getAttribute('data-memorial-id'));
        if (itemMemorialId === memorialId) {
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
    console.log('👥 가족 구성원 목록 업데이트:', members.length);

    const listContainer = document.getElementById('familyMembersList');
    const errorState = document.getElementById('familyErrorState');

    // 에러 상태 숨김
    if (errorState) errorState.style.display = 'none';

    // 목록 컨테이너 표시
    if (listContainer) listContainer.style.display = 'block';

    if (!members || members.length === 0) {
        listContainer.innerHTML = `
            <div class="empty-state fade-in">
                <i class="fas fa-users"></i>
                <h4>등록된 가족 구성원이 없습니다</h4>
                <p>새 가족 구성원을 초대하여<br>함께 소중한 추억을 나누세요</p>
            </div>
        `;
        return;
    }

    const membersHtml = members.map(member => {
        const memberName = member.member?.name || member.memberName || '알 수 없음';
        const relationshipDisplayName = member.relationshipDisplayName || '관계 정보 없음';
        const inviteStatus = member.inviteStatus || 'PENDING';
        const inviteStatusDisplayName = member.inviteStatusDisplayName || '상태 정보 없음';
        const memorialAccess = member.permissions?.memorialAccess || false;
        const lastAccessAt = member.dateTime?.lastAccessAt;

        return `
            <div class="family-member-card fade-in">
                <div class="member-content">
                    <div class="member-info">
                        <div class="member-avatar">${memberName.charAt(0)}</div>
                        <div class="member-details">
                            <div class="member-name">${memberName}</div>
                            <div class="member-relation">고인과의 관계: ${relationshipDisplayName}</div>
                            <div class="member-status status-${inviteStatus.toLowerCase()}">
                                <span>상태: ${inviteStatusDisplayName}</span>
                                ${lastAccessAt ? ` • 마지막 활동: ${formatDate(lastAccessAt)}` : ' • 활동 없음'}
                            </div>
                        </div>
                    </div>
                    <div class="member-actions">
                        <button class="permission-btn ${memorialAccess ? 'granted' : 'denied'}"
                                onclick="openPermissionModal(${member.id})"
                                title="${memorialAccess ? '권한 설정' : '권한 없음'}">
                            ${memorialAccess ? '권한 설정' : '권한 없음'}
                        </button>
                        <button class="menu-btn" onclick="showMemberMenu(${member.id}, '${memberName}')" 
                                aria-label="더보기" title="더보기">
                            <i class="fas fa-ellipsis-v"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');

    listContainer.innerHTML = membersHtml;
    console.log('✅ 가족 구성원 목록 업데이트 완료');
}

/**
 * 가족 구성원 통계 업데이트
 */
function updateFamilyStats(members) {
    const totalCount = document.getElementById('totalMembersCount');
    if (totalCount && members) {
        totalCount.textContent = members.length;
    }
}

/**
 * 전체 통계 정보 업데이트
 */
function updateStatistics() {
    const stats = familyPageState.statistics;

    // 전체 통계 업데이트 (있으면)
    const totalMemorialsElement = document.getElementById('totalMemorialsCount');
    const totalMembersElement = document.getElementById('totalMembersCount');
    const activeMembersElement = document.getElementById('activeMembersCount');

    if (totalMemorialsElement) totalMemorialsElement.textContent = stats.totalMemorials || 0;
    if (totalMembersElement) totalMembersElement.textContent = stats.totalMembers || 0;
    if (activeMembersElement) activeMembersElement.textContent = stats.activeMembers || 0;
}

/**
 * 데이터 새로고침 (API 호출)
 */
window.refreshFamilyMembersList = async function() {
    console.log('🔄 가족 구성원 목록 새로고침');

    if (!familyPageState.selectedMemorialId) {
        console.warn('⚠️ 선택된 메모리얼이 없습니다.');
        return;
    }

    try {
        showFamilyLoading(true);

        // 현재 선택된 메모리얼의 최신 데이터만 가져오기
        const response = await authFetch(`/api/family/memorial/${familyPageState.selectedMemorialId}/members`);
        const result = await response.json();

        if (result.status?.code === 'OK_0000') {
            let members = [];

            if (result.response?.data) {
                // 페이징된 응답
                members = result.response.data;
            } else if (Array.isArray(result.response)) {
                // 배열 응답
                members = result.response;
            }

            // 서버 데이터 업데이트 (해당 메모리얼만)
            familyPageState.serverFamilyMembers = familyPageState.serverFamilyMembers.filter(
                member => member.memorial.id !== familyPageState.selectedMemorialId
            );
            familyPageState.serverFamilyMembers.push(...members);

            // 현재 필터링된 데이터 업데이트
            familyPageState.filteredFamilyMembers = members;

            // UI 업데이트
            updateFamilyMembersList(members);
            updateFamilyStats(members);

            console.log('✅ 가족 구성원 목록 새로고침 완료:', members.length);

        } else {
            throw new Error(result.status?.message || '알 수 없는 오류');
        }

    } catch (error) {
        console.error('❌ 가족 구성원 목록 새로고침 실패:', error);
        showFamilyError('가족 구성원 목록을 불러올 수 없습니다.');
    } finally {
        showFamilyLoading(false);
    }
};

/**
 * 메모리얼 드롭다운 토글
 */
window.toggleMemorialDropdown = function() {
    const dropdown = document.getElementById('memorialDropdownMenu');
    const button = document.getElementById('memorialSelectBtn');

    if (!dropdown || !button) return;

    if (dropdown.classList.contains('show')) {
        closeMemorialDropdown();
    } else {
        openMemorialDropdown();
    }
};

/**
 * 메모리얼 드롭다운 열기
 */
function openMemorialDropdown() {
    const dropdown = document.getElementById('memorialDropdownMenu');
    const button = document.getElementById('memorialSelectBtn');

    if (dropdown) dropdown.classList.add('show');
    if (button) button.classList.add('active');
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
 * URL 관련 함수들
 */
function getMemorialIdFromURL() {
    const pathSegments = window.location.pathname.split('/');
    const lastSegment = pathSegments[pathSegments.length - 1];

    if (lastSegment && !isNaN(lastSegment) && lastSegment !== '') {
        return parseInt(lastSegment);
    }

    return null;
}

function updateURL(memorialId) {
    // 현재는 메모리얼별 URL 구조를 사용하지 않으므로 생략
    // 필요시 구현 가능
}

/**
 * 브라우저 히스토리 이벤트 처리
 */
function setupHistoryNavigation() {
    window.addEventListener('popstate', function(e) {
        console.log('🔙 브라우저 뒤로가기 감지');

        // 필요시 상태 복원 로직 구현
        const urlMemorialId = getMemorialIdFromURL();
        if (urlMemorialId && urlMemorialId !== familyPageState.selectedMemorialId) {
            window.isHistoryNavigation = true;
            selectMemorial(urlMemorialId);
            setTimeout(() => {
                window.isHistoryNavigation = false;
            }, 100);
        }
    });
}

/**
 * 드롭다운 외부 클릭 감지
 */
function setupDropdownOutsideClick() {
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

/**
 * 로딩 및 에러 상태 표시 함수들
 */
function showMemorialLoading(show) {
    const loading = document.getElementById('memorialLoading');
    if (loading) loading.style.display = show ? 'block' : 'none';
}

function showFamilyLoading(show) {
    const loading = document.getElementById('familyLoading');
    const listContainer = document.getElementById('familyMembersList');
    const errorState = document.getElementById('familyErrorState');

    if (loading) loading.style.display = show ? 'block' : 'none';
    if (listContainer && show) listContainer.style.display = 'none';
    if (errorState && show) errorState.style.display = 'none';
}

function showFamilyError(message) {
    const errorState = document.getElementById('familyErrorState');
    const listContainer = document.getElementById('familyMembersList');

    if (errorState) {
        errorState.style.display = 'block';
        const errorText = errorState.querySelector('h4');
        if (errorText) errorText.textContent = message;
    }
    if (listContainer) listContainer.style.display = 'none';

    showToast(message, 'error');
}

function showEmptyMemorialState() {
    const listContainer = document.getElementById('familyMembersList');
    if (listContainer) {
        listContainer.innerHTML = `
            <div class="empty-state fade-in">
                <i class="fas fa-plus-circle"></i>
                <h4>메모리얼이 없습니다</h4>
                <p>새로운 메모리얼을 등록하여<br>가족들과 추억을 나누세요</p>
                <a href="/mobile/memorial/create" class="btn btn-primary btn-sm">새 메모리얼 등록</a>
            </div>
        `;
    }
}

/**
 * 날짜 포맷팅 유틸리티
 */
function formatDate(dateString) {
    if (!dateString) return '활동 없음';

    const date = new Date(dateString);
    const now = new Date();
    const diffInHours = Math.floor((now - date) / (1000 * 60 * 60));

    if (diffInHours < 1) {
        return '방금 전';
    } else if (diffInHours < 24) {
        return `${diffInHours}시간 전`;
    } else if (diffInHours < 24 * 7) {
        const diffInDays = Math.floor(diffInHours / 24);
        return `${diffInDays}일 전`;
    } else {
        return `${date.getMonth() + 1}월 ${date.getDate()}일`;
    }
}

/**
 * 이벤트 바인딩 (공통 액션 API 사용)
 */
function bindEvents() {
    console.log('🔗 이벤트 바인딩');

    // 초대 방법 라디오 버튼 이벤트
    const emailMethod = document.getElementById('emailMethod');
    const smsMethod = document.getElementById('smsMethod');
    const emailGroup = document.getElementById('emailGroup');
    const phoneGroup = document.getElementById('phoneGroup');

    if (emailMethod && smsMethod) {
        emailMethod.addEventListener('change', function() {
            if (this.checked) {
                if (emailGroup) emailGroup.style.display = 'block';
                if (phoneGroup) phoneGroup.style.display = 'none';
                const emailInput = document.getElementById('inviteEmail');
                const phoneInput = document.getElementById('invitePhone');
                if (emailInput) emailInput.required = true;
                if (phoneInput) phoneInput.required = false;
            }
        });

        smsMethod.addEventListener('change', function() {
            if (this.checked) {
                if (emailGroup) emailGroup.style.display = 'none';
                if (phoneGroup) phoneGroup.style.display = 'block';
                const emailInput = document.getElementById('inviteEmail');
                const phoneInput = document.getElementById('invitePhone');
                if (emailInput) emailInput.required = false;
                if (phoneInput) phoneInput.required = true;
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
}

/**
 * 권한 스위치 연동 설정
 */
function setupPermissionSwitches() {
    const memorialSwitch = document.getElementById('memorialAccessSwitch');
    const videoCallSwitch = document.getElementById('videoCallAccessSwitch');

    if (memorialSwitch && videoCallSwitch) {
        memorialSwitch.addEventListener('change', function() {
            if (!this.checked) {
                videoCallSwitch.checked = false;
            }
        });

        videoCallSwitch.addEventListener('change', function() {
            if (this.checked) {
                memorialSwitch.checked = true;
            }
        });
    }
}

// =====  공통 액션 함수들 (기존 로직 유지) =====

/**
 * 초대 모달 열기
 */
window.openInviteModal = function() {
    if (!familyPageState.selectedMemorialId) {
        showToast('메모리얼을 먼저 선택해주세요.', 'warning');
        return;
    }

    const modal = new bootstrap.Modal(document.getElementById('inviteModal'));
    modal.show();
};

/**
 * 초대 보내기 (공통 API 사용)
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

    // 이메일 형식 검증
    if (method === 'email' && email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            showToast('올바른 이메일 주소를 입력해주세요.', 'warning');
            return;
        }
    }

    // 전화번호 형식 검증
    if (method === 'sms' && phone) {
        const phoneRegex = /^010-\d{4}-\d{4}$/;
        if (!phoneRegex.test(phone)) {
            showToast('올바른 전화번호 형식을 입력해주세요. (010-0000-0000)', 'warning');
            return;
        }
    }

    try {
        const btn = event.target;
        const originalText = btn.textContent;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 전송 중...';

        const inviteData = {
            method: method,
            contact: method === 'email' ? email : phone,
            relationship: relationship,
            memorialId: familyPageState.selectedMemorialId
        };

        console.log('📤 초대 전송:', inviteData);

        const response = await authFetch('/api/family/invite', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(inviteData)
        });

        const result = await response.json();

        if (result.status?.code === 'OK_0000') {
            bootstrap.Modal.getInstance(document.getElementById('inviteModal')).hide();
            showInviteSuccessModal(method, method === 'email' ? email : phone);
            resetInviteForm();

            // 3초 후 자동 새로고침
            setTimeout(() => {
                refreshFamilyMembersList();
            }, 3000);

        } else {
            showToast(result.status?.message || '초대 전송에 실패했습니다.', 'error');
        }

    } catch (error) {
        console.error('❌ 초대 전송 실패:', error);
        showToast('네트워크 오류가 발생했습니다. 다시 시도해주세요.', 'error');
    } finally {
        const btn = event.target;
        btn.disabled = false;
        btn.textContent = '초대 보내기';
    }
};

// 나머지 공통 액션 함수들 (기존 로직 유지)
// openPermissionModal, updatePermissions, showMemberMenu, confirmRemoveMember 등...

/**
 * 페이지 가시성 변경 시 데이터 동기화
 */
document.addEventListener('visibilitychange', function() {
    if (!document.hidden && familyPageState.isInitialized) {
        console.log('👁️ 페이지 가시성 복원 - 데이터 동기화');

        // 현재 선택된 메모리얼의 데이터만 새로고침
        if (familyPageState.selectedMemorialId) {
            setTimeout(() => {
                refreshFamilyMembersList();
            }, 1000);
        }
    }
});

console.log('🎉 family-list.js 로드 완료 (완전 개선 버전)');