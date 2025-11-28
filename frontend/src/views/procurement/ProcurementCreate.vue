<template>
  <div>
    <div class="page-header flex-between">
      <h2>采购申请创建</h2>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        <el-icon><Promotion /></el-icon>
        提交申请
      </el-button>
    </div>
    <section class="card-section">
      <h3>商品选择</h3>
      <el-select
        v-model="selectedProductId"
        filterable
        placeholder="请选择商品"
        @change="handleProductSelect"
        style="width: 380px"
      >
        <el-option
          v-for="product in products"
          :key="product.id"
          :label="`${product.name} (${product.sku})`"
          :value="product.id"
        />
      </el-select>
      <el-input-number
        v-model="form.quantity"
        :min="1"
        :step="10"
        placeholder="数量"
        style="margin-left: 16px"
      />
      <el-button type="primary" link style="margin-left: 12px" @click="addLine">
        <el-icon><Plus /></el-icon>
        加入清单
      </el-button>
    </section>
    <section class="card-section">
      <h3>申请明细</h3>
      <el-table :data="form.items" border>
        <el-table-column prop="sku" label="SKU" width="120" />
        <el-table-column prop="name" label="商品名称" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column label="操作" width="100">
          <template #default="{ $index }">
            <el-button type="danger" link @click="removeLine($index)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!form.items.length" description="请选择商品加入清单" />
    </section>
    <section class="card-section">
      <h3>备注信息</h3>
      <el-input
        v-model="form.remark"
        type="textarea"
        :rows="3"
        placeholder="填写采购理由或其他说明"
      />
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '../../services/api.js';

const products = ref([]);
const selectedProductId = ref(null);
const loading = ref(false);

const form = reactive({
  quantity: 1,
  remark: '',
  items: []
});

onMounted(async () => {
  products.value = await api.listProducts();
});

const handleProductSelect = id => {
  if (id) {
    form.quantity = 1;
  }
};

const addLine = () => {
  if (!selectedProductId.value) {
    ElMessage.warning('请选择商品');
    return;
  }
  const product = products.value.find(item => item.id === selectedProductId.value);
  if (!product) return;
  const existing = form.items.find(item => item.id === product.id);
  if (existing) {
    existing.quantity += form.quantity;
  } else {
    form.items.push({ id: product.id, sku: product.sku, name: product.name, quantity: form.quantity });
  }
  ElMessage.success('已加入清单');
};

const removeLine = index => {
  form.items.splice(index, 1);
};

const handleSubmit = async () => {
  if (!form.items.length) {
    ElMessage.error('请至少选择一件商品');
    return;
  }
  loading.value = true;
  try {
    await api.submitPurchase({
      remark: form.remark,
      items: form.items
    });
    ElMessage.success('采购申请提交成功');
    form.items = [];
    form.remark = '';
    selectedProductId.value = null;
  } finally {
    loading.value = false;
  }
};
</script>
