<template>
  <div class="verification-container">
    <div v-if="loading" class="loading-section">
      <div class="spinner"></div>
      <h3>이메일 인증 처리중...</h3>
    </div>

    <div v-else-if="verified" class="success-section">
      <div class="icon success-icon">✅</div>
      <h2>이메일 인증 완료!</h2>
      <p>회원가입이 성공적으로 완료되었습니다.</p>
      <button @click="goToLogin" class="btn btn-primary">
        로그인 하기
      </button>
    </div>

    <div v-else class="error-section">
      <div class="icon error-icon">❌</div>
      <h2>인증 실패</h2>
      <p>{{ errorMessage }}</p>
      <div class="button-group">
        <button @click="goToRegister" class="btn btn-secondary">
          다시 회원가입
        </button>
        <button @click="goToHome" class="btn btn-outline">
          홈으로
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'EmailVerification',
  data() {
    return {
      loading: true,
      verified: false,
      errorMessage: '유효하지 않거나 만료된 링크입니다.'
    }
  },
  async mounted() {
    await this.verifyToken()
  },
  methods: {
    async verifyToken() {
      const token = this.$route.query.token

      if (!token) {
        this.loading = false
        this.verified = false
        this.errorMessage = '인증 토큰이 없습니다.'
        return
      }

      try {
        const response = await axios.get(`/api/v1/users/verify?token=${token}`)
        this.verified = true
      } catch (error) {
        this.verified = false
        if (error.response?.data?.error) {
          this.errorMessage = error.response.data.error
        }
      } finally {
        this.loading = false
      }
    },

    goToLogin() {
      this.$router.push('/login')
    },

    goToRegister() {
      this.$router.push('/register')
    },

    goToHome() {
      this.$router.push('/')
    }
  }
}
</script>

<style scoped>
.verification-container {
  max-width: 500px;
  margin: 100px auto;
  text-align: center;
  padding: 40px 20px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background: white;
}

.loading-section {
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 20px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.success-section {
  color: #28a745;
}

.error-section {
  color: #dc3545;
}

.icon {
  font-size: 80px;
  margin-bottom: 20px;
  display: block;
}

.success-icon {
  color: #28a745;
}

.error-icon {
  color: #dc3545;
}

h2 {
  margin: 20px 0;
  font-weight: 600;
  font-size: 24px;
}

h3 {
  margin: 10px 0;
  font-weight: 500;
  font-size: 18px;
}

p {
  font-size: 16px;
  margin: 15px 0;
  color: #666;
  line-height: 1.5;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
  margin: 8px;
  transition: all 0.3s ease;
}

.btn-primary {
  background-color: #007bff;
  color: white;
}

.btn-primary:hover {
  background-color: #0056b3;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background-color: #545b62;
}

.btn-outline {
  background-color: transparent;
  color: #007bff;
  border: 2px solid #007bff;
}

.btn-outline:hover {
  background-color: #007bff;
  color: white;
}

.button-group {
  margin-top: 20px;
}

.button-group .btn {
  margin: 0 5px;
}

@media (max-width: 768px) {
  .verification-container {
    margin: 50px 20px;
    padding: 30px 15px;
  }

  .icon {
    font-size: 60px;
  }

  h2 {
    font-size: 20px;
  }

  .btn {
    width: 100%;
    margin: 5px 0;
  }
}
</style>
