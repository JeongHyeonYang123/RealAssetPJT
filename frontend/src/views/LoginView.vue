<template>
    <div class="login-background">
        <div class="login-card">
            <h2>로그인</h2>
            <form @submit.prevent="handleLogin">
                <input
                    type="email"
                    placeholder="이메일"
                    v-model="email"
                    required
                />
                <input
                    type="password"
                    placeholder="비밀번호"
                    v-model="password"
                    required
                />
                <div class="options">
                    <label class="remember-label">
                        <input type="checkbox" v-model="remember" />
                        ID 기억하기
                    </label>
                    <a href="/find-password">비밀번호 찾기</a>
                </div>
                <button type="submit" class="login-btn">로그인</button>
                <p v-if="loginError" class="error-msg">{{ loginError }}</p>
            </form>
            <div class="social-login">
                <button class="naver-btn only-img">
                    <img src="@/assets/naver-login.png" alt="네이버 로그인" />
                </button>
                <button class="kakao-btn only-img" @click="handleKakaoLogin">
                    <img src="@/assets/kakao-login.png" alt="카카오 로그인" />
                </button>
            </div>
            <div class="register-link">
                <a href="/register">회원가입</a>
            </div>
        </div>
    </div>
</template>

<script setup>
import {onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import {useMemberStore} from "@/store/member";

const router = useRouter();
const memberStore = useMemberStore();
const email = ref("");
const password = ref("");
const remember = ref(false);
const loginError = ref("");

onMounted(() => {
    const savedEmail = localStorage.getItem("rememberedEmail");
    if (savedEmail) {
        email.value = savedEmail;
        remember.value = true;
    }
});

const handleLogin = async () => {
    loginError.value = "";
    try {
        await memberStore.login({
            email: email.value,
            password: password.value,
        });
        if (remember.value) {
            localStorage.setItem("rememberedEmail", email.value);
        } else {
            localStorage.removeItem("rememberedEmail");
        }
        router.push("/");
    } catch (e) {
        loginError.value = "이메일 또는 비밀번호가 올바르지 않습니다.";
    }
};

const handleKakaoLogin = () => {
    const REST_API_KEY = import.meta.env.VITE_KAKAO_REST_API_KEY;
    const REDIRECT_URI = import.meta.env.VITE_KAKAO_REDIRECT_URI;
    const KAKAO_AUTH_URL = `https://kauth.kakao.com/oauth/authorize?client_id=${REST_API_KEY}&redirect_uri=${REDIRECT_URI}&response_type=code`;

    window.location.href = KAKAO_AUTH_URL;
};
</script>

<style scoped>
.login-background {
    min-height: 100vh;
    display: flex;
    align-items: flex-start;
    justify-content: center;
    background: #f4f6fb;
    padding-top: 48px;
}
.login-card {
    background: #fff;
    padding: 2.5rem 2rem;
    border-radius: 18px;
    box-shadow: 0 8px 32px rgba(60, 60, 120, 0.12);
    width: 420px;
    display: flex;
    flex-direction: column;
    align-items: center;
}
.login-card h2 {
    margin-bottom: 1.5rem;
    font-weight: 700;
    color: #222;
}
.login-card input[type="email"],
.login-card input[type="password"] {
    width: 100%;
    padding: 0.9rem 1rem;
    margin-bottom: 1rem;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    font-size: 1rem;
    transition: border 0.2s;
}
.login-card input:focus {
    border: 1.5px solid #4f8cff;
    outline: none;
}
.options {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 0.95rem;
    margin-bottom: 1.2rem;
    white-space: nowrap;
}
.login-btn {
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
.login-btn:hover {
    background: #2563eb;
}
.social-login {
    display: flex;
    gap: 0.7rem;
    width: 100%;
    margin-bottom: 1rem;
}
.social-login button {
    width: calc(50% - 0.35rem);
    min-width: 0;
    box-sizing: border-box;
    height: 44px;
    background: none;
    border: none;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
}
.only-img img {
    height: 44px;
    width: auto;
    display: block;
}
.register-link {
    margin-top: 0.5rem;
    font-size: 0.95rem;
}
.register-link a {
    color: #4f8cff;
    text-decoration: none;
    font-weight: 500;
}
.register-link a:hover {
    text-decoration: underline;
}
.remember-label {
    display: flex;
    align-items: center;
    gap: 0.2rem;
    white-space: nowrap;
}
.icon {
    width: 22px;
    height: 22px;
}
.error-msg {
    color: red;
    font-size: 0.9rem;
    margin-top: 0.5rem;
}
</style>
