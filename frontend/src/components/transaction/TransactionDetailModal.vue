<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>거래 상세 정보</h3>
        <button class="close-button" @click="$emit('close')">×</button>
      </div>
      
      <div class="modal-body">
        <div class="property-details">
          <h4>{{ transaction.propertyName }}</h4>
          <p class="address">{{ transaction.address }}</p>
          
          <div class="price-info">
            <div class="price-label">거래가</div>
            <div class="price-value">{{ formatPrice(transaction.price) }}</div>
          </div>
        </div>
        
        <div class="transaction-details">
          <div class="detail-item">
            <div class="detail-label">거래일</div>
            <div class="detail-value">{{ formatDetailDate(transaction.date) }}</div>
          </div>
          
          <div class="detail-item">
            <div class="detail-label">전용면적</div>
            <div class="detail-value">{{ transaction.area }}㎡ ({{ calculatePyeong(transaction.area) }}평)</div>
          </div>
          
          <div class="detail-item">
            <div class="detail-label">층수</div>
            <div class="detail-value">{{ transaction.floor }}층</div>
          </div>
          
          <div class="detail-item">
            <div class="detail-label">건축년도</div>
            <div class="detail-value">{{ transaction.builtYear }}년</div>
          </div>
          
          <div class="detail-item">
            <div class="detail-label">3.3㎡당 가격</div>
            <div class="detail-value">{{ calculatePricePerPyeong(transaction.price, transaction.area) }}만원</div>
          </div>
        </div>
        
        <div class="price-trend">
          <h5>최근 거래 동향</h5>
          <img src="https://via.placeholder.com/600x300" alt="가격 추이 그래프" class="trend-graph" />
        </div>
      </div>
      
      <div class="modal-footer">
        <button class="btn btn-outline" @click="$emit('close')">닫기</button>
        <button class="btn">매물 더보기</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {defineEmits, defineProps} from 'vue'
import {Transaction} from '../../types'

defineProps<{
  transaction: Transaction
}>()

defineEmits(['close'])

function formatDetailDate(dateString: string): string {
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  
  return `${year}년 ${month}월 ${day}일`
}

function formatPrice(price: number): string {
  if (price >= 10000) {
    const 억 = Math.floor(price / 10000)
    const 천만 = price % 10000
    return 천만 > 0 ? `${억}억 ${천만}만원` : `${억}억원`
  }
  return `${price}만원`
}

function calculatePyeong(squareMeters: number): string {
  return (squareMeters / 3.3058).toFixed(2)
}

function calculatePricePerPyeong(price: number, area: number): string {
  const pyeong = area / 3.3058
  return Math.round(price / pyeong).toLocaleString()
}
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

.modal-content {
  width: 100%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  background-color: var(--color-white);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  animation: slideUp 0.3s ease;
}

.modal-header {
  padding: var(--space-3);
  border-bottom: 1px solid var(--color-gray-200);
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  h3 {
    margin: 0;
    font-size: 1.25rem;
  }
  
  .close-button {
    background: none;
    border: none;
    font-size: 1.5rem;
    cursor: pointer;
    color: var(--color-gray-600);
    
    &:hover {
      color: var(--color-gray-900);
    }
  }
}

.modal-body {
  padding: var(--space-3);
}

.property-details {
  margin-bottom: var(--space-3);
  
  h4 {
    margin-bottom: var(--space-1);
    font-size: 1.25rem;
  }
  
  .address {
    color: var(--color-gray-600);
    margin-bottom: var(--space-2);
  }
  
  .price-info {
    background-color: var(--color-gray-100);
    padding: var(--space-2);
    border-radius: var(--radius-md);
    
    .price-label {
      font-size: 0.875rem;
      color: var(--color-gray-600);
      margin-bottom: var(--space-1);
    }
    
    .price-value {
      font-size: 1.5rem;
      font-weight: 700;
      color: var(--color-primary);
    }
  }
}

.transaction-details {
  margin-bottom: var(--space-3);
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-2);
  
  .detail-item {
    .detail-label {
      font-size: 0.875rem;
      color: var(--color-gray-600);
      margin-bottom: 4px;
    }
    
    .detail-value {
      font-size: 1rem;
      font-weight: 500;
    }
  }
}

.price-trend {
  h5 {
    margin-bottom: var(--space-2);
    font-size: 1rem;
  }
  
  .trend-graph {
    width: 100%;
    border-radius: var(--radius-md);
  }
}

.modal-footer {
  padding: var(--space-3);
  border-top: 1px solid var(--color-gray-200);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

@media (max-width: 768px) {
  .modal-content {
    max-width: 100%;
    max-height: 100%;
    border-radius: 0;
    height: 100%;
    display: flex;
    flex-direction: column;
  }
  
  .modal-body {
    flex: 1;
    overflow-y: auto;
  }
  
  .transaction-details {
    grid-template-columns: 1fr;
  }
}
</style>