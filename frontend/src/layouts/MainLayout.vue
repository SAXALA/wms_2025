<template>
  <RouterView v-if="isBlankLayout" />
  <div v-else class="layout">
    <SidebarMenu :collapsed="collapsed" @toggle="collapsed = !collapsed" />
    <div class="layout__content" :class="{ 'layout__content--full': collapsed }">
      <HeaderBar />
      <main class="layout__main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { RouterView, useRoute } from 'vue-router';
import SidebarMenu from '../components/SidebarMenu.vue';
import HeaderBar from '../components/HeaderBar.vue';

const collapsed = ref(false);
const route = useRoute();

const isBlankLayout = computed(() => route.meta.layout === 'blank');
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
}

.layout__content {
  flex: 1;
  transition: margin-left 0.2s ease;
}

.layout__content--full {
  margin-left: 64px;
}

.layout__main {
  padding: 24px;
}
</style>
