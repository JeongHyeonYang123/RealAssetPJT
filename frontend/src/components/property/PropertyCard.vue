<!-- components/property/PropertyCard.vue -->
<template>
  <div class="property-card" @click="$emit('click')">
    <div class="property-header">
      <h3>{{ property.aptNm }}</h3>
      <span v-if="property.umdNm" class="location">{{ property.umdNm }}</span>
      <button
        class="favorite-btn"
        @click.stop="toggleFavoriteApt(property.aptSeq)"
        aria-label="관심 매물 등록/해제"
      >
        <font-awesome-icon :icon="[isFavoriteApt ? 'fas' : 'far', 'heart']" size="lg" style="color: #ef4444;" />
      </button>
    </div>

    <div class="property-details">
      <div class="detail-row">
        <span class="label">거래가격:</span>
        <span class="price">{{ formatPrice(property.dealAmount) }}</span>
      </div>

      <div class="detail-row">
        <span class="label">전용면적:</span>
        <span class="area">{{ formatAreaWithPyung(property.excluUseAr) }}</span>
      </div>

      <!-- ✅ 평당 가격 추가 -->
      <div class="detail-row">
        <span class="label">평당 가격:</span>
        <span class="price-per-pyung">{{ formatPricePerPyung(property.dealAmount, property.excluUseAr) }}</span>
      </div>

      <div class="detail-row">
        <span class="label">층수:</span>
        <span>{{ property.floor }}층</span>
      </div>

      <div class="detail-row">
        <span class="label">거래일:</span>
        <span>{{ formatDate() }}</span>
      </div>

      <div class="detail-row" v-if="property.buildYear">
        <span class="label">건축년도:</span>
        <span>{{ property.buildYear }}년</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {HouseDeal} from '../../services/propertyApi'
import { computed } from 'vue';

interface Props {
  property: HouseDeal,
  favoriteAptSeqs: string[],
  toggleFavoriteApt: (aptSeq: string) => void
}

const props = defineProps<Props>()

const isFavoriteApt = computed(() => props.favoriteAptSeqs.includes(props.property.aptSeq));

// ✅ 평수 변환 함수들 추가
function squareMeterToPyung(area: number): number {
  return area * 0.3025;
}

function formatPyung(area: number): string {
  const pyung = squareMeterToPyung(area);
  return pyung.toFixed(1);
}

function formatAreaWithPyung(areaStr: string): string {
  const area = parseFloat(areaStr);
  if (isNaN(area)) return `${areaStr}㎡`;

  const pyung = formatPyung(area);
  return `${pyung}평 (${area}㎡)`;
}

// ✅ 평당 가격 계산 함수
function formatPricePerPyung(dealAmountStr: string, areaStr: string): string {
  const dealAmount = parseFloat(dealAmountStr.replace(/,/g, '')); // 만원 단위
  const area = parseFloat(areaStr);

  if (isNaN(dealAmount) || isNaN(area) || area === 0) {
    return '-';
  }

  const pyung = squareMeterToPyung(area);
  const pricePerPyung = Math.round(dealAmount / pyung); // 만원/평

  return `${pricePerPyung.toLocaleString()}만원/평`;
}

function formatPrice(amount: string) {
  const numAmount = parseFloat(amount.replace(/,/g, ''));
  if (isNaN(numAmount)) return `${amount}만원`;

  // 억 단위 변환
  if (numAmount >= 10000) {
    const eok = Math.floor(numAmount / 10000);
    const remainder = numAmount % 10000;

    if (remainder === 0) {
      return `${eok}억원`;
    } else {
      return `${eok}억 ${remainder.toLocaleString()}만원`;
    }
  } else {
    return `${numAmount.toLocaleString()}만원`;
  }
}

function formatDate() {
  return `${props.property.dealYear}.${props.property.dealMonth}.${props.property.dealDay}`
}
</script>

<style lang="scss" scoped>
.property-card {
  background: white;
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-gray-200);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-1px);
  }
}

.property-header {
  margin-bottom: var(--space-3);

  h3 {
    font-size: 1.125rem;
    font-weight: 600;
    color: var(--color-gray-900);
    margin-bottom: var(--space-1);
  }

  .location {
    color: var(--color-gray-600);
    font-size: 0.875rem;
  }
}

.property-details {
  .detail-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--space-1) 0;

    &:not(:last-child) {
      border-bottom: 1px solid var(--color-gray-100);
    }

    .label {
      color: var(--color-gray-600);
      font-size: 0.875rem;
    }

    .price {
      font-weight: 600;
      color: var(--color-primary);
      font-size: 1rem;
    }

    // ✅ 면적 스타일 추가
    .area {
      font-weight: 500;
      color: var(--color-gray-800);
      font-size: 0.9rem;
    }

    // ✅ 평당 가격 스타일 추가
    .price-per-pyung {
      font-weight: 500;
      color: var(--color-orange-600);
      font-size: 0.9rem;
    }
  }
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
</style>
