<template>
  <div class="mypage-view">
    <div class="container">
      <h1>마이페이지</h1>

      <!-- 사용자 정보 (로딩 상태 없이 바로 표시) -->
      <div v-if="userInfo" class="content-wrapper">
        <div class="profile-section">
          <div class="profile-info">
            <div class="profile-image">
              <img
                  :src="userInfo.profileImage || 'https://via.placeholder.com/100'"
                  alt="프로필 이미지"
                  style="width: 100px; height: 100px; object-fit: cover; border-radius: 50%;"
              />
            </div>
            <div class="profile-details">
              <h2>{{ userInfo.name }}님</h2>
              <p>{{ getMemberType }}</p>
              <div class="profile-actions">
                <button class="btn btn-outline" @click="handleLogout">
                  로그아웃
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="activity-section">
          <h2>나의 활동</h2>
          <div class="activity-grid">
            <div class="activity-card" @click="goToInterestProperties">
              <div class="activity-icon">♥</div>
              <div class="activity-label">관심 매물</div>
              <div class="activity-count">{{ userInfo.interestCount || 0 }}</div>
            </div>

            <div class="activity-card" @click="goToMyPosts">
              <div class="activity-icon">✍</div>
              <div class="activity-label">내가 쓴 글</div>
              <div class="activity-count">{{ userInfo.postCount || 0 }}</div>
            </div>

            <div class="activity-card" @click="goToMyComments">
              <div class="activity-icon">💬</div>
              <div class="activity-label">댓글</div>
              <div class="activity-count">{{ userInfo.commentCount || 0 }}</div>
            </div>
          </div>
        </div>

        <div class="favorites-section">
          <h2>최근 관심 매물</h2>
          <div v-if="isLoading" class="loading-indicator">불러오는 중...</div>
          <div v-else-if="recentFavoriteCards.length === 0" class="no-properties">
            <p>등록된 관심 매물이 없습니다.</p>
            <button class="btn btn-primary" @click="goToPropertySearch">
              매물 둘러보기
            </button>
          </div>
          <div v-else class="property-list">
            <div
              v-for="property in recentFavoriteCards"
              :key="property.aptSeq"
              class="property-card"
              @click="goToDetail(property.aptSeq)"
            >
              <div class="property-header">
                <h3>{{ property.aptNm }}</h3>
                <button
                  class="favorite-btn"
                  @click.stop="toggleFavoriteApt(property.aptSeq)"
                  aria-label="관심 매물 등록/해제"
                >
                  <font-awesome-icon :icon="[favoriteAptSeqs.includes(property.aptSeq) ? 'fas' : 'far', 'heart']" size="lg" style="color: #ef4444;" />
                </button>
              </div>
              <div v-if="property.latestDeal" class="property-details">
                <div>거래가격: <span class="price">{{ formatPrice(property.latestDeal?.dealAmount) }}</span></div>
                <div>전용면적: {{ formatAreaWithPyung(property.execluUseAr || property.excluUseAr || property.latestDeal?.execluUseAr || property.latestDeal?.excluUseAr) }}</div>
                <div>층수: {{ property.latestDeal?.floor }}층</div>
              </div>
              <div v-else class="no-deal">거래내역 없음</div>
            </div>
          </div>
        </div>

        <div class="settings-section">
          <h2>설정</h2>
          <div class="settings-list">
            <div class="setting-item">
              <div class="setting-label">알림 설정</div>
              <div class="setting-value">
                <label class="switch">
                  <input type="checkbox" v-model="notifications" />
                  <span class="slider"></span>
                </label>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-label">마케팅 정보 수신</div>
              <div class="setting-value">
                <label class="switch">
                  <input type="checkbox" v-model="marketing" />
                  <span class="slider"></span>
                </label>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-label">관심지역 설정</div>
              <div class="setting-value">
                <button class="btn btn-outline" @click="goToInterestArea">설정</button>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-label">회원 정보 수정</div>
              <div class="setting-value">
                <button class="btn btn-outline" @click="goToProfileEdit">수정</button>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-label">비밀번호 변경</div>
              <div class="setting-value">
                <button class="btn btn-outline" @click="goToChangePassword">변경</button>
              </div>
            </div>

            <div v-if="!isKakaoUser" class="setting-item">
              <div class="setting-label">회원 탈퇴</div>
              <div class="setting-value">
                <button class="btn btn-outline" @click="handleWithdraw">탈퇴</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 에러 상태만 표시 (초기 로딩 에러 시) -->
      <div v-else-if="hasError" class="error-section">
        <div class="error-content">
          <h3>사용자 정보를 불러올 수 없습니다</h3>
          <p>{{ errorMessage }}</p>
          <div class="error-actions">
            <button class="btn btn-primary" @click="retryLoadUserInfo">다시 시도</button>
            <button class="btn btn-outline" @click="goToLogin">로그인 페이지로</button>
          </div>
        </div>
      </div>

      <!-- 첫 로딩 시에만 간단한 플레이스홀더 -->
      <div v-else class="placeholder-content">
        <div class="profile-placeholder">
          <div class="image-placeholder"></div>
          <div class="text-placeholder">
            <div class="line-placeholder long"></div>
            <div class="line-placeholder short"></div>
          </div>
        </div>
        <div class="activity-placeholder">
          <div class="card-placeholder" v-for="i in 3" :key="i"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onActivated, watch, nextTick } from "vue";
import { useRouter, useRoute } from "vue-router";
import PropertyCard from "../components/property/PropertyCard.vue";
import { useMemberStore } from "@/store/member";
import { memberAi } from "@/axios";
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

// 인터페이스 정의
interface UserInfo {
  email: string;
  name: string;
  role: string;
  address: string;
  mno: number;
  profileImage: string;
  postCount: number;
  commentCount: number;
  interestCount: number;
}

interface InterestProperty {
  id: number;
  propertyId: number;
  propertyType: string;
  aptName: string;
  address: string;
  price: number;
  area: number;
}

const router = useRouter();
const route = useRoute();
const memberStore = useMemberStore();

// 상태 관리 (로딩 상태 제거)
const userInfo = ref<UserInfo | null>(null);
const interestProperties = ref<InterestProperty[]>([]);
const favoriteAptSeqs = ref<string[]>([]);
const hasError = ref(false);
const errorMessage = ref('');
const notifications = ref(true);
const marketing = ref(false);
const isLoading = ref(false);
const recentFavoriteCards = ref<any[]>([]);

// 관심매물 최신순 5개 추출
const recentInterestCards = computed(() => {
  // createdAt 기준 내림차순 정렬 후 5개
  return [...interestProperties.value]
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 5);
});

// ✅ 초기값 설정 (memberStore에서 기본 정보로 시작)
const initializeWithMemberStore = () => {
  const loginUser = memberStore.loginUser;
  if (loginUser) {
    // 기본 정보로 초기화 (API 응답 전까지 표시)
    userInfo.value = {
      email: loginUser.email || '',
      name: loginUser.name || '사용자',
      role: loginUser.role || 'USER',
      address: loginUser.address || '',
      mno: loginUser.mno || 0,
      profileImage: loginUser.profileImage || '',
      postCount: 0,
      commentCount: 0,
      interestCount: 0
    };
  }
};

// 컴포넌트 마운트 시 실행
onMounted(async () => {
  console.log('🔄 MyPage mounted');
  initializeWithMemberStore(); // 즉시 기본 정보 표시
  await loadUserInfoSilently(); // 백그라운드에서 최신 정보 로드
  await fetchRecentFavoriteCards();
});

// keep-alive 활성화 시
onActivated(async () => {
  console.log('🔄 MyPage activated');
  await loadUserInfoSilently();
  await fetchRecentFavoriteCards();
});

// 라우트 변경 감지
watch(() => route.path, async (newPath, oldPath) => {
  if (newPath === '/mypage' && oldPath !== '/mypage') {
    console.log('🔄 MyPage 진입 감지');
    await nextTick();
    await loadUserInfoSilently();
  }
}, { immediate: false });

// ✅ 조용한 사용자 정보 로드 (로딩 상태 없음)
async function loadUserInfoSilently() {
  hasError.value = false;
  errorMessage.value = '';

  try {
    const loginUser = memberStore.loginUser;
    if (!loginUser || !loginUser.mno) {
      throw new Error('로그인 정보가 없습니다.');
    }

    console.log('📡 백그라운드 API 호출 - 사용자 mno:', loginUser.mno);

    // 캐시 방지를 위한 타임스탬프 추가
    const timestamp = new Date().getTime();
    const response = await memberAi.get(`/api/v1/users/${loginUser.mno}?_t=${timestamp}`);

    if (response.data.success) {
      // ✅ 자연스럽게 데이터 업데이트
      userInfo.value = response.data.data;
      console.log('✅ 사용자 정보 업데이트 완료:', userInfo.value);

      // 관심 매물도 조용히 로드
      await loadInterestPropertiesSilently();
    } else {
      throw new Error(response.data.message || '사용자 정보 조회 실패');
    }
  } catch (error: any) {
    console.error('❌ 사용자 정보 로드 실패:', error);

    // 초기 로딩이 아닌 경우에만 에러 표시
    if (userInfo.value) {
      console.warn('기존 데이터 유지 - 백그라운드 업데이트 실패');
      return; // 기존 데이터 유지
    }

    // 초기 로딩 실패 시에만 에러 상태 표시
    hasError.value = true;
    if (error.response?.status === 401) {
      errorMessage.value = '인증이 만료되었습니다. 다시 로그인해주세요.';
    } else if (error.response?.status === 400) {
      errorMessage.value = error.response.data.message || '사용자 정보를 찾을 수 없습니다.';
    } else {
      errorMessage.value = '네트워크 오류가 발생했습니다.';
    }
  }
}

// ✅ 조용한 관심 매물 로드
async function loadInterestPropertiesSilently() {
  try {
    const timestamp = new Date().getTime();
    const response = await memberAi.get('/api/v1/favorites', {
      params: {
        page: 0,
        size: 3,
        sortBy: 'createdAt',
        sortDirection: 'DESC',
        _t: timestamp
      }
    });

    if (response.data.success) {
      interestProperties.value = response.data.data.properties || [];
      console.log('✅ 관심 매물 업데이트 완료:', interestProperties.value.length + '개');
    }
  } catch (error) {
    console.error('❌ 관심 매물 로드 실패:', error);
    // 조용히 실패 처리
  }
}

// 재시도
async function retryLoadUserInfo() {
  hasError.value = false;
  userInfo.value = null;
  initializeWithMemberStore();
  await loadUserInfoSilently();
}

// 로그인 페이지로 이동
function goToLogin() {
  memberStore.logout();
  router.push('/login');
}

// 컴퓨티드 속성들
const isKakaoUser = computed(() => {
  const email = userInfo.value?.email;
  return email ? email.endsWith('@kakao.com') : false;
});

const getMemberType = computed(() => {
  if (isKakaoUser.value) {
    return '카카오 회원';
  }
  return '일반회원';
});

// 네비게이션 함수들
function goToInterestProperties() {
  router.push('/interest-properties');
}

function goToPropertySearch() {
  router.push('/search');
}

function viewPropertyDetail(propertyId: number) {
  router.push(`/property/${propertyId}`);
}

function goToProfileEdit() {
  router.push({ name: 'profile-edit' });
}

function goToMyPosts() {
  router.push({ name: 'my-posts' });
}

function goToMyComments() {
  router.push({ name: 'my-comments' });
}

function goToInterestArea() {
  router.push({ name: 'interest-area' });
}

function goToChangePassword() {
  router.push('/change-password');
}

// 로그아웃
async function handleLogout() {
  if (confirm('로그아웃 하시겠습니까?')) {
    await memberStore.logout();
    await router.push("/");
  }
}

// 회원 탈퇴
async function handleWithdraw() {
  if (!confirm("정말로 회원을 탈퇴하시겠습니까?")) return;

  try {
    const mno = userInfo.value?.mno;
    if (!mno) {
      alert('사용자 정보가 없습니다.');
      return;
    }

    await memberAi.delete(`/api/v1/users/${mno}`);
    alert("회원 탈퇴가 완료되었습니다.");
    await memberStore.logout();
    await router.push("/");
  } catch (error: any) {
    alert(
        error?.response?.data?.message ||
        "회원 탈퇴 중 오류가 발생했습니다."
    );
  }
}

async function fetchRecentFavoriteCards() {
  isLoading.value = true;
  try {
    const response = await memberAi.get('/api/v1/favorites');
    const favs = response.data || [];
    favoriteAptSeqs.value = favs.map((fav: any) => fav.aptSeq);
    // 최신순 3개만
    const sortedFavs = [...favs].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()).slice(0, 3);
    // 각 aptSeq별로 상세+최근거래 fetch
    const cardArr = await Promise.all(
      sortedFavs.map(async (fav: any) => {
        try {
          const aptRes = await memberAi.get(`/api/v1/house/apartment/${fav.aptSeq}`);
          const dealRes = await memberAi.get(`/api/v1/house/deals/${fav.aptSeq}`);
          const latestDeal = Array.isArray(dealRes.data) && dealRes.data.length > 0 ? dealRes.data[0] : null;
          return {
            ...fav,
            ...aptRes.data.data,
            latestDeal
          };
        } catch (e) {
          return { ...fav, latestDeal: null };
        }
      })
    );
    recentFavoriteCards.value = cardArr;
  } finally {
    isLoading.value = false;
  }
}

async function toggleFavoriteApt(aptSeq: string) {
  try {
    if (favoriteAptSeqs.value.includes(aptSeq)) {
      const response = await memberAi.get('/api/v1/favorites');
      const favorite = response.data.find((fav: any) => fav.aptSeq === aptSeq);
      if (favorite) {
        await memberAi.delete(`/api/v1/favorites/${favorite.id}`);
        await fetchRecentFavoriteCards();
      }
    } else {
      await memberAi.post('/api/v1/favorites', {
        aptSeq,
        userMno: null // 백엔드에서 JWT로 추출
      });
      await fetchRecentFavoriteCards();
    }
  } catch (e) {}
}

function goToDetail(aptSeq: string) {
  router.push({ path: '/', query: { aptSeq } });
}

function formatPrice(amount: string | number): string {
  let numAmount = typeof amount === 'string' ? parseFloat(amount.replace(/,/g, '')) : amount;
  if (isNaN(numAmount)) return String(amount);
  if (numAmount >= 10000) {
    const eok = Math.floor(numAmount / 10000);
    const man = numAmount % 10000;
    return man > 0 ? `${eok}억 ${man.toLocaleString()}만원` : `${eok}억원`;
  }
  return `${numAmount.toLocaleString()}만원`;
}

function formatAreaWithPyung(area: string | number): string {
  const numArea = typeof area === 'string' ? parseFloat(area) : area;
  if (isNaN(numArea)) return String(area);
  const pyung = (numArea * 0.3025).toFixed(2);
  return `${pyung}평 (${numArea}㎡)`;
}
</script>

<style lang="scss" scoped>
.mypage-view {
  padding: var(--space-4) 0;
  min-height: 100vh;

  h1 {
    margin-bottom: var(--space-4);
    text-align: center;
    color: var(--color-gray-900);
    font-size: 2rem;
    font-weight: 700;
  }

  h2 {
    margin-bottom: var(--space-3);
    color: var(--color-gray-800);
    font-size: 1.5rem;
    font-weight: 600;
  }
}

/* ✅ 플레이스홀더 스켈레톤 UI */
.placeholder-content {
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeIn 0.3s ease;
}

.profile-placeholder {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  background: var(--color-white);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  margin-bottom: var(--space-4);

  .image-placeholder {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: loading 1.5s infinite;
  }

  .text-placeholder {
    flex: 1;

    .line-placeholder {
      height: 20px;
      border-radius: 4px;
      background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: loading 1.5s infinite;
      margin-bottom: var(--space-2);

      &.long {
        width: 200px;
        height: 24px;
      }

      &.short {
        width: 120px;
        height: 16px;
      }
    }
  }
}

.activity-placeholder {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-3);

  .card-placeholder {
    height: 120px;
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: loading 1.5s infinite;
    border-radius: var(--radius-lg);
  }
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.error-section {
  text-align: center;
  padding: var(--space-8) 0;

  .error-content {
    max-width: 400px;
    margin: 0 auto;

    h3 {
      color: var(--color-gray-800);
      margin-bottom: var(--space-2);
    }

    p {
      color: var(--color-gray-600);
      margin-bottom: var(--space-4);
      line-height: 1.6;
    }
  }

  .error-actions {
    display: flex;
    gap: var(--space-2);
    justify-content: center;
  }
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeIn 0.5s ease;
}

.profile-section {
  background: var(--color-white);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  margin-bottom: var(--space-4);
}

.profile-info {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.profile-image {
  img {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    object-fit: cover;
    border: 3px solid var(--color-gray-200);
  }
}

.profile-details {
  flex: 1;

  h2 {
    margin-bottom: var(--space-1);
    color: var(--color-gray-900);
  }

  p {
    color: var(--color-gray-600);
    margin-bottom: var(--space-3);
    font-size: 1rem;
  }
}

.profile-actions {
  display: flex;
  gap: var(--space-2);
}

.activity-section {
  margin-bottom: var(--space-4);
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-3);
}

.activity-card {
  background: var(--color-white);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-lg);
  }

  .activity-icon {
    font-size: 2.5rem;
    margin-bottom: var(--space-2);
  }

  .activity-label {
    color: var(--color-gray-600);
    margin-bottom: var(--space-1);
    font-weight: 500;
  }

  .activity-count {
    font-size: 2rem;
    font-weight: 700;
    color: var(--color-primary);
  }
}

.favorites-section {
  margin-bottom: var(--space-4);
}

.no-properties {
  text-align: center;
  padding: var(--space-6) 0;
  color: var(--color-gray-600);

  p {
    margin-bottom: var(--space-3);
  }
}

.property-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.property-card {
  background: white;
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-gray-200);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.property-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.property-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-2);
}

.property-header h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0;
}

.favorite-btn {
  background: transparent;
  border: none;
  padding: 0 0 0 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.property-details {
  font-size: 0.98rem;
  color: #333;
  margin-bottom: 4px;
}

.price {
  color: #2d60e8;
  font-weight: 700;
}

.no-deal {
  color: #aaa;
  font-size: 0.95rem;
  margin-top: 12px;
}

.loading-indicator {
  text-align: center;
  color: var(--color-gray-600);
  padding: 40px 0;
}

.settings-section {
  background: var(--color-white);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.settings-list {
  .setting-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 0;
    border-bottom: 1px solid var(--color-gray-200);

    &:last-child {
      border-bottom: none;
    }
  }

  .setting-label {
    font-weight: 500;
    color: var(--color-gray-700);
  }
}

.switch {
  position: relative;
  display: inline-block;
  width: 60px;
  height: 34px;

  input {
    opacity: 0;
    width: 0;
    height: 0;

    &:checked + .slider {
      background-color: var(--color-primary);
    }

    &:checked + .slider:before {
      transform: translateX(26px);
    }
  }

  .slider {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: var(--color-gray-300);
    transition: var(--transition-fast);
    border-radius: 34px;

    &:before {
      position: absolute;
      content: "";
      height: 26px;
      width: 26px;
      left: 4px;
      bottom: 4px;
      background-color: white;
      transition: var(--transition-fast);
      border-radius: 50%;
    }
  }
}

.btn {
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);

  &.btn-primary {
    background: var(--color-primary);
    color: white;

    &:hover {
      background: var(--color-primary-dark);
    }
  }

  &.btn-outline {
    background: transparent;
    color: var(--color-gray-700);
    border: 1px solid var(--color-gray-300);

    &:hover {
      background: var(--color-gray-50);
    }
  }
}

@media (max-width: 768px) {
  .profile-info {
    flex-direction: column;
    text-align: center;
  }

  .activity-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .property-grid {
    grid-template-columns: 1fr;
  }

  .profile-actions {
    justify-content: center;
  }

  .error-actions {
    flex-direction: column;

    .btn {
      width: 100%;
    }
  }
}
</style>
