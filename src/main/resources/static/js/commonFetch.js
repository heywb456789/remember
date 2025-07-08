// commonFetch.js

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

export async function checkAuthAndRedirect(redirectUrl, validatePath = '/api/auth/validate') {
  try {
    const token = localStorage.getItem('accessToken');
    const res = await fetch(validatePath, {
      method: 'GET',
      headers: { 'Authorization': `Bearer ${token}` }  // 토큰 헤더 방식일 때
    });
    // 204 No Content 면 인증 완료 상태
    if (res.status === 204) {
      window.location.replace(redirectUrl);
    }
    // 401 등은 아무 동작 안함 → 로그인 페이지 계속 보여줌
  } catch (err) {
    console.error('Auth check failed:', err);
    // 네트워크 오류도 그냥 무시
  }
}

export async function handleTokenRefresh() {
  const refreshToken = localStorage.getItem('refreshToken');
  const res = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });

  if (!res.ok) {
    localStorage.clear();
    window.location.href = '../login/login.html';
    throw new FetchError(
      res.status,              // httpStatus
      null,                    // statusCode
      '세션이 만료되었습니다. 다시 로그인 해주세요.', // statusMessage
      null                     // responseBody
    );
  }

  const data = await res.json();
  localStorage.setItem('accessToken', data.response.token);
}
//################################
// 1) Response → 에러 검사 및 래핑
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
  const msg  = body?.status?.message || res.statusText;
  throw new FetchError(res.status, code, msg, body);
}

// 2) 최종 캐치용 핸들러
export function handleFetchError(error) {
  if (error instanceof FetchError) {
    console.error('[API Error]', error);
    alert(error.statusMessage);
  } else {
    console.error('[Network/Error]', error);
    alert('네트워크 오류가 발생했습니다.');
  }
}
//##############################
/**
 * 1) 인증이 필수인 API 호출용
 *   - 토큰 없으면 바로 에러
 *   - 토큰 만료시 refresh → 재시도 → 실패면 에러
 */
export async function authFetch(url, options = {}) {
  const token = localStorage.getItem('accessToken');
  if (!token) throw new FetchError(401, null, 'Unauthorized', null);

  // 공통 헤더
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

  // 만료 → 리프레시 → 재시도
  if (res.status === 401) {
    await handleTokenRefresh();  // 기존 구현 유지
    const newToken = localStorage.getItem('accessToken');
    headers.Authorization = `Bearer ${newToken}`;
    res = await fetch(url, { method: options.method||'GET', headers, credentials:'include', body:options.body });
  }

  return processResponse(res);
}

/**
 * 2) 인증이 선택인 API 호출용
 *   - 토큰 있으면 authFetch 흐름
 *   - 토큰 없거나 authFetch 전부 실패 시 (401, refresh 실패) → 토큰 없이 fetch
 */
export async function optionalAuthFetch(url, options = {}) {
  const token = localStorage.getItem('accessToken');
  if (token) {
    try {
      return await authFetch(url, options);
    } catch (err) {
      // 401/리프레시 실패면 토큰 없이 재시도
      if (err instanceof FetchError && err.httpStatus === 401) {
        console.warn('토큰 없이 재시도:', err);
      } else {
        throw err;
      }
    }
  }

  // 토큰 없거나 authFetch 실패
  const res = await fetch(url, {
    method: options.method || 'GET',
    headers: options.headers || {},
    credentials: 'include',
    body: options.body
  });
  return processResponse(res);
}


export async function getUserId() {
  // 1) 로컬스토리지에서 토큰 꺼내기
  const accessToken  = localStorage.getItem('accessToken');
  const refreshToken = localStorage.getItem('refreshToken');

  // 2) 토큰 없으면 비로그인(0) 리턴
  if (!accessToken || !refreshToken) {
    return 0;
  }

  try {
    // 3) authFetch가 자동으로 accessToken 헤더를 붙여줄 경우
    //    (commonFetch.js 내부에 토큰 처리 로직이 없다면,
    //     fetch 직접 쓰시면서 Authorization 헤더 추가하세요)
    const response = await authFetch('/api/auth/me');
    const data     = await response.json();

    // 4) 정상 응답이면 data.response.id 리턴
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


//############# admin ###################

export async function adminAuthFetch(url, options = {}) {
  const token = localStorage.getItem('adminAccessToken');
  if (!token) {
    throw new Error('Unauthorized');
  }
  // 1) 기본 헤더 세팅
  let headers = {
    // JSON 바디일 때만 Content-Type
    ...(options.body instanceof FormData
      ? {}
      : { 'Content-Type': options.contentType || 'application/json' }
    ),
    'Authorization': `Bearer ${token}`,
    // 사용자가 직접 넘긴 추가 헤더
    ...(options.headers || {})
  };

  // 2) 첫 호출
  let res = await fetch(url, {
    method: options.method || 'GET',
    headers,
    credentials: 'include',
    body: options.body
  });

  // 토큰 만료
  if (res.status === 401 || res.status===403) {
    await adminHandleTokenRefresh();            // refresh 시도
    const newToken = localStorage.getItem('adminAccessToken');
    headers.Authorization = `Bearer ${newToken}`;

    res = await fetch(url, {
      method: options.method || 'GET',
      headers,
      credentials: 'include',
      body: options.body
    });
  }

  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }
  return res;
}


export async function adminHandleTokenRefresh() {
  const refreshToken = localStorage.getItem('adminRefreshToken');
  const res = await fetch('/admin/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });

  if (!res.ok) {
    localStorage.clear();
    window.location.href = '/admin/auth/login';
    throw new FetchError(
      res.status,              // httpStatus
      null,                    // statusCode
      '세션이 만료되었습니다. 다시 로그인 해주세요.', // statusMessage
      null                     // responseBody
    );
  }

  const data = await res.json();
  localStorage.setItem('adminAccessToken', data.response.token);
}
