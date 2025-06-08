<template>
  <div class="find-password-background">
    <div class="find-password-card">
      <h2>비밀번호 찾기</h2>

      <form @submit.prevent="handleFindPassword">
        <input
            type="email"
            placeholder="이메일"
            v-model="email"
            required
        />

        <input
            type="text"
            placeholder="이름"
            v-model="name"
            required
        />

        <button type="submit" class="find-password-btn" :disabled="loading">
          {{ loading ? '처리중...' : '비밀번호 찾기' }}
        </button>
      </form>

      <!-- 성공 메시지 -->
      <div v-if="showSuccessMessage" class="success-message">
        <div class="icon">📧</div>
        <h3>임시 비밀번호가 발송되었습니다!</h3>
        <p>입력하신 이메일로 임시 비밀번호를 발송했습니다.<br>
          이메일을 확인한 후 로그인해주세요.</p>
        <p class="note">이메일이 도착하지 않았다면 스팸함을 확인해주세요.</p>
      </div>

      <!-- 에러 메시지 -->
      <div v-if="error" class="error-message">
        {{ error }}
      </div>

      <div class="login-link">
        <router-link to="/login">
          로그인 페이지로 돌아가기
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref} from "vue";
import {useRouter} from "vue-router";
import {memberAiNoAuth} from "@/axios";

const router = useRouter();
const email = ref("");
const name = ref("");
const loading = ref(false);
const showSuccessMessage = ref(false);
const error = ref("");

const handleFindPassword = async () => {
  // 입력값 검증
  if (!email.value.trim() || !name.value.trim()) {
    error.value = "이메일과 이름을 모두 입력해주세요.";
    return;
  }

  // 이메일 형식 검증
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email.value)) {
    error.value = "올바른 이메일 형식이 아닙니다.";
    return;
  }

  loading.value = true;
  error.value = "";

  try {
    const response = await memberAiNoAuth.post("/api/v1/users/reset-password", {
      email: email.value,
      name: name.value,
    });

    showSuccessMessage.value = true;

    // 3초 후 로그인 페이지로 이동
    setTimeout(() => {
      router.push("/login");
    }, 3000);

  } catch (error: any) {
    let errorMessage = "비밀번호 찾기에 실패했습니다.";

    if (error.response?.status === 404) {
      errorMessage = "입력하신 정보와 일치하는 사용자가 없습니다.";
    } else if (error.response?.status === 400) {
      errorMessage = error.response.data.error || error.response.data.message || errorMessage;
    } else if (error.code === 'ECONNABORTED') {
      errorMessage = '서버 처리 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.';
    } else if (error.response?.status === 500) {
      errorMessage = '서버 오류가 발생했습니다. 관리자에게 문의해주세요.';
    }

    error.value = errorMessage;
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.find-password-background {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: #f4f6fb;
  padding-top: 48px;
}

.find-password-card {
  background: #fff;
  padding: 2.5rem 2rem;
  border-radius: 18px;
  box-shadow: 0 8px 32px rgba(60, 60, 120, 0.12);
  width: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.find-password-card h2 {
  margin-bottom: 1.5rem;
  font-weight: 700;
  color: #222;
}

.find-password-card form {
  width: 100%;
  display: flex;
  flex-direction: column;
}

.find-password-card input[type="text"],
.find-password-card input[type="email"] {
  width: 100%;
  padding: 0.9rem 1rem;
  margin-bottom: 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  transition: border 0.2s;
  box-sizing: border-box;
}

.find-password-card input:focus {
  border: 1.5px solid #4f8cff;
  outline: none;
}

.find-password-btn {
  width: 100%;
  padding: 0.9rem 0;
  background: #4f8cff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 1.2rem;
  transition: background 0.2s;
}

.find-password-btn:hover:not(:disabled) {
  background: #2563eb;
}

.find-password-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.success-message {
  text-align: center;
  color: #28a745;
  margin-bottom: 1rem;
}

.success-message .icon {
  font-size: 60px;
  margin-bottom: 20px;
}

.success-message h3 {
  margin-bottom: 15px;
  font-size: 18px;
  font-weight: 600;
}

.success-message p {
  margin-bottom: 10px;
  line-height: 1.6;
  font-size: 14px;
}

.note {
  color: #666 !important;
  font-style: italic;
  font-size: 12px !important;
}

.error-message {
  margin-bottom: 1rem;
  padding: 12px;
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
  border-radius: 6px;
  font-size: 14px;
  width: 100%;
  box-sizing: border-box;
}

.login-link {
  margin-top: 0.5rem;
  font-size: 0.95rem;
}

.login-link a {
  color: #4f8cff;
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}

/* 반응형 디자인 */
@media (max-width: 768px) {
  .find-password-background {
    padding-top: 20px;
    padding-left: 20px;
    padding-right: 20px;
  }

  .find-password-card {
    width: 100%;
    max-width: 400px;
    padding: 2rem 1.5rem;
  }
}

/* 입력 필드 플레이스홀더 스타일 */
.find-password-card input::placeholder {
  color: #999;
  font-size: 0.95rem;
}

/* 로딩 상태 애니메이션 */
.find-password-btn:disabled {
  position: relative;
}

.find-password-btn:disabled::after {
  content: '';
  position: absolute;
  width: 16px;
  height: 16px;
  margin: auto;
  border: 2px solid transparent;
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: button-loading-spinner 1s ease infinite;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

@keyframes button-loading-spinner {
  from {
    transform: rotate(0turn);
  }
  to {
    transform: rotate(1turn);
  }
}
</style>
