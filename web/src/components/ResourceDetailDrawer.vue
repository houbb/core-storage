<template>
  <Teleport to="body">
    <div class="drawer-overlay" @click.self="$emit('close')">
      <div class="drawer-card">
        <!-- 头部 -->
        <div class="drawer-header">
          <h3 class="drawer-title">📦 资源详情</h3>
          <button class="btn drawer-close" @click="$emit('close')">✕</button>
        </div>

        <!-- 加载 -->
        <div v-if="loading" class="loading">加载中...</div>

        <!-- 内容 -->
        <template v-if="resource && !loading">
          <!-- 操作按钮 -->
          <div class="detail-actions">
            <a class="btn btn-accent" :href="resource.downloadUrl" target="_blank" rel="noopener">⬇ 下载</a>
            <button class="btn" @click="copy('uuid')">📋 复制 UUID</button>
            <button class="btn btn-danger" @click="handleDelete" v-if="resource.status !== 'DELETED'">🗑 删除</button>
          </div>

          <!-- 左右双栏 -->
          <div class="detail-grid">
            <!-- 左栏：资源信息 -->
            <div class="detail-section">
              <h4>资源信息</h4>
              <dl>
                <div class="dl-row"><dt>资源名称</dt><dd>{{ resource.resourceName }}</dd></div>
                <div class="dl-row"><dt>资源类型</dt><dd><span class="badge badge-secondary">{{ resource.resourceType }}</span></dd></div>
                <div class="dl-row"><dt>分类</dt><dd>{{ formatLabel(resource.category) }}</dd></div>
                <div class="dl-row"><dt>描述</dt><dd>{{ resource.description || '-' }}</dd></div>
                <div class="dl-row"><dt>可见性</dt><dd><span class="badge badge-visibility">{{ resource.visibility }}</span></dd></div>
                <div class="dl-row"><dt>状态</dt><dd><span class="badge" :class="statusBadgeClass(resource.status)">{{ resource.status }}</span></dd></div>
                <div class="dl-row"><dt>引用数</dt><dd>{{ resource.referenceCount }}</dd></div>
              </dl>
            </div>

            <!-- 右栏：关联信息 -->
            <div class="detail-section">
              <h4>关联信息</h4>
              <dl>
                <div class="dl-row"><dt>Resource UUID</dt><dd class="mono">{{ resource.resourceUuid }}</dd></div>
                <div class="dl-row"><dt>Metadata UUID</dt><dd class="mono">{{ resource.metadataUuid }}</dd></div>
                <div class="dl-row"><dt>Owner</dt><dd>{{ resource.ownerType ? resource.ownerType + ' / ' + (resource.ownerId || '-') : '-' }}</dd></div>
                <div class="dl-row"><dt>创建时间</dt><dd>{{ formatTime(resource.createTime) }}</dd></div>
                <div class="dl-row"><dt>更新时间</dt><dd>{{ formatTime(resource.updateTime) }}</dd></div>
              </dl>
            </div>
          </div>

          <!-- 标签 -->
          <div class="tag-section" v-if="resource.tags && resource.tags.length > 0">
            <h4>标签</h4>
            <div class="tag-list">
              <span class="tag-pill" v-for="t in resource.tags" :key="t">{{ t }}</span>
            </div>
          </div>

          <!-- 扩展属性 -->
          <div class="property-section">
            <h4>扩展属性 ({{ propsCount }})</h4>
            <table class="prop-table" v-if="propsCount > 0">
              <thead>
                <tr><th>Key</th><th>Value</th></tr>
              </thead>
              <tbody>
                <tr v-for="(v, k) in properties" :key="k">
                  <td class="mono">{{ k }}</td>
                  <td>{{ v }}</td>
                </tr>
              </tbody>
            </table>
            <p v-else class="empty-hint">暂无扩展属性</p>
          </div>

          <!-- Toast -->
          <div v-if="copied" class="copy-toast">{{ copied }}</div>
        </template>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useResourceStore } from '../stores/resource'
import type { ResourceItem } from '../api/storage'

const props = defineProps<{ uuid: string }>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'refreshed'): void
}>()

const store = useResourceStore()
const resource = ref<ResourceItem | null>(null)
const properties = ref<Record<string, string>>({})
const loading = ref(true)
const copied = ref('')

const propsCount = computed(() => Object.keys(properties.value).length)

async function load() {
  loading.value = true
  try {
    resource.value = await store.fetchDetail(props.uuid)
    properties.value = await store.fetchProperties(props.uuid)
  } finally {
    loading.value = false
  }
}

async function handleDelete() {
  if (!resource.value) return
  if (!confirm(`确定删除资源「${resource.value.resourceName}」吗？`)) return
  await store.doDelete(props.uuid)
  emit('refreshed')
  emit('close')
}

async function copy(type: string) {
  const text = type === 'uuid' ? props.uuid : (resource.value?.metadataUuid || '')
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
  copied.value = type === 'uuid' ? 'UUID 已复制' : 'Metadata UUID 已复制'
  setTimeout(() => { copied.value = '' }, 2000)
}

function statusBadgeClass(status: string) {
  switch (status) {
    case 'READY': return 'badge-success'
    case 'REFERENCED': return 'badge-referenced'
    case 'UPLOADING': return 'badge-warning'
    case 'DELETED': return 'badge-danger'
    default: return ''
  }
}

function formatLabel(str: string | null) {
  if (!str) return '-'
  const map: Record<string, string> = {
    AVATAR: '头像', ATTACHMENT: '附件', PLUGIN: '插件', TEMPLATE: '模板',
    LOGO: 'Logo', BANNER: 'Banner', BACKUP: '备份', DATASET: '数据集',
    PROMPT: 'Prompt', MODEL: '模型', OTHER: '其他'
  }
  return map[str] || str
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
  width: 100px;
  flex-shrink: 0;
  color: var(--text-secondary);
  font-weight: 500;
}
.dl-row dd { word-break: break-all; }
.mono { font-family: monospace; font-size: 11px; }

.badge-secondary { background: #f0f0f0; color: #555; }
.badge-visibility { background: #e8f0fe; color: var(--accent); }
.badge-warning { background: #fff3cd; color: #856404; }
.badge-referenced { background: #e8f0fe; color: var(--accent); }

.tag-section, .property-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}
.tag-section h4, .property-section h4 {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}
.tag-list {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.tag-pill {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 10px;
  background: var(--accent-bg);
  color: var(--accent);
}

.prop-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.prop-table th {
  text-align: left;
  padding: 6px 8px;
  font-weight: 600;
  font-size: 11px;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border);
}
.prop-table td {
  padding: 6px 8px;
  border-bottom: 1px solid var(--border);
}
.empty-hint { font-size: 12px; color: var(--text-secondary); padding: 8px 0; }

.btn-danger {
  background: #fff1f0;
  color: #cf1322;
  border: 1px solid #ffa39e;
}
.btn-danger:hover { background: #ffd8d2; }

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