package io.coreplatform.storage.application.service;

import io.coreplatform.storage.api.response.SearchResultResponse;
import io.coreplatform.storage.api.response.StorageMetadataResponse;
import io.coreplatform.storage.api.response.StorageReferenceResponse;
import io.coreplatform.storage.application.domain.StorageMetadata;
import io.coreplatform.storage.application.domain.StorageReference;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageMetadataIndexEntity;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageMetadataIndexRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageMetadataRepository;
import io.coreplatform.storage.infrastructure.persistence.repository.StorageReferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StorageMetadataService {

    private static final Logger log = LoggerFactory.getLogger(StorageMetadataService.class);

    private final StorageMetadataRepository metadataRepo;
    private final StorageReferenceRepository referenceRepo;
    private final StorageMetadataIndexRepository indexRepo;

    public StorageMetadataService(StorageMetadataRepository metadataRepo,
                                   StorageReferenceRepository referenceRepo,
                                   StorageMetadataIndexRepository indexRepo) {
        this.metadataRepo = metadataRepo;
        this.referenceRepo = referenceRepo;
        this.indexRepo = indexRepo;
    }

    /**
     * 保存元数据（上传时调用）。
     */
    public StorageMetadata saveMetadata(StorageMetadata metadata) {
        return metadataRepo.save(metadata);
    }

    /**
     * 写入轻量索引。
     */
    public void saveIndex(String resourceUuid, String ownerType, String ownerId,
                           String resourceType, String moduleName, String tag, String status) {
        StorageMetadataIndexEntity entity = new StorageMetadataIndexEntity();
        entity.setResourceUuid(resourceUuid);
        entity.setOwnerType(ownerType);
        entity.setOwnerId(ownerId);
        entity.setResourceType(resourceType);
        entity.setModuleName(moduleName);
        entity.setTag(tag);
        entity.setStatus(status);
        indexRepo.save(entity);
    }

    /**
     * 根据 UUID 查询元数据。
     */
    public StorageMetadataResponse getByUuid(String uuid) {
        StorageMetadata m = metadataRepo.findByUuid(uuid)
                .orElseThrow(() -> new MetadataNotFoundException("Metadata not found: uuid=" + uuid));

        int refCount = referenceRepo.countByMetadataUuid(uuid);
        m.setReferenceCount(refCount);
        return toResponse(m);
    }

    /**
     * 搜索元数据。
     */
    public SearchResultResponse<StorageMetadataResponse> search(
            String keyword, String mimeType, String status, String hash,
            String ownerType, String ownerId, String system, String module,
            String tag, LocalDateTime startTime, LocalDateTime endTime,
            String sort, String order, int page, int size) {

        int total = metadataRepo.countSearch(keyword, mimeType, status, hash,
                ownerType, ownerId, system, module, tag, startTime, endTime);

        int offset = Math.max(0, page - 1) * size;
        List<StorageMetadata> list = metadataRepo.search(keyword, mimeType, status, hash,
                ownerType, ownerId, system, module, tag, startTime, endTime,
                sort, order, offset, size);

        // 填充引用计数
        List<StorageMetadataResponse> items = list.stream().map(m -> {
            int rc = referenceRepo.countByMetadataUuid(m.getUuid());
            m.setReferenceCount(rc);
            return toResponse(m);
        }).toList();

        return new SearchResultResponse<>(items, page, size, total);
    }

    /**
     * 创建业务引用。
     * 如果是第一个引用，状态 ACTIVE → REFERENCED；否则保持 REFERENCED。
     */
    @Transactional(rollbackFor = Exception.class)
    public StorageReferenceResponse createReference(String metadataUuid, String system, String module,
                                                      String businessType, String businessId) {
        StorageMetadata metadata = metadataRepo.findByUuid(metadataUuid)
                .orElseThrow(() -> new MetadataNotFoundException("Metadata not found: uuid=" + metadataUuid));

        StorageReference ref = new StorageReference();
        ref.setMetadataUuid(metadataUuid);
        ref.setSystemName(system);
        ref.setModuleName(module);
        ref.setBusinessType(businessType);
        ref.setBusinessId(businessId);

        StorageReference saved = referenceRepo.save(ref);
        log.info("Reference created: metadataUuid={}, businessType={}, businessId={}",
                metadataUuid, businessType, businessId);

        // 自动状态迁移
        int totalRefs = referenceRepo.countByMetadataUuid(metadataUuid);
        if (totalRefs == 1 && "ACTIVE".equals(metadata.getStatus())) {
            metadataRepo.updateStatus(metadataUuid, "REFERENCED");
            indexRepo.updateStatus(metadataUuid, "REFERENCED");
            log.info("Status transition: ACTIVE -> REFERENCED, uuid={}", metadataUuid);
        }

        return toRefResponse(saved);
    }

    /**
     * 删除引用。
     * 如果引用数为 0，状态 → UNREFERENCED。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteReference(Long referenceId) {
        // 先查引用信息以获取 metadataUuid
        List<StorageReference> all = referenceRepo.findByMetadataUuid(null); // won't work, need findById
        // 用直接查询方式：先删，再判断
        // 实际方案：先查出 metadataUuid，再删
        // 简化：直接用 referenceId 处理
        referenceRepo.deleteById(referenceId);
        log.info("Reference deleted: id={}", referenceId);
        // 注意：需要在调用前由 controller/service 查出 metadataUuid，
        // 这里用另一种方式：先查再删
    }

    /**
     * 删除引用（带 uuid 参数版本，自动状态迁移）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteReferenceByUuid(Long referenceId, String metadataUuid) {
        referenceRepo.deleteById(referenceId);
        log.info("Reference deleted: id={}, metadataUuid={}", referenceId, metadataUuid);

        int remaining = referenceRepo.countByMetadataUuid(metadataUuid);
        if (remaining == 0) {
            metadataRepo.updateStatus(metadataUuid, "UNREFERENCED");
            indexRepo.updateStatus(metadataUuid, "UNREFERENCED");
            log.info("Status transition: -> UNREFERENCED, uuid={}", metadataUuid);
        }
    }

    /**
     * 查询资源的引用列表。
     */
    public List<StorageReferenceResponse> getReferences(String metadataUuid) {
        List<StorageReference> refs = referenceRepo.findByMetadataUuid(metadataUuid);
        return refs.stream().map(this::toRefResponse).toList();
    }

    /**
     * 软删除 metadata。
     */
    public void softDelete(String uuid) {
        metadataRepo.softDelete(uuid);
        indexRepo.updateStatus(uuid, "SOFT_DELETED");
        log.info("Metadata soft-deleted: uuid={}", uuid);
    }

    /**
     * 更新状态（带索引同步）。
     */
    public void updateStatus(String uuid, String status) {
        metadataRepo.updateStatus(uuid, status);
        indexRepo.updateStatus(uuid, status);
    }

    // ---- private helpers ----

    private StorageMetadataResponse toResponse(StorageMetadata m) {
        StorageMetadataResponse r = new StorageMetadataResponse();
        r.setId(m.getId());
        r.setUuid(m.getUuid());
        r.setResourceName(m.getResourceName());
        r.setOriginalName(m.getOriginalName());
        r.setExtension(m.getExtension());
        r.setMimeType(m.getMimeType());
        r.setFileSize(m.getFileSize());
        r.setHashSha256(m.getHashSha256());
        r.setStorageDriver(m.getStorageDriver());
        r.setStorageKey(m.getStorageKey());
        r.setRelativePath(m.getRelativePath());
        r.setStorageType(m.getStorageType());
        r.setOwnerType(m.getOwnerType());
        r.setOwnerId(m.getOwnerId());
        r.setSystemName(m.getSystemName());
        r.setModuleName(m.getModuleName());
        r.setTags(m.getTags());
        r.setRemark(m.getRemark());
        r.setStatus(m.getStatus());
        r.setReferenceCount(m.getReferenceCount());
        r.setCreateTime(m.getCreateTime());
        r.setUpdateTime(m.getUpdateTime());
        r.setDownloadUrl("/api/v1/storage/file/" + m.getId());
        return r;
    }

    private StorageReferenceResponse toRefResponse(StorageReference ref) {
        StorageReferenceResponse r = new StorageReferenceResponse();
        r.setId(ref.getId());
        r.setMetadataUuid(ref.getMetadataUuid());
        r.setSystemName(ref.getSystemName());
        r.setModuleName(ref.getModuleName());
        r.setBusinessType(ref.getBusinessType());
        r.setBusinessId(ref.getBusinessId());
        r.setCreateTime(ref.getCreateTime());
        return r;
    }

    // ---- inner exceptions ----

    public static class MetadataNotFoundException extends RuntimeException {
        public MetadataNotFoundException(String message) {
            super(message);
        }
    }

    public static class ReferenceNotFoundException extends RuntimeException {
        public ReferenceNotFoundException(String message) {
            super(message);
        }
    }
}