import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { StorageMetadataItem, SearchResult, SearchParams } from '../api/storage'
import { searchMetadata, getMetadata, getReferences, createReference, deleteReference } from '../api/storage'
import type { StorageReferenceItem } from '../api/storage'

export const useMetadataStore = defineStore('metadata', () => {
  const items = ref<StorageMetadataItem[]>([])
  const total = ref(0)
  const totalPages = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(20)
  const loading = ref(false)

  async function search(params: SearchParams) {
    loading.value = true
    try {
      const result: SearchResult<StorageMetadataItem> = await searchMetadata(params)
      items.value = result.items
      total.value = result.total
      totalPages.value = result.totalPages
      currentPage.value = result.page
      pageSize.value = result.size
      return result
    } finally {
      loading.value = false
    }
  }

  async function fetchDetail(uuid: string): Promise<StorageMetadataItem> {
    return await getMetadata(uuid)
  }

  async function fetchReferences(metadataUuid: string): Promise<StorageReferenceItem[]> {
    return await getReferences(metadataUuid)
  }

  async function addReference(req: {
    metadataUuid: string
    system: string
    module: string
    businessType: string
    businessId: string
  }) {
    return await createReference(req)
  }

  async function removeReference(id: number, metadataUuid: string) {
    await deleteReference(id, metadataUuid)
  }

  return { items, total, totalPages, currentPage, pageSize, loading, search, fetchDetail, fetchReferences, addReference, removeReference }
})
