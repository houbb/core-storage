<template>
  <div class="resource-page">
    <h1 class="page-title">🧩 资源中心</h1>

    <div class="resource-layout">
      <!-- 左侧分类导航 -->
      <aside class="sidebar card">
        <h3 class="sidebar-title">资源分类</h3>
        <ul class="category-list">
          <li
            v-for="cat in categories"
            :key="cat.value"
            class="category-item"
            :class="{ active: filters.resourceType === cat.value }"
            @click="selectCategory(cat.value)"
          >
            <span class="cat-icon">{{ cat.icon }}</span>
            <span class="cat-label">{{ cat.label }}</span>
          </li>
        </ul>

        <h3 class="sidebar-title" style="margin-top:20px">可见性</h3>
        <ul class="category-list">
          <li
            v-for="v in visibilities"
            :key="v.value"
            class="category-item"
            :class="{ active: filters.visibility === v.value }"
            @click="selectVisibility(v.value)"
          >
            {{ v.label }}
          </li>
        </ul>
      </aside>

      <!-- 右侧资源列表 -->
      <main class="main-content">
        <!-- 搜索栏 -->
        <div class="search-bar">
          <input
            v-model="filters.keyword"
            class="search-input"
            placeholder="搜索资源名称 / UUID / 描述..."
            @keyup.enter="doSearch(1)"
          />
          <select v-model="filters.status" class="filter-select">
            <option value="">全部状态</option>
            <option value="READY">READY</option>
            <option value="REFERENCED">REFERENCED</option>
            <option value="UPLOADING">UPLOADING</option>
            <option value="DELETED">DELETED</option>
          </select>
          <button class="btn btn-primary" @click="doSearch(1)">搜索</button>
          <!-- 视图切换 -->
          <button class="btn view-toggle" @click="viewMode = viewMode === 'table' ? 'card' : 'table'">
            {{ viewMode === 'table' ? '📇 卡片' : '📋 表格' }}
          </button>
        </div>

        <!-- 加载状态 -->
        <div v-if="store.loading" class="loading">加载中...</div>

        <!-- 结果 -->
        <div class="card" v-if="!store.loading">
          <div class="result-header">
            <span class="result-count">共 {{ store.total }} 条资源</span>
            <span class="result-page">第 {{ store.currentPage }} / {{ store.totalPages || 1 }} 页</span>
          </div>

          <!-- 表格视图 -->
          <table v-if="viewMode === 'table' && store.items.length > 0" class="resource-table">
            <thead>
              <tr>
                <th>资源名称</th>
                <th>类型</th>
                <th>分类</th>
                <th>Owner</th>
                <th>可见性</th>
                <th>状态</th>
                <th>引用</th>
                <th>创建时间</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in store.items"
                :key="item.resourceUuid"
                class="resource-row"
                @click="openDetail(item.resourceUuid)"
              >
                <td class="col-name">{{ item.resourceName }}</td>
                <td><span class="badge badge-secondary">{{ item.resourceType }}</span></td>
                <td>{{ formatLabel(item.category) }}</td>
                <td>{{ item.ownerType ? item.ownerType + '/' + item.ownerId : '-' }}</td>
                <td><span class="badge badge-visibility">{{ item.visibility }}</span></td>
                <td><span class="badge" :class="statusBadgeClass(item.status)">{{ item.status }}</span></td>
                <td>{{ item.referenceCount }}</td>
                <td class="col-time">{{ formatTime(item.createTime) }}</td>
              </tr>
            </tbody>
          </table>

          <!-- 卡片视图 -->
          <div v-if="viewMode === 'card' && store.items.length > 0" class="card-grid">
            <div
              v-for="item in store.items"
              :key="item.resourceUuid"
              class="resource-card"
              @click="openDetail(item.resourceUuid)"
            >
              <div class="card-top">
                <span class="card-type-badge">{{ item.resourceType }}</span>
                <span class="badge" :class="statusBadgeClass(item.status)">{{ item.status }}</span>
              </div>
              <h4 class="card-name">{{ item.resourceName }}</h4>
              <p class="card-category">{{ formatLabel(item.category) }}</p>
              <div class="card-meta">
                <span>{{ item.ownerType ? item.ownerType + '/' + item.ownerId : 'No Owner' }}</span>
                <span>{{ item.visibility }}</span>
              </div>
              <div class="card-tags" v-if="item.tags && item.tags.length > 0">
                <span class="tag-pill" v-for="t in item.tags" :key="t">{{ t }}</span>
              </div>
              <div class="card-footer">
                <span class="card-refs">引用 {{ item.referenceCount }}</span>
                <span class="card-time">{{ formatTime(item.createTime) }}</span>
              </div>
            </div>
          </div>

          <div class="empty-state" v-if="store.items.length === 0">
            <p>没有找到匹配的资源</p>
          </div>

          <!-- 分页 -->
          <div class="pagination" v-if="store.totalPages > 1">
            <button class="btn" :disabled="store.currentPage <= 1" @click="doSearch(store.currentPage - 1)">上一页</button>
            <span class="page-info">{{ store.currentPage }} / {{ store.totalPages }}</span>
            <button class="btn" :disabled="store.currentPage >= store.totalPages" @click="doSearch(store.currentPage + 1)">下一页</button>
          </div>
        </div>
      </main>
    </div>

    <!-- 详情抽屉 -->
    <ResourceDetailDrawer
      v-if="detailUuid"
      :uuid="detailUuid"
      @close="detailUuid = null"
      @refreshed="doSearch(store.currentPage)"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useResourceStore } from '../stores/resource'
import ResourceDetailDrawer from '../components/ResourceDetailDrawer.vue'

const store = useResourceStore()

const filters = reactive({
  keyword: '',
  resourceType: '',
  category: '',
  visibility: '',
  status: '',
  tag: '',
  sort: 'createTime',
  order: 'desc',
})

const viewMode = ref<'table' | 'card'>('table')
const detailUuid = ref<string | null>(null)

const categories = [
  { value: 'IMAGE', label: '🖼 图片', icon: '🖼' },
  { value: 'VIDEO', label: '🎬 视频', icon: '🎬' },
  { value: 'AUDIO', label: '🎵 音频', icon: '🎵' },
  { value: 'DOCUMENT', label: '📄 文档', icon: '📄' },
  { value: 'ARCHIVE', label: '📦 压缩包', icon: '📦' },
  { value: 'PLUGIN', label: '🔌 插件', icon: '🔌' },
  { value: 'TEMPLATE', label: '📋 模板', icon: '📋' },
  { value: 'MODEL', label: '🤖 AI模型', icon: '🤖' },
  { value: 'BACKUP', label: '💾 备份', icon: '💾' },
  { value: 'EXPORT', label: '📤 导出', icon: '📤' },
  { value: 'ICON', label: '🔣 图标', icon: '🔣' },
  { value: 'FONT', label: '🔤 字体', icon: '🔤' },
  { value: 'DATASET', label: '📊 数据集', icon: '📊' },
  { value: 'OTHER', label: '📎 其他', icon: '📎' },
]

const visibilities = [
  { value: 'PUBLIC', label: '🌐 公开' },
  { value: 'LOGIN', label: '🔐 登录可见' },
  { value: 'PRIVATE', label: '🔒 私有' },
  { value: 'SYSTEM', label: '⚙ 系统' },
]

function selectCategory(type: string) {
  filters.resourceType = filters.resourceType === type ? '' : type
  doSearch(1)
}

function selectVisibility(v: string) {
  filters.visibility = filters.visibility === v ? '' : v
  doSearch(1)
}

async function doSearch(page: number) {
  await store.search({
    keyword: filters.keyword || undefined,
    resourceType: filters.resourceType || undefined,
    visibility: filters.visibility || undefined,
    status: filters.status || undefined,
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

onMounted(() => {
  doSearch(1)
})
</script>

<style scoped>
.resource-page { max-width: 100%; }

.resource-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.sidebar {
  width: 200px;
  flex-shrink: 0;
  padding: 16px;
}
.sidebar-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-secondary);
}
.category-list { list-style: none; }
.category-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.1s;
}
.category-item:hover { background: var(--bg-secondary); }
.category-item.active {
  background: var(--accent-bg);
  color: var(--accent);
  font-weight: 600;
}
.cat-icon { font-size: 14px; }

.main-content { flex: 1; min-width: 0; }

.search-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
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
.view-toggle { white-space: nowrap; }

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

/* 表格视图 */
.resource-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.resource-table th {
  text-align: left;
  padding: 8px 10px;
  font-weight: 600;
  font-size: 11px;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border);
}
.resource-table td {
  padding: 10px;
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
}
.resource-row { cursor: pointer; transition: background 0.1s; }
.resource-row:hover { background: var(--bg-secondary); }
.col-name { font-weight: 500; max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-time { font-size: 11px; color: var(--text-secondary); white-space: nowrap; }

/* 卡片视图 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}
.resource-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 14px;
  cursor: pointer;
  transition: box-shadow 0.15s, background 0.15s;
}
.resource-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  background: var(--bg-secondary);
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-type-badge {
  font-size: 10px;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-bg);
  padding: 2px 6px;
  border-radius: 4px;
}
.card-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-category {
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.card-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}
.card-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.tag-pill {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 10px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
}
.card-footer {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: var(--text-secondary);
  border-top: 1px solid var(--border);
  padding-top: 8px;
}

/* Badge 扩展 */
.badge-secondary { background: #f0f0f0; color: #555; }
.badge-visibility { background: #e8f0fe; color: var(--accent); }
.badge-warning { background: #fff3cd; color: #856404; }
.badge-referenced { background: #e8f0fe; color: var(--accent); }

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

@media (max-width: 700px) {
  .resource-layout { flex-direction: column; }
  .sidebar { width: 100%; }
}
</style>