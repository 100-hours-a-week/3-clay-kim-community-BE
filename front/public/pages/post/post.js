// 게시글 목록 페이지
import { renderPostCards } from '/components/postCard/postCard.js';

const API_BASE_URL = 'http://localhost:8080';
let currentCursor = null;
let hasNext = false;
const PAGE_SIZE = 10;

// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', () => {
  initPostList();
  attachEventListeners();
});

// 게시글 작성 버튼 이벤트
function attachEventListeners() {
  const btnWrite = document.getElementById('btnWrite');
  btnWrite.addEventListener('click', () => {
    const accessToken = localStorage.getItem('accessToken');
    if (!accessToken) {
      alert('로그인이 필요한 서비스입니다.');
      window.location.href = '/login';
      return;
    }
    window.location.href = '/pages/post/write.html';
  });
}

// 게시글 목록 초기화
async function initPostList() {
  try {
    await fetchPosts();
  } catch (error) {
    console.error('게시글 로딩 실패:', error);
    renderError();
  }
}

// 게시글 목록 불러오기 (cursor 기반)
async function fetchPosts(cursor = null) {
  let url = `${API_BASE_URL}/posts?size=${PAGE_SIZE}`;
  if (cursor) {
    url += `&cursor=${cursor}`;
  }

  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json'
    }
  });

  // const statusResponse = await fetch(`${API_BASE_URL}/posts/statuses`)

  if (!response.ok) {
    throw new Error('게시글을 불러오는데 실패했습니다.');
  }

  const result = await response.json();
  console.log('게시글 데이터:', result);
  
  const data = result.data;
  currentCursor = data.nextCursor;
  hasNext = data.hasNext;
  
  // postCard 컴포넌트 사용
  renderPostCards(data.posts || [], 'postList');
  
  // 페이지네이션 버튼 렌더링
  renderPagination();
}

// 페이지네이션 버튼 렌더링
function renderPagination() {
  const pagination = document.getElementById('pagination');
  
  if (!hasNext) {
    pagination.innerHTML = '';
    return;
  }
  
  pagination.innerHTML = `
    <button class="page-btn" id="btnLoadMore">더 보기</button>
  `;
  
  document.getElementById('btnLoadMore').addEventListener('click', loadMore);
}

// 더 보기 (다음 페이지)
async function loadMore() {
  if (!hasNext || !currentCursor) return;
  
  try {
    const response = await fetch(`${API_BASE_URL}/posts?cursor=${currentCursor}&size=${PAGE_SIZE}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      throw new Error('게시글을 불러오는데 실패했습니다.');
    }

    const result = await response.json();
    const data = result.data;
    
    currentCursor = data.nextCursor;
    hasNext = data.hasNext;
    
    // 기존 게시글 목록에 추가
    appendPosts(data.posts || []);
    
    // 페이지네이션 버튼 업데이트
    renderPagination();
  } catch (error) {
    console.error('게시글 로딩 실패:', error);
    alert('게시글을 불러오는데 실패했습니다.');
  }
}

// 게시글 추가 렌더링
function appendPosts(posts) {
  const postList = document.getElementById('postList');
  
  if (!posts || posts.length === 0) return;
  
  posts.forEach(post => {
    const postCard = document.createElement('div');
    postCard.innerHTML = createPostCardHTML(post);
    const postItem = postCard.firstElementChild;
    
    // 클릭 이벤트 등록
    postItem.addEventListener('click', () => {
      window.location.href = `/pages/post/postDetail.html?id=${post.id}`;
    });
    
    postList.appendChild(postItem);
  });
}

// 게시글 카드 HTML 생성 (postCard 컴포넌트 함수 복사)
function createPostCardHTML(post) {
  return `
    <div class="post-item" data-post-id="${post.id}">
      <div class="post-item-header">
        <div>
          <div class="post-item-title">${escapeHtml(post.title)}</div>
          <div class="post-item-author">${escapeHtml(post.nickname || post.authorEmail || post.author || '익명')}</div>
        </div>
      </div>
      
      <div class="post-item-meta">
        <div class="post-meta-item">
          <span class="icon">👁️</span>
          <span>${post.views || 0}</span>
        </div>
        <div class="post-meta-item">
          <span class="icon">❤️</span>
          <span>${post.likes || 0}</span>
        </div>
        <div class="post-meta-item">
          <span class="icon">💬</span>
          <span>${post.comments || 0}</span>
        </div>
        <div class="post-item-time">${formatTime(post.createdAt)}</div>
      </div>
    </div>
  `;
}

// 시간 포맷팅
function formatTime(timestamp) {
  if (!timestamp) return '방금 전';
  
  const date = new Date(timestamp);
  const now = new Date();
  const diff = Math.floor((now - date) / 1000);

  if (diff < 60) return '방금 전';
  if (diff < 3600) return `${date}분 전`;
//   if (diff < 3600) return `${Math.floor(diff / 60)}분 전`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
  if (diff < 604800) return `${date}`;
  
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
}

// HTML 이스케이프
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// 에러 렌더링
function renderError() {
  const postList = document.getElementById('postList');
  postList.innerHTML = `
    <div class="empty-state">
      <div class="empty-state-icon">⚠️</div>
      <div class="empty-state-text">게시글을 불러오는데 실패했습니다.</div>
      <button class="btn-write" onclick="location.reload()">
        다시 시도
      </button>
    </div>
  `;
}