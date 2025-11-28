<template>
  <div>
    <div class="page-header flex-between">
      <h2>出库申请创建</h2>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        <el-icon><Sell /></el-icon>
        提交出库单
      </el-button>
    </div>
    <section class="card-section">
      <div class="flex-between" style="margin-bottom: 16px">
        <el-input v-model="form.orderType" placeholder="出库类型 (销售/调拨)" style="width: 220px" />
        <el-input v-model="form.customer" placeholder="目的客户/仓库" style="width: 220px" />
        <el-date-picker v-model="form.expectedDate" type="date" placeholder="计划出库日期" />
      </div>
      <el-table :data="form.lines" border>
        <el-table-column prop="sku" label="SKU" width="120" />
        <el-table-column prop="name" label="物料名称" />
        <el-table-column label="数量" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" />
          </template>
        </el-table-column>
        <el-table-column label="批次" width="160">
          <template #default="{ row }">
            <el-input v-model="row.batch" placeholder="批次号" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeLine($index)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!form.lines.length" description="请添加出库物料" />
      <el-button style="margin-top: 12px" @click="addLine">
        <el-icon><Plus /></el-icon>
        添加物料
      </el-button>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '../../services/api.js';

const loading = ref(false);
const form = reactive({
  orderType: '',
  customer: '',
  expectedDate: '',
  lines: []
});

const addLine = () => {
  form.lines.push({ sku: '', name: '', quantity: 1, batch: '' });
};

const removeLine = index => {
  form.lines.splice(index, 1);
};

const handleSubmit = async () => {
  if (!form.lines.length) {
    ElMessage.error('请至少添加一行物料');
    return;
  }
  loading.value = true;
  try {
    await api.submitOutbound({ ...form });
    ElMessage.success('出库申请已提交');
    form.orderType = '';
    form.customer = '';
    form.expectedDate = '';
    form.lines = [];
  } finally {
    loading.value = false;
  }
};
</script>
