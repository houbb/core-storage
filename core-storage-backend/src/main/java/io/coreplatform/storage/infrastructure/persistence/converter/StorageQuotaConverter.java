package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageQuota;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageQuotaEntity;

/**
 * StorageQuota Entity ↔ Domain 转换器。
 */
public final class StorageQuotaConverter {

    private StorageQuotaConverter() {}

    public static StorageQuota toDomain(StorageQuotaEntity e) {
        if (e == null) return null;
        StorageQuota d = new StorageQuota();
        d.setId(e.getId());
        d.setTenantId(e.getTenantId());
        d.setResourceType(e.getResourceType());
        d.setLimitSize(e.getLimitSize());
        d.setUsedSize(e.getUsedSize());
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        return d;
    }

    public static StorageQuotaEntity toEntity(StorageQuota d) {
        if (d == null) return null;
        StorageQuotaEntity e = new StorageQuotaEntity();
        e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setResourceType(d.getResourceType());
        e.setLimitSize(d.getLimitSize());
        e.setUsedSize(d.getUsedSize());
        e.setCreateTime(d.getCreateTime());
        e.setUpdateTime(d.getUpdateTime());
        return e;
    }
}