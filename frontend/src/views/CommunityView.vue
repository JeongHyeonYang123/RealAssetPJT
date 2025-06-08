<template>
	<div class="community-view">
		<div class="container">
			<!-- 상단 헤더 영역 -->
			<div class="page-header">
				<h1>커뮤니티</h1>
				<p class="page-description">다양한 주제로 자유롭게 소통해보세요</p>
			</div>

			<!-- 검색 및 필터 섹션 -->
			<div class="filter-section">
				<!-- 검색바 -->
				<div class="search-container">
					<div class="search-bar-group">
						<input
							type="text"
							v-model="searchKeyword"
							placeholder="검색어를 입력하세요"
							@keyup.enter="searchPosts"
							class="search-input"
						/>
						<button class="search-btn" @click="searchPosts">
							<i class="fas fa-search"></i> 검색
						</button>
					</div>
					<button
						v-if="memberStore.isLoggedIn"
						class="write-btn"
						@click="goToWritePage"
					>
						<i class="fas fa-pen"></i> 글쓰기
					</button>
				</div>

				<!-- 필터 및 정렬 영역 -->
				<div class="filter-options">
					<!-- 카테고리 필터 -->
					<div class="category-filters">
						<button
							v-for="category in categories"
							:key="category.id"
							:class="[
                                'category-btn',
                                { active: activeCategory === category.id },
                            ]"
							@click="filterByCategory(category.id)"
						>
							{{ category.name }}
						</button>
					</div>

					<!-- 정렬 옵션 -->
					<div class="sort-container">
						<label for="sort-select" class="sort-label">정렬:</label>
						<select
							id="sort-select"
							v-model="sortBy"
							@change="fetchPosts"
							class="sort-select"
						>
							<option value="createdAt">최신순</option>
							<option value="views">조회순</option>
							<option value="likes">좋아요순</option>
							<option value="comments">댓글순</option>
						</select>
					</div>
				</div>
			</div>

			<!-- 로딩 상태 -->
			<div v-if="loading" class="loading-section">
				<div class="loading-spinner"></div>
				<p>게시글을 불러오는 중...</p>
			</div>

			<!-- 게시글 목록 -->
			<div v-else class="posts-list">
				<div v-if="posts.length === 0" class="no-posts">
					<p>게시글이 없습니다.</p>
				</div>
				<article
					v-for="post in posts"
					:key="post.id"
					class="post-card"
					@click="goToPostDetail(post.id)"
				>
					<div class="post-header">
						<h2>{{ post.title }}</h2>
						<div class="post-meta">
							<span class="author">{{ post.authorName }}</span>
							<span class="date">{{
									formatDate(post.createdAt)
								}}</span>
						</div>
					</div>

					<p class="post-excerpt">
						{{ truncateContent(post.content) }}
					</p>

					<!-- 태그 표시 -->
					<div
						v-if="post.tags && post.tags.length > 0"
						class="post-tags"
					>
                        <span v-for="tag in post.tags" :key="tag" class="tag"
						>#{{ tag }}</span
						>
					</div>

					<div class="post-footer">
						<div class="post-stats">
							<span class="views">조회 {{ post.views }}</span>
							<span class="comments"
							>댓글 {{ post.commentsCount }}</span
							>
							<span class="likes">좋아요 {{ post.likes }}</span>
						</div>
						<div class="post-category">{{ post.category }}</div>
					</div>
				</article>
			</div>

			<!-- 페이지네이션 -->
			<div v-if="totalPages > 1" class="pagination">
				<button
					class="page-btn"
					:disabled="currentPage === 0"
					@click="goToPage(currentPage - 1)"
				>
					이전
				</button>

				<button
					v-for="page in visiblePages"
					:key="page"
					:class="['page-btn', { active: page - 1 === currentPage }]"
					@click="goToPage(page - 1)"
				>
					{{ page }}
				</button>

				<button
					class="page-btn"
					:disabled="currentPage === totalPages - 1"
					@click="goToPage(currentPage + 1)"
				>
					다음
				</button>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useMemberStore } from "@/store/member";
import { memberAiNoAuth } from "@/axios";

// PostDto 타입 정의 (백엔드와 일치)
interface PostDto {
	id: number;
	authorMno: number;
	authorName: string;
	category: string;
	title: string;
	content: string;
	tags: string[];
	createdAt: string;
	updatedAt: string;
	views: number;
	likes: number;
	dislikes: number;
	commentsCount: number;
	isDeleted: boolean;
}

const router = useRouter();
const route = useRoute();
const memberStore = useMemberStore();

// 상태 관리
const posts = ref<PostDto[]>([]);
const loading = ref(false);
const searchKeyword = ref("");
const activeCategory = ref("all");
const sortBy = ref("createdAt");
const currentPage = ref(0);
const totalPages = ref(0);
const totalCount = ref(0);
const pageSize = ref(12);

// 카테고리 정의
const categories = [
	{ id: "all", name: "전체" },
	{ id: "시장동향", name: "시장동향" },
	{ id: "아파트후기", name: "아파트후기" },
	{ id: "질문/답변", name: "질문/답변" },
	{ id: "정보공유", name: "정보공유" },
];

// 컴포넌트 마운트 시 데이터 로드
onMounted(() => {
	fetchPosts();
});

// 라우트 변경 감지
watch(
	() => route.fullPath,
	() => {
		fetchPosts();
	}
);

// 카테고리 변경 감지
watch([activeCategory, sortBy], () => {
	currentPage.value = 0;
	fetchPosts();
});

// 게시글 목록 조회
async function fetchPosts() {
	loading.value = true;
	try {
		const params: Record<string, any> = {
			category:
				activeCategory.value === "all"
					? undefined
					: activeCategory.value,
			page: currentPage.value,
			size: pageSize.value,
			sortBy: sortBy.value,
			sortDirection: "DESC",
		};

		// 검색어가 있는 경우 추가
		if (searchKeyword.value.trim()) {
			params.searchKeyword = searchKeyword.value.trim();
		}

		const response = await memberAiNoAuth.get("/api/v1/posts", { params });

		if (response.data.success) {
			posts.value = response.data.data.posts;
			totalPages.value = response.data.data.totalPages;
			totalCount.value = response.data.data.totalCount;
		}
	} catch (error) {
		console.error("게시글 조회 실패:", error);
		alert("게시글을 불러오는데 실패했습니다.");
	} finally {
		loading.value = false;
	}
}

// 카테고리 필터링
function filterByCategory(categoryId: string) {
	activeCategory.value = categoryId;
	searchKeyword.value = ""; // 카테고리 변경 시 검색어 초기화
}

// 검색 실행
function searchPosts() {
	currentPage.value = 0;
	activeCategory.value = "all"; // 검색 시 카테고리 초기화
	fetchPosts();
}

// 페이지 이동
function goToPage(page: number) {
	currentPage.value = page;
	fetchPosts();
}

// 글쓰기 페이지로 이동
function goToWritePage() {
	if (!memberStore.isLoggedIn) {
		alert("로그인이 필요합니다.");
		router.push("/login");
		return;
	}
	router.push("/community/write");
}

// 게시글 상세보기로 이동
function goToPostDetail(postId: number) {
	router.push(`/community/${postId}`);
}

// 날짜 포맷팅
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
		return date.toLocaleDateString("ko-KR", {
			year: "numeric",
			month: "numeric",
			day: "numeric",
		});
	}
}

// 내용 요약
function truncateContent(content: string) {
	if (content.length > 150) {
		return content.substring(0, 150) + "...";
	}
	return content;
}

// 표시할 페이지 번호 계산
const visiblePages = computed(() => {
	const pages = [];
	const start = Math.max(1, currentPage.value - 2);
	const end = Math.min(totalPages.value, start + 4);

	for (let i = start; i <= end; i++) {
		pages.push(i);
	}
	return pages;
});
</script>

<style lang="scss" scoped>
/* 페이지 헤더 스타일 */
.page-header {
	margin-bottom: 2.5rem;
	text-align: center;
	padding: 2rem 0;
	background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
	border-radius: 12px;
	box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
	margin: 0 -1.5rem 2.5rem;
	padding: 2rem 1.5rem;

	h1 {
		font-size: 2.5rem;
		font-weight: 800;
		color: #1e293b;
		margin-bottom: 0.75rem;
		letter-spacing: -0.5px;
	}

	.page-description {
		color: #64748b;
		font-size: 1.125rem;
		max-width: 600px;
		margin: 0 auto;
		line-height: 1.6;
	}
}

/* 필터 섹션 스타일 */
.filter-section {
	background: white;
	border-radius: 12px;
	padding: 1.5rem;
	margin-bottom: 2rem;
	box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
	border: 1px solid #e2e8f0;
	box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
	padding: 1.5rem;
	margin-bottom: 2rem;
}

/* 검색 바 컨테이너 - 한 줄 정렬, 글쓰기 버튼 오른쪽 끝 */
.search-container {
	display: flex;
	flex-direction: row;
	align-items: center;
	justify-content: space-between;
	gap: 1rem;
	margin-bottom: 1.5rem;
}
.search-bar-group {
	display: flex;
	flex: 1;
	max-width: 700px;
	min-width: 400px;
}
.search-input {
	flex: 1;
	height: 42px;
	padding: 0 1rem;
	border: 1px solid #e2e8f0;
	border-radius: 10px 0 0 10px;
	outline: none;
	font-size: 1rem;
	color: #1e293b;
	background: #f8fafc;
	box-sizing: border-box;
	transition: border-color 0.2s, background 0.2s;
	display: flex;
	align-items: center;
}
.search-input:focus {
	border-color: #3b82f6;
	background: #fff;
}
.search-input::placeholder {
	color: #94a3b8;
	font-weight: 400;
}
.search-btn {
	height: 42px;
	padding: 0 1.1rem;
	border: 1px solid #e2e8f0;
	border-left: none;
	border-radius: 0 10px 10px 0;
	background: #3b82f6;
	color: white;
	font-size: 1rem;
	font-weight: 500;
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 0.4rem;
	cursor: pointer;
	transition: background 0.2s;
	box-sizing: border-box;
}
.search-btn:hover {
	background: #2563eb;
}
.search-btn i {
	font-size: 1em;
}
.write-btn {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	gap: 0.4rem;
	padding: 0.8rem 1.1rem;
	height: 42px;
	background: #ef4444;
	color: white;
	border: none;
	border-radius: 8px;
	font-weight: 500;
	cursor: pointer;
	transition: all 0.2s ease;
	white-space: nowrap;
	box-shadow: 0 2px 4px rgba(239, 68, 68, 0.2);
	font-size: 1rem;
}
.write-btn:hover {
	background: #dc2626;
	transform: translateY(-1px);
	box-shadow: 0 4px 6px rgba(239, 68, 68, 0.3);
}
.write-btn i {
	font-size: 1em;
}

/* 필터 옵션 스타일 */
.filter-options {
	display: flex;
	justify-content: space-between;
	align-items: center;
	flex-wrap: wrap;
	gap: 1.5rem;
	padding: 1.25rem 0;
	margin: 1.5rem 0;
	border-top: 1px solid #f1f5f9;
	border-bottom: 1px solid #f1f5f9;
}

/* 카테고리 필터 스타일 */
.category-filters {
	display: flex;
	gap: 0.5rem;
	flex-wrap: wrap;
	align-items: center;
	flex: 1;
	min-width: 0;
	overflow-x: auto;
	padding-bottom: 0.5rem;
	scrollbar-width: none;

	&::-webkit-scrollbar {
		display: none;
	}

	.category-btn {
		padding: 0.6rem 1.5rem;
		background: #f8fafc;
		color: #64748b;
		border: 1px solid #e2e8f0;
		border-radius: 20px;
		font-size: 0.9rem;
		font-weight: 500;
		cursor: pointer;
		transition: all 0.2s ease;
		white-space: nowrap;
		box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

		&:hover {
			background: #f1f5f9;
			color: #334155;
			transform: translateY(-1px);
		}

		&.active {
			background: #e0f2fe;
			color: #0369a1;
			border-color: #bae6fd;
			font-weight: 600;
			box-shadow: 0 1px 3px rgba(2, 132, 199, 0.1);

			&:hover {
				background: #bae6fd;
			}
		}
	}
}

/* 정렬 옵션 스타일 */
.sort-container {
	display: flex;
	align-items: center;
	gap: 0.75rem;
	background: #f8fafc;
	padding: 0.5rem;
	border-radius: 8px;
	margin-left: auto;

	.sort-label {
		font-size: 0.9rem;
		color: #64748b;
		white-space: nowrap;
		font-weight: 500;
	}

	.sort-select {
		padding: 0.6rem 2.5rem 0.6rem 1rem;
		border: 1px solid #e2e8f0;
		border-radius: 6px;
		background: white url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E") no-repeat right 0.75rem center;
		color: #334155;
		font-size: 0.9rem;
		font-weight: 500;
		cursor: pointer;
		appearance: none;
		transition: all 0.2s ease;
		min-width: 120px;
		box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

		&:hover {
			background-color: #f8fafc;
		}

		&:focus {
			outline: none;
			border-color: #3b82f6;
			box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
			background-color: white;
		}
	}
}

.community-view {
	padding: 0 1.5rem 4rem;
	max-width: 1200px;
	margin: 0 auto;

	.container {
		max-width: 100%;
		padding: 0;
	}
}

.loading-section {
	text-align: center;
	padding: 6rem 2rem;
	background: white;
	border-radius: 12px;
	margin: 2rem 0 3rem;
	box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
	border: 1px solid #f1f5f9;

	.loading-spinner {
		width: 56px;
		height: 56px;
		border: 4px solid rgba(99, 102, 241, 0.1);
		border-top-color: #6366f1;
		border-radius: 50%;
		animation: spin 1s linear infinite;
		margin: 0 auto 1.5rem;
		position: relative;

		&::after {
			content: '';
			position: absolute;
			top: -4px;
			left: -4px;
			right: -4px;
			bottom: -4px;
			border: 4px solid transparent;
			border-top-color: #a5b4fc;
			border-radius: 50%;
			animation: spin 1.5s linear infinite;
			opacity: 0.7;
		}
	}

	p {
		color: #64748b;
		font-size: 1.1rem;
		font-weight: 500;
		margin-top: 1rem;
		letter-spacing: 0.3px;
	}
}

@keyframes spin {
	0% {
		transform: rotate(0deg);
	}
	100% {
		transform: rotate(360deg);
	}
}

.posts-list {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
	gap: 1.75rem;
	margin: 2.5rem 0 3rem;
}

.no-posts {
	grid-column: 1 / -1;
	text-align: center;
	padding: 4rem 2rem;
	color: #64748b;
	background: white;
	border-radius: 12px;
	border: 1px dashed #e2e8f0;
	margin: 1rem 0 3rem;
}

.post-card {
	background: white;
	border-radius: 12px;
	overflow: hidden;
	box-shadow: 0 2px 4px rgba(0, 0, 0, 0.03), 0 1px 2px rgba(0, 0, 0, 0.05);
	transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	cursor: pointer;
	border: 1px solid #f1f5f9;
	display: flex;
	flex-direction: column;
	height: 100%;

	&:hover {
		transform: translateY(-4px);
		box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.05);
		border-color: #e2e8f0;
	}

	.post-header {
		padding: 1.5rem 1.75rem 1.25rem;
		border-bottom: 1px solid #f8fafc;
		flex: 1;

		h2 {
			font-size: 1.3rem;
			font-weight: 600;
			color: #1e293b;
			margin-bottom: 1rem;
			line-height: 1.4;
			display: -webkit-box;
			-webkit-line-clamp: 2;
			-webkit-box-orient: vertical;
			overflow: hidden;
			transition: color 0.2s ease;
		}
	}

	.post-meta {
		display: flex;
		align-items: center;
		gap: 1rem;
		font-size: 0.9rem;
		color: #64748b;
		margin-top: 1rem;

		.author {
			font-weight: 500;
			color: #334155;
			display: flex;
			align-items: center;
			gap: 0.5rem;

			&::before {
				content: '';
				display: inline-block;
				width: 6px;
				height: 6px;
				background: #e2e8f0;
				border-radius: 50%;
			}
		}

		.date {
			font-size: 0.85rem;
			color: #94a3b8;
		}
	}

	.post-excerpt {
		padding: 0 1.75rem 1.5rem;
		color: #475569;
		line-height: 1.7;
		font-size: 0.95rem;
		display: -webkit-box;
		-webkit-line-clamp: 3;
		-webkit-box-orient: vertical;
		overflow: hidden;
		margin-bottom: 0.5rem;
	}

	.post-tags {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
		padding: 0 1.75rem 1.25rem;
		margin: 0;

		.tag {
			background: #f8fafc;
			color: #475569;
			padding: 0.35rem 0.9rem;
			border-radius: 100px;
			font-size: 0.8rem;
			font-weight: 500;
			border: 1px solid #e2e8f0;
			transition: all 0.2s ease;

			&:hover {
				background: #f1f5f9;
				color: #334155;
				transform: translateY(-1px);
			}
		}
	}

	.post-footer {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 0.9rem 1.75rem;
		background: #f8fafc;
		margin-top: auto;
		border-top: 1px solid #f1f5f9;
		font-size: 0.9rem;

		.post-stats {
			display: flex;
			gap: 1.5rem;
			color: #64748b;

			span {
				display: flex;
				align-items: center;
				gap: 0.5rem;
				transition: color 0.2s ease;

				i {
					font-size: 1.1rem;
					color: #94a3b8;
					transition: color 0.2s ease;
				}

				&:hover {
					color: #334155;

					i {
						color: #64748b;
					}
				}
			}
		}

		.post-category {
			color: #0369a1;
			font-weight: 500;
			background: #e0f2fe;
			padding: 0.4rem 0.9rem;
			border-radius: 100px;
			font-size: 0.8rem;
			border: 1px solid #bae6fd;
			transition: all 0.2s ease;

			&:hover {
				background: #bae6fd;
				transform: translateY(-1px);
			}
		}
	}
}

.pagination {
	display: flex;
	justify-content: center;
	align-items: center;
	gap: var(--space-2);
	margin-top: var(--space-6);

	.page-btn {
		padding: 8px 12px;
		border: 1px solid var(--color-gray-300);
		background: white;
		color: var(--color-gray-700);
		border-radius: var(--radius-md);
		cursor: pointer;
		transition: all 0.2s ease;

		&:hover:not(:disabled) {
			background: var(--color-gray-50);
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

.btn {
	padding: 10px 20px;
	background: var(--color-primary);
	color: white;
	border: none;
	border-radius: var(--radius-md);
	font-weight: 500;
	cursor: pointer;
	transition: all 0.2s ease;

	&:hover {
		background: var(--color-primary-dark);
	}
}

/* 모바일 반응형 */
@media (max-width: 768px) {
	.search-container {
		flex-direction: column;
		align-items: stretch;
		gap: 1rem;

		.search-bar-group {
			max-width: 100%;
			min-width: auto;
		}

		.write-btn {
			align-self: center;
			width: auto;
		}
	}

	.community-view {
		padding: var(--space-3);
	}

	.posts-list {
		grid-template-columns: 1fr;
		gap: var(--space-3);
	}

	.post-card {
		padding: var(--space-3);

		.post-footer {
			flex-direction: column;
			align-items: flex-start;
			gap: var(--space-2);
		}
	}

	.filter-options {
		flex-direction: column;
		align-items: stretch;
		gap: 1rem;
	}

	.sort-container {
		margin-left: 0;
		justify-content: flex-start;
	}
}
</style>

