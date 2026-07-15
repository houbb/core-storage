import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { StorageFileInfo } from '../api/storage'
import { uploadFile, getFileInfo, deleteFile } from '../api/storage'

export const useStorageStore = defineStore('storage', () => {
  const files = ref<StorageFileInfo[]>([])

  async function upload(file: File, onProgress?: (p: number) => void, signal?: AbortSignal) {
    const result = await uploadFile(file, { onProgress, signal })
    files.value.unshift(result)
    return result
  }

  async function refreshInfo(id: number) {
    const info = await getFileInfo(id)
    const idx = files.value.findIndex((f) => f.id === id)
    if (idx !== -1) {
      files.value[idx] = info
    }
  }

  async function remove(id: number) {
    await deleteFile(id)
    files.value = files.value.filter((f) => f.id !== id)
  }

  return { files, upload, refreshInfo, remove }
})
