<template>
  <div>
    <div class="page-header flex-between">
      <h2>库存查询</h2>
      <el-input
        v-model="keyword"
        placeholder="搜索 SKU / 名称"
        clearable
        style="width: 300px"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>
    <section class="card-section">
      <el-table :data="inventory" border stripe>
        <el-table-column prop="sku" label="SKU" width="140" />
        <el-table-column prop="name" label="商品" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column label="库位" width="220">
          <template #default="{ row }">
            <el-tag v-if="row.locationLabel" size="small" type="info">
              {{ row.locationLabel }}
            </el-tag>
            <span v-else class="text-muted">主仓库</span>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="现有库存" width="120" />
        <el-table-column prop="lockedStock" label="锁定数量" width="120" />
        <el-table-column label="安全库存" width="120">
          <template #default="{ row }">
            <el-tag :type="row.quantity >= row.safetyStock ? 'success' : 'danger'">
              {{ row.safetyStock }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { api } from '../../services/api.js';

const keyword = ref('');
const inventory = ref([]);

const load = async () => {
  inventory.value = await api.listInventory({ keyword: keyword.value });
};

watch(keyword, () => {
  load();
}, { immediate: true });
</script>

<style scoped>
.text-muted {
  color: #94a3b8;
  font-size: 12px;
}
</style>
