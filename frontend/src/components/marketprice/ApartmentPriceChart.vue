<template>
  <div class="chart-container">
    <h2>지역별 아파트 매매가격지수</h2>
    <div class="loading" v-if="loading">데이터 로딩 중...</div>
    <Bar
        v-else
        :data="chartData"
        :options="chartOptions"
        :height="400"
    />
  </div>
</template>

<script>
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js'
import { Bar } from 'vue-chartjs'
import axios from 'axios'

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend)

export default {
  name: 'ApartmentPriceChart',
  components: {
    Bar
  },
  data() {
    return {
      loading: true,
      chartData: {
        labels: [],
        datasets: []
      },
      chartOptions: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: '지역별 아파트 매매가격지수 변화'
          },
          legend: {
            position: 'top'
          }
        },
        scales: {
          y: {
            beginAtZero: false,
            title: {
              display: true,
              text: '매매가격지수'
            }
          },
          x: {
            title: {
              display: true,
              text: '기간'
            }
          }
        }
      }
    }
  },
  async mounted() {
    await this.fetchPriceData()
  },
  methods: {
    async fetchPriceData() {
      try {
        const serviceKey = '9dc3f1dbd10049d6ad2f780e30968fb3' // 발급받은 인증키
        const baseUrl = 'https://www.reb.or.kr/r-one/openapi'

        // CORS 문제 해결을 위해 프록시 서버 또는 백엔드 API 사용
        const response = await axios.get('/api/apartment-price-index', {
          params: {
            serviceKey: serviceKey,
            format: 'json',
            numOfRows: 100
          }
        })

        this.processData(response.data)
      } catch (error) {
        console.error('API 호출 실패:', error)
        this.loading = false
      }
    },

    processData(apiData) {
      // API 응답 데이터 처리
      const regions = {
        'A1001': '전국',
        'A1002': '수도권',
        'A2001': '지방',
        'A1003': '서울'
      }

      // 최근 12개월 데이터 추출
      const processedData = this.groupDataByMonth(apiData.items, regions)

      // 차트 데이터 구성
      this.chartData = {
        labels: processedData.labels,
        datasets: [
          {
            label: '전국',
            data: processedData.전국,
            backgroundColor: 'rgba(31, 119, 180, 0.8)',
            borderColor: 'rgba(31, 119, 180, 1)',
            borderWidth: 1
          },
          {
            label: '서울',
            data: processedData.서울,
            backgroundColor: 'rgba(255, 127, 14, 0.8)',
            borderColor: 'rgba(255, 127, 14, 1)',
            borderWidth: 1
          },
          {
            label: '수도권',
            data: processedData.수도권,
            backgroundColor: 'rgba(44, 160, 44, 0.8)',
            borderColor: 'rgba(44, 160, 44, 1)',
            borderWidth: 1
          },
          {
            label: '지방',
            data: processedData.지방,
            backgroundColor: 'rgba(214, 39, 40, 0.8)',
            borderColor: 'rgba(214, 39, 40, 1)',
            borderWidth: 1
          }
        ]
      }

      this.loading = false
    },

    groupDataByMonth(data, regions) {
      // 데이터를 월별로 그룹화하고 지역별로 분류
      const monthlyData = {}

      data.forEach(item => {
        const month = this.formatDate(item.researchDate)
        const regionName = regions[item.regionCd]

        if (!monthlyData[month]) {
          monthlyData[month] = {}
        }

        if (regionName) {
          monthlyData[month][regionName] = parseFloat(item.indices)
        }
      })

      // 정렬된 월 목록 생성
      const sortedMonths = Object.keys(monthlyData).sort()

      const result = {
        labels: sortedMonths,
        전국: [],
        서울: [],
        수도권: [],
        지방: []
      }

      sortedMonths.forEach(month => {
        result.전국.push(monthlyData[month]['전국'] || 0)
        result.서울.push(monthlyData[month]['서울'] || 0)
        result.수도권.push(monthlyData[month]['수도권'] || 0)
        result.지방.push(monthlyData[month]['지방'] || 0)
      })

      return result
    },

    formatDate(dateString) {
      // YYYYMM 형식을 YYYY-MM으로 변환
      const year = dateString.substring(0, 4)
      const month = dateString.substring(4, 6)
      return `${year}-${month}`
    }
  }
}
</script>

<style scoped>
.chart-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.loading {
  text-align: center;
  padding: 50px;
  font-size: 18px;
}

h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}
</style>
