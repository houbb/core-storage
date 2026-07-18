<script setup lang="ts">
import { ref, computed } from 'vue'

interface Policy {
  id?: number
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
}

const props = defineProps<{ policy: Policy | null }>()
const emit = defineEmits(['close', 'saved'])

const form = ref({
  policyName: props.policy?.policyName || '',
  resourceType: props.policy?.resourceType || 'OTHER',
  category: props.policy?.category || 'OTHER',
  activeDays: props.policy?.activeDays ?? 0,
  warmDays: props.policy?.warmDays ?? 0,
  coldDays: props.policy?.coldDays ?? 0,
  archiveDays: props.policy?.archiveDays ?? 0,
  deleteDays: props.policy?.deleteDays ?? 0,
  description: props.policy?.description || '',
})

const isEdit = computed(() => !!props.policy)
const title = computed(() => isEdit.value ? '编辑策略' : '新建策略')

const resourceTypes = ['IMAGE','VIDEO','AUDIO','DOCUMENT','ARCHIVE','PLUGIN','TEMPLATE','MODEL','MODEL_3D','BACKUP','EXPORT','ICON','FONT','DATASET','OTHER']
const categories = ['AVATAR','ATTACHMENT','PLUGIN','TEMPLATE','LOGO','BANNER','BACKUP','DATASET','PROMPT','MODEL','OTHER']

async function submit() {
  const url = isEdit.value
    ? `/api/v1/storage/lifecycle/policies/${props.policy!.id}`
    : '/api/v1/storage/lifecycle/policies'
  const method = isEdit.value ? 'PUT' : 'POST'

  const body: Record<string, any> = { ...form.value }
  if (!isEdit.value) {
    // create: send all fields
  } else {
    // update: only send changed
  }

  try {
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    if (!res.ok) {
      const err = await res.json()
      alert(err.detail || 'Save failed')
      return
    }
    emit('saved')
  } catch (e) {
    console.error('Save policy failed', e)
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-card">
        <div class="modal-header">
          <h3>{{ title }}</h3>
          <button class="btn-close" @click="$emit('close')">✕</button>
        </div>

        <div class="modal-body">
          <div class="form-row">
            <label>策略名称</label>
            <input v-model="form.policyName" class="form-input" placeholder="如 Export-7Days" />
          </div>

          <div class="form-row form-row-2col">
            <div>
              <label>资源类型</label>
              <select v-model="form.resourceType" class="form-input">
                <option v-for="t in resourceTypes" :key="t" :value="t">{{ t }}</option>
              </select>
            </div>
            <div>
              <label>分类</label>
              <select v-model="form.category" class="form-input">
                <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
              </select>
            </div>
          </div>

          <h4 class="section-label">保留天数（0 = 不进入该阶段）</h4>
          <div class="days-grid">
            <div class="form-inline">
              <label>Active</label>
              <input v-model.number="form.activeDays" type="number" min="0" class="form-input form-input-sm" />
            </div>
            <div class="form-inline">
              <label>Warm</label>
              <input v-model.number="form.warmDays" type="number" min="0" class="form-input form-input-sm" />
            </div>
            <div class="form-inline">
              <label>Cold</label>
              <input v-model.number="form.coldDays" type="number" min="0" class="form-input form-input-sm" />
            </div>
            <div class="form-inline">
              <label>Archive</label>
              <input v-model.number="form.archiveDays" type="number" min="0" class="form-input form-input-sm" />
            </div>
            <div class="form-inline">
              <label>Delete</label>
              <input v-model.number="form.deleteDays" type="number" min="0" class="form-input form-input-sm" />
            </div>
          </div>

          <div class="form-row">
            <label>描述</label>
            <input v-model="form.description" class="form-input" placeholder="策略说明..." />
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn" @click="$emit('close')">取消</button>
          <button class="btn btn-primary" @click="submit">保存</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.35);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modal-card {
  background: #fff; border-radius: 10px; width: 520px; max-height: 90vh; overflow-y: auto;
  box-shadow: 0 8px 30px rgba(0,0,0,0.12);
}
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #eee; }
.modal-header h3 { margin: 0; font-size: 15px; }
.btn-close { border: none; background: none; font-size: 16px; cursor: pointer; color: #999; }
.modal-body { padding: 20px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 8px; padding: 12px 20px; border-top: 1px solid #eee; }

.form-row { margin-bottom: 14px; }
.form-row label { display: block; font-size: 12px; font-weight: 600; color: #666; margin-bottom: 4px; }
.form-input { width: 100%; padding: 7px 10px; border: 1px solid #ddd; border-radius: 5px; font-size: 13px; box-sizing: border-box; }
.form-input:focus { outline: none; border-color: var(--accent); }
.form-input-sm { width: 70px; text-align: center; }

.form-row-2col { display: flex; gap: 12px; }
.form-row-2col > div { flex: 1; }

.section-label { font-size: 12px; font-weight: 600; color: #666; margin: 16px 0 8px; }

.days-grid { display: flex; gap: 10px; flex-wrap: wrap; }
.form-inline { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.form-inline label { font-size: 10px; font-weight: 600; color: #999; }
</style>
