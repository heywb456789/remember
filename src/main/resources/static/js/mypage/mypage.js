// 마이페이지 스크립트
import {authFetch, handleFetchError, FetchError} from "../commonFetch.js";

document.addEventListener('DOMContentLoaded', function () {
  // 로그인 상태 확인
  checkLoginStatus();
  // 프로필 이미지 수정 기능
  initProfileImageEdit();
  // 사용자 이름 수정 기능
  initNameEdit();
  // 로그아웃 기능
  initLogout();
  // 계정 연동 기능
  initAccountLinking();
  // 회원 탈퇴 기능
  initAccountDeletion();
});

// 로그인 상태 확인
async function checkLoginStatus() {
  try {
    // 인증 상태 확인 API 호출 (authFetch 사용)
    await authFetch('/api/auth/validate', {method: 'GET'});
    // 인증 성공 시 활동 내역 데이터 로드
    fetchUserInfo();
  } catch (err) {
    if (err instanceof FetchError && err.httpStatus === 401) {
      alert('로그인이 필요합니다.');
      window.location.href = '/login/login.html';
    } else {
      handleFetchError(err);
    }
  }
}

// 사용자 정보 불러오기
async function fetchUserInfo() {
  try {
    const response = await authFetch('/api/auth/me');  // 기존 response.ok 체크 제거
    const data = await response.json();

    if (data.status?.code === 'OK_0000' && data.response) {
      displayUserInfo(data.response);
    } else {
      throw new Error('잘못된 응답 형식');
    }
  } catch (err) {
    if (err instanceof FetchError && err.httpStatus === 401) {
      alert('로그인이 필요합니다.');
      window.location.href = '/login/login.html';
    } else {
      handleFetchError(err);
    }
  }
}

// 사용자 정보 표시
function displayUserInfo(userInfo) {
  const userNameElement = document.getElementById('userName');

  // 사용자 이름
  const userName = userInfo.name || '사용자';
  userNameElement.textContent = userName;

  // 원래 이름으로 저장 (편집 취소 시 사용)
  userNameElement.setAttribute('data-original-name', userName);

  // 회원 등급 (role 값에 따라 표시)
  let userLevel = '비활성 회원';
  if (userInfo.role === 'USER_ACTIVE') {
    if(userInfo.status==='ACTIVE'){
      userLevel = '일반회원';
    }else{
      userLevel ='비활성 회원'
    }
  } else if (userInfo.role === 'ADMIN') {
    userLevel = '관리자';
  }
  document.querySelector('.profile-level').textContent = userLevel;

  // 프로필 이미지
  if (userInfo.profileImg && userInfo.profileImg.trim() !== '') {
    const img = document.createElement('img');
    img.src = userInfo.profileImg;
    img.alt = '프로필 이미지';

    const profileImageContainer = document.getElementById('profileImage');
    profileImageContainer.innerHTML = '';
    profileImageContainer.appendChild(img);
  }

  // SNS 연동 상태 (현재는 구현 전이므로 기본값으로 처리)
  const linkedAccounts = {
    twitter: false,
    dcinside: false
  };

  // 나중에 snsLinks가 구현되면 아래 주석 해제
  if (userInfo.twitterConnected) {
    linkedAccounts.twitter = true;
  }


  updateAccountLinkStatus(linkedAccounts);
}

// 프로필 이미지 수정 초기화
function initProfileImageEdit() {
  const profileImageEdit = document.getElementById('profileImageEdit');
  const profileImageInput = document.getElementById('profileImageInput');

  profileImageEdit.addEventListener('click', function () {
    profileImageInput.click();
  });

  profileImageInput.addEventListener('change', function (event) {
    if (event.target.files && event.target.files[0]) {
      const file = event.target.files[0];
      const reader = new FileReader();

      reader.onload = function (e) {
        const img = document.createElement('img');
        img.src = e.target.result;
        img.alt = '프로필 이미지';

        const profileImageContainer = document.getElementById('profileImage');
        profileImageContainer.innerHTML = '';
        profileImageContainer.appendChild(img);

        // 프로필 이미지 업로드 API 호출
        uploadProfileImage(file);
      };

      reader.readAsDataURL(file);
    }
  });
}

// 프로필 이미지 업로드 (서버 API 호출)
async function uploadProfileImage(file) {
  try {
    const formData = new FormData();
    formData.append('file', file);

    const response = await authFetch('/api/members/profile-image', {
      method: 'PUT',
      body: formData
    });
    await response.json();
    alert('프로필 이미지가 변경되었습니다.');
  } catch (err) {
    if (err instanceof FetchError && err.httpStatus === 401) {
      alert('로그인이 필요합니다.');
      window.location.href = '/login/login.html';
    } else {
      handleFetchError(err);
      alert('프로필 이미지 변경에 실패했습니다.');
    }
  }
}

function initNameEdit() {
  const editNameBtn = document.getElementById('editNameBtn');
  const cancelEditBtn = document.getElementById('cancelEditBtn');
  const userNameElement = document.getElementById('userName');

  // 원래 이름 저장
  if (userNameElement.textContent.trim() !== '') {
    userNameElement.setAttribute('data-original-name',
        userNameElement.textContent);
  }

  // 수정 버튼 클릭 이벤트
  editNameBtn.addEventListener('click', function () {
    // 현재 텍스트 가져오기
    const currentName = userNameElement.getAttribute('data-original-name')
        || userNameElement.textContent;
    console.log("편집 버튼 클릭, 현재 이름:", currentName);

    // 편집 모드인지 확인
    const isEditing = userNameElement.classList.contains('editing');

    if (!isEditing) {
      // 편집 모드로 전환
      startEditMode();
    } else {
      // 저장 모드 (이름 업데이트)
      const inputField = document.getElementById('userNameInput');
      if (!inputField) {
        console.error("입력 필드를 찾을 수 없음");
        return;
      }

      const newName = inputField.value.trim();

      if (newName && newName !== currentName) {
        // 새 이름으로 업데이트 API 호출
        updateUserName(newName);
      } else if (!newName) {
        // 이름이 비어있으면 경고
        alert('이름을 입력해주세요.');
        inputField.focus();
      } else {
        // 이름이 변경되지 않았으면 편집 모드 종료
        cancelNameEdit();
      }
    }
  });

  // 취소 버튼 클릭 이벤트
  cancelEditBtn.addEventListener('click', function () {
    console.log("취소 버튼 클릭");
    cancelNameEdit();
  });
}

function cancelNameEdit() {
  const userNameElement = document.getElementById('userName');
  const editNameBtn = document.getElementById('editNameBtn');
  const cancelEditBtn = document.getElementById('cancelEditBtn');

  if (!userNameElement || !editNameBtn || !cancelEditBtn) {
    console.error("필수 요소를 찾을 수 없음");
    return;
  }

  // 원래 이름 가져오기
  const originalName = userNameElement.getAttribute('data-original-name') || '';

  // 편집 모드 클래스 제거
  userNameElement.classList.remove('editing');

  // 원래 이름으로 복원
  userNameElement.innerHTML = '';
  userNameElement.textContent = originalName;

  // 수정 버튼 원상복구
  editNameBtn.innerHTML = '';
  const penIcon = document.createElement('i');
  penIcon.className = 'fa-solid fa-pen';
  editNameBtn.appendChild(penIcon);
  editNameBtn.classList.remove('save-btn');

  // 취소 버튼 숨기기
  cancelEditBtn.style.display = 'none';

  console.log("이름 편집 취소됨");
}

// 편집 버튼 클릭 핸들러 (함수 분리)
function handleEditButtonClick() {
  const userNameElement = document.getElementById('userName');
  const editNameBtn = document.getElementById('editNameBtn');

  if (!userNameElement || !editNameBtn) {
    console.error("필수 요소를 찾을 수 없음");
    return;
  }

  // 편집 모드인지 확인
  if (userNameElement.querySelector('input')) {
    // 이미 편집 모드인 경우 (저장 처리)
    const inputField = userNameElement.querySelector('input');
    const newName = inputField.value.trim();
    const currentName = userNameElement.getAttribute('data-original-name')
        || '';

    if (newName && newName !== currentName) {
      updateUserName(newName);
    } else {
      restoreOriginalName();
    }
  } else {
    // 편집 모드가 아닌 경우 (편집 시작)
    startEditMode();
  }
}


// 사용자 이름 업데이트 (서버 API 호출)
async function updateUserName(newName) {
  try {
    const userNameElement = document.getElementById('userName');
    const inputField = document.getElementById('userNameInput');
    inputField.disabled = true;

    const response = await authFetch('/api/members/name', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: newName })
    });
    const data = await response.json();

    if (data.status?.code === 'OK_0000') {
      cancelNameEdit();
      document.getElementById('userName').textContent = newName;
      document.getElementById('userName').setAttribute('data-original-name', newName);
      alert('이름이 변경되었습니다.');
    } else {
      throw new Error('서버 응답 오류');
    }
  } catch (err) {
    if (err instanceof FetchError && err.httpStatus === 401) {
      alert('로그인이 필요합니다.');
      window.location.href = '/login/login.html';
    } else {
      handleFetchError(err);
      document.getElementById('userNameInput').disabled = false;
    }
  }
}

// 편집 모드 시작
function startEditMode() {
  const userNameElement = document.getElementById('userName');
  const editNameBtn = document.getElementById('editNameBtn');
  const cancelEditBtn = document.getElementById('cancelEditBtn');

  if (!userNameElement || !editNameBtn || !cancelEditBtn) {
    console.error("필수 요소를 찾을 수 없음");
    return;
  }

  // 현재 이름 가져오기
  const currentName = userNameElement.getAttribute('data-original-name')
      || userNameElement.textContent;

  // 편집 모드로 전환
  userNameElement.classList.add('editing');

  // 입력 필드 생성 및 추가
  const inputField = document.createElement('input');
  inputField.type = 'text';
  inputField.id = 'userNameInput';
  inputField.className = 'user-name-input';
  inputField.value = currentName;

  // 기존 내용 비우고 입력 필드 추가
  userNameElement.innerHTML = '';
  userNameElement.appendChild(inputField);

  // 수정 버튼 변경
  editNameBtn.innerHTML = '';
  const checkIcon = document.createElement('i');
  checkIcon.className = 'fa-solid fa-check';
  editNameBtn.appendChild(checkIcon);
  editNameBtn.classList.add('save-btn');

  // 취소 버튼 표시
  cancelEditBtn.style.display = 'flex';

  // 입력 필드에 자동 포커스
  inputField.focus();
  inputField.select();

  // 입력 필드에 엔터 키 이벤트 추가
  inputField.addEventListener('keypress', function (event) {
    if (event.key === 'Enter') {
      event.preventDefault();
      const newName = inputField.value.trim();

      if (newName && newName !== currentName) {
        updateUserName(newName);
      } else if (!newName) {
        alert('이름을 입력해주세요.');
        inputField.focus();
      } else {
        cancelNameEdit();
      }
    }
  });
}

// 외부 클릭 처리
function handleOutsideClick(event) {
  const userNameElement = document.getElementById('userName');
  const editNameBtn = document.getElementById('editNameBtn');

  if (!userNameElement || !editNameBtn) {
    return;
  }

  const inputField = userNameElement.querySelector('input');
  if (!inputField) {
    return;
  }

  // 이름 영역이나 편집 버튼이 아닌 영역 클릭 시
  if (!userNameElement.contains(event.target) &&
      !editNameBtn.contains(event.target)) {
    restoreOriginalName();
    document.removeEventListener('click', handleOutsideClick);
  }
}

// 원래 이름으로 복원
function restoreOriginalName() {
  const userNameElement = document.getElementById('userName');
  const editNameBtn = document.getElementById('editNameBtn');

  if (!userNameElement || !editNameBtn) {
    return;
  }

  // 원래 이름 가져오기
  const originalName = userNameElement.getAttribute('data-original-name') || '';

  // 원래 이름으로 복원
  userNameElement.innerHTML = '';
  userNameElement.textContent = originalName;

  // 버튼 원상복구
  editNameBtn.innerHTML = '';
  const icon = document.createElement('i');
  icon.className = 'fa-solid fa-pen';
  editNameBtn.appendChild(icon);

  // 외부 클릭 이벤트 제거
  document.removeEventListener('click', handleOutsideClick);
}

// 로그아웃 초기화
function initLogout() {
  const logoutBtn = document.getElementById('logoutBtn');
  logoutBtn.addEventListener('click', async function () {
    if (!confirm('로그아웃 하시겠습니까?')) return;

    try {
      // 1) API 호출을 기다립니다
      await logoutAPI();

      // 2) 성공 메시지
      alert('로그아웃 되었습니다.');

    } catch (err) {
      // 3) 실패 시에도 토큰은 지워주고 메시지
      console.error(err);
      alert('로그아웃 중 오류가 발생했습니다.');
    } finally {
      // 4) 로컬스토리지에서 토큰 제거
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      // 5) 로그인 페이지로
      window.location.href = '/main/main.html';
    }
  });
}

// 회원 탈퇴 초기화
function initAccountDeletion() {
  const deleteBtn = document.getElementById('deleteBtn');
  if (!deleteBtn) {
    console.error("회원 탈퇴 버튼을 찾을 수 없습니다.");
    return;
  }

  deleteBtn.addEventListener('click', async function() {
    // 첫 번째 확인
    if (!confirm('정말로 회원 탈퇴를 진행하시겠습니까?')) return;

    // 두 번째 확인 (중요성 강조)
    if (!confirm('탈퇴 시 적립한 포인트 및 모든 개인정보 와 활동 내역이 삭제됩니다. <br/> 계속 진행하시겠습니까?')) return;

    try {
      // 버튼 비활성화 및 로딩 상태 표시
      const originalText = deleteBtn.innerHTML;
      deleteBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> 처리 중...';
      deleteBtn.style.pointerEvents = 'none';

      // 회원 탈퇴 API 호출
      await deleteAccount();

      // 성공 메시지
      alert('회원 탈퇴가 완료되었습니다. 그동안 서비스를 이용해 주셔서 감사합니다.');

      // 로컬 스토리지 토큰 삭제
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');

      // 메인 페이지로 이동
      window.location.href = '/main/main.html';
    } catch (err) {
      // 버튼 상태 복원
      deleteBtn.innerHTML = originalText;
      deleteBtn.style.pointerEvents = 'auto';

      // 오류 처리
      if (err instanceof FetchError && err.httpStatus === 401) {
        alert('로그인이 필요합니다.');
        window.location.href = '/login/login.html';
      } else {
        handleFetchError(err);
        alert('회원 탈퇴 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
      }
    }
  });
}

// 회원 탈퇴 API 호출
async function deleteAccount() {
  const res = await authFetch('/api/auth/delete', {
    method: 'POST'
  });

  // 응답 데이터 확인 (필요 시)
  const data = await res.json();
  if (!data.status || data.status.code !== 'OK_0000') {
    throw new Error('회원 탈퇴 처리 실패: ' + (data.status?.message || '알 수 없는 오류'));
  }

  return data;
}

// 로그아웃 API 호출
async function logoutAPI() {
  // DELETE /api/auth/logout 이 204나 200을 반환한다고 가정
  const res = await authFetch('/api/auth/logout', { method: 'DELETE' });
  return res;
}

// 계정 연동 초기화
function initAccountLinking() {
  const twitterLinkBtn = document.getElementById('twitterLinkBtn');
  const dcinsideLinkBtn = document.getElementById('dcinsideLinkBtn');

  twitterLinkBtn.addEventListener('click', function () {
    // X(트위터) 계정 연동 처리
    const isLinked = twitterLinkBtn.textContent === '연동해제';

    if (isLinked) {
      // 연동 해제 처리
      unlinkAccount('twitter');
    } else {
      // 연동 처리
      linkAccount('twitter');
    }
  });

  dcinsideLinkBtn.addEventListener('click', function () {
    // 디시인사이드 계정 연동 처리
    const isLinked = dcinsideLinkBtn.textContent === '연동해제';

    if (isLinked) {
      // 연동 해제 처리
      unlinkAccount('dcinside');
    } else {
      // 연동 처리
      linkAccount('dcinside');
    }
  });
}

// 계정 연동 상태 업데이트
function updateAccountLinkStatus(linkedAccounts) {
  // 트위터 연동 상태
  const twitterStatus = document.getElementById('twitterLinkStatus');
  const twitterLinkBtn = document.getElementById('twitterLinkBtn');

  if (linkedAccounts.twitter) {
    twitterStatus.textContent = '연동됨';
    twitterStatus.classList.remove('not-linked');
    twitterStatus.classList.add('linked');
    twitterLinkBtn.textContent = '연동해제';
    twitterLinkBtn.classList.add('unlink-btn');
    twitterLinkBtn.classList.remove('link-btn');
  } else {
    twitterStatus.textContent = '연동되지 않음';
    twitterStatus.classList.add('not-linked');
    twitterStatus.classList.remove('linked');
    twitterLinkBtn.textContent = '연동하기';
    twitterLinkBtn.classList.remove('unlink-btn');
    twitterLinkBtn.classList.add('link-btn');
  }

  // 디시인사이드 연동 상태
  const dcinsideStatus = document.getElementById('dcinsideLinkStatus');
  const dcinsideLinkBtn = document.getElementById('dcinsideLinkBtn');

  if (linkedAccounts.dcinside) {
    dcinsideStatus.textContent = '연동됨';
    dcinsideStatus.classList.remove('not-linked');
    dcinsideStatus.classList.add('linked');
    dcinsideLinkBtn.textContent = '연동해제';
    dcinsideLinkBtn.classList.add('unlink-btn');
    dcinsideLinkBtn.classList.remove('link-btn');
  } else {
    dcinsideStatus.textContent = '연동되지 않음';
    dcinsideStatus.classList.add('not-linked');
    dcinsideStatus.classList.remove('linked');
    dcinsideLinkBtn.textContent = '연동하기';
    dcinsideLinkBtn.classList.remove('unlink-btn');
    dcinsideLinkBtn.classList.add('link-btn');
  }
}

// 계정 연동 / 해제
async function linkAccount(accountType) {
  try {
    if(accountType==='twitter'){
      await callTwitter();
    }else {
      console.log("dc")
    }
  } catch (err) {
    handleFetchError(err);
    alert('계정 연동에 실패했습니다.');
  }
}

async function callTwitter(){
    try {
      const response = await authFetch(`/twitter/connect`);

      const result =  await response.json();

      window.location.href = result.response.connectUrl;

      return result;
    } catch (err) {
      console.error('오류 발생:', err);
      return null;
    }
}

async function unlinkAccount(accountType) {
  try {
    if(accountType==='twitter'){
      const response = await authFetch(`/twitter/disconnect`,{method: 'POST'});
      const data     = await response.json();
      if(!data.response){
        throw new Error();
      }
    }

    const linkedAccounts = {
      twitter: accountType === 'twitter' ? false : document.getElementById(
          'twitterLinkStatus').classList.contains('linked'),
      dcinside: accountType === 'dcinside' ? false : document.getElementById(
          'dcinsideLinkStatus').classList.contains('linked')
    };

    updateAccountLinkStatus(linkedAccounts);

    alert(`X(구 트위터) 연동 해제되었습니다.`);
  } catch (err) {
    handleFetchError(err);
    alert('계정 연동 해제에 실패했습니다.');
  }
}