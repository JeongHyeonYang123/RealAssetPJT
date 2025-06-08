<script setup>
import {ref} from 'vue'

const name = ref('')
const email = ref('')
const phone = ref('')
const resultMsg = ref('')
const loading = ref(false)

const handleSubmit = async (e) => {
  e.preventDefault()
  resultMsg.value = ''
  loading.value = true

  // 실제 API 엔드포인트로 교체 필요
  try {
    const res = await fetch('/user/profile/recover', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({
        name: name.value,
        email: email.value,
        phone: phone.value
      })
    })
    const data = await res.json()
    if (res.ok) {
      resultMsg.value = data.message || '비밀번호 재설정 메일을 발송했습니다.'
    } else {
      resultMsg.value = data.message || '정보가 일치하지 않습니다.'
    }
  } catch (err) {
    resultMsg.value = '서버 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="find-password-container">
    <div class="find-password-box">
      <h2>비밀번호 찾기</h2>
      <form @submit="handleSubmit" id="findPasswordForm">
        <label for="name">이름</label>
        <input type="text" id="name" v-model="name" required/>

        <label for="email">이메일</label>
        <input type="email" id="email" v-model="email" required/>

        <label for="phone">전화번호</label>
        <input type="tel" id="phone" v-model="phone" required/>

        <button type="submit" class="find-password-btn" :disabled="loading">
          {{ loading ? '처리 중...' : '비밀번호 찾기 ✔' }}
        </button>
      </form>
      <p v-if="resultMsg" style="color: #007bff; margin-top: 16px;">{{ resultMsg }}</p>
    </div>
  </main>
</template>

<style scoped>
@import '@/assets/css/password_recovery.css';
</style>
