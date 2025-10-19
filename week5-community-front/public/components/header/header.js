// Header Component - 함수형 버전

// 로그인 상태 확인
function isLoggedIn() {
  return !!localStorage.getItem('accessToken');
}

// 로그인된 상태 UI
function renderLoggedInUI() {
  const userEmail = localStorage.getItem('userEmail') || '사용자';
  return `
    <div class="header-user-info">
      <span class="header-user-email">${userEmail}</span>
    </div>
    <button class="header-btn header-btn-logout" id="logoutBtn">로그아웃</button>
  `;
}

// 로그아웃 상태 UI
function renderLoggedOutUI() {
  return `
    <a href="/login" class="header-btn header-btn-secondary">로그인</a>
    <a href="/pages/register/register.html" class="header-btn header-btn-primary">회원가입</a>
  `;
}

// 헤더 렌더링
function renderHeader() {
  const headerHTML = `
    <header class="header">
      <div class="header-container">
        <a href="/" class="header-logo"></a>
        <a href="/pages/post/post.html" class="header-logo">아무 말 대잔치</a>

        <div class="header-user">
          ${isLoggedIn() ? renderLoggedInUI() : renderLoggedOutUI()}
        </div>
      </div>
    </header>
  `;

  document.body.insertAdjacentHTML('afterbegin', headerHTML);
}

// 로그아웃 처리
function handleLogout() {
  if (confirm('로그아웃 하시겠습니까?')) {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userEmail');
    
    alert('로그아웃되었습니다.');
    window.location.href = '/';
  }
}

// 이벤트 리스너 등록
function attachEventListeners() {
  const logoutBtn = document.getElementById('logoutBtn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', handleLogout);
  }
}

// 헤더 초기화
function initHeader() {
  renderHeader();
  attachEventListeners();
}

// 페이지 로드 시 헤더 생성
document.addEventListener('DOMContentLoaded', initHeader);

export { initHeader };