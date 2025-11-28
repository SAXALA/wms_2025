<template>
  <div>
    <div class="page-header flex-between">
      <h2>操作日志查询</h2>
      <el-input
        v-model="keyword"
        placeholder="搜索操作人/模块"
        clearable
        style="width: 280px"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    <section class="card-section">
      <el-table :data="filtered" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="action" label="动作" width="160" />
        <el-table-column prop="details" label="详情" />
        <el-table-column prop="createdAt" label="时间" width="160" />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { api } from '../../services/api.js';

const keyword = ref('');
const logs = ref([]);

onMounted(async () => {
  logs.value = await api.listOperationLogs();
});

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return logs.value;
  return logs.value.filter(item =>
    item.operator.toLowerCase().includes(kw) ||
    item.module.toLowerCase().includes(kw)
  );
});
</script>
