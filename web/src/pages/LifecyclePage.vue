<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import LifecyclePolicyModal from '../components/LifecyclePolicyModal.vue'

interface DashboardData {
  stageCounts: Record<string, number>
  activeHolds: number
  pendingTasks: number
  totalPolicies: number
}

interface Policy {
  id: number
  policyName: string
  resourceType: string
  category: string
  activeDays: number
  warmDays: number
  coldDays: number
  archiveDays: number
  deleteDays: number
  enabled: boolean
  description: string | null
  createTime: string
}

const activeTab = ref<'dashboard' | 'policies'>('dashboard')
const dashboard = ref<DashboardData | null>(null)
const policies = ref<Policy[]>([])
const showPolicyModal = ref(false)
const editingPolicy = ref<Policy | null>(null)

async function loadDashboard() {
  try {
    const res = await fetch('/api/v1/storage/lifecycle/dashboard')
    dashboard.value = await res.json()
  } catch (e) {
    console.error('Failed to load dashboard', e)
  }
}

async function loadPolicies() {
  try {
    const res = await fetch('/api/v1/storage/lifecycle/policies')
    policies.value = await res.json()
  } catch (e) {
    console.error('Failed to load policies', e)
  }
}

function openCreatePolicy() {
  editingPolicy.value = null
  showPolicyModal.value = true
}

function openEditPolicy(p: Policy) {
  editingPolicy.value = p
  showPolicyModal.value = true
}

async function deletePolicy(id: number) {
  if (!confirm('确定要删除该策略？')) return
  try {
    await fetch(`/api/v1/storage/lifecycle/policies/${id}`, { method: 'DELETE' })
    await loadPolicies()
    await loadDashboard()
  } catch (e) {
    console.error('Failed to delete policy', e)
  }
}

function onPolicySaved() {
  showPolicyModal.value = false
  loadPolicies()
  loadDashboard()
}

function timelineSummary(p: Policy): string {
  const parts: string[] = []
  if (p.deleteDays > 0) parts.push(`${p.deleteDays}d→DELETE`)
  else if (p.archiveDays > 0) parts.push(`${p.archiveDays}d→ARCHIVE`)
  else if (p.coldDays > 0) parts.push(`${p.coldDays}d→COLD`)
  else if (p.warmDays > 0) parts.push(`${p.warmDays}d→WARM`)
  else parts.push('永久保留')
  return parts.join(' → ')
}

const stageColors: Record<string, string> = {
  ACTIVE: '#22c55e',
  WARM: '#f59e0b',
  COLD: '#3b82f6',
  ARCHIVED: '#8b5cf6',
  DELETED: '#ef4444',
}

onMounted(() => {
  loadDashboard()
  loadPolicies()
})
</script>

<template>
  <div class="lifecycle-page">
    <h1 class="page-title">🔄 生命周期治理</h1>

    <div class="tabs">
      <button class="tab-btn" :class="{ active: activeTab === 'dashboard' }" @click="activeTab = 'dashboard'">📊 仪表盘</button>
      <button class="tab-btn" :class="{ active: activeTab === 'policies' }" @click="activeTab = 'policies'">📋 策略管理</button>
    </div>

    <!-- Dashboard Tab -->
    <div v-if="activeTab === 'dashboard' && dashboard" class="tab-content">
      <div class="stats-grid">
        <div class="stat-card" v-for="(count, stage) in dashboard.stageCounts" :key="stage" :style="{ borderTopColor: stageColors[stage] || '#999' }">
          <div class="stat-count">{{ count }}</div>
          <div class="stat-label">{{ stage }}</div>
        </div>
      </div>

      <div class="summary-row">
        <div class="summary-card">
          <div class="summary-count">{{ dashboard.activeHolds }}</div>
          <div class="summary-label">⛓ Active Holds</div>
        </div>
        <div class="summary-card">
          <div class="summary-count">{{ dashboard.pendingTasks }}</div>
          <div class="summary-label">⏳ Pending Tasks</div>
        </div>
        <div class="summary-card">
          <div class="summary-count">{{ dashboard.totalPolicies }}</div>
          <div class="summary-label">📐 Total Policies</div>
        </div>
      </div>
    </div>

    <!-- Policies Tab -->
    <div v-if="activeTab === 'policies'" class="tab-content">
      <div class="toolbar">
        <button class="btn btn-primary" @click="openCreatePolicy">+ 新建策略</button>
      </div>

      <table class="policy-table" v-if="policies.length > 0">
        <thead>
          <tr>
            <th>策略名称</th>
            <th>资源类型</th>
            <th>分类</th>
            <th>时间线</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in policies" :key="p.id">
            <td class="col-name">{{ p.policyName }}</td>
            <td><span class="badge badge-secondary">{{ p.resourceType }}</span></td>
            <td>{{ p.category }}</td>
            <td class="col-timeline">{{ timelineSummary(p) }}</td>
            <td><span class="badge" :class="p.enabled ? 'badge-success' : 'badge-danger'">{{ p.enabled ? '启用' : '禁用' }}</span></td>
            <td class="col-actions">
              <button class="btn-sm" @click="openEditPolicy(p)">编辑</button>
              <button class="btn-sm btn-sm-danger" @click="deletePolicy(p.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="empty-state" v-else>
        <p>尚无生命周期策略</p>
        <p class="hint">点击"+ 新建策略"创建第一个策略</p>
      </div>
    </div>

    <!-- Policy Modal -->
    <LifecyclePolicyModal
      v-if="showPolicyModal"
      :policy="editingPolicy"
      @close="showPolicyModal = false"
      @saved="onPolicySaved"
    />
  </div>
</template>

<style scoped>
.lifecycle-page { max-width: 100%; }

.tabs {
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
.tab-btn.active { background: var(--accent); color: #fff; }

.tab-content { background: var(--bg-primary); border-radius: var(--radius-sm); padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }

.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 12px; margin-bottom: 20px; }
.stat-card {
  padding: 16px;
  border-radius: 8px;
  border-top: 3px solid #ccc;
  background: #fafafa;
  text-align: center;
}
.stat-count { font-size: 28px; font-weight: 700; color: var(--text-primary); }
.stat-label { font-size: 12px; color: var(--text-secondary); margin-top: 4px; }

.summary-row { display: flex; gap: 12px; }
.summary-card { flex: 1; padding: 14px; border-radius: 8px; background: #fafafa; text-align: center; }
.summary-count { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.summary-label { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }

.toolbar { margin-bottom: 12px; }

.policy-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.policy-table th { text-align: left; padding: 8px 10px; font-weight: 600; font-size: 11px; color: var(--text-secondary); border-bottom: 1px solid var(--border); }
.policy-table td { padding: 10px; border-bottom: 1px solid var(--border); }
.col-name { font-weight: 500; }
.col-timeline { font-size: 12px; color: var(--text-secondary); }
.col-actions { white-space: nowrap; }

.btn-sm { padding: 4px 10px; font-size: 11px; border: 1px solid var(--border); border-radius: 4px; background: #fff; cursor: pointer; }
.btn-sm:hover { background: var(--bg-secondary); }
.btn-sm-danger { color: #dc2626; border-color: #fecaca; }
.btn-sm-danger:hover { background: #fef2f2; }

.badge-secondary { background: #f0f0f0; color: #555; }

.empty-state { text-align: center; padding: 40px; color: var(--text-secondary); }
.hint { font-size: 12px; margin-top: 4px; }
</style>
