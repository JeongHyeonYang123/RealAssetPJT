<template>
    <div id="map" style="width: 100%; height: 500px"></div>
</template>

<script lang="ts">
import type { Apartment } from "@/types/Apartment";
import { defineComponent, onMounted, ref } from "vue";

declare global {
    interface Window {
        kakao: any;
    }
}
// ↓ 이 부분 추가!
declare const kakao: any;

export default defineComponent({
    setup() {
        const map = ref<kakao.maps.Map | null>(null);
        const markers = ref<kakao.maps.Marker[]>([]);

        // 마커 모두 제거
        function clearMarkers() {
            markers.value.forEach((marker) => marker.setMap(null));
            markers.value = [];
        }

        // 아파트 데이터 받아 마커 표시
        function showApartmentMarkers(apartments: Apartment[]) {
            clearMarkers();
            apartments.forEach((apt) => {
                const marker = new kakao.maps.Marker({
                    position: new kakao.maps.LatLng(
                        Number(apt.latitude),
                        Number(apt.longitude)
                    ),
                    map: map.value!,
                    title: apt.aptNm,
                });
                markers.value.push(marker);
            });
        }

        // 지도 이벤트 핸들러
        function fetchAndShowApartmentsIfZoomedIn() {
            if (!map.value) return;
            const level = map.value.getLevel();
            const ZOOM_THRESHOLD = 6; // 6 이하로 확대됐을 때만 표시

            if (level > ZOOM_THRESHOLD) {
                clearMarkers();
                return;
            }

            const bounds = map.value.getBounds();
            const sw = bounds.getSouthWest();
            const ne = bounds.getNorthEast();

            fetch(
                `/api/v1/house/apartments-in-bounds?swLat=${sw.getLat()}&swLng=${sw.getLng()}&neLat=${ne.getLat()}&neLng=${ne.getLng()}`
            )
                .then((res) => res.json())
                .then((apartments: Apartment[]) => {
                    showApartmentMarkers(apartments);
                });
        }

        onMounted(() => {
            // 카카오 맵 생성
            const container = document.getElementById("map");
            map.value = new kakao.maps.Map(container, {
                center: new kakao.maps.LatLng(33.450701, 126.570667),
                level: 7,
            });

            // 지도 이벤트 연결
            kakao.maps.event.addListener(
                map.value,
                "idle",
                fetchAndShowApartmentsIfZoomedIn
            );
        });

        return {};
    },
});
</script>
