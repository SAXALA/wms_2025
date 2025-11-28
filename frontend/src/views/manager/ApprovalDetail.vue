<template>
  <div>
    <div class="page-header flex-between">
      <h2>审批处理</h2>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item to="/manager/pending-approvals">待我审批</el-breadcrumb-item>
        <el-breadcrumb-item>{{ detail?.id ?? '加载中' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <el-skeleton v-if="loading" rows="4" style="padding: 16px" />
    <template v-else>
      <section class="card-section">
        <div class="flex-between" style="margin-bottom: 16px">
          <div>
            <h3>{{ detail?.type }} - {{ detail?.id }}</h3>
            <p style="color: #64748b">发起人：{{ detail?.applicant }} | 提交时间：{{ detail?.submittedAt }}</p>
          </div>
          <StatusTag v-if="detail" :status="detail.status" />
        </div>
        <el-input
          v-model="remark"
          type="textarea"
          :rows="5"
          placeholder="填写审批意见"
        />
        <div style="margin-top: 16px; text-align: right">
          <el-button @click="handleDecision('REJECTED')" :loading="submitting">驳回</el-button>
          <el-button type="primary" @click="handleDecision('APPROVED')" :loading="submitting">
            通过
          </el-button>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api } from '../../services/api.js';
import StatusTag from '../../components/StatusTag.vue';

const route = useRoute();
const router = useRouter();
const detail = ref(null);
const remark = ref('');
const loading = ref(false);
const submitting = ref(false);

const load = async () => {
  const id = route.params.id;
  if (!id) return;
  loading.value = true;
  try {
    detail.value = await api.fetchApprovalDetail(id);
  } finally {
    loading.value = false;
  }
};

onMounted(load);

const handleDecision = async status => {
  if (!detail.value) return;
  submitting.value = true;
  try {
    await api.processApproval(detail.value.id, { status, remark: remark.value });
    ElMessage.success(status === 'APPROVED' ? '已通过审批' : '已驳回');
    router.push('/manager/pending-approvals');
  } finally {
    submitting.value = false;
  }
};
</script>
