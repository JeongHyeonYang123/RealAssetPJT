<template>
    <div class="callback-container">
        <div class="loading">카카오 로그인 처리 중...</div>
    </div>
</template>

<script setup lang="ts">
import {onMounted} from "vue";
import {useRoute, useRouter} from "vue-router";
import {useMemberStore} from "@/store/member";

const router = useRouter();
const route = useRoute();
const memberStore = useMemberStore();

onMounted(async () => {
    const code = route.query.code as string;

    if (!code) {
        router.push("/login");
        return;
    }

    try {
        // 백엔드로 인증 코드 전송
        await memberStore.kakaoLogin(code);
        router.push("/");
    } catch (error) {
        console.error("카카오 로그인 실패:", error);
        router.push("/login");
    }
});
</script>

<style scoped>
.callback-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: #f4f6fb;
}

.loading {
    font-size: 1.2rem;
    color: #333;
}
</style>
