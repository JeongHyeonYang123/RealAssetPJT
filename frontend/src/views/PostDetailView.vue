<template>
	<div class="post-detail-view">
		<div v-if="loading" class="loading">게시글을 불러오는 중...</div>
		<div v-else-if="!post" class="error">게시글을 찾을 수 없습니다.</div>
		<div v-else>
			<!-- 게시글 내용 -->
			<article class="post-content">
				<div class="post-header">
					<div class="category-date">
						<span class="category">{{ post.category }}</span>
						<span class="date">{{ formatDate(post.createdAt) }}</span>
					</div>
					<div v-if="canModify" class="post-actions">
						<button class="btn btn-sm btn-secondary" @click="editPost">수정</button>
						<button class="btn btn-sm btn-danger" @click="deletePost">삭제</button>
					</div>
				</div>

				<h1 class="post-title">{{ post.title }}</h1>

				<div class="post-meta">
					<span class="author">{{ post.authorName }}</span>
					<div class="stats">
						<span>조회 {{ post.views }}</span>
						<span>좋아요 {{ post.likes }}</span>
						<span>싫어요 {{ post.dislikes }}</span>
					</div>
				</div>

				<div class="post-body">
					<div class="content">{{ post.content }}</div>
					<div v-if="post.tags && post.tags.length > 0" class="tags">
						<span v-for="tag in post.tags" :key="tag" class="tag">#{{ tag }}</span>
					</div>
				</div>

				<!-- 좋아요/싫어요 버튼 -->
				<div class="reaction-buttons">
					<button class="btn btn-like" @click="reactToPost(true)">
						👍 좋아요 ({{ post.likes }})
					</button>
					<button class="btn btn-dislike" @click="reactToPost(false)">
						👎 싫어요 ({{ post.dislikes }})
					</button>
				</div>
			</article>

			<!-- 댓글 섹션 -->
			<section class="comments-section">
				<h3>댓글 ({{ comments.length }})</h3>

				<!-- 댓글 작성 -->
				<div v-if="memberStore.isLoggedIn" class="comment-form">
          <textarea
			  v-model="newComment"
			  placeholder="댓글을 입력하세요..."
			  rows="3"
		  ></textarea>
					<button class="btn btn-primary" @click="addComment" :disabled="!newComment.trim()">
						댓글 작성
					</button>
				</div>

				<!-- 댓글 목록 -->
				<div class="comments-list">
					<div v-for="comment in comments" :key="comment.id" class="comment-item">
						<div class="comment-header">
							<span class="comment-author">{{ comment.authorName }}</span>
							<span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
							<div v-if="canModifyComment(comment)" class="comment-actions">
								<button class="btn-text" @click="deleteComment(comment.id)">삭제</button>
							</div>
						</div>
						<div class="comment-content">{{ comment.content }}</div>
					</div>
				</div>
			</section>
		</div>

		<!-- 하단 버튼 -->
		<div class="bottom-actions">
			<button class="btn btn-secondary" @click="goBack">목록으로</button>
		</div>
	</div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMemberStore } from '@/store/member'
import { memberAi, memberAiNoAuth } from '@/axios'

const router = useRouter()
const route = useRoute()
const memberStore = useMemberStore()

const post = ref(null)
const comments = ref([])
const newComment = ref('')
const loading = ref(true)

const postId = computed(() => Number(route.params.id))

const canModify = computed(() => {
	return memberStore.isLoggedIn &&
		post.value &&
		post.value.authorMno === memberStore.loginUser?.mno
})

onMounted(async () => {
	await fetchPost()
	await fetchComments()
})

async function fetchPost() {
	try {
		const response = await memberAiNoAuth.get(`/api/v1/posts/${postId.value}`)
		if (response.data.success) {
			post.value = response.data.data
		}
	} catch (error) {
		console.error('게시글 조회 실패:', error)
	} finally {
		loading.value = false
	}
}

async function fetchComments() {
	try {
		const response = await memberAi.get(`/api/v1/posts/${postId.value}/comments`)
		if (response.data.success) {
			comments.value = response.data.data
		}
	} catch (error) {
		console.error('댓글 조회 실패:', error)
	}
}

watch(
  () => route.params.id,
  (newId) => {
    postId.value = newId;
    fetchPost();
    fetchComments();
  }
);

async function reactToPost(isLike: boolean) {
	if (!memberStore.isLoggedIn) {
		alert('로그인이 필요합니다.')
		return
	}

	try {
		await memberAi.post(`/api/v1/posts/${postId.value}/reaction`, { isLike })
		await fetchPost() // 좋아요/싫어요 수 업데이트
	} catch (error) {
		console.error('반응 처리 실패:', error)
	}
}

async function addComment() {
	if (!memberStore.isLoggedIn) {
		alert('로그인이 필요합니다.')
		return
	}

	if (!newComment.value.trim()) return

	try {
		const commentData = {
			content: newComment.value.trim(),
			parentId: null,
			depth: 0,
			authorMno: memberStore.loginUser.mno,
			authorName: memberStore.loginUser.name
		}

		await memberAi.post(`/api/v1/posts/${postId.value}/comments`, commentData)
		newComment.value = ''
		await fetchComments()
		await fetchPost() // 댓글 수 업데이트
	} catch (error) {
		alert('댓글 작성에 실패했습니다.')
	}
}

function canModifyComment(comment: any) {
	return memberStore.isLoggedIn && comment.authorMno === memberStore.loginUser?.mno
}

async function deleteComment(commentId: number) {
	if (!confirm('댓글을 삭제하시겠습니까?')) return

	try {
		await memberAi.delete(`/api/v1/posts/comments/${commentId}`)
		await fetchComments()
		await fetchPost() // 댓글 수 업데이트
	} catch (error) {
		alert('댓글 삭제에 실패했습니다.')
	}
}

function editPost() {
	router.push(`/community/edit/${postId.value}`)
}

async function deletePost() {
	if (!confirm('게시글을 삭제하시겠습니까?')) return

	try {
		await memberAi.delete(`/api/v1/posts/${postId.value}`)
		alert('게시글이 삭제되었습니다.')
		router.push('/community')
	} catch (error) {
		alert('게시글 삭제에 실패했습니다.')
	}
}

function goBack() {
	router.push('/community')
}

function formatDate(dateString: string) {
	const date = new Date(dateString)
	return date.toLocaleString('ko-KR')
}
</script>

<style scoped>
.post-detail-view {
	max-width: 800px;
	margin: 0 auto;
	padding: 20px;
}

.loading, .error {
	text-align: center;
	padding: 40px;
	color: #666;
}

.post-content {
	background: white;
	border-radius: 8px;
	box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
	padding: 30px;
	margin-bottom: 30px;
}

.post-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 20px;
	padding-bottom: 15px;
	border-bottom: 1px solid #eee;
}

.category-date {
	display: flex;
	align-items: center;
	gap: 10px;
}

.category {
	background: #2d60e8;
	color: white;
	padding: 4px 8px;
	border-radius: 4px;
	font-size: 12px;
	font-weight: 500;
}

.date {
	color: #666;
	font-size: 14px;
}

.post-actions {
	display: flex;
	gap: 8px;
}

.post-title {
	font-size: 24px;
	font-weight: 700;
	color: #333;
	margin-bottom: 15px;
	line-height: 1.4;
}

.post-meta {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 20px;
	font-size: 14px;
	color: #666;
}

.stats {
	display: flex;
	gap: 15px;
}

.post-body {
	margin-bottom: 30px;
}

.content {
	font-size: 16px;
	line-height: 1.6;
	color: #333;
	margin-bottom: 20px;
	white-space: pre-wrap;
}

.tags {
	display: flex;
	gap: 8px;
	flex-wrap: wrap;
}

.tag {
	background: #f0f0f0;
	color: #666;
	padding: 4px 8px;
	border-radius: 4px;
	font-size: 12px;
}

.reaction-buttons {
	display: flex;
	gap: 10px;
	justify-content: center;
	padding: 20px 0;
	border-top: 1px solid #eee;
}

.btn-like, .btn-dislike {
	padding: 10px 20px;
	border: 1px solid #ddd;
	background: white;
	border-radius: 6px;
	cursor: pointer;
	transition: all 0.2s ease;
}

.btn-like:hover {
	background: #e8f5e8;
	border-color: #4caf50;
}

.btn-dislike:hover {
	background: #fde8e8;
	border-color: #f44336;
}

.comments-section {
	background: white;
	border-radius: 8px;
	box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
	padding: 30px;
	margin-bottom: 30px;
}

.comments-section h3 {
	margin-bottom: 20px;
	color: #333;
}

.comment-form {
	margin-bottom: 30px;
}

.comment-form textarea {
	width: 100%;
	padding: 12px;
	border: 1px solid #ddd;
	border-radius: 6px;
	resize: vertical;
	font-family: inherit;
	margin-bottom: 10px;
	box-sizing: border-box;
}

.comment-item {
	padding: 15px 0;
	border-bottom: 1px solid #eee;
}

.comment-item:last-child {
	border-bottom: none;
}

.comment-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 8px;
}

.comment-author {
	font-weight: 600;
	color: #333;
}

.comment-date {
	color: #666;
	font-size: 12px;
}

.comment-actions {
	display: flex;
	gap: 8px;
}

.btn-text {
	background: none;
	border: none;
	color: #666;
	cursor: pointer;
	font-size: 12px;
}

.comment-content {
	color: #333;
	line-height: 1.5;
}

.bottom-actions {
	text-align: center;
}

.btn {
	padding: 10px 20px;
	border: none;
	border-radius: 6px;
	font-size: 14px;
	font-weight: 500;
	cursor: pointer;
	transition: all 0.2s ease;
}

.btn-sm {
	padding: 6px 12px;
	font-size: 12px;
}

.btn-primary {
	background-color: #2d60e8;
	color: white;
}

.btn-secondary {
	background-color: #6c757d;
	color: white;
}

.btn-danger {
	background-color: #dc3545;
	color: white;
}

/* 반응형 디자인 */
@media (max-width: 768px) {
	.post-detail-view {
		padding: 10px;
	}

	.post-content, .comments-section {
		padding: 20px 15px;
	}

	.post-header {
		flex-direction: column;
		align-items: flex-start;
		gap: 10px;
	}

	.post-meta {
		flex-direction: column;
		align-items: flex-start;
		gap: 10px;
	}

	.reaction-buttons {
		flex-direction: column;
	}
}
</style>
