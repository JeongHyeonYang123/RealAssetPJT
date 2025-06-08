<template>
    <div class="map-wrapper">
        <!-- Top Center Search Bar -->
        <div class="map-top-search-bar">
            <select v-model="selectedSido" @change="onSidoChange">
                <option value="">시/도</option>
                <option
                    v-for="sido in sidoList"
                    :key="sido.admCode"
                    :value="sido.admCode"
                >
                    {{ sido.lowestAdmCodeNm }}
                </option>
            </select>
            <select
                v-model="selectedGugun"
                @change="onGugunChange"
                :disabled="!selectedSido"
            >
                <option value="">구/군</option>
                <option
                    v-for="gugun in gugunList"
                    :key="gugun.admCode"
                    :value="gugun.admCode"
                >
                    {{ gugun.lowestAdmCodeNm }}
                </option>
            </select>
            <select
                v-model="selectedDong"
                @change="onDongChange"
                :disabled="!selectedGugun"
            >
                <option value="">동</option>
                <option
                    v-for="dong in dongList"
                    :key="dong.admCode"
                    :value="dong.admCode"
                >
                    {{ dong.lowestAdmCodeNm }}
                </option>
            </select>
            <input
                v-model="searchText"
                type="text"
                placeholder="아파트명 검색"
                class="map-search-input"
                @keyup.enter="onSearch"
            />
            <button
                class="map-search-btn"
                @click="onSearch"
                :disabled="isLoading"
            >
                {{ isLoading ? "검색중..." : "검색" }}
            </button>
            <button class="map-reset-btn" @click="onReset">초기화</button>
        </div>

        <!-- 로딩 또는 에러 메시지 -->
        <div v-if="isLoading" class="search-status loading">
            <span
                >🔄
                {{
                    selectedLocationInfo ? "지역 이동중..." : "검색 중..."
                }}</span
            >
        </div>
        <div v-else-if="errorMessage" class="search-status error">
            <span>❌ {{ errorMessage }}</span>
            <button @click="clearError" class="clear-btn">✕</button>
        </div>
        <div v-else-if="selectedLocationInfo" class="search-status location">
            <span>📍 {{ selectedLocationInfo }}</span>
        </div>

        <!-- Map -->
        <div id="kakao-map" ref="mapContainer"></div>

        <!-- Controls -->
        <div class="map-controls">
            <button class="map-control-btn" @click="zoomIn">+</button>
            <button class="map-control-btn" @click="zoomOut">-</button>
            <button class="map-control-btn" @click="resetMap">
                <span class="reset-icon">⟳</span>
            </button>
        </div>

        <!-- 🤖 프리미엄 챗봇 아이콘 -->
        <div
            class="chatbot-icon"
            @click="toggleChatbot"
            :class="{ active: isChatbotOpen }"
        >
            <div class="icon-wrapper">
                <i class="fas fa-robot"></i>
                <div class="pulse-ring"></div>
            </div>
        </div>

        <!-- 🗨️ 프리미엄 채팅창 -->
        <div v-if="isChatbotOpen" class="chatbot-container show">
            <!-- 헤더 -->
            <div class="chatbot-header">
                <div class="header-left">
                    <div class="bot-avatar">
                        <i class="fas fa-robot"></i>
                    </div>
                    <div class="bot-info">
                        <h4>부동산 AI 상담사</h4>
                        <span class="status">온라인</span>
                    </div>
                </div>
                <button class="close-btn" @click="closeChatbot">
                    <i class="fas fa-times"></i>
                </button>
            </div>

            <!-- 메시지 영역 -->
            <div class="chatbot-messages" ref="messagesContainer">
                <div
                    v-for="(msg, index) in chatMessages"
                    :key="index"
                    class="message-wrapper"
                    :class="{
                        'user-message': msg.isUser,
                        'bot-message': !msg.isUser,
                    }"
                >
                    <!-- 봇 메시지 -->
                    <div v-if="!msg.isUser" class="message-container">
                        <div class="message-avatar">
                            <i class="fas fa-robot"></i>
                        </div>
                        <div class="message-bubble bot-bubble">
                            <div class="message-text">{{ msg.text }}</div>
                            <div class="message-time">
                                {{ formatTime(msg.timestamp) }}
                            </div>
                        </div>
                    </div>

                    <!-- 사용자 메시지 -->
                    <div v-else class="message-container user-container">
                        <div class="message-bubble user-bubble">
                            <div class="message-text">{{ msg.text }}</div>
                            <div class="message-time">
                                {{ formatTime(msg.timestamp) }}
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 타이핑 인디케이터 -->
                <div v-if="isTyping" class="message-wrapper bot-message">
                    <div class="message-container">
                        <div class="message-avatar">
                            <i class="fas fa-robot"></i>
                        </div>
                        <div class="typing-bubble">
                            <div class="typing-indicator">
                                <span></span>
                                <span></span>
                                <span></span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 입력 영역 -->
            <div class="chatbot-input">
                <div class="input-wrapper">
                    <input
                        v-model="currentMessage"
                        @keyup.enter="sendMessage"
                        placeholder="메시지를 입력하세요..."
                        :disabled="isTyping"
                        class="message-input"
                    />
                    <button
                        @click="sendMessage"
                        :disabled="!currentMessage.trim() || isTyping"
                        class="send-button"
                    >
                        <i class="fas fa-paper-plane"></i>
                    </button>
                </div>
            </div>
        </div>

        <!-- 채팅창 오버레이 -->
        <div
            v-if="isChatbotOpen"
            class="chatbot-overlay"
            @click="closeChatbot"
        ></div>
    </div>
</template>

<script setup lang="ts">
import {
    ref,
    onMounted,
    onUnmounted,
    defineEmits,
    defineExpose,
    computed,
    nextTick,
} from "vue";
import { Property, Transaction } from "../../types";
import {
    showApartmentMarkers,
    showSidoMarkers,
    showGugunMarkers,
    showDongMarkers,
    type SidoMarker,
    type GugunMarker,
    type DongMarker,
} from "../../util/ShowMarker.ts";
import type { Apartment } from "../../types/Apartment.ts";
import { admApi } from "../../../services/admApi";

declare global {
    interface Window {
        kakao: any;
    }
}

const emit = defineEmits([
    "map-ready",
    "marker-click",
    "search",
    "location-selected",
    "apartment-marker-click",
    "dong-marker-click",
    "gugun-marker-click",
    "sido-marker-click",
    "commercial-area-marker-click",
]);

const mapContainer = ref<HTMLElement | null>(null);
const map = ref<any>(null);
const geocoder = ref<any>(null);
const places = ref<any>(null);
const markers = ref<any[]>([]);
const clusterer = ref<any>(null);
const isMapInitialized = ref(false);
const apartments = ref<Apartment[]>([]);

// 로딩 및 에러 상태
const isLoading = ref(false);
const errorMessage = ref("");

// 🤖 챗봇 관련 상태
const isChatbotOpen = ref(false);
const isTyping = ref(false);
const currentMessage = ref("");
const messagesContainer = ref<HTMLElement | null>(null);

// 💬 채팅 메시지 데이터
const chatMessages = ref([
    {
        text: "안녕하세요! 부동산 AI 상담사입니다. 지도에서 보고 계신 아파트나 지역에 대해 궁금한 것이 있으시면 언제든 물어보세요! 😊",
        isUser: false,
        timestamp: new Date(),
    },
]);

// 상권 마커 클릭 핸들러
const handleCommercialMarkerClick = (commercialArea: any) => {
    console.log("MapComponent: 상권 마커 클릭", commercialArea);
    emit("commercial-area-marker-click", commercialArea);
};

// 아파트 마커 클릭 핸들러
const handleApartmentMarkerClick = (apartment: Apartment) => {
    console.log("MapComponent: 아파트 마커 클릭", apartment);
    emit("apartment-marker-click", apartment);
};

// API 연동된 드롭다운 데이터
const sidoList = ref<any[]>([]);
const gugunList = ref<any[]>([]);
const dongList = ref<any[]>([]);
const selectedSido = ref("");
const selectedGugun = ref("");
const selectedDong = ref("");
const searchText = ref("");

// 선택된 지역 정보 표시
const selectedLocationInfo = computed(() => {
    const parts = [];

    if (selectedSido.value) {
        const sido = sidoList.value.find(
            (s) => s.admCode === selectedSido.value
        );
        if (sido) parts.push(sido.lowestAdmCodeNm);
    }

    if (selectedGugun.value) {
        const gugun = gugunList.value.find(
            (g) => g.admCode === selectedGugun.value
        );
        if (gugun) parts.push(gugun.lowestAdmCodeNm);
    }

    if (selectedDong.value) {
        const dong = dongList.value.find(
            (d) => d.admCode === selectedDong.value
        );
        if (dong) parts.push(dong.lowestAdmCodeNm);
    }

    return parts.length > 0 ? parts.join(" ") : "";
});

const isDetailMode = ref(false);
const overlays = ref<any[]>([]);

function setDetailMode(flag) {
    isDetailMode.value = flag;
}

onMounted(() => {
    const KAKAO_KEY = import.meta.env.VITE_KAKAO_JS_KEY;
    loadSidoList();

    if (window.kakao && window.kakao.maps) {
        window.kakao.maps.load(initializeMap);
    } else {
        if (!document.querySelector('script[src*="dapi.kakao.com"]')) {
            const script = document.createElement("script");
            script.onload = () => window.kakao.maps.load(initializeMap);
            script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_KEY}&libraries=services,clusterer,drawing&autoload=false`;
            document.head.appendChild(script);
        } else {
            const checkKakaoLoaded = setInterval(() => {
                if (window.kakao && window.kakao.maps) {
                    clearInterval(checkKakaoLoaded);
                    window.kakao.maps.load(initializeMap);
                }
            }, 100);
        }
    }
});

onUnmounted(() => {
    if (map.value) {
        map.value.setMap(null);
        map.value = null;
    }
    markers.value.forEach((marker) => marker.setMap(null));
    markers.value = [];
    overlays.value.forEach((overlay) => overlay.setMap(null));
    overlays.value = [];
    if (clusterer.value) {
        clusterer.value.clear();
        clusterer.value = null;
    }
    geocoder.value = null;
    places.value = null;
    isMapInitialized.value = false;
});

// ===== 지도 이동 관련 함수들 =====

async function moveToLocation(locationName: string) {
    if (!geocoder.value || !map.value) {
        showError("지도 서비스를 사용할 수 없습니다.");
        return;
    }

    isLoading.value = true;

    return new Promise((resolve, reject) => {
        geocoder.value.addressSearch(
            locationName,
            (result: any, status: any) => {
                isLoading.value = false;

                if (status === window.kakao.maps.services.Status.OK) {
                    const coords = new window.kakao.maps.LatLng(
                        result[0].y,
                        result[0].x
                    );
                    map.value.setCenter(coords);

                    let zoomLevel = 5;
                    if (selectedDong.value) {
                        zoomLevel = 4;
                    } else if (selectedGugun.value) {
                        zoomLevel = 6;
                    } else if (selectedSido.value) {
                        zoomLevel = 8;
                    }

                    map.value.setLevel(zoomLevel);
                    showError("");
                    resolve(result[0]);
                } else {
                    showError(`"${locationName}" 지역을 찾을 수 없습니다.`);
                    reject(new Error("지역 검색 실패"));
                }
            }
        );
    });
}

async function searchApartmentByName(apartmentName: string) {
    if (!places.value || !map.value) {
        showError("지도 서비스를 사용할 수 없습니다.");
        return;
    }

    isLoading.value = true;

    return new Promise((resolve, reject) => {
        places.value.keywordSearch(
            apartmentName,
            (result: any, status: any) => {
                isLoading.value = false;

                if (status === window.kakao.maps.services.Status.OK) {
                    if (result.length > 0) {
                        const firstResult = result[0];
                        const coords = new window.kakao.maps.LatLng(
                            firstResult.y,
                            firstResult.x
                        );

                        map.value.setCenter(coords);
                        map.value.setLevel(3);

                        showError("");
                        resolve(firstResult);
                    } else {
                        showError(
                            `"${apartmentName}" 아파트를 찾을 수 없습니다.`
                        );
                        reject(new Error("검색 결과 없음"));
                    }
                } else {
                    showError(`"${apartmentName}" 아파트를 찾을 수 없습니다.`);
                    reject(new Error("아파트 검색 실패"));
                }
            }
        );
    });
}

// ===== API 연동 함수들 =====

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

async function onSidoChange() {
    selectedGugun.value = "";
    selectedDong.value = "";
    gugunList.value = [];
    dongList.value = [];

    if (selectedSido.value) {
        try {
            isLoading.value = true;
            const response = await admApi.getGugunList(selectedSido.value);
            if (response.success && response.data.admVOList?.admVOList) {
                gugunList.value = response.data.admVOList.admVOList;
            } else {
                showError("구군 정보를 불러오는데 실패했습니다.");
            }
        } catch (error: any) {
            showError("구군 정보를 불러오는데 실패했습니다.");
        } finally {
            isLoading.value = false;
        }
    }
}

async function onGugunChange() {
    selectedDong.value = "";
    dongList.value = [];

    if (selectedGugun.value) {
        try {
            isLoading.value = true;
            const response = await admApi.getDongList(selectedGugun.value);
            if (response.success && response.data.admVOList?.admVOList) {
                dongList.value = response.data.admVOList.admVOList;
            } else {
                showError("읍면동 정보를 불러오는데 실패했습니다.");
            }
        } catch (error: any) {
            showError("읍면동 정보를 불러오는데 실패했습니다.");
        } finally {
            isLoading.value = false;
        }
    }
}

async function onDongChange() {
    console.log("📍 읍면동 변경:", selectedDong.value);
    if (selectedDong.value) {
        await searchByLocation();
    }
}

async function searchByLocation() {
    if (!selectedLocationInfo.value) {
        showError("검색할 지역을 선택해주세요.");
        return;
    }

    console.log("🔄 지역 기반 검색:", selectedDong.value);
    isLoading.value = true;
    clearError();

    try {
        await moveToLocation(selectedLocationInfo.value);

        if (selectedDong.value) {
            await admApi.getDongCode(selectedDong.value);
        }

        emit("location-selected", {
            sido: selectedSido.value,
            gugun: selectedGugun.value,
            dong: selectedDong.value,
            locationInfo: selectedLocationInfo.value,
        });
    } catch (error: any) {
        showError("지역 검색 중 오류가 발생했습니다.");
    }
}

async function onSearch() {
    if (selectedLocationInfo.value && !searchText.value.trim()) {
        await searchByLocation();
        return;
    }

    if (!selectedLocationInfo.value && searchText.value.trim()) {
        try {
            await searchApartmentByName(searchText.value.trim());

            emit("search", {
                aptName: searchText.value,
                locationInfo: "",
            });
        } catch (error) {
            // 에러 처리는 searchApartmentByName에서 함
        }
        return;
    }

    if (selectedLocationInfo.value && searchText.value.trim()) {
        try {
            await searchApartmentInLocation(
                searchText.value.trim(),
                selectedLocationInfo.value
            );

            emit("search", {
                aptName: searchText.value,
                sido: selectedSido.value,
                gugun: selectedGugun.value,
                dong: selectedDong.value,
                locationInfo: selectedLocationInfo.value,
            });
        } catch (error) {
            await searchApartmentByName(searchText.value.trim());
        }
        return;
    }

    showError("지역을 선택하거나 아파트명을 입력해주세요.");
}

async function searchApartmentInLocation(
    apartmentName: string,
    locationName: string
) {
    if (!places.value || !map.value) {
        showError("지도 서비스를 사용할 수 없습니다.");
        return;
    }

    isLoading.value = true;

    return new Promise((resolve, reject) => {
        const searchKeyword = `${locationName} ${apartmentName}`;

        places.value.keywordSearch(
            searchKeyword,
            (result: any, status: any) => {
                isLoading.value = false;

                if (status === window.kakao.maps.services.Status.OK) {
                    if (result.length > 0) {
                        const firstResult = result[0];
                        const coords = new window.kakao.maps.LatLng(
                            firstResult.y,
                            firstResult.x
                        );

                        map.value.setCenter(coords);
                        map.value.setLevel(3);

                        showError("");
                        resolve(firstResult);
                    } else {
                        reject(
                            new Error(
                                `${locationName}에서 "${apartmentName}"을 찾을 수 없습니다.`
                            )
                        );
                    }
                } else {
                    reject(new Error("검색 실패"));
                }
            }
        );
    });
}

function onReset() {
    selectedSido.value = "";
    selectedGugun.value = "";
    selectedDong.value = "";
    searchText.value = "";
    gugunList.value = [];
    dongList.value = [];
    clearError();
    resetMap();
}

function showError(message: string) {
    errorMessage.value = message;
}

function clearError() {
    errorMessage.value = "";
}

// ===== 기존 지도 관련 함수들 =====

function initializeMap() {
    if (!mapContainer.value || isMapInitialized.value) return;

    try {
        const center = new window.kakao.maps.LatLng(37.5665, 126.978);
        const options = { center, level: 7 };

        map.value = new window.kakao.maps.Map(mapContainer.value, options);

        geocoder.value = new window.kakao.maps.services.Geocoder();
        places.value = new window.kakao.maps.services.Places();

        clusterer.value = new window.kakao.maps.MarkerClusterer({
            map: map.value,
            averageCenter: true,
            minLevel: 6,
            styles: [
                {
                    width: "50px",
                    height: "50px",
                    background: "rgba(45, 96, 232, 0.8)",
                    color: "#fff",
                    borderRadius: "50%",
                    textAlign: "center",
                    lineHeight: "50px",
                    fontSize: "14px",
                    fontWeight: "bold",
                },
            ],
        });

        window.kakao.maps.event.addListener(map.value, "idle", onMapIdle);
        isMapInitialized.value = true;
        emit("map-ready");
    } catch (error) {
        showError("지도 초기화에 실패했습니다.");
    }
}

function onMapIdle() {
    if (isDetailMode.value) return;
    if (!map.value) return;
    const level = map.value.getLevel();
    const bounds = map.value.getBounds();
    const sw = bounds.getSouthWest();
    const ne = bounds.getNorthEast();
    const params = {
        swLat: sw.getLat(),
        swLng: sw.getLng(),
        neLat: ne.getLat(),
        neLng: ne.getLng(),
    };
    clearMarkers();
    if (level >= 10) {
        fetchAndShowSidoMarkers(params);
    } else if (level >= 8 && level <= 9) {
        fetchAndShowGugunMarkers(params);
    } else if (level >= 5 && level <= 7) {
        fetchAndShowDongMarkers(params);
    } else if (level >= 1 && level <= 4) {
        fetchAndShowApartmentMarkers(params);
    }
}

function clearMarkers() {
    overlays.value.forEach((overlay) => overlay.setMap(null));
    overlays.value.length = 0;
}

async function fetchAndShowSidoMarkers(params: any) {
    const url = `http://localhost:8080/api/v1/house/areas/sido-avg-price?${new URLSearchParams(
        params
    )}`;
    console.log("Fetching sido data with URL:", url);

    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`Network response was not ok: ${response.status}`);
        }

        const contentType = response.headers.get("content-type");
        if (!contentType || !contentType.includes("application/json")) {
            throw new TypeError("Received content is not JSON");
        }

        const sidoData: SidoMarker[] = await response.json();
        console.log("Received sido data:", sidoData);

        showSidoMarkers(map.value, overlays.value, sidoData);
        console.log(`시도별 마커 ${sidoData.length}개 표시됨`);
    } catch (error) {
        console.error("Error fetching sido data:", error);
    }
}

async function fetchAndShowGugunMarkers(params: any) {
    const url = `http://localhost:8080/api/v1/house/areas/gugun-avg-price?${new URLSearchParams(
        params
    )}`;
    console.log("Fetching gugun data with URL:", url);

    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`Network response was not ok: ${response.status}`);
        }

        const contentType = response.headers.get("content-type");
        if (!contentType || !contentType.includes("application/json")) {
            throw new TypeError("Received content is not JSON");
        }

        const gugunData: GugunMarker[] = await response.json();
        console.log("Received gugun data:", gugunData);

        showGugunMarkers(map.value, overlays.value, gugunData);
        console.log(`구군별 마커 ${gugunData.length}개 표시됨`);
    } catch (error) {
        console.error("Error fetching gugun data:", error);
    }
}

async function fetchAndShowDongMarkers(params: any) {
    const url = `http://localhost:8080/api/v1/house/areas/dong-avg-price?${new URLSearchParams(
        params
    )}`;
    console.log("Fetching dong data with URL:", url);

    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`Network response was not ok: ${response.status}`);
        }

        const contentType = response.headers.get("content-type");
        if (!contentType || !contentType.includes("application/json")) {
            throw new TypeError("Received content is not JSON");
        }

        const dongData: DongMarker[] = await response.json();
        console.log("Received dong data:", dongData);

        showDongMarkers(map.value, overlays.value, dongData);
        console.log(`동별 마커 ${dongData.length}개 표시됨`);
    } catch (error) {
        console.error("Error fetching dong data:", error);
    }
}

async function fetchAndShowApartmentMarkers(params: any) {
    const url = `http://localhost:8080/api/v1/house/apartments-in-bounds?${new URLSearchParams(
        params
    )}`;
    console.log("Fetching apartments with URL:", url);

    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`Network response was not ok: ${response.status}`);
        }

        const contentType = response.headers.get("content-type");
        if (!contentType || !contentType.includes("application/json")) {
            throw new TypeError("Received content is not JSON");
        }

        const apartments: Apartment[] = await response.json();
        console.log("Received apartments:", apartments);

        showApartmentMarkers(
            map.value,
            overlays.value,
            apartments,
            handleApartmentMarkerClick
        );
        console.log(`아파트 마커 ${apartments.length}개 표시됨`);
    } catch (error) {
        console.error("Error fetching apartments:", error);
    }
}

// ✅ 거리 계산 함수 (단위: 미터)
function getDistanceFromLatLonInM(lat1, lon1, lat2, lon2) {
    const R = 6371e3;
    const toRad = (deg) => (deg * Math.PI) / 180;
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(toRad(lat1)) *
            Math.cos(toRad(lat2)) *
            Math.sin(dLon / 2) *
            Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

// MapComponent.vue의 searchNearbyCommercialAreas 함수 수정
function searchNearbyCommercialAreas(centerLat, centerLng) {
    console.log("🔍 주변 상권 검색 시작:", centerLat, centerLng);

    // ✅ 검색 시작 이벤트 emit
    emit("commercial-search-start");

    const categories = [
        { code: "FD6", name: "음식점", emoji: "🍽️", color: "#FF6B35" },
        { code: "CE7", name: "카페", emoji: "☕", color: "#8B4513" },
        { code: "CS2", name: "편의점", emoji: "🏪", color: "#32CD32" },
        { code: "MT1", name: "대형마트", emoji: "🛒", color: "#4169E1" },
        { code: "BK9", name: "은행", emoji: "🏦", color: "#FFD700" },
        { code: "HP8", name: "병원", emoji: "🏥", color: "#FD79A8" },
        { code: "PM9", name: "약국", emoji: "💊", color: "#98D8C8" },
        { code: "SW8", name: "지하철역", emoji: "🚇", color: "#6C5CE7" },
        { code: "PK6", name: "주차장", emoji: "🅿️", color: "#A8A8A8" },
    ];

    const centerPosition = new window.kakao.maps.LatLng(centerLat, centerLng);
    const allCommercialAreas = [];

    let completedCategories = 0;
    const totalCategories = categories.length;

    categories.forEach((category) => {
        places.value.categorySearch(
            category.code,
            (data, status) => {
                if (status === window.kakao.maps.services.Status.OK) {
                    data.forEach((place) => {
                        const distance = getDistanceFromLatLonInM(
                            centerLat,
                            centerLng,
                            place.y,
                            place.x
                        );

                        if (distance <= 1000) {
                            const commercialData = {
                                ...place,
                                category: category.name,
                                distance: Math.round(distance),
                            };

                            allCommercialAreas.push(commercialData);
                            addCommercialMarker(place, category, distance);
                        }
                    });
                }

                completedCategories++;

                // ✅ 모든 카테고리 검색 완료 시 데이터 emit
                if (completedCategories === totalCategories) {
                    emit("commercial-areas-loaded", allCommercialAreas);
                }
            },
            {
                location: centerPosition,
                radius: 1000,
                size: 15,
            }
        );
    });
}

// ✅ 상권 마커 추가 함수
function addCommercialMarker(place, category, distance) {
    const position = new window.kakao.maps.LatLng(place.y, place.x);

    const content = `
    <div class="commercial-marker"
         style="position:relative;width:36px;height:36px;cursor:pointer;z-index:100;"
         data-place-id="${place.id}">
      <div style="
        position:absolute;
        top:0;left:0;
        width:100%;height:100%;
        background:${category.color};
        border-radius:50%;
        box-shadow:0 3px 8px rgba(0,0,0,0.4);
        display:flex;
        align-items:center;
        justify-content:center;
        font-size:18px;
        border:3px solid white;
      ">
        ${category.emoji}
      </div>
      <div class="commercial-info" style="
        position:absolute;
        bottom:105%;
        left:50%;
        transform:translateX(-50%);
        background:white;
        padding:8px 12px;
        border-radius:8px;
        box-shadow:0 4px 12px rgba(0,0,0,0.3);
        white-space:nowrap;
        margin-bottom:5px;
        display:none;
        z-index:1000;
        border:1px solid #ddd;
        min-width:120px;
      ">
        <div style="font-weight:bold;color:#333;font-size:14px;">${
            place.place_name
        }</div>
        <div style="color:#666;font-size:12px;margin-top:4px;">${
            category.name
        }</div>
        <div style="color:#2196f3;font-size:11px;margin-top:2px;font-weight:500;">${Math.round(
            distance
        )}m</div>
        ${
            place.phone
                ? `<div style="color:#999;font-size:10px;margin-top:1px;">📞 ${place.phone}</div>`
                : ""
        }
      </div>
    </div>
  `;

    const overlay = new window.kakao.maps.CustomOverlay({
        position,
        content,
        yAnchor: 1,
        zIndex: 100,
    });

    try {
        overlay.setMap(map.value);
        overlays.value.push(overlay);

        console.log(
            `✅ 상권 마커 추가: ${place.place_name} (${Math.round(distance)}m)`
        );

        // DOM 이벤트 리스너 추가
        nextTick(() => {
            const element = document.querySelector(
                `[data-place-id="${place.id}"]`
            ) as HTMLElement;
            if (element) {
                element.addEventListener("mouseenter", () => {
                    const infoElement = element.querySelector(
                        ".commercial-info"
                    ) as HTMLElement;
                    if (infoElement) infoElement.style.display = "block";
                });

                element.addEventListener("mouseleave", () => {
                    const infoElement = element.querySelector(
                        ".commercial-info"
                    ) as HTMLElement;
                    if (infoElement) infoElement.style.display = "none";
                });

                element.addEventListener("click", () => {
                    console.log("🖱️ 상권 마커 클릭:", place.place_name);
                    handleCommercialMarkerClick({
                        ...place,
                        category: category.name,
                        distance: Math.round(distance),
                        storeName: place.place_name,
                        categoryMajorName: category.name,
                        latitude: place.y,
                        longitude: place.x,
                    });
                });
            }
        });
    } catch (error) {
        console.error(`❌ 상권 마커 추가 실패: ${place.place_name}`, error);
    }
}

// ✅ 수정된 addSingleApartmentMarker 함수 (카카오맵 Places 서비스 사용)
async function addSingleApartmentMarker(property) {
    if (!map.value) return;

    console.log("🏠 아파트 마커 추가 시작:", property.name);

    // 기존 오버레이 제거
    overlays.value.forEach((overlay) => overlay.setMap(null));
    overlays.value = [];

    const position = new window.kakao.maps.LatLng(property.lat, property.lng);
    map.value.setCenter(position);
    map.value.setLevel(4);

    // 아파트 마커 추가
    const apartmentContent = `
    <div class="speech-bubble-marker" style="position:relative;width:80px;height:80px;z-index:10;">
      <div class="crown" style="position:absolute;top:-18px;left:50%;transform:translateX(-50%);font-size:1.5rem;z-index:2;">👑</div>
      <img src="/speechbubble.png" style="width:80px;height:80px;display:block;">
      <div class="bubble-text"
           style="position:absolute;top:28px;left:0;width:100%;text-align:center;font-size:15px;font-weight:bold;color:#fff;text-shadow:0 1px 2px rgba(0,0,0,0.25);">
        <div style="font-size:14px;font-weight:700;margin-bottom:2px;">${
            property.name
        }</div>
        <div style="font-size:18px;font-weight:900;">${
            property.recentPrice ? formatPriceShort(property.recentPrice) : ""
        }</div>
      </div>
    </div>
  `;

    const apartmentOverlay = new window.kakao.maps.CustomOverlay({
        position,
        content: apartmentContent,
        yAnchor: 1,
        zIndex: 10,
    });

    apartmentOverlay.setMap(map.value);
    overlays.value.push(apartmentOverlay);

    // ✅ 카카오맵 Places 서비스로 주변 상권 검색
    if (places.value) {
        console.log("🏪 카카오맵 Places 서비스로 상권 검색 시작");
        searchNearbyCommercialAreas(property.lat, property.lng);
    } else {
        console.error("❌ Places 서비스가 초기화되지 않았습니다");
    }

    console.log("✅ 아파트 마커 추가 완료:", property.name);
}

// 기존 함수들...
function addTransactionMarkers(transactions: Transaction[]) {
    if (!map.value) {
        console.error("지도가 초기화되지 않았습니다 (addTransactionMarkers)");
        return;
    }

    markers.value.forEach((marker) => marker.setMap(null));
    markers.value = [];

    const filteredTransactions = filterTransactionsByPrice(transactions);

    const newMarkers = filteredTransactions.map((transaction) => {
        const position = new window.kakao.maps.LatLng(
            transaction.lat,
            transaction.lng
        );

        const content = document.createElement("div");
        content.className = "price-marker";
        content.innerHTML = `<span>${formatPriceShort(
            transaction.price
        )}</span>`;
        content.style.color = getPriceColor(transaction.price);
        content.style.cursor = "pointer";

        const customOverlay = new window.kakao.maps.CustomOverlay({
            position,
            content,
            yAnchor: 1,
        });

        window.kakao.maps.event.addListener(customOverlay, "click", () => {
            console.log("트랜잭션 마커 클릭:", transaction.propertyId);
            emit("marker-click", transaction.propertyId);
        });

        content.addEventListener("click", () => {
            console.log("트랜잭션 DOM 엘리먼트 클릭:", transaction.propertyId);
            emit("marker-click", transaction.propertyId);
        });

        customOverlay.setMap(map.value);
        return customOverlay;
    });

    markers.value = newMarkers;
    console.log(`트랜잭션 마커 ${markers.value.length}개 생성 완료`);
}

function focusOnSearchResults(properties: Property[]) {
    if (!map.value) {
        console.error("지도가 초기화되지 않았습니다 (focusOnSearchResults)");
        return;
    }

    if (properties.length === 0) {
        console.log("검색 결과가 없습니다");
        return;
    }

    console.log(`검색 결과 ${properties.length}개에 초점 맞추기`);

    const bounds = new window.kakao.maps.LatLngBounds();

    properties.forEach((property) => {
        const position = new window.kakao.maps.LatLng(
            property.lat,
            property.lng
        );
        bounds.extend(position);
    });

    map.value.setBounds(bounds);
    addPropertyMarkers(properties);
}

function focusOnProperty(property: Property) {
    if (!map.value) {
        console.error("지도가 초기화되지 않았습니다 (focusOnProperty)");
        return;
    }

    if (!property) {
        console.log("속성 정보가 없습니다");
        return;
    }

    console.log(
        "속성에 초점 맞추기:",
        property.name || property.aptName || property.title || "이름없음"
    );

    const position = new window.kakao.maps.LatLng(property.lat, property.lng);
    map.value.setCenter(position);
    map.value.setLevel(3);

    const properties = [property];
    addPropertyMarkers(properties);
}

function addPropertyMarkers(properties: Property[]) {
    if (!map.value) {
        console.error("지도가 초기화되지 않았습니다!");
        return;
    }

    markers.value.forEach((marker) => marker.setMap(null));
    markers.value = [];

    properties.forEach((property) => {
        const position = new window.kakao.maps.LatLng(
            property.lat,
            property.lng
        );
        const marker = new window.kakao.maps.Marker({
            position: position,
            clickable: true,
        });

        const infowindow = new window.kakao.maps.InfoWindow({
            content: `<div style="width:150px;text-align:center;padding:6px 0;">${
                property.name ||
                property.aptName ||
                property.title ||
                "이름없음"
            }</div>`,
            removable: true,
        });

        window.kakao.maps.event.addListener(marker, "click", function () {
            markers.value.forEach((m) => {
                if (m.infowindow && m.infowindow.getMap()) {
                    m.infowindow.close();
                }
            });
            infowindow.open(map.value, marker);
            emit("marker-click", property.id);
        });
        marker.setMap(map.value);
        marker.infowindow = infowindow;
        markers.value.push(marker);
    });
    console.log("마커 생성 완료:", markers.value.length);
}

function filterTransactionsByPrice(transactions: Transaction[]): Transaction[] {
    return transactions;
}

function formatPriceShort(price: number): string {
    const priceInBillion = price / 10000;
    if (priceInBillion >= 1) {
        return `${priceInBillion.toFixed(1)}억`;
    }
    return `${price}만`;
}

function getPriceColor(price: number): string {
    const priceInBillion = price / 10000;
    if (priceInBillion >= 10) return "#FF5722";
    if (priceInBillion >= 5) return "#F44336";
    if (priceInBillion >= 3) return "#FF9800";
    if (priceInBillion >= 1) return "#2196F3";
    return "#4CAF50";
}

function zoomIn() {
    if (!map.value) return;
    map.value.setLevel(map.value.getLevel() - 1);
}

function zoomOut() {
    if (!map.value) return;
    map.value.setLevel(map.value.getLevel() + 1);
}

function resetMap() {
    if (!map.value) return;
    map.value.setCenter(new window.kakao.maps.LatLng(37.5665, 126.978));
    map.value.setLevel(7);
    clearMarkers();
}

function addApartmentMarkers(newApartments: Apartment[]) {
    if (!map.value) return;
    apartments.value = newApartments;
    showApartmentMarkers(map.value, markers.value, apartments.value);
}

function moveToLatLng(lat, lng) {
    if (!map.value) return;
    const position = new window.kakao.maps.LatLng(lat, lng);
    map.value.setCenter(position);
}

function clearAllMarkers() {
    if (markers.value && markers.value.length > 0) {
        markers.value.forEach((marker) => marker.setMap(null));
        markers.value = [];
    }
    if (overlays.value && overlays.value.length > 0) {
        overlays.value.forEach((overlay) => overlay.setMap(null));
        overlays.value = [];
    }
}

// 🤖 챗봇 함수들
const toggleChatbot = () => {
    isChatbotOpen.value = !isChatbotOpen.value;
    if (isChatbotOpen.value) {
        nextTick(() => {
            scrollToBottom();
        });
    }
};

const closeChatbot = () => {
    isChatbotOpen.value = false;
};

const sendMessage = async () => {
    if (!currentMessage.value.trim() || isTyping.value) return;

    const userMessage = {
        text: currentMessage.value,
        isUser: true,
        timestamp: new Date(),
    };

    chatMessages.value.push(userMessage);
    const messageText = currentMessage.value;
    currentMessage.value = "";

    await nextTick();
    scrollToBottom();
    isTyping.value = true;

    try {
        const response = await fetch("/api/v1/chatbot/chat", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json",
            },
            body: JSON.stringify(messageText),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        chatMessages.value.push({
            text: data.answer,
            isUser: false,
            timestamp: new Date(),
        });

        // 아파트 추천 결과가 있는 경우 지도에 표시
        if (data.apiUsed === "apartment-recommend" && data.apartments) {
            // 아파트 마커 표시 로직
            if (data.apartments.length > 0) {
                const firstApartment = data.apartments[0];
                addSingleApartmentMarker({
                    name: firstApartment.aptName,
                    lat: firstApartment.latitude,
                    lng: firstApartment.longitude,
                    recentPrice: firstApartment.latestDealAmount,
                });
            }
        }

        console.log(`🤖 AI 분석: ${data.intent}, API: ${data.apiUsed}`);
    } catch (error) {
        console.error("챗봇 API 오류:", error);
        chatMessages.value.push({
            text: "죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
            isUser: false,
            timestamp: new Date(),
        });
    } finally {
        isTyping.value = false;
        await nextTick();
        scrollToBottom();
    }
};

const scrollToBottom = () => {
    if (messagesContainer.value) {
        messagesContainer.value.scrollTop =
            messagesContainer.value.scrollHeight;
    }
};

const formatTime = (timestamp: Date) => {
    return new Date(timestamp).toLocaleTimeString("ko-KR", {
        hour: "2-digit",
        minute: "2-digit",
    });
};

defineExpose({
    focusOnSearchResults,
    focusOnProperty,
    addTransactionMarkers,
    addApartmentMarkers,
    moveToLocation,
    moveToLatLng,
    searchApartmentByName,
    clearAllMarkers,
    addSingleApartmentMarker,
    setDetailMode,
});
</script>

<style lang="scss">
.map-wrapper {
    position: relative;
    width: 100%;
    height: 100%;
    min-height: 500px;
}

#kakao-map {
    width: 100%;
    height: 100%;
    min-height: 500px;
}

.map-controls {
    position: absolute;
    right: 16px;
    top: 16px;
    display: flex;
    flex-direction: column;
    z-index: 5;
}

.map-control-btn {
    width: 40px;
    height: 40px;
    border-radius: 4px;
    background: white;
    color: #333;
    font-size: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    cursor: pointer;
    border: none;

    &:hover {
        background: #f5f5f5;
    }

    .reset-icon {
        font-size: 16px;
    }
}

.price-marker,
.property-marker {
    padding: 6px 10px;
    background: white;
    border-radius: 16px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    font-weight: 500;
    font-size: 12px;
    position: relative;
    white-space: nowrap;

    &:after {
        content: "";
        position: absolute;
        bottom: -6px;
        left: 50%;
        transform: translateX(-50%);
        width: 0;
        height: 0;
        border-left: 6px solid transparent;
        border-right: 6px solid transparent;
        border-top: 6px solid white;
    }

    span {
        display: inline-block;
    }
}

.property-marker {
    background: var(--color-primary);

    span {
        color: white !important;
    }

    &:after {
        border-top-color: var(--color-primary);
    }
}

.custom-marker {
    padding: 6px 10px;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    font-weight: 500;
    font-size: 13px;
    position: relative;
    white-space: nowrap;
    border: 1px solid #ddd;
    cursor: pointer;
    z-index: 1;

    &:hover {
        background: #f0f0f0;
    }
}

/* ✅ 상권 마커 스타일 */
.commercial-marker {
    cursor: pointer;
    transition: transform 0.2s ease;

    &:hover {
        transform: scale(1.1);
    }
}

.commercial-info {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto",
        sans-serif;
}

.map-top-search-bar {
    position: absolute;
    top: 24px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 10;
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 8px;
    min-width: 600px;
    max-width: 90vw;
    background: none;
    box-shadow: none;
    border-radius: 0;
    padding: 0;
}

.map-top-search-bar select,
.map-top-search-bar .map-search-input {
    height: 36px;
    border: 1px solid #ddd;
    border-radius: 8px;
    padding: 0 12px;
    font-size: 15px;
    background: #fff;
    transition: all 0.2s ease;

    &:disabled {
        background-color: #f5f5f5;
        color: #999;
        cursor: not-allowed;
    }

    &:focus {
        outline: none;
        border-color: #2d60e8;
        box-shadow: 0 0 0 2px rgba(45, 96, 232, 0.1);
    }
}

.map-top-search-bar .map-search-input {
    width: 180px;
}

.map-top-search-bar .map-search-btn,
.map-top-search-bar .map-reset-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: auto;
    white-space: nowrap;
    height: 36px;
    border: none;
    border-radius: 8px;
    padding: 0 18px;
    font-size: 15px;
    background: #2d60e8;
    color: #fff;
    cursor: pointer;
    transition: background 0.2s;

    &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
    }
}

.map-top-search-bar .map-reset-btn {
    background: #f5f5f5;
    color: #333;
    margin-left: 4px;
}

.map-top-search-bar .map-search-btn:hover:not(:disabled) {
    background: #1a3fa6;
}

.map-top-search-bar .map-reset-btn:hover {
    background: #e0e0e0;
}

.search-status {
    position: absolute;
    top: 70px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 10;
    padding: 8px 16px;
    border-radius: 8px;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 8px;
    max-width: 90vw;

    &.loading {
        background-color: #f0f9ff;
        color: #0369a1;
        border: 1px solid #bae6fd;
    }

    &.error {
        background-color: #fef2f2;
        color: #dc2626;
        border: 1px solid #fecaca;
    }

    &.location {
        background-color: #f0fdf4;
        color: #16a34a;
        border: 1px solid #bbf7d0;
    }

    .clear-btn {
        background: none;
        border: none;
        color: inherit;
        cursor: pointer;
        font-size: 16px;
        padding: 0;

        &:hover {
            opacity: 0.7;
        }
    }
}

/* 프리미엄 챗봇 아이콘 스타일 */
.chatbot-icon {
    position: fixed;
    bottom: 30px;
    right: 30px;
    width: 70px;
    height: 70px;
    cursor: pointer;
    z-index: 1000;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    .icon-wrapper {
        position: relative;
        width: 100%;
        height: 100%;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 8px 32px rgba(102, 126, 234, 0.4);
        transition: all 0.3s ease;

        i {
            color: white;
            font-size: 32px;
            z-index: 3;
            animation: bounce 2s infinite;
        }
    }

    .pulse-ring {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        width: 70px;
        height: 70px;
        border: 2px solid rgba(102, 126, 234, 0.3);
        border-radius: 50%;
        animation: pulse 3s infinite;
    }

    &:hover .icon-wrapper {
        transform: scale(1.1);
        box-shadow: 0 12px 40px rgba(102, 126, 234, 0.6);
    }

    &.active .icon-wrapper {
        background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
    }
}

/* 프리미엄 채팅창 스타일 */
.chatbot-container {
    position: fixed;
    bottom: 120px;
    right: 30px;
    width: 420px;
    height: 650px;
    background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
    border-radius: 24px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15), 0 8px 32px rgba(0, 0, 0, 0.1);
    display: flex;
    flex-direction: column;
    z-index: 1001;
    transform: translateY(20px) scale(0.95);
    opacity: 0;
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(10px);
    animation: breathe 3s ease-in-out infinite;

    &.show {
        transform: translateY(0) scale(1);
        opacity: 1;
    }
}

/* 헤더 */
.chatbot-header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 20px 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-radius: 24px 24px 0 0;
    position: relative;

    .header-left {
        display: flex;
        align-items: center;
        gap: 12px;
    }

    .bot-avatar {
        width: 44px;
        height: 44px;
        background: rgba(255, 255, 255, 0.2);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        backdrop-filter: blur(10px);
        border: 2px solid rgba(255, 255, 255, 0.3);

        i {
            color: white;
            font-size: 20px;
        }
    }

    .bot-info {
        h4 {
            color: white;
            margin: 0;
            font-size: 16px;
            font-weight: 600;
            line-height: 1.2;
        }

        .status {
            color: rgba(255, 255, 255, 0.8);
            font-size: 12px;
            display: flex;
            align-items: center;
            gap: 6px;

            &::before {
                content: "";
                width: 8px;
                height: 8px;
                background: #4ade80;
                border-radius: 50%;
                animation: blink 2s infinite;
            }
        }
    }

    .close-btn {
        width: 36px;
        height: 36px;
        background: rgba(255, 255, 255, 0.1);
        border: none;
        border-radius: 50%;
        color: white;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s ease;
        backdrop-filter: blur(10px);

        &:hover {
            background: rgba(255, 255, 255, 0.2);
            transform: scale(1.1);
        }

        i {
            font-size: 14px;
        }
    }
}

/* 메시지 영역 */
.chatbot-messages {
    flex: 1;
    padding: 24px 20px;
    overflow-y: auto;
    background: linear-gradient(180deg, #fafbff 0%, #f8f9ff 100%);
    display: flex;
    flex-direction: column;
    gap: 16px;

    &::-webkit-scrollbar {
        width: 6px;
    }

    &::-webkit-scrollbar-track {
        background: transparent;
    }

    &::-webkit-scrollbar-thumb {
        background: rgba(102, 126, 234, 0.2);
        border-radius: 3px;
    }
}

.message-wrapper {
    display: flex;
    margin-bottom: 4px;

    &.user-message {
        justify-content: flex-end;
    }

    &.bot-message {
        justify-content: flex-start;
    }
}

.message-container {
    display: flex;
    align-items: flex-end;
    gap: 8px;
    max-width: 85%;

    &.user-container {
        flex-direction: row-reverse;
    }
}

.message-avatar {
    width: 32px;
    height: 32px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);

    i {
        color: white;
        font-size: 14px;
    }
}

.message-bubble {
    padding: 14px 18px;
    border-radius: 22px;
    position: relative;
    word-wrap: break-word;
    animation: messageSlideIn 0.3s ease-out;
    white-space: pre-wrap;
    line-height: 1.6;

    &.bot-bubble {
        background: white;
        color: #374151;
        border-bottom-left-radius: 8px;
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
        border: 1px solid rgba(0, 0, 0, 0.05);
        max-width: 85%;
    }

    &.user-bubble {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border-bottom-right-radius: 8px;
        box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);
        max-width: 85%;
    }

    .message-text {
        font-size: 15px;
        line-height: 1.6;
        margin-bottom: 8px;
        white-space: pre-wrap;
    }

    .message-time {
        font-size: 11px;
        opacity: 0.7;
    }
}

/* 타이핑 인디케이터 */
.typing-bubble {
    background: white;
    padding: 16px 20px;
    border-radius: 20px;
    border-bottom-left-radius: 6px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    border: 1px solid rgba(0, 0, 0, 0.05);
}

.typing-indicator {
    display: flex;
    gap: 4px;
    align-items: center;

    span {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #667eea;
        animation: typing 1.4s infinite ease-in-out;

        &:nth-child(1) {
            animation-delay: -0.32s;
        }
        &:nth-child(2) {
            animation-delay: -0.16s;
        }
        &:nth-child(3) {
            animation-delay: 0s;
        }
    }
}

/* 입력 영역 */
.chatbot-input {
    padding: 20px 24px 24px;
    background: white;
    border-radius: 0 0 24px 24px;
    border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.input-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
    background: #f8f9ff;
    border-radius: 24px;
    padding: 4px 4px 4px 20px;
    border: 2px solid transparent;
    transition: all 0.2s ease;

    &:focus-within {
        border-color: #667eea;
        background: white;
        box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
    }
}

.message-input {
    flex: 1;
    border: none;
    background: transparent;
    padding: 12px 0;
    font-size: 14px;
    color: #374151;
    outline: none;
    font-family: inherit;

    &::placeholder {
        color: #9ca3af;
    }

    &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
    }
}

.send-button {
    width: 40px;
    height: 40px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    border-radius: 50%;
    color: white;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);

    &:hover:not(:disabled) {
        transform: scale(1.05);
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
    }

    &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }

    i {
        font-size: 14px;
    }
}

/* 오버레이 */
.chatbot-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.1);
    z-index: 999;
}

/* 애니메이션 */
@keyframes pulse {
    0% {
        transform: translate(-50%, -50%) scale(1);
        opacity: 1;
    }
    100% {
        transform: translate(-50%, -50%) scale(1.5);
        opacity: 0;
    }
}

@keyframes bounce {
    0%,
    100% {
        transform: translateY(0);
    }
    50% {
        transform: translateY(-5px);
    }
}

@keyframes blink {
    0%,
    50% {
        opacity: 1;
    }
    51%,
    100% {
        opacity: 0.3;
    }
}

@keyframes messageSlideIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

@keyframes typing {
    0%,
    80%,
    100% {
        transform: scale(0.8);
        opacity: 0.5;
    }
    40% {
        transform: scale(1);
        opacity: 1;
    }
}

@keyframes breathe {
    0%,
    100% {
        transform: scale(1);
    }
    50% {
        transform: scale(1.02);
    }
}

/* 반응형 */
@media (max-width: 768px) {
    .chatbot-container {
        right: 15px;
        left: 15px;
        width: auto;
        bottom: 100px;
        height: 70vh;
        max-height: 600px;
    }

    .chatbot-icon {
        bottom: 20px;
        right: 20px;
        width: 56px;
        height: 56px;

        .icon-wrapper i {
            font-size: 24px;
        }

        .pulse-ring {
            width: 56px;
            height: 56px;
        }
    }
}

/* 다크모드 지원 */
@media (prefers-color-scheme: dark) {
    .chatbot-messages {
        background: linear-gradient(180deg, #1f2937 0%, #111827 100%);
    }

    .message-bubble.bot-bubble {
        background: #374151;
        color: #f9fafb;
        border-color: rgba(255, 255, 255, 0.1);
    }

    .input-wrapper {
        background: #374151;

        &:focus-within {
            background: #4b5563;
        }
    }

    .message-input {
        color: #f9fafb;

        &::placeholder {
            color: #9ca3af;
        }
    }
}
</style>
