<template>
  <div>
    <div class="page-header flex-between">
      <h2>入库申请创建</h2>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        <el-icon><CircleCheck /></el-icon>
        提交入库单
      </el-button>
    </div>

    <section class="card-section">
      <div class="form-header">
        <el-input v-model="form.reference" placeholder="关联单号 / 备注" style="width: 320px" />
        <el-date-picker v-model="form.expectedDate" type="date" placeholder="计划入库日期" />
      </div>
    </section>

    <section class="card-section">
      <h3 class="section-title">选择物料</h3>
      <div class="add-line-panel">
        <el-select
          v-model="selectedProductId"
          filterable
          placeholder="请选择商品"
          style="width: 320px"
        >
          <el-option
            v-for="product in products"
            :key="product.id"
            :label="`${product.name} (${product.sku})`"
            :value="product.id"
          />
        </el-select>
        <el-input-number v-model="selectedQuantity" :min="1" :step="10" placeholder="数量" />
        <el-select
          v-model="selectedLocationId"
          placeholder="选择库位 (可选)"
          style="width: 220px"
          clearable
          filterable
        >
          <el-option
            v-for="location in activeLocations"
            :key="location.id"
            :label="location.displayLabel"
            :value="location.id"
          />
        </el-select>
        <el-button type="primary" link @click="addLine">
          <el-icon><Plus /></el-icon>
          加入清单
        </el-button>
      </div>
    </section>

    <section class="card-section">
      <h3 class="section-title">入库明细</h3>
      <el-table :data="form.lines" border>
        <el-table-column prop="sku" label="SKU" width="140" />
        <el-table-column prop="name" label="物料名称" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column label="数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" />
          </template>
        </el-table-column>
        <el-table-column label="库位" width="220">
          <template #default="{ row }">
            <div class="location-cell">
              <el-select
                v-model="row.locationId"
                placeholder="选择库位"
                filterable
                clearable
                @change="value => syncRowLocation(row, value)"
              >
                <el-option
                  v-for="location in activeLocations"
                  :key="location.id"
                  :label="location.displayLabel"
                  :value="location.id"
                />
              </el-select>
              <el-tag v-if="row.locationLabel" size="small" type="info">
                {{ row.locationLabel }}
              </el-tag>
              <span v-else class="text-muted">未指定</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeLine($index)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!form.lines.length" description="请选择商品加入清单" />
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '../../services/api.js';

const loading = ref(false);
const products = ref([]);
const locations = ref([]);
const selectedProductId = ref(null);
const selectedQuantity = ref(1);
const selectedLocationId = ref(null);

const form = reactive({
  reference: '',
  expectedDate: '',
  lines: []
});

onMounted(async () => {
  products.value = await api.listProducts();
  locations.value = await api.listLocations({ includeInactive: false });
});

const activeLocations = computed(() => locations.value.filter(item => item.active));

const findLocation = id => activeLocations.value.find(item => String(item.id) === String(id));

const syncRowLocation = (row, locationId) => {
  const location = findLocation(locationId);
  if (!location) {
    row.locationId = null;
    row.locationLabel = '';
    return;
  }
  row.locationId = location.id;
  row.locationLabel = location.displayLabel;
};

const addLine = () => {
  if (!selectedProductId.value) {
    ElMessage.warning('请选择商品');
    return;
  }
  const product = products.value.find(item => item.id === selectedProductId.value);
  if (!product) {
    ElMessage.error('商品不存在');
    return;
  }
  if (!selectedQuantity.value || selectedQuantity.value <= 0) {
    ElMessage.error('数量必须大于 0');
    return;
  }
  if (!selectedLocationId.value) {
    ElMessage.error('请选择入库库位');
    return;
  }

  const location = findLocation(selectedLocationId.value);
  if (!location) {
    ElMessage.error('选择的库位不可用');
    return;
  }

  const existing = form.lines.find(item => item.productId === product.id);
  if (existing) {
    existing.quantity += Number(selectedQuantity.value);
    syncRowLocation(existing, location.id);
  } else {
    form.lines.push({
      productId: product.id,
      sku: product.sku,
      name: product.name,
      unit: product.unit,
      quantity: Number(selectedQuantity.value),
      locationId: location.id,
      locationLabel: location.displayLabel
    });
  }

  ElMessage.success('已加入入库清单');
  selectedProductId.value = null;
  selectedQuantity.value = 1;
  selectedLocationId.value = null;
};

const removeLine = index => {
  form.lines.splice(index, 1);
};

const handleSubmit = async () => {
  if (!form.lines.length) {
    ElMessage.error('请至少添加一行物料');
    return;
  }
  const invalid = form.lines.find(item => !item.productId || item.quantity <= 0 || !item.locationId);
  if (invalid) {
    ElMessage.error('请确保所有物料的商品、数量和库位有效');
    return;
  }
  loading.value = true;
  try {
    await api.submitInbound({ ...form });
    ElMessage.success('入库申请已提交');
    form.reference = '';
    form.expectedDate = '';
    form.lines = [];
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.form-header {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.section-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
}

.add-line-panel {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.location-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.text-muted {
  color: #94a3b8;
  font-size: 12px;
}
</style>
