<template>
  <div>
    <div class="page-header flex-between">
      <h2>用户管理</h2>
      <el-button type="primary" @click="openForm()">
        <el-icon><User /></el-icon>
        新建用户
      </el-button>
    </div>
    <section class="card-section">
      <div class="filters">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索用户名/姓名"
          style="width: 240px"
          clearable
        />
        <el-select v-model="filters.status" placeholder="状态" style="width: 160px">
          <el-option label="全部" value="ALL" />
          <el-option label="启用" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="loadUsers" :loading="loading">查询</el-button>
      </div>
      <el-table :data="users" border stripe>
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="department" label="部门" />
        <el-table-column label="角色">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="`${row.id}-${role}`" style="margin-right: 6px">
              {{ roleDict[role] ?? role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="ACTIVE"
              inactive-value="DISABLED"
              @change="status => changeStatus(row.id, status)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="140" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button link type="success" @click="openRoles(row)">分配角色</el-button>
            <el-button link type="danger">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          layout="total, prev, pager, next"
          :total="pagination.total"
          :page-size="pagination.pageSize"
          v-model:current-page="pagination.page"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <UserFormDrawer
      v-model="formVisible"
      :model="currentUser"
      :role-options="roleOptions"
      @submit="saveUser"
    />
    <RoleAssignmentDrawer
      v-model="roleVisible"
      :value="currentRoles"
      :roles="roleOptions"
      @change="assignRole"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '../../services/api.js';
import UserFormDrawer from './components/UserFormDrawer.vue';
import RoleAssignmentDrawer from './components/RoleAssignmentDrawer.vue';

const filters = reactive({ keyword: '', status: 'ALL' });
const pagination = reactive({ page: 1, pageSize: 20, total: 0 });
const users = ref([]);
const loading = ref(false);
const formVisible = ref(false);
const roleVisible = ref(false);
const currentUser = ref(null);
const currentRoles = ref([]);
const roleOptions = ref([]);
const roleDict = reactive({});

const buildRoleDict = roles => {
  roles.forEach(role => {
    roleDict[role.code] = role.name;
  });
};

const loadRoles = async () => {
  roleOptions.value = await api.listRoles();
  buildRoleDict(roleOptions.value);
};

const loadUsers = async () => {
  loading.value = true;
  try {
    const res = await api.listAdminUsers({
      ...filters,
      page: pagination.page,
      pageSize: pagination.pageSize
    });
    users.value = res.items;
    pagination.total = res.total;
    pagination.page = res.page;
    pagination.pageSize = res.pageSize;
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await Promise.all([loadRoles(), loadUsers()]);
});

const openForm = record => {
  currentUser.value = record ? { ...record } : null;
  formVisible.value = true;
};

const saveUser = async payload => {
  await api.saveAdminUser(payload);
  ElMessage.success('用户信息已保存');
  loadUsers();
};

const openRoles = record => {
  currentUser.value = record;
  currentRoles.value = [...(record.roles ?? [])];
  roleVisible.value = true;
};

const assignRole = async roles => {
  if (!currentUser.value) return;
  await api.assignRoles(currentUser.value.id, roles);
  ElMessage.success('角色已更新');
  roleVisible.value = false;
  loadUsers();
};

const changeStatus = async (id, status) => {
  await api.updateUserStatus(id, status);
  ElMessage.success('状态已更新');
};

const handlePageChange = page => {
  pagination.page = page;
  loadUsers();
};
</script>

<style scoped>
.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
