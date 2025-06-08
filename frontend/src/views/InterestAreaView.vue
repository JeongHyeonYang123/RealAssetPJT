<template>
  <div class="interest-area-view">
    <div class="container">
      <h1>관심지역 설정</h1>

      <div class="content-wrapper">
        <!-- 관심지역 추가 폼 -->
        <div class="form-section">
          <h2>새 관심지역 추가</h2>
          <div class="location-selector">
            <select v-model="selectedSido" @change="onSidoChange">
              <option value="">시/도</option>
              <option v-for="sido in sidoList" :key="sido.admCode" :value="sido.admCode">
                {{ sido.lowestAdmCodeNm }}
              </option>
            </select>
            <select v-model="selectedGugun" @change="onGugunChange" :disabled="!selectedSido">
              <option value="">구/군</option>
              <option v-for="gugun in gugunList" :key="gugun.admCode" :value="gugun.admCode">
                {{ gugun.lowestAdmCodeNm }}
              </option>
            </select>
            <select v-model="selectedDong" @change="onDongChange" :disabled="!selectedGugun">
              <option value="">동</option>
              <option v-for="dong in dongList" :key="dong.admCode" :value="dong.admCode">
                {{ dong.lowestAdmCodeNm }}
              </option>
            </select>
            <button class="btn btn-primary" @click="saveInterestArea" :disabled="!selectedDong">
              관심지역 추가
            </button>
          </div>
        </div>

        <!-- 관심지역 목록 -->
        <div class="list-section">
          <h2>관심지역 목록</h2>
          <div v-if="isLoading" class="loading-indicator">불러오는 중...</div>
          <div v-else-if="interestAreas.length === 0" class="no-areas">
            <p>등록된 관심지역이 없습니다.</p>
          </div>
          <div v-else class="area-list">
            <div v-for="area in interestAreas" :key="area.id" class="area-card">
              <div class="area-info">
                <h3>{{ formatAddress(area) }}</h3>
              </div>
              <button class="btn btn-outline delete-btn" @click="deleteInterestArea(area.id)">
                삭제
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { memberAi } from '@/axios';
import { useMemberStore } from '@/store/member';

const router = useRouter();
const memberStore = useMemberStore();
const isLoading = ref(false);
const interestAreas = ref<any[]>([]);

// 지역 선택 관련 상태
const selectedSido = ref('');
const selectedGugun = ref('');
const selectedDong = ref('');
const sidoList = ref<any[]>([]);
const gugunList = ref<any[]>([]);
const dongList = ref<any[]>([]);

// 지역 데이터 로드
onMounted(async () => {
  if (!memberStore.isLoggedIn) {
    alert('로그인이 필요한 서비스입니다.');
    router.push('/login');
    return;
  }
  await loadSidoList();
  await loadInterestAreas();
});

// 시/도 목록 로드
async function loadSidoList() {
  try {
    const response = await memberAi.get('/api/v1/adm/sido');
    if (response.data.success && response.data.data.admVOList?.admVOList) {
      sidoList.value = response.data.data.admVOList.admVOList;
    } else {
      console.error('시도 정보를 불러오는데 실패했습니다.');
    }
  } catch (error: any) {
    console.error('시도 정보를 불러오는데 실패했습니다:', error);
    if (error.response?.status === 401) {
      alert('로그인이 필요한 서비스입니다.');
      router.push('/login');
    }
  }
}

// 구/군 목록 로드
async function onSidoChange() {
  selectedGugun.value = '';
  selectedDong.value = '';
  gugunList.value = [];
  dongList.value = [];

  if (!selectedSido.value) return;

  try {
    const response = await memberAi.get(`/api/v1/adm/gugun?sidoCode=${selectedSido.value}`);
    if (response.data.success && response.data.data.admVOList?.admVOList) {
      gugunList.value = response.data.data.admVOList.admVOList;
    } else {
      console.error('구군 정보를 불러오는데 실패했습니다.');
    }
  } catch (error: any) {
    console.error('구군 정보를 불러오는데 실패했습니다:', error);
    if (error.response?.status === 401) {
      alert('로그인이 필요한 서비스입니다.');
      router.push('/login');
    }
  }
}

// 동 목록 로드
async function onGugunChange() {
  selectedDong.value = '';
  dongList.value = [];

  if (!selectedGugun.value) return;

  try {
    const response = await memberAi.get(`/api/v1/adm/dong?gugunCode=${selectedGugun.value}`);
    if (response.data.success && response.data.data.admVOList?.admVOList) {
      dongList.value = response.data.data.admVOList.admVOList;
    } else {
      console.error('읍면동 정보를 불러오는데 실패했습니다.');
    }
  } catch (error: any) {
    console.error('읍면동 정보를 불러오는데 실패했습니다:', error);
    if (error.response?.status === 401) {
      alert('로그인이 필요한 서비스입니다.');
      router.push('/login');
    }
  }
}

// onDongChange 함수 추가
function onDongChange() {
  // 동 선택 시 필요한 추가 작업이 있다면 여기에 구현
  console.log('Selected dong:', selectedDong.value);
}

// 관심지역 목록 로드
async function loadInterestAreas() {
  isLoading.value = true;
  try {
    const response = await memberAi.get('/api/v1/interest-areas');
    if (response.data.success) {
      // 각 관심지역의 전체 주소 정보 조회
      const areasWithAddress = await Promise.all(
        response.data.data.map(async (area: any) => {
          try {
            const dongResponse = await memberAi.get(`/api/v1/adm/dongcode?dongCode=${area.dongCode}`);
            if (dongResponse.data.success) {
              const addressData = dongResponse.data.data;
              // 상권 정보 조회
              const commercialResponse = await memberAi.get(`/api/v1/commercial-areas/${area.dongCode}`);
              return {
                ...area,
                fullAddress: `${addressData.sido} ${addressData.gugun} ${addressData.dong}`,
                commercialInfo: commercialResponse.data.data
              };
            }
          } catch (error) {
            console.error('주소 정보 조회 실패:', error);
          }
          return area;
        })
      );
      interestAreas.value = areasWithAddress;
    }
  } catch (error: any) {
    console.error('관심지역 목록 로드 실패:', error);
    if (error.response?.status === 401) {
      alert('로그인이 필요한 서비스입니다.');
      router.push('/login');
    }
  } finally {
    isLoading.value = false;
  }
}

// 관심지역 저장
async function saveInterestArea() {
  if (!selectedDong.value) return;

  if (!memberStore.isLoggedIn) {
    alert('로그인이 필요한 서비스입니다.');
    router.push('/login');
    return;
  }

  try {
    const response = await memberAi.post('/api/v1/interest-areas', {
      dongCode: selectedDong.value
    });

    if (response.data.success) {
      alert('관심지역이 추가되었습니다.');
      await loadInterestAreas();
      // 선택 초기화
      selectedSido.value = '';
      selectedGugun.value = '';
      selectedDong.value = '';
      gugunList.value = [];
      dongList.value = [];
    }
  } catch (error: any) {
    console.error('관심지역 저장 실패:', error);
    if (error.response?.status === 401) {
      alert('로그인이 필요한 서비스입니다.');
      router.push('/login');
    } else {
      alert('관심지역 추가에 실패했습니다.');
    }
  }
}

// 관심지역 삭제
async function deleteInterestArea(id: number) {
  if (!confirm('이 관심지역을 삭제하시겠습니까?')) return;

  if (!memberStore.isLoggedIn) {
    alert('로그인이 필요한 서비스입니다.');
    router.push('/login');
    return;
  }

  try {
    const response = await memberAi.delete(`/api/v1/interest-areas/${id}`);
    if (response.data.success) {
      alert('관심지역이 삭제되었습니다.');
      await loadInterestAreas();
    }
  } catch (error: any) {
    console.error('관심지역 삭제 실패:', error);
    if (error.response?.status === 401) {
      alert('로그인이 필요한 서비스입니다.');
      router.push('/login');
    } else {
      alert('관심지역 삭제에 실패했습니다.');
    }
  }
}

// 주소 포맷팅
function formatAddress(area: any): string {
  return area.fullAddress || '주소 정보 없음';
}
</script>

<style lang="scss" scoped>
.interest-area-view {
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

.content-wrapper {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 var(--space-4);
}

.form-section {
  background: var(--color-white);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  margin-bottom: var(--space-4);
}

.location-selector {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  select {
    padding: var(--space-2);
    border: 1px solid var(--color-gray-300);
    border-radius: var(--radius-md);
    font-size: 1rem;
    color: var(--color-gray-700);
    background-color: var(--color-white);

    &:disabled {
      background-color: var(--color-gray-100);
      cursor: not-allowed;
    }
  }

  button {
    margin-top: var(--space-2);
  }
}

.list-section {
  background: var(--color-white);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.area-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.area-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  background-color: var(--color-gray-50);

  .area-info {
    h3 {
      margin: 0;
      font-size: 1.1rem;
      color: var(--color-gray-800);
    }
  }

  .delete-btn {
    color: var(--color-danger);
    border-color: var(--color-danger);

    &:hover {
      background-color: var(--color-danger);
      color: white;
    }
  }
}

.loading-indicator {
  text-align: center;
  padding: var(--space-4);
  color: var(--color-gray-600);
}

.no-areas {
  text-align: center;
  padding: var(--space-4);
  color: var(--color-gray-600);
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
  justify-content: center;
  gap: var(--space-1);

  &.btn-primary {
    background: var(--color-primary);
    color: white;

    &:hover {
      background: var(--color-primary-dark);
    }

    &:disabled {
      background: var(--color-gray-300);
      cursor: not-allowed;
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
</style>