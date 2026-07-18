<template>
  <Teleport to="body">
    <div class="drawer-overlay" @click.self="$emit('close')">
      <div class="drawer-card">
        <!-- 头部 -->
        <div class="drawer-header">
          <h3 class="drawer-title">📄 资源详情</h3>
          <button class="btn drawer-close" @click="$emit('close')">✕</button>
        </div>

        <!-- 加载 -->
        <div v-if="loading" class="loading">加载中...</div>

        <!-- 内容 -->
        <template v-if="meta && !loading">
          <!-- 操作按钮 -->
          <div class="detail-actions">
            <a class="btn btn-accent" :href="meta.downloadUrl" target="_blank" rel="noopener">⬇ 下载</a>
            <button class="btn" @click="copy('uuid')">📋 复制 UUID</button>
            <button class="btn" @click="copy('hash')">📋 复制 Hash</button>
          </div>

          <!-- 左右双栏 -->
          <div class="detail-grid">
            <!-- 左栏：文件信息 -->
            <div class="detail-section">
              <h4>文件信息</h4>
              <dl>
                <div class="dl-row"><dt>原始文件名</dt><dd>{{ meta.originalName }}</dd></div>
                <div class="dl-row"><dt>MIME 类型</dt><dd>{{ meta.mimeType || '-' }}</dd></div>
                <div class="dl-row"><dt>文件大小</dt><dd>{{ formatSize(meta.fileSize) }}</dd></div>
                <div class="dl-row"><dt>扩展名</dt><dd>{{ meta.extension || '-' }}</dd></div>
                <div class="dl-row"><dt>状态</dt><dd><span class="badge" :class="statusBadgeClass(meta.status)">{{ meta.status }}</span></dd></div>
                <div class="dl-row"><dt>引用数</dt><dd>{{ meta.referenceCount }}</dd></div>
              </dl>
            </div>

            <!-- 右栏：元数据 -->
            <div class="detail-section">
              <h4>元数据</h4>
              <dl>
                <div class="dl-row"><dt>UUID</dt><dd class="mono">{{ meta.uuid }}</dd></div>
                <div class="dl-row"><dt>Hash (SHA-256)</dt><dd class="mono">{{ meta.hashSha256 ? meta.hashSha256.substring(0, 16) + '...' : '-' }}</dd></div>
                <div class="dl-row"><dt>Storage Driver</dt><dd>{{ meta.storageDriver }}</dd></div>
                <div class="dl-row"><dt>Storage Key</dt><dd class="mono">{{ meta.storageKey || '-' }}</dd></div>
                <div class="dl-row"><dt>Owner</dt><dd>{{ meta.ownerType ? meta.ownerType + ' / ' + (meta.ownerId || '-') : '-' }}</dd></div>
                <div class="dl-row"><dt>System</dt><dd>{{ meta.systemName || '-' }}</dd></div>
                <div class="dl-row"><dt>Module</dt><dd>{{ meta.moduleName || '-' }}</dd></div>
                <div class="dl-row"><dt>Tags</dt><dd>{{ meta.tags || '-' }}</dd></div>
                <div class="dl-row"><dt>Remark</dt><dd>{{ meta.remark || '-' }}</dd></div>
                <div class="dl-row"><dt>创建时间</dt><dd>{{ formatTime(meta.createTime) }}</dd></div>
              </dl>
            </div>
          </div>

          <!-- 引用列表 -->
          <div class="reference-section">
            <h4>引用关系 ({{ refs.length }})</h4>
            <table class="ref-table" v-if="refs.length > 0">
              <thead>
                <tr>
                  <th>System</th>
                  <th>Module</th>
                  <th>Business Type</th>
                  <th>Business ID</th>
                  <th>创建时间</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="ref in refs" :key="ref.id">
                  <td>{{ ref.systemName || '-' }}</td>
                  <td>{{ ref.moduleName || '-' }}</td>
                  <td>{{ ref.businessType || '-' }}</td>
                  <td>{{ ref.businessId || '-' }}</td>
                  <td>{{ formatTime(ref.createTime) }}</td>
                  <td>
                    <button class="link-btn" @click="handleDeleteRef(ref.id, meta.uuid)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
            <p v-else class="empty-hint">该资源暂无业务引用</p>
          </div>

          <!-- Toast -->
          <div v-if="copied" class="copy-toast">{{ copied }}</div>
        </template>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useMetadataStore } from '../stores/metadata'
import type { StorageMetadataItem, StorageReferenceItem } from '../api/storage'

const props = defineProps<{ uuid: string }>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'refreshed'): void
}>()

const store = useMetadataStore()
const meta = ref<StorageMetadataItem | null>(null)
const refs = ref<StorageReferenceItem[]>([])
const loading = ref(true)
const copied = ref('')

async function load() {
  loading.value = true
  try {
    meta.value = await store.fetchDetail(props.uuid)
    refs.value = await store.fetchReferences(props.uuid)
  } finally {
    loading.value = false
  }
}

async function handleDeleteRef(id: number, metadataUuid: string) {
  if (!confirm('确定删除该引用吗？')) return
  await store.removeReference(id, metadataUuid)
  refs.value = await store.fetchReferences(props.uuid)
  // 刷新详情（引用数可能变化）
  meta.value = await store.fetchDetail(props.uuid)
  emit('refreshed')
}

async function copy(type: string) {
  const text = type === 'uuid' ? props.uuid : (meta.value?.hashSha256 || '')
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
  }
  copied.value = type === 'uuid' ? 'UUID 已复制' : 'Hash 已复制'
  setTimeout(() => { copied.value = '' }, 2000)
}

function statusBadgeClass(status: string) {
  switch (status) {
    case 'ACTIVE': return 'badge-success'
    case 'REFERENCED': return 'badge-referenced'
    case 'UNREFERENCED': return 'badge-unreferenced'
    case 'SOFT_DELETED': return 'badge-danger'
    default: return ''
  }
}

function formatSize(bytes: number) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

function formatTime(t: string) {
  if (!t) return '-'
  const d = new Date(t)
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

watch(() => props.uuid, load, { immediate: true })
</script>

<style scoped>
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  z-index: 1000;
}
.drawer-card {
  background: var(--bg-primary);
  width: 680px;
  max-width: 95vw;
  height: 100vh;
  overflow-y: auto;
  padding: 24px;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
}
.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.drawer-title { font-size: 17px; font-weight: 700; }
.drawer-close { padding: 4px 10px; font-size: 14px; }

.loading { text-align: center; padding: 40px; color: var(--text-secondary); }

.detail-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}
@media (max-width: 600px) {
  .detail-grid { grid-template-columns: 1fr; }
}

.detail-section h4 {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--border);
}
.dl-row {
  display: flex;
  padding: 6px 0;
  border-bottom: 1px solid var(--border);
  font-size: 12px;
}
.dl-row:last-child { border-bottom: none; }
.dl-row dt {
  width: 110px;
  flex-shrink: 0;
  color: var(--text-secondary);
  font-weight: 500;
}
.dl-row dd { word-break: break-all; }
.mono { font-family: monospace; font-size: 11px; }

.badge-referenced { background: #e8f0fe; color: var(--accent); }
.badge-unreferenced { background: #fff3cd; color: #856404; }

.reference-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}
.reference-section h4 {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}
.ref-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.ref-table th {
  text-align: left;
  padding: 6px 8px;
  font-weight: 600;
  font-size: 11px;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border);
}
.ref-table td {
  padding: 8px;
  border-bottom: 1px solid var(--border);
}
.link-btn { background: none; border: none; color: var(--danger); cursor: pointer; font-size: 11px; padding: 2px 4px; }
.link-btn:hover { text-decoration: underline; }
.empty-hint { font-size: 12px; color: var(--text-secondary); padding: 16px 0; }

.copy-toast {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 8px 20px;
  background: #333;
  color: #fff;
  border-radius: 10px;
  font-size: 12px;
  z-index: 2000;
}
</style>