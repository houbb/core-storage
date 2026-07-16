package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageVersionAlias;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageVersionAliasEntity;

/**
 * Entity ↔ Domain 双向转换。
 */
public final class StorageVersionAliasConverter {

    private StorageVersionAliasConverter() {
    }

    public static StorageVersionAlias toDomain(StorageVersionAliasEntity e) {
        if (e == null) return null;
        StorageVersionAlias d = new StorageVersionAlias();
        d.setId(e.getId());
        d.setVersionUuid(e.getVersionUuid());
        d.setResourceUuid(e.getResourceUuid());
        d.setAliasName(e.getAliasName());
        d.setCreateTime(e.getCreateTime());
        return d;
    }

    public static StorageVersionAliasEntity toEntity(StorageVersionAlias d) {
        if (d == null) return null;
        StorageVersionAliasEntity e = new StorageVersionAliasEntity();
        e.setId(d.getId());
        e.setVersionUuid(d.getVersionUuid());
        e.setResourceUuid(d.getResourceUuid());
        e.setAliasName(d.getAliasName());
        e.setCreateTime(d.getCreateTime());
        return e;
    }
}