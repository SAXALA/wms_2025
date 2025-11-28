<template>
  <header class="header">
    <div class="header__title">
      <el-icon class="header__title-icon"><Collection /></el-icon>
      <span>仓储运营数字驾驶舱</span>
    </div>
    <div class="header__actions">
      <el-avatar size="small" icon="UserFilled" />
      <span class="header__user">管理员</span>
      <el-tag size="small" type="success">在线</el-tag>
    </div>
  </header>
</template>

<script setup>
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
</style>
