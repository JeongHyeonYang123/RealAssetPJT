<template>
  <header class="app-header">
    <div class="container header-container">
      <div class="logo">
        <router-link to="/">
                    <span class="logo-icon" aria-label="home">
                        <svg
                            width="28"
                            height="28"
                            viewBox="0 0 24 24"
                            fill="none"
                        >
                            <path
                                d="M3 10.5L12 4L21 10.5V20C21 20.55 20.55 21 20 21H4C3.45 21 3 20.55 3 20V10.5Z"
                                stroke="#2d60e8"
                                stroke-width="2"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                            />
                            <path
                                d="M9 21V12H15V21"
                                stroke="#2d60e8"
                                stroke-width="2"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                            />
                        </svg>
                    </span>
          <span class="logo-text">SSAFY HOME</span>
        </router-link>
      </div>

      <nav
          class="main-nav"
          :class="{ 'mobile-nav-open': isMobileNavOpen }"
      >
        <ul class="nav-list">
          <li><router-link to="/">홈</router-link></li>
          <li><router-link to="/search">매물검색</router-link></li>
          <li><router-link to="/market-price">시세</router-link></li>
          <li><router-link to="/news">뉴스</router-link></li>
          <li><router-link to="/community">커뮤니티</router-link></li>
        </ul>
      </nav>

      <div class="header-actions">
        <template v-if="memberStore.isLoggedIn">
          <span class="user-name">{{ memberStore.loginUser.name }}님</span>
          <button
              @click="goMyPage"
              class="btn btn-outline mypage-btn"
          >
            마이페이지
          </button>
        </template>
        <template v-else>
          <router-link to="/login" class="btn btn-outline login-btn">
            로그인
          </router-link>
          <router-link to="/register" class="btn btn-primary register-btn">
            회원가입
          </router-link>
        </template>
        <button class="mobile-menu-toggle" @click="toggleMobileNav">
          <span class="bar"></span>
          <span class="bar"></span>
          <span class="bar"></span>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import {onMounted, onUnmounted, ref} from "vue";
import {useRouter} from "vue-router";
import {useMemberStore} from "@/store/member";

const router = useRouter();
const memberStore = useMemberStore();
const isMobileNavOpen = ref(false);

function toggleMobileNav() {
  isMobileNavOpen.value = !isMobileNavOpen.value;
}

async function handleLogout() {
  try {
    await memberStore.logout();
    router.push("/");
  } catch (error) {
    console.error('로그아웃 실패:', error);
    // 실패해도 클라이언트 상태는 초기화되므로 홈으로 이동
    router.push("/");
  }
}

function goMyPage() {
  router.push("/mypage");
}

// 모바일 메뉴 외부 클릭시 닫기
function handleClickOutside(event: Event) {
  const target = event.target as HTMLElement;
  if (!target.closest('.main-nav') && !target.closest('.mobile-menu-toggle')) {
    isMobileNavOpen.value = false;
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style lang="scss" scoped>
.app-header {
  height: 60px;
  background-color: var(--color-white);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-container {
  width: 100%;
  max-width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding-left: 0;
  padding-right: 0;
}

.logo {
  margin-left: 80px;
  a {
    display: flex;
    align-items: center;
    text-decoration: none;
  }
  .logo-icon {
    display: flex;
    align-items: center;
    margin-right: 8px;
  }
  .logo-text {
    font-size: 1.5rem;
    font-weight: 900;
    color: var(--color-primary);
    letter-spacing: 0.02em;
  }
}

.header-actions {
  margin-right: 80px;
  display: flex;
  align-items: center;
  gap: 12px;

  .user-name {
    font-weight: 500;
    color: var(--color-gray-800);
    margin-right: 8px;
  }

  .btn {
    padding: 8px 16px;
    font-size: 0.875rem;
    border-radius: 6px;
    text-decoration: none;
    border: none;
    cursor: pointer;
    transition: all 0.2s;

    &.btn-outline {
      background: transparent;
      border: 1px solid var(--color-gray-300);
      color: var(--color-gray-700);

      &:hover {
        background: var(--color-gray-50);
        border-color: var(--color-primary);
        color: var(--color-primary);
      }
    }

    &.btn-primary {
      background: var(--color-primary);
      color: white;
      border: 1px solid var(--color-primary);

      &:hover {
        background: var(--color-primary-dark);
        border-color: var(--color-primary-dark);
      }
    }
  }
}

.main-nav {
  margin-left: var(--space-4);
  .nav-list {
    display: flex;
    list-style: none;
    margin: 0;
    padding: 0;

    li {
      margin: 0 var(--space-2);

      a {
        color: var(--color-gray-800);
        font-size: 1rem;
        font-weight: 500;
        position: relative;
        transition: color var(--transition-fast);
        text-decoration: none;
        padding: 4px 0;

        &::after {
          content: "";
          position: absolute;
          bottom: -4px;
          left: 0;
          width: 0;
          height: 2px;
          background-color: var(--color-primary);
          transition: width var(--transition-fast);
        }

        &:hover,
        &.router-link-active {
          color: var(--color-primary);
          &::after {
            width: 100%;
          }
        }
      }
    }
  }
}

.mobile-menu-toggle {
  display: none;
  flex-direction: column;
  justify-content: space-between;
  width: 24px;
  height: 18px;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
  margin-left: var(--space-2);

  .bar {
    height: 2px;
    width: 100%;
    background-color: var(--color-gray-800);
    border-radius: 1px;
    transition: transform var(--transition-fast),
    opacity var(--transition-fast);
  }
}

@media (max-width: 768px) {
  .logo {
    margin-left: 20px;
  }

  .header-actions {
    margin-right: 20px;

    .user-name,
    .register-btn {
      display: none;
    }
  }

  .main-nav {
    position: fixed;
    top: 60px;
    left: 0;
    width: 100%;
    height: 0;
    background-color: var(--color-white);
    overflow: hidden;
    transition: height var(--transition-normal);
    margin-left: 0;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);

    &.mobile-nav-open {
      height: auto;
    }

    .nav-list {
      flex-direction: column;
      padding: var(--space-2);

      li {
        margin: 0;

        a {
          display: block;
          padding: var(--space-2) 0;

          &::after {
            display: none;
          }
        }
      }
    }
  }

  .mobile-menu-toggle {
    display: flex;

    &.mobile-nav-open {
      .bar:nth-child(1) {
        transform: translateY(8px) rotate(45deg);
      }

      .bar:nth-child(2) {
        opacity: 0;
      }

      .bar:nth-child(3) {
        transform: translateY(-8px) rotate(-45deg);
      }
    }
  }
}

// Force .container to be full width in header
.container.header-container {
  max-width: 100% !important;
  width: 100% !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
}
</style>
