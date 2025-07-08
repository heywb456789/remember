/**
 * 관리자 관리 페이지 JavaScript
 */
import {adminAuthFetch} from '/js/commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
  'use strict';

  // DOM 요소
  const editAdminModal = document.getElementById('editAdminModal');
  const removeAdminModal = document.getElementById('removeAdminModal');
  const approveRequestModal = document.getElementById('approveRequestModal');
  const rejectRequestModal = document.getElementById('rejectRequestModal');
  const reasonViewModal = document.getElementById('reasonViewModal');

  // let addAdminModalInstance;
  let editAdminModalInstance;
  let removeAdminModalInstance;
  let approveRequestModalInstance;
  let rejectRequestModalInstance;
  let reasonViewModalInstance;

  // 모달 인스턴스 초기화
  if (typeof bootstrap !== 'undefined') {
    // addAdminModalInstance = new bootstrap.Modal(addAdminModal);
    editAdminModalInstance = new bootstrap.Modal(editAdminModal);
    removeAdminModalInstance = new bootstrap.Modal(removeAdminModal);
    approveRequestModalInstance = new bootstrap.Modal(approveRequestModal);
    rejectRequestModalInstance = new bootstrap.Modal(rejectRequestModal);
    reasonViewModalInstance = new bootstrap.Modal(reasonViewModal);
  }

  const btnSaveAdminEdit = document.getElementById('btnSaveAdminEdit');
  const editAdminId = document.getElementById('editAdminId');
  const editAdminName = document.getElementById('editAdminName');
  const editReason = document.getElementById('editReason');

  const btnConfirmRemoveAdmin = document.getElementById(
      'btnConfirmRemoveAdmin');
  const removeAdminId = document.getElementById('removeAdminId');
  const removeAdminName = document.getElementById('removeAdminName');

  const btnConfirmApprove = document.getElementById('btnConfirmApprove');
  const approveRequestId = document.getElementById('approveRequestId');
  const approveUserId = document.getElementById('approveUserId');
  const approveRequestName = document.getElementById('approveRequestName');
  const approveReason = document.getElementById('approveReason');

  const btnConfirmReject = document.getElementById('btnConfirmReject');
  const rejectRequestId = document.getElementById('rejectRequestId');
  const rejectUserId = document.getElementById('rejectUserId');
  const rejectRequestName = document.getElementById('rejectRequestName');
  const rejectReason = document.getElementById('rejectReason');

  const reasonContent = document.getElementById('reasonContent');

  // 초기화
  initPage();

  /**
   * 페이지 초기화 함수
   */
  function initPage() {
    // 이벤트 리스너 등록
    initEventListeners();

    // 툴팁 초기화
    initTooltips();

    // 탭 활성화 상태 유지
    initTabsState();
  }

  /**
   * 툴팁 초기화
   */
  function initTooltips() {
    const tooltipTriggerList = [].slice.call(
        document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    if (typeof bootstrap !== 'undefined') {
      tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
      });
    }
  }

  /**
   * 탭 활성화 상태 유지
   */
  function initTabsState() {
    // URL의 해시값 확인
    const hash = window.location.hash;
    if (hash) {
      // 해당 탭 활성화
      const tabId = hash.substring(1);
      const tabEl = document.querySelector(`a[href="#${tabId}"]`);
      if (tabEl && typeof bootstrap !== 'undefined') {
        const tab = new bootstrap.Tab(tabEl);
        tab.show();
      }
    }

    // 탭 변경 시 URL 해시 업데이트
    document.querySelectorAll('a[data-bs-toggle="tab"]').forEach(tab => {
      tab.addEventListener('shown.bs.tab', function (e) {
        const id = e.target.getAttribute('href').substring(1);
        window.location.hash = id;
      });
    });
  }

  /**
   * 이벤트 리스너 초기화
   */
  function initEventListeners() {

    // 관리자 편집 버튼 클릭 이벤트
    document.addEventListener('click', function (e) {
      const editBtn = e.target.closest('.btn-edit-admin');
      if (editBtn) {
        const adminId = editBtn.dataset.adminId;
        const adminName = editBtn.dataset.adminName;
        const adminRoles = editBtn.dataset.adminRoles
            ? editBtn.dataset.adminRoles.split(',') : [];
        showEditAdminModal(adminId, adminName, adminRoles);
      }
    });

    // 관리자 제거 버튼 클릭 이벤트
    document.addEventListener('click', function (e) {
      const removeBtn = e.target.closest('.btn-remove-admin');
      if (removeBtn) {
        const adminId = removeBtn.dataset.adminId;
        const adminName = removeBtn.dataset.adminName;
        showRemoveAdminModal(adminId, adminName);
      }
    });

    // 요청 승인 버튼 클릭 이벤트
    document.addEventListener('click', function (e) {
      const approveBtn = e.target.closest('.btn-approve-request');
      if (approveBtn) {
        const requestId = approveBtn.dataset.requestId;
        const userId = approveBtn.dataset.userId;
        const userName = approveBtn.dataset.userName;
        showApproveRequestModal(requestId, userId, userName);
      }
    });

    // 요청 거부 버튼 클릭 이벤트
    document.addEventListener('click', function (e) {
      const rejectBtn = e.target.closest('.btn-reject-request');
      if (rejectBtn) {
        const requestId = rejectBtn.dataset.requestId;
        const userId = rejectBtn.dataset.userId;
        const userName = rejectBtn.dataset.userName;
        showRejectRequestModal(requestId, userId, userName);
      }
    });

    // 요청 사유 보기 버튼 클릭 이벤트
    document.addEventListener('click', function (e) {
      const viewReasonBtn = e.target.closest('.view-reason-btn');
      if (viewReasonBtn) {
        const reason = viewReasonBtn.getAttribute('title') || '사유가 없습니다.';
        showReasonViewModal(reason);
      }
    });

    // 관리자 편집 저장 버튼 클릭 이벤트
    if (btnSaveAdminEdit) {
      btnSaveAdminEdit.addEventListener('click', saveAdminEdit);
    }

    // 관리자 제거 확인 버튼 클릭 이벤트
    if (btnConfirmRemoveAdmin) {
      btnConfirmRemoveAdmin.addEventListener('click', confirmRemoveAdmin);
    }

    // 요청 승인 확인 버튼 클릭 이벤트
    if (btnConfirmApprove) {
      btnConfirmApprove.addEventListener('click', confirmApproveRequest);
    }

    // 요청 거부 확인 버튼 클릭 이벤트
    if (btnConfirmReject) {
      btnConfirmReject.addEventListener('click', confirmRejectRequest);
    }

    // 블록 버튼 이벤트 리스너 등록
    document.addEventListener('click', function (e) {
      const blockBtn = e.target.closest('.btn-block-admin');
      if (blockBtn) {
        const adminId = blockBtn.dataset.adminId;
        const adminName = blockBtn.dataset.adminName;
        const isBlocked = blockBtn.dataset.isBlocked === 'true';

        if (confirm(
            `${adminName} ${isBlocked ? '관리자의 차단을 해제' : '관리자를 차단'}하시겠습니까?`)) {
          toggleAdminBlock(adminId, !isBlocked);
        }
      }
    });

  }

  /**
   * 관리자 편집 모달 표시
   * @param {string} adminId - 관리자 ID
   * @param {string} adminName - 관리자 이름
   * @param {Array} adminRoles - 관리자 권한 목록
   */
  // adminList.js 파일의 수정 부분
// showEditAdminModal 함수 내부 - 기존 체크박스 초기화 및 설정 코드 수정
  function showEditAdminModal(adminId, adminName, adminRoles) {
    // 모달 정보 설정
    editAdminId.value = adminId;
    editAdminName.textContent = adminName;
    editReason.value = '';

    // 권한 라디오버튼 설정 - 문자열인 경우 배열로 변환
    const roles = Array.isArray(adminRoles) ? adminRoles : [adminRoles];

    // 모든 라디오버튼 초기화
    document.querySelectorAll('input[name="editRoles"]').forEach(radio => {
      radio.checked = false;
    });

    // 첫 번째 권한에 맞는 라디오버튼 체크 (여러 권한이 있더라도 첫 번째 것만 선택)
    if (roles.length > 0) {
      const roleRadio = document.querySelector(
          `input[name="editRoles"][value="${roles[0]}"]`);
      if (roleRadio) {
        roleRadio.checked = true;
      }
    }

    // 모달 표시
    if (editAdminModalInstance) {
      editAdminModalInstance.show();
    } else if (typeof bootstrap !== 'undefined') {
      const modalInstance = new bootstrap.Modal(editAdminModal);
      modalInstance.show();
    }
  }

  /**
   * 관리자 제거 모달 표시
   * @param {string} adminId - 관리자 ID
   * @param {string} adminName - 관리자 이름
   */
  function showRemoveAdminModal(adminId, adminName) {
    // 모달 정보 설정
    removeAdminId.value = adminId;
    removeAdminName.textContent = adminName;

    // 모달 표시
    if (removeAdminModalInstance) {
      removeAdminModalInstance.show();
    } else if (typeof bootstrap !== 'undefined') {
      const modalInstance = new bootstrap.Modal(removeAdminModal);
      modalInstance.show();
    }
  }

  /**
   * 요청 승인 모달 표시
   * @param {string} requestId - 요청 ID
   * @param {string} userId - 사용자 ID
   * @param {string} userName - 사용자 이름
   */
  function showApproveRequestModal(requestId, userId, userName) {
    // 모달 정보 설정
    approveRequestId.value = requestId;
    approveUserId.value = userId;
    approveRequestName.textContent = userName;
    approveReason.value = '';

    // 권한 라디오버튼 초기화
    document.querySelectorAll('input[name="approveRoles"]').forEach(radio => {
      // 기본적으로 OPERATOR 권한만 체크
      radio.checked = radio.value === 'OPERATOR';
    });

    // 모달 표시
    if (approveRequestModalInstance) {
      approveRequestModalInstance.show();
    } else if (typeof bootstrap !== 'undefined') {
      const modalInstance = new bootstrap.Modal(approveRequestModal);
      modalInstance.show();
    }
  }

  /**
   * 요청 거부 모달 표시
   * @param {string} requestId - 요청 ID
   * @param {string} userId - 사용자 ID
   * @param {string} userName - 사용자 이름
   */
  function showRejectRequestModal(requestId, userId, userName) {
    // 모달 정보 설정
    rejectRequestId.value = requestId;
    rejectUserId.value = userId;
    rejectRequestName.textContent = userName;
    rejectReason.value = '';

    // 모달 표시
    if (rejectRequestModalInstance) {
      rejectRequestModalInstance.show();
    } else if (typeof bootstrap !== 'undefined') {
      const modalInstance = new bootstrap.Modal(rejectRequestModal);
      modalInstance.show();
    }
  }

  /**
   * 요청 사유 모달 표시
   * @param {string} reason - 요청 사유
   */
  function showReasonViewModal(reason) {
    // 모달 내용 설정
    reasonContent.textContent = reason;

    // 모달 표시
    if (reasonViewModalInstance) {
      reasonViewModalInstance.show();
    } else if (typeof bootstrap !== 'undefined') {
      const modalInstance = new bootstrap.Modal(reasonViewModal);
      modalInstance.show();
    }
  }

  /**
   * 관리자 편집 저장
   */
  async function saveAdminEdit() {
    const adminId = editAdminId.value;

    if (!adminId) {
      showAlert('관리자 ID를 찾을 수 없습니다.', 'warning');
      return;
    }

    // 선택된 권한 수집 - 라디오 버튼은 하나만 선택됨
    const selectedRole = document.querySelector(
        'input[name="editRoles"]:checked');
    if (!selectedRole) {
      showAlert('권한을 선택하세요.', 'warning');
      return;
    }

    const role = selectedRole.value;

    // 권한 변경 사유
    const reason = editReason.value.trim();

    // 버튼 비활성화 및 로딩 표시
    btnSaveAdminEdit.disabled = true;
    btnSaveAdminEdit.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 저장 중...';

    try {
      const response = await adminAuthFetch(`/admin/users/admin/${adminId}/role`,
          {
            method: 'PUT',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({
              role: role,
              reason: reason || null
            })
          });

      if (!response.ok) {
        throw new Error('서버 응답 오류: ' + response.status);
      }

      const result = await response.json();

      if (result.status.code === 'OK_0000') {
        // 관리자 편집 성공
        if (editAdminModalInstance) {
          editAdminModalInstance.hide();
        } else if (typeof bootstrap !== 'undefined') {
          const modalInstance = new bootstrap.Modal(editAdminModal);
          modalInstance.hide();
        }

        showAlert('관리자 권한이 성공적으로 변경되었습니다.', 'success');

        // 페이지 새로고침
        setTimeout(() => {
          window.location.reload();
        }, 1000);
      } else {
        throw new Error(result.message || '관리자 권한 변경 중 오류가 발생했습니다.');
      }
    } catch (error) {
      console.error('관리자 권한 변경 오류:', error);
      showAlert('관리자 권한 변경 중 오류가 발생했습니다: ' + error.message, 'danger');
    } finally {
      // 버튼 상태 복원
      btnSaveAdminEdit.innerHTML = '저장';
      btnSaveAdminEdit.disabled = false;
    }
  }

  /**
   * 관리자 제거 확인
   */
  async function confirmRemoveAdmin() {
    const adminId = removeAdminId.value;

    if (!adminId) {
      showAlert('관리자 ID를 찾을 수 없습니다.', 'warning');
      return;
    }

    // 버튼 비활성화 및 로딩 표시
    btnConfirmRemoveAdmin.disabled = true;
    btnConfirmRemoveAdmin.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 제거 중...';

    try {
      const response = await adminAuthFetch(
          `/admin/api/admin/${adminId}/remove`, {
            method: 'POST'
          });

      if (!response.ok) {
        throw new Error('서버 응답 오류: ' + response.status);
      }

      const result = await response.json();

      if (result.success) {
        // 관리자 제거 성공
        if (removeAdminModalInstance) {
          removeAdminModalInstance.hide();
        } else if (typeof bootstrap !== 'undefined') {
          const modalInstance = new bootstrap.Modal(removeAdminModal);
          modalInstance.hide();
        }

        showAlert('관리자가 성공적으로 제거되었습니다.', 'success');

        // 페이지 새로고침
        setTimeout(() => {
          window.location.reload();
        }, 1000);
      } else {
        throw new Error(result.message || '관리자 제거 중 오류가 발생했습니다.');
      }
    } catch (error) {
      console.error('관리자 제거 오류:', error);
      showAlert('관리자 제거 중 오류가 발생했습니다: ' + error.message, 'danger');
    } finally {
      // 버튼 상태 복원
      btnConfirmRemoveAdmin.innerHTML = '제거';
      btnConfirmRemoveAdmin.disabled = false;
    }
  }

  /**
   * 요청 승인 확인
   */
  async function confirmApproveRequest() {
    const requestId = approveRequestId.value;
    const userId = approveUserId.value;

    if (!requestId || !userId) {
      showAlert('요청 정보를 찾을 수 없습니다.', 'warning');
      return;
    }

    // 선택된 권한 수집 - 라디오 버튼은 하나만 선택됨
    const selectedRole = document.querySelector(
        'input[name="approveRoles"]:checked');
    if (!selectedRole) {
      showAlert('권한을 선택하세요.', 'warning');
      return;
    }

    const role = selectedRole.value;

    // 승인 사유
    const reason = approveReason.value.trim();

    // 버튼 비활성화 및 로딩 표시
    btnConfirmApprove.disabled = true;
    btnConfirmApprove.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 승인 중...';

    try {
      const response = await adminAuthFetch(
          `/admin/users/admin/${requestId}/approve`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({
              adminId: userId,
              role: role,  // 배열 대신 단일 값으로 변경
              reason: reason || null
            })
          });

      if (!response.ok) {
        throw new Error('서버 응답 오류: ' + response.status);
      }

      const result = await response.json();

      if (result.status.code === 'OK_0000') {
        // 요청 승인 성공
        if (approveRequestModalInstance) {
          approveRequestModalInstance.hide();
        } else if (typeof bootstrap !== 'undefined') {
          const modalInstance = new bootstrap.Modal(approveRequestModal);
          modalInstance.hide();
        }

        showAlert('관리자 등록 요청이 성공적으로 승인되었습니다.', 'success');

        // 페이지 새로고침
        setTimeout(() => {
          window.location.reload();
        }, 1000);
      } else {
        throw new Error(result.message || '요청 승인 중 오류가 발생했습니다.');
      }
    } catch (error) {
      console.error('요청 승인 오류:', error);
      showAlert('요청 승인 중 오류가 발생했습니다: ' + error.message, 'danger');
    } finally {
      // 버튼 상태 복원
      btnConfirmApprove.innerHTML = '승인';
      btnConfirmApprove.disabled = false;
    }
  }

  /**
   * 요청 거부 확인
   */
  async function confirmRejectRequest() {
    const requestId = rejectRequestId.value;

    if (!requestId) {
      showAlert('요청 정보를 찾을 수 없습니다.', 'warning');
      return;
    }

    const reason = rejectReason.value.trim();

    // 버튼 비활성화 및 로딩 표시
    btnConfirmReject.disabled = true;
    btnConfirmReject.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 거부 중...';

    try {
      const response = await adminAuthFetch(
          `/admin/api/admin/requests/${requestId}/reject`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({
              reason: reason || null
            })
          });

      if (!response.ok) {
        throw new Error('서버 응답 오류: ' + response.status);
      }

      const result = await response.json();

      if (result.success) {
        // 요청 거부 성공
        if (rejectRequestModalInstance) {
          rejectRequestModalInstance.hide();
        } else if (typeof bootstrap !== 'undefined') {
          const modalInstance = new bootstrap.Modal(rejectRequestModal);
          modalInstance.hide();
        }

        showAlert('관리자 등록 요청이 거부되었습니다.', 'success');

        // 페이지 새로고침
        setTimeout(() => {
          window.location.reload();
        }, 1000);
      } else {
        throw new Error(result.message || '요청 거부 중 오류가 발생했습니다.');
      }
    } catch (error) {
      console.error('요청 거부 오류:', error);
      showAlert('요청 거부 중 오류가 발생했습니다: ' + error.message, 'danger');
    } finally {
      // 버튼 상태 복원
      btnConfirmReject.innerHTML = '거부';
      btnConfirmReject.disabled = false;
    }
  }

  /**
   * 관리자 차단/차단해제 처리
   * @param {string} adminId - 관리자 ID
   * @param {boolean} blocked - 차단 여부 (true: 차단, false: 차단해제)
   */
  async function toggleAdminBlock(adminId, blocked) {
    if (!adminId) {
      showAlert('관리자 ID를 찾을 수 없습니다.', 'warning');
      return;
    }

    try {
      const response = await adminAuthFetch(
          `/admin/users/admin/${adminId}/status`, {
            method: 'PUT',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify({
              status: blocked ? 'BLOCKED' : 'ACTIVE'
            })
          });

      if (!response.ok) {
        throw new Error('서버 응답 오류: ' + response.status);
      }

      const result = await response.json();

      if (result.status.code ==='OK_0000') {
        showAlert(`관리자가 성공적으로 ${blocked ? '비활성화' : '활성화'}되었습니다.`, 'success');

        // 페이지 새로고침
        setTimeout(() => {
          window.location.reload();
        }, 1000);
      } else {
        throw new Error(
            result.message || `관리자 ${blocked ? '비활성화' : '활성화'} 중 오류가 발생했습니다.`);
      }
    } catch (error) {
      console.error(`관리자 ${blocked ? '비활성화' : '활성화'} 오류:`, error);
      showAlert(
          `관리자 ${blocked ? '비활성화' : '활성화'} 중 오류가 발생했습니다: ` + error.message,
          'danger');
    }
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

    // Toastr 사용
    if (window.toastr) {
      window.toastr[type](message);
      return;
    }

    // SweetAlert2 사용
    if (window.Swal) {
      window.Swal.fire({
        title: type.charAt(0).toUpperCase() + type.slice(1),
        text: message,
        icon: type,
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000
      });
      return;
    }

    // 기본 alert 사용
    alert(message);
  }
});