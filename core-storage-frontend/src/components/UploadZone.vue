<template>
  <div class="upload-zone card" :class="{ dragover: isDragover, uploading: activeList.length > 0 }">
    <!-- 空状态 -->
    <div v-if="tasks.length === 0" class="zone-empty">
      <div class="zone-icon">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17,8 12,3 7,8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
      </div>
      <p class="zone-hint">拖拽文件到这里上传</p>
      <p class="zone-sub">或 <button class="link-btn" @click="openPicker">选择文件</button> · 支持 Ctrl+V 粘贴图片</p>
    </div>

    <!-- 上传中/完成列表 -->
    <ul v-if="tasks.length > 0" class="task-list">
      <li v-for="t in tasks" :key="t.id" class="task-item">
        <span class="task-name">{{ t.file.name }}</span>
        <span class="task-size">{{ formatSize(t.file.size) }}</span>
        <div class="task-bar" v-if="t.status === 'uploading'">
          <div class="task-bar-fill" :style="{ width: t.progress + '%' }"></div>
        </div>
        <span v-if="t.status === 'uploading'" class="task-percent">{{ t.progress }}%</span>
        <span v-if="t.status === 'done'" class="badge badge-success">✓</span>
        <span v-if="t.status === 'error'" class="badge badge-danger">失败</span>
        <button v-if="t.status === 'uploading'" class="link-btn" @click="cancelTask(t.id)">取消</button>
        <button v-if="t.status === 'error'" class="link-btn" @click="retryTask(t.id)">重试</button>
      </li>
    </ul>

    <div v-if="tasks.length > 0" class="zone-footer">
      <button class="btn" @click="openPicker">+ 添加文件</button>
      <button class="btn btn-primary" @click="clearDone" v-if="doneCount > 0">清除已完成 ({{ doneCount }})</button>
    </div>

    <input ref="fileInputRef" type="file" multiple hidden @change="onFilesSelected" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useStorageStore } from '../stores/storage'

const store = useStorageStore()

interface UploadTask {
  id: number
  file: File
  status: 'uploading' | 'done' | 'error'
  progress: number
  controller: AbortController
}

let nextId = 1
const tasks = ref<UploadTask[]>([])
const isDragover = ref(false)
const fileInputRef = ref<HTMLInputElement>()

const activeList = computed(() => tasks.value.filter((t) => t.status === 'uploading'))
const doneCount = computed(() => tasks.value.filter((t) => t.status === 'done').length)

function openPicker() {
  fileInputRef.value?.click()
}

function addFiles(files: FileList | File[]) {
  for (const file of Array.from(files)) {
    const task: UploadTask = {
      id: nextId++,
      file,
      status: 'uploading',
      progress: 0,
      controller: new AbortController(),
    }
    tasks.value.push(task)
    startUpload(task)
  }
}

async function startUpload(task: UploadTask) {
  try {
    await store.upload(task.file, (p) => { task.progress = p }, task.controller.signal)
    task.status = 'done'
    task.progress = 100
  } catch (e: any) {
    if (e?.name === 'CanceledError' || e?.code === 'ERR_CANCELED') {
      tasks.value = tasks.value.filter((t) => t.id !== task.id)
    } else {
      task.status = 'error'
    }
  }
}

function cancelTask(id: number) {
  const task = tasks.value.find((t) => t.id === id)
  task?.controller.abort()
}

async function retryTask(id: number) {
  const task = tasks.value.find((t) => t.id === id)
  if (!task) return
  task.status = 'uploading'
  task.progress = 0
  task.controller = new AbortController()
  await startUpload(task)
}

function clearDone() {
  tasks.value = tasks.value.filter((t) => t.status !== 'done')
}

function onFilesSelected(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files) addFiles(input.files)
  input.value = ''
}

// 拖拽事件
function onDragOver(e: DragEvent) { e.preventDefault(); isDragover.value = true }
function onDragLeave() { isDragover.value = false }
function onDrop(e: DragEvent) {
  e.preventDefault()
  isDragover.value = false
  if (e.dataTransfer?.files) addFiles(e.dataTransfer.files)
}

// 粘贴事件
function onPaste(e: ClipboardEvent) {
  const items = e.clipboardData?.items
  if (!items) return
  const imageFiles: File[] = []
  for (const item of Array.from(items)) {
    if (item.kind === 'file') {
      const file = item.getAsFile()
      if (file) imageFiles.push(file)
    }
  }
  if (imageFiles.length > 0) {
    e.preventDefault()
    addFiles(imageFiles)
  }
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

onMounted(() => {
  document.addEventListener('paste', onPaste)
})
onUnmounted(() => {
  document.removeEventListener('paste', onPaste)
})
</script>

<style scoped>
.upload-zone {
  text-align: center;
  padding: 32px 20px;
  transition: border-color 0.2s, background 0.2s;
}
.upload-zone.dragover {
  border-color: var(--accent);
  background: var(--accent-bg);
}
.zone-empty { padding: 12px 0; }
.zone-icon { color: var(--text-secondary); margin-bottom: 12px; }
.zone-hint { font-size: 15px; font-weight: 600; margin-bottom: 6px; }
.zone-sub { font-size: 11px; color: var(--text-secondary); }

.link-btn { background: none; border: none; color: var(--accent); cursor: pointer; font-size: 12px; padding: 2px 6px; }
.link-btn:hover { text-decoration: underline; }

.task-list { list-style: none; text-align: left; }
.task-item {
  display: grid;
  grid-template-columns: 1fr auto auto auto;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.task-item:last-child { border-bottom: none; }
.task-name { font-weight: 500; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-size { font-size: 11px; color: var(--text-secondary); }
.task-bar {
  grid-column: 1 / -1;
  height: 4px;
  background: var(--bg-secondary);
  border-radius: 2px;
  overflow: hidden;
}
.task-bar-fill {
  height: 100%;
  background: var(--accent);
  border-radius: 2px;
  transition: width 0.3s ease;
}
.task-percent { font-size: 11px; color: var(--text-secondary); min-width: 32px; text-align: right; }

.zone-footer {
  margin-top: 16px;
  display: flex;
  gap: 8px;
  justify-content: center;
}
</style>