<template>
  <div>
    <div class="page-header flex-between">
      <h2>系统监控仪表盘</h2>
      <el-button @click="load" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>
    <section class="metrics">
      <div class="metric-card" v-for="metric in metrics" :key="metric.label">
        <div class="metric-card__label">{{ metric.label }}</div>
        <div class="metric-card__value">{{ metric.value }}</div>
        <div class="metric-card__meta">{{ metric.meta }}</div>
      </div>
    </section>
    <section class="card-section">
      <h3>API 请求量趋势</h3>
      <div ref="trafficRef" class="chart"></div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

const loading = ref(false);
const trafficRef = ref(null);
let chart;

const metrics = computed(() => [
  { label: '在线用户', value: 18, meta: '活跃角色覆盖采购、仓库、管理' },
  { label: '接口响应 (P95)', value: '210 ms', meta: '近 24 小时' },
  { label: '错误率', value: '0.6%', meta: '近 24 小时' },
  { label: '队列堆积', value: 3, meta: '消息队列待处理任务' }
]);

echarts.use([LineChart, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer]);

const renderChart = () => {
  chart = chart ?? echarts.init(trafficRef.value);
  const hours = Array.from({ length: 12 }, (_, i) => `${i * 2}:00`);
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['成功请求', '错误请求'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: hours },
    yAxis: { type: 'value' },
    series: [
      {
        name: '成功请求',
        type: 'line',
        smooth: true,
        areaStyle: { color: 'rgba(59, 130, 246, 0.2)' },
        lineStyle: { color: '#2563eb' },
        data: hours.map(() => Math.round(200 + Math.random() * 80))
      },
      {
        name: '错误请求',
        type: 'line',
        smooth: true,
        lineStyle: { color: '#ef4444' },
        data: hours.map(() => Math.round(Math.random() * 10))
      }
    ]
  });
};

const load = () => {
  loading.value = true;
  setTimeout(() => {
    renderChart();
    loading.value = false;
  }, 500);
};

onMounted(() => {
  load();
  window.addEventListener('resize', resize);
});

const resize = () => chart?.resize();

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize);
  chart?.dispose();
});
</script>

<style scoped>
.metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.metric-card {
  background: #fff;
  border-radius: 16px;
  padding: 18px;
  box-shadow: 0 15px 30px rgba(15, 23, 42, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.metric-card__label {
  font-size: 14px;
  color: #64748b;
}

.metric-card__value {
  font-size: 28px;
  font-weight: 700;
  margin: 8px 0;
}

.metric-card__meta {
  font-size: 12px;
  color: #94a3b8;
}

.chart {
  height: 360px;
}
</style>
