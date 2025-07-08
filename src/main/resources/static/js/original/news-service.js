import { optionalAuthFetch, handleFetchError, FetchError } from '../commonFetch.js';

/**
 * 뉴스 관련 API 호출을 처리하는 서비스
 */
export default class NewsService {
  /**
   * 뉴스 목록 가져오기
   * @param {number} page - 페이지 번호
   * @param {number} size - 페이지 크기
   * @param {string} category - 카테고리 (선택 사항)
   * @param {string} keyword - 검색어 (선택 사항)
   * @returns {Promise<Object>} 뉴스 목록 정보
   */
  static async getNewsList(page = 0, size = 10, category = '', keyword = '') {
    try {
      let url = `/api/news?page=${page}&size=${size}`;
      if (category) url += `&category=${category}`;
      if (keyword) url += `&searchType=ARTICLE_TITLE_CONTENT&searchText=${encodeURIComponent(keyword)}`;

      const response = await optionalAuthFetch(url);
      // 콘텐츠 없음
      if (response.status === 204) return { response: { data: [] } };

      return await response.json();
    } catch (err) {
      handleFetchError(err);
      throw err;
    }
  }

  /**
   * 특정 뉴스 상세 정보 가져오기
   * @param {string|number} newsId - 뉴스 ID
   * @returns {Promise<Object>} 뉴스 상세 정보
   */
  static async getNewsDetail(newsId) {
    try {
      const response = await optionalAuthFetch(`/api/news/${newsId}`);
      return await response.json();
    } catch (err) {
      handleFetchError(err);
      throw err;
    }
  }

  /**
   * 관련 뉴스 목록 가져오기
   * @param {string|number} newsId - 뉴스 ID
   * @param {number} limit - 가져올 뉴스 수
   * @returns {Promise<Object>} 관련 뉴스 목록
   */
  static async getRelatedNews(newsId, limit = 5) {
    try {
      const response = await optionalAuthFetch(`/api/news/${newsId}/related?limit=${limit}`);
      if (response.status === 204) return { response: { data: [] } };
      return await response.json();
    } catch (err) {
      handleFetchError(err);
      return { response: { data: [] } };
    }
  }

  /**
   * 인기 뉴스 목록 가져오기
   * @param {number} limit - 가져올 뉴스 수
   * @returns {Promise<Object>} 인기 뉴스 목록
   */
  static async getPopularNews(limit = 5) {
    try {
      const response = await optionalAuthFetch(`/api/news/popular?limit=${limit}`);
      if (response.status === 204) return { response: { data: [] } };
      return await response.json();
    } catch (err) {
      handleFetchError(err);
      return { response: { data: [] } };
    }
  }

  /**
   * 카테고리별 뉴스 가져오기
   * @param {string} category - 카테고리 코드
   * @param {number} limit - 가져올 뉴스 수
   * @returns {Promise<Object>} 카테고리별 뉴스 목록
   */
  static async getNewsByCategory(category, limit = 10) {
    try {
      const response = await optionalAuthFetch(`/api/news/category/${category}?limit=${limit}`);
      if (response.status === 204) return { response: { data: [] } };
      return await response.json();
    } catch (err) {
      handleFetchError(err);
      return { response: { data: [] } };
    }
  }
}