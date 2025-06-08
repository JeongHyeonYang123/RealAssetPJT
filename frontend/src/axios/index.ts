import type {InternalAxiosRequestConfig} from 'axios'
import axios, {AxiosError, AxiosInstance, AxiosRequestConfig, AxiosResponse} from 'axios'
import {useMemberStore} from '@/store/member'
import {useCommonStore} from '@/store/common'

const memberAi: AxiosInstance = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 30000,  // 타임아웃 증가
})

let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

function subscribeTokenRefresh(cb: (token: string) => void) {
    refreshSubscribers.push(cb);
}

function onRefreshed(token: string) {
    refreshSubscribers.forEach(cb => cb(token));
    refreshSubscribers = [];
}

memberAi.interceptors.request.use(
    async (config: InternalAxiosRequestConfig): Promise<InternalAxiosRequestConfig> => {
        console.log('[요청 발신]: ', config.method, config.url, config.data)
        handleTask(true)

        const memberStore = useMemberStore();

        // ✅ 토큰 유효성 검사 후 헤더 설정
        if (memberStore.tokens?.accessToken) {
            // 토큰 만료 여부 확인
            if (!memberStore.checkTokenValidity()) {
                console.log('토큰 만료 감지 - refresh 시도');
                try {
                    await memberStore.refresh();
                } catch (error) {
                    console.error('요청 전 토큰 갱신 실패:', error);
                    await memberStore.logout();
                    window.location.href = '/login';
                    return Promise.reject(new Error('토큰 갱신 실패'));
                }
            }

            config.headers = config.headers || {} as any;
            config.headers['Authorization'] = `Bearer ${memberStore.tokens.accessToken}`;
        }

        return config
    },
    (error: AxiosError): Promise<AxiosError> => {
        console.log('[요청 실패]: ', error)
        handleTask(false)
        return Promise.reject(error)
    },
)

memberAi.interceptors.response.use(
    (response: AxiosResponse): AxiosResponse => {
        console.log('[응답 수신 1]: ', response.status, response.data)
        handleTask(false)
        return response
    },
    async (error: AxiosError): Promise<any> => {
        console.log('[오류 수신 1]: ', error.response?.status, error.response?.data)
        handleTask(false)

        const memberStore = useMemberStore();
        const originalRequest = error.config as any;

        // ✅ 401 Unauthorized - 토큰 만료 처리 개선
        if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
            // Refresh Token이 없으면 즉시 로그아웃
            if (!memberStore.tokens?.refreshToken) {
                console.log('Refresh token 없음 - 로그아웃 처리');
                await memberStore.logout();
                if (typeof window !== 'undefined') {
                    window.location.href = '/login';
                }
                return Promise.reject(error);
            }

            // 이미 refresh 중이면 대기열에 추가
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    subscribeTokenRefresh((token: string) => {
                        if (originalRequest && token) {
                            originalRequest.headers['Authorization'] = `Bearer ${token}`;
                            resolve(memberAi(originalRequest));
                        } else {
                            reject(error);
                        }
                    });
                });
            }

            originalRequest._retry = true;
            isRefreshing = true;

            try {
                console.log('토큰 리프레시 시도');

                // ✅ memberStore의 refresh 액션 사용
                await memberStore.refresh();

                const newAccessToken = memberStore.tokens?.accessToken;

                if (newAccessToken) {
                    // ✅ 대기 중인 요청들에게 새 토큰 전달
                    onRefreshed(newAccessToken);

                    // ✅ 원본 요청에 새 토큰 적용하여 재시도
                    originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;

                    console.log('토큰 갱신 성공 - 원본 요청 재시도');
                    return memberAi(originalRequest);
                } else {
                    throw new Error('새 토큰을 받지 못했습니다');
                }

            } catch (refreshError) {
                console.log('토큰 리프레시 실패:', refreshError);

                // ✅ 실패한 모든 대기 요청들 처리
                refreshSubscribers.forEach(cb => cb(''));
                refreshSubscribers = [];

                await memberStore.logout();
                if (typeof window !== 'undefined') {
                    window.location.href = '/login';
                }
                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        }

        // ✅ 403 Forbidden - 권한 없음 처리
        if (error.response?.status === 403) {
            console.log('접근 권한 없음');
            // 특정 페이지에서는 메시지 표시
            const currentPath = typeof window !== 'undefined' ? window.location.pathname : '';
            if (currentPath.includes('/profile') || currentPath.includes('/mypage')) {
                alert('해당 정보에 접근할 권한이 없습니다.');
            }
        }

        // ✅ 기타 에러 처리
        if (error.response?.status === 500) {
            console.error('서버 내부 오류:', error.response.data);
        }

        return Promise.reject(error);
    },
)

// ✅ 인증이 필요 없는 요청용 인스턴스 (타임아웃 증가)
const memberAiNoAuth: AxiosInstance = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 30000,  // 회원가입, 비밀번호 찾기 등을 위해 증가
})

memberAiNoAuth.interceptors.request.use(
    async (config: AxiosRequestConfig): Promise<AxiosRequestConfig> => {
        console.log('[요청 발신]: ', config.method, config.url, config.data)
        handleTask(true)
        return config
    },
    (error: AxiosError): Promise<AxiosError> => {
        console.log('[요청 실패]: ', error)
        handleTask(false)
        return Promise.reject(error)
    },
)

memberAiNoAuth.interceptors.response.use(
    (response: AxiosResponse): AxiosResponse => {
        console.log('[응답 수신 2]: ', response.status, response.data)
        handleTask(false)
        return response
    },
    async (error: AxiosError): Promise<AxiosError> => {
        console.log('[오류 수신 2]: ', error)
        handleTask(false)

        // ✅ NoAuth 인스턴스에서도 기본적인 에러 처리
        if (error.response?.status === 500) {
            console.error('서버 내부 오류:', error.response.data);
        }

        return Promise.reject(error)
    },
)

const handleTask = (add: boolean): void => {
    const commonStore = useCommonStore()
    if (add) {
        commonStore.addTask()
    } else {
        commonStore.removeTask()
    }
}

export { memberAi, memberAiNoAuth }
