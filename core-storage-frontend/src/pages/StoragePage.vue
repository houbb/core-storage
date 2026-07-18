<template>
  <div class="storage-page">
    <h1 class="page-title">📁 Unified File Runtime</h1>

    <UploadZone />

    <div class="card" v-if="store.files.length > 0">
      <h2 class="section-title">最近上传</h2>
      <ul class="file-list">
        <li
          v-for="f in store.files"
          :key="f.id"
          class="file-row"
          @click="openDetail(f)"
        >
          <span class="file-row-name">{{ f.filename }}</span>
          <span class="file-row-size">{{ formatSize(f.size) }}</span>
          <span class="badge badge-success">ACTIVE</span>
        </li>
      </ul>
    </div>

    <div class="card empty-state" v-else>
      <p class="zone-sub">还没有上传过文件，拖拽文件到上方区域开始使用</p>
    </div>

    <FileDetailModal
      v-if="selectedFile"
      :file="selectedFile"
      @close="selectedFile = null"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useStorageStore } from '../stores/storage'
import type { StorageFileInfo } from '../api/storage'
import UploadZone from '../components/UploadZone.vue'
import FileDetailModal from '../components/FileDetailModal.vue'

const store = useStorageStore()
const selectedFile = ref<StorageFileInfo | null>(null)

function openDetail(file: StorageFileInfo) {
  selectedFile.value = file
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}
</script>

<style scoped>
.storage-page { max-width: 100%; }

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}

.file-list { list-style: none; }
.file-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  transition: background 0.1s;
}
.file-row:hover { background: var(--bg-secondary); margin: 0 -20px; padding-left: 20px; padding-right: 20px; border-radius: var(--radius-sm); }
.file-row:last-child { border-bottom: none; }
.file-row-name { flex: 1; font-weight: 500; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-row-size { font-size: 11px; color: var(--text-secondary); white-space: nowrap; }

.empty-state { text-align: center; padding: 28px; }
</style>