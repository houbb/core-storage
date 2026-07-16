package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageVersion;
import io.coreplatform.storage.application.domain.enums.VersionStatus;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageVersionEntity;

/**
 * Entity ↔ Domain 双向转换。
 */
public final class StorageVersionConverter {

    private StorageVersionConverter() {
    }

    public static StorageVersion toDomain(StorageVersionEntity e) {
        if (e == null) return null;
        StorageVersion d = new StorageVersion();
        d.setId(e.getId());
        d.setVersionUuid(e.getVersionUuid());
        d.setResourceUuid(e.getResourceUuid());
        d.setMetadataUuid(e.getMetadataUuid());
        d.setVersionName(e.getVersionName());
        d.setVersionCode(e.getVersionCode() != null ? e.getVersionCode() : 1);
        d.setStatus(safeEnum(VersionStatus.class, e.getStatus(), VersionStatus.DRAFT));
        d.setPublished(e.getPublished() != null && e.getPublished() == 1);
        d.setLatest(e.getLatest() != null && e.getLatest() == 1);
        d.setChecksum(e.getChecksum());
        d.setCreateTime(e.getCreateTime());
        d.setPublishTime(e.getPublishTime());
        return d;
    }

    public static StorageVersionEntity toEntity(StorageVersion d) {
        if (d == null) return null;
        StorageVersionEntity e = new StorageVersionEntity();
        e.setId(d.getId());
        e.setVersionUuid(d.getVersionUuid());
        e.setResourceUuid(d.getResourceUuid());
        e.setMetadataUuid(d.getMetadataUuid());
        e.setVersionName(d.getVersionName());
        e.setVersionCode(d.getVersionCode());
        e.setStatus(d.getStatus() != null ? d.getStatus().name() : "DRAFT");
        e.setPublished(d.isPublished() ? 1 : 0);
        e.setLatest(d.isLatest() ? 1 : 0);
        e.setChecksum(d.getChecksum());
        e.setCreateTime(d.getCreateTime());
        e.setPublishTime(d.getPublishTime());
        return e;
    }

    private static <E extends Enum<E>> E safeEnum(Class<E> enumClass, String value, E defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }
}