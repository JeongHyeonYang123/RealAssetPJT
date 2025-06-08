<!-- components/map/DealPriceChart.vue -->
<template>
  <div class="deal-price-chart">
    <canvas ref="chartCanvas" width="350" height="200"></canvas>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue';
import type { Deal } from '../../types/Deal';

interface Props {
  deals: Deal[];
}

const props = defineProps<Props>();
const chartCanvas = ref<HTMLCanvasElement | null>(null);
let chartInstance: any = null;

onMounted(async () => {
  await nextTick();
  createChart();
});

watch(() => props.deals, () => {
  createChart();
}, { deep: true });

const createChart = () => {
  if (!chartCanvas.value || !window.Chart) return;

  if (chartInstance) {
    chartInstance.destroy();
  }

  const chartData = prepareChartData();

  chartInstance = new window.Chart(chartCanvas.value, {
    type: 'line',
    data: {
      datasets: [{
        label: '거래가격',
        data: chartData,
        borderColor: '#2d60e8',
        backgroundColor: 'rgba(45, 96, 232, 0.1)',
        borderWidth: 2,
        tension: 0.1,
        pointBackgroundColor: '#2d60e8',
        pointBorderColor: '#ffffff',
        pointBorderWidth: 2,
        pointRadius: 4
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        }
      },
      scales: {
        x: {
          type: 'time',
          time: {
            unit: 'month',
            displayFormats: {
              month: 'YYYY.MM'
            }
          },
          title: {
            display: true,
            text: '거래시점'
          }
        },
        y: {
          title: {
            display: true,
            text: '거래가격 (만원)'
          },
          ticks: {
            callback: function(value: any) {
              return formatPrice(value);
            }
          }
        }
      }
    }
  });
};

const prepareChartData = () => {
  if (!props.deals || props.deals.length === 0) return [];

  return props.deals
      .filter(deal => deal.dealYear && deal.dealMonth && deal.dealDay && deal.dealAmount)
      .map(deal => {
        const date = new Date(deal.dealYear, deal.dealMonth - 1, deal.dealDay);
        const price = parseInt(deal.dealAmount.replace(/[^\d]/g, ''));

        return {
          x: date,
          y: price
        };
      })
      .sort((a, b) => a.x.getTime() - b.x.getTime());
};

const formatPrice = (price: number) => {
  if (price >= 10000) {
    const eok = Math.floor(price / 10000);
    const cheonman = price % 10000;
    return cheonman > 0 ? `${eok}억${cheonman}` : `${eok}억`;
  }
  return `${price}`;
};
</script>

<style scoped>
.deal-price-chart {
  margin-top: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  height: 220px;
}
</style>
