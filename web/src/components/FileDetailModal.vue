<template>
  <Teleport to="body">
    <div class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-card">
        <div class="modal-header">
          <h3 class="modal-title">文件详情</h3>
          <button class="btn modal-close" @click="$emit('close')">✕</button>
        </div>

        <dl class="detail-list" v-if="file">
          <div class="detail-row">
            <dt>文件名</dt>
            <dd>{{ file.filename }}</dd>
          </div>
          <div class="detail-row">
            <dt>大小</dt>
            <dd>{{ formatSize(file.size) }}</dd>
          </div>
          <div class="detail-row">
            <dt>下载地址</dt>
            <dd class="url-cell">{{ file.downloadUrl }}</dd>
          </div>
        </dl>

        <div class="modal-actions">
          <button class="btn btn-accent" @click="copyId">复制 File ID</button>
          <a class="btn btn-primary" :href="file?.downloadUrl" target="_blank" rel="noopener">下载文件</a>
          <button class="btn btn-danger" @click="handleDelete">删除</button>
        </div>

        <div v-if="copied" class="copy-toast">已复制 ID: {{ file?.id }}</div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { StorageFileInfo } from '../api/storage'
import { useStorageStore } from '../stores/storage'

const props = defineProps<{
  file: StorageFileInfo | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const store = useStorageStore()
const copied = ref(false)

async function copyId() {
  if (!props.file) return
  try {
    await navigator.clipboard.writeText(String(props.file.id))
  } catch {
    // fallback
    const ta = document.createElement('textarea')
    ta.value = String(props.file.id)
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
  }
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

async function handleDelete() {
  if (!props.file) return
  const ok = confirm(`确定删除「${props.file.filename}」吗？`)
  if (!ok) return
  await store.remove(props.file.id)
  emit('close')
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-card {
  background: var(--bg-primary);
  border-radius: var(--radius);
  padding: 24px;
  width: 420px;
  max-width: 90vw;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.modal-title { font-size: 17px; font-weight: 700; }
.modal-close { padding: 4px 10px; font-size: 14px; }

.detail-list { margin-bottom: 16px; }
.detail-row {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}
.detail-row:last-child { border-bottom: none; }
.detail-row dt {
  width: 80px;
  flex-shrink: 0;
  font-size: 11px;
  color: var(--text-secondary);
  font-weight: 500;
}
.detail-row dd { font-size: 13px; word-break: break-all; }
.url-cell { font-family: monospace; font-size: 11px; color: var(--accent); }

.modal-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.copy-toast {
  margin-top: 12px;
  padding: 6px 12px;
  background: var(--success);
  color: #fff;
  border-radius: 10px;
  font-size: 12px;
  text-align: center;
}
</style>