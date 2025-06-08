import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import KakaoCallback from '@/views/KakaoCallback.vue'
import { useMemberStore } from '@/store/member.ts'
import InterestAreaView from '@/views/InterestAreaView.vue'
import AptCommercialView from '@/views/AptCommercialView.vue'
import ChangePasswordView from '@/views/ChangePasswordView.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue')
  },
  {
    path: '/verify',
    name: 'email-verify',
    component: () => import('../views/EmailVerifyView.vue')
  },
  {
    path: '/find-password',
    name: 'find-password',
    component: () => import('../views/FindPasswordView.vue')
  },
  {
    path: '/property/:id',
    name: 'property-detail',
    component: () => import('../views/PropertyDetailView.vue')
  },
  {
    path: '/search',
    name: 'search',
    component: () => import('../views/SearchResultView.vue')
  },
  {
    path: '/market-price',
    name: 'market-price',
    component: () => import('../views/MarketPriceView.vue')
  },
  {
    path: '/news',
    name: 'news',
    component: () => import('../views/NewsView.vue')
  },
  {
    path: '/community',
    name: 'community',
    component: () => import('../views/CommunityView.vue')
  },
  {
    path: '/community/write',
    name: 'community-write',
    component: () => import('../views/PostWriteView.vue'),
    meta: { requireAuth: true }
  },
  {
    path: '/community/:id',
    name: 'community-detail',
    component: () => import('../views/PostDetailView.vue')
  },
  {
    path: '/community/edit/:id',
    name: 'community-edit',
    component: () => import('../views/PostWriteView.vue'),
    meta: { requireAuth: true }
  },
  {
    path: '/mypage',
    name: 'mypage',
    component: () => import('../views/MyPageView.vue'),
    meta: { requireAuth: true }
  },
  {
    path: '/profile/edit',
    name: 'profile-edit',
    component: () => import('../views/ProfileEditView.vue'),
    meta: { requireAuth: true }
  },
  {
    path: '/oauth/callback/kakao',
    name: 'kakao-callback',
    component: KakaoCallback
  },
  {
    path: '/my-posts',
    name: 'my-posts',
    component: () => import('../views/MyPostsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/my-comments',
    name: 'my-comments',
    component: () => import('../views/MyCommentsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/interest-properties',
    name: 'interest-properties',
    component: () => import('@/views/InterestPropertiesView.vue')
  },
  {
    path: '/interest-area',
    name: 'interest-area',
    component: InterestAreaView,
    meta: { requiresAuth: true }
  },
  {
    path: '/apt-commercial',
    name: 'AptCommercialView',
    component: AptCommercialView,
    meta: { requiresAuth: false }
  },
  {
    path: '/change-password',
    name: 'change-password',
    component: ChangePasswordView,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

router.beforeEach((to) => {
  const memberStore = useMemberStore()
  if (to.meta.requireAuth && !memberStore.isLoggedIn) {
    return { name: 'login', query: { to: to.path } }
  }
})

export default router
