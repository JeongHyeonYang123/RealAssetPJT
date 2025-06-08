<template>
  <div class="change-password-container">
    <div class="change-password-card">
      <h2>비밀번호 변경</h2>
      <form @submit.prevent="handleChangePassword">
        <div class="form-group">
          <label for="currentPassword">현재 비밀번호</label>
          <input id="currentPassword" type="password" v-model="currentPassword" required />
        </div>
        <div class="form-group">
          <label for="newPassword">새 비밀번호</label>
          <input id="newPassword" type="password" v-model="newPassword" required />
        </div>
        <div class="form-group">
          <label for="confirmPassword">새 비밀번호 확인</label>
          <input id="confirmPassword" type="password" v-model="confirmPassword" required />
        </div>
        <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
        <div class="button-group">
          <button type="button" class="btn btn-cancel" @click="goBack" :disabled="isLoading">취소</button>
          <button type="submit" class="btn btn-primary" :disabled="isLoading">{{ isLoading ? '처리 중...' : '변경' }}</button>
        </div>
      </form>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMemberStore } from '@/store/member';
import { memberAi } from '@/axios';

const router = useRouter();
const memberStore = useMemberStore();
const isLoading = ref(false);
const errorMessage = ref('');
const currentPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');

function goBack() {
  router.push('/mypage');
}

async function handleChangePassword() {
  errorMessage.value = '';
  if (!currentPassword.value || !newPassword.value || !confirmPassword.value) {
    errorMessage.value = '모든 항목을 입력해주세요.';
    return;
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = '새 비밀번호가 일치하지 않습니다.';
    return;
  }
  try {
    isLoading.value = true;
    const mno = memberStore.loginUser?.mno;
    if (!mno) {
      errorMessage.value = '로그인 정보가 없습니다.';
      return;
    }
    const response = await memberAi.put(`/api/v1/users/${mno}/password`, {
      currentPassword: currentPassword.value,
      newPassword: newPassword.value
    });
    if (response.data && response.data.success) {
      alert('비밀번호가 성공적으로 변경되었습니다.');
      router.push('/mypage');
    } else {
      throw new Error(response.data?.message || '비밀번호 변경에 실패했습니다.');
    }
  } catch (error: any) {
    errorMessage.value = error.response?.data?.message || error.message || '비밀번호 변경 중 오류가 발생했습니다.';
  } finally {
    isLoading.value = false;
  }
}
</script>
<style scoped>
.change-password-container {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: #f4f6fb;
  padding: 2rem 1rem;
}
.change-password-card {
  background: #fff;
  padding: 2.5rem 2rem;
  border-radius: 18px;
  box-shadow: 0 8px 32px rgba(60, 60, 120, 0.12);
  width: 100%;
  max-width: 400px;
}
.change-password-card h2 {
  margin-bottom: 1.8rem;
  font-weight: 700;
  color: #2c3e50;
  text-align: center;
}
.form-group {
  margin-bottom: 1.2rem;
}
.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #4a5568;
  font-size: 0.95rem;
}
.change-password-card input[type="password"] {
  width: 100%;
  padding: 0.85rem 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.2s ease;
  box-sizing: border-box;
  background-color: #f8fafc;
}
.change-password-card input:focus {
  outline: none;
  border-color: #4f8cff;
  box-shadow: 0 0 0 3px rgba(79, 140, 255, 0.1);
}
.button-group {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1.5rem;
}
.btn {
  padding: 0.8rem 1.8rem;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.btn-cancel {
  background: #e2e8f0;
  color: #4a5568;
}
.btn-cancel:hover:not(:disabled) {
  background: #cbd5e0;
}
.btn-primary {
  background: #4f8cff;
  color: #fff;
}
.btn-primary:hover:not(:disabled) {
  background: #3b7cff;
}
.btn-primary:disabled {
  background: #a0aec0;
}
.error-message {
  color: #e53e3e;
  font-size: 0.9rem;
  margin-top: 0.5rem;
  text-align: center;
  padding: 0.75rem;
  background-color: #fed7d7;
  border: 1px solid #feb2b2;
  border-radius: 6px;
}
</style> 