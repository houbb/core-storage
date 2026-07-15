import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1/storage',
  timeout: 30000,
})

export interface StorageFileInfo {
  id: number
  downloadUrl: string
  filename: string
  size: number
}

export interface UploadOptions {
  onProgress?: (percent: number) => void
  signal?: AbortSignal
}

export async function uploadFile(
  file: File,
  options: UploadOptions = {}
): Promise<StorageFileInfo> {
  const form = new FormData()
  form.append('file', file)

  const { data } = await api.post<StorageFileInfo>('/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (options.onProgress && e.total) {
        options.onProgress(Math.round((e.loaded * 100) / e.total))
      }
    },
    signal: options.signal,
  })

  return data
}

export async function getFileInfo(id: number): Promise<StorageFileInfo> {
  const { data } = await api.get<StorageFileInfo>(`/file/${id}/info`)
  return data
}

export function getDownloadUrl(id: number): string {
  return `/api/v1/storage/file/${id}`
}

export async function deleteFile(id: number): Promise<void> {
  await api.delete(`/file/${id}`)
}
