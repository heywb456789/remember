// commonFetch.js - 토마토리멤버 확장 버전

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

// =========================== 공통 유틸리티 ===========================

/**
 * 응답 처리 및 에러 검사
 */
async function processResponse(res) {
  if (res.ok) {
    return res;
  }

  let body = null;
  try {
    body = await res.json();
  } catch {
    // JSON 파싱 실패해도 그냥 넘어감
  }

  const code = body?.status?.code;
  const msg = body?.status?.message || res.statusText;
  throw new FetchError(res.status, code, msg, body);
}

/**
 * 최종 캐치용 에러 핸들러
 */
export function handleFetchError(error) {
  if (error instanceof FetchError) {
    console.error('[API Error]', error);
    alert(error.statusMessage);
  } else {
    console.error('[Network/Error]', error);
    alert('네트워크 오류가 발생했습니다.');
  }
}

/**
 * 로그인 상태 확인 및 리다이렉트
 */
export async function checkAuthAndRedirect(redirectUrl, validatePath = '/api/auth/validate') {
  try {
    const token = localStorage.getItem('accessToken');
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

// =========================== 회원용 API 함수 ===========================

/**
 * 회원 토큰 갱신
 */
export async function handleTokenRefresh() {
  const refreshToken = localStorage.getItem('refreshToken');
  const res = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });

  if (!res.ok) {
    localStorage.clear();
    window.location.href = '/mobile/login';
    throw new FetchError(
      res.status,
      null,
      '세션이 만료되었습니다. 다시 로그인 해주세요.',
      null
    );
  }

  const data = await res.json();
  localStorage.setItem('accessToken', data.response.accessToken);
  localStorage.setItem('refreshToken', data.response.refreshToken);

  console.log('회원 토큰 갱신 완료');
}

/**
 * 인증이 필수인 회원 API 호출용
 */
export async function authFetch(url, options = {}) {
  const token = localStorage.getItem('accessToken');
  if (!token) throw new FetchError(401, null, 'Unauthorized', null);

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
    await handleTokenRefresh();
    const newToken = localStorage.getItem('accessToken');
    headers.Authorization = `Bearer ${newToken}`;
    res = await fetch(url, {
      method: options.method || 'GET',
      headers,
      credentials: 'include',
      body: options.body
    });
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
  const res = await fetch(url, {
    method: options.method || 'GET',
    headers: options.headers || {},
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
    const data = await response.json();

    if (data.status?.code === 'OK_0000' && data.response?.id) {
      return data.response.id;
    } else {
      console.error('getUserId: 잘못된 응답 포맷', data);
      return 0;
    }
  } catch (err) {
    console.error('getUserId 오류:', err);
    return 0;
  }
}

// =========================== 관리자용 API 함수 ===========================

/**
 * 관리자 토큰 갱신
 */
export async function adminHandleTokenRefresh() {
  const refreshToken = localStorage.getItem('adminRefreshToken');
  const res = await fetch('/admin/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
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
  localStorage.setItem('adminAccessToken', data.response.accessToken);
  localStorage.setItem('adminRefreshToken', data.response.refreshToken);

  console.log('관리자 토큰 갱신 완료');
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
    await adminHandleTokenRefresh();
    const newToken = localStorage.getItem('adminAccessToken');
    headers.Authorization = `Bearer ${newToken}`;

    res = await fetch(url, {
      method: options.method || 'GET',
      headers,
      credentials: 'include',
      body: options.body
    });
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
    const data = await response.json();

    if (data.status?.code === 'OK_0000' && data.response) {
      return data.response;
    } else {
      console.error('getAdminInfo: 잘못된 응답 포맷', data);
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
      body: JSON.stringify({ phoneNumber, password, autoLogin })
    });

    const data = await response.json();

    if (data.status?.code === 'OK_0000') {
      localStorage.setItem('accessToken', data.response.accessToken);
      localStorage.setItem('refreshToken', data.response.refreshToken);
      return { success: true, data: data.response };
    } else {
      return { success: false, error: data.status?.message || '로그인 실패' };
    }
  } catch (error) {
    return { success: false, error: '네트워크 오류가 발생했습니다.' };
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
      body: JSON.stringify({ username, password, autoLogin })
    });

    const data = await response.json();

    if (data.status?.code === 'OK_0000') {
      localStorage.setItem('adminAccessToken', data.response.accessToken);
      localStorage.setItem('adminRefreshToken', data.response.refreshToken);
      return { success: true, data: data.response };
    } else {
      return { success: false, error: data.status?.message || '로그인 실패' };
    }
  } catch (error) {
    return { success: false, error: '네트워크 오류가 발생했습니다.' };
  }
}

/**
 * 회원 로그아웃
 */
export async function memberLogout() {
  try {
    // 서버에 로그아웃 요청 (토큰 무효화)
    await authFetch('/api/auth/logout', { method: 'POST' });
  } catch (error) {
    console.error('회원 로그아웃 API 오류:', error);
  } finally {
    // 로컬 저장소 정리
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/mobile/login';
  }
}

/**
 * 관리자 로그아웃
 */
export async function adminLogout() {
  try {
    // 서버에 로그아웃 요청 (토큰 무효화)
    await adminAuthFetch('/admin/api/auth/logout', { method: 'POST' });
  } catch (error) {
    console.error('관리자 로그아웃 API 오류:', error);
  } finally {
    // 로컬 저장소 정리
    localStorage.removeItem('adminAccessToken');
    localStorage.removeItem('adminRefreshToken');
    window.location.href = '/admin/view/login';
  }
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
  }
}

/**
 * DOMContentLoaded 이벤트에서 자동 동기화 설정
 */
if (typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', syncTokensFromPage);
}