<template>
    <div class="home-container">
        <!-- ✅ 리사이저블 사이드바 (확장 상태) -->
        <VueResizable
            v-if="!isSidebarCollapsed"
            :width="sidebarWidth"
            :min-width="350"
            :max-width="700"
            :active="['r']"
            :fit-parent="false"
            @resize:end="onSidebarResize"
            class="resizable-sidebar-wrapper"
        >
            <div class="sidebar">
                <div class="sidebar-header">
                    <button class="btn-icon" @click="toggleSidebar">
                        <svg
                            :style="{
                                transform: isSidebarCollapsed
                                    ? ''
                                    : 'rotate(180deg)',
                            }"
                            width="48"
                            height="48"
                            viewBox="0 0 48 48"
                            fill="none"
                            stroke="#2d60e8"
                            stroke-width="5"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                        >
                            <polyline points="16 40 32 24 16 8" />
                        </svg>
                    </button>
                </div>

                <div class="transaction-list">
                    <div v-if="isLoading" class="loading-indicator">
                        <div class="loading-spinner">🔄</div>
                        <p>정보를 불러오는 중...</p>
                    </div>

                    <!-- ✅ 선택된 매물 정보 -->
                    <div v-else-if="selectedProperty" class="property-info">
                        <!-- 기본 정보 -->
                        <div class="property-basic-info">
                            <div class="property-header">
                                <h3>{{ selectedProperty.name }}</h3>
                                <button
                                    class="favorite-btn"
                                    :class="{ 'is-favorite': isFavorite }"
                                    @click="toggleFavorite"
                                    aria-label="관심 매물 등록/해제"
                                >
                                    <font-awesome-icon
                                        :icon="[
                                            isFavorite ? 'fas' : 'far',
                                            'heart',
                                        ]"
                                        size="2x"
                                        style="color: #ef4444"
                                    />
                                </button>
                            </div>
                            <p class="address">
                                {{ selectedProperty.address }}
                            </p>

                            <div class="price-info">
                                <div class="price-label">
                                    {{
                                        selectedProperty.isDong
                                            ? "평균 거래가"
                                            : "최근 거래가"
                                    }}
                                </div>
                                <div class="price-value">
                                    {{
                                        formatPrice(
                                            selectedProperty.recentPrice
                                        )
                                    }}
                                </div>
                                <div
                                    v-if="
                                        selectedProperty.isDong &&
                                        selectedProperty.aptCount
                                    "
                                    class="apt-count"
                                >
                                    {{ selectedProperty.aptCount }}개 단지
                                </div>
                            </div>
                        </div>

                        <!-- ✅ 거래내역 차트 (Chart.js 사용) -->
                        <div
                            v-if="
                                !selectedProperty.isDong &&
                                selectedProperty.aptSeq
                            "
                            class="chart-section"
                        >
                            <h4 class="chart-title">📈 최근 거래 동향</h4>
                            <deal-chart :apt-seq="selectedProperty.aptSeq" />
                        </div>

                        <!-- ✅ 주변 상권 정보 (개선된 디자인) -->
                        <div
                            v-if="Object.keys(commercialAreas).length > 0"
                            class="commercial-areas-section"
                        >
                            <div class="section-header">
                                <div class="header-left">
                                    <h4>🏪 주변 상권</h4>
                                    <span class="total-count"
                                        >{{ getTotalCommercialCount() }}개</span
                                    >
                                </div>
                                <span class="radius-badge">반경 1km</span>
                            </div>

                            <div class="categories-grid">
                                <div
                                    v-for="(areas, category) in commercialAreas"
                                    :key="category"
                                    class="category-card"
                                >
                                    <div
                                        class="category-header"
                                        @click="toggleCategory(category)"
                                    >
                                        <div class="category-info">
                                            <span class="category-emoji">{{
                                                getCategoryEmoji(category)
                                            }}</span>
                                            <span class="category-name">{{
                                                category
                                            }}</span>
                                            <span class="category-count">{{
                                                areas.length
                                            }}</span>
                                        </div>
                                        <span
                                            class="expand-icon"
                                            :class="{
                                                expanded:
                                                    expandedCategories[
                                                        category
                                                    ],
                                            }"
                                        >
                                            <svg
                                                width="16"
                                                height="16"
                                                viewBox="0 0 24 24"
                                                fill="none"
                                                stroke="currentColor"
                                                stroke-width="2"
                                            >
                                                <polyline
                                                    points="6 9 12 15 18 9"
                                                ></polyline>
                                            </svg>
                                        </span>
                                    </div>

                                    <div
                                        v-if="expandedCategories[category]"
                                        class="commercial-list"
                                    >
                                        <div
                                            v-for="area in areas.slice(0, 5)"
                                            :key="area.id || area.place_name"
                                            class="commercial-item"
                                            @click="selectCommercialArea(area)"
                                            :class="{
                                                selected:
                                                    selectedCommercialArea?.id ===
                                                    area.id,
                                            }"
                                        >
                                            <div class="item-header">
                                                <h5 class="place-name">
                                                    {{
                                                        area.place_name ||
                                                        area.storeName ||
                                                        area.name
                                                    }}
                                                </h5>
                                                <span class="distance-badge"
                                                    >{{ area.distance }}m</span
                                                >
                                            </div>
                                            <div class="item-details">
                                                <p
                                                    v-if="
                                                        area.road_address_name ||
                                                        area.address_name
                                                    "
                                                    class="address"
                                                >
                                                    📍
                                                    {{
                                                        area.road_address_name ||
                                                        area.address_name
                                                    }}
                                                </p>
                                                <p
                                                    v-if="area.phone"
                                                    class="phone"
                                                >
                                                    📞 {{ area.phone }}
                                                </p>
                                            </div>
                                        </div>

                                        <div
                                            v-if="areas.length > 5"
                                            class="show-more-btn"
                                            @click="showMoreItems(category)"
                                        >
                                            <span
                                                >+{{ areas.length - 5 }}개
                                                더보기</span
                                            >
                                            <svg
                                                width="16"
                                                height="16"
                                                viewBox="0 0 24 24"
                                                fill="none"
                                                stroke="currentColor"
                                                stroke-width="2"
                                            >
                                                <polyline
                                                    points="6 9 12 15 18 9"
                                                ></polyline>
                                            </svg>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- ✅ 상권 정보 로딩 상태 -->
                        <div
                            v-else-if="isCommercialLoading"
                            class="commercial-loading"
                        >
                            <div class="loading-animation">
                                <div class="loading-dots">
                                    <div class="dot"></div>
                                    <div class="dot"></div>
                                    <div class="dot"></div>
                                </div>
                            </div>
                            <p>주변 상권 정보를 검색 중...</p>
                        </div>
                    </div>

                    <!-- 거래 내역 목록 -->
                    <div
                        v-else-if="transactions.length > 0"
                        class="transaction-list-content"
                    >
                        <h3>최근 거래 내역</h3>
                        <transaction-item
                            v-for="transaction in transactions"
                            :key="transaction.id"
                            :transaction="transaction"
                            @click="selectProperty(transaction.propertyId)"
                        />
                    </div>

                    <!-- 빈 상태 UI -->
                    <div v-else class="empty-state-welcome">
                        <div class="empty-character">
                            <div class="house-icon">🏠</div>
                            <div class="speech-bubble">
                                <span>{{
                                    funMessages[currentMessageIndex]
                                }}</span>
                            </div>
                        </div>

                        <div class="empty-content">
                            <h3>어서오세요!</h3>
                            <p>
                                지도에서 원하는 지역으로 드래그하거나<br />검색창에서
                                아파트를 찾아보세요
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </VueResizable>

        <!-- ✅ 접힌 상태 사이드바 -->
        <div v-else class="sidebar sidebar-collapsed">
            <div class="sidebar-header">
                <button class="btn-icon" @click="toggleSidebar">
                    <svg
                        width="48"
                        height="48"
                        viewBox="0 0 48 48"
                        fill="none"
                        stroke="#2d60e8"
                        stroke-width="5"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                    >
                        <polyline points="16 40 32 24 16 8" />
                    </svg>
                </button>
            </div>

            <div v-if="selectedProperty" class="mini-info">
                <p>{{ selectedProperty.name }}</p>
                <p>{{ formatPrice(selectedProperty.recentPrice) }}</p>
            </div>
        </div>

        <div class="map-container">
            <map-component
                ref="mapRef"
                @map-ready="onMapReady"
                @marker-click="selectProperty"
                @apartment-marker-click="onApartmentMarkerClick"
                @dong-marker-click="onDongMarkerClick"
                @gugun-marker-click="onGugunMarkerClick"
                @sido-marker-click="onSidoMarkerClick"
                @commercial-area-marker-click="onCommercialAreaMarkerClick"
                @commercial-areas-loaded="onCommercialAreasLoaded"
                @commercial-search-start="onCommercialSearchStart"
            />
        </div>

        <transaction-detail-modal
            v-if="selectedTransaction"
            :transaction="selectedTransaction"
            @close="selectedTransaction = null"
        />
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import VueResizable from "vue-resizable"; // ✅ 추가
import MapComponent from "../components/map/MapComponent.vue";
import TransactionItem from "../components/transaction/TransactionItem.vue";
import TransactionDetailModal from "../components/transaction/TransactionDetailModal.vue";
import DealChart from "../components/map/DealChart.vue";
import { usePropertyStore } from "../stores/property";
import { Property, Transaction } from "../types";
import type { Apartment } from "../types/Apartment";
import type { DongMarker, GugunMarker, SidoMarker } from "../util/ShowMarker";
import type { Deal } from "../types/Deal";
import { useMemberStore } from "@/store/member";
import { memberAi } from "@/axios";
import { useRoute } from "vue-router";
import { FontAwesomeIcon } from "@fortawesome/vue-fontawesome";

const propertyStore = usePropertyStore();
const mapRef = ref<InstanceType<typeof MapComponent> | null>(null);
const searchQuery = ref("");
const isSidebarCollapsed = ref(true);
const isLoading = ref(false);
const selectedProperty = ref<Property | null>(null);
const selectedTransaction = ref<Transaction | null>(null);
const dealHistory = ref<Deal[]>([]);
const memberStore = useMemberStore();
const isFavorite = ref(false);
const route = useRoute();

// ✅ 사이드바 너비 상태 (기본 500px로 증가)
const sidebarWidth = ref(500);

// 상권 관련 상태
const commercialAreas = ref<Record<string, any[]>>({});
const selectedCommercialArea = ref<any | null>(null);
const expandedCategories = ref<Record<string, boolean>>({});
const isCommercialLoading = ref(false);

// 빈 상태 UI용 데이터
const funMessages = ref([
    "텅~",
    "여기가 텅 비었네요!",
    "뭔가 찾고 계신가요?",
    "지도를 둘러보세요!",
]);
const currentMessageIndex = ref(0);

const transactions = computed(() => propertyStore.recentTransactions);

// ✅ 사이드바 리사이징 핸들러
const onSidebarResize = (
    eventName: string,
    left: number,
    top: number,
    width: number,
    height: number
) => {
    sidebarWidth.value = width;
    localStorage.setItem("sidebarWidth", width.toString());
};

onMounted(async () => {
    // ✅ 저장된 사이드바 너비 복원
    const savedWidth = localStorage.getItem("sidebarWidth");
    if (savedWidth) {
        sidebarWidth.value = parseInt(savedWidth);
    }

    const urlParams = new URLSearchParams(window.location.search);
    const aptSeq = urlParams.get("aptSeq");

    if (aptSeq) {
        if (mapRef.value) await mapRef.value.setDetailMode(true);
        await fetchApartmentInfo(aptSeq);
    } else {
        if (mapRef.value) await mapRef.value.setDetailMode(false);
    }

    setInterval(() => {
        currentMessageIndex.value =
            (currentMessageIndex.value + 1) % funMessages.value.length;
    }, 3000);
});

watch(
    () => route.query.aptSeq,
    async (newAptSeq, oldAptSeq) => {
        if (newAptSeq) {
            if (mapRef.value) await mapRef.value.setDetailMode(true);
            await fetchApartmentInfo(newAptSeq as string);
        } else if (oldAptSeq) {
            // aptSeq가 있었던 URL에서 홈으로 돌아올 때 페이지 새로고침
            window.location.href = "/";
        }
    }
);

async function fetchApartmentInfo(aptSeq: string) {
    try {
        console.log("📍 아파트 정보 조회 시작:", aptSeq);
        const response = await fetch(
            `http://localhost:8080/api/v1/house/apartment/${aptSeq}`
        );
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const result = await response.json();

        let recentPrice = 0;
        let transactions: Deal[] = [];
        try {
            const dealRes = await fetch(
                `http://localhost:8080/api/v1/house/deals/${aptSeq}`
            );
            if (dealRes.ok) {
                transactions = await dealRes.json();
                if (transactions.length > 0) {
                    transactions.sort((a, b) => {
                        const dateA = new Date(
                            a.dealYear,
                            a.dealMonth - 1,
                            a.dealDay
                        );
                        const dateB = new Date(
                            b.dealYear,
                            b.dealMonth - 1,
                            b.dealDay
                        );
                        return dateB.getTime() - dateA.getTime();
                    });
                    const latestDeal = transactions[0];
                    if (latestDeal && latestDeal.dealAmount) {
                        recentPrice =
                            parseInt(
                                latestDeal.dealAmount.replace(/[^\d]/g, "")
                            ) || 0;
                    }
                }
            }
        } catch (e) {}

        if (result.success && result.data) {
            const apartmentData = result.data;
            const property: Property = {
                id: parseInt(aptSeq) || 0,
                name: apartmentData.aptNm || "아파트명",
                address: composeAddressFromData(apartmentData),
                recentPrice,
                aptSeq: aptSeq,
                isDong: false,
                transactions,
                lat: parseFloat(apartmentData.latitude) || 0,
                lng: parseFloat(apartmentData.longitude) || 0,
                builtYear: apartmentData.buildYear || 0,
                totalUnits: 0,
            };

            selectedProperty.value = property;
            isSidebarCollapsed.value = false;
            commercialAreas.value = {};
            selectedCommercialArea.value = null;
            expandedCategories.value = {};

            if (mapRef.value && property.lat && property.lng) {
                await mapRef.value.clearAllMarkers();
                await mapRef.value.moveToLatLng(property.lat, property.lng);
                await mapRef.value.addSingleApartmentMarker(property);
            }

            await checkFavoriteStatus();
        }
    } catch (error) {
        console.error("❌ 아파트 정보 조회 실패:", error);
    }
}

const onCommercialSearchStart = () => {
    isCommercialLoading.value = true;
    commercialAreas.value = {};
};

const onCommercialAreasLoaded = (areas: any[]) => {
    console.log("🏪 HomeView: 상권 데이터 수신:", areas.length, "개");
    isCommercialLoading.value = false;

    const groupedAreas = areas.reduce((acc, area) => {
        const category = area.category || "기타";
        if (!acc[category]) acc[category] = [];
        acc[category].push(area);
        return acc;
    }, {});

    commercialAreas.value = groupedAreas;

    Object.keys(groupedAreas).forEach((category) => {
        expandedCategories.value[category] = true;
    });
};

const onCommercialAreaMarkerClick = (commercialArea: any) => {
    console.log("HomeView: 상권 마커 클릭됨", commercialArea);
    selectedCommercialArea.value = commercialArea;

    const category = commercialArea.category;
    if (category && !expandedCategories.value[category]) {
        expandedCategories.value[category] = true;
    }
};

const toggleCategory = (category: string) => {
    expandedCategories.value[category] = !expandedCategories.value[category];
};

const selectCommercialArea = (area: any) => {
    selectedCommercialArea.value = area;
    if (mapRef.value) {
        const lat = area.latitude || area.y;
        const lng = area.longitude || area.x;
        if (lat && lng) {
            mapRef.value.moveToLatLng(lat, lng);
        }
    }
};

const showMoreItems = (category: string) => {
    console.log(`${category} 카테고리의 전체 목록 보기`);
};

const getCategoryEmoji = (category: string) => {
    const emojis = {
        음식점: "🍽️",
        카페: "☕",
        편의점: "🏪",
        대형마트: "🛒",
        은행: "🏦",
        병원: "🏥",
        약국: "💊",
        지하철역: "🚇",
        주차장: "🅿️",
    };
    return emojis[category] || "🏪";
};

const getTotalCommercialCount = () => {
    return Object.values(commercialAreas.value).reduce(
        (total, areas) => total + areas.length,
        0
    );
};

const searchSuggestion = async (area: string) => {
    searchQuery.value = area;
};

function composeAddressFromData(apartmentData: any): string {
    if (apartmentData.roadNm && apartmentData.roadNmBonbun) {
        let address = `${apartmentData.roadNm} ${apartmentData.roadNmBonbun}`;
        if (apartmentData.roadNmBubun && apartmentData.roadNmBubun !== "0") {
            address += `-${apartmentData.roadNmBubun}`;
        }
        return address;
    } else if (apartmentData.umdNm && apartmentData.jibun) {
        return `${apartmentData.umdNm} ${apartmentData.jibun}`;
    } else {
        return "주소 정보 없음";
    }
}

function onMapReady() {
    console.log("🗺️ 지도 준비 완료");
    const urlParams = new URLSearchParams(window.location.search);
    const aptSeq = urlParams.get("aptSeq");
    if (aptSeq && !selectedProperty.value) {
        fetchApartmentInfo(aptSeq);
    }
}

const onApartmentMarkerClick = async (apartment: Apartment) => {
    const property: Property = {
        id: parseInt(apartment.aptSeq) || 0,
        name: apartment.name || apartment.aptNm || "아파트명",
        address: composeAddress(apartment),
        recentPrice: apartment.latestDealAmount
            ? parseInt(apartment.latestDealAmount.replace(/[^\d]/g, ""))
            : 0,
        aptSeq: apartment.aptSeq,
        isDong: false,
        transactions: [],
        lat: 0,
        lng: 0,
        builtYear: 0,
        totalUnits: 0,
    };

    selectedProperty.value = property;
    isSidebarCollapsed.value = false;
};

const composeAddress = (apartment: any): string => {
    let roadAddress = "";
    if (apartment.roadNm && apartment.roadNmSggCd && apartment.roadNmBonbun) {
        roadAddress = `${apartment.roadNm} ${apartment.roadNmBonbun}`;
        if (apartment.roadNmBubun) roadAddress += `-${apartment.roadNmBubun}`;
    }
    return (
        roadAddress || apartment.jibun || apartment.address || "주소 정보 없음"
    );
};

const onDongMarkerClick = (dong: DongMarker) => {
    const property: Property = {
        id: parseInt(dong.dong_code) || 0,
        name: dong.dong_name,
        address: `${dong.sido_name} ${dong.gugun_name} ${dong.dong_name}`,
        recentPrice: dong.avg_price || 0,
        aptSeq: dong.dong_code,
        isDong: true,
        aptCount: dong.apt_count,
        transactions: [],
        lat: 0,
        lng: 0,
        builtYear: 0,
        totalUnits: 0,
    };
    selectedProperty.value = property;
    isSidebarCollapsed.value = false;
};

const onGugunMarkerClick = (gugun: GugunMarker) => {
    const property: Property = {
        id: parseInt(gugun.dong_code) || 0,
        name: gugun.gugun_name,
        address: `${gugun.sido_name} ${gugun.gugun_name}`,
        recentPrice: gugun.avg_price || 0,
        aptSeq: gugun.dong_code,
        isDong: true,
        aptCount: gugun.apt_count,
        transactions: [],
        lat: 0,
        lng: 0,
        builtYear: 0,
        totalUnits: 0,
    };
    selectedProperty.value = property;
    isSidebarCollapsed.value = false;
};

const onSidoMarkerClick = (sido: SidoMarker) => {
    const property: Property = {
        id: parseInt(sido.dong_code) || 0,
        name: sido.sido_name,
        address: sido.sido_name,
        recentPrice: sido.avg_price || 0,
        aptSeq: sido.dong_code,
        isDong: true,
        aptCount: sido.apt_count,
        transactions: [],
        lat: 0,
        lng: 0,
        builtYear: 0,
        totalUnits: 0,
    };
    selectedProperty.value = property;
    isSidebarCollapsed.value = false;
};

function toggleSidebar() {
    isSidebarCollapsed.value = !isSidebarCollapsed.value;
}

async function toggleFavorite() {
    if (!memberStore.isLoggedIn) {
        alert("로그인이 필요합니다.");
        return;
    }
    if (!selectedProperty.value?.aptSeq) return;
    try {
        if (isFavorite.value) {
            // 관심매물 해제: id로 삭제
            const response = await memberAi.get("/api/v1/favorites");
            const favorite = response.data.find(
                (fav: any) => fav.aptSeq === selectedProperty.value.aptSeq
            );
            if (favorite) {
                await memberAi.delete(`/api/v1/favorites/${favorite.id}`);
                isFavorite.value = false;
            } else {
                alert("관심매물 정보를 찾을 수 없습니다.");
            }
        } else {
            // 관심매물 등록
            await memberAi.post("/api/v1/favorites", {
                aptSeq: selectedProperty.value.aptSeq,
            });
            isFavorite.value = true;
        }
    } catch (e) {
        alert("관심매물 처리 중 오류가 발생했습니다.");
    }
}

async function checkFavoriteStatus() {
    if (!memberStore.isLoggedIn || !selectedProperty.value?.aptSeq) {
        isFavorite.value = false;
        return;
    }
    try {
        const response = await memberAi.get("/api/v1/favorites");
        isFavorite.value = response.data.some(
            (fav: any) => fav.aptSeq === selectedProperty.value.aptSeq
        );
    } catch (e) {
        isFavorite.value = false;
    }
}

watch(selectedProperty, checkFavoriteStatus);

async function selectProperty(propertyId: number) {
    // 매물 선택 로직...
}

function formatPrice(price: number): string {
    if (!price) return "정보없음";
    if (price >= 10000) {
        const eok = Math.floor(price / 10000);
        const cheonman = price % 10000;
        return cheonman > 0 ? `${eok}억 ${cheonman}만원` : `${eok}억원`;
    }
    return `${price}만원`;
}
</script>

<style lang="scss" scoped>
.home-container {
    display: flex;
    height: calc(100vh - 60px);
    position: relative;
}

/* ✅ 리사이저블 사이드바 래퍼 스타일 */
.resizable-sidebar-wrapper {
    position: relative;
    z-index: 10;

    /* vue-resizable 핸들 스타일 커스터마이징 */
    :deep(.vue-resizable-handle) {
        background: linear-gradient(135deg, #667eea, #764ba2);
        width: 4px !important;
        cursor: col-resize;
        transition: all 0.2s ease;

        &:hover {
            background: linear-gradient(135deg, #5a6fd8, #6a4190);
            width: 6px !important;
        }

        &:active {
            background: #2d60e8;
            width: 8px !important;
        }
    }
}

.sidebar {
    position: relative;
    width: 100%; /* ✅ VueResizable이 너비 제어하므로 100% */
    height: 100%;
    background: #fff;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
    display: flex;
    flex-direction: column;

    &.sidebar-collapsed {
        width: 60px;
        z-index: 10;
    }
}

.sidebar-header {
    padding: 16px;
    display: flex;
    justify-content: flex-end;
    border-bottom: 1px solid #e9ecef;
}

.btn-icon {
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: #fff;
    color: #2d60e8;
    border: 2px solid #e9ecef;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
        background: #f8f9fa;
        border-color: #2d60e8;
        transform: scale(1.05);
    }
}

.transaction-list {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
}

.property-info {
    .property-basic-info {
        margin-bottom: 24px;
    }

    .property-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;

        h3 {
            margin: 0;
            font-size: 1.25rem;
            color: #1a202c;
            font-weight: 700;
        }
    }

    .address {
        color: #6b7280;
        margin-bottom: 16px;
        font-size: 14px;
    }

    .price-info {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        padding: 20px;
        border-radius: 16px;
        margin-bottom: 16px;

        .price-label {
            font-size: 0.875rem;
            opacity: 0.9;
            margin-bottom: 8px;
        }

        .price-value {
            font-size: 1.75rem;
            font-weight: 800;
        }

        .apt-count {
            font-size: 0.875rem;
            opacity: 0.8;
            margin-top: 4px;
        }
    }
}

/* ✅ 차트 섹션 스타일 */
.chart-section {
    margin-bottom: 24px;
    padding: 20px;
    background: #f8fafc;
    border-radius: 16px;
    border: 1px solid #e2e8f0;

    .chart-title {
        margin: 0 0 16px 0;
        font-size: 1.1rem;
        color: #1a202c;
        font-weight: 600;
    }
}

/* ✅ 상권 정보 스타일 */
.commercial-areas-section {
    .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;
        padding: 0 4px;

        .header-left {
            display: flex;
            align-items: center;
            gap: 12px;

            h4 {
                margin: 0;
                font-size: 1.1rem;
                color: #1a202c;
                font-weight: 700;
            }

            .total-count {
                background: linear-gradient(135deg, #667eea, #764ba2);
                color: white;
                padding: 4px 12px;
                border-radius: 20px;
                font-size: 0.8rem;
                font-weight: 600;
            }
        }

        .radius-badge {
            background: #e2e8f0;
            color: #64748b;
            padding: 4px 10px;
            border-radius: 12px;
            font-size: 0.75rem;
            font-weight: 500;
        }
    }
}

.categories-grid {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.category-card {
    background: white;
    border-radius: 12px;
    border: 1px solid #e2e8f0;
    overflow: hidden;
    transition: all 0.2s ease;

    &:hover {
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        transform: translateY(-2px);
    }
}

.category-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
        background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
    }

    .category-info {
        display: flex;
        align-items: center;
        gap: 12px;

        .category-emoji {
            font-size: 1.5rem;
        }

        .category-name {
            font-weight: 600;
            color: #374151;
            font-size: 1rem;
        }

        .category-count {
            background: #667eea;
            color: white;
            padding: 4px 8px;
            border-radius: 12px;
            font-size: 0.75rem;
            font-weight: 600;
            min-width: 24px;
            text-align: center;
        }
    }

    .expand-icon {
        color: #9ca3af;
        transition: transform 0.2s ease;

        &.expanded {
            transform: rotate(180deg);
        }
    }
}

.commercial-list {
    padding: 8px;
}

.commercial-item {
    padding: 16px;
    margin-bottom: 8px;
    background: #f9fafb;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.2s ease;
    border: 2px solid transparent;

    &:hover {
        background: #f3f4f6;
        transform: translateX(4px);
    }

    &.selected {
        background: #eff6ff;
        border-color: #3b82f6;
    }

    .item-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 8px;

        .place-name {
            margin: 0;
            font-size: 0.95rem;
            font-weight: 600;
            color: #1f2937;
            line-height: 1.3;
        }

        .distance-badge {
            background: linear-gradient(135deg, #10b981, #059669);
            color: white;
            padding: 4px 8px;
            border-radius: 8px;
            font-size: 0.75rem;
            font-weight: 600;
            white-space: nowrap;
        }
    }

    .item-details {
        .address,
        .phone {
            margin: 0;
            font-size: 0.8rem;
            color: #6b7280;
            line-height: 1.4;
        }

        .phone {
            margin-top: 4px;
        }
    }
}

.show-more-btn {
    margin-top: 8px;
    padding: 12px 16px;
    background: linear-gradient(135deg, #e5e7eb, #d1d5db);
    color: #4b5563;
    border-radius: 8px;
    cursor: pointer;
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
    font-size: 0.875rem;
    font-weight: 500;
    transition: all 0.2s ease;

    &:hover {
        background: linear-gradient(135deg, #d1d5db, #9ca3af);
        color: #374151;
    }
}

/* ✅ 로딩 애니메이션 */
.commercial-loading {
    text-align: center;
    padding: 40px 20px;
    color: #6b7280;

    .loading-animation {
        margin-bottom: 16px;
    }

    .loading-dots {
        display: inline-flex;
        gap: 4px;

        .dot {
            width: 8px;
            height: 8px;
            background: #667eea;
            border-radius: 50%;
            animation: bounce 1.4s ease-in-out infinite both;

            &:nth-child(1) {
                animation-delay: -0.32s;
            }
            &:nth-child(2) {
                animation-delay: -0.16s;
            }
        }
    }

    p {
        margin: 0;
        font-size: 0.9rem;
    }
}

@keyframes bounce {
    0%,
    80%,
    100% {
        transform: scale(0);
    }
    40% {
        transform: scale(1);
    }
}

.loading-indicator {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 200px;

    .loading-spinner {
        font-size: 2rem;
        margin-bottom: 16px;
        animation: spin 1s linear infinite;
    }

    p {
        color: #6b7280;
        margin: 0;
    }
}

@keyframes spin {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
    }
}

/* 빈 상태 UI */
.empty-state-welcome {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
    height: 60vh;
    text-align: center;
}

.empty-character {
    position: relative;
    margin-bottom: 30px;
    animation: float 3s ease-in-out infinite;
}

.house-icon {
    font-size: 4rem;
    margin-bottom: 10px;
}

.speech-bubble {
    position: relative;
    background: #ff6b6b;
    color: white;
    padding: 8px 16px;
    border-radius: 20px;
    font-size: 1.2rem;
    font-weight: bold;

    &::after {
        content: "";
        position: absolute;
        bottom: -8px;
        left: 50%;
        transform: translateX(-50%);
        border-left: 8px solid transparent;
        border-right: 8px solid transparent;
        border-top: 8px solid #ff6b6b;
    }
}

.empty-content {
    h3 {
        margin: 0 0 16px 0;
        font-size: 1.5rem;
        color: #2d60e8;
        font-weight: 700;
    }

    p {
        margin: 0 0 24px 0;
        color: #6c757d;
        line-height: 1.6;
    }
}

.suggestion-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: center;
}

.chip {
    padding: 8px 16px;
    background: #f8f9ff;
    border: 1px solid #e3f2fd;
    border-radius: 20px;
    color: #2d60e8;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
        background: #2d60e8;
        color: white;
        transform: translateY(-2px);
    }
}

@keyframes float {
    0%,
    100% {
        transform: translateY(0px);
    }
    50% {
        transform: translateY(-10px);
    }
}

.map-container {
    flex: 1;
    height: 100%;
}

.favorite-btn {
    background: transparent;
    border: none;
    cursor: pointer;
}

.mini-info {
    display: none;
    text-align: center;
    padding: 16px 8px;
    writing-mode: vertical-rl;

    .sidebar-collapsed & {
        display: block;
    }

    p {
        margin: 8px 0;
        font-size: 0.8rem;
        white-space: nowrap;
    }
}

/* 반응형 */
@media (max-width: 768px) {
    .resizable-sidebar-wrapper {
        width: 100% !important;
        position: fixed;
        top: 0;
        left: 0;
        height: 60%;
        z-index: 1000;
    }

    .home-container {
        flex-direction: column;
    }
}
</style>
