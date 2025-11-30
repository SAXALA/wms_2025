<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <div class="login-header">
        <el-icon class="login-icon"><Lock /></el-icon>
        <h2>仓储管理系统登录</h2>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleSubmit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleSubmit">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api } from '../../services/api.js';
import { useAuth } from '../../stores/auth.js';

const router = useRouter();
const route = useRoute();
const { resolveDefaultRoute } = useAuth();

const formRef = ref();
const loading = ref(false);

const form = reactive({
  username: '',
  password: ''
});

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const handleSubmit = () => {
  if (!formRef.value) return;
  formRef.value.validate(async valid => {
    if (!valid) return;
    loading.value = true;
    try {
      await api.login({
        username: form.username,
        password: form.password
      });
      ElMessage.success('登录成功');
      const redirect = route.query.redirect ? decodeURIComponent(route.query.redirect) : null;
      const target = redirect && redirect !== '/login' ? redirect : resolveDefaultRoute();
      router.replace(target);
    } catch (error) {
      console.error(error);
      ElMessage.error(error?.response?.data?.message ?? error.message ?? '登录失败');
    } finally {
      loading.value = false;
    }
  });
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 32px;
}

.login-card {
  width: 360px;
  padding: 24px 28px;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.35);
}

.login-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.login-header h2 {
  margin: 0;
  font-weight: 600;
  color: #0f172a;
}

.login-icon {
  font-size: 24px;
  color: #2563eb;
}

</style>
