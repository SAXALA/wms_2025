<template>
  <div class="operation-log">
    <div class="page-header flex-between">
      <h2>操作日志查询</h2>
      <el-button type="primary" @click="handleExport" :loading="exporting">
        <el-icon><Download /></el-icon>
        导出筛选结果
      </el-button>
    </div>

    <section class="card-section">
      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="操作人">
          <el-input
            v-model="filters.username"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="模块">
          <el-input
            v-model="filters.module"
            placeholder="如 用户管理"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="动作">
          <el-input
            v-model="filters.action"
            placeholder="如 创建用户"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            :default-time="['00:00:00', '23:59:59']"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logs" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="160" />
        <el-table-column prop="action" label="动作" width="180" />
        <el-table-column prop="details" label="详情" min-width="280" show-overflow-tooltip />
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ row.createdAtText }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="filters.pageSize"
          :current-page="filters.page"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import dayjs from 'dayjs';
import { ElMessage } from 'element-plus';
import { Download, Search } from '@element-plus/icons-vue';
import { api } from '../../services/api.js';

const filters = reactive({
  username: '',
  module: '',
  action: '',
  dateRange: [],
  page: 1,
  pageSize: 20
});

const logs = ref([]);
const total = ref(0);
const loading = ref(false);
const exporting = ref(false);

const buildQuery = (override = {}) => {
  const [start, end] = filters.dateRange || [];
  return {
    username: filters.username?.trim() || undefined,
    module: filters.module?.trim() || undefined,
    action: filters.action?.trim() || undefined,
    startTime: start ? dayjs(start).toISOString() : undefined,
    endTime: end ? dayjs(end).toISOString() : undefined,
    page: filters.page,
    size: filters.pageSize,
    ...override
  };
};

const fetchLogs = async () => {
  loading.value = true;
  try {
    const { records, total: totalCount, pageSize } = await api.listOperationLogs(buildQuery());
    logs.value = records;
    total.value = totalCount;
    filters.pageSize = pageSize || filters.pageSize;
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  filters.page = 1;
  fetchLogs();
};

const handleReset = () => {
  filters.username = '';
  filters.module = '';
  filters.action = '';
  filters.dateRange = [];
  filters.page = 1;
  fetchLogs();
};

const handlePageChange = page => {
  filters.page = page;
  fetchLogs();
};

const handleSizeChange = size => {
  filters.pageSize = size;
  filters.page = 1;
  fetchLogs();
};

const triggerDownload = (blob, fileName) => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName || `operation-logs-${Date.now()}.csv`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

const handleExport = async () => {
  exporting.value = true;
  try {
    const params = buildQuery({ page: undefined, size: undefined });
    const { blob, fileName } = await api.exportOperationLogs(params);
    triggerDownload(blob, fileName);
    ElMessage.success('已开始下载日志文件');
  } catch (error) {
    console.error('[operation-log] export failed', error);
    ElMessage.error('导出失败，请稍后重试');
  } finally {
    exporting.value = false;
  }
};

onMounted(fetchLogs);
</script>

<style scoped>
.filter-form {
  margin-bottom: 12px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.pagination {
  margin-top: 16px;
  text-align: right;
}
</style>
