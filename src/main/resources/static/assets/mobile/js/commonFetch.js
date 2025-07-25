// commonFetch.js - common.js에서 import 받아서 사용

// common.js에서 토스트 관련 함수들 import
import {
    showToast as originalShowToast,
    showLoading as originalShowLoading,
    hideLoading as originalHideLoading,
    showConfirm as originalShowConfirm
} from './common.js';

export class FetchError extends Error {
  constructor(status, statusCode, statusMessage, responseBody) {
    super(statusMessage || `HTTP ${status}`);
    this.name = 'FetchError';
    this.httpStatus = status;
    this.statusCode = statusCode;
    this.statusMessage = statusMessage;
    this.responseBody = responseBody;
  }
}

// =========================== 에러 메시지 추출 및 처리 유틸리티 ===========================

/**
 * 서버 응답에서 에러 메시지 추출
 * ResponseDTO 구조: { status: { code, message, httpStatus }, response }
 */
export function extractErrorMessage(error) {
  // 기본 에러 메시지
  let defaultMessage = '요청 처리 중 오류가 발생했습니다.';

  try {
    // 1. FetchError인 경우 (우리가 던진 에러)
    if (error instanceof FetchError) {
      return error.statusMessage || defaultMessage;
    }

    // 2. error.response가 있는 경우 (axios 응답 등)
    if (error.response) {
      const responseData = error.response.data;

      // 우리의 ResponseDTO 구조: { status: { message: "..." }, response: null }
      if (responseData?.status?.message) {
        return responseData.status.message;
      }

      // 기존 구조 호환성: { message: "..." }
      if (responseData?.message) {
        return responseData.message;
      }

      // HTTP 상태 코드별 기본 메시지
      return getDefaultErrorMessage(error.response.status);
    }

    // 3. error.responseBody가 있는 경우 (우리 FetchError)
    if (error.responseBody?.status?.message) {
      return error.responseBody.status.message;
    }

    // 4. error.message가 있는 경우
    if (error.message) {
      return error.message;
    }

    // 5. 문자열 에러인 경우
    if (typeof error === 'string') {
      return error;
    }

  } catch (e) {
    console.error('에러 메시지 추출 중 오류:', e);
  }

  return defaultMessage;
}

/**
 * HTTP 상태 코드별 기본 에러 메시지
 */
function getDefaultErrorMessage(status) {
  switch (status) {
    case 400:
      return '잘못된 요청입니다.';
    case 401:
      return '로그인이 필요합니다.';
    case 403:
      return '권한이 없습니다.';
    case 404:
      return '요청한 리소스를 찾을 수 없습니다.';
    case 409:
      return '요청이 현재 상태와 충돌합니다.';
    case 422:
      return '입력된 데이터가 올바르지 않습니다.';
    case 429:
      return '너무 많은 요청이 발생했습니다. 잠시 후 다시 시도해주세요.';
    case 500:
      return '서버 내부 오류가 발생했습니다.';
    case 502:
      return '서버가 일시적으로 사용할 수 없습니다.';
    case 503:
      return '서비스를 일시적으로 사용할 수 없습니다.';
    case 504:
      return '서버 응답 시간이 초과되었습니다.';
    default:
      return '요청 처리 중 오류가 발생했습니다.';
  }
}

// =========================== 토스트 함수들 (common.js에서 가져온 것 재export) ===========================

/**
 * 토스트 메시지 표시 - common.js에서 import한 함수 사용
 */
export function showToast(message, type = 'error') {
  return originalShowToast(message, type);
}

/**
 * 성공 메시지 표시
 */
export function showSuccess(message) {
  return originalShowToast(message, 'success');
}

/**
 * 정보 메시지 표시
 */
export function showInfo(message) {
  return originalShowToast(message, 'info');
}

/**
 * 경고 메시지 표시
 */
export function showWarning(message) {
  return originalShowToast(message, 'warning');
}

/**
 * 로딩 표시 (common.js에서 재export)
 */
export function showLoading(message) {
  return originalShowLoading(message);
}

/**
 * 로딩 숨김 (common.js에서 재export)
 */
export function hideLoading() {
  return originalHideLoading();
}

/**
 * 확인 다이얼로그 (common.js에서 재export)
 */
export function showConfirm(title, message, confirmText, cancelText) {
  return originalShowConfirm(title, message, confirmText, cancelText);
}

// =========================== 공통 에러 처리 함수 ===========================

/**
 * 공통 에러 처리 함수 - 토스트 표시 포함
 */
export function handleError(error, options = {}) {
  const {
    showToast: shouldShowToast = true,
    errorPrefix = '',
    customMessage = null,
    redirectOn401 = true
  } = options;

  console.error('[API Error]', error);

  // 커스텀 메시지가 있으면 우선 사용
  let message = customMessage || extractErrorMessage(error);

  // 에러 접두사 추가
  if (errorPrefix) {
    message = `${errorPrefix}: ${message}`;
  }

  // 토스트 표시
  if (shouldShowToast) {
    showToast(message, 'error');
  }

  // 401 에러 시 로그인 페이지로 리다이렉트
  if (redirectOn401 && (
    (error instanceof FetchError && error.httpStatus === 401) ||
    (error.response && error.response.status === 401) ||
    (error.status === 401)
  )) {
    setTimeout(() => {
      const currentPath = window.location.pathname;
      if (currentPath.startsWith('/admin')) {
        window.location.href = '/admin/view/login?reason=session_expired';
      } else {
        window.location.href = '/mobile/login?reason=session_expired';
      }
    }, 1500); // 토스트가 보일 시간을 준 후 리다이렉트
  }

  return message;
}

// =========================== 나머지 기존 코드들 (변경 없음) ===========================

/**
 * 응답 처리 및 에러 검사
 */
async function processResponse(res) {
  if (res.ok) {
    // Content-Type 확인 후 적절히 파싱
    const contentType = res.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return await res.json();  // ← JSON 파싱해서 리턴
    }
    return await res.text();
  }

  let body = null;
  try {
    const contentType = res.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      body = await res.json();
    } else {
      body = { message: await res.text() };
    }
  } catch {
    body = { message: '서버 응답을 처리할 수 없습니다.' };
  }

  const code = body?.status?.code;
  const msg = body?.status?.message || body?.message || res.statusText;
  throw new FetchError(res.status, code, msg, body);
}

/**
 * 최종 캐치용 에러 핸들러 (레거시 호환)
 */
export function handleFetchError(error, options = {}) {
  return handleError(error, options);
}

/**
 * 로그인 상태 확인 및 리다이렉트
 */
export async function checkAuthAndRedirect(redirectUrl, validatePath = '/api/auth/validate') {
  try {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      console.warn('토큰이 없어 인증 확인을 건너뜁니다.');
      return;
    }

    const res = await fetch(validatePath, {
      method: 'GET',
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (res.status === 204) {
      window.location.replace(redirectUrl);
    }
  } catch (err) {
    console.error('Auth check failed:', err);
  }
}

// =========================== 로그인 상태 확인 유틸리티 ===========================

/**
 * 로그인 상태 확인 (JWT 토큰 기반)
 */
export function checkLoginStatus() {
  const accessToken = localStorage.getItem('accessToken');
  const refreshToken = localStorage.getItem('refreshToken');

  if (!accessToken || !refreshToken) {
    console.warn('JWT 토큰이 없습니다.');
    return false;
  }

  // 토큰 만료 시간 체크 (JWT payload 디코딩)
  try {
    const payload = JSON.parse(atob(accessToken.split('.')[1]));
    const now = Math.floor(Date.now() / 1000);

    if (payload.exp && payload.exp < now) {
      console.warn('Access Token이 만료되었습니다.');
      return false;
    }

    console.log('JWT 토큰 유효:', {
      memberId: payload.sub,
      role: payload.role,
      expiresAt: new Date(payload.exp * 1000)
    });

    return true;
  } catch (error) {
    console.error('JWT 토큰 검증 오류:', error);
    return false;
  }
}

// =========================== 회원용 API 함수 ===========================

/**
 * 회원 토큰 갱신
 */
export async function handleTokenRefresh() {
  const refreshToken = localStorage.getItem('refreshToken');

  if (!refreshToken) {
    await performCompleteTokenCleanup();
    throw new FetchError(401, null, '리프레시 토큰이 없습니다.', null);
  }

  try {
    const res = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
      credentials: 'include'
    });

    if (!res.ok) {
      await performCompleteTokenCleanup();

      if (res.status === 401 || res.status === 403) {
        window.location.href = '/mobile/login?reason=session_expired';
      }

      throw new FetchError(res.status, null, '토큰 갱신 실패', null);
    }

    const data = await res.json();

    if (data.status?.code === 'OK_0000' && data.response) {
      localStorage.setItem('accessToken', data.response.accessToken);
      localStorage.setItem('refreshToken', data.response.refreshToken);

      console.log('토큰 갱신 성공');
      return data.response;

    } else {
      throw new FetchError(res.status, data.status?.code, data.status?.message, data);
    }

  } catch (error) {
    console.error('토큰 갱신 중 오류:', error);
    await performCompleteTokenCleanup();
    throw error;
  }
}

async function performCompleteTokenCleanup() {
  console.log('🗑️ 모든 토큰 정리 시작');

  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');

  window.dispatchEvent(new CustomEvent('tokenCleared', {
    detail: { reason: 'cleanup', timestamp: new Date().toISOString() }
  }));

  console.log('✅ 모든 토큰 정리 완료');
}

/**
 * 인증이 필수인 회원 API 호출용
 */
export async function authFetch(url, options = {}) {
  const token = localStorage.getItem('accessToken');
  if (!token) {
    throw new FetchError(401, null, '로그인이 필요합니다.', null);
  }

  let headers = {
    ...(options.body instanceof FormData ? {} : { 'Content-Type': options.contentType || 'application/json' }),
    'Authorization': `Bearer ${token}`,
    ...(options.headers || {})
  };

  // 1차 호출
  let res = await fetch(url, {
    method: options.method || 'GET',
    headers,
    credentials: 'include',
    body: options.body
  });

  // 토큰 만료 시 갱신 후 재시도
  if (res.status === 401) {
    try {
      await handleTokenRefresh();
      const newToken = localStorage.getItem('accessToken');
      headers.Authorization = `Bearer ${newToken}`;

      res = await fetch(url, {
        method: options.method || 'GET',
        headers,
        credentials: 'include',
        body: options.body
      });
    } catch (refreshError) {
      console.error('토큰 갱신 실패:', refreshError);
      throw refreshError;
    }
  }

  return processResponse(res);
}

/**
 * 인증이 선택인 회원 API 호출용
 */
export async function optionalAuthFetch(url, options = {}) {
  const token = localStorage.getItem('accessToken');

  if (token) {
    try {
      return await authFetch(url, options);
    } catch (err) {
      if (err instanceof FetchError && err.httpStatus === 401) {
        console.warn('토큰 없이 재시도:', err);
      } else {
        throw err;
      }
    }
  }

  // 토큰 없이 호출
  const headers = {
    ...(options.body instanceof FormData ? {} : { 'Content-Type': options.contentType || 'application/json' }),
    ...(options.headers || {})
  };

  const res = await fetch(url, {
    method: options.method || 'GET',
    headers,
    credentials: 'include',
    body: options.body
  });

  return processResponse(res);
}

/**
 * 현재 로그인된 회원 ID 조회
 */
export async function getUserId() {
  const accessToken = localStorage.getItem('accessToken');
  const refreshToken = localStorage.getItem('refreshToken');

  if (!accessToken || !refreshToken) {
    return 0;
  }

  try {
    const response = await authFetch('/api/auth/me');

    if (response.status?.code === 'OK_0000' && response.response?.id) {
      return response.response.id;
    } else {
      console.error('getUserId: 잘못된 응답 포맷', response);
      return 0;
    }
  } catch (err) {
    console.error('getUserId 오류:', err);
    return 0;
  }
}

/**
 * 현재 로그인된 회원 정보 조회
 */
export async function getUserInfo() {
  const accessToken = localStorage.getItem('accessToken');
  const refreshToken = localStorage.getItem('refreshToken');

  if (!accessToken || !refreshToken) {
    return null;
  }

  try {
    const response = await authFetch('/api/auth/me');

    if (response.status?.code === 'OK_0000' && response.response) {
      return response.response;
    } else {
      console.error('getUserInfo: 잘못된 응답 포맷', response);
      return null;
    }
  } catch (err) {
    console.error('getUserInfo 오류:', err);
    return null;
  }
}

// =========================== 관리자용 API 함수 ===========================

/**
 * 관리자 토큰 갱신
 */
export async function adminHandleTokenRefresh() {
  const refreshToken = localStorage.getItem('adminRefreshToken');

  if (!refreshToken) {
    throw new FetchError(401, null, '관리자 리프레시 토큰이 없습니다.', null);
  }

  const res = await fetch('/admin/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
    credentials: 'include'
  });

  if (!res.ok) {
    localStorage.clear();
    window.location.href = '/admin/view/login';
    throw new FetchError(
        res.status,
        null,
        '관리자 세션이 만료되었습니다. 다시 로그인 해주세요.',
        null
    );
  }

  const data = await res.json();

  if (data.status?.code === 'OK_0000' && data.response) {
    localStorage.setItem('adminAccessToken', data.response.accessToken);
    localStorage.setItem('adminRefreshToken', data.response.refreshToken);
    console.log('관리자 토큰 갱신 완료');
    return data.response;
  } else {
    throw new FetchError(res.status, data.status?.code, data.status?.message, data);
  }
}

/**
 * 관리자 인증 API 호출용
 */
export async function adminAuthFetch(url, options = {}) {
  const token = localStorage.getItem('adminAccessToken');
  if (!token) {
    throw new FetchError(401, null, '관리자 인증이 필요합니다.', null);
  }

  let headers = {
    ...(options.body instanceof FormData
            ? {}
            : { 'Content-Type': options.contentType || 'application/json' }
    ),
    'Authorization': `Bearer ${token}`,
    ...(options.headers || {})
  };

  // 1차 호출
  let res = await fetch(url, {
    method: options.method || 'GET',
    headers,
    credentials: 'include',
    body: options.body
  });

  // 토큰 만료 시 갱신 후 재시도
  if (res.status === 401 || res.status === 403) {
    try {
      await adminHandleTokenRefresh();
      const newToken = localStorage.getItem('adminAccessToken');
      headers.Authorization = `Bearer ${newToken}`;

      res = await fetch(url, {
        method: options.method || 'GET',
        headers,
        credentials: 'include',
        body: options.body
      });
    } catch (refreshError) {
      console.error('관리자 토큰 갱신 실패:', refreshError);
      throw refreshError;
    }
  }

  return processResponse(res);
}

/**
 * 관리자 로그인 상태 확인
 */
export async function checkAdminAuth(redirectUrl = '/admin/view/dashboard') {
  try {
    const token = localStorage.getItem('adminAccessToken');
    const res = await fetch('/admin/api/auth/me', {
      method: 'GET',
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (res.ok) {
      window.location.replace(redirectUrl);
    }
  } catch (err) {
    console.error('Admin auth check failed:', err);
  }
}

/**
 * 현재 로그인된 관리자 정보 조회
 */
export async function getAdminInfo() {
  const accessToken = localStorage.getItem('adminAccessToken');
  const refreshToken = localStorage.getItem('adminRefreshToken');

  if (!accessToken || !refreshToken) {
    return null;
  }

  try {
    const response = await adminAuthFetch('/admin/api/auth/me');

    if (response.status?.code === 'OK_0000' && response.response) {
      return response.response;
    } else {
      console.error('getAdminInfo: 잘못된 응답 포맷', response);
      return null;
    }
  } catch (err) {
    console.error('getAdminInfo 오류:', err);
    return null;
  }
}

// =========================== 통합 로그인/로그아웃 함수 ===========================

/**
 * 회원 로그인
 */
export async function memberLogin(phoneNumber, password, autoLogin = false) {
  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ phoneNumber, password, autoLogin })
    });

    const data = await response.json();

    if (data.status?.code === 'OK_0000' && data.response) {
      localStorage.setItem('accessToken', data.response.accessToken);
      localStorage.setItem('refreshToken', data.response.refreshToken);

      console.log('회원 로그인 성공:', data.response);
      return { success: true, data: data.response };
    } else {
      return {
        success: false,
        error: data.status?.message || '로그인에 실패했습니다.',
        errorCode: data.status?.code
      };
    }
  } catch (error) {
    console.error('회원 로그인 오류:', error);
    return {
      success: false,
      error: extractErrorMessage(error)
    };
  }
}

/**
 * 관리자 로그인
 */
export async function adminLogin(username, password, autoLogin = false) {
  try {
    const response = await fetch('/admin/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ username, password, autoLogin })
    });

    const data = await response.json();

    if (data.status?.code === 'OK_0000' && data.response) {
      localStorage.setItem('adminAccessToken', data.response.accessToken);
      localStorage.setItem('adminRefreshToken', data.response.refreshToken);

      console.log('관리자 로그인 성공:', data.response);
      return { success: true, data: data.response };
    } else {
      return {
        success: false,
        error: data.status?.message || '로그인에 실패했습니다.',
        errorCode: data.status?.code
      };
    }
  } catch (error) {
    console.error('관리자 로그인 오류:', error);
    return {
      success: false,
      error: extractErrorMessage(error)
    };
  }
}

/**
 * 회원 로그아웃
 */
export async function memberLogout() {
  try {
    // 서버에 로그아웃 요청 (토큰 무효화)
    try {
      await authFetch('/api/auth/logout', { method: 'POST' });
    } catch (error) {
      console.error('회원 로그아웃 API 오류:', error);
      // API 오류가 있어도 로컬 정리는 진행
    }
  } finally {
    // 로컬 저장소 정리
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');

    // 쿠키 정리 시도 (클라이언트에서 가능한 범위)
    document.cookie = 'MEMBER_ACCESS_TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
    document.cookie = 'MEMBER_REFRESH_TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';

    console.log('회원 로그아웃 완료');

    // 로그인 페이지로 리다이렉트
    window.location.href = '/mobile/login';
  }
}

/**
 * 관리자 로그아웃
 */
export async function adminLogout() {
  try {
    // 서버에 로그아웃 요청 (토큰 무효화)
    try {
      await adminAuthFetch('/admin/api/auth/logout', { method: 'POST' });
    } catch (error) {
      console.error('관리자 로그아웃 API 오류:', error);
      // API 오류가 있어도 로컬 정리는 진행
    }
  } finally {
    // 로컬 저장소 정리
    localStorage.removeItem('adminAccessToken');
    localStorage.removeItem('adminRefreshToken');

    // 쿠키 정리 시도
    document.cookie = 'ADMIN_ACCESS_TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
    document.cookie = 'ADMIN_REFRESH_TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';

    console.log('관리자 로그아웃 완료');

    // 관리자 로그인 페이지로 리다이렉트
    window.location.href = '/admin/view/login';
  }
}

// =========================== API 래퍼 함수들 ===========================

/**
 * API 요청 래퍼 - 자동 에러 처리 포함
 */
export async function apiCall(fetchFunction, options = {}) {
  const {
    showLoadingToast = false,
    loadingMessage = '처리 중...',
    showSuccessToast = false,
    successMessage = '',
    showErrorToast = true,
    errorPrefix = '',
    customErrorMessage = null,
    redirectOn401 = true
  } = options;

  try {
    if (showLoadingToast) {
      showInfo(loadingMessage);
    }

    const result = await fetchFunction();

    if (showSuccessToast && successMessage) {
      showSuccess(successMessage);
    }

    return result;

  } catch (error) {
    if (showErrorToast) {
      handleError(error, {
        showToast: true,
        errorPrefix,
        customMessage: customErrorMessage,
        redirectOn401
      });
    }
    throw error;
  }
}

/**
 * GET 요청 래퍼
 */
export async function apiGet(url, options = {}) {
  return apiCall(() => authFetch(url), options);
}

/**
 * POST 요청 래퍼
 */
export async function apiPost(url, data, options = {}) {
  return apiCall(() => authFetch(url, {
    method: 'POST',
    body: JSON.stringify(data)
  }), options);
}

/**
 * PUT 요청 래퍼
 */
export async function apiPut(url, data, options = {}) {
  return apiCall(() => authFetch(url, {
    method: 'PUT',
    body: JSON.stringify(data)
  }), options);
}

/**
 * DELETE 요청 래퍼
 */
export async function apiDelete(url, options = {}) {
  return apiCall(() => authFetch(url, {
    method: 'DELETE'
  }), options);
}

// =========================== 토큰 동기화 유틸리티 ===========================

/**
 * 페이지 로드 시 토큰 동기화 (모바일 뷰용)
 * 서버에서 갱신된 토큰을 localStorage와 동기화
 */
export function syncTokensFromPage() {
  // 메타 태그에서 새 토큰 정보 확인
  const newAccessToken = document.querySelector('meta[name="new-access-token"]')?.content;
  const newRefreshToken = document.querySelector('meta[name="new-refresh-token"]')?.content;

  if (newAccessToken && newRefreshToken) {
    localStorage.setItem('accessToken', newAccessToken);
    localStorage.setItem('refreshToken', newRefreshToken);
    console.log('페이지 로드 시 토큰 동기화 완료');

    // 메타 태그 제거 (보안)
    document.querySelector('meta[name="new-access-token"]')?.remove();
    document.querySelector('meta[name="new-refresh-token"]')?.remove();

    // 토큰 동기화 이벤트 발생
    window.dispatchEvent(new CustomEvent('tokenSynced', {
      detail: {
        accessToken: newAccessToken,
        refreshToken: newRefreshToken,
        syncedAt: new Date().toISOString()
      }
    }));
  }
}

/**
 * 네트워크 상태 확인
 */
export function isNetworkAvailable() {
  return navigator.onLine;
}

/**
 * API 요청 재시도 로직
 */
export async function retryFetch(fetchFunction, maxRetries = 3, delay = 1000) {
  let lastError;

  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fetchFunction();
    } catch (error) {
      lastError = error;

      if (i < maxRetries - 1) {
        console.warn(`API 요청 실패, ${delay}ms 후 재시도 (${i + 1}/${maxRetries}):`, error);
        await new Promise(resolve => setTimeout(resolve, delay));
        delay *= 2; // 지수 백오프
      }
    }
  }

  throw lastError;
}

/**
 * DOMContentLoaded 이벤트에서 자동 동기화 설정
 */
if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', syncTokensFromPage);
}