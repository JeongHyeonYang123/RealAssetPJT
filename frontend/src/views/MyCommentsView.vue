<template>
    <div class="my-comments-view">
        <div class="container">
            <!-- 헤더 -->
            <div class="page-header">
                <h1>내가 작성한 댓글</h1>
                <p class="page-description">
                    작성한 댓글을 한눈에 확인하고 관리하세요
                </p>
            </div>

            <!-- 필터 및 검색 -->
            <div class="filter-section">
                <div class="filter-top">
                    <div class="search-bar">
                        <input
                            type="text"
                            v-model="searchKeyword"
                            placeholder="댓글 내용을 검색하세요..."
                            @keyup.enter="searchComments"
                        />
                        <button class="search-btn" @click="searchComments">
                            <i class="fas fa-search"></i> 검색
                        </button>
                    </div>

                    <div class="stats">
                        <span class="total-count">총 {{ totalCount }}개</span>
                    </div>
                </div>

                <div class="filter-options">
                    <div class="sort-container">
                        <select
                            v-model="sortBy"
                            @change="fetchComments"
                            class="sort-select"
                        >
                            <option value="createdAt">최신순</option>
                            <option value="likes">좋아요순</option>
                        </select>
                    </div>
                </div>
            </div>

            <!-- 댓글 목록 -->
            <div v-if="loading" class="loading-section">
                <div class="loading-spinner"></div>
                <p>댓글을 불러오는 중...</p>
            </div>

            <div v-else class="comments-section">
                <div v-if="comments.length === 0" class="no-comments">
                    <div class="no-comments-content">
                        <i class="fas fa-comment empty-icon"></i>
                        <h3>작성한 댓글이 없습니다</h3>
                        <p>커뮤니티에서 다른 사람들과 소통해보세요!</p>
                        <button class="btn btn-primary" @click="goToCommunity">
                            커뮤니티 둘러보기
                        </button>
                    </div>
                </div>

                <div v-else class="comments-list">
                    <article
                        v-for="comment in comments"
                        :key="comment.id"
                        class="comment-card"
                    >
                        <!-- 댓글이 달린 원본 게시글 정보 -->
                        <div
                            class="original-post"
                            @click="goToPost(comment.postId)"
                        >
                            <div class="post-info">
                                <span class="post-category">{{
                                    comment.postCategory
                                }}</span>
                                <h3 class="post-title">
                                    {{ comment.postTitle }}
                                </h3>
                            </div>
                            <i class="fas fa-external-link-alt"></i>
                        </div>

                        <!-- 댓글 내용 -->
                        <div class="comment-content">
                            <div class="comment-header">
                                <div class="comment-meta">
                                    <span class="date">{{
                                        formatDate(comment.createdAt)
                                    }}</span>
                                </div>
                                <div class="comment-actions">
                                    <button
                                        class="action-btn edit"
                                        @click="editComment(comment)"
                                    >
                                        <i class="fas fa-edit"></i> 수정
                                    </button>
                                    <button
                                        class="action-btn delete"
                                        @click="deleteComment(comment.id)"
                                    >
                                        <i class="fas fa-trash"></i> 삭제
                                    </button>
                                </div>
                            </div>

                            <div class="comment-text">
                                {{ comment.content }}
                            </div>

                            <div class="comment-footer">
                                <div class="comment-stats">
                                    <span class="stat" v-if="comment.likes">
                                        <i class="fas fa-heart"></i>
                                        {{ comment.likes }}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </article>
                </div>
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

        <!-- 댓글 수정 모달 -->
        <div v-if="editingComment" class="modal-overlay" @click="cancelEdit">
            <div class="modal-content" @click.stop>
                <div class="modal-header">
                    <h3>댓글 수정</h3>
                    <button class="close-btn" @click="cancelEdit">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="modal-body">
                    <textarea
                        v-model="editingComment.content"
                        class="edit-textarea"
                        placeholder="댓글을 입력하세요..."
                        rows="4"
                    ></textarea>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-outline" @click="cancelEdit">
                        취소
                    </button>
                    <button class="btn btn-primary" @click="saveComment">
                        저장
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useMemberStore } from "@/store/member";
import { memberAi } from "@/axios";

interface CommentDto {
    id: number;
    content: string;
    authorMno: number;
    authorName: string;
    createdAt: string;
    updatedAt: string;
    postId: number;
    postTitle: string;
    postCategory: string;
    postAuthorMno: number;
    postAuthorName: string;
    likes?: number;
}

const router = useRouter();
const route = useRoute();
const memberStore = useMemberStore();

// 상태 관리
const comments = ref<CommentDto[]>([]);
const loading = ref(false);
const searchKeyword = ref("");
const sortBy = ref("createdAt");
const currentPage = ref(0);
const totalPages = ref(0);
const totalCount = ref(0);
const pageSize = ref(15);

// 편집 관련
const editingComment = ref<CommentDto | null>(null);

onMounted(() => {
    fetchComments();
});

// 라우트가 변경될 때마다(즉, 이 페이지로 다시 진입할 때마다) fetchComments 실행
watch(
    () => route.fullPath,
    () => {
        fetchComments();
    }
);

watch([sortBy], () => {
    currentPage.value = 0;
    fetchComments();
});

// 내 댓글 조회
async function fetchComments() {
    loading.value = true;
    try {
        if (!memberStore.isLoggedIn) {
            router.push("/login");
            return;
        }

        const params = {
            page: currentPage.value,
            size: pageSize.value,
        };

        // 새로운 API 엔드포인트 사용
        const response = await memberAi.get("/api/v1/posts/comments/my", {
            params,
        });

        if (response.data.success) {
            comments.value = response.data.data;

            // 페이지네이션 정보 설정 (백엔드에서 제공하는 경우)
            // totalPages.value = response.data.totalPages;
            // totalCount.value = response.data.totalCount;

            // 임시로 전체 데이터 개수 설정
            totalCount.value = comments.value.length;
            totalPages.value = Math.ceil(totalCount.value / pageSize.value);
        }
    } catch (error: any) {
        console.error("내 댓글 조회 실패:", error);
        if (error.response?.status === 401) {
            alert("로그인이 필요합니다.");
            router.push("/login");
        } else {
            alert(
                error.response?.data?.message ||
                    "댓글을 불러오는데 실패했습니다."
            );
        }
    } finally {
        loading.value = false;
    }
}

function searchComments() {
    currentPage.value = 0;
    fetchComments();
}

function goToPage(page: number) {
    currentPage.value = page;
    fetchComments();
}

function goToCommunity() {
    router.push("/community");
}

function goToPost(postId: number) {
    router.push(`/community/${postId}`);
}

function editComment(comment: CommentDto) {
    editingComment.value = { ...comment };
}

function cancelEdit() {
    editingComment.value = null;
}

async function saveComment() {
    if (!editingComment.value) return;

    try {
        await memberAi.put(
            `/api/v1/posts/comments/${editingComment.value.id}`,
            {
                content: editingComment.value.content,
                postId: editingComment.value.postId,
            }
        );

        alert("댓글이 수정되었습니다.");
        editingComment.value = null;
        fetchComments(); // 목록 새로고침
    } catch (error: any) {
        alert(error.response?.data?.message || "수정에 실패했습니다.");
    }
}

async function deleteComment(commentId: number) {
    if (!confirm("정말로 이 댓글을 삭제하시겠습니까?")) return;

    try {
        await memberAi.delete(`/api/v1/posts/comments/${commentId}`);
        alert("댓글이 삭제되었습니다.");
        fetchComments(); // 목록 새로고침
    } catch (error: any) {
        alert(error.response?.data?.message || "삭제에 실패했습니다.");
    }
}

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
.my-comments-view {
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
    margin-bottom: 1rem;
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

    .stats {
        .total-count {
            font-size: 0.9rem;
            color: var(--color-gray-600);
            font-weight: 500;
        }
    }
}

.filter-options {
    display: flex;
    justify-content: flex-end;

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

.no-comments {
    text-align: center;
    padding: 4rem 2rem;

    .no-comments-content {
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

        .btn {
            padding: 0.75rem 1.5rem;
            background: var(--color-primary);
            color: white;
            border: none;
            border-radius: 8px;
            cursor: pointer;

            &:hover {
                background: var(--color-primary-dark);
            }
        }
    }
}

.comments-list {
    display: grid;
    gap: 1.5rem;
}

.comment-card {
    background: white;
    border-radius: 12px;
    border: 1px solid #f1f5f9;
    overflow: hidden;
    transition: all 0.2s ease;

    &:hover {
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        transform: translateY(-1px);
    }
}

.original-post {
    background: #f8fafc;
    padding: 1rem 1.5rem;
    border-bottom: 1px solid #e2e8f0;
    cursor: pointer;
    display: flex;
    justify-content: space-between;
    align-items: center;
    transition: background 0.2s ease;

    &:hover {
        background: #f1f5f9;
    }

    .post-info {
        .post-category {
            background: var(--color-primary-light);
            color: var(--color-primary);
            padding: 0.25rem 0.75rem;
            border-radius: 12px;
            font-size: 0.75rem;
            font-weight: 500;
            margin-right: 1rem;
        }

        .post-title {
            color: var(--color-gray-800);
            font-size: 1rem;
            font-weight: 500;
            margin: 0;
            display: inline;
        }
    }

    i {
        color: var(--color-gray-400);
        font-size: 0.9rem;
    }
}

.comment-content {
    padding: 1.5rem;
}

.comment-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;

    .comment-meta {
        display: flex;
        align-items: center;
        gap: 1rem;

        .date {
            color: var(--color-gray-500);
            font-size: 0.9rem;
        }
    }

    .comment-actions {
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

.comment-text {
    color: var(--color-gray-700);
    line-height: 1.6;
    margin-bottom: 1rem;
    padding: 1rem;
    background: #f8fafc;
    border-radius: 8px;
    border-left: 3px solid var(--color-primary);
}

.comment-footer {
    .comment-stats {
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

// 모달 스타일
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
}

.modal-content {
    background: white;
    border-radius: 12px;
    width: 90%;
    max-width: 500px;
    max-height: 80vh;
    overflow: hidden;
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid #e2e8f0;

    h3 {
        margin: 0;
        color: var(--color-gray-900);
    }

    .close-btn {
        background: none;
        border: none;
        font-size: 1.25rem;
        color: var(--color-gray-500);
        cursor: pointer;

        &:hover {
            color: var(--color-gray-700);
        }
    }
}

.modal-body {
    padding: 1.5rem;

    .edit-textarea {
        width: 100%;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        padding: 1rem;
        font-size: 0.9rem;
        resize: vertical;
        min-height: 100px;

        &:focus {
            outline: none;
            border-color: var(--color-primary);
        }
    }
}

.modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
    padding: 1.5rem;
    border-top: 1px solid #e2e8f0;

    .btn {
        padding: 0.75rem 1.5rem;
        border-radius: 6px;
        cursor: pointer;
        border: 1px solid;

        &.btn-outline {
            background: white;
            color: var(--color-gray-700);
            border-color: #e2e8f0;

            &:hover {
                background: #f8fafc;
            }
        }

        &.btn-primary {
            background: var(--color-primary);
            color: white;
            border-color: var(--color-primary);

            &:hover {
                background: var(--color-primary-dark);
            }
        }
    }
}

@media (max-width: 768px) {
    .filter-top {
        flex-direction: column;
        align-items: stretch;
    }

    .comment-header {
        flex-direction: column;
        align-items: flex-start;
        gap: 1rem;
    }

    .comment-actions {
        align-self: flex-end;
    }

    .original-post {
        flex-direction: column;
        align-items: flex-start;
        gap: 0.5rem;
    }
}
</style>
