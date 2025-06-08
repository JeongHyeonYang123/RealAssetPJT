<template>
  <div class="register-background">
    <div class="register-card">
      <h2>회원가입</h2>
      <form @submit.prevent="handleRegister">
        <input type="text" placeholder="이름" v-model="name" required />

        <!-- 이메일 입력 -->
        <div class="email-check-row">
          <input
              type="email"
              placeholder="이메일"
              v-model="email"
              @input="debouncedCheckEmail"
              required
              :class="{
                            'input-success': canUseEmail && emailCheckMsg,
                            'input-error': !canUseEmail && emailCheckMsg,
                        }"
          />
        </div>
        <p v-if="emailCheckMsg" :class="canUseEmail ? 'success-msg' : 'error-msg'">
          {{ emailCheckMsg }}
        </p>

        <!-- 비밀번호 입력 -->
        <input
            type="password"
            placeholder="비밀번호"
            v-model="password"
            @input="checkPasswordSecurity"
            required
            :class="{
                        'input-success': isPasswordSecure && password,
                        'input-error': !isPasswordSecure && password,
                    }"
        />
        <div v-if="password" class="password-rules">
          <p :class="passwordRules.length ? 'rule-success' : 'rule-error'">
            ✓ 8자 이상
          </p>
          <p :class="passwordRules.uppercase ? 'rule-success' : 'rule-error'">
            ✓ 대문자 포함
          </p>
          <p :class="passwordRules.lowercase ? 'rule-success' : 'rule-error'">
            ✓ 소문자 포함
          </p>
          <p :class="passwordRules.number ? 'rule-success' : 'rule-error'">
            ✓ 숫자 포함
          </p>
          <p :class="passwordRules.special ? 'rule-success' : 'rule-error'">
            ✓ 특수문자 포함
          </p>
        </div>

        <!-- 비밀번호 확인 -->
        <input
            type="password"
            placeholder="비밀번호 확인"
            v-model="passwordConfirm"
            @input="checkPasswordMatch"
            required
            :class="{
                        'input-success': isPasswordMatch && passwordConfirm,
                        'input-error': !isPasswordMatch && passwordConfirm,
                    }"
        />
        <p v-if="passwordConfirm" :class="isPasswordMatch ? 'success-msg' : 'error-msg'">
          {{ passwordMatchMsg }}
        </p>

        <input
            type="text"
            placeholder="주소"
            v-model="address"
            required
        />

        <button type="submit" class="register-btn" :disabled="!canRegister">
          회원가입
        </button>
      </form>
      <div class="login-link">
        <router-link to="/login"
        >이미 계정이 있으신가요? 로그인하기</router-link
        >
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, ref} from "vue";
import {useRouter} from "vue-router";
import {memberAiNoAuth} from "@/axios";

const router = useRouter();
const name = ref("");
const email = ref("");
const password = ref("");
const passwordConfirm = ref("");
const address = ref("");

// 이메일 관련 상태
const canUseEmail = ref(false);
const emailCheckMsg = ref("");

// 비밀번호 보안 규칙 상태
const passwordRules = ref({
  length: false,
  uppercase: false,
  lowercase: false,
  number: false,
  special: false
});

// 비밀번호 일치 상태
const isPasswordMatch = ref(false);
const passwordMatchMsg = ref("");

// 이메일 중복 체크 (디바운싱)
let debounceTimer: number | null = null;
const debouncedCheckEmail = () => {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = window.setTimeout(checkEmail, 400);
};

const checkEmail = async () => {
  if (!email.value) {
    emailCheckMsg.value = "";
    canUseEmail.value = false;
    return;
  }

  // 이메일 형식 검증
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email.value)) {
    emailCheckMsg.value = "올바른 이메일 형식이 아닙니다.";
    canUseEmail.value = false;
    return;
  }

  try {
    const res = await memberAiNoAuth.get(
        `/api/v1/users/email/${encodeURIComponent(email.value)}`
    );
    if (res.data.data.canUse) {
      emailCheckMsg.value = "사용 가능한 이메일입니다.";
      canUseEmail.value = true;
    } else {
      emailCheckMsg.value = "이미 사용 중인 이메일입니다.";
      canUseEmail.value = false;
    }
  } catch (e) {
    emailCheckMsg.value = "이메일 확인 중 오류가 발생했습니다.";
    canUseEmail.value = false;
  }
};

// 비밀번호 보안 규칙 검증
const checkPasswordSecurity = () => {
  const pwd = password.value;

  passwordRules.value = {
    length: pwd.length >= 8,
    uppercase: /[A-Z]/.test(pwd),
    lowercase: /[a-z]/.test(pwd),
    number: /\d/.test(pwd),
    special: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(pwd)
  };

  // 비밀번호 확인도 다시 체크
  if (passwordConfirm.value) {
    checkPasswordMatch();
  }
};

// 비밀번호 보안 여부 계산
const isPasswordSecure = computed(() => {
  return Object.values(passwordRules.value).every(rule => rule);
});

// 비밀번호 일치 확인
const checkPasswordMatch = () => {
  if (!passwordConfirm.value) {
    passwordMatchMsg.value = "";
    isPasswordMatch.value = false;
    return;
  }

  if (password.value === passwordConfirm.value) {
    passwordMatchMsg.value = "비밀번호가 일치합니다.";
    isPasswordMatch.value = true;
  } else {
    passwordMatchMsg.value = "비밀번호가 일치하지 않습니다.";
    isPasswordMatch.value = false;
  }
};

// 회원가입 가능 여부 계산
const canRegister = computed(() => {
  return canUseEmail.value &&
      isPasswordSecure.value &&
      isPasswordMatch.value &&
      name.value.trim() &&
      address.value.trim();
});

// 회원가입 불가능 사유 메시지
const getRegisterErrorMessage = () => {
  const errors = [];

  if (!name.value.trim()) errors.push("이름을 입력해주세요.");
  if (!canUseEmail.value) {
    if (!email.value) errors.push("이메일을 입력해주세요.");
    else errors.push("사용 가능한 이메일을 입력해주세요.");
  }
  if (!isPasswordSecure.value) {
    errors.push("비밀번호가 보안 규칙을 만족하지 않습니다.");
  }
  if (!isPasswordMatch.value) {
    errors.push("비밀번호 확인이 일치하지 않습니다.");
  }
  if (!address.value.trim()) errors.push("주소를 입력해주세요.");

  return errors.join("\n");
};

const handleRegister = async () => {
  // 회원가입 조건 체크
  if (!canRegister.value) {
    alert(getRegisterErrorMessage());
    return;
  }

  try {
    const res = await memberAiNoAuth.post("/api/v1/users", {
      name: name.value,
      email: email.value,
      password: password.value,
      address: address.value,
    });

    alert('회원가입 신청이 완료되었습니다. 이메일을 확인해주세요.');
    router.push("/login");

  } catch (error: any) {
    let errorMessage = "회원가입에 실패했습니다.";

    if (error.code === 'ECONNABORTED') {
      errorMessage = '서버 처리 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.';
    } else if (error.response?.status === 400) {
      errorMessage = error.response.data.error || error.response.data.message || errorMessage;
    } else if (error.response?.status === 500) {
      errorMessage = '서버 오류가 발생했습니다. 관리자에게 문의해주세요.';
    } else if (!navigator.onLine) {
      errorMessage = '네트워크 오류가 발생했습니다. 인터넷 연결을 확인해주세요.';
    }

    alert(errorMessage);
  }
};
</script>

<style scoped>
.register-background {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: #f4f6fb;
  padding-top: 48px;
}

.register-card {
  background: #fff;
  padding: 2.5rem 2rem;
  border-radius: 18px;
  box-shadow: 0 8px 32px rgba(60, 60, 120, 0.12);
  width: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.register-card h2 {
  margin-bottom: 1.5rem;
  font-weight: 700;
  color: #222;
}

.register-card form {
  width: 100%;
  display: flex;
  flex-direction: column;
}

.register-card input[type="text"],
.register-card input[type="email"],
.register-card input[type="password"] {
  width: 100%;
  padding: 0.9rem 1rem;
  margin-bottom: 0.5rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  transition: border 0.2s;
  box-sizing: border-box;
}

.register-card input:focus {
  border: 1.5px solid #4f8cff;
  outline: none;
}

.email-check-row {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0;
}

.success-msg {
  color: #22c55e;
  margin-bottom: 1rem;
  font-size: 0.85rem;
  margin-top: 0.25rem;
}

.error-msg {
  color: #e53e3e;
  margin-bottom: 1rem;
  font-size: 0.85rem;
  margin-top: 0.25rem;
}

/* 비밀번호 규칙 표시 */
.password-rules {
  margin-bottom: 1rem;
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 0.8rem;
}

.password-rules p {
  margin: 0.25rem 0;
  display: flex;
  align-items: center;
}

.rule-success {
  color: #22c55e;
}

.rule-error {
  color: #e53e3e;
}

.register-btn {
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
  margin-top: 1rem;
  transition: background 0.2s;
}

.register-btn:hover:not(:disabled) {
  background: #2563eb;
}

.register-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
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

.input-success {
  border: 1.5px solid #22c55e !important;
  background: #f0fdf4;
}

.input-error {
  border: 1.5px solid #e53e3e !important;
  background: #fef2f2;
}
</style>
