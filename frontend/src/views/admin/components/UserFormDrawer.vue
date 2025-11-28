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
        <el-input v-model="form.department" />
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
import { ElMessage } from 'element-plus';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  model: {
    type: Object,
    default: () => ({ username: '', realName: '', department: '', status: 'ACTIVE' })
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
  status: 'ACTIVE'
};

const form = reactive({ ...baseForm });
const formRef = ref();
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
};

watch(
  () => props.modelValue,
  val => {
    visible.value = val;
    if (val) {
      Object.assign(form, baseForm, props.model ?? {});
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
      emit('submit', { ...form });
      ElMessage.success('保存成功');
      visible.value = false;
      Object.assign(form, baseForm);
    } finally {
      loading.value = false;
    }
  });
};
</script>
