<template>
  <div>
    <div class="page-header flex-between">
      <h2>我的申请列表</h2>
      <el-input
        v-model="keyword"
        placeholder="搜索单号或状态"
        clearable
        style="width: 280px"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    <section class="card-section">
      <el-table :data="filteredRequests" border stripe>
        <el-table-column label="申请单号" width="160">
          <template #default="{ row }">
            {{ row.displayId ?? row.id }}
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" width="160" />
        <el-table-column label="商品明细">
          <template #default="{ row }">
            <el-tag v-for="item in row.items" :key="item.sku" style="margin-right: 8px">
              {{ item.name }} x {{ item.quantity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="">
            <el-button link type="primary">查看详情</el-button>
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

const keyword = ref('');
const requests = ref([]);

onMounted(async () => {
  requests.value = await api.listPurchaseRequests();
});

const filteredRequests = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return requests.value;
  return requests.value.filter(item => {
    const id = (item.displayId ?? item.id ?? '').toLowerCase();
    const status = (item.status ?? '').toLowerCase();
    return id.includes(kw) || status.includes(kw);
  });
});
</script>
