<template>
  <div class="metadata-page">
    <h1 class="page-title">📋 Metadata Center</h1>

    <!-- 搜索栏 -->
    <div class="search-bar card">
      <div class="search-row">
        <input
          v-model="filters.keyword"
          class="search-input"
          placeholder="搜索文件名 / UUID / 资源名..."
          @keyup.enter="doSearch(1)"
        />
        <select v-model="filters.status" class="filter-select">
          <option value="">全部状态</option>
          <option value="ACTIVE">ACTIVE</option>
          <option value="REFERENCED">REFERENCED</option>
          <option value="UNREFERENCED">UNREFERENCED</option>
          <option value="SOFT_DELETED">SOFT_DELETED</option>
        </select>
        <select v-model="filters.mimeType" class="filter-select">
          <option value="">全部类型</option>
          <option value="image">🖼 图片</option>
          <option value="video">🎬 视频</option>
          <option value="audio">🎵 音频</option>
          <option value="pdf">📄 PDF</option>
          <option value="zip">📦 压缩包</option>
          <option value="text">📝 文本</option>
        </select>
        <button class="btn btn-primary" @click="doSearch(1)">搜索</button>
        <button class="btn" @click="showAdvanced = !showAdvanced">
          {{ showAdvanced ? '收起' : '高级过滤' }}
        </button>
      </div>

      <!-- 高级过滤 -->
      <div v-if="showAdvanced" class="advanced-filters">
        <div class="filter-grid">
          <div class="filter-field">
            <label>Owner Type</label>
            <input v-model="filters.ownerType" class="filter-input" placeholder="如 user" />
          </div>
          <div class="filter-field">
            <label>Owner ID</label>
            <input v-model="filters.ownerId" class="filter-input" placeholder="如 1001" />
          </div>
          <div class="filter-field">
            <label>System</label>
            <input v-model="filters.system" class="filter-input" placeholder="如 core-user" />
          </div>
          <div class="filter-field">
            <label>Module</label>
            <input v-model="filters.module" class="filter-input" placeholder="如 avatar" />
          </div>
          <div class="filter-field">
            <label>Tag</label>
            <input v-model="filters.tag" class="filter-input" placeholder="如 important" />
          </div>
          <div class="filter-field">
            <label>Hash (SHA-256)</label>
            <input v-model="filters.hash" class="filter-input" placeholder="精确匹配" />
          </div>
          <div class="filter-field">
            <label>开始时间</label>
            <input v-model="filters.startTime" type="datetime-local" class="filter-input" />
          </div>
          <div class="filter-field">
            <label>结束时间</label>
            <input v-model="filters.endTime" type="datetime-local" class="filter-input" />
          </div>
        </div>
        <div class="filter-row">
          <label>排序：</label>
          <select v-model="filters.sort" class="filter-select-sm">
            <option value="createTime">创建时间</option>
            <option value="size">文件大小</option>
            <option value="name">文件名</option>
          </select>
          <select v-model="filters.order" class="filter-select-sm">
            <option value="desc">降序</option>
            <option value="asc">升序</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="store.loading" class="loading">加载中...</div>

    <!-- 结果表格 -->
    <div class="card" v-if="!store.loading">
      <div class="result-header">
        <span class="result-count">共 {{ store.total }} 条</span>
        <span class="result-page">第 {{ store.currentPage }} / {{ store.totalPages || 1 }} 页</span>
      </div>

      <table class="metadata-table" v-if="store.items.length > 0">
        <thead>
          <tr>
            <th>UUID</th>
            <th>文件名</th>
            <th>类型</th>
            <th>大小</th>
            <th>状态</th>
            <th>引用</th>
            <th>创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="item in store.items"
            :key="item.uuid"
            class="metadata-row"
            @click="openDetail(item.uuid)"
          >
            <td class="col-uuid" :title="item.uuid">{{ item.uuid.substring(0, 8) }}...</td>
            <td class="col-name">{{ item.originalName }}</td>
            <td>{{ item.mimeType || '-' }}</td>
            <td>{{ formatSize(item.fileSize) }}</td>
            <td>
              <span class="badge" :class="statusBadgeClass(item.status)">{{ item.status }}</span>
            </td>
            <td>{{ item.referenceCount }}</td>
            <td class="col-time">{{ formatTime(item.createTime) }}</td>
          </tr>
        </tbody>
      </table>

      <div class="empty-state" v-else>
        <p>没有找到匹配的元数据</p>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="store.totalPages > 1">
        <button class="btn" :disabled="store.currentPage <= 1" @click="doSearch(store.currentPage - 1)">上一页</button>
        <span class="page-info">{{ store.currentPage }} / {{ store.totalPages }}</span>
        <button class="btn" :disabled="store.currentPage >= store.totalPages" @click="doSearch(store.currentPage + 1)">下一页</button>
      </div>
    </div>

    <!-- 详情抽屉 -->
    <MetadataDetailDrawer
      v-if="detailUuid"
      :uuid="detailUuid"
      @close="detailUuid = null"
      @refreshed="doSearch(store.currentPage)"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useMetadataStore } from '../stores/metadata'
import MetadataDetailDrawer from '../components/MetadataDetailDrawer.vue'

const store = useMetadataStore()

const filters = reactive({
  keyword: '',
  mimeType: '',
  status: '',
  hash: '',
  ownerType: '',
  ownerId: '',
  system: '',
  module: '',
  tag: '',
  startTime: '',
  endTime: '',
  sort: 'createTime',
  order: 'desc',
})

const showAdvanced = ref(false)
const detailUuid = ref<string | null>(null)

async function doSearch(page: number) {
  await store.search({
    keyword: filters.keyword || undefined,
    mimeType: filters.mimeType || undefined,
    status: filters.status || undefined,
    hash: filters.hash || undefined,
    ownerType: filters.ownerType || undefined,
    ownerId: filters.ownerId || undefined,
    system: filters.system || undefined,
    module: filters.module || undefined,
    tag: filters.tag || undefined,
    startTime: filters.startTime ? new Date(filters.startTime).toISOString() : undefined,
    endTime: filters.endTime ? new Date(filters.endTime).toISOString() : undefined,
    sort: filters.sort,
    order: filters.order,
    page,
    size: 20,
  })
}

function openDetail(uuid: string) {
  detailUuid.value = uuid
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

onMounted(() => {
  doSearch(1)
})
</script>

<style scoped>
.metadata-page { max-width: 100%; }

.search-bar { padding: 16px 20px; }
.search-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.search-input {
  flex: 1;
  min-width: 200px;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  background: var(--bg-primary);
  color: var(--text-primary);
}
.search-input:focus { outline: none; border-color: var(--accent); }
.filter-select {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  background: var(--bg-primary);
}

.advanced-filters {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}
.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 10px;
}
@media (max-width: 700px) {
  .filter-grid { grid-template-columns: repeat(2, 1fr); }
}
.filter-field { display: flex; flex-direction: column; gap: 4px; }
.filter-field label { font-size: 11px; color: var(--text-secondary); }
.filter-input {
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: 4px;
  font-size: 12px;
  background: var(--bg-primary);
}
.filter-input:focus { outline: none; border-color: var(--accent); }
.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}
.filter-select-sm {
  padding: 4px 8px;
  border: 1px solid var(--border);
  border-radius: 4px;
  font-size: 12px;
  background: var(--bg-primary);
}

.loading {
  text-align: center;
  padding: 32px;
  color: var(--text-secondary);
  font-size: 14px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}
.result-count { font-size: 12px; color: var(--text-secondary); }
.result-page { font-size: 12px; color: var(--text-secondary); }

.metadata-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.metadata-table th {
  text-align: left;
  padding: 8px 10px;
  font-weight: 600;
  font-size: 11px;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border);
}
.metadata-table td {
  padding: 10px;
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
}
.metadata-row { cursor: pointer; transition: background 0.1s; }
.metadata-row:hover { background: var(--bg-secondary); }
.col-uuid { font-family: monospace; font-size: 11px; }
.col-name { font-weight: 500; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-time { font-size: 11px; color: var(--text-secondary); white-space: nowrap; }

.badge-referenced {
  background: #e8f0fe;
  color: var(--accent);
}
.badge-unreferenced {
  background: #fff3cd;
  color: #856404;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}
.page-info { font-size: 12px; color: var(--text-secondary); }

.empty-state { text-align: center; padding: 40px; color: var(--text-secondary); font-size: 13px; }
</style>