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

export interface UploadMetadata {
  ownerType?: string
  ownerId?: string
  system?: string
  module?: string
  businessType?: string
  businessId?: string
  tags?: string
  remark?: string
}

export async function uploadFile(
  file: File,
  options: UploadOptions = {},
  metadata: UploadMetadata = {}
): Promise<StorageFileInfo> {
  const form = new FormData()
  form.append('file', file)
  if (metadata.ownerType) form.append('ownerType', metadata.ownerType)
  if (metadata.ownerId) form.append('ownerId', metadata.ownerId)
  if (metadata.system) form.append('system', metadata.system)
  if (metadata.module) form.append('module', metadata.module)
  if (metadata.businessType) form.append('businessType', metadata.businessType)
  if (metadata.businessId) form.append('businessId', metadata.businessId)
  if (metadata.tags) form.append('tags', metadata.tags)
  if (metadata.remark) form.append('remark', metadata.remark)

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

// ---- P1 Metadata APIs ----

export interface StorageMetadataItem {
  id: number
  uuid: string
  resourceName: string
  originalName: string
  extension: string
  mimeType: string
  fileSize: number
  hashSha256: string
  storageDriver: string
  storageKey: string
  relativePath: string
  storageType: string
  ownerType: string
  ownerId: string
  systemName: string
  moduleName: string
  tags: string
  remark: string
  status: string
  referenceCount: number
  createTime: string
  updateTime: string
  downloadUrl: string
}

export interface SearchResult<T> {
  items: T[]
  page: number
  size: number
  total: number
  totalPages: number
}

export interface SearchParams {
  keyword?: string
  mimeType?: string
  status?: string
  hash?: string
  ownerType?: string
  ownerId?: string
  system?: string
  module?: string
  tag?: string
  startTime?: string
  endTime?: string
  sort?: string
  order?: string
  page?: number
  size?: number
}

export async function searchMetadata(params: SearchParams): Promise<SearchResult<StorageMetadataItem>> {
  const { data } = await api.get<SearchResult<StorageMetadataItem>>('/metadata/search', {
    params: {
      ...params,
      startTime: params.startTime || undefined,
      endTime: params.endTime || undefined,
    },
  })
  return data
}

export async function getMetadata(uuid: string): Promise<StorageMetadataItem> {
  const { data } = await api.get<StorageMetadataItem>(`/metadata/${uuid}`)
  return data
}

export interface StorageReferenceItem {
  id: number
  metadataUuid: string
  systemName: string
  moduleName: string
  businessType: string
  businessId: string
  createTime: string
}

export async function getReferences(metadataUuid: string): Promise<StorageReferenceItem[]> {
  const { data } = await api.get<StorageReferenceItem[]>(`/metadata/${metadataUuid}/references`)
  return data
}

export async function createReference(req: {
  metadataUuid: string
  system: string
  module: string
  businessType: string
  businessId: string
}): Promise<StorageReferenceItem> {
  const { data } = await api.post<StorageReferenceItem>('/reference', req)
  return data
}

export async function deleteReference(id: number, metadataUuid: string): Promise<void> {
  await api.delete(`/reference/${id}`, { params: { metadataUuid } })
}
