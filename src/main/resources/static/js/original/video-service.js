
import { optionalAuthFetch, handleFetchError } from '../commonFetch.js';
/**
 * 비디오 관련 API 호출을 처리하는 서비스
 */
export default class VideoService {
  /**
   * 최신 비디오 단일 항목 가져오기
   * @returns {Promise<Object>} 비디오 정보
   */
  static async getLatestVideo() {
    try {
      const response = await optionalAuthFetch(`/api/videos?page=0&size=1&type=YOUTUBE_VIDEO`);

      return await response.json();
    } catch (err) {
      console.error('최신 비디오를 가져오는 중 오류 발생:', err);
      // handleFetchError(err);
      return null;
    }
  }

  /**
   * 여러 개의 최신 비디오 가져오기
   * @param {number} limit - 가져올 비디오 수
   * @returns {Promise<Array>} 비디오 목록
   */
  static async getLatestVideos(page=0, limit = 4, originalType='YOUTUBE_VIDEO', keyword='') {
    let url = `/api/videos?page=${page}&size=${limit}&type=${originalType}`;
    if (keyword) url += `&searchType=VIDEO_TITLE_CONTENT&searchText=${encodeURIComponent(keyword)}`;

    try {
      const response = await optionalAuthFetch(url)
      if (response.status === 204) return [];

      return await response.json();
    } catch (err) {
      console.error('비디오 목록을 가져오는 중 오류 발생:', err);
      // handleFetchError(err);
      return [];
    }
  }

  /**
   * 카테고리별 비디오 가져오기
   * @param {string} category - 카테고리 이름
   * @param {number} limit - 가져올 비디오 수
   * @returns {Promise<Array>} 비디오 목록
   */
  static async getVideosByCategory(category, limit = 5) {
    try {
      const response = await fetch(`/api/videos/category?category=${encodeURIComponent(category)}&limit=${limit}`);
      if (response.status === 204) return [];
      return await response.json();
    } catch (err) {
      console.error(`${category} 카테고리 비디오를 가져오는 중 오류 발생:`, err);
      // handleFetchError(err);
      return [];
    }
  }
}