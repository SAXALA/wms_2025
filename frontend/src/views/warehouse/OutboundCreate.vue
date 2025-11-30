<template>
  <div>
    <div class="page-header flex-between">
      <h2>出库申请创建</h2>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        <el-icon><Sell /></el-icon>
        提交出库单
      </el-button>
    </div>

    <section class="card-section">
      <div class="form-header">
        <el-input v-model="form.reference" placeholder="出库事由 / 备注" style="width: 320px" />
        <el-input v-model="form.destination" placeholder="目的客户 / 仓库" style="width: 260px" />
        <el-date-picker v-model="form.expectedDate" type="date" placeholder="计划出库日期" />
      </div>
    </section>

    <section class="card-section">
      <h3 class="section-title">选择出库物料</h3>
      <div class="add-line-panel">
        <el-select
          v-model="selectedProductId"
          filterable
          placeholder="请选择可出库的商品"
          style="width: 320px"
        >
          <el-option
            v-for="stock in stockOptions"
            :key="stock.productId"
            :label="stockOptionLabel(stock)"
            :value="stock.productId"
          />
        </el-select>
        <el-input-number v-model="selectedQuantity" :min="1" :step="5" placeholder="数量" />
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
        <el-button link @click="refreshInventory">刷新库存</el-button>
      </div>
      <el-alert
        v-if="!stockOptions.length"
        title="当前没有可出库的库存，请先补充库存或刷新数据。"
        type="warning"
        show-icon
        style="margin-top: 12px"
      />
    </section>

    <section class="card-section">
      <h3 class="section-title">出库明细</h3>
      <el-table :data="form.lines" border>
        <el-table-column prop="sku" label="SKU" width="140" />
        <el-table-column prop="name" label="物料名称" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column label="可用库存" width="150">
          <template #default="{ row }">
            <el-tag :type="row.quantity < row.available ? 'success' : row.quantity === row.available ? 'warning' : 'danger'">
              {{ row.available }}
            </el-tag>
            <div class="available-note">锁定 {{ row.lockedStock }}</div>
          </template>
        </el-table-column>
        <el-table-column label="出库数量" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="1"
              :max="row.available"
              :disabled="row.available <= 0"
              @change="() => clampQuantity(row)"
            />
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
      <el-empty v-if="!form.lines.length" description="请先选择库存加入出库清单" />
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '../../services/api.js';

const submitting = ref(false);
const stocks = ref([]);
const locations = ref([]);
const selectedProductId = ref(null);
const selectedQuantity = ref(1);
const selectedLocationId = ref(null);

const form = reactive({
  reference: '',
  destination: '',
  expectedDate: '',
  lines: []
});

const fetchInventory = async () => {
  const data = await api.listInventory({});
  stocks.value = data.map(stock => ({
    ...stock,
    available: Math.max(0, Number(stock.quantity ?? 0) - Number(stock.lockedStock ?? 0))
  }));
};

const fetchLocations = async () => {
  locations.value = await api.listLocations({ includeInactive: false });
};

const initialize = async () => {
  try {
    await Promise.all([fetchInventory(), fetchLocations()]);
    syncLinesWithStock();
  } catch (error) {
    console.error(error);
    ElMessage.error('加载出库数据失败，请稍后重试');
  }
};

onMounted(() => {
  initialize();
});

const stockOptions = computed(() => stocks.value.filter(item => item.available > 0));
const activeLocations = computed(() => locations.value.filter(item => item.active));

const stockOptionLabel = stock => `${stock.name} (${stock.sku}) · 可用 ${stock.available}`;
const findStock = productId => stocks.value.find(item => String(item.productId) === String(productId));
const findLocation = id => activeLocations.value.find(item => String(item.id) === String(id));

const syncLinesWithStock = () => {
  form.lines.forEach(line => {
    const stock = findStock(line.productId);
    if (!stock) {
      line.available = 0;
      return;
    }
    line.currentStock = stock.quantity;
    line.lockedStock = stock.lockedStock;
    line.available = stock.available;
    const locationCandidate = findLocation(line.locationId) ?? findLocation(stock.locationId);
    if (locationCandidate) {
      line.locationId = locationCandidate.id;
      line.locationLabel = locationCandidate.displayLabel;
      line.locationCode = locationCandidate.code;
      line.locationName = locationCandidate.name;
    } else if (!line.locationLabel && stock.locationLabel) {
      line.locationLabel = stock.locationLabel;
      line.locationId = null;
    }
    clampQuantity(line);
  });
};

const syncRowLocation = (row, locationId) => {
  const location = findLocation(locationId);
  if (!location) {
    row.locationId = null;
    row.locationLabel = '';
    row.locationCode = '';
    row.locationName = '';
    return;
  }
  row.locationId = location.id;
  row.locationLabel = location.displayLabel;
  row.locationCode = location.code;
  row.locationName = location.name;
};

const addLine = () => {
  if (!selectedProductId.value) {
    ElMessage.warning('请选择可用库存');
    return;
  }
  const stock = findStock(selectedProductId.value);
  if (!stock) {
    ElMessage.error('库存数据不存在');
    return;
  }
  if (stock.available <= 0) {
    ElMessage.error('该商品当前无可出库库存');
    return;
  }
  const quantity = Number(selectedQuantity.value ?? 0);
  if (!Number.isFinite(quantity) || quantity <= 0) {
    ElMessage.error('数量必须大于 0');
    return;
  }
  if (quantity > stock.available) {
    ElMessage.warning(`最大可出库数量为 ${stock.available}`);
  }
  const targetQuantity = Math.min(quantity, stock.available);
  const existing = form.lines.find(item => item.productId === stock.productId);
  const preferredLocationId = selectedLocationId.value ?? stock.locationId ?? null;
  const preferredLocation = findLocation(preferredLocationId);
  const fallbackLabel = stock.locationLabel ?? '';
  if (existing) {
    const original = existing.quantity;
    existing.quantity = Math.min(existing.quantity + targetQuantity, stock.available);
    if (existing.quantity === original && original >= stock.available) {
      ElMessage.warning('已达到该商品的最大可出库数量');
    }
    if (preferredLocation) {
      syncRowLocation(existing, preferredLocation.id);
    } else if (!existing.locationLabel && fallbackLabel) {
      existing.locationLabel = fallbackLabel;
      existing.locationId = null;
    }
    clampQuantity(existing);
  } else {
    const locationCandidate = preferredLocation ?? findLocation(stock.locationId);
    form.lines.push({
      productId: stock.productId,
      sku: stock.sku,
      name: stock.name,
      unit: stock.unit,
      quantity: targetQuantity,
      currentStock: stock.quantity,
      lockedStock: stock.lockedStock,
      available: stock.available,
      locationId: locationCandidate?.id ?? null,
      locationLabel: locationCandidate?.displayLabel ?? fallbackLabel,
      locationCode: locationCandidate?.code ?? stock.locationCode ?? '',
      locationName: locationCandidate?.name ?? stock.locationName ?? ''
    });
  }
  ElMessage.success('已加入出库清单');
  selectedProductId.value = null;
  selectedQuantity.value = 1;
  selectedLocationId.value = null;
};

const clampQuantity = row => {
  const stock = findStock(row.productId);
  const available = stock?.available ?? row.available ?? 0;
  row.available = available;
  if (available <= 0) {
    if (row.quantity > 0) {
      ElMessage.warning('该商品已无可用库存，请调整或移除');
    }
    row.quantity = 0;
    return;
  }
  if (!row.quantity || row.quantity <= 0) {
    row.quantity = 1;
  }
  if (row.quantity > available) {
    row.quantity = available;
    ElMessage.warning('出库数量不能超过可用库存');
  }
};

const removeLine = index => {
  form.lines.splice(index, 1);
};

const formatDateValue = value => {
  if (!value) return '';
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  return date.toISOString().split('T')[0];
};

const buildReferenceNote = () => {
  const segments = [];
  if (form.reference?.trim()) {
    segments.push(form.reference.trim());
  }
  if (form.destination?.trim()) {
    segments.push(`目的地: ${form.destination.trim()}`);
  }
  const dateText = formatDateValue(form.expectedDate);
  if (dateText) {
    segments.push(`计划出库: ${dateText}`);
  }
  return segments.join(' | ') || '前端创建的出库申请';
};

const refreshInventory = async (showMessage = true) => {
  try {
    await fetchInventory();
    syncLinesWithStock();
    if (showMessage) {
      ElMessage.success('库存已刷新');
    }
  } catch (error) {
    console.error(error);
    ElMessage.error('刷新库存失败');
  }
};

const handleSubmit = async () => {
  if (!form.lines.length) {
    ElMessage.error('请至少添加一行出库物料');
    return;
  }
  const invalid = form.lines.find(item => !item.productId || item.quantity <= 0);
  if (invalid) {
    ElMessage.error('请确保所有物料信息有效');
    return;
  }
  const overflows = form.lines.find(item => item.quantity > item.available);
  if (overflows) {
    ElMessage.error('出库数量不能超过可用库存');
    return;
  }
  const payloadLines = form.lines
    .filter(item => Number.isFinite(item.productId) && item.quantity > 0)
    .map(item => ({
      productId: item.productId,
      quantity: item.quantity,
      locationId: item.locationId ?? null
    }));
  if (!payloadLines.length) {
    ElMessage.error('请至少添加一条有效的出库记录');
    return;
  }
  submitting.value = true;
  try {
    await api.submitOutbound({
      reference: buildReferenceNote(),
      lines: payloadLines
    });
    ElMessage.success('出库申请已提交');
    form.reference = '';
    form.destination = '';
    form.expectedDate = '';
    form.lines = [];
    selectedProductId.value = null;
    selectedQuantity.value = 1;
    selectedLocationId.value = null;
    await refreshInventory(false);
  } finally {
    submitting.value = false;
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

.available-note {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
