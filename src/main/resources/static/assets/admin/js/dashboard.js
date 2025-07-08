/**
 * Spark Admin Panel - Dashboard JS
 */

(() => {
  'use strict';

  /**
   * 문서 로드 완료 시 실행할 함수
   */
  const initDashboard = () => {
    initMovementChart();
    initBrowserChart();
    initCalendar();
    initVisitorsMap();
  };

  /**
   * 최근 이동 차트 초기화
   */
  const initMovementChart = () => {
    const ctx = document.getElementById('movementChart');
    if (!ctx) return;

    const labels = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    
    // 그라데이션 생성
    const gradient = ctx.getContext('2d').createLinearGradient(0, 0, 0, 400);
    gradient.addColorStop(0, 'rgba(54, 162, 235, 0.7)');
    gradient.addColorStop(1, 'rgba(54, 162, 235, 0)');
    
    // 차트 데이터
    const data = {
      labels: labels,
      datasets: [
        {
          label: 'Movement',
          data: [5, 10, 15, 12, 18, 15, 22, 25, 28, 24, 20, 15],
          fill: true,
          backgroundColor: gradient,
          borderColor: 'rgba(54, 162, 235, 1)',
          tension: 0.4,
          pointRadius: 3,
          pointBackgroundColor: 'rgba(54, 162, 235, 1)',
          pointBorderColor: '#fff',
          pointBorderWidth: 2
        }
      ]
    };
    
    // 차트 옵션
    const options = {
      responsive: true,
      maintainAspectRatio: true,
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          mode: 'index',
          intersect: false,
          backgroundColor: 'rgba(0, 0, 0, 0.7)',
          titleFont: {
            size: 12
          },
          bodyFont: {
            size: 12
          },
          padding: 10
        }
      },
      scales: {
        x: {
          grid: {
            display: false
          }
        },
        y: {
          beginAtZero: true,
          max: 30,
          ticks: {
            stepSize: 5
          },
          grid: {
            borderDash: [3, 3],
            color: 'rgba(0, 0, 0, 0.05)'
          }
        }
      },
      layout: {
        padding: 0
      }
    };

    // 차트 생성
    new Chart(ctx, {
      type: 'line',
      data: data,
      options: options
    });
  };

  /**
   * 브라우저 사용 차트 초기화
   */
  const initBrowserChart = () => {
    const ctx = document.getElementById('browserChart');
    if (!ctx) return;

    // 차트 데이터
    const data = {
      labels: ['Chrome', 'Firefox', 'Safari', 'Edge', 'Others'],
      datasets: [
        {
          data: [65, 15, 12, 5, 3],
          backgroundColor: [
            '#4285f4', // Chrome (Google Blue)
            '#ff9800', // Firefox (Orange)
            '#5ac8fa', // Safari (Blue)
            '#0078d7', // Edge (Blue)
            '#607d8b'  // Others (Gray)
          ],
          borderWidth: 0,
          hoverOffset: 5
        }
      ]
    };

    // 차트 옵션
    const options = {
      responsive: true,
      maintainAspectRatio: true,
      cutout: '70%',
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          backgroundColor: 'rgba(0, 0, 0, 0.7)',
          titleFont: {
            size: 12
          },
          bodyFont: {
            size: 12
          },
          padding: 10
        }
      },
      layout: {
        padding: 0
      }
    };
    
    // 차트 생성
    new Chart(ctx, {
      type: 'doughnut',
      data: data,
      options: options
    });
  };

  /**
   * 캘린더 이벤트 초기화
   */
  const initCalendar = () => {
    const prevMonthBtn = document.querySelector('.prev-month');
    const nextMonthBtn = document.querySelector('.next-month');
    const monthNameEl = document.querySelector('.month-name');
    
    if (!prevMonthBtn || !nextMonthBtn || !monthNameEl) return;
    
    let currentDate = new Date();
    
    // 현재 월 표시
    updateCalendarTitle();
    
    // 이전/다음 월 이벤트
    prevMonthBtn.addEventListener('click', () => {
      currentDate.setMonth(currentDate.getMonth() - 1);
      updateCalendarTitle();
    });
    
    nextMonthBtn.addEventListener('click', () => {
      currentDate.setMonth(currentDate.getMonth() + 1);
      updateCalendarTitle();
    });
    
    // 날짜 클릭 이벤트
    document.querySelectorAll('.calendar-table td:not(.prev-month):not(.next-month)').forEach(day => {
      day.addEventListener('click', () => {
        // 기존 선택 날짜 제거
        document.querySelectorAll('.calendar-table td.selected').forEach(el => {
          el.classList.remove('selected');
        });
        
        // 새 날짜 선택
        day.classList.add('selected');
      });
    });
    
    // 캘린더 제목 업데이트 함수
    function updateCalendarTitle() {
      const months = ['January', 'February', 'March', 'April', 'May', 'June', 
                      'July', 'August', 'September', 'October', 'November', 'December'];
      monthNameEl.textContent = `${months[currentDate.getMonth()]} ${currentDate.getFullYear()}`;
    }
  };

  /**
   * 방문자 맵 초기화 (실제 맵 통합을 위한 준비)
   */
  const initVisitorsMap = () => {
    const mapElement = document.getElementById('visitorsMap');
    if (!mapElement) return;
    
    // 여기서는 이미지로 대체하므로 별도의 초기화는 필요 없음
    // 실제 구현 시 아래와 같이 지도 라이브러리 초기화
    
    /*
    // leaflet.js 또는 Google Maps 등을 사용한 예시
    const map = L.map('visitorsMap').setView([40, -95], 4);
    
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);
    
    // 방문자 데이터 포인트 (예시)
    const visitors = [
      { lat: 40.7128, lng: -74.0060, count: 120 }, // New York
      { lat: 34.0522, lng: -118.2437, count: 80 }, // Los Angeles
      { lat: 51.5074, lng: -0.1278, count: 60 },   // London
      { lat: 48.8566, lng: 2.3522, count: 40 },    // Paris
      { lat: 35.6762, lng: 139.6503, count: 30 }   // Tokyo
    ];
    
    // 방문자 마커 추가
    visitors.forEach(visitor => {
      L.circleMarker([visitor.lat, visitor.lng], {
        radius: Math.sqrt(visitor.count) / 2,
        fillColor: '#3498db',
        color: '#2980b9',
        weight: 1,
        opacity: 0.8,
        fillOpacity: 0.6
      }).addTo(map);
    });
    */
  };

  /**
   * 통계 카드 애니메이션
   */
  const initStatCardAnimation = () => {
    const statValues = document.querySelectorAll('.card-value');
    
    statValues.forEach(element => {
      const finalValue = element.textContent;
      const isNumeric = !isNaN(parseInt(finalValue.replace(/[,$]/g, '')));
      
      if (isNumeric) {
        const isMoney = finalValue.includes('$');
        let targetValue = parseFloat(finalValue.replace(/[,$]/g, ''));
        let startValue = 0;
        let duration = 1000; // 1초
        let startTime = null;
        
        element.textContent = isMoney ? '$0' : '0';
        
        function animateValue(timestamp) {
          if (!startTime) startTime = timestamp;
          const progress = Math.min((timestamp - startTime) / duration, 1);
          const currentValue = Math.floor(progress * (targetValue - startValue) + startValue);
          
          if (isMoney) {
            element.textContent = '$' + currentValue.toLocaleString();
          } else {
            element.textContent = currentValue.toLocaleString();
          }
          
          if (progress < 1) {
            window.requestAnimationFrame(animateValue);
          } else {
            element.textContent = finalValue; // 최종값 정확히 설정
          }
        }
        
        window.requestAnimationFrame(animateValue);
      }
    });
  };

  // 문서 로드 완료 시 초기화
  document.addEventListener('DOMContentLoaded', () => {
    initDashboard();
    initStatCardAnimation();
  });
})();