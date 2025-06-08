<template>
  <div class="interest-properties-view">
    <div class="container">
      <h1>관심 매물 목록</h1>
      <div v-if="isLoading" class="loading-indicator">불러오는 중...</div>
      <div v-else-if="favoriteCards.length === 0" class="no-properties">
        <p>등록된 관심 매물이 없습니다.</p>
      </div>
      <div v-else class="property-list">
        <div
          v-for="property in favoriteCards"
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated } from 'vue';
import { useRouter } from 'vue-router';
import { memberAi } from '@/axios';

const router = useRouter();
const isLoading = ref(true);
const favoriteCards = ref<any[]>([]);
const favoriteAptSeqs = ref<string[]>([]);

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

async function fetchFavoriteCards() {
  isLoading.value = true;
  try {
    const response = await memberAi.get('/api/v1/favorites');
    const favorites = response.data || [];
    favoriteAptSeqs.value = favorites.map((fav: any) => fav.aptSeq);

    // 각 aptSeq별로 아파트 상세 + 최근 거래 1건 fetch
    const cardArr = await Promise.all(
      favorites.map(async (fav: any) => {
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
    favoriteCards.value = cardArr;
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
        await fetchFavoriteCards();
      }
    } else {
      await memberAi.post('/api/v1/favorites', {
        aptSeq,
        userMno: null // 백엔드에서 JWT로 추출
      });
      await fetchFavoriteCards();
    }
  } catch (e) {}
}

function goToDetail(aptSeq: string) {
  router.push({ path: '/', query: { aptSeq } });
}

function formatDealDate(deal: any): string {
  if (deal.dealYear && deal.dealMonth && deal.dealDay) {
    return `${deal.dealYear}.${deal.dealMonth.toString().padStart(2, '0')}.${deal.dealDay.toString().padStart(2, '0')}`;
  }
  return '';
}

onMounted(() => {
  fetchFavoriteCards();
});
onActivated(() => {
  fetchFavoriteCards();
});
</script>

<style scoped>
.interest-properties-view {
  min-height: 100vh;
  padding: var(--space-4) 0;
}
.container {
  max-width: 1200px;
  margin: 0 auto;
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
.no-properties {
  text-align: center;
  color: var(--color-gray-600);
  padding: 40px 0;
}
</style> 