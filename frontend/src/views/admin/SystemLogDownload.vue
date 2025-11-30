<template>
  <div class="system-log">
    <div class="page-header flex-between">
      <h2>系统日志下载</h2>
      <el-button type="primary" @click="handleExportAll" :loading="exporting">
        <el-icon><Download /></el-icon>
        导出全部日志
      </el-button>
    </div>

    <section class="card-section">
      <el-table :data="archives" border stripe v-loading="loading">
        <el-table-column prop="fileName" label="日志文件" min-width="260" />
        <el-table-column label="记录数" width="140" align="center">
          <template #default="{ row }">{{ row.recordCount }} 条</template>
        </el-table-column>
        <el-table-column label="最近记录" width="200">
          <template #default="{ row }">{{ row.generatedAtText || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDownload(row)" :disabled="downloading">
              下载
            </el-button>
            <el-button link type="success" @click="handlePreview(row)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="previewVisible" :title="previewTitle" width="720px">
      <el-table :data="previewLogs" border stripe v-loading="previewLoading">
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="140" />
        <el-table-column prop="action" label="动作" width="160" />
        <el-table-column prop="details" label="详情" min-width="240" show-overflow-tooltip />
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ row.createdAtText }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import dayjs from 'dayjs';
import { ElMessage } from 'element-plus';
import { Download } from '@element-plus/icons-vue';
import { api } from '../../services/api.js';

const archives = ref([]);
const loading = ref(false);
const exporting = ref(false);
const downloading = ref(false);

const previewVisible = ref(false);
const previewLoading = ref(false);
const previewTitle = ref('日志预览');
const previewLogs = ref([]);

const fetchArchives = async () => {
  loading.value = true;
  try {
    const data = await api.listLogArchives({ limit: 60 });
    archives.value = data.map(item => ({
      ...item,
      generatedAtText: item.generatedAtText || (item.lastRecordAt ? dayjs(item.lastRecordAt).format('YYYY-MM-DD HH:mm:ss') : '')
    }));
  } finally {
    loading.value = false;
  }
};

const triggerDownload = (blob, fileName) => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName || `operation-logs-${Date.now()}.csv`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

const handleExportAll = async () => {
  exporting.value = true;
  try {
    const { blob, fileName } = await api.exportOperationLogs({});
    triggerDownload(blob, fileName);
    ElMessage.success('已开始导出全部日志');
  } catch (error) {
    console.error('[system-log] export failed', error);
    ElMessage.error('导出失败，请稍后重试');
  } finally {
    exporting.value = false;
  }
};

const handleDownload = async archive => {
  if (!archive?.date) {
    return;
  }
  downloading.value = true;
  try {
    const { blob, fileName } = await api.downloadLogArchive(archive.date);
    triggerDownload(blob, fileName);
  } catch (error) {
    console.error('[system-log] download failed', error);
    ElMessage.error('下载失败，请稍后重试');
  } finally {
    downloading.value = false;
  }
};

const handlePreview = async archive => {
  if (!archive?.date) return;
  previewVisible.value = true;
  previewTitle.value = `日志预览：${archive.fileName}`;
  previewLoading.value = true;
  try {
    previewLogs.value = await api.previewLogArchive(archive.date, { size: 100 });
  } catch (error) {
    console.error('[system-log] preview failed', error);
    ElMessage.error('加载预览失败');
  } finally {
    previewLoading.value = false;
  }
};

onMounted(fetchArchives);
</script>

<style scoped>
.system-log :deep(.el-table__cell) {
  white-space: nowrap;
}
</style>
