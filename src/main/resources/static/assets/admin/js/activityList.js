/**
 * 활동내역 심사 페이지 JavaScript
 */
import {adminAuthFetch} from '/js/commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    // DOM 요소
    const searchForm = document.getElementById('searchForm');
    const dateRangeInput = document.getElementById('dateRange');
    const selectAllCheckbox = document.getElementById('selectAll');
    const btnBulkApprove = document.getElementById('btnBulkApprove');
    const btnBulkReject = document.getElementById('btnBulkReject');
    const btnConfirmBulkApprove = document.getElementById('btnConfirmBulkApprove');
    const btnConfirmBulkReject = document.getElementById('btnConfirmBulkReject');
    const bulkRejectReason = document.getElementById('bulkRejectReason');
    const otherRejectReason = document.getElementById('otherRejectReason');
    const otherReasonContainer = document.getElementById('otherReasonContainer');

    // 모달 요소
    const activityPreviewModal = new bootstrap.Modal(document.getElementById('activityPreviewModal'));
    const bulkApproveModal = new bootstrap.Modal(document.getElementById('bulkApproveModal'));
    const bulkRejectModal = new bootstrap.Modal(document.getElementById('bulkRejectModal'));
    const singleApproveModal = new bootstrap.Modal(document.getElementById('singleApproveModal'));

    // 전역 변수
    let currentActivityId = null;

    // 초기화
    initPage();

    /**
     * 페이지 초기화 함수
     */
    function initPage() {
        // 이벤트 리스너 등록
        initEventListeners();

        // Flatpickr 달력 초기화
        initFlatpickrDateRange();

        // 툴팁 초기화
        initTooltips();
    }

    /**
     * 툴팁 초기화
     */
    function initTooltips() {
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }

    /**
     * 이벤트 리스너 초기화
     */
    function initEventListeners() {
        // 검색 폼 제출 이벤트
        if (searchForm) {
            searchForm.addEventListener('submit', function(e) {
                e.preventDefault();
                performSearch();
            });
        }

        // 전체 선택 체크박스 이벤트
        if (selectAllCheckbox) {
            selectAllCheckbox.addEventListener('change', function() {
                const checkboxes = document.querySelectorAll('.activity-select:not(:disabled)');
                checkboxes.forEach(checkbox => {
                    checkbox.checked = selectAllCheckbox.checked;
                });
                updateBulkActionButtons();
            });
        }

        // 개별 체크박스 이벤트
        document.addEventListener('change', function(e) {
            if (e.target.classList.contains('activity-select')) {
                updateSelectAllCheckbox();
                updateBulkActionButtons();
            }
        });

        // 미리보기 버튼 이벤트
        document.addEventListener('click', function(e) {
            const viewBtn = e.target.closest('.btn-view');
            if (viewBtn) {
                e.preventDefault();
                showPreviewModal(
                    viewBtn.dataset.id,
                    viewBtn.dataset.title,
                    viewBtn.dataset.link
                );
            }
        });

        // 링크 복사 버튼 이벤트
        document.getElementById('btnCopyLink').addEventListener('click', function() {
            const linkInput = document.getElementById('previewLink');
            linkInput.select();
            document.execCommand('copy');
            showToast('링크가 클립보드에 복사되었습니다.', 'success');
        });

        // 링크 열기 버튼 이벤트
        document.getElementById('btnOpenLink').addEventListener('click', function(e) {
            const url = this.getAttribute('href');
            if (!url || url === '#') {
                e.preventDefault();
                showToast('유효한 링크가 아닙니다.', 'warning');
            }
        });

        // 승인 버튼 이벤트
        document.addEventListener('click', function(e) {
            const approveBtn = e.target.closest('.btn-approve');
            if (approveBtn) {
                e.preventDefault();
                showSingleApproveModal(approveBtn.dataset.id);
            }
        });

        // 거절 버튼 이벤트
        document.addEventListener('click', function(e) {
            const rejectBtn = e.target.closest('.btn-reject');
            if (rejectBtn) {
                e.preventDefault();
                showRejectModal(rejectBtn.dataset.id);
            }
        });

        // 모달 내 승인 버튼 이벤트
        document.getElementById('btnApproveInModal').addEventListener('click', function() {
            if (currentActivityId) {
                showSingleApproveModal(currentActivityId);
                activityPreviewModal.hide();
            }
        });

        // 모달 내 거절 버튼 이벤트
        document.getElementById('btnRejectInModal').addEventListener('click', function() {
            if (currentActivityId) {
                showRejectModal(currentActivityId);
                activityPreviewModal.hide();
            }
        });

        // 일괄 승인 버튼 이벤트
        if (btnBulkApprove) {
            btnBulkApprove.addEventListener('click', showBulkApproveModal);
        }

        // 일괄 거절 버튼 이벤트
        if (btnBulkReject) {
            btnBulkReject.addEventListener('click', showBulkRejectModal);
        }

        // 일괄 승인 확인 버튼 이벤트
        if (btnConfirmBulkApprove) {
            btnConfirmBulkApprove.addEventListener('click', confirmBulkApprove);
        }

        // 일괄 거절 확인 버튼 이벤트
        if (btnConfirmBulkReject) {
            btnConfirmBulkReject.addEventListener('click', function() {
                // 거절 사유
                let reason = bulkRejectReason.value;
                if (reason === 'OTHER' && otherRejectReason.value.trim()) {
                    reason = otherRejectReason.value.trim();
                }

                if (!reason) {
                    showToast('거절 사유를 선택하거나 입력해주세요.', 'warning');
                    return;
                }

                // 체크된 항목이 있는지 확인 (일괄 거절인 경우)
                const checkedBoxes = document.querySelectorAll('.activity-select:checked:not(:disabled)');
                if (checkedBoxes.length > 0) {
                    // 일괄 거절 처리
                    confirmBulkReject();
                } else if (currentActivityId) {
                    // 단일 거절 처리
                    rejectActivity(currentActivityId, reason);
                    bulkRejectModal.hide();
                } else {
                    bulkRejectModal.hide();
                }
            });
        }

        // 거절 사유 선택 변경 이벤트
        if (bulkRejectReason) {
            bulkRejectReason.addEventListener('change', function() {
                otherReasonContainer.style.display =
                    bulkRejectReason.value === 'OTHER' ? 'block' : 'none';
            });
        }

        // 단일 승인 확인 버튼 이벤트
        const btnConfirmSingleApprove = document.getElementById('btnConfirmSingleApprove');
        if (btnConfirmSingleApprove) {
            btnConfirmSingleApprove.addEventListener('click', function() {
                if (currentActivityId) {
                    const pointType = document.getElementById('singlePointType').value;
                    approveActivity(currentActivityId, pointType);
                    singleApproveModal.hide();
                }
            });
        }
    }

    /**
     * 전체 선택 체크박스 상태 업데이트
     */
    function updateSelectAllCheckbox() {
        const checkboxes = document.querySelectorAll('.activity-select:not(:disabled)');
        const checkedBoxes = document.querySelectorAll('.activity-select:checked:not(:disabled)');

        if (selectAllCheckbox) {
            if (checkboxes.length === 0) {
                selectAllCheckbox.checked = false;
                selectAllCheckbox.disabled = true;
            } else {
                selectAllCheckbox.disabled = false;
                selectAllCheckbox.checked = checkboxes.length === checkedBoxes.length;
            }
        }
    }

    /**
     * 일괄 작업 버튼 상태 업데이트
     */
    function updateBulkActionButtons() {
        const checkedBoxes = document.querySelectorAll('.activity-select:checked:not(:disabled)');
        const hasSelection = checkedBoxes.length > 0;

        if (btnBulkApprove) {
            btnBulkApprove.disabled = !hasSelection;
        }

        if (btnBulkReject) {
            btnBulkReject.disabled = !hasSelection;
        }
    }

    /**
     * 검색 실행
     */
    function performSearch() {
        if (searchForm) {
            searchForm.submit();
        }
    }

    /**
     * 활동 미리보기 모달 표시
     * @param {string} id - 활동 ID
     * @param {string} title - 활동 제목
     * @param {string} link - 활동 링크
     */
    function showPreviewModal(id, title, link) {
        currentActivityId = id;

        // 모달 제목 설정
        document.getElementById('activityPreviewModalTitle').textContent = title || '활동 미리보기';

        // 링크 입력 필드 설정
        const previewLinkInput = document.getElementById('previewLink');
        previewLinkInput.value = link || '';

        // 링크 열기 버튼 설정
        const openLinkBtn = document.getElementById('btnOpenLink');
        openLinkBtn.href = link || '#';

        // 미리보기 컨테이너 설정
        const previewContainer = document.getElementById('previewContainer');
        previewContainer.innerHTML = '';

        try {
            // 유효한 URL인지 확인
            new URL(link);

            // 소셜 미디어 링크 처리
            if (link.includes('twitter.com') || link.includes('x.com')) {
                previewContainer.innerHTML = '<div class="alert alert-info">트위터/X 콘텐츠는 새 창에서 확인해주세요.</div>';
            } else if (link.includes('facebook.com')) {
                previewContainer.innerHTML = '<div class="alert alert-info">페이스북 콘텐츠는 새 창에서 확인해주세요.</div>';
            } else if (link.includes('instagram.com')) {
                previewContainer.innerHTML = '<div class="alert alert-info">인스타그램 콘텐츠는 새 창에서 확인해주세요.</div>';
            } else if (link.includes('youtube.com') || link.includes('youtu.be')) {
                // 유튜브 영상 임베드
                const videoId = getYoutubeVideoId(link);
                if (videoId) {
                    const iframe = document.createElement('iframe');
                    iframe.width = '100%';
                    iframe.height = '315';
                    iframe.src = `https://www.youtube.com/embed/${videoId}`;
                    iframe.frameBorder = '0';
                    iframe.allow = 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture';
                    iframe.allowFullscreen = true;
                    previewContainer.appendChild(iframe);
                } else {
                    previewContainer.innerHTML = '<div class="alert alert-warning">유튜브 비디오 ID를 추출할 수 없습니다.</div>';
                }
            } else {
                // 일반 웹페이지는 iframe으로 표시 시도
                previewContainer.innerHTML = '<div class="alert alert-info">이 링크는 미리보기가 제한될 수 있습니다. 새 창에서 확인해주세요.</div>';
            }
        } catch (e) {
            // 유효하지 않은 URL
            previewContainer.innerHTML = '<div class="alert alert-danger">유효하지 않은 URL입니다.</div>';
        }

        // 모달 표시
        activityPreviewModal.show();
    }

    /**
     * 유튜브 URL에서 비디오 ID 추출
     * @param {string} url - 유튜브 URL
     * @returns {string|null} - 비디오 ID 또는 null
     */
    function getYoutubeVideoId(url) {
        try {
            const urlObj = new URL(url);

            // youtube.com 형식 URL
            if (urlObj.hostname.includes('youtube.com')) {
                return urlObj.searchParams.get('v');
            }

            // youtu.be 형식 URL
            if (urlObj.hostname === 'youtu.be') {
                return urlObj.pathname.substring(1);
            }

            return null;
        } catch (e) {
            return null;
        }
    }

    /**
     * 단일 활동 승인 모달 표시
     * @param {string} activityId - 활동 ID
     */
    function showSingleApproveModal(activityId) {
        currentActivityId = activityId;

        // 기본 포인트 유형 선택 (첫 번째 옵션)
        const singlePointTypeSelect = document.getElementById('singlePointType');
        if (singlePointTypeSelect) {
            singlePointTypeSelect.selectedIndex = 0;
        }

        // 승인 모달 표시
        singleApproveModal.show();
    }

    /**
     * 활동 승인
     * @param {string} activityId - 활동 ID
     * @param {string} pointType - 포인트 유형
     */
    async function approveActivity(activityId, pointType) {
        const btn = document.querySelector(`.btn-approve[data-id="${activityId}"]`);
        if (btn) {
            const originalText = btn.innerHTML;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
            btn.disabled = true;

            try {
                const response = await adminAuthFetch(`/admin/activities/${activityId}/approve`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ pointType: pointType })
                });

                if (response.ok) {
                    const result = await response.json();
                    showToast('활동이 성공적으로 승인되었습니다.', 'success');

                    // 페이지 새로고침
                    setTimeout(() => {
                        window.location.reload();
                    }, 1000);
                } else {
                    const error = await response.json();
                    throw new Error(error.message || '승인 처리 중 오류가 발생했습니다.');
                }
            } catch (error) {
                console.error('활동 승인 오류:', error);
                showToast(`오류: ${error.message || '알 수 없는 오류가 발생했습니다.'}`, 'error');

                // 버튼 복원
                btn.innerHTML = originalText;
                btn.disabled = false;
            }
        }
    }

    /**
     * 거절 모달 표시
     * @param {string} activityId - 활동 ID
     */
    function showRejectModal(activityId) {
        currentActivityId = activityId;

        // 거절 사유 초기화
        if (bulkRejectReason) {
            bulkRejectReason.value = 'INVALID_LINK';
            otherReasonContainer.style.display = 'none';
        }
        if (otherRejectReason) {
            otherRejectReason.value = '';
        }

        // 일괄 거절 모달 표시
        document.getElementById('rejectSelectedCount').textContent = '1';
        bulkRejectModal.show();
    }

    /**
     * 활동 거절
     * @param {string} activityId - 활동 ID
     * @param {string} reason - 거절 사유
     */
    async function rejectActivity(activityId, reason) {
        const btn = document.querySelector(`.btn-reject[data-id="${activityId}"]`);
        if (btn) {
            const originalText = btn.innerHTML;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
            btn.disabled = true;

            try {
                const response = await adminAuthFetch(`/admin/activities/${activityId}/reject`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ reason: reason })
                });

                if (response.ok) {
                    const result = await response.json();
                    showToast('활동이 성공적으로 거절되었습니다.', 'success');

                    // 페이지 새로고침
                    setTimeout(() => {
                        window.location.reload();
                    }, 1000);
                } else {
                    const error = await response.json();
                    throw new Error(error.message || '거절 처리 중 오류가 발생했습니다.');
                }
            } catch (error) {
                console.error('활동 거절 오류:', error);
                showToast(`오류: ${error.message || '알 수 없는 오류가 발생했습니다.'}`, 'error');

                // 버튼 복원
                btn.innerHTML = originalText;
                btn.disabled = false;
            }
        }
    }

    /**
     * 일괄 승인 모달 표시
     */
    function showBulkApproveModal() {
        const checkedBoxes = document.querySelectorAll('.activity-select:checked:not(:disabled)');
        if (checkedBoxes.length === 0) {
            showToast('선택된 항목이 없습니다.', 'warning');
            return;
        }

        document.getElementById('approveSelectedCount').textContent = checkedBoxes.length;

        // 포인트 유형 기본값 설정
        const pointTypeSelect = document.getElementById('pointType');
        if (pointTypeSelect) {
            pointTypeSelect.selectedIndex = 0;
        }

        bulkApproveModal.show();
    }

    /**
     * 일괄 거절 모달 표시
     */
    function showBulkRejectModal() {
        const checkedBoxes = document.querySelectorAll('.activity-select:checked:not(:disabled)');
        if (checkedBoxes.length === 0) {
            showToast('선택된 항목이 없습니다.', 'warning');
            return;
        }

        document.getElementById('rejectSelectedCount').textContent = checkedBoxes.length;

        // 거절 사유 초기화
        if (bulkRejectReason) {
            bulkRejectReason.value = 'INVALID_LINK';
            otherReasonContainer.style.display = 'none';
        }
        if (otherRejectReason) {
            otherRejectReason.value = '';
        }

        bulkRejectModal.show();
    }

    /**
     * 일괄 승인 확인
     */
    async function confirmBulkApprove() {
        const checkedBoxes = document.querySelectorAll('.activity-select:checked:not(:disabled)');
        if (checkedBoxes.length === 0) {
            bulkApproveModal.hide();
            return;
        }

        const activityIds = Array.from(checkedBoxes).map(checkbox => checkbox.dataset.id);
        const pointType = document.getElementById('pointType').value;

        btnConfirmBulkApprove.disabled = true;
        btnConfirmBulkApprove.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> 처리 중...';

        try {
            const response = await adminAuthFetch('/admin/activities/bulk-approve', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    activityIds: activityIds,
                    pointType: pointType
                })
            });

            if (response.ok) {
                const result = await response.json();
                showToast(`${result.successCount || activityIds.length}개의 활동이 성공적으로 승인되었습니다.`, 'success');

                // 페이지 새로고침
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                const error = await response.json();
                throw new Error(error.message || '일괄 승인 처리 중 오류가 발생했습니다.');
            }
        } catch (error) {
            console.error('일괄 승인 오류:', error);
            showToast(`오류: ${error.message || '알 수 없는 오류가 발생했습니다.'}`, 'error');
        } finally {
            btnConfirmBulkApprove.disabled = false;
            btnConfirmBulkApprove.innerHTML = '<i class="fas fa-check me-1"></i> 승인';
            bulkApproveModal.hide();
        }
    }

    /**
     * 일괄 거절 확인
     */
    async function confirmBulkReject() {
        const checkedBoxes = document.querySelectorAll('.activity-select:checked:not(:disabled)');
        if (checkedBoxes.length === 0) {
            bulkRejectModal.hide();
            return;
        }

        const activityIds = Array.from(checkedBoxes).map(checkbox => checkbox.dataset.id);

        // 거절 사유
        let reason = bulkRejectReason.value;
        if (reason === 'OTHER' && otherRejectReason.value.trim()) {
            reason = otherRejectReason.value.trim();
        }

        if (!reason) {
            showToast('거절 사유를 선택하거나 입력해주세요.', 'warning');
            return;
        }

        btnConfirmBulkReject.disabled = true;
        btnConfirmBulkReject.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> 처리 중...';

        try {
            const response = await adminAuthFetch('/admin/activities/bulk-reject', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    activityIds: activityIds,
                    reason: reason
                })
            });

            if (response.ok) {
                const result = await response.json();
                showToast(`${result.successCount || activityIds.length}개의 활동이 성공적으로 거절되었습니다.`, 'success');

                // 페이지 새로고침
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                const error = await response.json();
                throw new Error(error.message || '일괄 거절 처리 중 오류가 발생했습니다.');
            }
        } catch (error) {
            console.error('일괄 거절 오류:', error);
            showToast(`오류: ${error.message || '알 수 없는 오류가 발생했습니다.'}`, 'error');
        } finally {
            btnConfirmBulkReject.disabled = false;
            btnConfirmBulkReject.innerHTML = '<i class="fas fa-times me-1"></i> 거절';
            bulkRejectModal.hide();
        }
    }

    /**
     * Flatpickr 날짜 범위 선택기 초기화 함수
     */
    function initFlatpickrDateRange() {
        if (!dateRangeInput || typeof flatpickr === 'undefined') {
            console.error('Flatpickr가 로드되지 않았거나 dateRangeInput 요소가 없습니다.');
            return;
        }

        try {
            // 날짜 범위 값 파싱
            const currentValue = dateRangeInput.value.trim();
            let initialDates = null;

            if (currentValue) {
                // 날짜 범위 분리
                const parts = currentValue.split(/\s*~\s*/);
                if (parts.length === 2) {
                    const startDate = parts[0].trim();
                    const endDate = parts[1].trim();

                    // 날짜 객체로 변환 시도
                    try {
                        initialDates = [new Date(startDate), new Date(endDate)];

                        // 유효하지 않은 날짜 확인
                        if (isNaN(initialDates[0].getTime()) || isNaN(initialDates[1].getTime())) {
                            console.warn('유효하지 않은 날짜 형식입니다:', startDate, endDate);
                            initialDates = null;
                        }
                    } catch (e) {
                        console.warn('날짜 변환 실패:', e);
                        initialDates = null;
                    }
                }
            }

            // 상위 요소에 특별한 클래스 추가
            const wrapper = dateRangeInput.closest('.input-group');
            if (!wrapper) return;

            wrapper.classList.add('date-range-input-group');

            // 클리어 버튼 생성
            let clearBtn = wrapper.querySelector('.date-clear-btn');
            if (!clearBtn) {
                clearBtn = document.createElement('button');
                clearBtn.type = 'button';
                clearBtn.className = 'date-clear-btn';
                clearBtn.innerHTML = '<i class="fas fa-times"></i>';
                wrapper.appendChild(clearBtn);
            }

            // 초기 클리어 버튼 표시 여부
            clearBtn.style.display = initialDates ? 'flex' : 'none';

            // 달력 아이콘 설정
            const calendarIcon = wrapper.querySelector('.input-group-text');
            if (calendarIcon) {
                calendarIcon.style.cursor = 'pointer';
            }

            // Flatpickr 설정
            const fpInstance = flatpickr(dateRangeInput, {
                mode: 'range',
                dateFormat: 'Y-m-d',  // 날짜 형식을 명확하게 지정
                locale: 'ko',
                disableMobile: true,
                defaultDate: initialDates,
                onChange: function(selectedDates) {
                    if (selectedDates.length === 2) {
                        // 날짜가 모두 선택된 경우, 명확한 형식으로 표시
                        const formatted = selectedDates.map(date => formatDate(date));
                        dateRangeInput.value = formatted.join(' ~ ');
                        clearBtn.style.display = 'flex';
                    }
                }
            });

            // 달력 아이콘 클릭 이벤트
            if (calendarIcon) {
                calendarIcon.addEventListener('click', () => fpInstance.toggle());
            }

            // 클리어 버튼 클릭 이벤트
            if (clearBtn) {
                clearBtn.addEventListener('click', e => {
                    e.preventDefault();
                    e.stopPropagation();
                    fpInstance.clear();
                    dateRangeInput.value = '';
                    clearBtn.style.display = 'none';
                });
            }
        } catch (error) {
            console.error('Flatpickr 초기화 중 오류가 발생했습니다:', error);
        }
    }

    /**
     * Date 객체를 YYYY-MM-DD 형식으로 변환
     * @param {Date} date
     * @returns {string}
     */
    function formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    /**
     * 토스트 알림 표시
     * @param {string} message - 알림 메시지
     * @param {string} type - 알림 타입 (success, error, warning, info)
     */
    function showToast(message, type = 'info') {
        // 만약 Toastr 라이브러리가 있다면 사용
        if (window.toastr) {
            toastr[type](message);
            return;
        }

        // 커스텀 알림 시스템 사용
        if (window.CustomNotification) {
            window.CustomNotification.show(message, type);
            return;
        }

        // 기본 alert 사용
        alert(message);
    }
});