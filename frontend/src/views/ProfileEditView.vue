<template>
  <div class="profile-edit-background">
    <div class="profile-edit-card">
      <h2>프로필 수정</h2>

      <form @submit.prevent="handleProfileEdit">
        <div class="form-group">
          <label for="name">이름</label>
          <input
              id="name"
              type="text"
              placeholder="이름을 입력하세요"
              v-model="formData.name"
              required
          />
        </div>

        <div class="form-group">
          <label for="email">이메일</label>
          <input
              id="email"
              type="email"
              placeholder="이메일"
              v-model="formData.email"
              disabled
              class="disabled-input"
          />
        </div>

        <div class="form-group">
          <label for="address">주소</label>
          <input
              id="address"
              type="text"
              placeholder="주소를 입력하세요"
              v-model="formData.address"
              required
          />
        </div>

        <div class="form-group">
          <label for="profileImage">프로필 이미지 URL</label>
          <input
              id="profileImage"
              type="text"
              placeholder="프로필 이미지 URL을 입력하세요"
              v-model="formData.profileImage"
          />
        </div>

        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>

        <div class="button-group">
          <button
              type="button"
              class="btn btn-cancel"
              @click="goBack"
              :disabled="isLoading"
          >
            취소
          </button>
          <button
              type="submit"
              class="btn btn-primary"
              :disabled="isLoading || !isFormValid"
          >
            <span v-if="isLoading">처리 중...</span>
            <span v-else>저장</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useMemberStore } from '@/store/member';
import { memberAi } from '@/axios';

const router = useRouter();
const memberStore = useMemberStore();

const isLoading = ref(false);
const errorMessage = ref('');

const formData = reactive({
  name: '',
  email: '',
  address: '',
  profileImage: ''
});

const isFormValid = computed(() => {
  const nameValid = !!(formData.name && formData.name.trim());
  const addressValid = !!(formData.address && formData.address.trim());
  return nameValid && addressValid;
});

function checkAuthStatus(): boolean {
  if (!memberStore.loginUser?.mno) {
    alert('로그인이 필요합니다.');
    router.push('/login');
    return false;
  }

  if (!memberStore.tokens?.accessToken) {
    alert('인증 토큰이 없습니다. 다시 로그인해주세요.');
    router.push('/login');
    return false;
  }

  return true;
}

async function fetchUserData() {
  if (!checkAuthStatus()) return;

  try {
    isLoading.value = true;
    errorMessage.value = '';

    const response = await memberAi.get(`/api/v1/users/${memberStore.loginUser.mno}`);

    console.log('userData:', response.data?.data);

    if (response.data && response.data.success) {
      const userData = response.data.data;

      Object.assign(formData, {
        name: userData.name || '',
        email: userData.email || '',
        address: userData.address || '',
        profileImage: userData.profileImage || ''
      });
    } else {
      throw new Error(response.data?.message || '사용자 정보를 불러오는데 실패했습니다.');
    }
  } catch (error: any) {
    if (error.response?.status === 401) {
      alert('인증이 만료되었습니다. 다시 로그인해주세요.');
      await memberStore.logout();
      router.push('/login');
    } else if (error.response?.status === 403) {
      alert('접근 권한이 없습니다.');
      router.push('/mypage');
    } else {
      errorMessage.value = error.response?.data?.message || '사용자 정보를 불러오는데 실패했습니다.';
    }
  } finally {
    isLoading.value = false;
  }
}

// ✅ Store 액션을 사용하는 프로필 업데이트 함수
async function handleProfileEdit() {
  if (!checkAuthStatus()) return;

  if (!isFormValid.value) {
    alert('이름과 주소를 모두 입력해주세요.');
    return;
  }

  try {
    isLoading.value = true;
    errorMessage.value = '';

    const updateData = { ...formData };

    const response = await memberAi.put(`/api/v1/users/${memberStore.loginUser.mno}`, updateData);

    if (response.data && response.data.success === true) {
      // ✅ Store 액션 사용으로 sessionStorage에 자동 반영
      memberStore.updateProfile({
        name: formData.name,
        email: formData.email,
        address: formData.address,
        profileImage: formData.profileImage
      });

      alert('프로필이 성공적으로 수정되었습니다.');
      router.push('/mypage');
    } else {
      throw new Error(response.data?.message || '프로필 수정에 실패했습니다.');
    }
  } catch (error: any) {
    if (error.response?.status === 401) {
      alert('인증이 만료되었습니다. 다시 로그인해주세요.');
      await memberStore.logout();
      router.push('/login');
    } else if (error.response?.status === 403) {
      alert('수정 권한이 없습니다.');
    } else {
      errorMessage.value = error.response?.data?.message || error.message || '프로필 수정 중 오류가 발생했습니다.';
    }
  } finally {
    isLoading.value = false;
  }
}

function goBack() {
  router.push('/mypage');
}

onMounted(async () => {
  if (checkAuthStatus()) {
    await fetchUserData();
  }
});
</script>

<style scoped>
.profile-edit-background {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: #f4f6fb;
  padding: 2rem 1rem;
}

.profile-edit-card {
  background: #fff;
  padding: 2.5rem 2rem;
  border-radius: 18px;
  box-shadow: 0 8px 32px rgba(60, 60, 120, 0.12);
  width: 100%;
  max-width: 500px;
}

.profile-edit-card h2 {
  margin-bottom: 1.8rem;
  font-weight: 700;
  color: #2c3e50;
  text-align: center;
}

.profile-edit-card form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.form-group {
  margin-bottom: 0.5rem;
  width: 100%;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #4a5568;
  font-size: 0.95rem;
}

.profile-edit-card input[type="text"],
.profile-edit-card input[type="email"] {
  width: 100%;
  padding: 0.85rem 1rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 1rem;
  transition: all 0.2s ease;
  box-sizing: border-box;
  background-color: #f8fafc;
}

.profile-edit-card input:focus {
  outline: none;
  border-color: #4f8cff;
  box-shadow: 0 0 0 3px rgba(79, 140, 255, 0.1);
}

.profile-edit-card input.disabled-input {
  background-color: #f1f5f9;
  color: #64748b;
  cursor: not-allowed;
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

/* 반응형 디자인 */
@media (max-width: 480px) {
  .profile-edit-card {
    padding: 1.8rem 1.5rem;
  }

  .button-group {
    flex-direction: column;
  }

  .btn {
    width: 100%;
  }
}
</style>
