// src/utils/showApartmentMarkers.ts

import type { Apartment } from '../types/Apartment.ts';
import speechBubbleIcon from '../assets/img/speechbubble.png';

declare global {
    interface Window {
        kakao: any;
    }
}
declare const kakao: any;

// 타입 정의는 그대로 유지...
export interface SidoMarker {
    dong_code: string;
    sido_name: string;
    lat: number;
    lng: number;
    avg_price: number;
    apt_count: number;
    updated_at?: string;
}

export interface GugunMarker {
    dong_code: string;
    sido_name: string;
    gugun_name: string;
    lat: number;
    lng: number;
    avg_price: number;
    apt_count: number;
    updated_at?: string;
}

export interface DongMarker {
    dong_code: string;
    sido_name: string;
    gugun_name: string;
    dong_name: string;
    lat: number;
    lng: number;
    avg_price: number;
    apt_count: number;
    updated_at?: string;
}

/**
 * 지도에 아파트 마커를 표시하는 함수 - 호버 팝업 추가
 */
export function showApartmentMarkers(
    map: kakao.maps.Map,
    overlays: kakao.maps.CustomOverlay[],
    apartments: Apartment[],
    onMarkerClick?: (apartment: Apartment) => void
) {
    // 기존 오버레이 제거
    overlays.forEach(overlay => overlay.setMap(null));
    overlays.length = 0;

    apartments.forEach(apt => {
        const position = new kakao.maps.LatLng(Number(apt.latitude), Number(apt.longitude));
        const priceStr = apt.latestDealAmount ? formatPrice(apt.latestDealAmount) : '정보없음';
        const areaStr = apt.latestExcluUseAr ? Math.round(apt.latestExcluUseAr / 3.30579) + '평' : '정보없음';

        // ✅ 말풍선 마커 HTML
        const content = `
          <div class="speech-bubble-marker" style="position:relative;width:64px;height:64px;cursor:pointer;" data-apt-seq="${apt.aptSeq}">
            <img src="${speechBubbleIcon}"
                 style="width:64px;height:64px;display:block;">
            <div class="bubble-text"
                 style="position:absolute;top:14px;left:0;width:100%;text-align:center;font-size:13px;font-weight:bold;color:#ffffff;pointer-events:none;">
              <div>${priceStr}</div>
              <div style="font-size:12px;font-weight:normal;">${areaStr}</div>
            </div>
          </div>
        `;

        // 말풍선 마커 오버레이 생성
        const overlay = new kakao.maps.CustomOverlay({
            position,
            content,
            yAnchor: 1
        });

        // ✅ 호버 시 표시할 아파트명 인포윈도우 생성
        const aptName = apt.name || apt.aptNm || '아파트명 정보 없음';
        const infoWindow = new kakao.maps.InfoWindow({
            content: `<div style="padding:8px 12px;background:white;border:1px solid #ccc;border-radius:4px;box-shadow:0 2px 6px rgba(0,0,0,0.2);font-size:13px;color:#333;white-space:nowrap;">${aptName}</div>`,
            removable: false
        });

        overlay.setMap(map);
        overlays.push(overlay);

        // ✅ DOM 요소에 마우스 이벤트 추가
        setTimeout(() => {
            const element = document.querySelector(`[data-apt-seq="${apt.aptSeq}"]`) as HTMLElement;
            if (element) {
                // 마우스 호버 시 아파트명 표시
                element.addEventListener('mouseenter', () => {
                    infoWindow.open(map, {
                        getPosition: () => position
                    } as any);
                });

                // 마우스 떠날 시 아파트명 숨김
                element.addEventListener('mouseleave', () => {
                    infoWindow.close();
                });

                // 클릭 이벤트 (기존 기능 유지)
                if (onMarkerClick) {
                    element.addEventListener('click', () => {
                        // 인포윈도우 닫기
                        infoWindow.close();

                        // 지도 중심을 해당 마커 좌표로 부드럽게 이동
                        const moveLatLng = new kakao.maps.LatLng(Number(apt.latitude), Number(apt.longitude));
                        map.panTo(moveLatLng);

                        // 바운스 애니메이션 효과
                        element.classList.remove('marker-bounce');
                        void element.offsetWidth;
                        element.classList.add('marker-bounce');
                        element.addEventListener('animationend', () => {
                            element.classList.remove('marker-bounce');
                        }, { once: true });

                        // 실제 마커 클릭 로직
                        onMarkerClick(apt);
                    });
                }
            }
        }, 100);
    });
}

/**
 * 지도에 시도별 마커를 표시하는 함수 - 호버 기능 추가
 */
export function showSidoMarkers(
    map: kakao.maps.Map,
    overlays: kakao.maps.CustomOverlay[],
    sidoData: SidoMarker[],
    onMarkerClick?: (sido: SidoMarker) => void
) {
    overlays.forEach(overlay => overlay.setMap(null));
    overlays.length = 0;

    sidoData.forEach(sido => {
        const position = new kakao.maps.LatLng(Number(sido.lat), Number(sido.lng));
        const priceStr = sido.avg_price ? formatPrice(sido.avg_price) : '정보없음';

        const content = `
          <div class="speech-bubble-marker sido-marker" style="position:relative;width:80px;height:80px;cursor:pointer;" data-sido-code="${sido.dong_code}">
            <img src="${speechBubbleIcon}"
                 style="width:80px;height:80px;display:block;">
            <div class="bubble-text"
                 style="position:absolute;top:16px;left:0;width:100%;text-align:center;font-size:14px;font-weight:bold;color:#ffffff;pointer-events:none;">
              <div style="font-size:13px;">${sido.sido_name}</div>
              <div style="font-size:14px;margin-top:2px;">${priceStr}</div>
              <div style="font-size:11px;color:#ddd;margin-top:1px;">${sido.apt_count}개 단지</div>
            </div>
          </div>
        `;

        const overlay = new kakao.maps.CustomOverlay({
            position,
            content,
            yAnchor: 1
        });

        // ✅ 시도명 호버 인포윈도우
        const infoWindow = new kakao.maps.InfoWindow({
            content: `<div style="padding:8px 12px;background:white;border:1px solid #ccc;border-radius:4px;box-shadow:0 2px 6px rgba(0,0,0,0.2);font-size:13px;color:#333;white-space:nowrap;">${sido.sido_name} (평균 ${priceStr})</div>`,
            removable: false
        });

        overlay.setMap(map);
        overlays.push(overlay);

        // 이벤트 리스너 추가
        setTimeout(() => {
            const element = document.querySelector(`[data-sido-code="${sido.dong_code}"]`) as HTMLElement;
            if (element) {
                element.addEventListener('mouseenter', () => {
                    infoWindow.open(map, {
                        getPosition: () => position
                    } as any);
                });

                element.addEventListener('mouseleave', () => {
                    infoWindow.close();
                });

                if (onMarkerClick) {
                    element.addEventListener('click', () => {
                        infoWindow.close();
                        onMarkerClick(sido);
                    });
                }
            }
        }, 100);
    });
}

/**
 * 지도에 구군별 마커를 표시하는 함수 - 호버 기능 추가
 */
export function showGugunMarkers(
    map: kakao.maps.Map,
    overlays: kakao.maps.CustomOverlay[],
    gugunData: GugunMarker[],
    onMarkerClick?: (gugun: GugunMarker) => void
) {
    overlays.forEach(overlay => overlay.setMap(null));
    overlays.length = 0;

    gugunData.forEach(gugun => {
        const position = new kakao.maps.LatLng(Number(gugun.lat), Number(gugun.lng));
        const priceStr = gugun.avg_price ? formatPrice(gugun.avg_price) : '정보없음';

        const content = `
          <div class="speech-bubble-marker gugun-marker" style="position:relative;width:72px;height:72px;cursor:pointer;" data-gugun-code="${gugun.dong_code}">
            <img src="${speechBubbleIcon}"
                 style="width:72px;height:72px;display:block;">
            <div class="bubble-text"
                 style="position:absolute;top:15px;left:0;width:100%;text-align:center;font-size:13px;font-weight:bold;color:#ffffff;pointer-events:none;">
              <div style="font-size:12px;">${gugun.gugun_name}</div>
              <div style="font-size:13px;margin-top:2px;">${priceStr}</div>
              <div style="font-size:10px;color:#ddd;margin-top:1px;">${gugun.apt_count}개</div>
            </div>
          </div>
        `;

        const overlay = new kakao.maps.CustomOverlay({
            position,
            content,
            yAnchor: 1
        });

        // ✅ 구군명 호버 인포윈도우
        const infoWindow = new kakao.maps.InfoWindow({
            content: `<div style="padding:8px 12px;background:white;border:1px solid #ccc;border-radius:4px;box-shadow:0 2px 6px rgba(0,0,0,0.2);font-size:13px;color:#333;white-space:nowrap;">${gugun.gugun_name} (평균 ${priceStr})</div>`,
            removable: false
        });

        overlay.setMap(map);
        overlays.push(overlay);

        // 이벤트 리스너 추가
        setTimeout(() => {
            const element = document.querySelector(`[data-gugun-code="${gugun.dong_code}"]`) as HTMLElement;
            if (element) {
                element.addEventListener('mouseenter', () => {
                    infoWindow.open(map, {
                        getPosition: () => position
                    } as any);
                });

                element.addEventListener('mouseleave', () => {
                    infoWindow.close();
                });

                if (onMarkerClick) {
                    element.addEventListener('click', () => {
                        infoWindow.close();
                        onMarkerClick(gugun);
                    });
                }
            }
        }, 100);
    });
}

/**
 * 지도에 동별 마커를 표시하는 함수 - 호버 기능 추가
 */
export function showDongMarkers(
    map: kakao.maps.Map,
    overlays: kakao.maps.CustomOverlay[],
    dongData: DongMarker[],
    onMarkerClick?: (dong: DongMarker) => void
) {
    overlays.forEach(overlay => overlay.setMap(null));
    overlays.length = 0;

    dongData.forEach(dong => {
        const position = new kakao.maps.LatLng(Number(dong.lat), Number(dong.lng));
        const priceStr = dong.avg_price ? formatPrice(dong.avg_price) : '정보없음';

        const content = `
          <div class="speech-bubble-marker dong-marker" style="position:relative;width:64px;height:64px;cursor:pointer;" data-dong-code="${dong.dong_code}">
            <img src="${speechBubbleIcon}"
                 style="width:64px;height:64px;display:block;">
            <div class="bubble-text"
                 style="position:absolute;top:14px;left:0;width:100%;text-align:center;font-size:12px;font-weight:bold;color:#ffffff;pointer-events:none;">
              <div style="font-size:11px;">${dong.dong_name}</div>
              <div style="font-size:12px;margin-top:2px;">${priceStr}</div>
              <div style="font-size:10px;color:#ddd;margin-top:1px;">${dong.apt_count}개</div>
            </div>
          </div>
        `;

        const overlay = new kakao.maps.CustomOverlay({
            position,
            content,
            yAnchor: 1
        });

        // ✅ 동명 호버 인포윈도우
        const infoWindow = new kakao.maps.InfoWindow({
            content: `<div style="padding:8px 12px;background:white;border:1px solid #ccc;border-radius:4px;box-shadow:0 2px 6px rgba(0,0,0,0.2);font-size:13px;color:#333;white-space:nowrap;">${dong.dong_name} (평균 ${priceStr})</div>`,
            removable: false
        });

        overlay.setMap(map);
        overlays.push(overlay);

        // 이벤트 리스너 추가
        setTimeout(() => {
            const element = document.querySelector(`[data-dong-code="${dong.dong_code}"]`) as HTMLElement;
            if (element) {
                element.addEventListener('mouseenter', () => {
                    infoWindow.open(map, {
                        getPosition: () => position
                    } as any);
                });

                element.addEventListener('mouseleave', () => {
                    infoWindow.close();
                });

                if (onMarkerClick) {
                    element.addEventListener('click', () => {
                        infoWindow.close();
                        onMarkerClick(dong);
                    });
                }
            }
        }, 100);
    });
}

/**
 * 지도 레벨에 따라 적절한 마커를 표시하는 통합 함수
 */
export function showMarkersByLevel(
    map: kakao.maps.Map,
    overlays: kakao.maps.CustomOverlay[],
    level: number,
    apartments?: Apartment[],
    sidoData?: SidoMarker[],
    gugunData?: GugunMarker[],
    dongData?: DongMarker[],
    onApartmentClick?: (apartment: Apartment) => void,
    onSidoClick?: (sido: SidoMarker) => void,
    onGugunClick?: (gugun: GugunMarker) => void,
    onDongClick?: (dong: DongMarker) => void
) {
    if (level >= 10) {
        if (sidoData) {
            showSidoMarkers(map, overlays, sidoData, onSidoClick);
        }
    } else if (level >= 8 && level <= 9) {
        if (gugunData) {
            showGugunMarkers(map, overlays, gugunData, onGugunClick);
        }
    } else if (level >= 5 && level <= 7) {
        if (dongData) {
            showDongMarkers(map, overlays, dongData, onDongClick);
        }
    } else if (level >= 1 && level <= 4) {
        if (apartments) {
            showApartmentMarkers(map, overlays, apartments, onApartmentClick);
        }
    }
}

// 기존 formatPrice 함수 유지
function formatPrice(price: any) {
    if (price == null) return '정보없음';
    const num = Number(String(price).replace(/[^0-9.]/g, ''));
    if (isNaN(num) || num === 0) return '정보없음';
    if (num >= 10000) {
        return (Math.round((num / 10000) * 100) / 100).toFixed(1) + '억';
    } else {
        return (Math.round((num / 1000) * 100) / 100).toFixed(1) + '천';
    }
}
