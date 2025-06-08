<template>
  <div class="apt-commercial-container">
    <div class="sidebar" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
      <div class="sidebar-header">
        <button class="btn-icon" @click="toggleSidebar">
          <svg :style="{ transform: isSidebarCollapsed ? '' : 'rotate(180deg)' }" width="48" height="48" viewBox="0 0 48 48" fill="none" stroke="#2d60e8" stroke-width="5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="16 40 32 24 16 8"/>
          </svg>
        </button>
      </div>
      <div v-if="!isSidebarCollapsed && property" class="property-info">
        <div class="property-basic-info">
          <div class="property-header" style="display: flex; align-items: center; gap: 8px; min-width: 0;">
            <h3 style="margin: 0; flex: 1 1 auto; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">{{ property.name }}</h3>
          </div>
          <p class="address">{{ property.address }}</p>
          <div class="price-info">
            <div class="price-label">최근 거래가</div>
            <div class="price-value">{{ formatPrice(property.recentPrice) }}</div>
          </div>
        </div>
        <div v-if="dealHistory.length > 0" class="deal-history">
          <h4>거래내역</h4>
          <div class="deal-list">
            <div v-for="deal in dealHistory" :key="deal.id" class="deal-item">
              <div class="deal-floor">{{ deal.floor }}층</div>
              <div class="deal-info">
                <div class="deal-price">{{ formatPrice(deal.dealAmount) }}</div>
                <div class="deal-date">{{ formatDealDate(deal) }}</div>
                <div class="deal-area">{{ deal.area }}㎡</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="map-container">
      <div id="kakao-map" ref="mapContainer"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const mapContainer = ref<HTMLElement | null>(null);
const isSidebarCollapsed = ref(false);
const property = ref<any>(null);
const dealHistory = ref<any[]>([]);
let map: any = null;

function toggleSidebar() {
  isSidebarCollapsed.value = !isSidebarCollapsed.value;
}

function formatPrice(price: number | string): string {
  if (!price) return '정보없음';
  const num = typeof price === 'string' ? parseInt(price.replace(/[^\d]/g, '')) : price;
  if (num >= 10000) {
    const eok = Math.floor(num / 10000);
    const cheonman = num % 10000;
    return cheonman > 0 ? `${eok}억 ${cheonman}만원` : `${eok}억원`;
  }
  return `${num}만원`;
}

function formatDealDate(deal: any): string {
  if (deal.dealYear && deal.dealMonth && deal.dealDay) {
    return `${deal.dealYear}.${deal.dealMonth.toString().padStart(2, '0')}.${deal.dealDay.toString().padStart(2, '0')}`;
  }
  return '날짜 정보 없음';
}

// speechbubble.png 경로
const speechBubbleIcon = '/speechbubble.png';

onMounted(async () => {
  const aptSeq = route.query.aptSeq as string;
  if (!aptSeq) return;

  // 1. 아파트 정보 및 거래내역 불러오기
  const aptRes = await fetch(`http://localhost:8080/api/v1/house/apartment/${aptSeq}`);
  const aptData = await aptRes.json();
  if (aptData.success && aptData.data) {
    const apartment = aptData.data;
    let recentPrice = 0;
    // 거래내역
    let deals: any[] = [];
    try {
      const dealRes = await fetch(`http://localhost:8080/api/v1/house/deals/${aptSeq}`);
      if (dealRes.ok) {
        deals = await dealRes.json();
        if (deals.length > 0) {
          deals.sort((a, b) => {
            const dateA = new Date(a.dealYear, a.dealMonth - 1, a.dealDay);
            const dateB = new Date(b.dealYear, b.dealMonth - 1, b.dealDay);
            return dateB.getTime() - dateA.getTime();
          });
          const latestDeal = deals[0];
          if (latestDeal && latestDeal.dealAmount) {
            recentPrice = parseInt(latestDeal.dealAmount.replace(/[^\d]/g, '')) || 0;
          }
        }
      }
    } catch {}
    property.value = {
      name: apartment.aptNm || '아파트명',
      address: apartment.roadNm ? `${apartment.roadNm} ${apartment.roadNmBonbun}` : (apartment.umdNm ? `${apartment.umdNm} ${apartment.jibun}` : '주소 정보 없음'),
      recentPrice,
      lat: parseFloat(apartment.latitude) || 0,
      lng: parseFloat(apartment.longitude) || 0
    };
    dealHistory.value = deals;
    // 2. 지도 초기화 및 마커 표시
    await nextTick();
    initMap();
  }
});

function initMap() {
  if (!mapContainer.value) return;
  const { lat, lng } = property.value;
  // @ts-ignore
  map = new window.kakao.maps.Map(mapContainer.value, { center: new window.kakao.maps.LatLng(lat, lng), level: 4 });
  // 아파트 마커 (speechbubble+왕관)
  const content = `
    <div class="speech-bubble-marker" style="position:relative;width:80px;height:80px;">
      <div class="crown" style="position:absolute;top:-18px;left:50%;transform:translateX(-50%);font-size:1.5rem;z-index:2;pointer-events:none;">👑</div>
      <img src="${speechBubbleIcon}" style="width:80px;height:80px;display:block;">
      <div class="bubble-text" style="position:absolute;top:28px;left:0;width:100%;text-align:center;font-size:15px;font-weight:bold;color:#fff;pointer-events:none;text-shadow:0 1px 2px rgba(0,0,0,0.25);">
        <div style="font-size:14px;font-weight:700;margin-bottom:2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${property.value.name}</div>
        <div style="font-size:18px;font-weight:900;letter-spacing:0.5px;">${property.value.recentPrice ? formatPrice(property.value.recentPrice) : ''}</div>
      </div>
    </div>
  `;
  // @ts-ignore
  const overlay = new window.kakao.maps.CustomOverlay({ position: new window.kakao.maps.LatLng(lat, lng), content, yAnchor: 1 });
  overlay.setMap(map);
  // 3. Places API로 반경 1km 상권 마커 표시
  showNearbyCommercialMarkers(lat, lng);
}

function showNearbyCommercialMarkers(lat: number, lng: number) {
  // @ts-ignore
  const ps = new window.kakao.maps.services.Places();
  const categories = [
    { code: 'FD6', name: '음식점', emoji: '🍽️' },
    { code: 'CE7', name: '카페', emoji: '☕' },
    { code: 'CS2', name: '편의점', emoji: '🏪' },
    { code: 'MT1', name: '마트', emoji: '🛒' },
    { code: 'BK9', name: '은행', emoji: '🏦' }
  ];
  categories.forEach(category => {
    ps.categorySearch(category.code, function(data: any[], status: string) {
      if (status === window.kakao.maps.services.Status.OK) {
        data.forEach(place => {
          // 이모지 커스텀 오버레이 마커
          const content = `
            <div style="font-size: 2rem; background: white; border-radius: 50%; border: 1px solid #ccc; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;">
              ${category.emoji}
            </div>
          `;
          // @ts-ignore
          const overlay = new window.kakao.maps.CustomOverlay({
            position: new window.kakao.maps.LatLng(place.y, place.x),
            content: content,
            yAnchor: 1
          });
          overlay.setMap(map);
        });
      }
    }, {
      location: new window.kakao.maps.LatLng(lat, lng),
      radius: 1000
    });
  });
}
</script>

<style scoped>
.apt-commercial-container {
  display: flex;
  height: calc(100vh - 60px);
  position: relative;
}
.sidebar {
  position: relative;
  width: 400px;
  height: 100%;
  background-color: var(--color-white);
  box-shadow: var(--shadow-md);
  z-index: 10;
  transition: width var(--transition-normal);
  display: flex;
  flex-direction: column;
}
.sidebar-collapsed {
  width: 60px;
}
.sidebar-header {
  padding: var(--space-2);
  display: flex;
  justify-content: flex-end;
  align-items: center;
  border-bottom: 1px solid var(--color-gray-200);
}
.btn-icon {
  width: 48px;
  height: 48px;
  min-width: 48px;
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fff;
  color: #2d60e8;
  border: 1.5px solid #e0e0e0;
  box-shadow: 0 2px 8px rgba(45, 96, 232, 0.08);
  z-index: 20;
  position: relative;
  transition: background 0.2s, color 0.2s;
}
.property-info {
  padding: 24px;
}
.property-header h3 {
  font-size: 1.25rem;
  color: #1a202c;
  font-weight: 600;
}
.address {
  color: var(--color-gray-600);
  margin-bottom: 8px;
  font-size: 14px;
}
.price-info {
  background-color: var(--color-gray-100);
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
}
.price-label {
  font-size: 0.875rem;
  color: var(--color-gray-600);
  margin-bottom: 4px;
}
.price-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #2d60e8;
}
.deal-history {
  margin-top: 20px;
}
.deal-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.deal-item {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #2d60e8;
}
.deal-floor {
  min-width: 40px;
  font-weight: 600;
  color: #2d60e8;
  font-size: 14px;
}
.deal-info {
  flex: 1;
  margin-left: 12px;
}
.deal-price {
  font-weight: 600;
  color: #1a202c;
  font-size: 14px;
}
.deal-date {
  font-size: 12px;
  color: #6c757d;
  margin-top: 2px;
}
.deal-area {
  font-size: 12px;
  color: #6c757d;
  margin-top: 2px;
}
.map-container {
  flex-grow: 1;
  height: 100%;
}
</style> 