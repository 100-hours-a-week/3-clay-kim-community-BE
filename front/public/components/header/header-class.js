// Header Component
class Header {
  constructor() {
    this.accessToken = localStorage.getItem('accessToken');
    this.userEmail = localStorage.getItem('userEmail');
    this.render();
    this.attachEventListeners();
  }

  // 로그인 상태 확인
  isLoggedIn() {
    return !!this.accessToken;
  }

  // 헤더 렌더링
  render() {
    const headerHTML = `
      <header class="header">
        <div class="header-container">
          <a href="/pages/post/post.html" class="header-logo"></a>
          <a href="/pages/post/post.html" class="header-logo">아무 말 대잔치</a>
          

          <div class="header-user">
            ${this.isLoggedIn() ? this.renderLoggedIn() : this.renderLoggedOut()}
          </div>
        </div>
      </header>
    `;

    // 헤더 삽입
    document.body.insertAdjacentHTML('afterbegin', headerHTML);
  }

  // 로그인된 상태 UI
  renderLoggedIn() {
    return `
      <div class="header-user-info">
        <span class="header-user-email">${this.userEmail || '사용자'}</span>
      </div>
      <button class="header-btn header-btn-logout" id="logoutBtn">로그아웃</button>
    `;
  }

  // 로그아웃 상태 UI
  renderLoggedOut() {
    return `
      <a href="/login" class="header-btn header-btn-secondary">로그인</a>
      <a href="/pages/register/register.html" class="header-btn header-btn-primary">회원가입</a>
    `;
  }

  // 이벤트 리스너 등록
  attachEventListeners() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => this.handleLogout());
    }
  }

  // 로그아웃 처리
  handleLogout() {
    if (confirm('로그아웃 하시겠습니까?')) {
      // 로컬 스토리지에서 토큰 제거
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userEmail');
      
      alert('로그아웃되었습니다.');
      window.location.href = '/';
    }
  }
}

// 페이지 로드 시 헤더 자동 생성
document.addEventListener('DOMContentLoaded', () => {
  new Header();
});

export default Header;