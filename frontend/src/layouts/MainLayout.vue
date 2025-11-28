<template>
  <div class="layout">
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
import { ref } from 'vue';
import { RouterView } from 'vue-router';
import SidebarMenu from '../components/SidebarMenu.vue';
import HeaderBar from '../components/HeaderBar.vue';

const collapsed = ref(false);
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
