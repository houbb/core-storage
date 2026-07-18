package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.StorageVersionHistory;
import io.coreplatform.storage.application.domain.enums.VersionAction;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageVersionHistoryEntity;

/**
 * Entity ↔ Domain 双向转换。
 */
public final class StorageVersionHistoryConverter {

    private StorageVersionHistoryConverter() {
    }

    public static StorageVersionHistory toDomain(StorageVersionHistoryEntity e) {
        if (e == null) return null;
        StorageVersionHistory d = new StorageVersionHistory();
        d.setId(e.getId());
        d.setVersionUuid(e.getVersionUuid());
        d.setResourceUuid(e.getResourceUuid());
        d.setAction(safeEnum(VersionAction.class, e.getAction(), VersionAction.CREATED));
        d.setPreviousStatus(e.getPreviousStatus());
        d.setNewStatus(e.getNewStatus());
        d.setOperatorId(e.getOperatorId());
        d.setRemark(e.getRemark());
        d.setCreateTime(e.getCreateTime());
        return d;
    }

    public static StorageVersionHistoryEntity toEntity(StorageVersionHistory d) {
        if (d == null) return null;
        StorageVersionHistoryEntity e = new StorageVersionHistoryEntity();
        e.setId(d.getId());
        e.setVersionUuid(d.getVersionUuid());
        e.setResourceUuid(d.getResourceUuid());
        e.setAction(d.getAction() != null ? d.getAction().name() : "CREATED");
        e.setPreviousStatus(d.getPreviousStatus());
        e.setNewStatus(d.getNewStatus());
        e.setOperatorId(d.getOperatorId());
        e.setRemark(d.getRemark());
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