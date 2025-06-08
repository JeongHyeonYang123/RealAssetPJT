import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import router from '@/router'
import {memberAi} from '@/axios'
import {jwtDecode} from 'jwt-decode'

interface LoginCredentials {
    email: string;
    password: string;
}

interface LoginUser {
    name?: string;
    email?: string;
    role?: string;
    mno?: number;
    address?: string;
    profileImage?: string; // 프로필 이미지 추가
}

interface TokenData {
    accessToken?: string | null;
    refreshToken?: string | null;
    [key: string]: any;
}

interface DecodedToken {
    name: string;
    email: string;
    role: string;
    mno: number;  // mno 추가
    exp: number;
    profileImage?: string; // 프로필 이미지 추가
    [key: string]: any;
}

export const useMemberStore = defineStore(
    'member',
    () => {
        const _isLoggedIn = ref<boolean>(false)
        const _loginUser = ref<LoginUser>({})
        const _tokens = ref<TokenData>({})

        const isLoggedIn = computed<boolean>(() => _isLoggedIn.value)
        const loginUser = computed<LoginUser>(() => _loginUser.value)
        const tokens = computed<TokenData>(() => _tokens.value)

        // ✅ 토큰에서 사용자 정보 추출하는 헬퍼 함수
        const extractUserFromToken = (token: string): LoginUser | null => {
            try {
                const decoded: DecodedToken = jwtDecode(token);
                return {
                    mno: decoded.mno,
                    name: decoded.name,
                    email: decoded.email,
                    role: decoded.role,
                    profileImage: decoded.profileImage // profileImage도 추출
                };
            } catch (error) {
                console.error('토큰 디코딩 실패:', error);
                return null;
            }
        }

        const login = async ({ email, password }: LoginCredentials): Promise<void> => {
            try {
                const response = await memberAi({
                    url: "/api/auth/login",
                    method: 'POST',
                    data: { email, password },
                    headers: { 'Content-Type': 'application/json' }
                })

                _tokens.value = {
                    accessToken: response.data.accessToken,
                    refreshToken: response.data.refreshToken
                }

                // 토큰에서 사용자 정보 추출 (백업으로 response.data 사용)
                const tokenUser = extractUserFromToken(response.data.accessToken);

                _loginUser.value = {
                    mno: tokenUser?.mno || response.data.mno,
                    name: tokenUser?.name || response.data.name,
                    email: tokenUser?.email || response.data.email,
                    role: tokenUser?.role || response.data.role,
                    address: response.data.address,
                    profileImage: tokenUser?.profileImage || response.data.profileImage // 토큰에서 추출 우선, 없으면 응답에서
                }
                _isLoggedIn.value = true

                console.log('로그인 성공:', _loginUser.value)

            } catch (e: any) {
                _isLoggedIn.value = false
                _loginUser.value = {}
                _tokens.value = {}
                console.error('로그인 실패:', e)
                throw new Error('로그인 실패: ' + (e.response?.data?.message || e.message))
            }
        }

        // 프로필 업데이트 액션
        const updateProfile = (profileData: Partial<LoginUser>): void => {
            if (_loginUser.value) {
                _loginUser.value = {
                    ..._loginUser.value,
                    ...profileData
                }
                console.log('프로필 업데이트 완료:', _loginUser.value)
            }
        }

        const logout = async (): Promise<void> => {
            try {
                if (_tokens.value.refreshToken) {
                    await memberAi({
                        url: "/api/auth/logout",
                        method: 'POST',
                        headers: { 'Refresh-Token': _tokens.value.refreshToken }
                    })
                }
            } catch (error) {
                console.error('로그아웃 요청 실패:', error)
            } finally {
                _loginUser.value = {}
                _isLoggedIn.value = false
                _tokens.value = {}

                console.log('로그아웃 완료')

                // 인증이 필요한 페이지에서 로그아웃된 경우 홈으로 리다이렉트
                if (router.currentRoute.value.meta?.requireAuth) {
                    router.push('/')
                }
            }
        }

        const refresh = async (): Promise<void> => {
            if (!_tokens.value.refreshToken) {
                throw new Error('Refresh token이 없습니다')
            }

            try {
                const response = await memberAi({
                    url: "/api/auth/refresh",
                    method: 'POST',
                    headers: { 'Refresh-Token': _tokens.value.refreshToken }
                })

                // 응답 구조에 따라 조정
                const newAccessToken = response.data.accessToken || response.data.data?.accessToken;
                const newRefreshToken = response.data.refreshToken || response.data.data?.refreshToken || _tokens.value.refreshToken;

                _tokens.value = {
                    accessToken: newAccessToken,
                    refreshToken: newRefreshToken
                }

                // 새로운 액세스 토큰에서 사용자 정보 업데이트
                if (newAccessToken) {
                    const tokenUser = extractUserFromToken(newAccessToken);
                    if (tokenUser) {
                        _loginUser.value = {
                            ..._loginUser.value,
                            ...tokenUser
                        }
                    }
                }

                console.log('토큰 갱신 성공')

            } catch (error) {
                console.error('토큰 갱신 실패:', error)
                await logout()
                throw error
            }
        }

        const kakaoLogin = async (code: string): Promise<void> => {
            try {
                const response = await memberAi.post("/api/auth/kakao", { code })
                const { accessToken, refreshToken, ...user } = response.data

                _tokens.value = { accessToken, refreshToken }

                // 토큰에서 사용자 정보 추출 (백업으로 response.data 사용)
                const tokenUser = extractUserFromToken(accessToken);

                _loginUser.value = {
                    mno: tokenUser?.mno || user.mno,
                    name: tokenUser?.name || user.name,
                    email: tokenUser?.email || user.email,
                    role: tokenUser?.role || user.role,
                    address: user.address,
                    profileImage: tokenUser?.profileImage || user.profileImage // 토큰에서 추출 우선, 없으면 응답에서
                }
                _isLoggedIn.value = true

                console.log('카카오 로그인 성공:', _loginUser.value)

            } catch (error) {
                _tokens.value = {}
                _loginUser.value = {}
                _isLoggedIn.value = false
                console.error('카카오 로그인 실패:', error)
                throw error
            }
        }

        // 토큰 유효성 검사 개선
        const checkTokenValidity = (): boolean => {
            if (!_tokens.value.accessToken) return false

            try {
                const decoded: DecodedToken = jwtDecode(_tokens.value.accessToken)
                const currentTime = Date.now() / 1000

                // 토큰에서 mno 확인하여 로그인 사용자와 일치하는지 검증
                if (decoded.mno && _loginUser.value.mno && decoded.mno !== _loginUser.value.mno) {
                    console.warn('토큰의 mno와 로그인 사용자 mno가 불일치')
                    return false
                }

                return decoded.exp > currentTime
            } catch (error) {
                console.error('토큰 검증 실패:', error)
                return false
            }
        }

        // 토큰에서 사용자 정보 동기화하는 함수 추가
        const syncUserFromToken = (): void => {
            if (_tokens.value.accessToken) {
                const tokenUser = extractUserFromToken(_tokens.value.accessToken);
                if (tokenUser) {
                    _loginUser.value = {
                        ..._loginUser.value,
                        ...tokenUser
                    }
                    console.log('토큰에서 사용자 정보 동기화 완료:', _loginUser.value)
                }
            }
        }

        // 토큰 시간 관련 (기존 코드 유지)
        const tokenTime = ref<number>(0)
        let intervalId: number | undefined

        const resetTokenTime = (): void => {
            window.clearInterval(intervalId)
            tokenTime.value = 0
            intervalId = window.setInterval(() => {
                tokenTime.value++
            }, 1000)
        }

        const tokenStatus = computed<string>(() => {
            if (!_tokens.value.refreshToken) {
                window.clearInterval(intervalId)
                tokenTime.value = 0
                return '로그아웃 상태'
            }
            if (tokenTime.value > 120) {
                return 'refresh token 만료'
            } else if (tokenTime.value > 60) {
                return 'access token 만료'
            } else {
                return 'token 유효'
            }
        })

        return {
            isLoggedIn,
            loginUser,
            tokens,
            login,
            logout,
            refresh,
            kakaoLogin,
            updateProfile,
            checkTokenValidity,
            syncUserFromToken,  // 새로 추가된 함수
            extractUserFromToken,  // 새로 추가된 함수
            tokenTime,
            tokenStatus,
            resetTokenTime,
            // 내부 상태 (필요한 경우에만 노출)
            _isLoggedIn,
            _loginUser,
            _tokens
        }
    },
    {
        persist: {
            storage: sessionStorage
        }
    }
)
