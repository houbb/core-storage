package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageReference;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageReferenceEntity;

/**
 * Entity ↔ Domain 双向转换。
 */
public final class StorageReferenceConverter {

    private StorageReferenceConverter() {
    }

    public static StorageReference toDomain(StorageReferenceEntity e) {
        if (e == null) return null;
        StorageReference d = new StorageReference();
        d.setId(e.getId());
        d.setMetadataUuid(e.getMetadataUuid());
        d.setSystemName(e.getSystemName());
        d.setModuleName(e.getModuleName());
        d.setBusinessType(e.getBusinessType());
        d.setBusinessId(e.getBusinessId());
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        d.setCreateUser(e.getCreateUser());
        d.setUpdateUser(e.getUpdateUser());
        return d;
    }

    public static StorageReferenceEntity toEntity(StorageReference d) {
        if (d == null) return null;
        StorageReferenceEntity e = new StorageReferenceEntity();
        e.setId(d.getId());
        e.setMetadataUuid(d.getMetadataUuid());
        e.setSystemName(d.getSystemName());
        e.setModuleName(d.getModuleName());
        e.setBusinessType(d.getBusinessType());
        e.setBusinessId(d.getBusinessId());
        e.setCreateTime(d.getCreateTime());
        e.setUpdateTime(d.getUpdateTime());
        e.setCreateUser(d.getCreateUser());
        e.setUpdateUser(d.getUpdateUser());
        return e;
    }
}