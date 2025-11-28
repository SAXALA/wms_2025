<template>
  <aside :class="['sidebar', { 'sidebar--collapsed': collapsed }]">
    <div class="sidebar__brand">
      <el-icon class="sidebar__brand-icon"><OfficeBuilding /></el-icon>
      <span v-if="!collapsed">WMS 2025</span>
    </div>
    <div class="sidebar__collapse" @click="$emit('toggle')">
      <el-icon><Fold /></el-icon>
    </div>
    <el-menu
      :default-active="activePath"
      class="sidebar__menu"
      :collapse="collapsed"
      router
    >
      <template v-for="section in menu" :key="section.base">
        <el-sub-menu :index="section.base">
          <template #title>
            <el-icon><component :is="section.icon" /></el-icon>
            <span>{{ section.title }}</span>
          </template>
          <el-menu-item
            v-for="item in section.children"
            :key="item.path"
            :index="item.path"
          >
            {{ item.title }}
          </el-menu-item>
        </el-sub-menu>
      </template>
    </el-menu>
  </aside>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { menu } from '../router/menuConfig.js';

const props = defineProps({
  collapsed: {
    type: Boolean,
    default: false
  }
});

defineEmits(['toggle']);

const route = useRoute();
const activePath = computed(() => route.path);
</script>

<style scoped>
.sidebar {
  width: 220px;
  background: #0f172a;
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  min-height: 100vh;
  transition: width 0.2s ease;
}

.sidebar--collapsed {
  width: 64px;
}

.sidebar__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  font-weight: 600;
  font-size: 18px;
}

.sidebar__brand-icon {
  font-size: 22px;
}

.sidebar__collapse {
  text-align: right;
  padding: 0 12px 8px;
  color: #94a3b8;
  cursor: pointer;
}

.sidebar__menu {
  flex: 1;
  border-right: none;
  background: transparent;
}
</style>
