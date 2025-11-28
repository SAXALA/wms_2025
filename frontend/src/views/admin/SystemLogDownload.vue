<template>
  <div>
    <div class="page-header flex-between">
      <h2>系统日志下载</h2>
      <el-button type="primary" @click="handleExport">
        <el-icon><Download /></el-icon>
        导出全部
      </el-button>
    </div>
    <section class="card-section">
      <el-table :data="logs" border stripe>
        <el-table-column prop="name" label="日志文件" />
        <el-table-column prop="size" label="大小" width="120" />
        <el-table-column prop="generatedAt" label="生成时间" width="180" />
        <el-table-column label="操作" width="140">
          <template #default="">
            <el-button link type="primary">下载</el-button>
            <el-button link type="success">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';

const logs = ref([
  { name: 'application-2025-11-28.log', size: '1.2 MB', generatedAt: '2025-11-28 08:00' },
  { name: 'audit-2025-11-27.log', size: '820 KB', generatedAt: '2025-11-27 23:59' },
  { name: 'scheduler-2025-11-27.log', size: '400 KB', generatedAt: '2025-11-27 21:00' }
]);

const handleExport = () => {
  ElMessage.success('已触发日志导出任务');
};
</script>
