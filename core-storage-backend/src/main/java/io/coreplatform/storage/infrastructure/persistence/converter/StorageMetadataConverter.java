package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageMetadata;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageMetadataEntity;

/**
 * Entity ↔ Domain 双向转换。
 */
public final class StorageMetadataConverter {

    private StorageMetadataConverter() {
    }

    public static StorageMetadata toDomain(StorageMetadataEntity e) {
        if (e == null) return null;
        StorageMetadata d = new StorageMetadata();
        d.setId(e.getId());
        d.setUuid(e.getUuid());
        d.setResourceName(e.getResourceName());
        d.setOriginalName(e.getOriginalName());
        d.setExtension(e.getExtension());
        d.setMimeType(e.getMimeType());
        d.setFileSize(e.getFileSize() != null ? e.getFileSize() : 0);
        d.setHashSha256(e.getHashSha256());
        d.setStorageDriver(e.getStorageDriver());
        d.setStorageKey(e.getStorageKey());
        d.setRelativePath(e.getRelativePath());
        d.setStorageName(e.getStorageName());
        d.setStorageType(e.getStorageType());
        d.setOwnerType(e.getOwnerType());
        d.setOwnerId(e.getOwnerId());
        d.setSystemName(e.getSystemName());
        d.setModuleName(e.getModuleName());
        d.setTags(e.getTags());
        d.setRemark(e.getRemark());
        d.setStatus(e.getStatus());
        d.setDeleted(e.getDeleted() != null && e.getDeleted() != 0);
        d.setTenantId(e.getTenantId());
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        d.setCreateUser(e.getCreateUser());
        d.setUpdateUser(e.getUpdateUser());
        d.setReferenceCount(e.getReferenceCount() != null ? e.getReferenceCount() : 0);
        return d;
    }

    public static StorageMetadataEntity toEntity(StorageMetadata d) {
        if (d == null) return null;
        StorageMetadataEntity e = new StorageMetadataEntity();
        e.setId(d.getId());
        e.setUuid(d.getUuid());
        e.setResourceName(d.getResourceName());
        e.setOriginalName(d.getOriginalName());
        e.setExtension(d.getExtension());
        e.setMimeType(d.getMimeType());
        e.setFileSize(d.getFileSize());
        e.setHashSha256(d.getHashSha256());
        e.setStorageDriver(d.getStorageDriver());
        e.setStorageKey(d.getStorageKey());
        e.setRelativePath(d.getRelativePath());
        e.setStorageName(d.getStorageName());
        e.setStorageType(d.getStorageType());
        e.setOwnerType(d.getOwnerType());
        e.setOwnerId(d.getOwnerId());
        e.setSystemName(d.getSystemName());
        e.setModuleName(d.getModuleName());
        e.setTags(d.getTags());
        e.setRemark(d.getRemark());
        e.setStatus(d.getStatus());
        e.setDeleted(d.isDeleted() ? 1 : 0);
        e.setTenantId(d.getTenantId());
        e.setCreateTime(d.getCreateTime());
        e.setUpdateTime(d.getUpdateTime());
        e.setCreateUser(d.getCreateUser());
        e.setUpdateUser(d.getUpdateUser());
        return e;
    }
}