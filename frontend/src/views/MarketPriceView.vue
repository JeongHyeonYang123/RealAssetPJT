<template>
    <div class="market-price-container">
        <div class="header">
            <h1>부동산 시세 분석</h1>
            <p class="subtitle">
                {{ formatDateTitle(targetMonth) }} 데이터 (현재 기준 1개월 전)
            </p>
        </div>

        <div class="content">
            <!-- 차트 섹션 (2열 그리드) - 순서 변경 -->
            <div class="chart-grid">
                <!-- 월별 시세 차트 (왼쪽) -->
                <div class="chart-card">
                    <h3>
                        지난 {{ monthlyDataYears }}년간 월별 전국 아파트 시세
                    </h3>
                    <div class="chart-container">
                        <Line
                            v-if="
                                monthlyDataLoaded &&
                                monthlyChartData.labels.length > 0
                            "
                            :data="monthlyChartData"
                            :options="lineChartOptions"
                        />
                        <div v-else class="loading-spinner">
                            월별 시세 데이터 로딩 중...
                        </div>
                    </div>
                </div>

                <!-- 지역별 가격 차트 (오른쪽) -->
                <div class="chart-card">
                    <h3>
                        지역별 아파트 평균 매매가격 ({{
                            formatDateTitle(targetMonth)
                        }})
                    </h3>
                    <div class="chart-container">
                        <Bar
                            v-if="
                                regionDataLoaded &&
                                regionChartData.labels.length > 0
                            "
                            :data="regionChartData"
                            :options="barChartOptions"
                        />
                        <div v-else class="loading-spinner">
                            {{ formatDateTitle(targetMonth) }} 데이터 로딩 중...
                        </div>
                    </div>
                </div>
            </div>

            <!-- 관심지역 섹션 -->
            <div class="interest-section">
                <div class="chart-card">
                    <h3>나의 관심지역 평균가격</h3>
                    <div class="interest-area-container">
                        <div
                            v-if="interestAreasLoading"
                            class="loading-spinner-small"
                        >
                            로딩 중...
                        </div>
                        <div
                            v-else-if="interestAreas.length === 0"
                            class="no-data"
                        >
                            <p>등록된 관심지역이 없습니다.</p>
                            <button
                                @click="refreshInterestAreas"
                                class="refresh-btn"
                            >
                                새로고침
                            </button>
                        </div>
                        <div v-else class="interest-areas-table">
                            <table>
                                <thead>
                                    <tr>
                                        <th>지역명</th>
                                        <th>평균가격</th>
                                        <th>등록일</th>
                                        <th>상태</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr
                                        v-for="area in interestAreas"
                                        :key="area.id"
                                    >
                                        <td>
                                            <div class="region-info">
                                                <span class="region-name">{{
                                                    area.fullRegionName ||
                                                    "조회중"
                                                }}</span>
                                                <span
                                                    v-if="area.regionDetails"
                                                    class="region-details"
                                                    >{{
                                                        area.regionDetails
                                                    }}</span
                                                >
                                            </div>
                                        </td>
                                        <td>
                                            <span
                                                v-if="area.averagePrice > 0"
                                                class="price-value"
                                            >
                                                {{
                                                    formatPrice(
                                                        area.averagePrice
                                                    )
                                                }}
                                            </span>
                                            <span v-else class="no-price"
                                                >가격 정보 없음</span
                                            >
                                        </td>

                                        <td>
                                            {{ formatDate(area.createdAt) }}
                                        </td>
                                        <td>
                                            <span
                                                :class="
                                                    getStatusClass(area.status)
                                                "
                                                class="status-badge"
                                            >
                                                {{ area.status }}
                                            </span>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import { memberAi, memberAiNoAuth } from "@/axios";
import { Bar, Line } from "vue-chartjs";
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    BarElement,
    LineElement,
    PointElement,
    Title,
    Tooltip,
    Legend,
} from "chart.js";
import { useRoute } from "vue-router";

ChartJS.register(
    CategoryScale,
    LinearScale,
    BarElement,
    LineElement,
    PointElement,
    Title,
    Tooltip,
    Legend
);

// API 설정
const API_KEY = "9dc3f1dbd10049d6ad2f780e30968fb3";
const API_URL = "https://www.reb.or.kr/r-one/openapi/SttsApiTblData.do";

// 지역 코드 매핑
const REGION_MAPPING = {
    500001: "전국",
    500002: "수도권",
    500003: "지방권",
    520010: "서울",
};

// 월별 시세 조회 기간 설정
const monthlyDataYears = 3; // 3년간 데이터

// 1개월 전 날짜 계산 함수
function getOneMonthAgo(): string {
    const currentDate = new Date();
    currentDate.setMonth(currentDate.getMonth() - 1);
    const year = currentDate.getFullYear();
    const month = String(currentDate.getMonth() + 1).padStart(2, "0");
    return `${year}${month}`;
}

// 날짜 포맷팅 함수
function formatDateTitle(dateStr: string): string {
    const year = dateStr.substring(0, 4);
    const month = dateStr.substring(4, 6);
    return `${year}년 ${parseInt(month)}월`;
}

// n년 전 날짜 계산 함수
function getNYearsAgo(years: number): string {
    const currentDate = new Date();
    currentDate.setFullYear(currentDate.getFullYear() - years);
    const year = currentDate.getFullYear();
    const month = String(currentDate.getMonth() + 1).padStart(2, "0");
    return `${year}${month}`;
}

// 계산된 값들
const targetMonth = getOneMonthAgo();

// 반응형 데이터
const regionDataLoaded = ref(false);
const monthlyDataLoaded = ref(false);
const interestAreasLoading = ref(false);
const interestAreas = ref([]);
const realEstatePriceData = ref([]);

// 차트 데이터
const regionChartData = ref({
    labels: ["전국", "수도권", "지방권", "서울"],
    datasets: [
        {
            label: "평균 매매가격 (천원)",
            data: [],
            backgroundColor: [
                "rgba(54, 162, 235, 0.8)",
                "rgba(255, 99, 132, 0.8)",
                "rgba(75, 192, 192, 0.8)",
                "rgba(255, 206, 86, 0.8)",
            ],
            borderColor: [
                "rgba(54, 162, 235, 1)",
                "rgba(255, 99, 132, 1)",
                "rgba(75, 192, 192, 1)",
                "rgba(255, 206, 86, 1)",
            ],
            borderWidth: 2,
        },
    ],
});

// 월별 시세 차트 데이터
const monthlyChartData = ref({
    labels: [],
    datasets: [
        {
            label: "전국 아파트 매매가격 (천원)",
            data: [],
            borderColor: "rgba(75, 192, 192, 1)",
            backgroundColor: "rgba(75, 192, 192, 0.1)",
            borderWidth: 2,
            tension: 0.3,
            fill: true,
        },
    ],
});

// 차트 옵션들
const barChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
        legend: { display: false },
        title: {
            display: true,

            font: { size: 16 },
        },
        tooltip: {
            callbacks: {
                label: function (context) {
                    const value = context.parsed.y;
                    return `${context.label}: ${formatPrice(value)}`;
                },
            },
        },
    },
    scales: {
        y: {
            beginAtZero: true,
            title: {
                display: true,
                text: "가격 (억원)",
                font: { size: 12 },
            },
            ticks: {
                callback: function (value) {
                    return (value / 100000).toFixed(1) + "억";
                },
            },
        },
        x: {
            title: {
                display: true,
                text: "지역",
                font: { size: 12 },
            },
        },
    },
};

const lineChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
        legend: { display: false },
        title: {
            display: true,

            font: { size: 16 },
        },
        tooltip: {
            callbacks: {
                label: function (context) {
                    const value = context.parsed.y;
                    return `${context.label}: ${formatPrice(value)}`;
                },
            },
        },
    },
    scales: {
        y: {
            beginAtZero: false,
            title: {
                display: true,
                text: "가격 (억원)",
                font: { size: 12 },
            },
            ticks: {
                callback: function (value) {
                    return (value / 100000).toFixed(1) + "억";
                },
            },
        },
        x: {
            title: {
                display: true,
                text: "기간",
                font: { size: 12 },
            },
        },
    },
}; // 동시성 제한 병렬 실행 유틸리티
async function runWithConcurrencyLimit(tasks, limit) {
    const results = [];
    const executing = [];
    for (const task of tasks) {
        const p = task().then((result) => results.push(result));
        executing.push(p);
        if (executing.length >= limit) {
            await Promise.race(executing);
            // 완료된 Promise 제거
            executing.splice(
                executing.findIndex((e) => e === p),
                1
            );
        }
    }
    await Promise.all(executing);
    return results;
}

// 월별 시세 데이터 조회 (병렬 호출 + 동시성 제한)
async function fetchMonthlyPriceData() {
    try {
        console.log("=== 월별 시세 데이터 조회 시작 ===");
        monthlyDataLoaded.value = false;

        const monthlyData = [];
        const currentDate = new Date();
        const fetchTasks = [];
        const totalMonths = monthlyDataYears * 12;

        // 요청 생성
        for (let i = totalMonths; i >= 1; i--) {
            const targetDate = new Date(currentDate);
            targetDate.setMonth(targetDate.getMonth() - i);

            const year = targetDate.getFullYear();
            const month = String(targetDate.getMonth() + 1).padStart(2, "0");
            const yearMonth = `${year}${month}`;

            // 각 월별 요청을 함수로 래핑
            fetchTasks.push(async () => {
                try {
                    console.log(
                        `${yearMonth} 데이터 조회 중... (${
                            totalMonths - i + 1
                        }/${totalMonths})`
                    );

                    const response = await memberAiNoAuth.get(API_URL, {
                        params: {
                            KEY: API_KEY,
                            STATBL_ID: "A_2024_00060",
                            DTACYCLE_CD: "MM",
                            WRTTIME_IDTFR_ID: yearMonth,
                            Type: "json",
                        },
                        timeout: 5000, // 타임아웃 단축
                    });

                    if (response.status === 200) {
                        const apiData = response.data.SttsApiTblData;
                        let rows = [];

                        if (Array.isArray(apiData) && apiData.length > 1) {
                            rows = apiData[1].row;
                        } else if (apiData.row) {
                            rows = Array.isArray(apiData.row)
                                ? apiData.row
                                : [apiData.row];
                        }

                        // 전국 데이터 찾기 (CLS_ID: 500001)
                        const nationalData = rows.find(
                            (row) => row.CLS_ID === "500001"
                        );
                        if (nationalData) {
                            monthlyData.push({
                                yearMonth: yearMonth,
                                label: `${year}.${month}`,
                                price: parseFloat(nationalData.DTA_VAL) || 0,
                            });
                            console.log(
                                `${yearMonth}: ${nationalData.DTA_VAL}천원`
                            );
                        }
                    }
                } catch (error) {
                    console.warn(`${yearMonth} 데이터 조회 실패:`, error);
                }
            });
        }

        // 병렬 실행 (동시성 제한: 5)
        await runWithConcurrencyLimit(fetchTasks, 5);

        // 월별 데이터 정렬 (혹시 순서가 섞일 수 있으니)
        monthlyData.sort((a, b) => a.yearMonth.localeCompare(b.yearMonth));

        // 차트 데이터 설정
        if (monthlyData.length > 0) {
            monthlyChartData.value.labels = monthlyData.map(
                (item) => item.label
            );
            monthlyChartData.value.datasets[0].data = monthlyData.map(
                (item) => item.price
            );

            console.log(
                `월별 시세 차트 데이터 설정 완료: ${
                    monthlyData.length
                }개월 (성공률: ${Math.round(
                    (monthlyData.length / totalMonths) * 100
                )}%)`
            );
        } else {
            console.warn(
                "월별 시세 데이터가 없습니다. 더미 데이터를 생성합니다."
            );

            // 더미 데이터 생성
            const dummyLabels = [];
            const dummyData = [];
            const basePrice = 450000;

            for (let i = totalMonths; i >= 1; i--) {
                const targetDate = new Date(currentDate);
                targetDate.setMonth(targetDate.getMonth() - i);
                const year = targetDate.getFullYear();
                const month = String(targetDate.getMonth() + 1).padStart(
                    2,
                    "0"
                );

                dummyLabels.push(`${year}.${month}`);
                // 추세가 있는 더미 데이터 생성
                const trend = (totalMonths - i) * 1000; // 상승 추세
                const variation = (Math.random() - 0.5) * 20000; // 변동성
                dummyData.push(basePrice + trend + variation);
            }

            monthlyChartData.value.labels = dummyLabels;
            monthlyChartData.value.datasets[0].data = dummyData;

            console.log("더미 월별 시세 데이터 생성 완료");
        }
    } catch (error) {
        console.error("월별 시세 데이터 조회 실패:", error);
    } finally {
        monthlyDataLoaded.value = true;
    }
}

// 기존 함수들 (모두 동일)
async function fetchInterestAreas() {
    try {
        console.log("=== 관심지역 조회 시작 ===");
        interestAreasLoading.value = true;

        const response = await memberAi.get("/api/v1/interest-areas/my");

        console.log("관심지역 API 응답:", response.data);

        if (response.data.success && response.data.data) {
            const areas = response.data.data;

            console.log(`총 ${areas.length}개의 관심지역 처리 시작`);

            const areaPromises = areas.map(async (area, index) => {
                try {
                    console.log(
                        `[${index + 1}/${areas.length}] 동코드 ${
                            area.dongCode
                        } 처리 중...`
                    );

                    const regionInfo = await fetchRegionInfo(area.dongCode);

                    console.log(`[${index + 1}/${areas.length}] 지역 정보:`, {
                        dongCode: area.dongCode,
                        fullRegionName: regionInfo.fullRegionName,
                        regionDetails: regionInfo.regionDetails,
                    });

                    const priceInfo = await fetchAreaPriceFromRealEstateData(
                        regionInfo.gugunName,
                        regionInfo.dongName,
                        area.dongCode
                    );

                    console.log(`[${index + 1}/${areas.length}] 가격 정보:`, {
                        dongCode: area.dongCode,
                        averagePrice: priceInfo.averagePrice,
                        dataSource: priceInfo.dataSource,
                    });

                    return {
                        ...area,
                        ...regionInfo,
                        ...priceInfo,
                        status: "조회 완료",
                    };
                } catch (error) {
                    console.warn(
                        `[${index + 1}/${areas.length}] ${
                            area.dongCode
                        } 정보 조회 실패:`,
                        error
                    );
                    return {
                        ...area,
                        fullRegionName: "조회 실패",
                        regionDetails: `동코드: ${area.dongCode} (오류: ${error.message})`,
                        averagePrice: 0,
                        monthlyChange: 0,
                        status: "조회 실패",
                    };
                }
            });

            interestAreas.value = await Promise.all(areaPromises);

            console.log("=== 관심지역 데이터 처리 완료 ===");
            console.log(
                "처리 결과 요약:",
                interestAreas.value.map((area) => ({
                    dongCode: area.dongCode,
                    fullRegionName: area.fullRegionName,
                    averagePrice: area.averagePrice,
                    dataSource: area.dataSource,
                    status: area.status,
                }))
            );
        }
    } catch (error) {
        console.error("관심지역 조회 실패:", error);

        if (error.response?.status === 401) {
            console.log("인증 오류 - 인터셉터가 처리 중...");
        } else if (error.response?.status === 403) {
            console.log("권한 없음 - 관심지역 조회 권한이 없습니다.");
        }

        interestAreas.value = [];
    } finally {
        interestAreasLoading.value = false;
    }
}

async function fetchRegionInfo(dongCode: string) {
    try {
        console.log(`=== 지역 정보 조회 시작: ${dongCode} ===`);

        if (!dongCode || dongCode.length !== 10) {
            console.warn(`유효하지 않은 동코드: ${dongCode}`);
            throw new Error("유효하지 않은 동코드");
        }

        const response = await memberAi.get("/api/v1/adm/dongcode", {
            params: {
                dongCode: dongCode,
            },
        });

        console.log(`${dongCode} 지역 정보 API 응답:`, response.data);

        if (response.data.success && response.data.data) {
            const regionData = response.data.data;

            const sidoName = regionData.sido || "";
            const gugunName = regionData.gugun || "";
            const dongName = regionData.dong || "";

            let fullRegionName = "";
            let regionDetails = "";

            if (gugunName && dongName) {
                fullRegionName = `${gugunName} ${dongName}`;
            } else if (dongName) {
                fullRegionName = dongName;
            } else if (gugunName) {
                fullRegionName = gugunName;
            } else {
                fullRegionName = generateRegionNameFromCode(dongCode);
            }

            const addressParts = [sidoName, gugunName, dongName].filter(
                (name) => name && name.trim()
            );
            if (addressParts.length > 0) {
                regionDetails = addressParts.join(" ");
            } else {
                regionDetails = `동코드: ${dongCode}`;
            }

            const result = {
                fullRegionName: fullRegionName,
                regionDetails: regionDetails,
                sidoName: sidoName,
                gugunName: gugunName,
                dongName: dongName,
                originalData: regionData,
            };

            console.log(`${dongCode} 지역 정보 파싱 결과:`, result);
            return result;
        } else {
            console.warn(`${dongCode} API 응답에 데이터 없음:`, response.data);
            throw new Error("지역 정보 조회 실패 - 응답 데이터 없음");
        }
    } catch (error) {
        console.error(`지역 정보 조회 실패 (${dongCode}):`, error);

        const fallbackRegionName = generateRegionNameFromCode(dongCode);

        return {
            fullRegionName: fallbackRegionName,
            regionDetails: `동코드: ${dongCode} (조회 실패: ${error.message})`,
            sidoName: "",
            gugunName: "",
            dongName: "",
            error: error.message,
        };
    }
}

function generateRegionNameFromCode(dongCode: string): string {
    if (!dongCode || dongCode.length !== 10) {
        return "알 수 없는 지역";
    }

    const sidoCode = dongCode.substring(0, 2);
    const sggCode = dongCode.substring(2, 5);

    const sidoMap = {
        "11": "서울특별시",
        "26": "부산광역시",
        "27": "대구광역시",
        "28": "인천광역시",
        "29": "광주광역시",
        "30": "대전광역시",
        "31": "울산광역시",
        "36": "세종특별자치시",
        "41": "경기도",
        "43": "충청북도",
        "44": "충청남도",
        "52": "전북특별자치도",
        "45": "전라북도",
        "46": "전라남도",
        "47": "경상북도",
        "48": "경상남도",
        "50": "제주특별자치도",
    };

    const sidoName = sidoMap[sidoCode] || `시도코드${sidoCode}`;

    if (sidoCode === "11") {
        const guMap = {
            "110": "종로구",
            "140": "중구",
            "170": "용산구",
            "200": "성동구",
            "215": "광진구",
            "230": "동대문구",
            "260": "중랑구",
            "290": "성북구",
            "305": "강북구",
            "320": "도봉구",
            "350": "노원구",
            "380": "은평구",
            "410": "서대문구",
            "440": "마포구",
            "470": "양천구",
            "500": "강서구",
            "530": "구로구",
            "545": "금천구",
            "560": "영등포구",
            "590": "동작구",
            "620": "관악구",
            "650": "서초구",
            "680": "강남구",
            "710": "송파구",
            "740": "강동구",
        };

        const guName = guMap[sggCode];
        return guName ? guName : `${sidoName} ${sggCode}구`;
    }

    return `${sidoName} 지역`;
}

async function fetchAreaPriceFromRealEstateData(
    gugunName: string,
    dongName: string,
    dongCode: string
) {
    try {
        console.log(
            `시세 정보 조회: ${gugunName} ${dongName} (동코드: ${dongCode})`
        );

        if (dongCode && dongCode.length === 10) {
            try {
                console.log(`dong_code_superman API 호출: ${dongCode}`);

                const response = await memberAi.get("/api/v1/house/avg-price", {
                    params: {
                        dongCode: dongCode,
                    },
                });

                console.log(
                    `${dongCode} dong_code_superman API 응답:`,
                    response.data
                );

                if (response.data.success && response.data.data) {
                    const priceData = response.data.data;

                    return {
                        averagePrice: priceData.avg_price || 0,
                        monthlyChange: calculateMonthlyChange(
                            priceData.avg_price
                        ),
                        dealCount: priceData.apt_count || 0,
                        lat: priceData.lat || null,
                        lng: priceData.lng || null,
                        updatedAt: priceData.updated_at || null,
                        dataSource: "dong_code_superman",
                    };
                }
            } catch (dongCodeApiError) {
                console.warn(
                    `dong_code_superman API 실패 (${dongCode}):`,
                    dongCodeApiError
                );
            }
        }

        const regionPrice = realEstatePriceData.value.find((item) => {
            const itemRegion = item.regionName || "";
            return (
                itemRegion.includes(gugunName) || itemRegion.includes(dongName)
            );
        });

        if (regionPrice) {
            console.log(
                `전체 시세 데이터에서 ${gugunName} ${dongName} 가격 정보 찾음:`,
                regionPrice
            );
            return {
                averagePrice: regionPrice.price || 0,
                monthlyChange: regionPrice.monthlyChange || 0,
                dealCount: regionPrice.dealCount || 0,
                dataSource: "korea_real_estate_board",
            };
        }

        console.log(`기타 API로 ${gugunName} ${dongName} 조회 시도`);

        try {
            const response = await memberAi.get(
                "/api/v1/real-estate/average-price",
                {
                    params: {
                        gugunName: gugunName,
                        dongName: dongName,
                        yearMonth: targetMonth,
                        propertyType: "APT",
                    },
                }
            );

            if (response.data.success && response.data.data) {
                const priceData = response.data.data;
                return {
                    averagePrice: priceData.averagePrice || 0,
                    monthlyChange: priceData.monthlyChange || 0,
                    dealCount: priceData.dealCount || 0,
                    dataSource: "real_estate_api",
                };
            }
        } catch (apiError) {
            console.warn("기타 부동산 API 실패:", apiError);
        }

        console.warn(`모든 API 실패 - ${gugunName} ${dongName} (${dongCode})`);
        return {
            averagePrice: 0,
            monthlyChange: 0,
            dealCount: 0,
            dataSource: "no_data",
        };
    } catch (error) {
        console.error(`가격 정보 조회 실패 (${gugunName} ${dongName}):`, error);

        return {
            averagePrice: 0,
            monthlyChange: 0,
            dealCount: 0,
            dataSource: "error",
        };
    }
}

function calculateMonthlyChange(currentPrice: number): number {
    if (!currentPrice || currentPrice === 0) return 0;

    if (currentPrice > 100000) {
        return Math.random() * 4 - 2;
    } else if (currentPrice > 50000) {
        return Math.random() * 6 - 3;
    } else {
        return Math.random() * 8 - 4;
    }
}

async function fetchAndStoreRealEstateData() {
    try {
        console.log("=== 전체 시세 데이터 조회 시작 ===");

        const response = await memberAiNoAuth.get(API_URL, {
            params: {
                KEY: API_KEY,
                STATBL_ID: "A_2024_00060",
                DTACYCLE_CD: "MM",
                WRTTIME_IDTFR_ID: targetMonth,
                Type: "json",
            },
        });

        if (response.status === 200) {
            const apiData = response.data.SttsApiTblData;
            let rows = [];

            if (Array.isArray(apiData) && apiData.length > 1) {
                rows = apiData[1].row;
            } else if (apiData.row) {
                rows = Array.isArray(apiData.row) ? apiData.row : [apiData.row];
            }

            realEstatePriceData.value = rows.map((row) => ({
                regionId: row.CLS_ID,
                regionName: row.CLS_NM,
                price: parseFloat(row.DTA_VAL) || 0,
                monthlyChange: Math.random() * 10 - 5,
                dealCount: Math.floor(Math.random() * 50) + 10,
            }));

            console.log(
                "전체 시세 데이터 저장 완료:",
                realEstatePriceData.value.length,
                "건"
            );

            const targetRegions = Object.keys(REGION_MAPPING).map(Number);
            const regionPrices = {};

            rows.forEach((row) => {
                const clsId = parseInt(row.CLS_ID);
                const price = parseFloat(row.DTA_VAL);

                if (targetRegions.includes(clsId) && !isNaN(price)) {
                    const regionName = REGION_MAPPING[clsId];
                    regionPrices[regionName] = price;
                }
            });

            const orderedPrices = [
                regionPrices["전국"] || 0,
                regionPrices["수도권"] || 0,
                regionPrices["지방권"] || 0,
                regionPrices["서울"] || 0,
            ];

            regionChartData.value.datasets[0].data = orderedPrices;
            console.log("차트 데이터 업데이트 완료:", orderedPrices);
        }
    } catch (error) {
        console.error("전체 시세 데이터 조회 실패:", error);

        const dummyData = [450000, 680000, 250000, 1300000];
        regionChartData.value.datasets[0].data = dummyData;

        realEstatePriceData.value = [
            {
                regionId: "500001",
                regionName: "전국",
                price: 450000,
                monthlyChange: 2.1,
                dealCount: 1500,
            },
            {
                regionId: "500002",
                regionName: "수도권",
                price: 680000,
                monthlyChange: 1.8,
                dealCount: 800,
            },
            {
                regionId: "500003",
                regionName: "지방권",
                price: 250000,
                monthlyChange: -0.5,
                dealCount: 700,
            },
            {
                regionId: "520010",
                regionName: "서울",
                price: 1300000,
                monthlyChange: 3.2,
                dealCount: 400,
            },
        ];
    } finally {
        regionDataLoaded.value = true;
    }
}

function refreshInterestAreas() {
    fetchInterestAreas();
}

function formatPrice(price: number): string {
    if (!price || price === 0) {
        return "가격 정보 없음";
    }

    if (price >= 100000) {
        return `${(price / 100000).toFixed(1)}억원`;
    } else if (price >= 10000) {
        return `${(price / 10000).toFixed(1)}억원`;
    } else if (price >= 1000) {
        return `${(price / 1000).toFixed(1)}천원`;
    }
    return `${Math.round(price)}원`;
}

function formatPriceChange(change: number): string {
    if (change === 0) return "0%";
    return `${change > 0 ? "+" : ""}${change.toFixed(1)}%`;
}

function getPriceChangeClass(change: number): string {
    if (change > 0) return "price-up";
    if (change < 0) return "price-down";
    return "price-neutral";
}

function getStatusClass(status: string): string {
    switch (status) {
        case "조회 완료":
            return "status-success";
        case "조회 실패":
            return "status-error";
        case "조회중":
            return "status-loading";
        default:
            return "status-default";
    }
}

function formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString("ko-KR", {
        year: "numeric",
        month: "short",
        day: "numeric",
    });
}

// 라이프사이클
onMounted(async () => {
    console.log("=== 컴포넌트 마운트 ===");
    console.log("axios 인터셉터를 통한 자동 토큰 관리 활성화");

    // 병렬로 데이터 로딩
    await Promise.all([
        fetchAndStoreRealEstateData(),
        fetchMonthlyPriceData(), // 빠른 API 호출로 더 빠르게 로딩
        fetchInterestAreas(),
    ]);

    console.log("모든 데이터 로딩 완료");
});

const route = useRoute();

// 관심지역 데이터 자동 갱신을 위한 watch 설정
watch(
    () => route.fullPath,
    () => {
        console.log("�� 관심지역 데이터 자동 갱신");
        fetchInterestAreas();
    }
);
</script>

<style lang="scss" scoped>
.market-price-container {
    padding: var(--space-4);
    height: calc(100vh - 60px);
    overflow-y: auto;

    /* 가운데 정렬 및 최대 너비 설정 */
    max-width: 1400px;
    margin: 0 auto;
    width: 100%;
}

.header {
    margin-bottom: var(--space-4);

    h1 {
        font-size: 1.75rem;
        font-weight: 700;
        color: var(--color-gray-900);
        margin-bottom: 4px;
    }

    .subtitle {
        font-size: 1rem;
        color: var(--color-gray-600);
        font-weight: 500;
    }
}

.content {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
}

/* 차트 그리드 (2열) */
.chart-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--space-4);
}

/* 관심지역 섹션 (전체 너비) */
.interest-section {
    width: 100%;
}

.chart-card {
    background-color: var(--color-white);
    border-radius: var(--radius-lg);
    padding: var(--space-4);
    box-shadow: var(--shadow-md);

    h3 {
        font-size: 1.25rem;
        font-weight: 600;
        color: var(--color-gray-800);
        margin-bottom: var(--space-3);
    }
}

.chart-container {
    height: 400px;
    width: 100%;
}

.loading-spinner {
    height: 400px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: var(--color-gray-50);
    border-radius: var(--radius-md);
    color: var(--color-gray-500);
    font-weight: 500;
}

.interest-area-container {
    height: auto;
    min-height: 300px;
    max-height: 500px;
    overflow-y: auto;
}

.loading-spinner-small {
    height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--color-gray-500);
}

.no-data {
    height: 200px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--color-gray-500);

    p {
        margin-bottom: 16px;
    }

    .refresh-btn {
        padding: 8px 16px;
        background-color: var(--color-primary);
        color: white;
        border: none;
        border-radius: var(--radius-md);
        cursor: pointer;
        font-size: 14px;

        &:hover {
            background-color: var(--color-primary-dark);
        }
    }
}

.interest-areas-table {
    font-size: 14px;

    table {
        width: 100%;
        border-collapse: collapse;

        th,
        td {
            padding: 12px 8px;
            text-align: left;
            border-bottom: 1px solid var(--color-gray-200);
        }

        th {
            font-weight: 600;
            background-color: var(--color-gray-50);
            color: var(--color-gray-700);
            font-size: 12px;
        }

        td {
            color: var(--color-gray-800);
            font-size: 13px;
        }

        .price-up {
            color: #e74c3c;
            font-weight: 600;
        }

        .price-down {
            color: #27ae60;
            font-weight: 600;
        }

        .price-neutral {
            color: var(--color-gray-600);
        }
    }
}

.region-info {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .region-name {
        font-weight: 600;
        color: var(--color-gray-800);
        font-size: 14px;
    }

    .region-details {
        font-size: 11px;
        color: var(--color-gray-500);
        line-height: 1.3;
    }
}

.price-value {
    font-weight: 600;
    color: var(--color-gray-800);
}

.no-price {
    color: var(--color-gray-400);
    font-style: italic;
}

.status-badge {
    padding: 4px 8px;
    border-radius: 12px;
    font-size: 11px;
    font-weight: 500;

    &.status-success {
        background-color: #d4edda;
        color: #155724;
    }

    &.status-error {
        background-color: #f8d7da;
        color: #721c24;
    }

    &.status-loading {
        background-color: #fff3cd;
        color: #856404;
    }

    &.status-default {
        background-color: var(--color-gray-100);
        color: var(--color-gray-600);
    }
}

/* 반응형 디자인 */
@media (max-width: 1440px) {
    .market-price-container {
        max-width: 100%;
        margin: 0;
        padding: var(--space-3);
    }
}

@media (max-width: 1024px) {
    .chart-grid {
        grid-template-columns: 1fr;
    }

    .chart-container {
        height: 300px;
    }

    .interest-area-container {
        height: auto;
        max-height: 400px;
    }
}

@media (max-width: 768px) {
    .market-price-container {
        padding: var(--space-2);
    }

    .chart-container {
        height: 250px;
    }

    .interest-areas-table {
        font-size: 12px;

        table {
            th,
            td {
                padding: 8px 4px;
            }
        }
    }
}
</style>
