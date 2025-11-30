<template>
  <header class="header">
    <div class="header__title">
      <el-icon class="header__title-icon"><Collection /></el-icon>
      <span>仓储运营数字驾驶舱</span>
    </div>
    <div class="header__actions">
      <el-avatar size="small" icon="UserFilled" />
      <div class="header__user-block">
        <span class="header__user">{{ displayName }}</span>
        <span class="header__role">{{ roleLabel }}</span>
      </div>
      <el-tag size="small" :type="statusTagType">{{ statusText }}</el-tag>
      <el-button link type="danger" @click="handleLogout">退出登录</el-button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api } from '../services/api.js';
import { useAuth } from '../stores/auth.js';

const ROLE_LABELS = {
  PURCHASER: '采购员',
  OPERATOR: '仓库管理员',
  MANAGER: '仓库经理',
  ADMIN: '系统管理员'
};

const router = useRouter();
const auth = useAuth();

const displayName = computed(() => auth.state.username || '未登录');

const roleLabel = computed(() => {
  if (!auth.state.roles?.length) {
    return '未分配角色';
  }
  const role = auth.state.roles.find(item => ROLE_LABELS[item]) ?? auth.state.roles[0];
  return ROLE_LABELS[role] ?? role;
});

const statusTagType = computed(() => (auth.isAuthenticated.value ? 'success' : 'info'));
const statusText = computed(() => (auth.isAuthenticated.value ? '在线' : '离线'));

const handleLogout = () => {
  api.logout();
  ElMessage.success('已退出登录');
  router.replace('/login');
};
</script>

<style scoped>
.header {
  height: 60px;
  background-color: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 5px 20px rgba(15, 23, 42, 0.06);
}

.header__title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.header__title-icon {
  font-size: 20px;
  color: #2563eb;
}

.header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #475569;
}

.header__user {
  font-weight: 500;
}

.header__user-block {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}

.header__role {
  font-size: 12px;
  color: #94a3b8;
}
</style>
