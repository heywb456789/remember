/**
 * 회원 포인트 상세 페이지 JavaScript
 */
import {adminAuthFetch} from '/js/commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    // DOM 요소
    const searchForm = document.getElementById('searchForm');
    const dateRangeInput = document.getElementById('dateRange');
    const btnExportExcel = document.getElementById('btnExportExcel');
    const btnRevokePoint = document.getElementById('btnRevokePoint');
    const revokePointModal = new bootstrap.Modal(document.getElementById('revokePointModal'));
    const btnConfirmRevoke = document.getElementById('btnConfirmRevoke');

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

        // 콘텐츠 셀 토글 설정
        initContentCellToggle();
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

            // 검색 버튼 클릭 이벤트
            const searchButton = searchForm.querySelector('button[type="submit"]');
            if (searchButton) {
                searchButton.addEventListener('click', function(e) {
                    e.preventDefault();
                    performSearch();
                });
            }
        }

        // 엑셀 내보내기 버튼 이벤트
        if (btnExportExcel) {
            btnExportExcel.addEventListener('click', exportToExcel);
        }

        // 포인트 회수 버튼 이벤트
        if (btnRevokePoint) {
            btnRevokePoint.addEventListener('click', function() {
                openRevokePointModal();
            });
        }

        // 포인트 회수 확인 버튼 이벤트
        if (btnConfirmRevoke) {
            btnConfirmRevoke.addEventListener('click', confirmRevokePoint);
        }

        // 회수 금액 입력 이벤트 - 최대값 제한
        const revokeAmountInput = document.getElementById('revokeAmount');
        if (revokeAmountInput) {
            const maxPoint = parseFloat(document.querySelector('.text-primary span').textContent.replace(/,/g, ''));
            revokeAmountInput.max = maxPoint;

            revokeAmountInput.addEventListener('input', function() {
                const value = parseFloat(this.value);
                if (value > maxPoint) {
                    this.value = maxPoint;
                } else if (value < 0.1) {
                    this.value = 0.1;
                }
            });
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
     * 엑셀 내보내기
     */
    function exportToExcel() {
        const memberId = document.getElementById('memberId').value;

        // 현재 검색 조건을 유지한 채로 엑셀 내보내기
        const searchParams = new URLSearchParams(window.location.search);
        searchParams.set('export', 'excel');

        const downloadUrl = `/admin/users/points/${memberId}/export?${searchParams.toString()}`;
        window.location.href = downloadUrl;
    }

    /**
     * 포인트 회수 모달 열기
     */
    function openRevokePointModal() {
        // 포인트 회수 모달 내 입력 필드 초기화
        document.getElementById('revokeAmount').value = '';
        document.getElementById('revokeReason').value = '';

        // 모달 표시
        revokePointModal.show();
    }

    /**
     * 포인트 회수 확인
     */
    async function confirmRevokePoint() {
        const memberId = document.getElementById('memberId').value;
        const amount = parseFloat(document.getElementById('revokeAmount').value);
        const reason = document.getElementById('revokeReason').value.trim();

        // 입력값 검증
        if (!amount || amount <= 0) {
            showToast('유효한 회수 금액을 입력해주세요.', 'warning');
            return;
        }

        if (!reason) {
            showToast('회수 사유를 입력해주세요.', 'warning');
            return;
        }

        // 최대 포인트 확인
        const maxPoint = parseFloat(document.querySelector('.text-primary span').textContent.replace(/,/g, ''));
        if (amount > maxPoint) {
            showToast(`최대 ${maxPoint.toLocaleString()}P까지 회수 가능합니다.`, 'warning');
            return;
        }

        // 확인 메시지
        if (!confirm(`${amount.toLocaleString()}P를 회수하시겠습니까?`)) {
            return;
        }

        // 회수 버튼 비활성화
        btnConfirmRevoke.disabled = true;
        const originalButtonText = btnConfirmRevoke.innerHTML;
        btnConfirmRevoke.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 처리 중...';

        try {
            const response = await adminAuthFetch('/admin/points/member-revoke', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    memberId,
                    amount,
                    reason
                })
            });

            const result = await response.json();
            if (!result.response) {
                throw new Error('포인트 회수 중 오류가 발생했습니다.');
            }


            if (result.response) {
                showToast('포인트가 성공적으로 회수되었습니다.', 'success');
                revokePointModal.hide();

                // 페이지 새로고침
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                throw new Error(result.message || '포인트 회수 중 오류가 발생했습니다.');
            }
        } catch (error) {
            console.error('포인트 회수 오류:', error);
            showToast(error.message, 'error');
        } finally {
            btnConfirmRevoke.disabled = false;
            btnConfirmRevoke.innerHTML = originalButtonText;
        }
    }

    /**
     * 콘텐츠 셀 토글 초기화
     * - 콘텐츠가 길 경우 클릭 시 확장/축소되는 기능
     */
    function initContentCellToggle() {
        const contentCells = document.querySelectorAll('.content-cell');

        contentCells.forEach(cell => {
            const content = cell.querySelector('span');
            if (!content || content.textContent.trim() === '-') return;

            // 콘텐츠가 길 경우 줄임표 처리
            if (content.offsetWidth < content.scrollWidth) {
                cell.classList.add('expandable');

                cell.addEventListener('click', function() {
                    this.classList.toggle('expanded');
                });
            }
        });
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