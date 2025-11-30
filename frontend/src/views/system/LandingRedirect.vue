<template>
  <div class="landing-redirect">
    <el-skeleton rows="3" animated />
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuth } from '../../stores/auth.js';

const router = useRouter();
const route = useRoute();
const auth = useAuth();

const navigate = target => {
  if (route.fullPath === target) {
    return;
  }
  router.replace(target);
};

onMounted(() => {
  if (!auth.isAuthenticated.value) {
    navigate('/login');
    return;
  }
  const target = auth.resolveDefaultRoute();
  navigate(target);
});
</script>

<style scoped>
.landing-redirect {
  padding: 80px;
}
</style>
