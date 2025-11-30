<template>
  <el-drawer v-model="visible" :title="title" size="40%" destroy-on-close>
    <el-form :model="form" :rules="rules" ref="formRef" label-width="96px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" :disabled="!!form.id" />
      </el-form-item>
      <el-form-item label="姓名" prop="realName">
        <el-input v-model="form.realName" />
      </el-form-item>
      <el-form-item label="部门" prop="department">
        <el-select v-model="form.department" placeholder="请选择部门">
          <el-option label="采购部" value="采购部" />
          <el-option label="仓储部" value="仓储部" />
          <el-option label="物流部" value="物流部" />
          <el-option label="财务部" value="财务部" />
        </el-select>
      </el-form-item>
      <template v-if="!form.id">
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password />
        </el-form-item>
      </template>
      <el-form-item label="角色" prop="roles">
        <el-select v-model="form.roles" multiple placeholder="请选择角色">
          <el-option
            v-for="role in roleOptions"
            :key="role.code"
            :label="role.name ?? role.code"
            :value="role.code"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <div style="text-align: right">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">保存</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  model: {
    type: Object,
    default: () => ({ username: '', realName: '', department: '', status: 'ACTIVE', roles: [] })
  },
  roleOptions: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['update:modelValue', 'submit']);
const visible = ref(false);
const loading = ref(false);
const baseForm = {
  id: null,
  username: '',
  realName: '',
  department: '',
  status: 'ACTIVE',
  roles: [],
  password: '',
  confirmPassword: ''
};

const form = reactive({ ...baseForm });
const formRef = ref();
const validatePassword = (_rule, value, callback) => {
  if (form.id) {
    callback();
    return;
  }
  if (!value || value.length < 8) {
    callback(new Error('密码至少 8 位'));
    return;
  }
  if (value !== form.confirmPassword) {
    callback(new Error('两次密码不一致'));
    return;
  }
  callback();
};

const validateConfirmPassword = (_rule, value, callback) => {
  if (form.id) {
    callback();
    return;
  }
  if (value !== form.password) {
    callback(new Error('两次密码不一致'));
    return;
  }
  callback();
};

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  department: [{ required: true, message: '请选择部门', trigger: 'change' }],
  roles: [{ required: true, message: '请至少选择一个角色', trigger: 'change' }]
};

watch(
  () => props.modelValue,
  val => {
    visible.value = val;
    if (val) {
      Object.assign(form, baseForm, props.model ?? {});
      form.roles = Array.isArray(props.model?.roles) ? [...props.model.roles] : [];
      form.password = '';
      form.confirmPassword = '';
    }
  }
);

watch(visible, val => {
  emit('update:modelValue', val);
});

const title = computed(() => (form.id ? '编辑用户' : '创建用户'));

const handleSubmit = () => {
  formRef.value.validate(async valid => {
    if (!valid) return;
    loading.value = true;
    try {
      const payload = {
        id: form.id,
        username: form.username,
        realName: form.realName,
        department: form.department,
        status: form.status,
        roles: [...form.roles]
      };
      if (!form.id) {
        payload.password = form.password;
      }
      emit('submit', payload);
      visible.value = false;
      Object.assign(form, baseForm);
    } finally {
      loading.value = false;
    }
  });
};
</script>
