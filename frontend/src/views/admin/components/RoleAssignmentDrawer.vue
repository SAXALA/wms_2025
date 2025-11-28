<template>
  <el-drawer v-model="visible" title="角色分配" size="30%" destroy-on-close>
    <el-checkbox-group v-model="selectedRoles" class="role-list">
      <el-checkbox v-for="role in roles" :key="role.code" :label="role.code">
        {{ role.name }}
      </el-checkbox>
    </el-checkbox-group>
    <template #footer>
      <div style="text-align: right">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="confirm">保存</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  roles: { type: Array, default: () => [] },
  value: { type: Array, default: () => [] }
});

const emit = defineEmits(['update:modelValue', 'change']);
const visible = ref(false);
const selectedRoles = ref([]);

watch(
  () => props.modelValue,
  val => {
    visible.value = val;
    if (val) {
      selectedRoles.value = [...props.value];
    }
  }
);

watch(visible, val => emit('update:modelValue', val));

const confirm = () => {
  emit('change', selectedRoles.value);
  visible.value = false;
};
</script>

<style scoped>
.role-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
