<template>
    <div class="my-posts-view">
      <div class="container">
        <!-- 헤더 -->
        <div class="page-header">
          <h1>내가 작성한 글</h1>
          <p class="page-description">작성한 게시글을 한눈에 확인하고 관리하세요</p>
        </div>
  
        <!-- 필터 및 검색 -->
        <div class="filter-section">
          <div class="filter-top">
            <div class="search-bar">
              <input
                type="text"
                v-model="searchKeyword"
                placeholder="제목이나 내용을 검색하세요..."
                @keyup.enter="handleSearch"
              />
              <button class="search-btn" @click="handleSearch">
                <i class="fas fa-search"></i> 검색
              </button>
            </div>
            
            <div class="actions">
              <button class="btn btn-primary" @click="goToWritePage">
                <i class="fas fa-pen"></i> 새 글 작성
              </button>
            </div>
          </div>
  
          <div class="filter-options">
            <!-- 카테고리 필터 (클라이언트 사이드 필터링) -->
            <div class="category-filters">
              <button
                v-for="category in categories"
                :key="category.id"
                :class="['category-btn', { active: activeCategory === category.id }]"
                @click="filterByCategory(category.id)"
              >
                {{ category.name }}
              </button>
            </div>
  
            <!-- 통계 및 정렬 -->
            <div class="stats-and-sort">
              <div class="stats">
                <span class="total-count">
                  {{ filteredPosts.length > 0 ? `${filteredPosts.length}개 표시` : '검색 결과 없음' }}
                  <span class="total-info">(전체 {{ totalCount }}개)</span>
                </span>
              </div>
              <div class="sort-container">
                <select v-model="sortBy" @change="handleSortChange" class="sort-select">
                  <option value="latest">최신순</option>
                  <option value="views">조회순</option>
                  <option value="likes">좋아요순</option>
                  <option value="comments">댓글순</option>
                </select>
              </div>
            </div>
          </div>
        </div>
  
        <!-- 게시글 목록 -->
        <div v-if="loading" class="loading-section">
          <div class="loading-spinner"></div>
          <p>게시글을 불러오는 중...</p>
        </div>
  
        <div v-else class="posts-section">
          <div v-if="filteredPosts.length === 0 && !loading" class="no-posts">
            <div class="no-posts-content">
              <i class="fas fa-edit empty-icon"></i>
              <h3>
                {{ searchKeyword || activeCategory !== 'all' 
                  ? '검색 조건에 맞는 게시글이 없습니다' 
                  : '작성한 게시글이 없습니다' }}
              </h3>
              <p>
                {{ searchKeyword || activeCategory !== 'all' 
                  ? '다른 검색어나 카테고리를 시도해보세요' 
                  : '첫 번째 게시글을 작성해보세요!' }}
              </p>
              <div class="no-posts-actions">
                <button v-if="searchKeyword || activeCategory !== 'all'" 
                        class="btn btn-outline" 
                        @click="clearFilters">
                  필터 초기화
                </button>
                <button class="btn btn-primary" @click="goToWritePage">
                  글 작성하기
                </button>
              </div>
            </div>
          </div>
  
          <div v-else class="posts-list">
            <article
              v-for="post in paginatedPosts"
              :key="post.id"
              class="post-card"
            >
              <div class="post-header">
                <div class="post-meta">
                  <span class="category">{{ post.category }}</span>
                  <span class="date">{{ formatDate(post.createdAt) }}</span>
                </div>
                <div class="post-actions">
                  <button class="action-btn edit" @click="editPost(post.id)">
                    <i class="fas fa-edit"></i> 수정
                  </button>
                  <button class="action-btn delete" @click="deletePost(post.id)">
                    <i class="fas fa-trash"></i> 삭제
                  </button>
                </div>
              </div>
  
              <div class="post-content" @click="goToPostDetail(post.id)">
                <h2 class="post-title">{{ post.title }}</h2>
                <p class="post-excerpt">{{ truncateContent(post.content) }}</p>
                
                <div v-if="post.tags && post.tags.length > 0" class="post-tags">
                  <span v-for="tag in post.tags" :key="tag" class="tag">#{{ tag }}</span>
                </div>
              </div>
  
              <div class="post-footer">
                <div class="post-stats">
                  <span class="stat">
                    <i class="fas fa-eye"></i> {{ post.views || 0 }}
                  </span>
                  <span class="stat">
                    <i class="fas fa-comment"></i> {{ post.commentsCount || 0 }}
                  </span>
                  <span class="stat">
                    <i class="fas fa-heart"></i> {{ post.likes || 0 }}
                  </span>
                </div>
              </div>
            </article>
          </div>
        </div>
  
        <!-- 클라이언트 사이드 페이지네이션 -->
        <div v-if="clientTotalPages > 1" class="pagination">
          <button
            class="page-btn"
            :disabled="clientCurrentPage === 0"
            @click="goToClientPage(clientCurrentPage - 1)"
          >
            이전
          </button>
  
          <button
            v-for="page in visibleClientPages"
            :key="page"
            :class="['page-btn', { active: page - 1 === clientCurrentPage }]"
            @click="goToClientPage(page - 1)"
          >
            {{ page }}
          </button>
  
          <button
            class="page-btn"
            :disabled="clientCurrentPage === clientTotalPages - 1"
            @click="goToClientPage(clientCurrentPage + 1)"
          >
            다음
          </button>
        </div>
  
        <!-- 더 많은 데이터가 있을 때 서버에서 더 로드 -->
        <div v-if="hasMoreData" class="load-more-section">
          <button class="btn btn-outline load-more-btn" @click="loadMorePosts" :disabled="loadingMore">
            <i v-if="loadingMore" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-plus"></i>
            {{ loadingMore ? '로딩 중...' : '더 많은 글 보기' }}
          </button>
        </div>
      </div>
    </div>
  </template>
  
  <script setup lang="ts">
  import { computed, ref, onMounted, watch } from "vue";
  import { useRouter, useRoute } from "vue-router";
  import { useMemberStore } from "@/store/member";
  import { memberAi } from "@/axios";
  
  interface PostDto {
    id: number;
    authorMno: number;
    authorName: string;
    category: string;
    title: string;
    content: string;
    tags: string[];
    createdAt: string;
    views: number;
    likes: number;
    commentsCount: number;
  }
  
  const router = useRouter();
  const route = useRoute();
  const memberStore = useMemberStore();
  
  // 상태 관리
  const allPosts = ref<PostDto[]>([]);  // 서버에서 받아온 모든 게시글
  const loading = ref(false);
  const loadingMore = ref(false);
  const searchKeyword = ref("");
  const activeCategory = ref("all");
  const sortBy = ref("latest");
  
  // 서버 페이징 관련
  const currentPage = ref(0);
  const totalPages = ref(0);
  const totalCount = ref(0);
  const pageSize = ref(20);  // API 기본값
  
  // 클라이언트 페이징 관련 (필터링된 결과에 대해)
  const clientCurrentPage = ref(0);
  const clientPageSize = ref(12);
  
  // 카테고리 정의
  const categories = [
    { id: "all", name: "전체" },
    { id: "시장동향", name: "시장동향" },
    { id: "아파트후기", name: "아파트후기" },
    { id: "질문/답변", name: "질문/답변" },
    { id: "정보공유", name: "정보공유" },
  ];
  
  onMounted(() => {
    fetchMyPosts();
  });
  
  // 라우터 이벤트를 감지하여 페이지 진입 시마다 게시글 목록을 새로 불러옴
  watch(
    () => route.path,
    (newPath) => {
      if (newPath === '/mypage/posts') {
        fetchMyPosts();
      }
    }
  );
  
  // 필터링된 게시글 (검색어, 카테고리 적용)
  const filteredPosts = computed(() => {
    let filtered = [...allPosts.value];
    
    // 카테고리 필터
    if (activeCategory.value !== 'all') {
      filtered = filtered.filter(post => post.category === activeCategory.value);
    }
    
    // 검색어 필터
    if (searchKeyword.value.trim()) {
      const keyword = searchKeyword.value.toLowerCase();
      filtered = filtered.filter(post => 
        post.title.toLowerCase().includes(keyword) ||
        post.content.toLowerCase().includes(keyword)
      );
    }
    
    // 정렬
    filtered.sort((a, b) => {
      switch (sortBy.value) {
        case 'views':
          return (b.views || 0) - (a.views || 0);
        case 'likes':
          return (b.likes || 0) - (a.likes || 0);
        case 'comments':
          return (b.commentsCount || 0) - (a.commentsCount || 0);
        case 'latest':
        default:
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }
    });
    
    return filtered;
  });
  
  // 클라이언트 사이드 페이지네이션
  const paginatedPosts = computed(() => {
    const start = clientCurrentPage.value * clientPageSize.value;
    const end = start + clientPageSize.value;
    return filteredPosts.value.slice(start, end);
  });
  
  const clientTotalPages = computed(() => {
    return Math.ceil(filteredPosts.value.length / clientPageSize.value);
  });
  
  const hasMoreData = computed(() => {
    return currentPage.value < totalPages.value - 1;
  });
  
  const visibleClientPages = computed(() => {
    const pages = [];
    const start = Math.max(1, clientCurrentPage.value - 2);
    const end = Math.min(clientTotalPages.value, start + 4);
  
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  });
  
  // 필터 변경 시 클라이언트 페이지 초기화
  watch([searchKeyword, activeCategory, sortBy], () => {
    clientCurrentPage.value = 0;
  });
  
  // ✅ /my API를 사용한 내 게시글 조회
  async function fetchMyPosts(page = 0, append = false) {
    loading.value = !append;
    loadingMore.value = append;
    
    try {
      if (!memberStore.isLoggedIn) {
        router.push('/login');
        return;
      }
  
      console.log('🔄 내 게시글 조회 API 호출:', `/api/v1/posts/my?page=${page}&size=${pageSize.value}`);
  
      const response = await memberAi.get("/api/v1/posts/my", {
        params: {
          page: page,
          size: pageSize.value
        }
      });
  
      if (response.data.success) {
        const data = response.data.data;
        
        if (append) {
          // 더 보기인 경우 기존 데이터에 추가
          allPosts.value = [...allPosts.value, ...data.posts];
        } else {
          // 새로 조회하는 경우 기존 데이터 교체
          allPosts.value = data.posts || [];
        }
        
        currentPage.value = data.currentPage;
        totalPages.value = data.totalPages;
        totalCount.value = data.totalCount;
        
        console.log('✅ 내 게시글 조회 성공:', {
          받은_게시글_수: data.posts?.length || 0,
          현재_페이지: data.currentPage,
          전체_페이지: data.totalPages,
          전체_게시글_수: data.totalCount
        });
      }
    } catch (error: any) {
      console.error("❌ 내 게시글 조회 실패:", error);
      if (error.response?.status === 401) {
        alert('로그인이 필요합니다.');
        router.push('/login');
      } else {
        alert("게시글을 불러오는데 실패했습니다.");
      }
    } finally {
      loading.value = false;
      loadingMore.value = false;
    }
  }
  
  // 더 많은 게시글 로드
  async function loadMorePosts() {
    if (hasMoreData.value && !loadingMore.value) {
      await fetchMyPosts(currentPage.value + 1, true);
    }
  }
  
  // 필터링 관련 함수들
  function filterByCategory(categoryId: string) {
    activeCategory.value = categoryId;
    clientCurrentPage.value = 0;
  }
  
  function handleSearch() {
    clientCurrentPage.value = 0;
  }
  
  function clearFilters() {
    searchKeyword.value = "";
    activeCategory.value = "all";
    sortBy.value = "latest";
    clientCurrentPage.value = 0;
  }
  
  function handleSortChange() {
    clientCurrentPage.value = 0;
  }
  
  // 클라이언트 페이지네이션
  function goToClientPage(page: number) {
    clientCurrentPage.value = page;
    // 스크롤을 맨 위로
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  
  // 네비게이션 함수들
  function goToWritePage() {
    router.push("/community/write");
  }
  
  function goToPostDetail(postId: number) {
    router.push(`/community/${postId}`);
  }
  
  function editPost(postId: number) {
    router.push(`/community/edit/${postId}`);
  }
  
  async function deletePost(postId: number) {
    if (!confirm("정말로 이 게시글을 삭제하시겠습니까?")) return;
  
    try {
      await memberAi.delete(`/api/v1/posts/${postId}`);
      alert("게시글이 삭제되었습니다.");
      
      // 삭제된 게시글을 로컬에서 제거
      allPosts.value = allPosts.value.filter(post => post.id !== postId);
      totalCount.value -= 1;
      
      // 현재 페이지에 게시글이 없으면 이전 페이지로 이동
      if (paginatedPosts.value.length === 0 && clientCurrentPage.value > 0) {
        clientCurrentPage.value -= 1;
      }
    } catch (error: any) {
      alert(error.response?.data?.message || "삭제에 실패했습니다.");
    }
  }
  
  // 유틸리티 함수들
  function formatDate(dateString: string) {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  
    if (days === 0) {
      const hours = Math.floor(diff / (1000 * 60 * 60));
      if (hours === 0) {
        const minutes = Math.floor(diff / (1000 * 60));
        return `${minutes}분 전`;
      }
      return `${hours}시간 전`;
    } else if (days === 1) {
      return "어제";
    } else if (days < 7) {
      return `${days}일 전`;
    } else {
      return date.toLocaleDateString("ko-KR");
    }
  }
  
  function truncateContent(content: string) {
    if (content && content.length > 100) {
      return content.substring(0, 100) + "...";
    }
    return content || "";
  }
  </script>
  
  <style lang="scss" scoped>
  .my-posts-view {
    padding: var(--space-4) 0;
    max-width: 1200px;
    margin: 0 auto;
  
    .page-header {
      text-align: center;
      margin-bottom: 2rem;
      
      h1 {
        font-size: 2.5rem;
        font-weight: 700;
        color: var(--color-gray-900);
        margin-bottom: 0.5rem;
      }
      
      .page-description {
        color: var(--color-gray-600);
        font-size: 1.1rem;
      }
    }
  }
  
  .filter-section {
    background: white;
    border-radius: 12px;
    padding: 1.5rem;
    margin-bottom: 2rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    border: 1px solid #e2e8f0;
  }
  
  .filter-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
    gap: 1rem;
  
    .search-bar {
      display: flex;
      flex: 1;
      max-width: 400px;
  
      input {
        flex: 1;
        padding: 0.75rem 1rem;
        border: 1px solid #e2e8f0;
        border-radius: 8px 0 0 8px;
        font-size: 0.9rem;
  
        &:focus {
          outline: none;
          border-color: var(--color-primary);
        }
      }
  
      .search-btn {
        padding: 0.75rem 1.5rem;
        background: var(--color-primary);
        color: white;
        border: none;
        border-radius: 0 8px 8px 0;
        cursor: pointer;
        font-size: 0.9rem;
  
        &:hover {
          background: var(--color-primary-dark);
        }
      }
    }
  
    .actions {
      .btn {
        padding: 0.75rem 1.5rem;
        border-radius: 8px;
        font-weight: 500;
        cursor: pointer;
        border: none;
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
  
        &.btn-primary {
          background: var(--color-primary);
          color: white;
  
          &:hover {
            background: var(--color-primary-dark);
          }
        }
      }
    }
  }
  
  .filter-options {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 1rem;
  }
  
  .category-filters {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
  
    .category-btn {
      padding: 0.5rem 1rem;
      background: #f8fafc;
      color: #64748b;
      border: 1px solid #e2e8f0;
      border-radius: 20px;
      font-size: 0.9rem;
      cursor: pointer;
      transition: all 0.2s ease;
  
      &:hover {
        background: #f1f5f9;
      }
  
      &.active {
        background: var(--color-primary);
        color: white;
        border-color: var(--color-primary);
      }
    }
  }
  
  .stats-and-sort {
    display: flex;
    align-items: center;
    gap: 1rem;
  
    .stats {
      .total-count {
        font-size: 0.9rem;
        color: var(--color-gray-600);
        font-weight: 500;
  
        .total-info {
          color: var(--color-gray-500);
          font-weight: 400;
        }
      }
    }
  
    .sort-select {
      padding: 0.5rem 1rem;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      background: white;
      cursor: pointer;
    }
  }
  
  .loading-section {
    text-align: center;
    padding: 4rem 0;
  
    .loading-spinner {
      width: 40px;
      height: 40px;
      border: 3px solid #f0f0f0;
      border-top: 3px solid var(--color-primary);
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 0 auto 1rem;
    }
  }
  
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
  
  .no-posts {
    text-align: center;
    padding: 4rem 2rem;
  
    .no-posts-content {
      .empty-icon {
        font-size: 4rem;
        color: #e2e8f0;
        margin-bottom: 1rem;
      }
  
      h3 {
        color: var(--color-gray-700);
        margin-bottom: 0.5rem;
      }
  
      p {
        color: var(--color-gray-500);
        margin-bottom: 2rem;
      }
  
      .no-posts-actions {
        display: flex;
        gap: 1rem;
        justify-content: center;
        flex-wrap: wrap;
  
        .btn {
          padding: 0.75rem 1.5rem;
          border-radius: 8px;
          font-weight: 500;
          cursor: pointer;
          border: none;
  
          &.btn-primary {
            background: var(--color-primary);
            color: white;
  
            &:hover {
              background: var(--color-primary-dark);
            }
          }
  
          &.btn-outline {
            background: white;
            color: var(--color-gray-700);
            border: 1px solid #e2e8f0;
  
            &:hover {
              background: #f8fafc;
            }
          }
        }
      }
    }
  }
  
  .posts-list {
    display: grid;
    gap: 1.5rem;
  }
  
  .post-card {
    background: white;
    border-radius: 12px;
    padding: 1.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    border: 1px solid #f1f5f9;
    transition: all 0.2s ease;
  
    &:hover {
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      transform: translateY(-1px);
    }
  }
  
  .post-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
  
    .post-meta {
      display: flex;
      align-items: center;
      gap: 1rem;
  
      .category {
        background: var(--color-primary-light);
        color: var(--color-primary);
        padding: 0.25rem 0.75rem;
        border-radius: 12px;
        font-size: 0.8rem;
        font-weight: 500;
      }
  
      .date {
        color: var(--color-gray-500);
        font-size: 0.9rem;
      }
    }
  
    .post-actions {
      display: flex;
      gap: 0.5rem;
  
      .action-btn {
        padding: 0.5rem 1rem;
        border: 1px solid;
        border-radius: 6px;
        font-size: 0.8rem;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        gap: 0.25rem;
  
        &.edit {
          background: white;
          color: var(--color-primary);
          border-color: var(--color-primary);
  
          &:hover {
            background: var(--color-primary-light);
          }
        }
  
        &.delete {
          background: white;
          color: #ef4444;
          border-color: #ef4444;
  
          &:hover {
            background: #fef2f2;
          }
        }
      }
    }
  }
  
  .post-content {
    cursor: pointer;
    margin-bottom: 1rem;
  
    .post-title {
      font-size: 1.25rem;
      font-weight: 600;
      color: var(--color-gray-900);
      margin-bottom: 0.5rem;
      line-height: 1.4;
    }
  
    .post-excerpt {
      color: var(--color-gray-600);
      line-height: 1.6;
      margin-bottom: 1rem;
    }
  
    .post-tags {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
  
      .tag {
        background: #f8fafc;
        color: #64748b;
        padding: 0.25rem 0.5rem;
        border-radius: 12px;
        font-size: 0.8rem;
      }
    }
  }
  
  .post-footer {
    .post-stats {
      display: flex;
      gap: 1rem;
  
      .stat {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        color: var(--color-gray-500);
        font-size: 0.9rem;
  
        i {
          font-size: 0.8rem;
        }
      }
    }
  }
  
  .pagination {
    display: flex;
    justify-content: center;
    gap: 0.5rem;
    margin-top: 2rem;
  
    .page-btn {
      padding: 0.5rem 1rem;
      border: 1px solid #e2e8f0;
      background: white;
      color: var(--color-gray-700);
      border-radius: 6px;
      cursor: pointer;
  
      &:hover:not(:disabled) {
        background: #f8fafc;
      }
  
      &.active {
        background: var(--color-primary);
        color: white;
        border-color: var(--color-primary);
      }
  
      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  }
  
  .load-more-section {
    text-align: center;
    margin-top: 2rem;
    padding-top: 2rem;
    border-top: 1px solid #e2e8f0;
  
    .load-more-btn {
      padding: 1rem 2rem;
      font-size: 1rem;
      
      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
  
      .fa-spinner {
        animation: spin 1s linear infinite;
      }
    }
  }
  
  @media (max-width: 768px) {
    .filter-top {
      flex-direction: column;
      align-items: stretch;
    }
  
    .filter-options {
      flex-direction: column;
      align-items: stretch;
    }
  
    .post-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 1rem;
    }
  
    .post-actions {
      align-self: flex-end;
    }
  
    .no-posts-actions {
      flex-direction: column;
    }
  }
  </style>
  