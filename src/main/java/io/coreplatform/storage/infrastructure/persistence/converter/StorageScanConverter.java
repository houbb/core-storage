package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageScan;
import io.coreplatform.storage.application.domain.enums.ScanStatus;
import io.coreplatform.storage.application.domain.enums.ScanType;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageScanEntity;

/**
 * StorageScan Entity ↔ Domain 转换器。
 */
public final class StorageScanConverter {

    private StorageScanConverter() {}

    public static StorageScan toDomain(StorageScanEntity e) {
        if (e == null) return null;
        StorageScan d = new StorageScan();
        d.setId(e.getId());
        d.setResourceUuid(e.getResourceUuid());
        d.setScanType(safeEnum(ScanType.class, e.getScanType(), ScanType.VIRUS));
        d.setStatus(safeEnum(ScanStatus.class, e.getStatus(), ScanStatus.PENDING));
        d.setResultMessage(e.getResultMessage());
        d.setScanTime(e.getScanTime());
        d.setCreateTime(e.getCreateTime());
        return d;
    }

    public static StorageScanEntity toEntity(StorageScan d) {
        if (d == null) return null;
        StorageScanEntity e = new StorageScanEntity();
        e.setId(d.getId());
        e.setResourceUuid(d.getResourceUuid());
        e.setScanType(d.getScanType() != null ? d.getScanType().name() : null);
        e.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        e.setResultMessage(d.getResultMessage());
        e.setScanTime(d.getScanTime());
        e.setCreateTime(d.getCreateTime());
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