<template>
  <div class="transaction-item" @click="$emit('click')">
    <div class="transaction-header">
      <h5 class="property-name">{{ transaction.propertyName }}</h5>
      <span class="transaction-date">{{ formatDate(transaction.date) }}</span>
    </div>
    
    <div class="transaction-details">
      <div class="transaction-price">
        {{ formatPrice(transaction.price) }}
      </div>
      <div class="transaction-info">
        <span class="info-item">{{ transaction.floor }}층</span>
        <span class="info-divider"></span>
        <span class="info-item">{{ transaction.area }}㎡</span>
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

defineEmits(['click'])

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  
  return `${year}.${month}`
}

function formatPrice(price: number): string {
  if (price >= 10000) {
    const 억 = Math.floor(price / 10000)
    const 천만 = price % 10000
    return 천만 > 0 ? `${억}억 ${천만}만원` : `${억}억원`
  }
  return `${price}만원`
}
</script>

<style lang="scss" scoped>
.transaction-item {
  padding: var(--space-2);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-2);
  background-color: var(--color-white);
  border: 1px solid var(--color-gray-200);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
  cursor: pointer;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }
}

.transaction-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-1);
}

.property-name {
  font-size: 1rem;
  margin: 0;
  font-weight: 500;
}

.transaction-date {
  font-size: 0.75rem;
  color: var(--color-gray-500);
}

.transaction-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.transaction-price {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-primary);
}

.transaction-info {
  display: flex;
  align-items: center;
  
  .info-item {
    font-size: 0.875rem;
    color: var(--color-gray-600);
  }
  
  .info-divider {
    width: 3px;
    height: 3px;
    background-color: var(--color-gray-400);
    border-radius: 50%;
    margin: 0 6px;
  }
}
</style>