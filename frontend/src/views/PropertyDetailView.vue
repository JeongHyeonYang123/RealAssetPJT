<template>
  <div class="property-detail-view">
    <div class="property-header">
      <div class="container">
        <nav class="breadcrumb">
          <router-link to="/">홈</router-link> &gt;
          <span>{{ property ? property.name : '로딩 중...' }}</span>
        </nav>
        
        <div v-if="property" class="property-title">
          <h1>{{ property.name }}</h1>
          <p class="address">{{ property.address }}</p>
        </div>
      </div>
    </div>
    
    <div class="container">
      <div class="loading-indicator" v-if="isLoading">
        <p>정보를 불러오는 중...</p>
      </div>
      
      <div v-else-if="property" class="property-content">
        <div class="property-summary-card">
          <div class="property-image">
            <img src="https://via.placeholder.com/500x350" alt="아파트 이미지" />
          </div>
          
          <div class="property-summary">
            <div class="summary-item">
              <div class="summary-label">최근 거래가</div>
              <div class="summary-value price">{{ formatPrice(property.recentPrice) }}</div>
            </div>
            
            <div class="summary-item">
              <div class="summary-label">건축년도</div>
              <div class="summary-value">{{ property.builtYear }}년</div>
            </div>
            
            <div class="summary-item">
              <div class="summary-label">총 세대수</div>
              <div class="summary-value">{{ property.totalUnits }}세대</div>
            </div>
          </div>
        </div>
        
        <div class="transaction-history-section">
          <h2>실거래 내역</h2>
          
          <div class="filter-controls">
            <div class="filter-group">
              <label>면적</label>
              <select v-model="areaFilter">
                <option value="all">전체</option>
                <option value="small">60㎡ 이하</option>
                <option value="medium">60㎡~85㎡</option>
                <option value="large">85㎡ 초과</option>
              </select>
            </div>
            
            <div class="filter-group">
              <label>정렬</label>
              <select v-model="sortOrder">
                <option value="date-desc">최신순</option>
                <option value="date-asc">과거순</option>
                <option value="price-desc">가격 높은순</option>
                <option value="price-asc">가격 낮은순</option>
              </select>
            </div>
          </div>
          
          <div class="transaction-table">
            <table>
              <thead>
                <tr>
                  <th>거래일</th>
                  <th>층수</th>
                  <th>면적</th>
                  <th>거래가</th>
                </tr>
              </thead>
              <tbody>
                <tr 
                  v-for="transaction in filteredTransactions" 
                  :key="transaction.id"
                  @click="showTransactionDetail(transaction)"
                >
                  <td>{{ formatDate(transaction.date) }}</td>
                  <td>{{ transaction.floor }}층</td>
                  <td>{{ transaction.area }}㎡</td>
                  <td class="price-cell">{{ formatPrice(transaction.price) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        
        <div class="price-trend-section">
          <h2>가격 추이</h2>
          <div class="trend-chart">
            <img src="https://via.placeholder.com/1000x400" alt="가격 추이 차트" />
          </div>
        </div>
        
        <div class="neighborhood-section">
          <h2>주변 인프라</h2>
          <div class="neighborhood-map">
            <map-component />
          </div>
        </div>
      </div>
      
      <div v-else class="error-state">
        <p>정보를 불러올 수 없습니다. 다시 시도해주세요.</p>
        <router-link to="/" class="btn">홈으로 돌아가기</router-link>
      </div>
    </div>
    
    <transaction-detail-modal 
      v-if="selectedTransaction" 
      :transaction="selectedTransaction"
      @close="selectedTransaction = null"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute} from 'vue-router'
import MapComponent from '../components/map/MapComponent.vue'
import TransactionDetailModal from '../components/transaction/TransactionDetailModal.vue'
import {usePropertyStore} from '../stores/property'
import {Property, Transaction} from '../types'

const route = useRoute()
const propertyStore = usePropertyStore()

const property = ref<Property | null>(null)
const isLoading = ref(true)
const areaFilter = ref('all')
const sortOrder = ref('date-desc')
const selectedTransaction = ref<Transaction | null>(null)

const propertyId = computed(() => Number(route.params.id))

onMounted(async () => {
  if (propertyId.value) {
    await fetchPropertyDetail(propertyId.value)
  }
})

async function fetchPropertyDetail(id: number) {
  isLoading.value = true
  try {
    await propertyStore.fetchPropertyDetail(id)
    property.value = propertyStore.currentProperty
  } catch (error) {
    console.error('Error fetching property:', error)
  } finally {
    isLoading.value = false
  }
}

const filteredTransactions = computed(() => {
  if (!property.value) return []
  
  let transactions = [...property.value.transactions]
  
  // Apply area filter
  if (areaFilter.value !== 'all') {
    transactions = transactions.filter(transaction => {
      const area = transaction.area
      
      if (areaFilter.value === 'small') return area <= 60
      if (areaFilter.value === 'medium') return area > 60 && area <= 85
      if (areaFilter.value === 'large') return area > 85
      
      return true
    })
  }
  
  // Apply sorting
  transactions.sort((a, b) => {
    if (sortOrder.value === 'date-desc') {
      return new Date(b.date).getTime() - new Date(a.date).getTime()
    } else if (sortOrder.value === 'date-asc') {
      return new Date(a.date).getTime() - new Date(b.date).getTime()
    } else if (sortOrder.value === 'price-desc') {
      return b.price - a.price
    } else if (sortOrder.value === 'price-asc') {
      return a.price - b.price
    }
    return 0
  })
  
  return transactions
})

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  
  return `${year}.${month}.${day}`
}

function formatPrice(price: number): string {
  if (price >= 10000) {
    const 억 = Math.floor(price / 10000)
    const 천만 = price % 10000
    return 천만 > 0 ? `${억}억 ${천만}만원` : `${억}억원`
  }
  return `${price}만원`
}

function showTransactionDetail(transaction: Transaction) {
  selectedTransaction.value = transaction
}
</script>

<style lang="scss" scoped>
.property-detail-view {
  min-height: calc(100vh - 60px);
  padding-bottom: var(--space-6);
}

.property-header {
  background-color: var(--color-primary);
  color: var(--color-white);
  padding: var(--space-4) 0;
  margin-bottom: var(--space-4);
}

.breadcrumb {
  margin-bottom: var(--space-2);
  font-size: 0.875rem;
  
  a {
    color: rgba(255, 255, 255, 0.8);
    
    &:hover {
      color: var(--color-white);
    }
  }
  
  span {
    color: var(--color-white);
  }
}

.property-title {
  h1 {
    margin-bottom: var(--space-1);
    font-size: 2rem;
  }
  
  .address {
    font-size: 1rem;
    opacity: 0.9;
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

.error-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 300px;
  
  p {
    color: var(--color-gray-600);
    font-size: 1.125rem;
    margin-bottom: var(--space-3);
  }
}

.property-content {
  > div {
    margin-bottom: var(--space-4);
  }
}

.property-summary-card {
  display: flex;
  background-color: var(--color-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.property-image {
  flex: 0 0 40%;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.property-summary {
  flex: 1;
  padding: var(--space-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.summary-item {
  .summary-label {
    font-size: 0.875rem;
    color: var(--color-gray-600);
    margin-bottom: 4px;
  }
  
  .summary-value {
    font-size: 1.25rem;
    font-weight: 500;
    
    &.price {
      font-size: 1.75rem;
      font-weight: 700;
      color: var(--color-primary);
    }
  }
}

h2 {
  font-size: 1.5rem;
  margin-bottom: var(--space-3);
}

.filter-controls {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
  
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

.transaction-table {
  background-color: var(--color-white);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: auto;
  
  table {
    width: 100%;
    border-collapse: collapse;
    
    th, td {
      padding: 12px 16px;
      text-align: left;
      border-bottom: 1px solid var(--color-gray-200);
    }
    
    th {
      background-color: var(--color-gray-100);
      font-weight: 500;
      color: var(--color-gray-800);
    }
    
    tbody tr {
      cursor: pointer;
      transition: background-color var(--transition-fast);
      
      &:hover {
        background-color: var(--color-gray-100);
      }
    }
    
    .price-cell {
      font-weight: 500;
      color: var(--color-primary);
    }
  }
}

.trend-chart, .neighborhood-map {
  background-color: var(--color-white);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  overflow: hidden;
  
  img {
    width: 100%;
    display: block;
  }
}

.neighborhood-map {
  height: 400px;
}

@media (max-width: 768px) {
  .property-summary-card {
    flex-direction: column;
  }
  
  .property-image {
    flex: 0 0 200px;
  }
  
  .filter-controls {
    flex-direction: column;
    gap: var(--space-2);
  }
}
</style>