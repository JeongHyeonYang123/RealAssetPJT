<template>
  <div class="search-result-view">
    <div class="search-header">
      <div class="container">
        <h1>매물 검색 결과</h1>

        <div class="search-form">
          <!-- 지역 선택 드롭다운 추가 -->
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
          </div>

          <div class="search-input">
            <input
                type="text"
                v-model="searchQuery"
                placeholder="아파트명으로 검색"
                @keyup.enter="search"
            />
            <button
                class="search-button"
                @click="search"
                :disabled="isLoading"
            >
              {{ isLoading ? "검색중..." : "검색" }}
            </button>
          </div>

          <div class="filter-controls">
            <div class="filter-group">
              <label>가격대</label>
              <select
                  v-model="priceFilter"
                  @change="onFilterChange"
              >
                <option value="all">전체</option>
                <option value="under1">1억 이하</option>
                <option value="1to3">1억~3억</option>
                <option value="3to5">3억~5억</option>
                <option value="5to10">5억~10억</option>
                <option value="over10">10억 이상</option>
              </select>
            </div>

            <div class="filter-group">
              <label>면적</label>
              <select
                  v-model="areaFilter"
                  @change="onFilterChange"
              >
                <option value="all">전체</option>
                <option value="small">18평 이하 (60㎡)</option>
                <option value="medium">18평~26평 (60㎡~85㎡)</option>
                <option value="large">26평 초과 (85㎡)</option>
              </select>
            </div>

            <div class="filter-group">
              <label>정렬</label>
              <select
                  v-model="sortOrder"
                  @change="onFilterChange"
              >
                <option value="price-asc">가격 낮은순</option>
                <option value="price-desc">가격 높은순</option>
                <option value="date-desc">최신순</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="container">
      <div class="search-results-container">
        <!-- 검색 결과 요약 -->
        <div v-if="searchResults && !isLoading" class="search-summary">
          <p>
            {{ searchResults.totalCount }}건의 거래내역을
            찾았습니다.
          </p>
        </div>

        <!-- 로딩 -->
        <div class="loading-indicator" v-if="isLoading">
          <p>검색 결과를 불러오는 중...</p>
        </div>

        <!-- 에러 -->
        <div v-else-if="error" class="error-message">
          <p>{{ error }}</p>
          <button @click="search" class="retry-button">
            다시 시도
          </button>
        </div>

        <!-- 검색 결과 -->
        <div
            v-else-if="
                        searchResults && searchResults.properties.length > 0
                    "
            class="property-list"
        >
          <property-card
              v-for="property in searchResults.properties"
              :key="property.no"
              :property="enhancePropertyWithPyung(property)"
              :favorite-apt-seqs="favoriteAptSeqs"
              :toggle-favorite-apt="toggleFavoriteApt"
              @click="viewPropertyDetail(property.aptSeq)"
          />
        </div>

        <!-- 검색 결과 없음 -->
        <div
            v-else-if="
                        searchResults && searchResults.properties.length === 0
                    "
            class="empty-results"
        >
          <p>검색 결과가 없습니다.</p>
          <p>다른 조건으로 검색해 보세요.</p>
        </div>

        <!-- 페이지네이션 -->
        <div
            v-if="searchResults && searchResults.properties.length > 0"
            class="pagination"
        >
          <button
              class="pagination-button"
              :disabled="currentPage === 1 || isLoading"
              @click="goToPage(currentPage - 1)"
          >
            이전
          </button>

          <div class="page-numbers">
            <button
                v-for="page in visiblePages"
                :key="page"
                :class="[
                                'page-number',
                                { active: page === currentPage },
                            ]"
                @click="goToPage(page)"
                :disabled="isLoading"
            >
              {{ page }}
            </button>
          </div>

          <button
              class="pagination-button"
              :disabled="
                            currentPage === searchResults.totalPages ||
                            isLoading
                        "
              @click="goToPage(currentPage + 1)"
          >
            다음
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import PropertyCard from "../components/property/PropertyCard.vue";
import {
  type HouseDeal,
  propertyApi,
  type PropertySearchResponse,
} from "../../services/propertyApi";
import { admApi } from "../../services/admApi";
import { memberAi } from "@/axios";
import { useMemberStore } from "@/store/member";

const router = useRouter();
const route = useRoute();
const memberStore = useMemberStore();

// 반응형 데이터
const searchQuery = ref("");
const isLoading = ref(false);
const error = ref("");
const priceFilter = ref("all");
const areaFilter = ref("all");
const sortOrder = ref("date-desc");
const currentPage = ref(1);
const searchResults = ref<PropertySearchResponse | null>(null);

// 지역 선택 관련 데이터 - 항상 초기화됨
const selectedSido = ref("");
const selectedGugun = ref("");
const selectedDong = ref("");
const sidoList = ref([]);
const gugunList = ref([]);
const dongList = ref([]);

const itemsPerPage = 10;

const favoriteAptSeqs = ref<string[]>([]);

// ✅ 평수 변환 함수들 추가
function squareMeterToPyung(area: number): number {
  return area * 0.3025;
}

function formatPyung(area: number): string {
  const pyung = squareMeterToPyung(area);
  return pyung.toFixed(1);
}

function formatAreaWithPyung(area: number): string {
  const pyung = formatPyung(area);
  return `${pyung}평 (${area}㎡)`;
}

// ✅ 간단한 평수 계산 (암산용)
function quickPyungCalculation(area: number): number {
  // 끝자리 제거하고 3 곱하기 방식
  const simplified = Math.floor(area / 10);
  return simplified * 3;
}

// ✅ 매물 데이터에 평수 정보 추가
function enhancePropertyWithPyung(property: HouseDeal) {
  return {
    ...property,
    pyung: formatPyung(parseFloat(property.area)),
    areaWithPyung: formatAreaWithPyung(parseFloat(property.area)),
    quickPyung: quickPyungCalculation(parseFloat(property.area))
  };
}

onMounted(() => {
  // 시도 목록 로드
  loadSidoList();

  // URL에서 검색 파라미터 가져오기 (지역 정보 제외)
  const query = route.query.q as string;
  const page = route.query.page as string;
  const priceF = route.query.priceFilter as string;
  const areaF = route.query.areaFilter as string;
  const sort = route.query.sortOrder as string;

  if (query) {
    searchQuery.value = query;
    if (page) currentPage.value = parseInt(page) || 1;
    if (priceF) priceFilter.value = priceF;
    if (areaF) areaFilter.value = areaF;
    if (sort) sortOrder.value = sort;

    // 지역 선택값은 URL에서 복원하지 않음 (항상 초기화)
    // 검색만 실행
    search();
  }

  fetchFavoriteAptSeqs();
});

// 지역 선택값 초기화 함수
function resetLocationSelections() {
  selectedSido.value = "";
  selectedGugun.value = "";
  selectedDong.value = "";
  gugunList.value = [];
  dongList.value = [];
}

// 시도 목록 로드
async function loadSidoList() {
  try {
    const response = await admApi.getSidoList();
    if (response.success && response.data.admVOList?.admVOList) {
      sidoList.value = response.data.admVOList.admVOList;
    } else {
      showError("시도 정보를 불러오는데 실패했습니다.");
    }
  } catch (error: any) {
    showError("시도 정보를 불러오는데 실패했습니다.");
  }
}

// 시도 변경 시
async function onSidoChange() {
  selectedGugun.value = "";
  selectedDong.value = "";
  gugunList.value = [];
  dongList.value = [];

  if (selectedSido.value) {
    try {
      const response = await admApi.getGugunList(selectedSido.value);
      if (response.success && response.data.admVOList?.admVOList) {
        gugunList.value = response.data.admVOList.admVOList;
      } else {
        showError("구군 정보를 불러오는데 실패했습니다.");
      }
    } catch (error: any) {
      showError("구군 정보를 불러오는데 실패했습니다.");
    }
  }

  // 검색어가 있으면 검색 실행
  if (searchQuery.value.trim()) {
    currentPage.value = 1;
    search();
  }
}

// 구군 변경 시
async function onGugunChange() {
  selectedDong.value = "";
  dongList.value = [];

  if (selectedGugun.value) {
    try {
      const response = await admApi.getDongList(selectedGugun.value);
      if (response.success && response.data.admVOList?.admVOList) {
        dongList.value = response.data.admVOList.admVOList;
      } else {
        showError("읍면동 정보를 불러오는데 실패했습니다.");
      }
    } catch (error: any) {
      showError("읍면동 정보를 불러오는데 실패했습니다.");
    }
  }

  // 검색어가 있으면 검색 실행
  if (searchQuery.value.trim()) {
    currentPage.value = 1;
    search();
  }
}

// 동 변경 시
async function onDongChange() {
  console.log("📍 읍면동 변경:", selectedDong.value);

  // 검색어가 있으면 검색 실행
  if (searchQuery.value.trim()) {
    currentPage.value = 1;
    search();
  }
}

// 검색 함수
async function search() {
  if (!searchQuery.value.trim()) {
    error.value = "아파트명을 입력해주세요.";
    return;
  }

  isLoading.value = true;
  error.value = "";

  try {
    const response = await propertyApi.searchProperties({
      aptName: searchQuery.value,
      dongCode: selectedDong.value,
      priceFilter: priceFilter.value,
      areaFilter: areaFilter.value,
      sortOrder: sortOrder.value,
      page: currentPage.value,
      size: itemsPerPage,
    });

    if (response.success) {
      searchResults.value = response.data;
      updateURL();
    } else {
      error.value = response.message || "검색 중 오류가 발생했습니다.";
      searchResults.value = null;
    }
  } catch (err: any) {
    console.error("Search error:", err);
    error.value =
        err.response?.data?.message || "검색 중 오류가 발생했습니다.";
    searchResults.value = null;
  } finally {
    isLoading.value = false;
  }
}

// 필터 변경 시 호출
function onFilterChange() {
  if (searchQuery.value.trim()) {
    currentPage.value = 1;
    search();
  }
}

// 페이지 이동
function goToPage(page: number) {
  if (
      page < 1 ||
      (searchResults.value && page > searchResults.value.totalPages)
  )
    return;

  currentPage.value = page;
  search();
}

// URL 업데이트 - 지역 정보는 URL에 저장하지 않음
function updateURL() {
  router.push({
    path: "/search",
    query: {
      q: searchQuery.value,
      page: currentPage.value.toString(),
      priceFilter:
          priceFilter.value !== "all" ? priceFilter.value : undefined,
      areaFilter:
          areaFilter.value !== "all" ? areaFilter.value : undefined,
      sortOrder:
          sortOrder.value !== "date-desc" ? sortOrder.value : undefined,
      // 지역 정보는 URL에 저장하지 않음 (새로고침 시 초기화됨)
    },
  });
}

// 에러 표시 함수
function showError(message: string) {
  error.value = message;
}

// 보여줄 페이지 번호들 계산
const visiblePages = computed(() => {
  if (!searchResults.value) return [];

  const totalPages = searchResults.value.totalPages;
  const current = currentPage.value;
  const pages: number[] = [];

  let start = Math.max(1, current - 2);
  let end = Math.min(totalPages, start + 4);

  if (end - start < 4) {
    start = Math.max(1, end - 4);
  }

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  return pages;
});

// 상세 페이지로 이동
function viewPropertyDetail(aptSeq: string) {
  router.push({ path: '/', query: { aptSeq } });
}

async function fetchFavoriteAptSeqs() {
  if (!memberStore.isLoggedIn) {
    favoriteAptSeqs.value = [];
    return;
  }
  try {
    const response = await memberAi.get('/api/v1/favorites');
    favoriteAptSeqs.value = response.data.map((fav: any) => fav.aptSeq);
  } catch (e) {
    favoriteAptSeqs.value = [];
  }
}

async function toggleFavoriteApt(aptSeq: string) {
  if (!memberStore.isLoggedIn) {
    alert('로그인이 필요합니다.');
    return;
  }
  if (favoriteAptSeqs.value.includes(aptSeq)) {
    // 삭제
    try {
      const response = await memberAi.get('/api/v1/favorites');
      const favorite = response.data.find((fav: any) => fav.aptSeq === aptSeq);
      if (favorite) {
        await memberAi.delete(`/api/v1/favorites/${favorite.id}`);
        await fetchFavoriteAptSeqs();
      }
    } catch (e) {}
  } else {
    // 등록
    try {
      await memberAi.post('/api/v1/favorites', {
        aptSeq,
        userMno: memberStore.loginUser?.mno
      });
      await fetchFavoriteAptSeqs();
    } catch (e) {}
  }
}
</script>

<style lang="scss" scoped>
.search-result-view {
  min-height: calc(100vh - 60px);
  padding-bottom: var(--space-6);
}

.search-header {
  background-color: var(--color-primary);
  color: var(--color-white);
  padding: var(--space-4) 0;
  margin-bottom: var(--space-4);

  h1 {
    margin-bottom: var(--space-3);
    font-size: 2rem;
  }
}

.search-form {
  background-color: var(--color-white);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  box-shadow: var(--shadow-md);
}

/* 지역 선택 드롭다운 스타일 */
.location-selector {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-3);

  select {
    flex: 1;
    padding: 12px 16px;
    border: 1px solid var(--color-gray-300);
    border-radius: var(--radius-md);
    font-size: 1rem;
    background-color: var(--color-white);
    cursor: pointer;

    &:disabled {
      background-color: var(--color-gray-100);
      color: var(--color-gray-500);
      cursor: not-allowed;
    }

    &:focus {
      outline: none;
      border-color: var(--color-primary);
      box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb), 0.2);
    }
  }
}

.search-input {
  display: flex;
  margin-bottom: var(--space-3);

  input {
    flex: 1;
    border-top-right-radius: 0;
    border-bottom-right-radius: 0;
    padding: 12px 16px;
    font-size: 1rem;
  }

  .search-button {
    border-top-left-radius: 0;
    border-bottom-left-radius: 0;
    padding: 0 var(--space-3);
    background-color: var(--color-primary);
    color: white;
    border: 1px solid var(--color-primary);
    cursor: pointer;

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
}

.filter-controls {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2) var(--space-3);

  .filter-group {
    display: flex;
    align-items: center;
    gap: var(--space-1);

    label {
      font-size: 0.875rem;
      color: var(--color-gray-700);
      white-space: nowrap;
    }

    select {
      padding: 6px 12px;
      border-radius: var(--radius-md);
      border: 1px solid var(--color-gray-300);
      font-size: 0.875rem;
      min-width: 120px;
    }
  }
}

.search-summary {
  margin-bottom: var(--space-3);

  p {
    color: var(--color-gray-600);
    font-size: 0.875rem;
  }
}

.loading-indicator {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;

  p {
    color: var(--color-gray-600);
    font-size: 1.125rem;
  }
}

.error-message {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 300px;
  text-align: center;

  p {
    color: var(--color-red-600);
    font-size: 1.125rem;
    margin-bottom: var(--space-2);
  }

  .retry-button {
    padding: 8px 16px;
    background-color: var(--color-primary);
    color: white;
    border: none;
    border-radius: var(--radius-md);
    cursor: pointer;
  }
}

.empty-results {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 300px;
  text-align: center;

  p {
    color: var(--color-gray-600);
    font-size: 1.125rem;
    margin-bottom: var(--space-1);
  }
}

.property-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: var(--space-4);

  .pagination-button {
    padding: 8px 16px;
    border: 1px solid var(--color-gray-300);
    background-color: var(--color-white);
    color: var(--color-gray-700);
    border-radius: var(--radius-md);
    cursor: pointer;

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    &:hover:not(:disabled) {
      background-color: var(--color-gray-100);
    }
  }

  .page-numbers {
    display: flex;
    margin: 0 var(--space-2);

    .page-number {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 4px;
      border-radius: var(--radius-md);
      border: 1px solid var(--color-gray-300);
      background-color: var(--color-white);
      color: var(--color-gray-700);
      cursor: pointer;

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      &:hover:not(:disabled) {
        background-color: var(--color-gray-100);
      }

      &.active {
        background-color: var(--color-primary);
        color: var(--color-white);
        border-color: var(--color-primary);
      }
    }
  }
}

/* 모바일 반응형 */
@media (max-width: 768px) {
  .location-selector {
    flex-direction: column;

    select {
      width: 100%;
    }
  }

  .filter-controls {
    flex-direction: column;
    align-items: flex-start;

    .filter-group {
      width: 100%;

      select {
        flex: 1;
      }
    }
  }

  .property-list {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }

  .pagination {
    .page-numbers {
      .page-number {
        display: none;

        &.active {
          display: flex;
        }
      }
    }
  }
}
</style>
