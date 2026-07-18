<script setup lang="ts">
import { ref, onMounted } from 'vue'

const props = defineProps<{ uuid: string }>()

const lifecycleStage = ref('ACTIVE')
const holds = ref<any[]>([])
const loading = ref(true)

async function loadData() {
  try {
    const [stateRes, holdsRes] = await Promise.all([
      fetch(`/api/v1/storage/lifecycle/resources/${props.uuid}/state`),
      fetch(`/api/v1/storage/lifecycle/resources/${props.uuid}/holds`),
    ])
    const state = await stateRes.json()
    lifecycleStage.value = state.lifecycleStage || 'ACTIVE'
    holds.value = await holdsRes.json()
  } catch (e) {
    console.error('Failed to load lifecycle data', e)
  } finally {
    loading.value = false
  }
}

const stages = ['ACTIVE', 'WARM', 'COLD', 'ARCHIVED', 'DELETED']
const stageColors: Record<string, string> = {
  ACTIVE: '#22c55e', WARM: '#f59e0b', COLD: '#3b82f6', ARCHIVED: '#8b5cf6', DELETED: '#ef4444',
}

async function archiveNow() {
  if (!confirm('确认立即归档该资源？')) return
  await fetch(`/api/v1/storage/lifecycle/resources/${props.uuid}/archive`, { method: 'POST' })
  loadData()
}

async function restoreNow() {
  if (!confirm('确认恢复该资源到 ACTIVE？')) return
  await fetch(`/api/v1/storage/lifecycle/resources/${props.uuid}/restore`, { method: 'POST' })
  loadData()
}

const addingHold = ref(false)
const holdForm = ref({ holdType: 'LEGAL', reason: '', operatorId: 'admin' })

async function placeHold() {
  try {
    const res = await fetch(`/api/v1/storage/lifecycle/resources/${props.uuid}/hold`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...holdForm.value, expireTime: null }),
    })
    if (res.ok) { addingHold.value = false; loadData() }
  } catch (e) { console.error(e) }
}

async function releaseHold() {
  if (!confirm('解除所有 Hold？')) return
  await fetch(`/api/v1/storage/lifecycle/resources/${props.uuid}/hold?operatorId=admin`, { method: 'DELETE' })
  loadData()
}

const activeHolds = () => holds.value.filter((h: any) => !h.released)

onMounted(loadData)
</script>

<template>
  <div class="lifecycle-panel" v-if="!loading">
    <h4>🔄 生命周期</h4>

    <!-- Stage timeline -->
    <div class="timeline">
      <div
        v-for="stage in stages"
        :key="stage"
        class="stage-dot-wrapper"
      >
        <div
          class="stage-dot"
          :class="{ active: stage === lifecycleStage, passed: stages.indexOf(stage) < stages.indexOf(lifecycleStage) }"
          :style="{ borderColor: stageColors[stage], background: stage === lifecycleStage || stages.indexOf(stage) < stages.indexOf(lifecycleStage) ? stageColors[stage] : '#fff' }"
        ></div>
        <div class="stage-label" :class="{ current: stage === lifecycleStage }">{{ stage }}</div>
      </div>
    </div>

    <div class="stage-info">
      <span>当前阶段：</span>
      <span class="badge" :style="{ background: stageColors[lifecycleStage], color: '#fff' }">{{ lifecycleStage }}</span>
    </div>

    <!-- Hold status -->
    <div class="hold-section">
      <div class="hold-header">
        <span>⛓ Legal Hold: </span>
        <span v-if="activeHolds().length > 0" class="hold-status active">已冻结 ({{ activeHolds().length }})</span>
        <span v-else class="hold-status inactive">无</span>
      </div>

      <div v-if="activeHolds().length > 0" class="hold-list">
        <div v-for="h in activeHolds()" :key="h.id" class="hold-item">
          <span class="hold-type">{{ h.holdType }}</span>
          <span class="hold-reason">{{ h.reason || '-' }}</span>
        </div>
        <button class="btn-sm btn-sm-danger" @click="releaseHold">解除 Hold</button>
      </div>
    </div>

    <!-- Actions -->
    <div class="actions-section">
      <button class="btn-sm" @click="archiveNow" :disabled="lifecycleStage === 'ARCHIVED' || lifecycleStage === 'DELETED'">📦 归档</button>
      <button class="btn-sm" @click="restoreNow" :disabled="lifecycleStage !== 'ARCHIVED'">🔄 恢复</button>
      <button class="btn-sm" @click="addingHold = true" :disabled="activeHolds().length > 0">⛓ Hold</button>
    </div>

    <!-- Add Hold mini-form -->
    <div v-if="addingHold" class="hold-form">
      <select v-model="holdForm.holdType" class="form-select">
        <option value="LEGAL">LEGAL</option>
        <option value="AUDIT">AUDIT</option>
        <option value="INVESTIGATION">INVESTIGATION</option>
      </select>
      <input v-model="holdForm.reason" class="form-input-mini" placeholder="原因..." />
      <button class="btn-sm btn-sm-primary" @click="placeHold">确认</button>
      <button class="btn-sm" @click="addingHold = false">取消</button>
    </div>
  </div>
</template>

<style scoped>
.lifecycle-panel { padding: 12px 0; }

.timeline { display: flex; align-items: flex-start; justify-content: space-between; margin: 12px 0 8px; position: relative; }
.timeline::before {
  content: '';
  position: absolute; top: 8px; left: 0; right: 0; height: 2px;
  background: #e5e7eb; z-index: 0;
}
.stage-dot-wrapper { display: flex; flex-direction: column; align-items: center; gap: 4px; z-index: 1; }
.stage-dot {
  width: 16px; height: 16px; border-radius: 50%; border: 2px solid #d1d5db;
  background: #fff; box-shadow: 0 0 0 3px #fff;
}
.stage-dot.active { width: 20px; height: 20px; box-shadow: 0 0 0 3px rgba(0,0,0,0.1); }
.stage-label { font-size: 10px; color: var(--text-secondary); font-weight: 500; }
.stage-label.current { font-weight: 700; color: var(--text-primary); }

.stage-info { margin: 8px 0; font-size: 12px; }
.stage-info .badge { padding: 2px 8px; border-radius: 4px; font-size: 11px; }

.hold-section { margin-top: 10px; padding-top: 10px; border-top: 1px solid var(--border); }
.hold-header { font-size: 12px; margin-bottom: 4px; }
.hold-status.active { color: #ef4444; font-weight: 600; }
.hold-status.inactive { color: #22c55e; }
.hold-list { margin-top: 4px; }
.hold-item { display: flex; gap: 8px; font-size: 11px; padding: 3px 0; }
.hold-type { font-weight: 600; color: #ef4444; }
.hold-reason { color: var(--text-secondary); }

.actions-section { margin-top: 10px; display: flex; gap: 6px; }
.btn-sm {
  padding: 4px 10px; font-size: 11px; border: 1px solid var(--border); border-radius: 4px;
  background: #fff; cursor: pointer;
}
.btn-sm:hover:not(:disabled) { background: var(--bg-secondary); }
.btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-sm-danger { color: #dc2626; border-color: #fecaca; }
.btn-sm-primary { background: var(--accent); color: #fff; border-color: var(--accent); }

.hold-form {
  margin-top: 8px; display: flex; gap: 6px; align-items: center;
  padding: 8px; background: #fef2f2; border-radius: 6px;
}
.form-select, .form-input-mini {
  padding: 4px 8px; font-size: 11px; border: 1px solid #ddd; border-radius: 4px;
}
.form-input-mini { width: 120px; }
</style>
