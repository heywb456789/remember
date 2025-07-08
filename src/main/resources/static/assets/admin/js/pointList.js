/**
 * 포인트 내역 목록 페이지 JavaScript
 */
import {adminAuthFetch} from '/js/commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    // DOM 요소
    const searchForm = document.getElementById('searchForm');
    const dateRangeInput = document.getElementById('dateRange');
    const btnExportExcel = document.getElementById('btnExportExcel');

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

            // 검색 버튼 클릭 이벤트
            const searchButton = searchForm.querySelector('button[type="submit"]');
            if (searchButton) {
                searchButton.addEventListener('click', function(e) {
                    e.preventDefault();
                    performSearch();
                });
            }

            // 검색어 입력 필드에서 엔터 키 이벤트
            const searchTextInput = document.getElementById('searchText');
            if (searchTextInput) {
                searchTextInput.addEventListener('keypress', function(e) {
                    if (e.key === 'Enter') {
                        e.preventDefault();
                        performSearch();
                    }
                });
            }
        }

        // 엑셀 내보내기 버튼 이벤트
        if (btnExportExcel) {
            btnExportExcel.addEventListener('click', exportToExcel);
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
     * 전체 내역 엑셀 내보내기
     */
    function exportToExcel() {
        // 현재 검색 조건을 유지한 채로 엑셀 내보내기
        const searchParams = new URLSearchParams(window.location.search);
        searchParams.set('export', 'excel');

        const downloadUrl = `/admin/points/export?${searchParams.toString()}`;
        window.location.href = downloadUrl;
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