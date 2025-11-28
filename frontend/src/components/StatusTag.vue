<template>
  <el-tag :type="mappedType" size="small">{{ label }}</el-tag>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  status: {
    type: String,
    required: true
  }
});

const statuses = {
  DRAFT: { label: '草稿', type: 'info' },
  SUBMITTED: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  EXECUTING: { label: '执行中', type: 'warning' },
  DONE: { label: '已完成', type: 'success' }
};

const mappedType = computed(() => statuses[props.status]?.type ?? 'info');
const label = computed(() => statuses[props.status]?.label ?? props.status);
</script>
