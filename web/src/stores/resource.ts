import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  searchResources,
  getResource,
  updateResource,
  deleteResource,
  getResourceProperties,
  setResourceProperties,
  type ResourceItem,
  type SearchResult,
  type SearchResourceParams,
} from '../api/storage'

export const useResourceStore = defineStore('resource', () => {
  const items = ref<ResourceItem[]>([])
  const loading = ref(false)
  const total = ref(0)
  const currentPage = ref(1)
  const totalPages = ref(1)
  const size = ref(20)

  async function search(params: SearchResourceParams) {
    loading.value = true
    try {
      const result: SearchResult<ResourceItem> = await searchResources(params)
      items.value = result.items
      total.value = result.total
      currentPage.value = result.page
      totalPages.value = result.totalPages
      size.value = result.size
    } finally {
      loading.value = false
    }
  }

  async function fetchDetail(uuid: string): Promise<ResourceItem> {
    return await getResource(uuid)
  }

  async function doUpdate(uuid: string, data: {
    resourceName?: string
    description?: string
    category?: string
    visibility?: string
    tags?: string[]
  }): Promise<ResourceItem> {
    const result = await updateResource(uuid, data)
    // 刷新列表中对应项
    const idx = items.value.findIndex(i => i.resourceUuid === uuid)
    if (idx >= 0) {
      items.value[idx] = result
    }
    return result
  }

  async function doDelete(uuid: string): Promise<void> {
    await deleteResource(uuid)
    items.value = items.value.filter(i => i.resourceUuid !== uuid)
    total.value = Math.max(0, total.value - 1)
  }

  async function fetchProperties(uuid: string): Promise<Record<string, string>> {
    return await getResourceProperties(uuid)
  }

  async function doSetProperties(uuid: string, properties: Record<string, string>): Promise<void> {
    await setResourceProperties(uuid, properties)
  }

  return {
    items,
    loading,
    total,
    currentPage,
    totalPages,
    size,
    search,
    fetchDetail,
    doUpdate,
    doDelete,
    fetchProperties,
    doSetProperties,
  }
})