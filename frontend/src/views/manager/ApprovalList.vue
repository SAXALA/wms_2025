<template>
  <div>
    <div class="page-header flex-between">
      <h2>待我审批</h2>
      <el-select v-model="filterType" placeholder="类型筛选" clearable style="width: 200px">
        <el-option label="采购申请" value="PROCUREMENT" />
        <el-option label="入库申请" value="INBOUND" />
        <el-option label="出库申请" value="OUTBOUND" />
      </el-select>
    </div>
    <section class="card-section">
      <el-table :data="filtered" border stripe>
        <el-table-column label="单据号" width="160">
          <template #default="{ row }">
            {{ row.displayId ?? row.id }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="140">
          <template #default="{ row }">
            {{ row.typeLabel ?? row.type }}
          </template>
        </el-table-column>
        <el-table-column prop="applicant" label="发起人" width="120" />
        <el-table-column prop="submittedAt" label="提交时间" width="160" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openApproval(row.id)">审批</el-button>
            <el-button link>详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../../services/api.js';
import StatusTag from '../../components/StatusTag.vue';

const router = useRouter();
const filterType = ref('');
const approvals = ref([]);

onMounted(async () => {
  approvals.value = await api.listApprovals();
});

const filtered = computed(() => {
  if (!filterType.value) return approvals.value;
  return approvals.value.filter(item => item.type === filterType.value);
});

const openApproval = id => {
  router.push(`/manager/approval/${id}`);
};
</script>
