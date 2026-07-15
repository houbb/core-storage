package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageFile;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageFileEntity;

/**
 * Entity ↔ Domain 双向转换。
 */
public final class StorageFileConverter {

    private StorageFileConverter() {
    }

    public static StorageFile toDomain(StorageFileEntity e) {
        if (e == null) return null;
        StorageFile d = new StorageFile();
        d.setId(e.getId());
        d.setUuid(e.getUuid());
        d.setOriginalName(e.getOriginalName());
        d.setStorageName(e.getStorageName());
        d.setExtension(e.getExtension());
        d.setMimeType(e.getMimeType());
        d.setSize(e.getSize() != null ? e.getSize() : 0);
        d.setStorageType(e.getStorageType());
        d.setRelativePath(e.getRelativePath());
        d.setHash(e.getHash());
        d.setStatus(e.getStatus());
        d.setDeleted(e.getDeleted() != null && e.getDeleted() != 0);
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        d.setCreateUser(e.getCreateUser());
        d.setUpdateUser(e.getUpdateUser());
        return d;
    }

    public static StorageFileEntity toEntity(StorageFile d) {
        if (d == null) return null;
        StorageFileEntity e = new StorageFileEntity();
        e.setId(d.getId());
        e.setUuid(d.getUuid());
        e.setOriginalName(d.getOriginalName());
        e.setStorageName(d.getStorageName());
        e.setExtension(d.getExtension());
        e.setMimeType(d.getMimeType());
        e.setSize(d.getSize());
        e.setStorageType(d.getStorageType());
        e.setRelativePath(d.getRelativePath());
        e.setHash(d.getHash());
        e.setStatus(d.getStatus());
        e.setDeleted(d.isDeleted() ? 1 : 0);
        e.setCreateTime(d.getCreateTime());
        e.setUpdateTime(d.getUpdateTime());
        e.setCreateUser(d.getCreateUser());
        e.setUpdateUser(d.getUpdateUser());
        return e;
    }
}