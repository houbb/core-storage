<script setup lang="ts">
import { ref } from 'vue'
import StoragePage from './pages/StoragePage.vue'
import MetadataPage from './pages/MetadataPage.vue'
import ResourcePage from './pages/ResourcePage.vue'
import LifecyclePage from './pages/LifecyclePage.vue'

const activeTab = ref<'upload' | 'metadata' | 'resource' | 'lifecycle'>('upload')
</script>

<template>
  <div class="app-tabs">
    <button
      class="tab-btn"
      :class="{ active: activeTab === 'upload' }"
      @click="activeTab = 'upload'"
    >
      📤 上传
    </button>
    <button
      class="tab-btn"
      :class="{ active: activeTab === 'metadata' }"
      @click="activeTab = 'metadata'"
    >
      📋 元数据
    </button>
    <button
      class="tab-btn"
      :class="{ active: activeTab === 'resource' }"
      @click="activeTab = 'resource'"
    >
      🧩 资源中心
    </button>
    <button
      class="tab-btn"
      :class="{ active: activeTab === 'lifecycle' }"
      @click="activeTab = 'lifecycle'"
    >
      🔄 生命周期
    </button>
  </div>

  <StoragePage v-if="activeTab === 'upload'" />
  <MetadataPage v-else-if="activeTab === 'metadata'" />
  <ResourcePage v-else-if="activeTab === 'resource'" />
  <LifecyclePage v-else />
</template>

<style scoped>
.app-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  background: var(--bg-primary);
  border-radius: var(--radius-sm);
  padding: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.tab-btn {
  flex: 1;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.tab-btn:hover { color: var(--text-primary); }
.tab-btn.active {
  background: var(--accent);
  color: #fff;
}
</style>