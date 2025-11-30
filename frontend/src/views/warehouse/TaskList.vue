<template>
  <div>
    <div class="page-header flex-between">
      <h2>待执行任务列表</h2>
      <el-switch v-model="showOnlyPending" active-text="仅显示待执行" />
    </div>
    <section class="card-section">
      <el-table :data="filteredTasks" border stripe>
        <el-table-column label="任务编号" width="160">
          <template #default="{ row }">
            {{ row.displayId ?? row.id }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.type === 'INBOUND' ? 'success' : 'warning'">
              {{ row.typeLabel ?? (row.type === 'INBOUND' ? '入库' : '出库') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignee" label="执行人" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="">
            <!-- <el-button link >开始执行</el-button> -->
            <el-button link type="primary">查看明细</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { api } from '../../services/api.js';
import StatusTag from '../../components/StatusTag.vue';

const showOnlyPending = ref(false);
const tasks = ref([]);

onMounted(async () => {
  tasks.value = await api.listWarehouseTasks();
});

const filteredTasks = computed(() => {
  if (!showOnlyPending.value) return tasks.value;
  return tasks.value.filter(item => item.status !== 'DONE');
});
</script>
