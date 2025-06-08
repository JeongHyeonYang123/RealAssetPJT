<template>
	<div class="deal-analysis-container">
		<!-- 상단 가격 정보 -->
		<div class="price-header">
			<h3 class="price-title">{{ getPriceTitle() }}</h3>
			<div class="main-price">{{ averagePrice }}</div>
			<div v-if="validPriceData.length > 0" class="total-deals">
				총 {{ validPriceData.length }}건의 거래내역
			</div>
		</div>

		<!-- 탭 메뉴 -->
		<div class="tab-menu">
			<button
				v-for="period in periods"
				:key="period.key"
				:class="['tab-btn', { 'active': selectedPeriod === period.key }]"
				@click="selectedPeriod = period.key"
			>
				{{ period.label }}
			</button>
		</div>

		<!-- 로딩/에러 상태 -->
		<div v-if="loading" class="chart-loading">
			<div class="loading-spinner"></div>
			<p>거래 데이터를 불러오는 중...</p>
		</div>

		<div v-else-if="error" class="chart-error">
			<p>{{ error }}</p>
		</div>

		<!-- ✅ Chart.js 기반 라인차트 -->
		<div v-else-if="hasValidData" class="chart-section">
			<!-- 최근 거래 정보 레이블 -->
			<div v-if="latestDealInfo" class="latest-deal-label">
				{{ latestDealInfo.date }} 최신 거래: {{ latestDealInfo.price }} ({{ latestDealInfo.count }}건)
			</div>

			<!-- ✅ 추세 메시지 -->
			<div v-if="trendMessage" class="trend-message">
				{{ trendMessage }}
			</div>

			<!-- Chart.js 차트들 -->
			<div class="charts-container">
				<!-- 가격 추이 라인차트 + 추세선 -->
				<div class="price-chart-wrapper">
					<h4>💰 가격 추이</h4>
					<div class="chart-container">
						<Line
							:data="priceChartDataWithTrend"
							:options="priceChartOptions"
							:key="chartKey"
						/>
					</div>
				</div>

				<!-- 거래량 라인차트 -->
				<div class="volume-chart-wrapper">
					<h4>📊 거래량 추이</h4>
					<div class="chart-container">
						<Line
							:data="volumeChartData"
							:options="volumeChartOptions"
							:key="chartKey + 1"
						/>
					</div>
				</div>
			</div>

			<!-- 통계 요약 카드 -->
			<div class="summary-cards">
				<div class="summary-card">
					<div class="card-icon">📈</div>
					<div class="card-content">
						<div class="card-label">최고가</div>
						<div class="card-value">{{ maxPriceFormatted }}</div>
					</div>
				</div>
				<div class="summary-card">
					<div class="card-icon">📉</div>
					<div class="card-content">
						<div class="card-label">최저가</div>
						<div class="card-value">{{ minPriceFormatted }}</div>
					</div>
				</div>
			</div>
		</div>

		<div v-else class="chart-empty">
			<div class="empty-icon">📊</div>
			<p>거래 데이터가 없습니다.</p>
			<p>아파트를 선택해주세요.</p>
		</div>

		<!-- 거래 내역 테이블 -->
		<div v-if="dealData.length > 0" class="deal-history-table">
			<div class="table-header">
				<h4>📋 최근 거래내역</h4>
			</div>
			<div class="table-content">
				<div class="table-row table-header-row">
					<span class="col-date">계약일</span>
					<span class="col-area">면적/층수</span>
					<span class="col-price">거래가격</span>
				</div>
				<div class="table-body">
					<div
						v-for="deal in recentDeals"
						:key="deal.no || Math.random()"
						class="table-row"
					>
						<span class="col-date">{{ formatTableDate(deal) }}</span>
						<span class="col-area">
              {{ deal.excluUseAr ? Math.round(deal.excluUseAr / 3.30579) + '평' : '-' }}
              <small v-if="deal.floor" class="floor-info">{{ deal.floor }}층</small>
            </span>
						<span class="col-price price-highlight">{{ formatDealAmount(deal.dealAmount) }}</span>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { Line } from 'vue-chartjs';
import {
	Chart as ChartJS,
	CategoryScale,
	LinearScale,
	PointElement,
	LineElement,
	Title,
	Tooltip,
	Legend,
	Filler
} from 'chart.js';
import type { Deal } from '@/types/Deal';

// Chart.js 컴포넌트 등록
ChartJS.register(
	CategoryScale,
	LinearScale,
	PointElement,
	LineElement,
	Title,
	Tooltip,
	Legend,
	Filler
);

// Props
const props = defineProps<{
	aptSeq: string | null;
}>();

// 반응형 데이터
const loading = ref(false);
const error = ref<string | null>(null);
const dealData = ref<Deal[]>([]);
const selectedPeriod = ref('all');
const chartKey = ref(0);

// 기간 옵션
const periods = [
	{ key: 'all', label: '전체 기간' },
	{ key: '3years', label: '최근 3년' },
	{ key: '1year', label: '최근 1년' }
];

// 전체 거래내역 반영
const validPriceData = computed(() => {
	const filtered = dealData.value.filter(deal =>
		deal.dealAmount &&
		deal.dealAmount !== 'null' &&
		deal.dealAmount.trim() !== '' &&
		deal.dealYear &&
		deal.dealMonth &&
		deal.dealDay
	);

	if (selectedPeriod.value === '3years') {
		const threeYearsAgo = new Date();
		threeYearsAgo.setFullYear(threeYearsAgo.getFullYear() - 3);
		return filtered.filter(deal => {
			const dealDate = new Date(deal.dealYear, deal.dealMonth - 1, deal.dealDay);
			return dealDate >= threeYearsAgo;
		});
	} else if (selectedPeriod.value === '1year') {
		const oneYearAgo = new Date();
		oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
		return filtered.filter(deal => {
			const dealDate = new Date(deal.dealYear, deal.dealMonth - 1, deal.dealDay);
			return dealDate >= oneYearAgo;
		});
	}

	return filtered;
});

const hasValidData = computed(() => validPriceData.value.length > 0);

const recentDeals = computed(() => {
	return validPriceData.value
		.sort((a, b) => {
			const dateA = new Date(a.dealYear, a.dealMonth - 1, a.dealDay);
			const dateB = new Date(b.dealYear, b.dealMonth - 1, b.dealDay);
			return dateB.getTime() - dateA.getTime();
		})
		.slice(0, 15);
});

const averagePrice = computed(() => {
	if (validPriceData.value.length === 0) return '정보없음';

	const prices = validPriceData.value
		.map(d => parseInt(d.dealAmount?.replace(/[^\d]/g, '') || '0'))
		.filter(p => p > 0);

	if (prices.length === 0) return '정보없음';

	const avg = prices.reduce((sum, price) => sum + price, 0) / prices.length;
	return formatPrice(Math.round(avg));
});

const latestDealInfo = computed(() => {
	if (validPriceData.value.length === 0) return null;

	const latest = recentDeals.value[0];
	if (!latest) return null;

	const sameMonthDeals = validPriceData.value.filter(deal =>
		deal.dealYear === latest.dealYear && deal.dealMonth === latest.dealMonth
	);

	return {
		date: `${latest.dealYear}.${latest.dealMonth.toString().padStart(2, '0')}`,
		price: formatDealAmount(latest.dealAmount),
		count: sameMonthDeals.length
	};
});

// Chart.js용 데이터
const monthlyPriceData = computed(() => {
	if (validPriceData.value.length === 0) return [];

	const monthlyData = validPriceData.value.reduce((acc, deal) => {
		const key = `${deal.dealYear}-${deal.dealMonth.toString().padStart(2, '0')}`;
		if (!acc[key]) acc[key] = [];
		acc[key].push(parseInt(deal.dealAmount?.replace(/[^\d]/g, '') || '0'));
		return acc;
	}, {} as Record<string, number[]>);

	const result = Object.entries(monthlyData)
		.map(([key, prices]) => {
			const avg = prices.reduce((sum, p) => sum + p, 0) / prices.length;
			return {
				key,
				avg: Math.round(avg),
				year: key.split('-')[0],
				month: key.split('-')[1]
			};
		})
		.sort((a, b) => a.key.localeCompare(b.key));

	return selectedPeriod.value === 'all' && result.length > 24
		? result.filter((_, index) => index % Math.ceil(result.length / 24) === 0)
		: result;
});

const monthlyVolumeData = computed(() => {
	if (validPriceData.value.length === 0) return [];

	const monthlyVolume = validPriceData.value.reduce((acc, deal) => {
		const key = `${deal.dealYear}-${deal.dealMonth.toString().padStart(2, '0')}`;
		acc[key] = (acc[key] || 0) + 1;
		return acc;
	}, {} as Record<string, number>);

	const result = Object.entries(monthlyVolume)
		.map(([key, count]) => ({
			key,
			count,
			year: key.split('-')[0],
			month: key.split('-')[1]
		}))
		.sort((a, b) => a.key.localeCompare(b.key));

	return selectedPeriod.value === 'all' && result.length > 24
		? result.filter((_, index) => index % Math.ceil(result.length / 24) === 0)
		: result;
});

// ✅ Chart.js 가격 차트 데이터 (추세선 포함)
const priceChartData = computed(() => ({
	labels: monthlyPriceData.value.map(data => `${data.year.slice(2)}.${data.month}`),
	datasets: [
		{
			label: '평균 거래가',
			data: monthlyPriceData.value.map(data => Math.round(data.avg / 10000)), // 억 단위
			borderColor: '#667eea',
			backgroundColor: 'rgba(102, 126, 234, 0.1)',
			borderWidth: 3,
			fill: true,
			tension: 0.4,
			pointBackgroundColor: '#667eea',
			pointBorderColor: '#ffffff',
			pointBorderWidth: 2,
			pointRadius: 5,
			pointHoverRadius: 7,
		}
	]
}));

// ✅ 추세선(선형 회귀) 계산 함수
function linearRegression(x: number[], y: number[]) {
	const n = x.length;
	const xMean = x.reduce((a, b) => a + b, 0) / n;
	const yMean = y.reduce((a, b) => a + b, 0) / n;
	const num = x.reduce((sum, xi, i) => sum + (xi - xMean) * (y[i] - yMean), 0);
	const den = x.reduce((sum, xi) => sum + (xi - xMean) ** 2, 0);
	const slope = den === 0 ? 0 : num / den;
	const intercept = yMean - slope * xMean;
	return { slope, intercept };
}

// ✅ 연평균 상승률(%) 계산 함수
function calculateAnnualGrowthRate(prices) {
	if (prices.length < 2) {
		throw new Error("가격 배열에는 최소 2개의 값이 필요합니다.");
	}

	const startPrice = prices[0];
	const endPrice = prices[prices.length - 1];
	const years = prices.length - 1;

	const cagr = Math.pow(endPrice / startPrice, 1 / years) - 1;

	return cagr * 100; // 백분율(%)로 반환
}


// ✅ 가격차트 + 추세선 데이터셋
const priceChartDataWithTrend = computed(() => {
	const base = priceChartData.value;
	const prices = monthlyPriceData.value.map(d => d.avg);
	const months = prices.map((_, i) => i + 1);
	if (prices.length < 2) return base;

	// 선형 회귀로 추세선 계산
	const { slope, intercept } = linearRegression(months, prices);

	// 추세선 y값 생성
	const trendLine = months.map(x => slope * x + intercept);

	// Chart.js용 데이터셋 추가
	return {
		...base,
		datasets: [
			...base.datasets,
			{
				label: '추세선',
				data: trendLine.map(v => Math.round(v / 10000)), // 억 단위
				borderColor: '#ff3366',
				borderWidth: 2,
				borderDash: [8, 6],
				pointRadius: 0,
				fill: false,
				order: 0,
			}
		]
	};
});

// ✅ 연평균 10% 이상 오르면 추천 메시지
const trendMessage = computed(() => {
	const prices = monthlyPriceData.value.map(d => d.avg);
	if (prices.length < 2) return '';
	const annualGrowthRate = calculateAnnualGrowthRate(prices);
	if (annualGrowthRate >= 10.0) {
		return `📈 연평균${annualGrowthRate.toFixed(1)}% 상승. 매수 추천!`;
	} else {
		return `상승률이 10% 미만입니다. (${annualGrowthRate.toFixed(1)}%)`;
	}
});

// ✅ Chart.js 거래량 차트 데이터
const volumeChartData = computed(() => ({
	labels: monthlyVolumeData.value.map(data => `${data.year.slice(2)}.${data.month}`),
	datasets: [
		{
			label: '거래량',
			data: monthlyVolumeData.value.map(data => data.count),
			borderColor: '#9aa0a6',
			backgroundColor: 'rgba(154, 160, 166, 0.1)',
			borderWidth: 2,
			fill: true,
			tension: 0.4,
			pointBackgroundColor: '#9aa0a6',
			pointBorderColor: '#ffffff',
			pointBorderWidth: 2,
			pointRadius: 4,
			pointHoverRadius: 6,
		}
	]
}));

// ✅ Chart.js 가격 차트 옵션
const priceChartOptions = computed(() => ({
	responsive: true,
	maintainAspectRatio: false,
	plugins: {
		legend: {
			display: false
		},
		tooltip: {
			mode: 'index' as const,
			intersect: false,
			backgroundColor: 'rgba(0, 0, 0, 0.8)',
			titleColor: '#ffffff',
			bodyColor: '#ffffff',
			cornerRadius: 8,
			padding: 12,
			callbacks: {
				label: function(context) {
					return `평균 거래가: ${context.parsed.y.toFixed(1)}억원`;
				}
			}
		}
	},
	scales: {
		y: {
			beginAtZero: false,
			grid: {
				color: 'rgba(0, 0, 0, 0.1)',
			},
			ticks: {
				callback: function(value) {
					return `${value}억`;
				},
				font: {
					size: 11
				},
				color: '#5f6368'
			}
		},
		x: {
			grid: {
				display: false,
			},
			ticks: {
				font: {
					size: 11
				},
				color: '#5f6368',
				maxRotation: 45
			}
		}
	},
	interaction: {
		mode: 'nearest' as const,
		axis: 'x' as const,
		intersect: false
	},
	elements: {
		point: {
			hoverBorderWidth: 3
		}
	}
}));

// ✅ Chart.js 거래량 차트 옵션
const volumeChartOptions = computed(() => ({
	responsive: true,
	maintainAspectRatio: false,
	plugins: {
		legend: {
			display: false
		},
		tooltip: {
			mode: 'index' as const,
			intersect: false,
			backgroundColor: 'rgba(0, 0, 0, 0.8)',
			titleColor: '#ffffff',
			bodyColor: '#ffffff',
			cornerRadius: 8,
			padding: 12,
			callbacks: {
				label: function(context) {
					return `거래량: ${context.parsed.y}건`;
				}
			}
		}
	},
	scales: {
		y: {
			beginAtZero: true,
			grid: {
				color: 'rgba(0, 0, 0, 0.1)',
			},
			ticks: {
				callback: function(value) {
					return `${value}건`;
				},
				font: {
					size: 11
				},
				color: '#5f6368'
			}
		},
		x: {
			grid: {
				display: false,
			},
			ticks: {
				font: {
					size: 11
				},
				color: '#5f6368',
				maxRotation: 45
			}
		}
	},
	interaction: {
		mode: 'nearest' as const,
		axis: 'x' as const,
		intersect: false
	}
}));

// 통계 정보
const maxPriceFormatted = computed(() => {
	if (monthlyPriceData.value.length === 0) return '';
	const max = Math.max(...monthlyPriceData.value.map(d => d.avg));
	return formatPrice(max);
});

const minPriceFormatted = computed(() => {
	if (monthlyPriceData.value.length === 0) return '';
	const min = Math.min(...monthlyPriceData.value.map(d => d.avg));
	return formatPrice(min);
});

const peakVolumeMonth = computed(() => {
	if (monthlyVolumeData.value.length === 0) return '-';
	const maxVolume = Math.max(...monthlyVolumeData.value.map(d => d.count));
	const peakMonth = monthlyVolumeData.value.find(d => d.count === maxVolume);
	return peakMonth ? `${peakMonth.year}.${peakMonth.month}` : '-';
});

// API 호출
const fetchDealData = async (aptSeq: string) => {
	loading.value = true;
	error.value = null;

	try {
		console.log('API 호출 시작:', aptSeq);
		const response = await fetch(`http://localhost:8080/api/v1/house/deals/${aptSeq}`);

		if (!response.ok) {
			throw new Error(`API 호출 실패: ${response.status}`);
		}

		const data: Deal[] = await response.json();
		dealData.value = data || [];
		console.log(`전체 거래내역 ${data.length}건 로드됨:`, data);

	} catch (err) {
		console.error('거래내역 조회 중 오류:', err);
		error.value = '거래내역을 불러오는 중 오류가 발생했습니다.';
		dealData.value = [];
	} finally {
		loading.value = false;
	}
};

// 유틸리티 함수들
const formatPrice = (price: number): string => {
	if (price >= 10000) {
		const eok = Math.floor(price / 10000);
		const cheonman = price % 10000;
		return cheonman > 0 ? `${eok}억 ${cheonman.toLocaleString()}` : `${eok}억`;
	}
	return `${price.toLocaleString()}만`;
};

const formatDealAmount = (amount: string | undefined): string => {
	if (!amount) return '-';
	const price = parseInt(amount.replace(/[^\d]/g, ''));
	return formatPrice(price);
};

const formatTableDate = (deal: Deal): string => {
	if (!deal.dealYear || !deal.dealMonth || !deal.dealDay) return '-';
	const year = deal.dealYear.toString().slice(-2);
	const month = deal.dealMonth.toString().padStart(2, '0');
	const day = deal.dealDay.toString().padStart(2, '0');
	return `${year}.${month}.${day}`;
};

const getPriceTitle = () => {
	switch (selectedPeriod.value) {
		case 'all':
			return '전체 거래내역 평균';
		case '3years':
			return '최근 3년 거래내역 평균';
		case '1year':
			return '최근 1년 거래내역 평균';
		default:
			return '거래내역 평균';
	}
};

// aptSeq 변경 감지
watch(() => props.aptSeq, (newAptSeq) => {
	if (newAptSeq) {
		fetchDealData(newAptSeq);
	} else {
		dealData.value = [];
		error.value = null;
	}
}, { immediate: true });

// 기간 변경 시 차트 새로고침
watch(selectedPeriod, () => {
	chartKey.value++;
});
</script>

<style>
.deal-analysis-container {
	max-width: 880px;
	margin: 0 auto;
	background: #fff;
	border-radius: 20px;
	box-shadow: 0 2px 12px rgba(0,0,0,0.08);
	padding: 32px 24px 36px 24px;
}

.price-header {
	text-align: center;
	margin-bottom: 18px;
}
.price-title {
	font-size: 1.2rem;
	color: #667eea;
	margin-bottom: 4px;
	font-weight: 700;
}
.main-price {
	font-size: 2.2rem;
	font-weight: bold;
	color: #22223b;
	margin-bottom: 2px;
}
.total-deals {
	font-size: 0.98rem;
	color: #888;
	margin-top: 2px;
}

.tab-menu {
	display: flex;
	justify-content: center;
	gap: 12px;
	margin-bottom: 10px;
}
.tab-btn {
	background: #f3f4fa;
	border: none;
	border-radius: 12px;
	padding: 8px 18px;
	font-size: 1rem;
	color: #667eea;
	font-weight: 600;
	cursor: pointer;
	transition: background 0.2s;
}
.tab-btn.active {
	background: #667eea;
	color: #fff;
}

.chart-loading, .chart-error, .chart-empty {
	text-align: center;
	margin: 40px 0;
}
.loading-spinner {
	width: 32px;
	height: 32px;
	border: 4px solid #eee;
	border-top: 4px solid #667eea;
	border-radius: 50%;
	animation: spin 1s linear infinite;
	margin: 0 auto 12px auto;
}
@keyframes spin {
	to { transform: rotate(360deg); }
}
.empty-icon {
	font-size: 2.5rem;
	margin-bottom: 8px;
}

.chart-section {
	margin-top: 18px;
}
.latest-deal-label {
	text-align: right;
	color: #ff7e5f;
	font-size: 1rem;
	margin-bottom: 8px;
	font-weight: 600;
}
.trend-message {
	background: #fff4ec;
	color: #ff3366;
	font-weight: bold;
	font-size: 1.1rem;
	border-radius: 10px;
	padding: 12px 20px;
	margin-bottom: 20px;
	text-align: center;
	box-shadow: 0 1px 4px rgba(255,51,102,0.08);
}

.charts-container {
	display: flex;
	flex-wrap: wrap;
	gap: 30px;
	margin-bottom: 30px;
}
.price-chart-wrapper, .volume-chart-wrapper {
	flex: 1 1 350px;
	min-width: 320px;
}
.chart-container {
	background: #fff;
	border-radius: 16px;
	box-shadow: 0 2px 8px rgba(0,0,0,0.06);
	padding: 20px;
	margin-bottom: 24px;
	min-height: 250px;
	height: 320px;
}

.summary-cards {
	display: flex;
	gap: 18px;
	justify-content: center;
	margin-bottom: 18px;
}
.summary-card {
	background: #f3f4fa;
	border-radius: 14px;
	padding: 16px 24px;
	min-width: 120px;
	text-align: center;
	box-shadow: 0 1px 4px rgba(102,126,234,0.06);
	display: flex;
	align-items: center;
	gap: 10px;
}
.card-icon {
	font-size: 1.6rem;
}
.card-label {
	font-size: 0.98rem;
	color: #888;
}
.card-value {
	font-size: 1.2rem;
	font-weight: bold;
	color: #22223b;
}

.deal-history-table {
	margin-top: 18px;
	background: #f9fafd;
	border-radius: 14px;
	padding: 18px 16px;
	box-shadow: 0 1px 4px rgba(102,126,234,0.06);
}
.table-header {
	font-weight: bold;
	font-size: 1.05rem;
	color: #667eea;
	margin-bottom: 8px;
}
.table-content {
	width: 100%;
}
.table-row {
	display: flex;
	align-items: center;
	border-bottom: 1px solid #ececec;
	padding: 7px 0;
	font-size: 0.98rem;
}
.table-header-row {
	background: #f3f4fa;
	border-radius: 8px;
	font-weight: 700;
	color: #667eea;
	padding: 7px 0;
}
.col-date {
	flex: 1.2;
}
.col-area {
	flex: 1.5;
}
.col-price {
	flex: 1.5;
}
.price-highlight {
	color: #ff7e5f;
	font-weight: bold;
}
.floor-info {
	color: #888;
	margin-left: 5px;
	font-size: 0.92em;
}
</style>
