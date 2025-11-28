<template>
  <div>
    <div class="page-header flex-between">
      <h2>库存监控看板</h2>
      <el-radio-group v-model="viewMode">
        <el-radio-button label="overview">概览</el-radio-button>
        <el-radio-button label="alert">预警</el-radio-button>
      </el-radio-group>
    </div>
    <section class="card-section">
      <h3>库区温度图</h3>
      <div ref="heatmapRef" class="chart"></div>
    </section>
    <section class="card-section">
      <h3>预警列表</h3>
      <el-table :data="filteredAlerts" border stripe>
        <el-table-column prop="sku" label="SKU" width="140" />
        <el-table-column prop="name" label="物料" />
        <el-table-column prop="warehouse" label="仓库" width="120" />
        <el-table-column prop="quantity" label="当前库存" width="120" />
        <el-table-column prop="safetyStock" label="安全库存" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="">
            <el-tag type="danger">低库存</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import * as echarts from 'echarts/core';
import { HeatmapChart } from 'echarts/charts';
import { TooltipComponent, VisualMapComponent, GridComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { api } from '../../services/api.js';

echarts.use([HeatmapChart, TooltipComponent, VisualMapComponent, GridComponent, CanvasRenderer]);

const heatmapRef = ref(null);
const viewMode = ref('overview');
const alerts = ref([]);
let heatmap;

const filteredAlerts = computed(() => {
  if (viewMode.value === 'overview') return alerts.value.slice(0, 5);
  return alerts.value.filter(item => item.quantity < item.safetyStock);
});

const renderHeatmap = data => {
  heatmap = heatmap ?? echarts.init(heatmapRef.value);
  heatmap.setOption({
    tooltip: {
      position: 'top'
    },
    grid: {
      height: '70%',
      top: '10%'
    },
    xAxis: {
      type: 'category',
      data: ['A区', 'B区', 'C区', 'D区', 'E区'],
      splitArea: { show: true }
    },
    yAxis: {
      type: 'category',
      data: ['货架1', '货架2', '货架3', '货架4'],
      splitArea: { show: true }
    },
    visualMap: {
      min: 0,
      max: 100,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%'
    },
    series: [
      {
        name: '库存充足率',
        type: 'heatmap',
        data,
        label: { show: true },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  });
};

const load = async () => {
  const inventory = await api.listInventory();
  alerts.value = inventory;
  const heatmapData = [];
  const zones = ['A区', 'B区', 'C区', 'D区', 'E区'];
  const racks = ['货架1', '货架2', '货架3', '货架4'];
  zones.forEach((zone, x) => {
    racks.forEach((rack, y) => {
      const ratio = Math.round(40 + Math.random() * 60);
      heatmapData.push([x, y, ratio]);
    });
  });
  renderHeatmap(heatmapData);
};

const resizeHandler = () => {
  heatmap?.resize();
};

onMounted(() => {
  load();
  window.addEventListener('resize', resizeHandler);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler);
  heatmap?.dispose();
});
</script>

<style scoped>
.chart {
  height: 360px;
}
</style>
