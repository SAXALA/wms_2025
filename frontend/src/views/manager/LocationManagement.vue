<template>
  <div>
    <div class="page-header flex-between">
      <h2>库位管理</h2>
      <div class="header-actions">
        <el-switch
          v-model="filters.includeInactive"
          active-text="显示停用库位"
        />
        <el-input
          v-model="filters.keyword"
          placeholder="搜索编码 / 名称"
          clearable
          class="keyword-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新增库位
        </el-button>
      </div>
    </div>

    <section class="card-section">
      <el-table :data="filteredLocations" stripe border :loading="loading">
        <el-table-column prop="code" label="编码" width="140" />
        <el-table-column label="名称" min-width="200">
          <template #default="{ row }">
            <span>{{ row.name }}</span>
            <el-tag v-if="!row.active" size="small" type="warning" class="inline-tag">已停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">
              {{ row.active ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最近更新" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              type="warning"
              @click="handleToggle(row)"
            >{{ row.active ? '停用' : '启用' }}</el-button>
            <el-popconfirm
              title="确认删除该库位？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              confirm-button-type="danger"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !filteredLocations.length" description="暂无库位" />
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" @closed="resetForm">
      <el-form label-width="88px">
        <el-form-item label="编码">
          <el-input v-model="form.code" maxlength="64" placeholder="如 A01-01" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="128" placeholder="库位名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            maxlength="256"
            :autosize="{ minRows: 2, maxRows: 4 }"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.active" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../../services/api.js';

const loading = ref(false);
const saving = ref(false);
const locations = ref([]);
const dialogVisible = ref(false);
const editingLocationId = ref(null);

const filters = reactive({
  keyword: '',
  includeInactive: false
});

const form = reactive({
  code: '',
  name: '',
  description: '',
  active: true
});

const dialogTitle = computed(() => (editingLocationId.value ? '编辑库位' : '新增库位'));

const loadLocations = async () => {
  loading.value = true;
  try {
    locations.value = await api.listLocations({ includeInactive: true });
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadLocations();
});

const filteredLocations = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase();
  return locations.value
    .filter(item => (filters.includeInactive ? true : item.active))
    .filter(item => {
      if (!keyword) return true;
      return [item.code, item.name].some(field => field?.toLowerCase().includes(keyword));
    });
});

const openCreate = () => {
  editingLocationId.value = null;
  resetForm();
  dialogVisible.value = true;
};

const openEdit = location => {
  editingLocationId.value = location.id;
  form.code = location.code;
  form.name = location.name;
  form.description = location.description ?? '';
  form.active = location.active;
  dialogVisible.value = true;
};

const resetForm = () => {
  form.code = '';
  form.name = '';
  form.description = '';
  form.active = true;
};

const submitForm = async () => {
  if (!form.code.trim()) {
    ElMessage.warning('请输入库位编码');
    return;
  }
  if (!form.name.trim()) {
    ElMessage.warning('请输入库位名称');
    return;
  }

  saving.value = true;
  try {
    const payload = {
      code: form.code.trim(),
      name: form.name.trim(),
      description: form.description?.trim() || '',
      active: form.active
    };
    if (editingLocationId.value) {
      await api.updateLocation(editingLocationId.value, payload);
      ElMessage.success('库位已更新');
    } else {
      await api.createLocation(payload);
      ElMessage.success('库位已创建');
    }
    dialogVisible.value = false;
    await loadLocations();
  } finally {
    saving.value = false;
  }
};

const handleToggle = async location => {
  try {
    await ElMessageBox.confirm(
      `确认${location.active ? '停用' : '启用'}库位「${location.code}」？`,
      '提示',
      {
        type: 'warning'
      }
    );
  } catch (error) {
    return;
  }
  await api.toggleLocationStatus(location.id, !location.active);
  ElMessage.success(`库位已${location.active ? '停用' : '启用'}`);
  await loadLocations();
};

const handleDelete = async location => {
  await api.deleteLocation(location.id);
  ElMessage.success('库位已删除');
  await loadLocations();
};

watch(
  () => filters.includeInactive,
  value => {
    if (!value && !locations.value.some(item => item.active)) {
      filters.keyword = '';
    }
  }
);
</script>

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.keyword-input {
  width: 240px;
}

.inline-tag {
  margin-left: 8px;
}
</style>
