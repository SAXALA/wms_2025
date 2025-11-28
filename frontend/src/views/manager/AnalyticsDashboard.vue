<template>
  <div>
    <div class="page-header flex-between">
      <h2>报表分析仪表盘</h2>
      <el-button type="primary" @click="load" :loading="loading">
        <el-icon><RefreshRight /></el-icon>
        刷新数据
      </el-button>
    </div>
    <section class="stats-grid">
      <div class="stat-card" v-for="stat in statsCards" :key="stat.label">
        <div class="stat-card__title">{{ stat.label }}</div>
        <div class="stat-card__value">{{ stat.value }}</div>
        <div class="stat-card__desc">{{ stat.desc }}</div>
      </div>
    </section>
    <section class="card-section">
      <h3>出入库趋势对比</h3>
      <div ref="trendRef" class="chart"></div>
    </section>
    <section class="card-section">
      <h3>库存结构分布</h3>
      <div ref="pieRef" class="chart"></div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue';
import * as echarts from 'echarts/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import { TooltipComponent, LegendComponent, GridComponent, TitleComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { api } from '../../services/api.js';

echarts.use([BarChart, LineChart, PieChart, TooltipComponent, LegendComponent, GridComponent, TitleComponent, CanvasRenderer]);

const loading = ref(false);
const stats = reactive({ inbound: 0, outbound: 0, inventoryValue: 0, turnoverDays: 0 });
const trendData = ref([]);
const trendRef = ref(null);
const pieRef = ref(null);
let trendChart;
let pieChart;

const statsCards = computed(() => [
  { label: '月度入库量', value: stats.inbound, desc: '单位：批次' },
  { label: '月度出库量', value: stats.outbound, desc: '单位：批次' },
  { label: '库存总价值', value: `${stats.inventoryValue} M¥`, desc: '按平均采购价估算' },
  { label: '库存周转天数', value: stats.turnoverDays, desc: '目标：≤ 45 天' }
]);

const renderTrend = () => {
  if (!trendRef.value) return;
  trendChart = trendChart ?? echarts.init(trendRef.value);
  const categories = trendData.value.map(item => item.date);
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库', '出库'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: categories },
    yAxis: { type: 'value' },
    series: [
      { name: '入库', type: 'bar', data: trendData.value.map(item => item.inbound), itemStyle: { color: '#22c55e' } },
      { name: '出库', type: 'line', data: trendData.value.map(item => item.outbound), smooth: true, lineStyle: { color: '#3b82f6' } }
    ]
  });
};

const renderPie = () => {
  if (!pieRef.value) return;
  pieChart = pieChart ?? echarts.init(pieRef.value);
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '库存结构',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: { label: { show: true, fontSize: 18, fontWeight: 'bold' } },
        labelLine: { show: false },
        data: [
          { value: 45, name: '原材料' },
          { value: 30, name: '周转工具' },
          { value: 15, name: '包装耗材' },
          { value: 10, name: '备品备件' }
        ]
      }
    ]
  });
};

const load = async () => {
  loading.value = true;
  try {
    const [statRes, trendRes] = await Promise.all([
      api.getDashboardStats(),
      api.getInventoryTrends()
    ]);
    Object.assign(stats, statRes);
    trendData.value = trendRes;
    renderTrend();
    renderPie();
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  load();
  window.addEventListener('resize', resizeCharts);
});

const resizeCharts = () => {
  trendChart?.resize();
  pieChart?.resize();
};

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts);
  trendChart?.dispose();
  pieChart?.dispose();
});
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 12px 30px rgba(37, 99, 235, 0.25);
}

.stat-card:nth-child(2) {
  background: linear-gradient(135deg, #0ea5e9, #0f172a);
}

.stat-card:nth-child(3) {
  background: linear-gradient(135deg, #f97316, #ea580c);
}

.stat-card:nth-child(4) {
  background: linear-gradient(135deg, #22c55e, #15803d);
}

.stat-card__title {
  font-size: 14px;
  opacity: 0.8;
}

.stat-card__value {
  font-size: 28px;
  font-weight: 700;
  margin: 8px 0;
}

.stat-card__desc {
  font-size: 13px;
  opacity: 0.8;
}

.chart {
  height: 360px;
}
</style>
