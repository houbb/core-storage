package io.coreplatform.storage.infrastructure.persistence.converter;

import io.coreplatform.storage.application.domain.SyncTask;
import io.coreplatform.storage.application.domain.enums.SyncTaskStatus;
import io.coreplatform.storage.application.domain.enums.SyncTaskType;
import io.coreplatform.storage.infrastructure.persistence.entity.SyncTaskEntity;

/**
 * Entity ↔ Domain 双向转换。
 */
public final class SyncTaskConverter {

    private SyncTaskConverter() {
    }

    public static SyncTask toDomain(SyncTaskEntity e) {
        if (e == null) return null;
        SyncTask d = new SyncTask();
        d.setId(e.getId());
        d.setTaskType(safeEnum(SyncTaskType.class, e.getTaskType(), SyncTaskType.SYNC));
        d.setResourceUuid(e.getResourceUuid());
        d.setSourceProfile(e.getSourceProfile());
        d.setTargetProfile(e.getTargetProfile());
        d.setStatus(safeEnum(SyncTaskStatus.class, e.getStatus(), SyncTaskStatus.PENDING));
        d.setProgress(e.getProgress());
        d.setErrorMessage(e.getErrorMessage());
        d.setCreateTime(e.getCreateTime());
        d.setUpdateTime(e.getUpdateTime());
        return d;
    }

    public static SyncTaskEntity toEntity(SyncTask d) {
        if (d == null) return null;
        SyncTaskEntity e = new SyncTaskEntity();
        e.setId(d.getId());
        e.setTaskType(d.getTaskType() != null ? d.getTaskType().name() : "SYNC");
        e.setResourceUuid(d.getResourceUuid());
        e.setSourceProfile(d.getSourceProfile());
        e.setTargetProfile(d.getTargetProfile());
        e.setStatus(d.getStatus() != null ? d.getStatus().name() : "PENDING");
        e.setProgress(d.getProgress());
        e.setErrorMessage(d.getErrorMessage());
        e.setCreateTime(d.getCreateTime());
        e.setUpdateTime(d.getUpdateTime());
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