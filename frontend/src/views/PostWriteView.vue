<template>
	<div class="write-post-view">
		<h1>{{ isEdit ? '글수정' : '글쓰기' }}</h1>
		<form @submit.prevent="submitPost">
			<!-- 카테고리 선택 -->
			<div class="form-group">
				<label for="category">카테고리</label>
				<select id="category" v-model="category" required>
					<option value="">카테고리를 선택하세요</option>
					<option value="시장동향">시장동향</option>
					<option value="아파트후기">아파트후기</option>
					<option value="질문/답변">질문/답변</option>
					<option value="정보공유">정보공유</option>
				</select>
			</div>

			<!-- 제목 -->
			<div class="form-group">
				<label for="title">제목</label>
				<input
					type="text"
					id="title"
					v-model="title"
					placeholder="제목을 입력하세요"
					maxlength="255"
					required
				/>
			</div>

			<!-- 내용 -->
			<div class="form-group">
				<label for="content">내용</label>
				<textarea
					id="content"
					v-model="content"
					placeholder="내용을 입력하세요"
					required
				></textarea>
			</div>

			<!-- 태그 -->
			<div class="form-group">
				<label for="tags">태그 (선택사항)</label>
				<input
					type="text"
					id="tags"
					v-model="tags"
					placeholder="태그를 입력하세요 (쉼표로 구분)"
					maxlength="255"
				/>
				<small class="help-text">예: 강남, 래미안, 실거주후기</small>
			</div>

			<!-- 버튼 그룹 -->
			<div class="button-group">
				<button
					type="button"
					class="btn btn-cancel"
					@click="goBack"
					:disabled="isSubmitting"
				>
					취소
				</button>
				<button
					type="submit"
					class="btn btn-primary"
					:disabled="isSubmitting"
				>
					{{ isSubmitting ? '처리중...' : (isEdit ? '수정완료' : '작성완료') }}
				</button>
			</div>
		</form>
	</div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMemberStore } from '@/store/member'
import { memberAi } from '@/axios'

const router = useRouter()
const route = useRoute()
const memberStore = useMemberStore()

const category = ref('')
const title = ref('')
const content = ref('')
const tags = ref('')
const isSubmitting = ref(false)
const isEdit = ref(false)
const postId = ref<number | null>(null)

// 폼 데이터 초기화 함수
function resetForm() {
	category.value = ''
	title.value = ''
	content.value = ''
	tags.value = ''
	isSubmitting.value = false
	isEdit.value = false
	postId.value = null
}

onMounted(async () => {
	if (!memberStore.isLoggedIn) {
		alert('로그인이 필요합니다.')
		router.push('/login')
		return
	}

	// 수정 모드인지 확인
	if (route.params.id) {
		isEdit.value = true
		postId.value = Number(route.params.id)
		await loadPost()
	}
})

// 컴포넌트가 언마운트될 때 폼 초기화
onUnmounted(() => {
	resetForm()
})

async function loadPost() {
	try {
		const response = await memberAi.get(`/api/v1/posts/${postId.value}`)
		if (response.data.success) {
			const post = response.data.data
			category.value = post.category
			title.value = post.title
			content.value = post.content
			tags.value = post.tags ? post.tags.join(', ') : ''
		}
	} catch (error) {
		alert('게시글을 불러오는데 실패했습니다.')
		router.push('/community')
	}
}

async function submitPost() {
	if (!category.value) {
		alert('카테고리를 선택해주세요.')
		return
	}

	if (!title.value.trim()) {
		alert('제목을 입력해주세요.')
		return
	}

	if (!content.value.trim()) {
		alert('내용을 입력해주세요.')
		return
	}

	isSubmitting.value = true

	try {
		const postData = {
			category: category.value,
			title: title.value.trim(),
			content: content.value.trim(),
			tags: tags.value ? tags.value.split(',').map(tag => tag.trim()).filter(tag => tag) : []
		}

		if (isEdit.value) {
			await memberAi.put(`/api/v1/posts/${postId.value}`, postData)
			alert('글이 성공적으로 수정되었습니다.')
		} else {
			await memberAi.post('/api/v1/posts', postData)
			alert('글이 성공적으로 작성되었습니다.')
			resetForm()
		}

		// 페이지 이동 시 강제로 새로고침
		window.location.href = '/community'

	} catch (error: any) {
		alert(error.response?.data?.message || '처리에 실패했습니다.')
	} finally {
		isSubmitting.value = false
	}
}

function goBack() {
	if (title.value || content.value) {
		if (confirm('작성 중인 내용이 있습니다. 정말 나가시겠습니까?')) {
			router.push('/community')
		}
	} else {
		router.push('/community')
	}
}
</script>

<style scoped>
.write-post-view {
	max-width: 800px;
	margin: 0 auto;
	padding: 20px;
	background: white;
	border-radius: 8px;
	box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

h1 {
	color: #333;
	margin-bottom: 30px;
	text-align: center;
	font-size: 24px;
}

.form-group {
	margin-bottom: 20px;
}

label {
	display: block;
	margin-bottom: 8px;
	font-weight: 600;
	color: #333;
	font-size: 14px;
}

input, textarea, select {
	width: 100%;
	padding: 12px;
	border: 1px solid #ddd;
	border-radius: 6px;
	font-size: 14px;
	transition: border-color 0.2s ease;
	box-sizing: border-box;
}

input:focus, textarea:focus, select:focus {
	outline: none;
	border-color: #2d60e8;
	box-shadow: 0 0 0 2px rgba(45, 96, 232, 0.1);
}

select {
	background-color: white;
	cursor: pointer;
}

textarea {
	height: 300px;
	resize: vertical;
	font-family: inherit;
	line-height: 1.5;
}

.help-text {
	display: block;
	margin-top: 5px;
	font-size: 12px;
	color: #666;
}

.button-group {
	display: flex;
	gap: 12px;
	justify-content: center;
	margin-top: 30px;
}

.btn {
	padding: 12px 24px;
	border: none;
	border-radius: 6px;
	font-size: 14px;
	font-weight: 500;
	cursor: pointer;
	transition: all 0.2s ease;
	min-width: 100px;
}

.btn-primary {
	background-color: #2d60e8;
	color: white;
}

.btn-primary:hover:not(:disabled) {
	background-color: #1a4bc7;
	transform: translateY(-1px);
}

.btn-primary:disabled {
	background-color: #ccc;
	cursor: not-allowed;
}

.btn-cancel {
	background-color: #6c757d;
	color: white;
}

.btn-cancel:hover {
	background-color: #545b62;
}

/* 반응형 디자인 */
@media (max-width: 768px) {
	.write-post-view {
		margin: 10px;
		padding: 15px;
	}

	.button-group {
		flex-direction: column;
	}

	.btn {
		width: 100%;
	}
}
</style>
