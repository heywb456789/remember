import {
  optionalAuthFetch,
  handleFetchError,
  FetchError
} from '../commonFetch.js';

const mapCategoryLabel = {
  ALL: '전체',
  ORIGINAL_VIDEO: '동영상',
  ORIGINAL_SHORTS: '쇼츠',
  ORIGINAL_NEWS: '뉴스',
  BOARD_POST: '자유게시판',
  VOTE_POST: '투표광장'
};
const catToTab = {
  ALL: 'all', ORIGINAL_VIDEO: 'video', ORIGINAL_SHORTS: 'shorts',
  ORIGINAL_NEWS: 'article', BOARD_POST: 'board', VOTE_POST: 'vote'
};
const tabToCat = {
  all: 'ALL', video: 'ORIGINAL_VIDEO', shorts: 'ORIGINAL_SHORTS',
  article: 'ORIGINAL_NEWS', board: 'BOARD_POST', vote: 'VOTE_POST'
};

let currentKeyword = '';

document.addEventListener('DOMContentLoaded', async () => {
  try {
    const params      = new URLSearchParams(window.location.search);
    const initCat     = params.get('searchCategory') || 'ALL';
    const initSection = params.get('searchSection')  || 'ALL';
    const initKeyword = params.get('searchKeyword')  || '';
    const initPage    = parseInt(params.get('page')  || '0', 10);
    const initSize    = parseInt(params.get('size')  || '10',10);

    currentKeyword = initKeyword;

    // 1) 필터 & 입력 초기화
    document.getElementById('search-filter-section').value = catToTab[initCat];
    document.getElementById('search-filter-field').value   = initSection.toLowerCase();
    document.getElementById('search-input').value          = initKeyword;

    // 2) 탭 초기 active 설정
    document.querySelectorAll('.search-tab-item').forEach(tab => {
      tab.classList.toggle('active', tab.dataset.tab === catToTab[initCat]);
    });

    // 3) 폼 submit (Enter 혹은 돋보기 버튼)
    const searchForm = document.querySelector('.search-form');
    searchForm.addEventListener('submit', e => {
      e.preventDefault();
      reloadSearch(0, document.getElementById('search-filter-section').value);
    });

    // 4) 탭 클릭 시
    document.querySelectorAll('.search-tab-item')
            .forEach(tab => tab.addEventListener('click', () =>
              reloadSearch(0, tab.dataset.tab)));

    // 5) select 변경 시
    document.getElementById('search-filter-section')
            .addEventListener('change', () =>
              reloadSearch(0, document.getElementById('search-filter-section').value));
    document.getElementById('search-filter-field')
            .addEventListener('change', () =>
              reloadSearch(0, document.getElementById('search-filter-section').value));

    // 6) 최초 API 호출
    await loadSearchResults(initCat, initSection, initKeyword, initPage, initSize);
  } catch (err) {
    handleFetchError(err);
  }
});

function reloadSearch(newPage, forceTab) {
  const tabVal = (forceTab !== undefined)
      ? forceTab
      : document.getElementById('search-filter-section').value;
  const category = tabToCat[tabVal] || 'ALL';
  const sectionScope = document.getElementById('search-filter-field')
  .value.toUpperCase() || 'ALL';
  const keyword = document.getElementById('search-input')
  .value.trim();

  const params = new URLSearchParams({
    searchCategory: category,
    searchSection: sectionScope,
    searchKeyword: keyword,
    page: newPage,
    size: '10'
  });
  window.history.replaceState({}, '',
      `/main/search.html?${params}`
  );
  loadSearchResults(category, sectionScope, keyword, newPage, 10);
}

async function loadSearchResults(category, sectionScope, keyword, page, size) {
  try {
    const res = await optionalAuthFetch(
      `/api/search?searchCategory=${category}` +
      `&searchSection=${sectionScope}` +
      `&searchKeyword=${encodeURIComponent(keyword)}` +
      `&page=${page}&size=${size}`
    );
    const json                         = await res.json();
    const { data, pagination, counts } = json.response;

    // 탭 카운트 & active 처리
    document.querySelectorAll('.search-tab-item').forEach(tab => {
      const tabKey = tabToCat[tab.dataset.tab];
      const cnt    = counts[tabKey] != null ? counts[tabKey] : 0;
      tab.querySelector('.tab-count').textContent = `(${cnt})`;
      tab.classList.toggle('active', tab.dataset.tab === catToTab[category]);
    });

    // active 탭 보이도록 스크롤
    const activeEl = document.querySelector('.search-tab-item.active');
    if (activeEl) activeEl.scrollIntoView({
      behavior: 'smooth', inline: 'center', block: 'nearest'
    });

    // 결과 필터링 & 렌더링
    const filtered = (category === 'ALL')
      ? data
      : data.filter(it => it.searchCategory === category);
    renderList(filtered);

    // 페이지네이션
    renderPagination(pagination);
  } catch (err) {
    handleFetchError(err);
  }
}

function renderList(items) {
  const cont = document.getElementById('searchResults');
  if (!items.length) {
    cont.innerHTML = '<div class="empty-state">검색 결과가 없습니다.</div>';
    return;
  }
  cont.innerHTML = items.map(it => {
    const thumb       = it.imageUrl ? `<img src="${it.imageUrl}" class="list-thumb">` : '';

    const highlighted = it.title.replace(new RegExp(currentKeyword, 'gi'), m => `<span class="highlight">${m}</span>`);

    const badge = `<span class="content-type">${mapCategoryLabel[it.searchCategory]}</span>`;

    const date  = `<span class="item-meta">${it.createdAt.slice(0,10).replace(/-/g,'.')}</span>`;

    return `
      <div class="result-item" data-redirectionurl="${it.redirectionUrl}">
        ${thumb}
        <div class="item-text">${highlighted}${badge}</div>
        ${date}
      </div>`;

  }).join('');

  cont.querySelectorAll('.result-item')
      .forEach(el => el.addEventListener('click', () =>
        window.location.href = el.dataset.redirectionurl));
}

function renderPagination(pg) {
  const pager = document.getElementById('pagination');

  let html = `<span class="pagination-item prev" data-page="${pg.currentPage-1}">
                <i class="fas fa-chevron-left"></i></span>`;

  for (let i=1; i<=pg.totalPages; i++) {
    html += `<span class="pagination-item ${i===pg.currentPage?'active':''}" data-page="${i-1}">${i}</span>`;
  }

  html += `<span class="pagination-item next" data-page="${pg.currentPage+1}"><i class="fas fa-chevron-right"></i></span>`;

  pager.innerHTML = html;

  pager.querySelectorAll('.pagination-item').forEach(btn => {

    const p = parseInt(btn.dataset.page, 10);

    if (!isNaN(p) && p>=0 && p<pg.totalPages) {
      btn.addEventListener('click', () => reloadSearch(p));
    }
  });
}
