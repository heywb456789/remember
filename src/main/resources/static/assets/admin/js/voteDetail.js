/**
 * 투표 상세 페이지 JavaScript
 */
import {adminAuthFetch} from '../../../js/commonFetch.js';

document.addEventListener('DOMContentLoaded', function () {
  'use strict';

  // DOM 요소
  const btnRefresh = document.getElementById('btnRefresh');
  const btnExportCSV = document.getElementById('btnExportCSV');
  const voteResultsBody = document.getElementById('voteResultsBody');
  const voteChartCanvas = document.getElementById('voteResultsChart');

  // 투표 ID 가져오기 (URL에서 추출)
  const voteId = getVoteIdFromUrl();

  // 데이터 및 상태 변수
  let voteData = null;
  let resultsChart = null;

  // 초기화 함수 호출
  initPage();

  /**
   * 페이지 초기화 함수
   */
  function initPage() {
    // 이미 페이지에 표시된 데이터로 차트 초기화
    initChart();

    // 이벤트 리스너 등록
    if (btnRefresh) {
      btnRefresh.addEventListener('click', refreshVoteData);
    }

    if (btnExportCSV) {
      btnExportCSV.addEventListener('click', exportToCSV);
    }
  }

  /**
   * URL에서 투표 ID 추출
   * @returns {number} 투표 ID
   */
  function getVoteIdFromUrl() {
    const path = window.location.pathname;      // e.g. "/admin/vote/5"
    const segments = path.split('/').filter(Boolean); // ["admin","vote","5"]
    const last = segments[segments.length - 1]; // "5"
    const id = parseInt(last, 10);
    return Number.isNaN(id) ? 0 : id;
  }

  /**
   * 투표 데이터 새로고침
   */
  async function refreshVoteData() {
    window.location.reload();
    // if (!voteId) {
    //   showAlert('유효한 투표 ID가 없습니다.', 'warning');
    //   return;
    // }
    //
    // btnRefresh.disabled = true;
    // btnRefresh.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
    //
    // try {
    //   const res = await adminAuthFetch(`/admin/vote/detail/${voteId}`, {
    //     method: 'GET',
    //     headers: {'Content-Type': 'application/json'}
    //   });
    //
    //   if (!res.ok) {
    //     throw new Error('서버 응답 오류: ' + res.status);
    //   }
    //
    //   const result = await res.json();
    //
    //   if (result.status.code === 'OK_0000') {
    //     voteData = result.data;
    //     updateVoteResults(voteData);
    //     showAlert('투표 데이터가 새로고침되었습니다.', 'success');
    //   } else {
    //     throw new Error(result.message || '데이터를 가져오는 중 오류가 발생했습니다.');
    //   }
    // } catch (err) {
    //   console.error(err);
    //   showAlert('데이터를 가져오는 중 오류가 발생했습니다: ' + err.message, 'danger');
    // } finally {
    //   btnRefresh.disabled = false;
    //   btnRefresh.innerHTML = '<i class="fas fa-sync-alt"></i> 새로고침';
    // }
  }

  /**
   * 투표 결과 업데이트
   * @param {Object} data - 투표 데이터
   */
  function updateVoteResults(data) {
    if (!data || !data.voteOptions) {
      return;
    }

    // 총 투표수 계산
    const totalVotes = data.voteOptions.reduce(
        (sum, option) => sum + option.voteCount, 0);

    // 비율을 계산하고 내림차순으로 정렬
    const sortedOptions = data.voteOptions
    .map(option => ({
      ...option,
      percentage: totalVotes > 0 ? Math.round(
          (option.voteCount / totalVotes) * 100) : 0
    }))
    .sort((a, b) => b.voteCount - a.voteCount);

    // 테이블 업데이트
    updateResultsTable(sortedOptions);

    // 차트 업데이트
    updateResultsChart(sortedOptions);
  }

  /**
   * 투표 결과 테이블 업데이트
   * @param {Array} options - 정렬된 투표 옵션 배열
   */
  function updateResultsTable(options) {
    if (!voteResultsBody) {
      return;
    }

    voteResultsBody.innerHTML = '';

    options.forEach((option, index) => {
      const row = document.createElement('tr');

      row.innerHTML = `
        <td class="text-center">
          ${index < 3
          ? `<span class="badge bg-primary">${index + 1}</span>`
          : `<span class="text-muted">${index + 1}</span>`}
        </td>
        <td>${option.optionName}</td>
        <td class="text-center">${option.voteCount}</td>
        <td>
          <div class="d-flex align-items-center">
            <div class="progress flex-grow-1 me-2" style="height: 8px;">
              <div class="progress-bar" style="width: ${option.percentage}%;"></div>
            </div>
            <span class="text-muted">${option.percentage}%</span>
          </div>
        </td>
      `;

      voteResultsBody.appendChild(row);
    });
  }

  /**
   * 차트 초기화
   */
  function initChart() {
    if (!voteChartCanvas) {
      return;
    }

    // 서버에서 렌더링된 데이터 추출
    const tableRows = document.querySelectorAll('#voteResultsBody tr');
    if (!tableRows.length) {
      return;
    }

    const labels = [];
    const data = [];
    const backgroundColors = [];

    // 컬러 팔레트
    const colorPalette = [
      'rgba(54, 162, 235, 0.8)',
      'rgba(255, 99, 132, 0.8)',
      'rgba(75, 192, 192, 0.8)',
      'rgba(255, 206, 86, 0.8)',
      'rgba(153, 102, 255, 0.8)',
      'rgba(255, 159, 64, 0.8)',
      'rgba(199, 199, 199, 0.8)'
    ];

    tableRows.forEach((row, index) => {
      const optionName = row.cells[1].textContent.trim();
      const percentage = parseInt(
          row.cells[3].querySelector('.text-muted').textContent);

      labels.push(optionName);
      data.push(percentage);
      backgroundColors.push(colorPalette[index % colorPalette.length]);
    });

    // Chart.js 설정
    const ctx = voteChartCanvas.getContext('2d');
    resultsChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: backgroundColors,
          borderColor: 'rgba(255, 255, 255, 0.8)',
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              boxWidth: 12,
              padding: 15
            }
          },
          tooltip: {
            callbacks: {
              label: function (context) {
                return `${context.label}: ${context.parsed}%`;
              }
            }
          }
        },
        animation: {
          animateRotate: true,
          animateScale: true
        }
      }
    });
  }

  /**
   * 차트 업데이트
   * @param {Array} options - 정렬된 투표 옵션 배열
   */
  function updateResultsChart(options) {
    if (!resultsChart) {
      return;
    }

    const labels = options.map(option => option.optionName);
    const data = options.map(option => option.percentage);

    resultsChart.data.labels = labels;
    resultsChart.data.datasets[0].data = data;
    resultsChart.update();
  }

  /**
   * CSV로 내보내기
   */
  function exportToCSV() {
    if (!voteData && !document.querySelector('#voteResultsBody tr')) {
      showAlert('내보낼 데이터가 없습니다.', 'warning');
      return;
    }

    // 페이지에 표시된 데이터 사용
    const tableRows = document.querySelectorAll('#voteResultsBody tr');
    const rows = [];

    // 헤더 추가
    rows.push(['순위', '선택지', '득표수', '비율(%)']);

    // 데이터 행 추가
    tableRows.forEach((row, index) => {
      const rank = index + 1;
      const option = row.cells[1].textContent.trim();
      const votes = row.cells[2].textContent.trim();
      const percentage = row.cells[3].querySelector(
          '.text-muted').textContent.trim();

      rows.push([rank, option, votes, percentage]);
    });

    // CSV 문자열 생성
    let csvContent = 'data:text/csv;charset=utf-8,';

    rows.forEach(rowArray => {
      const row = rowArray.join(',');
      csvContent += row + '\r\n';
    });

    // 다운로드
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download',
        `vote_results_${voteId}_${getFormattedDate()}.csv`);
    document.body.appendChild(link);

    link.click();
    document.body.removeChild(link);
  }

  /**
   * 날짜 형식 지정 (파일명용)
   * @returns {string} 형식화된 날짜 문자열
   */
  function getFormattedDate() {
    const now = new Date();
    return now.getFullYear() +
        ('0' + (now.getMonth() + 1)).slice(-2) +
        ('0' + now.getDate()).slice(-2) + '_' +
        ('0' + now.getHours()).slice(-2) +
        ('0' + now.getMinutes()).slice(-2);
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

    // 기본 alert 사용
    alert(message);
  }
});