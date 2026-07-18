package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageRegion;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageRegionEntity;

/**
 * StorageRegion Entity ↔ Domain 转换器。
 */
public final class StorageRegionConverter {

    private StorageRegionConverter() {}

    public static StorageRegion toDomain(StorageRegionEntity e) {
        if (e == null) return null;
        StorageRegion d = new StorageRegion();
        d.setRegionCode(e.getRegionCode());
        d.setRegionName(e.getRegionName());
        d.setEndpoint(e.getEndpoint());
        d.setDriverName(e.getDriverName());
        d.setCreateTime(e.getCreateTime());
        return d;
    }

    public static StorageRegionEntity toEntity(StorageRegion d) {
        if (d == null) return null;
        StorageRegionEntity e = new StorageRegionEntity();
        e.setRegionCode(d.getRegionCode());
        e.setRegionName(d.getRegionName());
        e.setEndpoint(d.getEndpoint());
        e.setDriverName(d.getDriverName());
        e.setCreateTime(d.getCreateTime());
        return e;
    }
}