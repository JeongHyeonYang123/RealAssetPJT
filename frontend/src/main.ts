import {createApp} from 'vue'
import {createPinia} from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'
import './assets/styles/main.scss'
import axios from 'axios'
import {useMemberStore} from '@/store/member'
import './assets/styles/map-markers.css'
import { library } from '@fortawesome/fontawesome-svg-core';
import { faHeart as fasHeart } from '@fortawesome/free-solid-svg-icons';
import { faHeart as farHeart } from '@fortawesome/free-regular-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

// 백엔드 URL로 baseURL 설정
axios.defaults.baseURL = 'http://localhost:8080'

const app = createApp(App)
const pinia = createPinia()

pinia.use(piniaPluginPersistedstate)
app.use(pinia)

// Pinia store 초기화 (persist 설정으로 자동 복원됨)
const memberStore = useMemberStore()

// 기존 localStorage 정리 (더 이상 필요 없음)
localStorage.removeItem('accessToken')
localStorage.removeItem('user')

// 기존 액시오스 인터셉터 (store와 연동)
axios.interceptors.request.use(
    (config) => {
        // 로그인/회원가입 요청이 아닌 경우에만 토큰 추가
        if (!config.url?.includes('/api/auth/login') &&
            !config.url?.includes('/api/auth/register') &&
            !config.url?.includes('/api/auth/kakao')) {
            const accessToken = memberStore.tokens?.accessToken
            if (accessToken) {
                config.headers['Authorization'] = `Bearer ${accessToken}`
            }
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 토큰 갱신을 위한 별도 axios 인스턴스 (무한 루프 방지)
const refreshAxios = axios.create({
    baseURL: 'http://localhost:8080'
})

let isRefreshing = false
let refreshSubscribers: ((token: string) => void)[] = []

function subscribeTokenRefresh(cb: (token: string) => void) {
    refreshSubscribers.push(cb)
}

function onRefreshed(token: string) {
    refreshSubscribers.forEach(cb => cb(token))
    refreshSubscribers = []
}

// 응답 인터셉터 설정
axios.interceptors.response.use(
    (response) => {
        // 기존 로직: 응답에서 새 토큰이 있는 경우 처리
        if (response.data?.status === 401 && response.data?.accessToken) {
            const newToken = response.data.accessToken
            memberStore._tokens = {
                ...memberStore._tokens,
                accessToken: newToken
            }
            const originalRequest = response.config
            if (originalRequest) {
                originalRequest.headers['Authorization'] = `Bearer ${newToken}`
                return axios(originalRequest)
            }
        }
        return response
    },
    async (error) => {
        const originalRequest = error.config

        // 401 에러이고 재시도하지 않은 경우
        if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
            if (!memberStore.tokens?.refreshToken) {
                await memberStore.logout()
                return Promise.reject(error)
            }

            // 이미 갱신 중인 경우 대기
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    subscribeTokenRefresh((token: string) => {
                        originalRequest.headers['Authorization'] = `Bearer ${token}`
                        resolve(axios(originalRequest))
                    })
                })
            }

            originalRequest._retry = true
            isRefreshing = true

            try {
                // refresh token으로 새 토큰 발급
                const response = await refreshAxios({
                    url: '/api/auth/refresh',
                    method: 'POST',
                    headers: {
                        'Refresh-Token': memberStore.tokens.refreshToken
                    }
                })

                const newAccessToken = response.data.accessToken || response.data.data?.accessToken

                // store 업데이트 (persist로 자동 저장됨)
                memberStore._tokens = {
                    ...memberStore._tokens,
                    accessToken: newAccessToken,
                    refreshToken: response.data.refreshToken || response.data.data?.refreshToken || memberStore.tokens.refreshToken
                }

                // 대기 중인 모든 요청에 새 토큰 전달
                onRefreshed(newAccessToken)

                // 원래 요청 재시도
                originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`
                return axios(originalRequest)

            } catch (refreshError) {
                console.error('토큰 갱신 실패:', refreshError)
                await memberStore.logout()
                return Promise.reject(refreshError)
            } finally {
                isRefreshing = false
            }
        }

        return Promise.reject(error)
    }
)

// 기존 전역 axios 설정 유지
app.config.globalProperties.$axios = axios

app.use(router)

library.add(fasHeart, farHeart);
app.component('font-awesome-icon', FontAwesomeIcon);

app.mount('#app')
